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
package org.sosy_lab.cpachecker.cfa.types.java;

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.AArrayType;


public class JArrayType extends AArrayType implements JReferenceType {

  private final int dimension;

  public JArrayType(final JType pElementType, final int pDimension) {

    super(pElementType);
    dimension = pDimension;
  }

  @Override
  public String toASTString(String pDeclarator) {
    StringBuilder astString = new StringBuilder(getElementType().toASTString(""));

    for (int dim = 0; dim < dimension ; dim++) {
        astString.append("[]");
    }
    astString.append(" ");
    astString.append(pDeclarator);

    return  astString.toString();
  }

  public JType getElementType() {
    return (JType) super.getType();
  }

  public int getDimensions() {
    return dimension;
  }

  @Override
  public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + Objects.hashCode(dimension);
      result = prime * result + super.hashCode();
      return result;
  }

  @Override
  public boolean equals(Object obj) {
      if (this == obj) {
          return true;
      }

      if (!(obj instanceof JArrayType) && !super.equals(obj)) {
          return false;
      }

      JArrayType other = (JArrayType) obj;

      return dimension == other.dimension;
    }
}