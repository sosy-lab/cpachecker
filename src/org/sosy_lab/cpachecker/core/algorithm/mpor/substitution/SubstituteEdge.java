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
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Sets;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

/** A simple wrapper for substitutes to {@link CFAEdge}s. */
public class SubstituteEdge {

  public final CFAEdge cfaEdge;

  public final ThreadEdge threadEdge;

  public final ImmutableSet<CParameterDeclaration> accessedMainFunctionArgs;

  public final ImmutableMap<CVariableDeclaration, CSimpleDeclaration> pointerAssignment;

  public final ImmutableTable<
          CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      pointerFieldMemberAssignments;

  // POINTER DEREFERENCES ==========================================================================

  /** The set of accessed pointer derefs i.e. reads and writes. */
  public final ImmutableSet<CSimpleDeclaration> accessedPointerDereferences;

  /** The set of read pointer derefs including reads, e.g. {@code var = 42 + *ptr;} */
  public final ImmutableSet<CSimpleDeclaration> readPointerDereferences;

  /** The set of written pointer derefs, .e.g {@code *ptr = 42;} */
  public final ImmutableSet<CSimpleDeclaration> writtenPointerDereferences;

  // GLOBAL VARIABLES ==============================================================================

  /** The set of global variable declarations that this edge accesses. */
  public final ImmutableSet<CVariableDeclaration> accessedGlobalVariables;

  public final ImmutableSet<CVariableDeclaration> readGlobalVariables;

  public final ImmutableSet<CVariableDeclaration> writtenGlobalVariables;

  // FIELD MEMBERS =================================================================================

  public final ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      accessedFieldMembers;

  public final ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      readFieldMembers;

  public final ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      writtenFieldMembers;

  // FUNCTION POINTERS =============================================================================

  public final ImmutableSet<CFunctionDeclaration> accessedFunctionPointers;

  private SubstituteEdge(
      CFAEdge pCfaEdge,
      ThreadEdge pThreadEdge,
      ImmutableSet<CParameterDeclaration> pAccessedMainFunctionArgs,
      ImmutableMap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignment,
      ImmutableTable<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>
          pPointerFieldMemberAssignments,
      ImmutableSet<CSimpleDeclaration> pWrittenPointerDereferences,
      ImmutableSet<CSimpleDeclaration> pAccessedPointerDereferences,
      ImmutableSet<CVariableDeclaration> pWrittenGlobalVariables,
      ImmutableSet<CVariableDeclaration> pAccessedGlobalVariables,
      ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
          pAccessedFieldMembers,
      ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
          pWrittenFieldMembers,
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
    // pointer assignments
    pointerAssignment = pPointerAssignment;
    pointerFieldMemberAssignments = pPointerFieldMemberAssignments;
    // pointers dereferences
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
    // field members
    accessedFieldMembers = pAccessedFieldMembers;
    writtenFieldMembers = pWrittenFieldMembers;
    readFieldMembers = MPORUtil.symmetricDifference(accessedFieldMembers, writtenFieldMembers);
    // functions
    accessedFunctionPointers = pAccessedFunctionPointers;
  }

  public static SubstituteEdge of(CFAEdge pCfaEdge, ThreadEdge pThreadEdge) {
    return new SubstituteEdge(
        pCfaEdge,
        pThreadEdge,
        ImmutableSet.of(),
        ImmutableMap.of(),
        ImmutableTable.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of(),
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
        pTracker.getPointerFieldMemberAssignments(),
        pTracker.getWrittenPointerDereferences(),
        pTracker.getAccessedPointerDereferences(),
        pTracker.getWrittenGlobalVariables(),
        pTracker.getAccessedGlobalVariables(),
        pTracker.getAccessedFieldMembers(),
        pTracker.getWrittenFieldMembers(),
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

  public ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      getFieldMembersByAccessType(BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableSetMultimap.of();
      case ACCESS -> accessedFieldMembers;
      case READ -> readFieldMembers;
      case WRITE -> writtenFieldMembers;
    };
  }
}
