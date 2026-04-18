// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.MemoryModelUtil.CFieldMemberDeclarationVisitor;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitutionTracker.CFieldReferenceTrackerResult;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitutionTracker.CVariableDeclarationTrackerResult;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class MPORSubstitutionTrackerUtil {

  // Copy ==========================================================================================

  private static void copyContents(MPORSubstitutionTracker pFrom, MPORSubstitutionTracker pTo) {
    for (CVariableDeclaration mainFunctionArg : pFrom.getAccessedMainFunctionArgs()) {
      pTo.addAccessedMainFunctionArg(mainFunctionArg);
    }
    // pointer assignments
    for (var entry : pFrom.getPointerAssignments().entrySet()) {
      pTo.addPointerAssignment(
          entry.getKey().declaration(),
          entry.getKey().fieldMember(),
          entry.getKey().expression(),
          entry.getValue().declaration(),
          entry.getKey().fieldMember(),
          entry.getValue().expression());
    }
    // pointer dereferences
    for (CVariableDeclarationTrackerResult accessedPointerDereference :
        pFrom.getAccessedPointerDereferences()) {
      pTo.addAccessedPointerDereference(
          accessedPointerDereference.variableDeclaration(),
          accessedPointerDereference.expression());
    }
    for (CVariableDeclarationTrackerResult writtenPointerDereference :
        pFrom.getWrittenPointerDereferences()) {
      pTo.addWrittenPointerDereference(
          writtenPointerDereference.variableDeclaration(), writtenPointerDereference.expression());
    }
    // pointer dereferences from field members
    for (CFieldReferenceTrackerResult fieldReferencePointerDereference :
        pFrom.getAccessedFieldReferencePointerDereferences()) {
      pTo.addAccessedFieldReferencePointerDereference(
          fieldReferencePointerDereference.fieldOwner(),
          fieldReferencePointerDereference.fieldMember(),
          fieldReferencePointerDereference.fieldReference());
    }
    for (CFieldReferenceTrackerResult fieldReferencePointerDereference :
        pFrom.getWrittenFieldReferencePointerDereferences()) {
      pTo.addWrittenFieldReferencePointerDereference(
          fieldReferencePointerDereference.fieldOwner(),
          fieldReferencePointerDereference.fieldMember(),
          fieldReferencePointerDereference.fieldReference());
    }
    // declarations accessed
    for (CVariableDeclarationTrackerResult accessedDeclaration : pFrom.getAccessedDeclarations()) {
      pTo.addAccessedDeclaration(
          accessedDeclaration.variableDeclaration(), accessedDeclaration.expression());
    }
    for (CVariableDeclarationTrackerResult writtenDeclaration : pFrom.getWrittenDeclarations()) {
      pTo.addWrittenDeclaration(
          writtenDeclaration.variableDeclaration(), writtenDeclaration.expression());
    }
    // field members accessed
    for (CFieldReferenceTrackerResult fieldMember : pFrom.getAccessedFieldMembers()) {
      pTo.addAccessedFieldMember(
          fieldMember.fieldOwner(), fieldMember.fieldMember(), fieldMember.fieldReference());
    }
    for (CFieldReferenceTrackerResult fieldMember : pFrom.getWrittenFieldMembers()) {
      pTo.addWrittenFieldMember(
          fieldMember.fieldOwner(), fieldMember.fieldMember(), fieldMember.fieldReference());
    }
  }

  // Declarations ==================================================================================

  /**
   * If applicable, adds the {@link CVariableDeclaration} of {@code pIdExpression} to the respective
   * sets. {@code pIsWrite} is used to determine whether the expression to substitute is written,
   * i.e. a LHS in an assignment.
   */
  static void trackDeclarationAccess(
      MPOROptions pOptions,
      CIdExpression pIdExpression,
      boolean pIsWrite,
      boolean pIsPointerDereference,
      boolean pIsFieldReference,
      MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    // writing pointers (aliasing) may not be allowed -> reject program
    InputRejection.checkPointerWrite(pIsWrite, pOptions, pIdExpression);

    // exclude field references, we track field members separately. field owner is tracked via the
    // CIdExpression, e.g. if we assign struct_a = struct_b without any field reference.
    if (pIsFieldReference) {
      return;
    }
    // exclude pointer dereferences, they are handled separately
    if (!pIsPointerDereference) {
      CSimpleDeclaration simpleDeclaration = pIdExpression.getDeclaration();
      pTracker.addAccessedDeclaration(simpleDeclaration, pIdExpression);
      CType type = simpleDeclaration.getType();
      boolean isMutex = PthreadObjectType.PTHREAD_MUTEX_T.equalsType(type);
      // treat pthread_mutex_t lock/unlock as writes, otherwise interleavings are lost
      if (pIsWrite || isMutex) {
        pTracker.addWrittenDeclaration(simpleDeclaration, pIdExpression);
      }
    }
  }

  static void trackContentFromLocalVariableDeclaration(
      boolean pIsDeclaration,
      LocalVariableDeclarationSubstitute pLocalVariableDeclarationSubstitute,
      MPORSubstitutionTracker pTracker) {

    // only track the global variables when actually substituting the declaration. otherwise when
    // we use the local, non-pointer variable, the global variable is considered as accessed too
    if (pIsDeclaration) {
      if (pLocalVariableDeclarationSubstitute.tracker().isPresent()) {
        MPORSubstitutionTrackerUtil.copyContents(
            pLocalVariableDeclarationSubstitute.tracker().orElseThrow(), pTracker);
      }
    }
  }

  // Pointer Assignments ===========================================================================

  static void trackPointerAssignment(
      CLeftHandSide pLeftHandSide, CExpression pRightHandSide, MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    InputRejection.checkFunctionPointerAssignment(pRightHandSide);
    InputRejection.checkPointerWriteBinaryExpression(pLeftHandSide, pRightHandSide);

    CSimpleDeclaration leftHandSideDeclaration =
        pLeftHandSide.accept(new CLeftHandSideSimpleDeclarationVisitor());
    if (isAnyCPointerType(leftHandSideDeclaration.getType())) {
      CPointerAssignmentVisitResult leftHandSideVisitResult =
          pLeftHandSide.accept(new CPointerAssignmentVisitor());
      if (leftHandSideVisitResult != null) {
        // if LHS has a field member that is not CPointerType, then it is not a pointer assignment
        if (leftHandSideVisitResult.fieldMember().isPresent()) {
          if (!isAnyCPointerType(leftHandSideVisitResult.fieldMember().orElseThrow().getType())) {
            return;
          }
        }
        CPointerAssignmentVisitResult rightHandSideVisitResult =
            pRightHandSide.accept(new CPointerAssignmentVisitor());
        // visitResult can be null, e.g., if pRightHandSide is a literal int like '0'
        if (rightHandSideVisitResult != null) {
          pTracker.addPointerAssignment(
              leftHandSideVisitResult.declaration(),
              leftHandSideVisitResult.fieldMember(),
              leftHandSideVisitResult.expression(),
              rightHandSideVisitResult.declaration(),
              rightHandSideVisitResult.fieldMember(),
              rightHandSideVisitResult.expression());
        }
      }
    }
  }

  static void trackPointerAssignmentInVariableDeclaration(
      CVariableDeclaration pVariableDeclaration,
      CIdExpression pIdExpression,
      MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    checkArgument(
        pVariableDeclaration.equals(pIdExpression.getDeclaration()),
        "pVariableDeclaration must be equal to pIdExpression.getDeclaration().");
    if (pVariableDeclaration.getInitializer()
        instanceof CInitializerExpression initializerExpression) {
      trackPointerAssignment(pIdExpression, initializerExpression.getExpression(), pTracker);
    }
  }

  /**
   * Checks if {@code pType} or any nested type is a {@link CPointerType}. The search for nested
   * types stops when encountering the name of a {@link PthreadObjectType} so that these are not
   * fully searched.
   *
   * <p>For example, {@link PthreadObjectType#PTHREAD_MUTEX_T} contains an inner pointer somewhere,
   * even if the outer type is not a pointer, but then it would be treated as a pointer. But for the
   * sequentialization, it is only necessary to treat a {@link PthreadObjectType#PTHREAD_MUTEX_T} as
   * a pointer if it is a pointer itself, not any of its inner types.
   */
  private static boolean isAnyCPointerType(CType pType) {
    ImmutableSet<String> stopNames = PthreadObjectType.getAllPthreadObjectTypeNames();
    TypeCollectorWithStopNames typeCollectorWithStop = new TypeCollectorWithStopNames(stopNames);
    pType.accept(typeCollectorWithStop);
    return typeCollectorWithStop.getCollectedTypes().stream()
        .anyMatch(t -> t instanceof CPointerType);
  }

  // Pointer Dereferences ==========================================================================

  static void trackPointerDereferenceByPointerExpression(
      CPointerExpression pPointerExpression, boolean pIsWrite, MPORSubstitutionTracker pTracker) {

    if (pPointerExpression.getOperand() instanceof CIdExpression idExpression) {
      // do not consider CFunctionDeclarations
      if (SubstituteUtil.isSubstitutable(idExpression.getDeclaration())) {
        if (pIsWrite) {
          pTracker.addWrittenPointerDereference(idExpression.getDeclaration(), idExpression);
        }
        pTracker.addAccessedPointerDereference(idExpression.getDeclaration(), idExpression);
      }
    }
  }

  static void trackPointerDereferenceByArraySubscriptExpression(
      CArraySubscriptExpression pArraySubscriptExpression,
      boolean pIsWrite,
      MPORSubstitutionTracker pTracker) {

    // TODO if the subscript expression is an integer literal, track the exact index, not just the
    //  entire array
    CExpression arrayExpression = pArraySubscriptExpression.getArrayExpression();
    if (arrayExpression instanceof CIdExpression idExpression) {
      if (idExpression.getExpressionType() instanceof CPointerType) {
        // do not consider CFunctionDeclarations
        if (SubstituteUtil.isSubstitutable(idExpression.getDeclaration())) {
          if (pIsWrite) {
            pTracker.addWrittenPointerDereference(idExpression.getDeclaration(), idExpression);
          }
          pTracker.addAccessedPointerDereference(idExpression.getDeclaration(), idExpression);
        }
      }
    }
  }

  private static void trackPointerDereferenceByFieldReference(
      CFieldReference pFieldReference, boolean pIsWrite, MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    if (pFieldReference.isPointerDereference()) {
      // if pFieldReference is a pointer dereference, its owner type must be CPointerType
      CPointerType pointerType = (CPointerType) pFieldReference.getFieldOwner().getExpressionType();
      CSimpleDeclaration fieldOwner =
          MPORUtil.recursivelyFindFieldOwner(pFieldReference).getDeclaration();
      CCompositeTypeMemberDeclaration fieldMember =
          pointerType.getType().accept(new CFieldMemberDeclarationVisitor(pFieldReference));
      if (pIsWrite) {
        pTracker.addWrittenFieldReferencePointerDereference(
            fieldOwner, fieldMember, pFieldReference);
      }
      pTracker.addAccessedFieldReferencePointerDereference(
          fieldOwner, fieldMember, pFieldReference);
    }
  }

  static void trackFieldReference(
      CFieldReference pFieldReference, boolean pIsWrite, MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    trackPointerDereferenceByFieldReference(pFieldReference, pIsWrite, pTracker);
    // CIdExpression is e.g. 'queue' in 'queue->amount'
    if (pFieldReference.getFieldOwner() instanceof CIdExpression idExpression) {
      // typedef is e.g. 'QType' or for pointers 'QType*'
      if (idExpression.getExpressionType() instanceof CTypedefType typedefType) {
        trackFieldReferenceByTypedefType(
            pFieldReference, idExpression, typedefType, pIsWrite, pTracker);

      } else if (idExpression.getExpressionType() instanceof CPointerType pointerType) {
        if (pointerType.getType() instanceof CTypedefType typedefType) {
          trackFieldReferenceByTypedefType(
              pFieldReference, idExpression, typedefType, pIsWrite, pTracker);

        } else if (pointerType.getType() instanceof CElaboratedType elaboratedType) {
          trackFieldReferenceByElaboratedType(
              pFieldReference, idExpression, elaboratedType, pIsWrite, pTracker);
        }
      }

    } else if (pFieldReference.getFieldOwner() instanceof CFieldReference fieldReference) {
      // recursively handle inner structs until outer struct is found, e.g. outer.inner.member
      trackFieldReference(fieldReference, pIsWrite, pTracker);
    }
  }

  private static void trackFieldReferenceByTypedefType(
      CFieldReference pFieldReference,
      CIdExpression pIdExpression,
      CTypedefType pTypedefType,
      boolean pIsWrite,
      MPORSubstitutionTracker pTracker) {

    // elaborated type is e.g. struct __anon_type_QType
    if (pTypedefType.getRealType() instanceof CElaboratedType elaboratedType) {
      trackFieldReferenceByElaboratedType(
          pFieldReference, pIdExpression, elaboratedType, pIsWrite, pTracker);
    } else if (pTypedefType.getRealType() instanceof CTypedefType typeDefType) {
      trackFieldReferenceByTypedefType(
          pFieldReference, pIdExpression, typeDefType, pIsWrite, pTracker);
    }
  }

  private static void trackFieldReferenceByElaboratedType(
      CFieldReference pFieldReference,
      CIdExpression pIdExpression,
      CElaboratedType pElaboratedType,
      boolean pIsWrite,
      MPORSubstitutionTracker pTracker) {

    // composite type contains the composite type members, e.g. 'amount'
    if (pElaboratedType.getRealType() instanceof CCompositeType compositeType) {
      for (CCompositeTypeMemberDeclaration memberDeclaration : compositeType.getMembers()) {
        if (memberDeclaration.getName().equals(pFieldReference.getFieldName())) {
          CSimpleDeclaration simpleDeclaration = pIdExpression.getDeclaration();
          if (pIsWrite) {
            pTracker.addWrittenFieldMember(simpleDeclaration, memberDeclaration, pFieldReference);
          }
          pTracker.addAccessedFieldMember(simpleDeclaration, memberDeclaration, pFieldReference);
        }
      }
    }
  }

  // Visitors ======================================================================================

  private static final class TypeCollectorWithStopNames
      extends DefaultCTypeVisitor<Void, NoException> {

    private final Set<CType> collectedTypes;

    private final ImmutableSet<String> stopNames;

    TypeCollectorWithStopNames(ImmutableSet<String> pStopNames) {
      collectedTypes = new HashSet<>();
      stopNames = pStopNames;
    }

    ImmutableSet<CType> getCollectedTypes() {
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

  private static final class CLeftHandSideSimpleDeclarationVisitor
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

  public record CPointerAssignmentVisitResult(
      CSimpleDeclaration declaration,
      Optional<CCompositeTypeMemberDeclaration> fieldMember,
      CExpression expression) {}

  public static final class CPointerAssignmentVisitor
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
          Objects.requireNonNull(
              pFieldReference
                  .getFieldOwner()
                  .getExpressionType()
                  .accept(new CFieldMemberDeclarationVisitor(pFieldReference)));
      return new CPointerAssignmentVisitResult(
          fieldOwnerResult.declaration, Optional.of(fieldMember), pFieldReference);
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
      return new CPointerAssignmentVisitResult(
          pIdExpression.getDeclaration(), Optional.empty(), pIdExpression);
    }

    @Override
    protected @Nullable CPointerAssignmentVisitResult visitDefault(CExpression exp) {
      return null;
    }
  }
}
