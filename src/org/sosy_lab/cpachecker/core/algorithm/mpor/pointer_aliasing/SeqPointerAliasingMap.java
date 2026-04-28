// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
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

  public final ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pointerAssignments;

  /**
   * Keep track of {@code start_routine arg} assignments in {@code pthread_create} separately, since
   * even a local memory address passed here is implicitly global.
   */
  public final ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> startRoutineArgAssignments;

  /**
   * The map of call context-sensitive {@link SeqMemoryLocation} mapped to their assigned {@link
   * SeqMemoryLocation}. Each parameter is only assigned once due to function cloning. Note that
   * this is not restricted to pointers, since non-pointer parameters can be made implicitly global
   * through global pointers.
   */
  public final ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> parameterAssignments;

  /** The subset of parameters that are pointers. */
  public final ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pointerParameterAssignments;

  public final ImmutableSet<SeqMemoryLocation> pointerDereferences;

  SeqPointerAliasingMap(
      MPOROptions pOptions,
      ImmutableSet<SeqMemoryLocation> pAllMemoryLocations,
      ImmutableMap<SeqMemoryLocation, Integer> pRelevantMemoryLocationIds,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pParameterAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
      ImmutableSet<SeqMemoryLocation> pPointerDereferences,
      MachineModel pMachineModel)
      throws UnsupportedCodeException {

    checkArguments(
        pOptions,
        pRelevantMemoryLocationIds,
        pPointerAssignments,
        pParameterAssignments,
        pPointerParameterAssignments,
        pMachineModel);

    allMemoryLocations = pAllMemoryLocations;
    relevantMemoryLocationAmount = pRelevantMemoryLocationIds.size();
    relevantMemoryLocations = pRelevantMemoryLocationIds;
    startRoutineArgAssignments = pStartRoutineArgAssignments;
    pointerAssignments = pPointerAssignments;
    parameterAssignments = pParameterAssignments;
    pointerParameterAssignments = pPointerParameterAssignments;
    pointerDereferences = pPointerDereferences;
  }

  private static void checkArguments(
      MPOROptions pOptions,
      ImmutableMap<SeqMemoryLocation, Integer> pRelevantMemoryLocationIds,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pParameterAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments,
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

    // check that all left hand sides in pointer assignments are CPointerType
    for (SeqMemoryLocation memoryLocation : pPointerAssignments.keySet()) {
      if (memoryLocation.fieldMember().isEmpty()) {
        // if there is no field member, then the declaration must be a valid CPointerType
        checkArgument(
            isValidDeclarationPointerType(memoryLocation.declaration().getType()),
            "The CType of the memory locations declaration is not a valid CPointerType: %s",
            memoryLocation.declaration().getType());
      } else {
        CCompositeTypeMemberDeclaration memberDeclaration =
            memoryLocation.fieldMember().orElseThrow();
        // if there is a field member and the field owner is not a valid CPointerType
        // then the field member must be a validCPointerType
        if (!isValidDeclarationPointerType(memoryLocation.declaration().getType())) {
          checkArgument(
              isValidDeclarationPointerType(memberDeclaration.getType()),
              "The CType of the memory locations field member is not a valid CPointerType: %s",
              memberDeclaration.getType());
        }
      }
    }

    // check that pointer assignments contains all pointer parameter assignments (i.e. subset)
    for (var entry : pPointerParameterAssignments.entrySet()) {
      checkArgument(
          pParameterAssignments.containsKey(entry.getKey()),
          "pParameterAssignments must contain all pPointerParameterAssignments");
      checkArgument(
          Objects.equals(pParameterAssignments.get(entry.getKey()), entry.getValue()),
          "pParameterAssignments must contain all pPointerParameterAssignments");
    }
  }

  private static boolean isValidDeclarationPointerType(CType pType) {
    if (pType instanceof CPointerType) {
      return true;
    }
    // CArrayType.getType() corresponds to the CType of the arrays elements
    if (pType instanceof CArrayType arrayType && arrayType.getType() instanceof CPointerType) {
      return true;
    }
    return false;
  }

  // boolean helpers ===============================================================================

  /**
   * Returns true if the pointer in {@code pMemoryLocation} is assigned a pointer {@code ptr}, or
   * the address of a non-pointer {@code &non_ptr}.
   */
  static boolean isLeftHandSideInPointerAssignment(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments) {

    final boolean isLeftHandSide =
        pPointerAssignments.containsKey(pMemoryLocation)
            || pStartRoutineArgAssignments.containsKey(pMemoryLocation)
            || pPointerParameterAssignments.containsKey(pMemoryLocation);
    if (pMemoryLocation.isFieldOwnerPointerType()) {
      return isLeftHandSide
          || isLeftHandSideInPointerAssignment(
              pMemoryLocation.getFieldOwnerMemoryLocation(),
              pPointerAssignments,
              pStartRoutineArgAssignments,
              pPointerParameterAssignments);
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

  static ImmutableSet<SeqMemoryLocation> getPointerAssignmentRightHandSides(
      SeqMemoryLocation pMemoryLocation,
      ImmutableSetMultimap<SeqMemoryLocation, SeqMemoryLocation> pPointerAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pStartRoutineArgAssignments,
      ImmutableMap<SeqMemoryLocation, SeqMemoryLocation> pPointerParameterAssignments) {

    ImmutableSet.Builder<SeqMemoryLocation> rRightHandSides = ImmutableSet.builder();

    if (pPointerAssignments.containsKey(pMemoryLocation)) {
      rRightHandSides.addAll(pPointerAssignments.get(pMemoryLocation));
    }
    if (pStartRoutineArgAssignments.containsKey(pMemoryLocation)) {
      rRightHandSides.add(Objects.requireNonNull(pStartRoutineArgAssignments.get(pMemoryLocation)));
    }
    if (pPointerParameterAssignments.containsKey(pMemoryLocation)) {
      rRightHandSides.add(
          Objects.requireNonNull(pPointerParameterAssignments.get(pMemoryLocation)));
    }
    if (pMemoryLocation.isFieldOwnerPointerType()) {
      rRightHandSides.addAll(
          getPointerAssignmentRightHandSides(
              pMemoryLocation.getFieldOwnerMemoryLocation(),
              pPointerAssignments,
              pStartRoutineArgAssignments,
              pPointerParameterAssignments));
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
