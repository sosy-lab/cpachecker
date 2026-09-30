// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssARGPathGraph;
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

  /** Merges the paths of a graph, or {@code null} if a component cannot merge conditions. */
  private final @Nullable CompositeGraphViolationConditionOperator graphOperator;

  public CompositeViolationConditionOperator(List<ConfigurableProgramAnalysis> pAnalyses) {
    analyses = pAnalyses;
    graphOperator = CompositeGraphViolationConditionOperator.of(pAnalyses).orElse(null);
  }

  @Override
  public List<AbstractState> computeViolationConditions(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    if (!(pARGPath instanceof DssARGPathGraph graph)) {
      return computeForPath(pARGPath, pPreviousCondition);
    }
    if (graphOperator != null) {
      return graphOperator.compute(graph, pPreviousCondition);
    }
    ImmutableList.Builder<AbstractState> conditions = ImmutableList.builder();
    for (ARGPath path : graph.paths()) {
      conditions.addAll(computeForPath(path, pPreviousCondition));
    }
    return conditions.build();
  }

  /** Combines the conditions of all components for a single path. */
  private List<AbstractState> computeForPath(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    List<List<AbstractState>> components = new ArrayList<>();
    for (ConfigurableProgramAnalysis cpa : analyses) {
      if (cpa instanceof DistributedConfigurableProgramAnalysis dcpa) {
        List<AbstractState> conditions =
            dcpa.getViolationConditionOperator()
                .computeViolationConditions(pARGPath, pPreviousCondition);
        if (conditions.isEmpty()) {
          return ImmutableList.of();
        }
        components.add(conditions);
      } else {
        CFANode location = AbstractStates.extractLocation(pARGPath.getFirstState());
        location = location == null ? CFANode.newDummyCFANode() : location;
        components.add(
            ImmutableList.of(
                cpa.getInitialState(location, StateSpacePartition.getDefaultPartition())));
      }
    }
    return transformedImmutableListCopy(Lists.cartesianProduct(components), CompositeState::new);
  }
}
