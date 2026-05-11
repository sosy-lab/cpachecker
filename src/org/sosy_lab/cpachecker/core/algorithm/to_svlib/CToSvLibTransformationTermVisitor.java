// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBitVectorConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRealConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermVisitor;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibSymbolApplicationRelationalTerm;
import org.sosy_lab.cpachecker.exceptions.NoException;

class CToSvLibTransformationTermVisitor implements SvLibTermVisitor<Void, NoException> {

  ImmutableList.Builder<SvLibSymbolApplicationTerm> assignmentTermsCollector;

  CToSvLibTransformationTermVisitor(
      ImmutableList.Builder<SvLibSymbolApplicationTerm> pAssignmentTermsCollector) {
    assignmentTermsCollector = pAssignmentTermsCollector;
  }

  @Override
  public Void accept(SvLibAtTerm pSvLibAtTerm) throws NoException {
    return null;
  }

  @Override
  public Void accept(SvLibSymbolApplicationTerm pSvLibSymbolApplicationTerm) throws NoException {
    if (pSvLibSymbolApplicationTerm.getSymbol().getName().equals("=")
        && pSvLibSymbolApplicationTerm.getTerms().size() == 2) {
      assignmentTermsCollector.add(pSvLibSymbolApplicationTerm);
    } else {
      for (SvLibTerm term : pSvLibSymbolApplicationTerm.getTerms()) {
        term.accept(this);
      }
    }

    return null;
  }

  @Override
  public Void accept(SvLibIdTerm pSvLibIdTerm) throws NoException {
    return null;
  }

  @Override
  public Void accept(SvLibIntegerConstantTerm pSvLibIntegerConstantTerm) throws NoException {
    return null;
  }

  @Override
  public Void accept(SvLibSymbolApplicationRelationalTerm pSvLibSymbolApplicationRelationalTerm)
      throws NoException {
    return null;
  }

  @Override
  public Void accept(SvLibBooleanConstantTerm pSvLibBooleanConstantTerm) throws NoException {
    return null;
  }

  @Override
  public Void accept(SvLibRealConstantTerm pSvLibRealConstantTerm) throws NoException {
    return null;
  }

  @Override
  public Void accept(SvLibBitVectorConstantTerm pSvLibBitVectorConstantTerm) throws NoException {
    return null;
  }
}
