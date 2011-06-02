package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;


public class ABMPredicateTransferRelation extends PredicateTransferRelation {

  private BlockPartitioning partitioning = null;

  public ABMPredicateTransferRelation(PredicateCPA pCpa)
      throws InvalidConfigurationException {
    super(pCpa);
  }

  public void setPartitioning(BlockPartitioning pPartitioning) {
    checkState(partitioning == null);
    partitioning = pPartitioning;
  }

  @Override
  protected boolean isBlockEnd(CFANode pSuccLoc, PathFormula pPf) {
    return super.isBlockEnd(pSuccLoc, pPf) || partitioning.isCallNode(pSuccLoc) || partitioning.isReturnNode(pSuccLoc);
  }
}
