// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public enum BitVectorDataType {
  __UINT8_T(8, SeqSimpleType.UNSIGNED_CHAR),
  __UINT16_T(16, SeqSimpleType.UNSIGNED_SHORT_INT),
  __UINT32_T(32, SeqSimpleType.UNSIGNED_INT),
  __UINT64_T(64, SeqSimpleType.UNSIGNED_LONG_INT);

  public final int size;
  public final CSimpleType simpleType;

  BitVectorDataType(int pSize, CSimpleType pSimpleType) {
    size = pSize;
    simpleType = pSimpleType;
  }

  public String toASTString() {
    return SeqToken.__MPOR_SEQ__ + SeqToken.uint + size + SeqToken._t;
  }

  public CTypeDeclaration buildDeclaration() {
    return new CTypeDefDeclaration(
        FileLocation.DUMMY, true, simpleType, toASTString(), toASTString());
  }
}
