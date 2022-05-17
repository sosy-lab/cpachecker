// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.AssumptionCollectorAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.PostCondition;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.PreCondition;
import org.sosy_lab.cpachecker.core.algorithm.giageneration.GIAGenerator.GIAGeneratorOptions;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessAssumptionFilter;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;

public class GIAARGGenerator {

  private final LogManager logger;
  private final GIAGeneratorOptions optinons;
  private final MachineModel machineModel;
  private final Configuration config;
  private final ConfigurableProgramAnalysis cpa;

  private Function<ARGState, Optional<CounterexampleInfo>> getCounterexampleInfo;

  public GIAARGGenerator(
      LogManager pLogger,
      GIAGeneratorOptions pOptions,
      MachineModel pMachineModel,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig)
      throws InvalidConfigurationException {

    this.logger = pLogger;
    this.optinons = pOptions;
    this.machineModel = pMachineModel;
    this.cpa = pCpa;
    this.config = pConfig;

    AssumptionToEdgeAllocator assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(config, logger, machineModel);
    getCounterexampleInfo =
        s -> ARGUtils.tryGetOrCreateCounterexampleInformation(s, cpa, assumptionToEdgeAllocator);
  }

  int produceGIA4ARG(
      Appendable pOutput, UnmodifiableReachedSet pReached, GIAGeneratorOptions pOptinons)
      throws IOException, InterruptedException {
    final AbstractState firstState = pReached.getFirstState();
    if (!(firstState instanceof ARGState)) {
      pOutput.append("Cannot dump assumption as automaton if ARGCPA is not used.");
    }



    final ARGState pArgRoot = (ARGState) pReached.getFirstState();

    // Compute all target states: States marked as target
    Set<ARGState> targetStates = getAllTargetStates(pReached);
    // Compute all non.target states: States that (1) are explored, (2) are not covered,  (3) do not
    // have a successor (4) no target states
    Set<ARGState> nonTargetStates = getAllNonTargetStates(pReached);
    Set<ARGState> unknownStates = new HashSet<>();

    // Determine, which of the nodes are in F_unknown:
    if (!optinons.isOverApproxAnalysis()) {
      unknownStates.addAll(nonTargetStates);
      // as this analysis is not allowed to add states to F_NT, clear it
      nonTargetStates = new HashSet<>();
    }
    if (!optinons.isUnderApproxAnalysis()) {
      unknownStates.addAll(targetStates);
      // as this analysis is not allowed to add states to F_T, clear it
      targetStates = new HashSet<>();
    }

    // Determine all paths that are relevant
    final Set<ARGState> finalStates =
        Sets.union(nonTargetStates, Sets.union(targetStates, unknownStates));
//    BiPredicate<ARGState, ARGState> isRelevantEdge = getRelevantEdges(finalStates);
//    Predicate<ARGState> relevantState = Predicates.in(finalStates);

    Multimap<ARGState, CFAEdgeWithAssumptions> edgesWithAssumptions = ImmutableListMultimap.of();
//    if (optinons.isGenGIA4Refinement()) {
      for (ARGState errorState : targetStates) {
        CounterexampleInfo pCounterExample = getCounterexampleInfo.apply(errorState).orElse(null);
        storeInterpolantsAsAssumptions(
            Optional.ofNullable(pCounterExample), edgesWithAssumptions);
      }
//    }

    Set<GIAARGStateEdge> relevantEdges = new HashSet<>();
//        buildGraph(
//            pArgRoot,
//            relevantState,
//            isRelevantEdge,
//            edgesWithAssumptions,
//            WitnessFactory.collectReachableEdges(
//                pArgRoot, ARGState::getChildren, relevantState, isRelevantEdge));
    // TODO: Adapt implementation
    //    // remove unnecessary edges leading to sink
    //    relevantEdges = removeUnnecessarySinkEdges(relevantEdges);
    //
    //    // Merge nodes with empty or repeated edges
    //    int sizeBeforeMerging = relevantEdges.size();
    //    mergeRepeatedEdges(relevantEdges);
    //    int sizeAfterMerging = relevantEdges.size();
    //    logger.logf(
    //        Level.ALL,
    //        "Witness graph shrinked from %s edges to %s edges when merging edges.",
    //        sizeBeforeMerging,
    //        sizeAfterMerging);
    //
    //    // merge redundant sibling edges leading to the sink together, if possible
    //    mergeRedundantSinkEdges();

    // scan reached set for all relevant states
    // Relevant state: Function enter, branching, loopHead or error states
    // We start at the root and iterate through the ARG
    //    Set<GIAARGStateEdge> relevantEdges = new HashSet<>();
    List<ARGState> toProcess = new ArrayList<>();
    toProcess.add(pArgRoot);
    List<ARGState> processed = new ArrayList<>();

    logger.log(
        Level.INFO,
        "Final states found are "
            + String.join(
                ",",
            finalStates.stream()
                    .map(a -> Integer.toString(a.getStateId()))
                    .collect(ImmutableList.toImmutableList())));

    while (!toProcess.isEmpty()) {
      ARGState state = toProcess.remove(0);
      logger.logf(
          Level.INFO,
          "Taking %s from the list, processed %s, toProcess %s",
          state.getStateId(),
          processed.stream().map(a -> a.getStateId()).collect(ImmutableList.toImmutableList()),
          toProcess);

      for (ARGState child : state.getChildren()) {
        addAllRelevantEdges(
            state, child, finalStates, toProcess, relevantEdges, processed, edgesWithAssumptions);
      }
      processed.add(state);
    }
    logger.log(
        Level.WARNING,
        relevantEdges.stream().map(e -> e.toString()).collect(ImmutableList.toImmutableList()));

    // Now, a short cleaning is applied:
    // All states leading to the error state are replaced by the error state directly
    //    Map<ARGState, Optional<ARGState>> statesToReplaceToReplacement = new HashMap<>();
    //    Set<GIAARGStateEdge> toRemove = new HashSet<>();
    //    relevantEdges.stream()
    //        .filter(e -> e.getTarget().isPresent() &&
    // finalStates.contains(e.getTarget().orElseThrow()))
    //        .forEach(
    //            e -> {
    //              statesToReplaceToReplacement.put(e.getSource(), e.getTarget());
    //              toRemove.add(e);
    //            });
    //    HashSet<GIAARGStateEdge> toAdd = new HashSet<>();
    //    for(GIAARGStateEdge edge : relevantEdges){
    //      if (edge.getTarget().isPresent() &&
    // statesToReplaceToReplacement.containsKey(edge.getTarget().orElseThrow())){
    //        toRemove.add(edge);
    //        Optional<ARGState> replacement =
    // statesToReplaceToReplacement.get(edge.getTarget().orElseThrow());
    //        if (replacement.isPresent())
    //        toAdd.add(new GIAARGStateEdge(edge.source,replacement.orElseThrow(),
    // edge.getEdge()));
    //      }
    //    }
    //    relevantEdges.removeAll(toRemove);
    //    relevantEdges.addAll(toAdd);
    return writeGIAForViolationWitness(pOutput, pArgRoot, relevantEdges, false, targetStates, nonTargetStates, unknownStates);
  }

