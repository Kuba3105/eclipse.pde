package org.eclipse.pde.internal;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.model.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;

import java.net.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import org.eclipse.swt.widgets.*;

import java.util.*;
import java.io.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.preferences.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.model.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 */
public class ExternalModelManager {
	private Vector models;
	private static final String KEY_ERROR_TITLE = "Errors.SetupError";
	private static final String KEY_SCANNING_PROBLEMS =
		"ExternalModelManager.scanningProblems";
	private static final String KEY_ERROR_NO_HOME =
		"Errors.SetupError.NoPlatformHome";
	private static final String KEY_PROCESSING_PATH =
		"ExternalModelManager.processingPath";
	private Vector listeners = new Vector();

	public ExternalModelManager() {
	}

	public void addModelProviderListener(IModelProviderListener listener) {
		listeners.add(listener);
	}

	public void clear() {
		models = null;
	}

	private static IPath createEclipseRelativeHome(String installLocation) {
		IPath fullPath = new Path(installLocation);
		IPath eclipseHome = getEclipseHome();
		int nmatching = fullPath.matchingFirstSegments(eclipseHome);
		return fullPath.removeFirstSegments(nmatching);
	}

	public IPluginExtensionPoint findExtensionPoint(String fullID) {
		if (fullID == null || fullID.length() == 0)
			return null;
		// separate plugin ID first
		int lastDot = fullID.lastIndexOf('.');
		if (lastDot == -1)
			return null;
		String pluginID = fullID.substring(0, lastDot);
		IPlugin plugin = findPlugin(pluginID);
		if (plugin == null)
			return null;
		String pointID = fullID.substring(lastDot + 1);
		IPluginExtensionPoint[] points = plugin.getExtensionPoints();
		for (int i = 0; i < points.length; i++) {
			IPluginExtensionPoint point = points[i];
			if (point.getId().equals(pointID))
				return point;
		}
		return null;
	}

	public IPlugin findPlugin(String id) {
		if (models == null)
			loadModels(new NullProgressMonitor());
		if (models == null)
			return null;
		for (int i = 0; i < models.size(); i++) {
			IPluginModel model = (IPluginModel) models.elementAt(i);
			IPlugin plugin = model.getPlugin();
			if (plugin.getId().equals(id))
				return plugin;
		}
		return null;
	}

