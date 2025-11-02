// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public sealed interface K3ConstantTerm extends K3Term
    permits K3BooleanConstantTerm, K3IntegerConstantTerm {

  Object getValue();

  static K3ConstantTerm of(Object pValue, K3Type pType) {
    if (pType instanceof K3SmtLibType pK3SmtLibType) {
      return switch (pK3SmtLibType) {
        case BOOL -> {
          if (pValue instanceof Boolean b) {
            yield new K3BooleanConstantTerm(b, FileLocation.DUMMY);
          } else {
            throw new IllegalArgumentException(
                "Expected Boolean value for K3SmtLibType.BOOL, but got: " + pValue);
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
                "Expected Integer value for K3SmtLibType.INT, but got: " + pValue);
          }
          yield new K3IntegerConstantTerm(i, FileLocation.DUMMY);
        }
        default ->
            throw new UnsupportedOperationException(
                "Cannot create constant expression for type: " + pK3SmtLibType);
      };
    }

    throw new UnsupportedOperationException("Cannot create constant expression for type: " + pType);
  }
}
