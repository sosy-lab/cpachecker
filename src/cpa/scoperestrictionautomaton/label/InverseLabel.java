/**
 * 
 */
package cpa.scoperestrictionautomaton.label;

/**
 * @author holzera
 *
 */
public class InverseLabel<E> implements Label<E> {
	
	private Label<E> mLabel;
	
	public InverseLabel(Label<E> pLabel) {
		assert(pLabel != null);
	  
		mLabel = pLabel;
	}

	@Override
	public boolean matches(E pE) {
		return !mLabel.matches(pE);
	}

}
