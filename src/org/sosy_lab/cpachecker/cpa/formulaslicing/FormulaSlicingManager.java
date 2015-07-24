package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.formulaslicing.SlicingAbstractedState.SubsumedSlicingState;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

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
        outPath, iOldState.getAbstraction());

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
      if (shouldPerformSlicing(pCFAEdge)) {
        SlicingAbstractedState ancestor = iState.getAbstraction();

        boolean isInsideAncestorLoop =
            loopTransitionFinder.getEdgesInSCC(ancestor.getNode()).contains(pCFAEdge);

        PathFormula loopTransition = loopTransitionFinder.generateLoopTransition(
            iState.getPathFormula().getSsa(),
            iState.getPathFormula().getPointerTargetSet(),
            successor);
        BooleanFormula inductiveWeakening;
        BooleanFormula strengthening = fmgr.instantiate(
                ancestor.getAbstraction(),
                ancestor.getSSA());
        try {

          // If we are inside the ancestor loop, then:
          // 1) Slice obtained from the ancestor can be safely conjoined to the
          // slice we have obtained (with proper instantiation)
          // 2) The strengthening can contain the predicate both before and
          // after the transition.
          PathFormula toSlice = iState.getPathFormula();
          if (isInsideAncestorLoop) {
            strengthening = bfmgr.and(strengthening, fmgr.instantiate(
                ancestor.getAbstraction(), iState.getPathFormula().getSsa()
            ));
          } else {
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
                    loopTransition, strengthening);
          } finally {
            statistics.formulaSlicingTimer.stop();
          }

          if (isInsideAncestorLoop) {
            inductiveWeakening = fmgr.simplify(bfmgr.and(
                inductiveWeakening, ancestor.getAbstraction()
            ));
          }
        } catch (SolverException ex) {
          throw new CPATransferException("Originating exception: ", ex);
        }
        return Collections.singleton(
            SlicingAbstractedState.of(
                inductiveWeakening, iState.getPathFormula().getSsa(),
                iState.getPathFormula().getPointerTargetSet(), fmgr, successor)
        );

      } else {

        // We are coming from inside the loop => the (other) abstracted state
        // should already exist.
        // We use a special flag to indicate that no slicing is necessary.
        return Collections.singleton(SubsumedSlicingState.getInstance());
      }

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
    BooleanFormula prevSlice = iState.getAbstraction().getAbstraction();
    BooleanFormula instantiatedFormula =
        fmgr.instantiate(prevSlice, iState.getAbstraction().getSSA());
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
      SlicingState pState2) {
    Preconditions.checkState(pState1.isAbstracted() == pState2.isAbstracted());

    if (!pState1.isAbstracted()) {

      // Use the coverage relation for intermediate states.
      SlicingIntermediateState iState1 = pState1.asIntermediate();
      SlicingIntermediateState iState2 = pState2.asIntermediate();
      return iState1.isMergedInto(iState2) &&
          iState1.getAbstraction().equals(iState2.getAbstraction());
    } else {
      if (fmgr.simplify(pState1.asAbstracted().getAbstraction()).equals(
          fmgr.simplify(pState2.asAbstracted().getAbstraction())
      )) {
        return true;
      }

      if (pState1.asAbstracted().getAbstraction().equals(bfmgr.makeBoolean(false))) {
        return true;
      }
      return false;
    }
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(SlicingState pState,
      UnmodifiableReachedSet pStates, AbstractState pFullState) {

    if (pState instanceof SubsumedSlicingState) {

      // Replace with an existing abstracted sibling.
      Optional<SlicingAbstractedState> sibling = findSibling(
          pStates.getReached(pFullState));

      // todo: this condition can be violated, in LPI + formula slicing mode
      Verify.verify(sibling.isPresent());
      Verify.verify(sibling.get().isAbstracted());
      return Optional.of(PrecisionAdjustmentResult.create(
          sibling.get(), new Precision() {
      }, Action.CONTINUE));
    }

    return Optional.of(PrecisionAdjustmentResult.create(
        pState, new Precision() { }, Action.CONTINUE));
  }


  private SlicingState joinIntermediateStates(
      SlicingIntermediateState newState,
      SlicingIntermediateState oldState) throws InterruptedException {
    Preconditions.checkState(newState.getAbstraction().equals(
        oldState.getAbstraction()));

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
        mergedPath, oldState.getAbstraction()
    );
    newState.setMergedInto(out);
    oldState.setMergedInto(out);
    return out;
  }

  private SlicingState joinAbstractedStates(
      SlicingAbstractedState newState,
      SlicingAbstractedState oldState) throws InterruptedException {
    Preconditions.checkState(newState.getNode() == oldState.getNode());

    if (newState.getAbstraction().equals(bfmgr.makeBoolean(false))
        || oldState == newState) {
      return oldState;
    }
    if (oldState.getAbstraction().equals(bfmgr.makeBoolean(false))) {
      return newState;
    }
    logger.log(Level.INFO, "Joining abstracted states",
        "this should be quite rare");

    // Merging PointerTargetSet.
    PathFormula p1 = abstractStateToIntermediate(newState).getPathFormula();
    PathFormula p2 = abstractStateToIntermediate(oldState).getPathFormula();
    PointerTargetSet merged = pfmgr.makeOr(p1, p2).getPointerTargetSet();

    return SlicingAbstractedState.of(
        fmgr.simplify(bfmgr.or(newState.getAbstraction(),
            oldState.getAbstraction())),
        oldState.getSSA(), // arbitrary
        merged, fmgr, newState.getNode()
    );
  }


  private SlicingIntermediateState abstractStateToIntermediate(
      SlicingAbstractedState pSlicingAbstractedState) {
    return SlicingIntermediateState.of(
        new PathFormula(
            bfmgr.makeBoolean(true),
            pSlicingAbstractedState.getSSA(),
            pSlicingAbstractedState.getPointerTargetSet(),
            0), pSlicingAbstractedState);
  }

  /**
   * <=>
   * 1) Target is a loop-head.
   * 2) {@code edge} is NOT a back edge.
   *    (for the entire loop or for the SCC in question?)
   */
  private boolean shouldPerformSlicing(CFAEdge edge) {
    CFANode succ = edge.getSuccessor();

    // todo: define precisely what do we need here, information
    // provided by CPAchecker might not be sufficient.
    // Note that we need whether we are inside the _local_ loop,
    // not the global SCC.
    boolean fromInsideLoop = false;
    for (Loop loop : cfa.getLoopStructure().get().getLoopsForLoopHead(succ)) {
      if (loop.getInnerLoopEdges().contains(edge)) {
        fromInsideLoop = true;
      }
    }
    return shouldPerformAbstraction(succ)
        && !fromInsideLoop;
  }

  private boolean shouldPerformAbstraction(CFANode node) {

    // Slicing is only performed on the loop heads.
    return cfa.getLoopStructure().get().getAllLoopHeads().contains(node);
  }

  /**
   * Find the SlicingAbstractedState sibling: something about-to-be-merged
   * with the argument state, and that has the same partitioning predicate.
   */
  private Optional<SlicingAbstractedState> findSibling(
      Collection<AbstractState> pSiblings) {

    // todo: Code duplication with PolicyIterationManager#findSibling.
    if (pSiblings.isEmpty()) {
      return Optional.absent();
    }

    SlicingAbstractedState out = null;
    boolean found = false;
    for (AbstractState sibling : pSiblings) {
      out = AbstractStates.extractStateByType(sibling,
          SlicingAbstractedState.class);
      if (out != null) {
        found = true;
        break;
      }
    }
    if (found) {
      return Optional.of(out);
    }
    return Optional.absent();
  }
}
