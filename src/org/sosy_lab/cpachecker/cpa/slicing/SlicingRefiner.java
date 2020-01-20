/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.slicing;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.slicing.SlicingPrecision.FullPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException.Reason;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.slicing.Slicer;
import org.sosy_lab.cpachecker.util.slicing.SlicerFactory;

/**
 * Refiner for {@link SlicingPrecision}. Precision refinement is done through program slicing [1].
 *
 * <p>For a set of infeasible paths, the last edge of each path is used as a slicing criterion. The
 * union of the corresponding program slices are the increment to the existing slicing precision.
 *
 * <p>[1] Weiser, 1984: Program Slicing.
 */
@Options(prefix = "cpa.slicing.refinement")
public class SlicingRefiner implements Refiner, StatisticsProvider {

  @Option(
    secure = true,
    description =
        "Allow counterexamples that are valid only on the program slice."
            + " If you set this to `false`, you may have to set takeEagerSlice=true to avoid failed "
            + "refinements. If this is set to true, the counterexample check won't work (in "
            + "general), so you have to turn it off."
  )
  private boolean counterexampleCheckOnSlice = false;

  @Option(
      secure = true,
      description =
          "Use all assumptions of a target path as slicing criteria, not just the edge to the target"
              + " location.")
  private boolean takeEagerSlice = false;

  @Option(
      secure = true,
      description =
          "Add all assumptions of an infeasible target path to the slice, in addition"
              + " to the original slice")
  private boolean addCexConstraintsToSlice = true;

  @Option(secure = true, description = "What kind of restart to do after a successful refinement")
  private RestartStrategy restartStrategy = RestartStrategy.PIVOT;

  private Slicer slicer;
  private CFA cfa;

  private enum RestartStrategy {
    /**
     * Restart at the pivot element, i.e., the first abstract state for which the precision changes.
     */
    PIVOT,
    /** Restart at the root, i.e., the initial abstract state. * */
    ROOT
  }

  private final PathExtractor pathExtractor;
  private final ARGCPA argCpa;

  private final TransferRelation transfer;
  private final WrapperPrecision currentPrecision;
  private final Precision fullPrecision;
  private final AbstractState initialState;

  private Set<Integer> previousTargetPaths = new HashSet<>();

  private int refinementCount = 0;

