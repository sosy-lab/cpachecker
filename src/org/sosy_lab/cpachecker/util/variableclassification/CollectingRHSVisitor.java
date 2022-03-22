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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.variableclassification.VariableAndFieldRelevancyComputer.VarFieldDependencies;

final class CollectingRHSVisitor
    extends DefaultCExpressionVisitor<VarFieldDependencies, NoException>
    implements CRightHandSideVisitor<VarFieldDependencies, NoException> {

  private final CFA cfa;
  private final VariableOrField lhs;
  private final boolean addressed;

  private CollectingRHSVisitor(final CFA pCfa, final VariableOrField lhs, final boolean addressed) {
    cfa = checkNotNull(pCfa);
    this.lhs = checkNotNull(lhs);
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
      // For unions, we add a dependency on all fields, because writes to all of them are relevant.
      // Also, all (nested) members of CCompositeType members of a union are relevant
      result = addNestedDependenciesAsNecessary(result, ownerType);
    } else {
      result = result.withDependency(lhs, field);
    }

    if (addressed) {
      return result.withAddressedField(field);
    } else {
      return result;
    }
  }

  private VarFieldDependencies addNestedDependenciesAsNecessary(
      VarFieldDependencies pResult, CCompositeType pType) {
    VarFieldDependencies result = pResult;
    for (CCompositeTypeMemberDeclaration member : pType.getMembers()) {
      result = result.withDependency(lhs, VariableOrField.newField(pType, member.getName()));
      // Inner composite members might be CElaboratedType and have to be unboxed to be handle them
      // well
      CType memberType = TypeUtils.getRealExpressionType(member.getType());
      if (memberType instanceof CCompositeType) {
        result = addNestedDependenciesAsNecessary(result, (CCompositeType) memberType);
      }
    }

    return result;
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
    checkNotNull(e);
    return VarFieldDependencies.emptyDependencies();
  }
}
