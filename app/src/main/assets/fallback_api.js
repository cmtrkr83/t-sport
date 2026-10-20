(async function () {
  const BEARER = "__BEARER__";
  const CSRF = "__CSRF__";
  const MAX_PAGES = __MAX_PAGES__;
  const TOKEN = "__TOKEN__";

  function findBottomCursor(data) {
    const instructions = data?.data?.search_by_raw_query?.search_timeline?.timeline?.instructions || [];
    for (const instruction of instructions) {
      for (const entry of (instruction.entries || [])) {
        if ((entry.entryId || "").startsWith("cursor-bottom")) {
          return entry.content?.value || null;
        }
      }
    }
    return null;
  }

  try {
    performance.setResourceTimingBufferSize(1000);
    const urls = performance.getEntriesByType("resource")
      .map((entry) => entry.name)
      .filter((name) => name.includes("/i/api/graphql/") && name.includes("/SearchTimeline"));
    if (!urls.length) {
      TSportBridge.apiResult(TOKEN, "null");
      return;
    }
    const requestUrl = new URL(urls[urls.length - 1]);
    const features = requestUrl.searchParams.get("features");
    const variables = JSON.parse(requestUrl.searchParams.get("variables") || "{}");
    const endpoint = requestUrl.origin + requestUrl.pathname;
    variables.count = 40;

    const pages = [];
    let cursor = null;
    for (let page = 0; page < MAX_PAGES; page++) {
      if (cursor) variables.cursor = cursor; else delete variables.cursor;
      let query = "?variables=" + encodeURIComponent(JSON.stringify(variables));
      if (features) query += "&features=" + encodeURIComponent(features);
      const response = await fetch(endpoint + query, {
        credentials: "include",
        headers: {
          "authorization": "Bearer " + BEARER,
          "x-csrf-token": CSRF,
          "x-twitter-active-user": "yes",
          "x-twitter-auth-type": "OAuth2Session",
          "x-twitter-client-language": "tr"
        }
      });
      if (response.status === 401 || response.status === 403) {
        TSportBridge.apiResult(TOKEN, "SESSION_EXPIRED");
        return;
      }
      if (!response.ok) break;
      const data = await response.json();
      pages.push(data);
      cursor = findBottomCursor(data);
      if (!cursor) break;
    }
    TSportBridge.apiResult(TOKEN, JSON.stringify(pages));
  } catch (error) {
    TSportBridge.apiResult(TOKEN, "null");
  }
})();
