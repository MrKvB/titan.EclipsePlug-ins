/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.metrics.topview;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titanium.utils.LocationHighlighter;

/**
 * Double click listener for the {@link TopView}.
 *
 * @author poroszd
 *
 */
class DCListener implements IDoubleClickListener {
	@Override
	public void doubleClick(final DoubleClickEvent event) {
		if (event.getSelection() instanceof IStructuredSelection) {
			final Object o = ((IStructuredSelection) event.getSelection()).getFirstElement();
			Location loc = null;
			if (o instanceof Module) {
				loc = ((Module) o).getLocation();

				LocationHighlighter.jumpToLocation(loc);
			}
		}
	}
}
