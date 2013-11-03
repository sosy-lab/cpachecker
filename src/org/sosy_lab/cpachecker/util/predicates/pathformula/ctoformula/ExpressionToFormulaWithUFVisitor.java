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

import org.eclipse.cdt.internal.core.dom.parser.c.CFunctionType;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ExpressionToFormulaWithUFVisitor extends ExpressionToFormulaVisitor {

  public ExpressionToFormulaWithUFVisitor(final CToFormulaWithUFConverter cToFormulaConverter,
                                          final CFAEdge cfaEdge,
                                          final String function,
                                          final SSAMapBuilder ssa,
                                          final Constraints constraints,
                                          final PointerTargetSetBuilder pts) {

    super(cToFormulaConverter, cfaEdge, function, ssa, constraints);

    this.conv = cToFormulaConverter;
    this.pts = pts;

    this.baseVisitor = new BaseVisitor(conv, cfaEdge, pts);
  }

  @Override
  public Formula visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    final Formula base = e.getArrayExpression().accept(this);
    final CExpression subscript = e.getSubscriptExpression();
    final CType subscriptType = PointerTargetSet.simplifyType(subscript.getExpressionType());
    final Formula index = conv.makeCast(subscriptType, CPointerType.POINTER_TO_VOID, subscript.accept(this), edge);
    final CType resultType = PointerTargetSet.simplifyType(e.getExpressionType());
    final int size = pts.getSize(resultType);
    final Formula coeff = conv.fmgr.makeNumber(conv.pointerType, size);
    final Formula offset = conv.fmgr.makeMultiply(coeff, index);
    lastTarget = conv.fmgr.makePlus(base, offset);
    return conv.isCompositeType(resultType) ? (Formula) lastTarget :
           conv.makeDereferece(resultType, (Formula) lastTarget, ssa, pts);
  }

  static CFieldReference eliminateArrow(final CFieldReference e, final CFAEdge edge)
  throws UnrecognizedCCodeException {
    if (e.isPointerDereference()) {
      final CType fieldOwnerType = PointerTargetSet.simplifyType(e.getFieldOwner().getExpressionType());
      if (fieldOwnerType instanceof CPointerType) {
        return new CFieldReference(e.getFileLocation(),
                                   e.getExpressionType(),
                                   e.getFieldName(),
                                   new CPointerExpression(e.getFieldOwner().getFileLocation(),
                                                          ((CPointerType) fieldOwnerType).getType(),
                                                          e.getFieldOwner()),
                                   false);
      } else {
        throw new UnrecognizedCCodeException("Can't dereference a non-pointer in the field reference", edge, e);
      }
    } else {
      return e;
    }
  }

  @Override
  public Formula visit(CFieldReference e) throws UnrecognizedCCodeException {

    e = eliminateArrow(e, edge);

    final Variable variable = e.accept(baseVisitor);
    final CType resultType = PointerTargetSet.simplifyType(e.getExpressionType());
    if (variable != null) {
      final String variableName = variable.getName();
      lastTarget = variableName;
      if (pts.isDeferredAllocationPointer(variableName)) {
        usedDeferredAllocationPointers.put(variableName, CPointerType.POINTER_TO_VOID);
      }
      return conv.makeVariable(variable, ssa, pts);
    } else {
      final CType fieldOwnerType = PointerTargetSet.simplifyType(e.getFieldOwner().getExpressionType());
      if (fieldOwnerType instanceof CCompositeType) {
        final Formula base = e.getFieldOwner().accept(this);
        final String fieldName = e.getFieldName();
        usedFields.add(Pair.of((CCompositeType) fieldOwnerType, fieldName));
        final Formula offset = conv.fmgr.makeNumber(conv.pointerType,
                                                    pts.getOffset((CCompositeType) fieldOwnerType, fieldName));
        lastTarget = conv.fmgr.makePlus(base, offset);
        return conv.isCompositeType(resultType) ? (Formula) lastTarget :
               conv.makeDereferece(resultType, (Formula) lastTarget, ssa, pts);
      } else {
        throw new UnrecognizedCCodeException("Field owner of a non-composite type", edge, e);
      }
    }
  }

  static boolean isSimpleTarget(final CExpression e) {
    if (e instanceof CIdExpression) {
      return true;
    } else if (e instanceof CFieldReference) {
      return isSimpleTarget(((CFieldReference) e).getFieldOwner());
    } else {
      return false;
    }
  }

  static boolean isRevealingType(final CType type) {
    return (type instanceof CPointerType || type instanceof CArrayType) &&
           !type.equals(CPointerType.POINTER_TO_VOID);
  }

  @Override
  public Formula visit(final CCastExpression e) throws UnrecognizedCCodeException {
    final CExpression operand = e.getOperand();
    final Formula result = super.visit(e);
    final CType resultType = PointerTargetSet.simplifyType(e.getExpressionType());
    if (isRevealingType(resultType) && isSimpleTarget(operand) &&
        lastTarget instanceof String &&
        pts.isDeferredAllocationPointer(((String) lastTarget))) {
      assert usedDeferredAllocationPointers.containsKey(lastTarget) &&
             usedDeferredAllocationPointers.get(lastTarget).equals(CPointerType.POINTER_TO_VOID) :
             "Wrong assumptions on deferred allocations tracking: unknown pointer encountered";
      usedDeferredAllocationPointers.put((String) lastTarget, resultType);
    }

    if (operand instanceof CPointerExpression
        && !(resultType instanceof CFunctionType)) {
      // Heuristic:
      // When there is (t)*p, we treat it like *((*t)p)
      // This means the UF for type t get's used instead of the UF for actual type of p.
      final CExpression pointer = ((CPointerExpression)operand).getOperand();

      lastTarget = pointer.accept(this);
      return conv.isCompositeType(resultType) ? (Formula) lastTarget :
             conv.makeDereferece(resultType, (Formula) lastTarget, ssa, pts);
    }

    return result;
  }

  @Override
  public Formula visit(final CIdExpression e) throws UnrecognizedCCodeException {
    Variable variable = e.accept(baseVisitor);
    final CType resultType = PointerTargetSet.simplifyType(e.getExpressionType());
    if (variable != null) {
      if (!(e.getDeclaration() instanceof CFunctionDeclaration)) {
        final String variableName = variable.getName();
        lastTarget = variableName;
        if (pts.isDeferredAllocationPointer(variableName)) {
          usedDeferredAllocationPointers.put(variableName, CPointerType.POINTER_TO_VOID);
        }
        return conv.makeVariable(variable, ssa, pts);
      } else {
        return conv.makeConstant(variable, ssa, pts);
      }
    } else {
      variable = conv.scopedIfNecessary(e, ssa, function);
      lastTarget = conv.makeConstant(Variable.create(PointerTargetSet.getBaseName(variable.getName()),
                                                     PointerTargetSet.getBaseType(resultType)),
                                     ssa,
                                     pts);
      return conv.isCompositeType(resultType) ? (Formula) lastTarget :
             conv.makeDereferece(resultType, (Formula) lastTarget, ssa, pts);
    }
  }

  @Override
  public Formula visit(final CTypeIdExpression e) throws UnrecognizedCCodeException {
    if (e.getOperator() == TypeIdOperator.SIZEOF) {
      return handleSizeof(e, e.getType());
    } else {
      return visitDefault(e);
    }
  }

  private Formula handleSizeof(final CExpression e, final CType type) throws UnrecognizedCCodeException {
    return conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(PointerTargetSet.simplifyType(e.getExpressionType()),
                                                             pts),
                                pts.getSize(type));
  }

  @Override
  public Formula visit(CTypeIdInitializerExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Unhandled initializer", edge, e);
  }

  @Override
  public Formula visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    final CExpression operand = e.getOperand();
    final CType resultType = PointerTargetSet.simplifyType(e.getExpressionType());
    switch (e.getOperator()) {
    case MINUS:
    case PLUS:
    case NOT:
    case TILDE:
      return super.visit(e);
    case SIZEOF:
      return handleSizeof(e, PointerTargetSet.simplifyType(operand.getExpressionType()));
    case AMPER:
      if (!(resultType instanceof CFunctionType)) {
        final Variable baseVariable = operand.accept(baseVisitor);
        if (baseVariable == null) {
          final int oldUsedFieldsSize = usedFields.size();
          Formula addressExpression = operand.accept(this);
          for (int i = oldUsedFieldsSize; i < usedFields.size(); i++) {
            addressedFields.add(usedFields.get(i));
          }
          if (!conv.isCompositeType(PointerTargetSet.simplifyType(operand.getExpressionType()))) {
            addressExpression = (Formula) lastTarget;
            lastTarget = null;
            return addressExpression;
          } else {
            return addressExpression;
          }
        } else {
          final Variable oldBaseVariable = baseVisitor.getLastBase();
          final Variable newBaseVariable = oldBaseVariable.withType(
            PointerTargetSet.getBaseType(oldBaseVariable.getType()));
          final Formula baseAddress = conv.makeConstant(
            newBaseVariable.withName(PointerTargetSet.getBaseName(oldBaseVariable.getName())), ssa, pts);
          conv.addSharingConstraints(edge,
                                     baseAddress,
                                     oldBaseVariable,
                                     initializedFields,
                                     ssa,
                                     constraints,
                                     pts);
          if (ssa.getIndex(oldBaseVariable.getName()) != CToFormulaWithUFConverter.VARIABLE_UNSET) {
            ssa.deleteVariable(oldBaseVariable.getName());
          }
          conv.addPreFilledBase(newBaseVariable.getName(),
                                oldBaseVariable.getType(),
                                pts.isPreparedBase(newBaseVariable.getName()),
                                false,
                                constraints,
                                pts);
          sharedBases.add(Pair.of(newBaseVariable.getName(), oldBaseVariable.getType()));
          return visit(e);
        }
      } else {
        lastTarget = null;
        return operand.accept(this);
      }
      default:
        throw new UnrecognizedCCodeException("Unknown unary operator", edge, e);
    }
  }

  @Override
  public Formula visit(CPointerExpression e) throws UnrecognizedCCodeException {
    final CExpression operand = e.getOperand();
    final CType resultType = PointerTargetSet.simplifyType(e.getExpressionType());
    if (!(resultType instanceof CFunctionType)) {
      lastTarget = operand.accept(this);
      return conv.isCompositeType(resultType) ? (Formula) lastTarget :
             conv.makeDereferece(resultType, (Formula) lastTarget, ssa, pts);
    } else {
      lastTarget = null;
      return operand.accept(this);
    }
  }

