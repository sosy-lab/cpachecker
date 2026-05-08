// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqMemoryLocation;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;

/** A simple wrapper for substitutes to {@link CFAEdge}s. */
public class SubstituteEdge {

  /** The substituted {@link CFAEdge}. */
  public final CFAEdge cfaEdge;

  private final CFAEdgeForThread threadEdge;

  public final ImmutableSet<CVariableDeclaration> accessedMainFunctionArgs;

  public final ImmutableListMultimap<CParameterDeclaration, CIdExpression> parameterSubstitutes;

  // POINTER ASSIGNMENTS ===========================================================================

  public final ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments;

  // POINTER DEREFERENCES ==========================================================================

  /** The set of accessed pointer dereferences i.e. reads and writes. */
  final ImmutableSet<SeqMemoryLocation> accessedPointerDereferences;

  /** The set of read pointer dereferences including reads, e.g. {@code var = 42 + *ptr;} */
  private final ImmutableSet<SeqMemoryLocation> readPointerDereferences;

  /** The set of written pointer dereferences, .e.g {@code *ptr = 42;} */
  private final ImmutableSet<SeqMemoryLocation> writtenPointerDereferences;

  // MEMORY LOCATIONS ==============================================================================

  /** The set of global variable declarations that this edge accesses. */
  final ImmutableSet<SeqMemoryLocation> accessedMemoryLocations;

  private final ImmutableSet<SeqMemoryLocation> readMemoryLocations;

  private final ImmutableSet<SeqMemoryLocation> writtenMemoryLocations;

  private SubstituteEdge(
      CFAEdge pCfaEdge,
      CFAEdgeForThread pThreadEdge,
      ImmutableSet<CVariableDeclaration> pAccessedMainFunctionArgs,
      ImmutableListMultimap<CParameterDeclaration, CIdExpression> pParameterSubstitutes,
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

    accessedMainFunctionArgs = pAccessedMainFunctionArgs;

    parameterSubstitutes = pParameterSubstitutes;
    pointerAssignments = pPointerAssignments;

    accessedPointerDereferences = pAccessedPointerDereferences;
    writtenPointerDereferences = pWrittenPointerDereferences;
    readPointerDereferences =
        Sets.symmetricDifference(writtenPointerDereferences, accessedPointerDereferences)
            .immutableCopy();

    accessedMemoryLocations = pAccessedMemoryLocations;
    writtenMemoryLocations = pWrittenMemoryLocations;
    readMemoryLocations =
        Sets.symmetricDifference(writtenMemoryLocations, accessedMemoryLocations).immutableCopy();
  }

  static SubstituteEdge of(CFAEdge pCfaEdge, CFAEdgeForThread pThreadEdge) {
    return new SubstituteEdge(
        pCfaEdge,
        pThreadEdge,
        ImmutableSet.of(),
        ImmutableListMultimap.of(),
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
  static SubstituteEdge of(
      CFAEdge pCfaEdge,
      CFAEdgeForThread pThreadEdge,
      MPORSubstitution pSubstitution,
      MPORSubstitutionTracker pTracker) {

    ImmutableListMultimap<CParameterDeclaration, CIdExpression> parameterSubstitutes =
        pThreadEdge.callContext.isPresent()
            ? pSubstitution
                .parameterSubstitutes
                .row(pThreadEdge.callContext.orElseThrow())
                .entrySet()
                .stream()
                .flatMap(
                    entry -> entry.getValue().stream().map(val -> Map.entry(entry.getKey(), val)))
                .collect(
                    ImmutableListMultimap.toImmutableListMultimap(Entry::getKey, Entry::getValue))
            : ImmutableListMultimap.of();

    return new SubstituteEdge(
        pCfaEdge,
        pThreadEdge,
        pTracker.getAccessedMainFunctionArgs(),
        parameterSubstitutes,
        pTracker.getPointerAssignments(),
        SubstituteUtil.getPointerDereferencesByAccessType(pTracker, SeqMemoryAccessType.ACCESS),
        SubstituteUtil.getPointerDereferencesByAccessType(pTracker, SeqMemoryAccessType.WRITE),
        SubstituteUtil.getMemoryLocationsByAccessType(pTracker, SeqMemoryAccessType.ACCESS),
        SubstituteUtil.getMemoryLocationsByAccessType(pTracker, SeqMemoryAccessType.WRITE));
  }

  public ImmutableSet<SeqMemoryLocation> getMemoryLocationsByAccessType(
      SeqMemoryAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> accessedMemoryLocations;
      case READ -> readMemoryLocations;
      case WRITE -> writtenMemoryLocations;
    };
  }

  public ImmutableSet<SeqMemoryLocation> getPointerDereferencesByAccessType(
      SeqMemoryAccessType pAccessType) {

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
