// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;

public class SeqLeftHandSideBuilder {

  public static ImmutableList<CLeftHandSide> buildPcLeftHandSides(
      int pNumThreads, boolean pScalarPc) {

    ImmutableList.Builder<CLeftHandSide> rPcExpressions = ImmutableList.builder();
    rPcExpressions.addAll(
        pScalarPc
            ? SeqExpressionBuilder.buildScalarPcExpressions(pNumThreads)
            : SeqExpressionBuilder.buildArrayPcExpressions(pNumThreads));
    return rPcExpressions.build();
  }
}
