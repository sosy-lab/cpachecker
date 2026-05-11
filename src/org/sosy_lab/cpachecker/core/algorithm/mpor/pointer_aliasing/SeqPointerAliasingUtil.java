// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class SeqPointerAliasingUtil {

  /**
   * Tries to extract and map the left-hand side {@link SeqMemoryLocation} to the right-hand side
   * {@link SeqMemoryLocation}.
   *
   * @return The {@link Map#entry(Object, Object)} of memory locations or {@link Optional#empty()}
   *     if the memory locations cannot be extracted or if the left-hand side is not a {@link
   *     CPointerType}.
   */
  public static Optional<Map.Entry<SeqMemoryLocation, SeqMemoryLocation>> tryMapPointerAssignment(
      CLeftHandSide pLeftHandSide,
      CRightHandSide pRightHandSide,
      Optional<CFAEdgeForThread> pLeftHandSideCallContext,
      Optional<CFAEdgeForThread> pRightHandSideCallContext,
      CFA pInputCfa)
      throws UnsupportedCodeException {

    CSimpleDeclaration leftHandSideDeclaration =
        Iterables.getOnlyElement(
            SeqPointerAliasingUtil.getAllSimpleDeclarationsInExpression(pLeftHandSide, false));
    ImmutableSet<String> stopNames = PthreadObjectType.getAllPthreadObjectTypeNames();

    if (SeqPointerAliasingUtil.isAnyTypeTargetClass(
        leftHandSideDeclaration.getType(), CPointerType.class, stopNames)) {

      InputRejection.checkFunctionPointerInRightHandSide(pRightHandSide);
      // only check for CBinaryExpression in right-hand sides if the left-hand side is not a pointer
      // dereference. e.g. using a pointer dereference in an increment is fine.
      if (!SeqPointerAliasingUtil.isPointerDereference(pLeftHandSide)) {
        InputRejection.checkBinaryExpressionInRightHandSide(pRightHandSide);
      }

      CPointerAssignmentVisitResult leftHandSideVisitResult =
          pLeftHandSide.accept(new CPointerAssignmentVisitor());
      if (leftHandSideVisitResult != null) {
        // if LHS has a field member that is not CPointerType, then it is not a pointer assignment
        if (leftHandSideVisitResult.fieldMember().isPresent()) {
          CType fieldMemberType = leftHandSideVisitResult.fieldMember().orElseThrow().getType();
          if (!SeqPointerAliasingUtil.isAnyTypeTargetClass(
              fieldMemberType, CPointerType.class, stopNames)) {
            return Optional.empty();
          }
        }
        SeqMemoryLocation leftHandSideMemoryLocation =
            SeqMemoryLocation.of(
                pLeftHandSideCallContext,
                leftHandSideVisitResult.declaration(),
                leftHandSideVisitResult.fieldMember());
        switch (pRightHandSide) {
          case CExpression expression -> {
            CPointerAssignmentVisitResult rightHandSideVisitResult =
                expression.accept(new CPointerAssignmentVisitor());
            // visitResult can be null, e.g., if pRightHandSide is a literal int like '0'
            if (rightHandSideVisitResult != null) {
              SeqMemoryLocation rightHandSideMemoryLocation =
                  SeqMemoryLocation.of(
                      pRightHandSideCallContext,
                      rightHandSideVisitResult.declaration(),
                      rightHandSideVisitResult.fieldMember());
              return Optional.of(
                  Map.entry(leftHandSideMemoryLocation, rightHandSideMemoryLocation));
            }
          }
          case CFunctionCallExpression functionCallExpression -> {
            // do not track defined functions, since they have return statements that are tracked
            if (!MPORUtil.isFunctionDefined(functionCallExpression, pInputCfa)) {
              SeqMemoryLocation rightHandSideMemoryLocation =
                  SeqMemoryLocation.of(
                      pRightHandSideCallContext,
                      functionCallExpression.getDeclaration(),
                      Optional.empty(),
                      Optional.of(functionCallExpression));
              return Optional.of(
                  Map.entry(leftHandSideMemoryLocation, rightHandSideMemoryLocation));
            }
          }
        }
      }
    }
    return Optional.empty();
  }

  private record CPointerAssignmentVisitResult(
      CSimpleDeclaration declaration, Optional<CCompositeTypeMemberDeclaration> fieldMember) {}

  private static final class CPointerAssignmentVisitor
      extends DefaultCExpressionVisitor<CPointerAssignmentVisitResult, NoException> {

    @Override
    public CPointerAssignmentVisitResult visit(
        CArraySubscriptExpression pArraySubscriptExpression) {
      return pArraySubscriptExpression.getArrayExpression().accept(this);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CFieldReference pFieldReference) {
      CPointerAssignmentVisitResult fieldOwnerResult = pFieldReference.getFieldOwner().accept(this);
      CCompositeTypeMemberDeclaration fieldMember =
          SeqPointerAliasingUtil.getCompositeTypeMemberDeclarationByFieldName(
              pFieldReference.getFieldOwner().getExpressionType(), pFieldReference.getFieldName());
      return new CPointerAssignmentVisitResult(
          fieldOwnerResult.declaration(), Optional.of(fieldMember));
    }

    @Override
    public CPointerAssignmentVisitResult visit(CPointerExpression pPointerExpression) {
      return pPointerExpression.getOperand().accept(this);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CComplexCastExpression pComplexCastExpression) {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CCastExpression pCastExpression) {
      return pCastExpression.getOperand().accept(this);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CUnaryExpression pUnaryExpression) {
      return pUnaryExpression.getOperand().accept(this);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CIdExpression pIdExpression) {
      return new CPointerAssignmentVisitResult(pIdExpression.getDeclaration(), Optional.empty());
    }

    @Override
    protected @Nullable CPointerAssignmentVisitResult visitDefault(CExpression exp) {
      return null;
    }
  }

  // CType Collection

  /**
   * Checks if {@code pType} or any nested type is an instance of {@code pTargetClass}. The search
   * for nested types stops when encountering any name in {@code pStopNames}.
   */
  public static boolean isAnyTypeTargetClass(
      CType pType, Class<? extends CType> pTargetClass, ImmutableSet<String> pStopNames) {

    return getAllTypesInType(pType, pStopNames).stream().anyMatch(t -> pTargetClass.isInstance(t));
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
  public static ImmutableSet<CType> getAllTypesInType(
      CType pType, ImmutableSet<String> pStopNames) {

    CTypeCollectorWithStop collector = new CTypeCollectorWithStopNames(pStopNames);
    pType.accept(collector);
    return collector.getCollectedTypes();
  }

  private static class CTypeCollectorWithStopNames extends CTypeCollectorWithStop {

    private final ImmutableSet<String> stopNames;

    CTypeCollectorWithStopNames(ImmutableSet<String> pStopNames) {
      stopNames = pStopNames;
    }

    @Override
    boolean shouldStop(CType pType) {
      // not all CType contain a name, so we filter for the ones that have a name
      return switch (pType) {
        case CCompositeType compositeType -> stopNames.contains(compositeType.getName());
        case CElaboratedType elaboratedType -> stopNames.contains(elaboratedType.getName());
        case CFunctionType functionType -> stopNames.contains(functionType.getName());
        case CTypedefType typedefType -> stopNames.contains(typedefType.getName());
        default -> false;
      };
    }
  }

  private abstract static class CTypeCollectorWithStop extends CTypeTraversalVisitor {

    private final ImmutableSet.Builder<CType> collected = ImmutableSet.builder();

    @Override
    void onVisit(CType pType) {
      collected.add(pType);
    }

    /** Returns the non-empty set of {@link CType} collected during the search. */
    ImmutableSet<CType> getCollectedTypes() {
      ImmutableSet<CType> collectedTypes = collected.build();
      checkState(!collectedTypes.isEmpty(), "The set of collected types is empty.");
      return collectedTypes;
    }
  }

  private abstract static class CTypeTraversalVisitor
      extends DefaultCTypeVisitor<Void, NoException> {

    /**
     * Called when {@code pType} is visited. This function is called before {@link
     * CTypeTraversalVisitor#shouldStop(CType)}.
     */
    abstract void onVisit(CType pType);

    /** Whether the search should stop after visiting {@code pType}. */
    abstract boolean shouldStop(CType pType);

    private final Set<CType> visitedTypes = new HashSet<>();

    private boolean shouldSearch(CType pType) {
      onVisit(pType);
      // Prevent any circular searches. This should be handled here, otherwise all
      // subclasses have to implement it because circular searches should never be desired.
      return visitedTypes.add(pType) && !shouldStop(pType);
    }

    @Override
    public Void visitDefault(CType pType) {
      onVisit(pType);
      return null;
    }

    @Override
    public Void visit(CArrayType pType) {
      if (shouldSearch(pType)) {
        pType.getType().accept(this);
        if (pType.getLength() != null) {
          pType.getLength().getExpressionType().accept(this);
        }
      }
      return null;
    }

    @Override
    public Void visit(CCompositeType pType) {
      if (shouldSearch(pType)) {
        for (CCompositeTypeMemberDeclaration member : pType.getMembers()) {
          member.getType().accept(this);
        }
      }
      return null;
    }

    @Override
    public Void visit(CElaboratedType pType) {
      if (shouldSearch(pType) && pType.getRealType() != null) {
        pType.getRealType().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CFunctionType pType) {
      if (shouldSearch(pType)) {
        for (CType parameter : pType.getParameters()) {
          parameter.accept(this);
        }
      }
      return null;
    }

    @Override
    public Void visit(CPointerType pType) {
      if (shouldSearch(pType)) {
        pType.getType().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CTypedefType pType) {
      if (shouldSearch(pType)) {
        pType.getRealType().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CBitFieldType pType) {
      if (shouldSearch(pType)) {
        pType.getType().accept(this);
      }
      return null;
    }
  }

  // CCompositeTypeMemberDeclaration

  public static CCompositeTypeMemberDeclaration getCompositeTypeMemberDeclarationByFieldName(
      CType pType, String pFieldName) {

    for (CCompositeTypeMemberDeclaration declaration :
        getAllCompositeTypeMemberDeclarationsInType(pType, ImmutableSet.of())) {
      if (declaration.getName().equals(pFieldName)) {
        return declaration;
      }
    }
    throw new IllegalArgumentException(
        String.format(
            "No CCompositeTypeMemberDeclaration with name %s found in pType.", pFieldName));
  }

  public static ImmutableList<CCompositeTypeMemberDeclaration>
      getAllCompositeTypeMemberDeclarationsInType(CType pType, ImmutableSet<String> pStopNames) {

    CCompositeTypeMemberDeclarationCollectorWithStopNames collector =
        new CCompositeTypeMemberDeclarationCollectorWithStopNames(pStopNames);
    pType.accept(collector);
    return collector.getCollectedCompositeTypeMemberDeclarations();
  }

  private static class CCompositeTypeMemberDeclarationCollectorWithStopNames
      extends CTypeCollectorWithStopNames {

    private final List<CCompositeTypeMemberDeclaration> collected = new ArrayList<>();

    private final Set<CCompositeType> visitedCompositeTypes = new HashSet<>();

    CCompositeTypeMemberDeclarationCollectorWithStopNames(ImmutableSet<String> pStopNames) {
      super(pStopNames);
    }

    /**
     * Returns the possibly empty list of {@link CCompositeTypeMemberDeclaration} collected during
     * the search.
     */
    ImmutableList<CCompositeTypeMemberDeclaration> getCollectedCompositeTypeMemberDeclarations() {
      return ImmutableList.copyOf(collected);
    }

    @Override
    void onVisit(CType pType) {
      if (pType instanceof CCompositeType compositeType
          && visitedCompositeTypes.add(compositeType)) {
        collected.addAll(compositeType.getMembers());
      }
    }
  }

  // CLeftHandSide Visitors

  public static boolean isPointerDereference(CLeftHandSide pLeftHandSide) {
    return pLeftHandSide.accept(new CLeftHandSidePointerDereferenceVisitor());
  }

  private static final class CLeftHandSidePointerDereferenceVisitor
      implements CLeftHandSideVisitor<Boolean, NoException> {

    @Override
    public Boolean visit(CArraySubscriptExpression pArraySubscriptExpression) throws NoException {

      return true;
    }

    @Override
    public Boolean visit(CFieldReference pFieldReference) throws NoException {
      return pFieldReference.isPointerDereference();
    }

    @Override
    public Boolean visit(CIdExpression pIdExpression) throws NoException {
      return false;
    }

    @Override
    public Boolean visit(CPointerExpression pPointerExpression) throws NoException {
      return true;
    }

    @Override
    public Boolean visit(CComplexCastExpression pComplexCastExpression) throws NoException {

      CLeftHandSide operandLeftHandSide = (CLeftHandSide) pComplexCastExpression.getOperand();
      return operandLeftHandSide.accept(this);
    }
  }

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

  public static ImmutableSet<CSimpleDeclaration> getAllSimpleDeclarationsInExpression(
      CExpression pExpression, boolean pSearchSubscriptExpression) {

    CSimpleDeclarationCollector simpleDeclarationCollector =
        new CSimpleDeclarationCollector(pSearchSubscriptExpression);
    pExpression.accept(simpleDeclarationCollector);
    return simpleDeclarationCollector.getCollectedSimpleDeclarations();
  }

  private static class CSimpleDeclarationCollector extends CExpressionTraversalVisitor {

    private final boolean searchSubscriptExpression;

    private final ImmutableSet.Builder<CSimpleDeclaration> simpleDeclarations =
        ImmutableSet.builder();

    private CSimpleDeclarationCollector(boolean pSearchSubscriptExpression) {
      searchSubscriptExpression = pSearchSubscriptExpression;
    }

    /** Returns the possibly empty set of {@link CSimpleDeclaration} collected during the search. */
    private ImmutableSet<CSimpleDeclaration> getCollectedSimpleDeclarations() {
      return simpleDeclarations.build();
    }

    @Override
    boolean shouldSearchSubscriptExpression() {
      return searchSubscriptExpression;
    }

    @Override
    void onVisit(CExpression pExpression) {
      if (pExpression instanceof CIdExpression idExpression) {
        simpleDeclarations.add(idExpression.getDeclaration());
      }
    }
  }

  public static class CExpressionCollector<T extends CExpression>
      extends CExpressionTraversalVisitor {

    private final Class<T> expressionToCollect;

    private final ImmutableSet.Builder<T> collected = ImmutableSet.builder();

    public CExpressionCollector(Class<T> pExpressionToCollect) {
      expressionToCollect = pExpressionToCollect;
    }

    /** Returns the possibly empty set collected expressions during the search. */
    public ImmutableSet<T> getCollected() {
      return collected.build();
    }

    @Override
    void onVisit(CExpression pExpression) {
      if (expressionToCollect.isInstance(pExpression)) {
        collected.add(expressionToCollect.cast(pExpression));
      }
    }

    @Override
    boolean shouldStop(CExpression pExpression) {
      return expressionToCollect.isInstance(pExpression);
    }
  }

  private abstract static class CExpressionTraversalVisitor
      extends DefaultCExpressionVisitor<Void, NoException> {

    /**
     * Called when {@code pExpression} is visited. This function is called before {@link
     * CExpressionTraversalVisitor#shouldStop(CExpression)}.
     */
    abstract void onVisit(CExpression pExpression);

    /**
     * Whether the search should stop after visiting {@code pType}.
     *
     * @return {@code false} by default
     */
    boolean shouldStop(@SuppressWarnings("unused") CExpression pExpression) {
      return false;
    }

    /**
     * Whether the subscript {@link CExpression} of {@link CArraySubscriptExpression} should be
     * searched.
     *
     * @return {@code true} by default
     */
    boolean shouldSearchSubscriptExpression() {
      return true;
    }

    private final Set<CExpression> visitedExpressions = new HashSet<>();

    private boolean shouldSearch(CExpression pExpression) {
      onVisit(pExpression);
      // Prevent any circular searches. This should be handled here, otherwise all
      // subclasses have to implement it because circular searches should never be desired.
      return visitedExpressions.add(pExpression) && !shouldStop(pExpression);
    }

    @Override
    public Void visit(CArraySubscriptExpression pArraySubscriptExpression) {
      if (shouldSearch(pArraySubscriptExpression)) {
        pArraySubscriptExpression.getArrayExpression().accept(this);
        if (shouldSearchSubscriptExpression()) {
          pArraySubscriptExpression.getSubscriptExpression().accept(this);
        }
      }
      return null;
    }

    @Override
    public Void visit(CFieldReference pFieldReference) {
      if (shouldSearch(pFieldReference)) {
        pFieldReference.getFieldOwner().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CPointerExpression pPointerExpression) {
      if (shouldSearch(pPointerExpression)) {
        pPointerExpression.getOperand().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pComplexCastExpression) {
      if (shouldSearch(pComplexCastExpression)) {
        pComplexCastExpression.getOperand().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CBinaryExpression pBinaryExpression) {
      if (shouldSearch(pBinaryExpression)) {
        pBinaryExpression.getOperand1().accept(this);
        pBinaryExpression.getOperand2().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CCastExpression pCastExpression) {
      if (shouldSearch(pCastExpression)) {
        pCastExpression.getOperand().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pUnaryExpression) {
      if (shouldSearch(pUnaryExpression)) {
        pUnaryExpression.getOperand().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CIdExpression pIdExpression) {
      if (shouldSearch(pIdExpression)) {
        pIdExpression.accept(this);
      }
      return null;
    }

    @Override
    protected Void visitDefault(CExpression pExpression) {
      return null;
    }
  }
}
