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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the type "void".
 * It does not allow any modifiers and has only a single instance.
 */
public final class CVoidType implements CType {

  private static final long serialVersionUID = 1385808708190595556L;

  public final static CVoidType VOID = new CVoidType(false, false);

  private final static CVoidType CONST_VOID = new CVoidType(true, false);
  private final static CVoidType VOLATILE_VOID = new CVoidType(false, true);
  private final static CVoidType CONST_VOLATILE_VOID = new CVoidType(true, true);

  public static CVoidType create(boolean pIsConst, boolean pIsVolatile) {
    if (pIsConst) {
      return pIsVolatile ? CONST_VOLATILE_VOID : CONST_VOID;
    } else {
      return pIsVolatile ? VOLATILE_VOID : VOID;
    }
  }

  private final boolean isConst;
  private final boolean isVolatile;

  private CVoidType(boolean pIsConst, boolean pIsVolatile) {
    isConst = pIsConst;
    isVolatile = pIsVolatile;
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
    return true; // C standard § 6.2.5 (19)
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    return toASTString("");
  }

  @Override
  public String toASTString(String pDeclarator) {
    checkNotNull(pDeclarator);
    List<String> parts = new ArrayList<>();

    if (isConst()) {
      parts.add("const");
    }
    if (isVolatile()) {
      parts.add("volatile");
    }

    parts.add("void");
    parts.add(Strings.emptyToNull(pDeclarator));

    return Joiner.on(' ').skipNulls().join(parts);
  }

  @Override
  public CVoidType getCanonicalType() {
    return this;
  }

  @Override
  public CVoidType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    return create(isConst || pForceConst, isVolatile || pForceVolatile);
  }

  private Object readResolve() {
    return create(isConst, isVolatile);
  }
}
