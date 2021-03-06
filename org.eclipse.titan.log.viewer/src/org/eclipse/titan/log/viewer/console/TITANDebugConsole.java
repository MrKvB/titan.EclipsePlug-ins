/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.log.viewer.console;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;

public final class TITANDebugConsole {
	private static final String TITLE = "TITAN Log Viewer Debug console";
	private static MessageConsole console = null;

	private TITANDebugConsole() {
	}

	public static synchronized MessageConsole getConsole() {
		if (null == console) {
			console = new MessageConsole(TITLE, null);
			console.activate();
			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] {console});
		}
		return console;
	}
}
