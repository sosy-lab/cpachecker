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
package org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

/**
 * Instances of this class are CFA edges representing constant assumptions
 * based on program counter values. These edges are used in the program
 * counter value decision trees succeeding the artificial loops in the single
 * loop transformation and preceding the referenced subgraphs.
 */
class CProgramCounterValueAssumeEdge extends CAssumeEdge implements ProgramCounterValueAssumeEdge {

  /**
   * The program counter value assumed.
   */
  private final int pcValue;

  /**
   * Creates a new C program counter value assume edge between the given
   * predecessor and successor, using the given expression builder to build
   * the binary assumption expression of the equation between the given
   * program counter value and the given program counter value id expression.
   *
   * @param pExpressionBuilder the expression builder used to create the
   * assume expression.
   * @param pPredecessor the predecessor node of the edge.
   * @param pSuccessor the successor node of the edge.
   * @param pPCIdExpression the program counter id expression.
   * @param pPCValue the assumed program counter value.
   * @param pTruthAssumption if {@code true} the equation is assumed to be
   * true, if {@code false}, the equation is assumed to be false.
   */
  public CProgramCounterValueAssumeEdge(CBinaryExpressionBuilder pExpressionBuilder,
      CFANode pPredecessor,
      CFANode pSuccessor,
      CIdExpression pPCIdExpression,
      int pPCValue,
      boolean pTruthAssumption) {
    super(buildRawStatement(pPCValue, pPCIdExpression, pTruthAssumption),
        FileLocation.DUMMY,
        pPredecessor,
        pSuccessor,
        buildExpression(pPCValue, pPCIdExpression, pExpressionBuilder),
        pTruthAssumption);
    this.pcValue = pPCValue;
  }

  @Override
  public int getProgramCounterValue() {
    return pcValue;
  }

  /**
   * Builds the raw statement of the assumption.
   *
   * @param pPCValue the assumed program counter value.
   * @param pPCIdExpression the program counter id expression.
   * @param pTruthAssumption if {@code true} the equation is assumed to be
   *
   * true, if {@code false}, the equation is assumed to be false.
   * @return the raw statement of the assumption.
   */
  private static String buildRawStatement(int pPCValue, CIdExpression pPCIdExpression, boolean pTruthAssumption) {
    String rawStatement = String.format("%s == %d",  pPCIdExpression.getName(), pPCValue);
    if (!pTruthAssumption) {
      rawStatement = String.format("!(%s)", rawStatement);
    }
    return rawStatement;
  }

  /**
   * Builds the assume expression.
   *
   * @param pPCValue the assumed program counter value.
   * @param pPCIdExpression the program counter id expression.
   * @param pExpressionBuilder the expression builder used to create the
   * assume expression.
   *
   * @return the assume expression.
   */
  private static CExpression buildExpression(int pPCValue,
      CIdExpression pPCIdExpression,
      CBinaryExpressionBuilder pExpressionBuilder) {
    return pExpressionBuilder.buildBinaryExpressionUnchecked(
        pPCIdExpression,
        new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(pPCValue)),
        BinaryOperator.EQUALS);
  }

}