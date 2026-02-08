// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibSymbolApplicationRelationalTerm;

public interface SvLibTermVisitor<R, X extends Exception> {

  R accept(SvLibAtTerm pSvLibAtTerm) throws X;

  R accept(SvLibSymbolApplicationTerm pSvLibSymbolApplicationTerm) throws X;

  R accept(SvLibIdTerm pSvLibIdTerm) throws X;

  R accept(SvLibIntegerConstantTerm pSvLibIntegerConstantTerm) throws X;

  R accept(SvLibSymbolApplicationRelationalTerm pSvLibSymbolApplicationRelationalTerm) throws X;

  R accept(SvLibBooleanConstantTerm pSvLibBooleanConstantTerm) throws X;

  R accept(SvLibRealConstantTerm pSvLibRealConstantTerm) throws X;
}
