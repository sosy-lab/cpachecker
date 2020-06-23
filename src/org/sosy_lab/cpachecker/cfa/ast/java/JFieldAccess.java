// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 *   JFieldAccess is no FieldAccess, but a qualified FieldAccess.
 *   Distinction between Fields and Variables are made through
 *   Declarations JVariableDeclaration and JFieldDeclarations
 *   JField Access makes the distinction between non-static
 *   fields with qualifier, and the rest.
 *
 */
public final class JFieldAccess extends JIdExpression {

  //TODO Investigate if this should be refactored.

  private static final long serialVersionUID = 2729676155903102814L;
  private final JIdExpression qualifier;

  public JFieldAccess(FileLocation pFileLocation, JType pType, String pName, JFieldDeclaration pDeclaration, JIdExpression pQualifier) {
    super(pFileLocation, pType, pName, pDeclaration);
    qualifier = pQualifier;
  }

  @Override
  public JFieldDeclaration getDeclaration() {
    return (JFieldDeclaration) super.getDeclaration();
  }

  public JIdExpression getReferencedVariable() {
    return qualifier;
  }

  @Override
  public String toASTString(boolean pQualified) {
    // TODO Change to something simpler.
    // It seems some CPAs depend on this method for
    // getting variable names, investigate and change
    return super.toASTString(pQualified);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(qualifier);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JFieldAccess)
        || super.equals(obj)) {
      return false;
    }

    JFieldAccess other = (JFieldAccess) obj;

    return Objects.equals(other.qualifier, qualifier);
  }

}
