/**
 * 
 */
package cpa.scoperestrictionautomaton.label;

/**
 * @author holzera
 *
 */
public class NegationLabel<E> implements Label<E> {
	
	private Label<E> mLabel;
	
	public NegationLabel(Label<E> pLabel) {
		assert(pLabel != null);
	  
		mLabel = pLabel;
	}

	@Override
	public boolean matches(E pE) {
		return !mLabel.matches(pE);
	}

}
