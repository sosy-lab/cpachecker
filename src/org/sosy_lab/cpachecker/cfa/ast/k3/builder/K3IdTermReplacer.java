// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3.builder;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Function;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3BooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3FinalRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3FinalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SymbolApplicationRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Term;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3TermVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class K3IdTermReplacer implements K3TermVisitor<K3FinalRelationalTerm, NoException> {

  private final Function<K3IdTerm, K3FinalRelationalTerm> replacementFunction;

  public K3IdTermReplacer(Function<K3IdTerm, K3FinalRelationalTerm> pReplacementFunction) {
    replacementFunction = pReplacementFunction;
  }

  @Override
  public K3FinalRelationalTerm accept(K3FinalTerm pK3FinalTerm) throws NoException {
    K3FinalRelationalTerm innerTerm = pK3FinalTerm.getTerm().accept(this);
    if (!(innerTerm instanceof K3IdTerm pIdTerm)) {
      throw new IllegalStateException(
          "Using a non-id term inside K3FinalTerm is not supported: " + innerTerm);
    }

    return new K3FinalTerm(pK3FinalTerm.getFileLocation(), pIdTerm);
  }

  @Override
  public K3FinalRelationalTerm accept(K3SymbolApplicationTerm pK3SymbolApplicationTerm)
      throws NoException {
    K3FinalRelationalTerm symbolReplacedTerm = pK3SymbolApplicationTerm.getSymbol().accept(this);
    if (!(symbolReplacedTerm instanceof K3IdTerm pIdTerm)) {
      throw new IllegalStateException(
          "Using a non-id term as symbol in K3SymbolApplicationTerm is not supported: "
              + symbolReplacedTerm);
    }

    List<K3FinalRelationalTerm> argsReplacedTerms =
        transformedImmutableListCopy(pK3SymbolApplicationTerm.getTerms(), t -> t.accept(this));
    if (argsReplacedTerms.stream().anyMatch(t -> !(t instanceof K3IdTerm))) {
      throw new IllegalStateException(
          "Using a non-id term as argument in K3SymbolApplicationTerm is not supported: "
              + argsReplacedTerms);
    }

    List<K3Term> argsAsTerms = transformedImmutableListCopy(argsReplacedTerms, t -> (K3Term) t);

    return new K3SymbolApplicationTerm(pIdTerm, argsAsTerms, FileLocation.DUMMY);
  }

  @Override
  public K3FinalRelationalTerm accept(K3IdTerm pK3IdTerm) throws NoException {
    return replacementFunction.apply(pK3IdTerm);
  }

  @Override
  public K3FinalRelationalTerm accept(K3IntegerConstantTerm pK3IntegerConstantTerm)
      throws NoException {
    return pK3IntegerConstantTerm;
  }

  @Override
  public K3FinalRelationalTerm accept(
      K3SymbolApplicationRelationalTerm pK3SymbolApplicationRelationalTerm) throws NoException {
    K3FinalRelationalTerm symbolReplacedTerm =
        pK3SymbolApplicationRelationalTerm.getSymbol().accept(this);
    if (!(symbolReplacedTerm instanceof K3IdTerm pIdTerm)) {
      throw new IllegalStateException(
          "Using a non-id term as symbol in K3SymbolApplicationTerm is not supported: "
              + symbolReplacedTerm);
    }

    List<K3FinalRelationalTerm> argsReplacedTerms =
        transformedImmutableListCopy(
            pK3SymbolApplicationRelationalTerm.getTerms(), t -> t.accept(this));

    return new K3SymbolApplicationRelationalTerm(pIdTerm, argsReplacedTerms, FileLocation.DUMMY);
  }

  @Override
  public K3FinalRelationalTerm accept(K3BooleanConstantTerm pK3BooleanConstantTerm)
      throws NoException {
    return pK3BooleanConstantTerm;
  }
}
