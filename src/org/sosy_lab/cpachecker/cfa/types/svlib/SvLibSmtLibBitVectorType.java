// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.svlib;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serial;
import org.sosy_lab.java_smt.api.FormulaType;

public final class SvLibSmtLibBitVectorType implements SvLibSmtLibType {

  @Serial private static final long serialVersionUID = 5949937597523924743L;
  private final int size;

  public SvLibSmtLibBitVectorType(int pSize) {
    checkArgument(pSize > 0, "Size must be > 0");
    size = pSize;
  }

  public int getSize() {
    return size;
  }

  @Override
  public FormulaType<?> toFormulaType() {
    return FormulaType.getBitvectorTypeWithSize(size);
  }

  @Override
  public String toASTString() {
    return "(_ BitVec " + size + ")";
  }

  @Override
  public String toASTString(String declarator) {
    return declarator + " : (_ BitVec " + size + ")";
  }

  @Override
  public int hashCode() {
    return size;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibSmtLibBitVectorType other && size == other.size;
  }
}
