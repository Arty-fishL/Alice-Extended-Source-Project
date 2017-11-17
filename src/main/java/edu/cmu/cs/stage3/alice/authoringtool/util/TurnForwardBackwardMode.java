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

package edu.cmu.cs.stage3.alice.authoringtool.util;

/**
 * @author Jason Pratt
 */
public class TurnForwardBackwardMode extends DefaultMoveMode {
	public TurnForwardBackwardMode(final edu.cmu.cs.stage3.alice.authoringtool.MainUndoRedoStack undoRedoStack,
			final edu.cmu.cs.stage3.alice.core.Scheduler scheduler) {
		super(undoRedoStack, scheduler);
	}

	@Override
	public void mouseDragged(final java.awt.event.MouseEvent ev, final int dx, final int dy) {
		if (pickedTransformable != null) {
			helper.setTransformationRightNow(edu.cmu.cs.stage3.math.MathUtilities.createIdentityMatrix4d(), camera);
			helper.setPositionRightNow(zeroVec, pickedTransformable);
			pickedTransformable.rotateRightNow(edu.cmu.cs.stage3.math.MathUtilities.getXAxis(), -dy * .01, helper);
		}
	}
}