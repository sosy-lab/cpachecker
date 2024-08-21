// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public interface ACSLPredicateVisitor<R, X extends Exception> {

  R visitTrue() throws X;

  R visitFalse() throws X;

  R visit(ACSLSimplePredicate pred) throws X;

  R visit(ACSLLogicalPredicate pred) throws X;

  R visit(ACSLTernaryCondition pred) throws X;

  R visit(PredicateAt pred) throws X;
}
