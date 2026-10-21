package com.tsport.links.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tsport.links.R
import com.tsport.links.auth.AuthRepository
import com.tsport.links.auth.LoginActivity
import com.tsport.links.databinding.ActivityMainBinding
import com.tsport.links.links.LinkExtractor
import com.tsport.links.links.LinkItem
import com.tsport.links.links.TcoResolver
import com.tsport.links.search.ApiFallbackScraper
import com.tsport.links.search.DomLinkScraper
import com.tsport.links.search.ScrapeBridge
import com.tsport.links.search.SearchState
import com.tsport.links.search.SearchViewModel
import com.tsport.links.search.SessionExpiredException
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()
    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(LinkExtractor(TcoResolver()))
    }
    private lateinit var bridge: ScrapeBridge
    private lateinit var domScraper: DomLinkScraper
    private lateinit var fallbackScraper: ApiFallbackScraper
    private lateinit var adapter: LinkAdapter
    private var loginShown = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        adapter = LinkAdapter(::openLink)
        binding.linkList.layoutManager = LinearLayoutManager(this)
        binding.linkList.adapter = adapter

        bridge = ScrapeBridge()
        domScraper = DomLinkScraper(binding.scrapeWebView, bridge, readAsset("scrape_dom.js"))
        fallbackScraper = ApiFallbackScraper(binding.scrapeWebView, bridge, readAsset("fallback_api.js"))
        configureWebView(binding.scrapeWebView, bridge)

        binding.hashtagInput.setText(prefs().getString(KEY_LAST_HASHTAG, ""))
        binding.searchButton.setOnClickListener { startSearch() }
        binding.logoutButton.setOnClickListener { confirmLogout() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!authRepository.isLoggedIn()) {
            openLogin()
        } else {
            if (loginShown) viewModel.reset()
            loginShown = false
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(webView: WebView, bridge: ScrapeBridge) {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = TcoResolver.USER_AGENT
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webView.addJavascriptInterface(bridge, "TSportBridge")
        webView.isFocusable = false
        webView.isClickable = false
    }

    private fun startSearch() {
        if (!isNetworkAvailable()) {
            viewModel.fail(getString(R.string.status_no_internet))
            return
        }
        if (!authRepository.isLoggedIn()) {
            viewModel.fail(getString(R.string.session_expired))
            return
        }
        val raw = binding.hashtagInput.text.toString()
        prefs().edit().putString(KEY_LAST_HASHTAG, raw).apply()
        getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(binding.hashtagInput.windowToken, 0)
        viewModel.search(raw) { searchUrl ->
            val domCandidates = domScraper.scrape(searchUrl)
            if (!authRepository.isLoggedIn()) {
                throw SessionExpiredException()
            }
            domCandidates + fallbackScraper.scrape(authRepository.csrfToken().orEmpty())
        }
    }

    private fun render(state: SearchState) {
        val searching = state is SearchState.Searching
        binding.progress.visibility = if (searching) View.VISIBLE else View.GONE
        binding.searchButton.isEnabled = !searching
        binding.logoutButton.isEnabled = !searching
        binding.hashtagInput.isEnabled = !searching

        when (state) {
            SearchState.Idle -> {
                binding.statusText.setText(R.string.status_idle)
                adapter.submit(emptyList())
            }
            SearchState.Searching -> {
                binding.statusText.setText(R.string.status_searching)
                adapter.submit(emptyList())
            }
            is SearchState.Success -> {
                binding.statusText.text = getString(R.string.status_found, state.links.size)
                adapter.submit(state.links)
                if (isTelevision()) binding.linkList.requestFocus()
            }
            SearchState.Empty -> {
                binding.statusText.setText(R.string.status_empty)
                adapter.submit(emptyList())
            }
            is SearchState.Failure -> {
                binding.statusText.text = state.message
                adapter.submit(emptyList())
                if (state.message == getString(R.string.session_expired)) {
                    openLogin()
                }
                if (isTelevision()) binding.searchButton.requestFocus()
            }
        }
    }

    private fun openLink(item: LinkItem) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.status_no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.logout_title)
            .setMessage(R.string.logout_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ ->
                authRepository.logout {
                    WebStorage.getInstance().deleteAllData()
                    viewModel.reset()
                    binding.hashtagInput.setText("")
                    adapter.submit(emptyList())
                    binding.statusText.setText(R.string.session_expired)
                    openLogin()
                }
            }
            .show()
    }

    private fun openLogin() {
        if (loginShown) return
        loginShown = true
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun applySystemBarInsets() {
        val base = (24 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                base + bars.left,
                base + bars.top,
                base + bars.right,
                base + bars.bottom
            )
            insets
        }
    }

    override fun onDestroy() {
        viewModel.cancel()
        binding.scrapeWebView.destroy()
        super.onDestroy()
    }

    private fun isNetworkAvailable(): Boolean {
        val manager = getSystemService<ConnectivityManager>() ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isTelevision(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    private fun readAsset(name: String): String =
        assets.open(name).bufferedReader().use { it.readText() }

    private fun prefs() = getSharedPreferences("tsport", MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_HASHTAG = "last_hashtag"
    }
}
