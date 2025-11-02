// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.java_smt.api.FormulaType;

public final class K3ProductType implements K3Type {

  @Serial private static final long serialVersionUID = 265032494258079236L;
  private final List<K3Type> elementTypes;

  public K3ProductType(List<K3Type> pElementTypes) {
    elementTypes = pElementTypes;
  }

  @Override
  public FormulaType<?> toFormulaType() {
    throw new UnsupportedOperationException("JavaSMT does not support custom types");
  }

  @Override
  public String toASTString(String declarator) {
    return declarator
        + " ["
        + String.join(", ", elementTypes.stream().map(K3Type::toString).toList())
        + "]";
  }

  public List<K3Type> getElementTypes() {
    return elementTypes;
  }

  @Override
  public K3ConstantTerm defaultValue() {
    throw new UnsupportedOperationException("Product types do not have a default value");
  }
}
