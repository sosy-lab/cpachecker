// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.functions.VerifierNondetFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqFunctionDeclarations {

  public static final CFunctionDeclaration ABORT =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.ABORT,
          SeqToken.abort,
          ImmutableList.of(),
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  public static final CFunctionDeclaration VERIFIER_NONDET_INT =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.VERIFIER_NONDET_INT,
          VerifierNondetFunctionType.INT.getName(),
          ImmutableList.of(),
          ImmutableSet.of());

  public static final CFunctionDeclaration VERIFIER_NONDET_UINT =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.VERIFIER_NONDET_UINT,
          VerifierNondetFunctionType.UINT.getName(),
          ImmutableList.of(),
          ImmutableSet.of());

  public static final CFunctionDeclaration REACH_ERROR =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.REACH_ERROR,
          SeqToken.reach_error,
          ImmutableList.of(
              SeqParameterDeclarations.FILE,
              SeqParameterDeclarations.LINE,
              SeqParameterDeclarations.FUNCTION),
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  public static final CFunctionDeclaration ASSERT_FAIL =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.ASSERT_FAIL,
          SeqToken.ASSERT_FAIL_KEYWORD,
          ImmutableList.of(
              SeqParameterDeclarations.ASSERTION,
              SeqParameterDeclarations.FILE,
              SeqParameterDeclarations.LINE,
              SeqParameterDeclarations.FUNCTION),
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  public static final CFunctionDeclaration ASSUME =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.ASSUME,
          SeqNameUtil.buildFunctionName(SeqToken.assume),
          ImmutableList.of(SeqParameterDeclarations.COND),
          ImmutableSet.of());

  public static final CFunctionDeclaration MAIN =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.MAIN,
          SeqToken.main,
          ImmutableList.of(),
          ImmutableSet.of());

  public static final CFunctionDeclaration MALLOC =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          SeqFunctionTypes.MALLOC,
          SeqToken.malloc,
          ImmutableList.of(SeqParameterDeclarations.SIZE),
          ImmutableSet.of());
}
