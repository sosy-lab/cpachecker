package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

@Options(prefix="cpa.slicing")
public class FormulaSlicingManager implements IFormulaSlicingManager {
  private final PathFormulaManager pfmgr;
  private final BooleanFormulaManager bfmgr;
  private final FormulaManagerView fmgr;
  private final LogManager logger;
  private final CFA cfa;
  private final LoopTransitionFinder loopTransitionFinder;
  private final InductiveWeakeningManager inductiveWeakeningManager;
  private final Solver solver;
  private final FormulaSlicingStatistics statistics;

  @Option(secure=true, description="Check target states reachability")
  private boolean checkTargetStates = true;

  @Option(secure=true, description="Check abstract state subsumption using SMT")
  private boolean expensiveSubsumptionCheck = true;

  public FormulaSlicingManager(
      Configuration config,
      PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr, LogManager pLogger,
      CFA pCfa,
      LoopTransitionFinder pLoopTransitionFinder,
      InductiveWeakeningManager pInductiveWeakeningManager, Solver pSolver,
      FormulaSlicingStatistics pStatistics)
      throws InvalidConfigurationException {
    config.inject(this);
    fmgr = pFmgr;
    pfmgr = pPfmgr;
    logger = pLogger;
    cfa = pCfa;
    loopTransitionFinder = pLoopTransitionFinder;
    inductiveWeakeningManager = pInductiveWeakeningManager;
    solver = pSolver;
    bfmgr = pFmgr.getBooleanFormulaManager();
    statistics = pStatistics;
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
  public SlicingState join(SlicingState newState,
      SlicingState oldState) throws CPAException, InterruptedException {
    Preconditions.checkState(oldState.isAbstracted() == newState.isAbstracted());

    SlicingState out;
    if (oldState.isAbstracted()) {
      out = joinAbstractedStates(oldState.asAbstracted(),
          newState.asAbstracted());
    } else {
      out = joinIntermediateStates(oldState.asIntermediate(),
          newState.asIntermediate());
    }

    return out;
  }


  /**
   * Slicing is performed in the {@code strengthen} call.
   */
  @Override
  public Collection<? extends SlicingState> strengthen(
      SlicingState state, List<AbstractState> otherStates,
      CFAEdge pCFAEdge)
      throws CPATransferException, InterruptedException {
    Preconditions.checkState(!state.isAbstracted());

    CFANode successor = pCFAEdge.getSuccessor();

    SlicingIntermediateState iState = state.asIntermediate();

    if (shouldPerformAbstraction(successor)) {
      Optional<SlicingAbstractedState> existingSlice = findSliceIfExists(
          iState);

      if (existingSlice.isPresent()) {
        return Collections.singleton(existingSlice.get());
      }

      SlicingAbstractedState parent = iState.getAbstractParent();

      Optional<SlicingAbstractedState> outerState = findOuterStateIfExists(
          iState
      );

      // Over-approximation of the loop effect: disjunction over all possible
      // transitions through the loop.
      PathFormula possibleLoopTransitions = loopTransitionFinder.generateLoopTransition(
          iState.getPathFormula().getSsa(),
          iState.getPathFormula().getPointerTargetSet(),
          successor);

      BooleanFormula inductiveWeakening;

      // We attempt to slice the previously obtained weakening as well.
      BooleanFormula strengthening = fmgr.instantiate(
          parent.getAbstraction(), parent.getSSA());

      // If we are inside the outer loop, then:
      // 1) Slice obtained from the ancestor can be safely conjoined to the
      // slice we have obtained (with proper instantiation)
      // 2) The strengthening can contain the predicate both before and
      // after the transition.
      PathFormula toSlice = iState.getPathFormula();
      if (outerState.isPresent()) {
        strengthening = bfmgr.and(
            strengthening,
            fmgr.instantiate(
              outerState.get().getAbstraction(),
                iState.getPathFormula().getSsa()
            )
        );
      } else {

        // Slice the previously obtained weakening again.
        toSlice = toSlice.updateFormula(
            bfmgr.and(strengthening, toSlice.getFormula())
        );
      }
      toSlice = toSlice.updateFormula(fmgr.simplify(toSlice.getFormula()));

      try {
        statistics.formulaSlicingTimer.start();
        inductiveWeakening =
            inductiveWeakeningManager.slice(
                toSlice,
                possibleLoopTransitions, strengthening);
      } catch(SolverException ex) {
        throw new CPATransferException("Originating exception: ", ex);
      } finally {
        statistics.formulaSlicingTimer.stop();
      }

      if (outerState.isPresent()) {
        inductiveWeakening = fmgr.simplify(bfmgr.and(
            inductiveWeakening, outerState.get().getAbstraction()
        ));
      }
      return Collections.singleton(
          SlicingAbstractedState.of(
              inductiveWeakening, iState.getPathFormula().getSsa(),
              iState.getPathFormula().getPointerTargetSet(), fmgr, successor,
              Optional.of(iState))
      );

    } else {

      boolean hasTargetState = false;
      for (AbstractState oState : otherStates) {
        if (AbstractStates.isTargetState(oState)) {
          hasTargetState = true;
          break;
        }
      }

      if (checkTargetStates && hasTargetState) {
        try {
          if (isUnreachable(state.asIntermediate())) {
            return Collections.emptyList();
          }
        } catch (SolverException e) {
          throw new CPATransferException("Reachability checking failed", e);
        }
      }
      return Collections.singleton(state);
    }
  }

  private boolean isUnreachable(SlicingIntermediateState iState)
      throws SolverException, InterruptedException {
    BooleanFormula prevSlice = iState.getAbstractParent().getAbstraction();
    BooleanFormula instantiatedFormula =
        fmgr.instantiate(prevSlice, iState.getAbstractParent().getSSA());
    BooleanFormula reachabilityQuery = bfmgr.and(
        iState.getPathFormula().getFormula(), instantiatedFormula);
    return solver.isUnsat(reachabilityQuery);
  }

  @Override
  public SlicingState getInitialState(CFANode node) {
    return SlicingAbstractedState.empty(fmgr, node);
  }

  @Override
  public boolean isLessOrEqual(SlicingState pState1,
      SlicingState pState2) throws InterruptedException, CPAException {
    Preconditions.checkState(pState1.isAbstracted() == pState2.isAbstracted());

    if (!pState1.isAbstracted()) {

      // Use the coverage relation for intermediate states.
      SlicingIntermediateState iState1 = pState1.asIntermediate();
      SlicingIntermediateState iState2 = pState2.asIntermediate();
      return (iState1.isMergedInto(iState2) ||
          iState1.getPathFormula().equals(iState2.getPathFormula())) &&
          iState1.getAbstractParent() == iState2.getAbstractParent();

    } else {
      SlicingAbstractedState aState1 = pState1.asAbstracted();
      SlicingAbstractedState aState2 = pState2.asAbstracted();
      BooleanFormula abstraction1 = aState1.getAbstraction();
      BooleanFormula abstraction2 = aState2.getAbstraction();

      if (fmgr.simplify(abstraction1).equals(fmgr.simplify(abstraction2))
          || abstraction1.equals(bfmgr.makeBoolean(false))
          || abstraction2.equals(bfmgr.makeBoolean(true))) {
        return true;
      }

      if (expensiveSubsumptionCheck) {
        try {
          return isLessOrEqual(
              pState1.asAbstracted().getAbstraction(),
              pState2.asAbstracted().getAbstraction()
          );
        } catch (SolverException e) {
          throw new CPAException("Solver failed on a query", e);
        }
      } else {
        return true;
      }
    }
  }

  /**
   * Check whether one formula subsumes another one.
   * NOTE: for this to work {@code f2} must not contain intermediate variables.
   */
  private boolean isLessOrEqual(BooleanFormula f1, BooleanFormula f2)
      throws SolverException, InterruptedException {
    return solver.isUnsat(bfmgr.and(f1, bfmgr.not(f2)));
  }

  private SlicingIntermediateState joinIntermediateStates(
      SlicingIntermediateState newState,
      SlicingIntermediateState oldState) throws InterruptedException {
    Preconditions.checkState(newState.getAbstractParent().equals(
        oldState.getAbstractParent()));

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

  private SlicingState joinAbstractedStates(
      SlicingAbstractedState newState,
      SlicingAbstractedState oldState) throws InterruptedException {
    Preconditions.checkState(newState.getNode() == oldState.getNode());

    // The only state with a generating state not being present
    // is the initial one, and that should not be merged.
    Preconditions.checkState(newState.getGeneratingState().isPresent() &&
        oldState.getGeneratingState().isPresent());

    if (newState.getAbstraction().equals(bfmgr.makeBoolean(false))
        || oldState == newState) {
      return oldState;
    }
    if (oldState.getAbstraction().equals(bfmgr.makeBoolean(false))) {
      return newState;
    }
    logger.log(Level.INFO, "Joining abstracted states",
        "this should be quite rare");

    return SlicingAbstractedState.of(
        fmgr.simplify(bfmgr.or(newState.getAbstraction(),
            oldState.getAbstraction())),
        oldState.getSSA(),
        oldState.getPointerTargetSet(),
        fmgr,
        newState.getNode(),

        // todo: this might not be valid if they both are not coming from the loop.
        oldState.getGeneratingState()
    );
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

  private boolean shouldPerformAbstraction(CFANode node) {

    // Slicing is only performed on the loop heads.
    return cfa.getLoopStructure().get().getAllLoopHeads().contains(node);
  }

  /**
   * @return a *unique* {@link Loop} for a given {@code loopHead}
   */
  private Loop getLoopForLoopHead(CFANode loopHead) {
    Set<Loop> loops = cfa.getLoopStructure().get().getLoopsForLoopHead(loopHead);
    Verify.verify(loops.size() == 1);
    return loops.iterator().next();
  }

  /**
   * @return already existing slice which can be used in place of this one,
   * by traversing the chain of backpointers, if it exists.
   */
  private Optional<SlicingAbstractedState> findSliceIfExists(
      SlicingIntermediateState iState) {
    Loop loop = getLoopForLoopHead(iState.getNode());
    SlicingAbstractedState pState = iState.getAbstractParent();
    while (pState.getGeneratingState().isPresent()) {
      if (getLoopForLoopHead(pState.getNode()) == loop) {
        return Optional.of(pState);
      }
      pState = pState.getGeneratingState().get().getAbstractParent();
    }
    return Optional.absent();
  }

  /**
   * @return the state corresponding to the outer loop if one exists.
   */
  private Optional<SlicingAbstractedState> findOuterStateIfExists(
      SlicingIntermediateState iState
  ) {
    Loop loop = getLoopForLoopHead(iState.getNode());

    SlicingAbstractedState aState = iState.getAbstractParent();
    while (aState.getGeneratingState().isPresent()) {
      Loop backpointerLoop = getLoopForLoopHead(aState.getNode());
      if (backpointerLoop.isOuterLoopOf(loop)) {
        return Optional.of(aState);
      }
      aState = aState.getGeneratingState().get().getAbstractParent();
    }
    return Optional.absent();
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(SlicingState pState,
      UnmodifiableReachedSet pStates, AbstractState pFullState) {

    return Optional.of(PrecisionAdjustmentResult.create(
        pState, SingletonPrecision.getInstance(), Action.CONTINUE));
  }
}