//  @Override
//  public Formula visit(final CBinaryExpression e) throws UnrecognizedCCodeException {
//    final BinaryOperator operator = e.getOperator();
//
//    // these operators expect numeric arguments
//    final CType resultType = e.getExpressionType();
//    final FormulaType<?> returnFormulaType = conv.getFormulaTypeFromCType(resultType, pts);
//
//    final CExpression operand1Expression = conv.makeCastFromArrayToPointerIfNecessary(e.getOperand1(), resultType);
//    final CExpression operand2Expression = conv.makeCastFromArrayToPointerIfNecessary(e.getOperand2(), resultType);
//
//    final CType operand1Type = operand1Expression.getExpressionType();
//    final CType operand2Type = operand2Expression.getExpressionType();
//
//    final CType operand1PromotedType = conv.getPromotedCType(operand1Type);
//    Formula operand1 = conv.makeCast(operand1Type,
//                                     operand1PromotedType,
//                                     operand1Expression.accept(this));
//    final CType operand2PromotedType = conv.getPromotedCType(operand2Type);
//    Formula operand2 = conv.makeCast(operand2Type,
//                                     operand2PromotedType,
//                                     operand2Expression.accept(this));
//
//    final CType implicitType;
//    // FOR SHIFTS: The type of the result is that of the promoted left operand. (6.5.7 3)
//    if (operator == BinaryOperator.SHIFT_LEFT || operator == BinaryOperator.SHIFT_RIGHT) {
//      implicitType = operand1PromotedType;
//      // TODO: This is probably not correct as we only need the right formula-type but not a cast
//      operand2 = conv.makeCast(operand2PromotedType, operand1PromotedType, operand2);
//      // TODO: UNDEFINED: When the right side is negative the result is not defined
//    } else {
//      implicitType = conv.getImplicitCType(operand1PromotedType, operand2PromotedType);
//      operand1 = conv.makeCast(operand1PromotedType, implicitType, operand1);
//      operand2 = conv.makeCast(operand2PromotedType, implicitType, operand2);
//    }
//
//    final boolean isSignedOperation = CtoFormulaTypeUtils.isSignedType(implicitType);
//
//    Formula result;
//    switch (operator) {
//    case PLUS:
//      result = conv.fmgr.makePlus(operand1, operand2);
//      break;
//    case MINUS:
//      result =  conv.fmgr.makeMinus(operand1, operand2);
//      break;
//    case MULTIPLY:
//      result =  conv.fmgr.makeMultiply(operand1, operand2);
//      break;
//    case DIVIDE:
//      result =  conv.fmgr.makeDivide(operand1, operand2, isSignedOperation);
//      break;
//    case MODULO:
//      result =  conv.fmgr.makeModulo(operand1, operand2, isSignedOperation);
//      break;
//    case BINARY_AND:
//      result =  conv.fmgr.makeAnd(operand1, operand2);
//      break;
//    case BINARY_OR:
//      result =  conv.fmgr.makeOr(operand1, operand2);
//      break;
//    case BINARY_XOR:
//      result =  conv.fmgr.makeXor(operand1, operand2);
//      break;
//    case SHIFT_LEFT:
//      // NOTE: The type of the result is that of the promoted left operand. (6.5.7 3)
//      result =  conv.fmgr.makeShiftLeft(operand1, operand2);
//      break;
//    case SHIFT_RIGHT:
//      // NOTE: The type of the result is that of the promoted left operand. (6.5.7 3)
//      result =  conv.fmgr.makeShiftRight(operand1, operand2, isSignedOperation);
//      break;
//
//    case GREATER_THAN:
//    case GREATER_EQUAL:
//    case LESS_THAN:
//    case LESS_EQUAL:
//    case EQUALS:
//    case NOT_EQUALS: {
//      final BooleanFormula booleanResult;
//      switch (operator) {
//        case GREATER_THAN:
//          booleanResult = conv.fmgr.makeGreaterThan(operand1, operand2, isSignedOperation);
//          break;
//        case GREATER_EQUAL:
//          booleanResult = conv.fmgr.makeGreaterOrEqual(operand1, operand2, isSignedOperation);
//          break;
//        case LESS_THAN:
//          booleanResult = conv.fmgr.makeLessThan(operand1, operand2, isSignedOperation);
//          break;
//        case LESS_EQUAL:
//          booleanResult = conv.fmgr.makeLessOrEqual(operand1, operand2, isSignedOperation);
//          break;
//        case EQUALS:
//          booleanResult = conv.fmgr.makeEqual(operand1, operand2);
//          break;
//        case NOT_EQUALS:
//          booleanResult = conv.bfmgr.not(conv.fmgr.makeEqual(operand1, operand2));
//          break;
//        default:
//          throw new AssertionError();
//      }
//      result = conv.ifTrueThenOneElseZero(returnFormulaType, booleanResult);
//      break;
//    }
//    default:
//      throw new UnrecognizedCCodeException("Unknown binary operator", edge, e);
//    }
//
//    if (returnFormulaType != conv.fmgr.getFormulaType(result)) {
//      // Could be because both types got promoted
//      if (!areEqual(operand1PromotedType, operand1Type) && !areEqual(operand2PromotedType, operand2Type)) {
//        // We have to cast back to the return type
//        result = conv.makeCast(implicitType, resultType, result);
//      }
//    }
//
//    assert returnFormulaType == conv.fmgr.getFormulaType(result)
//           : "Returntype and Formulatype do not match in visit(CBinaryExpression)";
//    return result;
//  }

  public Object getLastTarget() {
    return lastTarget;
  }

  public ImmutableList<Pair<CCompositeType, String>> getUsedFields() {
    return ImmutableList.copyOf(usedFields);
  }

  public ImmutableList<Pair<CCompositeType, String>> getAddressedFields() {
    return ImmutableList.copyOf(addressedFields);
  }

  public ImmutableList<Pair<CCompositeType, String>> getInitializedFields() {
    return ImmutableList.copyOf(initializedFields);
  }

  public ImmutableList<Pair<String, CType>> getSharedBases() {
    return ImmutableList.copyOf(sharedBases);
  }

  public ImmutableMap<String, CType> getUsedDeferredAllocationPointers() {
    return ImmutableMap.copyOf(usedDeferredAllocationPointers);
  }

  public void reset() {
    lastTarget = null;
    sharedBases.clear();
    usedFields.clear();
    addressedFields.clear();
    initializedFields.clear();
    usedDeferredAllocationPointers.clear();
  }

  @SuppressWarnings("hiding")
  private final CToFormulaWithUFConverter conv;
  private final PointerTargetSetBuilder pts;

  private final BaseVisitor baseVisitor;

  private Object lastTarget;
  private final List<Pair<String, CType>> sharedBases = new ArrayList<>();
  private final List<Pair<CCompositeType, String>> usedFields = new ArrayList<>();
  private final List<Pair<CCompositeType, String>> addressedFields = new ArrayList<>();
  private final List<Pair<CCompositeType, String>> initializedFields = new ArrayList<>();
  private final Map<String, CType> usedDeferredAllocationPointers = new HashMap<>();
}
