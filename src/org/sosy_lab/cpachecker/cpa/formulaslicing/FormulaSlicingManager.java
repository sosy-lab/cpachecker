package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LiveVariables;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Options(prefix="cpa.slicing")
public class FormulaSlicingManager implements IFormulaSlicingManager {
  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final InductiveWeakeningManager inductiveWeakeningManager;
  private final Solver solver;
  private final FormulaSlicingStatistics statistics;
  private final SemiCNFManager semiCNFManager;
  private final LiveVariables liveVariables;
  private final ShutdownNotifier shutdownNotifier;
  private final LoopStructure loopStructure;

  @Option(secure=true, description="Check target states reachability")
  private boolean checkTargetStates = true;

  @Option(secure = true, description="Use information from outer states to strengthen")
  private boolean useOuterStrengthening = true; // TODO: false by default? too expensive?

  @Option(secure=true, description="Replace dead variables")
  private boolean eliminateDeadVars = true;

  @Option(secure=true, description="Filter clauses by liveness")
  private boolean filterByLiveness = true;

  public FormulaSlicingManager(
      Configuration config,
      PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr,
      CFA pCfa,
      InductiveWeakeningManager pInductiveWeakeningManager,
      Solver pSolver,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    shutdownNotifier = pShutdownNotifier;
    config.inject(this);
    fmgr = pFmgr;
    pfmgr = pPfmgr;
    inductiveWeakeningManager = pInductiveWeakeningManager;
    solver = pSolver;
    bfmgr = pFmgr.getBooleanFormulaManager();
    semiCNFManager = new SemiCNFManager(fmgr, config);
    statistics = new FormulaSlicingStatistics();
    Preconditions.checkState(pCfa.getLiveVariables().isPresent() &&
      pCfa.getLoopStructure().isPresent());
    liveVariables = pCfa.getLiveVariables().get();
    loopStructure = pCfa.getLoopStructure().get();
  }

