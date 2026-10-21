package com.tsport.links.search

import com.tsport.links.links.LinkCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TweetJsonLinkParserTest {

    @Test
    fun `extracts tweet urls with author and skips profile data`() {
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
                                  "core": {
                                    "user_results": {
                                      "result": {
                                        "avatar": {
                                          "image_url": "https://pbs.twimg.com/profile/1.jpg"
                                        },
                                        "legacy": {
                                          "name": "Spor Muhabiri",
                                          "screen_name": "spor",
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
                                  },
                                  "legacy": {
                                    "full_text": "Canlı yayın",
                                    "entities": {
                                      "urls": [{
                                        "expanded_url": "https://x.com/i/broadcasts/abc",
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
                                            "expanded_url": "https://x.com/i/broadcasts/note"
                                          }]
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

        val candidates = TweetJsonLinkParser.extractCandidates(json)
        val byUrl = candidates.associate { it.url to it.author }

        assertEquals("Spor Muhabiri", byUrl["https://x.com/i/broadcasts/abc"])
        assertEquals("Spor Muhabiri", byUrl["https://t.co/abc"])
        assertEquals("Spor Muhabiri", byUrl["https://x.com/i/broadcasts/note"])
        assertEquals("Spor Muhabiri", byUrl["https://example.net/card"])
        assertNull(byUrl["https://pbs.twimg.com/profile/1.jpg"])
        assertNull(byUrl["https://profile.example/bio"])
    }

    @Test
    fun `deduplicates urls and keeps first non blank author`() {
        val json = """
            {
              "first": {
                "legacy": {
                  "full_text": "a",
                  "entities": {
                    "urls": [{"expanded_url": "https://x.com/i/broadcasts/abc"}]
                  }
                }
              },
              "second": {
                "core": {
                  "user_results": {
                    "result": {
                      "legacy": {"name": "Ayşe Yılmaz"}
                    }
                  }
                },
                "legacy": {
                  "full_text": "b",
                  "entities": {
                    "urls": [{"expanded_url": "https://x.com/i/broadcasts/abc"}]
                  }
                }
              }
            }
        """.trimIndent()

        val candidates = TweetJsonLinkParser.extractCandidates(json)
        assertEquals(1, candidates.size)
        assertEquals("Ayşe Yılmaz", candidates.single().author)
    }

    @Test
    fun `falls back to screen name when display name is missing`() {
        val json = """
            {
              "result": {
                "core": {
                  "user_results": {
                    "result": {
                      "legacy": {"screen_name": "spor"}
                    }
                  }
                },
                "legacy": {
                  "full_text": "a",
                  "entities": {
                    "urls": [{"expanded_url": "https://x.com/i/broadcasts/abc"}]
                  }
                }
              }
            }
        """.trimIndent()

        assertEquals("@spor", TweetJsonLinkParser.extractCandidates(json).single().author)
    }

    @Test
    fun `returns empty list for null blank or malformed json`() {
        assertTrue(TweetJsonLinkParser.extractCandidates(null).isEmpty())
        assertTrue(TweetJsonLinkParser.extractCandidates("").isEmpty())
        assertTrue(TweetJsonLinkParser.extractCandidates("{bad").isEmpty())
    }
}
