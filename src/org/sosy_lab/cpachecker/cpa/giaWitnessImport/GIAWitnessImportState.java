// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giaWitnessImport;

import java.io.Serializable;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.giageneration.GIAGenerator;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public class GIAWitnessImportState
    implements LatticeAbstractState<GIAWitnessImportState>,
        Serializable,
        Graphable,
        ExpressionTreeReportingState {

  private static final long serialVersionUID = -7715698130885641252L;
  private final LogManager logger;

  private final ExpressionTree<AExpression> tree;
  private final @Nullable AutomatonState automatonState;

  public GIAWitnessImportState(LogManager pLogger) {
    this.tree = ExpressionTrees.getTrue();
    automatonState = null;
    this.logger = pLogger;
  }

  public GIAWitnessImportState(
      ExpressionTree<AExpression> pTree,
      AutomatonState pAutomatonState,
      LogManager pLogger) {
    this.automatonState = pAutomatonState;
    this.tree = pTree;
    this.logger = pLogger;
  }

  public GIAWitnessImportState copy() {
    return new GIAWitnessImportState(tree, automatonState, logger);
  }

  @Override
  public GIAWitnessImportState join(GIAWitnessImportState other) throws InterruptedException {
    logger.log(
        Level.WARNING, "Merging of TestCaseGenStates is not supported! Returning the other state");
    return other;
  }

  @Override
  public boolean isLessOrEqual(GIAWitnessImportState other)
      throws CPAException, InterruptedException {
    // We don't need to compare the automatonState, as they are not relevant for less or equal
    return this.tree.equals(other.tree);
  }

  @Override
  public String toDOTLabel() {
    return String.format(
        "[%s], \n ++%s++",
        tree.toString(),
        this.automatonState != null
            ? this.automatonState.getInternalStateName()
            : "");
  }

  @Override
  public boolean shouldBeHighlighted() {
    if (this.automatonState != null) {
      return this.automatonState
          .getInternalStateName()
          .equals(GIAGenerator.NAME_OF_NEWTESTINPUT_STATE);
    }
    return false;
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximation(
      FunctionEntryNode pFunctionScope, CFANode pLocation) throws InterruptedException {
    return ExpressionTrees.cast(tree);
  }

  public GIAWitnessImportState cleanAndCopy() {
    return new GIAWitnessImportState(logger);
  }
}
