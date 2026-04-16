// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * A class to track certain expressions, statements, ... (such as pointer dereferences and variable
 * accesses) during substitution.
 */
public class MPORSubstitutionTracker {

  record CFieldReferenceTrackerResult(
      CVariableDeclaration fieldOwner,
      CCompositeTypeMemberDeclaration fieldMember,
      CFieldReference fieldReference) {}

  record CVariableDeclarationTrackerResult(
      CVariableDeclaration variableDeclaration, CExpression expression) {}

  /**
   * The set of accessed main function arguments, used to decide whether to assign them
   * non-deterministically. The nondet assignment may be expensive for some verifiers and should
   * only be done if needed.
   */
  private final Set<CVariableDeclaration> accessedMainFunctionArgs;

  // POINTER ASSIGNMENTS ===========================================================================

  /** Pointer assignments updates to the address. */
  private final Map<CVariableDeclarationTrackerResult, CVariableDeclarationTrackerResult>
      pointerAssignments;

  /**
   * e.g. {@code ptr = &outer.inner}. {@link CCompositeTypeMemberDeclaration} is always the
   * innermost member, {@link CVariableDeclaration} the declaration of the outer struct.
   */
  private final Map<CVariableDeclarationTrackerResult, CFieldReferenceTrackerResult>
      pointerFieldMemberAssignments;

  // POINTER DEREFERENCES ==========================================================================

  /**
   * Accessed pointer dereferences e.g. of the form {@code x = *ptr;}. Contains both reads and
   * writes.
   */
  private final Set<CVariableDeclarationTrackerResult> accessedPointerDereferences;

  /** Written pointer dereferences e.g. of the form {@code *ptr = x;}. */
  private final Set<CVariableDeclarationTrackerResult> writtenPointerDereferences;

  private final Set<CFieldReferenceTrackerResult> accessedFieldReferencePointerDereferences;

  private final Set<CFieldReferenceTrackerResult> writtenFieldReferencePointerDereferences;

  // GLOBAL VARIABLES ==============================================================================

  /**
   * Accessed variables e.g. of the form {@code if (x == 0);} where {@code x} is a variable.
   * Contains both reads and writes.
   */
  private final Set<CVariableDeclarationTrackerResult> accessedDeclarations;

  /** Written variables e.g. of the form {@code x = 42;}. */
  private final Set<CVariableDeclarationTrackerResult> writtenDeclarations;

  // FIELD MEMBERS =================================================================================

  /**
   * Maps accessed field members to the declaration of their owner, e.g. of the form {@code x =
   * field->member;} where {@code field} is the field owner. {@link CCompositeTypeMemberDeclaration}
   * are not unique to the instance of a struct, forcing us to use the {@link CSimpleDeclaration} of
   * the owner too. Contains both reads and writes.
   */
  private final Set<CFieldReferenceTrackerResult> accessedFieldMembers;

  /**
   * Maps written field members to the declaration of their owner, e.g. of the form {@code
   * field->member = 42;}.
   */
  private final Set<CFieldReferenceTrackerResult> writtenFieldMembers;

  public MPORSubstitutionTracker() {
    accessedMainFunctionArgs = new HashSet<>();

    pointerAssignments = new HashMap<>();
    pointerFieldMemberAssignments = new HashMap<>();

    accessedPointerDereferences = new HashSet<>();
    writtenPointerDereferences = new HashSet<>();

    accessedFieldReferencePointerDereferences = new HashSet<>();
    writtenFieldReferencePointerDereferences = new HashSet<>();

    accessedDeclarations = new HashSet<>();
    writtenDeclarations = new HashSet<>();

    accessedFieldMembers = new HashSet<>();
    writtenFieldMembers = new HashSet<>();
  }

  // add methods ===================================================================================

  public void addAccessedMainFunctionArg(CSimpleDeclaration pMainFunctionArg) {
    accessedMainFunctionArgs.add(MPORUtil.convertToVariableDeclaration(pMainFunctionArg));
  }

