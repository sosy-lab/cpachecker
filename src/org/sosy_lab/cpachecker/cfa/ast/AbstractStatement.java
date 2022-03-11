// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

public abstract class AbstractStatement extends AbstractAstNode implements AStatement {

  private static final long serialVersionUID = -596325723181327583L;

  protected AbstractStatement(FileLocation pFileLocation) {
    super(pFileLocation);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof AbstractStatement)) {
      return false;
    }

    return super.equals(obj);
  }
}
