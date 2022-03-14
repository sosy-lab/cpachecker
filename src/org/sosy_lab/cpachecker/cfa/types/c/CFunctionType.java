// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;

public class CFunctionType extends AFunctionType implements CType {

  private static final long serialVersionUID = 4154771254170820716L;

  public static CFunctionType functionTypeWithReturnType(CType pReturnType) {
    return new CFunctionType(checkNotNull(pReturnType), ImmutableList.of(), false);
  }

  public static final CFunctionType NO_ARGS_VOID_FUNCTION =
      functionTypeWithReturnType(CVoidType.VOID);

  private @Nullable String name = null;

  public CFunctionType(CType pReturnType, List<CType> pParameters, boolean pTakesVarArgs) {
    super(pReturnType, pParameters, pTakesVarArgs);
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
    name = checkNotNull(pName);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<CType> getParameters() {
    return (List<CType>) super.getParameters();
  }

  @Override
  public String toASTString(final String pDeclarator) {
    return toASTString(
        pDeclarator, Lists.transform(getParameters(), pInput -> pInput.toASTString("")));
  }

  String toASTString(final String pDeclarator, final Iterable<?> pParameters) {
    checkNotNull(pDeclarator);
    final StringBuilder lASTString = new StringBuilder();

    if (pDeclarator.startsWith("*")) {
      // this is a function pointer, insert parentheses
      lASTString.append("(");
      lASTString.append(pDeclarator);
      lASTString.append(")");
    } else {
      lASTString.append(pDeclarator);
    }

    lASTString.append("(");
    Joiner.on(", ").appendTo(lASTString, pParameters);
    if (takesVarArgs()) {
      if (!getParameters().isEmpty()) {
        lASTString.append(", ");
      }
      lASTString.append("...");
    }
    lASTString.append(")");

    // The return type can span the rest of the type, so we cannot prefix but need this trick.
    String nameAndParams = lASTString.toString();
    return getReturnType().toASTString(nameAndParams);
  }

  @Override
  public boolean isConst() {
    return false;
  }

  @Override
  public boolean isVolatile() {
    return false;
  }

  @Override
  public boolean isIncomplete() {
    return false;
  }

  @Override
  public <R, X extends Exception> R accept(CTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * Be careful, this method compares the CType as it is to the given object, typedefs won't be
   * resolved. If you want to compare the type without having typedefs in it use
   * #getCanonicalType().equals()
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof CFunctionType && super.equals(obj);
  }

  @Override
  public CFunctionType getCanonicalType() {
    return getCanonicalType(false, false);
  }

  @Override
  public CFunctionType getCanonicalType(boolean pForceConst, boolean pForceVolatile) {
    // We ignore pForceConst and pForceVolatile.
    // const and volatile functions are undefined according to C11 §6.7.3 (9),
    // so we used to throw an exception here, but ignoring it does not hurt.
    // Probably no compiler does something else than ignoring these qualifiers as well.

    ImmutableList.Builder<CType> newParameterTypes = ImmutableList.builder();
    for (CType parameter : getParameters()) {
      newParameterTypes.add(parameter.getCanonicalType());
    }
    return new CFunctionType(
        getReturnType().getCanonicalType(), newParameterTypes.build(), takesVarArgs());
  }
}