  // TODO: Addept implementation
   // // // Merge sibling edges (with the same source) that lead to the sink if possible. */
  //  private Set<GIAARGStateEdge> mergeRepeatedEdges(Set<GIAARGStateEdge> pRelevantEdges) {
  //
  //    Multimap<ARGState, GIAARGStateEdge> edgesLeaving = HashMultimap.create();
  //     pRelevantEdges.forEach(e -> edgesLeaving.put(e.source, e));
  //
  //      for (Collection<GIAARGStateEdge> leavingEdgesCollection : edgesLeaving.asMap().values()) {
  //        // We only need to do something if we have siblings
  //        if (leavingEdgesCollection.size() > 1) {
  //
  //          // Determine all siblings that go to the sink
  //          List<GIAARGStateEdge> toSink =
  //              leavingEdgesCollection.stream()
  //                  .filter(e -> e.getTarget().isEmpty())
  //                  .collect(Collectors.toCollection(ArrayList::new));
  //
  //          // If multiple siblings go to the sink, we want to try to merge them
  //          if (toSink.size() > 1) {
  //
  //            ListIterator<GIAARGStateEdge> edgeToSinkIterator = toSink.listIterator();
  //            Set<GIAARGStateEdge> removed = Sets.newIdentityHashSet();
  //            while (edgeToSinkIterator.hasNext()) {
  //              GIAARGStateEdge edge = edgeToSinkIterator.next();
  //
  //              // If the edge has already been marked as removed, throw it out
  //              if (removed.contains(edge)) {
  //                edgeToSinkIterator.remove();
  //                continue;
  //              }
  //
  //              // Search a viable merge partner for the current edge
  //              Optional<GIAARGStateEdge> merged = Optional.empty();
  //              GIAARGStateEdge other = null;
  //              for (GIAARGStateEdge otherEdge : toSink) {
  //                if (edge != otherEdge && !removed.contains(otherEdge)) {
  //                  merged = edge.tryMerge(otherEdge);
  //                  if (merged.isPresent()) {
  //                    other = otherEdge;
  //                    break;
  //                  }
  //                }
  //              }
  //
  //              // If we determined a merge partner, apply the merge result
  //              if (merged.isPresent()) {
  //                // Remove the two merge partners
  //                removeEdge(edge);
  //                removeEdge(other);
  //
  //                // Directly remove the old version of the current edge
  //                // and mark the other edge as removed
  //                edgeToSinkIterator.remove();
  //                removed.add(other);
  //
  //                // Add the merged edge to the graph
  //                putEdge(merged.orElseThrow());
  //                edgeToCFAEdges.putAll(merged.orElseThrow(), edgeToCFAEdges.get(edge));
  //                edgeToCFAEdges.putAll(merged.orElseThrow(), edgeToCFAEdges.get(other));
  //                edgeToCFAEdges.removeAll(edge);
  //                edgeToCFAEdges.removeAll(other);
  //
  //                // Add the merged edge to the set of siblings to consider it for further merges
  //                edgeToSinkIterator.add(merged.orElseThrow());
  //                edgeToSinkIterator.previous();
  //              }
  //            }
  //          }
  //        }
  //      }
  //
  //    return pRelevantEdges;
  //  }
  //
  //  /**
  //   * Remove edges that lead to the sink but have a sibling edge that has the same label.
  //   *
  //   * <p>We additionally remove irrelevant edges. This is needed for concurrency witnesses at
  //   * thread-creation.
  //   *
  //   * @param pRelevantEdges
  //   * @return
  //   */
  //  private Set<GIAARGStateEdge> removeUnnecessarySinkEdges(Set<GIAARGStateEdge> pRelevantEdges) {
  //    final Collection<GIAARGStateEdge> toRemove = Sets.newIdentityHashSet();
  //
  //    for (GIAARGStateEdge edge : pRelevantEdges) {
  //      if (edge.getTarget().isEmpty()) {
  //        for (GIAARGStateEdge otherEdge : pRelevantEdges) {
  //          // ignore the edge itself, as well as already handled edges.
  //          if (edge != otherEdge && !toRemove.contains(otherEdge)) {
  //            // remove edges with either identical labels or irrelevant edge-transition
  //            if (edge.getEdge().equals(otherEdge.getEdge()) ){
  //                //TODO: implement this check
  ////                || isEdgeIrrelevant(edge)) {
  //              toRemove.add(edge);
  //              break;
  //            }
  //          }
  //        }
  //      }
  //    }
  //    for (GIAARGStateEdge edge : toRemove) {
  //      boolean removed = removeEdge(edge);
  //      assert removed;
  //    }
  //    return pRelevantEdges;
  //  }
  //  /**
  //   * this predicate marks intermediate edges that do not contain relevant information and can
  //   * therefore be shortcut.
  //   * @param pEdge
  //   */
  //  private boolean isEdgeIrrelevant(GIAARGStateEdge pEdge) {
  //    final ARGState source = pEdge.getSource();
  //    final Optional<ARGState> target = pEdge.getTarget();
  //    final TransitionCondition label = pEdge.getLabel();
  //
  //    if (isIrrelevantNode(target) || isEdgeIrrelevantByFaultLocalization(pEdge)) {
  //      return true;
  //    }
  //
  //    final ExpressionTree<Object> sourceInv = stateQuasiInvariants.get(source);
  //    final ExpressionTree<Object> targetInv = stateQuasiInvariants.get(target);
  //    if (sourceInv != null && targetInv != null && !sourceInv.equals(targetInv)) {
  //      return false;
  //    }
  //
  //    if (label.getMapping().isEmpty()) {
  //      return true;
  //    }
  //
  //    if (source.equals(target)) {
  //      return false;
  //    }
  //
  //    // An edge is never irrelevant if there are conflicting scopes
  //    ExpressionTree<Object> sourceTree = getStateInvariant(source);
  //    if (sourceTree != null) {
  //      String sourceScope = stateScopes.get(source);
  //      String targetScope = stateScopes.get(target);
  //      if (sourceScope != null && targetScope != null && !sourceScope.equals(targetScope)) {
  //        return false;
  //      }
  //    }
  //
  //    // An edge is irrelevant if it is the only leaving edge of a
  //    // node and it is empty or all its non-assumption contents
  //    // are summarized by a preceding edge
  //    boolean summarizedByPreceedingEdge =
  //        Iterables.any(
  //            enteringEdges.get(source),
  //            pPrecedingEdge -> pPrecedingEdge.getLabel().summarizes(label));
  //
  //    if ((!label.hasTransitionRestrictions()
  //        || summarizedByPreceedingEdge
  //        || (label.getMapping().size() == 1
  //        && label.getMapping().containsKey(KeyDef.FUNCTIONEXIT)))
  //        && (leavingEdges.get(source).size() == 1)) {
  //      return true;
  //    }
  //
  //    if (Iterables.all(
  //        leavingEdges.get(source),
  //        pLeavingEdge -> !pLeavingEdge.getLabel().hasTransitionRestrictions())) {
  //      return true;
  //    }
  //
  //    // Some keys are not sufficient to limit the explored state space,
  //    // i.e., by cutting off branching control flow.
  //    // They are only a weak hint on the analysis direction.
  //    // We remove edges that only contain such insufficient keys.
  //    if (witnessOptions.removeInsufficientEdges()
  //        && INSUFFICIENT_KEYS.containsAll(label.getMapping().keySet())) {
  //      return true;
  //    }
  //
  //    return false;
  //  }
  //
  //  /**
  //   * this predicate marks intermediate nodes that do not contain relevant information and can
  //   * therefore be shortcut.
  //   */
  //  private boolean isIrrelevantNode(Optional<ARGState>  pNode) {
  //    if (pNode.isEmpty()) return false;
  //    @Nullable PredicateAbstractState state =
  //        AbstractStates.extractStateByType(pNode.orElseThrow(), PredicateAbstractState.class);
  //    if (state != null &&
  //        !ExpressionTrees.getTrue().equals(sate))) {
  //      return false;
  //    }
  //    if (hasFlagsOrProperties(pNode)) {
  //      return false;
  //    }
  //    if (enteringEdges.get(pNode).isEmpty()) {
  //      return false;
  //    }
  //    for (Edge edge : enteringEdges.get(pNode)) {
  //      if (!edge.getLabel().getMapping().isEmpty()) {
  //        return false;
  //      }
  //    }
  //    return true;
  //  }
  //
  //  private boolean hasFlagsOrProperties(ARGState s) {
  //    EnumSet<NodeFlag> sourceNodeFlags = EnumSet.noneOf(NodeFlag.class);
  //    String sourceStateNodeId = pGraphBuilder.getId(s);
  //    if (sourceStateNodeId.equals(entryStateNodeId)) {
  //      sourceNodeFlags.add(NodeFlag.ISENTRY);
  //    }
  //    if (this.cfa.getLoopStructure(). {
  //      sourceNodeFlags.add(NodeFlag.ISCYCLEHEAD);
  //      if (cycleHeadToQuasiInvariant.isPresent()) {
  //      }
  //    }
  //    sourceNodeFlags.addAll(extractNodeFlags(s));
  //    nodeFlags.putAll(sourceStateNodeId, sourceNodeFlags);
  //    if (graphType == WitnessType.VIOLATION_WITNESS) {
  //      violatedProperties.putAll(sourceStateNodeId, extractViolatedProperties(s));
  //    }
  //  }

