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
package org.sosy_lab.cpachecker.cpa.smg;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGEdgePointsToAndState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGField;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * This class evaluates expressions using {@link SMGState}.
 * It should not change the {@link SMGState}, to permit
 * evaluating expressions independently of the transfer relation,
 * enabling other cpas to interact more easily with SMGCPA.
 */
public class SMGExpressionEvaluator {

  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;

  public SMGExpressionEvaluator(LogManagerWithoutDuplicates pLogger, MachineModel pMachineModel) {
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
    public SMGAddressAndState visit(CUnaryExpression lValue) throws CPATransferException {

      throw new UnrecognizedCCodeException(lValue.toASTString() + " is not an lValue", getCfaEdge(), lValue);
    }

    @Override
    public SMGAddressAndState visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      throw new AssertionError("This expression is not a lValue expression.");
    }
  }

  private SMGAddressAndState getAddressOfField(SMGState pSmgState, CFAEdge cfaEdge, CFieldReference fieldReference)
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

    SMGAddressValueAndState fieldOwnerAddressAndState = evaluateAddress(pSmgState, cfaEdge, fieldOwner);

    SMGAddressValue fieldOwnerAddress = fieldOwnerAddressAndState.getValue();
    SMGState newState = fieldOwnerAddressAndState.getSmgState();

    if (fieldOwnerAddress.isUnknown()) {
      return  SMGAddressAndState.of(newState);
    }

    String fieldName = fieldReference.getFieldName();

    SMGField field = getField(cfaEdge, ownerType, fieldName);

    if (field.isUnknown()) {
      return SMGAddressAndState.of(newState);
    }

    SMGAddress addressOfFieldOwner = fieldOwnerAddress.getAddress();

    SMGExplicitValue fieldOffset = addressOfFieldOwner.add(field.getOffset()).getOffset();

    SMGObject fieldObject = addressOfFieldOwner.getObject();

    SMGAddress address = SMGAddress.valueOf(fieldObject, fieldOffset);

    return SMGAddressAndState.of(newState, address);
  }

  public SMGValueAndState readValue(SMGState pSmgState, SMGObject pObject,
      SMGExplicitValue pOffset, CType pType, CFAEdge pEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    if (pOffset.isUnknown() || pObject == null) {
      return SMGValueAndState.of(pSmgState);
    }

    int fieldOffset = pOffset.getAsInt();

    boolean doesNotFitIntoObject = fieldOffset < 0
        || fieldOffset + getSizeof(pEdge, pType) > pObject.getSize();

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(Level.WARNING, pEdge.getFileLocation() + ":", "Field " + "("
          + fieldOffset + ", " + pType.toASTString("") + ")"
          + " does not fit object " + pObject.toString() + ".");

      return SMGValueAndState.of(pSmgState);
    }

    // We don't want to modify the state while reading
    SMGSymbolicValue value = pSmgState.readValue(pObject, fieldOffset, pType).getValue();

    return SMGValueAndState.of(pSmgState, value);
  }

  private SMGField getField(CFAEdge edge, CType ownerType, String fieldName) throws UnrecognizedCCodeException {

    if (ownerType instanceof CElaboratedType) {

      CType realType = ((CElaboratedType) ownerType).getRealType();

      if (realType == null) {
        return SMGField.getUnknownInstance();
      }

      return getField(edge, realType, fieldName);
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

  public SMGExplicitValue evaluateExplicitValueV2(SMGState smgState,
      CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {
    return evaluateExplicitValue(smgState, cfaEdge, rValue).getValue();
  }

  protected SMGExplicitValueAndState evaluateExplicitValue(SMGState smgState,
      CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    ExplicitValueVisitor visitor = new ExplicitValueVisitor(smgState, null, machineModel, logger, cfaEdge);

    Value value = rValue.accept(visitor);
    SMGState newState = visitor.getNewState();

    if (!value.isExplicitlyKnown() || !value.isNumericValue()) {

      // Sometimes, we can get the explicit Value from SMGCPA, especially if the
      // result happens to
      // be a pointer to the Null Object, or through reinterpretation
      SMGValueAndState symbolicValueAndState = evaluateExpressionValue(
          newState, cfaEdge, rValue);

      SMGSymbolicValue symbolicValue = symbolicValueAndState.getValue();
      newState = symbolicValueAndState.getSmgState();

      if (!symbolicValue.isUnknown()) {
        if (symbolicValue == SMGKnownSymValue.ZERO) {
          return SMGExplicitValueAndState.of(newState, SMGKnownExpValue.ZERO);
        }

        if (symbolicValue instanceof SMGAddressValue) {
          SMGAddressValue address = (SMGAddressValue) symbolicValue;

          if (address.getObject() == SMGObject.getNullObject()) {
            return SMGExplicitValueAndState.of(newState, SMGKnownExpValue.valueOf(address.getOffset().getAsLong()));
          }
        }
      }

      return SMGExplicitValueAndState.of(newState);
    } else {
      long longValue = value.asNumericValue().longValue();
      return SMGExplicitValueAndState.of(newState, SMGKnownExpValue.valueOf(longValue));
    }
  }

  public SMGSymbolicValue evaluateExpressionValueV2(SMGState smgState,
      CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {
    return evaluateExpressionValue(smgState, cfaEdge, rValue).getValue();
  }

  protected SMGValueAndState evaluateExpressionValue(SMGState smgState, CFAEdge cfaEdge,
      CRightHandSide rValue) throws CPATransferException {

    CType expressionType = getRealExpressionType(rValue);

    if (expressionType instanceof CPointerType
        || expressionType instanceof CArrayType
        || isStructOrUnionType(expressionType)
        || expressionType instanceof CFunctionType) {
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

  private SMGValueAndState evaluateNonAddressValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    ExpressionValueVisitor visitor = getExpressionValueVisitor(cfaEdge, newState);

    SMGValueAndState symbolicValue = rValue.accept(visitor);

    return symbolicValue;
  }

  protected SMGValueAndState evaluateAssumptionValue(SMGState newState,
      CFAEdge cfaEdge, CExpression rValue) throws CPATransferException {

    ExpressionValueVisitor visitor = getAssumeVisitor(cfaEdge, newState);
    return rValue.accept(visitor);
  }

  public SMGSymbolicValue evaluateAssumptionValueV2(SMGState newState,
      CFAEdge cfaEdge, CExpression rValue) throws CPATransferException {

    return evaluateAssumptionValue(newState, cfaEdge, rValue).getValue();
  }

  protected SMGAddressValueAndState evaluateAddress(SMGState pState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    CType expressionType = getRealExpressionType(rValue);

    if (expressionType instanceof CFunctionType) {

      // TODO Represantation of functions

      return SMGAddressValueAndState.of(pState, SMGUnknownValue.getInstance());
    } else if (expressionType instanceof CPointerType) {

      PointerVisitor visitor = getPointerVisitor(cfaEdge, pState);

      SMGValueAndState addressAndState = rValue.accept(visitor);
      return getAddressFromSymbolicValue(addressAndState);
    } else if (isStructOrUnionType(expressionType)) {
      /* expressions with structs or unions as
       * result will be evaluated to their addresses.
       * The address can be used e.g. to copy the struct.
       */

      StructAndUnionVisitor visitor = getStructAndUnionVisitor(cfaEdge, pState);

      SMGAddressAndState structAddressAndState = rValue.accept(visitor);
      SMGState newState = structAddressAndState.getSmgState();
      SMGAddress structAddress = structAddressAndState.getAddress();
      return createAddress(newState, structAddress);
    } else if (expressionType instanceof CArrayType) {

      ArrayVisitor visitor = getArrayVisitor(cfaEdge, pState);

      SMGAddressAndState arrayAddressAndState = rValue.accept(visitor);
      SMGAddress arrayAddress = arrayAddressAndState.getAddress();
      SMGState newState = arrayAddressAndState.getSmgState();
      return createAddress(newState, arrayAddress);
    } else {
      throw new AssertionError("The method evaluateAddress may not be called" +
          "with the type " + expressionType.toASTString(""));
    }
  }

  public SMGAddressValue evaluateAddressV2(SMGState newState, CFAEdge cfaEdge,
      CRightHandSide rValue) throws CPATransferException {
    return evaluateAddress(newState, cfaEdge, rValue).getValue();
  }

  public CType getRealExpressionType(CType type) {
    return type.getCanonicalType();
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
  private abstract class AddressVisitor extends DefaultCExpressionVisitor<SMGAddressAndState, CPATransferException>
      implements CRightHandSideVisitor<SMGAddressAndState, CPATransferException> {

    private final CFAEdge cfaEdge;
    private final SMGState initialSmgState;

    public AddressVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      initialSmgState = pSmgState;
    }

    @Override
    protected SMGAddressAndState visitDefault(CExpression pExp) throws CPATransferException {
      return SMGAddressAndState.of(getInitialSmgState());
    }

    @Override
    public SMGAddressAndState visit(CIdExpression variableName) throws CPATransferException {

      SMGObject object = getInitialSmgState().getObjectForVisibleVariable(variableName.getName());

      return SMGAddressAndState.of(getInitialSmgState(), SMGAddress.valueOf(object, SMGKnownExpValue.ZERO));
    }

    @Override
    public SMGAddressAndState visit(CArraySubscriptExpression exp) throws CPATransferException {
      return evaluateArraySubscriptAddress(getInitialSmgState(), getCfaEdge(), exp);
    }

    @Override
    public SMGAddressAndState visit(CFieldReference pE) throws CPATransferException {
      return getAddressOfField(getInitialSmgState(), getCfaEdge(), pE);
    }

    @Override
    public SMGAddressAndState visit(CPointerExpression pointerExpression)
        throws CPATransferException {

      /*
       * The address of a pointer expression (*x) is defined as the evaluation
       * of the pointer x. This is consistent with the meaning of a pointer
       * expression in the left hand side of an assignment *x = ...
       */
      CExpression operand = pointerExpression.getOperand();

      assert operand.getExpressionType().getCanonicalType() instanceof CPointerType
      || operand.getExpressionType().getCanonicalType() instanceof CArrayType;

      SMGAddressValueAndState addressValueAndState = evaluateAddress(
          getInitialSmgState(), getCfaEdge(), operand);
      SMGAddressValue addressValue = addressValueAndState.getValue();
      SMGState newState = addressValueAndState.getSmgState();

      if (addressValue.isUnknown()) {
        return SMGAddressAndState.of(newState);
      }

      return SMGAddressAndState.of(newState, addressValue.getAddress());
    }

    public final CFAEdge getCfaEdge() {
      return cfaEdge;
    }

    public final SMGState getInitialSmgState() {
      return initialSmgState;
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
    public SMGAddressValueAndState visit(CIntegerLiteralExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValue(super.visit(exp));
    }

    @Override
    public SMGAddressValueAndState visit(CCharLiteralExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValue(super.visit(exp));
    }

    @Override
    public SMGAddressValueAndState visit(CFloatLiteralExpression pExp) throws CPATransferException {
      return getAddressFromSymbolicValue(super.visit(pExp));
    }

    @Override
    public SMGAddressValueAndState visit(CIdExpression exp) throws CPATransferException {

      CType c = getRealExpressionType(exp);

      if (c instanceof CArrayType) {
        // a == &a[0];
        return createAddressOfVariable(exp);
      }

      return getAddressFromSymbolicValue(super.visit(exp));
    }

    @Override
    public SMGAddressValueAndState visit(CUnaryExpression unaryExpression) throws CPATransferException {

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
      case TILDE:
      default:
        // Can't evaluate these Addresses
        return SMGAddressValueAndState.of(getInitialSmgState());
      }
    }

    private SMGAddressValueAndState handleAmper(CRightHandSide amperOperand) throws CPATransferException {
      if (amperOperand instanceof CIdExpression) {
        // &a
        return createAddressOfVariable((CIdExpression) amperOperand);
      } else if (amperOperand instanceof CPointerExpression) {
        // &(*(a))

        CExpression rValue = ((CPointerExpression) amperOperand).getOperand();

        return evaluateAddress(getInitialSmgState(), getCfaEdge(), rValue);
      } else if (amperOperand instanceof CFieldReference) {
        // &(a.b)
        return createAddressOfField((CFieldReference) amperOperand);
      } else if (amperOperand instanceof CArraySubscriptExpression) {
        // &(a[b])
        return createAddressOfArraySubscript((CArraySubscriptExpression) amperOperand);
      } else {
        return SMGAddressValueAndState.of(getInitialSmgState());
      }
    }

    private SMGAddressValueAndState createAddressOfArraySubscript(CArraySubscriptExpression lValue)
        throws CPATransferException {

      CExpression arrayExpression = lValue.getArrayExpression();

      SMGAddressValueAndState arrayAddressAndState = evaluateAddress(getInitialSmgState(), getCfaEdge(), arrayExpression);
      SMGAddressValue arrayAddress = arrayAddressAndState.getValue();
      SMGState newState = arrayAddressAndState.getSmgState();

      if (arrayAddress.isUnknown()) {
        return SMGAddressValueAndState.of(newState);
      }

      CExpression subscriptExpr = lValue.getSubscriptExpression();

      SMGExplicitValueAndState subscriptValueAndState = evaluateExplicitValue(
          newState, getCfaEdge(), subscriptExpr);

      SMGExplicitValue subscriptValue = subscriptValueAndState.getValue();
      newState = subscriptValueAndState.getSmgState();

      if (subscriptValue.isUnknown()) {
        return SMGAddressValueAndState.of(newState);
      }

      SMGExplicitValue arrayOffset = arrayAddress.getOffset();

      int typeSize = getSizeof(getCfaEdge(), getRealExpressionType(lValue));

      SMGExplicitValue sizeOfType = SMGKnownExpValue.valueOf(typeSize);

      SMGExplicitValue offset = arrayOffset.add(subscriptValue).multiply(sizeOfType);

      return createAddress(newState, arrayAddress.getObject(), offset);
    }

    private SMGAddressValueAndState createAddressOfField(CFieldReference lValue)
        throws CPATransferException {

      SMGAddressAndState addressOfFieldAndState = getAddressOfField(
          getInitialSmgState(), getCfaEdge(), lValue);
      SMGAddress addressOfField = addressOfFieldAndState.getAddress();
      SMGState newState = addressOfFieldAndState.getSmgState();

      if (addressOfField.isUnknown()) {
        return SMGAddressValueAndState.of(newState);
      }

      return createAddress(addressOfFieldAndState.getSmgState(),
          addressOfField.getObject(), addressOfField.getOffset());
    }

    private SMGAddressValueAndState createAddressOfVariable(CIdExpression idExpression) throws SMGInconsistentException {

      SMGState state = getInitialSmgState();

      SMGObject variableObject = state.getObjectForVisibleVariable(idExpression.getName());

      if (variableObject == null) {
        return SMGAddressValueAndState.of(state);
      } else {
        return createAddress(state, variableObject, SMGKnownExpValue.ZERO);
      }
    }

    @Override
    public SMGAddressValueAndState visit(CPointerExpression pointerExpression) throws CPATransferException {

      return getAddressFromSymbolicValue(super.visit(pointerExpression));
    }

    @Override
    public SMGAddressValueAndState visit(CBinaryExpression binaryExp) throws CPATransferException {

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
        return SMGAddressValueAndState.of(getInitialSmgState()); // If both or neither are Addresses,
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
        throw new UnrecognizedCCodeException("Expected either "
      + lVarInBinaryExp.toASTString() + " or "
      + rVarInBinaryExp.toASTString() +
      "to be a pointer.", binaryExp);
      }

      CType typeOfPointer = addressType.getType().getCanonicalType();

      return handlePointerArithmetic(getInitialSmgState(), getCfaEdge(),
          address, pointerOffset, typeOfPointer, lVarIsAddress,
          binaryExp);
    }

    @Override
    public SMGAddressValueAndState visit(CArraySubscriptExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValue(super.visit(exp));
    }

    @Override
    public SMGAddressValueAndState visit(CFieldReference exp) throws CPATransferException {
      return getAddressFromSymbolicValue(super.visit(exp));
    }

    @Override
    public SMGAddressValueAndState visit(CCastExpression pCast) throws CPATransferException {
      // TODO Maybe cast values to pointer to null Object with offset as explicit value
      // for pointer arithmetic substraction ((void *) 4) - ((void *) 3)?
      return getAddressFromSymbolicValue(super.visit(pCast));
    }
  }

  private SMGAddressValueAndState handlePointerArithmetic(SMGState initialSmgState,
      CFAEdge cfaEdge, CExpression address, CExpression pointerOffset,
      CType typeOfPointer, boolean lVarIsAddress,
      CBinaryExpression binaryExp) throws CPATransferException {

    BinaryOperator binaryOperator = binaryExp.getOperator();

    switch (binaryOperator) {
    case PLUS:
    case MINUS: {

      SMGAddressValueAndState addressValueAndState = evaluateAddress(
          initialSmgState, cfaEdge, address);
      SMGAddressValue addressValue = addressValueAndState.getValue();
      SMGState newState = addressValueAndState.getSmgState();

      SMGExplicitValueAndState offsetValueAndState = evaluateExplicitValue(
          newState, cfaEdge, pointerOffset);
      SMGExplicitValue offsetValue = offsetValueAndState.getValue();
      newState = offsetValueAndState.getSmgState();

      if (addressValue.isUnknown() || offsetValue.isUnknown()) {
        return SMGAddressValueAndState.of(newState);
      }

      SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge, typeOfPointer));

      SMGExplicitValue pointerOffsetValue = offsetValue.multiply(typeSize);

      SMGObject target = addressValue.getObject();

      SMGExplicitValue addressOffset = addressValue.getOffset();

      switch (binaryOperator) {
      case PLUS:
        return createAddress(newState, target, addressOffset.add(pointerOffsetValue));
      case MINUS:
        if (lVarIsAddress) {
          return createAddress(newState, target, addressOffset.subtract(pointerOffsetValue));
        } else {
          throw new UnrecognizedCCodeException("Expected pointer arithmetic "
              + " with + or - but found " + binaryExp.toASTString(), binaryExp);
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
      return SMGAddressValueAndState.of(initialSmgState);
    }
  }

  private SMGAddressAndState evaluateArraySubscriptAddress(
      SMGState initialSmgState, CFAEdge cfaEdge, CArraySubscriptExpression exp)
      throws CPATransferException {

    SMGAddressValueAndState arrayAddressAndState = evaluateAddress(
        initialSmgState, cfaEdge, exp.getArrayExpression());
    SMGAddressValue arrayAddress = arrayAddressAndState.getValue();
    SMGState newState = arrayAddressAndState.getSmgState();

    if (arrayAddress.isUnknown()) {
      // assume address is invalid
      newState = handleUnknownDereference(newState, cfaEdge).getSmgState();
      return SMGAddressAndState.of(newState);
    }

    SMGExplicitValueAndState subscriptValueAndState = evaluateExplicitValue(
        newState, cfaEdge, exp.getSubscriptExpression());
    SMGExplicitValue subscriptValue = subscriptValueAndState.getValue();
    newState = subscriptValueAndState.getSmgState();

    if (subscriptValue.isUnknown()) {
   // assume address is invalid
      //throw new SMGInconsistentException("Can't properly evaluate array subscript");
      newState = handleUnknownDereference(newState, cfaEdge).getSmgState();
      return SMGAddressAndState.of(newState);
    }

    SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge,
        exp.getExpressionType()));

    SMGExplicitValue subscriptOffset = subscriptValue.multiply(typeSize);

    return SMGAddressAndState.of(newState,
        arrayAddress.getAddress().add(subscriptOffset));
  }

  SMGAddressValueAndState createAddress(SMGEdgePointsToAndState pEdgeAndState) {

    SMGEdgePointsTo edge = pEdgeAndState.getValue();
    SMGState newState = pEdgeAndState.getSmgState();

    if (edge == null) {
      return SMGAddressValueAndState.of(newState);
    }

    SMGAddressValue addressVal = SMGKnownAddVal.valueOf(edge.getValue(),
        edge.getObject(), edge.getOffset());

    return SMGAddressValueAndState.of(newState, addressVal);
  }

  private SMGAddressValueAndState createAddress(SMGState pNewState,
      SMGAddress pAddress) throws SMGInconsistentException {

    if (pAddress.isUnknown()) {
      return SMGAddressValueAndState.of(pNewState);
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
  SMGAddressValueAndState getAddressFromSymbolicValue(SMGValueAndState pAddressValueAndState) throws SMGInconsistentException {

    if (pAddressValueAndState instanceof SMGAddressValueAndState) {
      return (SMGAddressValueAndState) pAddressValueAndState;
    }

    SMGSymbolicValue pAddressValue = pAddressValueAndState.getValue();
    SMGState smgState = pAddressValueAndState.getSmgState();

    if (pAddressValue instanceof SMGAddressValue) {
      return SMGAddressValueAndState.of(smgState,
          (SMGAddressValue) pAddressValue);
    }

    if (pAddressValue.isUnknown()) {
      return SMGAddressValueAndState.of(smgState);
    }

    if (!smgState.isPointer(pAddressValue.getAsInt())) {
      return SMGAddressValueAndState.of(smgState);
    }

    SMGEdgePointsTo edge = smgState.getPointerFromValue(pAddressValue.getAsInt());

    return createAddress(SMGEdgePointsToAndState.of(smgState, edge));
  }

  SMGAddressValueAndState createAddress(SMGState pSmgState, SMGObject pTarget,
      SMGExplicitValue pOffset) throws SMGInconsistentException {

    SMGAddressValueAndState addressValueAndState = getAddress(pSmgState, pTarget, pOffset);

    if (addressValueAndState.getValue().isUnknown()) {

      SMGKnownSymValue value = SMGKnownSymValue.valueOf(SMGValueFactory
          .getNewValue());
      SMGKnownAddVal addressValue = SMGKnownAddVal.valueOf(pTarget,
          (SMGKnownExpValue) pOffset, value);
      return SMGAddressValueAndState.of(addressValueAndState.getSmgState(),
          addressValue);
    }

    return addressValueAndState;
  }

  SMGAddressValueAndState getAddress(SMGState pSmgState, SMGObject pTarget,
      SMGExplicitValue pOffset) throws SMGInconsistentException {

    if (pTarget == null || pOffset.isUnknown()) {
      return SMGAddressValueAndState.of(pSmgState);
    }

    Integer address = pSmgState.getAddress(pTarget, pOffset.getAsInt());

    if (address == null) {
      return SMGAddressValueAndState.of(pSmgState);
    }

    SMGEdgePointsToAndState edgeAndState = SMGEdgePointsToAndState.of(pSmgState, pSmgState.getPointerFromValue(address));
    return createAddress(edgeAndState);
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
      implements CRightHandSideVisitor<SMGAddressAndState, CPATransferException> {

    public ArrayVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
    }

    @Override
    protected SMGAddressAndState visitDefault(CExpression exp) {
      return SMGAddressAndState.of(getInitialSmgState());
    }

    @Override
    public SMGAddressAndState visit(CUnaryExpression unaryExpression) throws CPATransferException {
      throw new AssertionError("The result of any unary expression " +
          "cannot be an array type.");
    }

    @Override
    public SMGAddressAndState visit(CBinaryExpression binaryExp) throws CPATransferException {

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
        return SMGAddressAndState.of(getInitialSmgState()); // If both or neither are Addresses,
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
        throw new UnrecognizedCCodeException("Expected either "
      + lVarInBinaryExp.toASTString() + " or "
      + rVarInBinaryExp.toASTString() +
      "to be a pointer to an array.", binaryExp);
      }

      // a = &a[0]
      SMGAddressValueAndState result =
          handlePointerArithmetic(getInitialSmgState(), getCfaEdge(),
              address, arrayOffset, addressType, lVarIsAddress, binaryExp);
      return result.asSMGAddressAndState();
    }

    @Override
    public SMGAddressAndState visit(CIdExpression pVariableName) throws CPATransferException {

      SMGAddressAndState addressAndState = super.visit(pVariableName);

      //TODO correct?
      // parameter declaration array types are converted to pointer
      if (pVariableName.getDeclaration() instanceof CParameterDeclaration) {
        SMGAddress address = addressAndState.getAddress();
        SMGState newState = addressAndState.getSmgState();

        SMGValueAndState pointerAndState =
            readValue(newState, address.getObject(),
                address.getOffset(), getRealExpressionType(pVariableName), getCfaEdge());

        SMGAddressValueAndState trueAddressAndState = getAddressFromSymbolicValue(pointerAndState);

        return trueAddressAndState.asSMGAddressAndState();
      } else {
        return addressAndState;
      }
    }

    @Override
    public SMGAddressAndState visit(CCastExpression cast) throws CPATransferException {

      CExpression op = cast.getOperand();

      if (op.getExpressionType() instanceof CArrayType) {
        return cast.getOperand().accept(this);
      } else {
        //TODO cast reinterpretation
        return SMGAddressAndState.of(getInitialSmgState());
      }
    }

    @Override
    public SMGAddressAndState visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      return SMGAddressAndState.of(getInitialSmgState());
    }
  }

  class AssumeVisitor extends ExpressionValueVisitor {
    private BinaryRelationEvaluator relation = null;

    public AssumeVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
    }

    @Override
    public SMGValueAndState visit(CBinaryExpression pExp)
        throws CPATransferException {
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

        SMGValueAndState leftSideValAndState = evaluateExpressionValue(getInitialSmgState(),
            cfaEdge, leftSideExpression);
        SMGSymbolicValue leftSideVal = leftSideValAndState.getValue();
        SMGState newState = leftSideValAndState.getSmgState();

        SMGValueAndState rightSideValAndState = evaluateExpressionValue(
            newState, cfaEdge, rightSideExpression);
        SMGSymbolicValue rightSideVal = rightSideValAndState.getValue();
        newState = rightSideValAndState.getSmgState();

        SMGSymbolicValue result = evaluateBinaryAssumption(newState,
            binaryOperator, leftSideVal, rightSideVal);

        return SMGValueAndState.of(newState, result);
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

      private final SMGSymbolicValue val1;
      private final SMGSymbolicValue val2;

      private final SMGState smgState;

      /**
       * Creates an object of the BinaryRelationEvaluator. The object is used to
       * determine the relation between two symbolic values in the context of
       * the given smgState and the given binary operator. Note that the given
       * symbolic values, which may also be address values, do not have to be
       * part of the given Smg. The definition of an smg implies conditions for
       * its values, even if they are not part of it.
       *
       * @param newState the values are compared in the context of the given smg.
       * @param pOp the given binary operator, that describes an boolean
       *          expression between two values.
       * @param pV1 the first operand.
       * @param pV2 the second operand
       * @throws SMGInconsistentException
       */
      public BinaryRelationEvaluator(SMGState newState, BinaryOperator pOp,
          SMGSymbolicValue pV1, SMGSymbolicValue pV2)
          throws SMGInconsistentException {

        smgState = newState;

        val1 = pV1;
        val2 = pV2;

        // If a value is unknown, we can't make further assumptions about it.
        if (pV2.isUnknown() || pV1.isUnknown()) {
          return;
        }

        boolean isPointerOp1 = isPointer(pV1);
        boolean isPointerOp2 = isPointer(pV2);

        int v1 = pV1.getAsInt();
        int v2 = pV2.getAsInt();

        boolean areEqual = (v1 == v2);
        boolean areNonEqual = (isUnequal(pV1, pV2, isPointerOp1, isPointerOp2));

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
        case GREATER_EQUAL:
        case LESS_EQUAL:
        case LESS_THAN:
        case GREATER_THAN:
          switch (pOp) {
          case LESS_EQUAL:
          case GREATER_EQUAL:
            if (areEqual) {
              isTrue = true;
              impliesEqWhenTrue = true;
              impliesNeqWhenFalse = true;
            } else {
              impliesNeqWhenFalse = true;
            }
            break;
          case GREATER_THAN:
          case LESS_THAN:
            if(areEqual) {
              isFalse = true;
            }

            impliesNeqWhenTrue = true;
            break;
          default:
            throw new AssertionError("Impossible case thrown");
          }

          if (isPointerOp1 && isPointerOp2) {
            SMGAddressValue pointer1 = getAddressOfPointer(pV1);
            SMGAddressValue pointer2 = getAddressOfPointer(pV2);
            SMGObject object1 = pointer1.getObject();
            SMGObject object2 = pointer2.getObject();

            // there can be more precise comparsion when pointer point to the
            // same object.
            if (object1 == object2) {
              int offset1 = pointer1.getOffset().getAsInt();
              int offset2 = pointer2.getOffset().getAsInt();

              switch (pOp) {
              case GREATER_EQUAL:
                isTrue = offset1 >= offset2;
                isFalse = !isTrue;
                break;
              case GREATER_THAN:
                isTrue = offset1 > offset2;
                isFalse = !isTrue;
                break;
              case LESS_EQUAL:
                isTrue = offset1 <= offset2;
                isFalse = !isTrue;
                break;
              case LESS_THAN:
                isTrue = offset1 < offset2;
                isFalse = !isTrue;
                break;
              default:
                throw new AssertionError("Impossible case thrown");
              }
            }
          }
          break;
        default:
          throw new AssertionError(
              "Binary Relation with non-relational operator: " + pOp.toString());
        }
      }

      private boolean isPointer(SMGSymbolicValue symVal) {

        if (symVal.isUnknown()) {
          return false;
        }

        if (symVal instanceof SMGAddressValue) {
          return true;
        }

        if (smgState.isPointer(symVal.getAsInt())) {
          return true;
        } else {
          return false;
        }
      }

      private boolean isUnequal(SMGSymbolicValue pValue1, SMGSymbolicValue pValue2, boolean isPointerOp1,
          boolean isPointerOp2) throws SMGInconsistentException {

        int value1 = pValue1.getAsInt();
        int value2 = pValue2.getAsInt();

        if (isPointerOp1 && isPointerOp2) {

          if (value1 != value2) {

            SMGAddressValue pointerValue1 = getAddressOfPointer(pValue1);
            SMGAddressValue pointerValue2 = getAddressOfPointer(pValue2);

            /* This is just a safety check,
            equal pointers should have equal symbolic values.*/
            return pointerValue1.getObject() != pointerValue2.getObject()
                || pointerValue1.getOffset() != pointerValue2.getOffset();
          } else {
            return false;
          }
        } else if (isPointerOp1 && value2 == 0 ||
            isPointerOp2 && value1 == 0) {
          return value1 != value2;
        } else {
          return smgState.isInNeq(pValue1, pValue2);
        }
      }

      private SMGAddressValue getAddressOfPointer(SMGSymbolicValue pPointer)
          throws SMGInconsistentException {

        assert !pPointer.isUnknown();

        if (pPointer instanceof SMGAddressValue) {
          return (SMGAddressValue) pPointer;
        } else {

          SMGEdgePointsTo edge = smgState.getPointerFromValue(pPointer
              .getAsInt());

          return SMGKnownAddVal.valueOf(edge.getValue(), edge.getObject(),
              edge.getOffset());
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

      public SMGSymbolicValue getVal2() {
        return val2;
      }

      public SMGSymbolicValue getVal1() {
        return val1;
      }
    }

    public SMGSymbolicValue evaluateBinaryAssumption(SMGState newState, BinaryOperator pOp, SMGSymbolicValue v1, SMGSymbolicValue v2) throws SMGInconsistentException {
      relation = new BinaryRelationEvaluator(newState, pOp, v1, v2);
      if (relation.isFalse()) {
        return SMGKnownSymValue.FALSE;
      } else if (relation.isTrue()) {
        return SMGKnownSymValue.TRUE;
      }

      return SMGUnknownValue.getInstance();
    }

    public boolean impliesEqOn(boolean pTruth) {
      if (relation == null) {
        return false;
      }
      return relation.impliesEq(pTruth);
    }

    public boolean impliesNeqOn(boolean pTruth) {
      if (relation == null) {
        return false;
      }
      return relation.impliesNeq(pTruth);
    }

    public SMGSymbolicValue impliesVal1() {
      return relation.getVal1();
    }

    public SMGSymbolicValue impliesVal2() {
      return relation.getVal2();
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
      implements CRightHandSideVisitor<SMGAddressAndState, CPATransferException> {

    public StructAndUnionVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
      super(pCfaEdge, pNewState);
    }

    @Override
    public SMGAddressAndState visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      return SMGAddressAndState.of(getInitialSmgState());
    }

    @Override
    public SMGAddressAndState visit(CCastExpression cast) throws CPATransferException {

      CExpression op = cast.getOperand();

      if (isStructOrUnionType(op.getExpressionType())) {
        return cast.getOperand().accept(this);
      } else {
        //TODO cast reinterpretation
        return SMGAddressAndState.of(getInitialSmgState());
      }

    }
  }

  /**
   * This class evaluates expressions that evaluate not to a
   * pointer, array, struct or union type.
   * The result of this evaluation is a {@link SMGSymbolicValue}.
   * The value represents a symbolic value of the SMG.
   *
   */
  class ExpressionValueVisitor extends DefaultCExpressionVisitor<SMGValueAndState, CPATransferException>
    implements CRightHandSideVisitor<SMGValueAndState, CPATransferException> {

    protected final CFAEdge cfaEdge;
    protected final SMGState initialSmgState;

    public ExpressionValueVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      initialSmgState = pSmgState;
    }

    @Override
    protected SMGValueAndState visitDefault(CExpression pExp) {
      return SMGValueAndState.of(getInitialSmgState());
    }

    @Override
    public SMGValueAndState visit(CArraySubscriptExpression exp) throws CPATransferException {

      SMGAddressAndState addressAndState = evaluateArraySubscriptAddress(getInitialSmgState(), getCfaEdge(), exp);
      SMGAddress address = addressAndState.getAddress();
      SMGState newState = addressAndState.getSmgState();

      if (address.isUnknown()) {
        return SMGValueAndState.of(newState);
      }

      return readValue(newState, address.getObject(), address.getOffset(), getRealExpressionType(exp), cfaEdge);
    }

    @Override
    public SMGValueAndState visit(CIntegerLiteralExpression exp) throws CPATransferException {

      BigInteger value = exp.getValue();

      boolean isZero = value.equals(BigInteger.ZERO);

      SMGSymbolicValue val = (isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance());
      return SMGValueAndState.of(getInitialSmgState(), val);
    }

    @Override
    public SMGValueAndState visit(CCharLiteralExpression exp) throws CPATransferException {

      char value = exp.getCharacter();

      SMGSymbolicValue val = (value == 0) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
      return SMGValueAndState.of(getInitialSmgState(), val);
    }

    @Override
    public SMGValueAndState visit(CFieldReference fieldReference) throws CPATransferException {

      SMGAddressAndState addressOfFieldAndState = getAddressOfField(getInitialSmgState(), getCfaEdge(), fieldReference);
      SMGAddress addressOfField = addressOfFieldAndState.getAddress();
      SMGState newState = addressOfFieldAndState.getSmgState();


      if (addressOfField.isUnknown()) {
        return SMGValueAndState.of(newState);
      }

      CType fieldType = fieldReference.getExpressionType().getCanonicalType();

      return readValue(newState, addressOfField.getObject(), addressOfField.getOffset(), fieldType, cfaEdge);
    }

    @Override
    public SMGValueAndState visit(CFloatLiteralExpression exp)
        throws CPATransferException {

      boolean isZero = exp.getValue().equals(BigDecimal.ZERO);

      SMGSymbolicValue val = isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
      return SMGValueAndState.of(getInitialSmgState(), val);
    }

    @Override
    public SMGValueAndState visit(CIdExpression idExpression)
        throws CPATransferException {

      CSimpleDeclaration decl = idExpression.getDeclaration();

      if (decl instanceof CEnumerator) {

        long enumValue = ((CEnumerator) decl).getValue();

        SMGSymbolicValue val = enumValue == 0 ? SMGKnownSymValue.ZERO
            : SMGUnknownValue.getInstance();
        return SMGValueAndState.of(getInitialSmgState(), val);

      } else if (decl instanceof CVariableDeclaration
          || decl instanceof CParameterDeclaration) {
        SMGState smgState = getInitialSmgState();

        SMGObject variableObject = smgState
            .getObjectForVisibleVariable(idExpression.getName());

        return readValue(smgState, variableObject, SMGKnownExpValue.ZERO,
            getRealExpressionType(idExpression), cfaEdge);
      }

      return SMGValueAndState.of(getInitialSmgState());
    }

    @Override
    public SMGValueAndState visit(CUnaryExpression unaryExpression) throws CPATransferException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case AMPER:
        throw new UnrecognizedCCodeException("Can't use & of expression " + unaryOperand.toASTString(), cfaEdge,
            unaryExpression);

      case MINUS:
        SMGValueAndState valueAndState = unaryOperand.accept(this);
        SMGSymbolicValue value = valueAndState.getValue();

        SMGSymbolicValue val = value.equals(SMGKnownSymValue.ZERO) ? value
            : SMGUnknownValue.getInstance();
        return SMGValueAndState.of(valueAndState.getSmgState(), val);

      case SIZEOF:
        int size = getSizeof(cfaEdge, getRealExpressionType(unaryOperand));
        val = (size == 0) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
        return SMGValueAndState.of(getInitialSmgState(), val);
      case TILDE:

      default:
        return SMGValueAndState.of(getInitialSmgState());
      }
    }

    @Override
    public SMGValueAndState visit(CPointerExpression pointerExpression) throws CPATransferException {

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

    @Override
    public SMGValueAndState visit(CTypeIdExpression typeIdExp) throws UnrecognizedCCodeException {

      TypeIdOperator typeOperator = typeIdExp.getOperator();
      CType type = typeIdExp.getType();

      switch (typeOperator) {
      case SIZEOF:
        SMGSymbolicValue val = getSizeof(cfaEdge, type) == 0 ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
        return SMGValueAndState.of(getInitialSmgState(), val);
      default:
        return SMGValueAndState.of(getInitialSmgState());
        //TODO Investigate the other Operators.
      }
    }

    @Override
    public SMGValueAndState visit(CBinaryExpression exp) throws CPATransferException {

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

        SMGValueAndState lValAndState = evaluateExpressionValue(getInitialSmgState(), getCfaEdge(), lVarInBinaryExp);
        SMGSymbolicValue lVal = lValAndState.getValue();
        SMGState newState = lValAndState.getSmgState();

        if (lVal.equals(SMGUnknownValue.getInstance())) {
          return SMGValueAndState.of(newState);
        }

        SMGValueAndState rValAndState = evaluateExpressionValue(getInitialSmgState(), getCfaEdge(), rVarInBinaryExp);
        SMGSymbolicValue rVal = rValAndState.getValue();
        newState = rValAndState.getSmgState();

        if (rVal.equals(SMGUnknownValue.getInstance())) {
          return SMGValueAndState.of(newState);
        }

        boolean isZero;

        switch (binaryOperator) {
        case PLUS:
        case SHIFT_LEFT:
        case BINARY_OR:
        case BINARY_XOR:
        case SHIFT_RIGHT:
          isZero = lVal.equals(SMGKnownSymValue.ZERO) && rVal.equals(SMGKnownSymValue.ZERO);
          SMGSymbolicValue val = (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
          return SMGValueAndState.of(newState, val);

        case MINUS:
        case MODULO:
          isZero = (lVal.equals(rVal));
          val = (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
          return SMGValueAndState.of(newState, val);

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if (rVal.equals(SMGKnownSymValue.ZERO)) {
            return SMGValueAndState.of(newState);
          }

          isZero = lVal.equals(SMGKnownSymValue.ZERO);
          val = (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
          return SMGValueAndState.of(newState, val);

        case MULTIPLY:
        case BINARY_AND:
          isZero = lVal.equals(SMGKnownSymValue.ZERO)
              || rVal.equals(SMGKnownSymValue.ZERO);
          val = (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
          return SMGValueAndState.of(newState, val);

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

        SMGValueAndState lValAndState = evaluateExpressionValue(getInitialSmgState(), getCfaEdge(), lVarInBinaryExp);
        SMGSymbolicValue lVal = lValAndState.getValue();
        SMGState newState = lValAndState.getSmgState();

        if (lVal.equals(SMGUnknownValue.getInstance())) {
          return SMGValueAndState.of(newState);
        }

        SMGValueAndState rValAndState = evaluateExpressionValue(getInitialSmgState(), getCfaEdge(), rVarInBinaryExp);
        SMGSymbolicValue rVal = rValAndState.getValue();
        newState = rValAndState.getSmgState();

        if (rVal.equals(SMGUnknownValue.getInstance())) {
          return SMGValueAndState.of(newState);
        }

        AssumeVisitor v = getAssumeVisitor(getCfaEdge(), newState);

        SMGSymbolicValue assumptionVal = v.evaluateBinaryAssumption(newState, binaryOperator, lVal, rVal);

        if (assumptionVal == SMGKnownSymValue.FALSE) {
          return SMGValueAndState.of(newState, SMGKnownSymValue.ZERO);
        } else {
          return SMGValueAndState.of(newState);
        }
      }

      default:
        return SMGValueAndState.of(getInitialSmgState());
      }
    }

    @Override
    public SMGValueAndState visit(CCastExpression cast) throws CPATransferException {
      // For different types we need different visitors,
      // TODO doesn't calculate type reinterpretations
      return evaluateExpressionValue(getInitialSmgState(), getCfaEdge(), cast.getOperand());
    }

    protected SMGValueAndState dereferenceArray(CExpression exp, CType derefType) throws CPATransferException {

      ArrayVisitor v = getArrayVisitor(getCfaEdge(), getInitialSmgState());

      SMGAddressAndState addressAndState = exp.accept(v);
      SMGAddress address = addressAndState.getAddress();
      SMGState newState = addressAndState.getSmgState();

      if (address.isUnknown()) {
        // We can't resolve the field to dereference, therefore
        // we must assume, that it is invalid

        return handleUnknownDereference(newState, cfaEdge);
      }

      // a == &a[0]
      if (derefType instanceof CArrayType) {
        return createAddress(newState, address.getObject(), address.getOffset());
      } else {
        return readValue(newState, address.getObject(), address.getOffset(), derefType, cfaEdge);
      }
    }

    protected final SMGValueAndState dereferencePointer(CExpression exp,
        CType derefType) throws CPATransferException {

      SMGAddressValueAndState addressAndState = evaluateAddress(
          getInitialSmgState(), getCfaEdge(), exp);
      SMGAddressValue address = addressAndState.getValue();
      SMGState newState = addressAndState.getSmgState();

      if (address.isUnknown()) {
        // We can't resolve the field to dereference , therefore
        // we must assume, that it is invalid
        return handleUnknownDereference(newState, getCfaEdge());
      }

      // a == &a[0]
      if (derefType instanceof CArrayType) {
        return createAddress(newState, address.getObject(), address.getOffset());
      } else {
        return readValue(newState, address.getObject(), address.getOffset(), derefType, cfaEdge);
      }
    }

    @Override
    public SMGValueAndState visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      return SMGValueAndState.of(getInitialSmgState());
    }

    public SMGState getInitialSmgState() {
      return initialSmgState;
    }

    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }
  }

  class ExplicitValueVisitor extends AbstractExpressionValueVisitor {

    private final CFAEdge edge;

    // Will be updated while evaluating left hand side expressions.
    private SMGState smgState;

    public ExplicitValueVisitor(SMGState pSmgState, String pFunctionName,
        MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger,
        CFAEdge pEdge) {
      super(pFunctionName, pMachineModel, pLogger);
      smgState = pSmgState;
      edge = pEdge;
    }

    public SMGState getNewState() {
      return smgState;
    }

    public CFAEdge getEdge() {
      return edge;
    }

    private SMGExplicitValue getExplicitValue(SMGSymbolicValue pValue) {

      if (pValue.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      SMGExplicitValue explicitValue = smgState.getExplicit((SMGKnownSymValue) pValue);

      return explicitValue;
    }

    protected void setSmgState(SMGState pSmgState) {
      smgState = pSmgState;
    }

    @Override
    public Value visit(CBinaryExpression binaryExp)
        throws UnrecognizedCCodeException {

      Value value = super.visit(binaryExp);

      if (value.isUnknown() && isPointerComparison(binaryExp)) {
        /* We may be able to get an explicit Value from pointer comaprisons. */

        SMGValueAndState symValueAndState = null;

        try {
          symValueAndState = evaluateAssumptionValue(smgState, edge, binaryExp);
        } catch (CPATransferException e) {
          UnrecognizedCCodeException e2 = new UnrecognizedCCodeException(
              "SMG cannot be evaluated", binaryExp);
          e2.initCause(e);
          throw e2;
        }

        SMGSymbolicValue symValue = symValueAndState.getValue();
        smgState = symValueAndState.getSmgState();

        if (symValue.equals(SMGKnownSymValue.TRUE)) {
          return new NumericValue(1);
        } else if (symValue.equals(SMGKnownSymValue.FALSE)) {
          return new NumericValue(0);
        }
      }

      return value;
    }

    private boolean isPointerComparison(CBinaryExpression pE) {

      switch (pE.getOperator()) {
      case EQUALS:
      case LESS_EQUAL:
      case GREATER_EQUAL:
      case GREATER_THAN:
      case LESS_THAN:
        //TODO Check, if one of the two operand types is expressed as pointer, e.g. pointer, struct, array, etc
        return true;
      }

      return false;
    }

    @Override
    protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
        throws UnrecognizedCCodeException {
      return evaluateLeftHandSideExpression(pCPointerExpression);
    }

    private Value evaluateLeftHandSideExpression(CLeftHandSide leftHandSide)
        throws UnrecognizedCCodeException {

      SMGValueAndState valueAndState = null;

      try {
        valueAndState = evaluateExpressionValue(smgState, edge, leftHandSide);
      } catch (CPATransferException e) {
        UnrecognizedCCodeException e2 =
            new UnrecognizedCCodeException("SMG cannot be evaluated", leftHandSide);
        e2.initCause(e);
        throw e2;
      }

      SMGSymbolicValue value = valueAndState.getValue();
      smgState = valueAndState.getSmgState();

      SMGExplicitValue expValue = getExplicitValue(value);

      if (expValue.isUnknown()) {
        return UnknownValue.getInstance();
      } else {
        return new NumericValue(expValue.getAsLong());
      }
    }

    @Override
    protected Value evaluateCIdExpression(CIdExpression pCIdExpression) throws UnrecognizedCCodeException {
      return evaluateLeftHandSideExpression(pCIdExpression);
    }

    @Override
    protected Value evaluateJIdExpression(JIdExpression pVarName) {
      return null;
    }

    @Override
    protected Value evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException {
      return evaluateLeftHandSideExpression(pLValue);
    }

    @Override
    protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
        throws UnrecognizedCCodeException {
      return evaluateLeftHandSideExpression(pLValue);
    }

  }


  /*
   * These Methods are designed to be overwritten to enable
   * sub classes to, for example, change the smgState while
   * evaluating expressions.
   *
   */
  protected SMGValueAndState handleUnknownDereference(SMGState smgState, CFAEdge edge) {
    return SMGValueAndState.of(smgState);
  }

  protected StructAndUnionVisitor getStructAndUnionVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new StructAndUnionVisitor(pCfaEdge, pNewState);
  }

  protected ArrayVisitor getArrayVisitor(CFAEdge pCfaEdge, SMGState pSmgState) {
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

  public static class SMGAddressValueAndState extends SMGValueAndState {

    private SMGAddressValueAndState(SMGState pState, SMGAddressValue pValue) {
      super(pState, pValue);
    }

    public SMGAddressAndState asSMGAddressAndState() {
      return SMGAddressAndState.of(getSmgState(), getValue().getAddress());
    }

    private SMGAddressValueAndState(SMGState pState) {
      super(pState);
    }

    @Override
    public SMGAddressValue getValue() {
      return (SMGAddressValue) super.getValue();
    }

    public static SMGAddressValueAndState of(SMGState pState,
        SMGAddressValue pValue) {
      return new SMGAddressValueAndState(pState, pValue);
    }

    public static SMGAddressValueAndState of(SMGState pState) {
      return new SMGAddressValueAndState(pState);
    }

    @Override
    public String toString() {
      // TODO Auto-generated method stub
      return super.toString();
    }
  }

  public static class SMGAddressAndState {
    private final SMGState smgState;
    private final SMGAddress address;

    private SMGAddressAndState(SMGState pState, SMGAddress pAddress) {
      smgState = pState;
      address = pAddress;
    }

    private SMGAddressAndState(SMGState pState) {
      smgState = pState;
      address = SMGAddress.getUnknownInstance();
    }

    public SMGAddress getAddress() {
      return address;
    }

    public SMGState getSmgState() {
      return smgState;
    }

    public static SMGAddressAndState of(SMGState pState) {
      return new SMGAddressAndState(pState);
    }

    public static SMGAddressAndState of(SMGState pState, SMGAddress pAddress) {
      return new SMGAddressAndState(pState, pAddress);
    }

    @Override
    public String toString() {
      // TODO Auto-generated method stub
      return address.toString() + " StateId: " + smgState.getId();
    }
  }

  public static class SMGValueAndState {
    private final SMGState smgState;
    private final SMGSymbolicValue value;

    private SMGValueAndState(SMGState pState, SMGSymbolicValue pValue) {
      smgState = pState;
      value = pValue;
    }

    public SMGValueAndState(SMGState pState) {
      smgState = pState;
      value = SMGUnknownValue.getInstance();
    }

    public SMGSymbolicValue getValue() {
      return value;
    }

    public static SMGValueAndState of(SMGState pState) {
      return new SMGValueAndState(pState);
    }

    public SMGState getSmgState() {
      return smgState;
    }

    public static SMGValueAndState of(SMGState pState, SMGSymbolicValue pValue) {
      return new SMGValueAndState(pState, pValue);
    }

    @Override
    public String toString() {
      return value.toString() + " StateId: " + smgState.getId();
    }
  }

  public static class SMGExplicitValueAndState {
    private final SMGState smgState;
    private final SMGExplicitValue value;

    private SMGExplicitValueAndState(SMGState pState, SMGExplicitValue pValue) {
      smgState = pState;
      value = pValue;
    }

    public SMGExplicitValueAndState(SMGState pState) {
      smgState = pState;
      value = SMGUnknownValue.getInstance();
    }

    public SMGExplicitValue getValue() {
      return value;
    }

    public static SMGExplicitValueAndState of(SMGState pState) {
      return new SMGExplicitValueAndState(pState);
    }

    public SMGState getSmgState() {
      return smgState;
    }

    public static SMGExplicitValueAndState of(SMGState pState, SMGExplicitValue pValue) {
      return new SMGExplicitValueAndState(pState, pValue);
    }

    @Override
    public String toString() {
      return value.toString() + " StateId: " + smgState.getId();
    }
  }
}