// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class PointerAssignments {

  private final ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pointerAssignments;

  private final ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
      pointerParameterAssignments;

  public PointerAssignments(
      ImmutableSetMultimap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableTable<ThreadEdge, CParameterDeclaration, CSimpleDeclaration>
          pPointerParameterAssignments) {

    pointerAssignments = pPointerAssignments;
    pointerParameterAssignments = pPointerParameterAssignments;
  }

  public boolean contains(CVariableDeclaration pVariableDeclaration) {
    return pointerAssignments.containsKey(pVariableDeclaration);
  }

  public boolean contains(ThreadEdge pCallContext, CParameterDeclaration pParameterDeclaration) {
    return pointerParameterAssignments.contains(pCallContext, pParameterDeclaration);
  }

  public ImmutableSet<CSimpleDeclaration> get(CVariableDeclaration pVariableDeclaration) {
    return pointerAssignments.get(pVariableDeclaration);
  }

  public CSimpleDeclaration get(
      ThreadEdge pCallContext, CParameterDeclaration pParameterDeclaration) {

    return pointerParameterAssignments.get(pCallContext, pParameterDeclaration);
  }
}
