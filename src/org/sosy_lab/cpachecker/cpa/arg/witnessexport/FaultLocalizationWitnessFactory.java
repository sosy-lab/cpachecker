// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;
import static org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.SINK_NODE_ID;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.WitnessTransitionExtractorAlgorithm.AppendixFaultInfo;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser.GraphMLDocumentData;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTracker.TracingInformation;
import org.sosy_lab.cpachecker.cpa.automaton.GraphMLTransition;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.automaton.VerificationTaskMetaData;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.Simplifier;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;

public class FaultLocalizationWitnessFactory extends WitnessFactory {

  // used for witness reduction with fault localization
  // ignored if empty
  private final Set<CFAEdge> edgesInFault = new HashSet<>();
  private final Multimap<ARGState, TracingInformation> edgeOriginalInformation =
      ArrayListMultimap.create();
  private final Set<GraphMLTransition> originalTransitions = new HashSet<>();

  FaultLocalizationWitnessFactory(
      WitnessOptions pOptions,
      CFA pCfa,
      LogManager pLogger,
      VerificationTaskMetaData pMetaData,
      ExpressionTreeFactory<Object> pFactory,
      Simplifier<Object> pSimplifier,
      @Nullable String pDefaultSourceFileName,
      WitnessType pGraphType,
      InvariantProvider pInvariantProvider) {
    super(
        pOptions,
        pCfa,
        pLogger,
        pMetaData,
        pFactory,
        pSimplifier,
        pDefaultSourceFileName,
        pGraphType,
        pInvariantProvider);
  }

  @Override
  protected TransitionCondition conformWithOriginalWitness(
      TransitionCondition pResult, CFAEdge pEdge, final Collection<ARGState> pFromStates) {
    checkArgument(
        pFromStates.size() == 1,
        "pFromStates cannot contain more than one element but has: " + pFromStates);
    Map<KeyDef, String> keys = new HashMap<>();
    ARGState fromState = Iterables.getOnlyElement(pFromStates);
    if (edgeOriginalInformation.containsKey(fromState)) {
      Collection<TracingInformation> tracingInformation = edgeOriginalInformation.get(fromState);
      Collection<TracingInformation> filteredTracingInformation =
          tracingInformation.stream()
              .filter(ti -> ti.getEdge().equals(pEdge))
              .collect(ImmutableSet.toImmutableSet());
      if (filteredTracingInformation.isEmpty()) {
        return pResult;
      }
      TracingInformation information = Iterables.getOnlyElement(filteredTracingInformation);
      GraphMLTransition transition = information.getTransition();
      if (transition == null) {
        return pResult;
      }
      transition.getExplicitAssumptionScope().ifPresent(s -> keys.put(KeyDef.ASSUMPTIONSCOPE, s));
      if (!transition.getAssumptions().isEmpty()) {
        keys.put(KeyDef.ASSUMPTION, Joiner.on("; ").join(transition.getAssumptions()));
      }
      transition
          .getExplicitAssumptionResultFunction()
          .ifPresent(s -> keys.put(KeyDef.ASSUMPTIONRESULTFUNCTION, s));
      transition.getFunctionEntry().ifPresent(s -> keys.put(KeyDef.FUNCTIONENTRY, s));
      transition.getFunctionExit().ifPresent(s -> keys.put(KeyDef.FUNCTIONEXIT, s));
      pResult = pResult.removeAndCopy(KeyDef.CONTROLCASE);
      if (transition.getTransition() != null) {
        Set<String> controlCases =
            GraphMLDocumentData.getDataOnNode(transition.getTransition(), KeyDef.CONTROLCASE);
        if (!controlCases.isEmpty()) {
          keys.put(KeyDef.CONTROLCASE, Iterables.getOnlyElement(controlCases));
        }
      }
      assert transition.getLineMatcherPredicate().orElseThrow().test(pEdge.getFileLocation());
      keys.put(
          KeyDef.STARTLINE,
          pResult
              .getMapping()
              .getOrDefault(
                  KeyDef.STARTLINE, pEdge.getFileLocation().getStartingLineInOrigin() + ""));
      keys.put(
          KeyDef.ENDLINE,
          pResult
              .getMapping()
              .getOrDefault(KeyDef.ENDLINE, pEdge.getFileLocation().getEndingLineInOrigin() + ""));
      if (pEdge.getPredecessor().isLoopStart()) {
        if (transition.entersLoopHead()) {
          keys.put(KeyDef.ENTERLOOPHEAD, "true");
        }
      }
    }
    TransitionCondition tc = pResult;
    for (KeyDef value : KeyDef.values()) {
      tc = tc.removeAndCopy(value);
    }
    for (Entry<KeyDef, String> keyDefStringEntry : keys.entrySet()) {
      tc = tc.putAndCopy(keyDefStringEntry.getKey(), keyDefStringEntry.getValue());
    }
    return tc;
  }

