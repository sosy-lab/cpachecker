/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class and its subclasses represent locale Variable declarations or Field declarations.
 *
 * e.g. Type a = b;
 */
public class JVariableDeclaration extends AVariableDeclaration implements JDeclaration {

  // TODO refactor to be either abstract or final

  private static final boolean IS_LOCAL = false;
  private final boolean isFinal;

  protected JVariableDeclaration(FileLocation pFileLocation, boolean pIsGlobal, JType pType, String pName,
      String pOrigName, String pQualifiedName, AInitializer pInitializer, boolean pIsFinal) {
    super(pFileLocation, pIsGlobal, pType, pName, pOrigName, pQualifiedName, pInitializer);

    isFinal = pIsFinal;
  }

  public JVariableDeclaration(FileLocation pFileLocation,  JType pType, String pName,
      String pOrigName, String pQualifiedName, AInitializer pInitializer, boolean pIsFinal) {
    super(pFileLocation, IS_LOCAL, pType, pName, pOrigName, pQualifiedName, pInitializer);

    isFinal = pIsFinal;
  }

  @Override
  public JType getType() {
    return (JType) super.getType();
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    if (isFinal) {
    lASTString.append("final ");
    }

    lASTString.append(getType().toASTString(getName()));

    if (getInitializer() != null) {
      lASTString.append(" = ");
      lASTString.append(getInitializer().toASTString());
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

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + (isFinal ? 1231 : 1237);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof JVariableDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    JVariableDeclaration other = (JVariableDeclaration) obj;

    return other.isFinal == isFinal;
  }

}