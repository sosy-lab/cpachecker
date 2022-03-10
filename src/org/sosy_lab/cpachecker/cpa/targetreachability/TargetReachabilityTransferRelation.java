// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.targetreachability;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class TargetReachabilityTransferRelation extends SingleEdgeTransferRelation {

  private final ImmutableSet<CFANode> targetReachableFrom;

  TargetReachabilityTransferRelation(ImmutableSet<CFANode> pTargetReachableFrom) {
    targetReachableFrom = pTargetReachableFrom;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (targetReachableFrom.contains(cfaEdge.getSuccessor())) {
      return Collections.singleton(ReachabilityState.RELEVANT_TO_TARGET);
    } else {
      return Collections.singleton(ReachabilityState.IRRELEVANT_TO_TARGET);
    }
  }
}
