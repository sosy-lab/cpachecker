// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class MemoryModelUtil {

  // CType Collection

  /**
   * Checks if {@code pType} or any nested type is an instance of {@code pTargetType}. The search
   * for nested types stops when encountering any name in {@code pStopNames}.
   */
  public static boolean isAnyTypeTargetType(
      CType pType, Class<? extends CType> pTargetType, ImmutableSet<String> pStopNames) {

    return getNestedTypes(pType, pStopNames).stream().anyMatch(t -> pTargetType.isInstance(t));
  }

  /**
   * Checks if {@code pType} or any nested type has the name {@code pTargetName}. The search for
   * nested types stops when encountering any name in {@code pStopNames}.
   */
  public static boolean isAnyTypeTargetName(
      CType pType, String pTargetName, ImmutableSet<String> pStopNames) {

    // strip the name, it may contain trailing whitespaces
    return getNestedTypes(pType, pStopNames).stream()
        .anyMatch(t -> t.toASTString("").strip().equals(pTargetName));
  }

  /**
   * Returns all {@link CType} nested within {@code pType}, including {@code pType}. The search for
   * nested types stops when encountering any name in {@code pStopNames}.
   *
   * <p>For example, {@link PthreadObjectType#PTHREAD_MUTEX_T} contains an inner pointer somewhere,
   * even if the outer type is not a pointer, but then it would be treated as a pointer. But for the
   * sequentialization, it is only necessary to treat a {@link PthreadObjectType#PTHREAD_MUTEX_T} as
   * a pointer if it is a pointer itself, not any of its inner types.
   */
  public static ImmutableSet<CType> getNestedTypes(CType pType, ImmutableSet<String> pStopNames) {
    TypeCollectorWithStopNames typeCollectorWithStop = new TypeCollectorWithStopNames(pStopNames);
    pType.accept(typeCollectorWithStop);
    return typeCollectorWithStop.getCollectedTypes();
  }

  public static final class TypeCollectorWithStopNames
      extends DefaultCTypeVisitor<Void, NoException> {

    private final Set<CType> collectedTypes;

    private final ImmutableSet<String> stopNames;

    public TypeCollectorWithStopNames(ImmutableSet<String> pStopNames) {
      collectedTypes = new HashSet<>();
      stopNames = pStopNames;
    }

    public ImmutableSet<CType> getCollectedTypes() {
      return ImmutableSet.copyOf(collectedTypes);
    }

    @Override
    public @Nullable Void visitDefault(CType pT) {
      collectedTypes.add(pT);
      return null;
    }

    @Override
    public @Nullable Void visit(CArrayType pArrayType) {
      if (collectedTypes.add(pArrayType)) {
        // just visit the actual CArrayType, and stop there if needed
        pArrayType.getType().accept(this);
        if (pArrayType.getLength() != null) {
          pArrayType.getLength().getExpressionType().accept(this);
        }
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CCompositeType pCompositeType) {
      if (collectedTypes.add(pCompositeType)) {
        if (!stopNames.contains(pCompositeType.getName())) {
          for (CCompositeTypeMemberDeclaration member : pCompositeType.getMembers()) {
            member.getType().accept(this);
          }
        }
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CElaboratedType pElaboratedType) {
      if (collectedTypes.add(pElaboratedType)) {
        if (!stopNames.contains(pElaboratedType.getName())) {
          if (pElaboratedType.getRealType() != null) {
            pElaboratedType.getRealType().accept(this);
          }
        }
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CFunctionType pFunctionType) {
      if (collectedTypes.add(pFunctionType)) {
        if (!stopNames.contains(pFunctionType.getName())) {
          for (CType parameterType : pFunctionType.getParameters()) {
            parameterType.accept(this);
          }
        }
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CPointerType pPointerType) {
      if (collectedTypes.add(pPointerType)) {
        // just visit the actual CPointerType, and stop there if needed
        pPointerType.getType().accept(this);
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CTypedefType pTypedefType) {
      if (collectedTypes.add(pTypedefType)) {
        if (!stopNames.contains(pTypedefType.getName())) {
          pTypedefType.getRealType().accept(this);
        }
      }
      return null;
    }

    @Override
    public @Nullable Void visit(CBitFieldType pCBitFieldType) {
      if (collectedTypes.add(pCBitFieldType)) {
        // just visit the actual CBitFieldType, and stop there if needed
        pCBitFieldType.getType().accept(this);
      }
      return null;
    }
  }

  // CCompositeTypeMemberDeclaration

  public static CCompositeTypeMemberDeclaration getCompositeTypeMemberDeclarationByFieldName(
      CType pTypeToSearch, String pFieldName) {

    for (CCompositeTypeMemberDeclaration declaration :
        getNestedCompositeTypeMemberDeclarations(pTypeToSearch, ImmutableSet.of())) {
      if (declaration.getName().equals(pFieldName)) {
        return declaration;
      }
    }
    throw new IllegalArgumentException(
        String.format(
            "No CCompositeTypeMemberDeclaration with name %s found in pTypeToSearch.", pFieldName));
  }

  public static ImmutableSet<CCompositeTypeMemberDeclaration>
      getCompositeTypeMemberDeclarationsByTypeName(CType pTypeToSearch, String pTypeName) {
    ImmutableSet.Builder<CCompositeTypeMemberDeclaration> rDeclarations = ImmutableSet.builder();
    for (CCompositeTypeMemberDeclaration declaration :
        getNestedCompositeTypeMemberDeclarations(pTypeToSearch, ImmutableSet.of())) {
      if (declaration.getType().toASTString("").strip().equals(pTypeName)) {
        rDeclarations.add(declaration);
      }
    }
    return rDeclarations.build();
  }

  public static ImmutableList<CCompositeTypeMemberDeclaration>
      getNestedCompositeTypeMemberDeclarations(
          CType pTypeToSearch, ImmutableSet<String> pStopNames) {

    CCompositeTypeMemberDeclarationCollectorWithStopNames collector =
        new CCompositeTypeMemberDeclarationCollectorWithStopNames(pStopNames);
    pTypeToSearch.accept(collector);
    return collector.getCollectedCompositeTypeMemberDeclarations();
  }

  private static final class CCompositeTypeMemberDeclarationCollectorWithStopNames
      extends DefaultCTypeVisitor<Void, NoException> {

    private final ImmutableList.Builder<CCompositeTypeMemberDeclaration>
        collectedCompositeTypeMemberDeclarations;

    private final Set<CCompositeType> visitedCompositeTypes;

    private final ImmutableSet<String> stopNames;

    private CCompositeTypeMemberDeclarationCollectorWithStopNames(ImmutableSet<String> pStopNames) {
      collectedCompositeTypeMemberDeclarations = ImmutableList.builder();
      visitedCompositeTypes = new HashSet<>();
      stopNames = pStopNames;
    }

    private ImmutableList<CCompositeTypeMemberDeclaration>
        getCollectedCompositeTypeMemberDeclarations() {
      return collectedCompositeTypeMemberDeclarations.build();
    }

    @Override
    public Void visitDefault(CType pT) {
      return null;
    }

    @Override
    public Void visit(CArrayType pArrayType) {
      pArrayType.getType().accept(this);
      return null;
    }

    @Override
    public Void visit(CCompositeType pCompositeType) {
      // prevent call stack overflow from circular references
      if (visitedCompositeTypes.add(pCompositeType)) {
        if (!stopNames.contains(pCompositeType.getName())) {
          collectedCompositeTypeMemberDeclarations.addAll(pCompositeType.getMembers());
          for (CCompositeTypeMemberDeclaration memberDeclaration : pCompositeType.getMembers()) {
            memberDeclaration.getType().accept(this);
          }
        }
      }
      return null;
    }

    @Override
    public Void visit(CElaboratedType pElaboratedType) {
      if (!stopNames.contains(pElaboratedType.getName())) {
        if (pElaboratedType.getRealType() != null) {
          pElaboratedType.getRealType().accept(this);
        }
      }
      return null;
    }

    @Override
    public Void visit(CFunctionType pFunctionType) {
      if (!stopNames.contains(pFunctionType.getName())) {
        for (CType parameterType : pFunctionType.getParameters()) {
          parameterType.accept(this);
        }
      }
      return null;
    }

    @Override
    public Void visit(CPointerType pPointerType) {
      pPointerType.getType().accept(this);
      return null;
    }

    @Override
    public Void visit(CTypedefType pTypedefType) {
      if (!stopNames.contains(pTypedefType.getName())) {
        pTypedefType.getRealType().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CBitFieldType pCBitFieldType) {
      pCBitFieldType.getType().accept(this);
      return null;
    }
  }

  // CLeftHandSide Visitors

  public static final class CLeftHandSideSimpleDeclarationVisitor
      implements CLeftHandSideVisitor<CSimpleDeclaration, NoException> {

    @Override
    public CSimpleDeclaration visit(CArraySubscriptExpression pArraySubscriptExpression)
        throws NoException {

      CLeftHandSide arrayLeftHandSide =
          (CLeftHandSide) pArraySubscriptExpression.getArrayExpression();
      return arrayLeftHandSide.accept(this);
    }

    @Override
    public CSimpleDeclaration visit(CFieldReference pFieldReference) throws NoException {
      CLeftHandSide fieldOwnerLeftHandSide = (CLeftHandSide) pFieldReference.getFieldOwner();
      return fieldOwnerLeftHandSide.accept(this);
    }

    @Override
    public CSimpleDeclaration visit(CIdExpression pIdExpression) throws NoException {
      return pIdExpression.getDeclaration();
    }

    @Override
    public CSimpleDeclaration visit(CPointerExpression pPointerExpression) throws NoException {
      CLeftHandSide operandLeftHandSide = (CLeftHandSide) pPointerExpression.getOperand();
      return operandLeftHandSide.accept(this);
    }

    @Override
    public CSimpleDeclaration visit(CComplexCastExpression pComplexCastExpression)
        throws NoException {

      CLeftHandSide operandLeftHandSide = (CLeftHandSide) pComplexCastExpression.getOperand();
      return operandLeftHandSide.accept(this);
    }
  }

  // CExpression Visitors

  public static ImmutableSet<CSimpleDeclaration> getNestedSimpleDeclarations(
      CExpression pExpression) {

    CSimpleDeclarationCollector simpleDeclarationCollector = new CSimpleDeclarationCollector();
    pExpression.accept(simpleDeclarationCollector);
    return simpleDeclarationCollector.getSimpleDeclarations();
  }

  private static final class CSimpleDeclarationCollector
      extends DefaultCExpressionVisitor<Void, NoException> {

    private final Set<CSimpleDeclaration> simpleDeclarations;

    private CSimpleDeclarationCollector() {
      simpleDeclarations = new HashSet<>();
    }

    private ImmutableSet<CSimpleDeclaration> getSimpleDeclarations() {
      return ImmutableSet.copyOf(simpleDeclarations);
    }

    @Override
    public Void visit(CArraySubscriptExpression pArraySubscriptExpression) {
      pArraySubscriptExpression.getArrayExpression().accept(this);
      pArraySubscriptExpression.getSubscriptExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CFieldReference pFieldReference) {
      pFieldReference.getFieldOwner().accept(this);
      return null;
    }

    @Override
    public Void visit(CPointerExpression pPointerExpression) {
      pPointerExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pComplexCastExpression) {
      pComplexCastExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CBinaryExpression pBinaryExpression) {
      pBinaryExpression.getOperand1().accept(this);
      pBinaryExpression.getOperand2().accept(this);
      return null;
    }

    @Override
    public Void visit(CCastExpression pCastExpression) {
      pCastExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pUnaryExpression) {
      pUnaryExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CIdExpression pIdExpression) {
      simpleDeclarations.add(pIdExpression.getDeclaration());
      return null;
    }

    @Override
    protected Void visitDefault(CExpression pExpression) {
      return null;
    }
  }

  public static final class CFieldReferenceCollector
      extends DefaultCExpressionVisitor<Void, NoException> {

    private final Set<CFieldReference> fieldReferences;

    public CFieldReferenceCollector() {
      fieldReferences = new HashSet<>();
    }

    ImmutableSet<CFieldReference> getFieldReferences() {
      return ImmutableSet.copyOf(fieldReferences);
    }

    @Override
    public Void visit(CArraySubscriptExpression pArraySubscriptExpression) {
      pArraySubscriptExpression.getSubscriptExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CFieldReference pFieldReference) {
      fieldReferences.add(pFieldReference);
      pFieldReference.getFieldOwner().accept(this);
      return null;
    }

    @Override
    public Void visit(CPointerExpression pPointerExpression) {
      pPointerExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pComplexCastExpression) {
      pComplexCastExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CBinaryExpression pBinaryExpression) {
      pBinaryExpression.getOperand1().accept(this);
      pBinaryExpression.getOperand2().accept(this);
      return null;
    }

    @Override
    public Void visit(CCastExpression pCastExpression) {
      pCastExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pUnaryExpression) {
      pUnaryExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CIdExpression pIdExpression) {
      return null;
    }

    @Override
    protected Void visitDefault(CExpression pExpression) {
      return null;
    }
  }
}
