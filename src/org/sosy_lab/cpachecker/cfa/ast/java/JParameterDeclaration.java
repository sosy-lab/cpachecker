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

import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class contains Parameter Declarations for methods.
 * It contains a type and a name.
 */
public class JParameterDeclaration extends AParameterDeclaration implements JSimpleDeclaration {

  private final boolean isFinal;

  public JParameterDeclaration(FileLocation pFileLocation, JType pType, String pName, boolean pIsFinal) {
    super(pFileLocation, pType, pName);
    isFinal = pIsFinal;
  }

  @Override
  public JType getType() {
    return (JType) super.getType();
  }

  public boolean isFinal() {
    return isFinal;
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
    if (this == obj) { return true; }

    if (!(obj instanceof JParameterDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    JParameterDeclaration other = (JParameterDeclaration) obj;

    return other.isFinal == isFinal;
  }

}
