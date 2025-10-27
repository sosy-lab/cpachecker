// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Ascii;

public enum K3SmtLibType implements K3Type {
  INT,
  BOOL,
  STRING,
  REAL;

  @Override
  public String toASTString(String declarator) {
    return declarator + " : " + Ascii.toLowerCase(name());
  }
}
