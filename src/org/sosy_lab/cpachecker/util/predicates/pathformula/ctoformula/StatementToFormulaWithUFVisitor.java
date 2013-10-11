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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;


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
    final CType lhsType = PointerTargetSet.simplifyType(lhs.getExpressionType());
    final CType rhsType = rhs != null ? PointerTargetSet.simplifyType(rhs.getExpressionType()) :
      new CSimpleType(true, false, CBasicType.CHAR, false,false, false, false, false, false, false);

    delegate.reset();
    lhs.accept(delegate);
    // addBases(delegate.getSharedBases(), pts);
    addEssentialFields(delegate.getInitializedFields(), pts);
    final ImmutableList<Pair<CCompositeType, String>> lhsUsedFields = ImmutableList.copyOf(delegate.getUsedFields());

    final Object lastTarget = delegate.getLastTarget();
    assert lastTarget instanceof String || lastTarget instanceof Formula;

    final List<Pair<CCompositeType, String>> rhsAddressedFields;
    final Formula rhsFormula;
    if (rhs != null &&
        (!(rhs instanceof CFunctionCallExpression) ||
         !(((CFunctionCallExpression) rhs).getFunctionNameExpression() instanceof CIdExpression) ||
         !isNondetFunctionName(
            ((CIdExpression)((CFunctionCallExpression) rhs).getFunctionNameExpression()).getName()))) {
      delegate.reset();
      rhsFormula = rhs.accept(this);
      // addBases(delegate.getSharedBases(), pts);
      addEssentialFields(delegate.getInitializedFields(), pts);
      rhsAddressedFields = delegate.getAddressedFields();
    } else {
      rhsFormula = null;
      rhsAddressedFields = ImmutableList.<Pair<CCompositeType,String>>of();
    }

    final String rhsName = delegate.getLastTarget() instanceof String ? (String) delegate.getLastTarget() : null;
    final Object rhsObject = !(rhsType instanceof CCompositeType) || rhsName == null ? rhsFormula : rhsName;
    final PointerTargetPattern pattern = lastTarget instanceof String ? null : lhs.accept(lvalueVisitor);
    final BooleanFormula result =
      conv.makeAssignment(lhsType, rhsType, lastTarget, rhsObject, pattern, false, null, ssa, constraints, pts);
    addEssentialFields(lhsUsedFields, pts);
    addEssentialFields(rhsAddressedFields, pts);
    return result;
  }

  private static void addFields(final CCompositeType type, final PointerTargetSetBuilder pts) {
    for (CCompositeTypeMemberDeclaration memberDeclaration : type.getMembers()) {
      pts.addField(type, memberDeclaration.getName());
      final CType memberType = PointerTargetSet.simplifyType(memberDeclaration.getType());
      if (memberType instanceof CCompositeType) {
        addFields((CCompositeType) memberType, pts);
      } else if (memberType instanceof CArrayType) {
        final CType elementType = PointerTargetSet.simplifyType(((CArrayType) memberType).getType());
        if (elementType instanceof CCompositeType) {
          addFields((CCompositeType) elementType, pts);
        }
      }
    }
  }

  public BooleanFormula visitComplexInitialization(final CDeclaration declaration, final List<?> initializerList) {
    final CType type = PointerTargetSet.simplifyType(declaration.getType());
    final String lhs = declaration.getQualifiedName();
    if (!PointerTargetSet.containsArray(type)) {
      return conv.makeAssignment(type, type, lhs, initializerList, null, false, null, ssa, constraints, pts);
    } else {
      final PointerTargetPattern pattern = new PointerTargetPattern(lhs, 0, 0);
      final BooleanFormula result = conv.makeAssignment(type,
                                                        type,
                                                        conv.makeConstant(Variable.create(lhs, type), ssa, pts),
                                                        initializerList,
                                                        pattern,
                                                        false,
                                                        null,
                                                        ssa,
                                                        constraints,
                                                        pts);
      pts.addBase(lhs, type);
      if (type instanceof CCompositeType) {
        addFields((CCompositeType) type, pts);
      }
      return result;
    }
  }

  public BooleanFormula visitAssume(final CExpression e, final boolean truthAssumtion)
  throws UnrecognizedCCodeException {
    delegate.reset();
    BooleanFormula result = conv.toBooleanFormula(e.accept(delegate));
    if (!truthAssumtion) {
      result = conv.bfmgr.not(result);
    }
    // addBases(delegate.getSharedBases(), pts);
    addEssentialFields(delegate.getInitializedFields(), pts);
    addEssentialFields(delegate.getUsedFields(), pts);
    return result;
  }

  public Object visitInitializer(CType type, final CInitializer topInitializer, final boolean isAutomatic)
  throws UnrecognizedCCodeException {
    type = PointerTargetSet.simplifyType(type);
    final CSimpleType integerType =
        new CSimpleType(true, false, CBasicType.CHAR, false, false, true, false, false, false, false);
    final Formula zero = conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(integerType, pts), 0);
    if (type instanceof CArrayType) {
      assert topInitializer instanceof CInitializerList : "Wrong array initializer";
      final CInitializerList initializerList = (CInitializerList) topInitializer;
      final CType elementType = PointerTargetSet.simplifyType(((CArrayType) type).getType());
      final CExpression lengthExpression = ((CArrayType) type).getLength();
      final Integer length;
      if (lengthExpression != null) {
        length = lengthExpression.accept(pts.getEvaluatingVisitor());
      } else {
        length = initializerList.getInitializers().size();
      }
      if (length == null) {
        throw new UnrecognizedCCodeException("Can't evaluate array size for initialization", edge, initializerList);
      }
      final List<Object> result = new ArrayList<>(length);
      for (int i = 0; i < length; ++i) {
        if (isAutomatic) {
          result.add(zero);
        } else {
          result.add(null);
        }
      }
      int index = 0;
      for (final CInitializer initializer : initializerList.getInitializers()) {
        if (!(initializer instanceof CDesignatedInitializer)) {
          result.set(index, visitInitializer(elementType, initializer, isAutomatic));
        } else {
          final CDesignatedInitializer designatedInitializer = (CDesignatedInitializer) initializer;
          final List<CDesignator> designators = designatedInitializer.getDesignators();
          final CDesignator designator;
          if (designators.size() > 1) {
            conv.log(Level.WARNING, "Nested designators are unsupported: " + designatedInitializer + " in line "+
                                    designatedInitializer.getFileLocation().getStartingLineNumber());
            continue;
          } else {
            designator = Iterables.getOnlyElement(designators);
          }
          final Object rhs = visitInitializer(elementType, designatedInitializer.getRightHandSide(), isAutomatic);
          if (designator instanceof CArrayRangeDesignator) {
            final Integer floor = ((CArrayRangeDesignator) designator).getFloorExpression()
                                                                      .accept(pts.getEvaluatingVisitor());
            final Integer ceiling = ((CArrayRangeDesignator) designator).getFloorExpression()
                                                                        .accept(pts.getEvaluatingVisitor());
            if (floor != null && ceiling != null) {
              for (int i = floor; i <= ceiling; i++) {
                result.set(i, rhs);
              }
              index = ceiling;
            } else {
              throw new UnrecognizedCCodeException("Can't evaluate array range designator bounds", edge, designator);
            }
          } else if (designator instanceof CArrayDesignator) {
            final Integer subscript = ((CArrayDesignator) designator).getSubscriptExpression()
                                                                      .accept(pts.getEvaluatingVisitor());
            if (subscript != null) {
              index = subscript;
              result.set(index, rhs);
            } else {
              throw new UnrecognizedCCodeException("Can't evaluate array designator subscript", edge, designator);
            }
          }
        }
        ++index;
      }
      return result;
    } else if (type instanceof CCompositeType && ((CCompositeType) type).getKind() == ComplexTypeKind.STRUCT) {
      assert topInitializer instanceof CInitializerList : "Wrong structure initializer";
      final CInitializerList initializerList = (CInitializerList) topInitializer;
      final CCompositeType compositeType = (CCompositeType) type;
      final int size = compositeType.getMembers().size();
      final Map<String, Pair<Integer, CType>> members = new HashMap<>(size);
      final List<CType> memberTypes = new ArrayList<>(size);
      int index = 0;
      for (final CCompositeTypeMemberDeclaration memberDeclaration : ((CCompositeType) type).getMembers()) {
        final CType memberType = PointerTargetSet.simplifyType(memberDeclaration.getType());
        members.put(memberDeclaration.getName(), Pair.of(index, memberType));
        memberTypes.add(memberType);
        index++;
      }
      final List<Object> result = new ArrayList<>(size);
      for (int i = 0; i < size; ++i) {
        if (isAutomatic) {
          result.add(zero);
        } else {
          result.add(null);
        }
      }
      index = 0;
      for (final CInitializer initializer : initializerList.getInitializers()) {
        if (!(initializer instanceof CDesignatedInitializer)) {
          result.set(index, visitInitializer(memberTypes.get(index), initializer, isAutomatic));
        } else {
          final CDesignatedInitializer designatedInitializer = (CDesignatedInitializer) initializer;
          final List<CDesignator> designators = designatedInitializer.getDesignators();
          final CDesignator designator;
          if (designators.size() > 1) {
            conv.log(Level.WARNING, "Nested designators are unsupported: " + designatedInitializer + " in line "+
                                    designatedInitializer.getFileLocation().getStartingLineNumber());
            continue;
          } else {
            designator = Iterables.getOnlyElement(designators);
          }
          if (designator instanceof CFieldDesignator) {

            final Pair<Integer, CType> indexType = members.get(((CFieldDesignator) designator).getFieldName());
            final Object rhs = visitInitializer(indexType.getSecond(),
                                                designatedInitializer.getRightHandSide(),
                                                isAutomatic);
            result.set(indexType.getFirst(), rhs);
          } else {
            throw new UnrecognizedCCodeException("Wrong designator", edge, designator);
          }
        }
        index++;
      }
      return result;
    } else {
      assert topInitializer instanceof CInitializerExpression : "Unrecognized initializer";
      return ((CInitializerExpression) topInitializer).getExpression().accept(this);
    }
  }

  private static CType getSizeofType(CExpression e) {
    if (e instanceof CUnaryExpression &&
        ((CUnaryExpression) e).getOperator() == UnaryOperator.SIZEOF) {
      return PointerTargetSet.simplifyType(((CUnaryExpression) e).getOperand().getExpressionType());
    } else if (e instanceof CTypeIdExpression &&
               ((CTypeIdExpression) e).getOperator() == TypeIdOperator.SIZEOF) {
      return PointerTargetSet.simplifyType(((CTypeIdExpression) e).getType());
    } else {
      return null;
    }
  }

  @Override
  public Formula visit(final CFunctionCallExpression e) throws UnrecognizedCCodeException {
    final CExpression functionNameExpression = e.getFunctionNameExpression();
    final CType resultType = PointerTargetSet.simplifyType(e.getExpressionType());
    final List<CExpression> parameters = e.getParameterExpressions();
    if (functionNameExpression instanceof CIdExpression) {
      final String functionName = ((CIdExpression) functionNameExpression).getName();
      if (functionName.equals(CToFormulaWithUFConverter.ASSUME_FUNCTION_NAME) && parameters.size() == 1) {
        final BooleanFormula condition = visitAssume(parameters.get(0), true);
        constraints.addConstraint(condition);
        return conv.makeFreshVariable(functionName, resultType, ssa, pts);
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
        final Formula result = conv.makeConstant(Variable.create(newBaseName, newBaseType), ssa, pts);
        if (functionName.equals(conv.successfulZallocFunctionName)) {
          final CSimpleType integerType =
            new CSimpleType(true, false, CBasicType.CHAR, false, false, true, false, false, false, false);
          final BooleanFormula initialization = conv.makeAssignment(
              newBaseType,
              integerType,
              result,
              conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(integerType, pts), 0),
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

  public String getFuncitonName() {
    return function;
  }

  public void forceShared(final CDeclaration declaration) {
    pts.addBase(declaration.getQualifiedName(), declaration.getType());
  }

  public void forceShared(final CParameterDeclaration declaration) {
    pts.addBase(declaration.getQualifiedName(), declaration.getType());
  }

  public void declareCompositeType(final CCompositeType compositeType) {
    pts.addCompositeType(compositeType);
  }

  @SuppressWarnings("hiding")
  protected final CToFormulaWithUFConverter conv;
  protected final PointerTargetSetBuilder pts;
  @SuppressWarnings("hiding")
  protected final ExpressionToFormulaWithUFVisitor delegate;
  protected final LvalueToPointerTargetPatternVisitor lvalueVisitor;
}
