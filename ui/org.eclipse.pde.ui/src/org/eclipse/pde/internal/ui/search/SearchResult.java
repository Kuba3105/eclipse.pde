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
package org.eclipse.pde.internal.ui.search;

import java.io.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.search.ui.*;
import org.eclipse.search.ui.text.*;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.*;

public class SearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter {
	protected ISearchQuery fQuery;
	
	public SearchResult(ISearchQuery query) {
		fQuery = query;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getEditorMatchAdapter()
	 */
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getLabel()
	 */
	public String getLabel() {
		int numMatches = getMatchCount();	
		return fQuery.getLabel() + " - " + numMatches + " " + (numMatches == 1 ? PDEPlugin.getResourceString("SearchResult.match") : PDEPlugin.getResourceString("SearchResult.matches")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getTooltip()
	 */
	public String getTooltip() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return PDEPluginImages.DESC_PSEARCH_OBJ;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.ISearchResult#getQuery()
	 */
	public ISearchQuery getQuery() {
		return fQuery;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.IEditorMatchAdapter#isShownInEditor(org.eclipse.search.ui.text.Match, org.eclipse.ui.IEditorPart)
	 */
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		Object element = match.getElement();
		if (element instanceof IPluginObject)
			return isMatchContained(editor, (IPluginObject)element);
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.IEditorMatchAdapter#computeContainedMatches(org.eclipse.search.ui.text.AbstractTextSearchResult, org.eclipse.ui.IEditorPart)
	 */
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		ArrayList list = new ArrayList();
		Object[] objects = result.getElements();
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof IPluginObject) {
				IPluginObject object = (IPluginObject)objects[i];
				if (isMatchContained(editor, object)){
					Match[] matches = getMatches(object);
					for (int j = 0; j < matches.length; j++) {
						IDocument document = getDocument(editor, matches[j]);
						if (document != null)
							list.add(ManifestEditorOpener.findExactMatch(document, matches[j]));
					}
				}
			}
		}
		return (Match[]) list.toArray(new Match[list.size()]);
	}
	


	/* (non-Javadoc)
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getFileMatchAdapter()
	 */
	public IFileMatchAdapter getFileMatchAdapter() {
		return null;
	}
	
	protected boolean isMatchContained(IEditorPart editor, IPluginObject object) {
		IFile resource = (IFile)editor.getEditorInput().getAdapter(IFile.class);
		if (resource != null) {
			IResource objectResource = object.getModel().getUnderlyingResource();
			if (objectResource != null)
				return resource.getProject().equals(objectResource.getProject());
		}
		File file = (File)editor.getEditorInput().getAdapter(File.class);
		if (file != null) {
			IPath path = new Path(object.getModel().getInstallLocation());
			IPath filePath = null;
			if ("MANIFEST.MF".equals(file.getName())) //$NON-NLS-1$
				filePath = new Path(file.getParentFile().getParent());
			else if (file.getName().endsWith("jar")) { //$NON-NLS-1$
				filePath = new Path(file.getPath());
			} else {
				filePath = new Path(file.getParent());
			}
			return path.equals(filePath);		
		}
		return false;
	}
	
	protected IDocument getDocument(IEditorPart editor, Match match) {
		IDocument document = null;
		if (editor instanceof ISearchEditorAccess) {
			document = ((ISearchEditorAccess)editor).getDocument(match);
		} else if (editor instanceof ITextEditor) {
			document = ((ITextEditor)editor).getDocumentProvider().getDocument(editor.getEditorInput());
		}
		return document;
	}

}
