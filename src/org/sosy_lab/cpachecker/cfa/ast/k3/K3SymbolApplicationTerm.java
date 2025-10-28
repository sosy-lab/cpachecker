// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import com.google.common.base.Verify;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public final class K3SymbolApplicationTerm implements K3Term {
  @Serial private static final long serialVersionUID = 492023370394214710L;
  private final K3IdTerm symbol;
  private final List<K3Term> terms;
  private final FileLocation fileLocation;

  private static boolean wellFormedTerms(K3IdTerm pSymbol, List<K3Term> pTerms) {
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

  public K3SymbolApplicationTerm(
      K3IdTerm pSymbol, List<K3Term> pTerms, FileLocation pFileLocation) {
    terms = pTerms;
    fileLocation = pFileLocation;
    symbol = pSymbol;
    Verify.verify(wellFormedTerms(symbol, terms));
  }

  @Override
  public <R, X extends Exception> R accept(K3AstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
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

  public List<K3Term> getTerms() {
    return terms;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "";
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
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof K3SymbolApplicationTerm other
        && symbol.equals(other.symbol)
        && terms.equals(other.terms);
  }
}
