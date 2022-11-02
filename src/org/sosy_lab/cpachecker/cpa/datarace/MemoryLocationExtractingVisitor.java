// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class MemoryLocationExtractingVisitor
    implements CExpressionVisitor<Map<MemoryLocation, CType>, UnrecognizedCodeException> {

  private final String functionName;

  public MemoryLocationExtractingVisitor(String pFunctionName) {
    functionName = pFunctionName;
  }

  @Override
  public Map<MemoryLocation, CType> visit(CBinaryExpression pBinaryExpression)
      throws UnrecognizedCodeException {
    Map<MemoryLocation, CType> leftLocations = pBinaryExpression.getOperand1().accept(this);
    Map<MemoryLocation, CType> rightLocations = pBinaryExpression.getOperand2().accept(this);
    // TODO: Handle pointer, e.g. "&x + 1"
    return ImmutableMap.<MemoryLocation, CType>builder()
        .putAll(leftLocations)
        .putAll(rightLocations)
        .buildOrThrow();
  }

  @Override
  public Map<MemoryLocation, CType> visit(CCastExpression pCastExpression)
      throws UnrecognizedCodeException {
    // TODO: Might have to handle cast to a pointer type
    return pCastExpression.getOperand().accept(this);
  }

  @Override
  public Map<MemoryLocation, CType> visit(CCharLiteralExpression pCharLiteralExpression)
      throws UnrecognizedCodeException {
    return ImmutableMap.of();
  }

  @Override
  public Map<MemoryLocation, CType> visit(CFloatLiteralExpression pFloatLiteralExpression)
      throws UnrecognizedCodeException {
    return ImmutableMap.of();
  }

  @Override
  public Map<MemoryLocation, CType> visit(CIntegerLiteralExpression pIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    return ImmutableMap.of();
  }

  @Override
  public Map<MemoryLocation, CType> visit(CStringLiteralExpression pStringLiteralExpression)
      throws UnrecognizedCodeException {
    return ImmutableMap.of();
  }

  @Override
  public Map<MemoryLocation, CType> visit(CTypeIdExpression pTypeIdExpression)
      throws UnrecognizedCodeException {
    return ImmutableMap.of();
  }

  @Override
  public Map<MemoryLocation, CType> visit(CUnaryExpression pUnaryExpression)
      throws UnrecognizedCodeException {
    Map<MemoryLocation, CType> memoryLocations = pUnaryExpression.getOperand().accept(this);
    if (pUnaryExpression.getOperator() != UnaryOperator.AMPER) {
      return memoryLocations;
    }
    // TODO: Handle pointer
    return memoryLocations;
  }

  @Override
  public Map<MemoryLocation, CType> visit(CImaginaryLiteralExpression pImaginaryLiteralExpression)
      throws UnrecognizedCodeException {
    return pImaginaryLiteralExpression.getValue().accept(this);
  }

  @Override
  public Map<MemoryLocation, CType> visit(CAddressOfLabelExpression pAddressOfLabelExpression)
      throws UnrecognizedCodeException {
    return ImmutableMap.of();
  }

  @Override
  public Map<MemoryLocation, CType> visit(CArraySubscriptExpression pArraySubscriptExpression)
      throws UnrecognizedCodeException {
    return ImmutableMap.of(
        getMemoryLocation(pArraySubscriptExpression),
        pArraySubscriptExpression.getExpressionType());
  }

  @Override
  public Map<MemoryLocation, CType> visit(CFieldReference pFieldReference)
      throws UnrecognizedCodeException {
    return ImmutableMap.of(getMemoryLocation(pFieldReference), pFieldReference.getExpressionType());
  }

  @Override
  public Map<MemoryLocation, CType> visit(CIdExpression pIdExpression)
      throws UnrecognizedCodeException {
    return ImmutableMap.of(getMemoryLocation(pIdExpression), pIdExpression.getExpressionType());
  }

  @Override
  public Map<MemoryLocation, CType> visit(CPointerExpression pPointerExpression)
      throws UnrecognizedCodeException {
    CType pointerType = pPointerExpression.getExpressionType();
    if (pPointerExpression.getOperand() instanceof CIdExpression) {
      CIdExpression idExpression = (CIdExpression) pPointerExpression.getOperand();
      pointerType = idExpression.getDeclaration().getType();
    }
    return ImmutableMap.of(getMemoryLocation(pPointerExpression), pointerType);
  }

  @Override
  public Map<MemoryLocation, CType> visit(CComplexCastExpression pComplexCastExpression)
      throws UnrecognizedCodeException {
    return pComplexCastExpression.getOperand().accept(this);
  }

  public MemoryLocation getMemoryLocation(AExpression pExpression)
      throws UnrecognizedCodeException {
    if (pExpression instanceof CIdExpression) {
      CIdExpression var = (CIdExpression) pExpression;
      String varName = var.getName();
      if (var.getDeclaration() != null) {
        CSimpleDeclaration decl = var.getDeclaration();
        if (!((decl instanceof CDeclaration && ((CDeclaration) decl).isGlobal())
            || decl instanceof CEnumerator)) {
          return MemoryLocation.forLocalVariable(functionName, varName);
        }
      }
      return MemoryLocation.parseExtendedQualifiedName(varName);
    } else if (pExpression instanceof CFieldReference) {
      CFieldReference fieldRef = (CFieldReference) pExpression;
      String varName = fieldRef.getFieldName();
      CExpression owner = fieldRef.getFieldOwner();
      if (owner != null) {
        varName =
            getMemoryLocation(owner) + (fieldRef.isPointerDereference() ? "->" : ".") + varName;
      }
      return MemoryLocation.fromQualifiedName(varName);
    } else if (pExpression instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arraySubscript = (CArraySubscriptExpression) pExpression;
      CExpression owner = arraySubscript.getArrayExpression();
      return MemoryLocation.parseExtendedQualifiedName(
          String.format("%s[*]", getMemoryLocation(owner)));
    } else if (pExpression instanceof CPointerExpression) {
      CPointerExpression pe = (CPointerExpression) pExpression;
      if (pe.getOperand() instanceof CLeftHandSide) {
        return getMemoryLocation(pe.getOperand());
      }
      // TODO
      return MemoryLocation.forLocalVariable(functionName, pExpression.toString());
    } else if (pExpression instanceof CCastExpression) {
      CCastExpression cast = (CCastExpression) pExpression;
      return getMemoryLocation(cast.getOperand());
    } else {
      return MemoryLocation.forLocalVariable(functionName, pExpression.toString());
    }
  }
}
