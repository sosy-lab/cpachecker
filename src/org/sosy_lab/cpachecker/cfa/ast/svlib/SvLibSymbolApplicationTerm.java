// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class SvLibSymbolApplicationTerm extends SvLibGeneralSymbolApplicationTerm
    implements SvLibTerm {
  @Serial private static final long serialVersionUID = 492023370394214710L;

  public SvLibSymbolApplicationTerm(
      SvLibIdTerm pSymbol, List<SvLibTerm> pTerms, FileLocation pFileLocation) {
    super(pSymbol, pTerms, pFileLocation);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ImmutableList<SvLibTerm> getTerms() {
    return (ImmutableList<SvLibTerm>) ImmutableList.copyOf(super.getTerms());
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibTermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibExpressionVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibSymbolApplicationTerm other && super.equals(other);
  }
}
