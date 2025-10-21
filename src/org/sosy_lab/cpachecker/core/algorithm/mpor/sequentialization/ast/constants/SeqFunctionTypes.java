// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

public class SeqFunctionTypes {

  public static final CFunctionType ABORT =
      new CFunctionType(CVoidType.VOID, ImmutableList.of(), false);

  public static final CFunctionType VERIFIER_NONDET_INT =
      new CFunctionType(CNumericTypes.INT, ImmutableList.of(), false);

  public static final CFunctionType VERIFIER_NONDET_UINT =
      new CFunctionType(CNumericTypes.UNSIGNED_INT, ImmutableList.of(), false);

  public static final CFunctionTypeWithNames REACH_ERROR =
      new CFunctionTypeWithNames(
          CVoidType.VOID,
          ImmutableList.of(
              SeqParameterDeclarations.FILE,
              SeqParameterDeclarations.LINE,
              SeqParameterDeclarations.FUNCTION),
          false);

  public static final CFunctionTypeWithNames ASSERT_FAIL =
      new CFunctionTypeWithNames(
          CVoidType.VOID,
          ImmutableList.of(
              SeqParameterDeclarations.ASSERTION,
              SeqParameterDeclarations.FILE,
              SeqParameterDeclarations.LINE,
              SeqParameterDeclarations.FUNCTION),
          false);

  public static final CFunctionTypeWithNames ASSUME =
      new CFunctionTypeWithNames(
          CVoidType.VOID, ImmutableList.of(SeqParameterDeclarations.COND), false);

  public static final CFunctionType MAIN =
      new CFunctionType(CNumericTypes.INT, ImmutableList.of(), false);

  public static final CFunctionTypeWithNames MALLOC =
      new CFunctionTypeWithNames(
          CPointerType.POINTER_TO_VOID, ImmutableList.of(SeqParameterDeclarations.SIZE), false);
}
