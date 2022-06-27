// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.giageneration;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.PostCondition;
import org.sosy_lab.cpachecker.core.algorithm.fault_localization.by_unsatisfiability.trace_formula.TraceFormula.PreCondition;
import org.sosy_lab.cpachecker.core.algorithm.giageneration.GIAGenerator.GIAGeneratorOptions;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessAssumptionFilter;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetCPA;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class GIAARGGenerator {

  private final LogManager logger;
  private final GIAGeneratorOptions options;
  private final MachineModel machineModel;
  private final Configuration config;
  private final ConfigurableProgramAnalysis cpa;
  private final FormulaManagerView formulaManager;
  private final Level logLevel = Level.INFO;

  private final Function<ARGState, Optional<CounterexampleInfo>> getCounterexampleInfo;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;

  public GIAARGGenerator(
      LogManager pLogger,
      GIAGeneratorOptions pOptions,
      CFA pCfa,
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      FormulaManagerView pFormulaManager,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {

    this.logger = pLogger;
    this.options = pOptions;
    this.machineModel = pCfa.getMachineModel();
    this.cfa = pCfa;
    this.cpa = pCpa;
    this.config = pConfig;
    this.formulaManager = pFormulaManager;
    this.shutdownNotifier = pShutdownNotifier;
    AssumptionToEdgeAllocator assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(config, logger, machineModel);
    getCounterexampleInfo =
        s -> ARGUtils.tryGetOrCreateCounterexampleInformation(s, cpa, assumptionToEdgeAllocator);
  }

  int produceGIA4ARG(Appendable pOutput, UnmodifiableReachedSet pReached)
      throws IOException, InterruptedException, CPAException {
    final AbstractState firstState = pReached.getFirstState();
    if (!(firstState instanceof ARGState)) {
      pOutput.append("Cannot dump assumption as automaton if ARGCPA is not used.");
    }

    logger.log(Level.INFO, "Starting generation of GIA");


    /**
     * now we compute all states that needs to be merged. Two states will be merged if: (1) They
     * have the same child and the child has both as states as parents<br>
     * (2) The edges between them is a blank edge with file Location "none"
     */
    HashMultimap<ARGState, ARGState> statesToMerge = HashMultimap.create();
    pReached.asCollection().stream()
        .map(as -> AbstractStates.extractStateByType(as, ARGState.class))
        .filter(as -> as != null)
        .filter(as -> as.getParents().size() == 2)
        .filter(
            as -> {
              @Nullable CFANode node = AbstractStates.extractLocation(as);
              return node != null
                  && CFAUtils.enteringEdges(node).size() == 2
                  && CFAUtils.enteringEdges(node)
                      .allMatch(
                          edge ->
                              edge instanceof BlankEdge
                                  && edge.getRawStatement().isEmpty()
                                  && edge.getFileLocation().toString().equals("none"));
            })
        .forEach(as -> as.getParents().forEach(p -> statesToMerge.put(as, p)));
    Set<Set<ARGState>> statesThatAreEqual = buildEquivaenceClasses(statesToMerge);

    final ARGState pArgRoot = (ARGState) pReached.getFirstState();

    // Compute all target states: States marked as target
    Set<ARGState> targetStates = getAllTargetStates(pReached);
    // Compute all non.target states: States that (1) are explored, (2) are not covered,  (3) do not
    // have a successor (4) no target states
    Set<ARGState> nonTargetStates = getAllNonTargetStates(pReached);
    Set<ARGState> unknownStates = new HashSet<>();

    // Determine, which of the nodes are in F_unknown:
    if (!options.isOverApproxAnalysis()) {
      if (!options.ignoreExpandedStates()) {
        unknownStates.addAll(nonTargetStates);
      }
      // as this analysis is not allowed to add states to F_NT, clear it
      nonTargetStates = new HashSet<>();
    }
    if (!options.isUnderApproxAnalysis()) {
      unknownStates.addAll(targetStates);
      // as this analysis is not allowed to add states to F_T, clear it
      targetStates = new HashSet<>();
    }

    Map<ARGState, Set<CFAEdge>> coveredGoals = new HashMap<>();
    if (options.generateGIAForTesttargets()) {
      coveredGoals = computeCoveredGoals(pReached);
    }

    Set<GIAARGStateEdge<ARGState>> relevantEdges =
        getGiaargStateEdges(
            pReached, pArgRoot, targetStates, nonTargetStates, unknownStates, coveredGoals);
    if (options.isOptimizeForTestcases()) {
      Set<CFAEdge> coveredGoalsSet =
          coveredGoals.values().stream().reduce(new HashSet<>(), Sets::union);
      Set<CFAEdge> uncoveredGoals =
          Objects.requireNonNull(CPAs.retrieveCPA(cpa, TestTargetCPA.class)).getTestTargets();
      uncoveredGoals.removeAll(coveredGoalsSet);

      if (!uncoveredGoals.isEmpty()) {
        ReachedSet reached = computeReachedForMissedTestgoals();
        ARGState argRootOfSecondSet =
            AbstractStates.extractStateByType(reached.getFirstState(), ARGState.class);
        if (argRootOfSecondSet != null) {
          final Set<ARGState> additionalUnknownStates = getUncoveredGoals(reached, uncoveredGoals);
          Set<GIAARGStateEdge<ARGState>> moreRelevantEdges =
              getGiaargStateEdges(
                  reached,
                  argRootOfSecondSet,
                  new HashSet<>(),
                  new HashSet<>(),
                  additionalUnknownStates,
                  new HashMap<>());
          unknownStates.addAll(additionalUnknownStates);
          relevantEdges = merge(relevantEdges, moreRelevantEdges, pArgRoot, argRootOfSecondSet);
        }
      }
    }
    relevantEdges = mergeStates(relevantEdges, statesThatAreEqual);
    logger.log(
        logLevel,
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
    GIAWriter<ARGState> writer = new GIAWriter<>(true);

    return writer.writeGIA(
        pOutput, pArgRoot, relevantEdges, targetStates, nonTargetStates, unknownStates);
  }

  private Set<GIAARGStateEdge<ARGState>> mergeStates(Set<GIAARGStateEdge<ARGState>> pRelevantEdges, Set<Set<ARGState>> pStatesThatAreEqual)
      throws CPAException {
    for (Set<ARGState> toMerge : pStatesThatAreEqual){
      if (toMerge.size() > 1){
        ARGState keep = toMerge.stream().findFirst().orElseThrow();
        ImmutableSet<ARGState> statesToReplace =
            toMerge.stream().filter(s -> !s.equals(keep)).collect(ImmutableSet.toImmutableSet());
        for (ARGState toReplace : statesToReplace){
          for (GIAARGStateEdge<ARGState> edge : pRelevantEdges){
            if (edge.getSource().equals(toReplace)){
              edge.setSource(keep);
            }if (edge.getTarget().isPresent() && edge.getTarget().orElseThrow().equals(toReplace)){
              edge.setTarget(keep);
            }
          }
        }
      }else{
        throw new CPAException(String.format("the merging of states failed, as %s does contain at most one state",toMerge ));
      }
    }
    return pRelevantEdges.stream().distinct().collect(Collectors.toUnmodifiableSet());
  }

  private Set<Set<ARGState>> buildEquivaenceClasses(HashMultimap<ARGState, ARGState> pStatesToMerge)
      throws CPAException {

    Set<Set<ARGState>> res = new HashSet<>();
    Set<ARGState> keysToProcess = new HashSet<>(pStatesToMerge.keySet());
    while (!keysToProcess.isEmpty()) {
      ARGState currentKey = keysToProcess.stream().findFirst().orElseThrow();
      res.add(handleCurrentState(currentKey, pStatesToMerge, keysToProcess, true));
    }
    return res;
  }

  /**
   * @param pCurrentKey the current key
   * @param pStatesToMerge the set
   * @param pKeysToProcess the processed keys
   * @param pNotCheckForParents true if we want to search backwards, false if we do not want to
   *     search backwards anymore!
   * @return the computed set
   * @throws CPAException if an unhandled case occurs
   */
  private Set<ARGState> handleCurrentState(
      ARGState pCurrentKey,
      HashMultimap<ARGState, ARGState> pStatesToMerge,
      Set<ARGState> pKeysToProcess,
      boolean pNotCheckForParents)
      throws CPAException {
    if (!pStatesToMerge.containsKey(pCurrentKey)) {
      return Sets.newHashSet(pCurrentKey);
    }
    Set<Entry<ARGState, ARGState>> keysPointingToCurrentKey = new HashSet<>();
    if (pNotCheckForParents) {
      keysPointingToCurrentKey =
          pStatesToMerge.entries().stream()
              .filter(ent -> ent.getValue().equals(pCurrentKey))
              .collect(ImmutableSet.toImmutableSet());
    }
    if (keysPointingToCurrentKey.isEmpty()) {
      // we found a node from which we can start merging
      pKeysToProcess.remove(pCurrentKey);
      // check if the target nodes also will be merged and compute the set for merged states for
      // these
      Set<ARGState> acc = new HashSet<>();
      for (ARGState val : pStatesToMerge.get(pCurrentKey)) {
        Set<ARGState> argStates = handleCurrentState(val, pStatesToMerge, pKeysToProcess, false);
        acc = Sets.union(acc, argStates);
      }
      return acc;
    } else if (keysPointingToCurrentKey.size() == 1) {
      // we are not at the point where we can merge, hence restart with the parent of the current
      // node (the one the current will be merged with
      return handleCurrentState(
          keysPointingToCurrentKey.stream().findFirst().orElseThrow().getKey(),
          pStatesToMerge,
          pKeysToProcess,
          pNotCheckForParents);
    } else {
      throw new CPAException(
          String.format(
              "Dont know how multiple succesor states while merging for %s, hence aborting",
              pCurrentKey.toString()));
    }
  }

  private Set<GIAARGStateEdge<ARGState>> merge(
      Set<GIAARGStateEdge<ARGState>> pRelevantEdges,
      Set<GIAARGStateEdge<ARGState>> pMoreRelevantEdges,
      ARGState pRootFirst,
      ARGState pRootOther) {
    for (GIAARGStateEdge<ARGState> edge : pMoreRelevantEdges) {
      if (edge.getSource().equals(pRootOther)) {
        edge.setSource(pRootFirst);
      }
      if (edge.getTarget().isPresent() && edge.getTarget().orElseThrow().equals(pRootOther)) {
        edge.setTarget(pRootFirst);
      }
      pRelevantEdges.add(edge);
    }
    return pRelevantEdges;
  }

  private ReachedSet computeReachedForMissedTestgoals() throws CPAException {
    try {
      ConfigurationBuilder configBuilder = Configuration.builder();
      configBuilder.setOption("cpa.composite.aggregateBasicBlocks", "false");
      configBuilder.setOption("analysis.checkCounterexamples", "false");
      configBuilder.setOption("cpa", "cpa.arg.ARGCPA");
      configBuilder.setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA");
      configBuilder.setOption("analysis.stopAfterError", "false");
      configBuilder.setOption("analysis.collectAssumptions", "true");
      configBuilder.setOption(
          "CompositeCPA.cpas",
          "cpa.location.LocationCPA, cpa.callstack.CallstackCPA, "
              + "cpa.functionpointer.FunctionPointerCPA,"
              + "cpa.assumptions.storage.AssumptionStorageCPA");
      Configuration localConfig = configBuilder.build();
      ReachedSetFactory reachedSetFactory = new ReachedSetFactory(localConfig, logger);
      CPABuilder builder = new CPABuilder(localConfig, logger, shutdownNotifier, reachedSetFactory);
      ConfigurableProgramAnalysis localCPA =
          builder.buildCPAs(cfa, Specification.alwaysSatisfied(), AggregatedReachedSets.empty());
      CPAAlgorithm algorithm = CPAAlgorithm.create(localCPA, logger, localConfig, shutdownNotifier);
      CFANode rootNode = cfa.getMainFunction();
      StateSpacePartition partition = StateSpacePartition.getDefaultPartition();

      ReachedSet reachedSet = reachedSetFactory.createAndInitialize(localCPA, rootNode, partition);
      //noinspection ResultOfMethodCallIgnored
      algorithm.run(reachedSet);
      return reachedSet;
    } catch (InterruptedException | InvalidConfigurationException | CPAException pE) {
      throw new CPAException("Failed to compute the missing edges!", pE);
    }
  }

  private Set<ARGState> getUncoveredGoals(
      UnmodifiableReachedSet pReached, Set<CFAEdge> pUncoveredGoals) {
    return pReached.asCollection().stream()
        .map(as -> AbstractStates.extractStateByType(as, ARGState.class))
        .filter(as -> as != null)
        .filter(
            as ->
                as.getParents().stream()
                    .anyMatch(
                        parent ->
                            pUncoveredGoals.contains(
                                AbstractStates.extractLocation(parent)
                                    .getEdgeTo(AbstractStates.extractLocation(as)))))
        .collect(ImmutableSet.toImmutableSet());
  }

  @Nonnull
  private Set<GIAARGStateEdge<ARGState>> getGiaargStateEdges(
      UnmodifiableReachedSet pReached,
      ARGState pArgRoot,
      Set<ARGState> targetStates,
      Set<ARGState> nonTargetStates,
      Set<ARGState> unknownStates,
      Map<ARGState, Set<CFAEdge>> coveredGoals)
      throws InterruptedException {
    if (options.generateGIAForTesttargets()) {

      try {
        TestTargetCPA testCpa = new TestTargetCPA(cfa, config);
        Set<CFAEdge> testTargets = testCpa.getTestTargets();
        unknownStates.addAll(
            pReached.asCollection().stream()
                .map(as -> AbstractStates.extractStateByType(as, ARGState.class))
                .filter(Objects::nonNull)
                .filter(
                    as -> {
                      @Nullable CFANode loc = AbstractStates.extractLocation(as);
                      Optional<ARGState> parent = as.getParents().stream().findFirst();
                      if (parent.isEmpty()) return false;
                      CFAEdge edge =
                          Objects.requireNonNull(
                                  AbstractStates.extractLocation(parent.orElseThrow()))
                              .getEdgeTo(AbstractStates.extractLocation(as));
                      return loc != null && testTargets.contains(edge);
                    })
                .collect(ImmutableList.toImmutableList()));
      } catch (InvalidConfigurationException pE) {
        logger.logUserException(Level.INFO, pE, "Failed to generate additional unknown states!");
      }
    }

    // Determine all paths that are relevant
    final Set<ARGState> finalStates =
        Sets.union(nonTargetStates, Sets.union(targetStates, unknownStates));
    //    BiPredicate<ARGState, ARGState> isRelevantEdge = getRelevantEdges(finalStates);
    //    Predicate<ARGState> relevantState = Predicates.in(finalStates);

    Multimap<ARGState, CFAEdgeWithAssumptions> edgesWithAssumptions = ArrayListMultimap.create();
    // TODO: Check when this option is needed (not needed for generating testcases
    if (options.isGenGIA4Refinement()) {
      for (ARGState errorState : targetStates) {
        CounterexampleInfo pCounterExample = getCounterexampleInfo.apply(errorState).orElse(null);
        storeInterpolantsAsAssumptions(Optional.ofNullable(pCounterExample), edgesWithAssumptions);
      }
    }

    ImmutableSet<ARGState> statesWithInterpolant = ImmutableSet.<ARGState>builder().build();
    if (options.isStoreInterpolantsInGIA()) {
      // Get all states that have some invariants, because for them the invariant will be printed in
      // the GIA
      statesWithInterpolant =
          pReached.asCollection().stream()
              .filter(
                  s -> {
                    @Nullable PredicateAbstractState pState =
                        AbstractStates.extractStateByType(s, PredicateAbstractState.class);
                    if (pState == null) return false;
                    if (!pState.isAbstractionState()) return false;
                    // Remove all non-abstract states and abstract states with true abstraction
                    // formula
                    return !formulaManager
                        .getBooleanFormulaManager()
                        .isTrue(pState.getAbstractionFormula().asFormula());
                  })
              .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
              .collect(ImmutableSet.toImmutableSet());
    }

    // Get all states that have some assumptions
    ImmutableSet<ARGState> statesWithAssumption =
        pReached.asCollection().stream()
            .filter(
                s -> {
                  @Nullable AssumptionStorageState pState =
                      AbstractStates.extractStateByType(s, AssumptionStorageState.class);
                  if (pState == null) return false;
                  boolean trueAssumption = pState.isAssumptionTrue();
                  return !trueAssumption;
                })
            .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
            .collect(ImmutableSet.toImmutableSet());

    Set<GIAARGStateEdge<ARGState>> relevantEdges = new HashSet<>();
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
    LinkedHashSet<ARGState> toProcess = new LinkedHashSet<>();
    toProcess.add(pArgRoot);
    Set<ARGState> processed = new HashSet<>();

    logger.log(
        logLevel,
        "Final states found are "
            + String.join(
                ",",
                finalStates.stream()
                    .map(a -> Integer.toString(a.getStateId()))
                    .collect(ImmutableList.toImmutableList())));

    Set<ARGState> statesReachingFinalState = computeSetOfTargetReachingStates(finalStates);

    while (!toProcess.isEmpty()) {
      ARGState state = toProcess.stream().findFirst().orElseThrow();
      toProcess.remove(state);
      logger.logf(
          logLevel,
          "Taking %s from the list, processed %s, toProcess %s",
          state.getStateId(),
          processed.stream().map(a -> a.getStateId()).collect(ImmutableList.toImmutableList()),
          toProcess);

      for (ARGState child : state.getChildren()) {
        addAllRelevantEdges(
            state,
            child,
            finalStates,
            toProcess,
            relevantEdges,
            processed,
            edgesWithAssumptions,
            statesWithInterpolant,
            statesWithAssumption,
            statesReachingFinalState);
      }
      processed.add(state);
    }

    relevantEdges = updateForBreakEdges(relevantEdges);

    if (options.isOptimizeForTestcases()) {

      Set<ARGState> ignoreStates = optimizeForTestcase(pReached, coveredGoals);
      Set<GIAARGStateEdge<ARGState>> toKeep =
          new HashSet<>(
              relevantEdges.stream()
                  .filter(e -> e.getSource().equals(pArgRoot))
                  .collect(ImmutableList.toImmutableList()));
      Set<GIAARGStateEdge<ARGState>> nextEdgeToProcess =
          new HashSet<>(
              relevantEdges.stream()
                  .filter(e -> e.getSource().equals(pArgRoot))
                  .collect(ImmutableList.toImmutableList()));
      while (!nextEdgeToProcess.isEmpty()) {
        GIAARGStateEdge<ARGState> current = nextEdgeToProcess.stream().findFirst().orElseThrow();
        nextEdgeToProcess.remove(current);

        Optional<ARGState> currenTarget = current.getTarget();
        if (currenTarget.isPresent() && !ignoreStates.contains(currenTarget.orElseThrow())) {
          toKeep.add(current);
          nextEdgeToProcess.addAll(
              relevantEdges.stream()
                  .filter(e -> e.getSource().equals(currenTarget.orElseThrow()))
                  .collect(ImmutableList.toImmutableList()));
        }
      }
      relevantEdges = toKeep;
    }

    relevantEdges = addMissingAssumeEdges(relevantEdges);

    return relevantEdges;
  }

  private Set<ARGState> computeSetOfTargetReachingStates(Set<ARGState> pFinalStates) {
    Set<ARGState> processed = new HashSet<>(pFinalStates);
    Set<ARGState> targetReachingStates = new HashSet<>(processed);
    Set<ARGState> toProcess = new LinkedHashSet<>(targetReachingStates);
    while (!toProcess.isEmpty()) {
      ARGState current = toProcess.stream().findFirst().orElseThrow();
      toProcess.remove(current);
      ImmutableSet<ARGState> toProcessNext =
          current.getParents().stream()
              .filter(p -> !processed.contains(p))
              .collect(ImmutableSet.toImmutableSet());
      toProcess.addAll(toProcessNext);
      processed.add(current);
      targetReachingStates.addAll(current.getParents());
    }
    return targetReachingStates;
  }

  private Set<GIAARGStateEdge<ARGState>> addMissingAssumeEdges(
      Set<GIAARGStateEdge<ARGState>> pRelevantEdges) {
    Map<ARGState, List<GIAARGStateEdge<ARGState>>> nodesToEdges = new HashMap<>();
    pRelevantEdges.forEach(
        e -> {
          if (nodesToEdges.containsKey(e.getSource())) {
            nodesToEdges.get(e.getSource()).add(e);
          } else {
            nodesToEdges.put(e.getSource(), Lists.newArrayList(e));
          }
        });
    for (Entry<ARGState, List<GIAARGStateEdge<ARGState>>> edge : nodesToEdges.entrySet()) {
      List<GIAARGStateEdge<ARGState>> assumeEdgesPresent =
          edge.getValue().stream()
              .filter(e -> e.getEdge() instanceof AssumeEdge)
              .collect(ImmutableList.toImmutableList());
      if (assumeEdgesPresent.stream().map(e -> e.getEdge()).distinct().count() == 1) {
        // Add all edges (assumptions) that are not present
        CFAEdge cfaEdge = assumeEdgesPresent.get(0).getEdge();
        pRelevantEdges.addAll(
            CFAUtils.leavingEdges(cfaEdge.getPredecessor()).stream()
                .filter(e -> !e.equals(cfaEdge))
                .map(e -> new GIAARGStateEdge<>(assumeEdgesPresent.get(0).source, e))
                .collect(ImmutableList.toImmutableList()));
      }
    }
    return pRelevantEdges;
  }

  private Map<ARGState, Set<CFAEdge>> computeCoveredGoals(UnmodifiableReachedSet pReached) {
    // Compute all covered goals for the Abstract states
    Map<ARGState, Set<CFAEdge>> stateToCoveredGoals = new HashMap<>();
    LinkedHashSet<AbstractState> toProcess = new LinkedHashSet<>(pReached.asCollection());
    while (!toProcess.isEmpty()) {
      AbstractState state = toProcess.stream().findFirst().orElseThrow();
      toProcess.remove(state);
      ARGState current = AbstractStates.extractStateByType(state, ARGState.class);
      if (current == null) {
        return new HashMap<>();
      }
      if (stateToCoveredGoals.containsKey(current)) continue;
      if (current.getChildren().stream().allMatch(c -> stateToCoveredGoals.containsKey(c))) {

        Set<CFAEdge> coveredByCurrent =
            current.isTarget()
                ? Sets.newHashSet(
                    AbstractStates.extractLocation(Lists.newArrayList(current.getParents()).get(0))
                        .getEdgeTo(AbstractStates.extractLocation(current)))
                : new HashSet<>();
        stateToCoveredGoals.put(
            current,
            current.getChildren().stream()
                .map(c -> stateToCoveredGoals.get(c))
                .reduce(coveredByCurrent, Sets::union));
      } else {
        toProcess.add(current);
      }
    }
    return stateToCoveredGoals;
  }

  private Set<ARGState> optimizeForTestcase(
      UnmodifiableReachedSet pReached, Map<ARGState, Set<CFAEdge>> pCoveredGoals) {

    if (AbstractStates.extractStateByType(pReached.getFirstState(), TestTargetState.class)
        != null) {

      Optional<AbstractState> splitPointOpt =
          pReached.asCollection().stream()
              .filter(
                  s ->
                      AbstractStates.extractStateByType(s, ValueAnalysisState.class) != null
                          && Objects.requireNonNull(
                                  AbstractStates.extractStateByType(s, ValueAnalysisState.class))
                              .isSplitPoint())
              .findFirst();
      if (splitPointOpt.isPresent()) {
        AbstractState splitPoint = splitPointOpt.orElseThrow();
        Set<Set<CFAEdge>> coveredByChilds = new HashSet<>();
        Set<ARGState> toRemove = new HashSet<>();
        for (ARGState child :
            Objects.requireNonNull(AbstractStates.extractStateByType(splitPoint, ARGState.class))
                .getChildren()) {
          Set<CFAEdge> covered =
              pCoveredGoals.getOrDefault(
                  AbstractStates.extractStateByType(child, ARGState.class), new HashSet<>());
          if (!covered.isEmpty() && coveredByChilds.contains(covered)) {
            toRemove.add(child);

          } else {
            coveredByChilds.add(covered);
          }
        }

        //        try {
        //          ReachedSetFactory factory = new ReachedSetFactory(config, logger);
        //          ReachedSet newReached = factory.create(cpa);
        //          ArrayList<ARGState> toAdd = new ArrayList<>();
        //          toAdd.add(AbstractStates.extractStateByType(pReached.getFirstState(),
        // ARGState.class));
        //          ARGState argSplit = AbstractStates.extractStateByType(splitPoint,
        // ARGState.class);
        //          while (!toAdd.isEmpty()) {
        //            ARGState current = toAdd.remove(0);
        //            if (toRemove.contains(current)) {
        //              logger.logf(Level.INFO, "Skipping %s", current);
        //              argSplit.deleteChild(current);
        //              continue;
        //            } else {
        //              newReached.add(current, pReached.getPrecision(current));
        //              toAdd.addAll(
        //                 current.getChildren());
        //            }
        //          }
        //          return newReached;
        //        } catch (InvalidConfigurationException pE) {
        //          logger.logUserException(Level.INFO, pE, "Optimizing the reached set failed,
        // skipping it");
        //          return pReached;
        //        }
        return toRemove;
      }
    }
    return new HashSet<>();
  }

  /**
   * If there are break edges present, simplify the GIA as follows: The edge of the break should
   * lead directly to the else branch of the loop head. This is any other ARG state with same
   * location but different from the current break target state
   */
  private Set<GIAARGStateEdge<ARGState>> updateForBreakEdges(
      Set<GIAARGStateEdge<ARGState>> pRelevantEdges) {
    if (cfa.getLoopStructure().isEmpty()) return pRelevantEdges;
    for (GIAARGStateEdge<ARGState> edge : pRelevantEdges) {
      if (edge.getEdge() instanceof AssumeEdge
          && nextStatementIsBreak(edge.getEdge().getSuccessor())) {
        CFANode targetNodeOfBreak = edge.getEdge().getSuccessor();
        Optional<GIAARGStateEdge<ARGState>> edgeToReplace =
            pRelevantEdges.stream()
                .filter(
                    e -> AbstractStates.extractLocation(e.getSource()).equals(targetNodeOfBreak))
                .findFirst();

        if (edgeToReplace.isPresent() && edgeToReplace.orElseThrow().getTarget().isPresent()) {
          Optional<GIAARGStateEdge<ARGState>> newTarget =
              pRelevantEdges.stream()
                  .filter(
                      e ->
                          !e.equals(edge)
                              && e.getTarget().isPresent()
                              && e.getTarget()
                                  .orElseThrow()
                                  .equals(edgeToReplace.orElseThrow().getTarget().orElseThrow()))
                  .findFirst();
          edge.setTarget(newTarget.get().getSource());
        }
        //
        //          // The node is part of a loop
        //          if (loop.isPresent()) {
        //          loop.orElseThrow().getOutgoingEdges().
        //
        //
        //
        //
        //          Optional<CFANode> optLoophead = loop.orElseThrow().getLoopHeads().stream()
        //              //.filter(node -> CFAUtils.leavingEdges(node).allMatch(e -> e instanceof
        // AssumeEdge))
        //              .findFirst();
        //          if (optLoophead.isPresent()){
        //            //we have a loophead
        //            Optional<CFAEdge> loopLeavingEdge =
        //                CFAUtils.leavingEdges(optLoophead.orElseThrow()).stream()
        //                    .filter(e ->
        // loop.orElseThrow().getOutgoingEdges().contains(e)).findFirst();
        //            if (loopLeavingEdge.isPresent()){
        //              //we found the edge to which target we want to redirect the current break
        // edge
        //              Optional<GIAARGStateEdge<ARGState>> targetOpt = pRelevantEdges.stream()
        //                  .filter(e ->
        // e.getEdge().equals(loopLeavingEdge.orElseThrow())).findFirst();
        //              if (targetOpt.isPresent() &&
        // targetOpt.orElseThrow().getTarget().isPresent()){
        //                edge.setTarget(targetOpt.orElseThrow().getTarget().orElseThrow());
        //              }
        //            }
        //          }
        //        }
      }
    }

    return pRelevantEdges;
  }

  private Optional<Loop> getOutermostLoopContainingNode(
      LoopStructure pLoopStrc, CFANode pSuccessor) {
    Loop outerLoop = null;
    for (Loop current : pLoopStrc.getAllLoops()) {
      if (current.getOutgoingEdges().stream().anyMatch(e -> e.getSuccessor().equals(pSuccessor))) {
        if (outerLoop == null) {
          outerLoop = current;
        } else if (current.isOuterLoopOf(outerLoop)) {
          outerLoop = current;
        }
      }
    }
    return Optional.ofNullable(outerLoop);
  }

  private boolean nextStatementIsBreak(CFANode pSuccessor) {
    // FIXME: check and fix if break is blank edge
    return pSuccessor.getNumLeavingEdges() == 1
        && pSuccessor.getLeavingEdge(0) instanceof BlankEdge
        && pSuccessor.getLeavingEdge(0).getDescription().equals("break");
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
  //   * Code is taken from {@link
  // org.sosy_lab.cpachecker.cpa.arg.witnessexport.GraphBuilder#ARG_PATH}
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
  //                    new GIAARGStateEdge(s, multiEdgeIndexForSource, siblingEdge,
  // Optional.empty()));
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
  //                new GIAARGStateEdge(s, multiEdgeIndexForSource, edgeToNextState,
  // Optional.empty()));
  //          } else {
  //            edges.add(new GIAARGStateEdge(s, edgeToNextState));
  //          }
  //        }
  //      }
  //    }
  //    return edges;
  //  }
  //
  //  private Optional<String> getAssumptionForEdge(
  //      ARGState pS, CFAEdge pEdgeToNextState, Multimap<ARGState, CFAEdgeWithAssumptions>
  // pValueMap) {
  //    if (pValueMap.get(pS).stream().anyMatch(e -> e.getCFAEdge().equals(pEdgeToNextState))) {
  //      CFAEdgeWithAssumptions ass =
  //          pValueMap.get(pS).stream()
  //              .filter(e -> e.getCFAEdge().equals(pEdgeToNextState))
  //              .findFirst()
  //              .orElseThrow();
  //      return Optional.of(ass.getAsCode());
  //    }
  //    return Optional.empty();
  //  }
  //
  //  private BiPredicate<ARGState, ARGState> getRelevantEdges(Set<ARGState> pFinalStates) {
  //    Set<Pair<ARGState, ARGState>> relevantEdges = new HashSet<>();
  //    for (ARGState finalState : pFinalStates) {
  //      ImmutableSet<ARGState> nodes = ARGUtils.getAllStatesOnPathsTo(finalState);
  //      for (ARGState node : nodes) {
  //        for (ARGState successors : node.getChildren()) {
  //          if (nodes.contains(successors)) {
  //            relevantEdges.add(Pair.of(node, successors));
  //          }
  //        }
  //      }
  //    }
  //    return BiPredicates.pairIn(relevantEdges);
  //  }

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
            s ->
                s != null
                    && !s.isTarget()
                    && s.wasExpanded()
                    && !s.isCovered()
                    && s.getChildren().isEmpty())
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Traverse allpath from parent to its child node, until a relevant edge is found
   *
   * @param pCurrentState the current state that is processed
   * @param pChild its child to check for beeing relevant (i.e. any edge on the path leading to that
   *     edge )
   * @param pFinalStates the target states
   * @param pToProcess the states to process
   * @param pRelevantEdges the relevent ades to add the result to
   * @param pProcessed the edges already processed
   * @param pEdgesWithAssumptions the assumptions from the preicse ce
   * @param pStatesWithInterpolant the states that contain a non-trivial interpolation and are thus
   * @param pStatesWithAssumption states with assumptions
   * @param pStatesReachingFinalState states reaching a final state (more efficient to compute once
   *     in advance
   */
  private void addAllRelevantEdges(
      ARGState pCurrentState,
      ARGState pChild,
      Set<ARGState> pFinalStates,
      LinkedHashSet<ARGState> pToProcess,
      Set<GIAARGStateEdge<ARGState>> pRelevantEdges,
      Set<ARGState> pProcessed,
      Multimap<ARGState, CFAEdgeWithAssumptions> pEdgesWithAssumptions,
      ImmutableSet<ARGState> pStatesWithInterpolant,
      ImmutableSet<ARGState> pStatesWithAssumption,
      Set<ARGState> pStatesReachingFinalState)
      throws InterruptedException {

    // Check, if the pChild is relevant
    Optional<Pair<CFAEdge, Optional<ARGState>>> relevantEdge =
        gedEdgeIfIsRelevant(
            pChild,
            pCurrentState,
            pFinalStates,
            pEdgesWithAssumptions,
            pStatesWithInterpolant,
            pStatesWithAssumption,
            pStatesReachingFinalState);
    boolean edgesAdded = false;
    if (relevantEdge.isPresent()) {
      // now, create a new edge.
      Pair<CFAEdge, Optional<ARGState>> pair = relevantEdge.orElseThrow();
      if (pair.getSecond().isEmpty()) {
        pRelevantEdges.add(new GIAARGStateEdge<>(pCurrentState, pair.getFirst()));
      } else {
        // Only check for assumptions for edges not leading to sink nodes
        // TODO Validate that this handling is in fact correct and the ARG node is the target node
        Optional<CFAEdgeWithAssumptions> optAddAssumption =
            pEdgesWithAssumptions.get(pair.getSecond().orElseThrow()).stream()
                .filter(e -> e.getCFAEdge().equals(pair.getFirst()))
                .findFirst();
        Optional<String> additionalAssumption =
            optAddAssumption.map(CFAEdgeWithAssumptions::getAsCode);
        pRelevantEdges.add(
            new GIAARGStateEdge<>(
                pCurrentState,
                pair.getSecond().orElseThrow(),
                pair.getFirst(),
                retriveInterpolantAndAssumptionsIfPresent(
                    pair.getSecond().orElseThrow(), pStatesWithInterpolant, pStatesWithAssumption),
                additionalAssumption));
        if (!pProcessed.contains(pChild)
            && (options.generateGIAForTesttargets()
                || !pFinalStates.contains(pair.getSecond().get()))) {
          logger.logf(logLevel, "Adding %s", pChild.getStateId());
          pToProcess.add(pChild);
        }
      }
      edgesAdded = true;
    } else {

      List<CFAEdge> pahtToChild = Lists.newArrayList(pCurrentState.getFirstPathToChild(pChild));
      List<GIAARGStateEdge<ARGState>> edgesToAdd = new ArrayList<>();
      ARGState lastNodeUsed = pChild;
      // Check if there are any edges on the path that are relevant and if so, create for
      // each relevant edge an edge
      if (pahtToChild.size() > 1) {
        logger.logf(logLevel, "Processing a Multi-node");
        while (!pahtToChild.isEmpty()) {
          CFAEdge currentEdge = pahtToChild.get(pahtToChild.size() - 1);
          pahtToChild.remove(currentEdge);
          if (isRelevantEdge(currentEdge)) {
            ARGState intermediateState = new ARGState(null, null);
            edgesToAdd.add(new GIAARGStateEdge<>(intermediateState, lastNodeUsed, currentEdge));
            //            lastNodeUsed.addParent(intermediateState);
            lastNodeUsed = intermediateState;
          }
        }
        // remove the lastNodeused and replace it by parent
        ARGState finalLastNodeUsed = lastNodeUsed;
        ImmutableList<GIAARGStateEdge<ARGState>> edgesToUpdate =
            edgesToAdd.stream()
                .filter(e -> e.getSource().equals(finalLastNodeUsed))
                .collect(ImmutableList.toImmutableList());
        for (GIAARGStateEdge<ARGState> edge : edgesToUpdate) {
          edgesToAdd.remove(edge);
          if (edge.getTarget().isPresent())
            edgesToAdd.add(
                new GIAARGStateEdge<>(
                    pCurrentState, edge.getTarget().orElseThrow(), edge.getEdge()));
        }
        if (!edgesToAdd.isEmpty()) {
          pRelevantEdges.addAll(edgesToAdd);
          if (!pProcessed.contains(pChild)) {
            logger.logf(logLevel, "Adding %s", pChild.getStateId());
            pToProcess.add(pChild);
          }
          edgesAdded = true;
        }
      }
    }
    if (!edgesAdded) {
      for (ARGState grandChild : pChild.getChildren()) {
        logger.logf(
            logLevel,
            "No match found for parent %s and child %s, coninue with grandchild %s",
            pCurrentState.getStateId(),
            pChild.getStateId(),
            grandChild.getStateId());
        // As there might be cycles with not-processed nodes, only continue with nodes that are
        // already expanded
        if (grandChild.wasExpanded() || pFinalStates.contains(grandChild)) {
          addAllRelevantEdges(
              pCurrentState,
              grandChild,
              pFinalStates,
              pToProcess,
              pRelevantEdges,
              pProcessed,
              pEdgesWithAssumptions,
              pStatesWithInterpolant,
              pStatesWithAssumption,
              pStatesReachingFinalState);
        } else {
          // Add an edge to qtemp if needed
          Optional<GIAARGStateEdge<ARGState>> additionalEdgeToQtemp =
              getEdgeForNotExpandedNode(
                  pCurrentState,
                  grandChild,
                  pFinalStates,
                  pEdgesWithAssumptions,
                  pStatesWithInterpolant,
                  pStatesWithAssumption,
                  pStatesReachingFinalState);
          if (additionalEdgeToQtemp.isPresent()) {
            pRelevantEdges.add(additionalEdgeToQtemp.orElseThrow());
          }
        }
      }
    }
  }

  private Set<ExpressionTree<Object>> retriveInterpolantAndAssumptionsIfPresent(
      ARGState pState,
      ImmutableSet<ARGState> pStatesWithInterpolant,
      ImmutableSet<ARGState> pStatesWithAssumption)
      throws InterruptedException {
    Set<ExpressionTree<Object>> res = new HashSet<>();

    if (pStatesWithInterpolant.contains(pState))
      res.add(
          Objects.requireNonNull(
                  AbstractStates.extractStateByType(pState, PredicateAbstractState.class))
              .getAbstractionFormula()
              .asExpressionTree(AbstractStates.extractLocation(pState)));
    if (pStatesWithAssumption.contains(pState)) {
      @Nullable AssumptionStorageState assumptionState =
          AbstractStates.extractStateByType(pState, AssumptionStorageState.class);
      if (Objects.nonNull(assumptionState))
        res.add(
            ExpressionTrees.fromFormula(
                assumptionState.getAssumption(),
                assumptionState.getFormulaManager(),
                AbstractStates.extractLocation(pState)));
    }
    return res;
  }

  /**
   * An Edge is relevant, if: <br>
   * 1. The child is a loophead <br>
   * 2. The child is the direct successor of the parent and the edge is an assumeEdge or function
   * call edge <br>
   * 3. The child is a final state <br>
   * 4 Alternative: The child is a {@link FunctionCallEdge} or {@link
   * org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge}<br>
   * 5. The grand child is unknown <br>
   * 6. It contains an assumption <br>
   * 7. It contians an interpolant or assumption
   *
   * @param pChild the child
   * @param pParent the parent
   * @param pFinalStates the list of final states
   * @param pEdgesWithAssumptions additional assumptinos for precise CEs
   * @param pStatesWithInterpolant the set of states with interpolants assigned to
   * @param pStatesWithAssumption the set of states with assumption
   * @param pStatesReachingFinalState the set of arg states reaching a target node (computed in
   *     advance for efficiency)
   * @return the last edge on the path from parent to child, if the edge is relevant, otherwise an
   *     empty optional and the final node to use for the edge
   */
  private Optional<Pair<CFAEdge, Optional<ARGState>>> gedEdgeIfIsRelevant(
      ARGState pChild,
      ARGState pParent,
      Set<ARGState> pFinalStates,
      Multimap<ARGState, CFAEdgeWithAssumptions> pEdgesWithAssumptions,
      ImmutableSet<ARGState> pStatesWithInterpolant,
      ImmutableSet<ARGState> pStatesWithAssumption,
      Set<ARGState> pStatesReachingFinalState) {
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
    boolean case4 =
        pathToChild.get(pathToChild.size() - 1) instanceof FunctionCallEdge
            || pathToChild.get(pathToChild.size() - 1) instanceof FunctionReturnEdge;
    // This is an old option!
    //        lastEdge instanceof BlankEdge
    //            &&
    // lastEdge.getDescription().contains(GIAGenerator.DESC_OF_DUMMY_FUNC_START_EDGE));
    // Case 5:
    // boolean case5a = pFinalStates.contains(pChild);
    //    boolean case5b = false;
    //        boolean case5b = pChild.getChildren().stream().anyMatch(gc ->
    // pUnknownStates.contains(gc));

    boolean case6 =
        pEdgesWithAssumptions.values().stream().anyMatch(e -> e.getCFAEdge().equals(lastEdge));
    boolean case7 =
        pStatesWithInterpolant.contains(pChild) || pStatesWithAssumption.contains(pChild);

    // If the pChild cannot reach the error state, do not add it to the toProcessed
    // and let the edge goto the qTemp State (as not relevant for the path)
    // If it can reach the error state, add the edge and the child to the toProcess
    if (case2 || case3 || case4 || case6 || case7) {
      if (case7 || pStatesReachingFinalState.contains(pChild)) {
        return Optional.of(Pair.of(lastEdge, Optional.of(pChild)));
      } else {
        return Optional.of(Pair.of(lastEdge, Optional.empty()));
      }
      //          } else if (case5b) {
      //            // If case 5 applies, we want to return the error state
      //            if (!canReachFinalState(pChild, pUnknownStates)) {
      //              return Optional.of(Pair.of(lastEdge, Optional.empty()));
      //            } else {
      //              return Optional.of(
      //                  Pair.of(
      //                      lastEdge,
      //                      Optional.of(
      //                          pChild.getChildren().stream()
      //                              .filter(gc -> pUnknownStates.contains(gc))
      //                              .findFirst()
      //                              .orElseThrow())));
      //            }
    }

    return Optional.empty();
  }

  private Optional<GIAARGStateEdge<ARGState>> getEdgeForNotExpandedNode(
      ARGState pParent,
      ARGState pGrandChild,
      Set<ARGState> pTargetStates,
      Multimap<ARGState, CFAEdgeWithAssumptions> pEdgesWithAssumptions,
      ImmutableSet<ARGState> pStatesWithInterpolant,
      ImmutableSet<ARGState> pStatesWithAssumption,
      Set<ARGState> pStatesReachingFinalState) {
    Optional<Pair<CFAEdge, Optional<ARGState>>> relevantEdge =
        gedEdgeIfIsRelevant(
            pGrandChild,
            pParent,
            pTargetStates,
            pEdgesWithAssumptions,
            pStatesWithInterpolant,
            pStatesWithAssumption,
            pStatesReachingFinalState);
    if (relevantEdge.isPresent()) {
      return Optional.of(new GIAARGStateEdge<>(pParent, relevantEdge.orElseThrow().getFirst()));
    } else {
      return Optional.empty();
    }
  }

  private boolean isRelevantEdge(CFAEdge pCFAEdge) {
    return pCFAEdge instanceof FunctionCallEdge
        || (pCFAEdge instanceof BlankEdge
            && pCFAEdge.getDescription().contains(GIAGenerator.DESC_OF_DUMMY_FUNC_START_EDGE));
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
  //            Level.WARNING, String.format("Cannot export state %s, as it is not an ARG State",
  // s));
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
  //          parentsWithOtherAutomatonState.add(Pair.of(parent,
  // parentAutomatonState.orElseThrow()));
  //        }
  //      }
  //      if (!parentsWithOtherAutomatonState.isEmpty()) {
  //        for (Pair<ARGState, AutomatonState> parentPair : parentsWithOtherAutomatonState) {
  //          // Create the edge
  //          CFAEdge edge = GIAGenerator.getEdge(parentPair, argState);
  //          edgesToAdd.add(
  //              new GIAAutomatonStateEdge(parentPair.getSecond(), currentAutomatonState, edge));
  //          // Check, if the parent node has any other outgoing edges, they have to be added
  // aswell
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
  //        logLevel, edgesToAdd.stream().map(e ->
  // e.toString()).collect(Collectors.joining("\n")));
  //
  //    return writeGIAForViolationWitness(
  //        output, rootState, edgesToAdd, optinons.isAutomatonIgnoreAssumptions());
  //  }

  //  /**
  //   * Create an GIA for the given set of edges Beneth printing the edges, each node gets a
  // self-loop
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