  /** Adds the counterexample infos to pEdgesWithAssumptions */
  private void storeInterpolantsAsAssumptions(
      Optional<CounterexampleInfo> pCounterExample,
      Multimap<ARGState, CFAEdgeWithAssumptions> pEdgesWithAssumptions) {
    Collection<CFAEdge> edgesInFault = new HashSet<>();

    if (pCounterExample.isPresent()) {
      CounterexampleInfo cex = pCounterExample.orElseThrow();
      if (cex.isPreciseCounterExample()) {
        Multimap<ARGState, CFAEdgeWithAssumptions> valueMap =
            Multimaps.transformValues(
                pCounterExample.orElseThrow().getExactVariableValues(),
                WitnessAssumptionFilter::filterRelevantAssumptions);

        if (cex instanceof FaultLocalizationInfo) {
          FaultLocalizationInfo fInfo = (FaultLocalizationInfo) cex;
          List<Fault> faults = fInfo.getRankedList();
          if (!faults.isEmpty()) {
            Fault bestFault = faults.get(0);

            FluentIterable.from(bestFault)
                .transform(fc -> fc.correspondingEdge())
                .copyInto(edgesInFault);
            if (fInfo.getPostcondition().isPresent()) {
              PostCondition postCondition = fInfo.getPostcondition().orElseThrow();
              edgesInFault.addAll(postCondition.getIgnoredEdges());
              edgesInFault.addAll(postCondition.responsibleEdges());
            }
            if (fInfo.getPrecondition().isPresent()) {
              PreCondition preCondition = fInfo.getPrecondition().orElseThrow();
              edgesInFault.addAll(preCondition.responsibleEdges());
            }
            valueMap = Multimaps.filterValues(valueMap, v -> edgesInFault.contains(v.getCFAEdge()));
          }
        }
        pEdgesWithAssumptions.putAll(valueMap);
      }
    }
  }

