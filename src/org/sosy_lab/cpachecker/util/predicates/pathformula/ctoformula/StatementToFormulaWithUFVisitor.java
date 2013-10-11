/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;

import com.google.common.collect.ImmutableList;


public class StatementToFormulaWithUFVisitor extends StatementToFormulaVisitor {

  public StatementToFormulaWithUFVisitor(final ExpressionToFormulaWithUFVisitor delegate,
                                         final LvalueToPointerTargetPatternVisitor lvalueVisitor) {
    super(delegate);
    this.delegate = delegate;
    this.lvalueVisitor = lvalueVisitor;
    this.conv = delegate.conv;
    this.pts = delegate.pts;
  }

  @Override
  public BooleanFormula visit(final CExpressionAssignmentStatement e) throws UnrecognizedCCodeException {
    return visit((CAssignment) e);
  }

  @Override
  public BooleanFormula visit(final CFunctionCallAssignmentStatement e) throws UnrecognizedCCodeException {
    return visit((CAssignment) e);
  }

  private static void addBases(final List<Pair<String, CType>> bases, final PointerTargetSetBuilder pts) {
    for (final Pair<String, CType> base : bases) {
      pts.addBase(base.getFirst(), base.getSecond());
    }
  }

  private static void addFields(final List<Pair<CCompositeType, String>> fields, final PointerTargetSetBuilder pts) {
    for (final Pair<CCompositeType, String> field : fields) {
      pts.addField(field.getFirst(), field.getSecond());
    }
  }

  private static void addEssentialFields(final List<Pair<CCompositeType, String>> fields,
                                         final PointerTargetSetBuilder pts) {
    for (final Pair<CCompositeType, String> field : fields) {
      if (!pts.addField(field.getFirst(), field.getSecond())) {
        pts.shallowRemoveField(field.getFirst(), field.getSecond());
      }
    }
  }

  private boolean isNondetFunctionName(final String name) {
    return conv.nondetFunctions.contains(name) || conv.nondetFunctionsPattern.matcher(name).matches();
  }

  @Override
  public BooleanFormula visit(final CAssignment e) throws UnrecognizedCCodeException {
    final CRightHandSide rhs = e.getRightHandSide();
    final CExpression lhs = e.getLeftHandSide();
    final CType lhsType = lhs.getExpressionType();
    final CType rhsType = rhs.getExpressionType();

    delegate.reset();
    lhs.accept(delegate);
    addBases(delegate.getSharedBases(), pts);
    addEssentialFields(delegate.getInitializedFields(), pts);
    final List<Pair<CCompositeType, String>> lhsUsedFields = ImmutableList.copyOf(delegate.getUsedFields());

    final Object lastTarget = delegate.getLastTarget();
    assert lastTarget instanceof String || lastTarget instanceof Formula;

    final Formula rhsFormula;
    if (!(rhs instanceof CFunctionCallExpression) ||
        !(((CFunctionCallExpression) rhs).getFunctionNameExpression() instanceof CIdExpression) ||
        !isNondetFunctionName(((CIdExpression)((CFunctionCallExpression) rhs).getFunctionNameExpression()).getName())) {
      delegate.reset();
      rhsFormula = rhs.accept(this);
      addBases(delegate.getSharedBases(), pts);
      addEssentialFields(delegate.getInitializedFields(), pts);
    } else {
      rhsFormula = null;
    }

    final String rhsName = delegate.getLastTarget() instanceof String ? (String) delegate.getLastTarget() : null;
    final Object rhsObject = !(rhsType instanceof CCompositeType) || rhsName == null ? rhsFormula : rhsName;
    final PointerTargetPattern pattern = lastTarget instanceof String ? null : lhs.accept(lvalueVisitor);
    final BooleanFormula result =
      conv.makeAssignment(lhsType, rhsType, lastTarget, rhsObject, pattern, false, null, ssa, constraints, pts);
    addEssentialFields(lhsUsedFields, pts);
    return result;
  }

