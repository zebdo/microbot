package net.runelite.client.plugins.microbot.ui.search;

import net.runelite.client.plugins.config.SearchablePlugin;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MicrobotPluginSearch
{
    public static <T extends SearchablePlugin> List<T> search(Collection<T> searchablePlugins, String query)
    {
        String q = query == null ? "" : query.toLowerCase();

        return searchablePlugins.stream()
                .filter(plugin -> plugin.getSearchableName().toLowerCase().contains(q))
                .sorted(Comparator.comparing(SearchablePlugin::isPinned, Comparator.reverseOrder())
                        .thenComparing(SearchablePlugin::getSearchableName))
                .collect(Collectors.toList());
    }
}
