package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.jdt.core.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.ui.PDEPlugin;
import java.net.*;
import java.util.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.ui.IPluginStructureData;

public class MultiPageEditorTemplate extends PDETemplateSection {
	private static final String KEY_TITLE = "MultiPageEditorTemplate.title";
	private static final String KEY_DESC = "MultiPageEditorTemplate.desc";
	private static final String KEY_PACKAGE_LABEL =
		"MultiPageEditorTemplate.packageName";
	private static final String KEY_CLASS_LABEL =
		"MultiPageEditorTemplate.className";
	private static final String KEY_CONTRIBUTOR_LABEL =
		"MultiPageEditorTemplate.contributor";
	private static final String KEY_EDITOR_LABEL =
		"MultiPageEditorTemplate.editorName";
	private static final String KEY_DEFAULT_EDITOR_NAME =
		"MultiPageEditorTemplate.defaultEditorName";
	private static final String KEY_EXTENSIONS_LABEL =
		"MultiPageEditorTemplate.extensions";
	/**
	 * Constructor for MultiPageEditorTemplate.
	 */
	public MultiPageEditorTemplate() {
	}

	public String getSectionId() {
		return "multiPageEditor";
	}
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		// first page	
		addOption(
			KEY_PACKAGE_NAME,
			PDEPlugin.getResourceString(KEY_PACKAGE_LABEL),
			(String) null,
			0);
		addOption(
			"editorClassName",
			PDEPlugin.getResourceString(KEY_CLASS_LABEL),
			"MultiPageEditor",
			0);
		addOption(
			"contributorClassName",
			PDEPlugin.getResourceString(KEY_CONTRIBUTOR_LABEL),
			"MultiPageEditorContributor",
			0);
		addOption(
			"editorName",
			PDEPlugin.getResourceString(KEY_EDITOR_LABEL),
			PDEPlugin.getResourceString(KEY_DEFAULT_EDITOR_NAME),
			0);
		addOption(
			"extensions",
			PDEPlugin.getResourceString(KEY_EXTENSIONS_LABEL),
			"mpe",
			0);
	}

	protected void initializeFields(IPluginStructureData sdata, IFieldData data) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		String pluginId = sdata.getPluginId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".editors");
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".editors");
	}

	public boolean isDependentOnFirstPage() {
		return true;
	}

	public void addPages(Wizard wizard) {
		setPageCount(1);
		createOptions();
		WizardPage page = createPage(0);
		page.setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		page.setDescription(PDEPlugin.getResourceString(KEY_DESC));
		wizard.addPage(page);
	}

	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		} else
			resetPageState();
	}

	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.editors";
	}

	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.editors", true);
		IPluginModelFactory factory = model.getFactory();

		String editorClassName =
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption("editorClassName");
		String contributorClassName =
			getStringOption(KEY_PACKAGE_NAME)
				+ "."
				+ getStringOption("contributorClassName");

		IPluginElement editorElement = factory.createElement(extension);
		editorElement.setName("editor");
		editorElement.setAttribute("id", editorClassName);
		editorElement.setAttribute("name", getStringOption("editorName"));
		editorElement.setAttribute("icon", "icons/sample.gif");
		editorElement.setAttribute("extensions", getStringOption("extensions"));

		editorElement.setAttribute("class", editorClassName);
		editorElement.setAttribute("contributorClass", contributorClassName);
		extension.add(editorElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
}