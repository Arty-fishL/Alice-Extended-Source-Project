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
public class QuaternionKey extends Key {
	protected edu.cmu.cs.stage3.math.Quaternion q;

	public QuaternionKey(final double time, final edu.cmu.cs.stage3.math.Quaternion q) {
		this.q = q;
		setTime(time);
		setValueComponents(new double[] { q.x, q.y, q.z, q.w });
	}

	private final edu.cmu.cs.stage3.math.Quaternion qSample = new edu.cmu.cs.stage3.math.Quaternion();

	@Override
	public Object createSample(final double[] components) {
		qSample.x = components[0];
		qSample.y = components[1];
		qSample.z = components[2];
		qSample.w = components[3];
		return qSample;
		// return new edu.cmu.cs.stage3.math.Quaternion( components[0],
		// components[1], components[2], components[3] );
	}

	public edu.cmu.cs.stage3.math.Quaternion getQuaternion() {
		return q;
	}

	@Override
	public String toString() {
		final String className = this.getClass().getName();
		final double[] components = getValueComponents();
		final int numComponents = components.length;

		final StringBuffer repr = new StringBuffer();
		repr.append(className);
		repr.append("[");
		repr.append(getTime());
		repr.append(",");
		for (int i = 0; i < numComponents - 1; i++) {
			repr.append(components[i]);
			repr.append(",");
		}
		repr.append(components[numComponents - 1]);
		repr.append("]");

		return repr.toString();
	}

	public static QuaternionKey valueOf(final String s) {
		final java.util.StringTokenizer st = new java.util.StringTokenizer(s, " \t,[]");

		/* Unused ?? final String className = */ st.nextToken();
		final double time = Double.parseDouble(st.nextToken());
		final edu.cmu.cs.stage3.math.Quaternion value = new edu.cmu.cs.stage3.math.Quaternion(
				Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()),
				Double.parseDouble(st.nextToken()), Double.parseDouble(st.nextToken()));

		return new QuaternionKey(time, value);
	}
}