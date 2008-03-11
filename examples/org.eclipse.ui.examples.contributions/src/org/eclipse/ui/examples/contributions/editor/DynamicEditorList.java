/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.ui.examples.contributions.editor;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.examples.contributions.model.PersonInput;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;

/**
 * Provide a dynamic list of open editors to activate.
 * 
 * @since 3.4
 */
public class DynamicEditorList extends CompoundContributionItem {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.actions.CompoundContributionItem#getContributionItems()
	 */
	protected IContributionItem[] getContributionItems() {
		// maybe we can find a better way for contributed IContributionItems
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		ArrayList menuList = new ArrayList();
		IEditorReference[] editors = window.getActivePage()
				.getEditorReferences();

		int editorNum = 1;
		for (int i = 0; i < editors.length && editorNum < 10; i++) {
			try {
				if (editors[i].getId().equals(InfoEditor.ID)) {
					menuList.add(createItem(editorNum++, editors[i]));
				}
			} catch (PartInitException e) {
			}

		}
		return (IContributionItem[]) menuList
				.toArray(new IContributionItem[menuList.size()]);
	}

	private IContributionItem createItem(int i, IEditorReference ref)
			throws PartInitException {
		CommandContributionItemParameter p = new CommandContributionItemParameter(
				PlatformUI.getWorkbench(), null, ActivateEditorHandler.ID,
				CommandContributionItem.STYLE_PUSH);
		p.parameters = new HashMap();
		PersonInput editorInput = (PersonInput) ref.getEditorInput();
		p.parameters.put(ActivateEditorHandler.PARM_EDITOR, new Integer(
				editorInput.getIndex()));
		String menuNum = Integer.toString(i);
		p.label = menuNum + " " + ref.getTitle(); //$NON-NLS-1$
		p.mnemonic = menuNum;
		return new CommandContributionItem(p);
	}
}
