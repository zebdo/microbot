/*
 * Copyright (c) 2023 Microbot
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.microbot.externalplugins;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Binder;
import com.google.inject.CreationException;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ExternalPluginsChanged;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.ui.SplashScreen;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class MicrobotPluginManager
{
    private static final File PLUGIN_DIR = new File(RuneLite.RUNELITE_DIR, "microbot-plugins");
    private static final File PLUGIN_LIST = new File(PLUGIN_DIR, "plugins.json");

    private final OkHttpClient okHttpClient;
    private final MicrobotPluginClient microbotPluginClient;
    private final EventBus eventBus;
    private final ScheduledExecutorService executor;
    private final PluginManager pluginManager;
    private final Gson gson;

	@Getter
    private final Map<String, MicrobotPluginManifest> manifestMap = new ConcurrentHashMap<>();

    /**
     * Loads plugin manifests from the remote Microbot plugin service and refreshes the local manifest cache.
     *
     * Replaces the contents of {@code manifestMap} with the manifests returned by the remote client and
     * posts an {@link ExternalPluginsChanged} event to notify listeners of the update.
     * Any exceptions during manifest retrieval are caught and logged.
     */
    private void loadManifest() {
        try {
            List<MicrobotPluginManifest> manifests = microbotPluginClient.downloadManifest();
            manifestMap.clear();
            for (MicrobotPluginManifest manifest : manifests) {
                manifestMap.put(manifest.getInternalName(), manifest);
            }
            log.info("Loaded {} plugin manifests.", manifestMap.size());
            eventBus.post(new ExternalPluginsChanged());
        } catch (Exception e) {
            log.error("Failed to fetch plugin manifests", e);
        }
    }

	/**
	 * Creates and initializes the MicrobotPluginManager singleton.
	 *
	 * Initializes injected dependencies, ensures the plugin directory and plugin list file exist
	 * (creating them with an empty JSON array if missing), loads the remote plugin manifest cache,
	 * and schedules periodic manifest refreshes every 10 minutes.
	 */
	@Inject
	private MicrobotPluginManager(
		OkHttpClient okHttpClient,
		MicrobotPluginClient microbotPluginClient,
		EventBus eventBus,
		ScheduledExecutorService executor,
		PluginManager pluginManager,
		Gson gson)
	{
		this.okHttpClient = okHttpClient;
		this.microbotPluginClient = microbotPluginClient;
		this.eventBus = eventBus;
		this.executor = executor;
		this.pluginManager = pluginManager;
		this.gson = gson;

		PLUGIN_DIR.mkdirs();

		if (!PLUGIN_LIST.exists())
		{
			try
			{
				PLUGIN_LIST.createNewFile();
				Files.asCharSink(PLUGIN_LIST, StandardCharsets.UTF_8).write("[]");
			}
			catch (IOException e)
			{
				log.error("Unable to create Microbot plugin list", e);
			}
		}

		loadManifest();
		executor.scheduleWithFixedDelay(this::loadManifest, 10, 10, TimeUnit.MINUTES);
	}

    /**
     * Reads and returns the list of installed external plugin internal names from the on-disk plugin list.
     *
     * The method parses the JSON array stored at PLUGIN_LIST and returns it as a List of strings.
     * If the file is empty, malformed, or an IO error occurs, an empty list is returned (errors are logged).
     *
     * @return a non-null List of installed plugin internal names (empty if none or on read/parse failure)
     */
    public List<String> getInstalledPlugins()
    {
        List<String> plugins = new ArrayList<>();
        try (FileReader reader = new FileReader(PLUGIN_LIST))
        {
            plugins = gson.fromJson(reader, new TypeToken<List<String>>(){}.getType());
            if (plugins == null)
            {
                plugins = new ArrayList<>();
            }
        }
        catch (IOException | JsonSyntaxException e)
        {
            log.error("Error reading Microbot plugin list", e);
        }
        return plugins;
    }

    public void saveInstalledPlugins(List<String> plugins)
    {
        try
        {
            Files.asCharSink(PLUGIN_LIST, StandardCharsets.UTF_8).write(gson.toJson(plugins));
        }
        catch (IOException e)
        {
            log.error("Error writing Microbot plugin list", e);
        }
    }

    /**
     * Returns the File pointing to the plugin JAR for the given internal plugin name.
     *
     * The returned File is constructed as {@code PLUGIN_DIR/<internalName>.jar}; this method does
     * not check that the file exists or is readable.
     *
     * @param internalName the internal identifier of the plugin (used as the JAR filename without extension)
     * @return a File representing the expected JAR file location for the plugin
     */
    private File getPluginJarFile(String internalName)
    {
        return new File(PLUGIN_DIR, internalName + ".jar");
    }

	/**
	 * Asynchronously installs a plugin described by the provided manifest.
	 *
	 * <p>The installation task (run on the manager's executor) performs these steps:
	 * - validates the manifest is not disabled and that the current client version meets the manifest's minimum requirement;
	 * - resolves the plugin JAR URL from the manifest and downloads the JAR;
	 * - verifies the downloaded JAR's SHA-256 hash against the manifest;
	 * - writes the JAR to the local sideload directory, updates the internal manifest cache and the persisted installed-plugins list;
	 * - attempts to load the newly installed sideload plugin.</p>
	 *
	 * <p>All I/O and verification errors are logged; this method does not throw exceptions and returns immediately after scheduling the task.</p>
	 *
	 * @param manifest a MicrobotPluginManifest describing the plugin to install (internal name, download URL metadata, expected SHA-256, min client version, and enabled/disabled flag)
	 */
	public void install(MicrobotPluginManifest manifest)
	{
		executor.execute(() -> {
			// Check if plugin is disabled
			if (manifest.isDisable())
			{
				log.error("Plugin {} is disabled and cannot be installed.", manifest.getInternalName());
				return;
			}

			// Check version compatibility before installing
			if (!isClientVersionCompatible(manifest.getMinClientVersion()))
			{
				log.error("Plugin {} requires client version {} or higher, but current version is {}. Installation aborted.",
					manifest.getInternalName(), manifest.getMinClientVersion(), RuneLiteProperties.getMicrobotVersion());
				return;
			}

			try
			{
				HttpUrl url = microbotPluginClient.getJarURL(manifest);
				if (url == null)
				{

					log.error("Invalid URL for plugin: {}", manifest.getInternalName());
					return;
				}

				Request request = new Request.Builder()
					.url(url)
					.build();

				try (Response response = okHttpClient.newCall(request).execute())
				{
					if (!response.isSuccessful())
					{
						log.error("Error downloading plugin: {}, code: {}", manifest.getInternalName(), response.code());
						return;
					}

					byte[] jarData = response.body().bytes();

					// Verify the SHA-256 hash
					if (!verifyHash(jarData, manifest.getSha256()))
					{
						log.error("Plugin hash verification failed for: {}", manifest.getInternalName());
						return;
					}

					manifestMap.put(manifest.getInternalName(), manifest);
					// Save the jar file
					File pluginFile = getPluginJarFile(manifest.getInternalName());
					Files.write(jarData, pluginFile);
					List<String> plugins = getInstalledPlugins();
					if (!plugins.contains(manifest.getInternalName()))
					{
						plugins.add(manifest.getInternalName());
						saveInstalledPlugins(plugins);
					}
					loadSideLoadPlugin(manifest.getInternalName());
				}
			}
			catch (IOException e)
			{
				log.error("Error installing plugin: {}", manifest.getInternalName(), e);
			}
		});
	}

    /**
     * Removes an installed external plugin identified by its internal name.
     *
     * This is performed asynchronously on the manager's executor. The method locates loaded plugins
     * whose class simple name or PluginDescriptor name matches the supplied internalName (case-insensitive
     * match or exact match), and only considers plugins marked as external. For each match it:
     * - Disables the plugin if enabled, attempts to stop it if active, and removes it from the PluginManager.
     * - Deletes the corresponding plugin JAR file returned by getPluginJarFile(internalName) if present.
     * - Removes the internalName from the persisted installed-plugins list and saves the updated list.
     * Finally, posts an ExternalPluginsChanged event to the event bus.
     *
     * Errors encountered while disabling/stopping/removing plugins or deleting files are logged and do not
     * propagate to the caller.
     *
     * @param internalName the plugin's internal identifier (class simple name or PluginDescriptor name);
     *                     matching is case-insensitive and also supports exact-case matches
     */
    public void remove(String internalName)
    {
        executor.execute(() -> {
            List<Plugin> pluginsToRemove = pluginManager.getPlugins().stream()
                    .filter(plugin -> {
                        PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
                        if (descriptor == null) {
                            return false;
                        }

                        boolean isExternal = descriptor.isExternal();
                        String className = plugin.getClass().getSimpleName();
                        String descriptorName = descriptor.name();

                        boolean nameMatches = className.equals(internalName) ||
                                            descriptorName.equals(internalName) ||
                                            className.toLowerCase().equals(internalName.toLowerCase()) ||
                                            descriptorName.toLowerCase().equals(internalName.toLowerCase());

                        return isExternal && nameMatches;
                    })
                    .collect(Collectors.toList());

            for (Plugin plugin : pluginsToRemove) {
                if (pluginManager.isPluginEnabled(plugin)) {
                    try {
                        pluginManager.setPluginEnabled(plugin, false);

                        if (pluginManager.isPluginActive(plugin)) {
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    pluginManager.stopPlugin(plugin);
                                } catch (PluginInstantiationException e) {
                                    log.warn("Error stopping plugin {}: {}", plugin.getClass().getSimpleName(), e.getMessage());
                                }
                            });
                        }
                    } catch (Exception e) {
                        log.warn("Error stopping plugin {}: {}", plugin.getClass().getSimpleName(), e.getMessage());
                    }
                }

                pluginManager.remove(plugin);
            }

            File pluginFile = getPluginJarFile(internalName);
            if (pluginFile.exists()) {
                pluginFile.delete();
            }

            List<String> plugins = getInstalledPlugins();
            if (plugins.contains(internalName))
            {
                plugins.remove(internalName);
                saveInstalledPlugins(plugins);
            }

            eventBus.post(new ExternalPluginsChanged());
        });
    }

    private boolean verifyHash(byte[] jarData, String expectedHash)
    {
        if ((expectedHash == null || expectedHash.isEmpty()) || (jarData == null || jarData.length == 0))
        {
            throw new IllegalArgumentException("Hash or jar data is null/empty");
        }

        String computedHash = calculateSHA256Hash(jarData);
        return computedHash.equals(expectedHash);
    }

    /**
     * Calculate SHA-256 hash for byte array data and return as hex string
     */
    private String calculateSHA256Hash(byte[] data)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            int offset = 0;
            int bufferSize = 8192;

            while (offset < data.length)
            {
                int bytesToProcess = Math.min(bufferSize, data.length - offset);
                digest.update(data, offset, bytesToProcess);
                offset += bytesToProcess;
            }

            byte[] hash = digest.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash)
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            log.trace("Error computing SHA-256 hash", e);
			throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Ensures the application's "microbot-plugins" sideload directory exists and returns its contents.
     *
     * If the directory does not exist this method attempts to create it. Returns the array of files
     * contained in the sideload directory, or null if the directory does not exist or its contents
     * cannot be listed (e.g., I/O error or permission issue).
     *
     * @return an array of File objects for files in the sideload directory, or null if unavailable
     */
    public static File[] createSideloadingFolder() {
        final File MICROBOT_PLUGINS = new File(RuneLite.RUNELITE_DIR, "microbot-plugins");
        if (!java.nio.file.Files.exists(MICROBOT_PLUGINS.toPath())) {
            try {
                java.nio.file.Files.createDirectories(MICROBOT_PLUGINS.toPath());
                log.debug("Directory for sideloading was created successfully.");
                return MICROBOT_PLUGINS.listFiles();
            } catch (IOException e) {
                log.trace("Error creating directory for sideloading!", e);
            }
        }
        return MICROBOT_PLUGINS.listFiles();
    }

	/**
	 * Synchronizes the set of loaded external plugins with the persisted installed list.
	 *
	 * Compares currently loaded external plugins against the list returned by getInstalledPlugins()
	 * and initiates removal for any loaded external plugin whose internal name is not present in that list.
	 * Removal is performed via remove(...) and may trigger plugin unload and filesystem cleanup.
	 */
	public void syncPlugins()
	{
		List<String> installed = getInstalledPlugins();
		Map<String, Plugin> loadedByInternalName = pluginManager.getPlugins().stream()
			.filter(p -> p.getClass().isAnnotationPresent(PluginDescriptor.class))
			.filter(p -> {
				PluginDescriptor d = p.getClass().getAnnotation(PluginDescriptor.class);
				return d != null && d.isExternal();
			})
			.collect(Collectors.toMap(
				p -> p.getClass().getAnnotation(PluginDescriptor.class).name(),
				p -> p,
				(a, b) -> a
			));

		// Remove plugins that are loaded but not installed
		for (Map.Entry<String, Plugin> entry : loadedByInternalName.entrySet())
		{
			if (!installed.contains(entry.getKey()))
			{
				remove(entry.getKey());
			}
		}
	}

    /**
	 * Loads a single sideloaded external plugin (if installed and not already loaded).
	 *
	 * This method looks for a jar named "<internalName>.jar" in the sideload folder, verifies
	 * that the plugin is recorded as installed, validates the jar's SHA-256 against the
	 * manifest in the manifest cache, loads classes using MicrobotPluginClassLoader, and
	 * delegates instantiation to loadPlugins. If the plugin is successfully loaded an
	 * ExternalPluginsChanged event is posted.
	 *
	 * The method returns silently when the sideload folder is unavailable, the plugin is not
	 * in the installed list, the plugin is already loaded, the manifest is missing, or the
	 * jar fails hash validation. Any instantiation or IO errors are caught and logged.
	 *
	 * @param internalName internal plugin name (used as the manifest key and the jar file name without ".jar")
	 */
	private void loadSideLoadPlugin(String internalName)
	{
		File[] files = createSideloadingFolder();
		if (files == null)
		{
			return;
		}
		List<String> installedPlugins = getInstalledPlugins();
		if (!installedPlugins.contains(internalName))
		{
			return; // Not installed
		}
		Set<String> loadedInternalNames = pluginManager.getPlugins().stream()
			.filter(p -> p.getClass().isAnnotationPresent(PluginDescriptor.class))
			.filter(p -> p.getClass().getAnnotation(PluginDescriptor.class).isExternal())
			.map(p -> p.getClass().getAnnotation(PluginDescriptor.class).name())
			.collect(Collectors.toSet());
		if (loadedInternalNames.contains(internalName))
		{
			return; // Already loaded
		}
		boolean loaded = false;
		for (File f : files)
		{
			if (!f.getName().equals(internalName + ".jar"))
			{
				continue;
			}
			MicrobotPluginManifest manifest = manifestMap.get(internalName);
			if (manifest == null)
			{
				log.warn("No manifest found for plugin {}. Skipping hash validation and load.", internalName);
				return;
			}
			try
			{
				byte[] fileBytes = Files.toByteArray(f);
				// Validate hash before loading
				if (!verifyHash(fileBytes, manifest.getSha256()))
				{
					log.error("Hash mismatch for plugin {}. Skipping load.", internalName);
					return;
				}
				List<Class<?>> plugins = new ArrayList<>();
				MicrobotPluginClassLoader classLoader = new MicrobotPluginClassLoader(getClass().getClassLoader(), f.getName(), fileBytes);
				Set<String> classNamesToLoad = classLoader.getLoadedClassNames();
				for (String className : classNamesToLoad)
				{
					try
					{
						Class<?> clazz = classLoader.loadClass(className);
						plugins.add(clazz);
					}
					catch (ClassNotFoundException e)
					{
						log.trace("Class not found during sideloading: {}", className, e);
					}
				}
				loadPlugins(plugins, null);
				loaded = true;
			}
			catch (PluginInstantiationException | IOException e)
			{
				log.trace("Error loading side-loaded plugin!", e);
			}
		}
		if (loaded)
		{
			eventBus.post(new ExternalPluginsChanged());
		}
	}

	/**
	 * Loads all installed sideloaded external plugins found in the sideload folder.
	 *
	 * <p>Performs a sync of currently loaded external plugins against the installed list, then scans
	 * the sideload directory for JAR files whose base name matches an installed plugin internal name.
	 * For each installed-but-not-yet-loaded external plugin it delegates to {@link #loadSideLoadPlugin(String)}.</p>
	 *
	 * <p>Non-JAR files and plugins not present in the installed plugins list are ignored.</p>
	 */
	public void loadSideLoadPlugins()
	{
		syncPlugins();
		File[] files = createSideloadingFolder();
		if (files == null)
		{
			return;
		}
		List<String> installedPlugins = getInstalledPlugins();
		Set<String> loadedInternalNames = pluginManager.getPlugins().stream()
			.filter(p -> p.getClass().isAnnotationPresent(PluginDescriptor.class))
			.filter(p -> p.getClass().getAnnotation(PluginDescriptor.class).isExternal())
			.map(p -> p.getClass().getAnnotation(PluginDescriptor.class).name())
			.collect(Collectors.toSet());
		for (File f : files)
		{
			if (!f.getName().endsWith(".jar"))
			{
				continue;
			}
			String internalName = f.getName().replace(".jar", "");
			if (!installedPlugins.contains(internalName))
			{
				continue; // Skip if not in installed list
			}
			if (loadedInternalNames.contains(internalName))
			{
				continue; // Already loaded
			}
			loadSideLoadPlugin(internalName);
		}
	}

    /**
         * Returns a topologically sorted list of the nodes in the given directed graph using Kahn's algorithm.
         *
         * <p>Nodes with no incoming edges will appear before their successors. The relative order of nodes
         * that are not constrained by dependencies is not guaranteed and may vary between invocations.</p>
         *
         * @param graph the directed graph to sort
         * @param <T>   the node element type
         * @return a list of graph nodes in topologically sorted order
         * @throws RuntimeException if the graph contains at least one cycle
         */
    @VisibleForTesting
    static <T> List<T> topologicalSort(Graph<T> graph) {
        MutableGraph<T> graphCopy = Graphs.copyOf(graph);
        List<T> l = new ArrayList<>();
        Set<T> s = graphCopy.nodes().stream()
                .filter(node -> graphCopy.inDegree(node) == 0)
                .collect(Collectors.toSet());
        while (!s.isEmpty()) {
            Iterator<T> it = s.iterator();
            T n = it.next();
            it.remove();

            l.add(n);

            for (T m : new HashSet<>(graphCopy.successors(n))) {
                graphCopy.removeEdge(n, m);
                if (graphCopy.inDegree(m) == 0) {
                    s.add(m);
                }
            }
        }
        if (!graphCopy.edges().isEmpty()) {
            throw new RuntimeException("Graph has at least one cycle");
        }
        return l;
    }

	/**
	 * Loads and registers Plugin classes after resolving dependencies and returns instances of the successfully
	 * loaded plugins.
	 *
	 * <p>This method:
	 * <ul>
	 *   <li>skips plugin classes that are already loaded by the PluginManager,</li>
	 *   <li>validates that classes have a PluginDescriptor and extend Plugin,</li>
	 *   <li>skips external plugins that are disabled or whose minimum client version is not met,</li>
	 *   <li>builds a directed dependency graph from {@link PluginDependency} annotations and topologically
	 *       sorts it,</li>
	 *   <li>instantiates plugins in dependency order, registers each instance with the PluginManager, and
	 *       invokes the optional progress callback for each plugin loaded,</li>
	 *   <li>logs and skips plugins that fail to instantiate.</li>
	 * </ul>
	 *
	 * @param plugins a list of plugin classes to consider for loading
	 * @param onPluginLoaded optional progress callback receiving (loadedCount, totalCount); may be null
	 * @return a list of plugin instances that were successfully instantiated and registered
	 * @throws PluginInstantiationException if the dependency graph contains a cycle (preventing a load order)
	 */
	public List<Plugin> loadPlugins(List<Class<?>> plugins, BiConsumer<Integer, Integer> onPluginLoaded) throws PluginInstantiationException
	{
		MutableGraph<Class<? extends Plugin>> graph = GraphBuilder
			.directed()
			.build();

		Set<Class<?>> alreadyLoaded = pluginManager.getPlugins().stream()
			.map(Object::getClass)
			.collect(Collectors.toSet());

		for (Class<?> clazz : plugins)
		{
			if (alreadyLoaded.contains(clazz))
			{
				log.debug("Plugin {} is already loaded, skipping duplicate.", clazz.getSimpleName());
				continue;
			}
			PluginDescriptor pluginDescriptor = clazz.getAnnotation(PluginDescriptor.class);

			if (pluginDescriptor == null)
			{
				if (clazz.getSuperclass() == Plugin.class)
				{
					log.error("Class {} is a plugin, but has no plugin descriptor", clazz);
				}
				continue;
			}

			if (clazz.getSuperclass() != Plugin.class)
			{
				log.error("Class {} has plugin descriptor, but is not a plugin", clazz);
				continue;
			}

			// Check version compatibility for external plugins
			if (pluginDescriptor.isExternal() && !isClientVersionCompatible(pluginDescriptor.minClientVersion()))
			{
				log.error("Plugin {} requires client version {} or higher, but current version is {}. Skipping plugin loading.",
					clazz.getSimpleName(), pluginDescriptor.minClientVersion(), RuneLiteProperties.getMicrobotVersion());
				continue;
			}

			// Check if the plugin is disabled
			if (pluginDescriptor.disable())
			{
				log.error("Plugin {} has been disabled upstream", clazz.getSimpleName());
				continue;
			}

			graph.addNode((Class<Plugin>) clazz);
		}

		// Build plugin graph
		for (Class<? extends Plugin> pluginClazz : graph.nodes())
		{
			PluginDependency[] pluginDependencies = pluginClazz.getAnnotationsByType(PluginDependency.class);

			for (PluginDependency pluginDependency : pluginDependencies)
			{
				if (graph.nodes().contains(pluginDependency.value()))
				{
					graph.putEdge(pluginDependency.value(), pluginClazz);
				}
			}
		}

		if (Graphs.hasCycle(graph))
		{
			throw new PluginInstantiationException("Plugin dependency graph contains a cycle!");
		}

		List<Class<? extends Plugin>> sortedPlugins = topologicalSort(graph);

		int loaded = 0;
		List<Plugin> newPlugins = new ArrayList<>();
		for (Class<? extends Plugin> pluginClazz : sortedPlugins)
		{
			Plugin plugin;
			try
			{
				plugin = instantiate(pluginManager.getPlugins(), (Class<Plugin>) pluginClazz);
				log.info("Microbot pluginManager loaded " + plugin.getClass().getSimpleName());
				newPlugins.add(plugin);
				pluginManager.addPlugin(plugin);
			}
			catch (PluginInstantiationException ex)
			{
				log.error("Error instantiating plugin!", ex);
			}

			loaded++;
			if (onPluginLoaded != null)
			{
				onPluginLoaded.accept(loaded, sortedPlugins.size());
			}
		}

		return newPlugins;
	}

    /**
     * Instantiates a plugin class, wires its declared {@link PluginDependency} requirements, and installs a Guice injector for it.
     *
     * <p>This method:
     * <ul>
     *   <li>Validates that every {@link PluginDependency} on the class is present in {@code scannedPlugins} (throws {@link PluginInstantiationException} if any are missing).</li>
     *   <li>Creates the plugin instance via its no-arg constructor (wraps instantiation failures in {@link PluginInstantiationException}; {@link ThreadDeath} is rethrown).</li>
     *   <li>Builds a parent injector based on the resolved dependencies:
     *     <ul>
     *       <li>If multiple dependencies exist, creates a child injector that binds and installs each dependency as a module.</li>
     *       <li>If a single dependency exists, reuses that dependency's injector as the parent.</li>
     *       <li>Otherwise uses the global Microbot injector as the parent.</li>
     *     </ul>
     *   </li>
     *   <li>Creates a child injector that binds the newly created plugin instance and installs the plugin as a Guice module, then assigns the resulting injector to the plugin.</li>
     * </ul>
     *
     * @return the instantiated and injected {@link Plugin}
     * @throws PluginInstantiationException if a dependency is unmet or if instantiation/injector creation fails
     */
    private Plugin instantiate(Collection<Plugin> scannedPlugins, Class<Plugin> clazz) throws PluginInstantiationException {
        PluginDependency[] pluginDependencies = clazz.getAnnotationsByType(PluginDependency.class);
        List<Plugin> deps = new ArrayList<>();
        for (PluginDependency pluginDependency : pluginDependencies) {
            Optional<Plugin> dependency = scannedPlugins.stream().filter(p -> p.getClass() == pluginDependency.value()).findFirst();
            if (!dependency.isPresent()) {
                throw new PluginInstantiationException("Unmet dependency for " + clazz.getSimpleName() + ": " + pluginDependency.value().getSimpleName());
            }
            deps.add(dependency.get());
        }

        Plugin plugin;
        try {
            plugin = clazz.getDeclaredConstructor().newInstance();
        } catch (ThreadDeath e) {
            throw e;
        } catch (Throwable ex) {
            throw new PluginInstantiationException(ex);
        }

        try {
            Injector parent = Microbot.getInjector();

            if (deps.size() > 1) {
                List<com.google.inject.Module> modules = new ArrayList<>(deps.size());
                for (Plugin p : deps) {
                    // Create a module for each dependency
                    com.google.inject.Module module = (Binder binder) ->
                    {
                        binder.bind((Class<Plugin>) p.getClass()).toInstance(p);
                        binder.install(p);
                    };
                    modules.add(module);
                }

                // Create a parent injector containing all of the dependencies
                parent = parent.createChildInjector(modules);
            } else if (!deps.isEmpty()) {
                // With only one dependency we can simply use its injector
                parent = deps.get(0).getInjector();
            }

            // Create injector for the module
            Module pluginModule = (Binder binder) ->
            {
                // Since the plugin itself is a module, it won't bind itself, so we'll bind it here
                binder.bind(clazz).toInstance(plugin);
                binder.install(plugin);
            };
            Injector pluginInjector = parent.createChildInjector(pluginModule);
            plugin.setInjector(pluginInjector);
        } catch (CreationException ex) {
            throw new PluginInstantiationException(ex);
        }

        log.debug("Loaded plugin {}", clazz.getSimpleName());
        return plugin;
    }

    /**
     * Check if the current client version is compatible with the required minimum version
     */
    public boolean isClientVersionCompatible(String minClientVersion) {
        if (minClientVersion == null || minClientVersion.isEmpty()) {
            return true;
        }

        String currentVersion = RuneLiteProperties.getMicrobotVersion();
        if (currentVersion == null) {
            log.warn("Unable to determine current Microbot version");
            return false;
        }

        return compareVersions(currentVersion, minClientVersion) >= 0;
    }

    /**
     * Compare two version strings using semantic versioning with support for 4-part versions
     * Supports formats like: 1.9.7, 1.9.7.1, 1.9.8, 1.9.8.1
     * @param version1 The first version to compare
     * @param version2 The second version to compare
     * @return -1 if version1 < version2, 0 if equal, 1 if version1 > version2
     */
    @VisibleForTesting
    static int compareVersions(String version1, String version2) {
        if (version1 == null && version2 == null) return 0;
        if (version1 == null) return -1;
        if (version2 == null) return 1;

        // Split versions by dots and handle up to 4 parts (major.minor.patch.build)
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");

        int maxLength = Math.max(v1Parts.length, v2Parts.length);

        for (int i = 0; i < maxLength; i++) {
            int v1Part = i < v1Parts.length ? parseVersionPart(v1Parts[i]) : 0;
            int v2Part = i < v2Parts.length ? parseVersionPart(v2Parts[i]) : 0;

            if (v1Part < v2Part) return -1;
            if (v1Part > v2Part) return 1;
        }

        return 0;
    }

    /**
     * Parse a version part, extracting only the numeric portion
     */
    private static int parseVersionPart(String part) {
        if (part == null || part.isEmpty()) return 0;

        StringBuilder numericPart = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (!Character.isDigit(c)) break;
			numericPart.append(c);
        }

        try {
            return numericPart.length() > 0 ? Integer.parseInt(numericPart.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void loadCorePlugins(List<Class<?>> plugins) throws IOException, PluginInstantiationException
    {
        SplashScreen.stage(.59, null, "Loading plugins");

        loadPlugins(plugins, (loaded, total) ->
                SplashScreen.stage(.60, .70, null, "Loading plugins", loaded, total, false));
    }
}
