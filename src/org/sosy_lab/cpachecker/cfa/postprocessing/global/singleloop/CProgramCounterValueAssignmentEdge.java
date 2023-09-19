// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

/**
 * Instances of this class are CFA edges representing the assignment of values to the program
 * counter variable.
 */
class CProgramCounterValueAssignmentEdge extends CStatementEdge
    implements ProgramCounterValueAssignmentEdge {

  private static final long serialVersionUID = 3343680508515226739L;

  /** The program counter value. */
  private int pcValue;

  /**
   * Creates a new C program counter value assignment edge between the given predecessor and
   * successor for the given program counter id expression and the program counter value to be
   * assigned.
   *
   * @param pPredecessor the predecessor of the new edge.
   * @param pSuccessor the successor of the new edge.
   * @param pPCIdExpression the program counter id expression to be used.
   * @param pPCValue the program counter value to be assigned.
   */
  public CProgramCounterValueAssignmentEdge(
      CFANode pPredecessor, CFANode pSuccessor, CIdExpression pPCIdExpression, int pPCValue) {
    super(
        buildRawStatement(pPCValue, pPCIdExpression),
        buildStatement(pPCValue, pPCIdExpression),
        FileLocation.DUMMY,
        pPredecessor,
        pSuccessor);
    this.pcValue = pPCValue;
  }

  @Override
  public int getProgramCounterValue() {
    return this.pcValue;
  }

  /**
   * Builds the raw statement for assigning the given value to the given id.
   *
   * @param pPCValue the value to assign.
   * @param pPCIdExpression the id to assign the value to.
   * @return the raw statement.
   */
  private static String buildRawStatement(int pPCValue, CIdExpression pPCIdExpression) {
    return String.format("%s = %d", pPCIdExpression.getName(), pPCValue);
  }

  /**
   * Builds the actual statement for assigning the given value to the given id.
   *
   * @param pPCValue the value to assign.
   * @param pPCIdExpression the id to assign the value to.
   * @return the actual statement.
   */
  private static CStatement buildStatement(int pPCValue, CIdExpression pPCIdExpression) {
    CExpression assignmentExpression =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(pPCValue));
    return new CExpressionAssignmentStatement(
        FileLocation.DUMMY, pPCIdExpression, assignmentExpression);
  }
}