  public void addPointerAssignment(
      CSimpleDeclaration pLeftHandSide,
      CExpression pLeftHandSideExpression,
      CSimpleDeclaration pRightHandSide,
      CExpression pRightHandSideExpression)
      throws UnsupportedCodeException {

    InputRejection.checkFunctionPointerAssignment(pRightHandSide);
    pointerAssignments.put(
        new CVariableDeclarationTrackerResult(
            MPORUtil.convertToVariableDeclaration(pLeftHandSide), pLeftHandSideExpression),
        new CVariableDeclarationTrackerResult(
            MPORUtil.convertToVariableDeclaration(pRightHandSide), pRightHandSideExpression));
  }

  public void addPointerFieldMemberAssignment(
      CSimpleDeclaration pLeftHandSide,
      CExpression pLeftHandSideExpression,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pMemberDeclaration,
      CFieldReference pFieldReference) {

    pointerFieldMemberAssignments.put(
        new CVariableDeclarationTrackerResult(
            MPORUtil.convertToVariableDeclaration(pLeftHandSide), pLeftHandSideExpression),
        new CFieldReferenceTrackerResult(
            MPORUtil.convertToVariableDeclaration(pFieldOwner),
            pMemberDeclaration,
            pFieldReference));
  }

  public void addAccessedPointerDereference(
      CSimpleDeclaration pAccessedPointerDereference, CExpression pExpression) {

    accessedPointerDereferences.add(
        new CVariableDeclarationTrackerResult(
            MPORUtil.convertToVariableDeclaration(pAccessedPointerDereference), pExpression));
  }

  public void addWrittenPointerDereference(
      CSimpleDeclaration pWrittenPointerDereference, CExpression pExpression) {

    writtenPointerDereferences.add(
        new CVariableDeclarationTrackerResult(
            MPORUtil.convertToVariableDeclaration(pWrittenPointerDereference), pExpression));
  }

  public void addAccessedFieldReferencePointerDereference(
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember,
      CFieldReference pFieldReference) {

    accessedFieldReferencePointerDereferences.add(
        new CFieldReferenceTrackerResult(
            MPORUtil.convertToVariableDeclaration(pFieldOwner), pFieldMember, pFieldReference));
  }

  public void addWrittenFieldReferencePointerDereference(
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pFieldMember,
      CFieldReference pFieldReference) {

    writtenFieldReferencePointerDereferences.add(
        new CFieldReferenceTrackerResult(
            MPORUtil.convertToVariableDeclaration(pFieldOwner), pFieldMember, pFieldReference));
  }

  public void addAccessedDeclaration(
      CSimpleDeclaration pAccessedDeclaration, CExpression pExpression) {

    accessedDeclarations.add(
        new CVariableDeclarationTrackerResult(
            MPORUtil.convertToVariableDeclaration(pAccessedDeclaration), pExpression));
  }

  public void addWrittenDeclaration(
      CSimpleDeclaration pWrittenDeclaration, CExpression pExpression) {

    writtenDeclarations.add(
        new CVariableDeclarationTrackerResult(
            MPORUtil.convertToVariableDeclaration(pWrittenDeclaration), pExpression));
  }

  public void addAccessedFieldMember(
      CSimpleDeclaration pOwnerDeclaration,
      CCompositeTypeMemberDeclaration pAccessedFieldMember,
      CFieldReference pFieldReference) {

    accessedFieldMembers.add(
        new CFieldReferenceTrackerResult(
            MPORUtil.convertToVariableDeclaration(pOwnerDeclaration),
            pAccessedFieldMember,
            pFieldReference));
  }

  public void addWrittenFieldMember(
      CSimpleDeclaration pOwnerDeclaration,
      CCompositeTypeMemberDeclaration pWrittenFieldMember,
      CFieldReference pFieldReference) {

    writtenFieldMembers.add(
        new CFieldReferenceTrackerResult(
            MPORUtil.convertToVariableDeclaration(pOwnerDeclaration),
            pWrittenFieldMember,
            pFieldReference));
  }

  // getters =======================================================================================

  ImmutableSet<CVariableDeclaration> getAccessedMainFunctionArgs() {
    return ImmutableSet.copyOf(accessedMainFunctionArgs);
  }

  ImmutableMap<CVariableDeclarationTrackerResult, CVariableDeclarationTrackerResult>
      getPointerAssignments() {
    return ImmutableMap.copyOf(pointerAssignments);
  }

