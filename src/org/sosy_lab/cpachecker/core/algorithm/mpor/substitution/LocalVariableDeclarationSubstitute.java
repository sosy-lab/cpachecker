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
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;

public class LocalVariableDeclarationSubstitute {

  // TODO what if multiple declarations have no call context - duplicate key in map?
  /** Not every local variable declaration has a calling context, hence {@link Optional}s. */
  public final ImmutableMap<Optional<ThreadEdge>, CIdExpression> substitutes;

  /** The set of global variables used to initialize this local variable. */
  public final ImmutableSet<CVariableDeclaration> accessedGlobalVariables;

  public LocalVariableDeclarationSubstitute(
      ImmutableMap<Optional<ThreadEdge>, CIdExpression> pSubstitutes,
      ImmutableSet<CVariableDeclaration> pAccessedGlobalVariables) {

    substitutes = pSubstitutes;
    accessedGlobalVariables = pAccessedGlobalVariables;
  }
}
