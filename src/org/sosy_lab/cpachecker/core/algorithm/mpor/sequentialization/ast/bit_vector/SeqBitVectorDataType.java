// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.bit_vector;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;

public enum SeqBitVectorDataType {
  UINT8_T(8, CNumericTypes.UNSIGNED_CHAR),
  UINT16_T(16, CNumericTypes.UNSIGNED_SHORT_INT),
  UINT32_T(32, CNumericTypes.UNSIGNED_INT),
  UINT64_T(64, CNumericTypes.UNSIGNED_LONG_INT);

  public final int size;

  public final CSimpleType simpleType;

  SeqBitVectorDataType(int pSize, CSimpleType pSimpleType) {
    size = pSize;
    simpleType = pSimpleType;
  }

  public String toASTString() {
    return "uint" + size + "_t_sequentialized";
  }

  public CTypeDeclaration buildDeclaration() {
    return new CTypeDefDeclaration(
        FileLocation.DUMMY, true, simpleType, toASTString(), toASTString());
  }

  public static SeqBitVectorDataType getTypeByBinaryLength(int pBinaryLength) {
    checkArgument(pBinaryLength >= 0, "pBinaryLength cannot be negative, got %s", pBinaryLength);

    if (pBinaryLength <= SeqBitVectorDataType.UINT8_T.size) {
      return SeqBitVectorDataType.UINT8_T;
    }
    if (pBinaryLength <= SeqBitVectorDataType.UINT16_T.size) {
      return SeqBitVectorDataType.UINT16_T;
    }
    if (pBinaryLength <= SeqBitVectorDataType.UINT32_T.size) {
      return SeqBitVectorDataType.UINT32_T;
    }
    if (pBinaryLength <= SeqBitVectorDataType.UINT64_T.size) {
      return SeqBitVectorDataType.UINT64_T;
    }

    throw new IllegalArgumentException(
        String.format(
            "pBinaryLength %s is too long, maximum is %s",
            pBinaryLength, SeqBitVectorUtil.MAX_BINARY_LENGTH));
  }
}
