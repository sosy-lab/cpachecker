// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import java.io.Serial;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public abstract sealed class K3GeneralSymbolApplicationTerm implements K3RelationalTerm
    permits K3SymbolApplicationRelationalTerm, K3SymbolApplicationTerm {

  @Serial private static final long serialVersionUID = -1896197197042124013L;

  private final K3IdTerm symbol;
  private final List<? extends K3RelationalTerm> terms;
  private final FileLocation fileLocation;

  protected K3GeneralSymbolApplicationTerm(
      K3IdTerm pSymbol, List<? extends K3RelationalTerm> pTerms, FileLocation pFileLocation) {
    terms = pTerms;
    fileLocation = pFileLocation;
    symbol = pSymbol;
    Verify.verify(K3GeneralSymbolApplicationTerm.wellFormedTerms(symbol, terms));
  }

  public K3IdTerm getSymbol() {
    return symbol;
  }

  @Override
  public K3Type getExpressionType() {
    K3Type type = symbol.getExpressionType();
    if (type instanceof K3FunctionType functionType) {
      return functionType.getReturnType();
    }
    return type;
  }

  public @NonNull List<@NonNull ? extends K3RelationalTerm> getTerms() {
    return terms;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "("
        + symbol.toASTString(pAAstNodeRepresentation)
        + " "
        + Joiner.on(" ").join(terms.stream().map(K3RelationalTerm::toASTString).toList())
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "("
        + symbol.toASTString(pAAstNodeRepresentation)
        + " "
        + Joiner.on(" ").join(terms.stream().map(K3RelationalTerm::toASTString).toList())
        + ")";
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + symbol.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3GeneralSymbolApplicationTerm other
        && symbol.equals(other.symbol)
        && terms.equals(other.terms);
  }

  protected static boolean wellFormedTerms(
      K3IdTerm pSymbol, List<? extends K3RelationalTerm> pTerms) {
    if (pSymbol.getExpressionType() instanceof K3FunctionType functionType) {
      List<K3Type> parameterTypes = functionType.getParameters();
      if (parameterTypes.size() != pTerms.size()) {
        return false;
      }
      for (int i = 0; i < parameterTypes.size(); i++) {
        if (!K3Type.compatibleTypes(parameterTypes.get(i), pTerms.get(i).getExpressionType())) {
          return false;
        }
      }
      return true;
    } else {
      return pTerms.isEmpty();
    }
  }
}