  @Override
  public Collection<? extends SlicingState> getAbstractSuccessors(
      SlicingState oldState, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    statistics.propagation.start();
    SlicingIntermediateState iOldState;

    if (oldState.isAbstracted()) {
      iOldState = abstractStateToIntermediate(oldState.asAbstracted());
    } else {
      iOldState = oldState.asIntermediate();
    }

    PathFormula outPath = pfmgr.makeAnd(iOldState.getPathFormula(), edge);
    SlicingIntermediateState out = SlicingIntermediateState.of(
        edge.getSuccessor(), outPath, iOldState.getAbstractParent());
    statistics.propagation.stop();

    return Collections.singleton(out);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(
      SlicingState pState,
      UnmodifiableReachedSet pStates, AbstractState pFullState)
      throws CPAException, InterruptedException
  {
    SlicingIntermediateState iState;
    if (pState.isAbstracted()) {

      // We do not use the other invariant => do not repeat the computation.
      return Optional.of(
          PrecisionAdjustmentResult.create(
              pState, SingletonPrecision.getInstance(), Action.CONTINUE)
      );
    } else {
      iState = pState.asIntermediate();
    }

    boolean hasTargetState = Iterables.filter(
        AbstractStates.asIterable(pFullState),
        AbstractStates.IS_TARGET_STATE).iterator().hasNext();
    boolean shouldPerformAbstraction = shouldPerformAbstraction(iState.getNode(), pFullState);
    if (hasTargetState && checkTargetStates || shouldPerformAbstraction) {
      if (isUnreachable(iState)) {
        return Optional.absent();
      }
    }

    if (shouldPerformAbstraction) {

      ImmutableList<SlicingAbstractedState> trace = findTraceToPrev(pStates, pFullState, pState);

      SlicingAbstractedState out;
      if (!trace.isEmpty()) { // Perform slicing, there is a relevant "parent" element.
        out = performSlicing(iState, trace);
      } else {
        out = toSemiClauses(iState);
      }

      return Optional.of(
          PrecisionAdjustmentResult.create(
              out, SingletonPrecision.getInstance(), Action.CONTINUE)
      );
    } else {
      return Optional.of(PrecisionAdjustmentResult.create(
          pState, SingletonPrecision.getInstance(), Action.CONTINUE));
    }
  }
  private SlicingAbstractedState toSemiClauses(SlicingIntermediateState iState)
      throws InterruptedException {
    try {
      statistics.semiCnfConversion.start();
      return toSemiClauses0(iState);
    } finally {
      statistics.semiCnfConversion.stop();
    }
  }

  /**
   * Convert the input state to the set of instantiated semi-clauses.
   */
  private SlicingAbstractedState toSemiClauses0(SlicingIntermediateState iState)
      throws InterruptedException {
    PathFormula pf = iState.getPathFormula();
    SSAMap ssa = pf.getSsa();
    CFANode node = iState.getNode();

    if (eliminateDeadVars) {

      // TODO: dead variable elimination should also be attempted on the
      // clauses which come from the previous abstraction.
      statistics.deadVarElimination.start();
      pf = fmgr.eliminateDeadVarsFixpoint(pf);
      statistics.deadVarElimination.stop();
    }

    Set<BooleanFormula> clauses = semiCNFManager.toClauses(pf.getFormula());

    SlicingAbstractedState abstractParent = iState.getAbstractParent();
    if (useOuterStrengthening) {
      clauses = Sets.union(
          clauses, abstractParent.getInstantiatedAbstraction()
      );
    }

    Set<BooleanFormula> finalClauses = new HashSet<>();
    for (BooleanFormula clause : clauses) {
      if (!fmgr.getDeadFunctionNames(clause, ssa).isEmpty()) {
        continue;
      }
      if (filterByLiveness &&
          // TODO: avoid re-calculating #extractFunctionNames twice.
          Sets.intersection(
              ImmutableSet.copyOf(liveVariables.getLiveVariableNamesForNode(node)),
              fmgr.extractFunctionNames(fmgr.uninstantiate(clause))).isEmpty()
          ) {

        continue;
      }
      finalClauses.add(fmgr.uninstantiate(clause));
    }

    return SlicingAbstractedState.ofClauses(
        finalClauses,
        ssa,
        iState.getPathFormula().getPointerTargetSet(),
        fmgr,
        iState.getNode(),
        Optional.of(iState)
    );
  }

  private  final Map<Pair<SlicingIntermediateState, ImmutableList<SlicingAbstractedState>>,
      SlicingAbstractedState> slicingCache = new HashMap<>();

  private SlicingAbstractedState performSlicing(
      SlicingIntermediateState iState,
      ImmutableList<SlicingAbstractedState> trace
  ) throws CPAException, InterruptedException {
    SlicingAbstractedState out = slicingCache.get(Pair.of(iState, trace));
    if (out != null) {
      return out;
    }

    SlicingAbstractedState first = trace.get(0);
    List<SlicingAbstractedState> traceWithoutFirst = trace.subList(1, trace.size());

    List<SlicingIntermediateState> predecessors = Lists.transform(traceWithoutFirst,
        new Function<SlicingAbstractedState, SlicingIntermediateState>() {
          @Override
          public SlicingIntermediateState apply(SlicingAbstractedState input) {
            return input.getGeneratingState().get();
          }
        });

    Set<PathFormulaWithStartSSA> taus = approximateLoopTransitions(predecessors, iState);
    Set<PathFormulaWithStartSSA> difference = Sets.difference(taus, first.getInductiveUnder());
    if (difference.isEmpty()) {
      // Just copy the state, no new information => no need to re-perform the slicing.
      return SlicingAbstractedState.copyOf(first);
    } else {
      // Slice with respect to the remaining transitions.
      taus = difference;
    }

    // now slice "first" wrt \tau
    SlicingAbstractedState abstractParent = first.getGeneratingState()
        .get().getAbstractParent();
    BooleanFormula strengthening = useOuterStrengthening ?
                                       bfmgr.and(abstractParent.getInstantiatedAbstraction())
                                                         : bfmgr.makeBoolean(true);
    Set<BooleanFormula> finalClauses = first.getAbstraction();
    for (PathFormulaWithStartSSA tau : taus) { // Intersection of all slices attained.
      shutdownNotifier.shutdownIfNecessary();
      try {
        statistics.inductiveWeakening.start();
        finalClauses = Sets.intersection(
            inductiveWeakeningManager.findInductiveWeakeningForSemiCNF(
                tau.getStartMap(),
                finalClauses,
                tau.getPathFormula(),
                strengthening
            ),
            finalClauses
        );
      } catch (SolverException pE) {
        throw new CPAException("Solver call failed", pE);
      } finally {
        statistics.inductiveWeakening.stop();
      }
    }

    out = SlicingAbstractedState.makeSliced(
        finalClauses,
        first.getSSA(), // It is crucial to use the previous SSA so that PathFormulas stay
                        // the same.
        iState.getPathFormula().getPointerTargetSet(),
        fmgr,
        iState.getNode(),
        Optional.of(iState),
        taus
    );
    slicingCache.put(Pair.of(iState, trace), out);
    return out;
  }

  /**
   * Over-approximate all possible transitions through the nested loop as a disjunction of
   * all possible inner transitions.
   */
  private Set<PathFormulaWithStartSSA> approximateLoopTransitions(
      List<SlicingIntermediateState> paths, SlicingIntermediateState iState) {
    Set<PathFormulaWithStartSSA> out = new HashSet<>(paths.size() + 1);
    for (SlicingIntermediateState s : Iterables.concat(paths, ImmutableList.of(iState))) {
      out.add(
          new PathFormulaWithStartSSA(
              s.getPathFormula().updateFormula(fmgr.simplify(s.getPathFormula().getFormula())),
              s.getAbstractParent().getSSA()
          )
      );
    }
    return out;
  }

  private boolean isUnreachable(SlicingIntermediateState iState)
      throws InterruptedException, CPAException {

    BooleanFormula prevSlice = bfmgr.and(iState.getAbstractParent().getAbstraction());
    BooleanFormula instantiatedFormula =
        fmgr.instantiate(prevSlice, iState.getAbstractParent().getSSA());
    BooleanFormula reachabilityQuery = bfmgr.and(
        iState.getPathFormula().getFormula(), instantiatedFormula);
    try {
      statistics.reachability.start();
      return solver.isUnsat(reachabilityQuery);
    } catch (SolverException pE) {
      throw new CPAException("Solver exception suppressed: ", pE);
    } finally {
      statistics.reachability.stop();
    }
  }

  @Override
  public SlicingState getInitialState(CFANode node) {
    return SlicingAbstractedState.empty(fmgr, node);
  }

  @Override
  public boolean isLessOrEqual(SlicingState pState1, SlicingState pState2) {
    Preconditions.checkState(pState1.isAbstracted() == pState2.isAbstracted());

    if (pState1.isAbstracted()) {
      return isLessOrEqualAbstracted(pState1.asAbstracted(), pState2.asAbstracted());
    } else {
      return isLessOrEqualIntermediate(pState1.asIntermediate(), pState2.asIntermediate());
    }
  }

  private boolean isLessOrEqualIntermediate(
      SlicingIntermediateState pState1,
      SlicingIntermediateState pState2) {
    SlicingIntermediateState iState1 = pState1.asIntermediate();
    SlicingIntermediateState iState2 = pState2.asIntermediate();
    return (iState1.isMergedInto(iState2) ||
        iState1.getPathFormula().getFormula().equals(iState2.getPathFormula().getFormula()))
        && isLessOrEqualAbstracted(iState1.getAbstractParent(), iState2.getAbstractParent());
  }

  private boolean isLessOrEqualAbstracted(
      SlicingAbstractedState pState1,
      SlicingAbstractedState pState2
  ) {
    // More clauses => more constraints => the state is *smaller*.
    return pState1.getAbstraction().containsAll(pState2.getAbstraction());
  }

  private SlicingIntermediateState joinIntermediateStates(
      SlicingIntermediateState newState,
      SlicingIntermediateState oldState) throws InterruptedException {

    if (!newState.getAbstractParent().equals(oldState.getAbstractParent())) {

      // No merge.
      return oldState;
    }

    if (newState.isMergedInto(oldState)) {
      return oldState;
    } else if (oldState.isMergedInto(newState)) {
      return newState;
    }

    if (oldState.getPathFormula().equals(newState.getPathFormula())) {
      return newState;
    }
    PathFormula mergedPath = pfmgr.makeOr(newState.getPathFormula(),
        oldState.getPathFormula());

    SlicingIntermediateState out = SlicingIntermediateState.of(
        oldState.getNode(), mergedPath, oldState.getAbstractParent()
    );
    newState.setMergedInto(out);
    oldState.setMergedInto(out);
    return out;
  }

  private SlicingIntermediateState abstractStateToIntermediate(
      SlicingAbstractedState pSlicingAbstractedState) {
    return SlicingIntermediateState.of(
        pSlicingAbstractedState.getNode(),
        new PathFormula(
            bfmgr.makeBoolean(true),
            pSlicingAbstractedState.getSSA(),
            pSlicingAbstractedState.getPointerTargetSet(),
            0), pSlicingAbstractedState);
  }

  private boolean shouldPerformAbstraction(CFANode node, AbstractState pFullState) {

    LoopstackState loopState = AbstractStates.extractStateByType(pFullState,
        LoopstackState.class);
    Preconditions.checkState(loopState != null, "LoopstackCPA must be enabled for formula slicing"
        + " to work.");

    // Slicing is only performed on the loop heads.
    return loopStructure.getAllLoopHeads().contains(node)
        && loopState.isLoopCounterAbstracted();
  }

  @Override
  public SlicingState merge(SlicingState pState1, SlicingState pState2) throws InterruptedException {
    Preconditions.checkState(pState1.isAbstracted() == pState2.isAbstracted());

    if (pState1.isAbstracted()) {

      // No merge.
      return pState2;
    } else {
      SlicingIntermediateState iState1 = pState1.asIntermediate();
      SlicingIntermediateState iState2 = pState2.asIntermediate();
      return joinIntermediateStates(iState1, iState2);
    }
  }

  /**
   * Return a sequence of abstracted states which get to the
   * given SlicingState.
   *
   * The trace is returned in the chronological order.
   */
  private ImmutableList<SlicingAbstractedState> findTraceToPrev(UnmodifiableReachedSet states,
                                                       AbstractState pArgState,
                                                       SlicingState state) {
    Set<SlicingAbstractedState> filteredSiblings =
        ImmutableSet.copyOf(
            AbstractStates.projectToType(
                states.getReached(pArgState),
                SlicingAbstractedState.class)
        );
    if (filteredSiblings.isEmpty()) {
      return ImmutableList.of();
    }

    LinkedList<SlicingAbstractedState> trace = new LinkedList<>();

    // We follow the chain of backpointers until we intersect something in the
    // same partition.
    // The chain is necessary as we might have nested loops.
    SlicingState a = state;
    while (true) {
      if (a.isAbstracted()) {
        SlicingAbstractedState aState = a.asAbstracted();
        trace.addFirst(aState);

        if (filteredSiblings.contains(aState)) {
          return ImmutableList.copyOf(trace);
        } else {

          if (!aState.getGeneratingState().isPresent()) {
            // Empty.
            return ImmutableList.of();
          }
          a = aState.getGeneratingState().get().getAbstractParent();
        }

      } else {
        SlicingIntermediateState iState = a.asIntermediate();
        a = iState.getAbstractParent();
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }
}
