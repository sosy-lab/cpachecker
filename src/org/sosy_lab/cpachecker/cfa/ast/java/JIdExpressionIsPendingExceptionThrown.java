// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;

public class JIdExpressionIsPendingExceptionThrown extends JIdExpression {
  public JIdExpressionIsPendingExceptionThrown() {
    super(FileLocation.DUMMY, JSimpleType.getBoolean(), "isPendingExceptionThrown", null);
  }
}
