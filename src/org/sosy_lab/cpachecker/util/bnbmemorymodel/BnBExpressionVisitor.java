/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.bnbmemorymodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

public class BnBExpressionVisitor implements CRightHandSideVisitor<Void, UnrecognizedCCodeException> {

  private Boolean refd = false;

  private Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visitResult =
      new HashMap<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>>();

  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> getVisitResult() {
    return visitResult;
  }

  public void clearVisitResult(){
    visitResult.clear();
  }

  @Override
  public Void visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException{

    for(CExpression param : pIastFunctionCallExpression.getParameterExpressions()){
      param.accept(this);
    }

    return null;
  }

  @Override
  public Void visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCCodeException {
    CExpression operand = pIastUnaryExpression.getOperand();
    if (pIastUnaryExpression.getOperator() == UnaryOperator.AMPER &&
        operand instanceof CFieldReference) {

      refd = true;
      operand.accept(this);
      refd = false;

    } else {
      operand.accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCCodeException {
    pIastBinaryExpression.getOperand1().accept(this);
    pIastBinaryExpression.getOperand2().accept(this);
    return null;
  }

  @Override
  public Void visit(
      CCastExpression pIastCastExpression) throws UnrecognizedCCodeException {
    pIastCastExpression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(
      CPointerExpression pointerExpression) throws UnrecognizedCCodeException {
    pointerExpression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(
      CComplexCastExpression complexCastExpression) throws UnrecognizedCCodeException {
    complexCastExpression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {
    CExpression parent = pIastFieldReference.getFieldOwner();
    CType parentType = parent.getExpressionType();

    parentType = getUnmaskedCompositeType(parentType);

    CType fieldType = pIastFieldReference.getExpressionType();

    HashMap<CType, HashMap<CType, HashSet<String>>> part = new HashMap<>();
    HashMap<CType, HashSet<String>> part2 = new HashMap<>();
    HashSet<String> set = new HashSet<>();

    set.add(pIastFieldReference.getFieldName());
    part2.put(parentType, set);
    part.put(fieldType, part2);

    if (visitResult.isEmpty() || !visitResult.containsKey(refd)){
      visitResult.put(refd, part);
    } else if (!visitResult.get(refd).containsKey(fieldType)){
      visitResult.get(refd).put(fieldType, part2);
    } else if (!visitResult.get(refd).get(fieldType).containsKey(parentType)){
      visitResult.get(refd).get(fieldType).put(parentType, set);
    } else {
      visitResult.get(refd).get(fieldType).get(parentType).addAll(set);
    }
    return null;
  }

  private CType getUnmaskedCompositeType(CType maskedType) {

    while (maskedType instanceof CElaboratedType) {
      maskedType = ((CElaboratedType) maskedType).getRealType();
    }

    while (maskedType instanceof CTypedefType) {
      maskedType = ((CTypedefType) maskedType).getRealType();
    }

    while (maskedType instanceof CPointerType) {
      maskedType = ((CPointerType)maskedType).getType();
    }

    return maskedType;
  }

  //Don't think we even need this
  @Override
  public Void visit(CIdExpression pIastIdExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Void visit(CCharLiteralExpression pIastCharLiteralExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Void visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Void visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Void visit(CStringLiteralExpression pIastStringLiteralExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Void visit(CTypeIdExpression pIastTypeIdExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Void visit(CImaginaryLiteralExpression pIastLiteralExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Void visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws UnrecognizedCCodeException {
    return null;
  }

  @Override
  public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws UnrecognizedCCodeException {
    return null;
  }

}
