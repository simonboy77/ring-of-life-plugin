package com.simonboy77;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import net.runelite.client.RuneLite;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikiScraper {
    private final static String baseUrl = "https://oldschool.runescape.wiki";
    private final static String baseWikiUrl = baseUrl + "/w/";
    private final static String baseWikiLookupUrl = baseWikiUrl + "Special:Lookup";

    public static OkHttpClient httpClient = RuneLiteAPI.CLIENT;
    private static Document doc;

    private static final String USER_AGENT = RuneLite.USER_AGENT + " (ring-of-life)";

    public static int getMonsterMaxHit(String monsterName, int monsterId) {
        var lambdaWrapper = new Object(){ int maxHit = 0; }; // No clue

        String url;
        if (monsterId > -1) {
            url = getWikiUrlWithId(monsterName, monsterId);
        } else {
            url = getWikiUrl(monsterName);
        }

        requestAsync(url).whenCompleteAsync((responseHTML, ex) -> {
            doc = Jsoup.parse(responseHTML);
            //Elements tableHeaders = doc.select("h2 span.mw-headline, h3 span.mw-headline, h4 span.mw-headline");
            Elements tableHeaders = doc.select("infobox-monster");
            int tableCount = tableHeaders.size();

            lambdaWrapper.maxHit = tableCount;

            for (Element tableHeader : tableHeaders) {
                String tableHeaderText = tableHeader.text();
                String tableHeaderTextLower = tableHeaderText.toLowerCase();
                //Boolean isDropsTableHeader = tableHeaderTextLower.contains("drop") || tableHeaderTextLower.contains("levels") || isDropsHeaderForEdgeCases(monsterName, tableHeaderText);
            }
        });

        return lambdaWrapper.maxHit;
    }

    public static String filterTableContent(String cellContent) {
        return cellContent.replaceAll("\\[.*\\]", "");
    }

    public static String getWikiUrl(String itemOrMonsterName) {
        String sanitizedName = sanitizeName(itemOrMonsterName);
        return baseWikiUrl + sanitizedName;
    }

    public static String getWikiUrlWithId(String monsterName, int id) {
        String sanitizedName = sanitizeName(monsterName);
        // --- Handle edge cases for specific pages ---
        if(id == 7851 || id == 7852) {
            // Redirect Dusk and Dawn to Grotesque Guardians page
            id = -1;
            sanitizedName = "Grotesque_Guardians";
        }
        // ---
        return baseWikiLookupUrl + "?type=npc&id=" + String.valueOf(id) + "&name=" + sanitizedName;
    }

    public static String sanitizeName(String name) {
        // --- Handle edge cases for specific pages ---
        if (name.equalsIgnoreCase("tzhaar-mej")) {
            name = "tzhaar-mej (monster)";
        }
        if(name.equalsIgnoreCase("dusk") || name.equalsIgnoreCase("dawn")) {
            name = "grotesque guardians";
        }
        // ---
        name = name.trim().toLowerCase().replaceAll("\\s+", "_");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private static CompletableFuture<String> requestAsync(String url) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Request request = new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();

        httpClient
                .newCall(request)
                .enqueue(
                        new Callback() {
                            @Override
                            public void onFailure(Call call, IOException ex) {
                                future.completeExceptionally(ex);
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                try (ResponseBody responseBody = response.body()) {
                                    if (!response.isSuccessful()) future.complete("");

                                    future.complete(responseBody.string());
                                } finally {
                                    response.close();
                                }
                            }
                        });

        return future;
    }
}