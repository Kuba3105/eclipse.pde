/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.extension;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.model.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.views.navigator.*;
public abstract class BaseExtensionPointMainPage extends WizardPage {
	public static final String KEY_PLUGIN_ID = "BaseExtensionPoint.pluginId"; //$NON-NLS-1$
	public static final String KEY_ID = "BaseExtensionPoint.id"; //$NON-NLS-1$
	public static final String KEY_NAME = "BaseExtensionPoint.name"; //$NON-NLS-1$
	public static final String KEY_SCHEMA = "BaseExtensionPoint.schema"; //$NON-NLS-1$
	public static final String KEY_SCHEMA_LOCATION = "BaseExtensionPoint.schemaLocation"; //$NON-NLS-1$
	public static final String KEY_EDIT = "BaseExtensionPoint.edit"; //$NON-NLS-1$
	public static final String KEY_SHARED = "BaseExtensionPoint.shared"; //$NON-NLS-1$
	public static final String KEY_MISSING_ID = "BaseExtensionPoint.missingId"; //$NON-NLS-1$
	public static final String KEY_NO_PLUGIN_MISSING_ID = "BaseExtensionPoint.noPlugin.missingId"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_OVERVIEW = "BaseExtensionPoint.sections.overview"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_SINCE = "BaseExtensionPoint.sections.since"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_USAGE = "BaseExtensionPoint.sections.usage"; //$NON-NLS-1$
	public static final String KEY_GENERATING = "BaseExtensionPoint.generating"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_API = "BaseExtensionPoint.sections.api"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_SUPPLIED = "BaseExtensionPoint.sections.supplied"; //$NON-NLS-1$
	public static final String KEY_SECTIONS_COPYRIGHT = "BaseExtensionPoint.sections.copyright"; //$NON-NLS-1$
	public static final String SETTINGS_PLUGIN_ID = "BaseExtensionPoint.settings.pluginId"; //$NON-NLS-1$
	public static final String SCHEMA_DIR = "schema"; //$NON-NLS-1$

