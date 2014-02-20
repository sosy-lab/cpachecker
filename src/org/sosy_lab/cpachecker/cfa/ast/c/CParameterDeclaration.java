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

import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;


/**
 * This is the declaration of a function parameter. It contains a type and a name.
 */
public final class CParameterDeclaration extends AParameterDeclaration implements CSimpleDeclaration {

  private String qualifiedName;

  public CParameterDeclaration(FileLocation pFileLocation,
                                  CType pType,
                                  String pName) {
    super(pFileLocation, pType, checkNotNull(pName));
  }


  public void setQualifiedName(String pQualifiedName) {
    checkState(qualifiedName == null);
    qualifiedName = checkNotNull(pQualifiedName);
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public CType getType() {
    return (CType)super.getType();
  }

  public CVariableDeclaration asVariableDeclaration() {
    return new CVariableDeclaration(getFileLocation(), false, CStorageClass.AUTO,
        getType(), getName(), getOrigName(), getQualifiedName(), null);
  }

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

    if (!(obj instanceof CParameterDeclaration)) {
      return false;
    }

    return super.equals(obj);
  }


  @Override
  public <R, X extends Exception> R accept(CSimpleDeclarationVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }
}