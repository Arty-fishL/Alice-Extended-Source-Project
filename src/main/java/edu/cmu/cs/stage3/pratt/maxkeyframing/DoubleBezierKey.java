/*
 * Copyright (c) 1999-2003, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice",
 *    nor may "Alice" appear in their name, without prior written
 *    permission of Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    "This product includes software developed by Carnegie Mellon University"
 */

package edu.cmu.cs.stage3.pratt.maxkeyframing;

/**
 * @author Jason Pratt
 */
public class DoubleBezierKey extends BezierKey {
	public DoubleBezierKey(final double time, final double value, final double incomingControl,
			final double outgoingControl) {
		super(time, new double[] { value }, new double[] { incomingControl }, new double[] { outgoingControl });
	}

	@Override
	public Object createSample(final double[] components) {
		return new Double(components[0]);
	}

	public static DoubleBezierKey valueOf(final String s) {
		final java.util.StringTokenizer st = new java.util.StringTokenizer(s, " \t,[]");

		/** Unused ?? final String className = */ st.nextToken();
		final double time = Double.parseDouble(st.nextToken());
		final double value = Double.parseDouble(st.nextToken());
		final double incomingControl = Double.parseDouble(st.nextToken());
		final double outgoingControl = Double.parseDouble(st.nextToken());

		return new DoubleBezierKey(time, value, incomingControl, outgoingControl);
	}
}
