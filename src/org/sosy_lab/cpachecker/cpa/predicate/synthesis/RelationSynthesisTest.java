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
package org.sosy_lab.cpachecker.cpa.predicate.synthesis;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression.createDummyLiteral;
import static org.sosy_lab.cpachecker.cpa.predicate.synthesis.RelationUtils.*;
import static org.sosy_lab.cpachecker.util.test.TestDataTools.makeVariable;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;


public class RelationSynthesisTest {

  private DefaultRelationStore relstoreBwd;
  private RelationSynthesis relsynth;
  private LogManager logger;

  private CIdExpression a;
  private CIdExpression b;
  private CIdExpression c;

  private CIntegerLiteralExpression l1;
  private CIntegerLiteralExpression l7;
  private CIntegerLiteralExpression l65535;

  private CBinaryExpression c_eq_7;
  private CBinaryExpression c_plus_1;
  private CBinaryExpression c_eq_c_plus_1;
  private CBinaryExpression a_lt_c;
  private CBinaryExpression a_eq_c;
  private CBinaryExpression a_gt_l65535;

  @Before
  public void setUp() throws Exception {
    Configuration.defaultConfiguration();
    Configuration config = Configuration
        .builder()
        .setOption("cpa.predicate.solver", "smtinterpol")
        .build();

    logger = mock(LogManager.class);
    relstoreBwd = new DefaultRelationStore(
        config,
        logger,
        mock(CFA.class),
        AnalysisDirection.BACKWARD);

    relsynth = new RelationSynthesis(logger, relstoreBwd);

    makeTestData();
  }

  private void makeTestData() throws UnrecognizedCCodeException {
    CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(MachineModel.LINUX64, logger);

    a = makeVariable("a", CNumericTypes.INT);
    b = makeVariable("b", CNumericTypes.INT);
    c = makeVariable("c", CNumericTypes.INT);

    l1 = CIntegerLiteralExpression.ONE;
    l7 = createDummyLiteral(7, CNumericTypes.INT);
    l65535 = createDummyLiteral(65535, CNumericTypes.INT);

    a_gt_l65535 = builder.buildBinaryExpression(a, l65535, BinaryOperator.GREATER_THAN);
    a_lt_c = builder.buildBinaryExpression(a, c, BinaryOperator.LESS_THAN);
    a_eq_c = builder.buildBinaryExpression(a, c, BinaryOperator.EQUALS);
    c_eq_7 = builder.buildBinaryExpression(c, l7, BinaryOperator.EQUALS);
    c_plus_1 = builder.buildBinaryExpression(c, l1, BinaryOperator.PLUS);
    c_eq_c_plus_1 = builder.buildBinaryExpression(c, c_plus_1, BinaryOperator.EQUALS);
  }

  private void addFactToStore(CBinaryExpression pFact, SSAMap pSsa) {
    relstoreBwd.addFact(pFact, pSsa, 0);
  }

  private void addFactToStore(CBinaryExpression pFact, SSAMap pSsa, int lhsDelta) {
    relstoreBwd.addFact(pFact, pSsa, lhsDelta);
  }

  @Test
  public void testFactGeneration1() {
    SSAMap ssa = SSAMap.emptySSAMap().withDefault(0);
    addFactToStore(a_gt_l65535, ssa);
    addFactToStore(a_lt_c, ssa);
    ssa = ssa.builder().setIndex("c", CNumericTypes.INT, 1).build();
    addFactToStore(c_eq_c_plus_1, ssa, -1);
    ssa = ssa.builder().setIndex("c", CNumericTypes.INT, 2).build();
    addFactToStore(c_eq_c_plus_1, ssa, -1);
    ssa = ssa.builder().setIndex("c", CNumericTypes.INT, 3).build();
    addFactToStore(c_eq_c_plus_1, ssa, -1);
    addFactToStore(c_eq_7, ssa);


    TreeSet<String> resultExpressions = Sets.newTreeSet(Iterables.transform(

        relsynth.getCombinedExpressionsOn(Collections.singleton(a), ssa),

        new Function<CExpression,String>(){
          @Override
          public String apply(CExpression pArg0) {
            return pArg0.toParenthesizedASTString();
          }
        }));

    assertTrue(resultExpressions.contains("(a < (((7 + 1) + 1) + 1))"));
    assertTrue(resultExpressions.contains("(a > 65535)"));
  }

  @Test
  public void testInstanciateAssignBackward() {
    SSAMap ssa = SSAMap.emptySSAMap().builder()
        .setIndex("a", CNumericTypes.INT, 10).build();

    CAssignment assign_a_7 = new CExpressionAssignmentStatement(a.getFileLocation(), a, l7);
    CAssignment in = instanciateAssign(assign_a_7, ssa, AnalysisDirection.BACKWARD);

    assertEquals("(a@10 = 7;)", in.toParenthesizedASTString());
  }

