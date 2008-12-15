/**
 * 
 */
package cpa.common.automaton;

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
	
	@Override
	public boolean equals(Object pObject) {
	  if (pObject == null) {
	    return false;
	  }
	  
	  if (!(pObject instanceof NegationLabel<?>)) {
	    return false;
	  }
	  
	  NegationLabel<?> lLabel = (NegationLabel<?>)pObject;
	  
	  return mLabel.equals(lLabel);
	}
	
	@Override
	public int hashCode() {
	  return mLabel.hashCode();
	}

	@Override
	public String toString() {
	  return "NOT(" + mLabel + ")";
	}
}
