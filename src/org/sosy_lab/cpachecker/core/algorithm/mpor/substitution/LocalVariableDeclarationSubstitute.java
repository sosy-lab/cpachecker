// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class LocalVariableDeclarationSubstitute {

  // TODO what if multiple declarations have no call context - duplicate key in map?
  /** Not every local variable declaration has a calling context, hence {@link Optional}s. */
  public final ImmutableMap<Optional<ThreadEdge>, CIdExpression> substitutes;

  /** The set of global variables used to initialize this local variable. */
  public final ImmutableSet<CVariableDeclaration> accessedGlobalVariables;

  public final ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
      accessedFieldMembers;

  /** The map of pointer assignments in this local variable declaration. */
  public final ImmutableMap<CVariableDeclaration, CSimpleDeclaration> pointerAssignments;

  public final ImmutableTable<
          CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>
      pointerFieldMemberAssignments;

  /** The set of pointer dereferences used in this local variable declaration. */
  public final ImmutableSet<CSimpleDeclaration> pointerDereferences;

  public LocalVariableDeclarationSubstitute(
      ImmutableMap<Optional<ThreadEdge>, CIdExpression> pSubstitutes,
      ImmutableSet<CVariableDeclaration> pAccessedGlobalVariables,
      ImmutableSetMultimap<CVariableDeclaration, CCompositeTypeMemberDeclaration>
          pAccessedFieldMembers,
      ImmutableMap<CVariableDeclaration, CSimpleDeclaration> pPointerAssignments,
      ImmutableTable<CVariableDeclaration, CSimpleDeclaration, CCompositeTypeMemberDeclaration>
          pPointerFieldMemberAssignments,
      ImmutableSet<CSimpleDeclaration> pPointerDereferences) {

    substitutes = pSubstitutes;
    accessedGlobalVariables = pAccessedGlobalVariables;
    accessedFieldMembers = pAccessedFieldMembers;
    pointerAssignments = pPointerAssignments;
    pointerFieldMemberAssignments = pPointerFieldMemberAssignments;
    pointerDereferences = pPointerDereferences;
  }
}
