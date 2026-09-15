// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibSymbolApplicationRelationalTerm;
import org.sosy_lab.cpachecker.exceptions.NoException;

public abstract class SvLibIdTermReplacer
    implements SvLibTermVisitor<SvLibRelationalTerm, NoException> {

  public abstract SvLibRelationalTerm replace(SvLibIdTerm pIdTerm);

  @Override
  public SvLibRelationalTerm accept(SvLibAtTerm pSvLibAtTerm) throws NoException {
    SvLibRelationalTerm innerTerm = pSvLibAtTerm.getTerm().accept(this);
    if (!(innerTerm instanceof SvLibIdTerm pIdTerm)) {
      throw new IllegalStateException(
          "Using a non-id term inside SvLibFinalTerm is not supported: " + innerTerm);
    }

    return new SvLibAtTerm(pSvLibAtTerm.getFileLocation(), pSvLibAtTerm.getTagReference(), pIdTerm);
  }

  @Override
  public SvLibRelationalTerm accept(SvLibSymbolApplicationTerm pSvLibSymbolApplicationTerm)
      throws NoException {
    SvLibRelationalTerm symbolReplacedTerm = pSvLibSymbolApplicationTerm.getSymbol().accept(this);
    if (!(symbolReplacedTerm instanceof SvLibIdTerm pIdTerm)) {
      throw new IllegalStateException(
          "Using a non-id term as symbol in SvLibSymbolApplicationTerm is not supported: "
              + symbolReplacedTerm);
    }

    List<SvLibRelationalTerm> argsReplacedTerms =
        transformedImmutableListCopy(pSvLibSymbolApplicationTerm.getTerms(), t -> t.accept(this));
    if (argsReplacedTerms.stream().allMatch(t -> t instanceof SvLibTerm)) {
      List<SvLibTerm> argsAsTerms =
          transformedImmutableListCopy(argsReplacedTerms, t -> (SvLibTerm) t);

      return new SvLibSymbolApplicationTerm(pIdTerm, argsAsTerms, FileLocation.DUMMY);
    }

    return new SvLibSymbolApplicationRelationalTerm(pIdTerm, argsReplacedTerms, FileLocation.DUMMY);
  }

  @Override
  public SvLibRelationalTerm accept(SvLibIdTerm pSvLibIdTerm) throws NoException {
    return replace(pSvLibIdTerm);
  }

  @Override
  public SvLibRelationalTerm accept(SvLibIntegerConstantTerm pSvLibIntegerConstantTerm)
      throws NoException {
    return pSvLibIntegerConstantTerm;
  }

  @Override
  public SvLibRelationalTerm accept(
      SvLibSymbolApplicationRelationalTerm pSvLibSymbolApplicationRelationalTerm)
      throws NoException {
    SvLibRelationalTerm symbolReplacedTerm =
        pSvLibSymbolApplicationRelationalTerm.getSymbol().accept(this);
    if (!(symbolReplacedTerm instanceof SvLibIdTerm pIdTerm)) {
      throw new IllegalStateException(
          "Using a non-id term as symbol in SvLibSymbolApplicationTerm is not supported: "
              + symbolReplacedTerm);
    }

    List<SvLibRelationalTerm> argsReplacedTerms =
        transformedImmutableListCopy(
            pSvLibSymbolApplicationRelationalTerm.getTerms(), t -> t.accept(this));

    return new SvLibSymbolApplicationRelationalTerm(pIdTerm, argsReplacedTerms, FileLocation.DUMMY);
  }

  @Override
  public SvLibRelationalTerm accept(SvLibBooleanConstantTerm pSvLibBooleanConstantTerm)
      throws NoException {
    return pSvLibBooleanConstantTerm;
  }

  @Override
  public SvLibRelationalTerm accept(SvLibRealConstantTerm pSvLibRealConstantTerm)
      throws NoException {
    return pSvLibRealConstantTerm;
  }
}
