// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionRefinementStrategy.findAllPredicatesFromSubgraph;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGPrecision;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.GenericRefiner;
import org.sosy_lab.cpachecker.util.refinement.InterpolationTree;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.StrongestPostOperator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.smg2.refinement")
public class SMGRefiner extends GenericRefiner<SMGState, SMGInterpolant> {

  @Option(
      secure = true,
      description = "whether or not to do lazy-abstraction",
      name = "restart",
      toUppercase = true)
  private RestartStrategy restartStrategy = RestartStrategy.PIVOT;

  @Option(
      secure = true,
      description = "whether or not to use heuristic to avoid similar, repeated refinements")
  private boolean avoidSimilarRepeatedRefinement = false;

  @Option(
      secure = true, // name="refinement.basisStrategy",
      description =
          "Which base precision should be used for a new precision? ALL: During refinement, collect"
              + " precisions from the complete ARG. SUBGRAPH: During refinement, keep precision"
              + " from all removed parts (subgraph) of the ARG. CUTPOINT: Only the cut-point's"
              + " precision is kept. TARGET: Only the target state's precision is kept.")
  /* see also: {@link PredicateAbstractionRefinementStrategy} */
  /* There are usually more tracked variables at the target location that at the cut-point.
   * 05/2017: An evaluation on sv-benchmark files for ALL, SUBGRAPH, TARGET, and CUTPOINT showed:
   * - overall: SUBGRAPH >= ALL >> CUTPOINT > TARGET
   * - SUBGRAPH and ALL are nearly identical
   * - CUTPOINT has smallest number of solved files,
   *   especially there are many timeouts (900s) on the source files product-lines/email_spec*,
   *   and many solved tasks in ldv-linux-3.14/linux-3.14__complex_emg*
   * - TARGET is slowest and has less score
   */
  private BasisStrategy basisStrategy = BasisStrategy.SUBGRAPH;

  private enum BasisStrategy {
    ALL,
    SUBGRAPH,
    TARGET,
    CUTPOINT
  }

  /** keep log of previous refinements to identify repeated one */
  private final Set<Integer> previousRefinementIds = new HashSet<>();

  private final Set<Integer> previousValueRefinementIds = new HashSet<>();

  private final SMGFeasibilityChecker checker;

  private SMGConcreteErrorPathAllocator concreteErrorPathAllocator;

  private final ShutdownNotifier shutdownNotifier;

  // Statistics
  private final StatCounter rootRelocations = new StatCounter("Number of root relocations");
  private final StatCounter repeatedRefinements =
      new StatCounter("Number of similar, repeated refinements");

