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
package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import javax.annotation.Nullable;


/**
 * This type is used when the parser could not determine the correct type.
 */
public final class CProblemType implements CType {

  private static final long serialVersionUID = -5658149239682173246L;
  private final String typeName;

  public CProblemType(String pTypeName) {
    typeName = checkNotNull(pTypeName);
  }

  @Override
  public String toString() {
    return typeName;
  }

  @Override
  public boolean isConst() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isVolatile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isIncomplete() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    return typeName + " " + pDeclarator;
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + Objects.hashCode(typeName);
      return result;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }

    if (!(obj instanceof CProblemType)) {
      return false;
    }

    CProblemType other = (CProblemType) obj;

    return Objects.equals(typeName, other.typeName);
  }

  @Override
  public CProblemType getCanonicalType() {
    return this;
  }

  @Override
  public CProblemType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    return this;
  }
}
