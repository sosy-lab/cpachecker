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
import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil.CLeftHandSideSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class MPORSubstitutionTrackerUtil {

  // Copy ==========================================================================================

  private static void copyContents(MPORSubstitutionTracker pFrom, MPORSubstitutionTracker pTo) {
    for (CVariableDeclaration mainFunctionArg : pFrom.getAccessedMainFunctionArgs()) {
      pTo.addAccessedMainFunctionArg(mainFunctionArg);
    }
    // pointer assignments
    for (var entry : pFrom.getPointerAssignments().entrySet()) {
      pTo.addPointerAssignment(entry.getKey(), entry.getValue());
    }
    // pointer dereferences
    for (SeqMemoryLocation accessedPointerDereference : pFrom.getAccessedPointerDereferences()) {
      pTo.addAccessedPointerDereference(
          accessedPointerDereference.callContext(), accessedPointerDereference.declaration());
    }
    for (SeqMemoryLocation writtenPointerDereference : pFrom.getWrittenPointerDereferences()) {
      pTo.addWrittenPointerDereference(
          writtenPointerDereference.callContext(), writtenPointerDereference.declaration());
    }
    // pointer dereferences from field members
    for (SeqMemoryLocation fieldReferencePointerDereference :
        pFrom.getAccessedFieldReferencePointerDereferences()) {
      pTo.addAccessedFieldReferencePointerDereference(
          fieldReferencePointerDereference.callContext(),
          fieldReferencePointerDereference.declaration(),
          fieldReferencePointerDereference.fieldMember().orElseThrow());
    }
    for (SeqMemoryLocation fieldReferencePointerDereference :
        pFrom.getWrittenFieldReferencePointerDereferences()) {
      pTo.addWrittenFieldReferencePointerDereference(
          fieldReferencePointerDereference.callContext(),
          fieldReferencePointerDereference.declaration(),
          fieldReferencePointerDereference.fieldMember().orElseThrow());
    }
    // declarations accessed
    for (SeqMemoryLocation accessedDeclaration : pFrom.getAccessedDeclarations()) {
      pTo.addAccessedDeclaration(
          accessedDeclaration.callContext(), accessedDeclaration.declaration());
    }
    for (SeqMemoryLocation writtenDeclaration : pFrom.getWrittenDeclarations()) {
      pTo.addWrittenDeclaration(writtenDeclaration.callContext(), writtenDeclaration.declaration());
    }
    // field members accessed
    for (SeqMemoryLocation fieldMember : pFrom.getAccessedFieldMembers()) {
      pTo.addAccessedFieldMember(
          fieldMember.callContext(),
          fieldMember.declaration(),
          fieldMember.fieldMember().orElseThrow());
    }
    for (SeqMemoryLocation fieldMember : pFrom.getWrittenFieldMembers()) {
      pTo.addWrittenFieldMember(
          fieldMember.callContext(),
          fieldMember.declaration(),
          fieldMember.fieldMember().orElseThrow());
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
      Optional<CFAEdgeForThread> pCallContext,
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
      pTracker.addAccessedDeclaration(pCallContext, simpleDeclaration);
      CType type = simpleDeclaration.getType();
      boolean isMutex = PthreadObjectType.PTHREAD_MUTEX_T.equalsType(type);
      // treat pthread_mutex_t lock/unlock as writes, otherwise interleavings are lost
      if (pIsWrite || isMutex) {
        pTracker.addWrittenDeclaration(pCallContext, simpleDeclaration);
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
      CLeftHandSide pLeftHandSide,
      CRightHandSide pRightHandSide,
      Optional<CFAEdgeForThread> pCallContext,
      CFA pInputCfa,
      MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    Optional<Map.Entry<SeqMemoryLocation, SeqMemoryLocation>> pointerAssignment =
        SeqPointerAliasingUtil.tryMapPointerAssignment(
            // since all raw assignments from the input program are not across a function (e.g.
            // parameter or return value assignments), the same call context is used for the
            // left-hand and right-hand sides
            pLeftHandSide, pRightHandSide, pCallContext, pCallContext, pInputCfa);
    if (pointerAssignment.isPresent()) {
      pTracker.addPointerAssignment(
          pointerAssignment.orElseThrow().getKey(), pointerAssignment.orElseThrow().getValue());
    }
  }

  static void trackPointerAssignmentInVariableDeclaration(
      CVariableDeclaration pVariableDeclaration,
      CIdExpression pIdExpression,
      Optional<CFAEdgeForThread> pCallContext,
      CFA pInputCfa,
      MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    checkArgument(
        pVariableDeclaration.equals(pIdExpression.getDeclaration()),
        "pVariableDeclaration must be equal to pIdExpression.getDeclaration().");
    if (pVariableDeclaration.getInitializer()
        instanceof CInitializerExpression initializerExpression) {
      trackPointerAssignment(
          pIdExpression, initializerExpression.getExpression(), pCallContext, pInputCfa, pTracker);
    }
  }

  // Pointer Dereferences ==========================================================================

  /**
   * Tracks pointer dereferences through {@link CLeftHandSide}, e.g., {@code *var} or {@code arr[i]}
   * which is equivalent to {@code *(arr + i)}.
   */
  static void trackPointerDereferenceByLeftHandSide(
      CLeftHandSide pLeftHandSide,
      Optional<CFAEdgeForThread> pCallContext,
      boolean pIsWrite,
      MPORSubstitutionTracker pTracker) {

    CSimpleDeclaration declaration =
        pLeftHandSide.accept(new CLeftHandSideSimpleDeclarationVisitor());
    // do not consider CFunctionDeclarations
    if (SubstituteUtil.isSubstitutable(declaration)) {
      if (pIsWrite) {
        pTracker.addWrittenPointerDereference(pCallContext, declaration);
      }
      pTracker.addAccessedPointerDereference(pCallContext, declaration);
    }
  }

  // Field References ==============================================================================

  static void trackFieldReference(
      CFieldReference pFieldReference,
      Optional<CFAEdgeForThread> pCallContext,
      boolean pIsWrite,
      MPORSubstitutionTracker pTracker)
      throws UnsupportedCodeException {

    InputRejection.checkMultipleDeclarationsInFieldReferenceOwner(pFieldReference);

    ImmutableSet<CSimpleDeclaration> fieldOwnerDeclarations =
        SeqPointerAliasingUtil.getAllSimpleDeclarationsInExpression(pFieldReference, false);

    // it is possible that the CFieldReference contains no CSimpleDeclaration at all:
    // '((struct s *)0)->list' -> there is no CIdExpression and no CSimpleDeclaration to track
    if (!fieldOwnerDeclarations.isEmpty()) {
      CSimpleDeclaration fieldOwnerDeclaration = Iterables.getOnlyElement(fieldOwnerDeclarations);
      CCompositeTypeMemberDeclaration fieldMember =
          SeqPointerAliasingUtil.getCompositeTypeMemberDeclarationByFieldName(
              pFieldReference.getFieldOwner().getExpressionType(), pFieldReference.getFieldName());

      if (pFieldReference.isPointerDereference()) {
        if (pIsWrite) {
          pTracker.addWrittenFieldReferencePointerDereference(
              pCallContext, fieldOwnerDeclaration, fieldMember);
        }
        pTracker.addAccessedFieldReferencePointerDereference(
            pCallContext, fieldOwnerDeclaration, fieldMember);
      }

      if (pIsWrite) {
        pTracker.addWrittenFieldMember(pCallContext, fieldOwnerDeclaration, fieldMember);
      }
      pTracker.addAccessedFieldMember(pCallContext, fieldOwnerDeclaration, fieldMember);
    }
  }
}
