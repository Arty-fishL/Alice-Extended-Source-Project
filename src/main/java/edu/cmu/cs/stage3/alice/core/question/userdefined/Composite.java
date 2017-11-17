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

package edu.cmu.cs.stage3.alice.core.question.userdefined;

import edu.cmu.cs.stage3.alice.core.property.ElementArrayProperty;

public abstract class Composite extends Component {
	public final ElementArrayProperty components = new ElementArrayProperty(this, "components", null, Component[].class);

	protected Object[] execute(ElementArrayProperty eap) {
		for (int i = 0; i < eap.size(); i++) {
			Component component = (Component) eap.get(i);
			Object[] value = component.execute();
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	@Override
	public Object[] execute() {
		return execute(components);
	}
}