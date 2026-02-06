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
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.MemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

/** A simple wrapper for substitutes to {@link CFAEdge}s. */
public class SubstituteEdge {

  /** The substituted {@link CFAEdge}. */
  public final CFAEdge cfaEdge;

  private final CFAEdgeForThread threadEdge;

  public final ImmutableSet<CVariableDeclaration> accessedMainFunctionArgs;

  // POINTER ASSIGNMENTS ===========================================================================

  public final ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments;

  // POINTER DEREFERENCES ==========================================================================

  /** The set of accessed pointer derefs i.e. reads and writes. */
  public final ImmutableSet<SeqMemoryLocation> accessedPointerDereferences;

  /** The set of read pointer derefs including reads, e.g. {@code var = 42 + *ptr;} */
  public final ImmutableSet<SeqMemoryLocation> readPointerDereferences;

  /** The set of written pointer derefs, .e.g {@code *ptr = 42;} */
  public final ImmutableSet<SeqMemoryLocation> writtenPointerDereferences;

  // MEMORY LOCATIONS ==============================================================================

  /** The set of global variable declarations that this edge accesses. */
  public final ImmutableSet<SeqMemoryLocation> accessedMemoryLocations;

  public final ImmutableSet<SeqMemoryLocation> readMemoryLocations;

  public final ImmutableSet<SeqMemoryLocation> writtenMemoryLocations;

  private SubstituteEdge(
      CFAEdge pCfaEdge,
      CFAEdgeForThread pThreadEdge,
      ImmutableSet<CVariableDeclaration> pAccessedMainFunctionArgs,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableSet<SeqMemoryLocation> pAccessedPointerDereferences,
      ImmutableSet<SeqMemoryLocation> pWrittenPointerDereferences,
      ImmutableSet<SeqMemoryLocation> pAccessedMemoryLocations,
      ImmutableSet<SeqMemoryLocation> pWrittenMemoryLocations) {

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
  }

  public static SubstituteEdge of(CFAEdge pCfaEdge, CFAEdgeForThread pThreadEdge) {
    return new SubstituteEdge(
        pCfaEdge,
        pThreadEdge,
        ImmutableSet.of(),
        ImmutableMap.of(),
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
      CFAEdge pCfaEdge,
      CFAEdgeForThread pThreadEdge,
      MPORSubstitutionTracker pTracker) {

    return new SubstituteEdge(
        pCfaEdge,
        pThreadEdge,
        pTracker.getAccessedMainFunctionArgs(),
        SubstituteUtil.mapPointerAssignments(pOptions, pThreadEdge.callContext, pTracker),
        SubstituteUtil.getPointerDereferencesByAccessType(
            pOptions, pThreadEdge.callContext, pTracker, MemoryAccessType.ACCESS),
        SubstituteUtil.getPointerDereferencesByAccessType(
            pOptions, pThreadEdge.callContext, pTracker, MemoryAccessType.WRITE),
        SubstituteUtil.getMemoryLocationsByAccessType(
            pOptions, pThreadEdge.callContext, pTracker, MemoryAccessType.ACCESS),
        SubstituteUtil.getMemoryLocationsByAccessType(
            pOptions, pThreadEdge.callContext, pTracker, MemoryAccessType.WRITE));
  }

  public ImmutableSet<SeqMemoryLocation> getMemoryLocationsByAccessType(
      MemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> accessedMemoryLocations;
      case READ -> readMemoryLocations;
      case WRITE -> writtenMemoryLocations;
    };
  }

  public ImmutableSet<SeqMemoryLocation> getPointerDereferencesByAccessType(
      MemoryAccessType pAccessType) {

    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> accessedPointerDereferences;
      case READ -> readPointerDereferences;
      case WRITE -> writtenPointerDereferences;
    };
  }

  public CFAEdgeForThread getThreadEdge() {
    return threadEdge;
  }

  public Optional<CFAEdgeForThread> getCallContext() {
    return threadEdge.callContext;
  }

  public CFAEdge getOriginalCfaEdge() {
    return threadEdge.cfaEdge;
  }
}
