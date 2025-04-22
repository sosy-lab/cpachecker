// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;

public abstract sealed class AcslLiteralExpression extends ALiteralExpression
    implements AcslExpression permits AcslBooleanLiteralExpression {

  @Serial private static final long serialVersionUID = -8145502432341276L;

  protected AcslLiteralExpression(FileLocation pFileLocation, Type pType) {
    super(pFileLocation, pType);
  }

  @Override
  public abstract Object getValue();

  @Override
  public int hashCode() {
    return getValue().hashCode();
  }
}
