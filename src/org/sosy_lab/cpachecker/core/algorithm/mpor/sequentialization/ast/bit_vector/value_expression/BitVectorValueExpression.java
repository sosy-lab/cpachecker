// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.value_expression;

import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqASTNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector.BitVectorEncoding;

public abstract class BitVectorValueExpression implements SeqASTNode {

  static final String ZERO_BIT = "0";

  static final String ONE_BIT = "1";

  abstract BitVectorEncoding getEncoding();

  /** Whether this bit vector is 0, e.g. {@code 0b00000000} or {@code 0x00} or {@code 0}. */
  abstract boolean isZero();
}
