/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.editors.configeditor.pages.compgroupmc;

import org.antlr.v4.runtime.tree.ParseTree;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.titan.common.parsers.cfg.ConfigTreeNodeUtilities;
import org.eclipse.titan.common.parsers.cfg.indices.ComponentSectionHandler;
import org.eclipse.titan.common.parsers.cfg.indices.ComponentSectionHandler.Component;
import org.eclipse.titan.designer.editors.configeditor.ConfigEditor;

/**
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class ComponentSectionDropTargetListener implements DropTargetListener {

	private final TableViewer viewer;
	private final ConfigEditor editor;

	public ComponentSectionDropTargetListener(final TableViewer viewer, final ConfigEditor editor) {
		this.viewer = viewer;
		this.editor = editor;
	}

	@Override
	public void dragEnter(final DropTargetEvent event) {
		if (event.detail == DND.DROP_DEFAULT) {
			if ((event.operations & (DND.DROP_MOVE | DND.DROP_COPY)) == 0) {
				event.detail = DND.DROP_NONE;
			}
		}
	}

	@Override
	public void dragLeave(final DropTargetEvent event) {
		//Do nothing
	}

	@Override
	public void dragOperationChanged(final DropTargetEvent event) {
		if (event.detail == DND.DROP_DEFAULT) {
			if ((event.operations & (DND.DROP_MOVE | DND.DROP_COPY)) == 0) {
				event.detail = DND.DROP_NONE;
			}
		}
	}

	@Override
	public void dragOver(final DropTargetEvent event) {
		if (event.item == null) {
			event.feedback = DND.FEEDBACK_SCROLL;
			event.detail = DND.DROP_NONE;
		} else {
			if (event.item.getData() instanceof Component) {
				event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL | DND.FEEDBACK_SELECT;
			} else {
				event.feedback = DND.FEEDBACK_INSERT_BEFORE | DND.FEEDBACK_SCROLL;
			}
			if (event.detail == DND.DROP_NONE) {
				if ((event.operations & DND.DROP_MOVE) != 0) {
					event.detail = DND.DROP_MOVE;
				} else if ((event.operations & DND.DROP_COPY) != 0) {
					event.detail = DND.DROP_COPY;
				}
			}
		}
	}

	@Override
	public void drop(final DropTargetEvent event) {
		if (ComponentItemTransfer.getInstance().isSupportedType(event.currentDataType)) {
			if (event.item != null && viewer.getInput() != null) {
				final ComponentSectionHandler componentSectionHandler = (ComponentSectionHandler) viewer.getInput();
				final Component element = (Component) event.item.getData();
				final Component[] items = (Component[]) event.data;

				int baseindex = componentSectionHandler.getComponents().indexOf(element);

				final ParseTree parent = componentSectionHandler.getLastSectionRoot();
				ConfigTreeNodeUtilities.removeChild(parent, element.getRoot());
				ConfigTreeNodeUtilities.addChild(parent, element.getRoot(), baseindex);
				if (items.length > 0) {
					for (int i = 0; i < items.length - 1; i++) {
						componentSectionHandler.getComponents().add(++baseindex, items[i]);
					}

					componentSectionHandler.getComponents().add(++baseindex, items[items.length - 1]);
				}

				viewer.refresh(true);
				editor.setDirty();
			}
		}
	}

	@Override
	public void dropAccept(final DropTargetEvent event) {
		//Do nothing
	}

}
