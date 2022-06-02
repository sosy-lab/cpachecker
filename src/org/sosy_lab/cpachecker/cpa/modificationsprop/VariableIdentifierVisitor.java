// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.modificationsprop;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;

/** Provides a set of variable names used. */
public class VariableIdentifierVisitor
    extends DefaultCExpressionVisitor<Set<String>, PointerAccessException>
    implements CRightHandSideVisitor<Set<String>, PointerAccessException>,
        CLeftHandSideVisitor<Set<String>, PointerAccessException> {

  private final boolean disallowPointers;

  public VariableIdentifierVisitor(boolean pDisallowPointers) {
    disallowPointers = pDisallowPointers;
  }

  @Override
  public Set<String> visit(CFunctionCallExpression pExp) throws PointerAccessException {
    Set<String> resultSet = pExp.getFunctionNameExpression().accept(this); // TODO does this work?
    for (CExpression exp : pExp.getParameterExpressions()) {
      resultSet.addAll(exp.accept(this));
    }
    return resultSet;
  }

  @Override
  protected Set<String> visitDefault(final CExpression pExp) throws PointerAccessException {
    return new HashSet<>();
  }

  // We leave this exception-less for now, as we usually do not expect problems here.
  @Override
  public Set<String> visit(final CArraySubscriptExpression pExp) throws PointerAccessException {
    Set<String> resultSet = pExp.getArrayExpression().accept(this);
    resultSet.addAll(pExp.getSubscriptExpression().accept(this));
    return resultSet;
  }

  @Override
  public Set<String> visit(final CBinaryExpression pExp) throws PointerAccessException {
    Set<String> resultSet = pExp.getOperand1().accept(this);
    resultSet.addAll(pExp.getOperand2().accept(this));
    return resultSet;
  }

  @Override
  public Set<String> visit(final CCastExpression pExp) throws PointerAccessException {
    return pExp.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(final CComplexCastExpression pExp) throws PointerAccessException {
    return pExp.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(final CFieldReference pExp) throws PointerAccessException {
    if (disallowPointers
        && (pExp.isPointerDereference()
            || pExp.getFieldOwner().getExpressionType().getCanonicalType()
                instanceof CPointerType)) {
      throw new PointerAccessException();
    } else {
      return pExp.getFieldOwner().accept(this);
    }
  }

  @Override
  public Set<String> visit(final CIdExpression pExp) throws PointerAccessException {
    if (pExp.getDeclaration() != null) {
      return Sets.newHashSet(pExp.getDeclaration().getQualifiedName());
    }
    return Sets.newHashSet(pExp.getName());
  }

  @Override
  public Set<String> visit(final CUnaryExpression pExp) throws PointerAccessException {
    return pExp.getOperand().accept(this);
  }

  @Override
  public Set<String> visit(final CPointerExpression pExp) throws PointerAccessException {
    if (disallowPointers) {
      throw new PointerAccessException();
    } else {
      return pExp.getOperand().accept(this);
    }
  }
}
