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

package edu.cmu.cs.stage3.alice.core;

import edu.cmu.cs.stage3.alice.core.Behavior.Item;

public class SimulationExceptionWrapper extends SimulationException {
	/**
	 *
	 */
	private static final long serialVersionUID = 2038943987764257407L;
	private final Exception m_exception;

	public SimulationExceptionWrapper(final String detail, final java.util.Stack<Item> stack, final Element element,
			final Exception exception) {
		super(detail, stack, element);
		m_exception = exception;
	}

	public Exception getWrappedException() {
		return m_exception;
	}
}
