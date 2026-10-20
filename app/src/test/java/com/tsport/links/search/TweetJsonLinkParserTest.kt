package com.tsport.links.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TweetJsonLinkParserTest {

    @Test
    fun `extracts urls from tweet entities note tweets and cards`() {
        val json = """
            {
              "data": {
                "search_by_raw_query": {
                  "search_timeline": {
                    "timeline": {
                      "instructions": [{
                        "entries": [{
                          "content": {
                            "itemContent": {
                              "tweet_results": {
                                "result": {
                                  "legacy": {
                                    "entities": {
                                      "urls": [{
                                        "expanded_url": "https://example.com/live",
                                        "url": "https://t.co/abc"
                                      }]
                                    },
                                    "card": {
                                      "legacy": {
                                        "binding_values": [{
                                          "key": "card_url",
                                          "value": {
                                            "type": "STRING",
                                            "string_value": "https://example.net/card"
                                          }
                                        }]
                                      }
                                    }
                                  },
                                  "note_tweet": {
                                    "note_tweet_results": {
                                      "result": {
                                        "entity_set": {
                                          "urls": [{
                                            "expanded_url": "https://example.org/stream"
                                          }]
                                        }
                                      }
                                    }
                                  },
                                  "core": {
                                    "user_results": {
                                      "result": {
                                        "avatar": {
                                          "image_url": "https://pbs.twimg.com/profile/1.jpg"
                                        },
                                        "legacy": {
                                          "entities": {
                                            "url": {
                                              "urls": [{
                                                "expanded_url": "https://profile.example/bio"
                                              }]
                                            }
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }]
                      }]
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val urls = TweetJsonLinkParser.extractUrls(json)
        assertTrue(urls.contains("https://example.com/live"))
        assertTrue(urls.contains("https://t.co/abc"))
        assertTrue(urls.contains("https://example.org/stream"))
        assertTrue(urls.contains("https://example.net/card"))
        assertFalse(urls.contains("https://pbs.twimg.com/profile/1.jpg"))
        assertFalse(urls.contains("https://profile.example/bio"))
    }

    @Test
    fun `deduplicates urls`() {
        val json = """{"a":{"expanded_url":"https://example.com/x"},"b":{"expanded_url":"https://example.com/x"}}"""
        assertEquals(listOf("https://example.com/x"), TweetJsonLinkParser.extractUrls(json))
    }

    @Test
    fun `returns empty list for null blank or malformed json`() {
        assertEquals(emptyList<String>(), TweetJsonLinkParser.extractUrls(null))
        assertEquals(emptyList<String>(), TweetJsonLinkParser.extractUrls(""))
        assertEquals(emptyList<String>(), TweetJsonLinkParser.extractUrls("{bad"))
    }
}
