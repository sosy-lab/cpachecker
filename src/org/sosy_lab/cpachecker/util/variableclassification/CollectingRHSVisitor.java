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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.util.variableclassification.VariableAndFieldRelevancyComputer.VarFieldDependencies;

final class CollectingRHSVisitor
    extends DefaultCExpressionVisitor<VarFieldDependencies, RuntimeException>
    implements CRightHandSideVisitor<VarFieldDependencies, RuntimeException> {

  private final CFA cfa;
  private final VariableOrField lhs;
  private final boolean addressed;

  private CollectingRHSVisitor(final CFA pCfa, final VariableOrField lhs, final boolean addressed) {
    this.cfa = pCfa;
    this.lhs = lhs;
    this.addressed = addressed;
  }

  public static CollectingRHSVisitor create(final CFA pCfa, final VariableOrField lhs) {
    return new CollectingRHSVisitor(pCfa, lhs, false);
  }

  private CollectingRHSVisitor createAddressed() {
    return new CollectingRHSVisitor(cfa, lhs, true);
  }

  @Override
  public VarFieldDependencies visit(final CArraySubscriptExpression e) {
    return e.getSubscriptExpression()
        .accept(this)
        .withDependencies(e.getArrayExpression().accept(this));
  }

  @Override
  public VarFieldDependencies visit(final CFieldReference e) {
    CCompositeType ownerType = VariableAndFieldRelevancyComputer.getCanonicalFieldOwnerType(e);
    VariableOrField.Field field = VariableOrField.newField(ownerType, e.getFieldName());
    VarFieldDependencies result = e.getFieldOwner().accept(this);

    if (ownerType.getKind() == ComplexTypeKind.UNION) {
      // For unions, we add a dependency on all fields, because writes to all of them are relevant
      for (CCompositeTypeMemberDeclaration member : ownerType.getMembers()) {
        result = result.withDependency(lhs, VariableOrField.newField(ownerType, member.getName()));
      }
    } else {
      result = result.withDependency(lhs, field);
    }

    if (addressed) {
      return result.withAddressedField(field);
    } else {
      return result;
    }
  }

  @Override
  public VarFieldDependencies visit(final CBinaryExpression e) {
    return e.getOperand1().accept(this).withDependencies(e.getOperand2().accept(this));
  }

  @Override
  public VarFieldDependencies visit(final CUnaryExpression e) {
    if (e.getOperator() != UnaryOperator.AMPER) {
      return e.getOperand().accept(this);
    } else {
      return e.getOperand().accept(createAddressed());
    }
  }

  @Override
  public VarFieldDependencies visit(final CPointerExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public VarFieldDependencies visit(final CComplexCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public VarFieldDependencies visit(final CCastExpression e) {
    return e.getOperand().accept(this);
  }

  @Override
  public VarFieldDependencies visit(final CIdExpression e) {
    final CSimpleDeclaration decl = e.getDeclaration();
    final VariableOrField.Variable variable =
        VariableOrField.newVariable(decl != null ? decl.getQualifiedName() : e.getName());
    final VarFieldDependencies result =
        VarFieldDependencies.emptyDependencies().withDependency(lhs, variable);
    if (addressed) {
      return result.withAddressedVariable(variable);
    }
    return result;
  }

  @Override
  public VarFieldDependencies visit(CFunctionCallExpression e) {
    VarFieldDependencies result = VarFieldDependencies.emptyDependencies();
    CExpression functionNameExpression = e.getFunctionNameExpression();
    if (functionNameExpression instanceof CIdExpression) {
      CIdExpression idExpression = (CIdExpression) functionNameExpression;
      if (cfa.getAllFunctionNames().contains(idExpression.getName())) {
        result = functionNameExpression.accept(this);
      }
    } else {
      result = functionNameExpression.accept(this);
    }
    for (CExpression param : e.getParameterExpressions()) {
      result = result.withDependencies(param.accept(this));
    }
    return result;
  }

  @Override
  protected VarFieldDependencies visitDefault(final CExpression e) {
    return VarFieldDependencies.emptyDependencies();
  }
}
