/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

/**
 * This represents a type which was created by using typedef.
 */
public final class CTypedefType implements CType {

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((realType == null) ? 0 : realType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    CTypedefType other = (CTypedefType) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (realType == null) {
      if (other.realType != null)
        return false;
    } else if (!realType.equals(other.realType))
      return false;
    return true;
  }

  private final String name; // the typedef name
  private final CType realType; // the real type this typedef points to
  private boolean   isConst;
  private boolean   isVolatile;

  public CTypedefType(final boolean pConst, final boolean pVolatile,
      final String pName, CType pRealType ) {

    isConst = pConst;
    isVolatile = pVolatile;
    name = pName.intern();
    realType = checkNotNull(pRealType);
  }

  public String getName() {
    return name;
  }

  public CType getRealType() {
    return realType;
  }

  @Override
  public String toASTString(String pDeclarator) {
    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + name
        + " " + pDeclarator;
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }
}
