// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

/**
 * This is the base interface for all SV-LIB expressions in the CPAchecker framework. It encompasses
 * internal data structures, which only inherit from this class and are mostly used to map SV-LIB
 * concepts into idiomatic CPAchecker concepts. Actual SV-LIB expressions all inherit from {@link
 * SvLibTerm}. This distinction should be strict, since terms correspond to SMT-LIB S-Expressions,
 * and thus form the basis for logical formulas.
 */
public interface SvLibExpression extends AExpression, SvLibAstNode {

  @NonNull
  @Override
  SvLibType getExpressionType();

  <R, X extends Exception> R accept(SvLibExpressionVisitor<R, X> v) throws X;

  @Deprecated // Call accept() directly
  @Override
  default <
          R,
          R1 extends R,
          R2 extends R,
          R3 extends R,
          X1 extends Exception,
          X2 extends Exception,
          X3 extends Exception,
          V extends
              CExpressionVisitor<R1, X1> & JExpressionVisitor<R2, X2>
                  & SvLibExpressionVisitor<R3, X3>>
      R accept_(V pV) throws X3 {
    return accept(pV);
  }
}
