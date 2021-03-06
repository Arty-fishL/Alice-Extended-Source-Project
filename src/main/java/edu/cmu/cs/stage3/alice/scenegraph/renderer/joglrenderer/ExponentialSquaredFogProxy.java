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

package edu.cmu.cs.stage3.alice.scenegraph.renderer.joglrenderer;

import com.jogamp.opengl.GL2;

class ExponentialSquaredFogProxy extends FogProxy {
	private float m_density;

	@Override
	public void setup(final RenderContext context) {
		super.setup(context);
		context.gl.glFogi( GL2.GL_FOG_MODE, GL2.GL_EXP2 );
		context.gl.glFogf( GL2.GL_FOG_DENSITY, m_density );
	}

	@Override
	protected void changed(final edu.cmu.cs.stage3.alice.scenegraph.Property property, final Object value) {
		if (property == edu.cmu.cs.stage3.alice.scenegraph.ExponentialFog.DENSITY_PROPERTY) {
			m_density = ((Number) value).floatValue();
		} else {
			super.changed(property, value);
		}
	}
}
