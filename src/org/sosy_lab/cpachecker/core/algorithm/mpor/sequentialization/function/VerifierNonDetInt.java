// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.function;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.SeqExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqDataType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;

public class VerifierNonDetInt implements SeqFunction {

  @Override
  public String getReturnType() {
    return SeqDataType.INT;
  }

  @Override
  public String getName() {
    return SeqToken.VERIFIER_NONDET_INT;
  }

  @Override
  public ImmutableList<SeqExpression> getParameters() {
    return ImmutableList.of();
  }
}
