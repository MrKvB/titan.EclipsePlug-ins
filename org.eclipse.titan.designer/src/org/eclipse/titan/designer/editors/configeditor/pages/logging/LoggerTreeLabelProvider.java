/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor.pages.logging;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.titan.common.parsers.cfg.indices.LoggingSectionHandler;

/**
 * @author Adam Delic
 * */
public final class LoggerTreeLabelProvider extends LabelProvider {

	@Override
	public Image getImage(final Object element) {
		return null;
	}

	@Override
	public String getText(final Object element) {
		if (element != null && element instanceof LoggingSectionHandler.LoggerTreeElement) {
			final LoggingSectionHandler.LoggerTreeElement lte = (LoggingSectionHandler.LoggerTreeElement) element;
			if (lte.getPluginName() == null) {
				// this is a component
				if ("*".equals(lte.getComponentName())) {
					return "* (all valid components)";
				}
				return lte.getComponentName();
			}

			// this is a plugin
			if ("*".equals(lte.getPluginName())) {
				return "* (all valid plugins)";
			}

			final String pluginPath = lte.getLsh().componentPlugin(lte.getComponentName(), lte.getPluginName()).getPluginPath();
			if (pluginPath != null && !"".equals(pluginPath)) {
				final StringBuffer sb = new StringBuffer();
				sb.append(lte.getPluginName()).append(" (").append(pluginPath).append(')');
				return sb.toString();
			}

			return lte.getPluginName();
		}
		return "";
	}
}
