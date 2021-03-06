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

package edu.cmu.cs.stage3.alice.core.bubble;

import java.awt.geom.Rectangle2D.Double;

public class BubbleManager implements edu.cmu.cs.stage3.alice.scenegraph.renderer.event.RenderTargetListener {
	private Bubble[] m_bubbles;
	private edu.cmu.cs.stage3.alice.scenegraph.renderer.RenderTarget m_renderTarget;

	public void setBubbles(final Bubble[] bubbles) {
		m_bubbles = bubbles;
		if (m_renderTarget != null) {
			m_renderTarget.markDirty();
		}
	}

	private boolean layoutBubbles(final edu.cmu.cs.stage3.alice.scenegraph.renderer.RenderTarget rt) {
		boolean isPaintRequired = false;
		final java.util.Vector<Double> rects = new java.util.Vector<Double>();
		for (final Bubble bubbleI : m_bubbles) {
			;
			if (bubbleI.isShowing()) {
				bubbleI.calculateBounds(rt);
				bubbleI.calculateOrigin(rt);
				final java.awt.Point pixelOffset = bubbleI.getPixelOffset();
				if (pixelOffset != null) {
					final java.awt.geom.Rectangle2D rect = bubbleI.getTotalBound();
					if (rect != null) {
						rects.addElement(new java.awt.geom.Rectangle2D.Double(pixelOffset.x + rect.getX(),
								pixelOffset.y + rect.getY(), rect.getWidth(), rect.getHeight()));
					}
				}
				isPaintRequired = true;
			}
		}
		if (isPaintRequired) {
			final edu.cmu.cs.stage3.alice.scenegraph.Camera[] sgCameras = rt.getCameras();
			if (sgCameras.length > 0) {
				final edu.cmu.cs.stage3.alice.scenegraph.Camera sgCamera = sgCameras[0];
				final java.awt.Rectangle actualViewport = rt.getActualViewport(sgCamera);
				for (final Bubble bubbleI : m_bubbles) {
					;
					if (bubbleI.isShowing()) {
						final java.awt.Point pixelOffset = bubbleI.getPixelOffset();
						final java.awt.Point origin = bubbleI.getOrigin();
						/* Unused ??
						double half;
						if (origin.x > actualViewport.width / 2) {
							half = 0.5;
						} else {
							half = 0.0;
						}
						*/
						if (pixelOffset == null) {
							final java.awt.geom.Rectangle2D rect = bubbleI.getTotalBound();
							// int x = (int)( ( ( Math.random()* 0.4 ) + 0.05 +
							// half ) * ( actualViewport.width - rect.getWidth()
							// ) );
							// int y = (int)( ( ( Math.random()* 0.6 ) + 0.05 )
							// * ( actualViewport.height - rect.getHeight() ) );
							double x = origin.x;
							double y = origin.y - 48;
							if (rect != null) {
								y -= rect.getHeight();
								final double VIEWPORT_PAD = 64;
								x = Math.min(Math.max(x, VIEWPORT_PAD),
										actualViewport.width - rect.getWidth() - VIEWPORT_PAD);
								y = Math.min(Math.max(y, VIEWPORT_PAD),
										actualViewport.height - rect.getHeight() - VIEWPORT_PAD);
							}
							bubbleI.setPixelOffset(new java.awt.Point((int) x, (int) y));
						}
					}
				}
			}
		}
		return isPaintRequired;
	}

	@Override
	public void cleared(final edu.cmu.cs.stage3.alice.scenegraph.renderer.event.RenderTargetEvent ev) {
	}

	@Override
	public void rendered(final edu.cmu.cs.stage3.alice.scenegraph.renderer.event.RenderTargetEvent ev) {
		m_renderTarget = ev.getRenderTarget();
		if (layoutBubbles(m_renderTarget)) {
			final java.awt.Graphics g = m_renderTarget.getOffscreenGraphics();
			try {
				for (final Bubble bubbleI : m_bubbles) {
					;
					bubbleI.paint(g);
				}
			} finally {
				g.dispose();
			}
		}
	}
}