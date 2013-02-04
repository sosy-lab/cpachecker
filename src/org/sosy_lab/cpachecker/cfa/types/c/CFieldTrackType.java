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
package org.sosy_lab.cpachecker.cfa.types.c;

/**
 * We use this type to be able to track the type of structs
 */
public class CFieldTrackType implements CType {

  private final CType structType;
  private final CType structTypeRepectingCasts;
  private final CType fieldType;


  public CFieldTrackType(CType pFieldType, CType pStructType, CType pStructTypeRepectingCasts) {
    structType = pStructType;
    fieldType = pFieldType;
    structTypeRepectingCasts = pStructTypeRepectingCasts;
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
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    // We do not really want to participate
    return fieldType.accept(pVisitor);
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException("Do not use hashCode of CType");
  }

  @Override
  public boolean equals(Object obj) {
    return CTypeUtils.equals(fieldType, obj);
  }

}
