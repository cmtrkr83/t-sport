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
    let name = null;
    let handle = null;
    for (const span of userName.querySelectorAll("span")) {
      const text = (span.textContent || "").replace(/\s+/g, " ").trim();
      if (!text) continue;
      if (handle === null && /^@[A-Za-z0-9_]{1,15}$/.test(text)) handle = text;
      if (name === null && !text.startsWith("@") && !text.startsWith("·")) {
        name = text.split("·")[0].trim();
      }
    }
    if (name === null && handle === null) return null;
    return { name: name, handle: handle };
  }

  function add(href, author, handle) {
    if (!href || !/^https?:\/\//i.test(href)) return;
    if (!wanted(href)) return;
    const existing = found.get(href);
    if (
      existing === undefined ||
      (!existing.author && author) ||
      (!existing.handle && handle)
    ) {
      found.set(href, { author: author || null, handle: handle || null });
    }
  }

  function collect() {
    document.querySelectorAll('article[data-testid="tweet"]').forEach((article) => {
      const user = authorOf(article);
      const author = user ? user.name : null;
      const handle = user ? user.handle : null;
      // Broadcast links can be in the tweet text, a card, or a quoted tweet.
      article.querySelectorAll('a[href]').forEach((a) => add(a.href, author, handle));
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

  function broadcastCount() {
    let count = 0;
    for (const url of found.keys()) {
      if (isBroadcast(url)) count++;
    }
    return count;
  }

  // X renders search results asynchronously. Wait for the first article, but
  // stop as soon as enough usable links are on screen instead of always
  // spending the full budget.
  const firstResultDeadline = Math.min(started + 15000, started + BUDGET);
  while (articleCount() === 0 && !looksEmpty() && Date.now() < firstResultDeadline) {
    await sleep(500);
  }

  console.log("TSport scrape start articles=" + articleCount() + " url=" + location.href);
  collect();
  let stableWithLinks = 0;
  let lastFoundSize = found.size;
  for (let i = 0; i < 8 && found.size < MAX && Date.now() - started < BUDGET; i++) {
    if (looksEmpty()) break;
    window.scrollTo(0, document.body.scrollHeight);
    await sleep(1500);
    collect();
    if (found.size >= 12 || broadcastCount() >= 3) break;
    if (found.size > 0 && found.size === lastFoundSize) {
      stableWithLinks++;
      if (stableWithLinks >= 2) break;
    } else {
      stableWithLinks = 0;
    }
    lastFoundSize = found.size;
  }

  console.log("TSport scrape done found=" + found.size + " articles=" + articleCount() + " url=" + location.href);
  TSportBridge.domResult(TOKEN, JSON.stringify(
    Array.from(found, ([url, user]) => ({
      url: url,
      author: user.author,
      handle: user.handle
    })).slice(0, MAX)
  ));
})();
