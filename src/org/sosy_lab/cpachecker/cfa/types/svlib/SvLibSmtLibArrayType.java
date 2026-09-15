// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.svlib;

import java.io.Serial;
import org.sosy_lab.java_smt.api.FormulaType;

public final class SvLibSmtLibArrayType implements SvLibSmtLibType {

  @Serial private static final long serialVersionUID = -6428452921599985756L;
  private final SvLibSmtLibType keysType;
  private final SvLibSmtLibType valuesType;

  public SvLibSmtLibArrayType(SvLibSmtLibType pKeysType, SvLibSmtLibType pValuesType) {
    keysType = pKeysType;
    valuesType = pValuesType;
  }

  public SvLibSmtLibType getKeysType() {
    return keysType;
  }

  public SvLibSmtLibType getValuesType() {
    return valuesType;
  }

  @Override
  public FormulaType<?> toFormulaType() {
    return FormulaType.getArrayType(keysType.toFormulaType(), valuesType.toFormulaType());
  }

  @Override
  public String toASTString(String declarator) {
    return declarator
        + " : (Array "
        + keysType.toASTString("")
        + " "
        + valuesType.toASTString("")
        + ")";
  }

  @Override
  public String toASTString() {
    return "(Array " + keysType.toASTString() + " " + valuesType.toASTString() + ")";
  }

  @Override
  public int hashCode() {
    return keysType.hashCode() * 31 + valuesType.hashCode();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibSmtLibArrayType other
        && keysType.equals(other.keysType)
        && valuesType.equals(other.valuesType);
  }
}
