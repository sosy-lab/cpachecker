/**
 * 
 */
package cpa.common.automaton;

/**
 * @author holzera
 *
 * This label always returns true.
 */
public class TrueLabel<E> implements Label<E> {

  /* (non-Javadoc)
   * @see cpa.scoperestrictionautomaton.label.Label#matches(java.lang.Object)
   */
  @Override
  public boolean matches(E pE) {
    return true;
  }

  @Override
  public boolean equals(Object pObject) {
    if (pObject == null) {
      return false;
    }
    
    return (pObject instanceof TrueLabel<?>);
  }
  
  @Override
  public int hashCode() {
    return 1;
  }
  
  @Override
  public String toString() {
    return "true";
  }
}
