// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitutionTracker.CFieldReferenceTrackerResult;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitutionTracker.CVariableDeclarationTrackerResult;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class MPORSubstitutionTrackerUtil {

  // Copy ==========================================================================================

  private static void copyContents(MPORSubstitutionTracker pFrom, MPORSubstitutionTracker pTo)
      throws UnsupportedCodeException {

    for (CVariableDeclaration mainFunctionArg : pFrom.getAccessedMainFunctionArgs()) {
      pTo.addAccessedMainFunctionArg(mainFunctionArg);
    }
    // pointer assignments
    for (var entry : pFrom.getPointerAssignments().entrySet()) {
      pTo.addPointerAssignment(
          entry.getKey().variableDeclaration(),
          entry.getKey().expression(),
          entry.getValue().variableDeclaration(),
          entry.getValue().expression());
    }
    for (var entry : pFrom.getPointerFieldMemberAssignments().entrySet()) {
      pTo.addPointerFieldMemberAssignment(
          entry.getKey().variableDeclaration(),
          entry.getKey().expression(),
          entry.getValue().fieldOwner(),
          entry.getValue().fieldMember(),
          entry.getValue().fieldReference());
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

  // Track =========================================================================================

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
      MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    // only track the global variables when actually substituting the declaration. otherwise when
    // we use the local, non-pointer variable, the global variable is considered as accessed too
    if (pIsDeclaration) {
      if (pLocalVariableDeclarationSubstitute.tracker().isPresent()) {
        MPORSubstitutionTrackerUtil.copyContents(
            pLocalVariableDeclarationSubstitute.tracker().orElseThrow(), pTracker);
      }
    }
  }

  static void trackPointerAssignment(
      CExpressionAssignmentStatement pAssignment, MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    InputRejection.checkPointerWriteBinaryExpression(pAssignment);
    CLeftHandSide leftHandSide = pAssignment.getLeftHandSide();
    if (leftHandSide instanceof CIdExpression lhsId) {
      CSimpleDeclaration lhsDeclaration = lhsId.getDeclaration();
      if (lhsDeclaration.getType() instanceof CPointerType) {
        visitPointerAssignmentAndStoreResultInTracker(
            lhsDeclaration, lhsId, pAssignment.getRightHandSide(), pTracker);
      }
    }
  }

  static void trackPointerAssignmentInVariableDeclaration(
      CVariableDeclaration pVariableDeclaration, MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    InputRejection.checkFunctionPointerAssignment(pVariableDeclaration);
    InputRejection.checkPointerWriteBinaryExpression(pVariableDeclaration);
    if (pVariableDeclaration.getType() instanceof CPointerType) {
      CInitializer initializer = pVariableDeclaration.getInitializer();
      if (initializer instanceof CInitializerExpression initializerExpression) {
        CIdExpression variableIdExpression =
            SeqExpressionBuilder.buildIdExpression(pVariableDeclaration);
        visitPointerAssignmentAndStoreResultInTracker(
            pVariableDeclaration,
            variableIdExpression,
            initializerExpression.getExpression(),
            pTracker);
      }
    }
  }

  private static void visitPointerAssignmentAndStoreResultInTracker(
      CSimpleDeclaration pLeftHandSideDeclaration,
      CIdExpression pLeftHandSideIdExpression,
      CExpression pRightHandSide,
      MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    CPointerAssignmentVisitResult visitResult =
        pRightHandSide.accept(new CPointerAssignmentVisitor());
    // visitResult can be null, e.g., if pRightHandSide is a literal int like '0'
    if (visitResult != null) {
      switch (visitResult) {
        case CPointerAssignmentExpressionResult expressionResult ->
            pTracker.addPointerAssignment(
                pLeftHandSideDeclaration,
                pLeftHandSideIdExpression,
                expressionResult.declaration,
                expressionResult.expression);
        case CPointerAssignmentFieldReferenceResult fieldReferenceResult ->
            pTracker.addPointerFieldMemberAssignment(
                pLeftHandSideDeclaration,
                pLeftHandSideIdExpression,
                fieldReferenceResult.fieldOwner,
                fieldReferenceResult.fieldMember,
                fieldReferenceResult.fieldReference);
        default -> throw new IllegalStateException("Unexpected value: " + visitResult);
      }
    }
  }

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
          MPORUtil.recursivelyFindFieldMemberByFieldOwner(pFieldReference, pointerType.getType());
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

  private interface CPointerAssignmentVisitResult {}

  private record CPointerAssignmentExpressionResult(
      CSimpleDeclaration declaration, CExpression expression)
      implements CPointerAssignmentVisitResult {}

  private record CPointerAssignmentFieldReferenceResult(
      CSimpleDeclaration fieldOwner,
      CCompositeTypeMemberDeclaration fieldMember,
      CFieldReference fieldReference)
      implements CPointerAssignmentVisitResult {}

  private static final class CPointerAssignmentVisitor
      extends DefaultCExpressionVisitor<CPointerAssignmentVisitResult, UnsupportedCodeException> {

    @Override
    public CPointerAssignmentVisitResult visit(CArraySubscriptExpression pArraySubscriptExpression)
        throws UnsupportedCodeException {
      return pArraySubscriptExpression.getSubscriptExpression().accept(this);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CFieldReference pFieldReference)
        throws UnsupportedCodeException {

      CPointerAssignmentExpressionResult fieldOwnerResult =
          (CPointerAssignmentExpressionResult) pFieldReference.getFieldOwner().accept(this);
      CCompositeTypeMemberDeclaration fieldMember =
          MPORUtil.recursivelyFindFieldMemberByFieldOwner(
              pFieldReference, pFieldReference.getFieldOwner().getExpressionType());
      return new CPointerAssignmentFieldReferenceResult(
          fieldOwnerResult.declaration, fieldMember, pFieldReference);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CPointerExpression pPointerExpression)
        throws UnsupportedCodeException {
      return pPointerExpression.getOperand().accept(this);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CComplexCastExpression pComplexCastExpression)
        throws UnsupportedCodeException {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CCastExpression pCastExpression)
        throws UnsupportedCodeException {
      return pCastExpression.getOperand().accept(this);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CUnaryExpression pUnaryExpression)
        throws UnsupportedCodeException {
      return pUnaryExpression.getOperand().accept(this);
    }

    @Override
    public CPointerAssignmentVisitResult visit(CIdExpression pIdExpression) {
      return new CPointerAssignmentExpressionResult(pIdExpression.getDeclaration(), pIdExpression);
    }

    @Override
    protected @Nullable CPointerAssignmentVisitResult visitDefault(CExpression exp) {
      return null;
    }
  }
}
