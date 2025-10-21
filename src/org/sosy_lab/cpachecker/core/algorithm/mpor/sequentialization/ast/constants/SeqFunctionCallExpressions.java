// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;

public class SeqFunctionCallExpressions {

  public static final CFunctionCallExpression ABORT =
      SeqExpressionBuilder.buildFunctionCallExpression(
          CVoidType.VOID,
          SeqIdExpressions.ABORT,
          ImmutableList.of(),
          SeqFunctionDeclarations.ABORT);

  public static final CFunctionCallExpression VERIFIER_NONDET_INT =
      SeqExpressionBuilder.buildFunctionCallExpression(
          CNumericTypes.INT,
          SeqIdExpressions.VERIFIER_NONDET_INT,
          ImmutableList.of(),
          SeqFunctionDeclarations.VERIFIER_NONDET_INT);

  public static final CFunctionCallExpression VERIFIER_NONDET_UINT =
      SeqExpressionBuilder.buildFunctionCallExpression(
          CNumericTypes.UNSIGNED_INT,
          SeqIdExpressions.VERIFIER_NONDET_UINT,
          ImmutableList.of(),
          SeqFunctionDeclarations.VERIFIER_NONDET_UINT);
}
