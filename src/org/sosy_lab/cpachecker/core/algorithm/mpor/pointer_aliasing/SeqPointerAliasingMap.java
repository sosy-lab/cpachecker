// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

/**
 * A class to keep track of all memory locations in the concurrent input program and pointers and
 * the memory locations they point to.
 */
public class SeqPointerAliasingMap {

  private final ImmutableSet<SeqMemoryLocation> allMemoryLocations;

  private final int relevantMemoryLocationAmount;

  /**
   * The set of relevant {@link SeqMemoryLocation}s, i.e. all that are needed to decide whether a
   * statement commutes. This includes explicit and implicit (through pointers) memory locations.
   */
  private final ImmutableMap<SeqMemoryLocation, Integer> relevantMemoryLocations;

  public final ImmutableSet<SeqPointerAssignment> pointerAssignments;

  public final ImmutableSet<SeqMemoryLocation> pointerDereferences;

  SeqPointerAliasingMap(
      MPOROptions pOptions,
      ImmutableSet<SeqMemoryLocation> pAllMemoryLocations,
      ImmutableMap<SeqMemoryLocation, Integer> pRelevantMemoryLocationIds,
      ImmutableSet<SeqPointerAssignment> pPointerAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences,
      MachineModel pMachineModel)
      throws UnsupportedCodeException {

    if (pOptions.bitVectorEncoding().isDense) {
      final int maximumBitVectorLength =
          pMachineModel.getSizeofLongLongInt() * pMachineModel.getSizeofCharInBits();
      if (pRelevantMemoryLocationIds.size() > maximumBitVectorLength) {
        throw new UnsupportedCodeException(
            String.format(
                "The input program contains too many relevant memory locations (> %s). Try setting"
                    + " bitVectorEncoding=SPARSE.",
                maximumBitVectorLength),
            null);
      }
    }

    allMemoryLocations = pAllMemoryLocations;
    relevantMemoryLocationAmount = pRelevantMemoryLocationIds.size();
    relevantMemoryLocations = pRelevantMemoryLocationIds;
    pointerAssignments = pPointerAssignments;
    pointerDereferences = pPointerDereferences;
  }

  // boolean helpers ===============================================================================

  /**
   * Returns true if the pointer in {@code pMemoryLocation} is assigned a pointer {@code ptr}, or
   * the address of a non-pointer {@code &non_ptr}.
   */
  static boolean isLeftHandSideInPointerAssignment(
      SeqMemoryLocation pMemoryLocation, ImmutableSet<SeqPointerAssignment> pPointerAssignments) {

    final boolean isLeftHandSide =
        pPointerAssignments.stream()
            .anyMatch(a -> a.leftHandSideMemoryLocation().equals(pMemoryLocation));
    if (pMemoryLocation.isFieldOwnerPointerType()) {
      return isLeftHandSide
          || isLeftHandSideInPointerAssignment(
              pMemoryLocation.getFieldOwnerMemoryLocation(), pPointerAssignments);
    }
    return isLeftHandSide;
  }

  public boolean isMemoryLocationReachableByThread(
      SeqMemoryLocation pMemoryLocation,
      MPORThread pThread,
      ImmutableMap<CFAEdgeForThread, SubstituteEdge> pSubstituteEdges,
      SeqMemoryAccessType pAccessType) {

    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
      ImmutableSet<SeqMemoryLocation> memoryLocations =
          SeqMemoryLocationFinder.findMemoryLocationsBySubstituteEdge(
              substituteEdge, this, pAccessType);
      if (memoryLocations.contains(pMemoryLocation)) {
        return true;
      }
    }
    return false;
  }

  // getters =======================================================================================

  public ImmutableSet<SeqPointerAssignment> extractPointerAssignmentsByType(
      SeqPointerAssignmentType pType) {

    return pointerAssignments.stream()
        .filter(a -> a.type().equals(pType))
        .collect(ImmutableSet.toImmutableSet());
  }

  static ImmutableSet<SeqMemoryLocation> getPointerAssignmentRightHandSides(
      SeqMemoryLocation pMemoryLocation, ImmutableSet<SeqPointerAssignment> pPointerAssignments) {

    ImmutableSet.Builder<SeqMemoryLocation> rRightHandSides = ImmutableSet.builder();

    for (SeqPointerAssignment pointerAssignment : pPointerAssignments) {
      if (pointerAssignment.leftHandSideMemoryLocation().equals(pMemoryLocation)) {
        rRightHandSides.add(pointerAssignment.rightHandSideMemoryLocation());
      }
    }
    if (pMemoryLocation.isFieldOwnerPointerType()) {
      rRightHandSides.addAll(
          getPointerAssignmentRightHandSides(
              pMemoryLocation.getFieldOwnerMemoryLocation(), pPointerAssignments));
    }

    return rRightHandSides.build();
  }

  public int getRelevantMemoryLocationAmount() {
    return relevantMemoryLocationAmount;
  }

  public ImmutableSet<SeqMemoryLocation> getAllMemoryLocations() {
    return allMemoryLocations;
  }

  /**
   * Returns the set of relevant memory locations, i.e. all that are important to decide whether two
   * statements commute. These are all explicit and implicit global memory locations.
   */
  public ImmutableMap<SeqMemoryLocation, Integer> getRelevantMemoryLocations() {
    return relevantMemoryLocations;
  }
}
