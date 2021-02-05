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
  protected Set<String> visitDefault(CExpression pExp) throws NoException {
    return new HashSet<>();
  }

  @Override
  public Set<String> visit(CArraySubscriptExpression pE) throws NoException {

    Set<String> set1 = pE.getArrayExpression().accept(this);
    Set<String> set2 = pE.getSubscriptExpression().accept(this);
    set1.addAll(set2);
    return set1;
  }

  @Override
  public Set<String> visit(CBinaryExpression pE) throws NoException {
    Set<String> set1 = pE.getOperand1().accept(this);
    Set<String> set2 = pE.getOperand2().accept(this);
    set1.addAll(set2);
    return set1;
  }

  @Override
  public Set<String> visit(CCastExpression pE) throws NoException {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(CComplexCastExpression pE) throws NoException {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(CFieldReference pE) throws NoException {
    Set<String> set = pE.getFieldOwner().accept(this);
    set.add(pE.getFieldName()); // TODO: what is this exactly?
    return set;
  }

  @Override
  public Set<String> visit(CIdExpression pE) throws NoException {
    return Sets.newHashSet(pE.getDeclaration().getQualifiedName());
  }

  @Override
  public Set<String> visit(CUnaryExpression pE) throws NoException {
    return pE.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(CPointerExpression pE) throws NoException {
    return pE.getOperand().accept(this);
  }
}
