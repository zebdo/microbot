package net.runelite.client.plugins.microbot.util.item;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.ItemComposition;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Rs2ItemManager {

    private static final String ITEMS_JSON_PATH = "/item/items-summary.json";
    private Map<String, Rs2ItemSearchModel> itemsMap;

    public Rs2ItemManager() {
        loadItems();
    }

    /**
     * Loads the items from the JSON file.
     */
    private void loadItems() {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Rs2ItemSearchModel>>() {}.getType();
        try (InputStream inputStream = Rs2ItemManager.class.getResourceAsStream(ITEMS_JSON_PATH)) {
            if (inputStream == null) {
                System.err.println("Failed to load " + ITEMS_JSON_PATH);
                itemsMap = Collections.emptyMap();
                return;
            }
            itemsMap = gson.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), type);
            // print the number of items loaded
            System.out.println("Loaded " + itemsMap.size() + " items from " + ITEMS_JSON_PATH);
        } catch (IOException e) {
            throw new RuntimeException("Error loading items from JSON file", e);
        }
    }

    /**
     * Searches for items containing the given query string in their name.
     *
     * @param query The string to search for.
     * @return A list of Rs2ItemSearchModel objects matching the search.
     */
    public List<Rs2ItemSearchModel> searchItem(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String lowerQuery = query.toLowerCase();
        return itemsMap.values().stream()
                .filter(item -> item.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
    }

    // get item id by name
    public int getItemId(String itemName) {
        List<Rs2ItemSearchModel> items = searchItem(itemName);
        if (items.isEmpty()) {
            return -1;
        }
        return items.get(0).getId();
    }

    public ItemComposition getItemComposition(String itemName) {
        return Microbot.getClientThread().runOnClientThread(() -> Microbot.getItemManager().getItemComposition(getItemId(itemName)));
    }

    public int getPrice(String itemName) {
        ItemComposition itemComposition = getItemComposition(itemName);
        if (itemComposition == null) {
            return -1;
        }
        return itemComposition.getPrice();
    }

    // get GE price
    public int getGEPrice(String itemName) {
        return Rs2GrandExchange.getOfferPrice(getItemId(itemName));
    }


}
