// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
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
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.FunctionIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.IdentifierCreator;
import org.sosy_lab.cpachecker.util.identifiers.LocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.StructureIdentifier;

public class ExpressionHandler extends DefaultCExpressionVisitor<Void, NoException> {

  private final List<Pair<AbstractIdentifier, Access>> result;
  private final String fName;
  private Access accessMode;
  private VariableSkipper varSkipper;
  private final IdentifierCreator idCreator;

  public ExpressionHandler(
      Access mode,
      String functionName,
      VariableSkipper pSkipper,
      IdentifierCreator pIdCreator) {
    result = new ArrayList<>();
    accessMode = mode;
    fName = functionName;
    varSkipper = pSkipper;
    idCreator = pIdCreator;
  }

  @Override
  public Void visit(CArraySubscriptExpression expression) {
    addExpression(expression);
    accessMode = Access.READ;
    expression.getArrayExpression().accept(this);
    return null;
  }

  @Override
  public Void visit(CBinaryExpression expression) {
    checkArgument(
        accessMode == Access.READ, "Writing to BinaryExpression: %s", expression.toASTString());
    expression.getOperand1().accept(this);
    expression.getOperand2().accept(this);
    return null;
  }

  @Override
  public Void visit(CCastExpression expression) {
    expression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CFieldReference expression) {
    addExpression(expression);
    if (expression.isPointerDereference()) {
      accessMode = Access.READ;
      expression.getFieldOwner().accept(this);
    }
    return null;
  }

  @Override
  public Void visit(CIdExpression expression) {
    addExpression(expression);
    return null;
  }

  @Override
  public Void visit(CUnaryExpression expression) {
    if (expression.getOperator() == CUnaryExpression.UnaryOperator.AMPER) {
      addExpression(expression);
      return null;
    }
    // In all other unary operation we only read the operand
    accessMode = Access.READ;
    expression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CPointerExpression pPointerExpression) {
    // write: *s =
    addExpression(pPointerExpression);
    accessMode = Access.READ;
    pPointerExpression.getOperand().accept(this);
    return null;
  }

  @Override
  public Void visit(CComplexCastExpression pComplexCastExpression) {
    pComplexCastExpression.getOperand().accept(this);
    return null;
  }

  private void addExpression(CExpression e) {
    AbstractIdentifier id = idCreator.createIdentifier(e, 0);
    if (isRelevantForAnalysis(id)) {
      result.add(Pair.of(id, accessMode));
    }
  }

  public List<Pair<AbstractIdentifier, Access>> getProcessedExpressions() {
    return result;
  }

  private boolean isRelevantForAnalysis(AbstractIdentifier pId) {
    if (varSkipper.shouldBeSkipped(pId, fName)) {
      return false;
    }

    if (pId instanceof LocalVariableIdentifier && pId.getDereference() <= 0) {
      // we don't save in statistics ordinary local variables
      return false;
    }
    if (pId instanceof StructureIdentifier && !pId.isGlobal() && !pId.isDereferenced()) {
      // skips such cases, as 'a.b'
      return false;
    }

    if (pId instanceof FunctionIdentifier) {
      return false;
    }

    return true;
  }

  @Override
  protected Void visitDefault(CExpression pExp) {
    return null;
  }
}
