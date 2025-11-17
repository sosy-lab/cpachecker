// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.io.Serial;
import java.util.Objects;
import org.sosy_lab.java_smt.api.FormulaType;

public final class SvLibCustomType implements SvLibType {

  public static final SvLibCustomType InternalAnyType = new SvLibCustomType("#any", -1);

  @Serial private static final long serialVersionUID = -1560683119379278009L;
  private final String type;
  private final int arity;

  public SvLibCustomType(String pType, int pArity) {
    type = pType;
    arity = pArity;
  }

  public String getType() {
    return type;
  }

  public int getArity() {
    return arity;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }

    return pO instanceof SvLibCustomType other
        && Objects.equals(type, other.type)
        && arity == other.arity;
  }

  @Override
  public FormulaType<?> toFormulaType() {
    throw new UnsupportedOperationException("JavaSMT does not support custom types");
  }

  public String toPlainString() {
    return type + (arity >= 0 ? "<" + arity + ">" : "");
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(type);
  }

  @Override
  public String toASTString(String declarator) {
    return type + " " + declarator;
  }
}
