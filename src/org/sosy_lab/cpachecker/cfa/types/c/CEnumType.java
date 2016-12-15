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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;

public final class CEnumType implements CComplexType {

  private static final long serialVersionUID = -986078271714119880L;

  private final ImmutableList<CEnumerator> enumerators;
  private final String name;
  private final String origName;
  private boolean isConst;
  private boolean isVolatile;
  private Integer bitFieldSize;
  private int hashCache = 0;

  public CEnumType(final boolean pConst, final boolean pVolatile,
      final List<CEnumerator> pEnumerators, final String pName, final String pOrigName) {
    isConst = pConst;
    isVolatile = pVolatile;
    enumerators = ImmutableList.copyOf(pEnumerators);
    name = pName.intern();
    origName = pOrigName.intern();
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
  public boolean isIncomplete() {
    return false;
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
  public String getOrigName() {
    return origName;
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
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
    Joiner.on(",\n  ").appendTo(lASTString, transform(enumerators, CEnumerator::toASTString));
    lASTString.append("\n} ");
    lASTString.append(pDeclarator);

    return lASTString.toString() + (isBitField() ? " : " + getBitFieldSize() : "");
  }

  @Override
  public String toString() {
    return (isConst() ? "const " : "") +
           (isVolatile() ? "volatile " : "") +
           "enum " + name;
  }

  @Override
  public CEnumType withBitFieldSize(int pBitFieldSize) {
    if (isBitField() && bitFieldSize == pBitFieldSize) {
      return this;
    }
    CEnumType result = new CEnumType(isConst, isVolatile, enumerators, name, origName);
    result.bitFieldSize = pBitFieldSize;
    result.hashCache = 0;
    return result;
  }

  @Override
  public boolean isBitField() {
    return bitFieldSize != null;
  }

  @Override
  public int getBitFieldSize() {
    return bitFieldSize == null ? 0 : bitFieldSize;
  }

  @SuppressFBWarnings(
    value = "SE_NO_SUITABLE_CONSTRUCTOR",
    justification = "handled by serialization proxy"
  )
  public static final class CEnumerator extends AbstractSimpleDeclaration
      implements CSimpleDeclaration, Serializable {

    private static final long serialVersionUID = -2526725372840523651L;

    private final @Nullable Long  value;
    private CEnumType             enumType;
    private final String         qualifiedName;

    public CEnumerator(final FileLocation pFileLocation,
                          final String pName, final String pQualifiedName,
        final Long pValue) {
      super(pFileLocation, CNumericTypes.SIGNED_INT, pName);

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

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof CEnumerator) || !super.equals(obj)) {
        return false;
      }

      CEnumerator other = (CEnumerator) obj;

      return Objects.equals(value, other.value) && (qualifiedName.equals(other.qualifiedName));
      // do not compare the enumType, comparing it with == is wrong because types which
      // are the same but not identical would lead to wrong results
      // comparing it with equals is no good choice, too. This would lead to a stack
      // overflow
      //  && (enumType == other.enumType);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 7;
      result = prime * result + Objects.hashCode(value);
      result = prime * result + Objects.hashCode(enumType);
      result = prime * result + Objects.hashCode(qualifiedName);
      result = prime * result + super.hashCode();
      return result ;
    }

    /**
     * This method should be called only during parsing.
     */
    public void setEnum(CEnumType pEnumType) {
      checkState(enumType == null);
      enumType = checkNotNull(pEnumType);
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

    @Override
    public <R, X extends Exception> R accept(CSimpleDeclarationVisitor<R, X> pV) throws X {
      return pV.visit(this);
    }

    @Override
    public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
      return pV.visit(this);
    }

    private Object writeReplace() {
      return new SerializationProxy(this);
    }

    private static final class SerializationProxy implements Serializable {

      private static final long serialVersionUID = 3895126689420077689L;
      private final FileLocation fileLocation;
      private final @Nullable Long value;
      private final CEnumType enumType;
      private final String qualifiedName;

      private SerializationProxy(CEnumerator enumerator) {
        fileLocation = enumerator.getFileLocation();
        value = enumerator.value;
        enumType = enumerator.enumType;
        qualifiedName = enumerator.qualifiedName;
      }

      private Object readResolve() {
        CEnumerator result = new CEnumerator(fileLocation, qualifiedName, qualifiedName, value);
        result.setEnum(enumType);
        return result;
      }
    }
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    if (hashCache == 0) {
      final int prime = 31;
      int result = 7;
      result = prime * result + Objects.hashCode(isConst);
      result = prime * result + Objects.hashCode(isVolatile);
      result = prime * result + Objects.hashCode(name);
      result = prime * result + Objects.hashCode(bitFieldSize);
      hashCache = result;
    }
    return hashCache;
  }

  /**
   * Be careful, this method compares the CType as it is to the given object,
   * typedefs won't be resolved. If you want to compare the type without having
   * typedefs in it use #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CEnumType)) {
      return false;
    }

    CEnumType other = (CEnumType) obj;

    return isConst == other.isConst && isVolatile == other.isVolatile
           && Objects.equals(name, other.name)
           && Objects.equals(enumerators, other.enumerators)
           && Objects.equals(bitFieldSize, other.bitFieldSize);
  }

  @Override
  public boolean equalsWithOrigName(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CEnumType)) {
      return false;
    }

    CEnumType other = (CEnumType) obj;

    return isConst == other.isConst
           && isVolatile == other.isVolatile
           && (Objects.equals(name, other.name) || (origName.isEmpty() && other.origName.isEmpty()))
           && Objects.equals(enumerators, other.enumerators)
           && Objects.equals(bitFieldSize, other.bitFieldSize);
  }

  @Override
  public CEnumType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CEnumType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    if ((isConst == pForceConst) && (isVolatile == pForceVolatile) && !isBitField()) {
      return this;
    }
    CEnumType result = new CEnumType(isConst || pForceConst, isVolatile || pForceVolatile, enumerators, name, origName);
    if (isBitField()) {
      result = result.withBitFieldSize(bitFieldSize);
    }
    return result;
  }
}
