/******************************************************************************
 * Copyright (c) 2000-2020 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.ASN1;

import java.text.MessageFormat;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Error_Setting;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISetting;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.ASN1.Object.ASN1Objects;
import org.eclipse.titan.designer.AST.ASN1.Object.FieldName;
import org.eclipse.titan.designer.AST.ASN1.Object.FieldSpecification;
import org.eclipse.titan.designer.AST.ASN1.Object.FieldSpecification.Fieldspecification_types;
import org.eclipse.titan.designer.AST.ASN1.Object.FixedTypeValue_FieldSpecification;
import org.eclipse.titan.designer.AST.ASN1.Object.ObjectClass_Definition;
import org.eclipse.titan.designer.AST.ASN1.Object.ObjectSetElementVisitor_objectCollector;
import org.eclipse.titan.designer.AST.ASN1.Object.ObjectSet_FieldSpecification;
import org.eclipse.titan.designer.AST.ASN1.Object.ObjectSet_definition;
import org.eclipse.titan.designer.AST.ASN1.Object.Object_Definition;
import org.eclipse.titan.designer.AST.ASN1.Object.Object_FieldSpecification;
import org.eclipse.titan.designer.AST.ASN1.types.ObjectClassField_Type;
import org.eclipse.titan.designer.AST.ASN1.types.Open_Type;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;

/**
 * Class to represent InformationFromObjects.
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class InformationFromObj extends Reference {
	private static final String FULLNAMEPART = ".<fieldnames>";

	public static final String INVALIDREFERENCE = "Invalid reference `{0}'' (ObjectClass, ObjectSet or Object reference was expected)";
	private static final String UNSUPPORTEDCONSTRUCT = "Sorry, this construct is not supported";
	private static final String VALUESETFROMOBJECTS_NOT_SUPPORTED = "Sorry, ValueSetFromObjects not supported";
	private static final String INVALIDNOTATION1 = "This notation is not permitted" + " (object or objectset fieldreference was expected)";
	private static final String INVALIDNOTATION2 = "This notation is not permitted"
			+ " (type, (fixed- or variabletype) value or valueset fieldreference was expected)";
	private static final String INVALIDNOTATION3 = "This notation is not permitted"
			+ " (object, objectset, (fixed type) value or valueset fieldreference was expected)";


	/** ObjectClass, Object or ObjectSet. */
	private final Defined_Reference reference;

	private final FieldName fieldName;

	private enum SettingDetectionState {
		ObjectClass, ObjectSet, Object
	}

	/** the time when this setting was check the last time. */
	private CompilationTimeStamp lastTimeChecked;

	/**
	 * Cache storing the last setting referred to.
	 * Can only be reused in the same semantic check cycle.
	 * */
	private ISetting refdSetting = null;

	private Location location;

	public InformationFromObj(final Defined_Reference reference, final FieldName fieldName) {
		super(null);
		this.reference = reference;
		this.fieldName = fieldName;
		setIsErroneous(false);

		if (null != reference) {
			reference.setFullNameParent(this);
		}
		if (null != fieldName) {
			fieldName.setFullNameParent(this);
		}
	}

	public InformationFromObj newInstance() {
		return new InformationFromObj(reference, fieldName.newInstance());
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

	public FieldName getFieldName(){
		return fieldName;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (fieldName == child) {
			return builder.append(FULLNAMEPART);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		reference.setMyScope(scope);
	}

	@Override
	/** {@inheritDoc} */
	public Identifier getId() {
		return reference.getId();
	}

	@Override
	/** {@inheritDoc} */
	public String getDisplayName() {
		return reference.getDisplayName() + fieldName.getDisplayName();
	}

	@Override
	/** {@inheritDoc} */
	public ISetting getRefdSetting(final CompilationTimeStamp timestamp) {
		if (null != lastTimeChecked && !lastTimeChecked.isLess(timestamp)) {
			return refdSetting;
		}

		lastTimeChecked = timestamp;
		refdSetting = null;
		setIsErroneous(false);
		ISetting temporalSetting = reference.getRefdSetting(timestamp);

		if (null == temporalSetting) {
			setIsErroneous(true);
			refdSetting = new Error_Setting();
			return refdSetting;
		}

		SettingDetectionState currentState;

		ObjectClass_Definition objectClass = null;
		Object_Definition object = new Object_Definition(null);
		ObjectSet_definition fromObjectSet = newObjectSetDefinitionInstance();

		/* the first part */
		switch (temporalSetting.getSettingtype()) {
		case S_OS: {
			currentState = SettingDetectionState.ObjectSet;
			final ObjectSet_definition objectSet = ((ObjectSet) temporalSetting).getRefdLast(timestamp, null);
			objectClass = objectSet.getMyGovernor().getRefdLast(timestamp, null);
			final ObjectSetElementVisitor_objectCollector objectCollector = new ObjectSetElementVisitor_objectCollector(
					objectSet.getLocation(), objectClass, timestamp);
			objectCollector.visitObjectSet(objectSet, false);
			fromObjectSet = newObjectSetDefinitionInstance(objectCollector.getObjects());
			fromObjectSet.setMyGovernor(objectClass);
			break;
		}
		case S_O:
			currentState = SettingDetectionState.Object;
			object = ((ASN1Object) temporalSetting).getRefdLast(timestamp, null);
			objectClass = object.getMyGovernor().getRefdLast(timestamp, null);
			break;
		case S_OC:
			currentState = SettingDetectionState.ObjectClass;
			objectClass = ((ObjectClass) temporalSetting).getRefdLast(timestamp, null);
			break;
		case S_ERROR:
			setIsErroneous(true);
			refdSetting = new Error_Setting();
			return refdSetting;
		default:
			location.reportSemanticError(MessageFormat.format(INVALIDREFERENCE, getDisplayName()));
			setIsErroneous(true);
			refdSetting = new Error_Setting();
			return refdSetting;
		}

		final int nofFields = fieldName.getNofFields();

		/* the middle part */
		Identifier currentFieldName;
		FieldSpecification currentFieldSpecification;
		for (int i = 0; i < nofFields - 1; i++) {
			currentFieldName = fieldName.getFieldByIndex(i);
			currentFieldSpecification = objectClass.getFieldSpecifications().getFieldSpecificationByIdentifier(currentFieldName)
					.getLast();

			if (Fieldspecification_types.FS_ERROR.equals(currentFieldSpecification.getFieldSpecificationType())) {
				setIsErroneous(true);
				refdSetting = new Error_Setting();
				return refdSetting;
			}

			switch (currentState) {
			case ObjectClass:
				switch (currentFieldSpecification.getFieldSpecificationType()) {
				case FS_O: {
					final Object_FieldSpecification temporalFieldspec = (Object_FieldSpecification) currentFieldSpecification;
					objectClass = temporalFieldspec.getObjectClass().getRefdLast(timestamp, null);
				}
				break;
				case FS_OS: {
					final ObjectSet_FieldSpecification temporalFieldspec = (ObjectSet_FieldSpecification) currentFieldSpecification;
					objectClass = temporalFieldspec.getObjectClass().getRefdLast(timestamp, null);
				}
				break;
				case FS_ERROR:
					setIsErroneous(true);
					refdSetting = new Error_Setting();
					return refdSetting;
				default:
					location.reportSemanticError(INVALIDNOTATION1);
					setIsErroneous(true);
					refdSetting = new Error_Setting();
					return refdSetting;
				}
				break;
			case ObjectSet:
				switch (currentFieldSpecification.getFieldSpecificationType()) {
				case FS_O: {
					final Object_FieldSpecification temporalFieldspec = (Object_FieldSpecification) currentFieldSpecification;
					objectClass = temporalFieldspec.getObjectClass().getRefdLast(timestamp, null);
					final ObjectSetElementVisitor_objectCollector objectCollector = new ObjectSetElementVisitor_objectCollector(
							location, objectClass, timestamp);

					final ASN1Objects temporalObjects = fromObjectSet.getObjs();
					temporalObjects.trimToSize();
					for (int j = 0; j < temporalObjects.getNofObjects(); j++) {
						object = temporalObjects.getObjectByIndex(j).getRefdLast(timestamp, null);
						if (!object.hasFieldSettingWithNameDefault(currentFieldName)) {
							continue;
						}

						temporalSetting = object.getSettingByNameDefault(currentFieldName);
						object = ((Object_Definition) temporalSetting).getRefdLast(timestamp, null);
						objectCollector.visitObject(object);
					}

					fromObjectSet = newObjectSetDefinitionInstance(objectCollector.getObjects());
					fromObjectSet.setLocation(location);
					fromObjectSet.setMyGovernor(objectClass);
				}
				break;
				case FS_OS: {
					final ObjectSet_FieldSpecification temporalFieldspec = (ObjectSet_FieldSpecification) currentFieldSpecification;
					objectClass = temporalFieldspec.getObjectClass().getRefdLast(timestamp, null);
					final ObjectSetElementVisitor_objectCollector objectCollector = new ObjectSetElementVisitor_objectCollector(
							location, objectClass, timestamp);
					final ASN1Objects temporalObjects = fromObjectSet.getObjs();

					for (int j = 0; j < temporalObjects.getNofObjects(); j++) {
						object = temporalObjects.getObjectByIndex(j).getRefdLast(timestamp, null);

						if (!object.hasFieldSettingWithNameDefault(currentFieldName)) {
							continue;
						}

						temporalSetting = object.getSettingByNameDefault(currentFieldName);
						final ObjectSet_definition objectSet = ((ObjectSet_definition) temporalSetting).getRefdLast(timestamp, null);
						objectCollector.visitObjectSet(objectSet, false);
					}

					fromObjectSet = newObjectSetDefinitionInstance(objectCollector.getObjects());
					fromObjectSet.setLocation(location);
					fromObjectSet.setMyGovernor(objectClass);
				}
				break;
				case FS_ERROR:
					setIsErroneous(true);
					refdSetting = new Error_Setting();
					return refdSetting;
				default:
					location.reportSemanticError(INVALIDNOTATION1);
					setIsErroneous(true);
					refdSetting = new Error_Setting();
					return refdSetting;
				}
				break;
			case Object:
				switch (currentFieldSpecification.getFieldSpecificationType()) {
				case FS_O: {
					final Object_FieldSpecification temporalFieldspec = (Object_FieldSpecification) currentFieldSpecification;
					objectClass = temporalFieldspec.getObjectClass().getRefdLast(timestamp, null);
					temporalSetting = object.getSettingByNameDefault(currentFieldName);
					object = ((Object_Definition) temporalSetting).getRefdLast(timestamp, null);
				}
				break;
				case FS_OS: {
					currentState = SettingDetectionState.ObjectSet;
					final ObjectSet_FieldSpecification temporalFieldspec = (ObjectSet_FieldSpecification) currentFieldSpecification;
					objectClass = temporalFieldspec.getObjectClass().getRefdLast(timestamp, null);
					final ObjectSetElementVisitor_objectCollector objectCollector = new ObjectSetElementVisitor_objectCollector(
							fromObjectSet, timestamp);
					final ASN1Objects temporalObjects = fromObjectSet.getObjs();

					for (int j = 0; j < temporalObjects.getNofObjects(); j++) {
						object = temporalObjects.getObjectByIndex(j).getRefdLast(timestamp, null);
						if (!object.hasFieldSettingWithNameDefault(currentFieldName)) {
							continue;
						}

						temporalSetting = object.getSettingByNameDefault(currentFieldName);
						final ObjectSet_definition objectSet = ((ObjectSet_definition) temporalSetting).getRefdLast(timestamp, null);
						objectCollector.visitObjectSet(objectSet, false);
					}

					fromObjectSet = newObjectSetDefinitionInstance(objectCollector.getObjects());
					fromObjectSet.setLocation(location);
					fromObjectSet.setMyGovernor(objectClass);
				}
				break;
				case FS_ERROR:
					setIsErroneous(true);
					refdSetting = new Error_Setting();
					return refdSetting;
				default:
					location.reportSemanticError(INVALIDNOTATION1);
					setIsErroneous(true);
					refdSetting = new Error_Setting();
					return refdSetting;
				}
				break;
			default:
				// if this could happen it would be FATAL ERROR
				break;
			}
		}

		/* and the last part... */
		currentFieldName = fieldName.getFieldByIndex(nofFields - 1);
		currentFieldSpecification = objectClass.getFieldSpecifications().getFieldSpecificationByIdentifier(currentFieldName).getLast();
		temporalSetting = null;

		switch (currentState) {
		case ObjectClass:
			switch (currentFieldSpecification.getFieldSpecificationType()) {
			case FS_T: {
				final Open_Type type = new Open_Type(objectClass, currentFieldName);
				type.setLocation(location);
				type.setMyScope(myScope);
				temporalSetting = type;
			}
			break;
			case FS_V_FT: {
				final FixedTypeValue_FieldSpecification temporalFielspecification = (FixedTypeValue_FieldSpecification) currentFieldSpecification;
				final ObjectClassField_Type type = new ObjectClassField_Type(temporalFielspecification.getType(), objectClass,
						currentFieldName);
				type.setLocation(location);
				type.setMyScope(myScope);
				temporalSetting = type;
			}
			break;
			case FS_V_VT:
			case FS_VS_FT:
			case FS_VS_VT:
				location.reportSemanticError(UNSUPPORTEDCONSTRUCT);
				setIsErroneous(true);
				break;
			case FS_O:
			case FS_OS:
				location.reportSemanticError(INVALIDNOTATION2);
				setIsErroneous(true);
				break;
			default:
				setIsErroneous(true);
				break;
			}
			break;
		case ObjectSet:
			switch (currentFieldSpecification.getFieldSpecificationType()) {
			case FS_O: {
				final Object_FieldSpecification temporalFieldspec = (Object_FieldSpecification) currentFieldSpecification;
				objectClass = temporalFieldspec.getObjectClass().getRefdLast(timestamp, null);
				final ObjectSetElementVisitor_objectCollector objectCollector = new ObjectSetElementVisitor_objectCollector(location,
						objectClass, timestamp);
				final ASN1Objects temporalObjects = fromObjectSet.getObjs();
				for (int j = 0; j < temporalObjects.getNofObjects(); j++) {
					object = temporalObjects.getObjectByIndex(j).getRefdLast(timestamp, null);
					if (!object.hasFieldSettingWithNameDefault(currentFieldName)) {
						continue;
					}

					temporalSetting = object.getSettingByNameDefault(currentFieldName);
					object = ((Object_Definition) temporalSetting).getRefdLast(timestamp, null);
					objectCollector.visitObject(object);
				}

				fromObjectSet = newObjectSetDefinitionInstance(objectCollector.getObjects());
				fromObjectSet.setLocation(location);
				fromObjectSet.setMyGovernor(objectClass);
				fromObjectSet.setMyScope(myScope);
				temporalSetting = fromObjectSet;
			}
			break;
			case FS_OS: {
				final ObjectSet_FieldSpecification temporalFieldspec = (ObjectSet_FieldSpecification) currentFieldSpecification;
				objectClass = temporalFieldspec.getObjectClass().getRefdLast(timestamp, null);
				final ObjectSetElementVisitor_objectCollector objectCollector = new ObjectSetElementVisitor_objectCollector(location,
						objectClass, timestamp);
				final ASN1Objects temporalObjects = fromObjectSet.getObjs();

				for (int j = 0; j < temporalObjects.getNofObjects(); j++) {
					object = temporalObjects.getObjectByIndex(j).getRefdLast(timestamp, null);

					if (!object.hasFieldSettingWithNameDefault(currentFieldName)) {
						continue;
					}

					temporalSetting = object.getSettingByNameDefault(currentFieldName);
					final ObjectSet_definition objectSet = ((ObjectSet_definition) temporalSetting).getRefdLast(timestamp, null);
					objectCollector.visitObjectSet(objectSet, false);
				}

				fromObjectSet = newObjectSetDefinitionInstance(objectCollector.getObjects());
				fromObjectSet.setLocation(location);
				fromObjectSet.setMyGovernor(objectClass);
				fromObjectSet.setMyScope(myScope);
				temporalSetting = fromObjectSet;
			}
			break;
			case FS_V_FT:
			case FS_VS_FT:
				location.reportSemanticError(VALUESETFROMOBJECTS_NOT_SUPPORTED);
				setIsErroneous(true);
				break;
			case FS_ERROR:
				setIsErroneous(true);
				break;
			default:
				location.reportSemanticError(INVALIDNOTATION3);
				setIsErroneous(true);
				break;
			}
			break;
		case Object:
			temporalSetting = object.getSettingByNameDefault(currentFieldName);
			break;
		default:
			setIsErroneous(true);
			break;
		}

		refdSetting = temporalSetting;
		return refdSetting;
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getRefdAssignment(final CompilationTimeStamp timestamp, final boolean checkParameterList, final IReferenceChain refChain) {
		return reference.getRefdAssignment(timestamp, true, refChain);
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}
		if (reference != null && !reference.accept(v)) {
			return false;
		}
		if (fieldName != null && !fieldName.accept(v)) {
			return false;
		}
		return true;
	}

	/**
	 * @return ObjectSet_definition
	 */
	private ObjectSet_definition newObjectSetDefinitionInstance() {
		return new ObjectSet_definition();
	}

	/**
	 * @return ObjectSet_definition
	 */
	private ObjectSet_definition newObjectSetDefinitionInstance(final ASN1Objects aObjects ) {
		return new ObjectSet_definition( aObjects );
	}
}