  //  private Multimap<ARGState, CFAEdgeWithAssumptions> storeInterpolantsAsAssumptions(
  //      UnmodifiableReachedSet pReached) {
  //    Multimap<ARGState, CFAEdgeWithAssumptions> multimap = HashMultimap.create();
  //    ImmutableSet<ARGState> statesWithInvariants =
  //        pReached.asCollection().stream()
  //            .filter(
  //                s -> {
  //                  @Nullable PredicateAbstractState pState =
  //                      AbstractStates.extractStateByType(s, PredicateAbstractState.class);
  //                  if (pState == null) return false;
  //                  if (!pState.isAbstractionState()) return false;
  //                  // Remove all non-abstract states and abstract states with true abstraction
  //                  // formula
  //                  return !formulaManager
  //                      .getBooleanFormulaManager()
  //                      .isTrue(pState.getAbstractionFormula().asFormula());
  //                })
  //            .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
  //            .collect(ImmutableSet.toImmutableSet());
  //    for (AbstractState state : pReached) {
  //      ARGState e = AbstractStates.extractStateByType(state, ARGState.class);
  //    for (ARGState successor : e.getChildren()){
  //      if (statesWithInvariants.contains(successor)) {
  //
  //        AbstractionFormula assumption =
  //                AbstractStates.extractStateByType(successor, PredicateAbstractState.class)
  //                    .getAbstractionFormula();
  //        CFAEdge edge = e.getEdgeToChild(successor);
  //        try {
  //          new FormulaToCVisitor(formulaManager)
  //          ExpressionTree<CExpression> assumptionTree = assumption.asFormula()
  //          T t = assumptionTree.accept(new ToCExpressionVisitor(machineModel, logger));
  //          CExpression cExpr = null;
  //          CExpressionStatement assumptionAsExpr =
  //              new CExpressionStatement(edge.getFileLocation(), cExpr);
  //          multimap.put(e, new CFAEdgeWithAssumptions(
  //              edge, ImmutableList.of(assumptionAsExpr), ""));
  //        } catch (InterruptedException pE) {
  //            logger.logf(
  //                Level.WARNING,
  //                "Cannot export the assumption %s  at state %s due to %s",
  //                assumption,
  //                state,
  //                Throwables.getTrimmedStackTrace(pE));
  //        }
  //
  //
  //      }}
  //    }
  //
  //    return multimap;
  //  }
//
//  /**
//   * Code is taken from {@link org.sosy_lab.cpachecker.cpa.arg.witnessexport.GraphBuilder#ARG_PATH}
//   */
//  private Set<GIAARGStateEdge> buildGraph(
//      ARGState pRootState,
//      Predicate<? super ARGState> pPathStates,
//      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
//      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
//      Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges) {
//    Set<GIAARGStateEdge> edges = new HashSet<>();
//
//    int multiEdgeCount = 0;
//    for (Pair<ARGState, Iterable<ARGState>> argEdges : pARGEdges) {
//      ARGState s = argEdges.getFirst();
//      if (!s.equals(pRootState)
//          && s.getParents().stream().noneMatch(p -> pIsRelevantEdge.test(p, s))) {
//        continue;
//      }
//      // Process child states
//      for (ARGState child : argEdges.getSecond()) {
//        List<CFAEdge> allEdgeToNextState = s.getEdgesToChild(child);
//        Optional<String> multiEdgeIndexForSource = Optional.empty();
//        Optional<String> multiEdgeIndexForTarget;
//        CFAEdge edgeToNextState;
//
//        if (allEdgeToNextState.isEmpty()) {
//          edgeToNextState = null; // TODO no next state, what to do?
//
//        } else if (allEdgeToNextState.size() == 1) {
//          edgeToNextState = Iterables.getOnlyElement(allEdgeToNextState);
//
//          // this is a dynamic multi edge
//        } else {
//          // The successor state might have several incoming MultiEdges.
//          // In this case the state names like ARG<successor>_0 would occur
//          // several times.
//          // So we add this counter to the state names to make them unique.
//          multiEdgeCount++;
//
//          // inner part (without last edge)
//          for (int i = 0; i < allEdgeToNextState.size() - 1; i++) {
//            CFAEdge innerEdge = allEdgeToNextState.get(i);
//            // String pseudoStateId = getId(child, i, multiEdgeCount);
//            multiEdgeIndexForTarget = Optional.of(String.format("_%d_%d", i, multiEdgeCount));
//
//            assert !(innerEdge instanceof AssumeEdge);
//
//            // TODO: Think about the case that some assumptions are present for multi edges
//            //            boolean isAssumptionAvailableForEdge =
//            //                Iterables.any(pValueMap.get(s), a ->
//            // a.getCFAEdge().equals(innerEdge));
//            //            Optional<Collection<ARGState>> absentStates =
//            //                isAssumptionAvailableForEdge
//            //                    ? Optional.of(Collections.singleton(s))
//            //                    : Optional.empty();
//            //            pEdgeAppender.appendNewEdge(
//            //                prevStateId,
//            //                pseudoStateId,
//            //                innerEdge,
//            //                absentStates,
//            //                pValueMap,
//            //                CFAEdgeWithAdditionalInfo.of(innerEdge));
//            edges.add(
//                new GIAARGStateEdge(
//                    s,
//                    multiEdgeIndexForSource,
//                    child,
//                    multiEdgeIndexForTarget,
//                    innerEdge,
//                    Optional.empty()));
//
//            multiEdgeIndexForSource = multiEdgeIndexForTarget;
//          }
//
//          // last edge connecting it with the real successor
//          edgeToNextState = allEdgeToNextState.get(allEdgeToNextState.size() - 1);
//        }
//
//        // Only proceed with this state if the path states contain the child
//        if (pPathStates.apply(child) && pIsRelevantEdge.test(s, child)) {
//          // Child belongs to the path!
//          if (multiEdgeIndexForSource.isPresent()) {
//            edges.add(
//                new GIAARGStateEdge(
//                    s,
//                    multiEdgeIndexForSource,
//                    child,
//                    Optional.empty(),
//                    edgeToNextState,
//                    Optional.empty(),
//                    getAssumptionForEdge(s, edgeToNextState, pValueMap)));
//          } else {
//            edges.add(
//                new GIAARGStateEdge(
//                    s,
//                    child,
//                    edgeToNextState,
//                    Optional.empty(),
//                    getAssumptionForEdge(s, edgeToNextState, pValueMap)));
//          }
//
//          // For branchings, it is important to have both branches explicitly in the witness
//          if (edgeToNextState instanceof AssumeEdge) {
//            AssumeEdge assumeEdge = (AssumeEdge) edgeToNextState;
//            AssumeEdge siblingEdge = CFAUtils.getComplimentaryAssumeEdge(assumeEdge);
//            boolean addArtificialSinkEdge = true;
//            for (ARGState sibling : s.getChildren()) {
//              if (!Objects.equals(sibling, child)
//                  && siblingEdge.equals(s.getEdgeToChild(sibling))
//                  && pIsRelevantEdge.test(s, sibling)) {
//                addArtificialSinkEdge = false;
//                break;
//              }
//            }
//            if (addArtificialSinkEdge) {
//              // Child does not belong to the path --> add a branch to the SINK node!
//              if (multiEdgeIndexForSource.isPresent()) {
//                edges.add(
//                    new GIAARGStateEdge(s, multiEdgeIndexForSource, siblingEdge, Optional.empty()));
//              } else {
//                edges.add(new GIAARGStateEdge(s, siblingEdge));
//              }
//            }
//          }
//        } else {
//          // Child does not belong to the path --> add a branch to the SINK node!
//
//          if (multiEdgeIndexForSource.isPresent()) {
//            edges.add(
//                new GIAARGStateEdge(s, multiEdgeIndexForSource, edgeToNextState, Optional.empty()));
//          } else {
//            edges.add(new GIAARGStateEdge(s, edgeToNextState));
//          }
//        }
//      }
//    }
//    return edges;
//  }

