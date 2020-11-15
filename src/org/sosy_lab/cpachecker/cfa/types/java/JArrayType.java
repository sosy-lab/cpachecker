// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.java;

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
      result = prime * result + Integer.hashCode(dimension);
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