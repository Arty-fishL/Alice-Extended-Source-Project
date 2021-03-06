/*
 * Created on Aug 19, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package edu.cmu.cs.stage3.alice.core.response;

import edu.cmu.cs.stage3.alice.core.Element;
import edu.cmu.cs.stage3.alice.core.ReferenceFrame;
import edu.cmu.cs.stage3.alice.core.Transformable;
import edu.cmu.cs.stage3.math.Matrix33;

/**
 * @author caitlink
 *
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class StraightenAnimation extends TransformAnimation {

	public class RuntimeStraightenAnimation extends RuntimeTransformAnimation {
		java.util.Vector<Matrix33> bodyPartInitialOrientations = null;
		java.util.Vector<Transformable> bodyParts = null;

		edu.cmu.cs.stage3.math.Matrix33 normalOrientation = new edu.cmu.cs.stage3.math.Matrix33();

		@Override
		public void prologue(final double t) {
			super.prologue(t);
			bodyPartInitialOrientations = new java.util.Vector<Matrix33>();
			bodyParts = new java.util.Vector<Transformable>();
			normalOrientation.setForwardUpGuide(new javax.vecmath.Vector3d(0, 0, 1),
					new javax.vecmath.Vector3d(0, 1, 0));

			if (m_subject != null) {
				if (!(m_subject.getParent() instanceof edu.cmu.cs.stage3.alice.core.World)) {
					addBodyPart(m_subject); // we want to straighten top level
											// object too.
				}
				findChildren(m_subject);
			}

		}

		@Override
		public void update(final double t) {
			for (int i = 0; i < bodyPartInitialOrientations.size(); i++) {
				setOrientation(bodyParts.elementAt(i),
						bodyPartInitialOrientations.elementAt(i), normalOrientation,
						getPortion(t));
			}

			super.update(t);
		}

		private void findChildren(final edu.cmu.cs.stage3.alice.core.Transformable part) {
			final edu.cmu.cs.stage3.alice.core.Element[] kids = part
					.getChildren(edu.cmu.cs.stage3.alice.core.Transformable.class);
			for (final Element kid : kids) {
				final edu.cmu.cs.stage3.alice.core.Transformable trans = (edu.cmu.cs.stage3.alice.core.Transformable) kid;
				addBodyPart(trans);

				if (trans.getChildCount() > 0) {
					findChildren(trans);
				}
			}
		}

		private void addBodyPart(final edu.cmu.cs.stage3.alice.core.Transformable partToAdd) {
			bodyPartInitialOrientations
					.addElement(partToAdd.getOrientationAsAxes((ReferenceFrame) partToAdd.getParent()));
			bodyParts.addElement(partToAdd);
		}

		private void setOrientation(final edu.cmu.cs.stage3.alice.core.Transformable part,
				final edu.cmu.cs.stage3.math.Matrix33 initialOrient, final edu.cmu.cs.stage3.math.Matrix33 finalOrient,
				final double portion) {
			// System.out.println(portion);
			final edu.cmu.cs.stage3.math.Matrix33 currentOrient = edu.cmu.cs.stage3.math.Matrix33
					.interpolate(initialOrient, finalOrient, portion);
			if (part != null) {

				part.setOrientationRightNow(currentOrient, (ReferenceFrame) part.getParent());
			}
		}
	}

}
