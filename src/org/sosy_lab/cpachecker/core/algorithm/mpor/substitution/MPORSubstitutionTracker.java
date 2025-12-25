// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * A class to track certain expressions, statements, ... (such as pointer dereferences and variable
 * accesses) during substitution.
 */
public class MPORSubstitutionTracker {

  /**
   * The set of accessed main function arguments, used to decide whether to assign them
   * non-deterministically. The nondet assignment may be expensive for some verifiers and should
   * only be done if needed.
   */
  private final Set<CVariableDeclaration> accessedMainFunctionArgs;

  // POINTER ASSIGNMENTS ===========================================================================

  /** Pointer assignments updates to the address. */
  private final Map<CVariableDeclaration, CVariableDeclaration> pointerAssignments;

  /**
   * e.g. {@code ptr = &outer.inner}. {@link CCompositeTypeMemberDeclaration} is always the
   * innermost member, {@link CVariableDeclaration} the declaration of the outer struct.
   */
  private final Table<CVariableDeclaration, CVariableDeclaration, CCompositeTypeMemberDeclaration>
      pointerFieldMemberAssignments;

  // POINTER DEREFERENCES ==========================================================================

  /**
   * Accessed pointer dereferences e.g. of the form {@code x = *ptr;}. Contains both reads and
   * writes.
   */
  private final Set<CVariableDeclaration> accessedPointerDereferences;

  /** Written pointer dereferences e.g. of the form {@code *ptr = x;}. */
  private final Set<CVariableDeclaration> writtenPointerDereferences;

  private final SetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      accessedFieldReferencePointerDereferences;

  private final SetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      writtenFieldReferencePointerDereferences;

  // GLOBAL VARIABLES ==============================================================================

  /**
   * Accessed variables e.g. of the form {@code if (x == 0);} where {@code x} is a variable.
   * Contains both reads and writes.
   */
  private final Set<CVariableDeclaration> accessedDeclarations;

  /** Written variables e.g. of the form {@code x = 42;}. */
  private final Set<CVariableDeclaration> writtenDeclarations;

  // FIELD MEMBERS =================================================================================

  /**
   * Maps accessed field members to the declaration of their owner, e.g. of the form {@code x =
   * field->member;} where {@code field} is the field owner. {@link CCompositeTypeMemberDeclaration}
   * are not unique to the instance of a struct, forcing us to use the {@link CSimpleDeclaration} of
   * the owner too. Contains both reads and writes.
   */
  private final SetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      accessedFieldMembers;

  /**
   * Maps written field members to the declaration of their owner, e.g. of the form {@code
   * field->member = 42;}.
   */
  private final SetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      writtenFieldMembers;

  public MPORSubstitutionTracker() {
    accessedMainFunctionArgs = new HashSet<>();

    pointerAssignments = new HashMap<>();
    pointerFieldMemberAssignments = HashBasedTable.create();

    accessedPointerDereferences = new HashSet<>();
    writtenPointerDereferences = new HashSet<>();

    accessedFieldReferencePointerDereferences = HashMultimap.create();
    writtenFieldReferencePointerDereferences = HashMultimap.create();

    accessedDeclarations = new HashSet<>();
    writtenDeclarations = new HashSet<>();

    accessedFieldMembers = HashMultimap.create();
    writtenFieldMembers = HashMultimap.create();
  }

  // add methods ===================================================================================

  public void addAccessedMainFunctionArg(CSimpleDeclaration pMainFunctionArg) {
    accessedMainFunctionArgs.add(SubstituteUtil.asVariableDeclaration(pMainFunctionArg));
  }

  public void addPointerAssignment(
      CSimpleDeclaration pLeftHandSide, CSimpleDeclaration pRightHandSide)
      throws UnsupportedCodeException {

    InputRejection.checkFunctionPointerAssignment(pRightHandSide);
    pointerAssignments.put(
        SubstituteUtil.asVariableDeclaration(pLeftHandSide),
        SubstituteUtil.asVariableDeclaration(pRightHandSide));
  }

  public void addPointerFieldMemberAssignment(
      CSimpleDeclaration pLeftHandSide,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pMemberDeclaration) {

    pointerFieldMemberAssignments.put(
        SubstituteUtil.asVariableDeclaration(pLeftHandSide),
        SubstituteUtil.asVariableDeclaration(pFieldOwner),
        pMemberDeclaration);
  }

  public void addAccessedPointerDereference(CSimpleDeclaration pAccessedPointerDereference) {
    accessedPointerDereferences.add(
        SubstituteUtil.asVariableDeclaration(pAccessedPointerDereference));
  }

  public void addWrittenPointerDereference(CSimpleDeclaration pWrittenPointerDereference) {
    writtenPointerDereferences.add(
        SubstituteUtil.asVariableDeclaration(pWrittenPointerDereference));
  }

