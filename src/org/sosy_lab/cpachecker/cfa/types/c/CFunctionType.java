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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.AFunctionType;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

public class CFunctionType extends AFunctionType implements CType {

  private boolean   isConst;
  private boolean   isVolatile;
  private String name = null;

  public CFunctionType(
      boolean pConst,
      boolean pVolatile,
      CType pReturnType,
      List<CType> pParameters,
      boolean pTakesVarArgs) {
    super(pReturnType, pParameters, pTakesVarArgs);

    isConst = pConst;
    isVolatile = pVolatile;
  }

  @Override
  public CType getReturnType() {
    return (CType) super.getReturnType();
  }

  public String getName() {
    return name;
  }

  public void setName(String pName) {
    checkState(name == null);
    name = pName;
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CType> getParameters() {
    return (List<CType>) super.getParameters();
  }

  @Override
  public String toString() {
    return toASTString(Strings.nullToEmpty(getName()),
                       new Function<CType, String>() {
                         @Override
                         public String apply(final CType pInput) {
                           return pInput.toString();
                         }
                       });
  }

  @Override
  public String toASTString(final String pDeclarator) {
    return toASTString(pDeclarator,
                       new Function<CType, String>() {
                         @Override
                         public String apply(final CType pInput) {
                           return pInput.toASTString("");
                         }
                        });
  }

  public String toASTString(final String pDeclarator, final Function<CType, String> pTypeToString) {
    final StringBuilder lASTString = new StringBuilder();

    if (isConst()) {
      lASTString.append("const ");
    }
    if (isVolatile()) {
      lASTString.append("volatile ");
    }

    lASTString.append(pTypeToString.apply(getReturnType()));
    lASTString.append(" ");

    if (pDeclarator.startsWith("*")) {
      // this is a function pointer, insert parentheses
      lASTString.append("(");
      lASTString.append(pDeclarator);
      lASTString.append(")");
    } else {
      lASTString.append(pDeclarator);
    }

    lASTString.append("(");
    Joiner.on(", ").appendTo(lASTString, transform(getParameters(), pTypeToString));
    if (takesVarArgs()) {
      if (!getParameters().isEmpty()) {
        lASTString.append(", ");
      }
      lASTString.append("...");
    }
    lASTString.append(")");

    return lASTString.toString();
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(isConst);
    result = prime * result + Objects.hashCode(isVolatile);
    result = prime * result + super.hashCode();
    return result;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CFunctionType) || !super.equals(obj)) {
      return false;
    }

    CFunctionType other = (CFunctionType) obj;

    return isConst == other.isConst && isVolatile == other.isVolatile;
  }

  @Override
  public CFunctionType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CFunctionType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    List<CType> newParameterTypes = new ArrayList<>();
    Iterator<CType> it = getParameters().iterator();

    while (it.hasNext()) {
      newParameterTypes.add(it.next().getCanonicalType());
    }
    return new CFunctionType(isConst || pForceConst, isVolatile || pForceVolatile, getReturnType().getCanonicalType(), newParameterTypes, takesVarArgs());
  }
}
