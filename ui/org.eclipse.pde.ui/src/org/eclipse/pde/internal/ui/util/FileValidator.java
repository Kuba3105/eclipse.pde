/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.dialogs.*;

public class FileValidator implements ISelectionStatusValidator {

	public IStatus validate(Object[] selection) {
		if (selection.length > 0 && selection[0] instanceof IFile) {
			return new Status(
				IStatus.OK,
				PDEPlugin.getPluginId(),
				IStatus.OK,
				"", //$NON-NLS-1$
				null);
		}
		return new Status(
			IStatus.ERROR,
			PDEPlugin.getPluginId(),
			IStatus.ERROR,
			"", //$NON-NLS-1$
			null);
	}

}
