// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssARGPathGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssARGPathGraph.Incoming;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DistributedCallstackCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.BlockViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DistributedBlockCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DistributedFunctionPointerCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location.DistributedLocationCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.BackwardTransferViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.MergeableViolationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.pathrestriction.DecisionGraph;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Backwards dataflow over a frozen ARG, merging alternatives only when the other components are
 * equal.
 */
final class CompositeGraphViolationConditionOperator<T> {
  private record Component(
      int index,
      DistributedConfigurableProgramAnalysis cpa,
      BackwardTransferViolationConditionOperator transfer) {}

  private record Condition<T>(List<AbstractState> components, T condition, DecisionGraph witness) {}

  private final List<ConfigurableProgramAnalysis> analyses;
  private final List<Component> components = new ArrayList<>();
  private final int mergeableIndex;
  private final int blockIndex;
  private final MergeableViolationConditionOperator<T> mergeable;
  private final BlockViolationConditionOperator block;

  static boolean supports(List<ConfigurableProgramAnalysis> analyses) {
    int mergeable = 0;
    boolean block = false;
    for (ConfigurableProgramAnalysis cpa : analyses) {
      if (cpa instanceof DistributedConfigurableProgramAnalysis dcpa
          && dcpa.getViolationConditionOperator()
              instanceof MergeableViolationConditionOperator<?>) {
        mergeable++;
      } else if (cpa instanceof DistributedBlockCPA) {
        block = true;
      } else if (cpa instanceof DistributedConfigurableProgramAnalysis dcpa) {
        if (!(dcpa.getViolationConditionOperator()
                instanceof BackwardTransferViolationConditionOperator)
            || (!(cpa instanceof DistributedLocationCPA)
                && !(cpa instanceof DistributedCallstackCPA)
                && !(cpa instanceof DistributedFunctionPointerCPA))) {
          return false;
        }
      } else if (!(cpa instanceof ControlAutomatonCPA)) {
        return false;
      }
    }
    return mergeable == 1 && block;
  }

  static CompositeGraphViolationConditionOperator<?> create(
      List<ConfigurableProgramAnalysis> analyses) {
    for (int i = 0; i < analyses.size(); i++) {
      if (analyses.get(i) instanceof DistributedConfigurableProgramAnalysis dcpa
          && dcpa.getViolationConditionOperator()
              instanceof MergeableViolationConditionOperator<?> op) {
        return new CompositeGraphViolationConditionOperator<>(analyses, op, i);
      }
    }
    throw new IllegalArgumentException("No mergeable violation-condition operator");
  }

  private CompositeGraphViolationConditionOperator(
      List<ConfigurableProgramAnalysis> pAnalyses,
      MergeableViolationConditionOperator<T> pMergeable,
      int pMergeableIndex) {
    analyses = pAnalyses;
    mergeable = pMergeable;
    mergeableIndex = pMergeableIndex;
    int blk = -1;
    for (int i = 0; i < analyses.size(); i++) {
      ConfigurableProgramAnalysis cpa = analyses.get(i);
      if (i == mergeableIndex) {
        continue;
      }
      if (cpa instanceof DistributedBlockCPA) {
        blk = i;
      } else if (cpa instanceof DistributedConfigurableProgramAnalysis dcpa) {
        components.add(
            new Component(
                i,
                dcpa,
                (BackwardTransferViolationConditionOperator) dcpa.getViolationConditionOperator()));
      }
    }
    blockIndex = blk;
    block =
        (BlockViolationConditionOperator)
            ((DistributedConfigurableProgramAnalysis) analyses.get(blk))
                .getViolationConditionOperator();
  }

  List<AbstractState> compute(DssARGPathGraph graph, Optional<ARGState> previous)
      throws InterruptedException, CPATransferException, SolverException {
    Map<ARGState, Map<List<Map<String, String>>, Condition<T>>> values = new HashMap<>();
    List<AbstractState> initial = new ArrayList<>();
    for (Component component : components) {
      initial.add(
          component
              .transfer()
              .initialState(AbstractStates.extractLocation(graph.getLastState()), previous));
    }
    Map<List<Map<String, String>>, Condition<T>> target = new LinkedHashMap<>();
    target.put(
        key(initial),
        new Condition<>(initial, mergeable.initialCondition(previous), DecisionGraph.EMPTY));
    values.put(graph.getLastState(), target);
    for (ARGState node : graph.backwardOrder()) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      Map<List<Map<String, String>>, Condition<T>> atNode = values.get(node);
      if (atNode == null) {
        continue;
      }
      for (Incoming incoming : graph.incoming(node)) {
        Map<List<Map<String, String>>, Condition<T>> atParent =
            values.computeIfAbsent(incoming.parent(), unused -> new LinkedHashMap<>());
        for (Condition<T> condition : atNode.values()) {
          List<AbstractState> next = new ArrayList<>();
          boolean feasible = true;
          for (int i = 0; i < components.size(); i++) {
            Optional<AbstractState> state =
                components
                    .get(i)
                    .transfer()
                    .prepend(condition.components().get(i), incoming.edges());
            if (state.isEmpty()) {
              feasible = false;
              break;
            }
            next.add(state.orElseThrow());
          }
          if (!feasible) {
            continue;
          }
          T nextFormula = mergeable.prepend(condition.condition(), incoming.edges());
          DecisionGraph nextWitness = condition.witness().prepend(incoming.edges());
          List<Map<String, String>> key = key(next);
          Condition<T> old = atParent.get(key);
          if (old != null) {
            nextFormula = mergeable.union(old.condition(), nextFormula);
            nextWitness = DecisionGraph.union(ImmutableList.of(old.witness(), nextWitness));
          }
          atParent.put(key, new Condition<>(next, nextFormula, nextWitness));
        }
      }
    }
    List<AbstractState> result = new ArrayList<>();
    for (Condition<T> condition : values.getOrDefault(graph.getFirstState(), ImmutableMap.of()).values()) {
      Optional<AbstractState> formula =
          mergeable.finishGraph(graph.getFirstState(), condition.condition());
      if (formula.isEmpty()) {
        continue;
      }
      List<AbstractState> state = new ArrayList<>(Collections.nCopies(analyses.size(), null));
      state.set(mergeableIndex, formula.orElseThrow());
      state.set(
          blockIndex,
          block.withGraph(graph.getFirstState(), previous, condition.witness()).orElseThrow());
      for (int i = 0; i < components.size(); i++) {
        state.set(components.get(i).index(), condition.components().get(i));
      }
      for (int i = 0; i < analyses.size(); i++) {
        if (state.get(i) == null) {
          state.set(
              i,
              analyses
                  .get(i)
                  .getInitialState(
                      AbstractStates.extractLocation(graph.getFirstState()),
                      StateSpacePartition.getDefaultPartition()));
        }
      }
      result.add(new CompositeState(ImmutableList.copyOf(state)));
    }
    return result;
  }

  private List<Map<String, String>> key(List<AbstractState> states) {
    List<Map<String, String>> key = new ArrayList<>();
    for (int i = 0; i < states.size(); i++) {
      key.add(components.get(i).cpa().getSerializeOperator().serialize(states.get(i)));
    }
    return key;
  }
}