  private Optional<String> getAssumptionForEdge(
      ARGState pS, CFAEdge pEdgeToNextState, Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap) {
    if (pValueMap.get(pS).stream().anyMatch(e -> e.getCFAEdge().equals(pEdgeToNextState))) {
      CFAEdgeWithAssumptions ass =
          pValueMap.get(pS).stream()
              .filter(e -> e.getCFAEdge().equals(pEdgeToNextState))
              .findFirst()
              .orElseThrow();
      return Optional.of(ass.getAsCode());
    }
    return Optional.empty();
  }

  private BiPredicate<ARGState, ARGState> getRelevantEdges(Set<ARGState> pFinalStates) {
    Set<Pair<ARGState, ARGState>> relevantEdges = new HashSet<>();
    for (ARGState finalState : pFinalStates) {
      ImmutableSet<ARGState> nodes = ARGUtils.getAllStatesOnPathsTo(finalState);
      for (ARGState node : nodes) {
        for (ARGState successors : node.getChildren()) {
          if (nodes.contains(successors)) {
            relevantEdges.add(Pair.of(node, successors));
          }
        }
      }
    }
    return BiPredicates.pairIn(relevantEdges);
  }

  /**
   * Compute all non.target states: States that (1) are explored, (2) are not covered, (3) do not
   * have a successor (4) no target states
   *
   * @param pReached the reached set to filter
   * @return the set of states matching the 4 conditions
   */
  private Set<ARGState> getAllNonTargetStates(UnmodifiableReachedSet pReached) {
    return pReached.stream()
        .map(as -> AbstractStates.extractStateByType(as, ARGState.class))
        .filter(
            s -> !s.isTarget() && s.wasExpanded() && !s.isCovered() && s.getChildren().isEmpty())
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Traverse allpath from parent to its child node, until a relevant edge is found
   *
   * @param pParent the parent
   * @param pChild its child to check for beeing relevant (i.e. any edge on the path leading to that
   *     edge )
   * @param pFinalStates the target states
   * @param pToProcess the states to process
   * @param pRelevantEdges the relevent ades to add the result to
   * @param pProcessed the edges already processed
   * @param pEdgesWithAssumptions the assumptions from the preicse ce
   */
  private void addAllRelevantEdges(
      ARGState pParent,
      ARGState pChild,
      Set<ARGState> pFinalStates,
      List<ARGState> pToProcess,
      Set<GIAARGStateEdge> pRelevantEdges,
      List<ARGState> pProcessed,
      Multimap<ARGState, CFAEdgeWithAssumptions> pEdgesWithAssumptions) {

    // Check, if the pChild is relevant
    Optional<Pair<CFAEdge, Optional<ARGState>>> relevantEdge =
        gedEdgeIfIsRelevant(pChild, pParent, pFinalStates, pEdgesWithAssumptions);
    boolean edgesAdded = false;
    if (relevantEdge.isPresent()) {
      // now, create a new edge.
      Pair<CFAEdge, Optional<ARGState>> pair = relevantEdge.orElseThrow();
      if (pair.getSecond().isEmpty()) {
        pRelevantEdges.add(new GIAARGStateEdge(pParent, pair.getFirst()));
      } else {
        // Only check for assumptions for edges not leading to sink nodes
        // TODO Validate that this handling is in fact correct and the ARG node is the target node
        Optional<CFAEdgeWithAssumptions> optAddAssumption =
            pEdgesWithAssumptions.get(pair.getSecond().orElseThrow()).stream()
                .filter(e -> e.getCFAEdge().equals(pair.getFirst()))
                .findFirst();
        Optional<String> additionalAssumption =
            optAddAssumption.isPresent()
                ? Optional.of(optAddAssumption.get().getAsCode())
                : Optional.empty();
        // FIXME: Add Handling of Interpolants
        pRelevantEdges.add(
            new GIAARGStateEdge(
                pParent,
                pair.getSecond().orElseThrow(),
                pair.getFirst(),
                Optional.empty(),
                additionalAssumption));
        if (!pProcessed.contains(pChild) && !pFinalStates.contains(pair.getSecond().get())) {
          logger.logf(Level.INFO, "Adding %s", pChild.getStateId());
          pToProcess.add(pChild);
        }
      }
      edgesAdded = true;
    } else {

      List<CFAEdge> pahtToChild = Lists.newArrayList(pParent.getFirstPathToChild(pChild));
      List<GIAARGStateEdge> edgesToAdd = new ArrayList<>();
      ARGState lastNodeUsed = pChild;
      // Check if there are any edges on the path that are relevant and if so, create for
      // each relevant edge an edge
      if (pahtToChild.size() > 1) {
        logger.logf(Level.INFO, "Processing a Multi-node");
        while (!pahtToChild.isEmpty()) {
          CFAEdge currentEdge = pahtToChild.get(pahtToChild.size() - 1);
          pahtToChild.remove(currentEdge);
          if (isRelevantEdge(currentEdge)) {
            ARGState intermediateState = new ARGState(null, null);
            edgesToAdd.add(new GIAARGStateEdge(intermediateState, lastNodeUsed, currentEdge));
            //            lastNodeUsed.addParent(intermediateState);
            lastNodeUsed = intermediateState;
          }
        }
        // remove the lastNodeused and replace it by parent
        ARGState finalLastNodeUsed = lastNodeUsed;
        ImmutableList<GIAARGStateEdge> edgesToUpdate =
            edgesToAdd.stream()
                .filter(e -> e.getSource().equals(finalLastNodeUsed))
                .collect(ImmutableList.toImmutableList());
        for (GIAARGStateEdge edge : edgesToUpdate) {
          edgesToAdd.remove(edge);
          if (edge.getTarget().isPresent())
            edgesToAdd.add(
                new GIAARGStateEdge(pParent, edge.getTarget().orElseThrow(), edge.getEdge()));
        }
        if (!edgesToAdd.isEmpty()) {
          pRelevantEdges.addAll(edgesToAdd);
          if (!pProcessed.contains(pChild)) {
            logger.logf(Level.INFO, "Adding %s", pChild.getStateId());
            pToProcess.add(pChild);
          }
          edgesAdded = true;
        }
      }
    }
    if (!edgesAdded) {
      for (ARGState grandChild : pChild.getChildren()) {
        logger.logf(
            Level.INFO,
            "No match found for parent %s and child %s, coninue with grandchild %s",
            pParent.getStateId(),
            pChild.getStateId(),
            grandChild.getStateId());
        // As there might be cycles with not-rpcoessed nodes, only continue with nodes that are
        // already expanded
        if (grandChild.wasExpanded()) {
          addAllRelevantEdges(
              pParent,
              grandChild,
              pFinalStates,
              pToProcess,
              pRelevantEdges,
              pProcessed,
              pEdgesWithAssumptions);
        } else {
          // Add an edge to qtemp if needed
          Optional<GIAARGStateEdge> additionalEdgeToQtemp =
              getEdgeForNotExpandedNode(pParent, grandChild, pFinalStates, pEdgesWithAssumptions);
          if (additionalEdgeToQtemp.isPresent()) {
            pRelevantEdges.add(additionalEdgeToQtemp.orElseThrow());
          }
        }
      }
    }
  }

  /**
   * An Edge is relevant, if: <br>
   * 1. The child is a loophead <br>
   * 2. The child is the direct successor of the parent and the edge is an assumeEdge or function
   * call edge <br>
   * 3. The child is a final state <br>
   * 4 Alternative: THe child is a {@link FunctionEntryNode}<br>
   * 5. The child node is a final state or has a grandchild node that is a final state <br>
   * 6. It contains an assumption
   *
   * @param pChild the child
   * @param pParent the parent
   * @param pFinalStates the list of final states
   * @param pEdgesWithAssumptions additional assumptinos for precise CEs
   * @return the last edge on the path from parent to child, if the edge is relevant, otherwise an
   *     empty optional and the final node to use for the edge
   */
  private Optional<Pair<CFAEdge, Optional<ARGState>>> gedEdgeIfIsRelevant(
      ARGState pChild,
      ARGState pParent,
      Set<ARGState> pFinalStates,
      Multimap<ARGState, CFAEdgeWithAssumptions> pEdgesWithAssumptions) {
    List<CFAEdge> pathToChild = pParent.getFirstPathToChild(pChild);

    // CAse 1 is irrelevant
    // Case 1:
    //    if (child != null && child.isLoopStart()) {
    //      if (pathToChild.isEmpty()) {
    //        return Optional.empty(); // To avoid NPE
    //      } else {
    //        return Optional.ofNullable(pathToChild.get(pathToChild.size() - 1));
    //      }
    //    }

    if (pathToChild.isEmpty()) {
      return Optional.empty();
    }

    final CFAEdge lastEdge = pathToChild.get(pathToChild.size() - 1);
    // Case 2:
    boolean case2 = lastEdge instanceof AssumeEdge;
    // Case 3:
    boolean case3 = pFinalStates.contains(pChild);
    // Case 4:
    boolean case4 = AbstractStates.extractLocation(pChild) instanceof FunctionEntryNode;
    // This is an old option!
    //        lastEdge instanceof BlankEdge
    //            &&
    // lastEdge.getDescription().contains(GIAGenerator.DESC_OF_DUMMY_FUNC_START_EDGE));
    // Case 5:
    boolean case5a = pFinalStates.contains(pChild);
    boolean case5b = pChild.getChildren().stream().anyMatch(gc -> pFinalStates.contains(gc));

    boolean case6 =
        pEdgesWithAssumptions.values().stream().anyMatch(e -> e.getCFAEdge().equals(lastEdge));

    // If the pChild cannot reach the error state, do not add it to the toProcessed
    // and let the edge goto the qTemp State (as not relevant for the path)
    // If it can reach the error state, add the edge and the child to the toProcess
    if (case2 || case3 || case4 || case5a || case6) {
      if (case5a || canReachFinalState(pChild, pFinalStates)) {
        return Optional.of(Pair.of(lastEdge, Optional.of(pChild)));
      } else {
        return Optional.of(Pair.of(lastEdge, Optional.empty()));
      }
    } else if (case5b) {
      // If case 5 applies, we want to return the error state
      if (!canReachFinalState(pChild, pFinalStates)) {
        return Optional.of(Pair.of(lastEdge, Optional.empty()));
      } else {
        return Optional.of(
            Pair.of(
                lastEdge,
                Optional.of(
                    pChild.getChildren().stream()
                        .filter(gc -> pFinalStates.contains(gc))
                        .findFirst()
                        .orElseThrow())));
      }
    }

    return Optional.empty();
  }

  private Optional<GIAARGStateEdge> getEdgeForNotExpandedNode(
      ARGState pParent,
      ARGState pGrandChild,
      Set<ARGState> pTargetStates,
      Multimap<ARGState, CFAEdgeWithAssumptions> pEdgesWithAssumptions) {
    Optional<Pair<CFAEdge, Optional<ARGState>>> relevantEdge =
        gedEdgeIfIsRelevant(pGrandChild, pParent, pTargetStates, pEdgesWithAssumptions);
    if (relevantEdge.isPresent()) {
      return Optional.of(new GIAARGStateEdge(pParent, relevantEdge.orElseThrow().getFirst()));
    } else {
      return Optional.empty();
    }
  }

  private boolean isRelevantEdge(CFAEdge pCFAEdge) {
    return pCFAEdge instanceof FunctionCallEdge
        || (pCFAEdge instanceof BlankEdge
            && pCFAEdge.getDescription().contains(GIAGenerator.DESC_OF_DUMMY_FUNC_START_EDGE));
  }

  /**
   * Return true, if there is a children state that is the a final state
   *
   * @param pState the state to check the children for
   * @param pFinalStates the set of final states
   * @return Return true, if there is a children state that is the a final state
   */
  private boolean canReachFinalState(ARGState pState, Set<ARGState> pFinalStates) {
    for (ARGState child : pState.getChildren()) {
      if (pFinalStates.contains(child)) return true;
      else {
        if (canReachFinalState(child, pFinalStates)) {
          return true;
        } else {
          continue;
        }
      }
    }
    return false;
  }

//  private int produceGIA4WitnessTransformation(
//      Appendable output,
//      UnmodifiableReachedSet reached,
//      Optional<AutomatonState> automatonRootState)
//      throws IOException {
//    AutomatonState rootState = automatonRootState.orElseThrow();
//
//    Set<GIAAutomatonStateEdge> edgesToAdd = new HashSet<>();
//
//    // Next, filter the reached set fo all states, that have a different automaton
//    // state compared to their predecessors, as these are the states that need to be stored in
//    // the  GIA
//
//    for (AbstractState s : reached.asCollection()) {
//      Optional<AutomatonState> automatonStateOpt = GIAGenerator.getWitnessAutomatonState(s);
//      if (automatonStateOpt.isEmpty()) {
//        logger.log(
//            Level.WARNING,
//            String.format("Cannot export state %s, as no AutomatonState is present", s));
//        continue;
//      }
//      AutomatonState currentAutomatonState = automatonStateOpt.orElseThrow();
//      @Nullable ARGState argState = AbstractStates.extractStateByType(s, ARGState.class);
//      if (Objects.isNull(argState)) {
//        logger.log(
//            Level.WARNING, String.format("Cannot export state %s, as it is not an ARG State", s));
//        continue;
//      }
//
//      Set<Pair<ARGState, AutomatonState>> parentsWithOtherAutomatonState =
//          Sets.newConcurrentHashSet();
//
//      for (ARGState parent : argState.getParents()) {
//        Optional<AutomatonState> parentAutomatonState =
//            GIAGenerator.getWitnessAutomatonState(parent);
//        // If parent node has a automaton state and this is differnt to the one of the
//        // child, add the child to statesWithNewAutomatonState
//        if (parentAutomatonState.isPresent()
//            && !parentAutomatonState.orElseThrow().equals(currentAutomatonState)
//            && // automaton state is not already present in  parentsWithOtherAutomatonState
//            parentsWithOtherAutomatonState.stream()
//                .map(pair -> pair.getSecond())
//                .noneMatch(state -> parentAutomatonState.orElseThrow().equals(state))) {
//          parentsWithOtherAutomatonState.add(Pair.of(parent, parentAutomatonState.orElseThrow()));
//        }
//      }
//      if (!parentsWithOtherAutomatonState.isEmpty()) {
//        for (Pair<ARGState, AutomatonState> parentPair : parentsWithOtherAutomatonState) {
//          // Create the edge
//          CFAEdge edge = GIAGenerator.getEdge(parentPair, argState);
//          edgesToAdd.add(
//              new GIAAutomatonStateEdge(parentPair.getSecond(), currentAutomatonState, edge));
//          // Check, if the parent node has any other outgoing edges, they have to be added aswell
//          for (CFAEdge otherEdge :
//              CFAUtils.leavingEdges(AbstractStates.extractLocation(parentPair.getFirst()))) {
//            if (!otherEdge.equals(edge)) {
//              edgesToAdd.add(new GIAAutomatonStateEdge(parentPair.getSecond(), otherEdge));
//            }
//          }
//        }
//      }
//    }
//
//    logger.log(
//        Level.INFO, edgesToAdd.stream().map(e -> e.toString()).collect(Collectors.joining("\n")));
//
//    return writeGIAForViolationWitness(
//        output, rootState, edgesToAdd, optinons.isAutomatonIgnoreAssumptions());
//  }

//  /**
//   * Create an GIA for the given set of edges Beneth printing the edges, each node gets a self-loop
//   * and a node to the temp-location
//   *
//   * @param sb the appendable to print to
//   * @param rootState the root state of the automaton
//   * @param edgesToAdd the edges between states to add
//   * @throws IOException if the file cannot be accessed or does not exist
//   */
//  private int writeGIAForViolationWitness(
//      Appendable sb,
//      AutomatonState rootState,
//      Set<GIAAutomatonStateEdge> edgesToAdd,
//      boolean ignoreAssumptions)
//      throws IOException {
//    int numProducedStates = 0;
//    sb.append(GIAGenerator.AUTOMATON_HEADER);
//
//    String actionOnFinalEdges = "";
//
//    GIAGenerator.storeInitialNode(sb, edgesToAdd.isEmpty(), GIAGenerator.getName(rootState));
//    if (ignoreAssumptions) {
//      sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_TEMP_STATE));
//    } else {
//      sb.append(
//          String.format(
//              "    TRUE -> ASSUME {false} GOTO %s;\n\n", GIAGenerator.NAME_OF_TEMP_STATE));
//    }
//
//    sb.append(String.format("STATE %s :\n", GIAGenerator.NAME_OF_ERROR_STATE));
//    if (ignoreAssumptions) {
//      sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_ERROR_STATE));
//    } else {
//      sb.append(
//          String.format(
//              "    TRUE -> ASSUME {true} GOTO %s;\n\n", GIAGenerator.NAME_OF_ERROR_STATE));
//    }
//
//    // Fill the map to be able to iterate over the nodes
//    Map<AutomatonState, Set<GIAAutomatonStateEdge>> nodesToEdges = new HashMap<>();
//    edgesToAdd.forEach(
//        e -> {
//          if (nodesToEdges.containsKey(e.getSource())) {
//            nodesToEdges.get(e.getSource()).add(e);
//          } else {
//            nodesToEdges.put(e.getSource(), Sets.newHashSet(e));
//          }
//        });
//
//    for (final AutomatonState currentState :
//        nodesToEdges.keySet().stream()
//            .sorted(Comparator.comparing(GIAGenerator::getName))
//            .collect(ImmutableList.toImmutableList())) {
//
//      sb.append(String.format("STATE USEALL %s :\n", GIAGenerator.getName(currentState)));
//      numProducedStates++;
//
//      for (GIAAutomatonStateEdge edge : nodesToEdges.get(currentState)) {
//
//        sb.append("    MATCH \"");
//        AssumptionCollectorAlgorithm.escape(GIAGenerator.getEdgeString(edge.getEdge()), sb);
//        sb.append("\" -> ");
//        sb.append(String.format("GOTO %s", edge.getTargetName()));
//        sb.append(";\n");
//      }
//      if (!currentState.isTarget()) {
//        sb.append(
//            String.format(
//                "    MATCH OTHERWISE -> " + actionOnFinalEdges + "GOTO %s;\n\n",
//                GIAGenerator.getName(currentState)));
//        //        sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
//      }
//    }
//    sb.append("END AUTOMATON\n");
//
//    return numProducedStates;
//  }

  /**
   * Create an GIA for the given set of edges Beneth printing the edges, each node gets a self-loop
   * and a node to the temp-location
   *
   * @param sb the appendable to print to
   * @param rootState the root state of the automaton
   * @param edgesToAdd the edges between states to add
   * @param pTargetStates the target states
   * @param pNonTargetStates the non target states
   * @param pUnknownStates the unknwon states
   * @throws IOException if the file cannot be accessed or does not exist
   */
  private int writeGIAForViolationWitness(
      Appendable sb,
      ARGState rootState,
      Set<GIAARGStateEdge> edgesToAdd,
      boolean ignoreAssumptions,
      Set<ARGState> pTargetStates,
      Set<ARGState> pNonTargetStates,
      Set<ARGState> pUnknownStates)
      throws IOException {
    // TODO Refactor this method and the copied to one
    int numProducedStates = 0;
    sb.append(GIAGenerator.AUTOMATON_HEADER);

    String actionOnFinalEdges = "";

    GIAGenerator.storeInitialNode(sb, edgesToAdd.isEmpty(), GIAGenerator.getNameOrError(rootState));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_TEMP_STATE));
    } else {
      sb.append(
          String.format(
              "    TRUE -> ASSUME {true} GOTO %s;\n\n", GIAGenerator.NAME_OF_TEMP_STATE));
    }

    sb.append(String.format("TARGET STATE %s :\n", GIAGenerator.NAME_OF_ERROR_STATE));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_ERROR_STATE));
    } else {
      sb.append(
          String.format(
              "    TRUE -> ASSUME {true} GOTO %s;\n\n", GIAGenerator.NAME_OF_ERROR_STATE));
    }

    sb.append(String.format("NON_TARGET STATE %s :\n", GIAGenerator.NAME_OF_FINAL_STATE));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_FINAL_STATE));
    } else {
      sb.append(
          String.format(
              "    TRUE -> ASSUME {true} GOTO %s;\n\n", GIAGenerator.NAME_OF_FINAL_STATE));
    }

    sb.append(String.format("UNKNOWN STATE %s :\n", GIAGenerator.NAME_OF_UNKNOWN_STATE));
    if (ignoreAssumptions) {
      sb.append(String.format("    TRUE -> GOTO %s;\n\n", GIAGenerator.NAME_OF_UNKNOWN_STATE));
    } else {
      sb.append(
          String.format(
              "    TRUE -> ASSUME {true} GOTO %s;\n\n", GIAGenerator.NAME_OF_UNKNOWN_STATE));
    }



    // Fill the map to be able to iterate over the nodes
    Map<ARGState, Set<GIAARGStateEdge>> nodesToEdges = new HashMap<>();
    edgesToAdd.forEach(
        e -> {
          if (nodesToEdges.containsKey(e.getSource())) {
            nodesToEdges.get(e.getSource()).add(e);
          } else {
            nodesToEdges.put(e.getSource(), Sets.newHashSet(e));
          }
        });

    for (final ARGState currentState :
        nodesToEdges.keySet().stream()
            .sorted(Comparator.comparing(GIAGenerator::getNameOrError))
            .collect(ImmutableList.toImmutableList())) {



      sb.append(GIAGenerator.getDefStringForState(currentState, pTargetStates, pNonTargetStates, pUnknownStates));
      numProducedStates++;

      //Only add a node if it is neither in F_N nor F_NT
      if (!pTargetStates.contains(currentState) && ! pNonTargetStates.contains(currentState)){


      for (GIAARGStateEdge edge : nodesToEdges.get(currentState)) {

        sb.append("    MATCH \"");
        AssumptionCollectorAlgorithm.escape(GIAGenerator.getEdgeString(edge.getEdge()), sb);
        sb.append("\" -> ");
        sb.append(String.format("GOTO %s", edge.getTargetName(pTargetStates,pNonTargetStates,pUnknownStates)));
        sb.append(";\n");
      }    }
//     if (pTargetStates.contains(currentState) || pNonTargetStates.contains(currentState)
//      || pUnknownStates.contains(currentState)){
        sb.append(
            String.format(
                "    MATCH OTHERWISE -> " + actionOnFinalEdges + "GOTO %s;\n",
                GIAGenerator.getNameOrError(currentState)));
        //        sb.append("    TRUE -> " + actionOnFinalEdges + "GOTO __TRUE;\n\n");
//      }
     sb.append("\n");
    }
    sb.append("END AUTOMATON\n");

    return numProducedStates;
  }

  private Set<ARGState> getAllTargetStates(UnmodifiableReachedSet pReached) {
    Set<ARGState> targetStates = new HashSet<>();
    for (ARGState errorState :
        pReached.asCollection().stream()
            .filter(s -> AbstractStates.isTargetState(s))
            .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
            .collect(ImmutableList.toImmutableList())) {
      assert !errorState.isCovered();
      targetStates.add(errorState);
    }
    targetStates.addAll(
        pReached.asCollection().stream()
            .filter(
                state ->
                    AbstractStates.extractStateByType(state, AssumptionStorageState.class).isStop())
            .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
            .collect(ImmutableList.toImmutableList()));

    return targetStates;
  }
}
