// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.stopatleaves;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * @see org.sosy_lab.cpachecker.cpa.targetreachability.TargetReachabilityCPA
 */
public class StopAtLeavesCPA extends AbstractCPA {
  StopAtLeavesRelation relation;

  private StopAtLeavesCPA() {
    super("join", "sep", new FlatLatticeDomain(StopAtLeavesState.CONTINUE), null /* never used */);

    relation = new StopAtLeavesRelation(ImmutableList.of());
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(StopAtLeavesCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {

    return StopAtLeavesState.CONTINUE;
  }

  public void setLeaves(List<CFANode> pLeaves) {
    relation.setLeaves(pLeaves);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return relation;
  }
}
