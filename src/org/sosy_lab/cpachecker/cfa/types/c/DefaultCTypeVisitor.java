// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.c;

public abstract class DefaultCTypeVisitor<R, X extends Exception> implements CTypeVisitor<R, X> {

  public abstract R visitDefault(final CType t) throws X;

  @Override
  public R visit(final CArrayType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CCompositeType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CElaboratedType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CEnumType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CFunctionType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CPointerType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CProblemType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CSimpleType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CTypedefType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CVoidType t) throws X {
    return visitDefault(t);
  }

  @Override
  public R visit(final CBitFieldType t) throws X {
    return visitDefault(t);
  }
}
