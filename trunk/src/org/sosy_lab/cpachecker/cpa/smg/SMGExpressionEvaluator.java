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
package org.sosy_lab.cpachecker.cpa.smg;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
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
import org.sosy_lab.cpachecker.cpa.explicit.AbstractExplicitExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGField;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * This class evaluates expressions using {@link SMGState}.
 * It should not change the {@link SMGState}, to permit
 * evaluating expressions independently of the transfer relation,
 * enabling other cpas to interact more easily with SMGCPA.
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
      SMGExplicitValue pOffset, CType pType, CFAEdge pEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    if (pOffset.isUnknown() || pObject == null) {
      return SMGUnknownValue.getInstance();
    }

    int fieldOffset = pOffset.getAsInt();

    boolean doesNotFitIntoObject = fieldOffset < 0
        || fieldOffset + getSizeof(pEdge, pType) > pObject.getSize();

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(Level.WARNING, "Field " + "(" + fieldOffset + ", " + pType.toASTString("") + ")" +
          " does not fit object " + pObject.toString() + ".\n Line: " + pEdge.getLineNumber());

      // TODO Modifying read state, ugly ...
      pSmgState.setInvalidRead();
      return SMGUnknownValue.getInstance();
    }

    Integer value = pSmgState.readValue(pObject, fieldOffset, pType);

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

    ExplicitValueVisitor visitor = new ExplicitValueVisitor(smgState, null, machineModel, logger, cfaEdge);

    Long value = rValue.accept(visitor);

    if (value == null) {
      return SMGUnknownValue.getInstance();
    } else {
      return SMGKnownExpValue.valueOf(value);
    }
  }

  public SMGSymbolicValue evaluateExpressionValue(SMGState smgState, CFAEdge cfaEdge,
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

  public SMGSymbolicValue evaluateAssumptionValue(SMGState newState, CFAEdge cfaEdge, CExpression rValue)
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

  /**
   * This class evaluates expressions that evaluate to a
   * pointer type. The type of every expression visited by this
   * visitor has to be a {@link CPointerType }. The result
   * of this evaluation is a {@link SMGAddressValue}.
   * The object and the offset of the result represent
   * the address this pointer points to. The value represents
   * the value of the address itself. Note that the offset of
   * pointer addresses that point to the null object represent
   * also the explicit value of the pointer.
   */
  class PointerVisitor extends ExpressionValueVisitor {

    public PointerVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
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
    public SMGAddressValue visit(CIdExpression exp) throws CPATransferException {

      CType c = getRealExpressionType(exp);

      if (c instanceof CArrayType) {
        // a == &a[0];
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
      case PLUS:
      default:
        // Can't evaluate these Addresses
        // TODO we can, when the pointer points to the Null Object
        return SMGUnknownValue.getInstance();
      }
    }

    private SMGAddressValue handleAmper(CRightHandSide amperOperand) throws CPATransferException {
      if (amperOperand instanceof CIdExpression) {
        // &a
        return createAddressOfVariable((CIdExpression) amperOperand);
      } else if (amperOperand instanceof CPointerExpression) {
        // &(*(a))

        CExpression rValue = ((CPointerExpression) amperOperand).getOperand();

        return evaluateAddress(smgState, cfaEdge, rValue);
      } else if (amperOperand instanceof CFieldReference) {
        // &(a.b)
        return createAddressOfField((CFieldReference) amperOperand);
      } else if (amperOperand instanceof CArraySubscriptExpression) {
        // &(a[b])
        return createAddressOfArraySubscript((CArraySubscriptExpression) amperOperand);
      } else {
        return SMGUnknownValue.getInstance();
      }
    }

    private SMGAddressValue createAddressOfArraySubscript(CArraySubscriptExpression lValue)
        throws CPATransferException {

      CExpression arrayExpression = lValue.getArrayExpression();

      SMGAddressValue arrayAddress = evaluateAddress(smgState, cfaEdge, arrayExpression);

      if (arrayAddress.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      CExpression subscriptExpr = lValue.getSubscriptExpression();

      SMGExplicitValue subscriptValue = evaluateExplicitValue(smgState, cfaEdge, subscriptExpr);

      if (subscriptValue.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      SMGExplicitValue arrayOffset = arrayAddress.getOffset();

      int typeSize = getSizeof(cfaEdge, getRealExpressionType(lValue));

      SMGExplicitValue sizeOfType = SMGKnownExpValue.valueOf(typeSize);

      SMGExplicitValue offset = arrayOffset.add(subscriptValue).multiply(sizeOfType);

      return createAddress(smgState, arrayAddress.getObject(), offset);
    }

    private SMGAddressValue createAddressOfField(CFieldReference lValue) throws CPATransferException {

      SMGAddress addressOfField = getAddressOfField(smgState, cfaEdge, lValue);

      if (addressOfField.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

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

      return getAddressFromSymbolicValue(smgState, super.visit(pointerExpression));
    }

    @Override
    public SMGAddressValue visit(CBinaryExpression binaryExp) throws CPATransferException {

      CExpression lVarInBinaryExp = binaryExp.getOperand1();
      CExpression rVarInBinaryExp = binaryExp.getOperand2();
      CType lVarInBinaryExpType = getRealExpressionType(lVarInBinaryExp);
      CType rVarInBinaryExpType = getRealExpressionType(rVarInBinaryExp);

      boolean lVarIsAddress = lVarInBinaryExpType instanceof CPointerType;
      boolean rVarIsAddress = rVarInBinaryExpType instanceof CPointerType;

      CExpression address = null;
      CExpression pointerOffset = null;
      CPointerType addressType = null;

      if (lVarIsAddress == rVarIsAddress) {
        return SMGUnknownValue.getInstance(); // If both or neither are Addresses,
        //  we can't evaluate the address this pointer stores.
      } else if (lVarIsAddress) {
        address = lVarInBinaryExp;
        pointerOffset = rVarInBinaryExp;
        addressType = (CPointerType) lVarInBinaryExpType;
      } else if (rVarIsAddress) {
        address = rVarInBinaryExp;
        pointerOffset = lVarInBinaryExp;
        addressType = (CPointerType) rVarInBinaryExpType;
      } else {
        // TODO throw Exception, no Pointer
        return SMGUnknownValue.getInstance();
      }

      CType typeOfPointer = addressType.getType().getCanonicalType();

      return handlePointerArithmetic(getSmgState(), getCfaEdge(),
          address, pointerOffset, typeOfPointer, lVarIsAddress,
          binaryExp);
    }

    @Override
    public SMGAddressValue visit(CArraySubscriptExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }

    @Override
    public SMGAddressValue visit(CFieldReference exp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }

    @Override
    public SMGAddressValue visit(CCastExpression pCast) throws CPATransferException {
      // TODO Maybe cast values to pointer to null Object with offset as explicit value
      // for pointer arithmetic substraction ((void *) 4) - ((void *) 3)?
      return getAddressFromSymbolicValue(smgState, super.visit(pCast));
    }
  }

  private SMGAddressValue handlePointerArithmetic(SMGState smgState,
      CFAEdge cfaEdge, CExpression address, CExpression pointerOffset,
      CType typeOfPointer, boolean lVarIsAddress,
      CBinaryExpression binaryExp) throws CPATransferException {

    BinaryOperator binaryOperator = binaryExp.getOperator();

    switch (binaryOperator) {
    case PLUS:
    case MINUS: {

      SMGAddressValue addressValue = evaluateAddress(smgState, cfaEdge, address);

      SMGExplicitValue offsetValue = evaluateExplicitValue(smgState, cfaEdge, pointerOffset);

      if (addressValue.isUnknown() || offsetValue.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge, typeOfPointer));

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
          // TODO throw Exception, is invalid expression
          return SMGUnknownValue.getInstance();
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
    case MODULO:
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
      throw new UnrecognizedCCodeException("The operands of binary Expression "
          + binaryExp.toASTString() + " must have arithmetic types. "
          + address.toASTString() + " has a non arithmetic type",
          cfaEdge, binaryExp);

    default:
      return SMGUnknownValue.getInstance();
    }
  }

  private SMGAddress evaluateArraySubscriptAddress(SMGState smgState, CFAEdge cfaEdge,
      CArraySubscriptExpression exp) throws CPATransferException {

    SMGAddressValue arrayAddress = evaluateAddress(smgState, cfaEdge, exp.getArrayExpression());

    if (arrayAddress.isUnknown()) {
      return SMGAddress.UNKNOWN;
    }

    SMGExplicitValue subscriptValue = evaluateExplicitValue(smgState, cfaEdge, exp.getSubscriptExpression());

    if (subscriptValue.isUnknown()) {
      return SMGAddress.UNKNOWN;
    }

    SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge, exp.getExpressionType()));

    SMGExplicitValue subscriptOffset = subscriptValue.multiply(typeSize);

    return arrayAddress.getAddress().add(subscriptOffset);
  }

  SMGAddressValue createAddress(SMGEdgePointsTo pEdge) {

    if (pEdge == null) {
      return SMGUnknownValue.getInstance();
    }

    return SMGKnownAddVal.valueOf(pEdge.getValue(), pEdge.getObject(), pEdge.getOffset());
  }

  private SMGAddressValue createAddress(SMGState pNewState, SMGAddress pAddress) throws SMGInconsistentException {

    if (pAddress.isUnknown()) {
      return SMGUnknownValue.getInstance();
    }

    return createAddress(pNewState, pAddress.getObject(), pAddress.getOffset());
  }

  /**
   * Is given a symbolic Value, looks into the smg to determine if the symbolic
   * value represents a pointer, and transforms it into a {@link SMGAddressValue}
   * containing the symbolic value that represents the pointer as well as the
   * address the pointer is pointing to.
   *
   * Because all values in C represent an
   * address, and can e cast to a pointer, the method returns a instance of
   * {@link SMGUnknownValue} if the symbolic value does not represent a pointer
   * in the smg.
   *
   *
   *
   * @param pSmgState This contains the SMG.
   * @param pAddressValue the symbolic value that may represent a pointer in the smg
   * @return The address, otherwise unknown
   * @throws SMGInconsistentException thrown if the symbolic address is misinterpreted as a pointer.
   */
  SMGAddressValue getAddressFromSymbolicValue(SMGState pSmgState,
      SMGSymbolicValue pAddressValue) throws SMGInconsistentException {

    if (pAddressValue instanceof SMGAddressValue) {
      return (SMGAddressValue) pAddressValue;
    }

    if (pAddressValue.isUnknown()) {
      return SMGUnknownValue.getInstance();
    }

    if(!pSmgState.isPointer(pAddressValue.getAsInt())) {
      return SMGUnknownValue.getInstance();
    }

    SMGEdgePointsTo edge = pSmgState.getPointerFromValue(pAddressValue.getAsInt());

    return createAddress(edge);
  }

  SMGAddressValue createAddress(SMGState pSmgState, SMGObject pTarget, SMGExplicitValue pOffset)
      throws SMGInconsistentException {

    SMGAddressValue addressValue = getAddress(pSmgState, pTarget, pOffset);

    if (addressValue.isUnknown()) {

      SMGKnownSymValue value = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
      addressValue = SMGKnownAddVal.valueOf(pTarget, (SMGKnownExpValue)pOffset, value);
    }

    return addressValue;
  }

  SMGAddressValue getAddress(SMGState pSmgState, SMGObject pTarget,
      SMGExplicitValue pOffset) throws SMGInconsistentException {

    if (pTarget == null || pOffset.isUnknown()) {
      return SMGUnknownValue.getInstance();
    }

    Integer address = pSmgState.getAddress(pTarget, pOffset.getAsInt());

    if (address == null) {
      return SMGUnknownValue.getInstance();
    }

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
      throw new AssertionError("The result of any unary expression " +
          "cannot be an array type.");
    }

    @Override
    public SMGAddress visit(CBinaryExpression binaryExp) throws CPATransferException {

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

      // a = &a[0]
      SMGAddressValue result =
          handlePointerArithmetic(getSmgState(), getCfaEdge(),
              address, arrayOffset, addressType, lVarIsAddress, binaryExp);
      return result.getAddress();
    }

    @Override
    public SMGAddress visit(CCastExpression cast) throws CPATransferException {
      //TODO Bug, can introduce non array type in visitor
      return cast.getOperand().accept(this);
    }

    @Override
    public SMGAddress visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      return SMGAddress.UNKNOWN;
    }
  }

  class AssumeVisitor extends ExpressionValueVisitor {
    private BinaryRelationEvaluator relation = null;

    public AssumeVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
    }

    @Override
    public SMGSymbolicValue visit(CBinaryExpression pExp) throws CPATransferException {
      BinaryOperator binaryOperator = pExp.getOperator();

      switch (binaryOperator) {
      case EQUALS:
      case NOT_EQUALS:
      case LESS_EQUAL:
      case LESS_THAN:
      case GREATER_EQUAL:
      case GREATER_THAN:
        CExpression leftSideExpression = pExp.getOperand1();
        CExpression rightSideExpression = pExp.getOperand2();

        CFAEdge cfaEdge = getCfaEdge();

        SMGSymbolicValue leftSideVal = evaluateExpressionValue(smgState, cfaEdge, leftSideExpression);
        if (leftSideVal.isUnknown()) { return SMGUnknownValue.getInstance(); }
        SMGSymbolicValue rightSideVal = evaluateExpressionValue(smgState, cfaEdge, rightSideExpression);
        if (rightSideVal.isUnknown()) { return SMGUnknownValue.getInstance(); }

        SMGKnownSymValue knownRightSideVal = SMGKnownSymValue.valueOf(rightSideVal.getAsInt());
        SMGKnownSymValue knownLeftSideVal = SMGKnownSymValue.valueOf(leftSideVal.getAsInt());
        return evaluateBinaryAssumption(binaryOperator, knownLeftSideVal, knownRightSideVal);
      default:
        return super.visit(pExp);
      }
    }

    private class BinaryRelationEvaluator {

      private boolean isTrue = false;
      private boolean isFalse = false;

      private boolean impliesEqWhenTrue = false;
      private boolean impliesNeqWhenTrue = false;
      private boolean impliesEqWhenFalse = false;
      private boolean impliesNeqWhenFalse = false;

      public BinaryRelationEvaluator(BinaryOperator pOp, SMGSymbolicValue pV1, SMGSymbolicValue pV2) throws SMGInconsistentException {
        int v1 = pV1.getAsInt();
        int v2 = pV2.getAsInt();

        boolean areEqual = (v1 == v2);
        boolean areNonEqual = (smgState.isUnequal(v1, v2));

        switch (pOp) {
        case NOT_EQUALS:
          isTrue = areNonEqual;
          isFalse = areEqual;
          impliesEqWhenFalse = true;
          impliesNeqWhenTrue = true;
          break;
        case EQUALS:
          isTrue = areEqual;
          isFalse = areNonEqual;
          impliesEqWhenTrue = true;
          impliesNeqWhenFalse = true;
          break;
        case LESS_EQUAL:
        case GREATER_EQUAL:
          if (v1 == v2) {
            isTrue = true;
            impliesEqWhenTrue = true;
            impliesNeqWhenFalse = true;
          } else {
            impliesNeqWhenFalse = true;
            compareAsAddresses(pV1, pV2, pOp);
          }
          break;
        case GREATER_THAN:
        case LESS_THAN:
          compareAsAddresses(pV1, pV2, pOp);
          impliesNeqWhenTrue = true;
          break;
        default:
          throw new AssertionError("Binary Relation with non-relational operator: " + pOp.toString());
        }
      }

      // This method is dependent on the callsite, and is only called for greater/less
      // operators, because we can evaluate equality in a general way
      // TODO: make this callsite-independent
      // TODO: improve handling of the equal variants (remote the code duplication)
      private void compareAsAddresses(SMGSymbolicValue lVal, SMGSymbolicValue rVal, BinaryOperator binaryOperator) throws SMGInconsistentException {
        SMGAddressValue lAddress = getAddressFromSymbolicValue(getSmgState(), lVal);
        SMGAddressValue rAddress = getAddressFromSymbolicValue(getSmgState(), rVal);

        if (rAddress.isUnknown() || lAddress.isUnknown()) {
          return;
        }

        SMGObject lObject = lAddress.getObject();
        SMGObject rObject = rAddress.getObject();

        if (!lObject.equals(rObject)) {
          return;
        }

        long rOffset = rAddress.getOffset().getAsLong();
        long lOffset = lAddress.getOffset().getAsLong();

        // We already checked equality
        switch (binaryOperator) {
        case LESS_THAN:
          isTrue = lOffset < rOffset;
          isFalse = !isTrue;
          break;
        case LESS_EQUAL:
          isTrue = lOffset <= rOffset;
          isFalse = !isTrue;
          break;
        case GREATER_EQUAL:
          isTrue = lOffset > rOffset;
          isFalse = !isTrue;
          break;
        case GREATER_THAN:
          isTrue = lOffset > rOffset;
          isFalse = !isTrue;
          break;
        default:
          throw new AssertionError("compareAsAddresses shouldn't be called for operators not being LE/LT/GE/GT");
        }
      }

      public boolean isTrue() {
        return isTrue;
      }

      public boolean isFalse() {
        return isFalse;
      }

      public boolean impliesEq(boolean pTruth) {
        return pTruth ? impliesEqWhenTrue : impliesEqWhenFalse;
      }

      public boolean impliesNeq(boolean pTruth) {
        return pTruth ? impliesNeqWhenTrue : impliesNeqWhenFalse;
      }
    }

    private SMGSymbolicValue evaluateBinaryAssumption(BinaryOperator pOp, SMGKnownSymValue v1, SMGKnownSymValue v2) throws SMGInconsistentException {
      relation = new BinaryRelationEvaluator(pOp, v1, v2);
      if (relation.isFalse()) {
        return SMGKnownSymValue.FALSE;
      } else if (relation.isTrue()) {
        return SMGKnownSymValue.TRUE;
      }

      return SMGUnknownValue.getInstance();
    }

    @SuppressWarnings("unused")
    public boolean impliesEqOn(boolean pTruth) {
      if (relation == null) {
        return false;
      }
      return relation.impliesEq(pTruth);
    }

    @SuppressWarnings("unused")
    public boolean impliesNeqOn(boolean pTruth) {
      if (relation == null) {
        return false;
      }
      return relation.impliesNeq(pTruth);

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
  class StructAndUnionVisitor extends AddressVisitor
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

    protected final CFAEdge cfaEdge;
    protected final SMGState smgState;

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

      SMGAddress address = evaluateArraySubscriptAddress(smgState, cfaEdge, exp);

      if (address.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      SMGSymbolicValue value = readValue(smgState, address.getObject(), address.getOffset(), getRealExpressionType(exp), cfaEdge);

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

      if (addressOfField.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

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
        throw new UnrecognizedCCodeException("on pointer expression", cfaEdge, pointerExpression);
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

      if (isUnequal(smgState, value, SMGKnownSymValue.ZERO)) {
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
        return getSizeof(cfaEdge, type) == 0 ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
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

    private boolean isUnequal(SMGState pSmgState, SMGSymbolicValue pLVal, SMGSymbolicValue pRVal)
        throws SMGInconsistentException {

      if (pLVal.isUnknown() || pRVal.isUnknown()) {
        return false;
      }

      return pSmgState.isUnequal(pLVal.getAsInt(), pRVal.getAsInt());
    }

    @Override
    public SMGSymbolicValue visit(CCastExpression cast) throws CPATransferException {
      // For different types we need different visitors,
      // TODO doesn't calculate type reinterpretations
      return evaluateExpressionValue(getSmgState(), getCfaEdge(), cast.getOperand());
    }

    protected SMGSymbolicValue dereferenceArray(CExpression exp, CType derefType) throws CPATransferException {

      ArrayVisitor v = getArrayVisitor(cfaEdge, smgState);

      SMGAddress address = exp.accept(v);

      if (address.isUnknown()) {
        // We can't resolve the field to dereference, therefore
        // we must assume, that it is invalid
        smgState.setUnknownDereference(); //TODO technically not allowed here, changes smgState semantically
        return SMGUnknownValue.getInstance();
      }

      // a == &a[0]
      if (derefType instanceof CArrayType) {
        return createAddress(smgState, address.getObject(), address.getOffset());
      } else {
        return readValue(smgState, address.getObject(), address.getOffset(), derefType, cfaEdge);
      }
    }

    protected final SMGSymbolicValue dereferencePointer(CExpression exp, CType derefType)
        throws CPATransferException {

      SMGAddressValue address = evaluateAddress(smgState, cfaEdge, exp);

      if (address.isUnknown()) {
        // We can't resolve the field to dereference , therefore
        // we must assume, that it is invalid
        smgState.setUnknownDereference(); //TODO technically not allowed here, changes smgState semantically
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

  class ExplicitValueVisitor extends AbstractExplicitExpressionValueVisitor {

    private final SMGState smgState;

    public ExplicitValueVisitor(SMGState pSmgState,
        String pFunctionName, MachineModel pMachineModel,
        LogManager pLogger, CFAEdge pEdge) {
      super(pFunctionName, pMachineModel, pLogger, pEdge);
      smgState = pSmgState;
    }

    private Long getExplicitValue(SMGSymbolicValue pValue) {

      if (pValue.isUnknown()) {
        return null;
      }

      SMGExplicitValue explicitValue = smgState.getExplicit((SMGKnownSymValue) pValue);

      if (explicitValue.isUnknown()) {
        return null;
      }

      return explicitValue.getAsLong();
    }

    @Override
    protected Long evaluateCPointerExpression(CPointerExpression pCPointerExpression) throws UnrecognizedCCodeException {
      try {
        return getExplicitValue(evaluateExpressionValue(smgState, getEdge(), pCPointerExpression));
      } catch (CPATransferException e) {
        UnrecognizedCCodeException e2 =
            new UnrecognizedCCodeException("SMG cannot be evaluated", getEdge(), pCPointerExpression);
        e2.initCause(e);
        throw e2;
      }
    }

    @Override
    protected Long evaluateCIdExpression(CIdExpression pCIdExpression) throws UnrecognizedCCodeException {
      try {
        return getExplicitValue(evaluateExpressionValue(smgState, getEdge(), pCIdExpression));
      } catch (CPATransferException e) {
        UnrecognizedCCodeException e2 =
            new UnrecognizedCCodeException("SMG cannot be evaluated", getEdge(), pCIdExpression);
        e2.initCause(e);
        throw e2;
      }
    }

    @Override
    protected Long evaluateJIdExpression(JIdExpression pVarName) {
      return null;
    }

    @Override
    protected Long evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException {
      try {
        return getExplicitValue(evaluateExpressionValue(smgState, getEdge(), pLValue));
      } catch (CPATransferException e) {
        UnrecognizedCCodeException e2 = new UnrecognizedCCodeException("SMG cannot be evaluated", getEdge(), pLValue);
        e2.initCause(e);
        throw e2;
      }
    }

    @Override
    protected Long evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
        throws UnrecognizedCCodeException {
      try {
        return getExplicitValue(evaluateExpressionValue(smgState, getEdge(), pLValue));
      } catch (CPATransferException e) {
        UnrecognizedCCodeException e2 = new UnrecognizedCCodeException("SMG cannot be evaluated", getEdge(), pLValue);
        e2.initCause(e);
        throw e2;
      }
    }

  }


  /*
   class ExplicitValueVisitor extends DefaultCExpressionVisitor<SMGExplicitValue, CPATransferException>
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

        if (lValue.isUnknown()) {
          return SMGUnknownValue.getInstance();
        }

        SMGExplicitValue rValue = rVarInBinaryExp.accept(this);
        if (rValue.isUnknown()) {
          return SMGUnknownValue.getInstance();
        }

        switch (binaryOperator) {
        case PLUS:
          return lValue.add(rValue);

        case MINUS:
          return lValue.subtract(rValue);

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if (rValue.equals(SMGKnownExpValue.ZERO)) {
            return SMGUnknownValue.getInstance();
          }

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

      if (decl instanceof CEnumerator) {
        return SMGKnownExpValue.valueOf(((CEnumerator) decl).getValue());
      }

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
        SMGAddressValue address = evaluateAddress(smgState, cfaEdge, unaryExpression);

        if (address.isUnknown() || address.getObject().notNull()) {
          // valid expression, but we don't have explicit values for addresses.
          return SMGUnknownValue.getInstance();
        } else {
          // If the returned Address points to the null object, the value is its offset
          return address.getOffset();
        }

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
    public SMGExplicitValue visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
      return SMGUnknownValue.getInstance();
    }
  }

   */

  private StructAndUnionVisitor getStructAndUnionVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new StructAndUnionVisitor(pCfaEdge, pNewState);
  }

  private ArrayVisitor getArrayVisitor(CFAEdge pCfaEdge, SMGState pSmgState) {
    return new ArrayVisitor(pCfaEdge, pSmgState);
  }

  protected PointerVisitor getPointerVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new PointerVisitor(pCfaEdge, pNewState);
  }

  protected AssumeVisitor getAssumeVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
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

