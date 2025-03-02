// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.io.Serial;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class and its subclasses represent locale Variable declarations or Field declarations.
 *
 * <p>e.g. Type a = b;
 */
public sealed class JVariableDeclaration extends AVariableDeclaration implements JDeclaration
    permits JFieldDeclaration {

  // TODO refactor to be either abstract or final

  @Serial private static final long serialVersionUID = -3840765628515703031L;
  private static final boolean IS_LOCAL = false;
  private final boolean isFinal;

  protected JVariableDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      JType pType,
      String pName,
      String pOrigName,
      String pQualifiedName,
      AInitializer pInitializer,
      boolean pIsFinal) {
    super(pFileLocation, pIsGlobal, pType, pName, pOrigName, pQualifiedName, pInitializer);

    isFinal = pIsFinal;
  }

  public JVariableDeclaration(
      FileLocation pFileLocation,
      JType pType,
      String pName,
      String pOrigName,
      String pQualifiedName,
      AInitializer pInitializer,
      boolean pIsFinal) {
    super(pFileLocation, IS_LOCAL, pType, pName, pOrigName, pQualifiedName, pInitializer);

    isFinal = pIsFinal;
  }

  @Override
  public JType getType() {
    return (JType) super.getType();
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    StringBuilder lASTString = new StringBuilder();

    if (isFinal) {
      lASTString.append("final ");
    }
    lASTString.append(
        switch (pAAstNodeRepresentation) {
          case DEFAULT -> getType().toASTString(getName());
          case QUALIFIED -> getType().toASTString(getQualifiedName().replace("::", "__"));
          case ORIGINAL_NAMES -> getType().toASTString(getOrigName());
        });

    if (getInitializer() != null) {
      lASTString.append(" = ");
      lASTString.append(getInitializer().toASTString(pAAstNodeRepresentation));
    }

    lASTString.append(";");
    return lASTString.toString();
  }

  public boolean isFinal() {
    return isFinal;
  }

  @Override
  public <R, X extends Exception> R accept(JAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (isFinal ? 1231 : 1237);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof JVariableDeclaration other
        && super.equals(obj)
        && other.isFinal == isFinal;
  }
}
