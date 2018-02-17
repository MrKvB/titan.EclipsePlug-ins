/******************************************************************************
 * Copyright (c) 2000-2017 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.runtime.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.titan.runtime.core.TitanCharString.CharCoding;

/**
 * TTCN-3 Universal_charstring
 * @author Arpad Lovassy
 * @author Farkas Izabella Ingrid
 * @author Andrea Palfi
 */
public class TitanUniversalCharString extends Base_Type {

	/** Internal data */
	List<TitanUniversalChar> val_ptr;

	/** Character string values are stored in an optimal way */
	StringBuilder cstr;

	/**
	 * true, if cstr is used <br>
	 * false, if val_ptr is used
	 */
	boolean charstring;

	public TitanUniversalCharString() {
		super();
	}

	public TitanUniversalCharString(final TitanUniversalChar aOtherValue) {
		if (!aOtherValue.is_char()) {
			val_ptr = new ArrayList<TitanUniversalChar>();
			val_ptr.add(aOtherValue);
			charstring = false;
		} else {
			cstr = new StringBuilder();
			cstr.append(aOtherValue.getUc_cell());
			charstring = true;
		}
	}

	public TitanUniversalCharString(final char uc_group, final char uc_plane, final char uc_row,  final char uc_cell) {
		final TitanUniversalChar uc = new TitanUniversalChar(uc_group, uc_plane, uc_row, uc_cell);
		if (uc.is_char()) {
			cstr = new StringBuilder();
			cstr.append(uc_cell);
			charstring = true;
		} else {
			val_ptr = new ArrayList<TitanUniversalChar>();
			val_ptr.add(uc);
			charstring = false;
		}
	}

	public TitanUniversalCharString( final List<TitanUniversalChar> aOtherValue ) {
		val_ptr = copyList( aOtherValue );
		charstring = false;
	}

	public TitanUniversalCharString(final TitanUniversalChar[] aOther) {
		val_ptr = new ArrayList<TitanUniversalChar>();
		for(int i = 0; i < aOther.length; i++) {
			val_ptr.add( aOther[i] );
		}

		charstring = false;
	}

	public TitanUniversalCharString(final String aOtherValue) {
		cstr = new StringBuilder(aOtherValue);
		charstring = true;
	}

	public TitanUniversalCharString(final StringBuilder aOtherValue) {
		cstr = new StringBuilder(aOtherValue);
		charstring = true;
	}

	public TitanUniversalCharString(final TitanCharString aOtherValue) {
		aOtherValue.mustBound("Copying an unbound charstring value.");

		cstr = new StringBuilder(aOtherValue.getValue());
		charstring = true;
	}

	public TitanUniversalCharString(final TitanUniversalCharString aOtherValue) {
		aOtherValue.mustBound("Copying an unbound universal charstring value.");

		charstring = aOtherValue.charstring;
		if (charstring) {
			cstr = new StringBuilder(aOtherValue.cstr);
		} else {
			val_ptr = copyList(aOtherValue.val_ptr);
		}
	}

	public TitanUniversalCharString(final TitanUniversalCharString_Element aOtherValue) {
		aOtherValue.mustBound("Initialization of a universal charstring with an unbound universal charstring element.");

		if (aOtherValue.get_char().is_char()) {
			cstr = new StringBuilder();
			cstr.append(aOtherValue.get_char().getUc_cell());
			charstring = true;
		} else {
			val_ptr = new ArrayList<TitanUniversalChar>();
			val_ptr.add(aOtherValue.get_char());
			charstring = false;
		}
	}

	private static List<TitanUniversalChar> copyList(final List<TitanUniversalChar> uList) {
		if (uList == null) {
			return null;
		}

		final List<TitanUniversalChar> clonedList = new ArrayList<TitanUniversalChar>(uList.size());
		for (final TitanUniversalChar uc : uList) {
			clonedList.add(new TitanUniversalChar(uc));
		}

		return clonedList;
	}

	// originally operator universal_char*
	public List<TitanUniversalChar> getValue() {
		mustBound("Casting an unbound universal charstring value to const universal_char*.");

		if (charstring) {
			convertCstrToUni();
		}

		return val_ptr;
	}

	// takes ownership of aOtherValue
	public void setValue(final List<TitanUniversalChar> aOtherValue) {
		if (aOtherValue != null) {
			val_ptr = aOtherValue;
			cstr = null;
			charstring = false;
		}
	}

	// originally operator=
	public TitanUniversalCharString assign(final TitanUniversalCharString aOtherValue) {
		aOtherValue.mustBound("Assignment of an unbound universal charstring value.");

		if (aOtherValue != this) {
			val_ptr = aOtherValue.val_ptr;
			cstr = aOtherValue.cstr;
			charstring = aOtherValue.charstring;
		}

		return this;
	}

	// originally operator=
	public TitanUniversalCharString assign(final TitanUniversalCharString_Element aOtherValue) {
		aOtherValue.mustBound("Assignment of an unbound universal charstring element to a universal charstring.");

		if (aOtherValue.get_char().is_char()) {
			cstr = new StringBuilder();
			cstr.append(aOtherValue.get_char().getUc_cell());
			val_ptr = null;
			charstring = true;
		} else {
			val_ptr = new ArrayList<TitanUniversalChar>();
			val_ptr.add(aOtherValue.get_char());
			cstr = null;
			charstring = false;
		}

		return this;
	}

	// originally operator=
	public TitanUniversalCharString assign(final TitanCharString aOtherValue) {
		aOtherValue.mustBound("Assignment of an unbound charstring value.");

		if (!charstring) {
			cleanUp();
			charstring = true;
		}
		cstr = new StringBuilder(aOtherValue.getValue());

		return this;
	}

	// originally operator=
	public TitanUniversalCharString assign(final TitanCharString_Element aOtherValue) {
		aOtherValue.mustBound("Assignment of an unbound charstring element to a charstring.");

		if (!charstring) {
			cleanUp();
			charstring = true;
		}
		cstr = new StringBuilder();
		cstr.append(aOtherValue.get_char());

		return this;
	}

