// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.io.MoreFiles;
import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class ToCExpressionVisitorTest {

  final LogManager logger = LogManager.createTestLogManager();

  ToCExpressionVisitor expressionTreeVisitor;

  @Test
  public void testCacheMissAnd() throws IOException, ParserException, InterruptedException {
    // TODO implement test
    Path program_path = Path.of("test/programs/tocexpressionvisitor_test/boolvsbinaryand.c");
    CFA createdCFA =
        TestDataTools.makeCFA(
            IOUtils.toString(
                MoreFiles.asByteSource(program_path).openStream(), StandardCharsets.UTF_8));
    expressionTreeVisitor = new ToCExpressionVisitor(createdCFA.getMachineModel(), logger);
    // but then how do I get an ExpressionTree from a CFA?

    // or do I just create an ExpressionTree manually?
    ExpressionTree<AExpression> x =
        LeafExpression.of(CIntegerLiteralExpression.createDummyLiteral(0b01, CNumericTypes.INT));
    ExpressionTree<AExpression> y =
        LeafExpression.of(CIntegerLiteralExpression.createDummyLiteral(0b10, CNumericTypes.INT));

    assertThat(CIntegerLiteralExpression.createDummyLiteral(0b01, CNumericTypes.INT).getValue())
        .isNotEqualTo(
            CIntegerLiteralExpression.createDummyLiteral(0b10, CNumericTypes.INT).getValue());

    // ExpressionTree<AExpression> xandy = And.of(x, y);
    assertThat(x.equals(y)).isFalse(); // this fails... why?
  }

  @Test
  public void testCacheMissOr() {
    // TODO implement test

  }
}
