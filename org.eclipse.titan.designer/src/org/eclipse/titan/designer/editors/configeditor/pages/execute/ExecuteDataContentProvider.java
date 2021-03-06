/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor.pages.execute;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.titan.common.parsers.cfg.indices.ExecuteSectionHandler;

/**
 * @author Kristof Szabados
 * */
public final class ExecuteDataContentProvider implements IStructuredContentProvider {

	@Override
	public Object[] getElements(final Object inputElement) {
		if (inputElement != null) {
			if (inputElement instanceof ExecuteSectionHandler) {
				return ((ExecuteSectionHandler) inputElement).getExecuteitems().toArray();
			}
		}

		return new Object[] {};
	}

	@Override
	public void dispose() {
		//Do nothing
	}

	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
		//Do nothing
	}
}
