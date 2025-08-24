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
import com.google.common.collect.Sets;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

/** A simple wrapper for substitutes to {@link CFAEdge}s. */
public class SubstituteEdge {

  /** The substituted {@link CFAEdge}. */
  public final CFAEdge cfaEdge;

  private final ThreadEdge threadEdge;

  public final ImmutableSet<CParameterDeclaration> accessedMainFunctionArgs;

  // POINTER ASSIGNMENTS ===========================================================================

  public final ImmutableMap<CVariableDeclaration, MemoryLocation> pointerAssignments;

  // POINTER DEREFERENCES ==========================================================================

  /** The set of accessed pointer derefs i.e. reads and writes. */
  public final ImmutableSet<CSimpleDeclaration> accessedPointerDereferences;

  /** The set of read pointer derefs including reads, e.g. {@code var = 42 + *ptr;} */
  public final ImmutableSet<CSimpleDeclaration> readPointerDereferences;

  /** The set of written pointer derefs, .e.g {@code *ptr = 42;} */
  public final ImmutableSet<CSimpleDeclaration> writtenPointerDereferences;

  // FIELD REFERENCE POINTER DEREFERENCES ==========================================================

  public final ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      accessedFieldReferencePointerDereferences;

  public final ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      readFieldReferencePointerDereferences;

  public final ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      writtenFieldReferencePointerDereferences;

  // MEMORY LOCATIONS ==============================================================================

  /** The set of global variable declarations that this edge accesses. */
  public final ImmutableSet<MemoryLocation> accessedMemoryLocations;

  public final ImmutableSet<MemoryLocation> readMemoryLocations;

  public final ImmutableSet<MemoryLocation> writtenMemoryLocations;

  // FUNCTION POINTERS =============================================================================

  public final ImmutableSet<CFunctionDeclaration> accessedFunctionPointers;

  private SubstituteEdge(
      CFAEdge pCfaEdge,
      ThreadEdge pThreadEdge,
      ImmutableSet<CParameterDeclaration> pAccessedMainFunctionArgs,
      ImmutableMap<CVariableDeclaration, MemoryLocation> pPointerAssignments,
      ImmutableSet<CSimpleDeclaration> pAccessedPointerDereferences,
      ImmutableSet<CSimpleDeclaration> pWrittenPointerDereferences,
      ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
          pAccessedFieldReferencePointerDereferences,
      ImmutableSetMultimap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
          pWrittenFieldReferencePointerDereferences,
      ImmutableSet<MemoryLocation> pAccessedMemoryLocations,
      ImmutableSet<MemoryLocation> pWrittenMemoryLocations,
      ImmutableSet<CFunctionDeclaration> pAccessedFunctionPointers) {

    // TODO maybe make it an optional single entry then? ...
    checkArgument(
        pPointerAssignments.size() <= 1,
        "a single edge can have either 0 or 1 pointer assignments");
    checkArgument(
        pCfaEdge.equals(pThreadEdge.cfaEdge), "pCfaEdge and pThreadEdge cfaEdge must match");

    cfaEdge = pCfaEdge;
    threadEdge = pThreadEdge;
    // main function args
    accessedMainFunctionArgs = pAccessedMainFunctionArgs;
    // pointer assignments
    pointerAssignments = pPointerAssignments;
    // pointer dereferences
    accessedPointerDereferences = pAccessedPointerDereferences;
    writtenPointerDereferences = pWrittenPointerDereferences;
    readPointerDereferences =
        Sets.symmetricDifference(writtenPointerDereferences, accessedPointerDereferences)
            .immutableCopy();
    // field reference pointer dereferences
    accessedFieldReferencePointerDereferences = pAccessedFieldReferencePointerDereferences;
    writtenFieldReferencePointerDereferences = pWrittenFieldReferencePointerDereferences;
    readFieldReferencePointerDereferences =
        MPORUtil.symmetricDifference(
            writtenFieldReferencePointerDereferences, accessedFieldReferencePointerDereferences);
    // memory locations
    accessedMemoryLocations = pAccessedMemoryLocations;
    writtenMemoryLocations = pWrittenMemoryLocations;
    readMemoryLocations =
        Sets.symmetricDifference(writtenMemoryLocations, accessedMemoryLocations).immutableCopy();
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
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of(),
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
        SubstituteUtil.mapPointerAssignments(pTracker),
        pTracker.getAccessedPointerDereferences(),
        pTracker.getWrittenPointerDereferences(),
        pTracker.getAccessedFieldReferencePointerDereferences(),
        pTracker.getWrittenFieldReferencePointerDereferences(),
        SubstituteUtil.getMemoryLocationsByAccessType(pTracker, BitVectorAccessType.ACCESS),
        SubstituteUtil.getMemoryLocationsByAccessType(pTracker, BitVectorAccessType.WRITE),
        pTracker.getAccessedFunctionPointers());
  }

  public ImmutableSet<MemoryLocation> getMemoryLocationsByAccessType(
      BitVectorAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> accessedMemoryLocations;
      case READ -> readMemoryLocations;
      case WRITE -> writtenMemoryLocations;
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

  public ThreadEdge getThreadEdge() {
    return threadEdge;
  }

  public Optional<ThreadEdge> getCallContext() {
    return threadEdge.callContext;
  }

  public CFAEdge getOriginalEdge() {
    return threadEdge.cfaEdge;
  }
}
