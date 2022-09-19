// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import org.sosy_lab.cpachecker.cfa.types.Type;

/**
 * This interface represents all sorts of top-level declarations (i.e., declarations not nested
 * inside another type declaration). This excludes for examples function parameter declarations and
 * struct members. It includes local and global variables and types, as well as functions. This
 * class is only SuperClass of all abstract Classes and their Subclasses. The Interface {@link
 * ADeclaration} contains all language specific AST Nodes as well.
 */
public abstract class AbstractDeclaration extends AbstractSimpleDeclaration
    implements ADeclaration {

  private static final long serialVersionUID = 3218969369130423033L;
  private final boolean isGlobal;

  protected AbstractDeclaration(
      FileLocation pFileLocation, boolean pIsGlobal, Type pType, String pName) {
    super(pFileLocation, pType, pName, pName);
    isGlobal = pIsGlobal;
  }

  protected AbstractDeclaration(
      FileLocation pFileLocation, boolean pIsGlobal, Type pType, String pName, String pOrigName) {
    super(pFileLocation, pType, pName, pOrigName);
    isGlobal = pIsGlobal;
  }

  @Override
  public boolean isGlobal() {
    return isGlobal;
  }

  @Override
  public int hashCode() {
    return (isGlobal ? 1231 : 1237) + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AbstractDeclaration) || !super.equals(obj)) {
      return false;
    }

    AbstractDeclaration other = (AbstractDeclaration) obj;

    return other.isGlobal == isGlobal;
  }
}
