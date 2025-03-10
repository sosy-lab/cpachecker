// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AbstractAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class CDesignator extends AbstractAstNode implements CAstNode
    permits CArrayDesignator, CArrayRangeDesignator, CFieldDesignator {

  @Serial private static final long serialVersionUID = 6870178640888782994L;

  protected CDesignator(FileLocation pFileLoc) {
    super(pFileLoc);
  }

  public abstract <R, X extends Exception> R accept(CDesignatorVisitor<R, X> pV) throws X;

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CDesignator && super.equals(obj);
  }
}
