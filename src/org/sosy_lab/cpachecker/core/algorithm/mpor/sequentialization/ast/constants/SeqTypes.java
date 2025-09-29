// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqParameterDeclaration;

public class SeqTypes {

  public static class SeqArrayType {

    public static final CArrayType UNSIGNED_INT_ARRAY =
        new CArrayType(CTypeQualifiers.NONE, CNumericTypes.UNSIGNED_INT);
  }

  public static class SeqFunctionType {

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
                SeqParameterDeclaration.FILE,
                SeqParameterDeclaration.LINE,
                SeqParameterDeclaration.FUNCTION),
            false);

    public static final CFunctionTypeWithNames ASSERT_FAIL =
        new CFunctionTypeWithNames(
            CVoidType.VOID,
            ImmutableList.of(
                SeqParameterDeclaration.ASSERTION,
                SeqParameterDeclaration.FILE,
                SeqParameterDeclaration.LINE,
                SeqParameterDeclaration.FUNCTION),
            false);

    public static final CFunctionTypeWithNames ASSUME =
        new CFunctionTypeWithNames(
            CVoidType.VOID, ImmutableList.of(SeqParameterDeclaration.COND), false);

    public static final CFunctionType MAIN =
        new CFunctionType(CNumericTypes.INT, ImmutableList.of(), false);
  }
}
