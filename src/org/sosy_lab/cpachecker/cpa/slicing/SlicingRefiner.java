// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slicing;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
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
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;
import org.sosy_lab.cpachecker.util.refinement.PathExtractor;
import org.sosy_lab.cpachecker.util.refinement.PrefixProvider;
import org.sosy_lab.cpachecker.util.slicing.Slicer;

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
          "Allow counterexamples that are valid only on the program slice. If you set this to"
              + " `false`, you may have to set takeEagerSlice=true to avoid failed refinements. If"
              + " this is set to true, the counterexample check won't work (in general), so you"
              + " have to turn it off.")
  private boolean counterexampleCheckOnSlice = false;

  @Option(
      secure = true,
      description =
          "Use all assumptions of a target path as slicing criteria, not just the edge to the"
              + " target location.")
  private boolean takeEagerSlice = false;

  @Option(secure = true, description = "What kind of restart to do after a successful refinement")
  private RestartStrategy restartStrategy = RestartStrategy.PIVOT;

  @Option(
      secure = true,
      description =
          "How to refine the slice:\n"
              + "- CEX_ASSUME_DEPS: Add the dependencies of all counterexample assume edges to the"
              + " slice.\n"
              + "- INFEASIBLE_PREFIX_ASSUME_DEPS: Find an infeasible prefix and add the"
              + " dependencies of all assume edges that are part of the infeasible prefix to the"
              + " slice. Requires a prefix provider ('cpa.slicing.refinement.prefixProvider').\n"
              + "- CEX_FIRST_ASSUME_DEPS: Add the dependencies of the first counterexample assume"
              + " edges, that is not already part of the slice, to the slice.\n"
              + "- CEX_LAST_ASSUME_DEPS: Add the dependencies of the last counterexample assume"
              + " edges, that is not already part of the slice, to the slice.\n")
  private RefineStrategy refineStrategy = RefineStrategy.CEX_ASSUME_DEPS;

  @Option(
      secure = true,
      name = "prefixProvider",
      description =
          "Which prefix provider to use? "
              + "(give class name) If the package name starts with "
              + "'org.sosy_lab.cpachecker.', this prefix can be omitted.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
  private PrefixProvider.Factory prefixProviderFactory;

  private Slicer slicer;
  private CFA cfa;

  private enum RefineStrategy {

    /** Add the dependencies of all counterexample assume edges to the slice. */
    CEX_ASSUME_DEPS,

    /**
     * Find an infeasible prefix and add the dependencies of all assume edges that are part of the
     * infeasible prefix to the slice.
     */
    INFEASIBLE_PREFIX_ASSUME_DEPS,

    /**
     * Add the dependencies of the first counterexample assume edges, that is not already part of
     * the slice, to the slice.
     */
    CEX_FIRST_ASSUME_DEPS,

    /**
     * Add the dependencies of the last counterexample assume edges, that is not already part of the
     * slice, to the slice.
     */
    CEX_LAST_ASSUME_DEPS,
  }

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

  private final PrefixProvider prefixProvider;

  private Set<Integer> previousTargetPaths = new HashSet<>();

  private int refinementCount = 0;

  public static SlicingRefiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {

    SlicingCPA slicingCPA = CPAs.retrieveCPAOrFail(pCpa, SlicingCPA.class, SlicingRefiner.class);
    LogManager logger = slicingCPA.getLogger();
    Configuration config = slicingCPA.getConfig();
    CFA cfa = slicingCPA.getCfa();
    Slicer slicer = slicingCPA.getSlicer();

    ARGCPA argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, SlicingRefiner.class);
    PathExtractor pathExtractor = new PathExtractor(logger, config);

    CFANode initialCfaNode = cfa.getMainFunction();
    StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
    ConfigurableProgramAnalysis delegateCpa = Iterables.getOnlyElement(slicingCPA.getWrappedCPAs());
    CompositeCPA parentCompositeCpa =
        (CompositeCPA) Iterables.getOnlyElement(argCpa.getWrappedCPAs());

    TransferRelation transferRelation = parentCompositeCpa.getTransferRelation();
    try {
      AbstractState initialCompositeState =
          parentCompositeCpa.getInitialState(initialCfaNode, partition);
      Precision delegatePrecision = delegateCpa.getInitialPrecision(initialCfaNode, partition);
      WrapperPrecision parentPrecision =
          (WrapperPrecision) parentCompositeCpa.getInitialPrecision(initialCfaNode, partition);
      Precision slicingFullPrecision = new FullPrecision(delegatePrecision);
      Precision parentFullPrecision =
          parentPrecision.replaceWrappedPrecision(
              slicingFullPrecision, Predicates.instanceOf(SlicingPrecision.class));

      return new SlicingRefiner(
          pathExtractor,
          argCpa,
          slicer,
          cfa,
          transferRelation,
          initialCompositeState,
          parentPrecision,
          parentFullPrecision,
          config);

    } catch (InterruptedException pE) {
      throw new AssertionError(pE);
    }
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

    if (prefixProviderFactory != null) {
      prefixProvider = prefixProviderFactory.create(pArgCpa);
    } else {
      prefixProvider = null;
      if (refineStrategy == RefineStrategy.INFEASIBLE_PREFIX_ASSUME_DEPS) {
        throw new InvalidConfigurationException(
            "Refinement strategy "
                + RefineStrategy.INFEASIBLE_PREFIX_ASSUME_DEPS
                + " requires a prefix provider ('cpa.slicing.refinement.prefixProvider')");
      }
    }
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
      return updatePrecisionAndRemoveSubtree(pReached);
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

  private static Collection<? extends AbstractState> computeSuccessors(
      TransferRelation pTransferRelation, AbstractState pState, Precision pPrecision, CFAEdge pEdge)
      throws CPAException, InterruptedException {

    try {
      // we can always just use the delegate precision,
      // because this refinement procedure does not delegate to some other precision refinement.
      // Thus, there is no way that any initial precision could change, either way.
      return pTransferRelation.getAbstractSuccessorsForEdge(pState, pPrecision, pEdge);
    } catch (CPATransferException ex) {
      throw new CPAException(
          "Computation of successor failed for checking path: " + ex.getMessage(), ex);
    }
  }

  private static boolean isFeasible(
      ARGPath pTargetPath,
      AbstractState pInitialState,
      TransferRelation pTransferRelation,
      Precision pPrecision)
      throws CPAException, InterruptedException {

    AbstractState state = pInitialState;

    for (PathIterator iterator = pTargetPath.fullPathIterator(); iterator.hasNext(); ) {
      do {

        CFAEdge outgoingEdge = iterator.getOutgoingEdge();
        Collection<? extends AbstractState> successorSet =
            computeSuccessors(pTransferRelation, state, pPrecision, outgoingEdge);

        if (successorSet.isEmpty()) {
          return false;
        }

        state = Iterables.get(successorSet, 0);
        iterator.advance();

      } while (!iterator.isPositionWithState());
    }

    return true;
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

    return isFeasible(pTargetPath, initialState, transfer, precision);
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
   */
  private RefinedSlicingPrecision computeNewPrecision(final ReachedSet pReached)
      throws InterruptedException, CPAException {
    RefinedSlicingPrecision refinementRootsAndPrecision = getNewPrecision(pReached);
    refinementCount++;
    return refinementRootsAndPrecision;
  }

  private RefinedSlicingPrecision getNewPrecision(final ReachedSet pReached)
      throws InterruptedException, CPAException {
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa, refinementCount);
    
    Collection<ARGState> targetStates = pathExtractor.getTargetStates(argReached);
    Collection<ARGPath> targetPaths = pathExtractor.getTargetPaths(targetStates);
    Set<StateSlicingPrecision> newPrecs = new HashSet<>();
    boolean changed = false;

    for (ARGPath tp : targetPaths) {
      // we have to add the refinement root even if no new edge was added,
      // so that the precision of the corresponding ARG subtree is updated
      Set<CFAEdge> relevantEdges = getSlice(tp);
      ARGState refinementRoot = getRefinementRoot(tp, relevantEdges);
      SlicingPrecision oldPrec = mergeOnSubgraph(refinementRoot, pReached);
      SlicingPrecision newPrec = oldPrec.getNew(oldPrec.getWrappedPrec(), relevantEdges);
      newPrecs.add(new StateSlicingPrecision(refinementRoot, newPrec));

      if (!oldPrec.equals(newPrec)) {
        changed = true;
      }
    }

    return new RefinedSlicingPrecision(changed, newPrecs);
  }

  private Set<CFAEdge> getSlice(ARGPath pPath) throws InterruptedException, CPAException {

    List<CFAEdge> innerEdges = pPath.getInnerEdges();
    List<CFAEdge> criteriaEdges = new ArrayList<>(1);
    Set<CFAEdge> relevantEdges = new HashSet<>();

    List<CFAEdge> cexConstraints =
        innerEdges.stream()
            .filter(Predicates.instanceOf(CAssumeEdge.class))
            .collect(Collectors.toList());

    if (takeEagerSlice) {
      criteriaEdges.addAll(cexConstraints);
    } else {
      if (refineStrategy == RefineStrategy.INFEASIBLE_PREFIX_ASSUME_DEPS) {

        List<InfeasiblePrefix> prefixes = prefixProvider.extractInfeasiblePrefixes(pPath);
        if (!prefixes.isEmpty()) {
          Set<CFAEdge> prefixAssumeEdges =
              prefixes.get(0).getPath().getInnerEdges().stream()
                  .filter(edge -> edge.getEdgeType() == CFAEdgeType.AssumeEdge)
                  .collect(Collectors.toSet());
          criteriaEdges.addAll(prefixAssumeEdges);
        }

      } else {
        if (!isFeasible(pPath)) {

          if (refineStrategy == RefineStrategy.CEX_ASSUME_DEPS) {
            criteriaEdges.addAll(cexConstraints);
          } else if (refineStrategy == RefineStrategy.CEX_FIRST_ASSUME_DEPS
              || refineStrategy == RefineStrategy.CEX_LAST_ASSUME_DEPS) {

            SlicingPrecision slicingPrecision =
                Precisions.extractPrecisionByType(currentPrecision, SlicingPrecision.class);

            CFAEdge criteriaEdge = null;
            for (CFAEdge assumeEdge : cexConstraints) {
              if (!slicingPrecision.isRelevant(assumeEdge)) {
                criteriaEdge = assumeEdge;
                if (refineStrategy == RefineStrategy.CEX_FIRST_ASSUME_DEPS) {
                  break;
                }
              }
            }

            if (criteriaEdge != null) {
              criteriaEdges.add(criteriaEdge);
            }
          }
        }
      }
    }

    CFANode finalNode = AbstractStates.extractLocation(pPath.getLastState());
    List<CFAEdge> edgesToTarget =
        CFAUtils.enteringEdges(finalNode).filter(innerEdges::contains).toList();
    criteriaEdges.addAll(edgesToTarget);

    relevantEdges.addAll(slicer.getSlice(cfa, criteriaEdges).getRelevantEdges());

    return ImmutableSet.copyOf(relevantEdges);
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

  boolean updatePrecisionAndRemoveSubtree(final ReachedSet pReached)
      throws InterruptedException, CPAException {
    ARGReachedSet argReached = new ARGReachedSet(pReached, argCpa, refinementCount);
    RefinedSlicingPrecision refRootsAndPrecision = computeNewPrecision(pReached);
    if (refRootsAndPrecision.hasSliceChanged()) {
      updatePrecision(argReached, refRootsAndPrecision);
      return true;
    } else {
      return false;
    }
  }

  private void updatePrecision(
      final ARGReachedSet argReached, final RefinedSlicingPrecision refRootsAndPrecision)
      throws InterruptedException {
    for (StateSlicingPrecision prec : refRootsAndPrecision.getStatePrecisions()) {
      ARGState state = prec.getState();
      if (!state.isDestroyed()) {
        argReached.removeSubtree(
            state, prec.getPrecision(), Predicates.instanceOf(SlicingPrecision.class));
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

  private static final class StateSlicingPrecision {

    private final ARGState state;
    private final SlicingPrecision precision;

    private StateSlicingPrecision(ARGState pState, SlicingPrecision pPrecision) {
      state = pState;
      precision = pPrecision;
    }

    public ARGState getState() {
      return state;
    }

    public SlicingPrecision getPrecision() {
      return precision;
    }
  }

  private static final class RefinedSlicingPrecision {

    private final boolean sliceChanged;
    private final Set<StateSlicingPrecision> precisions;

    private RefinedSlicingPrecision(boolean pSliceChanged, Set<StateSlicingPrecision> pPrecisions) {

      sliceChanged = pSliceChanged;
      precisions = pPrecisions;
    }

    public boolean hasSliceChanged() {
      return sliceChanged;
    }

    public Set<StateSlicingPrecision> getStatePrecisions() {
      return precisions;
    }
  }
}
