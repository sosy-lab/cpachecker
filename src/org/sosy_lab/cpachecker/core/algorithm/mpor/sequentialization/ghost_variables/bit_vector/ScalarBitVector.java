// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class ScalarBitVector {

  public final ImmutableMap<MPORThread, CIdExpression> variables;

  public final BitVectorAccessType accessType;

  public ScalarBitVector(
      ImmutableMap<MPORThread, CIdExpression> pAccessVariables, BitVectorAccessType pAccessType) {

    checkArgument(!pAccessType.equals(BitVectorAccessType.NONE));
    variables = pAccessVariables;
    accessType = pAccessType;
  }
}
