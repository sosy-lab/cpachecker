// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class contains Parameter Declarations for methods.
 * It contains a type and a name.
 */
public final class JParameterDeclaration extends AParameterDeclaration
    implements JSimpleDeclaration {

  private static final long serialVersionUID = -5728272567926253312L;
  private final String qualifiedName;
  private final boolean isFinal;

  public JParameterDeclaration(FileLocation pFileLocation, JType pType,
      String pName, String pQualifiedName, boolean pIsFinal) {
    super(pFileLocation, pType, pName);
    qualifiedName = checkNotNull(pQualifiedName);
    isFinal = pIsFinal;
  }

  @Override
  public JType getType() {
    return (JType) super.getType();
  }

  public boolean isFinal() {
    return isFinal;
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
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
    result = prime * result + qualifiedName.hashCode();
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }

    if (!(obj instanceof JParameterDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    JParameterDeclaration other = (JParameterDeclaration) obj;

    return other.isFinal == isFinal
        && qualifiedName.equals(other.qualifiedName);
  }

}
