// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.java_smt.api.FormulaType;

public sealed interface SvLibType extends Type
    permits SvLibCustomType,
        SvLibFunctionType,
        SvLibProcedureType,
        SvLibProductType,
        SvLibSmtLibType {

  FormulaType<?> toFormulaType();

  SvLibConstantTerm defaultValue();

  static boolean canBeCastTo(SvLibType t1, SvLibType t2) {
    if (t1.equals(SvLibCustomType.InternalAnyType) || t2.equals(SvLibCustomType.InternalAnyType)) {
      return true;
    }
    // TODO: This may need to be refactored into a more general subtyping mechanism
    //        At least MathSat can cast Int into Real implicitly
    if (t1.equals(SvLibSmtLibType.REAL) && t2.equals(SvLibSmtLibType.INT)) {
      return true;
    }

    return t1.equals(t2);
  }

  static Optional<SvLibType> getTypeForString(String pType) {
    return Optional.ofNullable(
        switch (pType) {
          case "Int" -> SvLibSmtLibType.INT;
          case "Bool" -> SvLibSmtLibType.BOOL;
          case "String" -> SvLibSmtLibType.STRING;
          case "Real" -> SvLibSmtLibType.REAL;
          default -> null;
        });
  }
}
