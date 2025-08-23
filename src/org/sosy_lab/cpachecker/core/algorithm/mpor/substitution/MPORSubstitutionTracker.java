// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;

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

  // POINTER DEREFERENCES ==========================================================================

  /**
   * Accessed pointer dereferences e.g. of the form {@code x = *ptr;}. Contains both reads and
   * writes.
   */
  private final Set<CSimpleDeclaration> accessedPointerDereferences;

  /** Written pointer dereferences e.g. of the form {@code *ptr = x;}. */
  private final Set<CSimpleDeclaration> writtenPointerDereferences;

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
   * Accessed field members e.g. of the form {@code x = field->member;} where {@code field} is an
   * instance of a struct. Contains both reads and writes.
   */
  private final Set<CCompositeTypeMemberDeclaration> accessedFieldMembers;

  /** Written field members e.g. of the form {@code field->member = 42;}. */
  private final Set<CCompositeTypeMemberDeclaration> writtenFieldMembers;

  // FUNCTION POINTERS =============================================================================

  /** All accessed function pointers. */
  private final Set<CFunctionDeclaration> accessedFunctionPointers;

  public MPORSubstitutionTracker() {
    accessedMainFunctionArgs = new HashSet<>();
    pointerAssignments = new HashMap<>();

    accessedPointerDereferences = new HashSet<>();
    writtenPointerDereferences = new HashSet<>();

    accessedGlobalVariables = new HashSet<>();
    writtenGlobalVariables = new HashSet<>();

    accessedFieldMembers = new HashSet<>();
    writtenFieldMembers = new HashSet<>();

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

  public void addWrittenPointerDereference(CSimpleDeclaration pWrittenPointerDereference) {
    writtenPointerDereferences.add(pWrittenPointerDereference);
  }

  public void addAccessedPointerDereference(CSimpleDeclaration pAccessedPointerDereference) {
    accessedPointerDereferences.add(pAccessedPointerDereference);
  }

  public void addWrittenGlobalVariable(CVariableDeclaration pWrittenGlobalVariable) {
    writtenGlobalVariables.add(pWrittenGlobalVariable);
  }

  public void addAccessedGlobalVariable(CVariableDeclaration pAccessedGlobalVariable) {
    accessedGlobalVariables.add(pAccessedGlobalVariable);
  }

  public void addWrittenFieldMember(CCompositeTypeMemberDeclaration pWrittenFieldMember) {
    writtenFieldMembers.add(pWrittenFieldMember);
  }

  public void addAccessedFieldMember(CCompositeTypeMemberDeclaration pAccessedFieldMember) {
    accessedFieldMembers.add(pAccessedFieldMember);
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

  // pointer dereferences

  public ImmutableSet<CSimpleDeclaration> getAccessedPointerDereferences() {
    return ImmutableSet.copyOf(accessedPointerDereferences);
  }

  public ImmutableSet<CSimpleDeclaration> getWrittenPointerDereferences() {
    return ImmutableSet.copyOf(writtenPointerDereferences);
  }

  // global variables

  public ImmutableSet<CVariableDeclaration> getAccessedGlobalVariables() {
    return ImmutableSet.copyOf(accessedGlobalVariables);
  }

  public ImmutableSet<CVariableDeclaration> getWrittenGlobalVariables() {
    return ImmutableSet.copyOf(writtenGlobalVariables);
  }

  // field members

  public ImmutableSet<CCompositeTypeMemberDeclaration> getAccessedFieldMembers() {
    return ImmutableSet.copyOf(accessedFieldMembers);
  }

  public ImmutableSet<CCompositeTypeMemberDeclaration> getWrittenFieldMembers() {
    return ImmutableSet.copyOf(writtenFieldMembers);
  }

  // function pointers

  public ImmutableSet<CFunctionDeclaration> getAccessedFunctionPointers() {
    return ImmutableSet.copyOf(accessedFunctionPointers);
  }
}
