// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.bit_vector;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class ScalarBitVectorExpression implements BitVectorExpression {

  public final boolean value;

  public ScalarBitVectorExpression(boolean pValue) {
    value = pValue;
  }

  @Override
  public String toASTString() {
    return value ? SeqToken._1 : SeqToken._0;
  }

  @Override
  public BitVectorEncoding getEncoding() {
    return BitVectorEncoding.SCALAR;
  }

  @Override
  public boolean isZero() {
    return !value;
  }
}
