// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class CFieldDesignator extends CDesignator {

  private static final long serialVersionUID = -1418942274162299596L;
  private final String name;

  public CFieldDesignator(final FileLocation pFileLocation, final String pName) {
    super(pFileLocation);
    name = pName;
  }

  public String getFieldName() {
    return name;
  }

  @Override
  public String toASTString(boolean pQualified) {
    return toASTString();
  }

  @Override
  public String toASTString() {
    return "." + name;
  }

  @Override
  public String toParenthesizedASTString(boolean pQualified) {
    return toASTString(pQualified);
  }

  @Override
  public <R, X extends Exception> R accept(CDesignatorVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(name);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CFieldDesignator) || !super.equals(obj)) {
      return false;
    }

    CFieldDesignator other = (CFieldDesignator) obj;

    return Objects.equals(other.name, name);
  }
}
