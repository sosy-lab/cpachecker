// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.builder;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Function;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFinalRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFinalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class SvLibIdTermReplacer
    implements SvLibTermVisitor<SvLibFinalRelationalTerm, NoException> {

  private final Function<SvLibIdTerm, SvLibFinalRelationalTerm> replacementFunction;

  public SvLibIdTermReplacer(Function<SvLibIdTerm, SvLibFinalRelationalTerm> pReplacementFunction) {
    replacementFunction = pReplacementFunction;
  }

  @Override
  public SvLibFinalRelationalTerm accept(SvLibFinalTerm pSvLibFinalTerm) throws NoException {
    SvLibFinalRelationalTerm innerTerm = pSvLibFinalTerm.getTerm().accept(this);
    if (!(innerTerm instanceof SvLibIdTerm pIdTerm)) {
      throw new IllegalStateException(
          "Using a non-id term inside SvLibFinalTerm is not supported: " + innerTerm);
    }

    return new SvLibFinalTerm(pSvLibFinalTerm.getFileLocation(), pIdTerm);
  }

  @Override
  public SvLibFinalRelationalTerm accept(SvLibSymbolApplicationTerm pSvLibSymbolApplicationTerm)
      throws NoException {
    SvLibFinalRelationalTerm symbolReplacedTerm =
        pSvLibSymbolApplicationTerm.getSymbol().accept(this);
    if (!(symbolReplacedTerm instanceof SvLibIdTerm pIdTerm)) {
      throw new IllegalStateException(
          "Using a non-id term as symbol in SvLibSymbolApplicationTerm is not supported: "
              + symbolReplacedTerm);
    }

    List<SvLibFinalRelationalTerm> argsReplacedTerms =
        transformedImmutableListCopy(pSvLibSymbolApplicationTerm.getTerms(), t -> t.accept(this));
    if (argsReplacedTerms.stream().anyMatch(t -> !(t instanceof SvLibIdTerm))) {
      throw new IllegalStateException(
          "Using a non-id term as argument in SvLibSymbolApplicationTerm is not supported: "
              + argsReplacedTerms);
    }

    List<SvLibTerm> argsAsTerms =
        transformedImmutableListCopy(argsReplacedTerms, t -> (SvLibTerm) t);

    return new SvLibSymbolApplicationTerm(pIdTerm, argsAsTerms, FileLocation.DUMMY);
  }

  @Override
  public SvLibFinalRelationalTerm accept(SvLibIdTerm pSvLibIdTerm) throws NoException {
    return replacementFunction.apply(pSvLibIdTerm);
  }

  @Override
  public SvLibFinalRelationalTerm accept(SvLibIntegerConstantTerm pSvLibIntegerConstantTerm)
      throws NoException {
    return pSvLibIntegerConstantTerm;
  }

  @Override
  public SvLibFinalRelationalTerm accept(
      SvLibSymbolApplicationRelationalTerm pSvLibSymbolApplicationRelationalTerm)
      throws NoException {
    SvLibFinalRelationalTerm symbolReplacedTerm =
        pSvLibSymbolApplicationRelationalTerm.getSymbol().accept(this);
    if (!(symbolReplacedTerm instanceof SvLibIdTerm pIdTerm)) {
      throw new IllegalStateException(
          "Using a non-id term as symbol in SvLibSymbolApplicationTerm is not supported: "
              + symbolReplacedTerm);
    }

    List<SvLibFinalRelationalTerm> argsReplacedTerms =
        transformedImmutableListCopy(
            pSvLibSymbolApplicationRelationalTerm.getTerms(), t -> t.accept(this));

    return new SvLibSymbolApplicationRelationalTerm(pIdTerm, argsReplacedTerms, FileLocation.DUMMY);
  }

  @Override
  public SvLibFinalRelationalTerm accept(SvLibBooleanConstantTerm pSvLibBooleanConstantTerm)
      throws NoException {
    return pSvLibBooleanConstantTerm;
  }
}
