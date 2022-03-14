// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the type "void". It does not allow any modifiers and has only a single
 * instance.
 */
public final class CVoidType implements CType {

  private static final long serialVersionUID = 1385808708190595556L;

  public static final CVoidType VOID = new CVoidType(false, false);

  private static final CVoidType CONST_VOID = new CVoidType(true, false);
  private static final CVoidType VOLATILE_VOID = new CVoidType(false, true);
  private static final CVoidType CONST_VOLATILE_VOID = new CVoidType(true, true);

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
