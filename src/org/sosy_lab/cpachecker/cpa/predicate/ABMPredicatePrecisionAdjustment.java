package org.sosy_lab.cpachecker.cpa.predicate;

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.abm.ABMTransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates.RelevantPredicatesComputer;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;



/**
 * <code>PredicatePrecisionAdjustment</code> that only takes the relevant predicates into account when
 * computing an abstraction.
 * @author dwonisch
 *
 */
public class ABMPredicatePrecisionAdjustment extends PredicatePrecisionAdjustment {
  
  private ABMTransferRelation transfer;
  
  private final RelevantPredicatesComputer relevantPredicatesComputer;
  
  public ABMPredicatePrecisionAdjustment(ABMPredicateCPA pCpa) {
    super(pCpa);
    relevantPredicatesComputer = pCpa.getRelevantPredicatesComputer();
  }

  
  public void setTransferRelation(ABMTransferRelation transferRelation) {
    this.transfer = transferRelation;
  }
  
  private Collection<AbstractionPredicate> getReducedPredicateSet(Collection<AbstractionPredicate> pPreds, CFANode node) {
    if(!transfer.getCachedSubtreeManager().isCallNode(node) && !transfer.getCachedSubtreeManager().isReturnNode(node)) {
      return pPreds;
    }
    return relevantPredicatesComputer.getRelevantPredicates(transfer.getCurrentCachedSubtree(), pPreds);   
  }
  
  @Override
  protected AbstractionFormula computeAbstraction(AbstractionFormula pAbstractionFormula, PathFormula pPathFormula, Collection<AbstractionPredicate> pPreds, CFANode node) {
    return super.computeAbstraction(pAbstractionFormula, pPathFormula, getReducedPredicateSet(pPreds, node), node);    
  } 
}
