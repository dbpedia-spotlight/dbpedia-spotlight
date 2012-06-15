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


import it.unimi.dsi.io.InputBitStream;
import it.unimi.dsi.io.OutputBitStream;

import java.io.IOException;

/** An integer represented in fixed width. The provided width must
 * be smaller than 32.
 */

public class FixedWidthFloatLabel extends AbstractFloatLabel {

	/** Creates a new fixed-width float label.
	 * 
	 * @param key the (only) key of this label.
	 * @param value the value of this label.
	 */
	public FixedWidthFloatLabel( String key, float value ) {
		super( key, value );
	}

	/** Creates a new fixed-width int label of value 0.
	 * 
	 * @param key the (only) key of this label.
	 * @param width the label width (in bits).
	 */
	public FixedWidthFloatLabel( String key ) {
		this( key, (float)0 );
	}
	
	public FixedWidthFloatLabel( String... arg ) {
		this( arg[ 0 ], arg.length == 2 ? Float.parseFloat( arg[ 1 ] ) : 0 );
	}

	public FixedWidthFloatLabel copy() {
		return new FixedWidthFloatLabel( key, value );
	}

	public int fromBitStream( final InputBitStream inputBitStream, final int sourceUnused ) throws IOException {
		value = Float.intBitsToFloat( inputBitStream.readInt( Integer.SIZE ) );
		return Integer.SIZE;
	}

	public int toBitStream( final OutputBitStream outputBitStream, final int sourceUnused ) throws IOException {
		return outputBitStream.writeInt(Float.floatToIntBits(value), Integer.SIZE );
	}

	public String toString() {
		return key + ":" + value;
	}
	
	public String toSpec() {
		return this.getClass().getName() + "(" + key + ")";
	}

	public int fixedWidth() {
		return Float.SIZE;
	}
}
