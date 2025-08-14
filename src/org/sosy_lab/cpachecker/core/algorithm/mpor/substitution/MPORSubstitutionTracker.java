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
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

/**
 * A class to track certain expressions, statements, ... (such as pointer dereferences and global
 * variable accesses) during substitution.
 */
public class MPORSubstitutionTracker {

  public final boolean isImmutable;

  /**
   * The set of accessed main function arguments, used to decide whether to assign them
   * non-deterministically. The nondet assignment is expensive for some verifiers (e.g. CBMC) and
   * should only be done if needed.
   */
  private final Set<CParameterDeclaration> accessedMainFunctionArgs;

  /** Pointer assignments updates to the address. */
  private final Map<CVariableDeclaration, CSimpleDeclaration> pointerAssignments;

  /**
   * Accessed pointer dereferences e.g. of the form {@code x = *ptr;}. Contains both reads and
   * writes.
   */
  private final Set<CSimpleDeclaration> accessedPointerDereferences;

  /** Read pointer dereferences e.g. of the form {@code x = *ptr;}. */
  private final Set<CSimpleDeclaration> readPointerDereferences;

  /** Written pointer dereferences e.g. of the form {@code *ptr = x;}. */
  private final Set<CSimpleDeclaration> writtenPointerDereferences;

  /**
   * Accessed global variables e.g. of the form {@code x++;} where {@code x} is a global variable.
   * Contains both reads and writes.
   */
  private final Set<CVariableDeclaration> accessedGlobalVariables;

  /** Read global variables e.g. of the form {@code if (x == 0) ...}. */
  private final Set<CVariableDeclaration> readGlobalVariables;

  /** Written global variables e.g. of the form {@code x = 42;}. */
  private final Set<CVariableDeclaration> writtenGlobalVariables;

  /** All accessed function pointers. */
  private final Set<CFunctionDeclaration> accessedFunctionPointers;

  private MPORSubstitutionTracker() {
    isImmutable = false;
    accessedMainFunctionArgs = new HashSet<>();
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
      ImmutableSet<CParameterDeclaration> pAccessedMainFunctionArgs,
      ImmutableMap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableSet<CSimpleDeclaration> pAccessedPointerDereferences,
      ImmutableSet<CSimpleDeclaration> pWrittenPointerDereferences,
      ImmutableSet<CVariableDeclaration> pAccessedGlobalVariables,
      ImmutableSet<CVariableDeclaration> pWrittenGlobalVariables,
      ImmutableSet<CFunctionDeclaration> pAccessedFunctionPointers) {

    isImmutable = true;

    accessedMainFunctionArgs = pAccessedMainFunctionArgs;

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
        ImmutableSet.copyOf(accessedMainFunctionArgs),
        ImmutableMap.copyOf(pointerAssignments),
        ImmutableSet.copyOf(accessedPointerDereferences),
        ImmutableSet.copyOf(writtenPointerDereferences),
        ImmutableSet.copyOf(accessedGlobalVariables),
        ImmutableSet.copyOf(writtenGlobalVariables),
        ImmutableSet.copyOf(accessedFunctionPointers));
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

  public void addAccessedFunctionPointer(CFunctionDeclaration pAccessedFunctionPointer) {
    accessedFunctionPointers.add(pAccessedFunctionPointer);
  }

  // getters =======================================================================================

  public ImmutableSet<CParameterDeclaration> getAccessedMainFunctionArgs() {
    if (accessedMainFunctionArgs instanceof ImmutableSet<CParameterDeclaration> immutableSet) {
      return immutableSet;
    }
    return ImmutableSet.copyOf(accessedMainFunctionArgs);
  }

  public ImmutableMap<CVariableDeclaration, CSimpleDeclaration> getPointerAssignments() {
    if (pointerAssignments
        instanceof ImmutableMap<CVariableDeclaration, CSimpleDeclaration> immutableMap) {
      return immutableMap;
    }
    return ImmutableMap.copyOf(pointerAssignments);
  }

  // pointer dereferences

  public ImmutableSet<CSimpleDeclaration> getAccessedPointerDereferences() {
    if (accessedPointerDereferences instanceof ImmutableSet<CSimpleDeclaration> immutableSet) {
      return immutableSet;
    }
    return ImmutableSet.copyOf(accessedPointerDereferences);
  }

  public ImmutableSet<CSimpleDeclaration> getReadPointerDereferences() {
    if (readPointerDereferences instanceof ImmutableSet<CSimpleDeclaration> immutableSet) {
      return immutableSet;
    }
    return ImmutableSet.copyOf(readPointerDereferences);
  }

  public ImmutableSet<CSimpleDeclaration> getWrittenPointerDereferences() {
    if (writtenPointerDereferences instanceof ImmutableSet<CSimpleDeclaration> immutableSet) {
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
