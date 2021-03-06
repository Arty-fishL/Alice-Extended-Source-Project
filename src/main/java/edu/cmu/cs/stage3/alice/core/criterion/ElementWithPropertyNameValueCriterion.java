/*
 * Created on Jun 3, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package edu.cmu.cs.stage3.alice.core.criterion;

/**
 * @author caitlin
 *
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ElementWithPropertyNameValueCriterion implements edu.cmu.cs.stage3.util.Criterion {
	private String propertyName = null;
	private Object propertyValue = null;
	private boolean returnEqual = true;

	public ElementWithPropertyNameValueCriterion(final String propertyName, final Object value) {
		this(propertyName, value, true);
	}

	public ElementWithPropertyNameValueCriterion(final String propertyName, final Object value,
			final boolean returnEqual) {
		this.propertyName = propertyName;
		propertyValue = value;
		this.returnEqual = returnEqual;
	}

	@Override
	public boolean accept(final Object o) {
		if (o instanceof edu.cmu.cs.stage3.alice.core.Element) {
			final edu.cmu.cs.stage3.alice.core.Element element = (edu.cmu.cs.stage3.alice.core.Element) o;

			final edu.cmu.cs.stage3.alice.core.Property property = element.getPropertyNamed(propertyName);
			if (property != null) {
				if (property.getValue().equals(propertyValue)) {
					if (returnEqual) {
						return true;
					} else {
						return false;
					}
				} else {
					if (returnEqual) {
						return false;
					} else {
						return true;
					}
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
}
