// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
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

    for (CParameterDeclaration mainFunctionArg : pFrom.getAccessedMainFunctionArgs()) {
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
    for (CSimpleDeclaration accessedPointerDereference : pFrom.getAccessedPointerDereferences()) {
      pTo.addAccessedPointerDereference(accessedPointerDereference);
    }
    for (CSimpleDeclaration writtenPointerDereference : pFrom.getWrittenPointerDereferences()) {
      pTo.addWrittenPointerDereference(writtenPointerDereference);
    }
    // pointer dereferences from field members
    for (CSimpleDeclaration fieldOwner :
        pFrom.getAccessedFieldReferencePointerDereferences().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getAccessedFieldReferencePointerDereferences().get(fieldOwner)) {
        pTo.addAccessedFieldReferencePointerDereference(fieldOwner, fieldMember);
      }
    }
    for (CSimpleDeclaration fieldOwner :
        pFrom.getWrittenFieldReferencePointerDereferences().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getWrittenFieldReferencePointerDereferences().get(fieldOwner)) {
        pTo.addWrittenFieldReferencePointerDereference(fieldOwner, fieldMember);
      }
    }
    // declarations accessed
    for (CSimpleDeclaration accessedDeclaration : pFrom.getAccessedDeclarations()) {
      pTo.addAccessedDeclaration(accessedDeclaration);
    }
    for (CSimpleDeclaration writtenDeclaration : pFrom.getWrittenDeclarations()) {
      pTo.addWrittenDeclaration(writtenDeclaration);
    }
    // field members accessed
    for (CSimpleDeclaration fieldOwner : pFrom.getAccessedFieldMembers().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getAccessedFieldMembers().get(fieldOwner)) {
        pTo.addAccessedFieldMember(fieldOwner, fieldMember);
      }
    }
    for (CSimpleDeclaration fieldOwner : pFrom.getWrittenFieldMembers().keySet()) {
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
      Optional<MPORSubstitutionTracker> pTracker)
      throws UnsupportedCodeException {

    // writing pointers (aliasing) may not be allowed -> reject program
    InputRejection.checkPointerWrite(pIsWrite, pOptions, pIdExpression);

    // exclude field references, we track field members separately. field owner is tracked via the
    // CIdExpression, e.g. if we assign struct_a = struct_b without any field reference.
    if (pTracker.isEmpty() || pIsFieldReference) {
      return;
    }
    // exclude pointer dereferences, they are handled separately
    if (!pIsPointerDereference) {
      CSimpleDeclaration simpleDeclaration = pIdExpression.getDeclaration();
      pTracker.orElseThrow().addAccessedDeclaration(simpleDeclaration);
      CType type = simpleDeclaration.getType();
      boolean isMutex = PthreadObjectType.PTHREAD_MUTEX_T.equalsType(type);
      // treat pthread_mutex_t lock/unlock as writes, otherwise interleavings are lost
      if (pIsWrite || isMutex) {
        pTracker.orElseThrow().addWrittenDeclaration(simpleDeclaration);
      }
    }
  }

  public static void trackContentFromLocalVariableDeclaration(
      boolean pIsDeclaration,
      LocalVariableDeclarationSubstitute pLocalVariableDeclarationSubstitute,
      Optional<MPORSubstitutionTracker> pTracker)
      throws UnsupportedCodeException {

    if (pTracker.isEmpty()) {
      return;
    }
    // only track the global variables when actually substituting the declaration. otherwise when
    // we use the local, non-pointer variable, the global variable is considered as accessed too
    if (pIsDeclaration) {
      if (pLocalVariableDeclarationSubstitute.isTrackerPresent()) {
        MPORSubstitutionTrackerUtil.copyContents(
            pLocalVariableDeclarationSubstitute.getTracker(), pTracker.orElseThrow());
      }
    }
  }

  public static void trackMainFunctionArg(
      CParameterDeclaration pMainFunctionArg, Optional<MPORSubstitutionTracker> pTracker) {

    if (pTracker.isEmpty()) {
      return;
    }
    pTracker.orElseThrow().addAccessedMainFunctionArg(pMainFunctionArg);
  }

  public static void trackPointerAssignment(
      CExpressionAssignmentStatement pAssignment, Optional<MPORSubstitutionTracker> pTracker)
      throws UnsupportedCodeException {

    if (pTracker.isEmpty()) {
      return;
    }
    CLeftHandSide leftHandSide = pAssignment.getLeftHandSide();
    if (leftHandSide instanceof CIdExpression lhsId) {
      CSimpleDeclaration lhsDeclaration = lhsId.getDeclaration();
      if (lhsDeclaration.getType() instanceof CPointerType) {
        CExpression rightHandSide = pAssignment.getRightHandSide();
        Optional<CSimpleDeclaration> pointerDeclaration =
            MPORUtil.tryGetPointerDeclaration(rightHandSide);
        MPORSubstitutionTracker tracker = pTracker.orElseThrow();
        if (pointerDeclaration.isPresent()) {
          tracker.addPointerAssignment(lhsDeclaration, pointerDeclaration.orElseThrow());
        } else {
          Optional<Entry<CSimpleDeclaration, CCompositeTypeMemberDeclaration>> fieldMemberPointer =
              MPORUtil.tryGetFieldMemberPointer(rightHandSide);
          if (fieldMemberPointer.isPresent()) {
            tracker.addPointerFieldMemberAssignment(
                lhsDeclaration,
                fieldMemberPointer.orElseThrow().getKey(),
                fieldMemberPointer.orElseThrow().getValue());
          }
        }
      }
    }
  }

  public static void trackPointerAssignmentInVariableDeclaration(
      CVariableDeclaration pVariableDeclaration, Optional<MPORSubstitutionTracker> pTracker)
      throws UnsupportedCodeException {

    InputRejection.checkFunctionPointerAssignment(pVariableDeclaration);
    if (pTracker.isEmpty()) {
      return;
    }
    if (pVariableDeclaration.getType() instanceof CPointerType) {
      CInitializer initializer = pVariableDeclaration.getInitializer();
      if (initializer instanceof CInitializerExpression initializerExpression) {
        Optional<CSimpleDeclaration> initializerDeclaration =
            MPORUtil.tryGetPointerDeclaration(initializerExpression.getExpression());
        if (initializerDeclaration.isPresent()) {
          CSimpleDeclaration pointerDeclaration = initializerDeclaration.orElseThrow();
          if (SubstituteUtil.isSubstitutable(pointerDeclaration)) {
            pTracker.orElseThrow().addPointerAssignment(pVariableDeclaration, pointerDeclaration);
          }
        }
      }
    }
  }

  public static void trackPointerDereferenceByPointerExpression(
      CPointerExpression pPointerExpression,
      boolean pIsWrite,
      Optional<MPORSubstitutionTracker> pTracker) {

    if (pTracker.isEmpty()) {
      return;
    }
    if (pPointerExpression.getOperand() instanceof CIdExpression idExpression) {
      // do not consider CFunctionDeclarations
      if (SubstituteUtil.isSubstitutable(idExpression.getDeclaration())) {
        if (pIsWrite) {
          pTracker.orElseThrow().addWrittenPointerDereference(idExpression.getDeclaration());
        }
        pTracker.orElseThrow().addAccessedPointerDereference(idExpression.getDeclaration());
      }
    }
  }

  public static void trackPointerDereferenceByArraySubscriptExpression(
      CArraySubscriptExpression pArraySubscriptExpression,
      boolean pIsWrite,
      Optional<MPORSubstitutionTracker> pTracker) {

    if (pTracker.isEmpty()) {
      return;
    }
    // TODO if the subscript expression is an integer literal, track the exact index, not just the
    //  entire array
    CExpression arrayExpression = pArraySubscriptExpression.getArrayExpression();
    if (arrayExpression instanceof CIdExpression idExpression) {
      if (idExpression.getExpressionType() instanceof CPointerType) {
        // do not consider CFunctionDeclarations
        if (SubstituteUtil.isSubstitutable(idExpression.getDeclaration())) {
          if (pIsWrite) {
            pTracker.orElseThrow().addWrittenPointerDereference(idExpression.getDeclaration());
          }
          pTracker.orElseThrow().addAccessedPointerDereference(idExpression.getDeclaration());
        }
      }
    }
  }

  public static void trackPointerDereferenceByFieldReference(
      CFieldReference pFieldReference,
      boolean pIsWrite,
      Optional<MPORSubstitutionTracker> pTracker) {

    if (pTracker.isEmpty()) {
      return;
    }
    if (pFieldReference.isPointerDereference()) {
      assert pFieldReference.getFieldOwner().getExpressionType() instanceof CPointerType
          : "if pFieldReference is a pointer dereference, its owner type must be CPointerType";
      CPointerType pointerType = (CPointerType) pFieldReference.getFieldOwner().getExpressionType();
      CSimpleDeclaration fieldOwner =
          MPORUtil.recursivelyFindFieldOwner(pFieldReference).getDeclaration();
      CCompositeTypeMemberDeclaration fieldMember =
          MPORUtil.recursivelyFindFieldMemberByFieldOwner(pFieldReference, pointerType.getType());
      if (pIsWrite) {
        pTracker.orElseThrow().addWrittenFieldReferencePointerDereference(fieldOwner, fieldMember);
      }
      pTracker.orElseThrow().addAccessedFieldReferencePointerDereference(fieldOwner, fieldMember);
    }
  }

  public static void trackFieldReference(
      CFieldReference pFieldReference,
      boolean pIsWrite,
      Optional<MPORSubstitutionTracker> pTracker) {

    if (pTracker.isEmpty()) {
      return;
    }
    trackPointerDereferenceByFieldReference(pFieldReference, pIsWrite, pTracker);
    // CIdExpression is e.g. 'queue' in 'queue->amount'
    if (pFieldReference.getFieldOwner() instanceof CIdExpression idExpression) {
      // typedef is e.g. 'QType' or for pointers 'QType*'
      if (idExpression.getExpressionType() instanceof CTypedefType typedefType) {
        trackFieldReferenceByTypedefType(
            pFieldReference, idExpression, typedefType, pIsWrite, pTracker.orElseThrow());

      } else if (idExpression.getExpressionType() instanceof CPointerType pointerType) {
        if (pointerType.getType() instanceof CTypedefType typedefType) {
          trackFieldReferenceByTypedefType(
              pFieldReference, idExpression, typedefType, pIsWrite, pTracker.orElseThrow());

        } else if (pointerType.getType() instanceof CElaboratedType elaboratedType) {
          trackFieldReferenceByElaboratedType(
              pFieldReference, idExpression, elaboratedType, pIsWrite, pTracker.orElseThrow());
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
}