  @Override
  protected boolean isEdgeIrrelevant(Edge pEdge) {
    // always relevant if FL is deactivated
    if (edgesInFault.isEmpty()) {
      return true;
    }

    // remove empty transitions
    if (pEdge.getLabel().getMapping().isEmpty()) {
      return true;
    }

    Collection<CFAEdge> edges = edgeToCFAEdges.get(pEdge);
    assert edges.size() == 1;
    CFAEdge edge = Iterables.getOnlyElement(edges);

    boolean relevant = false;
    boolean sink = false;
    for (GraphMLTransition originalTransition : originalTransitions) {
      boolean lineMatch =
          originalTransition
              .getLineMatcherPredicate()
              .orElse(i -> false)
              .test(edge.getFileLocation());
      relevant |= lineMatch;

      sink =
          originalTransition.getTarget().isSinkState()
              && relevant
              && pEdge.getTarget().equalsIgnoreCase(SINK_NODE_ID);
      if (sink) {
        break;
      }
    }

    if (!relevant) {
      return true;
    }

    if (pEdge.getTarget().equals(SINK_NODE_ID) && !sink) {
      return true;
    }

    Set<String> importantNodes =
        transformedImmutableSetCopy(
            Multimaps.filterValues(edgeToCFAEdges, cfaEdge -> edgesInFault.contains(cfaEdge))
                .keySet(),
            e -> e.getSource());

    // not irrelevant if it is an edge to a sink node and the source node is part of the fault
    if (pEdge.getTarget().equals(SINK_NODE_ID)
        && importantNodes.contains(pEdge.getSource())
        && sink) {
      return false;
    }

    // not irrelevant if pEdge maps to
    if (edgesInFault.contains(edge)) {
      return false;
    }
    return true;
  }

  @Override
  public Witness produceWitness(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      final Predicate<? super ARGState> pIsCyclehead,
      final Optional<Function<? super ARGState, ExpressionTree<Object>>> cycleHeadToQuasiInvariant,
      Optional<CounterexampleInfo> pCounterExample,
      GraphBuilder pGraphBuilder)
      throws InterruptedException {
    edgesInFault.clear();
    edgeOriginalInformation.clear();
    originalTransitions.clear();
    // fault localization allows the reduction of the produced witness
    if (pCounterExample.isPresent()) {
      CounterexampleInfo cex = pCounterExample.orElseThrow();
      if (cex instanceof FaultLocalizationInfo) {
        FaultLocalizationInfo fInfo = (FaultLocalizationInfo) cex;
        Fault bestFault = fInfo.getRelevantEdgesForWitness(false);
        for (FaultInfo info : bestFault.getInfos()) {
          if (info instanceof AppendixFaultInfo) {
            edgeOriginalInformation.putAll(((AppendixFaultInfo) info).getInformation());
            originalTransitions.addAll(((AppendixFaultInfo) info).getTransitions());
          }
        }
        bestFault.forEach(fc -> edgesInFault.add(fc.correspondingEdge()));
        // valueMap = Multimaps.filterValues(valueMap, v -> edgesInFault.contains(v.getCFAEdge()));
      }
    }
    return super.produceWitness(
        pRootState,
        pIsRelevantState,
        pIsRelevantEdge,
        pIsCyclehead,
        cycleHeadToQuasiInvariant,
        pCounterExample,
        pGraphBuilder);
  }

  @Override
  protected TransitionCondition mergeMetaData(TransitionCondition pLabel, Edge pLeavingEdge) {
    Set<KeyDef> available = new HashSet<>(pLabel.getMapping().keySet());
    available.removeAll(pLeavingEdge.getLabel().getMapping().keySet());
    /*available.remove(KeyDef.STARTLINE);
    available.remove(KeyDef.ENDLINE);
    available.remove(KeyDef.ASSUMPTION);
    available.remove(KeyDef.ASSUMPTIONRESULTFUNCTION);
    available.remove(KeyDef.ASSUMPTIONSCOPE);*/
    pLabel = pLabel.putAllAndCopy(pLeavingEdge.getLabel());
    for (KeyDef keyDef : available) {
      pLabel = pLabel.removeAndCopy(keyDef);
    }
    return pLabel;
  }
}
