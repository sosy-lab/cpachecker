package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

import de.upb.agw.cpachecker.cpa.abm.util.CachedSubtreeManager;

public class ABMPredicateTransferRelation extends PredicateTransferRelation {

  private CachedSubtreeManager csmgr = null;
  
  public ABMPredicateTransferRelation(PredicateCPA pCpa)
      throws InvalidConfigurationException {
    super(pCpa);
  }

  public void setCsmgr(CachedSubtreeManager pCsmgr) {
    checkState(csmgr == null);
    csmgr = pCsmgr;
  }
  
  @Override
  protected boolean isBlockEnd(CFANode pSuccLoc, PathFormula pPf) {
    return super.isBlockEnd(pSuccLoc, pPf) || csmgr.isCallNode(pSuccLoc) || csmgr.isReturnNode(pSuccLoc);    
  }
}
