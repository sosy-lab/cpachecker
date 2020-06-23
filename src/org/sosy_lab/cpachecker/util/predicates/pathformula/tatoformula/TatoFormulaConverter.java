// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import static com.google.common.base.Preconditions.checkArgument;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

public class TatoFormulaConverter extends CtoFormulaConverter {
  /**
   * This enum serves as uniform type interface in order to convert between c and formula types
   * CTypes are not needed for timed automata and thus, methods in this class should use this enum
   * instead.
   */
  public static class VariableType {
    public static VariableType INTEGER =
        new VariableType(FormulaType.getBitvectorTypeWithSize(128), CNumericTypes.INT);
    public static VariableType FLOAT =
        new VariableType(FormulaType.getFloatingPointType(15, 112), CNumericTypes.FLOAT);

    private final FormulaType<?> formulaType;
    private final CType cType;

    VariableType(FormulaType<?> pFormulaType, CType pCType) {
      formulaType = pFormulaType;
      cType = pCType;
    }

    public FormulaType<?> getFormulaType() {
      return formulaType;
    }

    public CType getCType() {
      return cType;
    }

    public static VariableType fromCType(CType type) {
      if (type instanceof CSimpleType) {
        var simpleType = (CSimpleType) type;
        if (simpleType.getType().equals(CBasicType.INT)) {
          return INTEGER;
        } else if (simpleType.getType().equals(CBasicType.FLOAT)) {
          return FLOAT;
        }
      }
      throw new AssertionError("Unknown c type");
    }
  }

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
    var targetNode = edge.getSuccessor();
    if (sourceNode instanceof TCFAEntryNode && targetNode instanceof TCFANode) {
      return makeInitialFormula(edge, function, ssa);
    }
    if (edge.getEdgeType() == CFAEdgeType.TimedAutomatonEdge) {
      return makeTimedEdgeFormula((TCFAEdge) edge, function, ssa);
    }
    return super.createFormulaForEdge(edge, function, ssa, pts, constraints, errorConditions);
  }

  /** Creates a formula that represents the initial state of a timed automaton */
  private BooleanFormula makeInitialFormula(
      final CFAEdge initialEdge, final String function, final SSAMapBuilder ssa) {
    var declaration = ((TCFANode) initialEdge.getSuccessor()).getAutomatonDeclaration();

    var timeVariable = getCurrentTimeVariableFormula(function, ssa);
    var decimalZero = fmgr.makeNumber(VariableType.FLOAT.getFormulaType(), 0);
    var initialTimeGreaterZero = fmgr.makeGreaterOrEqual(timeVariable, decimalZero, true);

    var allClocksZero = bfmgr.makeTrue();
    for (var clockVariable : declaration.getClocks()) {
      var variable = makeFreshVariable(clockVariable.getName(), VariableType.FLOAT, ssa);
      var variableIsZero = fmgr.makeEqual(variable, decimalZero);
      allClocksZero = bfmgr.and(allClocksZero, variableIsZero);
    }

    var allActionCountsZero = bfmgr.makeTrue();
    var integerZero = fmgr.makeNumber(VariableType.INTEGER.getFormulaType(), 0);
    for (var action : declaration.getActions()) {
      var occurenceVarName = getActionOccurenceVariableName(function, action.getName());
      var occurenceVariable = makeVariable(occurenceVarName, VariableType.INTEGER, ssa);
      allActionCountsZero =
          bfmgr.and(fmgr.makeEqual(occurenceVariable, integerZero), allActionCountsZero);
    }

    var firstInvariantSatisfied = makeSuccessorInvariantFormula(initialEdge, function, ssa);

    var initFormula =
        bfmgr.and(
            initialTimeGreaterZero, firstInvariantSatisfied, allClocksZero, allActionCountsZero);

    return initFormula;
  }

  /** Creates t = t' + d /\ d >= 0, where d is a fresh unique variable. */
  private BooleanFormula makeTimeUpdateFormula(final String function, final SSAMapBuilder ssa) {
    var oldTimeVariable = getCurrentTimeVariableFormula(function, ssa);
    var newTimeVariable =
        makeFreshVariable(getTimeVariableNameForAutomaton(function), VariableType.FLOAT, ssa);
    var deltaVariable = makeConstant(generateNewDeltaVariableName(function), VariableType.FLOAT);
    var zero = fmgr.makeNumber(VariableType.FLOAT.getFormulaType(), 0);

    var deltaVarGreaterZero = fmgr.makeGreaterOrEqual(deltaVariable, zero, true);
    var timeStepFormula = fmgr.makePlus(oldTimeVariable, deltaVariable);
    var newTimeFormula = fmgr.makeEqual(newTimeVariable, timeStepFormula);

    return fmgr.makeAnd(newTimeFormula, deltaVarGreaterZero);
  }

  public static String getTimeVariableNameForAutomaton(String automatonName) {
    return automatonName + "#time";
  }

  public static String generateNewDeltaVariableName(String automatonName) {
    return automatonName + "#delta" + System.currentTimeMillis();
  }

  public static String getActionOccurenceVariableName(String automatonName, String actionName) {
    return automatonName + "#action_occurence#" + actionName;
  }

  public static String getActionVariableName(String automatonName, String actionName) {
    return automatonName + "#action#" + actionName;
  }

  private Formula getCurrentTimeVariableFormula(final String function, final SSAMapBuilder ssa) {
    return makeVariable(getTimeVariableNameForAutomaton(function), VariableType.FLOAT, ssa);
  }

  /**
   * Create a formula that expresses that the invariant of the successor of the edge is satisfied
   * This method should be called after the reset formulas were created, such that the SSA indices
   * correspond to variables after the transition.
   */
  private BooleanFormula makeSuccessorInvariantFormula(
      final CFAEdge edge, final String function, final SSAMapBuilder ssa) {
    var successor = (TCFANode) edge.getSuccessor();
    if (successor.getInvariant().isPresent()) {
      return makeFormulaFromCondition(successor.getInvariant().get(), function, ssa);
    }

    return bfmgr.makeTrue();
  }

  /**
   * Converts a timed automaton edge into a boolean formula representing the guard, invariant in the
   * target state, clock resets and one time delay transition
   */
  private BooleanFormula makeTimedEdgeFormula(
      final TCFAEdge edge, final String function, final SSAMapBuilder ssa) {
    var transitionFormula = bfmgr.makeTrue();

    // guard
    if (edge.getGuard().isPresent()) {
      var guardFormula = makeFormulaFromCondition(edge.getGuard().get(), function, ssa);
      transitionFormula = bfmgr.and(guardFormula, transitionFormula);
    }

    // clock reset
    var resetFormula = bfmgr.makeTrue();
    for (var variableToReset : edge.getVariablesToReset()) {
      var variableFormula = makeFreshVariable(variableToReset.getName(), VariableType.FLOAT, ssa);
      var variableResetFormula =
          fmgr.makeEqual(variableFormula, getCurrentTimeVariableFormula(function, ssa));
      resetFormula = bfmgr.and(resetFormula, variableResetFormula);
    }
    transitionFormula = bfmgr.and(resetFormula, transitionFormula);

    // successor invariant
    var successorInvariantFormula = makeSuccessorInvariantFormula(edge, function, ssa);
    transitionFormula = bfmgr.and(successorInvariantFormula, transitionFormula);

    // sync formula
    if (edge.getAction().isPresent()) {
      var action = edge.getAction().get();
      var one = fmgr.makeNumber(VariableType.INTEGER.getFormulaType(), 1);
      var occurenceVarName = getActionOccurenceVariableName(function, action.getName());
      var oldOccurenceVariable = makeVariable(occurenceVarName, VariableType.INTEGER, ssa);
      var newOccurenceVariable = makeFreshVariable(occurenceVarName, VariableType.INTEGER, ssa);
      var occurenceUpdate =
          fmgr.makeEqual(newOccurenceVariable, fmgr.makePlus(oldOccurenceVariable, one));
      transitionFormula = bfmgr.and(occurenceUpdate, transitionFormula);

      var actionVarName = getActionVariableName(function, action.getName());
      var actionVariable = makeFreshVariable(actionVarName, VariableType.FLOAT, ssa);
      var syncFormula =
          fmgr.makeEqual(actionVariable, getCurrentTimeVariableFormula(function, ssa));
      transitionFormula = bfmgr.and(syncFormula, transitionFormula);
    }

    // delay transition
    var delayFormula = makeTimeUpdateFormula(function, ssa);
    var invariantAfterDelayFormula = makeSuccessorInvariantFormula(edge, function, ssa);
    var delayTransitionFormula = bfmgr.and(delayFormula, invariantAfterDelayFormula);
    transitionFormula = bfmgr.and(delayTransitionFormula, transitionFormula);

    return transitionFormula;
  }

  /**
   * Overwrites the method of CtoFormulaConverter but uses VariableType as argument (CTypes are not
   * needed/known). Since the SSA map requires CTypes, this method simply passes a matching CType as
   * arugments to the respective methods.
   */
  protected Formula makeVariable(String name, VariableType type, SSAMapBuilder ssa) {
    int useIndex = getIndex(name, type.getCType(), ssa);
    return fmgr.makeVariable(type.getFormulaType(), name, useIndex);
  }

  /**
   * Overwrites the method of CtoFormulaConverter but uses VariableType as argument (CTypes are not
   * needed/known). Since the SSA map requires CTypes, this method simply passes a matching CType as
   * arugments to the respective methods.
   */
  protected Formula makeFreshVariable(String name, VariableType type, SSAMapBuilder ssa) {
    int useIndex;

    if (direction == AnalysisDirection.BACKWARD) {
      useIndex = getIndex(name, type.getCType(), ssa);
    } else {
      useIndex = makeFreshIndex(name, type.getCType(), ssa);
    }

    Formula result = fmgr.makeVariable(type.getFormulaType(), name, useIndex);

    if (direction == AnalysisDirection.BACKWARD) {
      makeFreshIndex(name, type.getCType(), ssa);
    }

    return result;
  }

  protected Formula makeConstant(String name, VariableType type) {
    return fmgr.makeVariableWithoutSSAIndex(type.getFormulaType(), name);
  }

  /*
   * Override in order to handle CTypes
   */
  @Override
  public BooleanFormula makeSsaUpdateTerm(
      final String variableName,
      final CType variableType,
      final int oldIndex,
      final int newIndex,
      final PointerTargetSet pts)
      throws InterruptedException {
    checkArgument(oldIndex > 0 && newIndex > oldIndex);
    var variableFormulaType = VariableType.fromCType(variableType).getFormulaType();

    final Formula oldVariable = fmgr.makeVariable(variableFormulaType, variableName, oldIndex);
    final Formula newVariable = fmgr.makeVariable(variableFormulaType, variableName, newIndex);

    return fmgr.assignment(newVariable, oldVariable);
  }

  protected BooleanFormula makeFormulaFromCondition(
      TaVariableCondition condition, String automatonName, SSAMapBuilder ssa) {
    return condition.getExpressions().stream()
        .reduce(
            bfmgr.makeTrue(),
            (res, expr) -> bfmgr.and(makeFormulaFromExpression(expr, automatonName, ssa), res),
            (f1, f2) -> bfmgr.and(f1, f2));
  }

  protected BooleanFormula makeFormulaFromExpression(
      TaVariableExpression expression, String automatonName, SSAMapBuilder ssa) {
    var variableFormula = makeVariable(expression.getVariable().getName(), VariableType.FLOAT, ssa);
    var timeVariableFormula = getCurrentTimeVariableFormula(automatonName, ssa);
    var differenceFormula = fmgr.makeMinus(timeVariableFormula, variableFormula);
    var constantFormula = makeFormulaFromNumber(expression.getConstant());

    switch (expression.getOperator()) {
      case GREATER:
        return fmgr.makeGreaterThan(differenceFormula, constantFormula, true);
      case GREATER_EQUAL:
        return fmgr.makeGreaterOrEqual(differenceFormula, constantFormula, true);
      case LESS:
        return fmgr.makeLessThan(differenceFormula, constantFormula, true);
      case LESS_EQUAL:
        return fmgr.makeLessOrEqual(differenceFormula, constantFormula, true);
      case EQUAL:
        return fmgr.makeEqual(differenceFormula, constantFormula);
      default:
        throw new AssertionError();
    }
  }

  protected Formula makeFormulaFromNumber(Number pNumber) {
    if (pNumber instanceof BigInteger) {
      return fmgr.makeNumber(VariableType.INTEGER.getFormulaType(), (BigInteger) pNumber);
    } else if (pNumber instanceof BigDecimal) {
      return fmgr.getFloatingPointFormulaManager()
          .makeNumber(
              (BigDecimal) pNumber, (FloatingPointType) VariableType.FLOAT.getFormulaType());
    } else {
      throw new AssertionError();
    }
  }
}
