/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.common.path;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.common.utils.environment.EnvironmentVariableResolver;
import org.eclipse.titan.common.utils.environment.EnvironmentVariableResolver.VariableNotFoundException;
import org.eclipse.ui.console.MessageConsole;

/**
 * Utility class to resolve eclipse paths.
 *
 * @author Kristof Szabados
 * @author Jeno Balasko
 */
public final class TITANPathUtilities {

	private TITANPathUtilities() {
		// Do nothing
	}

	/**
	 * Resolves the provided URI relative to the base URI Environment variables
	 * and path variables will be resolved if it is possible for all. If all
	 * variable can be resolved and the path is not absolute then the path will
	 * be prefixed with the basePath If there is any unresolved variable then
	 * the pathToBeResolved will not resolved but Unresolved path variables of
	 * form "[VAR]" will be replaced with "${VAR}. This way the return value of
	 * this function always can be used to build a Makefile content. This last
	 * feature is the extra related to resolvePathURI
	 *
	 * @param pathToBeResolved
	 * @param basePath
	 * @return the resolved string
	 */
	public static String resolvePathURIForMakefile(final String pathToBeResolved, final String basePath,
			final boolean reportDebugInformation, final MessageConsole outputConsole) {
		final URI uri = resolvePathURI(pathToBeResolved, basePath);
		if (uri != null) {
			return PathConverter.convert(URIUtil.toPath(uri).toOSString(), reportDebugInformation,
					outputConsole);
		} else {
			return EnvironmentVariableResolver.eclipseStyle().replaceEnvVarsWithUnixEnvVars(pathToBeResolved);
		}

	}

	/**
	 * Resolves the provided uri relative to the provided base uri. Environment
	 * variables and path variables will be resolved
	 *
	 * @param pathToBeResolved
	 *            the path to be resolved.
	 * @param basePath
	 *            the full path to which the resolvable one might be relative
	 *            to.
	 *
	 * @return the resolved uri.
	 */
	// TODO: call resolvePathURI, it is the same functionality!!!
	public static URI resolvePath(final String pathToBeResolved, final URI basePath) {
		Map<?, ?> envVariables;
		if (DebugPlugin.getDefault() != null) {
			envVariables = DebugPlugin.getDefault().getLaunchManager().getNativeEnvironmentCasePreserved();
		} else {
			envVariables = null;
		}

		String tmp2 = null;
		try {
			final String tmp1 = EnvironmentVariableResolver.eclipseStyle().resolve(pathToBeResolved, envVariables);
			tmp2 = EnvironmentVariableResolver.unixStyle().resolveIgnoreErrors(tmp1, envVariables);
		} catch (VariableNotFoundException e) {
			ErrorReporter.logError("There was an error while resolving `" + pathToBeResolved + "'");
			return null;
		}

		final IPathVariableManager pathVariableManager = ResourcesPlugin.getWorkspace().getPathVariableManager();
		URI uri = URIUtil.toURI(tmp2);// the trailing dots are removed but later corrected
		uri = pathVariableManager.resolveURI(uri);

		if (basePath != null && uri != null && !uri.isAbsolute()) {
			final String basePathString = URIUtil.toPath(basePath).toOSString();
			final String temp = PathUtil.getAbsolutePath(basePathString, tmp2);
			if (temp != null) {
				uri = URIUtil.toURI(temp);
			}
		}
		return uri;

	}

	/**
	 * Resolves the provided path relative to the provided base path.
	 *
	 * @param pathToBeResolved
	 *            the path to be resolved.
	 * @param basePath
	 *            the full path to which the resolvable one might be relative
	 *            to.
	 *
	 * @return the resolved path.
	 */
	public static URI resolvePathURI(final String pathToBeResolved, final String basePath) {
		final DebugPlugin debugPlugin = DebugPlugin.getDefault();

		if (debugPlugin == null) {
			ErrorReporter.logError("There was an error while resolving `" + pathToBeResolved + "'"
					+ "the DebugPlugin was not yet initialized");
			return URI.create(pathToBeResolved);
		}

		final Map<?, ?> envVariables = debugPlugin.getLaunchManager().getNativeEnvironmentCasePreserved();
		return resolvePathURI(pathToBeResolved, basePath, envVariables,
				ResourcesPlugin.getWorkspace().getPathVariableManager());
	}

	/**
	 * Resolves the provided path relative to the provided base path. If the
	 * pathToBeConverted is absolute or the basePath is null or empty string,
	 * the basePath is not used
	 *
	 * @param pathToBeResolved
	 *            the path to be resolved.
	 * @param basePath
	 *            the full path to which the resolvable one might be relative
	 *            to.
	 *
	 * @return the resolved path.
	 */
	private static URI resolvePathURI(final String pathToBeResolved, final String basePath,
			final Map<?, ?> envVariables, final IPathVariableManager pathVariableManager) {

		String tmp2 = null;
		try {
			final String tmp1 = EnvironmentVariableResolver.eclipseStyle().resolve(pathToBeResolved, envVariables);
			tmp2 = EnvironmentVariableResolver.unixStyle().resolve(tmp1, envVariables); // In case of error,
			// it throws exception
		} catch (VariableNotFoundException e) {
			ErrorReporter.logWarning("There was an error while resolving `" + pathToBeResolved + "'"); // this is a normal behavior
			return null;
		}

		final URI uri = URIUtil.toURI(tmp2);
		URI resolvedURI = pathVariableManager.resolveURI(uri);

		if (basePath != null && !"".equals(basePath) && !resolvedURI.isAbsolute()) {
			final String temp = PathUtil.getAbsolutePath(basePath, tmp2);
			if (temp != null) {
				resolvedURI = URIUtil.toURI(temp);
			}
		}

		return resolvedURI;
	}

}
