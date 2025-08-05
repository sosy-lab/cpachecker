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
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

/**
 * A class to track certain expressions, statements, ... (such as pointer dereferences and global
 * variable accesses) during substitution.
 */
public class MPORSubstitutionTracker {

  public final boolean isImmutable;
  private final Map<CVariableDeclaration, CVariableDeclaration> pointerAssignments;
  private final Set<CVariableDeclaration> accessedPointerDereferences;
  private final Set<CVariableDeclaration> readPointerDereferences;
  private final Set<CVariableDeclaration> writtenPointerDereferences;
  private final Set<CVariableDeclaration> accessedGlobalVariables;
  private final Set<CVariableDeclaration> readGlobalVariables;
  private final Set<CVariableDeclaration> writtenGlobalVariables;
  private final Set<CFunctionDeclaration> accessedFunctionPointers;

  private MPORSubstitutionTracker() {
    isImmutable = false;
    pointerAssignments = new HashMap<>();
    accessedPointerDereferences = new HashSet<>();
    readPointerDereferences = new HashSet<>();
    writtenPointerDereferences = new HashSet<>();
    accessedGlobalVariables = new HashSet<>();
    readGlobalVariables = new HashSet<>();
    writtenGlobalVariables = new HashSet<>();
    accessedFunctionPointers = new HashSet<>();
  }

  private MPORSubstitutionTracker(
      ImmutableMap<CVariableDeclaration, CVariableDeclaration> pPointerAssignments,
      ImmutableSet<CVariableDeclaration> pAccessedPointerDereferences,
      ImmutableSet<CVariableDeclaration> pWrittenPointerDereferences,
      ImmutableSet<CVariableDeclaration> pAccessedGlobalVariables,
      ImmutableSet<CVariableDeclaration> pWrittenGlobalVariables,
      ImmutableSet<CFunctionDeclaration> pAccessedFunctionPointers) {

    isImmutable = true;
    pointerAssignments = pPointerAssignments;

    accessedPointerDereferences = pAccessedPointerDereferences;
    writtenPointerDereferences = pWrittenPointerDereferences;
    readPointerDereferences =
        Sets.symmetricDifference(writtenPointerDereferences, accessedPointerDereferences)
            .immutableCopy();

    accessedGlobalVariables = pAccessedGlobalVariables;
    writtenGlobalVariables = pWrittenGlobalVariables;
    readGlobalVariables =
        Sets.symmetricDifference(writtenGlobalVariables, accessedGlobalVariables).immutableCopy();

    accessedFunctionPointers = pAccessedFunctionPointers;
  }

  public static MPORSubstitutionTracker mutableInstance() {
    return new MPORSubstitutionTracker();
  }

  public MPORSubstitutionTracker toImmutableCopy() {
    return new MPORSubstitutionTracker(
        ImmutableMap.copyOf(pointerAssignments),
        ImmutableSet.copyOf(accessedPointerDereferences),
        ImmutableSet.copyOf(writtenPointerDereferences),
        ImmutableSet.copyOf(accessedGlobalVariables),
        ImmutableSet.copyOf(writtenGlobalVariables),
        ImmutableSet.copyOf(accessedFunctionPointers));
  }

  // add methods ===================================================================================

  public void addPointerAssignment(
      CVariableDeclaration pLeftHandSide, CVariableDeclaration pRightHandSide) {

    pointerAssignments.put(pLeftHandSide, pRightHandSide);
  }

  public void addWrittenPointerDereference(CVariableDeclaration pWrittenPointerDereference) {
    writtenPointerDereferences.add(pWrittenPointerDereference);
  }

  public void addAccessedPointerDereference(CVariableDeclaration pAccessedPointerDereference) {
    accessedPointerDereferences.add(pAccessedPointerDereference);
  }

  public void addWrittenGlobalVariable(CVariableDeclaration pWrittenGlobalVariable) {
    writtenGlobalVariables.add(pWrittenGlobalVariable);
  }

  public void addAccessedGlobalVariable(CVariableDeclaration pAccessedGlobalVariable) {
    accessedGlobalVariables.add(pAccessedGlobalVariable);
  }

  public void addAccessedFunctionPointer(CFunctionDeclaration pAccessedFunctionPointer) {
    accessedFunctionPointers.add(pAccessedFunctionPointer);
  }

  // getters =======================================================================================

  public ImmutableMap<CVariableDeclaration, CVariableDeclaration> getPointerAssignments() {
    if (pointerAssignments
        instanceof ImmutableMap<CVariableDeclaration, CVariableDeclaration> immutableMap) {
      return immutableMap;
    }
    return ImmutableMap.copyOf(pointerAssignments);
  }

  // pointer dereferences

  public ImmutableSet<CVariableDeclaration> getAccessedPointerDereferences() {
    if (accessedPointerDereferences instanceof ImmutableSet<CVariableDeclaration> immutableSet) {
      return immutableSet;
    }
    return ImmutableSet.copyOf(accessedPointerDereferences);
  }

  public ImmutableSet<CVariableDeclaration> getReadPointerDereferences() {
    if (readPointerDereferences instanceof ImmutableSet<CVariableDeclaration> immutableSet) {
      return immutableSet;
    }
    return ImmutableSet.copyOf(readPointerDereferences);
  }

  public ImmutableSet<CVariableDeclaration> getWrittenPointerDereferences() {
    if (writtenPointerDereferences instanceof ImmutableSet<CVariableDeclaration> immutableSet) {
      return immutableSet;
    }
    return ImmutableSet.copyOf(writtenPointerDereferences);
  }

  // global variables

  public ImmutableSet<CVariableDeclaration> getAccessedGlobalVariables() {
    if (accessedGlobalVariables instanceof ImmutableSet<CVariableDeclaration> immutableSet) {
      return immutableSet;
    }
    return ImmutableSet.copyOf(accessedGlobalVariables);
  }

  public ImmutableSet<CVariableDeclaration> getReadGlobalVariables() {
    if (readGlobalVariables instanceof ImmutableSet<CVariableDeclaration> immutableSet) {
      return immutableSet;
    }
    return ImmutableSet.copyOf(readGlobalVariables);
  }

  public ImmutableSet<CVariableDeclaration> getWrittenGlobalVariables() {
    if (writtenGlobalVariables instanceof ImmutableSet<CVariableDeclaration> immutableSet) {
      return immutableSet;
    }
    return ImmutableSet.copyOf(writtenGlobalVariables);
  }

  // function pointers

  public ImmutableSet<CFunctionDeclaration> getAccessedFunctionPointers() {
    if (accessedFunctionPointers instanceof ImmutableSet<CFunctionDeclaration> immutableSet) {
      return immutableSet;
    }
    return ImmutableSet.copyOf(accessedFunctionPointers);
  }
}
