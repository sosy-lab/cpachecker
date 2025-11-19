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
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibAstNodeVisitor;

public final class SvLibSymbolApplicationRelationalTerm extends SvLibGeneralSymbolApplicationTerm
    implements SvLibFinalRelationalTerm {
  @Serial private static final long serialVersionUID = 492023370394214710L;

  public SvLibSymbolApplicationRelationalTerm(
      SvLibIdTerm pSymbol, List<SvLibFinalRelationalTerm> pTerms, FileLocation pFileLocation) {
    super(pSymbol, pTerms, pFileLocation);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ImmutableList<SvLibFinalRelationalTerm> getTerms() {
    return (ImmutableList<SvLibFinalRelationalTerm>) ImmutableList.copyOf(super.getTerms());
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
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibSymbolApplicationRelationalTerm other && super.equals(other);
  }
}
