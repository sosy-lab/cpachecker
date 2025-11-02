// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.java_smt.api.FormulaType;

public sealed interface K3Type extends Type
    permits K3CustomType, K3FunctionType, K3ProcedureType, K3ProductType, K3SmtLibType {

  FormulaType<?> toFormulaType();

  K3ConstantTerm defaultValue();

  static boolean compatibleTypes(K3Type t1, K3Type t2) {
    if (t1.equals(K3CustomType.InternalAnyType) || t2.equals(K3CustomType.InternalAnyType)) {
      return true;
    }
    return t1.equals(t2);
  }

  static Optional<K3Type> getTypeForString(String pType) {
    return Optional.ofNullable(
        switch (pType) {
          case "Int" -> K3SmtLibType.INT;
          case "Bool" -> K3SmtLibType.BOOL;
          case "String" -> K3SmtLibType.STRING;
          case "Real" -> K3SmtLibType.REAL;
          default -> null;
        });
  }
}
