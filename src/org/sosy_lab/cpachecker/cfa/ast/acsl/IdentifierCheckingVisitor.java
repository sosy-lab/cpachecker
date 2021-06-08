// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Visitor that checks whether used identifiers have been declared in the scope they are used in.
 */
public class IdentifierCheckingVisitor implements ACSLPredicateVisitor<Boolean, UnrecognizedCodeException> {

  ACSLTermToCExpressionVisitor visitor;

  public IdentifierCheckingVisitor(ACSLTermToCExpressionVisitor pVisitor) {
    visitor = pVisitor;
  }

  @Override
  public Boolean visitTrue() throws UnrecognizedCodeException {
    return true;
  }

  @Override
  public Boolean visitFalse() throws UnrecognizedCodeException {
    return true;
  }

  @Override
  public Boolean visit(ACSLSimplePredicate pred) throws UnrecognizedCodeException {
    try {
      pred.getTerm().accept(visitor);
      return true;
    } catch (AssertionError e) {
      return false;
    }
  }

  @Override
  public Boolean visit(ACSLLogicalPredicate pred) throws UnrecognizedCodeException {
    return pred.getLeft().accept(this) && pred.getRight().accept(this);
  }

  @Override
  public Boolean visit(ACSLTernaryCondition pred) throws UnrecognizedCodeException {
    return pred.getCondition().accept(this) && pred.getThen().accept(this) && pred.getOtherwise().accept(this);
  }

  @Override
  public Boolean visit(PredicateAt pred) throws UnrecognizedCodeException {
    // TODO: Or should this somehow find the scope which the at refers to?
    return pred.getInner().accept(this);
  }
}
