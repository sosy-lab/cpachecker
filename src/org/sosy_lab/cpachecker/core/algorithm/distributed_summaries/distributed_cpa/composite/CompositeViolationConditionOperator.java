// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

public class CompositeViolationConditionOperator implements ViolationConditionOperator {

  private final List<ConfigurableProgramAnalysis> analyses;

  public CompositeViolationConditionOperator(List<ConfigurableProgramAnalysis> pAnalyses) {
    analyses = pAnalyses;
  }

  @Override
  public Optional<AbstractState> computeViolationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    ImmutableList.Builder<AbstractState> states = ImmutableList.builder();
    for (ConfigurableProgramAnalysis cpa : analyses) {
      if (cpa instanceof DistributedConfigurableProgramAnalysis dcpa) {
        Optional<AbstractState> abstractState =
            dcpa.getViolationConditionOperator()
                .computeViolationCondition(pARGPath, pPreviousCondition);
        if (abstractState.isEmpty()) {
          return Optional.empty();
        }
        states.add(abstractState.orElseThrow());
      } else {
        CFANode location = AbstractStates.extractLocation(pARGPath.getFirstState());
        location = location == null ? CFANode.newDummyCFANode() : location;
        states.add(cpa.getInitialState(location, StateSpacePartition.getDefaultPartition()));
      }
    }
    return Optional.of(new CompositeState(states.build()));
  }
}
