/**
 * 
 */
package cpa.common.automaton;

/**
 * @author holzera
 *
 */
public class OrLabel<E> implements Label<E> {

  private Label<E> mLabel1;
  private Label<E> mLabel2;
  
  public OrLabel(Label<E> pLabel1, Label<E> pLabel2) {
    assert(pLabel1 != null);
    assert(pLabel2 != null);
    
    mLabel1 = pLabel1;
    mLabel2 = pLabel2;
  }
  
  /* (non-Javadoc)
   * @see cpa.scoperestrictionautomaton.label.Label#matches(java.lang.Object)
   */
  @Override
  public boolean matches(E pE) {
    assert(pE != null);
    
    return (mLabel1.matches(pE) || mLabel2.matches(pE));
  }
  
  @Override
  public boolean equals(Object pObject) {
    if (pObject == null) {
      return false;
    }
    
    if (!(pObject instanceof OrLabel<?>)) {
      return false;
    }
    
    OrLabel<?> lLabel = (OrLabel<?>)pObject;
    
    if ((mLabel1.equals(lLabel.mLabel1) && mLabel2.equals(mLabel2))
        || (mLabel1.equals(lLabel.mLabel2) && mLabel2.equals(mLabel1))) {
      return true;
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return mLabel1.hashCode() + mLabel2.hashCode();
  }
  
  @Override
  public String toString() {
    return "(" + mLabel1.toString() + " OR " + mLabel2.toString() + ")";
  }
}
