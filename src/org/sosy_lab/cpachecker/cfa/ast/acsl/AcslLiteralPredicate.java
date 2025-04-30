// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class AcslLiteralPredicate extends ALiteralExpression
    implements AcslPredicate permits AcslBooleanLiteralPredicate {

  @Serial private static final long serialVersionUID = -8145502432341276L;

  protected AcslLiteralPredicate(FileLocation pFileLocation) {
    super(pFileLocation, AcslBuiltinLogicType.BOOLEAN);
    checkNotNull(pFileLocation);
  }

  @Override
  public abstract Object getValue();

  @Override
  public int hashCode() {
    return getValue().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof AcslBooleanLiteralPredicate other
        && super.equals(other)
        && other.getValue().equals(getValue());
  }
}