  public void addAccessedFieldReferencePointerDereference(
      CSimpleDeclaration pFieldOwner, CCompositeTypeMemberDeclaration pFieldMember) {

    accessedFieldReferencePointerDereferences.put(
        SubstituteUtil.asVariableDeclaration(pFieldOwner), pFieldMember);
  }

  public void addWrittenFieldReferencePointerDereference(
      CSimpleDeclaration pFieldOwner, CCompositeTypeMemberDeclaration pFieldMember) {

    writtenFieldReferencePointerDereferences.put(
        SubstituteUtil.asVariableDeclaration(pFieldOwner), pFieldMember);
  }

  public void addAccessedDeclaration(CSimpleDeclaration pAccessedDeclaration) {
    accessedDeclarations.add(SubstituteUtil.asVariableDeclaration(pAccessedDeclaration));
  }

  public void addWrittenDeclaration(CSimpleDeclaration pWrittenDeclaration) {
    writtenDeclarations.add(SubstituteUtil.asVariableDeclaration(pWrittenDeclaration));
  }

  public void addAccessedFieldMember(
      CSimpleDeclaration pOwnerDeclaration, CCompositeTypeMemberDeclaration pAccessedFieldMember) {

    accessedFieldMembers.put(
        SubstituteUtil.asVariableDeclaration(pOwnerDeclaration), pAccessedFieldMember);
  }

  public void addWrittenFieldMember(
      CSimpleDeclaration pOwnerDeclaration, CCompositeTypeMemberDeclaration pWrittenFieldMember) {

    writtenFieldMembers.put(
        SubstituteUtil.asVariableDeclaration(pOwnerDeclaration), pWrittenFieldMember);
  }

  // getters =======================================================================================

  public ImmutableSet<CVariableDeclaration> getAccessedMainFunctionArgs() {
    return ImmutableSet.copyOf(accessedMainFunctionArgs);
  }

  public ImmutableMap<CVariableDeclaration, CVariableDeclaration> getPointerAssignments() {
    return ImmutableMap.copyOf(pointerAssignments);
  }

  public ImmutableTable<CVariableDeclaration, CVariableDeclaration, CCompositeTypeMemberDeclaration>
      getPointerFieldMemberAssignments() {

    return ImmutableTable.copyOf(pointerFieldMemberAssignments);
  }

  // pointer dereferences

  public ImmutableSet<CVariableDeclaration> getPointerDereferencesByAccessType(
      MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type variables");
      case ACCESS -> getAccessedPointerDereferences();
      case READ -> throw new IllegalArgumentException("no READ access type variables");
      case WRITE -> getWrittenPointerDereferences();
    };
  }

  public ImmutableSet<CVariableDeclaration> getAccessedPointerDereferences() {
    return ImmutableSet.copyOf(accessedPointerDereferences);
  }

  public ImmutableSet<CVariableDeclaration> getWrittenPointerDereferences() {
    return ImmutableSet.copyOf(writtenPointerDereferences);
  }

  public ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      getFieldReferencePointerDereferencesByAccessType(MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type variables");
      case ACCESS -> getAccessedFieldReferencePointerDereferences();
      case READ -> throw new IllegalArgumentException("no READ access type variables");
      case WRITE -> getWrittenFieldReferencePointerDereferences();
    };
  }

  public ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      getAccessedFieldReferencePointerDereferences() {

    return ImmutableSetMultimap.copyOf(accessedFieldReferencePointerDereferences);
  }

  public ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      getWrittenFieldReferencePointerDereferences() {

    return ImmutableSetMultimap.copyOf(writtenFieldReferencePointerDereferences);
  }

  // variables

  public ImmutableSet<CVariableDeclaration> getDeclarationsByAccessType(
      MemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type variables");
      case ACCESS -> getAccessedDeclarations();
      case READ -> throw new IllegalArgumentException("no READ access type variables");
      case WRITE -> getWrittenDeclarations();
    };
  }

  public ImmutableSet<CVariableDeclaration> getAccessedDeclarations() {
    return ImmutableSet.copyOf(accessedDeclarations);
  }

  public ImmutableSet<CVariableDeclaration> getWrittenDeclarations() {
    return ImmutableSet.copyOf(writtenDeclarations);
  }

  // field members

  public ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      getFieldMembersByAccessType(MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type field members");
      case ACCESS -> getAccessedFieldMembers();
      case READ -> throw new IllegalArgumentException("no READ access type field members");
      case WRITE -> getWrittenFieldMembers();
    };
  }

  public ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      getAccessedFieldMembers() {

    return ImmutableSetMultimap.copyOf(accessedFieldMembers);
  }

  public ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      getWrittenFieldMembers() {

    return ImmutableSetMultimap.copyOf(writtenFieldMembers);
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
