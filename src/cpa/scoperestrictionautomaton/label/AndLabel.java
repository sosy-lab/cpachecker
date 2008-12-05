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
    if (pLabel1 == null) {
      throw new IllegalArgumentException("First label is null!");
    }
    
    if (pLabel2 == null) {
      throw new IllegalArgumentException("Second label is null!");
    }
    
    mLabel1 = pLabel1;
    mLabel2 = pLabel2;
  }
  
  /* (non-Javadoc)
   * @see cpa.scoperestrictionautomaton.label.Label#matches(java.lang.Object)
   */
  @Override
  public boolean matches(E pE) {
    if (pE == null) {
      throw new IllegalArgumentException("pE is null!");
    }
    
    return (mLabel1.matches(pE) && mLabel2.matches(pE));
  }

}
