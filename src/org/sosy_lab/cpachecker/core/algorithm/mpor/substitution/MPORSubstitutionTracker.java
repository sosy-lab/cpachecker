// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

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
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;

/**
 * A class to track certain expressions, statements, ... (such as pointer dereferences and global
 * variable accesses) during substitution.
 */
public class MPORSubstitutionTracker {

  /**
   * The set of accessed main function arguments, used to decide whether to assign them
   * non-deterministically. The nondet assignment may be expensive for some verifiers and should
   * only be done if needed.
   */
  private final Set<CParameterDeclaration> accessedMainFunctionArgs;

  // POINTER ASSIGNMENTS ===========================================================================

  /** Pointer assignments updates to the address. */
  private final Map<CVariableDeclaration, CSimpleDeclaration> pointerAssignments;

  /**
   * e.g. {@code ptr = &outer.inner}. {@link CCompositeTypeMemberDeclaration} is always the
   * innermost member, {@link CSimpleDeclaration} the declaration of the outer struct.
   */
  private final Table<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      pointerFieldMemberAssignments;

  // POINTER DEREFERENCES ==========================================================================

  /**
   * Accessed pointer dereferences e.g. of the form {@code x = *ptr;}. Contains both reads and
   * writes.
   */
  private final Set<CSimpleDeclaration> accessedPointerDereferences;

  /** Written pointer dereferences e.g. of the form {@code *ptr = x;}. */
  private final Set<CSimpleDeclaration> writtenPointerDereferences;

  private final Map<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      accessedFieldReferencePointerDereference;

  private final Map<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      writtenFieldReferencePointerDereference;

  // GLOBAL VARIABLES ==============================================================================

  /**
   * Accessed global variables e.g. of the form {@code if (x == 0);} where {@code x} is a global
   * variable. Contains both reads and writes.
   */
  private final Set<CVariableDeclaration> accessedGlobalVariables;

  /** Written global variables e.g. of the form {@code x = 42;}. */
  private final Set<CVariableDeclaration> writtenGlobalVariables;

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

  // FUNCTION POINTERS =============================================================================

  /** All accessed function pointers. */
  private final Set<CFunctionDeclaration> accessedFunctionPointers;

  public MPORSubstitutionTracker() {
    accessedMainFunctionArgs = new HashSet<>();

    pointerAssignments = new HashMap<>();
    pointerFieldMemberAssignments = HashBasedTable.create();

    accessedPointerDereferences = new HashSet<>();
    writtenPointerDereferences = new HashSet<>();
    accessedFieldReferencePointerDereference = new HashMap<>();
    writtenFieldReferencePointerDereference = new HashMap<>();

    accessedGlobalVariables = new HashSet<>();
    writtenGlobalVariables = new HashSet<>();

    accessedFieldMembers = HashMultimap.create();
    writtenFieldMembers = HashMultimap.create();

    accessedFunctionPointers = new HashSet<>();
  }

  // add methods ===================================================================================

  public void addAccessedMainFunctionArg(CParameterDeclaration pParameterDeclaration) {
    accessedMainFunctionArgs.add(pParameterDeclaration);
  }

  public void addPointerAssignment(
      CVariableDeclaration pLeftHandSide, CSimpleDeclaration pRightHandSide) {

    checkArgument(
        !(pRightHandSide instanceof CFunctionDeclaration),
        "pRightHandSide cannot be CFunctionDeclaration");
    pointerAssignments.put(pLeftHandSide, pRightHandSide);
  }

  public void addPointerFieldMemberAssignment(
      CVariableDeclaration pLeftHandSide,
      CSimpleDeclaration pFieldOwner,
      CCompositeTypeMemberDeclaration pMemberDeclaration) {

    pointerFieldMemberAssignments.put(pLeftHandSide, pFieldOwner, pMemberDeclaration);
  }

  public void addAccessedPointerDereference(CSimpleDeclaration pAccessedPointerDereference) {
    accessedPointerDereferences.add(pAccessedPointerDereference);
  }

