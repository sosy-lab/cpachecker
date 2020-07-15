// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

public class JIdExpressionOfPendingException extends JIdExpression {
  public JIdExpressionOfPendingException(
      FileLocation pFileLocation, JType pType, String pName, JSimpleDeclaration pDeclaration) {
    super(pFileLocation, pType, pName, pDeclaration);
  }

  public static JIdExpressionOfPendingException create() {
    final JVariableDeclaration jVariableDeclaration =
        new JVariableDeclaration(
            FileLocation.DUMMY,
            JClassType.createUnresolvableType(),
            "dummy_pending_Exception",
            "dummy_pending_Exception",
            "dummy_pending_Exception",
            null,
            false);
    return new JIdExpressionOfPendingException(
        FileLocation.DUMMY,
        JClassType.createUnresolvableType(),
        "pending exception",
        jVariableDeclaration);
  }
}