  ImmutableMap<CVariableDeclarationTrackerResult, CFieldReferenceTrackerResult>
      getPointerFieldMemberAssignments() {

    return ImmutableMap.copyOf(pointerFieldMemberAssignments);
  }

  // pointer dereferences

  ImmutableSet<CVariableDeclarationTrackerResult> getPointerDereferencesByAccessType(
      MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type variables");
      case ACCESS -> getAccessedPointerDereferences();
      case READ -> throw new IllegalArgumentException("no READ access type variables");
      case WRITE -> getWrittenPointerDereferences();
    };
  }

  ImmutableSet<CVariableDeclarationTrackerResult> getAccessedPointerDereferences() {
    return ImmutableSet.copyOf(accessedPointerDereferences);
  }

  ImmutableSet<CVariableDeclarationTrackerResult> getWrittenPointerDereferences() {
    return ImmutableSet.copyOf(writtenPointerDereferences);
  }

  ImmutableSet<CFieldReferenceTrackerResult> getFieldReferencePointerDereferencesByAccessType(
      MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type variables");
      case ACCESS -> getAccessedFieldReferencePointerDereferences();
      case READ -> throw new IllegalArgumentException("no READ access type variables");
      case WRITE -> getWrittenFieldReferencePointerDereferences();
    };
  }

  ImmutableSet<CFieldReferenceTrackerResult> getAccessedFieldReferencePointerDereferences() {
    return ImmutableSet.copyOf(accessedFieldReferencePointerDereferences);
  }

  ImmutableSet<CFieldReferenceTrackerResult> getWrittenFieldReferencePointerDereferences() {
    return ImmutableSet.copyOf(writtenFieldReferencePointerDereferences);
  }

  // variables

  ImmutableSet<CVariableDeclarationTrackerResult> getDeclarationsByAccessType(
      MemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type variables");
      case ACCESS -> getAccessedDeclarations();
      case READ -> throw new IllegalArgumentException("no READ access type variables");
      case WRITE -> getWrittenDeclarations();
    };
  }

  ImmutableSet<CVariableDeclarationTrackerResult> getAccessedDeclarations() {
    return ImmutableSet.copyOf(accessedDeclarations);
  }

  ImmutableSet<CVariableDeclarationTrackerResult> getWrittenDeclarations() {
    return ImmutableSet.copyOf(writtenDeclarations);
  }

  // field members

  ImmutableSet<CFieldReferenceTrackerResult> getFieldMembersByAccessType(
      MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type field members");
      case ACCESS -> getAccessedFieldMembers();
      case READ -> throw new IllegalArgumentException("no READ access type field members");
      case WRITE -> getWrittenFieldMembers();
    };
  }

  ImmutableSet<CFieldReferenceTrackerResult> getAccessedFieldMembers() {
    return ImmutableSet.copyOf(accessedFieldMembers);
  }

  ImmutableSet<CFieldReferenceTrackerResult> getWrittenFieldMembers() {
    return ImmutableSet.copyOf(writtenFieldMembers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        accessedMainFunctionArgs,
        pointerAssignments,
        pointerFieldMemberAssignments,
        accessedPointerDereferences,
        writtenPointerDereferences,
        accessedFieldReferencePointerDereferences,
        writtenFieldReferencePointerDereferences,
        accessedDeclarations,
        writtenDeclarations,
        accessedFieldMembers,
        writtenFieldMembers);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther instanceof MPORSubstitutionTracker other
        && accessedMainFunctionArgs.equals(other.accessedMainFunctionArgs)
        && pointerAssignments.equals(other.pointerAssignments)
        && pointerFieldMemberAssignments.equals(other.pointerFieldMemberAssignments)
        && accessedPointerDereferences.equals(other.accessedPointerDereferences)
        && writtenPointerDereferences.equals(other.writtenPointerDereferences)
        && accessedFieldReferencePointerDereferences.equals(
            other.accessedFieldReferencePointerDereferences)
        && writtenFieldReferencePointerDereferences.equals(
            other.writtenFieldReferencePointerDereferences)
        && accessedDeclarations.equals(other.accessedDeclarations)
        && writtenDeclarations.equals(other.writtenDeclarations)
        && accessedFieldMembers.equals(other.accessedFieldMembers)
        && writtenFieldMembers.equals(other.writtenFieldMembers);
  }
}
