/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST;

import java.util.List;

import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameterList;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * The ArraySubReference class represents a part of a TTCN3 or ASN.1 reference, which was given in array notation ('[index]').
 * <p>
 * This is the only sub-reference type which does not have an identifier associated to it.
 *
 * @author Kristof Szabados
 * */
public final class ArraySubReference extends ASTNode implements ISubReference, ILocateableNode {
	public static final String INVALIDSUBREFERENCE = "Type `{0}'' can not be indexed";
	public static final String INVALIDVALUESUBREFERENCE = "Invalid array element reference: type `{0}'' can not be indexed";
	public static final String INVALIDSTRINGELEMENTINDEX = "A string element cannot be indexed";
	public static final String INTEGERINDEXEXPECTED = "An integer value was expected as index";
	public static final String NATIVEINTEGEREXPECTED = "Using a large integer value ({0}) as index is not supported";

	private static final String ARRAYSUBFULLNAME = ".<array_index>";
	private static final Identifier ID = new Identifier(Identifier.Identifier_type.ID_NAME, "");

	private final Value value;

	private Location location;

	public ArraySubReference(final Value value) {
		this.value = value;
		if (null != value) {
			value.setFullNameParent(this);
		}
	}

	public Value getValue() {
		return value;
	}

	@Override
	/** {@inheritDoc} */
	public Subreference_type getReferenceType() {
		return Subreference_type.arraySubReference;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (child == value) {
			return builder.append(ARRAYSUBFULLNAME);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (null != value) {
			value.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void setCodeSection(final CodeSectionType codeSection) {
		if (value != null) {
			value.setCodeSection(codeSection);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Identifier getId() {
		return ID;
	}

	@Override
	/** {@inheritDoc} */
	public void setLocation(final Location location) {
		this.location = location;
	}

	@Override
	/** {@inheritDoc} */
	public Location getLocation() {
		return location;
	}

	@Override
	/** {@inheritDoc} */
	public String toString() {
		return "arraySubReference";
	}

	@Override
	/** {@inheritDoc} */
	public void appendDisplayName(final StringBuilder builder) {
		builder.append('[');
		if (null != value) {
			builder.append(value.createStringRepresentation());
		}
		builder.append(']');
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		if (null != value) {
			value.updateSyntax(reparser, false);
			reparser.updateLocation(value.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (value == null) {
			return;
		}

		value.findReferences(referenceFinder, foundIdentifiers);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (value != null) {
			if (!value.accept(v)) {
				return false;
			}
		}
		return true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean hasSingleExpression(final FormalParameterList formalParameterList) {
		if (value != null) {
			if (!value.canGenerateSingleExpression()) {
				return false;
			}
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final ExpressionStruct expression, final boolean isFirst ) {
		expression.expression.append( "\t" );
		expression.expression.append( "//TODO: " );
		expression.expression.append( getClass().getSimpleName() );
		expression.expression.append( ".generateCode() is not implemented!\n" );
	}
}
