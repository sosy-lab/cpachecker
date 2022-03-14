// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** Interface for all possible right-hand sides of an assignment. */
public interface CRightHandSide extends CAstNode, ARightHandSide {

  <R, X extends Exception> R accept(CRightHandSideVisitor<R, X> pV) throws X;

  @Override
  CType getExpressionType();
}
