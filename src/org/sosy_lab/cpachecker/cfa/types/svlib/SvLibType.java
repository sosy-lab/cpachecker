// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.svlib;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.types.Type;

// TODO: This needs to be refactored into actual SMT-LIB types, and those which
//    can only appear in SV-LIB
public sealed interface SvLibType extends Type
    permits SvLibFunctionType, SvLibProcedureType, SvLibProductType, SvLibSmtLibType {

  static boolean canBeCastTo(SvLibType t1, SvLibType t2) {
    if (t1 instanceof SvLibAnyType || t2 instanceof SvLibAnyType) {
      return true;
    }
    // TODO: This may need to be refactored into a more general subtyping mechanism
    //        At least MathSat can cast Int into Real implicitly
    if (t1.equals(SvLibSmtLibPredefinedType.REAL) && t2.equals(SvLibSmtLibPredefinedType.INT)) {
      return true;
    }

    if (t1 instanceof SvLibSmtLibArrayType arr1 && t2 instanceof SvLibSmtLibArrayType arr2) {
      return canBeCastTo(arr1.getKeysType(), arr2.getKeysType())
          && canBeCastTo(arr1.getValuesType(), arr2.getValuesType());
    }

    return t1.equals(t2);
  }

  /**
   * This method returns a string representation of the type suitable for AST output.
   *
   * @return AST string representation of the type
   */
  String toASTString();

  static Optional<SvLibType> fromString(String pType) {
    return Optional.ofNullable(
        switch (pType) {
          case "Int" -> SvLibSmtLibPredefinedType.INT;
          case "Bool" -> SvLibSmtLibPredefinedType.BOOL;
          case "String" -> SvLibSmtLibPredefinedType.STRING;
          case "Real" -> SvLibSmtLibPredefinedType.REAL;
          default -> null;
        });
  }
}
