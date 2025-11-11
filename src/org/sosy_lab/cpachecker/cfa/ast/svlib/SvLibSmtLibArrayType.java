// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import org.sosy_lab.java_smt.api.FormulaType;

public final class SvLibSmtLibArrayType implements SvLibSmtLibType {

  private final SvLibType keysType;
  private final SvLibType valuesType;

  public SvLibSmtLibArrayType(SvLibType pKeysType, SvLibType pValuesType) {
    keysType = pKeysType;
    valuesType = pValuesType;
  }

  public SvLibType getKeysType() {
    return keysType;
  }

  public SvLibType getValuesType() {
    return valuesType;
  }

  @Override
  public FormulaType<?> toFormulaType() {
    return FormulaType.getArrayType(keysType.toFormulaType(), valuesType.toFormulaType());
  }

  @Override
  public SvLibConstantTerm defaultValue() {
    // TODO: This is not a problem, since we now no longer need the default
    //  value function, due to changes in the format which no longer
    //  require all variables to have an explicit value in CEXs.
    throw new UnsupportedOperationException("Arrays do not have a default value");
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
