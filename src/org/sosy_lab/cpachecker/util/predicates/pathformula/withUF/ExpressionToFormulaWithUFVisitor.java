/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.eclipse.cdt.internal.core.dom.parser.c.CFunctionType;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.Expression.Location;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.Expression.Location.AliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.Expression.Location.UnaliasedLocation;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.Expression.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

class ExpressionToFormulaWithUFVisitor
       extends DefaultCExpressionVisitor<Expression, UnrecognizedCCodeException> {

  public ExpressionToFormulaWithUFVisitor(final CToFormulaWithUFConverter cToFormulaConverter,
                                          final CFAEdge cfaEdge,
                                          final String function,
                                          final SSAMapBuilder ssa,
                                          final Constraints constraints,
                                          final @Nullable ErrorConditions errorConditions,
                                          final PointerTargetSetBuilder pts) {

    delegate = new ExpressionToFormulaVisitor(cToFormulaConverter, cfaEdge, function, ssa, constraints);

    this.conv = cToFormulaConverter;
    this.edge = cfaEdge;
    this.function = function;
    this.ssa = ssa;
    this.constraints = constraints;
    this.errorConditions = errorConditions;
    this.pts = pts;

    this.baseVisitor = new BaseVisitor(conv, cfaEdge, pts);
  }

  private void addEqualBaseAdressConstraint(final Formula p1, final Formula p2) {
    if (errorConditions != null) {
      constraints.addConstraint(conv.fmgr.makeEqual(conv.makeBaseAddressOfTerm(p1),
                                                    conv.makeBaseAddressOfTerm(p2)));
    }
  }

  Formula asValueFormula(final Expression e, final CType type, final boolean isSafe) {
    if (e.isValue()) {
      return e.asValue().getValue();
    } else if (e.asLocation().isAliased()) {
      return !isSafe ? conv.makeDereference(type, e.asLocation().asAliased().getAddress(), ssa, errorConditions) :
                       conv.makeSafeDereference(type, e.asLocation().asAliased().getAddress(), ssa);
    } else { // Unaliased location
      return conv.makeVariable(e.asLocation().asUnaliased().getVariableName(), type, ssa);
    }
  }

  Formula asValueFormula(final Expression e, final CType type) {
    return asValueFormula(e, type, false);
  }

  Formula asSafeValueFormula(final Expression e, final CType type) {
    return asValueFormula(e, type, true);
  }

  @Override
  public AliasedLocation visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    Expression base = e.getArrayExpression().accept(this);
    // There are two distinct kinds of arrays in C:
    // -- fixed-length arrays for which the aliased location of the first element is returned here
    // -- pointers implicitly converted to arrays for which either the aliased or unaliased location of the *pointer*
    //    is returned
    final CType baseType = CTypeUtils.simplifyType(e.getArrayExpression().getExpressionType());
    // Fixed-length arrays
    // TODO: Check if fixed-sized arrays and pointers can be clearly distinguished this way
    if (baseType instanceof CArrayType && ((CArrayType) baseType).getLength() != null) {
      assert base.isAliasedLocation();
    } else {
      // The address of the first element is needed i.e. the value of the pointer in the array expression
      base = AliasedLocation.ofAddress(asValueFormula(base, CToFormulaWithUFConverter.implicitCastToPointer(baseType)));
    }
    // Now we should always have the aliased location of the first array element
    assert base.isAliasedLocation();

    final CType elementType = CTypeUtils.simplifyType(e.getExpressionType());
    final CExpression subscript = e.getSubscriptExpression();
    final CType subscriptType = CTypeUtils.simplifyType(subscript.getExpressionType());
    final Formula index = conv.makeCast(subscriptType,
                                        CPointerType.POINTER_TO_VOID,
                                        asValueFormula(subscript.accept(this), subscriptType),
                                        edge);

    final Formula coeff = conv.fmgr.makeNumber(conv.voidPointerFormulaType, conv.ptsMgr.getSize(elementType));
    final Formula baseAddress = base.asAliasedLocation().getAddress();
    final Formula address = conv.fmgr.makePlus(baseAddress, conv.fmgr.makeMultiply(coeff, index));
    addEqualBaseAdressConstraint(baseAddress, address);
    return AliasedLocation.ofAddress(address);
  }

  static CFieldReference eliminateArrow(final CFieldReference e, final CFAEdge edge)
  throws UnrecognizedCCodeException {
    if (e.isPointerDereference()) {
      final CType fieldOwnerType = CTypeUtils.simplifyType(e.getFieldOwner().getExpressionType());
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
  public Location visit(CFieldReference e) throws UnrecognizedCCodeException {

    e = eliminateArrow(e, edge);

    final Variable variable = e.accept(baseVisitor);
    if (variable != null) {
      final String variableName = variable.getName();
      if (pts.isDeferredAllocationPointer(variableName)) {
        usedDeferredAllocationPointers.put(variableName, CPointerType.POINTER_TO_VOID);
      }
      return UnaliasedLocation.ofVariableName(variableName);
    } else {
      final CType fieldOwnerType = CTypeUtils.simplifyType(e.getFieldOwner().getExpressionType());
      if (fieldOwnerType instanceof CCompositeType) {
        final AliasedLocation base = e.getFieldOwner().accept(this).asAliasedLocation();

        final String fieldName = e.getFieldName();
        usedFields.add(Pair.of((CCompositeType) fieldOwnerType, fieldName));
        final Formula offset = conv.fmgr.makeNumber(conv.voidPointerFormulaType,
                                                    conv.ptsMgr.getOffset((CCompositeType) fieldOwnerType, fieldName));

        final Formula address = conv.fmgr.makePlus(base.getAddress(), offset);
        addEqualBaseAdressConstraint(base.getAddress(), address);
        return AliasedLocation.ofAddress(address);
      } else {
        throw new UnrecognizedCCodeException("Field owner of a non-composite type", edge, e);
      }
    }
  }

  static boolean isUnaliasedLocation(final CExpression e) {
    if (e instanceof CIdExpression) {
      return true;
    } else if (e instanceof CFieldReference) {
      return isUnaliasedLocation(((CFieldReference) e).getFieldOwner());
    } else {
      return false;
    }
  }

  static boolean isRevealingType(final CType type) {
    return (type instanceof CPointerType || type instanceof CArrayType) &&
           !type.equals(CPointerType.POINTER_TO_VOID);
  }

  @Override
  public Expression visit(final CCastExpression e) throws UnrecognizedCCodeException {
    final CType resultType = CTypeUtils.simplifyType(e.getExpressionType());
    final CExpression operand = conv.makeCastFromArrayToPointerIfNecessary(e.getOperand(), resultType);

    final Expression result = operand.accept(this);

    // TODO: is the second isUnaliasedLocation() check really needed?
    if (isRevealingType(resultType) && isUnaliasedLocation(operand) && result.isUnaliasedLocation()) {
      final String variableName =  result.asLocation().asUnaliased().getVariableName();
      if (pts.isDeferredAllocationPointer(variableName)) {
        assert usedDeferredAllocationPointers.containsKey(variableName) &&
               usedDeferredAllocationPointers.get(variableName).equals(CPointerType.POINTER_TO_VOID) :
              "Wrong assumptions on deferred allocations tracking: unknown pointer encountered";
        usedDeferredAllocationPointers.put(variableName, resultType);
      }
    }

    final CType operandType = CTypeUtils.simplifyType(operand.getExpressionType());
    if (CToFormulaWithUFConverter.isSimpleType(resultType)) {
      return Value.ofValue(conv.makeCast(operandType, resultType, asValueFormula(result, operandType), edge));
    } else if (CTypes.withoutConst(resultType).equals(CTypes.withoutConst(operandType))) {
      // Special case: conversion of non-scalar type to itself is allowed (and ignored)
      // Change of const modifier is ignored, too.
      return result;
    } else {
      throw new UnrecognizedCCodeException("Conversion to non-scalar type requested", edge, e);
    }
// TODO: The following heuristic should be implemented in more generally in the assignment to p
//    if (operand instanceof CPointerExpression
//        && !(resultType instanceof CFunctionType)) {
//      // Heuristic:
//      // When there is (t)*p, we treat it like *((*t)p)
//      // This means the UF for type t get's used instead of the UF for actual type of p.
//    }
  }

  @Override
  public Expression visit(final CIdExpression e) throws UnrecognizedCCodeException {
    Variable variable = e.accept(baseVisitor);
    final CType resultType = CTypeUtils.simplifyType(e.getExpressionType());
    if (variable != null) {
      if (!(e.getDeclaration() instanceof CFunctionDeclaration)) {
        final String variableName = variable.getName();
        if (pts.isDeferredAllocationPointer(variableName)) {
          usedDeferredAllocationPointers.put(variableName, CPointerType.POINTER_TO_VOID);
        }
        return UnaliasedLocation.ofVariableName(variableName);
      } else {
        return Value.ofValue(conv.makeConstant(variable));
      }
    } else {
      variable = conv.scopedIfNecessary(e, function);
      final Formula address = conv.makeConstant(PointerTargetSet.getBaseName(variable.getName()),
                                                CTypeUtils.getBaseType(resultType));
      return AliasedLocation.ofAddress(address);
    }
  }

  @Override
  public Expression visit(final CTypeIdExpression e) throws UnrecognizedCCodeException {
    if (e.getOperator() == TypeIdOperator.SIZEOF) {
      return handleSizeof(e, e.getType());
    } else {
      return visitDefault(e);
    }
  }

  private Value handleSizeof(final CExpression e, final CType type) throws UnrecognizedCCodeException {
    return Value.ofValue(
             conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(CTypeUtils.simplifyType(e.getExpressionType())),
                                                               conv.ptsMgr.getSize(type)));
  }

  @Override
  public Expression visit(CTypeIdInitializerExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Unhandled initializer", edge, e);
  }

  @Override
  public Value visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    final CExpression operand = e.getOperand();
    final CType operandType = CTypeUtils.simplifyType(operand.getExpressionType());
    final CType resultType = CTypeUtils.simplifyType(e.getExpressionType());
    final UnaryOperator operator = e.getOperator();
    switch (e.getOperator()) {
    case MINUS:
    case PLUS:
    case TILDE: {
      // Handle Integer Promotion
      final CType promoted = conv.getPromotedCType(operandType);
      Formula operandFormula = asValueFormula(operand.accept(this), operandType);
      operandFormula = conv.makeCast(operandType, promoted, operandFormula, edge);
      Formula result;
      if (operator == UnaryOperator.PLUS) {
        result = operandFormula;
      } else if (operator == UnaryOperator.MINUS) {
        result = conv.fmgr.makeNegate(operandFormula);
      } else {
        assert operator == UnaryOperator.TILDE
              : "This case should be impossible because of switch";
        result = conv.fmgr.makeNot(operandFormula);
      }

      final FormulaType<?> resultFormulaType = conv.getFormulaTypeFromCType(resultType);
      assert resultFormulaType == conv.fmgr.getFormulaType(result)
            : "Returntype and Formulatype do not match in visit(CUnaryExpression)";

      return Value.ofValue(result);
    }

    case NOT: {
      final Formula f = asValueFormula(operand.accept(this), operandType);
      final BooleanFormula term = conv.toBooleanFormula(f);
      return Value.ofValue(conv.ifTrueThenOneElseZero(conv.getFormulaTypeFromCType(resultType),
                                                      conv.bfmgr.not(term)));
    }

    case SIZEOF:
      return handleSizeof(e, CTypeUtils.simplifyType(operand.getExpressionType()));
    case AMPER:
      if (!(resultType instanceof CFunctionType)) {
        final Variable baseVariable = operand.accept(baseVisitor);
        if (baseVariable == null) {
          final int oldUsedFieldsSize = usedFields.size();
          AliasedLocation addressExpression = null;

          if (errorConditions != null && operand instanceof CFieldReference) {
            // for &(s->f) and &((*s).f) do special case because the pointer is
            // not actually dereferenced and thus we don't want to add error conditions
            // for invalid-deref
            final CFieldReference field = (CFieldReference) operand;
            CExpression fieldOwner = field.getFieldOwner();
            boolean isDeref = field.isPointerDereference();
            if (!isDeref && fieldOwner instanceof CPointerExpression) {
              fieldOwner = ((CPointerExpression)fieldOwner).getOperand();
              isDeref = true;
            }
            if (isDeref) {
              final CPointerType pointerType = (CPointerType)CTypeUtils.simplifyType(fieldOwner.getExpressionType());
              final Formula base = asSafeValueFormula(fieldOwner.accept(this), pointerType);
              final String fieldName = field.getFieldName();
              final CCompositeType compositeType = (CCompositeType)CTypeUtils.simplifyType(pointerType.getType());
              usedFields.add(Pair.of(compositeType, fieldName));
              final Formula offset = conv.fmgr.makeNumber(conv.voidPointerFormulaType,
                                                          conv.ptsMgr.getOffset(compositeType, fieldName));
              addressExpression = AliasedLocation.ofAddress(conv.fmgr.makePlus(base, offset));
              addEqualBaseAdressConstraint(base, addressExpression.getAddress());
            }
          }

          if (addressExpression == null) {
            addressExpression = operand.accept(this).asAliasedLocation();
          }

          for (int i = oldUsedFieldsSize; i < usedFields.size(); i++) {
            addressedFields.add(usedFields.get(i));
          }
          return Value.ofValue(addressExpression.getAddress());
        } else {
          final Variable oldBaseVariable = baseVisitor.getLastBase();
          final Variable newBaseVariable = oldBaseVariable.withType(
            CTypeUtils.getBaseType(oldBaseVariable.getType()));
          final Formula baseAddress = conv.makeConstant(
            newBaseVariable.withName(PointerTargetSet.getBaseName(oldBaseVariable.getName())));
          conv.addValueImportConstraints(edge,
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
        return operand.accept(this).asValue();
      }
      default:
        throw new UnrecognizedCCodeException("Unknown unary operator", edge, e);
    }
  }

  @Override
  public AliasedLocation visit(final CPointerExpression e) throws UnrecognizedCCodeException {
    final CExpression operand = e.getOperand();
    final CType operandType = CTypeUtils.simplifyType(operand.getExpressionType());
    return AliasedLocation.ofAddress(asValueFormula(operand.accept(this), operandType));
  }

  private Formula getPointerTargetSizeLiteral(final CPointerType pointerType, final CType implicitType) {
    final int pointerTargetSize = conv.ptsMgr.getSize(pointerType.getType());
    return conv.fmgr.makeNumber(conv.getFormulaTypeFromCType(implicitType), pointerTargetSize);
  }

  @Override
  public Value visit(final CBinaryExpression exp) throws UnrecognizedCCodeException {
    final BinaryOperator op = exp.getOperator();
    final CType returnType = CTypeUtils.simplifyType(exp.getExpressionType());
    final CType calculationType = CTypeUtils.simplifyType(exp.getCalculationType());

    // these operators expect numeric arguments
    final FormulaType<?> returnFormulaType = conv.getFormulaTypeFromCType(returnType);

    CExpression e1 = exp.getOperand1();
    CExpression e2 = exp.getOperand2();
    e1 = conv.makeCastFromArrayToPointerIfNecessary(e1, returnType);
    e2 = conv.makeCastFromArrayToPointerIfNecessary(e2, returnType);
    final CType t1 = CTypeUtils.simplifyType(e1.getExpressionType());
    final CType t2 = CTypeUtils.simplifyType(e2.getExpressionType());
    Formula f1 = asValueFormula(e1.accept(this), t1);
    Formula f2 = asValueFormula(e2.accept(this), t2);

    f1 = conv.makeCast(t1, calculationType, f1, edge);
    f2 = conv.makeCast(t2, calculationType, f2, edge);

    /* FOR SHIFTS:
     * We would not need to cast the second operand, but we do casting,
     * because Mathsat assumes 2 bitvectors of same length.
     *
     * This could be incorrect in cases of negative shifts and
     * signed/unsigned conversion, example: 5U<<(-1).
     * Instead of "undefined value", we return a possible wrong value.
     *
     * ISO-C 6.5.7 Bitwise shift operators
     * If the value of the right operand is negative or is greater than or equal
     * to the width of the promoted left operand, the behavior is undefined.
     */

    final boolean signed;
    if (calculationType instanceof CSimpleType) {
      signed = conv.machineModel.isSigned((CSimpleType)calculationType);
    } else {
      signed = false;
    }

    // to INT or bigger
    final CType promT1 = conv.getPromotedCType(t1).getCanonicalType();
    final CType promT2 = conv.getPromotedCType(t2).getCanonicalType();

    final Formula ret;

    switch (op) {
    case PLUS:
      if (!(promT1 instanceof CPointerType) && !(promT2 instanceof CPointerType)) { // Just an addition e.g. 6 + 7
        ret = conv.fmgr.makePlus(f1, f2);
      } else if (!(promT2 instanceof CPointerType)) {
        // operand1 is a pointer => we should multiply the second summand by the size of the pointer target
        ret =  conv.fmgr.makePlus(f1, conv.fmgr.makeMultiply(f2,
                                                             getPointerTargetSizeLiteral((CPointerType) promT1,
                                                             calculationType)));
        addEqualBaseAdressConstraint(ret, f1);
      } else if (!(promT1 instanceof CPointerType)) {
        // operand2 is a pointer => we should multiply the first summand by the size of the pointer target
        ret =  conv.fmgr.makePlus(f2, conv.fmgr.makeMultiply(f1,
                                                             getPointerTargetSizeLiteral((CPointerType) promT2,
                                                             calculationType)));
        addEqualBaseAdressConstraint(ret, f2);
      } else {
        throw new UnrecognizedCCodeException("Can't add pointers", edge, exp);
      }
      break;
    case MINUS:
      if (!(promT1 instanceof CPointerType) && !(promT2 instanceof CPointerType)) { // Just a subtraction e.g. 6 - 7
        ret =  conv.fmgr.makeMinus(f1, f2);
      } else if (!(promT2 instanceof CPointerType)) {
        // operand1 is a pointer => we should multiply the subtrahend by the size of the pointer target
        ret =  conv.fmgr.makeMinus(f1, conv.fmgr.makeMultiply(f2,
                                                              getPointerTargetSizeLiteral((CPointerType) promT1,
                                                                                            calculationType)));
      } else if (promT1 instanceof CPointerType) {
        // Pointer subtraction => (operand1 - operand2) / sizeof (*operand1)
        if (promT1.equals(promT2)) {
          ret = conv.fmgr.makeDivide(conv.fmgr.makeMinus(f1, f2),
                                     getPointerTargetSizeLiteral((CPointerType) promT1, calculationType),
                                     true);
        } else {
          throw new UnrecognizedCCodeException("Can't subtract pointers of different types", edge, exp);
        }
      } else {
        throw new UnrecognizedCCodeException("Can't subtract a pointer from a non-pointer", edge, exp);
      }
      break;
    case MULTIPLY:
      ret =  conv.fmgr.makeMultiply(f1, f2);
      break;
    case DIVIDE:
      ret =  conv.fmgr.makeDivide(f1, f2, signed);
      break;
    case MODULO:
      ret =  conv.fmgr.makeModulo(f1, f2, signed);
      break;
    case BINARY_AND:
      ret =  conv.fmgr.makeAnd(f1, f2);
      break;
    case BINARY_OR:
      ret =  conv.fmgr.makeOr(f1, f2);
      break;
    case BINARY_XOR:
      ret =  conv.fmgr.makeXor(f1, f2);
      break;
    case SHIFT_LEFT:

      // NOTE: The type of the result is that of the promoted left operand. (6.5.7 3)
      ret =  conv.fmgr.makeShiftLeft(f1, f2);
      break;
    case SHIFT_RIGHT:
      // NOTE: The type of the result is that of the promoted left operand. (6.5.7 3)
      ret =  conv.fmgr.makeShiftRight(f1, f2, signed);
      break;

    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL:
    case EQUALS:
    case NOT_EQUALS: {
      BooleanFormula result;
      switch (op) {
        case GREATER_THAN:
          result= conv.fmgr.makeGreaterThan(f1, f2, signed);
          break;
        case GREATER_EQUAL:
          result= conv.fmgr.makeGreaterOrEqual(f1, f2, signed);
          break;
        case LESS_THAN:
          result= conv.fmgr.makeLessThan(f1, f2, signed);
          break;
        case LESS_EQUAL:
          result= conv.fmgr.makeLessOrEqual(f1, f2, signed);
          break;
        case EQUALS:
          result= conv.fmgr.makeEqual(f1, f2);
          break;
        case NOT_EQUALS:
          result= conv.bfmgr.not(conv.fmgr.makeEqual(f1, f2));
          break;
        default:
          throw new AssertionError();
      }
      ret = conv.ifTrueThenOneElseZero(returnFormulaType, result);
      break;
    }
    default:
      throw new UnrecognizedCCodeException("Unknown binary operator", edge, exp);

    }

    // The CalculationType could be different from returnType, so we cast the result.
    // If the types are equal, the cast returns the Formula unchanged.
    final Formula castedResult = conv.makeCast(calculationType, returnType, ret, edge);

    assert returnFormulaType == conv.fmgr.getFormulaType(castedResult)
         : "Returntype and Formulatype do not match in visit(CBinaryExpression): " + exp;
    return Value.ofValue(castedResult);
  }

  ExpressionToFormulaVisitor getDelegate() {
    return delegate;
  }

  @Override
  protected Value visitDefault(final CExpression e) throws UnrecognizedCCodeException {
    return Value.ofValue(e.accept(delegate));
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
    sharedBases.clear();
    usedFields.clear();
    addressedFields.clear();
    initializedFields.clear();
    usedDeferredAllocationPointers.clear();
  }

  // The protected fields are inherited by StatementToFormulaWithUFVisitor,
  // expanding the functionality of this class to statements
  protected final CToFormulaWithUFConverter conv;
  protected final CFAEdge edge;
  protected final String function;
  protected final SSAMapBuilder ssa;
  protected final Constraints constraints;
  protected final @Nullable ErrorConditions errorConditions;
  protected final PointerTargetSetBuilder pts;

  private final BaseVisitor baseVisitor;
  protected final ExpressionToFormulaVisitor delegate;

  // This fields are made private to prevent reading them in StatementToFormulaWIthUFVisitor
  // The accessors for these fields return the copies of the original collections, these copies can be
  // safely saved and used later when the collections themselves will be modified
  private final List<Pair<String, CType>> sharedBases = new ArrayList<>();
  private final List<Pair<CCompositeType, String>> usedFields = new ArrayList<>();
  private final List<Pair<CCompositeType, String>> addressedFields = new ArrayList<>();
  private final List<Pair<CCompositeType, String>> initializedFields = new ArrayList<>();
  private final Map<String, CType> usedDeferredAllocationPointers = new HashMap<>();
}
