package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class TatoFormulaConverter extends CtoFormulaConverter {

  public TatoFormulaConverter(
      FormulaEncodingOptions pOptions,
      FormulaManagerView pFmgr,
      MachineModel pMachineModel,
      Optional<VariableClassification> pVariableClassification,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CtoFormulaTypeHandler pTypeHandler,
      AnalysisDirection pDirection) {
    super(
        pOptions,
        pFmgr,
        pMachineModel,
        pVariableClassification,
        pLogger,
        pShutdownNotifier,
        pTypeHandler,
        pDirection);
  }

  @Override
  protected BooleanFormula createFormulaForEdge(
      final CFAEdge edge,
      final String function,
      final SSAMapBuilder ssa,
      final PointerTargetSetBuilder pts,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCodeException, UnrecognizedCFAEdgeException, InterruptedException {
    var sourceNode = edge.getPredecessor();
    if (sourceNode instanceof TCFAEntryNode) {
      return makeInitialFormula(edge, function, ssa, pts, constraints, errorConditions);
    }
    if (edge.getEdgeType() == CFAEdgeType.TimedAutomatonEdge) {
      return makeTimedEdgeFormula(
          (TCFAEdge) edge, function, ssa, pts, constraints, errorConditions);
    }
    return super.createFormulaForEdge(edge, function, ssa, pts, constraints, errorConditions);
  }

  @Override
  protected CRightHandSideVisitor<Formula, UnrecognizedCodeException> createCRightHandSideVisitor(
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder ssa,
      PointerTargetSetBuilder pts,
      Constraints constraints,
      ErrorConditions errorConditions) {

    return new TaExpressionToDifferenceFormulaVisitor(
        this,
        fmgr,
        pEdge,
        pFunction,
        ssa,
        constraints,
        getCurrentTimeVariableFormula(pFunction, ssa));
  }

  /** Creates a formula that represents the initial state of a timed automaton */
  private BooleanFormula makeInitialFormula(
      final CFAEdge initialEdge,
      final String function,
      final SSAMapBuilder ssa,
      final PointerTargetSetBuilder pts,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCodeException, InterruptedException {
    var timeVariable = getCurrentTimeVariableFormula(function, ssa);
    var zero = fmgr.makeNumber(fmgr.getFormulaType(timeVariable), 0);
    var initialTimeGreaterZero = fmgr.makeGreaterOrEqual(timeVariable, zero, true);

    var declaration = (TaDeclaration) ((TCFANode) initialEdge.getSuccessor()).getFunction();
    var allClocksZero = bfmgr.makeTrue();
    for (var clockVariable : declaration.getClocks()) {
      var variable = makeFreshVariable(clockVariable, CNumericTypes.LONG_DOUBLE, ssa);
      var variableIsZero = fmgr.makeEqual(variable, zero);
      allClocksZero = bfmgr.and(allClocksZero, variableIsZero);
    }

    var firstInvariantSatisfied =
        makeSuccessorInvariantFormula(
            initialEdge, function, ssa, pts, constraints, errorConditions);

    var initFormula = bfmgr.and(initialTimeGreaterZero, firstInvariantSatisfied, allClocksZero);

    return initFormula;
  }

  /**
   * Creates a new free time variable that represents an updated version of the old time variable
   */
  private BooleanFormula makeTimeUpdateFormula(final String function, final SSAMapBuilder ssa) {
    var oldTimeVar = getCurrentTimeVariableFormula(function, ssa);
    var newTimeVar =
        makeFreshVariable(
            getTimeVariableNameForAutomaton(function), CNumericTypes.LONG_DOUBLE, ssa);

    return fmgr.makeGreaterOrEqual(newTimeVar, oldTimeVar, true);
  }

  private static String getTimeVariableNameForAutomaton(String automatonName) {
    return automatonName + "#time";
  }

  private Formula getCurrentTimeVariableFormula(final String function, final SSAMapBuilder ssa) {
    return makeVariable(getTimeVariableNameForAutomaton(function), CNumericTypes.LONG_DOUBLE, ssa);
  }

  /**
   * Create a formula that expresses that the invariant of the successor of the edge is satisfied
   */
  private BooleanFormula makeSuccessorInvariantFormula(
      final CFAEdge edge,
      final String function,
      final SSAMapBuilder ssa,
      final PointerTargetSetBuilder pts,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCodeException, InterruptedException {
    var successor = (TCFANode) edge.getSuccessor();
    return makePredicate(
        successor.getInvariant(), true, edge, function, ssa, pts, constraints, errorConditions);
  }

  /**
   * Converts a timed automaton edge into a boolean formula representing the guard, invariant in the
   * target state, clock resets and one time delay transition
   */
  private BooleanFormula makeTimedEdgeFormula(
      final TCFAEdge edge,
      final String function,
      final SSAMapBuilder ssa,
      final PointerTargetSetBuilder pts,
      final Constraints constraints,
      final ErrorConditions errorConditions)
      throws UnrecognizedCodeException, InterruptedException {

    // guard
    var guardFormula =
        makePredicate(
            edge.getGuard(), true, edge, function, ssa, pts, constraints, errorConditions);

    // clock reset
    var resetFormula = bfmgr.makeTrue();
    for (var variableToReset : edge.getVariablesToReset()) {
      var variableFormula =
          makeFreshVariable(variableToReset.getName(), variableToReset.getExpressionType(), ssa);
      var variableResetFormula =
          fmgr.makeEqual(variableFormula, getCurrentTimeVariableFormula(function, ssa));
      resetFormula = bfmgr.and(resetFormula, variableResetFormula);
    }

    // successor invariant
    var successorInvariantFormula =
        makeSuccessorInvariantFormula(edge, function, ssa, pts, constraints, errorConditions);

    var transitionFormula = bfmgr.and(guardFormula, resetFormula, successorInvariantFormula);

    // delay transition
    var delayFormula = makeTimeUpdateFormula(function, ssa);
    var invariantAfterDelayFormula =
        makeSuccessorInvariantFormula(edge, function, ssa, pts, constraints, errorConditions);
    var delayTransitionFormula = bfmgr.and(delayFormula, invariantAfterDelayFormula);

    return bfmgr.and(transitionFormula, delayTransitionFormula);
  }
}
