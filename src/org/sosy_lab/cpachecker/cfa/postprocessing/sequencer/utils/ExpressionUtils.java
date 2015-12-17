/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.utils;

import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context.AThread;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Function;


public class ExpressionUtils {

  public static final Function<CParameterDeclaration, CPointerExpression> POINTER_OF =
      new Function<CParameterDeclaration, CPointerExpression>() {

        @Override
        public CPointerExpression apply(CParameterDeclaration dec) {
          return new CPointerExpression(FileLocation.DUMMY, dec.getType(),
              new CIdExpression(FileLocation.DUMMY, dec));
        }
      };

   public static final Function<CSimpleDeclaration, CIdExpression> CID_EXPRESSION_OF =
       new Function<CSimpleDeclaration, CIdExpression>() {

        @Override
        public CIdExpression apply(CSimpleDeclaration dec) {
          return new CIdExpression(FileLocation.DUMMY, dec);
        }

   };


  public static CArraySubscriptExpression getArrayVarOfIndex(CVariableDeclaration arrayDec,
      int staticIndex) {
    return getArrayVarOfIndex(arrayDec, new CIntegerLiteralExpression(
            FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(staticIndex)));
  }

  public static CArraySubscriptExpression getArrayVarOfIndex(CVariableDeclaration arrayDec,
      CExpression subscriptExpression) {
    assert arrayDec.getType() instanceof CArrayType : "Cannot build CArraySubscriptExpression from the non array type: " + arrayDec.getType();
    assert subscriptExpression.getExpressionType() instanceof CSimpleType;

    CType arrayEntryType = ((CArrayType) arrayDec.getType()).getType();

    return new CArraySubscriptExpression(FileLocation.DUMMY,
        arrayEntryType, new CIdExpression(FileLocation.DUMMY,
            arrayDec), subscriptExpression);
  }

  /**
   * Returns a StatementEdge which assigns the right expression to the left
   * leftHandSide. NOTE that the used CFANodes are dummy nodes and have to be
   * replaced to use in cfa.
   *
   * @see CFAEdgeUtils#
   * @param leftHandSide where the given expression will be assigned
   * @param right the expression which will be assigned to the leftHandSide
   * @return a dummy AStatementEdge which represents the assignment
   */
  public static AStatementEdge getDummyAssignement(ALeftHandSide left, AExpression right) {
    if (left instanceof CLeftHandSide && right instanceof CExpression) {
      CStatement assignStatement = new CExpressionAssignmentStatement(FileLocation.DUMMY, (CLeftHandSide) left, (CExpression) right);
      return new CStatementEdge("", assignStatement, FileLocation.DUMMY,
          CFASequenceBuilder.DUMMY_NODE, CFASequenceBuilder.DUMMY_NODE);
    } else if (left instanceof JLeftHandSide && right instanceof JExpression) {
      throw new UnsupportedOperationException("Not implemented yet");
    } else {
      throw new AssertionError(
          "The left and right ast nodes must be from the same program language");
    }
  }

  public static AStatementEdge getDummyCStaticAssignement(ALeftHandSide left, long right) {
    return getDummyAssignement(left, new CIntegerLiteralExpression(FileLocation.DUMMY,
        CNumericTypes.INT, BigInteger.valueOf(right)));
  }

  public static AStatementEdge getDummyCStaticAssignement(ALeftHandSide left, boolean right) {
    if (right) {
      return getDummyAssignement(left, new CIntegerLiteralExpression(FileLocation.DUMMY,
          CNumericTypes.BOOL, BigInteger.valueOf(1)));
    } else {
      return getDummyAssignement(left, new CIntegerLiteralExpression(FileLocation.DUMMY,
          CNumericTypes.BOOL, BigInteger.valueOf(0)));
    }
  }

  public static AStatementEdge getDummyCStaticAssignement(ALeftHandSide left, char right) {
    return getDummyAssignement(left, new CCharLiteralExpression(FileLocation.DUMMY,
        CNumericTypes.CHAR, right));
  }

  public static CIntegerLiteralExpression getThreadNumberNumberExpression(AThread thread) {
    return new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, BigInteger.valueOf(thread.getThreadNumber()));
  }
}
