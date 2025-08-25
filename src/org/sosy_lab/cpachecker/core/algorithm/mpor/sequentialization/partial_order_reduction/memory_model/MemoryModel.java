// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.memory_model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

/**
 * A class to keep track of all memory locations in the concurrent input program, including pointers
 * associated with a memory location.
 */
public class MemoryModel {

  private final int memoryLocationAmount;

  private final ImmutableMap<MemoryLocation, Integer> memoryLocationIds;

  private final ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments;

  private final ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
      pointerParameterAssignments;

  MemoryModel(
      ImmutableMap<MemoryLocation, Integer> pMemoryLocationIds,
      ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
          pPointerParameterAssignments) {

    checkArguments(pPointerAssignments, pPointerParameterAssignments);
    memoryLocationAmount = pMemoryLocationIds.size();
    memoryLocationIds = pMemoryLocationIds;
    pointerAssignments = pPointerAssignments;
    pointerParameterAssignments = pPointerParameterAssignments;
  }

  private static void checkArguments(
      ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
          pPointerParameterAssignments) {

    for (CVariableDeclaration variableDeclaration : pPointerAssignments.keySet()) {
      checkArgument(
          variableDeclaration.getType() instanceof CPointerType,
          "variableDeclaration must be CPointerType, got %s",
          variableDeclaration.getType());
    }
    for (CParameterDeclaration parameterDeclaration : pPointerParameterAssignments.columnKeySet()) {
      checkArgument(
          parameterDeclaration.getType() instanceof CPointerType,
          "parameterDeclaration must be CPointerType, got %s",
          parameterDeclaration.getType());
    }
  }

  /**
   * Returns true if the pointer in {@code pVariableDeclaration} is assigned a pointer {@code ptr},
   * or the address of a non-pointer {@code &non_ptr}.
   */
  public boolean isAssignedPointer(CVariableDeclaration pVariableDeclaration) {
    checkArgument(
        pVariableDeclaration.getType() instanceof CPointerType,
        "pVariableDeclaration must be CPointerType, got %s",
        pVariableDeclaration.getType());
    return pointerAssignments.containsKey(pVariableDeclaration);
  }

  /**
   * Returns true if the pointer in {@code pParameterDeclaration} is assigned a pointer {@code ptr},
   * or the address of a non-pointer {@code &non_ptr} in any function call.
   */
  public boolean isAssignedPointerParameter(
      ThreadEdge pCallContext, CParameterDeclaration pParameterDeclaration) {

    checkArgument(
        pParameterDeclaration.getType() instanceof CPointerType,
        "pParameterDeclaration must be CPointerType, got %s",
        pParameterDeclaration.getType());
    return pointerParameterAssignments.contains(pCallContext, pParameterDeclaration);
  }

  public ImmutableSet<MemoryLocation> getRightHandSidesByPointer(
      CVariableDeclaration pVariableDeclaration) {

    return pointerAssignments.get(pVariableDeclaration);
  }

  public MemoryLocation getRightHandSideByPointerParameter(
      ThreadEdge pCallContext, CParameterDeclaration pParameterDeclaration) {

    return pointerParameterAssignments.get(pCallContext, pParameterDeclaration);
  }

  public int getMemoryLocationAmount() {
    return memoryLocationAmount;
  }

  public ImmutableMap<MemoryLocation, Integer> getAllMemoryLocationIds() {
    return memoryLocationIds;
  }

  public int getMemoryLocationId(MemoryLocation pMemoryLocation) {
    checkArgument(
        memoryLocationIds.containsKey(pMemoryLocation),
        "could not find pMemoryLocation in memoryLocationIds");
    return Objects.requireNonNull(memoryLocationIds.get(pMemoryLocation));
  }
}
