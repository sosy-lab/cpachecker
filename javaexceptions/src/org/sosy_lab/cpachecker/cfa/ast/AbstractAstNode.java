// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractAstNode implements AAstNode {

  private static final long serialVersionUID = -696796854111906290L;
  private final FileLocation fileLocation;

  protected AbstractAstNode(final FileLocation pFileLocation) {
    fileLocation = checkNotNull(pFileLocation);
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toParenthesizedASTString(boolean pQualified) {
    return "(" + toASTString(pQualified) + ")";
  }

  @Override
  public String toString() {
    return toASTString();
  }

  @Override
  public int hashCode() {
    return 2857;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof AbstractAstNode) {
      return true;
    }

    return false;
  }
}
