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

public class BnBExpressionVisitor implements CRightHandSideVisitor<Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>>, BnBException> {

  private final BnBMapMerger merger = new BnBMapMerger();

  private Boolean refd = false;

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CFunctionCallExpression pIastFunctionCallExpression) throws BnBException{
    Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> result = new HashMap<>();

    for(CExpression param : pIastFunctionCallExpression.getParameterExpressions()){
      result = merger.mergeMaps(result, param.accept(this));
    }

    return result;
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CUnaryExpression pIastUnaryExpression) throws BnBException {
    CExpression operand = pIastUnaryExpression.getOperand();
    if (pIastUnaryExpression.getOperator() == UnaryOperator.AMPER &&
        operand instanceof CFieldReference) {

      refd = true;
      Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> result =
          operand.accept(this);
      refd = false;

      return result;
    } else {
      return operand.accept(this);
    }
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CBinaryExpression pIastBinaryExpression) throws BnBException {
    Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> first =
        pIastBinaryExpression.getOperand1().accept(this);
    Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> second =
        pIastBinaryExpression.getOperand2().accept(this);

    return merger.mergeMaps(first, second);
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CCastExpression pIastCastExpression) throws BnBException {
    return pIastCastExpression.getOperand().accept(this);
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CPointerExpression pointerExpression) throws BnBException {
    return pointerExpression.getOperand().accept(this);
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CComplexCastExpression complexCastExpression) throws BnBException {
    return complexCastExpression.getOperand().accept(this);
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CFieldReference pIastFieldReference) throws BnBException {
    CExpression parent = pIastFieldReference.getFieldOwner();
    CType parentType = parent.getExpressionType();

    while (parentType instanceof CPointerType) {
      parentType = ((CPointerType)parentType).getType();
    }
    while (parentType instanceof CTypedefType) {
      parentType = ((CTypedefType)parentType).getRealType();
    }
    while (parentType instanceof CElaboratedType) {
      parentType = ((CElaboratedType)parentType).getRealType();
    }

    CType fieldType = pIastFieldReference.getExpressionType();

    Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> result = new HashMap<>();
    HashMap<CType, HashMap<CType, HashSet<String>>> part = new HashMap<>();
    HashMap<CType, HashSet<String>> part2 = new HashMap<>();
    HashSet<String> set = new HashSet<>();

    set.add(pIastFieldReference.getFieldName());
    part2.put(parentType, set);
    part.put(fieldType, part2);
    result.put(refd, part);

    return result;
  }

  //Don't think we even need this
  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CIdExpression pIastIdExpression) throws BnBException {
    return null;
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CCharLiteralExpression pIastCharLiteralExpression) throws BnBException {
    return null;
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CFloatLiteralExpression pIastFloatLiteralExpression) throws BnBException {
    return null;
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws BnBException {
    return null;
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CStringLiteralExpression pIastStringLiteralExpression) throws BnBException {
    return null;
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CTypeIdExpression pIastTypeIdExpression) throws BnBException {
    return null;
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CImaginaryLiteralExpression pIastLiteralExpression) throws BnBException {
    return null;
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CAddressOfLabelExpression pAddressOfLabelExpression) throws BnBException {
    return null;
  }

  @Override
  public Map<Boolean, HashMap<CType, HashMap<CType, HashSet<String>>>> visit(
      CArraySubscriptExpression pIastArraySubscriptExpression)
      throws BnBException {
    return null;
  }

}
