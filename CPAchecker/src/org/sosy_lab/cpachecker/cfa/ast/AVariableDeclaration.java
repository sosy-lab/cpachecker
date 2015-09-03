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
package org.sosy_lab.cpachecker.cfa.ast;


import static com.google.common.base.Preconditions.*;

import org.sosy_lab.cpachecker.cfa.types.Type;


public abstract class AVariableDeclaration extends AbstractDeclaration {

  private final String qualifiedName;
  private AInitializer initializer;

  public AVariableDeclaration(FileLocation pFileLocation, boolean pIsGlobal,
      Type pType, String pName, String pOrigName, String pQualifiedName,
      AInitializer pInitializer) {
    super(pFileLocation, pIsGlobal, pType, pName, pOrigName);
    qualifiedName = checkNotNull(pQualifiedName);
    initializer = pInitializer;
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  /**
   * The initial value of the variable
   * (only if present, null otherwise).
   */
  public AInitializer getInitializer() {
    return initializer;
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(getType().toASTString(getName()));

    if (initializer != null) {
      lASTString.append(" = ");
      lASTString.append(initializer.toASTString());
    }

    lASTString.append(";");

    return lASTString.toString();
  }

  protected void addInitializer(AInitializer pCInitializer) {
    checkState(getInitializer() == null);
    initializer = pCInitializer;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + qualifiedName.hashCode();
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

    if (!(obj instanceof AVariableDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    AVariableDeclaration other = (AVariableDeclaration) obj;

    return qualifiedName.equals(other.qualifiedName);
  }

}
