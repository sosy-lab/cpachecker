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
package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.*;

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class represents variable declarations.
 * Example code:
 *
 * int x = 0;
 * struct s { ... } st;
 */
public final class CVariableDeclaration extends AVariableDeclaration implements CDeclaration {

  private final String qualifiedName;
  private final CStorageClass    cStorageClass;

  public CVariableDeclaration(FileLocation pFileLocation, boolean pIsGlobal,
      CStorageClass pCStorageClass, CType pType, String pName, String pOrigName,
      String pQualifiedName,
      CInitializer pInitializer) {

    super(pFileLocation, pIsGlobal, pType, checkNotNull(pName), pOrigName, pInitializer);
    cStorageClass = pCStorageClass;
    qualifiedName = checkNotNull(pQualifiedName);

    checkArgument(!(cStorageClass == CStorageClass.EXTERN && getInitializer() != null), "Extern declarations cannot have an initializer");
    checkArgument(cStorageClass == CStorageClass.EXTERN || cStorageClass == CStorageClass.AUTO, "CStorageClass is " + cStorageClass);
    checkArgument(pIsGlobal || cStorageClass == CStorageClass.AUTO);
  }

  @Override
  public CType getType() {
    return (CType)super.getType();
  }

  /**
   * The storage class of this variable (either extern or auto).
   */
  public CStorageClass getCStorageClass() {
    return cStorageClass;
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public CInitializer getInitializer() {
    return (CInitializer) super.getInitializer();
  }

  /**
   * Add an initializer.
   * This is only possible if there is no initializer already.
   * DO NOT CALL this method after CFA construction!
   * @param pCInitializer the new initializer
   */
  public void addInitializer(CInitializer pCInitializer) {
    super.addInitializer(pCInitializer);
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(cStorageClass.toASTString());
    lASTString.append(getType().toASTString(getName()));

    if (getInitializer() != null) {
      lASTString.append(" = ");
      lASTString.append(getInitializer().toASTString());
    }

    lASTString.append(";");

    return lASTString.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(cStorageClass);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }

    if (!(obj instanceof CVariableDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    CVariableDeclaration other = (CVariableDeclaration) obj;

    return Objects.equals(other.cStorageClass, cStorageClass);
  }


}