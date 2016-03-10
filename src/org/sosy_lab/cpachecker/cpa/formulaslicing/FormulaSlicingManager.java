package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Options(prefix="cpa.slicing")
public class FormulaSlicingManager implements IFormulaSlicingManager {
  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final CFA cfa;
  private final InductiveWeakeningManager inductiveWeakeningManager;
  private final Solver solver;
  private final Stats statistics;
  private final SemiCNFManager semiCNFManager;
  private final LogManager logger;

  @Option(secure=true, description="Check target states reachability")
  private boolean checkTargetStates = true;

  @Option(secure = true, description="Use information from outer states to strengthen")
  private boolean useOuterStrengthening = true; // TODO: false by default? too expensive?

  @Option(secure=true, description="Replace dead variables")
  private boolean eliminateDeadVars = true;

  public FormulaSlicingManager(
      Configuration config,
      PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr,
      CFA pCfa,
      InductiveWeakeningManager pInductiveWeakeningManager, Solver pSolver, LogManager pLogger)
      throws InvalidConfigurationException {
    logger = pLogger;
    config.inject(this);
    fmgr = pFmgr;
    pfmgr = pPfmgr;
    cfa = pCfa;
    inductiveWeakeningManager = pInductiveWeakeningManager;
    solver = pSolver;
    bfmgr = pFmgr.getBooleanFormulaManager();
    semiCNFManager = new SemiCNFManager(fmgr, config);
    statistics = new Stats();
  }

  @Override
  public Collection<? extends SlicingState> getAbstractSuccessors(
      SlicingState oldState, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    SlicingIntermediateState iOldState;

    if (oldState.isAbstracted()) {
      iOldState = abstractStateToIntermediate(oldState.asAbstracted());
    } else {
      iOldState = oldState.asIntermediate();
    }

    PathFormula outPath = pfmgr.makeAnd(iOldState.getPathFormula(), edge);
    SlicingIntermediateState out = SlicingIntermediateState.of(
        edge.getSuccessor(), outPath, iOldState.getAbstractParent());

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

      // Caching the computation.
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

      List<SlicingAbstractedState> trace = findTraceToPrev(pStates, pFullState, pState);

      final SSAMap ssa = iState.getPathFormula().getSsa();
      Set<BooleanFormula> finalClauses;
      boolean isSliced;
      if (!trace.isEmpty() && !Iterables.getLast(trace).isInitial()) {
        isSliced = true;
        SlicingAbstractedState first = trace.get(0);
        List<SlicingAbstractedState> traceWithoutFirst = trace.subList(1, trace.size());
        List<SlicingIntermediateState> predecessors = Lists.transform(traceWithoutFirst,
            new Function<SlicingAbstractedState, SlicingIntermediateState>() {
              @Override
              public SlicingIntermediateState apply(SlicingAbstractedState input) {
                return input.getGeneratingState().get();
              }
            });


        List<Pair<PathFormula, SSAMap>> taus = approximateLoopTransitions(predecessors, iState);

        // now slice "first" wrt \tau
        BooleanFormula strengthening = useOuterStrengthening ?
                                       bfmgr.and(first.getGeneratingState()
                                           .get().getAbstractParent().getAbstraction())
                                       : bfmgr.makeBoolean(true);
        finalClauses = first.getAbstraction();
        for (Pair<PathFormula, SSAMap> tau : taus) {
          try {
            finalClauses = Sets.intersection(
                inductiveWeakeningManager.findInductiveWeakeningForSemiCNF(
                    tau.getSecondNotNull(),
                    finalClauses,
                    tau.getFirstNotNull(),
                    strengthening
                ),
                finalClauses
            );
          } catch (SolverException pE) {
            throw new CPAException("Solver call failed", pE);
          }
        }
      } else {

        // Just convert to a set of clauses.
        PathFormula pf = iState.getPathFormula();
        if (eliminateDeadVars) {
          pf = fmgr.eliminateDeadVarsFixpoint(pf);
        }

        Set<BooleanFormula> clauses =
            semiCNFManager.toClauses(pf.getFormula());

        finalClauses = new HashSet<>();
        for (BooleanFormula clause : clauses) {
          if (fmgr.getDeadFunctionNames(clause, ssa).isEmpty()) {
            finalClauses.add(fmgr.uninstantiate(clause));
          }
        }
        if (useOuterStrengthening) {
          finalClauses = Sets.union(finalClauses, iState.getAbstractParent().getAbstraction());
        }
        isSliced = false;
      }
      SlicingAbstractedState out = SlicingAbstractedState.of(
          finalClauses, ssa, iState.getPathFormula().getPointerTargetSet(),
          fmgr,
          iState.getNode(),
          Optional.of(iState),
          isSliced
      );
      return Optional.of(
          PrecisionAdjustmentResult.create(
              out, SingletonPrecision.getInstance(), Action.CONTINUE)
      );
    } else {
      return Optional.of(PrecisionAdjustmentResult.create(
          pState, SingletonPrecision.getInstance(), Action.CONTINUE));
    }


  }

  private List<Pair<PathFormula, SSAMap>> approximateLoopTransitions(
      List<SlicingIntermediateState> paths,
      SlicingIntermediateState iState
      ) throws InterruptedException {
    List<Pair<PathFormula, SSAMap>> out = new ArrayList<>(paths.size() + 1);
    for (SlicingIntermediateState s : Iterables.concat(paths, ImmutableList.of(iState))) {
      out.add(
          Pair.of(
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
      boolean out = solver.isUnsat(reachabilityQuery);
      if (out) {

      }
      return out;
    } catch (SolverException pE) {
      throw new CPAException("Solver exception suppressed: ", pE);
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
    // Slicing is only performed on the loop heads.
    return cfa.getLoopStructure().get().getAllLoopHeads().contains(node)
        && (loopState == null || loopState.isLoopCounterAbstracted());
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

  private List<SlicingAbstractedState> findTraceToPrev(UnmodifiableReachedSet states,
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

    // All states on the way, *excluding* the very first appearance.
    List<SlicingAbstractedState> trace = new ArrayList<>();

    // We follow the chain of backpointers.
    // The chain is necessary as we might have nested loops.
    SlicingState a = state;
    while (true) {
      if (a.isAbstracted()) {
        SlicingAbstractedState aState = a.asAbstracted();
        trace.add(aState);

        if (filteredSiblings.contains(aState)) {
          if (aState.isSliced()) {
            return ImmutableList.of();
          } else {
            return trace;
          }
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

  /**
   * Statistics for formula slicing.
   */
  private static class Stats implements Statistics {
    final Timer formulaSlicingTimer = new Timer();

    @Override
    public void printStatistics(PrintStream out, Result result,
                                ReachedSet reached) {
      out.printf("Time spent in formula slicing: %s (Max: %s), (Avg: %s)%n",
          formulaSlicingTimer,
          formulaSlicingTimer.getMaxTime().formatAs(TimeUnit.SECONDS),
          formulaSlicingTimer.getAvgTime().formatAs(TimeUnit.SECONDS));
    }

    @Override
    public String getName() {
      return "Formula Slicing Manager";
    }
  }
}
