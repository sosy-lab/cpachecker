// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.CParser.Factory;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
public class TubeState implements AbstractQueryableState, Partitionable, Serializable, Graphable,
                                  FormulaReportingState, Targetable {
  /**
   * Represents a TubeState object that contains a map of integer to boolean formulas.
   */
  private final ImmutableMap<Integer,String> asserts;

  public LogManager getLogManager() {
    return logManager;
  }

  private final LogManager logManager;
  /**
   * The boolean expression represented by this TubeState.
   */

  private final boolean isNegated;
  private final String booleanExp;
  /**
   * Represents a CFA edge, which is an edge in the control flow graph.
   */
  private final CFAEdge cfaEdge;
  /**
   * Represents a counter for tracking the number of errors.
   */
  private int errorCounter = 0;

  public CFA getCfa() {
    return cfa;
  }

  private final CFA cfa;

  public Function<FormulaManagerView, CtoFormulaConverter> getSupplier() {
    return supplier;
  }

  private final Function<FormulaManagerView,CtoFormulaConverter> supplier;

  /**
   * Represents the state of a tube.
   */
  public TubeState(CFAEdge pCFAEdge, ImmutableMap<Integer, String> pAssert, String exp, boolean pIsNegated, int pError_counter,
                   Function<FormulaManagerView,CtoFormulaConverter> pSupplier, LogManager pLogManager, CFA pCfa){
    this.cfaEdge = pCFAEdge;
    this.asserts = pAssert;
    this.booleanExp = exp;
    this.errorCounter = pError_counter;
    this.isNegated = pIsNegated;
    this.supplier = pSupplier;
    this.logManager = pLogManager;
    this.cfa = pCfa;
  }
  /**
   * Retrieves the assert formula at the specified line number and applies negation if specified.
   *
   * @param lineNumber The line number of the assert formula
   * @param negate     Whether to negate the assert formula or not
   * @return The assert formula at the specified line number
   */
  public String getAssertAtLine(int lineNumber, boolean negate){
    String f = asserts.get(lineNumber);
    if (negate){
      return "!("+ f + ")";
    }
    return f;
  }

  public boolean isNegated() {
    return isNegated;
  }

  /**
   * Retrieves the assertions stored in the current TubeState object.
   *
   * @return An ImmutableMap containing the assertions, where the key is the line number
   *         and the value is the BooleanFormula.
   */
  public ImmutableMap<Integer, String> getAsserts() {
    return this.asserts;
  }
  /**
   * Retrieves the boolean expression associated with the TubeState.
   *
   * @return the boolean expression
   */
  public String getBooleanExp() {
    return booleanExp;
  }
  /**
   * Retrieves the error counter value.
   *
   * @return The value of the error counter.
   */
  public int getErrorCounter(){
    return this.errorCounter;
  }

  /**
   * Increments the error counter by 1.
   */
  public void incrementErrorCounter(){
  this.errorCounter += 1;
}
  /**
   * Retrieves the CFA edge associated with the current instance of TubeState.
   *
   * @return the CFA edge associated with the current instance of TubeState
   */
  public CFAEdge getCfaEdge(){
    return this.cfaEdge;
  }
  /**
   * Returns the name of the CPA to which this state belongs.
   *
   * @return the name of the CPA
   */
  @Override
  public String getCPAName() {
    return getClass().getSimpleName();
  }
  /**
   * Retrieves the partition key for the current object.
   *
   * @return the partition key indicating the part of the partition to which the object belongs,
   *         or null if the object is not partitionable or the key is not applicable
   */
  @Override
  public @Nullable Object getPartitionKey() {
    return this;
  }
  /**
   * Returns a string representation of the TubeState object.
   *
   * @return the string representation of the TubeState object
   */
  @Override
  public String toString() {
    return "TubeState{" +
        "asserts=" + asserts +
        ", isNegated=" + isNegated +
        ", booleanExp='" + booleanExp + '\'' +
        ", errorCounter=" + errorCounter +
        '}';
  }

  /**
   * Returns the approximation of the Boolean formula associated with this TubeState.
   *
   * @param manager The FormulaManagerView to use for creating Boolean formulas.
   * @return The approximation of the Boolean formula. If the boolean expression is null,
   *         returns a Boolean formula representing true. Otherwise, returns the boolean expression itself.
   */
  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    String exp = this.getBooleanExp();
    if(exp == null){
      return manager.getBooleanFormulaManager().makeTrue();
    }
    BooleanFormula booleanFormula;
    try {
      booleanFormula = manager.uninstantiate(parseFormula(manager, exp));
    } catch (InvalidAutomatonException | UnrecognizedCodeException | InterruptedException pE) {
      throw new RuntimeException(pE);
    }
    return Objects.requireNonNullElseGet(booleanFormula,
        manager.getBooleanFormulaManager()::makeTrue);

  }

  @Override
  public String toDOTLabel() {
    return toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  BooleanFormula parseFormula(FormulaManagerView pFormulaManagerView, String entry) throws InvalidAutomatonException, InterruptedException, UnrecognizedCodeException {
    CStatement statements = CParserUtils.parseSingleStatement(entry, CParser.Factory.getParser(
        logManager, Factory.getDefaultOptions(), cfa.getMachineModel(),
        ShutdownNotifier.createDummy()),new CProgramScope(cfa, logManager));
    CExpression expression = getcExpression(statements);
    CtoFormulaConverter converter = supplier.apply(pFormulaManagerView);
    return converter.makePredicate(expression,new BlankEdge("", FileLocation.DUMMY, CFANode.newDummyCFANode(), CFANode.newDummyCFANode(), "test"),entry, SSAMap.emptySSAMap().builder());
  }


  private static CExpression getcExpression(CStatement statement) {
    CExpression expression = ((CExpressionStatement) statement).getExpression();
    if (expression == null) {
      throw new IllegalArgumentException(
          "Statement cannot be converted into CExpression. Invalid statement: " + statement);
    }
    return expression;
  }

  @Override
  public boolean isTarget() {
    return cfaEdge.getSuccessor().getNumLeavingEdges() == 0 || (cfaEdge.getCode().contains("reach_error();")&&(cfaEdge.getEdgeType().equals(
        CFAEdgeType.StatementEdge) || cfaEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)));
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return ImmutableSet.of();
  }
}
