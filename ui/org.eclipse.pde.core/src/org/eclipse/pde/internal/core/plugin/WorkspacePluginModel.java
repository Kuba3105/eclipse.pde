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
package org.eclipse.pde.internal.core.plugin;

import org.eclipse.pde.core.plugin.*;

public class WorkspacePluginModel extends WorkspacePluginModelBase implements
		IPluginModel {

	private static final long serialVersionUID = 1L;

	public WorkspacePluginModel(org.eclipse.core.resources.IFile file) {
		super(file);
	}

	public IPluginBase createPluginBase() {
		Plugin plugin = new Plugin();
		plugin.setModel(this);
		return plugin;
	}

	public IPlugin getPlugin() {
		return (IPlugin) getPluginBase();
	}
}