  public BooleanFormula visitAssume(final CExpression e) throws UnrecognizedCCodeException {
    delegate.reset();
    final BooleanFormula result = conv.toBooleanFormula(e.accept(delegate));
    addBases(delegate.getSharedBases(), pts);
    addEssentialFields(delegate.getInitializedFields(), pts);
    addEssentialFields(delegate.getUsedFields(), pts);
    return result;
  }

  private static CType getSizeofType(CExpression e) {
    if (e instanceof CUnaryExpression &&
        ((CUnaryExpression) e).getOperator() == UnaryOperator.SIZEOF) {
      return ((CUnaryExpression) e).getOperand().getExpressionType();
    } else if (e instanceof CTypeIdExpression &&
               ((CTypeIdExpression) e).getOperator() == TypeIdOperator.SIZEOF) {
      return ((CTypeIdExpression) e).getType();
    } else {
      return null;
    }
  }

  @Override
  public Formula visit(final CFunctionCallExpression e) throws UnrecognizedCCodeException {
    final CExpression functionNameExpression = e.getFunctionNameExpression();
    final CType resultType = e.getExpressionType();
    final List<CExpression> parameters = e.getParameterExpressions();
    if (functionNameExpression instanceof CIdExpression) {
      final String functionName = ((CIdExpression) functionNameExpression).getName();
      if (functionName.equals(CToFormulaWithUFConverter.ASSUME_FUNCTION_NAME) && parameters.size() == 1) {
        final BooleanFormula condition = visitAssume(parameters.get(0));
        constraints.addConstraint(condition);
        return conv.makeFreshVariable(functionName, resultType, ssa);
      } else if ((functionName.equals(conv.successfulAllocFunctionName) ||
                  functionName.equals(conv.successfulZallocFunctionName)) &&
                  parameters.size() == 1) {
        final CExpression parameter = parameters.get(0);
        final CType newBaseType;
        if (parameter instanceof CUnaryExpression) {
          newBaseType = getSizeofType(parameter);
        } else if (parameter instanceof CBinaryExpression &&
                   ((CBinaryExpression) parameter).getOperator() == BinaryOperator.MULTIPLY) {
          final CBinaryExpression product = (CBinaryExpression) parameter;
          final CType operand1Type = getSizeofType(product.getOperand1());
          final CType operand2Type = getSizeofType(product.getOperand2());
          if (operand1Type != null) {
            newBaseType = new CArrayType(false, false, operand1Type, product.getOperand2());
          } else if (operand2Type != null) {
            newBaseType = new CArrayType(false, false, operand2Type, product.getOperand1());
          } else {
            throw new UnrecognizedCCodeException("Can't determine type for internal memory allocation", edge, e);
          }
        } else {
          throw new UnrecognizedCCodeException("Can't determine type for internal memory allocation", edge, e);
        }
        final String newBaseName = FormulaManagerView.makeName(functionName,
                                                      conv.makeFreshIndex(functionName,
                                                                          new CPointerType(true, false, newBaseType),
                                                                          ssa));
        final Formula result = conv.makeConstant(Variable.create(newBaseName, newBaseType), ssa);
        if (functionName.equals(conv.successfulZallocFunctionName)) {
          final CSimpleType integerType =
            new CSimpleType(true, false, CBasicType.CHAR, false, false, true, false, false, false, false);
          final BooleanFormula initialization = conv.makeAssignment(
              newBaseType,
              integerType,
              result,
              conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(integerType), 0),
              new PointerTargetPattern(newBaseName, 0, 0),
              false,
              null,
              ssa,
              constraints,
              pts);
          constraints.addConstraint(initialization);
        }
        pts.addBase(newBaseName, newBaseType);
        return result;
      } else {
        return super.visit(e);
      }
    } else {
      return super.visit(e);
    }
  }

  @SuppressWarnings("hiding")
  protected final CToFormulaWithUFConverter conv;
  protected final PointerTargetSetBuilder pts;
  @SuppressWarnings("hiding")
  protected final ExpressionToFormulaWithUFVisitor delegate;
  protected final LvalueToPointerTargetPatternVisitor lvalueVisitor;
}
