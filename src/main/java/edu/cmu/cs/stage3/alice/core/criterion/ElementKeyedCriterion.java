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

package edu.cmu.cs.stage3.alice.core.criterion;

public class ElementKeyedCriterion implements edu.cmu.cs.stage3.util.Criterion {
	private final String m_key;
	private final boolean m_ignoreCase;

	public ElementKeyedCriterion(final String key) {
		this(key, true);
	}

	public ElementKeyedCriterion(final String key, final boolean ignoreCase) {
		m_key = key;
		m_ignoreCase = ignoreCase;
	}

	public String getKey() {
		return m_key;
	}

	public boolean getIgnoreCase() {
		return m_ignoreCase;
	}

	@Override
	public boolean accept(final Object o) {
		if (o instanceof edu.cmu.cs.stage3.alice.core.Element) {
			final String key = ((edu.cmu.cs.stage3.alice.core.Element) o).getKey();
			if (m_key == null) {
				return key == null;
			} else {
				if (m_ignoreCase) {
					return m_key.equalsIgnoreCase(key);
				} else {
					return m_key.equals(key);
				}
			}
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + "[" + m_key + "]";
	}

	protected static ElementKeyedCriterion valueOf(final String s, final Class<? extends ElementKeyedCriterion> cls) {
		final String beginMarker = cls.getName() + "[";
		final String endMarker = "]";
		final int begin = s.indexOf(beginMarker) + beginMarker.length();
		final int end = s.lastIndexOf(endMarker);
		try {
			final Class<?>[] types = { String.class };
			final Object[] values = { s.substring(begin, end) };
			final java.lang.reflect.Constructor<? extends ElementKeyedCriterion> constructor = cls.getConstructor(types);
			return constructor.newInstance(values);
		} catch (final Throwable t) {
			throw new RuntimeException();
		}
	}

	public static ElementKeyedCriterion valueOf(final String s) {
		return valueOf(s, edu.cmu.cs.stage3.alice.core.criterion.ElementKeyedCriterion.class);
	}
}