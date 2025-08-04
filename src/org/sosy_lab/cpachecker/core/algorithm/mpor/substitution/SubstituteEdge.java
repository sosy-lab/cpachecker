// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

/** A simple wrapper for substitutes to {@link CFAEdge}s. */
public class SubstituteEdge {

  public final CFAEdge cfaEdge;

  public final ThreadEdge threadEdge;

  public final ImmutableSet<CExpressionAssignmentStatement> pointerAssignments;

  /** The set of global variable declarations that this edge accesses. */
  public final ImmutableSet<CVariableDeclaration> accessedGlobalVariables;

  public final ImmutableSet<CVariableDeclaration> readGlobalVariables;

  public final ImmutableSet<CVariableDeclaration> writtenGlobalVariables;

  public final ImmutableSet<CFunctionDeclaration> accessedFunctionPointers;

  // TODO parameters are a bit trickier due to passed on parameters
  /** The list of parameters pointing to global variable declarations that this edge accesses. */
  // public final ImmutableList<CParameterDeclaration> globalParameterVariables;

  public SubstituteEdge(
      CFAEdge pCfaEdge,
      ThreadEdge pThreadEdge,
      ImmutableSet<CExpressionAssignmentStatement> pPointerAssignments,
      ImmutableSet<CVariableDeclaration> pWrittenGlobalVariables,
      ImmutableSet<CVariableDeclaration> pAccessedGlobalVariables,
      ImmutableSet<CFunctionDeclaration> pAccessedFunctionPointers) {

    checkArgument(
        pCfaEdge.equals(pThreadEdge.cfaEdge), "pCfaEdge and pThreadEdge cfaEdge must match");

    cfaEdge = pCfaEdge;
    threadEdge = pThreadEdge;
    pointerAssignments = pPointerAssignments;
    writtenGlobalVariables = pWrittenGlobalVariables;
    accessedGlobalVariables = pAccessedGlobalVariables;
    readGlobalVariables =
        Sets.symmetricDifference(writtenGlobalVariables, accessedGlobalVariables).immutableCopy();
    accessedFunctionPointers = pAccessedFunctionPointers;
  }

  public ImmutableSet<CVariableDeclaration> getGlobalVariablesByAccessType(
      BitVectorAccessType pAccessType) {
    return switch (pAccessType) {
      case NONE -> ImmutableSet.of();
      case ACCESS -> accessedGlobalVariables;
      case READ -> readGlobalVariables;
      case WRITE -> writtenGlobalVariables;
    };
  }
}
