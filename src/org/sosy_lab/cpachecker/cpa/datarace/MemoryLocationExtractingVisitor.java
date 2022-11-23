// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class MemoryLocationExtractingVisitor
    implements CExpressionVisitor<Set<OverapproximatingMemoryLocation>, NoException> {

  private final String functionName;

  public MemoryLocationExtractingVisitor(String pFunctionName) {
    functionName = pFunctionName;
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CBinaryExpression pBinaryExpression) {
    return ImmutableSet.<OverapproximatingMemoryLocation>builder()
        .addAll(pBinaryExpression.getOperand1().accept(this))
        .addAll(pBinaryExpression.getOperand2().accept(this))
        .build();
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CCastExpression pCastExpression) {
    return pCastExpression.getOperand().accept(this);
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CCharLiteralExpression pCharLiteralExpression) {
    return ImmutableSet.of();
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CFloatLiteralExpression pFloatLiteralExpression)
      throws NoException {
    return ImmutableSet.of();
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(
      CIntegerLiteralExpression pIntegerLiteralExpression) {
    return ImmutableSet.of();
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(
      CStringLiteralExpression pStringLiteralExpression) {
    return ImmutableSet.of();
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CTypeIdExpression pTypeIdExpression) {
    return ImmutableSet.of();
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CUnaryExpression pUnaryExpression) {
    return pUnaryExpression.getOperand().accept(this);
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(
      CImaginaryLiteralExpression pImaginaryLiteralExpression) {
    return pImaginaryLiteralExpression.getValue().accept(this);
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(
      CAddressOfLabelExpression pAddressOfLabelExpression) {
    return ImmutableSet.of();
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(
      CArraySubscriptExpression pArraySubscriptExpression) {
    return ImmutableSet.of(getMemoryLocation(pArraySubscriptExpression));
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CFieldReference pFieldReference) {
    return ImmutableSet.of(getMemoryLocation(pFieldReference));
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CIdExpression pIdExpression) {
    return ImmutableSet.of(getMemoryLocation(pIdExpression));
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CPointerExpression pPointerExpression) {
    return ImmutableSet.of(getMemoryLocation(pPointerExpression));
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CComplexCastExpression pComplexCastExpression) {
    return pComplexCastExpression.getOperand().accept(this);
  }

  public OverapproximatingMemoryLocation getMemoryLocation(AExpression pExpression) {
    if (!(pExpression instanceof CExpression)) {
      throw new AssertionError("Only C expressions are supported");
    }
    CExpression expression = (CExpression) pExpression;
    CType type = expression.getExpressionType();
    Set<MemoryLocation> potentialLocations = new HashSet<>();

    if (pExpression instanceof CIdExpression) {
      CIdExpression var = (CIdExpression) pExpression;
      String varName = var.getName();
      boolean isLocal = false;
      if (var.getDeclaration() != null) {
        CSimpleDeclaration decl = var.getDeclaration();
        if (!((decl instanceof CDeclaration && ((CDeclaration) decl).isGlobal())
            || decl instanceof CEnumerator)) {
          isLocal = true;
        }
      }
      if (isLocal) {
        potentialLocations.add(MemoryLocation.forLocalVariable(functionName, varName));
      } else {
        potentialLocations.add(MemoryLocation.parseExtendedQualifiedName(varName));
      }
    } else if (pExpression instanceof CFieldReference) {
      CFieldReference fieldRef = (CFieldReference) pExpression;
      String varName = fieldRef.getFieldName();
      CExpression owner = fieldRef.getFieldOwner();
      if (fieldRef.isPointerDereference()) {
        return new OverapproximatingMemoryLocation(type);
      } else {
        OverapproximatingMemoryLocation ownerLocation = getMemoryLocation(owner);
        if (ownerLocation.isAmbiguous()) {
          return new OverapproximatingMemoryLocation(type);
        } else {
          varName = ownerLocation.getMemoryLocations().iterator().next() + "." + varName;
          potentialLocations.add(MemoryLocation.fromQualifiedName(varName));
        }
      }
    } else if (pExpression instanceof CArraySubscriptExpression) {
      return new OverapproximatingMemoryLocation(type);
    } else if (pExpression instanceof CPointerExpression) {
      CPointerExpression pointerExpression = (CPointerExpression) pExpression;
      if (pointerExpression.getOperand() instanceof CIdExpression) {
        type = ((CIdExpression) pointerExpression.getOperand()).getDeclaration().getType();
      }
      return new OverapproximatingMemoryLocation(type);
    } else if (pExpression instanceof CCastExpression) {
      CCastExpression cast = (CCastExpression) pExpression;
      return getMemoryLocation(cast.getOperand());
    } else {
      potentialLocations.add(MemoryLocation.forLocalVariable(functionName, pExpression.toString()));
    }
    if (type instanceof CPointerType) {
      return new OverapproximatingMemoryLocation(type);
    }
    assert !potentialLocations.isEmpty();
    return new OverapproximatingMemoryLocation(
        potentialLocations, type, potentialLocations.size() > 1, true);
  }
}
