/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;

class AssignsParameterOfOtherFunction implements Predicate<AExpressionStatement> {

  private final CFAEdge edge;

  private final String qualifier;

  public AssignsParameterOfOtherFunction(CFAEdge pEdge) {
    edge = pEdge;
    String currentFunctionName = pEdge.getPredecessor().getFunctionName();
    qualifier = Strings.isNullOrEmpty(currentFunctionName) ? "" : currentFunctionName + "::";
  }

  @Override
  public boolean apply(AExpressionStatement pArg0) {
    AExpression exp = pArg0.getExpression();
    if (!(exp instanceof CExpression)) {
      return false;
    }
    CExpression cExp = (CExpression) exp;
    return cExp.accept(
        new CExpressionVisitor<Boolean, NoException>() {

          @Override
          public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
            return pIastArraySubscriptExpression.getArrayExpression().accept(this)
                && pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
          }

          @Override
          public Boolean visit(CFieldReference pIastFieldReference) {
            return pIastFieldReference.getFieldOwner().accept(this);
          }

          @Override
          public Boolean visit(CIdExpression pIastIdExpression) {
            CSimpleDeclaration declaration = pIastIdExpression.getDeclaration();
            if (declaration instanceof CParameterDeclaration && edge instanceof FunctionCallEdge) {
              return declaration.getQualifiedName().startsWith(qualifier);
            }
            return true;
          }

          @Override
          public Boolean visit(CPointerExpression pPointerExpression) {
            return pPointerExpression.getOperand().accept(this);
          }

          @Override
          public Boolean visit(CComplexCastExpression pComplexCastExpression) {
            return pComplexCastExpression.getOperand().accept(this);
          }

          @Override
          public Boolean visit(CBinaryExpression pIastBinaryExpression) {
            return pIastBinaryExpression.getOperand1().accept(this)
                && pIastBinaryExpression.getOperand2().accept(this);
          }

          @Override
          public Boolean visit(CCastExpression pIastCastExpression) {
            return pIastCastExpression.getOperand().accept(this);
          }

          @Override
          public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) {
            return true;
          }

          @Override
          public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
            return true;
          }

          @Override
          public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
            return true;
          }

          @Override
          public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) {
            return true;
          }

          @Override
          public Boolean visit(CTypeIdExpression pIastTypeIdExpression) {
            return true;
          }

          @Override
          public Boolean visit(CUnaryExpression pIastUnaryExpression) {
            return pIastUnaryExpression.getOperand().accept(this);
          }

          @Override
          public Boolean visit(CImaginaryLiteralExpression pIastLiteralExpression) {
            return pIastLiteralExpression.getValue().accept(this);
          }

          @Override
          public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
            return true;
          }
        });
  }
}
