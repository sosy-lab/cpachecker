// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import static com.google.common.truth.Truth.assertThat;

import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CFormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Tests for {@link ToCExpressionVisitor}. Verify that the visitor correctly converts Expression
 * trees into CExpressions (And/Or behave like Boolean and not like Binary operations).
 */
public class ToCExpressionVisitorTest {

  final LogManager logger = LogManager.createTestLogManager();

  ToCExpressionVisitor expressionTreeVisitor =
      new ToCExpressionVisitor(MachineModel.LINUX64, logger);

  final CBinaryExpressionBuilder builder =
      new CBinaryExpressionBuilder(MachineModel.LINUX64, logger);

  private ExpressionTree<AExpression> createLeftTree() throws UnrecognizedCodeException {
    return LeafExpression.of(
        builder.buildBinaryExpression(
            CIntegerLiteralExpression.createDummyLiteral(0b01, CNumericTypes.INT),
            CIntegerLiteralExpression.createDummyLiteral(0b01, CNumericTypes.INT),
            BinaryOperator.BITWISE_AND));
  }

  private ExpressionTree<AExpression> createRightTree() throws UnrecognizedCodeException {
    return LeafExpression.of(
        builder.buildBinaryExpression(
            CIntegerLiteralExpression.createDummyLiteral(0b10, CNumericTypes.INT),
            CIntegerLiteralExpression.createDummyLiteral(0b11, CNumericTypes.INT),
            BinaryOperator.BITWISE_AND));
  }

  private ExpressionTree<AExpression> createTreeOnlyFalse() throws UnrecognizedCodeException {
    return LeafExpression.of(
        builder.buildBinaryExpression(
            CIntegerLiteralExpression.createDummyLiteral(0b00, CNumericTypes.INT),
            CIntegerLiteralExpression.createDummyLiteral(0b00, CNumericTypes.INT),
            BinaryOperator.BITWISE_AND));
  }

  private BooleanFormula convertCExpressionToBooleanFormula(
      CExpression pResult, Configuration config, Solver smtSolver)
      throws InvalidConfigurationException, UnrecognizedCodeException, InterruptedException {
    FormulaManagerView formulaManager = smtSolver.getFormulaManager();
    CFormulaEncodingOptions options = new CFormulaEncodingOptions(config);
    CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(logger, MachineModel.LINUX64);

    CtoFormulaConverter converter =
        new CtoFormulaConverter(
            options,
            formulaManager,
            MachineModel.LINUX64,
            Optional.empty(),
            logger,
            ShutdownNotifier.createDummy(),
            typeHandler,
            AnalysisDirection.FORWARD);

    CAssumeEdge myAssumeEdge =
        new CAssumeEdge(
            "",
            FileLocation.DUMMY,
            CFANode.newDummyCFANode(),
            CFANode.newDummyCFANode(),
            pResult,
            true);

    return converter.makePredicate(pResult, myAssumeEdge, "", null);
  }

  @Test
  public void testNonZeroExpressionsAreTrue()
      throws UnrecognizedCodeException,
          InvalidConfigurationException,
          InterruptedException,
          SolverException {
    // The left and right operands of the And expression need to be binary expressions instead of
    // plain literals, otherwise LeafExpression.of() will simplify into an Expression tree that is
    // just true
    ExpressionTree<AExpression> left = createLeftTree();
    ExpressionTree<AExpression> right = createRightTree();
    ExpressionTree<AExpression> andExpression = And.of(left, right);
    ExpressionTree<AExpression> orExpression = Or.of(left, right);

    CExpression resultAnd = andExpression.accept(expressionTreeVisitor);
    CExpression resultOr = orExpression.accept(expressionTreeVisitor);

    Configuration config = TestDataTools.configurationForTest().build();
    Solver smtSolver = Solver.create(config, logger, ShutdownNotifier.createDummy());

    BooleanFormula bfAnd = convertCExpressionToBooleanFormula(resultAnd, config, smtSolver);
    BooleanFormula bfOr = convertCExpressionToBooleanFormula(resultOr, config, smtSolver);

    assertThat(smtSolver.isUnsat(bfAnd)).isFalse();
    assertThat(smtSolver.isUnsat(bfOr)).isFalse();
  }

  @Test
  public void testFalseExpressions()
      throws UnrecognizedCodeException,
          InvalidConfigurationException,
          InterruptedException,
          SolverException {
    ExpressionTree<AExpression> andExpression = And.of(createLeftTree(), createTreeOnlyFalse());
    ExpressionTree<AExpression> orExpression = Or.of(createTreeOnlyFalse(), createTreeOnlyFalse());

    CExpression resultAnd = andExpression.accept(expressionTreeVisitor);
    CExpression resultOr = orExpression.accept(expressionTreeVisitor);

    Configuration config = TestDataTools.configurationForTest().build();
    Solver smtSolver = Solver.create(config, logger, ShutdownNotifier.createDummy());

    BooleanFormula bfAnd = convertCExpressionToBooleanFormula(resultAnd, config, smtSolver);
    BooleanFormula bfOr = convertCExpressionToBooleanFormula(resultOr, config, smtSolver);

    assertThat(smtSolver.isUnsat(bfAnd)).isTrue();
    assertThat(smtSolver.isUnsat(bfOr)).isTrue();
  }
}
