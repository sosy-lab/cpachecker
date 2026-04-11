// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.base.Joiner;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public abstract class SvLibGeneralSymbolApplicationTerm implements SvLibRelationalTerm {

  @Serial private static final long serialVersionUID = -1896197197042124013L;

  private final SvLibIdTerm symbol;
  private final ImmutableList<? extends SvLibRelationalTerm> terms;
  private final FileLocation fileLocation;

  protected SvLibGeneralSymbolApplicationTerm(
      SvLibIdTerm pSymbol, List<? extends SvLibRelationalTerm> pTerms, FileLocation pFileLocation) {
    terms = ImmutableList.copyOf(pTerms);
    fileLocation = pFileLocation;
    symbol = pSymbol;
    Verify.verify(SvLibGeneralSymbolApplicationTerm.wellFormedTerms(symbol, terms));
  }

  public SvLibIdTerm getSymbol() {
    return symbol;
  }

  @Override
  public SvLibType getExpressionType() {
    SvLibType type = symbol.getExpressionType();
    if (type instanceof SvLibFunctionType functionType) {
      return functionType.getReturnType();
    }
    return type;
  }

  public @NonNull ImmutableList<? extends SvLibRelationalTerm> getTerms() {
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
        + Joiner.on(" ").join(terms.stream().map(SvLibRelationalTerm::toASTString).toList())
        + ")";
  }

  @Override
  public String toParenthesizedASTString(AAstNodeRepresentation pAAstNodeRepresentation) {
    return "("
        + symbol.toASTString(pAAstNodeRepresentation)
        + " "
        + Joiner.on(" ").join(terms.stream().map(SvLibRelationalTerm::toASTString).toList())
        + ")";
  }

  @Override
  public int hashCode() {
    return 31 * symbol.hashCode() + terms.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibGeneralSymbolApplicationTerm other
        && symbol.equals(other.symbol)
        && terms.equals(other.terms);
  }

  protected static boolean wellFormedTerms(
      SvLibIdTerm pSymbol, List<? extends SvLibRelationalTerm> pTerms) {
    if (pSymbol.getExpressionType() instanceof SvLibFunctionType functionType) {
      List<SvLibType> parameterTypes = functionType.getParameters();
      if (parameterTypes.size() != pTerms.size()) {
        return false;
      }
      for (int i = 0; i < parameterTypes.size(); i++) {
        if (!SvLibType.canBeCastTo(parameterTypes.get(i), pTerms.get(i).getExpressionType())) {
          return false;
        }
      }
      return true;
    } else {
      return pTerms.isEmpty();
    }
  }
}
