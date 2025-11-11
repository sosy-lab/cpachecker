// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public sealed interface SvLibConstantTerm extends SvLibTerm
    permits SvLibBooleanConstantTerm, SvLibIntegerConstantTerm, SvLibRealConstantTerm {

  Object getValue();

  static SvLibConstantTerm of(Object pValue, SvLibType pType) {
    if (pType instanceof SvLibSmtLibType pSvLibSmtLibType) {
      return switch (pSvLibSmtLibType) {
        case BOOL -> {
          if (pValue instanceof Boolean b) {
            yield new SvLibBooleanConstantTerm(b, FileLocation.DUMMY);
          } else {
            throw new IllegalArgumentException(
                "Expected Boolean value for SvLibSmtLibType.BOOL, but got: " + pValue);
          }
        }
        case INT -> {
          BigInteger i;
          if (pValue instanceof Integer val) {
            i = BigInteger.valueOf(val);
          } else if (pValue instanceof BigInteger bigInt) {
            i = bigInt;
          } else {
            throw new IllegalArgumentException(
                "Expected Integer value for SvLibSmtLibType.INT, but got: " + pValue);
          }
          yield new SvLibIntegerConstantTerm(i, FileLocation.DUMMY);
        }
        default ->
            throw new UnsupportedOperationException(
                "Cannot create constant expression for type: " + pSvLibSmtLibType);
      };
    }

    throw new UnsupportedOperationException("Cannot create constant expression for type: " + pType);
  }
}
