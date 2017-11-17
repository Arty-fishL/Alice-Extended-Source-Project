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

package edu.cmu.cs.stage3.alice.core.question.time;

public class DayOfWeekInMonth extends edu.cmu.cs.stage3.alice.core.question.IntegerQuestion {

	@Override
	public Object getValue() {
		final java.util.Calendar calendar = new java.util.GregorianCalendar();
		final java.util.Date date = new java.util.Date();
		calendar.setTime(date);
		return new Integer(calendar.get(java.util.Calendar.DAY_OF_WEEK_IN_MONTH));
	}
}