// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map.Entry;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
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
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class MPORSubstitutionTrackerUtil {

  // Copy ==========================================================================================

  public static void copyContents(MPORSubstitutionTracker pFrom, MPORSubstitutionTracker pTo)
      throws UnsupportedCodeException {

    for (CVariableDeclaration mainFunctionArg : pFrom.getAccessedMainFunctionArgs()) {
      pTo.addAccessedMainFunctionArg(mainFunctionArg);
    }
    // pointer assignments
    for (var entry : pFrom.getPointerAssignments().entrySet()) {
      pTo.addPointerAssignment(entry.getKey(), entry.getValue());
    }
    for (var cell : pFrom.getPointerFieldMemberAssignments().cellSet()) {
      pTo.addPointerFieldMemberAssignment(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
    }
    // pointer dereferences
    for (CVariableDeclaration accessedPointerDereference : pFrom.getAccessedPointerDereferences()) {
      pTo.addAccessedPointerDereference(accessedPointerDereference);
    }
    for (CVariableDeclaration writtenPointerDereference : pFrom.getWrittenPointerDereferences()) {
      pTo.addWrittenPointerDereference(writtenPointerDereference);
    }
    // pointer dereferences from field members
    for (CVariableDeclaration fieldOwner :
        pFrom.getAccessedFieldReferencePointerDereferences().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getAccessedFieldReferencePointerDereferences().get(fieldOwner)) {
        pTo.addAccessedFieldReferencePointerDereference(fieldOwner, fieldMember);
      }
    }
    for (CVariableDeclaration fieldOwner :
        pFrom.getWrittenFieldReferencePointerDereferences().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getWrittenFieldReferencePointerDereferences().get(fieldOwner)) {
        pTo.addWrittenFieldReferencePointerDereference(fieldOwner, fieldMember);
      }
    }
    // declarations accessed
    for (CVariableDeclaration accessedDeclaration : pFrom.getAccessedDeclarations()) {
      pTo.addAccessedDeclaration(accessedDeclaration);
    }
    for (CVariableDeclaration writtenDeclaration : pFrom.getWrittenDeclarations()) {
      pTo.addWrittenDeclaration(writtenDeclaration);
    }
    // field members accessed
    for (CVariableDeclaration fieldOwner : pFrom.getAccessedFieldMembers().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getAccessedFieldMembers().get(fieldOwner)) {
        pTo.addAccessedFieldMember(fieldOwner, fieldMember);
      }
    }
    for (CVariableDeclaration fieldOwner : pFrom.getWrittenFieldMembers().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getWrittenFieldMembers().get(fieldOwner)) {
        pTo.addWrittenFieldMember(fieldOwner, fieldMember);
      }
    }
  }

  // Track =========================================================================================

  /**
   * If applicable, adds the {@link CVariableDeclaration} of {@code pIdExpression} to the respective
   * sets. {@code pIsWrite} is used to determine whether the expression to substitute is written,
   * i.e. a LHS in an assignment.
   */
  public static void trackDeclarationAccess(
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
      pTracker.addAccessedDeclaration(simpleDeclaration);
      CType type = simpleDeclaration.getType();
      boolean isMutex = PthreadObjectType.PTHREAD_MUTEX_T.equalsType(type);
      // treat pthread_mutex_t lock/unlock as writes, otherwise interleavings are lost
      if (pIsWrite || isMutex) {
        pTracker.addWrittenDeclaration(simpleDeclaration);
      }
    }
  }

  public static void trackContentFromLocalVariableDeclaration(
      boolean pIsDeclaration,
      LocalVariableDeclarationSubstitute pLocalVariableDeclarationSubstitute,
      MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    // only track the global variables when actually substituting the declaration. otherwise when
    // we use the local, non-pointer variable, the global variable is considered as accessed too
    if (pIsDeclaration) {
      if (pLocalVariableDeclarationSubstitute.isTrackerPresent()) {
        MPORSubstitutionTrackerUtil.copyContents(
            pLocalVariableDeclarationSubstitute.getTracker(), pTracker);
      }
    }
  }

  public static void trackPointerAssignment(
      CExpressionAssignmentStatement pAssignment, MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    InputRejection.checkPointerWriteBinaryExpression(pAssignment);
    CLeftHandSide leftHandSide = pAssignment.getLeftHandSide();
    if (leftHandSide instanceof CIdExpression lhsId) {
      CSimpleDeclaration lhsDeclaration = lhsId.getDeclaration();
      if (lhsDeclaration.getType() instanceof CPointerType) {
        CExpression rightHandSide = pAssignment.getRightHandSide();
        Optional<CSimpleDeclaration> pointerDeclaration =
            tryExtractSingleDeclaration(rightHandSide);
        if (pointerDeclaration.isPresent()) {
          pTracker.addPointerAssignment(lhsDeclaration, pointerDeclaration.orElseThrow());
        } else {
          Optional<Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>> fieldMemberPointer =
              MPORUtil.tryGetFieldMemberPointer(rightHandSide);
          if (fieldMemberPointer.isPresent()) {
            pTracker.addPointerFieldMemberAssignment(
                lhsDeclaration,
                fieldMemberPointer.orElseThrow().getKey(),
                fieldMemberPointer.orElseThrow().getValue());
          }
        }
      }
    }
  }

  public static void trackPointerAssignmentInVariableDeclaration(
      CVariableDeclaration pVariableDeclaration, MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    InputRejection.checkFunctionPointerAssignment(pVariableDeclaration);
    InputRejection.checkPointerWriteBinaryExpression(pVariableDeclaration);
    if (pVariableDeclaration.getType() instanceof CPointerType) {
      CInitializer initializer = pVariableDeclaration.getInitializer();
      if (initializer instanceof CInitializerExpression initializerExpression) {
        Optional<CSimpleDeclaration> initializerDeclaration =
            tryExtractSingleDeclaration(initializerExpression.getExpression());
        if (initializerDeclaration.isPresent()) {
          CSimpleDeclaration pointerDeclaration = initializerDeclaration.orElseThrow();
          if (SubstituteUtil.isSubstitutable(pointerDeclaration)) {
            pTracker.addPointerAssignment(pVariableDeclaration, pointerDeclaration);
          }
        }
      }
    }
  }

  public static void trackPointerDereferenceByPointerExpression(
      CPointerExpression pPointerExpression, boolean pIsWrite, MPORSubstitutionTracker pTracker) {

    if (pPointerExpression.getOperand() instanceof CIdExpression idExpression) {
      // do not consider CFunctionDeclarations
      if (SubstituteUtil.isSubstitutable(idExpression.getDeclaration())) {
        if (pIsWrite) {
          pTracker.addWrittenPointerDereference(idExpression.getDeclaration());
        }
        pTracker.addAccessedPointerDereference(idExpression.getDeclaration());
      }
    }
  }

  public static void trackPointerDereferenceByArraySubscriptExpression(
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
            pTracker.addWrittenPointerDereference(idExpression.getDeclaration());
          }
          pTracker.addAccessedPointerDereference(idExpression.getDeclaration());
        }
      }
    }
  }

  public static void trackPointerDereferenceByFieldReference(
      CFieldReference pFieldReference, boolean pIsWrite, MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    if (pFieldReference.isPointerDereference()) {
      assert pFieldReference.getFieldOwner().getExpressionType() instanceof CPointerType
          : "if pFieldReference is a pointer dereference, its owner type must be CPointerType";
      CPointerType pointerType = (CPointerType) pFieldReference.getFieldOwner().getExpressionType();
      CSimpleDeclaration fieldOwner =
          MPORUtil.recursivelyFindFieldOwner(pFieldReference).getDeclaration();
      CCompositeTypeMemberDeclaration fieldMember =
          MPORUtil.recursivelyFindFieldMemberByFieldOwner(pFieldReference, pointerType.getType());
      if (pIsWrite) {
        pTracker.addWrittenFieldReferencePointerDereference(fieldOwner, fieldMember);
      }
      pTracker.addAccessedFieldReferencePointerDereference(fieldOwner, fieldMember);
    }
  }

  public static void trackFieldReference(
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

  public static void trackFieldReferenceByTypedefType(
      CFieldReference pFieldReference,
      CIdExpression pIdExpression,
      CTypedefType pTypedefType,
      boolean pIsWrite,
      MPORSubstitutionTracker pTracker) {

    // elaborated type is e.g. struct __anon_type_QType
    if (pTypedefType.getRealType() instanceof CElaboratedType elaboratedType) {
      trackFieldReferenceByElaboratedType(
          pFieldReference, pIdExpression, elaboratedType, pIsWrite, pTracker);
    }
  }

  public static void trackFieldReferenceByElaboratedType(
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
            pTracker.addWrittenFieldMember(simpleDeclaration, memberDeclaration);
          }
          pTracker.addAccessedFieldMember(simpleDeclaration, memberDeclaration);
        }
      }
    }
  }

  /** Extracts the single {@link CSimpleDeclaration} of {@code pExpression} if it can be found. */
  private static Optional<CSimpleDeclaration> tryExtractSingleDeclaration(CExpression pExpression)
      throws UnsupportedCodeException {

    checkArgument(
        !(pExpression instanceof CBinaryExpression),
        "pExpression cannot be CBinaryExpression, since there may be multiple"
            + " CSimpleDeclarations.");
    CIdExpression idExpression = pExpression.accept(new CPointerDeclarationVisitor());
    return Optional.ofNullable(idExpression).map(CIdExpression::getDeclaration);
  }

  private static final class CPointerDeclarationVisitor
      extends DefaultCExpressionVisitor<CIdExpression, UnsupportedCodeException> {

    @Override
    public CIdExpression visit(CArraySubscriptExpression pArraySubscriptExpression)
        throws UnsupportedCodeException {
      return pArraySubscriptExpression.getSubscriptExpression().accept(this);
    }

    @Override
    public CIdExpression visit(CFieldReference pFieldReference) throws UnsupportedCodeException {
      return pFieldReference.getFieldOwner().accept(this);
    }

    @Override
    public CIdExpression visit(CPointerExpression pPointerExpression)
        throws UnsupportedCodeException {
      return pPointerExpression.getOperand().accept(this);
    }

    @Override
    public CIdExpression visit(CComplexCastExpression pComplexCastExpression)
        throws UnsupportedCodeException {
      return pComplexCastExpression.getOperand().accept(this);
    }

    @Override
    public CIdExpression visit(CCastExpression pCastExpression) throws UnsupportedCodeException {
      return pCastExpression.getOperand().accept(this);
    }

    @Override
    public CIdExpression visit(CUnaryExpression pUnaryExpression) throws UnsupportedCodeException {
      return pUnaryExpression.getOperand().accept(this);
    }

    @Override
    public CIdExpression visit(CIdExpression pIdExpression) {
      return pIdExpression;
    }

    @Override
    protected @Nullable CIdExpression visitDefault(CExpression pExpression) {
      return null; // ignore
    }
  }
}
