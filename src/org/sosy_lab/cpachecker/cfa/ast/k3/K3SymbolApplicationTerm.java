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
import org.sosy_lab.cpachecker.cfa.types.Type;

public final class K3SymbolApplicationTerm extends K3ApplicationTerm {
  @Serial private static final long serialVersionUID = 492023370394214710L;
  private final String symbol;

  public K3SymbolApplicationTerm(String pSymbol, List<K3Term> pTerms, FileLocation pFileLocation) {
    super(pTerms, pFileLocation);
    symbol = pSymbol;
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  public String getSymbol() {
    return symbol;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3SymbolApplicationTerm other
        && symbol.equals(other.symbol)
        && super.equals(obj);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + symbol.hashCode();
  }

  @Override
  public <R, X extends Exception> R accept(K3TermVisitor<R, X> v) throws X {
    return v.accept(this);
  }

  @Override
  public Type getExpressionType() {
    // TODO: We will need to keep track of the type of the symbol
    return null;
  }
}
