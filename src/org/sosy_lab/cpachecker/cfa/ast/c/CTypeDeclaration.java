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

import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class represents declarations that define new types,
 * e.g.
 *
 * struct s { int i; }
 * typedef int my_int;
 */
public abstract class CTypeDeclaration extends ADeclaration implements CDeclaration {

  private final String qualifiedName;

  public CTypeDeclaration(FileLocation pFileLocation, boolean pIsGlobal,
      CType pType, String pName, String pQualifiedName) {
    super(pFileLocation, pIsGlobal, pType, pName, pName);
    qualifiedName = pQualifiedName;
  }

  @Override
  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public CType getType() {
    return (CType)super.getType();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CTypeDeclaration)) {
      return false;
    }

    return super.equals(obj);
  }

}
