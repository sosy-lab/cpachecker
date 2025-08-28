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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

/** A simple wrapper for substitutes to {@link CFAEdge}s. */
public class SubstituteEdge {

  /** The substituted {@link CFAEdge}. */
  public final CFAEdge cfaEdge;

  private final ThreadEdge threadEdge;

  public final ImmutableSet<CParameterDeclaration> accessedMainFunctionArgs;

  // POINTER ASSIGNMENTS ===========================================================================

  public final ImmutableMap<MemoryLocation, MemoryLocation> pointerAssignments;

  // POINTER DEREFERENCES ==========================================================================

  /** The set of accessed pointer derefs i.e. reads and writes. */
  public final ImmutableSet<MemoryLocation> accessedPointerDereferences;

  /** The set of read pointer derefs including reads, e.g. {@code var = 42 + *ptr;} */
  public final ImmutableSet<MemoryLocation> readPointerDereferences;

  /** The set of written pointer derefs, .e.g {@code *ptr = 42;} */
  public final ImmutableSet<MemoryLocation> writtenPointerDereferences;

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
      ImmutableMap<MemoryLocation, MemoryLocation> pPointerAssignments,
      ImmutableSet<MemoryLocation> pAccessedPointerDereferences,
      ImmutableSet<MemoryLocation> pWrittenPointerDereferences,
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
        ImmutableSet.of(),
        ImmutableSet.of(),
        ImmutableSet.of());
  }

  /**
   * Creates a {@link SubstituteEdge} based on the {@link MPORSubstitutionTracker} in {@code
   * pTracker}.
   */
  public static SubstituteEdge of(
      MPOROptions pOptions,
      MPORThread pThread,
      CFAEdge pCfaEdge,
      ThreadEdge pThreadEdge,
      MPORSubstitutionTracker pTracker) {

    return new SubstituteEdge(
        pCfaEdge,
        pThreadEdge,
        pTracker.getAccessedMainFunctionArgs(),
        SubstituteUtil.mapPointerAssignments(pOptions, pThread, pThreadEdge.callContext, pTracker),
        SubstituteUtil.getPointerDereferencesByAccessType(
            pOptions, pThread, pThreadEdge.callContext, pTracker, MemoryAccessType.ACCESS),
        SubstituteUtil.getPointerDereferencesByAccessType(
            pOptions, pThread, pThreadEdge.callContext, pTracker, MemoryAccessType.WRITE),
        SubstituteUtil.getMemoryLocationsByAccessType(
            pOptions, pThread, pThreadEdge.callContext, pTracker, MemoryAccessType.ACCESS),
        SubstituteUtil.getMemoryLocationsByAccessType(
            pOptions, pThread, pThreadEdge.callContext, pTracker, MemoryAccessType.WRITE),
        pTracker.getAccessedFunctionPointers());
  }

  public ImmutableSet<MemoryLocation> getMemoryLocationsByAccessType(MemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> accessedMemoryLocations;
      case READ -> readMemoryLocations;
      case WRITE -> writtenMemoryLocations;
    };
  }

  public ImmutableSet<MemoryLocation> getPointerDereferencesByAccessType(
      MemoryAccessType pAccessType) {

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

  public CFAEdge getOriginalCfaEdge() {
    return threadEdge.cfaEdge;
  }
}
