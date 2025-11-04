// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class ToCExpressionVisitorTest {

  final LogManager logger = LogManager.createTestLogManager();

  ToCExpressionVisitor expressionTreeVisitor;

  final CBinaryExpressionBuilder builder =
      new CBinaryExpressionBuilder(MachineModel.LINUX64, logger);

  @Test
  public void testCacheMissAnd()
      throws UnrecognizedCodeException,
          InvalidConfigurationException,
          IOException,
          ParserException,
          InterruptedException {

    // Option 1: construct CFA from c file
    Path program_path = Path.of("test/programs/tocexpressionvisitor_test/boolvsbinaryand.c");
    CFA cfa =
        TestDataTools.makeCFA(
            IOUtils.toString(
                MoreFiles.asByteSource(program_path).openStream(), StandardCharsets.UTF_8));

    // Option 2: construct CExpression by hand
    ExpressionTree<AExpression> left =
        LeafExpression.of(
            builder.buildBinaryExpression(
                CIntegerLiteralExpression.createDummyLiteral(0b01, CNumericTypes.INT),
                CIntegerLiteralExpression.createDummyLiteral(0b01, CNumericTypes.INT),
                BinaryOperator.BINARY_AND));

    ExpressionTree<AExpression> right =
        LeafExpression.of(
            builder.buildBinaryExpression(
                CIntegerLiteralExpression.createDummyLiteral(0b10, CNumericTypes.INT),
                CIntegerLiteralExpression.createDummyLiteral(0b11, CNumericTypes.INT),
                BinaryOperator.BINARY_AND));

    ExpressionTree<AExpression> andExpression = And.of(left, right);
    CExpression result = andExpression.accept(expressionTreeVisitor);

    Solver smtSolver =
        Solver.create(Configuration.defaultConfiguration(), logger, ShutdownNotifier.createDummy());
    FormulaManagerView formulaManager = smtSolver.getFormulaManager();
    BooleanFormulaManagerView bfmgr = formulaManager.getBooleanFormulaManager();

    Configuration config = TestDataTools.configurationForTest().build();
    FormulaEncodingOptions options = new FormulaEncodingOptions(config);
    CtoFormulaTypeHandler typeHandler = new CtoFormulaTypeHandler(logger, MachineModel.LINUX64);
    CtoFormulaConverter converter =
        new CtoFormulaConverter(
            options,
            formulaManager,
            MachineModel.LINUX64,
            cfa.getVarClassification(),
            logger,
            ShutdownNotifier.createDummy(),
            typeHandler,
            AnalysisDirection.FORWARD);

    // TODO: how do I get from a CExpression to a Formula the Solver can evaluate?
    // Or if I have the CFA, what is the best way to use it?
    // converter.makePredicate(result, )

    // assert that the result of the evaluated expression is true or that the c example never
    // reaches the else case
  }

  @Test
  public void testCacheMissOr() throws UnrecognizedCodeException {

    // TODO similar to test above

    ExpressionTree<AExpression> left =
        LeafExpression.of(
            builder.buildBinaryExpression(
                CIntegerLiteralExpression.createDummyLiteral(0b01, CNumericTypes.INT),
                CIntegerLiteralExpression.createDummyLiteral(0b01, CNumericTypes.INT),
                BinaryOperator.BINARY_AND));

    ExpressionTree<AExpression> right =
        LeafExpression.of(
            builder.buildBinaryExpression(
                CIntegerLiteralExpression.createDummyLiteral(0b10, CNumericTypes.INT),
                CIntegerLiteralExpression.createDummyLiteral(0b11, CNumericTypes.INT),
                BinaryOperator.BINARY_AND));

    ExpressionTree<AExpression> orExpression = Or.of(left, right);
    CExpression result = orExpression.accept(expressionTreeVisitor);
  }
}
