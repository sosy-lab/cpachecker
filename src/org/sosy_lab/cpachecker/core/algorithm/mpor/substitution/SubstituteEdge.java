// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/** A simple wrapper for substitutes to {@link CFAEdge}s. */
public class SubstituteEdge {

  public final CFAEdge cfaEdge;

  /** The set of global variable declarations that this edge accesses. */
  private final ImmutableSet<CVariableDeclaration> globalVariables;

  // TODO parameters are a bit trickier due to passed on parameters
  /** The set of parameters pointing to global variable declarations that this edge accesses. */
  // public final ImmutableSet<CParameterDeclaration> globalParameterVariables;

  public SubstituteEdge(CFAEdge pCfaEdge, ImmutableSet<CVariableDeclaration> pGlobalVariables) {
    cfaEdge = pCfaEdge;
    globalVariables = pGlobalVariables;
  }

  public ImmutableSet<CVariableDeclaration> getGlobalVariables() {
    return globalVariables;
  }
}
