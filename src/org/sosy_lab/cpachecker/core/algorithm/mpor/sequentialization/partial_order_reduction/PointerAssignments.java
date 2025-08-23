// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class PointerAssignments {

  private final ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pointerAssignments;

  private final ImmutableTable<
          CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      pointerFieldMemberAssignments;

  private final ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
      pointerParameterAssignments;

  public PointerAssignments(
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableTable<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>
          pPointerFieldMemberAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments) {

    pointerAssignments = pPointerAssignments;
    pointerFieldMemberAssignments = pPointerFieldMemberAssignments;
    pointerParameterAssignments = pPointerParameterAssignments;
  }

  public boolean isAssignedPointer(CVariableDeclaration pVariableDeclaration) {
    return pointerAssignments.containsKey(pVariableDeclaration);
  }

  public boolean isAssignedPointerFieldMember(CVariableDeclaration pVariableDeclaration) {
    return pointerFieldMemberAssignments.containsColumn(pVariableDeclaration);
  }

  public boolean isAssignedPointerParameter(
      ThreadEdge pCallContext, CParameterDeclaration pParameterDeclaration) {
    return pointerParameterAssignments.contains(pCallContext, pParameterDeclaration);
  }

  public ImmutableSet<CSimpleDeclaration> getRightHandSidesByPointer(
      CVariableDeclaration pVariableDeclaration) {

    return pointerAssignments.get(pVariableDeclaration);
  }

  public ImmutableMap<CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      getRightHandSidesByPointerFieldMember(CVariableDeclaration pVariableDeclaration) {

    return pointerFieldMemberAssignments.row(pVariableDeclaration);
  }

  public CSimpleDeclaration getRightHandSideByPointerParameter(
      ThreadEdge pCallContext, CParameterDeclaration pParameterDeclaration) {

    return pointerParameterAssignments.get(pCallContext, pParameterDeclaration);
  }
}
