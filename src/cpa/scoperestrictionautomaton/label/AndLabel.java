/**
 * 
 */
package cpa.scoperestrictionautomaton.label;

/**
 * @author holzera
 *
 */
public class AndLabel<E> implements Label<E> {

  private Label<E> mLabel1;
  private Label<E> mLabel2;
  
  public AndLabel(Label<E> pLabel1, Label<E> pLabel2) {
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
    
    return (mLabel1.matches(pE) && mLabel2.matches(pE));
  }

}
