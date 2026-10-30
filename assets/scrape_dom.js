(async function () {
  const MAX = __MAX__;
  const BUDGET = __BUDGET_MS__;
  const TOKEN = "__TOKEN__";
  const started = Date.now();
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const found = new Map();

  function hostOf(href) {
    try { return new URL(href).hostname.toLowerCase(); } catch (e) { return ""; }
  }

  function isXHost(host) {
    return host === "x.com" || host.endsWith(".x.com") ||
      host === "twitter.com" || host.endsWith(".twitter.com");
  }

  function isBroadcast(href) {
    try {
      const url = new URL(href);
      return isXHost(url.hostname.toLowerCase()) &&
        /^\/i\/broadcasts\/[A-Za-z0-9_-]+\/?$/.test(url.pathname);
    } catch (e) {
      return false;
    }
  }

  function wanted(href) {
    if (hostOf(href) === "t.co") return true;
    return isBroadcast(href);
  }

  function authorOf(article) {
    const userName = article.querySelector('[data-testid="User-Name"]');
    if (!userName) return null;
    for (const span of userName.querySelectorAll("span")) {
      const text = (span.textContent || "").replace(/\s+/g, " ").trim();
      if (!text || text.startsWith("@") || text.startsWith("·")) continue;
      return text.split("·")[0].trim();
    }
    return null;
  }

  function add(href, author) {
    if (!href || !/^https?:\/\//i.test(href)) return;
    if (!wanted(href)) return;
    const existing = found.get(href);
    if (existing === undefined || (!existing && author)) {
      found.set(href, author || null);
    }
  }

  function collect() {
    document.querySelectorAll('article[data-testid="tweet"]').forEach((article) => {
      const author = authorOf(article);
      // Broadcast links can be in the tweet text, a card, or a quoted tweet.
      article.querySelectorAll('a[href]').forEach((a) => add(a.href, author));
    });
  }

  function articleCount() {
    return document.querySelectorAll('article[data-testid="tweet"]').length;
  }

  function looksEmpty() {
    if (articleCount() > 0) return false;
    const text = (document.body.innerText || "").toLocaleLowerCase("tr");
    return /sonuç bulunamadı|sonuç yok|hiçbir sonuç|no results/.test(text);
  }

  // X renders search results asynchronously. On a cold TV WebView this can take
  // several seconds, so wait before treating an empty page as "no results".
  const firstResultDeadline = Math.min(started + 20000, started + BUDGET);
  while (articleCount() === 0 && !looksEmpty() && Date.now() < firstResultDeadline) {
    await sleep(500);
  }

  collect();
  let unchanged = 0;
  for (let i = 0; i < 15 && found.size < MAX && Date.now() - started < BUDGET; i++) {
    if (looksEmpty()) break;
    const count = articleCount();
    window.scrollTo(0, document.body.scrollHeight);
    await sleep(1500);
    collect();
    const nextCount = articleCount();
    if (nextCount === count) {
      if (nextCount === 0) continue;
      unchanged++;
      if (unchanged >= 2) break;
    } else {
      unchanged = 0;
    }
  }

  TSportBridge.domResult(TOKEN, JSON.stringify(Array.from(found, ([url, author]) => ({ url, author })).slice(0, MAX)));
})();
