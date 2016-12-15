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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.AArrayType;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nullable;

@SuppressFBWarnings(value="SE_NO_SUITABLE_CONSTRUCTOR",
    justification="handled by serialization proxy")
public final class CArrayType extends AArrayType implements CType {

  private static final long serialVersionUID = -6314468260643330323L;

  private final transient CExpression    length;
  private boolean   isConst;
  private boolean   isVolatile;

  public CArrayType(boolean pConst, boolean pVolatile,
      CType pType, @Nullable CExpression pLength) {
    super(pType);
    isConst = pConst;
    isVolatile = pVolatile;
    length = pLength;
  }

  @Override
  public CType getType() {
    return (CType) super.getType();
  }

  public @Nullable CExpression getLength() {
    return length;
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        +  getType().toASTString(pDeclarator+ ("[" + (length != null ? length.toASTString() : "") + "]"))
        ;
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
    return length == null; // C standard § 6.2.5 (22)
  }

  @Override
  public String toString() {
    return (isConst() ? "const " : "")
        + (isVolatile() ? "volatile " : "")
        + "("+ getType().toString() + (")[" + (length != null ? length.toASTString() : "") + "]");
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(length);
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
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CArrayType) || !super.equals(obj)) {
      return false;
    }

    CArrayType other = (CArrayType) obj;

    if (length instanceof CIntegerLiteralExpression && other.length instanceof CIntegerLiteralExpression) {
      if (!((CIntegerLiteralExpression)length).getValue().equals(((CIntegerLiteralExpression)other.length).getValue())) {
        return false;
      }
    } else {
      if (!Objects.equals(length, other.length)) {
        return false;
      }
    }

    return isConst == other.isConst && isVolatile == other.isVolatile;
  }

  @Override
  public CArrayType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CArrayType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    // C11 standard 6.7.3 (9) specifies that qualifiers like const and volatile
    // on an array type always refer to the element type, not the array type.
    // So we push these modifiers down to the element type here.
    return new CArrayType(false, false,
        getType().getCanonicalType(isConst || pForceConst,
                                   isVolatile || pForceVolatile),
        length);
  }

  @Override
  public boolean isBitField() {
    return false;
  }

  @Override
  public int getBitFieldSize() {
    return 0;
  }

  @Override
  public CType withBitFieldSize(int pBitFieldSize) {
    // Bit field size not supported
    assert false;
    return this;
  }

  private Object writeReplace() {
    return new SerializationProxy(this);
  }

  /**
   * javadoc to remove unused parameter warning
   * @param in the input stream
   */
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Proxy required");
  }

  private static class SerializationProxy implements Serializable {

    private static final long serialVersionUID = -2013901217157144921L;
    private final boolean isConst;
    private final boolean isVolatile;
    private final CType type;

    public SerializationProxy(CArrayType arrayType) {
      isConst = arrayType.isConst;
      isVolatile = arrayType.isVolatile;
      type = arrayType.getType();
    }

    private Object readResolve() {
      return new CArrayType(isConst, isVolatile, type, null);
    }
  }
}
