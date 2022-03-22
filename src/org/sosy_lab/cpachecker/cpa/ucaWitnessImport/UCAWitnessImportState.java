// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ucaWitnessImport;

import com.google.common.base.Optional;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.ucageneration.UCAGenerator;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.ucaTestcaseGen.TestcaseEntry;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public class UCAWitnessImportState
    implements LatticeAbstractState<UCAWitnessImportState>, Serializable, Graphable, ExpressionTreeReportingState {

  private static final long serialVersionUID = -7715698130885641252L;
  private final LogManager logger;

  private final ExpressionTree<AExpression> tree;
  private Optional<AutomatonState> automatonState;

  public UCAWitnessImportState(LogManager pLogger) {
    this.tree = ExpressionTrees.getTrue();
    automatonState = Optional.absent();
    this.logger = pLogger;
  }

  public UCAWitnessImportState(
      ExpressionTree<AExpression> pTree, Optional<AutomatonState> pAutomatonState, LogManager pLogger) {
    this.automatonState = pAutomatonState;
    this.tree = pTree;
    this.logger = pLogger;
  }

  public void setAutomatonState(Optional<AutomatonState> pAutomatonState) {
    automatonState = pAutomatonState;
  }

  public UCAWitnessImportState copy() {
    return new UCAWitnessImportState(tree, automatonState, logger);
  }

  @Override
  public UCAWitnessImportState join(UCAWitnessImportState other) throws InterruptedException {
    logger.log(
        Level.WARNING, "Merging of TestCaseGenStates is not supported! Returning the other state");
    return other;
  }

  @Override
  public boolean isLessOrEqual(UCAWitnessImportState other) throws CPAException, InterruptedException {
        // We don't need to compare the automatonState, as they are not relevant for less or equal
    return this.tree.equals(other.tree);
  }

  @Override
  public String toDOTLabel() {
    return String.format(
        "[%s], \n ++%s++",
        tree.toString(),
        this.automatonState.isPresent() ? this.automatonState.get().getInternalStateName() : "");
  }

  @Override
  public boolean shouldBeHighlighted() {
    if (this.automatonState.isPresent()) {
      return this.automatonState
          .get()
          .getInternalStateName()
          .equals(UCAGenerator.NAME_OF_NEWTESTINPUT_STATE);
    }
    return false;
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximation(
      FunctionEntryNode pFunctionScope, CFANode pLocation) throws InterruptedException {
    return ExpressionTrees.cast(tree);
  }

  public UCAWitnessImportState cleanAndCopy() {  return new UCAWitnessImportState(logger);
  }
}
