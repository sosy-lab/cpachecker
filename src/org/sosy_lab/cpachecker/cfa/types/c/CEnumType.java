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

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Iterables.transform;
import static org.sosy_lab.cpachecker.cfa.ast.c.CAstNode.TO_AST_STRING;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public final class CEnumType implements CComplexType {

  private final ImmutableList<CEnumerator> enumerators;
  private final String                     name;
  private boolean   isConst;
  private boolean   isVolatile;

  public CEnumType(final boolean pConst, final boolean pVolatile,
      final List<CEnumerator> pEnumerators, final String pName) {
    isConst = pConst;
    isVolatile = pVolatile;
    enumerators = ImmutableList.copyOf(pEnumerators);
    name = pName;
  }

  @Override
  public boolean isConst() {
    return isConst;
  }

  @Override
  public boolean isVolatile() {
    return isVolatile;
  }

  public ImmutableList<CEnumerator> getEnumerators() {
    return enumerators;
  }

  @Override
  public ComplexTypeKind getKind() {
    return ComplexTypeKind.ENUM;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getQualifiedName() {
    return ("enum " + name).trim();
  }

  @Override
  public String toASTString(String pDeclarator) {
    StringBuilder lASTString = new StringBuilder();

    if (isConst()) {
      lASTString.append("const ");
    }
    if (isVolatile()) {
      lASTString.append("volatile ");
    }

    lASTString.append("enum ");
    lASTString.append(name);

    lASTString.append(" {\n  ");
    Joiner.on(",\n  ").appendTo(lASTString, transform(enumerators, TO_AST_STRING));
    lASTString.append("\n} ");
    lASTString.append(pDeclarator);

    return lASTString.toString();
  }

  @Override
  public String toString() {
    return this.toASTString("");
  }

  public static final class CEnumerator extends ASimpleDeclarations implements CSimpleDeclaration {

    private static final CType INT_TYPE = new CSimpleType(true, false, CBasicType.INT, false, false, true, false, false, false, false);

    private final Long           value;
    private CEnumType             enumType;
    private final String         qualifiedName;

    public CEnumerator(final FileLocation pFileLocation,
                          final String pName, final String pQualifiedName,
        final Long pValue) {
      super(pFileLocation, INT_TYPE, pName);

      checkNotNull(pName);
      value = pValue;
      qualifiedName = checkNotNull(pQualifiedName);
    }

    /**
     * Get the enum that declared this enumerator.
     */
    public CEnumType getEnum() {
      return enumType;
    }

    /**
     * This method should be called only during parsing.
     */
    public void setEnum(CEnumType pEnumType) {
      checkState(enumType == null);
      enumType = pEnumType;
    }

    @Override
    public String getQualifiedName() {
      return qualifiedName;
    }

    @Override
    public CType getType() {
      return (CType) super.getType();
    }

    public long getValue() {
      checkState(value != null, "Need to check hasValue() before calling getValue()");
      return value;
    }

    public boolean hasValue() {
      return value != null;
    }

    @Override
    public String toASTString() {
      return getName()
          + (hasValue() ? " = " + String.valueOf(value) : "");
    }
  }


  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException("Do not use hashCode of CType");
  }

  @Override
  public boolean equals(Object obj) {
    return CTypeUtils.equals(this, obj);
  }
}