	public void fireModelProviderEvent(IModelProviderEvent e) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			IModelProviderListener listener = (IModelProviderListener) iter.next();
			listener.modelsChanged(e);
		}
	}

	public static IPath getEclipseHome() {
		IPath eclipseHome =
			JavaCore.getClasspathVariable(PDEPlugin.ECLIPSE_HOME_VARIABLE);
		if (eclipseHome == null) {
			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			String newValue = store.getString(PDEBasePreferencePage.PROP_PLATFORM_PATH);
			if (newValue == null || newValue.length() == 0)
				return null;
			eclipseHome = new Path(newValue);
			try {
				JavaCore.setClasspathVariable(
					PDEPlugin.ECLIPSE_HOME_VARIABLE,
					eclipseHome,
					null);
			} catch (JavaModelException e) {
				PDEPlugin.logException(e);
			}
		}
		return eclipseHome;
	}

	public IPluginModel[] getModels() {
		return getModels(new NullProgressMonitor());
	}

	public IPluginModel[] getModels(IProgressMonitor monitor) {
		if (models == null)
			loadModels(monitor);
		if (models != null) {
			IPluginModel[] result = new IPluginModel[models.size()];
			models.copyInto(result);
			return result;
		}
		return new IPluginModel[0];
	}

	public IPlugin getPlugin(int i) {
		if (models == null)
			loadModels(new NullProgressMonitor());
		if (models != null) {
			IPluginModel model = (IPluginModel) models.elementAt(i);
			return model.getPlugin();
		}
		return null;
	}

	public int getPluginCount() {
		if (models == null)
			loadModels(new NullProgressMonitor());
		return (models != null) ? models.size() : 0;
	}

	private String[] getPluginPaths() {
		return getPluginPaths(null);
	}

	private String[] getPluginPaths(String platformHome) {
		if (platformHome == null) {
			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			platformHome = store.getString(PDEBasePreferencePage.PROP_PLATFORM_PATH);
		}
		if (platformHome == null || platformHome.length() == 0) {
			Display.getCurrent().beep();
			MessageDialog.openError(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString(KEY_ERROR_TITLE),
				PDEPlugin.getResourceString(KEY_ERROR_NO_HOME));
			return new String[0];
		}
		String[] paths = new String[2];
		paths[0] = platformHome + File.separator + "plugins";
		paths[1] = platformHome + File.separator + "fragments";
		return paths;
	}

	public boolean hasEnabledModels() {
		if (models == null) {
			loadModels(new NullProgressMonitor());
		}
		for (int i = 0; i < models.size(); i++) {
			IPluginModel model = (IPluginModel) models.elementAt(i);
			if (model.isEnabled())
				return true;
		}
		return false;
	}

	private boolean loadModels(IProgressMonitor monitor) {
		long startTime = System.currentTimeMillis();
		boolean result = reload(null, monitor);
		if (result) {
			IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
			ExternalPluginsBlock.initialize(this, store);
		}
		long stopTime = System.currentTimeMillis();
		Object[] array = models.toArray();
		ArraySorter.INSTANCE.sortInPlace(array);

		for (int i = 0; i < array.length; i++) {
			models.set(i, array[i]);
		}
		long sortTime = System.currentTimeMillis();
		/*
		System.out.println("Load time: "+(stopTime - startTime));
		System.out.println("Sort time: "+(sortTime - stopTime));
		System.out.println("Total: "+(sortTime - startTime));
		*/
		return result;
	}

	protected static void processPluginDescriptorModel(
		Vector result,
		PluginDescriptorModel descriptorModel) {
		ExternalPluginModel model = new ExternalPluginModel();
		String location = descriptorModel.getLocation();
		try {
			URL url = new URL(location);
			String localLocation = url.getFile();
			IPath path = new Path(localLocation).removeTrailingSeparator();
			model.setInstallLocation(path.toOSString());
			model.setEclipseHomeRelativePath(
				createEclipseRelativeHome(model.getInstallLocation()));
		} catch (MalformedURLException e) {
			model.setInstallLocation(location);
		}
		model.load(descriptorModel);
		if (model.isLoaded()) {
			result.add(model);
			// force creation of the plugin object
			Plugin plugin = (Plugin) model.getPlugin();
			model.setEnabled(true);
		}
	}

	private void internalProcessPluginDirectories(
		Vector result,
		String[] pluginPaths,
		IProgressMonitor monitor) {
		MultiStatus errors = processPluginDirectories(result, pluginPaths, monitor);
		if (errors != null && errors.getChildren().length > 0) {
			ResourcesPlugin.getPlugin().getLog().log(errors);
		}
	}

	public static MultiStatus processPluginDirectories(
		Vector result,
		String[] pluginPaths,
		IProgressMonitor monitor) {
		try {
			MultiStatus errors =
				new MultiStatus(
					PDEPlugin.getPluginId(),
					1,
					PDEPlugin.getResourceString(KEY_SCANNING_PROBLEMS),
					null);
			URL[] urls = new URL[pluginPaths.length];
			for (int i = 0; i < pluginPaths.length; i++) {
				urls[i] = new URL("file:" + pluginPaths[i].replace('\\', '/') + "/");
			}

			//String pattern = PDEPlugin.getResourceString(KEY_PROCESSING_PATH);
			//String message = PDEPlugin.getFormattedMessage(pattern, pluginPath);
			//monitor.subTask(message);
			PluginRegistryModel registryModel =
				Platform.parsePlugins(urls, new Factory(errors));
			IStatus resolveStatus = registryModel.resolve(true, false);
			PluginDescriptorModel[] pluginDescriptorModels = registryModel.getPlugins();
			for (int i = 0; i < pluginDescriptorModels.length; i++) {
				PluginDescriptorModel pluginDescriptorModel = pluginDescriptorModels[i];
				monitor.subTask(pluginDescriptorModel.getId());
				if (pluginDescriptorModel.getEnabled())
					processPluginDescriptorModel(result, pluginDescriptorModels[i]);
			}
			errors.merge(resolveStatus);
			return errors;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public boolean reload(String platformPath, IProgressMonitor monitor) {
		models = new Vector();
		if (platformPath != null)
			setEclipseHome(platformPath);
		String[] paths = getPluginPaths(platformPath);
		if (paths.length == 0)
			return false;
		if (monitor == null)
			monitor = new NullProgressMonitor();

		internalProcessPluginDirectories(models, paths, monitor);

		return true;
	}

	public void removeModelProviderListener(IModelProviderListener listener) {
		listeners.remove(listener);
	}

	public void setEclipseHome(String newValue) {
		try {
			JavaCore.setClasspathVariable(
				PDEPlugin.ECLIPSE_HOME_VARIABLE,
				new Path(newValue),
				null);
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
	}
}