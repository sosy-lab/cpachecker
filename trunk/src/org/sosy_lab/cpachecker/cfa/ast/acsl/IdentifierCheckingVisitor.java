// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * Visitor that checks whether used identifiers have been declared in the scope they are used in.
 */
public class IdentifierCheckingVisitor
    implements ACSLTermVisitor<Boolean, NoException>, ACSLPredicateVisitor<Boolean, NoException> {

  private final CFA cfa;
  private final LogManager logger;

  public IdentifierCheckingVisitor(CFA pCfa, LogManager pLogger) {
    cfa = pCfa;
    logger = pLogger;
  }

  @Override
  public Boolean visitTrue() {
    return true;
  }

  @Override
  public Boolean visitFalse() {
    return true;
  }

  @Override
  public Boolean visit(ACSLSimplePredicate pred) {
    return pred.getTerm().accept(this);
  }

  @Override
  public Boolean visit(ACSLLogicalPredicate pred) {
    return pred.getLeft().accept(this) && pred.getRight().accept(this);
  }

  @Override
  public Boolean visit(ACSLTernaryCondition pred) {
    return pred.getCondition().accept(this)
        && pred.getThen().accept(this)
        && pred.getOtherwise().accept(this);
  }

  @Override
  public Boolean visit(PredicateAt pred) {
    // TODO: Or should this somehow find the scope which the at refers to?
    return pred.getInner().accept(this);
  }

  @Override
  public Boolean visit(ACSLBinaryTerm term) {
    return term.getLeft().accept(this) && term.getRight().accept(this);
  }

  @Override
  public Boolean visit(ACSLUnaryTerm term) {
    return term.getInnerTerm().accept(this);
  }

  @Override
  public Boolean visit(ACSLArrayAccess term) {
    return term.getArray().accept(this) && term.getIndex().accept(this);
  }

  @Override
  public Boolean visit(TermAt term) {
    // TODO: Or should this somehow find the scope which the at refers to?
    return term.getInner().accept(this);
  }

  @Override
  public Boolean visit(ACSLResult term) {
    return cfa.getFunctionHead(term.getFunctionName()).getReturnVariable().isPresent();
  }

  @Override
  public Boolean visit(ACSLCast term) {
    return term.getTerm().accept(this);
  }

  @Override
  public Boolean visit(BoundIdentifier term) {
    return true;
  }

  @Override
  public Boolean visit(ACSLIdentifier term) {
    CProgramScope scope = new CProgramScope(cfa, logger);
    CSimpleDeclaration variableDeclaration = scope.lookupVariable(term.getName());
    return variableDeclaration != null;
  }

  @Override
  public Boolean visit(ACSLIntegerLiteral term) {
    return true;
  }

  @Override
  public Boolean visit(ACSLStringLiteral term) {
    return true;
  }
}
