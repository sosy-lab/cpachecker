// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

public class SeqTypes {

  // CSimpleTypes ================================================================================

  public static final CSimpleType INT =
      new CSimpleType(
          false, false, CBasicType.INT, false, false, false, false, false, false, false);

  public static final CSimpleType CONST_INT =
      new CSimpleType(true, false, CBasicType.INT, false, false, false, false, false, false, false);

  // CArrayTypes ================================================================================

  public static final CArrayType INT_ARRAY = new CArrayType(false, false, INT);

  // CPointerTypes ===============================================================================

  /** A constant pointer to a constant int value (const int * const). */
  public static final CPointerType CONST_POINTER_CONST_INT =
      new CPointerType(true, false, CONST_INT);

  // CVoidTypes ==================================================================================

  public static final CVoidType VOID = CVoidType.VOID;

  // CFunctionTypes ==============================================================================

  public static final CFunctionType ABORT = new CFunctionType(VOID, ImmutableList.of(), false);

  public static final CFunctionType VERIFIER_NONDET_INT =
      new CFunctionType(INT, ImmutableList.of(), false);

  public static final CFunctionTypeWithNames ASSUME =
      new CFunctionTypeWithNames(VOID, ImmutableList.of(SeqDeclarations.COND), false);

  public static final CFunctionType MAIN = new CFunctionType(INT, ImmutableList.of(), false);
}
