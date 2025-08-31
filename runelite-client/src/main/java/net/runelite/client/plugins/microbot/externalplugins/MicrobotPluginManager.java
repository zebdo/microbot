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
import com.google.common.reflect.ClassPath;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Binder;
import com.google.inject.CreationException;
import com.google.inject.Injector;
import com.google.inject.Module;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.ExternalPluginsChanged;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.MicrobotConfig;
import net.runelite.client.plugins.microbot.util.misc.Rs2UiHelper;
import net.runelite.client.ui.SplashScreen;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class MicrobotPluginManager
{
    private static final File PLUGIN_DIR = new File(RuneLite.RUNELITE_DIR, "microbot-plugins");

    private final OkHttpClient okHttpClient;
    private final MicrobotPluginClient microbotPluginClient;
    private final EventBus eventBus;
    private final ScheduledExecutorService executor;
    private final PluginManager pluginManager;
    private final Gson gson;
    private final ConfigManager configManager;

    private final Map<String, MicrobotPluginManifest> manifestMap = new ConcurrentHashMap<>();

	@Inject
	private MicrobotPluginManager(
		OkHttpClient okHttpClient,
		MicrobotPluginClient microbotPluginClient,
		EventBus eventBus,
		ScheduledExecutorService executor,
		PluginManager pluginManager,
		Gson gson,
		ConfigManager configManager
	)
	{
		this.okHttpClient = okHttpClient;
		this.microbotPluginClient = microbotPluginClient;
		this.eventBus = eventBus;
		this.executor = executor;
		this.pluginManager = pluginManager;
		this.gson = gson;
		this.configManager = configManager;

		PLUGIN_DIR.mkdirs();
	}

	/**
	 * Initializes the MicrobotPluginManager
	 */
	public void init() {
		loadManifest();
		migrateLegacyPluginsJson();
		executor.scheduleWithFixedDelay(this::loadManifest, 10, 10, TimeUnit.MINUTES);
	}

	/**
     * Loads the plugin manifest list from the remote server and updates the local manifest map.
     * If the manifest has changed, posts an ExternalPluginsChanged event.
     */
    private void loadManifest()
	{
		try
		{
			List<MicrobotPluginManifest> manifests = microbotPluginClient.downloadManifest();
			Map<String, MicrobotPluginManifest> next = new HashMap<>(manifests.size());
			for (MicrobotPluginManifest m : manifests)
			{
				next.put(m.getInternalName(), m);
			}
			boolean changed = !next.keySet().equals(manifestMap.keySet())
				|| next.entrySet().stream().anyMatch(e -> {
				MicrobotPluginManifest cur = manifestMap.get(e.getKey());
				return cur == null || !Objects.equals(cur.getSha256(), e.getValue().getSha256());
			});
			if (changed)
			{
				manifestMap.clear();
				manifestMap.putAll(next);
				log.info("Loaded {} plugin manifests.", manifestMap.size());
				eventBus.post(new ExternalPluginsChanged());
			}
			else
			{
				log.debug("Plugin manifests unchanged ({} entries), skipping event.", manifestMap.size());
			}
		}
		catch (Exception e)
		{
			log.error("Failed to fetch plugin manifests", e);
		}
	}

	public Map<String, MicrobotPluginManifest> getManifestMap() {
		return Collections.unmodifiableMap(manifestMap);
	}

    /**
     * Migrates legacy plugins.json to the installedPlugins config if necessary.
     * Reads the old plugins.json file, converts it to the new format, and deletes the legacy file.
     */
    private void migrateLegacyPluginsJson() {
        String json = configManager.getConfiguration(MicrobotConfig.configGroup, MicrobotConfig.installedPlugins);
        if (json != null && !json.isEmpty()) {
            return;
        }
        File legacyFile = new File(PLUGIN_DIR, "plugins.json");
        if (!legacyFile.exists()) {
            return;
        }
        try {
            String legacyJson = Files.asCharSource(legacyFile, java.nio.charset.StandardCharsets.UTF_8).read();
            List<String> internalNames = gson.fromJson(legacyJson, new TypeToken<List<String>>(){}.getType());
            if (internalNames == null || internalNames.isEmpty()) {
                return;
            }
            List<MicrobotPluginManifest> manifests = new ArrayList<>();
            for (String internalName : internalNames) {
                MicrobotPluginManifest manifest = manifestMap.get(internalName);
                if (manifest != null) {
                    manifests.add(manifest);
                }
            }
            if (!manifests.isEmpty()) {
                saveInstalledPlugins(manifests);
                log.info("Migrated legacy plugins.json to installedPlugins config ({} plugins)", manifests.size());
                if (!legacyFile.delete()) {
                    log.warn("Failed to delete legacy plugins.json after migration: {}", legacyFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log.error("Failed to migrate legacy plugins.json", e);
        }
    }

	/**
     * Returns the list of installed Microbot plugins from the config manager.
     *
     * @return a list of installed MicrobotPluginManifest objects, or an empty list if none are installed
     */
    public List<MicrobotPluginManifest> getInstalledPlugins()
	{
		String json = configManager.getConfiguration(MicrobotConfig.configGroup, MicrobotConfig.installedPlugins);

		if (json == null || json.isEmpty()) {
			return new ArrayList<>();
		}

		try {
			List<MicrobotPluginManifest> plugins = gson.fromJson(
				json, new TypeToken<List<MicrobotPluginManifest>>() {}.getType()
			);
			return plugins != null ? plugins : new ArrayList<>();
		}
		catch (JsonSyntaxException e) {
			log.error("Error reading Microbot plugin list from config manager", e);
			configManager.setConfiguration(MicrobotConfig.configGroup, MicrobotConfig.installedPlugins, "[]");
			return new ArrayList<>();
		}
	}

	/**
     * Saves the list of installed Microbot plugins to the config manager.
     *
     * @param plugins the list of MicrobotPluginManifest objects to save
     */
    public void saveInstalledPlugins(List<MicrobotPluginManifest> plugins)
	{
		try {
			String json = gson.toJson(Objects.requireNonNullElse(plugins, Collections.emptyList()));
			configManager.setConfiguration(MicrobotConfig.configGroup, MicrobotConfig.installedPlugins, json);
		}
		catch (Exception e) {
			log.error("Error writing Microbot plugin list to config manager", e);
		}
	}

    /**
     * Gets the File object for the plugin JAR file corresponding to the given internal name.
     *
     * @param internalName the internal name of the plugin
     * @return the File object representing the plugin JAR
     */
    private File getPluginJarFile(String internalName)
    {
        return new File(PLUGIN_DIR, internalName + ".jar");
    }

	/**
	 * Creates an OkHttpClient instance that does not use any proxy settings.
	 * @param base
	 * @return
	 */
	private static OkHttpClient noProxy(OkHttpClient base) {
		return base.newBuilder()
				.proxy(Proxy.NO_PROXY)
				.proxySelector(ProxySelector.of(null))
				.build();
	}


	/**
     * Installs a Microbot plugin by downloading its JAR, saving it, and loading it into the client.
     *
     * @param manifest the MicrobotPluginManifest describing the plugin to install
     */
    public void install(MicrobotPluginManifest manifest) {
		executor.execute(() -> {
			String internalName = manifest.getInternalName();

			if (manifest.isDisable()) {
				log.error("Plugin {} is disabled and cannot be installed.", internalName);
				return;
			}

			if (!Rs2UiHelper.isClientVersionCompatible(manifest.getMinClientVersion())) {
				log.error("Plugin {} requires client version {} or higher, but current version is {}. Installation aborted.",
					internalName, manifest.getMinClientVersion(), RuneLiteProperties.getMicrobotVersion());
				return;
			}

			try {
				HttpUrl url = microbotPluginClient.getJarURL(manifest);
				if (url == null) {
					log.error("Invalid URL for plugin: {}", internalName);
					return;
				}

				OkHttpClient localClient = noProxy(okHttpClient);
				Request request = new Request.Builder()
						.url(url)
						.build();

				try (Response response = localClient.newCall(request).execute()) {
					if (!response.isSuccessful() || response.body() == null) {
						log.error("Error downloading plugin: {}, code: {}", internalName, response.code());
						return;
					}

					byte[] jarData = response.body().bytes();

					File pluginFile = getPluginJarFile(internalName);
					if (pluginFile.exists() && !pluginFile.delete()) {
						log.warn("Unable to delete plugin file: {}", pluginFile.getAbsolutePath());
					}
					Files.write(jarData, pluginFile);


					List<MicrobotPluginManifest> plugins = getInstalledPlugins();
					plugins.removeIf(p -> p.getInternalName().equals(internalName));
					plugins.add(manifest);
					saveInstalledPlugins(plugins);

					loadSideLoadPlugin(internalName);
				}
			} catch (IOException e) {
				log.error("Error installing plugin: {}", internalName, e);
			}
		});
	}

	/**
     * Removes a Microbot plugin by disabling, unloading, and deleting its JAR file.
     *
     * @param internalName the internal name of the plugin to remove
     */
    public void remove(String internalName) {
		executor.execute(() -> {
			List<Plugin> pluginsToRemove = pluginManager.getPlugins().stream()
				.filter(plugin -> {
					PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
					if (descriptor == null || !descriptor.isExternal()) {
						return false;
					}
					String className = plugin.getClass().getSimpleName();
					String descriptorName = descriptor.name();
					return className.equalsIgnoreCase(internalName) ||
						descriptorName.equalsIgnoreCase(internalName);
				})
				.collect(Collectors.toList());

			for (Plugin plugin : pluginsToRemove) {
				try {
					if (pluginManager.isPluginEnabled(plugin)) {
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
					}
				} catch (Exception e) {
					log.warn("Error disabling plugin {}: {}", plugin.getClass().getSimpleName(), e.getMessage());
				}

				pluginManager.remove(plugin);

				File jarFile = null;
				boolean closed = false;
				ClassLoader cl = plugin.getClass().getClassLoader();

				if (cl instanceof MicrobotPluginClassLoader) {
					jarFile = ((MicrobotPluginClassLoader) cl).getJarFile();
					try {
						((MicrobotPluginClassLoader) cl).close();
						closed = true;
					} catch (IOException e) {
						log.warn("Failed to close classloader for plugin {}: {}", plugin.getClass().getSimpleName(), e.getMessage());
					}
				} else {
					jarFile = getPluginJarFile(internalName);
				}

				if (jarFile != null && jarFile.exists()) {
					if (!jarFile.delete()) {
						log.warn("Failed to delete plugin file: {}", jarFile.getAbsolutePath());
					} else if (!closed) {
						log.info("Deleted plugin file: {} (classloader was not MicrobotPluginClassLoader)", jarFile.getAbsolutePath());
					}
				}
			}

			if (getInstalledPlugins().removeIf(m -> m.getInternalName().equals(internalName))) {
				saveInstalledPlugins(getInstalledPlugins());
			}

			eventBus.post(new ExternalPluginsChanged());
		});
	}

	/**
	 * Verifies that the SHA-256 hash of a locally installed plugin matches the
	 * authoritative hash from the manifest map.
	 * <p>
	 * This ensures the integrity of the plugin and detects tampering or corruption.
	 *
	 * @param internalName the internal name of the plugin to verify (must not be null or empty)
	 * @return {@code true} if the plugin exists in both the local and authoritative manifests
	 *         and the SHA-256 hashes match, {@code false} otherwise
	 * @throws IllegalArgumentException if {@code internalName} is null or empty
	 */
    private boolean verifyHash(String internalName)
    {
        if (internalName == null || internalName.isEmpty()) {
            throw new IllegalArgumentException("Internal name is null/empty");
        }

        List<MicrobotPluginManifest> plugins = getInstalledPlugins();
        MicrobotPluginManifest localManifest = plugins.stream()
            .filter(m -> internalName.equals(m.getInternalName()))
            .findFirst()
            .orElse(null);

        MicrobotPluginManifest authoritativeManifest = manifestMap.get(internalName);
        if (localManifest == null || authoritativeManifest == null) {
            return false;
        }
        String localHash = localManifest.getSha256();
        String authoritativeHash = authoritativeManifest.getSha256();

        if (localHash == null || localHash.isEmpty() || authoritativeHash == null || authoritativeHash.isEmpty()) {
            return false;
        }

        return localHash.equals(authoritativeHash);
    }

	public static File[] createSideloadingFolder()
	{
		try
		{
			Files.createParentDirs(PLUGIN_DIR);

			if (!PLUGIN_DIR.exists() && PLUGIN_DIR.mkdir())
			{
				log.debug("Directory for sideloading was created successfully.");
			}
		}
		catch (IOException e)
		{
			log.trace("Error creating directory for microbot-plugins!", e);
		}

		return PLUGIN_DIR.listFiles();
	}

    /**
     * Loads a single plugin from the sideload folder if not already loaded.
     */
	private void loadSideLoadPlugin(String internalName)
	{
		File pluginFile = getPluginJarFile(internalName);
		if (!pluginFile.exists())
		{
			log.debug("Plugin file {} does not exist", pluginFile);
			return;
		}
		List<MicrobotPluginManifest> installedPlugins = getInstalledPlugins();
		if (installedPlugins.stream().noneMatch(x -> x.getInternalName().equals(internalName)))
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
			return;
		}
		MicrobotPluginManifest manifest = manifestMap.get(internalName);
		if (manifest == null)
		{
			log.warn("No manifest found for plugin {}. Skipping hash validation and load.", internalName);
			return;
		}
		try
		{
			if (!verifyHash(manifest.getInternalName()))
			{
				log.warn("Plugin hash verification failed for: {}", manifest.getInternalName());
			}
			List<Class<?>> plugins = new ArrayList<>();
			MicrobotPluginClassLoader classLoader = new MicrobotPluginClassLoader(pluginFile, getClass().getClassLoader());
			for (ClassPath.ClassInfo classInfo : ClassPath.from(classLoader).getAllClasses())
			{
				try
				{
					Class<?> clazz = classLoader.loadClass(classInfo.getName());
					plugins.add(clazz);
				}
				catch (ClassNotFoundException e)
				{
					log.trace("Class not found during sideloading: {}", classInfo.getName(), e);
				}
			}
			loadPlugins(plugins, null);
			eventBus.post(new ExternalPluginsChanged());
		}
		catch (PluginInstantiationException | IOException e)
		{
			log.trace("Error loading side-loaded plugin!", e);
		}
	}

	public void loadSideLoadPlugins()
	{
		File[] files = createSideloadingFolder();
		if (files == null)
		{
			return;
		}
		List<MicrobotPluginManifest> installedPlugins = getInstalledPlugins();
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
			if (installedPlugins.stream().noneMatch(x -> x.getInternalName().equals(internalName)))
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
     * Topologically sort a graph. Uses Kahn's algorithm.
     *
     * @param graph - A directed graph
     * @param <T>   - The type of the item contained in the nodes of the graph
     * @return - A topologically sorted list corresponding to graph.
     * <p>
     * Multiple invocations with the same arguments may return lists that are not equal.
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
			if (pluginDescriptor.isExternal() && !Rs2UiHelper.isClientVersionCompatible(pluginDescriptor.minClientVersion()))
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
				log.info("Plugin loaded {}", plugin.getClass().getSimpleName());
				newPlugins.add(plugin);
				pluginManager.addPlugin(plugin);
				loaded++;
			}
			catch (PluginInstantiationException ex)
			{
				log.error("Error instantiating plugin!", ex);
			}

			if (onPluginLoaded != null)
			{
				onPluginLoaded.accept(loaded, sortedPlugins.size());
			}
		}

		return newPlugins;
	}

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

                // Create a parent injector containing all the dependencies
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

    public void loadCorePlugins(List<Class<?>> plugins) throws IOException, PluginInstantiationException
    {
        SplashScreen.stage(.59, null, "Loading plugins");

        loadPlugins(plugins, (loaded, total) ->
                SplashScreen.stage(.60, .70, null, "Loading plugins", loaded, total, false));
    }
}
