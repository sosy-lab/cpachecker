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

/**
 * Description of a Java array through its element type and number of dimensions.
 */
public class JArrayType extends AArrayType implements JReferenceType {

  private static final long serialVersionUID = -120299232751433551L;

  private final int dimension;

  /**
   * Creates a new <code>JArrayType</code> object that describes a Java array with the given
   * properties.
   *
   * @param pElementType the type of the array's elements described by a {@link JType}
   * @param pDimension the number of dimensions the array has
   */
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

  /**
   * Returns the type of the described array's elements.
   *
   * @return the type of the described array's elements
   */
  public JType getElementType() {
    return (JType) super.getType();
  }

  /**
   * Returns the number of dimensions of the described array.
   *
   * @return the number of dimensions of the described array
   */
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

  @Override
  public String toString() {
    return toASTString("");
  }
}