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
package org.sosy_lab.cpachecker.cpa.cpalien;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGField;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * This class evaluates expressions using {@link SMGState}.
 * It should not change the {@link SMGState}, to permit
 * evaluating expressions independently of the transfer relation,
 * enabling other cpas to interact more easily with CPAlien.
 */
public class SMGExpressionEvaluator {

  private final LogManager logger;
  private final MachineModel machineModel;

  public SMGExpressionEvaluator(LogManager pLogger, MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;
  }

  public int getSizeof(CFAEdge edge, CType pType) throws UnrecognizedCCodeException {

    try {
      return machineModel.getSizeof(pType);
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Could not resolve type.", edge);
    }
  }

  /**
   * This visitor evaluates the address of a LValue. It is predominantly
   * used to evaluate the left hand side of a Assignment.
   */
  public class LValueAssignmentVisitor extends AddressVisitor {

    public LValueAssignmentVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
    }

    @Override
    public SMGAddress visit(CUnaryExpression lValue) throws CPATransferException {

      throw new UnrecognizedCCodeException(lValue.toASTString() + " is not an lValue", getCfaEdge(), lValue);
    }

    @Override
    public SMGAddress visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      throw new AssertionError("This expression is not a lValue expression.");
    }

    @Override
    public SMGAddress visit(CComplexCastExpression lValue) throws CPATransferException {
      if (lValue.isImaginaryCast()) { throw new UnrecognizedCCodeException(lValue.toASTString() + " is not an lValue",
          getCfaEdge(), lValue); }

      // TODO evaluating complex numbers is not supported by now

      return SMGAddress.UNKNOWN;
    }

  }

  private SMGAddress getAddressOfField(SMGState smgState, CFAEdge cfaEdge, CFieldReference fieldReference)
      throws CPATransferException {

    CExpression fieldOwner = fieldReference.getFieldOwner();

    CType ownerType = getRealExpressionType(fieldOwner);

    /* Points to the start of this struct or union.
    *
    * Note that whether this field Reference is a pointer dereference x->b
    * or not x.b is indirectly resolved by whether the type of x is
    * a pointer type, in which case its expression is evaluated, or
    * a struct type, in which case the address of the expression
    * similar is evaluated.
    */

    SMGAddressValue fieldOwnerAddress = evaluateAddress(smgState, cfaEdge, fieldOwner);

    if (fieldOwnerAddress.isUnknown()) {
      return SMGAddress.UNKNOWN;
    }

    String fieldName = fieldReference.getFieldName();

    SMGField field = getField(cfaEdge, ownerType, fieldName);

    if (field.isUnknown()) {
      return SMGAddress.UNKNOWN;
    }

    SMGAddress addressOfFieldOwner = fieldOwnerAddress.getAddress();

    SMGExplicitValue fieldOffset = addressOfFieldOwner.add(field.getOffset()).getOffset();

    SMGObject fieldObject = addressOfFieldOwner.getObject();

    return SMGAddress.valueOf(fieldObject, fieldOffset);
  }

  public SMGSymbolicValue readValue(SMGState pSmgState, SMGObject pObject,
      SMGExplicitValue pOffset, CType pType, CFAEdge pEdge) throws SMGInconsistentException {

    if (pOffset.isUnknown() || pObject == null) {
      return SMGUnknownValue.getInstance();
    }

    Integer value = pSmgState.readValue(pObject, pOffset.getAsInt(), pType);

    if (value == null) {
      return SMGUnknownValue.getInstance();
    }

    return SMGKnownSymValue.valueOf(value);
  }

  private SMGField getField(CFAEdge edge, CType ownerType, String fieldName) throws UnrecognizedCCodeException {

    //TODO What if .getRealType = null ? Also, recursion unnecessary

    if (ownerType instanceof CElaboratedType) {
      return getField(edge, ((CElaboratedType) ownerType).getRealType(), fieldName);
    } else if (ownerType instanceof CCompositeType) {
      return getField(edge, (CCompositeType)ownerType, fieldName);
    } else if (ownerType instanceof CPointerType) {

      /* We do not explicitly transform x->b,
      so when we try to get the field b the ownerType of x
      is a pointer type.*/

      CType type = ((CPointerType) ownerType).getType();

      type = getRealExpressionType(type);

      return getField(edge, type, fieldName);
    }

    throw new AssertionError();
  }

  private SMGField getField(CFAEdge pEdge, CCompositeType ownerType, String fieldName) throws UnrecognizedCCodeException {

    List<CCompositeTypeMemberDeclaration> membersOfType = ownerType.getMembers();

    int offset = 0;

    for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
      String memberName = typeMember.getName();
      if (memberName.equals(fieldName)) {

      return new SMGField(SMGKnownExpValue.valueOf(offset),
          getRealExpressionType(typeMember.getType())); }

      if (!(ownerType.getKind() == ComplexTypeKind.UNION)) {
        offset = offset + getSizeof(pEdge, getRealExpressionType(typeMember.getType()));
      }
    }

    return new SMGField(SMGUnknownValue.getInstance(), ownerType);
  }

  boolean isStructOrUnionType(CType rValueType) {

    if (rValueType instanceof CElaboratedType) {
      CElaboratedType type = (CElaboratedType) rValueType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    if (rValueType instanceof CCompositeType) {
      CCompositeType type = (CCompositeType) rValueType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    return false;
  }

  public SMGExplicitValue evaluateExplicitValue(SMGState smgState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    ExplicitValueVisitor visitor = new ExplicitValueVisitor(smgState, cfaEdge);
    SMGExplicitValue value = rValue.accept(visitor);
    return value;
  }

  SMGSymbolicValue evaluateExpressionValue(SMGState smgState, CFAEdge cfaEdge,
      CRightHandSide rValue) throws CPATransferException {

    CType expressionType = getRealExpressionType(rValue);

    if (expressionType instanceof CPointerType
        || expressionType instanceof CArrayType
        || isStructOrUnionType(expressionType)) {
      /* expressions with Array Types as result
       *  are transformed. a = &(a[0]) */

      /* expressions with structs or unions as
       * result will be evaluated to their addresses.
       * The address can be used e.g. to copy the struct.
       */

      return evaluateAddress(smgState, cfaEdge, rValue);
    } else {
      return evaluateNonAddressValue(smgState, cfaEdge, rValue);
    }
  }

  private SMGSymbolicValue evaluateNonAddressValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    ExpressionValueVisitor visitor = getExpressionValueVisitor(cfaEdge, newState);

    SMGSymbolicValue symbolicValue = rValue.accept(visitor);

    return symbolicValue;
  }

  public SMGSymbolicValue evaluateAssumptionValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    ExpressionValueVisitor visitor = getAssumeVisitor(cfaEdge, newState);
    return rValue.accept(visitor);
  }

  public SMGAddressValue evaluateAddress(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    CType expressionType = getRealExpressionType(rValue);

    if (expressionType instanceof CPointerType) {

      PointerVisitor visitor = getPointerVisitor(cfaEdge, newState);

      SMGSymbolicValue address = rValue.accept(visitor);

      return getAddressFromSymbolicValue(newState, address);

    } else if (isStructOrUnionType(expressionType)) {
      /* expressions with structs or unions as
       * result will be evaluated to their addresses.
       * The address can be used e.g. to copy the struct.
       */

      StructAndUnionVisitor visitor = getStructAndUnionVisitor(cfaEdge, newState);

      SMGAddress structAddress = rValue.accept(visitor);

      return createAddress(newState, structAddress);

    } else if (expressionType instanceof CArrayType) {

      ArrayVisitor visitor = getArrayVisitor(cfaEdge, newState);

      SMGAddress arrayAddress = rValue.accept(visitor);

      return createAddress(newState, arrayAddress);
    } else {
      throw new AssertionError("The method evaluateAddress may not be called" +
          "with the type " + expressionType.toASTString(""));
    }
  }

  public CType getRealExpressionType(CType type) {

    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }

    return type;
  }

  public CType getRealExpressionType(CSimpleDeclaration decl) {
    return getRealExpressionType(decl.getType());
  }

  public CType getRealExpressionType(CRightHandSide exp) {
    return getRealExpressionType(exp.getExpressionType());
  }

  /**
   * This visitor is used to determine the address of an expression, mainly lValues.
   * It is used to prevent
   * code replication in other visitors who need this kind of functionality,
   * which is why its abstract.
   *
   */
  private abstract class AddressVisitor extends DefaultCExpressionVisitor<SMGAddress, CPATransferException>
      implements CRightHandSideVisitor<SMGAddress, CPATransferException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;

    public AddressVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      smgState = pSmgState;
    }

    @Override
    protected SMGAddress visitDefault(CExpression pExp) throws CPATransferException {
      return SMGAddress.UNKNOWN;
    }

    @Override
    public SMGAddress visit(CIdExpression variableName) throws CPATransferException {

      SMGObject object = smgState.getObjectForVisibleVariable(variableName.getName());

      return SMGAddress.valueOf(object, SMGKnownExpValue.ZERO);
    }

    @Override
    public SMGAddress visit(CArraySubscriptExpression exp) throws CPATransferException {
      return evaluateArraySubscriptAddress(smgState, cfaEdge, exp);
    }

    @Override
    public SMGAddress visit(CFieldReference pE) throws CPATransferException {
      return getAddressOfField(smgState, cfaEdge, pE);
    }

    @Override
    public SMGAddress visit(CPointerExpression pointerExpression) throws CPATransferException {

      /*
       * The address of a pointer expression (*x) is defined as the
       * evaluation of the pointer x. This is consistent with the meaning
       * of a pointer expression in the left hand side of an assignment *x = ...
       */

      CExpression operand = pointerExpression.getOperand();

      assert operand.getExpressionType().getCanonicalType() instanceof CPointerType;

      SMGAddressValue addressValue = evaluateAddress(getSmgState(), getCfaEdge(), operand);

      if (addressValue.isUnknown()) {
        return SMGAddress.UNKNOWN;
      }

      return addressValue.getAddress();
    }

    public final CFAEdge getCfaEdge() {
      return cfaEdge;
    }

    public final SMGState getSmgState() {
      return smgState;
    }

  }

  class PointerVisitor extends ExpressionValueVisitor
      implements CRightHandSideVisitor<SMGSymbolicValue, CPATransferException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;


    public PointerVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
      cfaEdge = super.getCfaEdge();
      smgState = super.getSmgState();
    }

    @Override
    public SMGAddressValue visit(CIntegerLiteralExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }

    @Override
    public SMGAddressValue visit(CCharLiteralExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }

    @Override
    public SMGAddressValue visit(CFloatLiteralExpression pExp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(pExp));
    }

    @Override
    public SMGAddressValue visit(CImaginaryLiteralExpression pExp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(pExp));
    }

    @Override
    public SMGAddressValue visit(CIdExpression exp) throws CPATransferException {

      CType c = getRealExpressionType(exp);

      if (c instanceof CArrayType) {
        // a == &a[0];
        return createAddressOfVariable(exp);
      } else if (isStructOrUnionType(c)) {
        // We use this temporary address to copy the values of the struct or union
        return createAddressOfVariable(exp);
      }

      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }

    @Override
    public SMGAddressValue visit(CUnaryExpression unaryExpression) throws CPATransferException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case AMPER:
        return handleAmper(unaryOperand);

      case SIZEOF:
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of "
            + unaryOperand.toASTString()
            + " as pointer type", cfaEdge, unaryExpression);

      case MINUS:
      case NOT:
      case TILDE:
      default:
        // Can't evaluate these Addresses
        return SMGUnknownValue.getInstance();
      }
    }

    private SMGAddressValue handleAmper(CExpression lValue) throws CPATransferException {
      if (lValue instanceof CIdExpression) {
        // &a
        return createAddressOfVariable((CIdExpression) lValue);
      } else if (lValue instanceof CPointerExpression) {
        // &(*(a))

        return getAddressFromSymbolicValue(smgState,
            ((CPointerExpression) lValue).getOperand().accept(this));

      } else if (lValue instanceof CFieldReference) {
        // &(a.b)
        return createAddressOfField((CFieldReference) lValue);
      } else if (lValue instanceof CArraySubscriptExpression) {
        // &a[b]
        return createAddressOfArraySubscript((CArraySubscriptExpression) lValue);
      } else {
        return SMGUnknownValue.getInstance();
      }
    }

    private SMGAddressValue createAddressOfArraySubscript(CArraySubscriptExpression lValue)
        throws CPATransferException {

      CExpression arrayExpression = lValue.getArrayExpression();

      SMGAddress arrayAddress = evaluateArrayExpression(smgState, cfaEdge, arrayExpression);

      if (arrayAddress.isUnknown()) { return SMGUnknownValue.getInstance(); }

      CExpression subscriptExpr = lValue.getSubscriptExpression();

      SMGExplicitValue subscriptValue = evaluateExplicitValue(smgState, cfaEdge, subscriptExpr);

      if (subscriptValue.isUnknown()) { return SMGUnknownValue.getInstance(); }

      SMGExplicitValue arrayOffset = arrayAddress.getOffset();

      int typeSize = getSizeof(cfaEdge, getRealExpressionType(lValue));

      SMGExplicitValue sizeOfType = SMGKnownExpValue.valueOf(typeSize);

      SMGExplicitValue offset = arrayOffset.add(subscriptValue).multiply(sizeOfType);

      return createAddress(smgState, arrayAddress.getObject(), offset);
    }

    private SMGAddressValue createAddressOfField(CFieldReference lValue) throws CPATransferException {

      SMGAddress addressOfField = getAddressOfField(smgState, cfaEdge, lValue);

      if (addressOfField.isUnknown()) { return SMGUnknownValue.getInstance(); }

      return createAddress(smgState, addressOfField.getObject(), addressOfField.getOffset());
    }

    private SMGAddressValue createAddressOfVariable(CIdExpression idExpression) throws SMGInconsistentException {

      SMGObject variableObject = smgState.getObjectForVisibleVariable(idExpression.getName());

      if (variableObject == null) {
        return SMGUnknownValue.getInstance();
      } else {
        return createAddress(smgState, variableObject, SMGKnownExpValue.ZERO);
      }
    }

    @Override
    public SMGAddressValue visit(CPointerExpression pointerExpression) throws CPATransferException {

      CExpression operand = pointerExpression.getOperand();
      CType operandType = getRealExpressionType(operand);
      CType expType = getRealExpressionType(pointerExpression);

      if (operandType instanceof CPointerType) {

        SMGSymbolicValue address = dereferencePointer(operand, expType);
        return getAddressFromSymbolicValue(smgState, address);

      } else if (operandType instanceof CArrayType) {

        SMGSymbolicValue address = dereferenceArray(operand, expType);
        return getAddressFromSymbolicValue(smgState, address);

      } else {
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of "
            + operand.toASTString()
            + " as pointer type", cfaEdge, pointerExpression);
      }
    }

    @Override
    public SMGAddressValue visit(CBinaryExpression binaryExp) throws CPATransferException {

      BinaryOperator binaryOperator = binaryExp.getOperator();
      CExpression lVarInBinaryExp = binaryExp.getOperand1();
      CExpression rVarInBinaryExp = binaryExp.getOperand2();
      CType lVarInBinaryExpType = getRealExpressionType(lVarInBinaryExp);
      CType rVarInBinaryExpType = getRealExpressionType(rVarInBinaryExp);

      boolean lVarIsAddress = lVarInBinaryExpType instanceof CPointerType;
      boolean rVarIsAddress = rVarInBinaryExpType instanceof CPointerType;

      CExpression address = null;
      CExpression pointerOffset = null;
      CType addressType = null;

      if (lVarIsAddress == rVarIsAddress) {
        return SMGUnknownValue.getInstance(); // If both or neither are Addresses,
        //  we can't evaluate the address this pointer stores.
      } else if (lVarIsAddress) {
        address = lVarInBinaryExp;
        pointerOffset = rVarInBinaryExp;
        addressType = lVarInBinaryExpType;
      } else if (rVarIsAddress) {
        address = rVarInBinaryExp;
        pointerOffset = lVarInBinaryExp;
        addressType = rVarInBinaryExpType;
      } else {
        // TODO throw Exception, no Pointer
        return SMGUnknownValue.getInstance();
      }

      switch (binaryOperator) {
      case PLUS:
      case MINUS: {

        SMGSymbolicValue addressVal = address.accept(this);

        if (!(addressVal instanceof SMGAddressValue)) { return SMGUnknownValue.getInstance(); }

        SMGAddressValue addressValue = (SMGAddressValue) addressVal;

        ExplicitValueVisitor v = new ExplicitValueVisitor(smgState, cfaEdge);

        SMGExplicitValue offsetValue = pointerOffset.accept(v);

        if (addressValue.isUnknown() || offsetValue.isUnknown()) { return addressValue; }

        SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge, addressType));

        SMGExplicitValue pointerOffsetValue = offsetValue.multiply(typeSize);

        SMGObject target = addressValue.getObject();

        SMGExplicitValue addressOffset = addressValue.getOffset();

        switch (binaryOperator) {
        case PLUS:
          return createAddress(smgState, target, addressOffset.add(pointerOffsetValue));
        case MINUS:
          if (lVarIsAddress) {
            return createAddress(smgState, target, addressOffset.subtract(pointerOffsetValue));
          } else {
            return createAddress(smgState, target, pointerOffsetValue.subtract(addressOffset));
          }
        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL:
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of " + binaryExp + " as pointer type",
            cfaEdge, binaryExp);
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case MODULO:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      default:
        return SMGUnknownValue.getInstance();
      }
    }

    @Override
    public SMGAddressValue visit(CArraySubscriptExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }

    @Override
    public SMGAddressValue visit(CFieldReference exp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }
  }

  private SMGAddress evaluateArraySubscriptAddress(SMGState smgState, CFAEdge cfaEdge,
      CArraySubscriptExpression exp) throws CPATransferException {

    SMGAddressValue arrayAddress = evaluateAddress(smgState, cfaEdge, exp.getArrayExpression());

    if (arrayAddress.isUnknown()) { return SMGAddress.UNKNOWN; }

    SMGExplicitValue subscriptValue = evaluateExplicitValue(smgState, cfaEdge, exp.getSubscriptExpression());

    if (subscriptValue.isUnknown()) { return SMGAddress.UNKNOWN; }

    SMGExplicitValue typeSize =
        SMGKnownExpValue.valueOf(getSizeof(cfaEdge, exp.getExpressionType()));

    SMGExplicitValue subscriptOffset = subscriptValue.multiply(typeSize);

    return arrayAddress.getAddress().add(subscriptOffset);
  }

  SMGAddressValue createAddress(SMGEdgePointsTo pEdge) {

    if (pEdge == null) { return SMGUnknownValue.getInstance(); }

    return SMGKnownAddVal.valueOf(pEdge.getValue(), pEdge.getObject(), pEdge.getOffset());
  }

  private SMGAddressValue createAddress(SMGState pNewState, SMGAddress pAddress) throws SMGInconsistentException {

    if (pAddress.isUnknown()) { return SMGUnknownValue.getInstance(); }

    return createAddress(pNewState, pAddress.getObject(), pAddress.getOffset());
  }

  SMGAddressValue getAddressFromSymbolicValue(SMGState pSmgState,
      SMGSymbolicValue pAddressValue) throws SMGInconsistentException {

    if (pAddressValue instanceof SMGAddressValue) { return (SMGAddressValue) pAddressValue; }

    if (pAddressValue.isUnknown()) { return SMGUnknownValue.getInstance(); }

    //TODO isPointer(symbolicValue)
    SMGEdgePointsTo edge = pSmgState.getPointerFromValue(pAddressValue.getAsInt());

    return createAddress(edge);
  }

  SMGAddressValue createAddress(SMGState pSmgState, SMGObject pTarget, SMGExplicitValue pOffset)
      throws SMGInconsistentException {

    SMGAddressValue addressValue = getAddress(pSmgState, pTarget, pOffset);

    if (addressValue.isUnknown()) {

      SMGKnownSymValue value = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
      addressValue = SMGKnownAddVal.valueOf(pTarget, (SMGKnownExpValue) pOffset, value);
    }

    return addressValue;
  }

  SMGAddressValue getAddress(SMGState pSmgState, SMGObject pTarget,
      SMGExplicitValue pOffset) throws SMGInconsistentException {

    if (pTarget == null || pOffset.isUnknown()) { return SMGUnknownValue.getInstance(); }

    Integer address = pSmgState.getAddress(pTarget, pOffset.getAsInt());

    if (address == null) { return SMGUnknownValue.getInstance(); }

    return createAddress(pSmgState.getPointerFromValue(address));
  }

  /**
   * This class evaluates expressions that evaluate to a
   * array type. The type of every expression visited by this
   * visitor has to be a {@link CArrayType }. The result of
   * the evaluation is an {@link SMGAddress}. The object
   * represents the memory this array is placed in, the offset
   * represents the start of the array in the object.
   */
  class ArrayVisitor extends AddressVisitor
      implements CRightHandSideVisitor<SMGAddress, CPATransferException> {

    public ArrayVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
    }

    @Override
    protected SMGAddress visitDefault(CExpression exp) {
      return SMGAddress.UNKNOWN;
    }

    @Override
    public SMGAddress visit(CUnaryExpression unaryExpression) throws CPATransferException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();

      switch (unaryOperator) {

      case SIZEOF:
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of " + unaryExpression
            + " as array type", getCfaEdge(), unaryExpression);
      case MINUS:
      case NOT:
      case TILDE:
      case AMPER:
      default:
        // Can't evaluate these ArrayExpressions
        return SMGAddress.UNKNOWN;
      }
    }

    @Override
    public SMGAddress visit(CBinaryExpression binaryExp) throws CPATransferException {

      BinaryOperator binaryOperator = binaryExp.getOperator();
      CExpression lVarInBinaryExp = binaryExp.getOperand1();
      CExpression rVarInBinaryExp = binaryExp.getOperand2();
      CType lVarInBinaryExpType = getRealExpressionType(lVarInBinaryExp);
      CType rVarInBinaryExpType = getRealExpressionType(rVarInBinaryExp);

      boolean lVarIsAddress = lVarInBinaryExpType instanceof CArrayType;
      boolean rVarIsAddress = rVarInBinaryExpType instanceof CArrayType;

      CExpression address = null;
      CExpression arrayOffset = null;
      CType addressType = null;

      if (lVarIsAddress == rVarIsAddress) {
        return SMGAddress.UNKNOWN; // If both or neither are Addresses,
        //  we can't evaluate the address this pointer stores.
      } else if (lVarIsAddress) {
        address = lVarInBinaryExp;
        arrayOffset = rVarInBinaryExp;
        addressType = lVarInBinaryExpType;
      } else if (rVarIsAddress) {
        address = rVarInBinaryExp;
        arrayOffset = lVarInBinaryExp;
        addressType = rVarInBinaryExpType;
      } else {
        // TODO throw Exception, no Pointer
        return SMGAddress.UNKNOWN;
      }

      switch (binaryOperator) {
      case PLUS:
      case MINUS: {

        SMGAddress addressVal = address.accept(this);

        if (addressVal.isUnknown()) { return addressVal; }

        ExplicitValueVisitor v = new ExplicitValueVisitor(getSmgState(), getCfaEdge());

        SMGExplicitValue offsetValue = arrayOffset.accept(v);

        if (offsetValue.isUnknown()) { return SMGAddress.UNKNOWN; }

        SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(getCfaEdge(), addressType));

        SMGExplicitValue arrayOffsetValue = offsetValue.multiply(typeSize);

        SMGObject target = addressVal.getObject();

        SMGExplicitValue addressOffset = addressVal.getOffset();

        switch (binaryOperator) {
        case PLUS:
          return SMGAddress.valueOf(target, addressOffset.add(addressOffset));
        case MINUS:
          if (lVarIsAddress) {
            return SMGAddress.valueOf(target, addressOffset.subtract(arrayOffsetValue));
          } else {
            return SMGAddress.valueOf(target, arrayOffsetValue.subtract(addressOffset));
          }
        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL:
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of "
            + binaryExp + " as pointer type", getCfaEdge(), binaryExp);
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case MODULO:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      default:
        return SMGAddress.UNKNOWN;
      }
    }

    @Override
    public SMGAddress visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      return SMGAddress.UNKNOWN;
    }

    @Override
    public SMGAddress visit(CCastExpression cast) throws CPATransferException {
      return cast.getOperand().accept(this);
    }

    @Override
    public SMGAddress visit(CComplexCastExpression cast) throws CPATransferException {
      // TODO evaluation Complex numbers is not supported by now
      return SMGAddress.UNKNOWN;
    }
  }

  private class AssumeVisitor extends ExpressionValueVisitor {

    private final SMGState smgState;

    public AssumeVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
      smgState = getSmgState();
    }

    @Override
    public SMGSymbolicValue visit(CBinaryExpression exp) throws CPATransferException {

      BinaryOperator binaryOperator = exp.getOperator();

      switch (binaryOperator) {
      case EQUALS:
      case NOT_EQUALS:
      case LESS_EQUAL:
      case LESS_THAN:
      case GREATER_EQUAL:
      case GREATER_THAN:

        CExpression lVarInBinaryExp = exp.getOperand1();
        CExpression rVarInBinaryExp = exp.getOperand2();

        SMGSymbolicValue lVal = evaluateExpressionValue(smgState, getCfaEdge(), lVarInBinaryExp);
        if (lVal.isUnknown()) { return SMGUnknownValue.getInstance(); }

        SMGSymbolicValue rVal = evaluateExpressionValue(smgState, getCfaEdge(), rVarInBinaryExp);
        if (rVal.isUnknown()) { return SMGUnknownValue.getInstance(); }

        boolean isZero;
        boolean isOne;

        switch (binaryOperator) {
        case NOT_EQUALS:
          isZero = lVal.equals(rVal);
          isOne = smgState.isUnequal(lVal.getAsInt(), rVal.getAsInt());
          break;
        case EQUALS:
          isOne = lVal.equals(rVal);
          isZero = smgState.isUnequal(lVal.getAsInt(), rVal.getAsInt());
          break;
        case LESS_EQUAL:
        case GREATER_EQUAL:
          isOne = lVal.equals(rVal);
          isZero = false;
          if (isOne) {
            break;
          }

          //$FALL-THROUGH$
        case GREATER_THAN:
        case LESS_THAN:

          SMGAddressValue rAddress = getAddressFromSymbolicValue(getSmgState(), rVal);

          if (rAddress.isUnknown()) { return SMGUnknownValue.getInstance(); }

          SMGAddressValue lAddress = getAddressFromSymbolicValue(getSmgState(), rVal);

          if (lAddress.isUnknown()) { return SMGUnknownValue.getInstance(); }

          SMGObject lObject = lAddress.getObject();
          SMGObject rObject = rAddress.getObject();

          if (!lObject.equals(rObject)) { return SMGUnknownValue.getInstance(); }

          long rOffset = rAddress.getOffset().getAsLong();
          long lOffset = lAddress.getOffset().getAsLong();

          // We already checked equality
          switch (binaryOperator) {
          case LESS_THAN:
          case LESS_EQUAL:
            isOne = lOffset < rOffset;
            isZero = !isOne;
            break;
          case GREATER_EQUAL:
          case GREATER_THAN:
            isOne = lOffset > rOffset;
            isZero = !isOne;
            break;
          default:
            throw new AssertionError();
          }
          break;
        default:
          throw new AssertionError();
        }

        if (isZero) {
          // return 0 if the expression does not hold
          return SMGKnownSymValue.FALSE;
        } else if (isOne) {
          // return a symbolic Value representing 1 if the expression does hold
          return SMGKnownSymValue.TRUE;
        } else {
          // otherwise return UNKNOWN
          return SMGUnknownValue.getInstance();
        }

      default:
        return super.visit(exp);
      }
    }
  }

  /**
   * This class evaluates expressions that evaluate to a
   * struct or union type. The type of every expression visited by this
   * visitor has to be either {@link CElaboratedType} or
   * {@link CComplexType}. Furthermore, it must not be a enum.
   * The result of the evaluation is an {@link SMGAddress}.
   * The object represents the memory this struct is placed in, the offset
   * represents the start of the struct.
   */
  class StructAndUnionVisitor extends ArrayVisitor
      implements CRightHandSideVisitor<SMGAddress, CPATransferException> {

    public StructAndUnionVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
      super(pCfaEdge, pNewState);
    }

    @Override
    public SMGAddress visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      return SMGAddress.UNKNOWN;
    }
  }

  /**
   * This class evaluates expressions that evaluate not to a
   * pointer, array, struct or union type.
   * The result of this evaluation is a {@link SMGSymbolicValue}.
   * The value represents a symbolic value of the SMG.
   *
   */
  class ExpressionValueVisitor extends DefaultCExpressionVisitor<SMGSymbolicValue, CPATransferException>
      implements CRightHandSideVisitor<SMGSymbolicValue, CPATransferException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;

    public ExpressionValueVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      smgState = pSmgState;
    }

    @Override
    protected SMGSymbolicValue visitDefault(CExpression pExp) {
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGSymbolicValue visit(CArraySubscriptExpression exp) throws CPATransferException {

      SMGAddress address = evaluateArraySubscriptExpression(smgState, cfaEdge, exp);

      if (address.isUnknown()) { return SMGUnknownValue.getInstance(); }

      SMGSymbolicValue value =
          readValue(smgState, address.getObject(), address.getOffset(), getRealExpressionType(exp), cfaEdge);

      return value;
    }

    @Override
    public SMGSymbolicValue visit(CIntegerLiteralExpression exp) throws CPATransferException {

      BigInteger value = exp.getValue();

      boolean isZero = value.equals(BigInteger.ZERO);

      return (isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance());
    }

    @Override
    public SMGSymbolicValue visit(CCharLiteralExpression exp) throws CPATransferException {

      char value = exp.getCharacter();

      return (value == 0) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
    }

    @Override
    public SMGSymbolicValue visit(CFieldReference fieldReference) throws CPATransferException {

      SMGAddress addressOfField = getAddressOfField(smgState, cfaEdge, fieldReference);

      if (addressOfField.isUnknown()) { return SMGUnknownValue.getInstance(); }

      CType fieldType = fieldReference.getExpressionType().getCanonicalType();

      return readValue(smgState, addressOfField.getObject(), addressOfField.getOffset(), fieldType, cfaEdge);
    }

    @Override
    public SMGSymbolicValue visit(CFloatLiteralExpression exp) throws CPATransferException {

      boolean isZero = exp.getValue().equals(BigDecimal.ZERO);

      return isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
    }

    @Override
    public SMGSymbolicValue visit(CIdExpression idExpression) throws CPATransferException {

      CSimpleDeclaration decl = idExpression.getDeclaration();

      if (decl instanceof CEnumerator) {

        long enumValue = ((CEnumerator) decl).getValue();

        return enumValue == 0 ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

      } else if (decl instanceof CVariableDeclaration
          || decl instanceof CParameterDeclaration) {

        SMGObject variableObject = smgState.getObjectForVisibleVariable(idExpression.getName());

        return readValue(smgState, variableObject, SMGKnownExpValue.ZERO,
            getRealExpressionType(idExpression), cfaEdge);
      }

      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGSymbolicValue visit(CUnaryExpression unaryExpression) throws CPATransferException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case AMPER:
        throw new UnrecognizedCCodeException("Can't use & of expression " + unaryOperand.toASTString(), cfaEdge,
            unaryExpression);

      case MINUS:
        SMGSymbolicValue value = unaryOperand.accept(this);
        return value.equals(SMGKnownSymValue.ZERO) ? value : SMGUnknownValue.getInstance();

      case SIZEOF:
        int size = getSizeof(cfaEdge, getRealExpressionType(unaryOperand));
        return (size == 0) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

      case NOT:
        return handleNot(unaryOperand);

      case TILDE:

      default:
        return SMGUnknownValue.getInstance();
      }
    }

    @Override
    public SMGSymbolicValue visit(CPointerExpression pointerExpression) throws CPATransferException {

      CExpression operand = pointerExpression.getOperand();
      CType operandType = getRealExpressionType(operand);
      CType expType = getRealExpressionType(pointerExpression);


      if (operandType instanceof CPointerType) {
        return dereferencePointer(operand, expType);
      } else if (operandType instanceof CArrayType) {
        return dereferenceArray(operand, expType);
      } else {
        throw new UnrecognizedCCodeException("dereference of non-pointer type", cfaEdge, pointerExpression);
      }
    }

    private SMGSymbolicValue handleNot(CExpression pUnaryOperand) throws CPATransferException {
      CType unaryOperandType = getRealExpressionType(pUnaryOperand);

      SMGSymbolicValue value;

      if (unaryOperandType instanceof CPointerType || unaryOperandType instanceof CArrayType) {
        value = evaluateAddress(smgState, cfaEdge, pUnaryOperand);
      } else {
        value = pUnaryOperand.accept(this);
      }

      if (value.equals(SMGKnownSymValue.ZERO)) {
        return SMGKnownSymValue.ZERO;
      } else if (isUnequal(smgState, value, SMGKnownSymValue.ZERO)) {
        return SMGKnownSymValue.ZERO;
      } else {
        return SMGUnknownValue.getInstance();
      }
    }

    @Override
    public SMGSymbolicValue visit(CTypeIdExpression typeIdExp) throws UnrecognizedCCodeException {

      TypeIdOperator typeOperator = typeIdExp.getOperator();
      CType type = typeIdExp.getType();

      switch (typeOperator) {
      case SIZEOF:
        return getSizeof(cfaEdge, type) == 0 ? SMGKnownSymValue.ZERO : SMGUnknownValue
            .getInstance();
      default:
        return SMGUnknownValue.getInstance();
        //TODO Investigate the other Operators.
      }
    }

    @Override
    public SMGSymbolicValue visit(CBinaryExpression exp) throws CPATransferException {

      BinaryOperator binaryOperator = exp.getOperator();
      CExpression lVarInBinaryExp = exp.getOperand1();
      CExpression rVarInBinaryExp = exp.getOperand2();

      switch (binaryOperator) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case MODULO:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR: {
        SMGSymbolicValue lVal = lVarInBinaryExp.accept(this);
        if (lVal.equals(SMGUnknownValue.getInstance())) { return SMGUnknownValue.getInstance(); }

        SMGSymbolicValue rVal = rVarInBinaryExp.accept(this);
        if (rVal.equals(SMGUnknownValue.getInstance())) { return SMGUnknownValue.getInstance(); }

        boolean isZero;

        switch (binaryOperator) {
        case PLUS:
        case SHIFT_LEFT:
        case BINARY_OR:
        case BINARY_XOR:
        case SHIFT_RIGHT:
          isZero = lVal.equals(SMGKnownSymValue.ZERO) && lVal.equals(SMGKnownSymValue.ZERO);
          return (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

        case MINUS:
        case MODULO:
          isZero = (lVal.equals(rVal));
          return (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if (rVal.equals(SMGKnownSymValue.ZERO)) { return SMGUnknownValue.getInstance(); }

          isZero = lVal.equals(SMGKnownSymValue.ZERO);
          return (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

        case MULTIPLY:
        case BINARY_AND:
          isZero = lVal.equals(SMGKnownSymValue.ZERO)
              || rVal.equals(SMGKnownSymValue.ZERO);
          return (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL: {

        SMGSymbolicValue lVal = lVarInBinaryExp.accept(this);
        if (lVal.equals(SMGUnknownValue.getInstance())) { return SMGUnknownValue.getInstance(); }

        SMGSymbolicValue rVal = rVarInBinaryExp.accept(this);
        if (rVal.equals(SMGUnknownValue.getInstance())) { return SMGUnknownValue.getInstance(); }

        boolean isZero;
        switch (binaryOperator) {
        case NOT_EQUALS:
          isZero = (lVal.equals(rVal));
          break;
        case EQUALS:
          isZero = isUnequal(smgState, lVal, rVal);
          break;
        case GREATER_THAN:
        case GREATER_EQUAL:
        case LESS_THAN:
        case LESS_EQUAL:
          isZero = false;
          break;

        default:
          throw new AssertionError();
        }

        if (isZero) {
          return SMGKnownSymValue.ZERO;
        } else {
          return SMGUnknownValue.getInstance();
        }
      }

      default:
        return SMGUnknownValue.getInstance();
      }
    }

    private boolean isUnequal(SMGState pSmgState, SMGSymbolicValue pLVal, SMGSymbolicValue pRVal) {

      if (pLVal.isUnknown() || pRVal.isUnknown()) { return false; }

      return pSmgState.isUnequal(pLVal.getAsInt(), pRVal.getAsInt());
    }

    @Override
    public SMGSymbolicValue visit(CCastExpression cast) throws CPATransferException {
      return cast.getOperand().accept(this);
    }

    @Override
    public SMGSymbolicValue visit(CComplexCastExpression cast) throws CPATransferException {
      // TODO evaluation complex numbers is not supported by now
      return SMGUnknownValue.getInstance();
    }

    protected SMGSymbolicValue dereferenceArray(CRightHandSide exp, CType derefType) throws CPATransferException {

      ArrayVisitor v = getArrayVisitor(cfaEdge, smgState);

      SMGAddress address = exp.accept(v);

      if (address.isUnknown()) {
        // We can't resolve the field to dereference, therefore
        // we must assume, that it is invalid
        smgState.setUnknownDereference();
        return SMGUnknownValue.getInstance();
      }

      // a == &a[0]
      if (derefType instanceof CArrayType) {
        return createAddress(smgState, address.getObject(), address.getOffset());
      } else {
        return readValue(smgState, address.getObject(), address.getOffset(), derefType, cfaEdge);
      }
    }

    protected SMGSymbolicValue dereferencePointer(CRightHandSide exp, CType derefType)
        throws CPATransferException {

      SMGAddressValue address = evaluateAddress(smgState, cfaEdge, exp);

      if (address.isUnknown()) {
        // We can't resolve the field to dereference , therefore
        // we must assume, that it is invalid
        smgState.setUnknownDereference();
        return SMGUnknownValue.getInstance();
      }

      // a == &a[0]
      if (derefType instanceof CArrayType) {
        return createAddress(smgState, address.getObject(), address.getOffset());
      } else {
        return readValue(smgState, address.getObject(), address.getOffset(), derefType, cfaEdge);
      }
    }

    @Override
    public SMGSymbolicValue visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      return SMGUnknownValue.getInstance();
    }

    public SMGState getSmgState() {
      return smgState;
    }

    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }
  }

  private class ExplicitValueVisitor extends DefaultCExpressionVisitor<SMGExplicitValue, CPATransferException>
      implements CRightHandSideVisitor<SMGExplicitValue, CPATransferException> {

    @SuppressWarnings("unused")
    private final SMGState smgState;
    private final CFAEdge cfaEdge;

    public ExplicitValueVisitor(SMGState pSmgState, CFAEdge pCfaEdge) {
      smgState = pSmgState;
      cfaEdge = pCfaEdge;
    }

    @Override
    protected SMGExplicitValue visitDefault(CExpression pExp) {
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CIntegerLiteralExpression exp) throws UnrecognizedCCodeException {
      return SMGKnownExpValue.valueOf(exp.asLong());
    }

    @Override
    public SMGExplicitValue visit(CBinaryExpression pE) throws CPATransferException {
      BinaryOperator binaryOperator = pE.getOperator();
      CExpression lVarInBinaryExp = pE.getOperand1();
      CExpression rVarInBinaryExp = pE.getOperand2();

      switch (binaryOperator) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR: {
        SMGExplicitValue lValue = lVarInBinaryExp.accept(this);

        if (lValue.isUnknown()) { return SMGUnknownValue.getInstance(); }

        SMGExplicitValue rValue = rVarInBinaryExp.accept(this);
        if (rValue.isUnknown()) { return SMGUnknownValue.getInstance(); }

        switch (binaryOperator) {
        case PLUS:
          return lValue.add(rValue);

        case MINUS:
          return lValue.subtract(rValue);

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if (rValue.equals(SMGKnownExpValue.ZERO)) { return SMGUnknownValue.getInstance(); }

          return lValue.divide(rValue);

        case MULTIPLY:
          return lValue.multiply(rValue);

        case SHIFT_LEFT:
          return lValue.shiftLeft(rValue);

        case BINARY_AND:
          return lValue.and(rValue);

        case BINARY_OR:
          return lValue.or(rValue);

        case BINARY_XOR:
          return lValue.xor(rValue);

        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL: {

        SMGExplicitValue lValue = lVarInBinaryExp.accept(this);
        if (lValue.isUnknown()) { return SMGUnknownValue.getInstance(); }

        SMGExplicitValue rValue = rVarInBinaryExp.accept(this);
        if (rValue.isUnknown()) { return SMGUnknownValue.getInstance(); }

        long rVal = rValue.getAsLong();
        long lVal = lValue.getAsLong();

        boolean result;
        switch (binaryOperator) {
        case EQUALS:
          result = lVal == rVal;
          break;
        case NOT_EQUALS:
          result = lVal != rVal;
          break;
        case GREATER_THAN:
          result = lVal > rVal;
          break;
        case GREATER_EQUAL:
          result = lVal >= rVal;
          break;
        case LESS_THAN:
          result = lVal < rVal;
          break;
        case LESS_EQUAL:
          result = lVal <= rVal;
          break;

        default:
          throw new AssertionError();
        }

        // return 1 if expression holds, 0 otherwise
        return (result ? SMGKnownExpValue.ONE : SMGKnownExpValue.ZERO);
      }

      case MODULO:
      case SHIFT_RIGHT:
      default:
        // TODO check which cases can be handled
        return SMGUnknownValue.getInstance();
      }
    }

    @Override
    public SMGExplicitValue visit(CIdExpression idExpression) throws UnrecognizedCCodeException {

      CSimpleDeclaration decl = idExpression.getDeclaration();

      if (decl instanceof CEnumerator) { return SMGKnownExpValue.valueOf(((CEnumerator) decl).getValue()); }

      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CUnaryExpression unaryExpression) throws CPATransferException {
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();


      SMGExplicitValue value = null;

      switch (unaryOperator) {
      case MINUS:
        value = unaryOperand.accept(this);
        return (value.isUnknown()) ? SMGUnknownValue.getInstance() : value.negate();

      case NOT:
        value = unaryOperand.accept(this);

        if (value.isUnknown()) {
          return SMGUnknownValue.getInstance();
        } else {
          return (value.equals(SMGKnownExpValue.ZERO)) ? SMGKnownExpValue.ONE : SMGKnownExpValue.ZERO;
        }

      case AMPER:
        // valid expression, but we don't have explicit values for addresses.
        return SMGUnknownValue.getInstance();

      case SIZEOF:

        int size = getSizeof(cfaEdge, getRealExpressionType(unaryOperand));
        return SMGKnownExpValue.valueOf(size);
      case TILDE:
      default:
        // TODO handle unimplemented operators
        return SMGUnknownValue.getInstance();
      }
    }

    @Override
    public SMGExplicitValue visit(CPointerExpression pointerExpression) throws CPATransferException {
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CArraySubscriptExpression exp) throws UnrecognizedCCodeException {
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CCharLiteralExpression exp) throws UnrecognizedCCodeException {
      // TODO Check if correct
      return SMGKnownExpValue.valueOf(exp.getValue());
    }

    @Override
    public SMGExplicitValue visit(CFieldReference exp) throws UnrecognizedCCodeException {
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CTypeIdExpression typeIdExp) throws UnrecognizedCCodeException {

      TypeIdOperator typeOperator = typeIdExp.getOperator();
      CType type = typeIdExp.getType();

      switch (typeOperator) {
      case SIZEOF:
        return SMGKnownExpValue.valueOf(getSizeof(cfaEdge, type));
      default:
        return SMGUnknownValue.getInstance();
        //TODO Investigate the other Operators.
      }
    }

    @Override
    public SMGExplicitValue visit(CCastExpression pE) throws CPATransferException {
      return pE.getOperand().accept(this);
    }

    @Override
    public SMGExplicitValue visit(CComplexCastExpression pE) throws CPATransferException {
      // TODO evaluating complex numbers is not supported by now
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CFunctionCallExpression pIastFunctionCallExpression)
        throws UnrecognizedCCodeException {
      return SMGUnknownValue.getInstance();
    }
  }

  SMGAddress evaluateArraySubscriptExpression(SMGState smgState, CFAEdge cfaEdge,
      CArraySubscriptExpression exp) throws CPATransferException {

    SMGAddress arrayMemoryAndOffset =
        evaluateArrayExpression(smgState, cfaEdge, exp.getArrayExpression());

    if (arrayMemoryAndOffset.isUnknown()) {
      return arrayMemoryAndOffset;
    }

    SMGExplicitValue subscriptValue = evaluateExplicitValue(smgState, cfaEdge, exp.getSubscriptExpression());

    if (subscriptValue.isUnknown()) {
      return SMGAddress.UNKNOWN;
    }

    SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge, exp.getExpressionType()));

    SMGExplicitValue subscriptOffset = subscriptValue.multiply(typeSize);

    return arrayMemoryAndOffset.add(subscriptOffset);
  }

  private SMGAddress evaluateArrayExpression(SMGState smgState, CFAEdge cfaEdge,
      CExpression arrayExpression) throws CPATransferException {

    CType arrayExpressionType = getRealExpressionType(arrayExpression);

    if (arrayExpressionType instanceof CPointerType) {

      SMGAddressValue address = evaluateAddress(smgState, cfaEdge, arrayExpression);

      return address.getAddress();

    } else if (arrayExpressionType instanceof CArrayType) {

      ArrayVisitor visitor = getArrayVisitor(cfaEdge, smgState);

      return arrayExpression.accept(visitor);
    } else {
      return SMGAddress.UNKNOWN;
    }
  }

  private StructAndUnionVisitor getStructAndUnionVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new StructAndUnionVisitor(pCfaEdge, pNewState);
  }

  private ArrayVisitor getArrayVisitor(CFAEdge pCfaEdge, SMGState pSmgState) {
    return new ArrayVisitor(pCfaEdge, pSmgState);
  }

  protected PointerVisitor getPointerVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new PointerVisitor(pCfaEdge, pNewState);
  }

  private ExpressionValueVisitor getAssumeVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new AssumeVisitor(pCfaEdge, pNewState);
  }

  protected ExpressionValueVisitor getExpressionValueVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new ExpressionValueVisitor(pCfaEdge, pNewState);
  }

  public LValueAssignmentVisitor getLValueAssignmentVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new LValueAssignmentVisitor(pCfaEdge, pNewState);
  }

  public LogManager getLogger() {
    return logger;
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }
}