  public static Refiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return AbstractARGBasedRefiner.forARGBasedRefiner(create0(pCpa), pCpa);
  }

  public static ARGBasedRefiner create0(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    final SMGCPA smgCpa = CPAs.retrieveCPAOrFail(pCpa, SMGCPA.class, SMGRefiner.class);

    smgCpa.injectRefinablePrecision();

    final LogManager logger = smgCpa.getLogger();
    final Configuration config = smgCpa.getConfiguration();
    final CFA cfa = smgCpa.getCFA();

    final StrongestPostOperator<SMGState> strongestPostOp =
        new SMGStrongestPostOperator(logger, config, cfa);

    final SMGFeasibilityChecker checker =
        new SMGFeasibilityChecker(strongestPostOp, logger, cfa, config);

    final GenericPrefixProvider<SMGState> prefixProvider =
        new SMGPrefixProvider(logger, cfa, config, smgCpa.getShutdownNotifier());

    return new SMGRefiner(
        checker,
        strongestPostOp,
        new PathExtractor(logger, config),
        prefixProvider,
        config,
        logger,
        smgCpa.getShutdownNotifier(),
        cfa);
  }

  SMGRefiner(
      final SMGFeasibilityChecker pFeasibilityChecker,
      final StrongestPostOperator<SMGState> pStrongestPostOperator,
      final PathExtractor pPathExtractor,
      final GenericPrefixProvider<SMGState> pPrefixProvider,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final CFA pCfa)
      throws InvalidConfigurationException {

    super(
        pFeasibilityChecker,
        new SMGPathInterpolator(
            pFeasibilityChecker,
            pStrongestPostOperator,
            pPrefixProvider,
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa),
        SMGInterpolantManager.getInstance(
            new SMGOptions(pConfig), pCfa.getMachineModel(), pLogger, pCfa),
        pPathExtractor,
        pConfig,
        pLogger);

    pConfig.inject(this, SMGRefiner.class);

    checker = pFeasibilityChecker;
    concreteErrorPathAllocator =
        new SMGConcreteErrorPathAllocator(pConfig, logger, pCfa.getMachineModel());
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  protected void refineUsingInterpolants(
      final ARGReachedSet pReached,
      final InterpolationTree<SMGState, SMGInterpolant> pInterpolationTree)
      throws InterruptedException {
    final UnmodifiableReachedSet reached = pReached.asReachedSet();
    final boolean predicatePrecisionIsAvailable = isPredicatePrecisionAvailable(reached);

    Map<ARGState, List<Precision>> refinementInformation = new LinkedHashMap<>();
    Collection<ARGState> refinementRoots =
        pInterpolationTree.obtainRefinementRoots(restartStrategy);

    for (ARGState root : refinementRoots) {
      shutdownNotifier.shutdownIfNecessary();
      root = relocateRefinementRoot(root, predicatePrecisionIsAvailable);

      if (refinementRoots.size() == 1
          && isSimilarRepeatedRefinement(
              pInterpolationTree.extractPrecisionIncrement(root).values(),
              extractPrecisionIncrement(root, pInterpolationTree).values())) {
        root = relocateRepeatedRefinementRoot(root);
      }

      List<Precision> precisions = new ArrayList<>(2);
      VariableTrackingPrecision basePrecision;
      switch (basisStrategy) {
        case ALL:
          basePrecision =
              mergeValuePrecisionsForSubgraph((ARGState) reached.getFirstState(), reached);
          break;
        case SUBGRAPH:
          basePrecision = mergeValuePrecisionsForSubgraph(root, reached);
          break;
        case TARGET:
          basePrecision = extractValuePrecision(reached.getPrecision(reached.getLastState()));
          break;
        case CUTPOINT:
          basePrecision = extractValuePrecision(reached.getPrecision(root));
          break;
        default:
          throw new AssertionError("unknown strategy for predicate basis.");
      }

      // merge the value precisions of the subtree, and refine it
      precisions.add(
          ((SMGPrecision) basePrecision)
              .withIncrement(pInterpolationTree.extractPrecisionIncrement(root))
              .withValueIncrement(extractPrecisionIncrement(root, pInterpolationTree)));

      // merge the predicate precisions of the subtree, if available
      if (predicatePrecisionIsAvailable) {
        precisions.add(findAllPredicatesFromSubgraph(root, reached));
      }

      refinementInformation.put(root, precisions);
    }

    for (Entry<ARGState, List<Precision>> info : refinementInformation.entrySet()) {
      shutdownNotifier.shutdownIfNecessary();
      List<Predicate<? super Precision>> precisionTypes = new ArrayList<>(2);

      precisionTypes.add(VariableTrackingPrecision.isMatchingCPAClass(SMGCPA.class));
      if (predicatePrecisionIsAvailable) {
        precisionTypes.add(Predicates.instanceOf(PredicatePrecision.class));
      }

      pReached.removeSubtree(info.getKey(), info.getValue(), precisionTypes);
    }
  }

  // TODO: incorporate this into a generic version
  private Multimap<CFANode, Value> extractPrecisionIncrement(
      ARGState root, InterpolationTree<SMGState, SMGInterpolant> pInterpolationTree) {
    Multimap<CFANode, Value> increment = LinkedHashMultimap.create();

    Deque<ARGState> todo = new ArrayDeque<>();
    todo.add(pInterpolationTree.getPredecessor(root));
    while (!todo.isEmpty()) {
      final ARGState currentState = todo.removeFirst();

      if (pInterpolationTree.stateHasNonTrivialInterpolant(currentState)
          && !currentState.isTarget()) {
        SMGInterpolant itp = pInterpolationTree.getInterpolantForState(currentState);
        for (Value heapValue : itp.getAllowedHeapValues()) {
          increment.put(AbstractStates.extractLocation(currentState), heapValue);
        }
      }

      if (!pInterpolationTree.stateHasFalseInterpolant(currentState)) {
        todo.addAll(pInterpolationTree.getSuccessors(currentState));
      }
    }

    return increment;
  }

  private boolean isPredicatePrecisionAvailable(final UnmodifiableReachedSet pReached) {
    return Precisions.extractPrecisionByType(
            pReached.getPrecision(pReached.getFirstState()), PredicatePrecision.class)
        != null;
  }

  private VariableTrackingPrecision mergeValuePrecisionsForSubgraph(
      final ARGState pRefinementRoot, final UnmodifiableReachedSet pReached) {
    // get all unique precisions from the subtree
    Set<VariableTrackingPrecision> uniquePrecisions = Sets.newIdentityHashSet();
    for (ARGState descendant : ARGUtils.getNonCoveredStatesInSubgraph(pRefinementRoot)) {
      uniquePrecisions.add(extractValuePrecision(pReached.getPrecision(descendant)));
    }

    // join all unique precisions into a single precision
    VariableTrackingPrecision mergedPrecision = Iterables.getLast(uniquePrecisions);
    for (VariableTrackingPrecision precision : uniquePrecisions) {
      mergedPrecision = mergedPrecision.join(precision);
    }

    return mergedPrecision;
  }

  private VariableTrackingPrecision extractValuePrecision(Precision precision) {
    return (VariableTrackingPrecision)
        Precisions.asIterable(precision)
            .firstMatch(VariableTrackingPrecision.isMatchingCPAClass(SMGCPA.class))
            .get();
  }

  /** A simple heuristic to detect similar repeated refinements. */
  private boolean isSimilarRepeatedRefinement(
      Collection<MemoryLocation> currentIncrement, Collection<Value> currentValueIncrement) {

    boolean isSimilar = false;
    int currentRefinementId = new TreeSet<>(currentIncrement).hashCode();
    // This .hashChode() might be trouble!
    int currentHeapRefinementId = currentValueIncrement.hashCode();

    // a refinement is considered a similar, repeated refinement
    // if the current increment was added in a previous refinement, already
    if (avoidSimilarRepeatedRefinement) {
      isSimilar =
          previousRefinementIds.contains(currentRefinementId)
              && previousValueRefinementIds.contains(currentHeapRefinementId);
    }

    previousRefinementIds.add(currentRefinementId);
    previousValueRefinementIds.add(currentHeapRefinementId);

    return isSimilar;
  }

  /**
   * This method chooses a new refinement root, in a bottom-up fashion along the error path. It
   * either picks the next state on the path sharing the same CFA location, or the (only) child of
   * the ARG root, what ever comes first.
   *
   * @param currentRoot the current refinement root
   * @return the relocated refinement root
   */
  private ARGState relocateRepeatedRefinementRoot(final ARGState currentRoot) {
    repeatedRefinements.inc();
    int currentRootNumber = AbstractStates.extractLocation(currentRoot).getNodeNumber();

    ARGPath path = ARGUtils.getOnePathTo(currentRoot);
    for (ARGState currentState : path.asStatesList().reverse()) {
      // skip identity, because a new root has to be found
      if (Objects.equals(currentState, currentRoot)) {
        continue;
      }

      if (currentRootNumber == AbstractStates.extractLocation(currentState).getNodeNumber()) {
        return currentState;
      }
    }

    return Iterables.getOnlyElement(path.getFirstState().getChildren());
  }

  private ARGState relocateRefinementRoot(
      final ARGState pRefinementRoot, final boolean predicatePrecisionIsAvailable)
      throws InterruptedException {

    // no relocation needed if only running value analysis,
    // because there, this does slightly degrade performance
    // when running VA+PA, merging/covering and refinements
    // of both CPAs could lead to the state, where in two
    // subsequent refinements, two identical error paths
    // were found, through different parts of the ARG
    // So now, when running VA+PA, the refinement root
    // is set to the lowest common ancestor of those states
    // that are covered by the states in the subtree of the
    // original refinement root
    if (!predicatePrecisionIsAvailable) {
      return pRefinementRoot;
    }

    // no relocation needed if restart at top
    if (restartStrategy == RestartStrategy.ROOT) {
      return pRefinementRoot;
    }

    final ImmutableList<ARGState> descendants = pRefinementRoot.getSubgraph().toList();
    final ImmutableSet<ARGState> coveredStates =
        from(descendants)
            .transformAndConcat(ARGState::getCoveredByThis)
            .append(pRefinementRoot)
            .toSet();
    shutdownNotifier.shutdownIfNecessary();

    // no relocation needed if set of descendants is closed under coverage
    if (descendants.containsAll(coveredStates)) {
      return pRefinementRoot;
    }

    SetMultimap<ARGState, ARGState> successorRelation = LinkedHashMultimap.create();

    Deque<ARGState> todo = new ArrayDeque<>(coveredStates);
    ARGState coverageTreeRoot = null;

    // build the coverage tree, bottom-up, starting from the covered states
    while (!todo.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      final ARGState currentState = todo.removeFirst();

      if (currentState.getParents().iterator().hasNext()) {
        ARGState parentState = currentState.getParents().iterator().next();
        todo.add(parentState);
        successorRelation.put(parentState, currentState);

      } else if (coverageTreeRoot == null) {
        coverageTreeRoot = currentState;
      }
    }

    // starting from the root of the coverage tree,
    // the new refinement root is either the first node
    // having two or more children, or the original
    // refinement root, what ever comes first
    shutdownNotifier.shutdownIfNecessary();
    ARGState newRefinementRoot = coverageTreeRoot;
    while (successorRelation.get(newRefinementRoot).size() == 1
        && !pRefinementRoot.equals(newRefinementRoot)) {
      newRefinementRoot = Iterables.getOnlyElement(successorRelation.get(newRefinementRoot));
    }

    rootRelocations.inc();
    return newRefinementRoot;
  }

  /**
   * This method creates a model for the given error path.
   *
   * @param errorPath the error path for which to create the model
   * @return the model for the given error path
   */
  @Override
  protected CFAPathWithAssumptions createModel(ARGPath errorPath)
      throws InterruptedException, CPAException {
    List<Pair<SMGState, List<CFAEdge>>> concretePath = checker.evaluate(errorPath);
    if (concretePath.size() < errorPath.getInnerEdges().size()) {
      // If concretePath is shorter than errorPath, this means that errorPath is actually
      // infeasible and should have been ruled out during refinement.
      // This happens because the value analysis does not always perform fully-precise
      // counterexample checks during refinement, for example if PathConditionsCPA is used.
      // This should be fixed, because return an infeasible counterexample to the user is wrong,
      // but we cannot do this here, so we just give up creating the model.
      logger.log(Level.WARNING, "Counterexample is imprecise and may be wrong.");
      return super.createModel(errorPath);
    }
    return concreteErrorPathAllocator.allocateAssignmentsToPath(concretePath);
  }

  @Override
  protected void printAdditionalStatistics(
      PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);

    writer
        .put(rootRelocations)
        .put(repeatedRefinements)
        .put(
            "Number of unique precision increments: ",
            previousRefinementIds.size()
                + " and number of heap value precision increments: "
                + previousValueRefinementIds.size());
  }
}