  public static SlicingRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    SlicingCPA slicingCPA = CPAs.retrieveCPAOrFail(pCpa, SlicingCPA.class, SlicingRefiner.class);
    LogManager logger = slicingCPA.getLogger();
    Configuration config = slicingCPA.getConfig();

    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, SlicingRefiner.class);
    PathExtractor pathExtractor = new PathExtractor(logger, config);
    CFA cfa = slicingCPA.getCfa();

    CFANode initialCfaNode = cfa.getMainFunction();
    StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
    ImmutableList<ConfigurableProgramAnalysis> wrappedCpas = slicingCPA.getWrappedCPAs();
    assert wrappedCpas.size() == 1
        : "Slicing CPA is not wrapping exactly one CPA, but " + wrappedCpas.size();
    ConfigurableProgramAnalysis wrapped = wrappedCpas.get(0);
    CompositeCPA outerCompositeCpa = (CompositeCPA) argCpa.getWrappedCPAs().get(0);

    TransferRelation transferRelation = outerCompositeCpa.getTransferRelation();
    AbstractState initialCompositeState;
    Precision wrappedPrecision;
    WrapperPrecision compositePrecision;
    try {
      initialCompositeState = outerCompositeCpa.getInitialState(initialCfaNode, partition);
      wrappedPrecision = wrapped.getInitialPrecision(initialCfaNode, partition);
      compositePrecision =
          (WrapperPrecision) outerCompositeCpa.getInitialPrecision(initialCfaNode, partition);
    } catch (InterruptedException pE) {
      throw new AssertionError(pE);
    }

    Precision fullSlicingPrecision = new FullPrecision(wrappedPrecision);
    Precision fullArgPrecision =
        compositePrecision.replaceWrappedPrecision(
            fullSlicingPrecision, Predicates.instanceOf(SlicingPrecision.class));

    ShutdownNotifier shutdownNotifier = slicingCPA.getShutdownNotifier();
    Slicer slicer = new SlicerFactory().create(logger, shutdownNotifier, config, cfa);

    return new SlicingRefiner(
        pathExtractor,
        argCpa,
        slicer,
        cfa,
        transferRelation,
        initialCompositeState,
        compositePrecision,
        fullArgPrecision,
        config);
  }

  private SlicingRefiner(
      final PathExtractor pPathExtractor,
      final ARGCPA pArgCpa,
      final Slicer pSlicer,
      final CFA pCfa,
      final TransferRelation pTransferRelation,
      final AbstractState pInitialState,
      final WrapperPrecision pCurrentArgPrecision,
      final Precision pFullPrecision,
      final Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    pathExtractor = pPathExtractor;
    argCpa = pArgCpa;
    slicer = pSlicer;
    cfa = pCfa;
    currentPrecision = pCurrentArgPrecision;
    fullPrecision = pFullPrecision;
    transfer = pTransferRelation;
    initialState = pInitialState;
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    boolean anyFeasible = false;

    for (ARGPath tp : getTargetPaths(pReached)) {
      int targetPathId = obtainTargetPathId(tp);
      if (previousTargetPaths.contains(targetPathId)) {
        throw new RefinementFailedException(Reason.RepeatedCounterexample, tp);
      }
      if (isFeasible(tp)) {
        CounterexampleInfo cex = getCounterexample(tp);
        tp.getLastState().addCounterexampleInformation(cex);
        anyFeasible = true;
      } else {

        previousTargetPaths.add(targetPathId);
      }
    }

    if (anyFeasible) {
      return false;
    } else {
      updatePrecisionAndRemoveSubtree(pReached);
      return true;
    }
  }

  CounterexampleInfo getCounterexample(ARGPath pTargetPath) {
    return CounterexampleInfo.feasibleImprecise(pTargetPath);
  }

  Collection<ARGPath> getTargetPaths(ReachedSet pReached) throws RefinementFailedException {
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa, refinementCount);

    Collection<ARGState> targetStates = pathExtractor.getTargetStates(argReached);
    List<ARGPath> targetPaths = pathExtractor.getTargetPaths(targetStates);

    return targetPaths;
  }

  private int obtainTargetPathId(final ARGPath pTargetPath) {
    return pTargetPath.toString().hashCode();
  }

  /**
   * Checks whether the given target path is feasible. Uses the transfer relation of the CPA wrapped
   * by the {@link SlicingCPA} to perform this check.
   *
   * @param pTargetPath target path to check for feasibility
   * @return a {@link CounterexampleInfo} that (among others) stores whether the target path is
   *     feasible
   * @throws CPAException if the wrapped transfer relation throws an Exception during the check
   * @throws InterruptedException if feasibility check got interrupted
   */
  boolean isFeasible(final ARGPath pTargetPath) throws CPAException, InterruptedException {

    PathIterator iterator = pTargetPath.fullPathIterator();
    CFAEdge outgoingEdge;
    Collection<? extends AbstractState> successorSet;
    AbstractState state = initialState;
    Precision precision;
    if (counterexampleCheckOnSlice) {
      Set<CFAEdge> sliceForTargetPath = getSlice(pTargetPath);
      SlicingPrecision fullSlicingPrecision =
          Precisions.extractPrecisionByType(fullPrecision, SlicingPrecision.class);
      assert fullSlicingPrecision != null
          : "No " + SlicingPrecision.class.getSimpleName() + " in precision: " + fullPrecision;

      SlicingPrecision targetPathSlice =
          new SlicingPrecision(fullSlicingPrecision.getWrappedPrec(), sliceForTargetPath);
      precision =
          currentPrecision.replaceWrappedPrecision(
              targetPathSlice, Predicates.instanceOf(SlicingPrecision.class));
    } else {
      precision = fullPrecision;
    }

    try {
      while (iterator.hasNext()) {
        do {
          outgoingEdge = iterator.getOutgoingEdge();
          // we can always just use the delegate precision,
          // because this refinement procedure does not delegate to some other precision refinement.
          // Thus, there is no way that any initial precision could change, either way.
          successorSet = transfer.getAbstractSuccessorsForEdge(state, precision, outgoingEdge);
          if (successorSet.isEmpty()) {
            return false;
          }
          // extract singleton successor state
          state = Iterables.get(successorSet, 0);
          iterator.advance();
        } while (!iterator.isPositionWithState());
      }
      return true;
    } catch (CPATransferException e) {
      throw new CPAException(
          "Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

  /**
   * Updates the precision of the given {@link ReachedSet} and returns the roots of the subtrees for
   * which the precision change is relevant. This means, in general, that these subtrees should be
   * removed and recomputed.
   *
   * <p>Precision is updated based on all target paths in the reached set, using program slicing
   * with the last CFA edge of each target path as slicing criterion. Precision is always updated
   * for <b>all</b> states in the reached set.
   *
   * @param pReached reached set to update precision for
   * @return the refinement roots in the reached set
   * @throws RefinementFailedException thrown if the given reached set does not contain target paths
   *     valid for refinement
   */
  Set<Pair<ARGState, SlicingPrecision>> computeNewPrecision(final ReachedSet pReached)
      throws RefinementFailedException, InterruptedException {
    Set<Pair<ARGState, SlicingPrecision>> refinementRootsAndPrecision = getNewPrecision(pReached);
    refinementCount++;
    return refinementRootsAndPrecision;
  }

  private Set<Pair<ARGState, SlicingPrecision>> getNewPrecision(final ReachedSet pReached)
      throws RefinementFailedException, InterruptedException {
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa, refinementCount);

    Collection<ARGState> targetStates = pathExtractor.getTargetStates(argReached);
    Collection<ARGPath> targetPaths = pathExtractor.getTargetPaths(targetStates);
    Set<Pair<ARGState, SlicingPrecision>> newPrecs = new HashSet<>();
    for (ARGPath tp : targetPaths) {
      // we have to add the refinement root even if no new edge was added,
      // so that the precision of the corresponding ARG subtree is updated
      Set<CFAEdge> relevantEdges = getSlice(tp);
      ARGState refinementRoot = getRefinementRoot(tp, relevantEdges);
      SlicingPrecision oldPrec = mergeOnSubgraph(refinementRoot, pReached);
      SlicingPrecision newPrec = oldPrec.getNew(oldPrec.getWrappedPrec(), relevantEdges);
      newPrecs.add(Pair.of(refinementRoot, newPrec));
    }

    return newPrecs;
  }

  private Set<CFAEdge> getSlice(ARGPath pPath) throws InterruptedException {
    List<CFAEdge> innerEdges = pPath.getInnerEdges();

    Set<CFAEdge> cexConstraints =
        innerEdges.stream()
            .filter(Predicates.instanceOf(CAssumeEdge.class))
            .collect(Collectors.toSet());

    List<CFAEdge> criteriaEdges = new ArrayList<>(1);
    if (takeEagerSlice) {
      criteriaEdges.addAll(cexConstraints);
    }
    CFANode finalNode = AbstractStates.extractLocation(pPath.getLastState());
    List<CFAEdge> edgesToTarget =
        CFAUtils.enteringEdges(finalNode).filter(innerEdges::contains).toList();
    criteriaEdges.addAll(edgesToTarget);

    Set<CFAEdge> relevantEdges = slicer.getSlice(cfa, criteriaEdges).getRelevantEdges();

    if (addCexConstraintsToSlice) {
      // this must always be added _after_ adding the slices, otherwise
      // slices may be incomplete
      relevantEdges = Sets.union(relevantEdges, ImmutableSet.copyOf(cexConstraints));
    }

    return relevantEdges;
  }

  private SlicingPrecision mergeOnSubgraph(
      final ARGState pRefinementRoot, final ReachedSet pReached) {
    // get all unique precisions from the subtree
    Set<SlicingPrecision> uniquePrecisions = Sets.newIdentityHashSet();
    for (ARGState descendant : ARGUtils.getNonCoveredStatesInSubgraph(pRefinementRoot)) {
      uniquePrecisions.add(extractSlicingPrecision(pReached, descendant));
    }

    // join all unique precisions into a single precision
    SlicingPrecision start = Iterables.getLast(uniquePrecisions);
    Set<CFAEdge> allRelevant = new HashSet<>(start.getRelevant());
    for (SlicingPrecision precision : uniquePrecisions) {
      allRelevant.addAll(precision.getRelevant());
    }

    return new SlicingPrecision(start.getWrappedPrec(), allRelevant);
  }

  private void updatePrecisionAndRemoveSubtree(final ReachedSet pReached)
      throws RefinementFailedException, InterruptedException {
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa, refinementCount);
    Set<Pair<ARGState, SlicingPrecision>> refRootsAndPrecision = computeNewPrecision(pReached);
    for (Pair<ARGState, SlicingPrecision> p : refRootsAndPrecision) {
      ARGState r = p.getFirst();
      if (!r.isDestroyed()) {
        argReached.removeSubtree(r, p.getSecond(), Predicates.instanceOf(SlicingPrecision.class));
      }
    }
  }

  private ARGState getRefinementRoot(final ARGPath pPath, final Collection<CFAEdge> relevantEdges) {
    switch (restartStrategy) {
      case PIVOT:
        PathIterator iterator = pPath.fullPathIterator();
        while (iterator.hasNext()) {
          if (relevantEdges.contains(iterator.getOutgoingEdge())) {
            return iterator.getNextAbstractState();
          }
          iterator.advance();
        }
        throw new AssertionError("Infeasible target path has empty program slice");
      case ROOT:
        // use first state after ARG root as refinement root
        return pPath.asStatesList().get(1);
      default:
        throw new AssertionError("Unhandled restart strategy: " + restartStrategy);
    }
  }

  private static SlicingPrecision extractSlicingPrecision(
      final ReachedSet pReached, final AbstractState pState) {
    return (SlicingPrecision)
        Precisions.asIterable(pReached.getPrecision(pState))
            .filter(Predicates.instanceOf(SlicingPrecision.class))
            .first()
            .orNull();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (slicer instanceof StatisticsProvider) {
      ((StatisticsProvider) slicer).collectStatistics(pStatsCollection);
    }
  }
}
