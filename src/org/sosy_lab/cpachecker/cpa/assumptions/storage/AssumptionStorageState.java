// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.assumptions.storage;

import static org.sosy_lab.cpachecker.util.expressions.ExpressionTrees.FUNCTION_DELIMITER;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.function.Function;
import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.globalinfo.SerializationInfoStorage;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** Abstract state for the Collector CPA. Encapsulate a symbolic formula */
public class AssumptionStorageState
    implements AbstractState, ExpressionTreeReportingState, Serializable {

  @Serial private static final long serialVersionUID = -3738604180058424317L;

  // this formula provides the assumption generated from other sources than heuristics,
  // e.g. assumptions for arithmetic overflow
  private final transient BooleanFormula assumption;

  // if a heuristic told us to stop the analysis, this formula provides the reason
  // if it is TRUE, there is no reason -> don't stop
  private final transient BooleanFormula stopFormula;

  private final transient FormulaManagerView fmgr;

  // the assumption represented by this class is always the conjunction of "assumption" and
  // "stopFormula"

  public AssumptionStorageState(
      FormulaManagerView pFmgr, BooleanFormula pAssumption, BooleanFormula pStopFormula) {
    assumption = Preconditions.checkNotNull(pAssumption);
    stopFormula = Preconditions.checkNotNull(pStopFormula);
    fmgr = pFmgr;

    // FALSE would mean "stop the analysis", but this should be signaled by stopFormula
    assert !fmgr.getBooleanFormulaManager().isFalse(assumption);
  }

  public FormulaManagerView getFormulaManager() {
    return fmgr;
  }

  public BooleanFormula getAssumption() {
    return assumption;
  }

  public Appender getAssumptionAsString() {
    return fmgr.dumpFormula(assumption);
  }

  public boolean isAssumptionTrue() {
    return fmgr.getBooleanFormulaManager().isTrue(assumption);
  }

  public boolean isAssumptionFalse() {
    return fmgr.getBooleanFormulaManager().isFalse(assumption);
  }

  public BooleanFormula getStopFormula() {
    return stopFormula;
  }

  public boolean isStopFormulaTrue() {
    return fmgr.getBooleanFormulaManager().isTrue(stopFormula);
  }

  public boolean isStopFormulaFalse() {
    return fmgr.getBooleanFormulaManager().isFalse(stopFormula);
  }

  @Override
  public String toString() {
    return (fmgr.getBooleanFormulaManager().isTrue(stopFormula) ? "" : "<STOP> ")
        + "assume: ("
        + assumption
        + " & "
        + stopFormula
        + ")";
  }

  public boolean isStop() {
    return !fmgr.getBooleanFormulaManager().isTrue(stopFormula);
  }

  public AssumptionStorageState join(AssumptionStorageState other) {
    // create the disjunction of the stop formulas
    // however, if one of them is true, we would loose the information from the other
    // so handle these special cases separately
    final BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    final BooleanFormula newStopFormula;
    if (isStopFormulaTrue()) {
      newStopFormula = other.getStopFormula();
    } else if (other.isStopFormulaTrue()) {
      newStopFormula = getStopFormula();
    } else {
      newStopFormula = bfmgr.or(getStopFormula(), other.getStopFormula());
    }

    return new AssumptionStorageState(
        fmgr, bfmgr.and(getAssumption(), other.getAssumption()), newStopFormula);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof AssumptionStorageState otherElement
        && assumption.equals(otherElement.assumption)
        && stopFormula.equals(otherElement.stopFormula);
  }

  @Override
  public int hashCode() {
    return assumption.hashCode() + 17 * stopFormula.hashCode();
  }

  /**
   * The condition under which the analysis did not stop at this state.
   *
   * <p>The assumption this state represents is the conjunction of its assumption and its stop
   * formula, which characterizes the states the analysis gave up on. Its negation therefore
   * describes the states which still need to be checked for the analysis to analyze the full
   * state-space.
   */
  private BooleanFormula statesRemainingToBeChecked() {
    BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();
    return bfmgr.not(bfmgr.and(assumption, stopFormula));
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximationAllVariablesInFunctionScope(
      FunctionEntryNode pFunctionScope, CFANode pLocation, MachineModel pMachineModel)
      throws InterruptedException, TranslationToExpressionTreeFailedException {
    return ExpressionTrees.fromFormula(
        statesRemainingToBeChecked(),
        fmgr,
        name ->
            !name.contains(FUNCTION_DELIMITER)
                || name.startsWith(pLocation.getFunctionName() + FUNCTION_DELIMITER),
        Function.identity(),
        pMachineModel);
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximationInputProgramInScopeVariables(
      FunctionEntryNode pFunctionScope,
      CFANode pLocation,
      AstCfaRelation pAstCfaRelation,
      boolean useOldKeywordForVariables,
      MachineModel pMachineModel)
      throws InterruptedException, TranslationToExpressionTreeFailedException {
    return ExpressionTrees.fromFormula(
        statesRemainingToBeChecked(),
        fmgr,
        name ->
            (!name.contains(FUNCTION_DELIMITER)
                    || name.startsWith(pLocation.getFunctionName() + FUNCTION_DELIMITER))
                && pAstCfaRelation
                    .getVariablesAndParametersInScope(pLocation)
                    .orElseThrow()
                    .anyMatch(
                        var ->
                            (pLocation.getFunctionName() + FUNCTION_DELIMITER + var.getName())
                                    .equals(name)
                                || var.getName().equals(name))
                && !name.contains("__CPAchecker_"),
        name -> useOldKeywordForVariables ? ExpressionTreeReportingState.oldValueOf(name) : name,
        pMachineModel);
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximationFunctionReturnVariableOnly(
      FunctionEntryNode pFunctionScope,
      AIdExpression pFunctionReturnVariable,
      MachineModel pMachineModel)
      throws ReportingMethodNotImplementedException {
    throw new ReportingMethodNotImplementedException(
        "The assumption storage does not describe the return value of a function.");
  }

  public AssumptionStorageState reset() {
    if (isAssumptionTrue() && isStopFormulaTrue()) {
      return this;
    }
    BooleanFormula trueFormula = fmgr.getBooleanFormulaManager().makeTrue();
    return new AssumptionStorageState(fmgr, trueFormula, trueFormula);
  }

  @Serial
  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    Preconditions.checkState(
        isAssumptionTrue() && isStopFormulaTrue(),
        "Assumption and stop formula must be true for serialization to be correctly restored");
    out.defaultWriteObject();
  }

  @Serial
  private Object readResolve() {
    FormulaManagerView fManager =
        SerializationInfoStorage.getInstance().getAssumptionStorageFormulaManager();
    return new AssumptionStorageState(
        fManager,
        fManager.getBooleanFormulaManager().makeTrue(),
        fManager.getBooleanFormulaManager().makeTrue());
  }
}
