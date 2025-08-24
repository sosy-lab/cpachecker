// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class PointerAssignments {

  private final ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pointerAssignments;

  private final ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
      pointerParameterAssignments;

  public PointerAssignments(
      ImmutableSetMultimap<CVariableDeclaration, MemoryLocation> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, MemoryLocation>
          pPointerParameterAssignments) {

    pointerAssignments = pPointerAssignments;
    pointerParameterAssignments = pPointerParameterAssignments;
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
}
