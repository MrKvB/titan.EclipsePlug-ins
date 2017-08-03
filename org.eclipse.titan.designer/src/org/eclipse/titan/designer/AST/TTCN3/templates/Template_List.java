/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.templates;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.TemplateRestriction;
import org.eclipse.titan.designer.AST.TTCN3.types.Array_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.SequenceOf_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.SetOf_Type;
import org.eclipse.titan.designer.AST.TTCN3.values.ArrayDimension;
import org.eclipse.titan.designer.AST.TTCN3.values.Integer_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.SequenceOf_Value;
import org.eclipse.titan.designer.AST.TTCN3.values.Values;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Represents a list of templates.
 *
 * @author Kristof Szabados
 * */
public final class Template_List extends CompositeTemplate {
	/** Indicates whether the embedded templates contain PERMUTATION_MATCH. */
	private boolean hasPermutation = false;

	// cache storing the value form of this list of templates if already
	// created, or null
	private SequenceOf_Value asValue = null;

	public Template_List(final ListOfTemplates templates) {
		super(templates);

		for (int i = 0, size = templates.getNofTemplates(); i < size; i++) {
			if (Template_type.PERMUTATION_MATCH.equals(templates.getTemplateByIndex(i).getTemplatetype())) {
				hasPermutation = true;
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public Template_type getTemplatetype() {
		return Template_type.TEMPLATE_LIST;
	}

	@Override
	/** {@inheritDoc} */
	public String getTemplateTypeName() {
		if (isErroneous) {
			return "erroneous value list notation";
		}

		return "value list notation";
	}

	@Override
	/** {@inheritDoc} */
	public String createStringRepresentation() {
		final StringBuilder builder = new StringBuilder();
		builder.append("{ ");
		for (int i = 0, size = templates.getNofTemplates(); i < size; i++) {
			if (i > 0) {
				builder.append(", ");
			}

			final ITTCN3Template template = templates.getTemplateByIndex(i);
			builder.append(template.createStringRepresentation());
		}
		builder.append(" }");

		if (lengthRestriction != null) {
			builder.append(lengthRestriction.createStringRepresentation());
		}
		if (isIfpresent) {
			builder.append("ifpresent");
		}

		return builder.toString();
	}

	public boolean hasAllFrom() {
		for (int i = 0, size = templates.getNofTemplates(); i < size; i++) {
			if (Template_type.ALLELEMENTSFROM.equals(templates.getTemplateByIndex(i).getTemplatetype())) {
				return true;
			}
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public TTCN3Template setTemplatetype(final CompilationTimeStamp timestamp, final Template_type newType) {
		switch (newType) {
		case NAMED_TEMPLATE_LIST:
			return Named_Template_List.convert(timestamp, this);
		default:
			return super.setTemplatetype(timestamp, newType);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected ITTCN3Template getReferencedArrayTemplate(final CompilationTimeStamp timestamp, final IValue arrayIndex,
			final IReferenceChain referenceChain) {
		IValue indexValue = arrayIndex.setLoweridToReference(timestamp);
		indexValue = indexValue.getValueRefdLast(timestamp, referenceChain);
		if (indexValue.getIsErroneous(timestamp)) {
			return null;
		}

		long index = 0;
		if (!indexValue.isUnfoldable(timestamp)) {
			if (Value_type.INTEGER_VALUE.equals(indexValue.getValuetype())) {
				index = ((Integer_Value) indexValue).getValue();
			} else {
				arrayIndex.getLocation().reportSemanticError("An integer value was expected as index");
				return null;
			}
		} else {
			return null;
		}

		final IType tempType = myGovernor.getTypeRefdLast(timestamp);
		if (tempType.getIsErroneous(timestamp)) {
			return null;
		}

		switch (tempType.getTypetype()) {
		case TYPE_SEQUENCE_OF: {
			if (index < 0) {
				final String message = MessageFormat
						.format("A non-negative integer value was expected instead of {0} for indexing a template of `sequence of'' type `{1}''",
								index, tempType.getTypename());
				arrayIndex.getLocation().reportSemanticError(message);
				return null;
			}

			final int nofElements = getNofTemplates();
			if (!(index < nofElements)) {
				final String message = MessageFormat
						.format("Index overflow in a template of `sequence of'' type `{0}'': the index is {1}, but the template has only {2} elements",
								tempType.getTypename(), index, nofElements);
				arrayIndex.getLocation().reportSemanticError(message		);
				return null;
			}
			break;
		}
		case TYPE_SET_OF: {
			if (index < 0) {
				final String message = MessageFormat
						.format("A non-negative integer value was expected instead of {0} for indexing a template of `set of'' type `{1}''",
								index, tempType.getTypename());
				arrayIndex.getLocation().reportSemanticError(message);
				return null;
			}

			final int nofElements = getNofTemplates();
			if (!(index < nofElements)) {
				final String message = MessageFormat
						.format("Index overflow in a template of `set of'' type `{0}'': the index is {1}, but the template has only {2} elements",
								tempType.getTypename(), index, nofElements);
				arrayIndex.getLocation().reportSemanticError(message);
				return null;
			}
			break;
		}
		case TYPE_ARRAY: {
			final ArrayDimension dimension = ((Array_Type) tempType).getDimension();
			dimension.checkIndex(timestamp, indexValue, Expected_Value_type.EXPECTED_DYNAMIC_VALUE);
			if (!dimension.getIsErroneous(timestamp)) {
				// re-base the index
				index -= dimension.getOffset();
				if (index < 0 || !(index < getNofTemplates()) ) {
					arrayIndex.getLocation().reportSemanticError(
							MessageFormat.format("The index value {0} is outside the array indexable range", index
									+ dimension.getOffset()));
					return null;
				}
			} else {
				return null;
			}
			break;
		}
		default:{
			final String message = MessageFormat.format("Invalid array element reference: type `{0}'' cannot be indexed",
					tempType.getTypename());
			arrayIndex.getLocation().reportSemanticError(message);
			return null;
		}
		}

		final ITTCN3Template returnValue = getTemplateByIndex((int) index);
		if (Template_type.TEMPLATE_NOTUSED.equals(returnValue.getTemplatetype())) {
			if (baseTemplate != null) {
				return baseTemplate.getTemplateReferencedLast(timestamp, referenceChain).getReferencedArrayTemplate(timestamp,
						indexValue, referenceChain);
			}

			return null;
		}

		return returnValue;
	}

	@Override
	/** {@inheritDoc} */
	public boolean isValue(final CompilationTimeStamp timestamp) {
		if (lengthRestriction != null || isIfpresent || getIsErroneous(timestamp)) {
			return false;
		}

		for (int i = 0, size = getNofTemplates(); i < size; i++) {
			if (!templates.getTemplateByIndex(i).isValue(timestamp)) {
				return false;
			}
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public IValue getValue() {
		if (asValue != null) {
			return asValue;
		}

		final Values values = new Values(false);
		for (int i = 0, size = getNofTemplates(); i < size; i++) {
			values.addValue(templates.getTemplateByIndex(i).getValue());
		}

		asValue = new SequenceOf_Value(values);
		asValue.setLocation(getLocation());
		asValue.setMyScope(getMyScope());
		asValue.setFullNameParent(getNameParent());

		return asValue;
	}

	@Override
	/** {@inheritDoc} */
	public void checkSpecificValue(final CompilationTimeStamp timestamp, final boolean allowOmit) {
		for (int i = 0, size = templates.getNofTemplates(); i < size; i++) {
			templates.getTemplateByIndex(i).checkSpecificValue(timestamp, true);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected void checkTemplateSpecificLengthRestriction(final CompilationTimeStamp timestamp, final Type_type typeType) {
		if (Type_type.TYPE_SEQUENCE_OF.equals(typeType) || Type_type.TYPE_SET_OF.equals(typeType)) {
			final int nofTemplatesGood = getNofTemplatesNotAnyornone(timestamp); //at least !

			final boolean hasAnyOrNone = templateContainsAnyornone();

			lengthRestriction.checkNofElements(timestamp, nofTemplatesGood, hasAnyOrNone, false, hasAnyOrNone, this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkValueomitRestriction(final CompilationTimeStamp timestamp, final String definitionName, final boolean omitAllowed, final Location usageLocation) {
		if (omitAllowed) {
			checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_OMIT, usageLocation);
		} else {
			checkRestrictionCommon(timestamp, definitionName, TemplateRestriction.Restriction_type.TR_VALUE, usageLocation);
		}

		boolean needsRuntimeCheck = false;
		for (int i = 0, size = templates.getNofTemplates(); i < size; i++) {
			if (templates.getTemplateByIndex(i).checkValueomitRestriction(timestamp, definitionName, true, usageLocation)) {
				needsRuntimeCheck = true;
			}
		}
		return needsRuntimeCheck;
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (lengthRestriction != null) {
			lengthRestriction.findReferences(referenceFinder, foundIdentifiers);
		}

		if (asValue != null) {
			asValue.findReferences(referenceFinder, foundIdentifiers);
		} else if (templates != null) {
			templates.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}
		if (asValue != null) {
			if (!asValue.accept(v)) {
				return false;
			}
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void setGenNamePrefix(final String prefix) {
		super.setGenNamePrefix(prefix);
		for (int i = 0; i < templates.getNofTemplates(); i++) {
			templates.getTemplateByIndex(i).setGenNamePrefix(prefix);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setGenNameRecursive(final String parameterGenName) {
		super.setGenNameRecursive(parameterGenName);

		if(myGovernor == null) {
			return;
		}

		IType type = myGovernor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
		long offset = 0;
		if(Type_type.TYPE_ARRAY.equals(type.getTypetype())) {
			offset = ((Array_Type) type).getDimension().getOffset();
		}
		for (int i = 0; i < templates.getNofTemplates(); i++) {
			StringBuilder embeddedName = new StringBuilder(parameterGenName);
			embeddedName.append('[').append(offset + i).append(']');
			templates.getTemplateByIndex(i).getTemplate().setGenNameRecursive(embeddedName.toString());
		}
	}

	@Override
	protected String getNameForStringRep() {
		return "";
	}

	/**
	 * Returns whether the template can be represented by an in-line
	 *  Java expression.
	 * */
	public boolean hasSingleExpression() {
		if (lengthRestriction != null || isIfpresent /* TODO:  || get_needs_conversion()*/) {
			return false;
		}
		//TODO fatal error
		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeExpression(JavaGenData aData, ExpressionStruct expression) {
		if (asValue != null) {
			asValue.generateCodeExpression(aData, expression);
			return;
		}

		if (myGovernor == null) {
			return;
		}

		// TODO not yet implemented
		super.generateCodeExpression(aData, expression);
	}

	@Override
	public void generateCodeInit(JavaGenData aData, StringBuilder source, String name) {
		if (asValue != null) {
			asValue.generateCodeInit(aData, source, name);
			return;
		}

		if (myGovernor == null) {
			return;
		}

		// TODO special case for empty list
		IType typeLast = myGovernor.getTypeRefdLast(CompilationTimeStamp.getBaseTimestamp());
		long indexOffset = 0;
		if (typeLast.getTypetype().equals(Type_type.TYPE_ARRAY)) {
			indexOffset = ((Array_Type) typeLast).getDimension().getOffset();
		}

		String ofTypeName;
		switch(typeLast.getTypetype()) {
		case TYPE_SEQUENCE_OF:
			ofTypeName = ((SequenceOf_Type) typeLast).getOfType().getGenNameTemplate(aData, source, myScope);
			break;
		case TYPE_SET_OF:
			ofTypeName = ((SetOf_Type) typeLast).getOfType().getGenNameTemplate(aData, source, myScope);
			break;
		case TYPE_ARRAY:
			ofTypeName = ((Array_Type) typeLast).getElementType().getGenNameTemplate(aData, source, myScope);
			break;
		default:
			//FATAL error
			return;
		}

		if (hasPermutation) {
			//FIXME implement
		} else if (hasAllFrom()) {
			//FIXME implement
		} else {
			source.append(MessageFormat.format("{0}.setSize({1});\n", name, getNofTemplates()));

			int index = 0;
			for (int i = 0, size = templates.getNofTemplates(); i < size; i++) {
				ITemplateListItem template = templates.getTemplateByIndex(i);
				switch (template.getTemplatetype()) {
				case PERMUTATION_MATCH:
					//FIXME implement
					break;
				case ALLELEMENTSFROM:
				case TEMPLATE_NOTUSED:
					index++;
					break;
				default:
					//TODO use generate_code_init_seof_element
					String embeddedName = MessageFormat.format("{0}.getAt({1})", name, index);
					((TemplateBody) template).generateCodeInit(aData, source, embeddedName);
					index++;
					break;
				}
			}
			//FIXME implement
		}
		// FIXME implement
		//TODO handle the case when we know everything in compilation time
		// TODO not yet implemented
		super.generateCodeInit(aData, source, name);
	}
}
