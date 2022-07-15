// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.variableclassification;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.variableclassification.VariableAndFieldRelevancyComputer.VarFieldDependencies;

final class CollectingLHSVisitor
    extends DefaultCExpressionVisitor<Pair<VariableOrField, VarFieldDependencies>, NoException> {

  private final CFA cfa;

  private CollectingLHSVisitor(CFA pCfa) {
    cfa = checkNotNull(pCfa);
  }

  public static CollectingLHSVisitor create(CFA pCfa) {
    return new CollectingLHSVisitor(pCfa);
  }

  @Override
  public Pair<VariableOrField, VarFieldDependencies> visit(final CArraySubscriptExpression e) {
    final Pair<VariableOrField, VarFieldDependencies> r = e.getArrayExpression().accept(this);
    return Pair.of(
        r.getFirst(),
        r.getSecond()
            .withDependencies(
                e.getSubscriptExpression().accept(CollectingRHSVisitor.create(cfa, r.getFirst()))));
  }

  @Override
  public Pair<VariableOrField, VarFieldDependencies> visit(final CFieldReference e) {
    final VariableOrField result =
        VariableOrField.newField(
            VariableAndFieldRelevancyComputer.getCanonicalFieldOwnerType(e), e.getFieldName());
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Pair.of(result, e.getFieldOwner().accept(CollectingRHSVisitor.create(cfa, result)));
  }

  @Override
  public Pair<VariableOrField, VarFieldDependencies> visit(final CPointerExpression e) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Pair.of(
        VariableOrField.unknown(),
        e.getOperand().accept(CollectingRHSVisitor.create(cfa, VariableOrField.unknown())));
  }

  @Override
  public Pair<VariableOrField, VarFieldDependencies> visit(final CComplexCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public Pair<VariableOrField, VarFieldDependencies> visit(final CCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public Pair<VariableOrField, VarFieldDependencies> visit(final CIdExpression e) {
    return Pair.of(
        VariableOrField.newVariable(e.getDeclaration().getQualifiedName()),
        VarFieldDependencies.emptyDependencies());
  }

  @Override
  protected Pair<VariableOrField, VarFieldDependencies> visitDefault(final CExpression e) {
    if (e instanceof CUnaryExpression
        && UnaryOperator.AMPER == ((CUnaryExpression) e).getOperator()) {
      // TODO dependency between address and variable?
      return ((CUnaryExpression) e).getOperand().accept(this);
    }

    throw new AssertionError(
        String.format(
            "The expression %s from %s should not occur in the left hand side",
            e, e.getFileLocation()));
  }
}
