// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

/** A simple wrapper for substitutes to {@link CFAEdge}s. */
public class SubstituteEdge {

  public final CFAEdge cfaEdge;

  public final ThreadEdge threadEdge;

  public final ImmutableSet<CParameterDeclaration> accessedMainFunctionArgs;

  public final ImmutableMap<CVariableDeclaration, CVariableDeclaration> pointerAssignment;

  /** The set of accessed pointer derefs i.e. reads and writes. */
  public final ImmutableSet<CSimpleDeclaration> accessedPointerDereferences;

  /** The set of read pointer derefs including reads, e.g. {@code var = 42 + *ptr;} */
  public final ImmutableSet<CSimpleDeclaration> readPointerDereferences;

  /** The set of written pointer derefs, .e.g {@code *ptr = 42;} */
  public final ImmutableSet<CSimpleDeclaration> writtenPointerDereferences;

  /** The set of global variable declarations that this edge accesses. */
  public final ImmutableSet<CVariableDeclaration> accessedGlobalVariables;

  public final ImmutableSet<CVariableDeclaration> readGlobalVariables;

  public final ImmutableSet<CVariableDeclaration> writtenGlobalVariables;

  public final ImmutableSet<CFunctionDeclaration> accessedFunctionPointers;

  private SubstituteEdge(
      CFAEdge pCfaEdge,
      ThreadEdge pThreadEdge,
      ImmutableSet<CParameterDeclaration> pAccessedMainFunctionArgs,
      ImmutableMap<CVariableDeclaration, CVariableDeclaration> pPointerAssignment,
      ImmutableSet<CSimpleDeclaration> pWrittenPointerDereferences,
      ImmutableSet<CSimpleDeclaration> pAccessedPointerDereferences,
      ImmutableSet<CVariableDeclaration> pWrittenGlobalVariables,
      ImmutableSet<CVariableDeclaration> pAccessedGlobalVariables,
      ImmutableSet<CFunctionDeclaration> pAccessedFunctionPointers) {

    // TODO maybe make it an optional single entry then? ...
    checkArgument(
        pPointerAssignment.size() <= 1, "a single edge can have either 0 or 1 pointer assignments");
    checkArgument(
        pCfaEdge.equals(pThreadEdge.cfaEdge), "pCfaEdge and pThreadEdge cfaEdge must match");

    cfaEdge = pCfaEdge;
    threadEdge = pThreadEdge;
    // main function args
    accessedMainFunctionArgs = pAccessedMainFunctionArgs;
    // pointers
    pointerAssignment = pPointerAssignment;
    writtenPointerDereferences = pWrittenPointerDereferences;
    accessedPointerDereferences = pAccessedPointerDereferences;
    readPointerDereferences =
        Sets.symmetricDifference(writtenPointerDereferences, accessedPointerDereferences)
            .immutableCopy();
    // global variables
    writtenGlobalVariables = pWrittenGlobalVariables;
    accessedGlobalVariables = pAccessedGlobalVariables;
    readGlobalVariables =
        Sets.symmetricDifference(writtenGlobalVariables, accessedGlobalVariables).immutableCopy();
    // functions
    accessedFunctionPointers = pAccessedFunctionPointers;
  }

  public static SubstituteEdge of(CFAEdge pCfaEdge, ThreadEdge pThreadEdge) {
    return new SubstituteEdge(
        pCfaEdge,
        pThreadEdge,
        ImmutableSet.of(),
        ImmutableMap.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of());
  }

  /**
   * Creates a {@link SubstituteEdge} based on the {@link MPORSubstitutionTracker} in {@code
   * pTracker}.
   */
  public static SubstituteEdge of(
      CFAEdge pCfaEdge, ThreadEdge pThreadEdge, MPORSubstitutionTracker pTracker) {

    return new SubstituteEdge(
        pCfaEdge,
        pThreadEdge,
        pTracker.getAccessedMainFunctionArgs(),
        pTracker.getPointerAssignments(),
        pTracker.getWrittenPointerDereferences(),
        pTracker.getAccessedPointerDereferences(),
        pTracker.getWrittenGlobalVariables(),
        pTracker.getAccessedGlobalVariables(),
        pTracker.getAccessedFunctionPointers());
  }

  public ImmutableSet<CVariableDeclaration> getGlobalVariablesByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> accessedGlobalVariables;
      case READ -> readGlobalVariables;
      case WRITE -> writtenGlobalVariables;
    };
  }

  public ImmutableSet<CSimpleDeclaration> getPointerDereferencesByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> accessedPointerDereferences;
      case READ -> readPointerDereferences;
      case WRITE -> writtenPointerDereferences;
    };
  }
}
