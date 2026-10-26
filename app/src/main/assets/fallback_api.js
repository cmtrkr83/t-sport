(async function () {
  const BEARER = "__BEARER__";
  const CSRF = "__CSRF__";
  const MAX_PAGES = __MAX_PAGES__;
  const TOKEN = "__TOKEN__";
  const QUERY = __QUERY__;
  const PRODUCT = __PRODUCT__;
  const QUERY_ID = __QUERY_ID__;
  const FALLBACK_QUERY_ID = __FALLBACK_QUERY_ID__;
  const FEATURES_JSON = __FEATURES__;
  const TX = __TX__;
  const CAPTURED_URL = __CAPTURED_URL__;

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

  function headers(transactionId) {
    const value = {
      "authorization": "Bearer " + BEARER,
      "x-csrf-token": CSRF,
      "x-twitter-active-user": "yes",
      "x-twitter-auth-type": "OAuth2Session",
      "x-twitter-client-language": "tr"
    };
    const tx = transactionId || TX;
    if (tx) value["x-client-transaction-id"] = tx;
    return value;
  }

  let transactionPromise = null;

  async function getTransactionId(path) {
    if (!globalThis.TSportClientTransaction || !globalThis.TSportFetchXDocument) {
      return "";
    }
    try {
      if (!transactionPromise) {
        transactionPromise = (async () => {
          const document = await globalThis.TSportFetchXDocument();
          return await globalThis.TSportClientTransaction.create(document);
        })();
      }
      const transaction = await transactionPromise;
      return await transaction.generateTransactionId("GET", path);
    } catch (error) {
      console.log("TSport tx error=" + String(error).slice(0, 200));
      transactionPromise = null;
      return "";
    }
  }

  function capturedRequest() {
    if (!CAPTURED_URL) return null;
    try {
      const url = new URL(CAPTURED_URL);
      return {
        endpoint: url.origin + url.pathname,
        features: url.searchParams.get("features"),
        variables: JSON.parse(url.searchParams.get("variables") || "{}")
      };
    } catch (e) {
      return null;
    }
  }

  async function requestPage(queryId, cursor, allowCaptured) {
    const captured = capturedRequest();
    let url;
    if (captured && allowCaptured && !cursor) {
      // Reuse the exact request X just made. A captured transaction id is
      // bound to that request, so changing its variables often causes a 404.
      url = CAPTURED_URL;
    } else if (captured && allowCaptured) {
      const variables = { ...captured.variables };
      if (cursor) variables.cursor = cursor;
      else delete variables.cursor;
      url = captured.endpoint + "?variables=" +
        encodeURIComponent(JSON.stringify(variables));
      if (captured.features) {
        url += "&features=" + encodeURIComponent(captured.features);
      }
    } else {
      const variables = {
        rawQuery: QUERY,
        count: 20,
        querySource: "typed_query",
        product: PRODUCT,
        withGrokTranslatedBio: false
      };
      if (cursor) variables.cursor = cursor;
      url = "https://x.com/i/api/graphql/" + queryId + "/SearchTimeline" +
        "?variables=" + encodeURIComponent(JSON.stringify(variables));
      if (FEATURES_JSON) {
        let features = FEATURES_JSON;
        try { features = JSON.stringify(JSON.parse(FEATURES_JSON)); } catch (e) { /* use raw */ }
        url += "&features=" + encodeURIComponent(features);
      }
    }

    const transactionId = await getTransactionId(new URL(url).pathname);
    console.log("TSport api request queryId=" + queryId + " product=" + PRODUCT +
      " tx=" + (transactionId ? transactionId.length : (TX ? TX.length : 0)) +
      " cursor=" + (cursor ? "yes" : "no") +
      " captured=" + (captured ? "yes" : "no"));
    return fetch(url, {
      method: "GET",
      credentials: "include",
      headers: headers(transactionId)
    });
  }

  try {
    if (!QUERY) {
      TSportBridge.apiResult(TOKEN, "null");
      return;
    }
    const queryIds = [];
    for (const id of [QUERY_ID, FALLBACK_QUERY_ID]) {
      if (id && !queryIds.includes(id)) queryIds.push(id);
    }

    const pages = [];
    let capturedUsed = false;
    for (const queryId of queryIds) {
      let cursor = null;
      let pageCount = 0;
      let usable = false;
      while (pageCount < MAX_PAGES) {
        const response = await requestPage(queryId, cursor, !capturedUsed);
        capturedUsed = true;
        if (response.status === 401 || response.status === 403) {
          TSportBridge.apiResult(TOKEN, "SESSION_EXPIRED");
          return;
        }
        if (!response.ok) {
          console.log("TSport api status=" + response.status + " queryId=" + queryId);
          break;
        }
        const data = await response.json();
        if (data?.errors?.length && !data?.data) {
          console.log("TSport api errors=" + JSON.stringify(data.errors).slice(0, 200));
          break;
        }
        pages.push(data);
        usable = true;
        pageCount++;
        cursor = findBottomCursor(data);
        if (!cursor) break;
      }
      if (usable) break;
    }

    console.log("TSport api done pages=" + pages.length);
    TSportBridge.apiResult(TOKEN, JSON.stringify(pages));
  } catch (error) {
    console.log("TSport api exception=" + String(error).slice(0, 200));
    TSportBridge.apiResult(TOKEN, "null");
  }
})();
