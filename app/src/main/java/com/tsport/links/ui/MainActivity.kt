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
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
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
import com.tsport.links.matchday.MatchDayState
import com.tsport.links.matchday.MatchDayViewModel
import com.tsport.links.matchday.TheSportsDbSource
import com.tsport.links.matchday.TodayMatch
import com.tsport.links.search.ApiFallbackScraper
import com.tsport.links.search.DomLinkScraper
import com.tsport.links.search.ScrapeBridge
import com.tsport.links.search.SearchOptions
import com.tsport.links.search.SearchQueryBuilder
import com.tsport.links.search.SearchState
import com.tsport.links.search.SearchTab
import com.tsport.links.search.SearchViewModel
import com.tsport.links.search.SessionExpiredException
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()
    private val viewModel: SearchViewModel by viewModels {
        SearchViewModelFactory(LinkExtractor(TcoResolver()))
    }
    private val matchDayViewModel: MatchDayViewModel by viewModels {
        MatchDayViewModelFactory(TheSportsDbSource())
    }
    private lateinit var bridge: ScrapeBridge
    private lateinit var domScraper: DomLinkScraper
    private lateinit var fallbackScraper: ApiFallbackScraper
    private lateinit var adapter: LinkAdapter
    private lateinit var matchAdapter: MatchAdapter
    private var currentSearchState: SearchState = SearchState.Idle
    private var currentMatchDayState: MatchDayState = MatchDayState.Loading
    private var matchdayFocusHandled = false
    private var lastMatchDayFocusKind: MatchDayFocusKind? = null
    private var matchdayPanelShown = false
    private var loginShown = false
    private var lastOptions: SearchOptions? = null
    private var lastTabFallback = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        adapter = LinkAdapter(::openLink)
        binding.linkList.layoutManager = LinearLayoutManager(this)
        binding.linkList.adapter = adapter

        matchAdapter = MatchAdapter(::startMatchSearch)
        binding.matchList.layoutManager = LinearLayoutManager(this)
        binding.matchList.adapter = matchAdapter

        bridge = ScrapeBridge()
        domScraper = DomLinkScraper(binding.scrapeWebView, bridge, readAsset("scrape_dom.js"))
        fallbackScraper = ApiFallbackScraper(
            binding.scrapeWebView,
            bridge,
            readAsset("fallback_api.js"),
            transactionScript = readAsset("transaction_bundle.js")
        )
        configureWebView(binding.scrapeWebView, bridge)

        binding.hashtagInput.setText(prefs().getString(KEY_LAST_HASHTAG, ""))
        binding.broadcastsCheck.isChecked = prefs().getBoolean(KEY_INCLUDE_BROADCASTS, false)
        binding.linksOnlyCheck.isChecked = prefs().getBoolean(KEY_LINKS_ONLY, false)
        if (prefs().getBoolean(KEY_TAB_LATEST, true)) {
            binding.tabLatest.isChecked = true
        } else {
            binding.tabPopular.isChecked = true
        }
        savedInstanceState?.let { state ->
            val hashtag = state.getString(KEY_STATE_HASHTAG)
            if (hashtag != null) {
                lastOptions = SearchOptions(
                    query = hashtag,
                    includeBroadcasts = state.getBoolean(KEY_STATE_INCLUDE_BROADCASTS, false),
                    linksOnly = state.getBoolean(KEY_STATE_LINKS_ONLY, false),
                    tab = if (state.getBoolean(KEY_STATE_TAB_LATEST, true)) {
                        SearchTab.LATEST
                    } else {
                        SearchTab.POPULAR
                    }
                )
                lastTabFallback = state.getBoolean(KEY_STATE_TAB_FALLBACK, false)
            }
        }
        binding.broadcastsCheck.setOnCheckedChangeListener { _, _ -> persistOptionSelection() }
        binding.linksOnlyCheck.setOnCheckedChangeListener { _, _ -> persistOptionSelection() }
        binding.tabGroup.setOnCheckedChangeListener { _, _ -> persistOptionSelection() }
        binding.searchButton.setOnClickListener { startSearch() }
        binding.logoutButton.setOnClickListener { confirmLogout() }
        binding.matchRetryButton.setOnClickListener { matchDayViewModel.retry() }
        binding.matchdayBackButton.setOnClickListener { showMatchdayAgain() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                matchDayViewModel.state.collect(::renderMatchDay)
            }
        }

        matchDayViewModel.load()
    }

    override fun onResume() {
        super.onResume()
        if (!authRepository.isLoggedIn()) {
            openLogin()
        } else {
            if (loginShown) viewModel.reset()
            loginShown = false
            if (currentSearchState is SearchState.Idle) matchDayViewModel.load()
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
        val options = SearchOptions(
            query = binding.hashtagInput.text.toString(),
            includeBroadcasts = binding.broadcastsCheck.isChecked,
            linksOnly = binding.linksOnlyCheck.isChecked,
            tab = if (binding.tabLatest.isChecked) SearchTab.LATEST else SearchTab.POPULAR
        )
        lastOptions = options
        lastTabFallback = false
        prefs().edit()
            .putString(KEY_LAST_HASHTAG, options.query)
            .putBoolean(KEY_INCLUDE_BROADCASTS, options.includeBroadcasts)
            .putBoolean(KEY_LINKS_ONLY, options.linksOnly)
            .putBoolean(KEY_TAB_LATEST, options.tab == SearchTab.LATEST)
            .apply()
        getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(binding.hashtagInput.windowToken, 0)
        viewModel.search(options) { searchUrl ->
            var tabFallback = false
            val domCandidates = domScraper.scrape(
                searchUrl,
                options.tab,
                timeoutMs = DOM_TIMEOUT_MS
            )
            if (options.tab == SearchTab.LATEST && !domScraper.lastTabSelected) {
                tabFallback = true
            }

            // "En son" can be empty for a query whose broadcasts only show in
            // "Popüler" (and vice versa). Try the other tab before giving up.
            var effectiveTab = options.tab
            val fallbackCandidates = if (domCandidates.isEmpty()) {
                val fallbackTab = if (options.tab == SearchTab.LATEST) {
                    SearchTab.POPULAR
                } else {
                    SearchTab.LATEST
                }
                val fallbackUrl = SearchQueryBuilder.buildSearchUrl(
                    options.copy(tab = fallbackTab)
                )
                effectiveTab = fallbackTab
                if (fallbackUrl == null) {
                    emptyList()
                } else {
                    val result = domScraper.scrape(
                        fallbackUrl,
                        fallbackTab,
                        timeoutMs = FALLBACK_DOM_TIMEOUT_MS
                    )
                    if (fallbackTab == SearchTab.LATEST && !domScraper.lastTabSelected) {
                        tabFallback = true
                    }
                    result
                }
            } else {
                emptyList()
            }
            lastTabFallback = tabFallback

            if (!authRepository.isLoggedIn()) {
                throw SessionExpiredException()
            }
            val apiCandidates = if (
                domCandidates.isEmpty() && fallbackCandidates.isEmpty()
            ) {
                fallbackScraper.scrape(
                    csrfToken = authRepository.csrfToken().orEmpty(),
                    query = Uri.parse(searchUrl).getQueryParameter("q").orEmpty(),
                    tab = effectiveTab,
                    timeoutMs = API_TIMEOUT_MS
                )
            } else {
                emptyList()
            }
            domCandidates + fallbackCandidates + apiCandidates
        }
    }

    private fun render(state: SearchState) {
        currentSearchState = state
        val searching = state is SearchState.Searching
        binding.progress.visibility = if (searching) View.VISIBLE else View.GONE
        binding.searchButton.isEnabled = !searching
        binding.logoutButton.isEnabled = !searching
        binding.hashtagInput.isEnabled = !searching
        binding.broadcastsCheck.isEnabled = !searching
        binding.linksOnlyCheck.isEnabled = !searching
        binding.tabPopular.isEnabled = !searching
        binding.tabLatest.isEnabled = !searching

        val tabWarning = if (
            state is SearchState.Success &&
            shouldShowTabFallback(lastOptions, lastTabFallback)
        ) {
            getString(R.string.status_tab_fallback)
        } else {
            null
        }

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
                val found = getString(R.string.status_found, state.links.size)
                binding.statusText.text = withTabWarning(found, tabWarning)
                adapter.submit(state.links)
            }
            SearchState.Empty -> {
                val empty = getString(R.string.status_empty)
                binding.statusText.text = withTabWarning(empty, tabWarning)
                adapter.submit(emptyList())
            }
            is SearchState.Failure -> {
                binding.statusText.text = state.message
                adapter.submit(emptyList())
                if (state.message == getString(R.string.session_expired)) {
                    openLogin()
                }
            }
        }

        updatePanelVisibility()
        if (isTelevision()) {
            when (state) {
                SearchState.Searching -> binding.matchdayBackButton.post {
                    if (currentSearchState is SearchState.Searching) {
                        binding.matchdayBackButton.requestFocus()
                    }
                }
                is SearchState.Success -> binding.linkList.post {
                    if (currentSearchState is SearchState.Success) {
                        binding.linkList.requestFocus()
                    }
                }
                is SearchState.Failure -> {
                    if (state.message != getString(R.string.session_expired)) {
                        binding.searchButton.requestFocus()
                    }
                }
                else -> Unit
            }
        }
    }

    private fun renderMatchDay(state: MatchDayState) {
        currentMatchDayState = state
        val focusKind = focusKindOf(state)
        if (focusKind != lastMatchDayFocusKind) {
            lastMatchDayFocusKind = focusKind
            matchdayFocusHandled = false
        }
        if (state is MatchDayState.Loading) {
            binding.matchLoadingGroup.isVisible = true
            binding.matchStateContainer.isVisible = false
            binding.matchList.isVisible = false
            binding.matchDateText.text = ""
            binding.matchSummary.isVisible = false
            matchAdapter.submit(emptyList())
        }
        when (state) {
            MatchDayState.Loading -> Unit
            is MatchDayState.Success -> {
                binding.matchLoadingGroup.isVisible = false
                binding.matchStateContainer.isVisible = false
                binding.matchList.isVisible = true
                binding.matchDateText.text = formatMatchDate(state.date).orEmpty()
                matchAdapter.submit(state.matches)
                binding.matchSummary.text = resources.getQuantityString(
                    R.plurals.matchday_summary,
                    state.matches.size,
                    state.matches.size
                )
                binding.matchSummary.isVisible = true
            }
            MatchDayState.Empty -> {
                binding.matchLoadingGroup.isVisible = false
                binding.matchList.isVisible = false
                binding.matchStateContainer.isVisible = true
                binding.matchStateIcon.setImageResource(R.drawable.ic_football)
                binding.matchStateTitle.setText(R.string.matchday_empty_title)
                binding.matchStateCopy.setText(R.string.matchday_empty_copy)
                binding.matchStateCopy.isVisible = true
                binding.matchRetryButton.isVisible = true
                binding.matchDateText.text = ""
                binding.matchSummary.setText(R.string.matchday_summary_empty)
                binding.matchSummary.isVisible = true
                matchAdapter.submit(emptyList())
            }
            MatchDayState.Failure -> {
                binding.matchLoadingGroup.isVisible = false
                binding.matchList.isVisible = false
                binding.matchStateContainer.isVisible = true
                binding.matchStateIcon.setImageResource(R.drawable.ic_football)
                binding.matchStateTitle.setText(R.string.matchday_error_title)
                binding.matchStateCopy.setText(R.string.matchday_error_copy)
                binding.matchStateCopy.isVisible = true
                binding.matchRetryButton.isVisible = true
                binding.matchDateText.text = ""
                binding.matchSummary.isVisible = false
                matchAdapter.submit(emptyList())
            }
        }
        updatePanelVisibility()
    }

    private fun updatePanelVisibility() {
        val showMatchday = shouldShowMatchday(currentSearchState)
        if (showMatchday && !matchdayPanelShown) {
            matchdayFocusHandled = false
        }
        matchdayPanelShown = showMatchday
        binding.matchdayPanel.isVisible = showMatchday
        binding.resultsPanel.isVisible = !showMatchday
        binding.matchdayBackButton.isVisible = shouldShowMatchdayBack(
            currentSearchState,
            getString(R.string.session_expired)
        )
        binding.matchdayAttribution.isVisible = showMatchday
        if (showMatchday) applyMatchDayFocus()
    }

    private fun applyMatchDayFocus() {
        val target = matchDayFocusTarget(
            isTelevision = isTelevision(),
            searchState = currentSearchState,
            matchState = currentMatchDayState,
            isTyping = isTyping(),
            panelVisible = binding.matchdayPanel.isVisible &&
                shouldShowMatchday(currentSearchState),
            focusHandled = matchdayFocusHandled
        )
        when (target) {
            MatchDayFocusTarget.FIRST_MATCH -> focusFirstMatchAfterLayout()
            MatchDayFocusTarget.SEARCH_FIELD -> {
                if (binding.hashtagInput.requestFocus()) {
                    matchdayFocusHandled = true
                }
            }
            MatchDayFocusTarget.NONE -> Unit
        }
    }

    private fun focusFirstMatchAfterLayout(attempt: Int = 0) {
        if (currentSearchState !is SearchState.Idle || isTyping()) return
        val list = binding.matchList
        val layoutManager = list.layoutManager as? LinearLayoutManager ?: return
        layoutManager.scrollToPosition(0)
        if (layoutManager.findViewByPosition(0)?.requestFocus() == true) {
            matchdayFocusHandled = true
            return
        }
        when {
            attempt == 0 -> list.doOnLayout {
                focusFirstMatchAfterLayout(attempt = 1)
            }
            attempt < MAX_MATCH_FOCUS_ATTEMPTS -> list.postOnAnimation {
                focusFirstMatchAfterLayout(attempt + 1)
            }
            else -> if (binding.hashtagInput.requestFocus()) {
                matchdayFocusHandled = true
            }
        }
    }

    private fun isTyping(): Boolean =
        binding.hashtagInput.hasFocus() && binding.hashtagInput.text.isNotBlank()

    private fun focusKindOf(state: MatchDayState): MatchDayFocusKind = when (state) {
        MatchDayState.Loading -> MatchDayFocusKind.LOADING
        MatchDayState.Empty -> MatchDayFocusKind.EMPTY
        MatchDayState.Failure -> MatchDayFocusKind.FAILURE
        is MatchDayState.Success ->
            if (state.matches.isEmpty()) {
                MatchDayFocusKind.EMPTY_LIST
            } else {
                MatchDayFocusKind.MATCHES
            }
    }

    private enum class MatchDayFocusKind {
        LOADING,
        MATCHES,
        EMPTY_LIST,
        EMPTY,
        FAILURE
    }

    private fun startMatchSearch(match: TodayMatch) {
        binding.hashtagInput.setText(match.query)
        binding.broadcastsCheck.isChecked = true
        persistOptionSelection()
        startSearch()
    }

    private fun showMatchdayAgain() {
        matchdayFocusHandled = false
        lastMatchDayFocusKind = null
        viewModel.reset()
        matchDayViewModel.load()
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
        val base = resources.getDimensionPixelSize(R.dimen.screen_padding)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        lastOptions?.let { options ->
            outState.putString(KEY_STATE_HASHTAG, options.query)
            outState.putBoolean(KEY_STATE_INCLUDE_BROADCASTS, options.includeBroadcasts)
            outState.putBoolean(KEY_STATE_LINKS_ONLY, options.linksOnly)
            outState.putBoolean(KEY_STATE_TAB_LATEST, options.tab == SearchTab.LATEST)
        }
        outState.putBoolean(KEY_STATE_TAB_FALLBACK, lastTabFallback)
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

    private fun persistOptionSelection() {
        prefs().edit()
            .putBoolean(KEY_INCLUDE_BROADCASTS, binding.broadcastsCheck.isChecked)
            .putBoolean(KEY_LINKS_ONLY, binding.linksOnlyCheck.isChecked)
            .putBoolean(KEY_TAB_LATEST, binding.tabLatest.isChecked)
            .apply()
    }

    companion object {
        private const val KEY_LAST_HASHTAG = "last_hashtag"
        private const val KEY_INCLUDE_BROADCASTS = "include_broadcasts"
        private const val KEY_LINKS_ONLY = "links_only"
        private const val KEY_TAB_LATEST = "tab_latest"
        private const val KEY_STATE_HASHTAG = "state_last_hashtag"
        private const val KEY_STATE_INCLUDE_BROADCASTS = "state_include_broadcasts"
        private const val KEY_STATE_LINKS_ONLY = "state_links_only"
        private const val KEY_STATE_TAB_LATEST = "state_tab_latest"
        private const val KEY_STATE_TAB_FALLBACK = "state_tab_fallback"
        private const val MAX_MATCH_FOCUS_ATTEMPTS = 6
        private const val DOM_TIMEOUT_MS = 50_000L
        private const val FALLBACK_DOM_TIMEOUT_MS = 40_000L
        private const val API_TIMEOUT_MS = 15_000L
    }
}
