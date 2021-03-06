/*
 * Created on Jan 31, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.cmu.cs.stage3.alice.core;

/**
 * @author caitlin
 *
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Amount extends edu.cmu.cs.stage3.util.Enumerable {
	/**
	 *
	 */
	private static final long serialVersionUID = -8943793631674416144L;
	private final int m_Amount_ID;
	// public static final SpatialRelation IN = new SpatialRelation();
	// public static final SpatialRelation ON = new SpatialRelation();
	// public static final SpatialRelation AT = new SpatialRelation();

	private Amount(final int amount_id) {
		m_Amount_ID = amount_id;
	}

	public static final Amount TINY = new Amount(0);
	public static final Amount LITTLE = new Amount(1);
	public static final Amount NORMAL = new Amount(2);
	public static final Amount BIG = new Amount(3);
	public static final Amount HUGE = new Amount(4);

	public int getAmount(final double amount, final edu.cmu.cs.stage3.math.Box subjectBoundingBox,
			final edu.cmu.cs.stage3.math.Box objectBoundingBox) {
		return m_Amount_ID;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (o != null && o instanceof Amount) {
			final Amount amount = (Amount) o;
			return m_Amount_ID == amount.m_Amount_ID;
		} else {
			return false;
		}
	}

	public static Amount valueOf(final String s) {
		return (Amount) edu.cmu.cs.stage3.util.Enumerable.valueOf(s, Amount.class);
	}
}
