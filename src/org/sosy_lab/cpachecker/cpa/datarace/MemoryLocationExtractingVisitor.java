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
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class MemoryLocationExtractingVisitor
    implements CExpressionVisitor<Set<OverapproximatingMemoryLocation>, NoException> {

  private final String functionName;
  private final Set<String> addressedVariables;

  public MemoryLocationExtractingVisitor(String pFunctionName, Set<String> pAddressedVariables) {
    functionName = pFunctionName;
    addressedVariables = pAddressedVariables;
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
    if (pUnaryExpression.getOperator() == UnaryOperator.AMPER) {
      return getAddressReads(pUnaryExpression.getOperand());
    }
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
    return getAccessedLocations(pArraySubscriptExpression);
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CFieldReference pFieldReference) {
    return getAccessedLocations(pFieldReference);
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CIdExpression pIdExpression) {
    return getAccessedLocations(pIdExpression);
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CPointerExpression pPointerExpression) {
    return getAccessedLocations(pPointerExpression);
  }

  @Override
  public Set<OverapproximatingMemoryLocation> visit(CComplexCastExpression pComplexCastExpression) {
    return pComplexCastExpression.getOperand().accept(this);
  }

  /**
   * Returns the location of the given lvalue and the locations read to compute its address. An
   * array only denotes its address, so its own location is not accessed.
   */
  private Set<OverapproximatingMemoryLocation> getAccessedLocations(CExpression pLvalue) {
    Set<OverapproximatingMemoryLocation> addressReads = getAddressReads(pLvalue);
    if (pLvalue.getExpressionType().getCanonicalType() instanceof CArrayType) {
      return addressReads;
    }
    return ImmutableSet.<OverapproximatingMemoryLocation>builder()
        .add(getMemoryLocation(pLvalue))
        .addAll(addressReads)
        .build();
  }

  /** Returns the locations read to compute the address of the given lvalue. */
  public Set<OverapproximatingMemoryLocation> getAddressReads(CExpression pLvalue) {
    return switch (pLvalue) {
      case CIdExpression unused -> ImmutableSet.of();
      case CPointerExpression pointerExpression -> pointerExpression.getOperand().accept(this);
      case CFieldReference fieldRef when fieldRef.isPointerDereference() ->
          fieldRef.getFieldOwner().accept(this);
      case CFieldReference fieldRef -> getAddressReads(fieldRef.getFieldOwner());
      case CArraySubscriptExpression subscript ->
          ImmutableSet.<OverapproximatingMemoryLocation>builder()
              .addAll(subscript.getArrayExpression().accept(this))
              .addAll(subscript.getSubscriptExpression().accept(this))
              .build();
      default -> pLvalue.accept(this);
    };
  }

  public OverapproximatingMemoryLocation getMemoryLocation(AExpression pExpression) {
    if (!(pExpression instanceof CExpression expression)) {
      throw new AssertionError("Only C expressions are supported");
    }

    CType type = expression.getExpressionType();
    Set<MemoryLocation> potentialLocations = new HashSet<>();
    boolean mayBePointedTo = true;

    switch (pExpression) {
      case CIdExpression var -> {
        String varName = var.getName();
        boolean isLocal = false;
        if (var.getDeclaration() != null) {
          CSimpleDeclaration decl = var.getDeclaration();
          if (!((decl instanceof CDeclaration cDeclaration && cDeclaration.isGlobal())
              || decl instanceof CEnumerator)) {
            isLocal = true;
          }
        }
        if (isLocal) {
          potentialLocations.add(MemoryLocation.forLocalVariable(functionName, varName));
        } else {
          potentialLocations.add(MemoryLocation.parseExtendedQualifiedName(varName));
        }
        mayBePointedTo =
            var.getDeclaration() == null
                || type.getCanonicalType() instanceof CArrayType
                || addressedVariables.contains(var.getDeclaration().getQualifiedName());
      }
      case CFieldReference fieldRef -> {
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
            mayBePointedTo = ownerLocation.mayBePointedTo();
          }
        }
      }
      case CArraySubscriptExpression unused -> {
        return new OverapproximatingMemoryLocation(type);
      }
      case CPointerExpression unused -> {
        return new OverapproximatingMemoryLocation(type);
      }
      case CCastExpression cast -> {
        return getMemoryLocation(cast.getOperand());
      }
      default ->
          potentialLocations.add(
              MemoryLocation.forLocalVariable(functionName, pExpression.toString()));
    }
    assert !potentialLocations.isEmpty();
    return new OverapproximatingMemoryLocation(
        potentialLocations, type, potentialLocations.size() > 1, true, mayBePointedTo);
  }
}