	// originally operator=
	@Override
	public TitanUniversalCharString assign(final Base_Type otherValue) {
		if (otherValue instanceof TitanUniversalCharString) {
			return assign((TitanUniversalCharString) otherValue);
		} else if (otherValue instanceof TitanCharString) {
			return assign((TitanCharString) otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to universal charstring", otherValue));
	}

	// originally operator=
	public TitanUniversalCharString assign(final TitanUniversalChar aOtherValue) {
		cleanUp();
		if (aOtherValue.is_char()) {
			charstring = true;
			cstr = new StringBuilder();
			cstr.append(aOtherValue.getUc_cell());
		} else {
			charstring = false;
			val_ptr = new ArrayList<TitanUniversalChar>();
			val_ptr.add(aOtherValue);
		}

		return this;
	}

	// originally operator=
	public TitanUniversalCharString assign(final char[] aOtherValue) {
		charstring = true;
		cstr = new StringBuilder();
		for (int i = 0; i < aOtherValue.length; ++i) {
			cstr.append(aOtherValue[i]);
		}

		return this;
	}

	// originally operator=
	public TitanUniversalCharString assign(final String aOtherValue) {
		charstring = true;
		cstr = new StringBuilder();
		for (int i = 0; i < aOtherValue.length(); ++i) {
			cstr.append(aOtherValue.charAt(i));
		}

		return this;
	}

	public boolean isBound() {
		return charstring ? cstr != null : val_ptr != null;
	}

	public boolean isPresent() {
		return isBound();
	};

	public boolean isValue() {
		return isBound();
	}

	public void mustBound(final String aErrorMessage) {
		if (charstring && cstr == null) {
			throw new TtcnError(aErrorMessage);
		}
		if (!charstring && val_ptr == null) {
			throw new TtcnError(aErrorMessage);
		}
	}

	// originally lengthof
	public TitanInteger lengthOf() {
		mustBound("Performing lengthof operation on an unbound universal charstring value.");

		if (charstring) {
			return new TitanInteger(cstr.length());
		}

		return new TitanInteger(val_ptr.size());
	}

	// originally operator==
	public boolean operatorEquals(final TitanUniversalCharString aOtherValue) {
		mustBound("The left operand of comparison is an unbound universal charstring value.");
		aOtherValue.mustBound("The right operand of comparison is an unbound universal charstring value.");

		if (charstring) {
			if (aOtherValue.charstring) {
				return cstr.toString().equals(aOtherValue.cstr.toString());
			} else {
				if (cstr.length() != aOtherValue.val_ptr.size()) {
					return false;
				}

				for (int i = 0; i < aOtherValue.val_ptr.size(); ++i) {
					if (!aOtherValue.val_ptr.get(i).is_char() || aOtherValue.val_ptr.get(i).getUc_cell() != cstr.charAt(i)){
						return false;
					}
				}

				return true;
			}

		}
		if (val_ptr.size() != aOtherValue.val_ptr.size()) {
			return false;
		}

		for (int i = 0; i < val_ptr.size(); ++i) {
			if (!val_ptr.get(i).operatorEquals(aOtherValue.val_ptr.get(i))) {
				return false;
			}
		}

		return true;
	}

	// originally operator==
	public boolean operatorEquals(final TitanUniversalCharString_Element aOtherValue) {
		mustBound("The left operand of comparison is an unbound universal charstring value.");
		aOtherValue.mustBound("The right operand of comparison is an unbound universal charstring element.");

		if (charstring) {
			return aOtherValue.get_char().is_char() && aOtherValue.get_char().getUc_cell() == cstr.charAt(0);
		}
		if (val_ptr.size() != 1) {
			return false;
		}

		return val_ptr.get(0).operatorEquals(aOtherValue.get_char());
	}

	// originally operator==
	public boolean operatorEquals(final TitanCharString aOtherValue) {
		mustBound("The left operand of comparison is an unbound universal charstring value.");
		aOtherValue.mustBound("The right operand of comparison is an unbound charstring value.");

		if (charstring) {
			return aOtherValue.toString().equals(cstr.toString());
		}
		if (val_ptr.size() != aOtherValue.lengthOf().getInt()) {
			return false;
		}

		for (int i = 0; i < val_ptr.size(); ++i) {
			if (val_ptr.get(i).getUc_group() != 0 || val_ptr.get(i).getUc_plane() != 0 || val_ptr.get(i).getUc_row() != 0
					|| val_ptr.get(i).getUc_cell() != aOtherValue.getValue().charAt(i)) {
				return false;
			}
		}

		return true;
	}

	// originally operator==
	public boolean operatorEquals(final TitanCharString_Element aOtherValue) {
		mustBound("The left operand of comparison is an unbound universal charstring value.");
		aOtherValue.mustBound("The right operand of comparison is an unbound charstring element.");

		if (charstring) {
			return cstr.charAt(0) == aOtherValue.get_char();
		}
		if (val_ptr.size() != 1) {
			return false;
		}
		return val_ptr.get(0).is_char() && val_ptr.get(0).getUc_cell() == aOtherValue.get_char();
	}

	@Override
	public boolean operatorEquals(final Base_Type otherValue) {
		if (otherValue instanceof TitanUniversalCharString) {
			return operatorEquals((TitanUniversalCharString) otherValue);
		}

		throw new TtcnError(MessageFormat.format("Internal Error: value `{0}'' can not be cast to universal charstring", otherValue));
	}

	// originally operator==
	public boolean operatorEquals(final TitanUniversalChar aOtherValue) {
		mustBound("The left operand of comparison is an unbound universal charstring value.");

		if (charstring) {
			return cstr.length() == 1 && aOtherValue.getUc_group() == 0 &&
					aOtherValue.getUc_plane() == 0 && aOtherValue.getUc_row() == 0 &&
					cstr.charAt(0) == aOtherValue.getUc_cell();
		}
		if (val_ptr.size() != 1) {
			return false;
		}

		return val_ptr.get(0).operatorEquals(aOtherValue);
	}

	// originally operator==
	public boolean operatorEquals(final String aOtherValue) {
		mustBound("The left operand of comparison is an unbound universal charstring value.");

		if (charstring) {
			return cstr.toString().equals(aOtherValue);
		}
		if (val_ptr.size() != aOtherValue.length()) {
			return false;
		}
		for (int i = 0; i < val_ptr.size(); ++i) {
			if (val_ptr.get(i).getUc_group() != 0 || val_ptr.get(i).getUc_plane() != 0 || val_ptr.get(i).getUc_row() != 0
					|| val_ptr.get(i).getUc_cell() != aOtherValue.charAt(i)) {
				return false;
			}
		}

		return true;
	}

	// originally operator!=
	public boolean operatorNotEquals(final TitanUniversalCharString aOtherValue) {
		return !operatorEquals(aOtherValue);
	}

	public boolean operatorNotEquals(final TitanUniversalCharString_Element aOtherValue) {
		return !operatorEquals(aOtherValue);
	}

	public boolean operatorNotEquals(final TitanCharString aOtherValue) {
		return !operatorEquals(aOtherValue);
	}

	public boolean operatorNotEquals(final TitanCharString_Element aOtherValue) {
		return !operatorEquals(aOtherValue);
	}

	public boolean operatorNotEquals(final Base_Type aOtherValue) {
		return !operatorEquals(aOtherValue);
	}

	public boolean operatorNotEquals(final TitanUniversalChar aOtherValue) {
		return !operatorEquals(aOtherValue);
	}

	public boolean operatorNotEquals(final String aOtherValue) {
		return !operatorEquals(aOtherValue);
	}

	/**
	 * this + aOtherValue
	 * originally UNIVERSAL_CHARSTRING UNIVERSAL_CHARSTRING::operator+ (const universal_char& other_value) const
	 */
	public TitanUniversalCharString concatenate( final TitanUniversalChar other_value ) {
		mustBound( "The left operand of concatenation is an unbound universal charstring value." );

		if (charstring) {
			if (other_value.is_char()) {
				return new TitanUniversalCharString( new StringBuilder(cstr).append( other_value.getUc_cell() ) );
			} else {
				final List<TitanUniversalChar> ulist = new ArrayList<TitanUniversalChar>();
				for (int i = 0; i < cstr.length(); ++i) {
					final TitanUniversalChar uc = new TitanUniversalChar( (char)0, (char)0, (char)0, cstr.charAt( i ) );
					ulist.add( uc );
				}
				ulist.add( other_value );
				final TitanUniversalCharString ret_val = new TitanUniversalCharString();
				ret_val.setValue( ulist );
				return ret_val;
			}
		}

		final TitanUniversalCharString ret_val = new TitanUniversalCharString( val_ptr );
		ret_val.val_ptr.add( other_value );
		return ret_val;
	}

	/**
	 * this + aOtherValue
	 * originally UNIVERSAL_CHARSTRING UNIVERSAL_CHARSTRING::operator+ (const char* other_value) const
	 */
	public TitanUniversalCharString concatenate( final String other_value ) {
		mustBound("The left operand of concatenation is an unbound universal charstring value.");

		int other_len;
		if (other_value == null) {
			other_len = 0;
		} else {
			other_len = other_value.length();
		}
		if (other_len == 0) {
			return new TitanUniversalCharString(this);
		}
		if ( charstring ) {
			return new TitanUniversalCharString( new StringBuilder(cstr).append( other_value ) );
		}
		final TitanUniversalCharString ret_val = new TitanUniversalCharString( val_ptr );
		for (int i = 0; i < other_len; i++) {
			final TitanUniversalChar uc = new TitanUniversalChar( (char)0, (char)0, (char)0, other_value.charAt( i ));
			ret_val.val_ptr.add(uc);
		}
		return ret_val;
	}

	/**
	 * this + aOtherValue
	 * originally UNIVERSAL_CHARSTRING UNIVERSAL_CHARSTRING::operator+ (const CHARSTRING& other_value) const
	 */
	public TitanUniversalCharString concatenate( final TitanCharString other_value ) {
		mustBound("The left operand of concatenation is an unbound universal charstring value.");
		other_value.mustBound("The right operand of concatenation is an unbound charstring value.");

		return concatenate( other_value.getValue().toString() );
	}

	/**
	 * this + aOtherValue
	 * originally UNIVERSAL_CHARSTRING UNIVERSAL_CHARSTRING::operator+ (const CHARSTRING_ELEMENT& other_value) const
	 */
	public TitanUniversalCharString concatenate( final TitanCharString_Element other_value ) {
		mustBound("The left operand of concatenation is an unbound universal charstring value.");
		other_value.mustBound("The right operand of concatenation is an unbound charstring element.");

		if ( charstring ) {
			return new TitanUniversalCharString( new StringBuilder(cstr).append( other_value.get_char() ) );
		}

		final TitanUniversalCharString ret_val = new TitanUniversalCharString( val_ptr );
		final TitanUniversalChar uc = new TitanUniversalChar( (char)0, (char)0, (char)0, other_value.get_char());
		ret_val.val_ptr.add( uc );
		return ret_val;
	}


	/**
	 * this + aOtherValue
	 * originally UNIVERSAL_CHARSTRING UNIVERSAL_CHARSTRING::operator+ (const UNIVERSAL_CHARSTRING& other_value) const
	 */
	public TitanUniversalCharString concatenate( final TitanUniversalCharString other_value ) {
		mustBound("The left operand of concatenation is an unbound universal charstring value.");
		other_value.mustBound("The right operand of concatenation is an unbound universal charstring value.");

		if (charstring) {
			if (cstr.length() == 0) {
				return new TitanUniversalCharString(other_value);
			}
			if (other_value.charstring) {
				if (other_value.cstr.length() == 0) {
					return new TitanUniversalCharString(this);
				}

				return new TitanUniversalCharString( new StringBuilder(cstr).append( other_value.cstr ) );
			} else {
				if (other_value.val_ptr.isEmpty()) {
					return new TitanUniversalCharString(this);
				}
				final List<TitanUniversalChar> ulist = new ArrayList<TitanUniversalChar>();
				for (int i = 0; i < cstr.length(); i++) {
					final TitanUniversalChar uc = new TitanUniversalChar( (char)0, (char)0, (char)0, cstr.charAt( i ) );
					ulist.add( uc );
				}
				ulist.addAll( other_value.val_ptr );
				final TitanUniversalCharString ret_val = new TitanUniversalCharString();
				ret_val.setValue( ulist );
				return ret_val;
			}
		} else {
			if (other_value.charstring) {
				final TitanUniversalCharString ret_val = new TitanUniversalCharString( val_ptr );
				final StringBuilder cs = other_value.cstr;
				final int cslen = cs.length();
				for (int i = 0; i < cslen; i++) {
					final TitanUniversalChar uc = new TitanUniversalChar( (char)0, (char)0, (char)0, cs.charAt( i ) );
					ret_val.getValue().add( uc );
				}
				return ret_val;
			} else {
				if (val_ptr.isEmpty()) {
					return new TitanUniversalCharString(other_value);
				}
				if (other_value.val_ptr.isEmpty()) {
					return new TitanUniversalCharString(this);
				}
				final TitanUniversalCharString ret_val = new TitanUniversalCharString( val_ptr );
				ret_val.getValue().addAll( other_value.val_ptr );
				return ret_val;
			}
		}
	}

	/**
	 * this + aOtherValue
	 * originally UNIVERSAL_CHARSTRING UNIVERSAL_CHARSTRING::operator+ (const UNIVERSAL_CHARSTRING_ELEMENT& other_value) const
	 */
	public TitanUniversalCharString concatenate( final TitanUniversalCharString_Element other_value ) {
		mustBound("The left operand of concatenation is an unbound universal charstring value.");
		other_value.mustBound("The right operand of concatenation is an unbound universal charstring element.");

		if ( charstring ) {
			return new TitanUniversalCharString(new StringBuilder(cstr).append( other_value.get_char().getUc_cell()));
		}

		final TitanUniversalCharString ret_val = new TitanUniversalCharString( val_ptr );
		ret_val.val_ptr.add( other_value.get_char() );
		return ret_val;
	}

	public void cleanUp() {
		val_ptr = null;
		cstr = null;
	}

	/**
	 * @return number of digits
	 */
	private int size() {
		return charstring ? cstr.length() : val_ptr.size();
	}

	//originally operator[](int)
	public TitanUniversalCharString_Element getAt(final int index_value) {
		if ( !isBound() && index_value == 0 ) {
			if ( charstring ) {
				cstr = new StringBuilder();
			} else {
				val_ptr = new ArrayList<TitanUniversalChar>();
			}
			return new TitanUniversalCharString_Element(false, this, 0);
		} else {
			mustBound("Accessing an element of an unbound universal charstring value.");

			if (index_value < 0) {
				throw new TtcnError("Accessing an universal charstring element using a negative index (" + index_value + ").");
			}

			final int n_nibbles = size();
			if (index_value > n_nibbles) {
				throw new TtcnError("Index overflow when accessing a universal charstring element: The index is " + index_value +
						", but the string has only " + n_nibbles + " characters.");
			}
			if (index_value == n_nibbles) {
				if ( charstring ) {
					cstr.append( (char)0 );
				} else {
					val_ptr.add( new TitanUniversalChar( (char)0, (char)0, (char)0, (char)0 ) );
				}
				return new TitanUniversalCharString_Element( false, this, index_value );
			} else {
				return new TitanUniversalCharString_Element( true, this, index_value );
			}
		}
	}

	//originally operator[](const INTEGER&)
	public TitanUniversalCharString_Element getAt(final TitanInteger index_value) {
		index_value.mustBound("Indexing a universal charstring value with an unbound integer value.");

		return getAt( index_value.getInt() );
	}

	//originally operator[](int) const
	public TitanUniversalCharString_Element constGetAt( final int index_value ) {
		mustBound("Accessing an element of an unbound universal charstring value.");

		if (index_value < 0) {
			throw new TtcnError("Accessing an universal charstring element using a negative index (" + index_value + ").");
		}

		final int n_nibbles = charstring ? cstr.length() : val_ptr.size();
		if (index_value >= n_nibbles) {
			throw new TtcnError("Index overflow when accessing a universal charstring element: The index is " + index_value +
					", but the string has only " + n_nibbles + " characters.");
		}

		return new TitanUniversalCharString_Element(true, this, index_value);
	}

	//originally operator[](const INTEGER&) const
	public TitanUniversalCharString_Element constGetAt( final TitanInteger index_value ) {
		index_value.mustBound("Indexing a universal charstring value with an unbound integer value.");

		return constGetAt( index_value.getInt() );
	}

	public static boolean isPrintable(final TitanUniversalChar uchar) {
		return uchar.getUc_group() == 0 && uchar.getUc_plane() == 0 && uchar.getUc_row() == 0 && TtcnLogger.isPrintable(uchar.getUc_cell());
	}

	private static enum States {
		INIT, PCHAR, UCHAR;
	}

	public void log(){
		if(charstring){
			new TitanCharString(cstr).log();
			return;
		}
		if (val_ptr != null) {
			States state = States.INIT;
			final StringBuilder buffer = new StringBuilder();
			for (int i = 0; i < val_ptr.size(); i++) {
				final TitanUniversalChar uchar = val_ptr.get(i);
				if (isPrintable(uchar)) {
					switch (state) {
					case UCHAR:
						buffer.append(" & ");
					case INIT:
						buffer.append('\"');
					case PCHAR:
						TtcnLogger.logCharEscaped(uchar.getUc_cell(), buffer);
						break;
					}
					state = States.PCHAR;
				} else {
					switch (state) {
					case PCHAR:
						buffer.append('\"');
					case UCHAR:
						buffer.append(" & ");
					case INIT:
						buffer.append(MessageFormat.format("char({0}, {1}, {2}, {3})", (int)uchar.getUc_group(), (int)uchar.getUc_plane(), (int)uchar.getUc_row(), (int)uchar.getUc_cell()));
						break;
					}
					state = States.UCHAR;
				}
			}
			switch (state) {
			case INIT:
				buffer.append("\"\"");
				break;
			case PCHAR:
				buffer.append('\"');
				break;
			default:
				break;
			}
			TtcnLogger.log_event_str(buffer.toString());

		} else {
			TtcnLogger.log_event_unbound();
		}

	}

	@Override
	public String toString() {
		if ( !isBound() ) {
			return "<unbound>";
		}

		if ( charstring ) {
			return cstr.toString();
		} else {
			final StringBuilder str = new StringBuilder();

			for (int i = 0; i < val_ptr.size(); ++i) {
				str.append(val_ptr.get(i).toString());
			}

			return str.toString();
		}
	}

	/**
	 * @return unicode string representation
	 */
	public String toUtf() {
		mustBound("Accessing an element of an unbound universal charstring value.");

		if ( charstring ) {
			return cstr.toString();
		} else {
			final StringBuilder str = new StringBuilder();

			for (int i = 0; i < val_ptr.size(); ++i) {
				str.append(val_ptr.get(i).toUtf());
			}

			return str.toString();
		}
	}

	@Override
	/** {@inheritDoc} */
	public void encode_text(final Text_Buf text_buf) {
		mustBound("Text encoder: Encoding an unbound universal charstring value.");

		if (charstring) {
			convertCstrToUni();
		}

		final int n_chars = val_ptr.size();
		text_buf.push_int(n_chars);
		for (int i = 0; i < n_chars; i++) {
			final TitanUniversalChar tempChar = val_ptr.get(i);
			byte buf[] = new byte[4];
			buf[0] = (byte)tempChar.getUc_group();
			buf[1] = (byte)tempChar.getUc_plane();
			buf[2] = (byte)tempChar.getUc_row();
			buf[3] = (byte)tempChar.getUc_cell();
			text_buf.pull_raw(4, buf);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void decode_text(final Text_Buf text_buf) {
		cleanUp();

		final int n_uchars = text_buf.pull_int().getInt();
		if (n_uchars < 0) {
			throw new TtcnError("Text decoder: Invalid length was received for an universal charstring.");
		}
		charstring = false;
		if (n_uchars > 0) {
			val_ptr = new ArrayList<TitanUniversalChar>(n_uchars);
			for (int i = 0; i < n_uchars; i++) {
				final byte buf[] = new byte[4];
				text_buf.pull_raw(4, buf);
				final TitanUniversalChar temp = new TitanUniversalChar((char)buf[0], (char)buf[1], (char)buf[2], (char)buf[3]);
				val_ptr.add(temp);
			}
		}
	}

	// intentionally package public
	final TitanUniversalChar charAt(final int i) {
		if (charstring) {
			return new TitanUniversalChar((char) 0, (char) 0, (char) 0, cstr.charAt(i));
		}

		return val_ptr.get(i);
	}

	// intentionally package public
	final void setCharAt(final int i, final TitanUniversalChar c) {
		if (charstring) {
			convertCstrToUni();
		}

		val_ptr.set(i, c);
	}

	final void setCharAt( final int i, final char c ) {
		if (charstring) {
			cstr.setCharAt( i, c );
		} else {
			val_ptr.set( i, new TitanUniversalChar( (char)0, (char)0, (char)0, c ) );
		}
	}

	// originally operator<<=
	public TitanUniversalCharString rotateLeft(int rotateCount) {
		mustBound("The left operand of rotate left operator is an unbound universal charstring value.");

		if (charstring) {
			return new TitanUniversalCharString(new TitanCharString(cstr).rotateLeft(rotateCount));
		}
		if (val_ptr.isEmpty()) {
			return this;
		}
		if (rotateCount >= 0) {
			rotateCount = rotateCount % val_ptr.size();
			if (rotateCount == 0) {
				return this;
			}

			final TitanUniversalCharString result = new TitanUniversalCharString();
			result.val_ptr = new ArrayList<TitanUniversalChar>();
			for (int i = 0; i < val_ptr.size() - rotateCount; i++) {
				result.val_ptr.add(i, val_ptr.get(i + rotateCount));
			}
			for (int i = val_ptr.size() - rotateCount; i < val_ptr.size(); i++) {
				result.val_ptr.add(i, val_ptr.get(i + rotateCount - val_ptr.size()));
			}

			return result;
		} else {
			return rotateRight(-rotateCount);
		}
	}

	public TitanUniversalCharString rotateLeft(final TitanInteger rotateCount) {
		rotateCount.mustBound("Unbound right operand of octetstring rotate left operator.");

		return rotateLeft(rotateCount.getInt());
	}

	// originally operator>>=
	public TitanUniversalCharString rotateRight(int rotateCount) {
		mustBound("The left operand of rotate right operator is an unbound universal charstring value.");

		if (charstring) {
			return new TitanUniversalCharString(new TitanCharString(cstr).rotateRight(rotateCount));
		}
		if (val_ptr.isEmpty()) {
			return this;
		}
		if (rotateCount >= 0) {
			rotateCount = rotateCount % val_ptr.size();
			if (rotateCount == 0) {
				return this;
			}

			final TitanUniversalCharString result = new TitanUniversalCharString();
			result.val_ptr = new ArrayList<TitanUniversalChar>();
			if (rotateCount > val_ptr.size()) {
				rotateCount = val_ptr.size();
			}
			for (int i = 0; i < rotateCount; i++) {
				result.val_ptr.add(i, val_ptr.get(i - rotateCount + val_ptr.size()));
			}
			for (int i = rotateCount; i < val_ptr.size(); i++) {
				result.val_ptr.add(i, val_ptr.get(i - rotateCount));
			}

			return result;
		} else {
			return rotateLeft(-rotateCount);
		}
	}

	public TitanUniversalCharString rotateRight(final TitanInteger rotateCount) {
		rotateCount.mustBound("Unbound right operand of octetstring rotate left operator.");

		return rotateRight(rotateCount.getInt());
	}

	public void convertCstrToUni() {
		val_ptr = new ArrayList<TitanUniversalChar>(cstr.length());
		for (int i = 0; i < cstr.length(); ++i) {
			val_ptr.add(i, new TitanUniversalChar((char) 0, (char) 0, (char) 0, cstr.charAt(i)));
		}
		charstring = false;
		cstr = null;
	}

	// static function

	public static boolean operatorEquals(final TitanUniversalChar ucharValue, final TitanUniversalCharString otherValue) {
		otherValue.mustBound("The right operand of comparison is an unbound universal charstring value.");

		if (otherValue.charstring) {
			if (otherValue.cstr.length() != 1) {
				return false;
			}
			return ucharValue.is_char() && ucharValue.getUc_cell() == otherValue.cstr.charAt(0);
		}
		if (otherValue.val_ptr.size() != 1) {
			return false;
		}

		return ucharValue.operatorEquals(otherValue.val_ptr.get(0));
	}

	public static boolean operatorNotEquals(final TitanUniversalChar ucharValue, final TitanUniversalCharString otherValue) {
		return !operatorEquals(ucharValue, otherValue);
	}

	public static TitanUniversalCharString concatenate(final TitanUniversalChar ucharValue, final TitanUniversalCharString otherValue) {
		otherValue.mustBound("The right operand of concatenation is an unbound universal charstring value.");

		if (otherValue.charstring) {
			if (ucharValue.is_char()) {
				return new TitanUniversalCharString(new StringBuilder(ucharValue.getUc_cell()).append(otherValue.cstr));
			} else {
				final List<TitanUniversalChar> ulist = new ArrayList<TitanUniversalChar>();
				ulist.add(ucharValue);
				for (int i = 0; i < otherValue.cstr.length(); ++i) {
					final TitanUniversalChar uc = new TitanUniversalChar((char) 0, (char) 0, (char) 0, otherValue.cstr.charAt(i));
					ulist.add(uc);
				}
				final TitanUniversalCharString ret_val = new TitanUniversalCharString();
				ret_val.setValue(ulist);
				return ret_val;
			}
		}

		final TitanUniversalCharString ret_val = new TitanUniversalCharString(ucharValue);
		ret_val.val_ptr.addAll(otherValue.val_ptr);
		return ret_val;
	}

	public static boolean operatorEquals(final String aOtherValue, final TitanUniversalCharString rightValue) {
		rightValue.mustBound("The left operand of comparison is an unbound universal charstring value.");

		if (rightValue.charstring) {
			return rightValue.cstr.toString().equals(aOtherValue);
		}
		if (rightValue.val_ptr.size() != aOtherValue.length()) {
			return false;
		}
		for (int i = 0; i < rightValue.val_ptr.size(); ++i) {
			if (rightValue.val_ptr.get(i).getUc_group() != 0 || rightValue.val_ptr.get(i).getUc_plane() != 0
					|| rightValue.val_ptr.get(i).getUc_row() != 0
					|| rightValue.val_ptr.get(i).getUc_cell() != aOtherValue.charAt(i)) {
				return false;
			}
		}

		return true;
	}

	public static TitanUniversalCharString concatenate(final String other_value, final TitanUniversalCharString rightValue) {
		rightValue.mustBound("The left operand of concatenation is an unbound universal charstring value.");

		int other_len;
		if (other_value == null) {
			other_len = 0;
		} else {
			other_len = other_value.length();
		}
		if (other_len == 0) {
			return new TitanUniversalCharString(rightValue);
		}
		if (rightValue.charstring) {
			return new TitanUniversalCharString(new StringBuilder(other_value).append(rightValue.cstr));
		}
		final TitanUniversalCharString ret_val = new TitanUniversalCharString();
		ret_val.val_ptr = new ArrayList<TitanUniversalChar>();
		for (int i = 0; i < other_len; i++) {
			final TitanUniversalChar uc = new TitanUniversalChar((char) 0, (char) 0, (char) 0, other_value.charAt(i));
			ret_val.val_ptr.add(uc);
		}
		ret_val.val_ptr.addAll(rightValue.val_ptr);
		return ret_val;
	}

	// decode

	public void decode_utf8(final char[] valueStr, final CharCoding code, final boolean checkBOM) {
		// approximate the number of characters
		final int lenghtOctets = valueStr.length;
		int lenghtUnichars = 0;
		for (int i = 0; i < lenghtOctets; i++) {
			// count all octets except the continuing octets (10xxxxxx)
			if ((valueStr[i] & 0xC0) != 0x80) lenghtUnichars++;
		}
		// allocate enough memory, start from clean state
		cleanUp();
		charstring=false;
		// FIXME: init_struct(lenghtUnichars)
		val_ptr = new ArrayList<TitanUniversalChar>(lenghtUnichars);
		for (int i = 0; i < lenghtUnichars; i++) {
			val_ptr.add(new TitanUniversalChar());
		}
		lenghtUnichars = 0;

		int start = checkBOM ? check_BOM(CharCoding.UTF_8, valueStr) : 0;
		for (int i = start; i < lenghtOctets; ) {
			// perform the decoding character by character
			if (valueStr[i] <= 0x7F)  {
				// character encoded on a single octet: 0xxxxxxx (7 useful bits)
				val_ptr.add(lenghtUnichars, new TitanUniversalChar((char)0,(char) 0,(char) 0, valueStr[i]));
				i++;
				lenghtUnichars++;
			} else if (valueStr[i] <= 0xBF)  {
				// continuing octet (10xxxxxx) without leading octet ==> malformed
				TTCN_EncDec_ErrorContext.error(TTCN_EncDec.error_type.ET_DEC_UCSTR, MessageFormat.format(
						"Malformed: At character position {0}, octet position {1}: continuing octet {0} without leading octet.",
						lenghtUnichars, i, valueStr[i]));
				i++;
			} else if (valueStr[i] <= 0xDF)  {
				// character encoded on 2 octets: 110xxxxx 10xxxxxx (11 useful bits)
				char[] octets = new char[2];
				octets[0] = (char) (valueStr[i] & 0x1F);

				fill_continuing_octets(1, octets, lenghtOctets, valueStr, i + 1, lenghtUnichars);
				
				val_ptr.set(lenghtUnichars, new TitanUniversalChar((char) 0, (char) 0,(char) (octets[0] >> 2), (char) ((octets[0] << 6) & 0xFF | octets[1])));

				if (val_ptr.get(lenghtUnichars).getUc_row() == 0x00 && 
						val_ptr.get(lenghtUnichars).getUc_cell() < 0x80) {
					TTCN_EncDec_ErrorContext.error(TTCN_EncDec.error_type.ET_DEC_UCSTR, MessageFormat.format(
							"Overlong: At character position {0}, octet position {1}: 2-octet encoding for quadruple (0, 0, 0, {2}).", 
							lenghtUnichars, i, val_ptr.get(lenghtUnichars).getUc_cell()));
				}
				i += 2;
				lenghtUnichars++;
			} else if (valueStr[i] <= 0xEF) {
				// character encoded on 3 octets: 1110xxxx 10xxxxxx 10xxxxxx
				// (16 useful bits)
				char[] octets = new char[3];
				octets[0] = (char) (valueStr[i] & 0x0F);
				fill_continuing_octets(2, octets /*+ 1*/, lenghtOctets, valueStr, i + 1, lenghtUnichars);

				val_ptr.set(lenghtUnichars, new TitanUniversalChar((char) 0, (char) 0,(char) ((octets[0] << 4) & 0xFF | octets[1] >> 2), (char) ((octets[1] << 6) & 0xFF | octets[2])));

				if (val_ptr.get(lenghtUnichars).getUc_row() < 0x08) {
					TTCN_EncDec_ErrorContext.error(TTCN_EncDec.error_type.ET_DEC_UCSTR,
							MessageFormat.format("Overlong: At character position {0}, octet position {1}: 3-octet encoding for quadruple (0, 0, {2}, {3}).", 
									lenghtUnichars, i, val_ptr.get(lenghtUnichars).getUc_row(), val_ptr.get(lenghtUnichars).getUc_cell()));
				}
				i += 3;
				lenghtUnichars++;
			} else if (valueStr[i] <= 0xF7) {
				// character encoded on 4 octets: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
				// (21 useful bits)
				char[] octets = new char[4];
				octets[0] = (char) (valueStr[i] & 0x07);
				fill_continuing_octets(3, octets /*+ 1*/, lenghtOctets, valueStr, i + 1, lenghtUnichars);

				val_ptr.set(lenghtUnichars, new TitanUniversalChar((char) 0, (char) ((octets[0] << 2) & 0xFF | octets[1] >> 4),(char) ((octets[1] << 4) & 0xFF | octets[2] >> 2), (char) ((octets[2] << 6) & 0xFF | octets[3])));

				if (val_ptr.get(lenghtUnichars).getUc_plane() == 0x00) {
					TTCN_EncDec_ErrorContext.error(TTCN_EncDec.error_type.ET_DEC_UCSTR,
							MessageFormat.format("Overlong: At character position {0}, octet position {1}: 4-octet encoding for quadruple (0, 0, {2}, {3}).", 
									lenghtUnichars, i, val_ptr.get(lenghtUnichars).getUc_row(), val_ptr.get(lenghtUnichars).getUc_cell()));
				}
				i += 4;
				lenghtUnichars++;
			} else if (valueStr[i] <= 0xFB) {
				// character encoded on 5 octets: 111110xx 10xxxxxx 10xxxxxx 10xxxxxx
				// 10xxxxxx (26 useful bits)

				char[] octets = new char[5];
				octets[0] = (char) (valueStr[i] & 0x03);
				fill_continuing_octets(4, octets /*+ 1*/, lenghtOctets, valueStr, i + 1, lenghtUnichars);

				val_ptr.set(lenghtUnichars, new TitanUniversalChar((char) octets[0], (char) ((octets[1] << 2) & 0xFF | octets[2] >> 4),(char) ((octets[2] << 4) & 0xFF | octets[3] >> 2), (char) ((octets[3] << 6) & 0xFF | octets[4])));

				if (val_ptr.get(lenghtUnichars).getUc_group() == 0x00 && val_ptr.get(lenghtUnichars).getUc_plane() < 0x20) {
					TTCN_EncDec_ErrorContext.error(TTCN_EncDec.error_type.ET_DEC_UCSTR,
							MessageFormat.format("Overlong: At character position {0}, octet position {1}: 5-octet encoding for quadruple (0, {4}, {2}, {3}).", 
									lenghtUnichars, i, val_ptr.get(lenghtUnichars).getUc_row(), val_ptr.get(lenghtUnichars).getUc_cell(),  val_ptr.get(lenghtUnichars).getUc_plane()));
				}
				i += 5;
				lenghtUnichars++;
			} else if (valueStr[i] <= 0xFD) {
				// character encoded on 6 octets: 1111110x 10xxxxxx 10xxxxxx 10xxxxxx
				// 10xxxxxx 10xxxxxx (31 useful bits)

				char[] octets = new char[6];
				octets[0] = (char) (valueStr[i] & 0x01);
				fill_continuing_octets(5, octets, lenghtOctets, valueStr, i + 1, lenghtUnichars);

				val_ptr.set(lenghtUnichars, new TitanUniversalChar((char) ((octets[0] << 6) & 0xFF | octets[1]), (char) ((octets[2] << 2) & 0xFF | octets[3] >> 4),(char) ((octets[3] << 4) & 0xFF | octets[4] >> 2), (char) ((octets[4] << 6) & 0xFF | octets[5])));

				if (val_ptr.get(lenghtUnichars).getUc_group() < 0x04) {
					TTCN_EncDec_ErrorContext.error(TTCN_EncDec.error_type.ET_DEC_UCSTR,
							MessageFormat.format("Overlong: At character position {0}, octet position {1}: 6-octet encoding for quadruple {2}.", 
									lenghtUnichars, i, val_ptr.get(lenghtUnichars).toString()));
				}
				i += 6;
				lenghtUnichars++;
			} else {
				// not used code points: FE and FF => malformed
				TTCN_EncDec_ErrorContext.error(TTCN_EncDec.error_type.ET_DEC_UCSTR,
						MessageFormat.format("Malformed: At character position {0}, octet position {1}: unused/reserved octet {2}.", lenghtUnichars, i, valueStr[i]));
				i++;
			}
		}

		if (val_ptr.size() != lenghtUnichars) {
			// truncate the memory and set the correct size in case of decoding errors
			// (e.g. skipped octets)

			if (lenghtUnichars > 0) {
				List<TitanUniversalChar> helper = new ArrayList<TitanUniversalChar>(lenghtUnichars);
				for (int i = 0; i < lenghtUnichars && i < val_ptr.size(); i++) {
					helper.add(val_ptr.get(i));
				}
				val_ptr = helper;
			} else {
				cleanUp();
			}
		}
	}

	public int check_BOM(final CharCoding code, final char[] ostr) {
		String coding_str;
		//BOM indicates that the byte order is determined by a byte order mark, 
		//if present at the beginning the length of BOM is returned.
		int length = ostr.length;
		switch (code) {
		case UTF32BE:
		case UTF32:
			if (4 <= length && 0x00 == ostr[0] && 0x00 == ostr[1] &&
			0xFE == ostr[2] && 0xFF == ostr[3]) {
				return 4;
			}
			coding_str = "UTF-32BE";
			break;
		case UTF32LE:
			if (4 <= length && 0xFF == ostr[0] && 0xFE == ostr[1] &&
			0x00 == ostr[2] && 0x00 == ostr[3]) {
				return 4;
			}
			coding_str = "UTF-32LE";
			break;
		case UTF16BE:
		case UTF16:
			if (2 <= length && 0xFE == ostr[0] && 0xFF == ostr[1]) {
				return 2;
			}
			coding_str = "UTF-16BE";
			break;
		case UTF16LE:
			if (2 <= length && 0xFF == ostr[0] && 0xFE == ostr[1]) {
				return 2;
			}
			coding_str = "UTF-16LE";
			break;
		case UTF_8:
			if (3 <= ostr.length && 0xEF == ostr[0] && 0xBB == ostr[1] && 0xBF == ostr[2]) {
				return 3;
			}
			coding_str = "UTF-8";
			break;
		default:
			throw new TtcnError(MessageFormat.format("Internal error: invalid expected coding ({0})", code));
		}

		if (TtcnLogger.log_this_event(TtcnLogger.Severity.DEBUG_UNQUALIFIED)) {
			TtcnLogger.begin_event(TtcnLogger.Severity.DEBUG_UNQUALIFIED);
			TtcnLogger.log_event_str("Warning: No ");
			TtcnLogger.log_event_str(coding_str);
			TtcnLogger.log_event_str(" Byte Order Mark(BOM) detected. It may result decoding errors");
			TtcnLogger.end_event();
		}
		return 0;
	}

	public static void fill_continuing_octets(final int n_continuing, final char[] continuing_ptr, final int n_octets,
			final char[] octets_ptr, final int start_pos, final int uchar_pos) {
		for (int i = 0; i < n_continuing; i++) {
			if (start_pos + i < n_octets) {
				char octet = octets_ptr[start_pos + i];
				if ((octet & 0xC0) != 0x80) {
					TTCN_EncDec_ErrorContext.error(TTCN_EncDec.error_type.ET_DEC_UCSTR,
							MessageFormat.format("Malformed: At character position {0}, octet position {1}: {2} is not a valid continuing octet.", uchar_pos, start_pos + i, octet));
				}
				continuing_ptr[i + 1] = (char) (octet & 0x3F);
			} else {
				if (start_pos + i == n_octets) {
					if (i > 0) {
						// only a part of octets is missing
						TTCN_EncDec_ErrorContext.error(TTCN_EncDec.error_type.ET_DEC_UCSTR,
								MessageFormat.format("Incomplete: At character position {0}, octet position {1}: {2} out of {3} continuing octets {4} missing from the end of the stream.",
										uchar_pos, start_pos + i, n_continuing - i, n_continuing, n_continuing - i > 1 ? "are" : "is"));
					} else {
						// all octets are missing
						TTCN_EncDec_ErrorContext.error(TTCN_EncDec.error_type.ET_DEC_UCSTR,
								MessageFormat.format("Incomplete: At character position {0}, octet position {1}: {2} continuing octet{3} missing from the end of the stream.",
										uchar_pos, start_pos, n_continuing, n_continuing > 1 ? "s are" : " is"));
					}
				}
				continuing_ptr[i + 1] = 0;
			}
		}
	}

	// encode

	public void encode_utf8(final TTCN_Buffer text_buf) {
		encode_utf8(text_buf, false);
	}

	public void encode_utf8(final TTCN_Buffer buf, final boolean addBOM) {
		// FIXME: implement
		System.out.println("encode_utf8: ");
		// Add BOM
		if (addBOM) {
			buf.put_c((char)0xEF);
			buf.put_c((char)0xBB);
			buf.put_c((char)0xBF);
		}

		if (charstring) {
			final char[] bstr = new char[cstr.length()];
			for (int i = 0; i < cstr.length(); i++) {
				bstr[i] =  cstr.charAt(i);
				System.out.println((int) bstr[i]);
			}
			buf.put_s(bstr);
			// put_s avoids the check for boundness in put_cs
		} else {
			for (int i=0; i<val_ptr.size(); i++) {
				char g = val_ptr.get(i).getUc_group();
				char p = val_ptr.get(i).getUc_plane();
				char r =val_ptr.get(i).getUc_row();
				char c =val_ptr.get(i).getUc_cell();
				if (g==0x00 && p<=0x1F) {
					if (p==0x00) {
						if (r==0x00 && c<=0x7F) {
							// 1 octet
							buf.put_c(c);
						} // r
						// 2 octets
						else if (r<=0x07) {
							buf.put_c((char)(0xC0|r<<2|c>>6));
							buf.put_c((char)(0x80|(c&0x3F)));
						} // r
						// 3 octets
						else {
							buf.put_c((char) (0xE0|r>>4));
							buf.put_c((char) (0x80|(r<<2&0x3C)|c>>6));
							buf.put_c((char) (0x80|(c&0x3F)));
						} // r
					} // p
					// 4 octets
					else {
						buf.put_c((char) (0xF0|p>>2));
						buf.put_c((char) (0x80|(p<<4&0x30)|r>>4));
						buf.put_c((char) (0x80|(r<<2&0x3C)|c>>6));
						buf.put_c((char) (0x80|(c&0x3F)));
					} // p
				} //g
				// 5 octets
				else if (g<=0x03) {
					buf.put_c((char) (0xF8|g));
					buf.put_c((char) (0x80|p>>2));
					buf.put_c((char) (0x80|(p<<4&0x30)|r>>4));
					buf.put_c((char) (0x80|(r<<2&0x3C)|c>>6));
					buf.put_c((char) (0x80|(c&0x3F)));
				} // g
				// 6 octets
				else {
					buf.put_c((char) (0xFC|g>>6));
					buf.put_c((char) (0x80|(g&0x3F)));
					buf.put_c((char) (0x80|p>>2));
					buf.put_c((char) (0x80|(p<<4&0x30)|r>>4));
					buf.put_c((char) (0x80|(r<<2&0x3C)|c>>6));
					buf.put_c((char) (0x80|(c&0x3F)));
				}
			} // for i
		}
	}
}
