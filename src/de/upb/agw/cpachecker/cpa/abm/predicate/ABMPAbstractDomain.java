package de.upb.agw.cpachecker.cpa.abm.predicate;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractDomain;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * <code>PredicateAbstractDomain</code> that ensures that two <code>TargetPredicateAbstractElement</code> are never related to each other
 * with respect to the <code>isLessOrEqual</code> relation.
 * @see TargetPredicateAbstractElement
 * @author dwonisch
 *
 */
public class ABMPAbstractDomain extends PredicateAbstractDomain {

  public ABMPAbstractDomain(PredicateCPA pCpa)
      throws InvalidConfigurationException {
    super(pCpa);
  }
  
  @Override
  public boolean isLessOrEqual(AbstractElement element1,
                                       AbstractElement element2) throws CPAException {
    if(element1 instanceof TargetPredicateAbstractElement || element2 instanceof TargetPredicateAbstractElement) {
      return false;
    }
    return super.isLessOrEqual(element1, element2);
  }

}
