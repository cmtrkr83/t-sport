(async function () {
  const MAX = __MAX__;
  const TOKEN = "__TOKEN__";
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const seen = new Set();

  function hostOf(href) {
    try { return new URL(href).hostname.toLowerCase(); } catch (e) { return ""; }
  }

  function blocked(href) {
    const host = hostOf(href);
    return host === "x.com" || host.endsWith(".x.com") ||
      host === "twitter.com" || host.endsWith(".twitter.com") ||
      host === "twimg.com" || host.endsWith(".twimg.com");
  }

  function add(href) {
    if (!href || !/^https?:\/\//i.test(href)) return;
    if (blocked(href)) return;
    seen.add(href);
  }

  function collect() {
    document.querySelectorAll('article[data-testid="tweet"]').forEach((article) => {
      article.querySelectorAll(
        'div[data-testid="tweetText"] a[href], div[data-testid="card.wrapper"] a[href]'
      ).forEach((a) => add(a.href));
    });
  }

  collect();
  let unchanged = 0;
  for (let i = 0; i < 10 && seen.size < MAX; i++) {
    const count = document.querySelectorAll('article[data-testid="tweet"]').length;
    window.scrollTo(0, document.body.scrollHeight);
    await sleep(1500);
    collect();
    const nextCount = document.querySelectorAll('article[data-testid="tweet"]').length;
    if (nextCount === count) {
      unchanged++;
      if (unchanged >= 2) break;
    } else {
      unchanged = 0;
    }
  }

  TSportBridge.domResult(TOKEN, JSON.stringify(Array.from(seen).slice(0, MAX)));
})();
