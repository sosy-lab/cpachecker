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
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil.CLeftHandSideSimpleDeclarationVisitor;
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

    InputRejection.checkFunctionPointerRightHandSide(pRightHandSide);
    InputRejection.checkPointerWriteBinaryExpression(pLeftHandSide, pRightHandSide);

    CSimpleDeclaration leftHandSideDeclaration =
        pLeftHandSide.accept(new CLeftHandSideSimpleDeclarationVisitor());
    CType leftHandSideType = leftHandSideDeclaration.getType();
    ImmutableSet<String> stopNames = PthreadObjectType.getAllPthreadObjectTypeNames();

    if (SeqPointerAliasingUtil.isAnyTypeTargetType(
        leftHandSideType, CPointerType.class, stopNames)) {
      CPointerAssignmentVisitResult leftHandSideVisitResult =
          pLeftHandSide.accept(new CPointerAssignmentVisitor());
      if (leftHandSideVisitResult != null) {
        // if LHS has a field member that is not CPointerType, then it is not a pointer assignment
        if (leftHandSideVisitResult.fieldMember().isPresent()) {
          CType fieldMemberType = leftHandSideVisitResult.fieldMember().orElseThrow().getType();
          if (!SeqPointerAliasingUtil.isAnyTypeTargetType(
              fieldMemberType, CPointerType.class, stopNames)) {
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

  private record CPointerAssignmentVisitResult(
      CSimpleDeclaration declaration,
      Optional<CCompositeTypeMemberDeclaration> fieldMember,
      CExpression expression) {}

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

  // Pointer Dereferences ==========================================================================

  /**
   * Tracks pointer dereferences through {@link CLeftHandSide}, e.g., {@code *var} or {@code arr[i]}
   * which is equivalent to {@code *(arr + i)}.
   */
  static void trackPointerDereferenceByLeftHandSide(
      CLeftHandSide pLeftHandSide, boolean pIsWrite, MPORSubstitutionTracker pTracker) {

    CSimpleDeclaration declaration =
        pLeftHandSide.accept(new CLeftHandSideSimpleDeclarationVisitor());
    // do not consider CFunctionDeclarations
    if (SubstituteUtil.isSubstitutable(declaration)) {
      CVariableDeclaration variableDeclaration = MPORUtil.convertToVariableDeclaration(declaration);
      CIdExpression idExpression = new CIdExpression(FileLocation.DUMMY, variableDeclaration);
      if (pIsWrite) {
        pTracker.addWrittenPointerDereference(declaration, idExpression);
      }
      pTracker.addAccessedPointerDereference(declaration, idExpression);
    }
  }

  // Field References ==============================================================================

  static void trackFieldReference(
      CFieldReference pFieldReference, boolean pIsWrite, MPORSubstitutionTracker pTracker) {

    if (pFieldReference.isPointerDereference()) {
      trackPointerDereferenceByFieldReference(pFieldReference, pIsWrite, pTracker);
    }

    CSimpleDeclaration fieldOwner =
        pFieldReference.accept(new CLeftHandSideSimpleDeclarationVisitor());
    CCompositeTypeMemberDeclaration fieldMember =
        SeqPointerAliasingUtil.getCompositeTypeMemberDeclarationByFieldName(
            pFieldReference.getFieldOwner().getExpressionType(), pFieldReference.getFieldName());
    if (pIsWrite) {
      pTracker.addWrittenFieldMember(fieldOwner, fieldMember, pFieldReference);
    }
    pTracker.addAccessedFieldMember(fieldOwner, fieldMember, pFieldReference);
  }

  private static void trackPointerDereferenceByFieldReference(
      CFieldReference pFieldReference, boolean pIsWrite, MPORSubstitutionTracker pTracker) {

    checkArgument(
        pFieldReference.isPointerDereference(), "pFieldReference must be pointer dereference.");
    checkArgument(
        pFieldReference.getFieldOwner().getExpressionType() instanceof CPointerType,
        "pFieldReference owner type must be CPointerType.");

    CPointerType pointerType = (CPointerType) pFieldReference.getFieldOwner().getExpressionType();
    CSimpleDeclaration fieldOwner =
        pFieldReference.accept(new CLeftHandSideSimpleDeclarationVisitor());
    CCompositeTypeMemberDeclaration fieldMember =
        SeqPointerAliasingUtil.getCompositeTypeMemberDeclarationByFieldName(
            pointerType.getType(), pFieldReference.getFieldName());
    if (pIsWrite) {
      pTracker.addWrittenFieldReferencePointerDereference(fieldOwner, fieldMember, pFieldReference);
    }
    pTracker.addAccessedFieldReferencePointerDereference(fieldOwner, fieldMember, pFieldReference);
  }
}
