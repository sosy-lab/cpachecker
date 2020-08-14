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

public class PendingExceptionOfJIdExpression extends JIdExpression {

  private static final long serialVersionUID = 7788790137238292607L;

  public PendingExceptionOfJIdExpression(
      FileLocation pFileLocation, JType pType, String pName, JSimpleDeclaration pDeclaration) {
    super(pFileLocation, pType, pName, pDeclaration);
  }

  public static PendingExceptionOfJIdExpression create() {
    final JVariableDeclaration jVariableDeclaration =
        new JVariableDeclaration(
            FileLocation.DUMMY,
            JClassType.createUnresolvableType(),
            "pending_Exception",
            "pending_Exception",
            "pending_Exception",
            null,
            false);
    return new PendingExceptionOfJIdExpression(
        FileLocation.DUMMY,
        jVariableDeclaration.getType(),
        "pending exception",
        jVariableDeclaration);
  }
}
