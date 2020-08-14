// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class PendingExceptionOfJRunTimeType extends JVariableRunTimeType {

  private static final long serialVersionUID = 8533391047865289737L;

  public PendingExceptionOfJRunTimeType() {
    super(FileLocation.DUMMY, PendingExceptionOfJIdExpression.create());
  }
}
