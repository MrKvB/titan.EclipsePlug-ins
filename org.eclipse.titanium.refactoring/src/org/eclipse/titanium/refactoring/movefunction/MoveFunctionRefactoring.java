/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titanium.refactoring.movefunction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.IVisitableNode;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.TTCN3.definitions.Def_Function;
import org.eclipse.titan.designer.AST.TTCN3.definitions.TTCN3Module;
import org.eclipse.titan.designer.AST.TTCN3.definitions.VisibilityModifier;
import org.eclipse.titan.designer.AST.TTCN3.types.ComponentTypeBody;
import org.eclipse.titan.designer.AST.TTCN3.types.Component_Type;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.GlobalParser;
import org.eclipse.titan.designer.parsers.ProjectSourceParser;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * @author Bianka Bekefi
 * */
public class MoveFunctionRefactoring extends Refactoring {

	public static final String PROJECTCONTAINSERRORS = "The project `{0}'' contains errors, which might corrupt the result of the refactoring";
	public static final String PROJECTCONTAINSTTCNPPFILES = "The project `{0}'' contains .ttcnpp files, which might corrupt the result of the refactoring";
	private static final String ONTHEFLYANALAYSISDISABLED = "The on-the-fly analysis is disabled, there is semantic information present to work on";

	private final Set<IProject> projects = new HashSet<IProject>();
	private Module destinationModule;
	private final List<Module> selectedModules = new ArrayList<Module>();
	private final IStructuredSelection structSelection;
	private final MoveFunctionSettings settings;
	private Map<Module, List<FunctionData> > functions;
	private RefactoringStatus result;

	private Object[] affectedObjects;		//the list of objects affected by the change

	public MoveFunctionRefactoring(final IStructuredSelection selection, final MoveFunctionSettings settings) {
		super();
		this.structSelection = selection;
		this.settings = settings;

		final Iterator<?> it = selection.iterator();
		while (it.hasNext()) {
			final Object o = it.next();
			if (o instanceof IResource) {
				final IProject temp = ((IResource) o).getProject();
				projects.add(temp);
			}
		}
	}

