// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

public class SeqFunctionCallExpressions {

  public static final CFunctionCallExpression VERIFIER_NONDET_INT =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CNumericTypes.INT,
          SeqIdExpressions.VERIFIER_NONDET_INT,
          ImmutableList.of(),
          SeqFunctionDeclarations.VERIFIER_NONDET_INT);

  public static final CFunctionCallExpression VERIFIER_NONDET_UINT =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CNumericTypes.UNSIGNED_INT,
          SeqIdExpressions.VERIFIER_NONDET_UINT,
          ImmutableList.of(),
          SeqFunctionDeclarations.VERIFIER_NONDET_UINT);
}
