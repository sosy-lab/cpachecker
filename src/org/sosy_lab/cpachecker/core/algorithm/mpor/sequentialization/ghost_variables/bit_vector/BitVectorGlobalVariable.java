// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorGlobalVariable {

  private final int id;

  private final CVariableDeclaration declaration;

  public final Optional<ImmutableMap<MPORThread, CIdExpression>> accessVariables;

  public BitVectorGlobalVariable(
      int pId,
      CVariableDeclaration pDeclaration,
      Optional<ImmutableMap<MPORThread, CIdExpression>> pAccessVariable) {

    id = pId;
    declaration = pDeclaration;
    accessVariables = pAccessVariable;
  }

  public int getId() {
    return id;
  }

  public CVariableDeclaration getDeclaration() {
    return declaration;
  }
}