	@Override
	public RefactoringStatus checkInitialConditions(final IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		result = new RefactoringStatus();
		try{
			pm.beginTask("Checking preconditions...", 2);

			final IPreferencesService prefs = Platform.getPreferencesService();//PreferenceConstants.USEONTHEFLYPARSING
			if (! prefs.getBoolean(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.USEONTHEFLYPARSING, false, null)) {
				result.addError(ONTHEFLYANALAYSISDISABLED);
			}

			// check that there are no ttcnpp files in the
			// project
			for (final IProject project : projects) {
				if (GlobalParser.hasTtcnppFiles(project)) {//FIXME actually all referencing and referenced projects need to be checked too !
					result.addError(MessageFormat.format(PROJECTCONTAINSTTCNPPFILES, project));
				}
			}
			pm.worked(1);

			// check that there are no error markers in the
			// project
			for (final IProject project : projects) {
				final IMarker[] markers = project.findMarkers(null, true, IResource.DEPTH_INFINITE);
				for (final IMarker marker : markers) {
					if (IMarker.SEVERITY_ERROR == marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR)) {
						result.addError(MessageFormat.format(PROJECTCONTAINSERRORS, project));
						break;
					}
				}
			}
			pm.worked(1);

		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace(e);
			result.addFatalError(e.getMessage());
		} finally {
			pm.done();
		}
		return result;
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}


	public Object[] getAffectedObjects() {
		return affectedObjects;
	}

	//METHODS FROM REFACTORING

	@Override
	public String getName() {
		return "Move function";
	}

	@Override
	public Change createChange(final IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if (this.structSelection == null) {
			return null;
		}

		final CompositeChange cchange = new CompositeChange("MoveFunctionRefactoring");
		boolean  first = true;
		for (final Map.Entry<Module, List<FunctionData>> entry : functions.entrySet()) {
			if (first) {
				final ChangeCreator chCreator = new ChangeCreator((IFile)entry.getKey().getLocation().getFile(), settings, entry.getValue(),
						projects.iterator().next(), new HashMap<Module, List<Module>>());
				chCreator.perform();
				final Change ch = chCreator.getChange();
				if (ch != null) {
					cchange.add(ch);
				}
				first = false;
			}
			else {
				final ChangeCreator chCreator = new ChangeCreator((IFile)entry.getKey().getLocation().getFile(), settings, entry.getValue(),
						projects.iterator().next());
				chCreator.perform();
				final Change ch = chCreator.getChange();
				if (ch != null) {
					cchange.add(ch);
				}
			}

		}
		return cchange;
	}

	public Map<Module, List<FunctionData> > getModules() {
		if (this.structSelection == null) {
			return null;
		}
		if (functions != null) {
			return functions;
		}
		final Iterator<?> it1 = this.structSelection.iterator();
		while (it1.hasNext()) {
			final Object o = it1.next();
			if (!(o instanceof IResource)) {
				continue;
			}

			final IResource res = (IResource)o;
			final SelectedModulesVisitor vis = new SelectedModulesVisitor();
			try {
				res.accept(vis);
			} catch (CoreException e) {
				ErrorReporter.logExceptionStackTrace("Error while processing the selected resources", e);
			}
			selectedModules.addAll(vis.getSelectedModules());
		}


		functions = new HashMap<Module, List<FunctionData> >();
		for (final Module module : selectedModules) {
			if (module instanceof TTCN3Module) {
				functions.put(module, new ArrayList<FunctionData>());
			}
		}
		return functions;
	}

	public Map<Module, List<FunctionData> > getFunctions() {
		return functions;
	}

	public List<FunctionData> selectMovableFunctions(final TTCN3Module ttcn3module, final SubMonitor progress) {
		if (!functions.get(ttcn3module).isEmpty()) {
			return functions.get(ttcn3module);
		}
		for (final ILocateableNode node : ttcn3module.getDefinitions()) {
			if (node instanceof Def_Function) {
				final Def_Function fun = (Def_Function)node;
				if (fun.getVisibilityModifier().equals(VisibilityModifier.Private)) {
					continue;
				}

				final ReferenceVisitor refVis = new ReferenceVisitor();
				fun.accept(refVis);
				boolean dependent = false;
				for (final ILocateableNode node2 : refVis.getLocations()) {
					if (node2 instanceof Reference) {
						final Assignment assignment = ((Reference) node2).getRefdAssignment(CompilationTimeStamp.getBaseTimestamp(), false, null);
						if (assignment != null && !assignment.isLocal() && assignment.getMyScope().getModuleScope().equals(fun.getMyScope().getModuleScope())) {
							dependent = true;
							break;
						}
					}
				}
				if (!dependent) {
					final FunctionData fd = new FunctionData(fun, createFunctionBody(fun, ttcn3module));
					fd.setModule(ttcn3module);
					functions.get(ttcn3module).add(fd);
				}
			}

			progress.worked(1);
		}
		return functions.get(ttcn3module);
	}

	public Map<Module, List<FunctionData>> chooseDestination() {
		for (final Map.Entry<Module, List<FunctionData> > entry : functions.entrySet()) {
			for (final FunctionData fun : entry.getValue()) {
				if (fun.isToBeMoved() && (fun.getRefactoringMethod() == null || settings.isChanged() /*!fun.getRefactoringMethod().equals(settings.getMethod()))*/ /*&& fun.getDestinations().isEmpty()*/)) {
					fun.clearDestinations();
					fun.setRefactoringMethod(settings.getMethod());

					final List<Module> usedModules = collectUsedModules(fun.getDefiniton(), entry.getKey());
					fun.setUsedModules(usedModules);
					if (usedModules.size() > 0) {
						switch (settings.getMethod()) {
						case LENGTH:
							chooseModuleByLength(usedModules, fun);
							addUnusedModules(fun, usedModules);
							break;
						case IMPORTS:
							chooseModuleByImports(usedModules, fun);
							break;
						case LENGTHANDIMPORTS:
							chooseModuleByLengthAndImports(usedModules, fun);
							addUnusedModules(fun, usedModules);
							break;
						}
					}
					if (settings.getMethod().equals(MoveFunctionMethod.COMPONENT)) {
						chooseModuleByComponent(fun);
					}
					//addUnusedModules(fun, usedModules);
				}
			}
		}
		settings.setChanged(false);
		return functions;
	}

	private String createFunctionBody(final Def_Function fun, final Module module) {
		try {
			final IFile file = (IFile)module.getLocation().getFile();
			final InputStream istream = file.getContents();
			final BufferedReader br = new BufferedReader(new InputStreamReader(istream, file.getCharset()));
			final int startOffset = fun.getLocation().getOffset();
			final int endOffset = fun.getLocation().getEndOffset();
			br.skip(startOffset);
			final char[] contentBuf = new char[endOffset-startOffset];
			br.read(contentBuf, 0, endOffset-startOffset);
			final String body2 = new String(contentBuf);
			br.close();
			istream.close();
			return body2;
		} catch (CoreException ce) {
			ErrorReporter.logExceptionStackTrace(ce);
		} catch (IOException ioe) {
			ErrorReporter.logExceptionStackTrace(ioe);
		}

		return "";
	}


	private void chooseModuleByLength(final List<Module> usedModules, final FunctionData function) {
		final List<Module> filteredList = filterByExcludedNames(usedModules);
		if (filteredList.size() > 0) {
			Collections.sort(filteredList, new Comparator<Module>() {
				@Override
				public int compare(final Module m1, final Module m2) {
					return ((m1.getLocation().getEndOffset()-m1.getLocation().getOffset())-(m2.getLocation().getEndOffset()-m2.getLocation().getOffset()));
				}
			});
			destinationModule = filteredList.get(0);
			final int destLength = destinationModule.getLocation().getEndOffset()-destinationModule.getLocation().getOffset();
			for (int i=1; i<filteredList.size(); i++) {
				final Module m = filteredList.get(i);
				if ((settings.getExcludedModuleNames() != null && settings.getExcludedModuleNames().matcher(m.getIdentifier().getTtcnName()).matches())
						/*&& (selectedModules.isEmpty() || selectedModules.contains(m))*/) {
					continue;
				}

				final int moduleLength = m.getLocation().getEndOffset()-m.getLocation().getOffset();
				if (moduleLength == destLength && !m.equals(destinationModule)) {
					function.addDestination(m, 100, -1);
				} else {
					function.addDestination(m, ((int)((double)destLength*100 / (double)moduleLength)), -1);
				}
			}
			function.addDestination(destinationModule, 100, -1);
		}
		//function.setFinalDestination(new Destination(destinationModule, 100, function, -1));
	}

	private List<Module> filterByExcludedNames(final List<Module> usedModules) {
		final List<Module> filteredList = new ArrayList<Module>();
		for (final Module m : usedModules) {
			if (!(settings.getExcludedModuleNames() != null && settings.getExcludedModuleNames().matcher(m.getIdentifier().getTtcnName()).matches())
					/*&& (selectedModules.isEmpty() || selectedModules.contains(m))*/) {

				filteredList.add(m);
			}
			else {
			}
		}
		return filteredList;
	}

	private void chooseModuleByImports(final List<Module> usedModules, final FunctionData function) {
		final List<Module> filteredList = filterByExcludedNames(usedModules);
		if (filteredList.size() > 0) {
			final List<Entry<Module, Integer>> list = countMissingImports(filteredList, function);
			destinationModule = list.get(0).getKey();
			final int min = list.get(0).getValue();
			function.addDestination(destinationModule, 100, min);
			list.remove(0);
			for (final Entry<Module, Integer> e : list) {
				if (e.getValue() == min) {
					function.addDestination(e.getKey(), 100, e.getValue());
				}
				else {
					function.addDestination(e.getKey(), (int)((double)min*100 / (double)e.getValue()), e.getValue());
				}
			}

			final  List<Module> filteredList2 = filterByExcludedNames(selectedModules);
			final  List<Entry<Module, Integer>> list2 = countMissingImports(filteredList2, function);
			for (final Entry<Module, Integer> e : list2) {
				if (!usedModules.contains(e.getKey()) && !function.getModule().equals(e.getKey())) {
					function.addDestination(e.getKey(), 0, e.getValue());
				}
			}

		}
	}

	private List<Entry<Module, Integer>> countMissingImports(final List<Module> usedModules, final FunctionData function) {
		final Map<Module, Integer> importsCounter = new HashMap<Module, Integer>();
		for (final Module m : usedModules) {
			if (m instanceof TTCN3Module) {
				int counter = 0;
				final List<Module> imports = ((TTCN3Module)m).getImportedModules();
				for (final Module m2 : usedModules) {
					if (!m.equals(m2) && imports.contains(m2)) {
						counter++;
					}
				}
				importsCounter.put(m, usedModules.size() - counter - 1);
			}
		}

		final List<Entry<Module, Integer>> list = new ArrayList<Entry<Module, Integer>>(importsCounter.entrySet());
		Collections.sort(list, new Comparator<Entry<Module, Integer>>() {
			@Override
			public int compare(final Entry<Module, Integer> e1, final Entry<Module, Integer> e2) {
				return e1.getValue().compareTo(e2.getValue());
			}
		});
		return list;
	}

	private void chooseModuleByLengthAndImports(final List<Module> usedModules, final FunctionData function) {
		final List<Entry<Module, Integer>> list = countMissingImports(usedModules, function);
		final int leastMissingImports = list.get(0).getValue();

		Collections.sort(list, new Comparator<Entry<Module, Integer>>() {
			@Override
			public int compare(final Entry<Module, Integer> m1, final Entry<Module, Integer> m2) {
				return ((m1.getKey().getLocation().getEndOffset()-m1.getKey().getLocation().getOffset())-(m2.getKey().getLocation().getEndOffset()-m2.getKey().getLocation().getOffset()));
			}
		});
		final Module shortestModule = list.get(0).getKey();
		final int shortestLength = shortestModule.getLocation().getEndOffset()-shortestModule.getLocation().getOffset();

		for (final Entry<Module, Integer> entry : list) {
			final int moduleLength = entry.getKey().getLocation().getEndOffset()-entry.getKey().getLocation().getOffset();
			final double value1 = ((double)shortestLength / (double)moduleLength);
			final double value2 = (double)leastMissingImports / (double)entry.getValue();
			final double value3 = value1*value2*100;
			function.addDestination(entry.getKey(), (int)value3, entry.getValue());
		}
	}

	private List<Module> collectUsedModules(final Def_Function function, final Module ttcn3module) {
		final List<Module> modules = new ArrayList<Module>();
		boolean dependent = false;
		for (final Module m : ttcn3module.getImportedModules()) {
			dependent = false;
			if (m instanceof TTCN3Module) {
				final ReferenceVisitor refVis = new ReferenceVisitor();
				function.accept(refVis);
				for (final ILocateableNode node2 : refVis.getLocations()) {
					if (node2 instanceof Reference) {
						final Assignment assignment = ((Reference) node2).getRefdAssignment(CompilationTimeStamp.getBaseTimestamp(), false, null);
						if (assignment != null && assignment.getMyScope().getModuleScope().equals(m)) {
							dependent = true;
						}
					}
				}
				if (dependent) {
					modules.add(m);
				}
			}
		}
		return modules;
	}


	public IProject getProject() {
		return projects.iterator().next();
	}

	public void chooseModuleByComponent(final FunctionData function) {
		final Component_Type comp = function.getDefiniton().getRunsOnType(CompilationTimeStamp.getBaseTimestamp());
		if (comp != null) {
			final Map<Module, Integer> compCounter = new HashMap<Module, Integer>();
			final List<Component_Type> extendedComps = new ArrayList<Component_Type>();
			for (final ComponentTypeBody ctb : comp.getComponentBody().getExtensions().getComponentBodies()) {
				if (!extendedComps.contains(ctb.getMyType())) {
					extendedComps.add(ctb.getMyType());
				}
			}

			for (int i=0; i<extendedComps.size(); i++) {
				final Component_Type ct = extendedComps.get(i);
				for (final ComponentTypeBody ctb : ct.getComponentBody().getExtensions().getComponentBodies()) {
					if (!extendedComps.contains(ctb.getMyType())) {
						extendedComps.add(ctb.getMyType());
					}
				}
			}

			for(final IProject project : projects) {
				final ProjectSourceParser projectSourceParser = GlobalParser.getProjectSourceParser(project);
				final List<Module> modules = filterByExcludedNames(new ArrayList<Module>(projectSourceParser.getModules()));
				for (final Module m : modules) {
					if (!m.equals(function.getModule())) {
						final ModuleVisitor vis = new ModuleVisitor(comp, extendedComps);
						m.accept(vis);
						compCounter.put(m, vis.getCounter());
					}
				}
			}

			final List<Entry<Module, Integer>> list = new ArrayList<Entry<Module, Integer>>(compCounter.entrySet());

			Collections.sort(list, new Comparator<Entry<Module, Integer>>() {
				@Override
				public int compare(final Entry<Module, Integer> m1, final Entry<Module, Integer> m2) {
					return (-1)*m1.getValue().compareTo(m2.getValue());
				}
			});
			destinationModule = list.get(0).getKey();
			final int max = list.get(0).getValue();
			if (max > 0) {
				function.addDestination(list.get(0).getKey(), 100, -1);
			}
			else {
				function.addDestination(list.get(0).getKey(), 0, -1);
			}
			for (int i=1; i<list.size(); i++) {
				if (!(list.get(i).getValue() == 0 & !selectedModules.contains(list.get(i).getKey()))) {
					if (max == 0) {
						function.addDestination(list.get(i).getKey(), 0, -1);
					} else if (list.get(i).getValue() == max) {
						function.addDestination(list.get(i).getKey(), 100, -1);
					} else {
						final double val = (double)list.get(i).getValue() / (double)max;
						function.addDestination(list.get(i).getKey(), (int)(val*100), -1);
					}
				}
			}
		}
	}

	public void addUnusedModules(final FunctionData fun, final List<Module> usedModules) {
		for (final Module m : selectedModules) {
			if (!usedModules.contains(m) && !m.equals(fun.getModule())) {
				fun.addDestination(m, 0, -1);
			}
		}
	}

	public RefactoringStatus getStatus() {
		return result;
	}


	private class SelectedModulesVisitor implements IResourceVisitor {

		private final List<Module> selectedModules = new ArrayList<Module>();

		public SelectedModulesVisitor() {
		}

		@Override
		public boolean visit(final IResource resource) throws CoreException {
			if (resource instanceof IFile) {
				final ProjectSourceParser sourceParser = GlobalParser.getProjectSourceParser(((IFile)resource).getProject());
				final Module module = sourceParser.containedModule((IFile)resource);
				if (module != null) {
					selectedModules.add(module);
				}
				//SKIP
				return false;
			}
			//CONTINUE
			return true;
		}

		public List<Module> getSelectedModules() {
			return selectedModules;
		}
	}

	private static class ModuleVisitor extends ASTVisitor {

		private int counter;
		private final Component_Type comp;
		private final List<Component_Type> components;

		public ModuleVisitor(final Component_Type comp, final List<Component_Type> components) {
			this.comp = comp;
			this.components = components;
			counter = 0;
		}

		@Override
		public int visit(final IVisitableNode node) {
			if (node instanceof Def_Function) {
				final Def_Function fun = (Def_Function)node;
				final Component_Type componentType = fun.getRunsOnType(CompilationTimeStamp.getBaseTimestamp());
				if (componentType !=null &&
						( componentType.equals(comp)
								|| components.contains(componentType))) {
					counter++;
				}
			}
			return V_CONTINUE;
		}

		public int getCounter() {
			return counter;
		}

	}


	private static class ReferenceVisitor extends ASTVisitor {

		private final NavigableSet<ILocateableNode> locations;

		ReferenceVisitor() {
			locations = new TreeSet<ILocateableNode>(new LocationComparator());
		}

		private NavigableSet<ILocateableNode> getLocations() {
			return locations;
		}

		@Override
		public int visit(final IVisitableNode node) {
			if (node instanceof Reference) {
				locations.add((Reference)node);
			}
			return V_CONTINUE;
		}


		private static class LocationComparator implements Comparator<ILocateableNode> {

			@Override
			public int compare(final ILocateableNode arg0, final ILocateableNode arg1) {
				final IResource f0 = arg0.getLocation().getFile();
				final IResource f1 = arg1.getLocation().getFile();
				if (!f0.equals(f1)) {
					return f0.getFullPath().toString().compareTo(f1.getFullPath().toString());
				}

				final int o0 = arg0.getLocation().getOffset();
				final int o1 = arg1.getLocation().getOffset();
				return (o0 < o1) ? -1 : ((o0 == o1) ? 0 : 1);
			}
		}
	}

	public Map<Module, List<FunctionData>> getDestinations() {
		return chooseDestination();
	}


	public static class MoveFunctionSettings {
		private MoveFunctionType type;
		private MoveFunctionMethod method;
		private Pattern excludedModuleNames = null;
		private boolean changed = true;

		public MoveFunctionType getType() {
			return type;
		}
		public void setType(final MoveFunctionType type) {
			this.type = type;
		}
		public MoveFunctionMethod getMethod() {
			return method;
		}

		public boolean isChanged() {
			return changed;
		}

		public void setMethod(final MoveFunctionMethod method) {
			this.method = method;
		}
		public Pattern getExcludedModuleNames() {
			return excludedModuleNames;
		}
		public void setExcludedModuleNames(final Pattern excludedModuleNames) {
			this.excludedModuleNames = excludedModuleNames;
		}

		public void setChanged(final boolean changed) {
			this.changed = changed;
		}
	}

	public MoveFunctionSettings getSettings() {
		return settings;
	}

}

enum MoveFunctionType {
	MODULE, COMPONENT
}

enum MoveFunctionMethod {
	LENGTH, IMPORTS, LENGTHANDIMPORTS, COMPONENT
}

