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
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqDeclarations.SeqParameterDeclaration;

public class SeqTypes {

  public static class SeqSimpleType {

    public static final CSimpleType INT =
        new CSimpleType(
            false, false, CBasicType.INT, false, false, false, false, false, false, false);

    public static final CSimpleType CONST_INT =
        new CSimpleType(
            true, false, CBasicType.INT, false, false, false, false, false, false, false);

    public static final CSimpleType UNSIGNED_INT =
        new CSimpleType(
            false, false, CBasicType.INT, false, false, false, true, false, false, false);

    public static final CSimpleType CONST_CHAR =
        new CSimpleType(
            true, false, CBasicType.CHAR, false, false, false, false, false, false, false);
  }

  public static class SeqArrayType {

    public static final CArrayType INT_ARRAY = new CArrayType(false, false, SeqSimpleType.INT);
  }

  public static class SeqPointerType {

    /** A constant pointer to a constant int value (const int * const). */
    public static final CPointerType POINTER_CONST_CHAR =
        new CPointerType(false, false, SeqSimpleType.CONST_CHAR);
  }

  public static class SeqVoidType {

    public static final CVoidType VOID = CVoidType.VOID;
  }

  public static class SeqFunctionType {

    public static final CFunctionType ABORT =
        new CFunctionType(SeqVoidType.VOID, ImmutableList.of(), false);

    public static final CFunctionType VERIFIER_NONDET_INT =
        new CFunctionType(SeqSimpleType.INT, ImmutableList.of(), false);

    public static final CFunctionType VERIFIER_NONDET_UINT =
        new CFunctionType(SeqSimpleType.UNSIGNED_INT, ImmutableList.of(), false);

    public static final CFunctionTypeWithNames REACH_ERROR =
        new CFunctionTypeWithNames(
            SeqVoidType.VOID,
            ImmutableList.of(
                SeqParameterDeclaration.FILE,
                SeqParameterDeclaration.LINE,
                SeqParameterDeclaration.FUNCTION),
            false);

    public static final CFunctionTypeWithNames ASSERT_FAIL =
        new CFunctionTypeWithNames(
            SeqVoidType.VOID,
            ImmutableList.of(
                SeqParameterDeclaration.ASSERTION,
                SeqParameterDeclaration.FILE,
                SeqParameterDeclaration.LINE,
                SeqParameterDeclaration.FUNCTION),
            false);

    public static final CFunctionTypeWithNames ASSUME =
        new CFunctionTypeWithNames(
            SeqVoidType.VOID, ImmutableList.of(SeqParameterDeclaration.COND), false);

    public static final CFunctionType MAIN =
        new CFunctionType(SeqSimpleType.INT, ImmutableList.of(), false);
  }
}
