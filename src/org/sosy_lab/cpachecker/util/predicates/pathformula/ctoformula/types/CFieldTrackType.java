/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types;

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * We use this type to be able to track the type of structs
 */
public class CFieldTrackType extends CtoFormulaCType {

  private final CType structType;
  private final CType structTypeRepectingCasts;
  private final CType fieldType;


  public CFieldTrackType(CType pFieldType, CType pStructType, CType pStructTypeRepectingCasts) {
    structType = pStructType;
    fieldType = pFieldType;
    structTypeRepectingCasts = pStructTypeRepectingCasts;
    assert !(fieldType instanceof CFieldTrackType);
    assert !(structTypeRepectingCasts instanceof CFieldTrackType);
  }

  @Override
  public String toASTString(String pDeclarator) {
    return fieldType.toASTString(pDeclarator);
  }

  @Override
  public boolean isConst() {
    return fieldType.isConst();
  }

  @Override
  public boolean isVolatile() {
    return fieldType.isVolatile();
  }

  public CType getType() {
    return fieldType;
  }

  public CType getStructType() {
    return structType;
  }

  public CType getStructTypeRepectingCasts() {
    return structTypeRepectingCasts;
  }


  @Override
  public String toString() {
    return fieldType.toString();
  }

  @Override
  public <R, X extends Exception> R accept(CtoFormulaTypeVisitor<R, X> pVisitor) throws X {
    // We do not really want to participate
    return fieldType.accept(pVisitor);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(structType);
    result = prime * result + Objects.hashCode(fieldType);
    result = prime * result + Objects.hashCode(structTypeRepectingCasts);
    return prime * result + super.hashCode();
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CFieldTrackType) || !super.equals(obj)) {
      return false;
    }

    CFieldTrackType other = (CFieldTrackType) obj;

    return Objects.equals(structType, other.structType) && Objects.equals(fieldType, other.fieldType)
           && Objects.equals(structTypeRepectingCasts, other.structTypeRepectingCasts);
  }

  @Override
  public CFieldTrackType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CFieldTrackType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    return new CFieldTrackType(fieldType.getCanonicalType(pForceConst, pForceVolatile),
                               structType.getCanonicalType(pForceConst, pForceVolatile),
                               structTypeRepectingCasts.getCanonicalType(pForceConst, pForceVolatile));
  }

}
