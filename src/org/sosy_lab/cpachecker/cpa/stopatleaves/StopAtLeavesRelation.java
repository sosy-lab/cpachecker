// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.stopatleaves;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * @see org.sosy_lab.cpachecker.cpa.targetreachability.TargetReachabilityTransferRelation
 */
public class StopAtLeavesRelation extends SingleEdgeTransferRelation {
  private List<CFANode> leaves;

  public StopAtLeavesRelation(List<CFANode> pLeaves) {
    leaves = pLeaves;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    var leaf = cfaEdge.getSuccessor();
    if (leaves.contains(leaf)) {
      return Collections.singleton(StopAtLeavesState.STOP);
    }

    return Collections.singleton(StopAtLeavesState.CONTINUE);
  }

  public void setLeaves(List<CFANode> pLeaves) {
    leaves = pLeaves;
  }
}
