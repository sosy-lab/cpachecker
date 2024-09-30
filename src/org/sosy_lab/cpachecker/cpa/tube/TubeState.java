// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube;

import com.google.common.collect.ImmutableMap;

import java.io.Serializable;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
public class TubeState implements AbstractQueryableState, Partitionable, Serializable,

                                  FormulaReportingState {
  /**
   * Represents a TubeState object that contains a map of integer to boolean formulas.
   */
  private final ImmutableMap<Integer,BooleanFormula> asserts;
  /**
   * The boolean expression represented by this TubeState.
   */

  private boolean isNegated;
  private final BooleanFormula booleanExp;
  /**
   * Represents a CFA edge, which is an edge in the control flow graph.
   */
  private final CFAEdge cfaEdge;
  /**
   * Represents a counter for tracking the number of errors.
   */
  private int errorCounter = 0;
  /**
   * The CtoFormulaConverter variable is an instance of the CtoFormulaConverter class.
   * It is a member variable of the TubeState class, used to convert C to Boolean formulas.
   *
   * Example usage:
   * TubeState tubeState = new TubeState();
   * CtoFormulaConverter converter = new CtoFormulaConverter();
   * tubeState.converter = converter;
   */
  public CtoFormulaConverter converter;
  /**
   * Represents the view of the formula manager.
   */
  FormulaManagerView formulaManagerView;
  /**
   * Manages boolean formulas and provides utility methods for manipulating and evaluating them.
   */
  BooleanFormulaManager booleanFormulaManager;
  /**
   * Represents the state of a tube.
   */
  public TubeState(CFAEdge pCFAEdge,ImmutableMap<Integer, BooleanFormula> pAssert, BooleanFormula exp, boolean isNegated, int pError_counter, FormulaManagerView pFormulaManagerView){
    this.cfaEdge = pCFAEdge;
    this.asserts = pAssert;
    this.booleanExp = exp;
    this.errorCounter = pError_counter;
    this.formulaManagerView = pFormulaManagerView;
    booleanFormulaManager = formulaManagerView.getBooleanFormulaManager();
    this.isNegated = isNegated;
  }
  /**
   * Retrieves the assert formula at the specified line number and applies negation if specified.
   *
   * @param lineNumber The line number of the assert formula
   * @param negate     Whether to negate the assert formula or not
   * @return The assert formula at the specified line number
   */
  public BooleanFormula getAssertAtLine(int lineNumber, boolean negate){
    BooleanFormula f = asserts.get(lineNumber);
    if (negate){
      return booleanFormulaManager.not(f);
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
  public ImmutableMap<Integer, BooleanFormula> getAsserts() {
    return this.asserts;
  }
  /**
   * Retrieves the boolean expression associated with the TubeState.
   *
   * @return the boolean expression
   */
  public BooleanFormula getBooleanExp() {
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
   * Retrieves the FormulaManagerView associated with the TubeState.
   *
   * @return The FormulaManagerView instance associated with the TubeState.
   */
  public FormulaManagerView getFormulaManagerView() {
    return formulaManagerView;
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
    return "TubeState: {" +
        "Asserts: " + asserts + "Boolean Expression: " + booleanExp + "Error Counter: " +
            errorCounter + '}';
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
    BooleanFormulaManager bfmgr = manager.getBooleanFormulaManager();
      if (booleanExp == null){
        return bfmgr.makeTrue();
      }else {
        return booleanExp;
      }
  }
}