  public void addWrittenPointerDereference(CSimpleDeclaration pWrittenPointerDereference) {
    writtenPointerDereferences.add(pWrittenPointerDereference);
  }

  public void addAccessedFieldReferencePointerDereference(
      CSimpleDeclaration pFieldOwner, CCompositeTypeMemberDeclaration pFieldMember) {

    accessedFieldReferencePointerDereference.put(pFieldOwner, pFieldMember);
  }

  public void addWrittenFieldReferencePointerDereference(
      CSimpleDeclaration pFieldOwner, CCompositeTypeMemberDeclaration pFieldMember) {

    writtenFieldReferencePointerDereference.put(pFieldOwner, pFieldMember);
  }

  public void addAccessedGlobalVariable(CVariableDeclaration pAccessedGlobalVariable) {
    accessedGlobalVariables.add(pAccessedGlobalVariable);
  }

  public void addWrittenGlobalVariable(CVariableDeclaration pWrittenGlobalVariable) {
    writtenGlobalVariables.add(pWrittenGlobalVariable);
  }

  public void addAccessedFieldMember(
      CVariableDeclaration pOwnerDeclaration,
      CCompositeTypeMemberDeclaration pAccessedFieldMember) {

    accessedFieldMembers.put(pOwnerDeclaration, pAccessedFieldMember);
  }

  public void addWrittenFieldMember(
      CVariableDeclaration pOwnerDeclaration, CCompositeTypeMemberDeclaration pWrittenFieldMember) {

    writtenFieldMembers.put(pOwnerDeclaration, pWrittenFieldMember);
  }

  public void addAccessedFunctionPointer(CFunctionDeclaration pAccessedFunctionPointer) {
    accessedFunctionPointers.add(pAccessedFunctionPointer);
  }

  // getters =======================================================================================

  public ImmutableSet<CParameterDeclaration> getAccessedMainFunctionArgs() {
    return ImmutableSet.copyOf(accessedMainFunctionArgs);
  }

  public ImmutableMap<CVariableDeclaration, CSimpleDeclaration> getPointerAssignments() {
    return ImmutableMap.copyOf(pointerAssignments);
  }

  public ImmutableTable<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      getPointerFieldMemberAssignments() {

    return ImmutableTable.copyOf(pointerFieldMemberAssignments);
  }

  // pointer dereferences

  public ImmutableSet<CSimpleDeclaration> getAccessedPointerDereferences() {
    return ImmutableSet.copyOf(accessedPointerDereferences);
  }

  public ImmutableSet<CSimpleDeclaration> getWrittenPointerDereferences() {
    return ImmutableSet.copyOf(writtenPointerDereferences);
  }

  public ImmutableMap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      getAccessedFieldReferencePointerDereferences() {

    return ImmutableMap.copyOf(accessedFieldReferencePointerDereference);
  }

  public ImmutableMap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      getWrittenFieldReferencePointerDereferences() {

    return ImmutableMap.copyOf(writtenFieldReferencePointerDereference);
  }

  // global variables

  public ImmutableSet<CVariableDeclaration> getGlobalVariablesByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> throw new IllegalArgumentException("no NONE access type global variables");
      case ACCESS -> getAccessedGlobalVariables();
      case READ -> throw new IllegalArgumentException("no READ access type global variables");
      case WRITE -> getWrittenGlobalVariables();
    };
  }

  public ImmutableSet<CVariableDeclaration> getAccessedGlobalVariables() {
    return ImmutableSet.copyOf(accessedGlobalVariables);
  }

  public ImmutableSet<CVariableDeclaration> getWrittenGlobalVariables() {
    return ImmutableSet.copyOf(writtenGlobalVariables);
  }

  // field members

  public ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      getFieldMembersByAccessType(BitVectorAccessType pAccessType) {

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

  // function pointers

  public ImmutableSet<CFunctionDeclaration> getAccessedFunctionPointers() {
    return ImmutableSet.copyOf(accessedFunctionPointers);
  }
}
