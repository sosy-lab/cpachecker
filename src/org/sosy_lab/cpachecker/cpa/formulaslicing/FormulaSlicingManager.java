package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

import com.google.common.base.Preconditions;

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

  @Option(secure=true, description="Check target states reachability")
  private boolean checkTargetStates = true;

  public FormulaSlicingManager(PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr, LogManager pLogger,
      CFA pCfa,
      LoopTransitionFinder pLoopTransitionFinder,
      InductiveWeakeningManager pInductiveWeakeningManager, Solver pSolver) {
    fmgr = pFmgr;
    pfmgr = pPfmgr;
    logger = pLogger;
    cfa = pCfa;
    loopTransitionFinder = pLoopTransitionFinder;
    inductiveWeakeningManager = pInductiveWeakeningManager;
    solver = pSolver;
    bfmgr = pFmgr.getBooleanFormulaManager();
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

        PathFormula loopTransition = loopTransitionFinder.generateLoopTransition(
            iState.getPathFormula().getSsa(),
            iState.getPathFormula().getPointerTargetSet(),
            successor);
        BooleanFormula inductiveWeakening;
        try {

          // todo: we should also use the information from the previous
          // abstracted state while doing the inductive weakening.
          // HOWEVER, it is important not to annotate it (as we already know
          // that it has to be inductive) => thus we might need to change the
          // interface of the InductiveWeakeningManager.
          inductiveWeakening =
              inductiveWeakeningManager.slice(iState.getPathFormula(),
                  loopTransition);
        } catch (SolverException ex) {
          throw new CPATransferException("Originating exception: ", ex);
        }
        return Collections.singleton(SlicingAbstractedState.of(inductiveWeakening, iState.getPathFormula().getSsa(),
            iState.getPathFormula().getPointerTargetSet(), fmgr));

      } else {

        // We are coming from inside the loop => the (other) abstracted state
        // should already exist.
        return Collections.emptySet();
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
    return SlicingAbstractedState.empty(fmgr);
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
      // todo: Comparison for abstracted states?
      // Do we need to deal with "false" and "true" values explicitly?
      // Namely, "true" is the biggest, "false" is the smallest.
      if (pState1.asAbstracted().getAbstraction().equals(bfmgr.makeBoolean(false))) {

        // False is the smallest.
        // todo: would AST comparison work though?
        return true;
      }
      return false;
    }
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

    logger.log(Level.INFO, "Joining abstracted states",
        "this should be quite rare");

    // HM what if I'll just set to bottom the generated abstract state if it
    // comes from within the loop? That would avoid the given problem.

    // Merging PointerTargetSet.
    PathFormula p1 = abstractStateToIntermediate(newState).getPathFormula();
    PathFormula p2 = abstractStateToIntermediate(oldState).getPathFormula();
    PointerTargetSet merged = pfmgr.makeOr(p1, p2).getPointerTargetSet();

    return SlicingAbstractedState.of(
        bfmgr.or(newState.getAbstraction(), oldState.getAbstraction()),
        newState.getSSA(), // arbitrary
        merged, fmgr
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
   */
  private boolean shouldPerformSlicing(CFAEdge edge) {
    CFANode succ = edge.getSuccessor();
    return shouldPerformAbstraction(succ)
        && !loopTransitionFinder.getEdgesInSCC(succ).contains(edge);
  }

  private boolean shouldPerformAbstraction(CFANode node) {

    // Slicing is only performed on the loop heads.
    return cfa.getLoopStructure().get().getAllLoopHeads().contains(node);
  }


}
