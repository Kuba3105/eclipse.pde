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
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;

public class ConvertedProjectWizard extends NewWizard {
	private ConvertedProjectsPage mainPage;
	private Vector selected;
	private IProject[] fUnconverted;
	private static final String KEY_WTITLE = "ConvertedProjectWizard.title"; //$NON-NLS-1$

public ConvertedProjectWizard(IProject[] projects, Vector initialSelection) {
	setDefaultPageImageDescriptor(PDEPluginImages.DESC_CONVJPPRJ_WIZ);
	setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
	setNeedsProgressMonitor(true);
	this.selected = initialSelection;
	this.fUnconverted = projects;
}

public void addPages() {
	mainPage = new ConvertedProjectsPage(fUnconverted, selected);
	addPage(mainPage);
}
public boolean performFinish() {
	return mainPage.finish();
}
}
