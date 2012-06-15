package es.yrbcn.graph.weighted;

/*		 
 * Copyright (C) 2007 Paolo Boldi and Sebastiano Vigna 
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

/** An abstract (single-attribute) float label.
 *
 * <p>This class provides basic methods for a label holding a floating-point number.
 * Concrete implementations may impose further requirements on the number.
 * 
 * <p>Implementing subclasses must provide constructors, {@link Label#copy()},
 * {@link Label#fromBitStream(it.unimi.dsi.io.InputBitStream, int)}, {@link Label#toBitStream(it.unimi.dsi.mg4j.io.OutputBitStream, int)}
 * and possibly ovveride {@link #toString()}.
 */

public abstract class AbstractFloatLabel extends it.unimi.dsi.webgraph.labelling.AbstractLabel implements it.unimi.dsi.webgraph.labelling.Label {
	/** The key of the attribute represented by this label. */
	protected final String key;
	/** The value of the attribute represented by this label. */
	public float value;

	/** Creates an int label with given key and value.
	 * 
	 * @param key the (only) key of this label.
	 * @param value the value of this label.
	 */
	public AbstractFloatLabel( String key, float value ) {
		this.key = key;
		this.value = value;
	}

	public String wellKnownAttributeKey() {
		return key;
	}

	public String[] attributeKeys() {
		return new String[] { key };
	}

	public Class[] attributeTypes() {
		return new Class[] { int.class };
	}

	public Object get( String theKey ) {
		return Float.valueOf( getFloat( theKey ) ); 
	}

	public int getInt( String theKey ) {
		return (int)getFloat( theKey );
	}

	public long getLong( String theKey ) {
		return (long)getFloat( theKey );
	}

	public float getFloat( String theKey ) {
		if ( key.equals( theKey ) ) return value;
		throw new IllegalArgumentException();
	}

	public double getDouble( String theKey ) {
		return (double)getFloat( theKey );
	}

	public Object get() {
		return Integer.valueOf( getInt() ); 
	}

	public int getInt() {
		return (int)value;
	}

	public long getLong() {
		return (long)value;
	}

	public float getFloat() {
		return value;
	}

	public double getDouble() {
		return value;
	}

	public String toString() {
		return key + ":" + value;
	}
	
	@Override
	public boolean equals( Object x ) {
		if ( x instanceof AbstractFloatLabel ) return ( value == ( (AbstractFloatLabel)x ).value );
		else return false;
	}
	
	@Override
	public int hashCode() {
		return new Float(value).hashCode();
	}
}
