/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.variableclassification;

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
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.variableclassification.VariableAndFieldRelevancyComputer.VarFieldDependencies;

final class CollectingLHSVisitor
    extends DefaultCExpressionVisitor<
        Pair<VariableOrField, VarFieldDependencies>, RuntimeException> {

  private final CFA cfa;

  private CollectingLHSVisitor(CFA pCfa) {
    cfa = pCfa;
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
    return Pair.of(
        result,
        e.getFieldOwner()
            .<VarFieldDependencies, RuntimeException>accept(
                CollectingRHSVisitor.create(cfa, result)));
  }

  @Override
  public Pair<VariableOrField, VarFieldDependencies> visit(final CPointerExpression e) {
    // Do not remove explicit type inference, otherwise build fails with IntelliJ
    return Pair.of(
        VariableOrField.unknown(),
        e.getOperand()
            .<VarFieldDependencies, RuntimeException>accept(
                CollectingRHSVisitor.create(cfa, VariableOrField.unknown())));
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
