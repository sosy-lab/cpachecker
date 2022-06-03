// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsrcd;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;

public class VariableIdentifierVisitor extends DefaultCExpressionVisitor<Set<String>, NoException> {

  @Override
  protected Set<String> visitDefault(final CExpression pExp) throws NoException {
    return new HashSet<>();
  }

  @Override
  public Set<String> visit(final CArraySubscriptExpression pE) throws NoException {
    Set<String> resultSet = pE.getArrayExpression().accept(this);
    resultSet.addAll(pE.getSubscriptExpression().accept(this));
    return resultSet;
  }

  @Override
  public Set<String> visit(final CBinaryExpression pE) throws NoException {
    Set<String> resultSet = pE.getOperand1().accept(this);
    resultSet.addAll(pE.getOperand2().accept(this));
    return resultSet;
  }

  @Override
  public Set<String> visit(final CCastExpression pE) throws NoException {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(final CComplexCastExpression pE) throws NoException {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(final CFieldReference pE) throws NoException {
    return pE.getFieldOwner().accept(this);
  }

  @Override
  public Set<String> visit(final CIdExpression pE) throws NoException {
    return Sets.newHashSet(pE.getDeclaration().getQualifiedName());
  }

  @Override
  public Set<String> visit(final CUnaryExpression pE) throws NoException {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(final CPointerExpression pE) throws NoException {
    return pE.getOperand().accept(this);
  }
}
