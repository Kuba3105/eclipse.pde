/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Hashtable;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.builders.FeatureRebuilder;
import org.eclipse.pde.internal.core.builders.PluginRebuilder;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.core.target.provisional.ITargetPlatformService;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class PDECore extends Plugin {
	public static final String PLUGIN_ID = "org.eclipse.pde.core"; //$NON-NLS-1$

	public static final IPath REQUIRED_PLUGINS_CONTAINER_PATH = new Path(PLUGIN_ID + ".requiredPlugins"); //$NON-NLS-1$
	public static final IPath JAVA_SEARCH_CONTAINER_PATH = new Path(PLUGIN_ID + ".externalJavaSearch"); //$NON-NLS-1$
	public static final IPath JRE_CONTAINER_PATH = new Path(JavaRuntime.JRE_CONTAINER);

	public static final String BINARY_PROJECT_VALUE = "binary"; //$NON-NLS-1$
	public static final String BINARY_REPOSITORY_PROVIDER = PLUGIN_ID + "." + "BinaryRepositoryProvider"; //$NON-NLS-1$ //$NON-NLS-2$

	public static final QualifiedName EXTERNAL_PROJECT_PROPERTY = new QualifiedName(PLUGIN_ID, "imported"); //$NON-NLS-1$
	public static final QualifiedName TOUCH_PROJECT = new QualifiedName(PLUGIN_ID, "TOUCH_PROJECT"); //$NON-NLS-1$

	public static final QualifiedName SCHEMA_PREVIEW_FILE = new QualifiedName(PLUGIN_ID, "SCHEMA_PREVIEW_FILE"); //$NON-NLS-1$

	// Shared instance
	private static PDECore inst;

	private static IPluginModelBase[] registryPlugins;
	private static PDEExtensionRegistry fExtensionRegistry = null;

	private static PDEPreferencesManager fPreferenceManager;

	public static PDECore getDefault() {
		return inst;
	}

	public PDEPreferencesManager getPreferencesManager() {
		if (fPreferenceManager == null) {
			fPreferenceManager = new PDEPreferencesManager(PLUGIN_ID);
		}
		return fPreferenceManager;
	}

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	public static void log(IStatus status) {
		if (status != null)
			ResourcesPlugin.getPlugin().getLog().log(status);
	}

	public static void log(Throwable e) {
		if (e instanceof InvocationTargetException)
			e = ((InvocationTargetException) e).getTargetException();
		IStatus status = null;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else if (e.getMessage() != null) {
			status = new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, e.getMessage(), e);
		}
		log(status);
	}

	public static void logErrorMessage(String message) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, null));
	}

	public static void logException(Throwable e) {
		logException(e, null);
	}

	public static void logException(Throwable e, String message) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else {
			if (message == null)
				message = e.getMessage();
			if (message == null)
				message = e.toString();
			status = new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, message, e);
		}
		log(status);
	}

	private PluginModelManager fModelManager;
	private FeatureModelManager fFeatureModelManager;

	private TargetDefinitionManager fTargetProfileManager;

	private PackageAdmin fPackageAdmin = null;
	private ServiceTracker fServiceTracker = null;

	// Schema registry
	private SchemaRegistry fSchemaRegistry;

	private SourceLocationManager fSourceLocationManager;
	private JavadocLocationManager fJavadocLocationManager;
	private SearchablePluginsManager fSearchablePluginsManager;

	// Tracing options manager
	private TracingOptionsManager fTracingOptionsManager;
	private BundleContext fBundleContext;
	private JavaElementChangeListener fJavaElementChangeListener;

	private FeatureRebuilder fFeatureRebuilder;

	private PluginRebuilder fPluginRebuilder;

	/**
	 * Target platform service.
	 */
	private ServiceRegistration fTargetPlatformService;

	public PDECore() {
		inst = this;
	}

	public URL getInstallURL() {
		try {
			return FileLocator.resolve(getDefault().getBundle().getEntry("/")); //$NON-NLS-1$
		} catch (IOException e) {
			return null;
		}
	}

	public IPluginModelBase findPluginInHost(String id) {
		if (registryPlugins == null) {
			URL[] pluginPaths = ConfiguratorUtils.getCurrentPlatformConfiguration().getPluginPath();
			PDEState state = new PDEState(pluginPaths, false, new NullProgressMonitor());
			registryPlugins = state.getTargetModels();
		}

		for (int i = 0; i < registryPlugins.length; i++) {
			if (registryPlugins[i].getPluginBase().getId().equals(id))
				return registryPlugins[i];
		}
		return null;
	}

	public PluginModelManager getModelManager() {
		if (fModelManager == null) {
			fModelManager = new PluginModelManager();
			// when initializing plug-in models, create the extension registry so it can track relevant (ModelChange) events.
			if (fExtensionRegistry == null)
				getExtensionsRegistry();
		}
		return fModelManager;
	}

	public TargetDefinitionManager getTargetProfileManager() {
		if (fTargetProfileManager == null)
			fTargetProfileManager = new TargetDefinitionManager();
		return fTargetProfileManager;
	}

	public FeatureModelManager getFeatureModelManager() {
		if (fFeatureModelManager == null)
			fFeatureModelManager = new FeatureModelManager();
		return fFeatureModelManager;
	}

	public JavaElementChangeListener getJavaElementChangeListener() {
		return fJavaElementChangeListener;
	}

	public SchemaRegistry getSchemaRegistry() {
		if (fSchemaRegistry == null)
			fSchemaRegistry = new SchemaRegistry();
		return fSchemaRegistry;
	}

	public PDEExtensionRegistry getExtensionsRegistry() {
		if (fExtensionRegistry == null) {
			fExtensionRegistry = new PDEExtensionRegistry();
		}
		return fExtensionRegistry;
	}

	public SourceLocationManager getSourceLocationManager() {
		if (fSourceLocationManager == null)
			fSourceLocationManager = new SourceLocationManager();
		return fSourceLocationManager;
	}

	public JavadocLocationManager getJavadocLocationManager() {
		if (fJavadocLocationManager == null)
			fJavadocLocationManager = new JavadocLocationManager();
		return fJavadocLocationManager;
	}

	public TracingOptionsManager getTracingOptionsManager() {
		if (fTracingOptionsManager == null)
			fTracingOptionsManager = new TracingOptionsManager();
		return fTracingOptionsManager;
	}

	public SearchablePluginsManager getSearchablePluginsManager() {
		if (fSearchablePluginsManager == null) {
			fSearchablePluginsManager = new SearchablePluginsManager();
			try {
				getWorkspace().addSaveParticipant(inst, fSearchablePluginsManager);
			} catch (CoreException e) {
				log(e);
			}
		}
		return fSearchablePluginsManager;
	}

	public boolean areModelsInitialized() {
		return fModelManager != null && fModelManager.isInitialized();
	}

	private Bundle getBundle(String symbolicName) {
		if (fPackageAdmin == null)
			return null;
		Bundle[] bundles = fPackageAdmin.getBundles(symbolicName, null);
		if (bundles == null)
			return null;
		// Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

	private boolean startEarly(String bundleName) throws BundleException {
		Bundle bundle = getBundle(bundleName);
		if (bundle == null)
			return false;
		bundle.start(Bundle.START_TRANSIENT);
		return true;
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		fBundleContext = context;

		fServiceTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
		fServiceTracker.open();
		fPackageAdmin = (PackageAdmin) fServiceTracker.getService();

		fJavaElementChangeListener = new JavaElementChangeListener();
		fJavaElementChangeListener.start();
		fPluginRebuilder = new PluginRebuilder();
		fPluginRebuilder.start();
		fFeatureRebuilder = new FeatureRebuilder();
		fFeatureRebuilder.start();

		fTargetPlatformService = context.registerService(ITargetPlatformService.class.getName(), TargetPlatformService.getDefault(), new Hashtable());

		// for now we need to start this bundle to ensure required services
		// are present when we need them (like IProfileRegistry)
		startEarly("org.eclipse.equinox.p2.exemplarysetup"); //$NON-NLS-1$
	}

	public BundleContext getBundleContext() {
		return fBundleContext;
	}

	public void stop(BundleContext context) throws CoreException {

		ITargetPlatformService tps = (ITargetPlatformService) acquireService(ITargetPlatformService.class.getName());
		if (tps instanceof TargetPlatformService) {
			((TargetPlatformService) tps).cleanOrphanedTargetDefinitionProfiles();
		}

		PDECore.getDefault().getPreferencesManager().savePluginPreferences();

		fJavaElementChangeListener.shutdown();
		fPluginRebuilder.stop();
		fFeatureRebuilder.stop();

		if (fSchemaRegistry != null) {
			fSchemaRegistry.shutdown();
			fSchemaRegistry = null;
		}
		if (fTargetProfileManager != null) {
			fTargetProfileManager.shutdown();
			fTargetProfileManager = null;
		}
		if (fSearchablePluginsManager != null) {
			getWorkspace().removeSaveParticipant(inst);
			fSearchablePluginsManager.shutdown();
			fSearchablePluginsManager = null;
		}
		if (fFeatureModelManager != null) {
			fFeatureModelManager.shutdown();
			fFeatureModelManager = null;
		}
		// always shut down extension registry before model manager (since it needs data from model manager)
		if (fExtensionRegistry != null) {
			fExtensionRegistry.stop();
			fExtensionRegistry = null;
		}
		if (fModelManager != null) {
			fModelManager.shutdown();
			fModelManager = null;
		}
		if (fTargetPlatformService != null) {
			fTargetPlatformService.unregister();
			fTargetPlatformService = null;
		}

		fServiceTracker.close();
		fPackageAdmin = null;
	}

	/**
	 * Returns a service with the specified name or <code>null</code> if none.
	 * 
	 * @param serviceName name of service
	 * @return service object or <code>null</code> if none
	 */
	public Object acquireService(String serviceName) {
		ServiceReference reference = fBundleContext.getServiceReference(serviceName);
		if (reference == null)
			return null;
		Object service = fBundleContext.getService(reference);
		if (service != null) {
			fBundleContext.ungetService(reference);
		}
		return service;
	}
}
