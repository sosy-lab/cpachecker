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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.types;

import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * We use this type to be able to track the type of structs
 */
class CFieldDereferenceTrackType extends CtoFormulaCType {

  private final CType fieldType;
  private final CType fieldPtrType;


  public CFieldDereferenceTrackType(CType pFieldPtrType, CType fieldType) {
    fieldPtrType = pFieldPtrType;
    this.fieldType = fieldType;
  }

  @Override
  public String toASTString(String pDeclarator) {
    return fieldPtrType.toASTString(pDeclarator);
  }

  @Override
  public boolean isConst() {
    return fieldPtrType.isConst();
  }

  @Override
  public boolean isVolatile() {
    return fieldPtrType.isVolatile();
  }

  public CType getType() {
    return fieldPtrType;
  }

  public CType getReferencingFieldType() {
    return fieldType;
  }

  @Override
  public String toString() {
    return fieldPtrType.toString();
  }

  @Override
  public <R, X extends Exception> R accept(CtoFormulaTypeVisitor<R, X> pVisitor) throws X {
    // We do not really want to participate
    return fieldPtrType.accept(pVisitor);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(fieldType);
    result = prime * result + Objects.hashCode(fieldPtrType);
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

    if (!(obj instanceof CFieldDereferenceTrackType) || !super.equals(obj)) {
      return false;
    }

    CFieldDereferenceTrackType other = (CFieldDereferenceTrackType) obj;

    return Objects.equals(fieldPtrType, other.fieldPtrType) && Objects.equals(fieldType, other.fieldType);
  }

  @Override
  public CFieldDereferenceTrackType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CFieldDereferenceTrackType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    return new CFieldDereferenceTrackType(fieldPtrType.getCanonicalType(pForceConst, pForceVolatile), fieldType.getCanonicalType(pForceConst, pForceVolatile));
  }

}