	private IContainer fContainer;
	private IProject fProject;
	protected Text fIdText;
	protected Text fPluginIdText;
	protected Text fNameText;
	protected Text fSchemaText;
	protected Text fSchemaLocationText;
	protected Button fOpenSchemaButton;
	protected Button fSharedSchemaButton;
	protected Button fPluginBrowseButton;
	protected Button fFindLocationButton;
	public BaseExtensionPointMainPage(IContainer container) {
		super("newExtensionPoint"); //$NON-NLS-1$
		this.fContainer = container;
		if (container != null)
			this.fProject = container.getProject();
		else
			this.fProject = null;
	}
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		layout.makeColumnsEqualWidth = false;
		container.setLayout(layout);
		Label label;
		GridData gd;
		if (isPluginIdNeeded()) {
			label = new Label(container, SWT.NONE);
			label.setText(PDEPlugin.getResourceString(KEY_PLUGIN_ID));
			fPluginIdText = new Text(container, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan=1;
			gd.widthHint = 275;
			fPluginIdText.setLayoutData(gd);
			fPluginIdText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validatePage(true);
				}
			});
			fPluginBrowseButton = new Button(container, SWT.PUSH);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
			gd.horizontalSpan =1;
			gd.widthHint = 50;
			fPluginBrowseButton.setLayoutData(gd);
			fPluginBrowseButton.setText(PDEPlugin.getResourceString("BaseExtensionPointMainPage.pluginBrowse")); //$NON-NLS-1$
			fPluginBrowseButton.setToolTipText(PDEPlugin.getResourceString("BaseExtensionPointMainPage.pluginId.tooltip")); //$NON-NLS-1$
			fPluginBrowseButton.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					handlePluginBrowse();
				}
			});
			SWTUtil.setButtonDimensionHint(fPluginBrowseButton);
		}
		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_ID));
		fIdText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fIdText.setLayoutData(gd);
		fIdText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fSchemaText
						.setText(getSchemaLocation()
								+ (getSchemaLocation().length() > 0 ? "/" : "") + fIdText.getText() + ".exsd"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				validatePage(false);
			}
		});
		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_NAME));
		fNameText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fNameText.setLayoutData(gd);
		fNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validatePage(false);
			}
		});
		if (isPluginIdNeeded() && !isPluginIdFinal()){
			label = new Label(container, SWT.NONE);
			label.setText(PDEPlugin.getResourceString(KEY_SCHEMA_LOCATION));
			fSchemaLocationText = new Text(container, SWT.SINGLE | SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.widthHint = 150;
			gd.grabExcessHorizontalSpace = true;
			fSchemaLocationText.setLayoutData(gd);
			fSchemaLocationText.addModifyListener(new ModifyListener(){
				public void modifyText(ModifyEvent e){
					validatePage(true);
				}
			});
			fFindLocationButton = new Button(container, SWT.PUSH);
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
			gd.widthHint = 50;
			fFindLocationButton.setLayoutData(gd);
			fFindLocationButton.setText(PDEPlugin.getResourceString("BaseExtensionPointMainPage.findBrowse")); //$NON-NLS-1$
			fFindLocationButton.setToolTipText(PDEPlugin.getResourceString("BaseExtensionPointMainPage.schemaLocation.tooltip")); //$NON-NLS-1$
			fFindLocationButton.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					handleSchemaLocation();
				}
			});
			SWTUtil.setButtonDimensionHint(fFindLocationButton);
		}
		label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString(KEY_SCHEMA));
		fSchemaText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fSchemaText.setLayoutData(gd);
		fSchemaText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e){
				validatePage(false);
			}
		});
		if (isSharedSchemaSwitchNeeded()) {
			fSharedSchemaButton = new Button(container, SWT.CHECK);
			fSharedSchemaButton.setText(PDEPlugin.getResourceString(KEY_SHARED));
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			fSharedSchemaButton.setLayoutData(gd);
		}
		fOpenSchemaButton = new Button(container, SWT.CHECK);
		fOpenSchemaButton.setText(PDEPlugin.getResourceString(KEY_EDIT));
		fOpenSchemaButton.setSelection(true);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fOpenSchemaButton.setLayoutData(gd);
		if (isPluginIdNeeded())
			fPluginIdText.setFocus();
		else
			fIdText.setFocus();
		setControl(container);
		validatePage(false);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.NEW_SCHEMA);
	}
	private InputStream createSchemaStream(String pluginId, String pointId,
			String name, boolean shared) {
		if (name.length() == 0)
			name = pointId;
		EditableSchema schema = new EditableSchema(pluginId, pointId, name, false);
		schema.setDescription(PDEPlugin
				.getResourceString(KEY_SECTIONS_OVERVIEW));
		DocumentSection section;
		section = new DocumentSection(schema, IDocumentSection.SINCE, PDEPlugin.getResourceString("BaseExtensionPointMainPage.since")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_SINCE));
		schema.addDocumentSection(section);
		SchemaElement element;
		if (!shared) {
			element = new SchemaElement(schema, "extension"); //$NON-NLS-1$
			SchemaComplexType complexType = new SchemaComplexType(schema);
			element.setType(complexType);
			SchemaCompositor compositor = new SchemaCompositor(element,
					ISchemaCompositor.SEQUENCE);
			complexType.setCompositor(compositor);
			SchemaAttribute attribute = new SchemaAttribute(element, "point"); //$NON-NLS-1$
			attribute.setType(new SchemaSimpleType(schema, "string")); //$NON-NLS-1$
			attribute.setUse(ISchemaAttribute.REQUIRED);
			complexType.addAttribute(attribute);
			attribute = new SchemaAttribute(element, "id"); //$NON-NLS-1$
			attribute.setType(new SchemaSimpleType(schema, "string")); //$NON-NLS-1$
			complexType.addAttribute(attribute);
			attribute = new SchemaAttribute(element, "name"); //$NON-NLS-1$
			attribute.setType(new SchemaSimpleType(schema, "string")); //$NON-NLS-1$
			attribute.setTranslatableProperty(true);
			complexType.addAttribute(attribute);
			schema.addElement(element);
		}
		section = new DocumentSection(schema, IDocumentSection.EXAMPLES,
		"Examples"); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_USAGE));
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.API_INFO,
		"API Information"); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString(KEY_SECTIONS_API));
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.IMPLEMENTATION,
		"Supplied Implementation"); //$NON-NLS-1$
		section.setDescription(PDEPlugin
				.getResourceString(KEY_SECTIONS_SUPPLIED));
		schema.addDocumentSection(section);
		section = new DocumentSection(schema, IDocumentSection.COPYRIGHT,
		"Copyright"); //$NON-NLS-1$
		section.setDescription(PDEPlugin
				.getResourceString(KEY_SECTIONS_COPYRIGHT));
		schema.addDocumentSection(section);
		StringWriter swriter = new StringWriter();
		try {
			PrintWriter writer = new PrintWriter(swriter, true);
			schema.save(writer);
			swriter.close();
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
		try {
			return new ByteArrayInputStream(swriter.toString().getBytes("UTF8")); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			return new ByteArrayInputStream(new byte[0]);
		}
	}
	private IFile generateSchemaFile(String pluginId, String id, String name,
			boolean shared, String schema, IProgressMonitor monitor)
	throws CoreException {
		IFile schemaFile = null;
		
		IWorkspace workspace = fContainer.getWorkspace();
		IPath schemaPath = new Path(schema).removeLastSegments(1);
		IPath newSchemaPath = fContainer.getProjectRelativePath().append(schemaPath);
		monitor.subTask(PDEPlugin.getResourceString(KEY_GENERATING));
		if (newSchemaPath.isEmpty() == false) {
			IFolder folder = fContainer.getProject().getFolder(newSchemaPath);
			CoreUtility.createFolder(folder, true, true, null);
		}
		InputStream source = createSchemaStream(pluginId, id, name, shared);
		IPath filePath = fContainer.getFullPath().append(schema);
		schemaFile = workspace.getRoot().getFile(filePath);
		if (!schemaFile.exists()) {
			// create for the first time
			schemaFile.create(source, true, monitor);
		} else {
			schemaFile.setContents(source, true, false, monitor);
		}
		monitor.done();
		IDE.setDefaultEditor(schemaFile, IPDEUIConstants.SCHEMA_EDITOR_ID);
		return schemaFile;
	}
	public IRunnableWithProgress getOperation() {
		final boolean openFile = fOpenSchemaButton.getSelection();
		final String id = fIdText.getText();
		final String name = fNameText.getText();
		final String schema = fSchemaText.getText();
		final boolean shared = fSharedSchemaButton != null ? fSharedSchemaButton
				.getSelection() : false;
				IRunnableWithProgress operation = new WorkspaceModifyOperation() {
					public void execute(IProgressMonitor monitor) {
						try {
							String schemaName = schema;
							if (!schema.endsWith(".exsd")) //$NON-NLS-1$
								schemaName = schema + ".exsd"; //$NON-NLS-1$
							
							IFile file = fContainer.getFile(new Path(schema));
							// do not overwrite if schema already exists
							if (!file.exists())
								file = generateSchemaFile(getPluginId(), id, name,
									shared, schemaName, monitor);
							
							if (file != null && openFile){
								fSchemaText.setText(file.getProjectRelativePath().toString());
								openSchemaFile(file);
							}
							
						} catch (CoreException e) {
							PDEPlugin.logException(e);
						} finally {
							monitor.done();
						}
					}
				};
				return operation;
	}
	public String getSchemaLocation() {
		if (fSchemaText!=null){
			String schema = fSchemaText.getText();
			if (schema.length() == 0) {
				if (fSchemaLocationText != null
						&& SCHEMA_DIR.equals(new Path(fSchemaLocationText
								.getText()).lastSegment())) {
					return ""; //$NON-NLS-1$
				}
				return SCHEMA_DIR;
			}
			
			int loc = schema.lastIndexOf("/"); //$NON-NLS-1$
			if (loc!=-1)
				return schema.substring(0,loc);
		}
		return ""; //$NON-NLS-1$
	}
	public String getPluginId() {
		if (fPluginIdText != null) {
			return fPluginIdText.getText();
		}
		return ""; //$NON-NLS-1$
	}
	
	protected boolean isPluginIdNeeded() {
		return false;
	}
	protected boolean isPluginIdFinal(){
		return false;
	}
	protected boolean isSharedSchemaSwitchNeeded() {
		return false;
	}
	private void openSchemaFile(final IFile file) {
		final IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
		Display d = ww.getShell().getDisplay();
		d.asyncExec(new Runnable() {
			public void run() {
				try {
					String editorId = IPDEUIConstants.SCHEMA_EDITOR_ID;
					ww.getActivePage().openEditor(new FileEditorInput(file),
							editorId);
				} catch (PartInitException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}
	public boolean checkFieldsFilled() {
		
		boolean empty = fIdText.getText().length() == 0 || fNameText.getText().length() == 0;
		if (!empty && isPluginIdNeeded()) {
			empty = getPluginId().length() == 0 || fSchemaText.getText().length() == 0 ;
		}
		if (!empty && !isPluginIdFinal())
			empty = fSchemaLocationText.getText().length() == 0;
		return !empty;
	}

	private void validatePage(boolean hasContainerChanged) {
		if (hasContainerChanged && !validateContainer())
			return;
		boolean isComplete = checkFieldsFilled();
		
		String message = null;
		if (!isComplete) {
			if (isPluginIdNeeded())
				message = PDEPlugin.getResourceString(KEY_MISSING_ID);
			else
				message = PDEPlugin.getResourceString(KEY_NO_PLUGIN_MISSING_ID);
		}
		setPageComplete(isComplete);
		setMessage(message, IMessageProvider.WARNING);
	}
	private boolean validateContainer() {
		if (isPluginIdNeeded() && !isPluginIdFinal()){
			String newContainerName = fSchemaLocationText.getText();
			IWorkspaceRoot root = PDECore.getWorkspace().getRoot();
			IPath workspacePath = root.getLocation();
			if (newContainerName.startsWith(workspacePath.toString()))
				newContainerName = newContainerName.replaceFirst(workspacePath.toString(), ""); //$NON-NLS-1$
			if (newContainerName.length() == 0){
				handleInvalidContainer();
				return false;
			}
			if (root.exists(new Path(newContainerName)))
				fContainer = root.getContainerForLocation(workspacePath.append(newContainerName));
			else if (fProject != null && fProject.exists(new Path(newContainerName)))
				fContainer = root.getContainerForLocation(fProject.getLocation().append(newContainerName));
			else{
				handleInvalidContainer();
				return false;
			}
			handleValidContainer();
			return true;
		}
		
		boolean exists = fContainer != null && fContainer.exists();
		if (!exists)
			handleInvalidContainer();
		return exists;
	}
	private void handleInvalidContainer(){
		setErrorMessage(PDEPlugin
				.getResourceString("BaseExtensionPointMainPage.noContainer")); //$NON-NLS-1$
		setPageComplete(false);
	}
	private void handleValidContainer(){
		setErrorMessage(null);
	}
	private void handlePluginBrowse(){
		PluginSelectionDialog dialog = new PluginSelectionDialog(getShell(), PDECore.getDefault().getWorkspaceModelManager().getAllModels(), false);
		dialog.create();
		if (dialog.open() == Window.OK){
			WorkspacePluginModelBase workspaceModelBase = (WorkspacePluginModelBase)dialog.getFirstResult();
			fPluginIdText.setText(workspaceModelBase.getPluginBase().getId());
		}
	}
	private void handleSchemaLocation(){
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
		dialog.setTitle(PDEPlugin.getResourceString("BaseExtensionPointMainPage.schemaLocation.title")); //$NON-NLS-1$
		dialog.setMessage(PDEPlugin.getResourceString("BaseExtensionPointMainPage.schemaLocation.desc")); //$NON-NLS-1$
		dialog.setDoubleClickSelects(false);
		dialog.setAllowMultiple(false);
		dialog.addFilter(new ViewerFilter(){
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IFile)
					return false;
				else if (isPluginIdFinal())
					return ((IResource)element).getProject().equals(fProject);
				return true;
			}
		});
		
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
		dialog.setInitialSelection(fProject);
		if (dialog.open() == Window.OK) {
			Object[] elements = dialog.getResult();
			if (elements.length >0){
				IResource elem = (IResource) elements[0];
				String newPath = getWorkspaceRelativePath(elem.getLocation().toString());
				fSchemaLocationText.setText(newPath + "/"); //$NON-NLS-1$
			}
		}
	}
	private String getWorkspaceRelativePath(String path){
		String workspacePath = PDECore.getWorkspace().getRoot().getLocation().toString();
		if (path.startsWith(workspacePath))
			path = path.replaceFirst(workspacePath, ""); //$NON-NLS-1$
		return path;
	}
}
