/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards;

import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.RequiredPluginsClasspathContainer;
import org.eclipse.pde.internal.core.SourceAttachmentManager;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * Insert the type's description here.
 * @see WizardPage
 */
public class RequiredPluginsContainerPage
	extends WizardPage
	implements IClasspathContainerPage, IClasspathContainerPageExtension {
	private IClasspathEntry entry;
	private CheckboxTableViewer viewer;
	private Image projectImage;
	private Image libraryImage;
	private Image slibraryImage;
	private IClasspathEntry[] realEntries;
	private IJavaProject javaProject;
	private Button attachSourceButton;
	private Hashtable replacedEntries;

	class EntryContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (realEntries != null)
				return realEntries;
			return new Object[0];
		}
	}

	class EntrySorter extends ViewerSorter {
		public int category(Object obj) {
			IClasspathEntry entry = (IClasspathEntry) obj;
			return entry.getEntryKind() == IClasspathEntry.CPE_PROJECT
				? -10
				: 0;
		}
	}

	class EntryLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getText(Object obj) {
			IClasspathEntry entry = (IClasspathEntry) obj;
			int kind = entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_PROJECT)
				return entry.getPath().segment(0);
			IPath path = entry.getPath();
			String name = path.lastSegment();
			return name
				+ " - " //$NON-NLS-1$
				+ path.uptoSegment(path.segmentCount() - 1).toOSString();
		}

		public Image getImage(Object obj) {
			IClasspathEntry entry = (IClasspathEntry) obj;
			int kind = entry.getEntryKind();
			if (kind == IClasspathEntry.CPE_PROJECT)
				return projectImage;
			else if (kind == IClasspathEntry.CPE_LIBRARY) {
				IPath sourceAtt = entry.getSourceAttachmentPath();
				return sourceAtt!=null?slibraryImage:libraryImage;
			}
			return null;
		}
		public String getColumnText(Object obj, int col) {
			return getText(obj);
		}
		public Image getColumnImage(Object obj, int col) {
			return getImage(obj);
		}
	}
	/**
	 * The constructor.
	 */
	public RequiredPluginsContainerPage() {
		super("requiredPluginsContainerPage"); //$NON-NLS-1$
		setTitle(PDEUIMessages.RequiredPluginsContainerPage_title); //$NON-NLS-1$
		setDescription(PDEUIMessages.RequiredPluginsContainerPage_desc); //$NON-NLS-1$
		projectImage =
			PlatformUI.getWorkbench().getSharedImages().getImage(
				IDE.SharedImages.IMG_OBJ_PROJECT);
		//libraryImage = PDEPluginImages.DESC_BUILD_VAR_OBJ.createImage();
		libraryImage =
			JavaUI.getSharedImages().getImage(
				org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE);
		slibraryImage =
				JavaUI.getSharedImages().getImage(
				org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE_WITH_SOURCE);
		setImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
		replacedEntries = new Hashtable();
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#createControl
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		Label label = new Label(container, SWT.NULL);
		label.setText(PDEUIMessages.RequiredPluginsContainerPage_label); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		viewer =
			CheckboxTableViewer.newCheckList(
				container,
				SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setContentProvider(new EntryContentProvider());
		viewer.setLabelProvider(new EntryLabelProvider());
		viewer.setSorter(new EntrySorter());
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				// Prevent user to change checkbox states
				viewer.setChecked(event.getElement(), !event.getChecked());
			}
		});
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged((IStructuredSelection) e.getSelection());
			}
		});
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 400;
		gd.heightHint = 300;
		viewer.getTable().setLayoutData(gd);
		attachSourceButton = new Button(container, SWT.PUSH);
		attachSourceButton.setText(PDEUIMessages.RequiredPluginsContainerPage_attachSource); //$NON-NLS-1$
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		attachSourceButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(attachSourceButton);
		attachSourceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAttachSource();
			}
		});
		attachSourceButton.setEnabled(false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(
			container,
			IHelpContextIds.PLUGINS_CONTAINER_PAGE);
		setControl(container);
		Dialog.applyDialogFont(container);
		if (realEntries != null)
			initializeView();
	}

	private void handleSelectionChanged(IStructuredSelection selection) {
		IClasspathEntry entry = (IClasspathEntry) selection.getFirstElement();
		boolean canAttach = true;
		if (entry == null
			|| entry.getEntryKind() != IClasspathEntry.CPE_LIBRARY)
			canAttach = false;
		attachSourceButton.setEnabled(canAttach);
	}

	private IClasspathEntry getEditableEntry(IClasspathEntry entry) {
		IClasspathEntry modifiedEntry =
			(IClasspathEntry) replacedEntries.get(entry);
		if (modifiedEntry != null)
			return modifiedEntry;
		return entry;
	}

	private void handleAttachSource() {
		IStructuredSelection ssel =
			(IStructuredSelection) viewer.getSelection();
		IClasspathEntry entry = (IClasspathEntry) ssel.getFirstElement();
		IClasspathEntry editableEntry = getEditableEntry(entry);

		IClasspathEntry newEntry = BuildPathDialogAccess.configureSourceAttachment(
				viewer.getControl().getShell(),
				editableEntry);
		if (newEntry != null)
			replacedEntries.put(entry, newEntry);
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#finish
	 */
	public boolean finish() {
		if (replacedEntries.size() > 0) {
			// must handle edited entries
			processReplacedEntries();
		}
		return true;
	}

	private void processReplacedEntries() {
		SourceAttachmentManager manager =
			PDECore.getDefault().getSourceAttachmentManager();
		for (Enumeration keys = replacedEntries.keys();
			keys.hasMoreElements();
			) {
			IClasspathEntry entry = (IClasspathEntry) keys.nextElement();
			IClasspathEntry newEntry =
				(IClasspathEntry) replacedEntries.get(entry);
			manager.addEntry(
				newEntry.getPath(),
				newEntry.getSourceAttachmentPath(),
				newEntry.getSourceAttachmentRootPath());
		}
		manager.save();
		resetContainer();
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#getSelection
	 */
	public IClasspathEntry getSelection() {
		return entry;
	}

	public void initialize(
		IJavaProject project,
		IClasspathEntry[] currentEntries) {
		javaProject = project;
	}

	/**
	 * Insert the method's description here.
	 * @see WizardPage#setSelection
	 */
	public void setSelection(IClasspathEntry containerEntry) {
		this.entry = containerEntry;
		createRealEntries();
		if (viewer != null)
			initializeView();
	}

	private void resetContainer() {
		IJavaProject javaProject = getJavaProject();
		if (javaProject != null) {
			try {
				IClasspathContainer container =
					JavaCore.getClasspathContainer(
						entry.getPath(),
						javaProject);
				if (container instanceof RequiredPluginsClasspathContainer) {
					((RequiredPluginsClasspathContainer) container).reset();
				}
			} catch (JavaModelException e) {
			}
		}
	}

	private void createRealEntries() {
		IJavaProject javaProject = getJavaProject();
		if (javaProject == null) {
			realEntries = new IClasspathEntry[0];
			return;
		}

		if (entry == null) {
/*
			entry = ClasspathUtilCore.createContainerEntry();
			try {
				ModelEntry.updateUnknownClasspathContainer(javaProject);
				realEntries = new IClasspathEntry[0];
			} catch (CoreException e) {
			}
*/
			entry = ClasspathUtilCore.createContainerEntry();
			PluginModelManager mng = PDECore.getDefault().getModelManager();
			ModelEntry entry = mng.findEntry(javaProject.getProject());
			if (entry!=null) {
				IClasspathContainer container = entry.getClasspathContainer();
				if (container!=null)
					realEntries = container.getClasspathEntries();
			}
		} else {
			try {
				IClasspathContainer container =
					JavaCore.getClasspathContainer(
						entry.getPath(),
						javaProject);
				if (container != null)
					realEntries = container.getClasspathEntries();
			} catch (JavaModelException e) {
			}
		}
		if (realEntries == null)
			realEntries = new IClasspathEntry[0];
	}

	private IJavaProject getJavaProject() {
		return javaProject;
	}

	private void initializeView() {
		viewer.setInput(entry);
		viewer.setAllGrayed(true);
		for (int i = 0; i < realEntries.length; i++) {
			if (realEntries[i].isExported())
				viewer.setChecked(realEntries[i], true);
		}
	}
}
