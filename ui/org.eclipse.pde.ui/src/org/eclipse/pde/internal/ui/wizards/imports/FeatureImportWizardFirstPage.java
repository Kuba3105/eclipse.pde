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
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public class FeatureImportWizardFirstPage extends WizardPage {

	private static final String SETTINGS_DROPLOCATION = "droplocation"; //$NON-NLS-1$
	private static final String SETTINGS_DOOTHER = "doother"; //$NON-NLS-1$
	private static final String SETTINGS_NOT_BINARY = "notbinary"; //$NON-NLS-1$

	private Label fOtherLocationLabel;
	private Button fRuntimeLocationButton;
	private Button fBrowseButton;
	private Combo fDropLocation;
	private Button fBinaryButton;

	public FeatureImportWizardFirstPage() {
		super("FeatureImportWizardPage"); //$NON-NLS-1$
		setTitle(PDEPlugin.getResourceString("FeatureImportWizard.FirstPage.title")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("FeatureImportWizard.FirstPage.desc")); //$NON-NLS-1$
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);

		fRuntimeLocationButton = new Button(composite, SWT.CHECK);
		fillHorizontal(fRuntimeLocationButton, 3, false);
		fRuntimeLocationButton.setText(
			PDEPlugin.getResourceString("FeatureImportWizard.FirstPage.runtimeLocation")); //$NON-NLS-1$

		fOtherLocationLabel = new Label(composite, SWT.NULL);
		fOtherLocationLabel.setText(
			PDEPlugin.getResourceString("FeatureImportWizard.FirstPage.otherFolder")); //$NON-NLS-1$

		fDropLocation = new Combo(composite, SWT.DROP_DOWN);
		fillHorizontal(fDropLocation, 1, true);

		fBrowseButton = new Button(composite, SWT.PUSH);
		fBrowseButton.setText(PDEPlugin.getResourceString("FeatureImportWizard.FirstPage.browse")); //$NON-NLS-1$
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPath chosen = chooseDropLocation();
				if (chosen != null)
					fDropLocation.setText(chosen.toOSString());
			}
		});
		fBrowseButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fBrowseButton);

		fBinaryButton = new Button(composite, SWT.CHECK);
		fillHorizontal(fBinaryButton, 3, false);
		fBinaryButton.setText(
			PDEPlugin.getResourceString("FeatureImportWizard.FirstPage.binaryImport")); //$NON-NLS-1$
		
		initializeFields(getDialogSettings());
		hookListeners();

		setControl(composite);
		Dialog.applyDialogFont(composite);
		WorkbenchHelp.setHelp(composite, IHelpContextIds.FEATURE_IMPORT_FIRST_PAGE);
	}

	private String getTargetHome() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		return preferences.getString(ICoreConstants.PLATFORM_PATH);
	}

	private void hookListeners() {
		fRuntimeLocationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setOtherEnabled(!fRuntimeLocationButton.getSelection());
				validateDropLocation();
				if (fRuntimeLocationButton.getSelection()) {
					fDropLocation.setText(getTargetHome());
				}
			}
		});

		fDropLocation.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				validateDropLocation();
			}
		});
		fDropLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateDropLocation();
			}
		});
	}

	private GridData fillHorizontal(Control control, int span, boolean grab) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = span;
		gd.grabExcessHorizontalSpace = grab;
		control.setLayoutData(gd);
		return gd;
	}

	private void initializeFields(IDialogSettings initialSettings) {
		String[] dropItems = new String[0];
		boolean doOther = false;
		boolean binary = true;

		if (initialSettings != null) {
			doOther = initialSettings.getBoolean(SETTINGS_DOOTHER);
			binary = !initialSettings.getBoolean(SETTINGS_NOT_BINARY);

			ArrayList items = new ArrayList();
			for (int i = 0; i < 6; i++) {
				String curr =
					initialSettings.get(
						SETTINGS_DROPLOCATION + String.valueOf(i));
				if (curr != null && !items.contains(curr)) {
					items.add(curr);
				}
			}
			dropItems = (String[]) items.toArray(new String[items.size()]);
		}
		fDropLocation.setItems(dropItems);
		fRuntimeLocationButton.setSelection(!doOther);
		setOtherEnabled(doOther);
		if (doOther) {
			if (dropItems.length > 0)
				fDropLocation.setText(dropItems[0]);
		} else {
			fDropLocation.setText(getTargetHome());
		}
		fBinaryButton.setSelection(binary);

		validateDropLocation();
	}

	private void setOtherEnabled(boolean enabled) {
		fOtherLocationLabel.setEnabled(enabled);
		fDropLocation.setEnabled(enabled);
		fBrowseButton.setEnabled(enabled);
	}

	public void storeSettings(boolean finishPressed) {
		IDialogSettings settings = getDialogSettings();
		boolean other = !fRuntimeLocationButton.getSelection();
		boolean binary = fBinaryButton.getSelection();
		if (finishPressed || fDropLocation.getText().length() > 0 && other) {
			settings.put(
				SETTINGS_DROPLOCATION + String.valueOf(0),
				fDropLocation.getText());
			String[] items = fDropLocation.getItems();
			int nEntries = Math.min(items.length, 5);
			for (int i = 0; i < nEntries; i++) {
				settings.put(
					SETTINGS_DROPLOCATION + String.valueOf(i + 1),
					items[i]);
			}
		}
		if (finishPressed) {
			settings.put(SETTINGS_DOOTHER, other);
			settings.put(SETTINGS_NOT_BINARY, !binary);
		}
	}

	/**
	 * Browses for a drop location.
	 */
	private IPath chooseDropLocation() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setFilterPath(fDropLocation.getText());
		dialog.setText(PDEPlugin.getResourceString("FeatureImportWizard.messages.folder.title")); //$NON-NLS-1$
		dialog.setMessage(PDEPlugin.getResourceString("FeatureImportWizard.messages.folder.message")); //$NON-NLS-1$
		String res = dialog.open();
		if (res != null) {
			return new Path(res);
		}
		return null;
	}

	private void validateDropLocation() {
		String errorMessage = null;
		if (isOtherLocation()) {
			IPath curr = getDropLocation();
			if (curr.segmentCount() == 0) {
				errorMessage =
					PDEPlugin.getResourceString(
						"FeatureImportWizard.errors.locationMissing"); //$NON-NLS-1$
			} else if (!Path.ROOT.isValidPath(fDropLocation.getText())) {
				errorMessage =
					PDEPlugin.getResourceString(
						"FeatureImportWizard.errors.buildFolderInvalid"); //$NON-NLS-1$
			} else {

				File file = curr.toFile();
				if (!file.exists() || !file.isDirectory()) {
					errorMessage =
						PDEPlugin.getResourceString(
							"FeatureImportWizard.errors.buildFolderMissing"); //$NON-NLS-1$
				}
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}
	
	public boolean isBinary(){
		return fBinaryButton.getSelection();
	}

	/**
	 * Returns the drop location.
	 */
	public IPath getDropLocation() {
		return new Path(fDropLocation.getText());
	}

	public boolean isOtherLocation() {
		return !fRuntimeLocationButton.getSelection();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		return getErrorMessage() == null;
	}
}