  @Test
  public void testRemoveSsaIndex() {
    assertEquals("a", removeSsaIndex("a@2"));
    assertEquals("aX", removeSsaIndex("aX@2"));
    assertEquals("b", removeSsaIndex("b@1232"));
  }

  @Test
  public void testInstanciateAssignForward() {
    SSAMap ssa = SSAMap.emptySSAMap().builder()
        .setIndex("a", CNumericTypes.INT, 10).build();

    CAssignment assign_a_7 = new CExpressionAssignmentStatement(a.getFileLocation(), a, l7);
    CAssignment in = instanciateAssign(assign_a_7, ssa, AnalysisDirection.FORWARD);

    assertEquals("(a@11 = 7;)", in.toParenthesizedASTString());
  }

  @Test
  public void testInlining1() {
    SSAMap ssa = SSAMap.emptySSAMap().withDefault(0).builder().build();
    addFactToStore(a_lt_c, ssa, 0);
    addFactToStore(c_eq_c_plus_1, ssa, 0);

    CExpression result = relstoreBwd.getInlined(
        instanciate(a_lt_c, ssa, 0),
        Collections.singleton(a)).getFirst();

    // should not lead to a recursion!!
    assertEquals("(a@0 < (c@0 + 1))", result.toParenthesizedASTString());
  }

  @Test
  public void testInliningWithSSAsCase1() {
    SSAMap ssa = SSAMap.emptySSAMap().builder()
        .setIndex("a", CNumericTypes.INT, 5)
        .setIndex("c", CNumericTypes.INT, 10).build();
    addFactToStore(c_eq_7, ssa); // c@10 == 7
    addFactToStore(c_eq_c_plus_1, ssa, +1); // c@11 == c@10 + 1
    ssa = ssa.builder().setIndex("c", CNumericTypes.INT, 11).build();
    addFactToStore(a_lt_c, ssa); // c@11

    CIdExpression a_ssa = instanciate(a, ssa, 0);
    CBinaryExpression query = instanciate(a_lt_c, ssa, 0);
    Pair<CExpression, Set<CIdExpression>> result = relstoreBwd.getInlined(query, Collections.singleton(a_ssa));

    assertEquals("(a@5 < (7 + 1))", result.getFirst().toParenthesizedASTString());
    assertEquals(0, result.getSecond().size());
  }

  @Test
  public void testInliningWithSSAsCase2() {
    SSAMap ssa = SSAMap.emptySSAMap().builder()
        .setIndex("a", CNumericTypes.INT, 5)
        .setIndex("c", CNumericTypes.INT, 10).build();
    addFactToStore(c_eq_c_plus_1, ssa, +1); // c@11 == c@10 + 1
    ssa = ssa.builder().setIndex("c", CNumericTypes.INT, 11).build();
    addFactToStore(a_lt_c, ssa); // c@11

    CIdExpression a_ssa = instanciate(a, ssa, 0);
    Pair<CExpression, Set<CIdExpression>> result = relstoreBwd.getInlined(instanciate(a_lt_c, ssa, 0), Collections.singleton(a_ssa));
    assertEquals("(a@5 < (c@10 + 1))", result.getFirst().toParenthesizedASTString());
    assertEquals(1, result.getSecond().size());
  }

  @Test
  public void testInliningWithSSAsCase3() {
    SSAMap ssa = SSAMap.emptySSAMap().builder()
        .setIndex("a", CNumericTypes.INT, 5)
        .setIndex("c", CNumericTypes.INT, 10).build();
    addFactToStore(c_eq_7, ssa); // c@10 == 7
    addFactToStore(c_eq_c_plus_1, ssa, +1); // c@11 == c@10 + 1
    ssa = ssa.builder().setIndex("c", CNumericTypes.INT, 11).build();
    addFactToStore(a_eq_c, ssa); // c@11

    CBinaryExpression query = instanciate(a_eq_c, ssa, 0);
    CIdExpression a_ssa = instanciate(a, ssa, 0);
    Pair<CExpression, Set<CIdExpression>> result = relstoreBwd.getInlined(query, Collections.singleton(a_ssa));

    assertEquals("(a@5 == (7 + 1))", result.getFirst().toParenthesizedASTString());
    assertEquals(0, result.getSecond().size());
  }

  @Test
  public void testScout() {
    assertTrue(includesOrGlobalVariable(a_gt_l65535, Collections.singleton(a)));
  }


}
