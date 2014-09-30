/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.predicate.synthesis.arithmethic;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cpa.predicate.synthesis.arithmethic.ExpressionSolver.SolvingFailedException;


public class NaiveArithmethicSolverTest {

  private NaiveArithmethicSolver solver;
  private LogManager logger;

  private CIntegerLiteralExpression l1000;
  private CIntegerLiteralExpression l1;
  private CIntegerLiteralExpression l2;
  private CIntegerLiteralExpression l5;
  private CIdExpression a;
  private CIdExpression b;
  private CIdExpression c;
  private CBinaryExpression l1_plus_2;
  private CBinaryExpression b_minus_c;
  private CBinaryExpression b_minus_c_gt_1000;
  private CBinaryExpression c_gt_2;
  private CBinaryExpression b_plus_c;
  private CBinaryExpression a_plus_1_plus_2;
  private CBinaryExpression l5_times_b_plus_c;
  private CBinaryExpression l5_times_b_plus_c_gt_a_plus_1_plus_2;

  private CIdExpression makeVariable(String varName, CSimpleType varType) {
    FileLocation loc = new FileLocation(0, "", 0, 0, 0);
    CVariableDeclaration decl = new CVariableDeclaration(loc, true, CStorageClass.AUTO, varType, varName, varName, varName, null);
    return new CIdExpression(loc, decl);
  }

  private CIntegerLiteralExpression makeIntLiteral(int value) {
    FileLocation loc = new FileLocation(0, "", 0, 0, 0);
    return new CIntegerLiteralExpression(loc, CNumericTypes.INT, BigInteger.valueOf(value));
  }

  @Before
  public void setUp() throws Exception {
    this.logger = mock(LogManager.class);
    this.solver = new NaiveArithmethicSolver();

    CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(MachineModel.LINUX32, logger);

    l1000 = makeIntLiteral(1000);
    l1 = makeIntLiteral(1);
    l2 = makeIntLiteral(2);
    l5 = makeIntLiteral(5);

    a = makeVariable("a", CNumericTypes.INT);
    b = makeVariable("b", CNumericTypes.INT);
    c = makeVariable("c", CNumericTypes.INT);

    l1_plus_2 = builder.buildBinaryExpression(l1, l2, BinaryOperator.PLUS);
    b_plus_c = builder.buildBinaryExpression(b, c, BinaryOperator.PLUS);
    c_gt_2 = builder.buildBinaryExpression(c, l2, BinaryOperator.GREATER_THAN);

    b_minus_c = builder.buildBinaryExpression(b, l2, BinaryOperator.MINUS);
    b_minus_c_gt_1000 = builder.buildBinaryExpression(b_minus_c, l1000, BinaryOperator.GREATER_THAN);

    l5_times_b_plus_c = builder.buildBinaryExpression(l5, b_plus_c, BinaryOperator.MULTIPLY);
    a_plus_1_plus_2 = builder.buildBinaryExpression(a, l1_plus_2, BinaryOperator.PLUS);
    l5_times_b_plus_c_gt_a_plus_1_plus_2 = builder.buildBinaryExpression(l5_times_b_plus_c, a_plus_1_plus_2, BinaryOperator.GREATER_THAN);
  }

  @Ignore @Test
  public void testCase1() throws SolvingFailedException {
    Set<CBinaryExpression> solution = solver.solve(Sets.newSet(b_minus_c_gt_1000, c_gt_2), Sets.newSet(b));

    assertEquals(1, solution.size());
    CBinaryExpression sol = solution.iterator().next();

    assertEquals("((b - 2) > 1000)", sol.toParenthesizedASTString());
  }

  @Ignore @Test
  public void testCase2() throws SolvingFailedException {
    // 5*(b+c) > a+1+2
    Set<CBinaryExpression> solution = solver.solve(Sets.newSet(l5_times_b_plus_c_gt_a_plus_1_plus_2), Sets.newSet(c));

    assertEquals(1, solution.size());
    CBinaryExpression sol = solution.iterator().next();

    assertEquals("((b - 2) > 1000)", sol.toParenthesizedASTString());
  }

}
