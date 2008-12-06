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
  public String toString() {
    return "true";
  }
}
