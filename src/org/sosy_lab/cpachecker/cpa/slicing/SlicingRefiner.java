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
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPathBuilder;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.slicing.SlicingPrecision.FullPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph.TraversalDirection;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;

/**
 * Refiner for {@link SlicingPrecision}. Precision refinement is done through program slicing [1].
 *
 * <p>For a set of infeasible paths, the last edge of each path is used as a slicing criterion. The
 * union of the corresponding program slices are the increment to the existing slicing precision.
 *
 * <p>[1] Weiser, 1984: Program Slicing.
 */
@Options(prefix = "cpa.slicing.refinement")
public class SlicingRefiner implements Refiner {

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
        "Use all assumptions of a target path as counterexamples, not just the edge to the target"
            + " location. Always set this to `true` if you use SlicingDelegatingRefiner."
  )
  private boolean takeEagerSlice = true;

  private final PathExtractor pathExtractor;
  private final ARGCPA argCpa;
  private final DependenceGraph depGraph;

  private final TransferRelation transfer;
  private final WrapperPrecision currentPrecision;
  private final Precision fullPrecision;
  private final AbstractState initialState;

  private Set<Integer> previousTargetPaths = new HashSet<>();

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

    if (compositePrecision instanceof WrapperPrecision) {
      Precision fullSlicingPrecision = new FullPrecision(wrappedPrecision);
      Precision fullArgPrecision =
          compositePrecision.replaceWrappedPrecision(
              fullSlicingPrecision, Predicates.instanceOf(SlicingPrecision.class));

      return new SlicingRefiner(
          pathExtractor,
          argCpa,
          cfa.getDependenceGraph()
              .orElseThrow(
                  () -> new InvalidConfigurationException("Dependence graph of CFA " + "missing")),
          transferRelation,
          initialCompositeState,
          compositePrecision,
          fullArgPrecision,
          config);

    } else {
      throw new AssertionError(
          "Precision of CompositeCPA is not wrapper precision: " + compositePrecision);
    }
  }

  private SlicingRefiner(
      final PathExtractor pPathExtractor,
      final ARGCPA pArgCpa,
      final DependenceGraph pDepGraph,
      final TransferRelation pTransferRelation,
      final AbstractState pInitialState,
      final WrapperPrecision pCurrentArgPrecision,
      final Precision pFullPrecision,
      final Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    pathExtractor = pPathExtractor;
    argCpa = pArgCpa;
    depGraph = pDepGraph;
    currentPrecision = pCurrentArgPrecision;
    fullPrecision = pFullPrecision;
    transfer = pTransferRelation;
    initialState = pInitialState;
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa);

    Collection<ARGState> targetStates = pathExtractor.getTargetStates(argReached);
    List<ARGPath> targetPaths = pathExtractor.getTargetPaths(targetStates);
    boolean anyFeasible = false;
    for (ARGPath tp : targetPaths) {
      CounterexampleInfo cex = isFeasible(tp, pReached);
      if (!cex.isSpurious()) {
        tp.getLastState().addCounterexampleInformation(cex);
        anyFeasible = true;
      }
    }

    if (anyFeasible) {
      return false;
    } else {
      updatePrecisionAndRemoveSubtree(pReached);
      return true;
    }
  }

  /**
   * Checks whether the given target path is feasible. Uses the transfer relation of the CPA wrapped
   * by the {@link SlicingCPA} to perform this check.
   *
   * @param pTargetPath target path to check for feasibility
   * @return a {@link CounterexampleInfo} that (among others) stores whether the target path is
   *     feasible
   * @throws CPAException if the wrapped transfer relation throws an Exception during the check
   * @throws InterruptedException
   */
  CounterexampleInfo isFeasible(final ARGPath pTargetPath, final ReachedSet pReached)
      throws CPAException, InterruptedException {

    PathIterator iterator = pTargetPath.fullPathIterator();
    CFAEdge outgoingEdge;
    Collection<? extends AbstractState> successorSet;
    AbstractState state = initialState;
    ARGState asArgState = new ARGState(state, null);
    ARGPathBuilder targetPathWithCorrectValues = ARGPath.builder();
    Precision precision;
    if (counterexampleCheckOnSlice) {
      Precision fullSlice = getNewPrecision(pReached).getSecond();
      precision =
          currentPrecision.replaceWrappedPrecision(
              fullSlice, Predicates.instanceOf(fullSlice.getClass()));
    } else {
      precision = fullPrecision;
    }

    try {
      while (iterator.hasNext()) {
        do {
          outgoingEdge = iterator.getOutgoingEdge();
          targetPathWithCorrectValues.add(asArgState, outgoingEdge);
          // we can always just use the delegate precision,
          // because this refinement procedure does not delegate to some other precision refinement.
          // Thus, there is no way that any initial precision could change, either way.
          successorSet = transfer.getAbstractSuccessorsForEdge(state, precision, outgoingEdge);
          if (successorSet.isEmpty()) {
            return CounterexampleInfo.spurious();
          } else {
            // extract singleton successor state
            state = Iterables.get(successorSet, 0);
            asArgState = new ARGState(state, asArgState);
          }
          iterator.advance();
        } while (!iterator.isPositionWithState());
      }
      return CounterexampleInfo.feasibleImprecise(targetPathWithCorrectValues.build(asArgState));
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
  @CanIgnoreReturnValue
  Set<ARGState> updatePrecision(final ReachedSet pReached) throws RefinementFailedException {
    Pair<Set<ARGState>, SlicingPrecision> refinementRootsAndPrecision = getNewPrecision(pReached);
    Precision newPrec = refinementRootsAndPrecision.getSecond();
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa);
    argReached.updatePrecisionGlobally(newPrec, Predicates.instanceOf(SlicingPrecision.class));
    return refinementRootsAndPrecision.getFirst();
  }

  private Pair<Set<ARGState>, SlicingPrecision> getNewPrecision(final ReachedSet pReached)
      throws RefinementFailedException {
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa);

    Collection<ARGState> targetStates = pathExtractor.getTargetStates(argReached);
    Collection<ARGPath> targetPaths = pathExtractor.getTargetPaths(targetStates);
    Set<CFAEdge> relevantEdges = new HashSet<>();
    Set<ARGState> refinementRoots = new HashSet<>();
    for (ARGPath tp : targetPaths) {
      Collection<ARGState> relevantStates = getRelevantStates(tp);
      for (ARGState criterion : relevantStates) {
        Set<CFAEdge> slice = getSlice(criterion);
        refinementRoots.add(getRefinementRoot(tp, slice));
        relevantEdges.addAll(slice);
      }
    }

    SlicingPrecision oldPrec = extractSlicingPrecision(pReached, pReached.getFirstState());
    SlicingPrecision newPrec = oldPrec.getNew(oldPrec.getWrappedPrec(), relevantEdges);
    return Pair.of(refinementRoots, newPrec);
  }

  private Collection<ARGState> getRelevantStates(final ARGPath pTargetPath) {
    List<ARGState> relevantStates = new ArrayList<>();
    ARGState target = pTargetPath.getLastState();
    relevantStates.add(target);

    if (takeEagerSlice) {
      PathIterator it = pTargetPath.pathIterator();
      while (it.hasNext()) {
        it.advance(); // skip the first state
        CFAEdge incoming = it.getIncomingEdge();
        if (incoming != null && incoming.getEdgeType() == CFAEdgeType.AssumeEdge) {
          relevantStates.add(it.getAbstractState());
        }
      }
    }
    return relevantStates;
  }

  private void updatePrecisionAndRemoveSubtree(final ReachedSet pReached)
      throws RefinementFailedException, InterruptedException {
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa);
    Set<ARGState> refinementRoots = updatePrecision(pReached);
    for (ARGState r : refinementRoots) {
      if (!r.isDestroyed()) {
        argReached.removeSubtree(r);
      }
    }
  }

  /** Returns the program slice for the given {@link ARGState} as slicing criterion. */
  Set<CFAEdge> getSlice(final ARGState pCriterion) {
    CFANode loc = AbstractStates.extractLocation(pCriterion);
    List<CFAEdge> criteria = CFAUtils.enteringEdges(loc).toList();

    Set<CFAEdge> relevantEdges = new HashSet<>();
    for (CFAEdge c : criteria) {
      relevantEdges.addAll(depGraph.getReachable(c, TraversalDirection.BACKWARD));
    }
    return relevantEdges;
  }

  private ARGState getRefinementRoot(final ARGPath pPath, final Set<CFAEdge> relevantEdges) {
    PathIterator iterator = pPath.fullPathIterator();
    while (iterator.hasNext()) {
      if (relevantEdges.contains(iterator.getOutgoingEdge())) {
        return iterator.getNextAbstractState();
      }
      iterator.advance();
    }
    throw new AssertionError("Infeasible target path has empty program slice");
  }

  private static SlicingPrecision extractSlicingPrecision(
      final ReachedSet pReached, final AbstractState pState) {
    return (SlicingPrecision)
        Precisions.asIterable(pReached.getPrecision(pState))
            .filter(Predicates.instanceOf(SlicingPrecision.class))
            .first()
            .orNull();
  }
}
