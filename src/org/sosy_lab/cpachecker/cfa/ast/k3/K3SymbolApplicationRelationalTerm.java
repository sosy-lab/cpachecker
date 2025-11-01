// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3SymbolApplicationRelationalTerm extends K3GeneralSymbolApplicationTerm
    implements K3FinalRelationalTerm {
  @Serial private static final long serialVersionUID = 492023370394214710L;

  public K3SymbolApplicationRelationalTerm(
      K3IdTerm pSymbol, List<K3FinalRelationalTerm> pTerms, FileLocation pFileLocation) {
    super(pSymbol, pTerms, pFileLocation);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<K3FinalRelationalTerm> getTerms() {
    return (List<K3FinalRelationalTerm>) super.getTerms();
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public <R, X extends Exception> R accept(K3TermVisitor<R, X> v) throws X {
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

    return obj instanceof K3SymbolApplicationRelationalTerm other && super.equals(other);
  }
}
