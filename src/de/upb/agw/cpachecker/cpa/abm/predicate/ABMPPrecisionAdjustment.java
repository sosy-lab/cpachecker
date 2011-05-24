package de.upb.agw.cpachecker.cpa.abm.predicate;

import java.util.Collection;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecisionAdjustment;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;


/**
 * <code>PredicatePrecisionAdjustment</code> that only takes the relevant predicates into account when
 * computing an abstraction.
 * @author dwonisch
 *
 */
public class ABMPPrecisionAdjustment extends PredicatePrecisionAdjustment {
  private ABMPTransferRelation transfer;
  
  public ABMPPrecisionAdjustment(ABMPredicateCPA pCpa) {
    super(pCpa);
  }

  
  protected void setPredicateTransferRelation(ABMPTransferRelation transferRelation) {
    this.transfer = transferRelation;
  }
  
  @Override
  public Triple<AbstractElement, Precision, Action> prec(AbstractElement pElement, Precision pPrecision, UnmodifiableReachedSet pElements) {
    Triple<AbstractElement, Precision, Action> result = super.prec(pElement, pPrecision, pElements);

    if((pElement instanceof Targetable) && ((Targetable)pElement).isTarget()) {
      return new Triple<AbstractElement, Precision, Action>(result.getFirst(), result.getSecond(), Action.BREAK);
    }
    else {
      return result;
    }
  }
  
  private Collection<AbstractionPredicate> getReducedPredicateSet(Collection<AbstractionPredicate> pPreds, CFANode node) {
    if(!transfer.getCachedSubtreeManager().isCallNode(node) && !transfer.getCachedSubtreeManager().isReturnNode(node)) {
      return pPreds;
    }
   // getLogger().log(Level.FINER, "Reducing predicate set for abstraction for node " + node);
    Collection<AbstractionPredicate> result = transfer.getRelevantPredicatesComputer().getRelevantPredicates(transfer.getCurrentCachedSubtree(), pPreds);   
   // getLogger().log(Level.FINEST, "Normal predicate set: " +  pPreds);
   // getLogger().log(Level.FINEST, "Reduced predicate set: " +  result);
    return result;    
  }
  
  @Override
  protected AbstractionFormula computeAbstraction(AbstractionFormula pAbstractionFormula, PathFormula pPathFormula, Collection<AbstractionPredicate> pPreds, CFANode node) {
    return super.computeAbstraction(pAbstractionFormula, pPathFormula, getReducedPredicateSet(pPreds, node), node);    
  } 
}
