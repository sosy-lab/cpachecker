// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorAccessType;

/** A simple wrapper for substitutes to {@link CFAEdge}s. */
public class SubstituteEdge {

  public final CFAEdge cfaEdge;

  /** The list of global variable declarations that this edge accesses. */
  public final ImmutableSet<CVariableDeclaration> accessedGlobalVariables;

  public final ImmutableSet<CVariableDeclaration> readGlobalVariables;

  public final ImmutableSet<CVariableDeclaration> writtenGlobalVariables;

  // TODO parameters are a bit trickier due to passed on parameters
  /** The list of parameters pointing to global variable declarations that this edge accesses. */
  // public final ImmutableList<CParameterDeclaration> globalParameterVariables;

  public SubstituteEdge(
      CFAEdge pCfaEdge,
      ImmutableSet<CVariableDeclaration> pWrittenGlobalVariables,
      ImmutableSet<CVariableDeclaration> pAccessedGlobalVariables) {

    cfaEdge = pCfaEdge;
    writtenGlobalVariables = pWrittenGlobalVariables;
    accessedGlobalVariables = pAccessedGlobalVariables;
    readGlobalVariables =
        Sets.symmetricDifference(writtenGlobalVariables, accessedGlobalVariables).immutableCopy();
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
