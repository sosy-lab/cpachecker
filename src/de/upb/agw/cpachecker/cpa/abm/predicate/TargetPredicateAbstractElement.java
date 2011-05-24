package de.upb.agw.cpachecker.cpa.abm.predicate;

import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * AbstractionElement that is <code>Targetable</code> and, in fact, always is a target. Used to notify outer analyzes
 * that a error location was found in an inner block.
 * @author dwonisch
 *
 */
public class TargetPredicateAbstractElement extends AbstractionElement
    implements Targetable {

  public TargetPredicateAbstractElement(PathFormula pPf, AbstractionFormula pA) {
    super(pPf, pA);
  }

  @Override
  public boolean isTarget() {
    return true;
  }
  
  @Override
  public String toString() {
    return "Target" + super.toString();
  }
}
