// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorVariables {
  public final ImmutableMap<MPORThread, CIdExpression> bitVectors;

  public final ImmutableList<ScalarBitVectorVariable> scalarBitVectors;

  public BitVectorVariables(
      ImmutableMap<MPORThread, CIdExpression> pBitVectors,
      ImmutableList<ScalarBitVectorVariable> pScalarBitVectors) {

    bitVectors = pBitVectors;
    scalarBitVectors = pScalarBitVectors;
  }

  public CIdExpression get(MPORThread pThread) {
    return bitVectors.get(pThread);
  }
}
