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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
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
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.MachineModel.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
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
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
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

  /**
   * Get the size of the given type in Bytes.
   *
   * When handling variable array type length,
   * additionally to the type itself, we also need the
   * cfa edge to determine the location of the program
   * we currently handle, the smg state to determine
   * the values of the variables at the current location,
   * and the expression with the given type to determine
   * the smg object that represents the array of the given type.
   *
   * @param edge The cfa edge which determines the location
   *             of the program.
   * @param pType We want to calculate the size of this type.
   * @param pState The state that contains the current variable values.
   * @param expression The expression, which evaluates to the value with the given type.
   * @return The size of the given type in bytes.
   */
  public int getSizeof(CFAEdge edge, CType pType, SMGState pState, CExpression expression) throws UnrecognizedCCodeException {

    CSizeOfVisitor v = getSizeOfVisitor(edge, pState, expression);

    try {
      return pType.accept(v);
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Could not resolve type.", edge);
    }
  }

  /**
   * Get the size of the given type in Bytes.
   *
   * When handling variable array type length,
   * additionally to the type itself, we also need the
   * cfa edge to determine the location of the program
   * we currently handle, and the smg state to determine
   * the values of the variables at the current location..
   *
   * This method can't calculate variable array type length for
   * arrays that are not declared in the cfa edge.
   *
   * @param edge The cfa edge which determines the location
   *             of the program.
   * @param pType We want to calculate the size of this type.
   * @param pState The state that contains the current variable values.
   * @return The size of the given type in bytes.
   */
  public int getSizeof(CFAEdge edge, CType pType, SMGState pState) throws UnrecognizedCCodeException {

    CSizeOfVisitor v = getSizeOfVisitor(edge, pState);

    try {
      return pType.accept(v);
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Could not resolve type.", edge);
    }
  }

  public int getBitSizeof(CFAEdge pEdge, CType pType, SMGState pState) throws UnrecognizedCCodeException {
    CSizeOfVisitor v;
    if (machineModel.isBitFieldsSupportEnabled()) {
      v = getBitSizeOfVisitor(pEdge, pState);
    } else {
      v = getSizeOfVisitor(pEdge, pState);
    }

    try {
      return pType.accept(v);
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Could not resolve type.", pEdge);
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
    public List<SMGAddressAndState> visit(CUnaryExpression lValue) throws CPATransferException {

      throw new UnrecognizedCCodeException(lValue.toASTString() + " is not an lValue", getCfaEdge(), lValue);
    }

    @Override
    public List<SMGAddressAndState> visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      throw new AssertionError("This expression is not a lValue expression.");
    }
  }

  private List<SMGAddressAndState> getAddressOfField(SMGState pSmgState, CFAEdge cfaEdge, CFieldReference fieldReference)
      throws CPATransferException {

    CExpression fieldOwner = fieldReference.getFieldOwner();

    CType ownerType = getRealExpressionType(fieldOwner);

    List<SMGAddressAndState> result = new ArrayList<>(4);

    /* Points to the start of this struct or union.
    *
    * Note that whether this field Reference is a pointer dereference x->b
    * or not x.b is indirectly resolved by whether the type of x is
    * a pointer type, in which case its expression is evaluated, or
    * a struct type, in which case the address of the expression
    * similar is evaluated.
    */

    SMGAddressValueAndStateList fieldOwnerAddressAndStates = evaluateAddress(pSmgState, cfaEdge, fieldOwner);

    for (SMGAddressValueAndState fieldOwnerAddressAndState : fieldOwnerAddressAndStates.asAddressValueAndStateList()) {

      SMGAddressValue fieldOwnerAddress = fieldOwnerAddressAndState.getObject();
      SMGState newState = fieldOwnerAddressAndState.getSmgState();

      String fieldName = fieldReference.getFieldName();

      SMGField field = getField(cfaEdge, ownerType, fieldName, newState, fieldReference);

      if (field.isUnknown() || fieldOwnerAddress.isUnknown()) {

        if (fieldReference.isPointerDereference()) {
          newState = handleUnknownDereference(newState, cfaEdge).getSmgState();
        }

        result.add(SMGAddressAndState.of(newState));
        continue;
      }

      SMGAddress addressOfFieldOwner = fieldOwnerAddress.getAddress();

      SMGExplicitValue fieldOffset = addressOfFieldOwner.add(field.getOffset()).getOffset();

      SMGObject fieldObject = addressOfFieldOwner.getObject();

      SMGAddress address = SMGAddress.valueOf(fieldObject, fieldOffset);

      result.add(SMGAddressAndState.of(newState, address));
    }

    return result;
  }

  public SMGValueAndState readValue(SMGState pSmgState, SMGObject pObject,
      SMGExplicitValue pOffset, CType pType, CFAEdge pEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    if (pOffset.isUnknown() || pObject == null) {
      return SMGValueAndState.of(pSmgState);
    }

    int fieldOffset = pOffset.getAsInt();

    //FIXME Does not work with variable array length.
    boolean doesNotFitIntoObject = fieldOffset < 0
        || fieldOffset + getSizeof(pEdge, pType, pSmgState) > pObject.getSize();

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(Level.WARNING, pEdge.getFileLocation() + ":", "Field " + "("
          + fieldOffset + ", " + pType.toASTString("") + ")"
          + " does not fit object " + pObject.toString() + ".");

      return SMGValueAndState.of(pSmgState);
    }

    // We don't want to modify the state while reading
    SMGSymbolicValue value = pSmgState.readValue(pObject, fieldOffset, pType).getObject();

    return SMGValueAndState.of(pSmgState, value);
  }

  private SMGField getField(CFAEdge edge, CType ownerType, String fieldName, SMGState pState, CExpression exp) throws UnrecognizedCCodeException {

    if (ownerType instanceof CElaboratedType) {

      CType realType = ((CElaboratedType) ownerType).getRealType();

      if (realType == null) {
        return SMGField.getUnknownInstance();
      }

      return getField(edge, realType, fieldName, pState, exp);
    } else if (ownerType instanceof CCompositeType) {
      return getField(edge, (CCompositeType) ownerType, fieldName, pState, exp);
    } else if (ownerType instanceof CPointerType) {

      /* We do not explicitly transform x->b,
      so when we try to get the field b the ownerType of x
      is a pointer type.*/

      CType type = ((CPointerType) ownerType).getType();

      type = getRealExpressionType(type);

      return getField(edge, type, fieldName, pState, exp);
    }

    throw new AssertionError();
  }

  private SMGField getField(CFAEdge pEdge, CCompositeType ownerType, String fieldName, SMGState pState, CExpression expression) throws UnrecognizedCCodeException {

    List<CCompositeTypeMemberDeclaration> membersOfType = ownerType.getMembers();

    int offset = 0;
    int bitFieldsSize = 0;

    for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
      String memberName = typeMember.getName();
      if (machineModel.isBitFieldsSupportEnabled() && typeMember.getType().isBitField()) {
        if (memberName.equals(fieldName)) {
          offset += bitFieldsSize;
          return new SMGField(SMGKnownExpValue.valueOf(offset),
              getRealExpressionType(typeMember.getType()));
        }

        if (!(ownerType.getKind() == ComplexTypeKind.UNION)) {
          bitFieldsSize += typeMember.getType().getBitFieldSize();
        }
      } else {
        if (bitFieldsSize > 0) {
          offset += bitFieldsSize;
          if (bitFieldsSize % machineModel.getSizeofCharInBits() > 0) {
            offset += machineModel.getSizeofCharInBits() - (bitFieldsSize % machineModel.getSizeofCharInBits());
          }
          bitFieldsSize = 0;
        }
        int padding = machineModel.getPadding(offset / machineModel.getSizeofCharInBits(), typeMember.getType()) *
            machineModel.getSizeofCharInBits();

        if (memberName.equals(fieldName)) {
          offset += padding;
          return new SMGField(SMGKnownExpValue.valueOf(offset),
            getRealExpressionType(typeMember.getType()));
        }

        if (!(ownerType.getKind() == ComplexTypeKind.UNION)) {
          offset = offset + padding + getSizeof(pEdge, getRealExpressionType(typeMember.getType()),
              pState, expression);
        }
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

    List<SMGExplicitValueAndState> result = evaluateExplicitValue(smgState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.get(0).getObject();
    } else {
      return SMGUnknownValue.getInstance();
    }
  }

  protected List<SMGExplicitValueAndState> evaluateExplicitValue(SMGState smgState,
      CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    List<SMGExplicitValueAndState> result = new ArrayList<>();

    ExplicitValueVisitor visitor = new ExplicitValueVisitor(smgState, null, machineModel, logger, cfaEdge);

    Value value = rValue.accept(visitor);
    SMGState newState = visitor.getNewState();

    if (!value.isExplicitlyKnown() || !value.isNumericValue()) {

      // Sometimes, we can get the explicit Value from SMGCPA, especially if the
      // result happens to
      // be a pointer to the Null Object, or through reinterpretation
      SMGValueAndStateList symbolicValueAndStates = evaluateExpressionValue(
          newState, cfaEdge, rValue);

      for (SMGValueAndState symbolicValueAndState : symbolicValueAndStates.getValueAndStateList()) {
        result.add(deriveExplicitValueFromSymbolicValue(symbolicValueAndState));
      }
    } else {
      long longValue = value.asNumericValue().longValue();
      result.add(SMGExplicitValueAndState.of(newState, SMGKnownExpValue.valueOf(longValue)));
    }

    for (SMGState additionalState : visitor.getSmgStatesToBeProccessed()) {
      result.addAll(evaluateExplicitValue(additionalState, cfaEdge, rValue));
    }

    return result;
  }

  private SMGExplicitValueAndState deriveExplicitValueFromSymbolicValue(SMGValueAndState symbolicValueAndState) {

    SMGSymbolicValue symbolicValue = symbolicValueAndState.getObject();
    SMGState newState = symbolicValueAndState.getSmgState();

    if (!symbolicValue.isUnknown()) {
      if (symbolicValue == SMGKnownSymValue.ZERO) {
        return SMGExplicitValueAndState.of(newState, SMGKnownExpValue.ZERO); }

      if (symbolicValue instanceof SMGAddressValue) {
        SMGAddressValue address = (SMGAddressValue) symbolicValue;

        if (address.getObject() == SMGObject.getNullObject()) { return SMGExplicitValueAndState.of(newState,
            SMGKnownExpValue.valueOf(address.getOffset().getAsLong() / machineModel.getSizeofCharInBits())); }
      }
    }

    return SMGExplicitValueAndState.of(newState);

  }

  public SMGSymbolicValue evaluateExpressionValueV2(SMGState smgState,
      CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    SMGValueAndStateList result = evaluateExpressionValue(smgState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.getValueAndStateList().get(0).getObject();
    } else {
      return SMGUnknownValue.getInstance();
    }
  }

  protected SMGValueAndStateList evaluateExpressionValue(SMGState smgState, CFAEdge cfaEdge,
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

  private SMGValueAndStateList evaluateNonAddressValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    ExpressionValueVisitor visitor = getExpressionValueVisitor(cfaEdge, newState);

    SMGValueAndStateList symbolicValues = rValue.accept(visitor);

    return symbolicValues;
  }

  protected SMGValueAndStateList evaluateAssumptionValue(SMGState newState,
      CFAEdge cfaEdge, CExpression rValue) throws CPATransferException {

    ExpressionValueVisitor visitor = getAssumeVisitor(cfaEdge, newState);
    return rValue.accept(visitor);
  }

  public SMGSymbolicValue evaluateAssumptionValueV2(SMGState newState,
      CFAEdge cfaEdge, CExpression rValue) throws CPATransferException {

    SMGValueAndStateList result = evaluateAssumptionValue(newState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.getValueAndStateList().get(0).getObject();
    } else {
      return SMGUnknownValue.getInstance();
    }
  }

  protected SMGAddressValueAndStateList evaluateAddress(SMGState pState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    CType expressionType = getRealExpressionType(rValue);

    if (expressionType instanceof CPointerType
        || (expressionType instanceof CFunctionType
            && rValue instanceof CUnaryExpression
            && ((CUnaryExpression) rValue).getOperator() == CUnaryExpression.UnaryOperator.AMPER)) {
      // Cfa treats &foo as CFunctionType

      PointerVisitor visitor = getPointerVisitor(cfaEdge, pState);

      SMGValueAndStateList addressAndStateList = rValue.accept(visitor);
      return getAddressFromSymbolicValues(addressAndStateList);
    } else if (isStructOrUnionType(expressionType)) {
      /* expressions with structs or unions as
       * result will be evaluated to their addresses.
       * The address can be used e.g. to copy the struct.
       */

      StructAndUnionVisitor visitor = getStructAndUnionVisitor(cfaEdge, pState);
      List<SMGAddressAndState> structAddressAndState = rValue.accept(visitor);
      return createAddresses(structAddressAndState);
    } else if (expressionType instanceof CArrayType) {

      ArrayVisitor visitor = getArrayVisitor(cfaEdge, pState);
      List<SMGAddressAndState> arrayAddressAndState = rValue.accept(visitor);
      return createAddresses(arrayAddressAndState);
    } else {
      throw new AssertionError("The method evaluateAddress may not be called" +
          "with the type " + expressionType.toASTString(""));
    }
  }

  public SMGAddressValue evaluateAddressV2(SMGState newState, CFAEdge cfaEdge,
      CRightHandSide rValue) throws CPATransferException {

    SMGAddressValueAndStateList result = evaluateAddress(newState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.asAddressValueAndStateList().get(0).getObject();
    } else {
      return SMGUnknownValue.getInstance();
    }
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
  private abstract class AddressVisitor extends DefaultCExpressionVisitor<List<SMGAddressAndState>, CPATransferException>
      implements CRightHandSideVisitor<List<SMGAddressAndState>, CPATransferException> {

    private final CFAEdge cfaEdge;
    private final SMGState initialSmgState;

    public AddressVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      initialSmgState = pSmgState;
    }

    @Override
    protected List<SMGAddressAndState> visitDefault(CExpression pExp) throws CPATransferException {
      return SMGAddressAndState.listOf(getInitialSmgState());
    }

    @Override
    public List<SMGAddressAndState> visit(CIdExpression variableName) throws CPATransferException {

      SMGState state = getInitialSmgState();
      SMGObject object = state.getObjectForVisibleVariable(variableName.getName());

      if (object == null && variableName.getDeclaration() != null) {
        CSimpleDeclaration dcl = variableName.getDeclaration();
        if (dcl instanceof CVariableDeclaration) {
          CVariableDeclaration varDcl = (CVariableDeclaration) dcl;

          if (varDcl.isGlobal()) {
            object = state.addGlobalVariable(getBitSizeof(getCfaEdge(), varDcl.getType(), state),
                varDcl.getName());
          } else {
            object = state.addLocalVariable(getBitSizeof(getCfaEdge(), varDcl.getType(), state),
                varDcl.getName());
          }
        }
      }

      return SMGAddressAndState.listOf(getInitialSmgState(),
          SMGAddress.valueOf(object, SMGKnownExpValue.ZERO));
    }

    @Override
    public List<SMGAddressAndState> visit(CArraySubscriptExpression exp) throws CPATransferException {
      return evaluateArraySubscriptAddress(getInitialSmgState(), getCfaEdge(), exp);
    }

    @Override
    public List<SMGAddressAndState> visit(CFieldReference pE) throws CPATransferException {
      return getAddressOfField(getInitialSmgState(), getCfaEdge(), pE);
    }

    @Override
    public List<SMGAddressAndState> visit(CPointerExpression pointerExpression)
        throws CPATransferException {

      /*
       * The address of a pointer expression (*x) is defined as the evaluation
       * of the pointer x. This is consistent with the meaning of a pointer
       * expression in the left hand side of an assignment *x = ...
       */
      CExpression operand = pointerExpression.getOperand();

      assert getRealExpressionType(operand) instanceof CPointerType
        || getRealExpressionType(operand) instanceof CArrayType;

      SMGAddressValueAndStateList addressValueAndState = evaluateAddress(
          getInitialSmgState(), getCfaEdge(), operand);

      return addressValueAndState.asAddressAndStateList();
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
    public SMGAddressValueAndStateList visit(CIntegerLiteralExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValues(super.visit(exp));
    }

    @Override
    public SMGAddressValueAndStateList visit(CCharLiteralExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValues(super.visit(exp));
    }

    @Override
    public SMGAddressValueAndStateList visit(CFloatLiteralExpression pExp) throws CPATransferException {
      return getAddressFromSymbolicValues(super.visit(pExp));
    }

    @Override
    public SMGAddressValueAndStateList visit(CIdExpression exp) throws CPATransferException {

      CType c = getRealExpressionType(exp);

      if (c instanceof CArrayType) {
        // a == &a[0];
        return createAddressOfVariable(exp);
      }

      return getAddressFromSymbolicValues(super.visit(exp));
    }

    @Override
    public SMGAddressValueAndStateList visit(CUnaryExpression unaryExpression) throws CPATransferException {

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
        return SMGAddressValueAndStateList.of(getInitialSmgState());
      }
    }

    private SMGAddressValueAndStateList handleAmper(CRightHandSide amperOperand) throws CPATransferException {

      if (getRealExpressionType(amperOperand) instanceof CFunctionType
          && amperOperand instanceof CIdExpression) {
        // function type &foo
        SMGAddressValueAndStateList addressValueAndStates = createAddressOfFunction((CIdExpression) amperOperand);
        return addressValueAndStates;
      } else if (amperOperand instanceof CIdExpression) {
        // &a
        SMGAddressValueAndStateList addressValueAndState = createAddressOfVariable((CIdExpression) amperOperand);
        return addressValueAndState;
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
        return SMGAddressValueAndStateList.of(getInitialSmgState());
      }
    }

    protected SMGAddressValueAndStateList createAddressOfFunction(CIdExpression idFunctionExpression)
        throws SMGInconsistentException {

      SMGState state = getInitialSmgState();

      SMGObject functionObject =
          state.getObjectForFunction((CFunctionDeclaration) idFunctionExpression.getDeclaration());

      if (functionObject == null) {
        return SMGAddressValueAndStateList.of(state);
      }

      return createAddress(state, functionObject, SMGKnownExpValue.ZERO);
    }

    private SMGAddressValueAndStateList createAddressOfArraySubscript(CArraySubscriptExpression lValue)
        throws CPATransferException {

      CExpression arrayExpression = lValue.getArrayExpression();

      List<SMGAddressValueAndState> result = new ArrayList<>(4);

      SMGAddressValueAndStateList arrayAddressAndStates =
          evaluateAddress(getInitialSmgState(), getCfaEdge(), arrayExpression);

      for (SMGAddressValueAndState arrayAddressAndState : arrayAddressAndStates.asAddressValueAndStateList()) {

        SMGAddressValue arrayAddress = arrayAddressAndState.getObject();
        SMGState newState = arrayAddressAndState.getSmgState();

        if (arrayAddress.isUnknown()) {
          result.add(SMGAddressValueAndState.of(newState));
          continue;
        }

        CExpression subscriptExpr = lValue.getSubscriptExpression();

        List<SMGExplicitValueAndState> subscriptValueAndStates = evaluateExplicitValue(
            newState, getCfaEdge(), subscriptExpr);

        for (SMGExplicitValueAndState subscriptValueAndState : subscriptValueAndStates) {

          SMGExplicitValue subscriptValue = subscriptValueAndState.getObject();
          newState = subscriptValueAndState.getSmgState();

          if (subscriptValue.isUnknown()) {
            result.add(SMGAddressValueAndState.of(newState));
            continue;
          }

          SMGExplicitValue arrayOffset = arrayAddress.getOffset();

          int typeSize = getSizeof(getCfaEdge(), getRealExpressionType(lValue), newState, lValue);

          SMGExplicitValue sizeOfType = SMGKnownExpValue.valueOf(typeSize);

          SMGExplicitValue offset = arrayOffset.add(subscriptValue).multiply(sizeOfType);

          SMGAddressValueAndStateList resultAddressAndState = createAddress(newState, arrayAddress.getObject(), offset);
          result.addAll(resultAddressAndState.asAddressValueAndStateList());
        }
      }

      return SMGAddressValueAndStateList.copyOfAddressValueList(result);
    }

    private SMGAddressValueAndStateList createAddressOfField(CFieldReference lValue)
        throws CPATransferException {

      List<SMGAddressValueAndState> result = new ArrayList<>(2);

      List<SMGAddressAndState> addressOfFieldAndStates = getAddressOfField(
          getInitialSmgState(), getCfaEdge(), lValue);

      for (SMGAddressAndState addressOfFieldAndState : addressOfFieldAndStates) {

        SMGAddress addressOfField = addressOfFieldAndState.getObject();
        SMGState newState = addressOfFieldAndState.getSmgState();

        if (addressOfField.isUnknown()) {
          result.add(SMGAddressValueAndState.of(newState));
          continue;
        }

        SMGAddressValueAndStateList resultAddressValueAndState = createAddress(addressOfFieldAndState.getSmgState(),
            addressOfField.getObject(), addressOfField.getOffset());

        result.addAll(resultAddressValueAndState.asAddressValueAndStateList());
      }

      return SMGAddressValueAndStateList.copyOfAddressValueList(result);
    }

    private SMGAddressValueAndStateList createAddressOfVariable(CIdExpression idExpression) throws SMGInconsistentException {

      SMGState state = getInitialSmgState();

      SMGObject variableObject = state.getObjectForVisibleVariable(idExpression.getName());

      if (variableObject == null) {
        return SMGAddressValueAndStateList.of(state);
      } else {
        return createAddress(state, variableObject, SMGKnownExpValue.ZERO);
      }
    }

    @Override
    public SMGAddressValueAndStateList visit(CPointerExpression pointerExpression) throws CPATransferException {

      return getAddressFromSymbolicValues(super.visit(pointerExpression));
    }

    @Override
    public SMGAddressValueAndStateList visit(CBinaryExpression binaryExp) throws CPATransferException {

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
        return SMGAddressValueAndStateList.of(getInitialSmgState()); // If both or neither are Addresses,
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

      CType typeOfPointer = getRealExpressionType(addressType.getType());

      return handlePointerArithmetic(getInitialSmgState(), getCfaEdge(),
          address, pointerOffset, typeOfPointer, lVarIsAddress,
          binaryExp);
    }

    @Override
    public SMGAddressValueAndStateList visit(CArraySubscriptExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValues(super.visit(exp));
    }

    @Override
    public SMGAddressValueAndStateList visit(CFieldReference exp) throws CPATransferException {
      return getAddressFromSymbolicValues(super.visit(exp));
    }

    @Override
    public SMGAddressValueAndStateList visit(CCastExpression pCast) throws CPATransferException {
      // TODO Maybe cast values to pointer to null Object with offset as explicit value
      // for pointer arithmetic substraction ((void *) 4) - ((void *) 3)?
      return getAddressFromSymbolicValues(super.visit(pCast));
    }
  }

  private SMGAddressValueAndStateList handlePointerArithmetic(SMGState initialSmgState,
      CFAEdge cfaEdge, CExpression address, CExpression pointerOffset,
      CType typeOfPointer, boolean lVarIsAddress,
      CBinaryExpression binaryExp) throws CPATransferException {

    BinaryOperator binaryOperator = binaryExp.getOperator();

    switch (binaryOperator) {
    case PLUS:
    case MINUS: {

      List<SMGAddressValueAndState> result = new ArrayList<>(4);

      SMGAddressValueAndStateList addressValueAndStates = evaluateAddress(
          initialSmgState, cfaEdge, address);

      for(SMGAddressValueAndState addressValueAndState : addressValueAndStates.asAddressValueAndStateList()) {


        SMGAddressValue addressValue = addressValueAndState.getObject();
        SMGState newState = addressValueAndState.getSmgState();

        List<SMGExplicitValueAndState> offsetValueAndStates = evaluateExplicitValue(
            newState, cfaEdge, pointerOffset);

        for(SMGExplicitValueAndState offsetValueAndState : offsetValueAndStates) {

          SMGExplicitValue offsetValue = offsetValueAndState.getObject();
          newState = offsetValueAndState.getSmgState();

          if (addressValue.isUnknown() || offsetValue.isUnknown()) {
            result.add(SMGAddressValueAndState.of(newState));
            continue;
          }

          SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge, typeOfPointer,
              newState, address));

          SMGExplicitValue pointerOffsetValue = offsetValue.multiply(typeSize);

          SMGObject target = addressValue.getObject();

          SMGExplicitValue addressOffset = addressValue.getOffset();

          switch (binaryOperator) {
          case PLUS:
            SMGAddressValueAndStateList resultAddressValueAndStateList = createAddress(newState, target, addressOffset.add(pointerOffsetValue));
            result.addAll(resultAddressValueAndStateList.asAddressValueAndStateList());
            break;
          case MINUS:
            if (lVarIsAddress) {
              resultAddressValueAndStateList = createAddress(newState, target, addressOffset.subtract(pointerOffsetValue));
              result.addAll(resultAddressValueAndStateList.asAddressValueAndStateList());
              break;
            } else {
              throw new UnrecognizedCCodeException("Expected pointer arithmetic "
                  + " with + or - but found " + binaryExp.toASTString(), binaryExp);
            }
          default:
            throw new AssertionError();
          }
        }
      }

      return SMGAddressValueAndStateList.copyOfAddressValueList(result);
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
      return SMGAddressValueAndStateList.of(initialSmgState);
    }
  }

  private List<SMGAddressAndState> evaluateArraySubscriptAddress(
      SMGState initialSmgState, CFAEdge cfaEdge, CArraySubscriptExpression exp)
          throws CPATransferException {

    List<SMGAddressAndState> result = new ArrayList<>(2);

    SMGAddressValueAndStateList arrayAddressAndStates = evaluateAddress(
        initialSmgState, cfaEdge, exp.getArrayExpression());

    for (SMGAddressValueAndState arrayAddressAndState : arrayAddressAndStates.asAddressValueAndStateList()) {
      SMGAddressValue arrayAddress = arrayAddressAndState.getObject();
      SMGState newState = arrayAddressAndState.getSmgState();

      List<SMGExplicitValueAndState> subscriptValueAndStates = evaluateExplicitValue(
          newState, cfaEdge, exp.getSubscriptExpression());

      for (SMGExplicitValueAndState subscriptValueAndState : subscriptValueAndStates) {
        SMGExplicitValue subscriptValue = subscriptValueAndState.getObject();
        newState = subscriptValueAndState.getSmgState();

        if (subscriptValue.isUnknown()) {
          if (newState.isTrackPredicatesEnabled()  && !arrayAddress.isUnknown()) {
            SMGValueAndStateList subscriptSymbolicValueAndStates =
                evaluateNonAddressValue(newState, cfaEdge, exp.getSubscriptExpression());
            for (SMGValueAndState symbolicValueAndState: subscriptSymbolicValueAndStates.getValueAndStateList()) {
              SMGSymbolicValue value = symbolicValueAndState.getObject();
              newState = subscriptValueAndState.getSmgState();
              if (!value.isUnknown() && !newState
                  .isObjectExternallyAllocated(arrayAddress.getObject())) {
                int size = arrayAddress.getObject().getSize();
                int typeSize = getSizeof(cfaEdge, exp.getExpressionType(), newState, exp);
                int index = (size / typeSize) + 1;
                int subscriptSize = getSizeof(cfaEdge, exp.getSubscriptExpression().getExpressionType(), newState, exp);
                newState.addErrorPredicate(value, subscriptSize, SMGKnownExpValue.valueOf(index),
                    subscriptSize, cfaEdge);
              }
            }
          } else {
            // assume address is invalid
            newState = handleUnknownDereference(newState, cfaEdge).getSmgState();
          }

          result.add(SMGAddressAndState.of(newState));
          continue;
        }

        SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge,
            exp.getExpressionType(), newState, exp));

        SMGExplicitValue subscriptOffset = subscriptValue.multiply(typeSize);

        SMGAddressAndState addressAndStateResult =
            SMGAddressAndState.of(newState, arrayAddress.getAddress().add(subscriptOffset));
        result.add(addressAndStateResult);
      }
    }

    return result;
  }

  private SMGAddressValueAndStateList createAddresses(List<SMGAddressAndState> pAddresses)
      throws SMGInconsistentException {

    List<SMGAddressValueAndState> result = new ArrayList<>(pAddresses.size());

    for (SMGAddressAndState addressAndState : pAddresses) {
      result.addAll(createAddress(addressAndState).asAddressValueAndStateList());
    }

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
  }

  private SMGAddressValueAndStateList createAddress(SMGAddressAndState addressAndState) throws SMGInconsistentException {

    SMGState state = addressAndState.getSmgState();
    SMGAddress address = addressAndState.getObject();

    if (address.isUnknown()) {
      return SMGAddressValueAndStateList.of(state);
    }

    return createAddress(state, address.getObject(), address.getOffset());
  }

  /**
   * Is given a list of symbolic Values, looks into the respective smgs to determine if the symbolic
   * values represents pointers, and transform them into a {@link SMGAddressValueAndStateList}.
   *
   * @param pAddressValueAndStateList This contains the list of smgs and symbolic values.
   * @return The address, otherwise unknown
   * @throws SMGInconsistentException thrown if the symbolic address is misinterpreted as a pointer.
   */
  SMGAddressValueAndStateList getAddressFromSymbolicValues(SMGValueAndStateList pAddressValueAndStateList)
      throws SMGInconsistentException {

    if (pAddressValueAndStateList instanceof SMGAddressValueAndStateList) {
      return (SMGAddressValueAndStateList) pAddressValueAndStateList;
    } else {

      List<SMGAddressValueAndState> addressAndStateList = new ArrayList<>(pAddressValueAndStateList.size());

      for (SMGValueAndState valueAndState : pAddressValueAndStateList.getValueAndStateList()) {
        addressAndStateList.addAll(getAddressFromSymbolicValue(valueAndState).asAddressValueAndStateList());
      }
      return SMGAddressValueAndStateList.copyOfAddressValueList(addressAndStateList);
    }
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
   * @param pAddressValueAndState This contains the SMG.
   * @return The address, otherwise unknown
   * @throws SMGInconsistentException thrown if the symbolic address is misinterpreted as a pointer.
   */
  SMGAddressValueAndStateList getAddressFromSymbolicValue(SMGValueAndState pAddressValueAndState) throws SMGInconsistentException {

    if (pAddressValueAndState instanceof SMGAddressValueAndState) {
      return SMGAddressValueAndStateList.of((SMGAddressValueAndState) pAddressValueAndState);
    }

    SMGSymbolicValue pAddressValue = pAddressValueAndState.getObject();
    SMGState smgState = pAddressValueAndState.getSmgState();

    if (pAddressValue instanceof SMGAddressValue) {
      return SMGAddressValueAndStateList.of(SMGAddressValueAndState.of(smgState,
          (SMGAddressValue) pAddressValue));
    }

    if (pAddressValue.isUnknown()) {
      return SMGAddressValueAndStateList.of(smgState);
    }

    if (!smgState.isPointer(pAddressValue.getAsInt())) {
      return SMGAddressValueAndStateList.of(smgState);
    }

    SMGAddressValueAndStateList addressValues =
        smgState.getPointerFromValue(pAddressValue.getAsInt());

    return addressValues;
  }

  SMGAddressValueAndStateList createAddress(SMGState pSmgState, SMGObject pTarget,
      SMGExplicitValue pOffset) throws SMGInconsistentException {

    SMGAddressValueAndStateList addressValueAndStates = getAddress(pSmgState, pTarget, pOffset);

    List<SMGAddressValueAndState> result = new ArrayList<>(addressValueAndStates.size());

    for (SMGAddressValueAndState addressValueAndState : addressValueAndStates.asAddressValueAndStateList()) {
      if (addressValueAndState.getObject().isUnknown()) {

        SMGKnownSymValue value = SMGKnownSymValue.valueOf(SMGValueFactory
            .getNewValue());
        SMGKnownAddVal addressValue = SMGKnownAddVal.valueOf(pTarget,
            (SMGKnownExpValue) pOffset, value);
        result.add(SMGAddressValueAndState.of(addressValueAndState.getSmgState(), addressValue));
      } else {
        result.add(addressValueAndState);
      }
    }

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
  }

  SMGAddressValueAndStateList getAddress(SMGState pSmgState, SMGObject pTarget,
      SMGExplicitValue pOffset) throws SMGInconsistentException {

    if (pTarget == null || pOffset.isUnknown()) {
      return SMGAddressValueAndStateList.of(pSmgState);
    }

    SMGRegion regionTarget;

    if(pTarget instanceof SMGRegion) {
      regionTarget = (SMGRegion) pTarget;
    } else if(pTarget == SMGObject.getNullObject()) {
      SMGAddressValueAndState result = SMGAddressValueAndState.of(pSmgState, SMGKnownAddVal.valueOf(0, pTarget, pOffset.getAsInt()));
      return SMGAddressValueAndStateList.of(result);
    } else {
      throw new AssertionError("Abstraction " + pTarget.toString() + " was not materialised.");
    }

    Integer address = pSmgState.getAddress(regionTarget, pOffset.getAsInt());

    if (address == null) {
      return SMGAddressValueAndStateList.of(pSmgState);
    }

    SMGAddressValueAndStateList addressValues = pSmgState.getPointerFromValue(address);

    return addressValues;
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
      implements CRightHandSideVisitor<List<SMGAddressAndState>, CPATransferException> {

    public ArrayVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
    }

    @Override
    protected List<SMGAddressAndState> visitDefault(CExpression exp) {
      return SMGAddressAndState.listOf(getInitialSmgState());
    }

    @Override
    public List<SMGAddressAndState> visit(CUnaryExpression unaryExpression) throws CPATransferException {
      throw new AssertionError("The result of any unary expression " +
          "cannot be an array type.");
    }

    @Override
    public List<SMGAddressAndState> visit(CBinaryExpression binaryExp) throws CPATransferException {

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
        return SMGAddressAndState.listOf(getInitialSmgState()); // If both or neither are Addresses,
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
      SMGAddressValueAndStateList result =
          handlePointerArithmetic(getInitialSmgState(), getCfaEdge(),
              address, arrayOffset, addressType, lVarIsAddress, binaryExp);
      return result.asAddressAndStateList();
    }

    @Override
    public List<SMGAddressAndState> visit(CIdExpression pVariableName) throws CPATransferException {

      List<SMGAddressAndState> addressAndStates = super.visit(pVariableName);

      //TODO correct?
      // parameter declaration array types are converted to pointer
      if (pVariableName.getDeclaration() instanceof CParameterDeclaration) {

        CType type = getRealExpressionType(pVariableName);
        if (type instanceof CArrayType) {
          // if function declaration is in form 'int foo(char b[32])' then omit array length
          //TODO support C11 6.7.6.3 7:
          // actual argument shall provide access to the first element of
          // an array with at least as many elements as specified by the size expression
          type = new CPointerType(type.isConst(), type.isVolatile(), ((CArrayType) type).getType());
        }

        List<SMGAddressAndState> result = new ArrayList<>(addressAndStates.size());

        for (SMGAddressAndState addressAndState : addressAndStates) {

          SMGAddress address = addressAndState.getObject();
          SMGState newState = addressAndState.getSmgState();

          SMGValueAndState pointerAndState =
              readValue(newState, address.getObject(),
                  address.getOffset(), type, getCfaEdge());

          SMGAddressValueAndStateList trueAddressAndState = getAddressFromSymbolicValue(pointerAndState);

          result.addAll(trueAddressAndState.asAddressAndStateList());
        }

        return result;
      } else {
        return addressAndStates;
      }
    }

    @Override
    public List<SMGAddressAndState> visit(CCastExpression cast) throws CPATransferException {

      CExpression op = cast.getOperand();

      if (op.getExpressionType() instanceof CArrayType) {
        return cast.getOperand().accept(this);
      } else {
        //TODO cast reinterpretation
        return SMGAddressAndState.listOf(getInitialSmgState());
      }
    }

    @Override
    public List<SMGAddressAndState> visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      return SMGAddressAndState.listOf(getInitialSmgState());
    }
  }

  class AssumeVisitor extends ExpressionValueVisitor {
    private Map<SMGState,BinaryRelationResult> relations = new HashMap<>();

    public AssumeVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
    }

    @Override
    public SMGValueAndStateList visit(CBinaryExpression pExp)
        throws CPATransferException {
      BinaryOperator binaryOperator = pExp.getOperator();

      switch (binaryOperator) {
      case EQUALS:
      case NOT_EQUALS:
      case LESS_EQUAL:
      case LESS_THAN:
      case GREATER_EQUAL:
      case GREATER_THAN:
        List<SMGValueAndState> result = new ArrayList<>(4);

        CExpression leftSideExpression = pExp.getOperand1();
        CExpression rightSideExpression = pExp.getOperand2();

        CFAEdge cfaEdge = getCfaEdge();

        SMGValueAndStateList leftSideValAndStates = evaluateExpressionValue(getInitialSmgState(),
            cfaEdge, leftSideExpression);

        for (SMGValueAndState leftSideValAndState : leftSideValAndStates.getValueAndStateList()) {
          SMGSymbolicValue leftSideVal = leftSideValAndState.getObject();
          SMGState newState = leftSideValAndState.getSmgState();

          SMGValueAndStateList rightSideValAndStates = evaluateExpressionValue(
              newState, cfaEdge, rightSideExpression);

          for (SMGValueAndState rightSideValAndState : rightSideValAndStates.getValueAndStateList()) {
            SMGSymbolicValue rightSideVal = rightSideValAndState.getObject();
            newState = rightSideValAndState.getSmgState();

              SMGValueAndStateList resultValueAndStates = evaluateBinaryAssumption(newState,
                  binaryOperator, leftSideVal, rightSideVal);

              for (SMGValueAndState resultValueAndState : resultValueAndStates.getValueAndStateList()) {
                newState = resultValueAndState.getSmgState();
                SMGSymbolicValue resultValue = resultValueAndState.getObject();

                //TODO: separate modifiable and unmodifiable visitor
                int leftSideTypeSize = getSizeof(cfaEdge, leftSideExpression.getExpressionType(), newState);
                int rightSideTypeSize = getSizeof(cfaEdge, rightSideExpression.getExpressionType(), newState);
                newState.addPredicateRelation(leftSideVal, leftSideTypeSize,
                    rightSideVal, rightSideTypeSize, binaryOperator, cfaEdge);
                result.add(SMGValueAndState.of(newState, resultValue));
              }
          }
        }

        return SMGValueAndStateList.copyOf(result);
      default:
        return super.visit(pExp);
      }
    }

    private boolean isPointer(SMGState pNewSmgState, SMGSymbolicValue symVal) {

      if (symVal.isUnknown()) {
        return false;
      }

      if (symVal instanceof SMGAddressValue) {
        return true;
      }

      if (pNewSmgState.isPointer(symVal.getAsInt())) {
        return true;
      } else {
        return false;
      }
    }

    private boolean isUnequal(SMGState pNewState, SMGSymbolicValue pValue1,
        SMGSymbolicValue pValue2, boolean isPointerOp1,
        boolean isPointerOp2) {

      int value1 = pValue1.getAsInt();
      int value2 = pValue2.getAsInt();

      if (isPointerOp1 && isPointerOp2) {

        return value1 != value2;
      } else if ((isPointerOp1 && value2 == 0) || (isPointerOp2 && value1 == 0)) {
        return value1 != value2;
      } else {
        return pNewState.isInNeq(pValue1, pValue2);
      }
    }

    private PointerComparisonResult comparePointer(SMGKnownAddVal pV1, SMGKnownAddVal pV2, BinaryOperator pOp) {

      SMGObject object1 = pV1.getObject();
      SMGObject object2 = pV2.getObject();

      boolean isTrue = false;
      boolean isFalse = true;

      // there can be more precise comparsion when pointer point to the
      // same object.
      if (object1 == object2) {
        int offset1 = pV1.getOffset().getAsInt();
        int offset2 = pV2.getOffset().getAsInt();

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
      return PointerComparisonResult.valueOf(isTrue, isFalse);
    }

    private SMGValueAndState evaluateBinaryAssumptionOfConcreteSymbolicValues(SMGState pNewState, BinaryOperator pOp, SMGKnownSymValue pV1, SMGKnownSymValue pV2) {

      boolean isPointerOp1 = pV1 instanceof SMGKnownAddVal;
      boolean isPointerOp2 = pV2 instanceof SMGKnownAddVal;

      int v1 = pV1.getAsInt();
      int v2 = pV2.getAsInt();

      boolean areEqual = (v1 == v2);
      boolean areNonEqual = (isUnequal(pNewState, pV1, pV2, isPointerOp1, isPointerOp2));

      boolean isTrue = false;
      boolean isFalse = false;
      boolean impliesEqWhenFalse = false;
      boolean impliesNeqWhenTrue = false;
      boolean impliesEqWhenTrue = false;
      boolean impliesNeqWhenFalse = false;

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
            SMGKnownAddVal p1 = (SMGKnownAddVal) pV1;
            SMGKnownAddVal p2 = (SMGKnownAddVal) pV2;
            PointerComparisonResult result = comparePointer(p1, p2, pOp);
            isFalse = result.isFalse();
            isTrue = result.isTrue();
          }
        break;
      default:
        throw new AssertionError(
            "Binary Relation with non-relational operator: " + pOp.toString());
      }

      BinaryRelationResult relationResult = new BinaryRelationResult(isTrue, isFalse, impliesEqWhenFalse, impliesNeqWhenFalse, impliesEqWhenTrue, impliesNeqWhenTrue, pV1, pV2);
      relations.put(pNewState, relationResult);

      if(isTrue) {
        return SMGValueAndState.of(pNewState, SMGKnownSymValue.TRUE);
      } else if(isFalse) {
        return SMGValueAndState.of(pNewState, SMGKnownSymValue.FALSE);
      } else {
        return SMGValueAndState.of(pNewState);
      }
    }

    public SMGValueAndStateList evaluateBinaryAssumption(SMGState pNewState, BinaryOperator pOp, SMGSymbolicValue pV1, SMGSymbolicValue pV2) throws SMGInconsistentException {

      // If a value is unknown, we can't make further assumptions about it.
      if (pV2.isUnknown() || pV1.isUnknown()) {
        return SMGValueAndStateList.of(pNewState);
      }

      boolean isPointerOp1 = isPointer(pNewState, pV1);
      boolean isPointerOp2 = isPointer(pNewState, pV2);

      SMGValueAndStateList operand1AndStates;

      if(isPointerOp1) {
        operand1AndStates = getAddressFromSymbolicValue(SMGValueAndState.of(pNewState, pV1));
      } else {
        operand1AndStates = SMGValueAndStateList.of(pNewState, pV1);
      }

      List<SMGValueAndState> result = new ArrayList<>(4);

      for(SMGValueAndState operand1AndState : operand1AndStates.getValueAndStateList()) {

        SMGKnownSymValue operand1 = (SMGKnownSymValue) operand1AndState.getObject();
        SMGState newState = operand1AndState.getSmgState();

        SMGValueAndStateList operand2AndStates;

        if(isPointerOp2) {
          operand2AndStates = getAddressFromSymbolicValue(SMGValueAndState.of(newState, pV2));
        } else {
          operand2AndStates = SMGValueAndStateList.of(pNewState, pV2);
        }

        for (SMGValueAndState operand2AndState : operand2AndStates.getValueAndStateList()) {

          SMGKnownSymValue operand2 = (SMGKnownSymValue) operand2AndState.getObject();
          newState = operand2AndState.getSmgState();

          SMGValueAndState resultValueAndState = evaluateBinaryAssumptionOfConcreteSymbolicValues(newState, pOp, operand1, operand2);
          result.add(resultValueAndState);
        }
      }

      return SMGValueAndStateList.copyOf(result);
    }

    public boolean impliesEqOn(boolean pTruth, SMGState pState) {
      if (!relations.containsKey(pState)) {
        return false;
      }
      return relations.get(pState).impliesEq(pTruth);
    }

    public boolean impliesNeqOn(boolean pTruth, SMGState pState) {
      if (!relations.containsKey(pState)) {
        return false;
      }
      return relations.get(pState).impliesNeq(pTruth);
    }

    public SMGSymbolicValue impliesVal1(SMGState pState) {
      return relations.get(pState).getVal1();
    }

    public SMGSymbolicValue impliesVal2(SMGState pState) {
      return relations.get(pState).getVal2();
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
      implements CRightHandSideVisitor<List<SMGAddressAndState>, CPATransferException> {

    public StructAndUnionVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
      super(pCfaEdge, pNewState);
    }

    @Override
    public List<SMGAddressAndState> visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      return SMGAddressAndState.listOf(getInitialSmgState());
    }

    @Override
    public List<SMGAddressAndState> visit(CCastExpression cast) throws CPATransferException {

      CExpression op = cast.getOperand();

      if (isStructOrUnionType(op.getExpressionType())) {
        return cast.getOperand().accept(this);
      } else {
        //TODO cast reinterpretation
        return SMGAddressAndState.listOf(getInitialSmgState());
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
  class ExpressionValueVisitor extends DefaultCExpressionVisitor<SMGValueAndStateList, CPATransferException>
    implements CRightHandSideVisitor<SMGValueAndStateList, CPATransferException> {

    protected final CFAEdge cfaEdge;
    protected final SMGState initialSmgState;

    public ExpressionValueVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      initialSmgState = pSmgState;
    }

    @Override
    protected SMGValueAndStateList visitDefault(CExpression pExp) {
      return SMGValueAndStateList.of(getInitialSmgState());
    }

    @Override
    public SMGValueAndStateList visit(CArraySubscriptExpression exp) throws CPATransferException {

      List<SMGAddressAndState> addressAndStateList =
          evaluateArraySubscriptAddress(getInitialSmgState(), getCfaEdge(), exp);

      List<SMGValueAndState> result = new ArrayList<>(addressAndStateList.size());

      for (SMGAddressAndState addressAndState : addressAndStateList) {
        SMGAddress address = addressAndState.getObject();
        SMGState newState = addressAndState.getSmgState();

        if (address.isUnknown()) {
          result.add(SMGValueAndState.of(newState));
          continue;
        }

        SMGValueAndState symbolicValueResultAndState = readValue(newState, address.getObject(), address.getOffset(), getRealExpressionType(exp), cfaEdge);
        result.add(symbolicValueResultAndState);
      }

      return SMGValueAndStateList.copyOf(result);
    }

    @Override
    public SMGValueAndStateList visit(CIntegerLiteralExpression exp) throws CPATransferException {

      BigInteger value = exp.getValue();

      boolean isZero = value.equals(BigInteger.ZERO);

      SMGSymbolicValue val = (isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance());
      return SMGValueAndStateList.of(getInitialSmgState(), val);
    }

    @Override
    public SMGValueAndStateList visit(CCharLiteralExpression exp) throws CPATransferException {

      char value = exp.getCharacter();

      SMGSymbolicValue val = (value == 0) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
      return SMGValueAndStateList.of(getInitialSmgState(), val);
    }

    @Override
    public SMGValueAndStateList visit(CFieldReference fieldReference) throws CPATransferException {

      List<SMGValueAndState> result = new ArrayList<>(2);
      List<SMGAddressAndState> addressOfFieldAndStates =
          getAddressOfField(getInitialSmgState(), getCfaEdge(), fieldReference);

      for (SMGAddressAndState addressOfFieldAndState : addressOfFieldAndStates) {
        SMGAddress addressOfField = addressOfFieldAndState.getObject();
        SMGState newState = addressOfFieldAndState.getSmgState();


        if (addressOfField.isUnknown()) {
          result.add(SMGValueAndState.of(newState));
          continue;
        }

        CType fieldType = getRealExpressionType(fieldReference);

        SMGValueAndState resultState = readValue(newState, addressOfField.getObject(), addressOfField.getOffset(), fieldType, cfaEdge);

        result.add(resultState);
      }

      return SMGValueAndStateList.copyOf(result);
    }

    @Override
    public SMGValueAndStateList visit(CFloatLiteralExpression exp)
        throws CPATransferException {

      boolean isZero = exp.getValue().equals(BigDecimal.ZERO);

      SMGSymbolicValue val = isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
      return SMGValueAndStateList.of(getInitialSmgState(), val);
    }

    @Override
    public SMGValueAndStateList visit(CIdExpression idExpression)
        throws CPATransferException {

      CSimpleDeclaration decl = idExpression.getDeclaration();

      if (decl instanceof CEnumerator) {

        long enumValue = ((CEnumerator) decl).getValue();

        SMGSymbolicValue val = enumValue == 0 ? SMGKnownSymValue.ZERO
            : SMGUnknownValue.getInstance();
        return SMGValueAndStateList.of(getInitialSmgState(), val);

      } else if (decl instanceof CVariableDeclaration
          || decl instanceof CParameterDeclaration) {
        SMGState smgState = getInitialSmgState();

        SMGObject variableObject = smgState
            .getObjectForVisibleVariable(idExpression.getName());

        SMGValueAndState result = readValue(smgState, variableObject, SMGKnownExpValue.ZERO,
            getRealExpressionType(idExpression), cfaEdge);

        return SMGValueAndStateList.of(result);
      }

      return SMGValueAndStateList.of(getInitialSmgState());
    }

    @Override
    public SMGValueAndStateList visit(CUnaryExpression unaryExpression) throws CPATransferException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case AMPER:
        throw new UnrecognizedCCodeException("Can't use & of expression " + unaryOperand.toASTString(), cfaEdge,
            unaryExpression);

      case MINUS:

        List<SMGValueAndState> result = new ArrayList<>(2);

        SMGValueAndStateList valueAndStates = unaryOperand.accept(this);

        for (SMGValueAndState valueAndState : valueAndStates.getValueAndStateList()) {

          SMGSymbolicValue value = valueAndState.getObject();

          SMGSymbolicValue val = value.equals(SMGKnownSymValue.ZERO) ? value
              : SMGUnknownValue.getInstance();
          result.add(SMGValueAndState.of(valueAndState.getSmgState(), val));
        }

        return SMGValueAndStateList.copyOf(result);

      case SIZEOF:
        int size = getSizeof(cfaEdge, getRealExpressionType(unaryOperand), getInitialSmgState(), unaryOperand);
        SMGSymbolicValue val = (size == 0) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
        return SMGValueAndStateList.of(getInitialSmgState(), val);
      case TILDE:

      default:
        return SMGValueAndStateList.of(getInitialSmgState());
      }
    }

    @Override
    public SMGValueAndStateList visit(CPointerExpression pointerExpression) throws CPATransferException {

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
    public SMGValueAndStateList visit(CTypeIdExpression typeIdExp) throws UnrecognizedCCodeException {

      TypeIdOperator typeOperator = typeIdExp.getOperator();
      CType type = typeIdExp.getType();

      switch (typeOperator) {
      case SIZEOF:
        SMGSymbolicValue val =
            getSizeof(cfaEdge, type, getInitialSmgState(), typeIdExp) == 0 ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
        return SMGValueAndStateList.of(getInitialSmgState(), val);
      default:
        return SMGValueAndStateList.of(getInitialSmgState());
      //TODO Investigate the other Operators.
      }
    }

    @Override
    public SMGValueAndStateList visit(CBinaryExpression exp) throws CPATransferException {

      BinaryOperator binaryOperator = exp.getOperator();
      CExpression lVarInBinaryExp = exp.getOperand1();
      CExpression rVarInBinaryExp = exp.getOperand2();
      List<SMGValueAndState> result = new ArrayList<>(4);

      SMGValueAndStateList lValAndStates = evaluateExpressionValue(getInitialSmgState(), getCfaEdge(), lVarInBinaryExp);

      for (SMGValueAndState lValAndState : lValAndStates.getValueAndStateList()) {

        SMGSymbolicValue lVal = lValAndState.getObject();
        SMGState newState = lValAndState.getSmgState();

        SMGValueAndStateList rValAndStates = evaluateExpressionValue(newState, getCfaEdge(), rVarInBinaryExp);

        for (SMGValueAndState rValAndState : rValAndStates.getValueAndStateList()) {

          SMGSymbolicValue rVal = rValAndState.getObject();
          newState = rValAndState.getSmgState();

          if (rVal.equals(SMGUnknownValue.getInstance())
              || lVal.equals(SMGUnknownValue.getInstance())) {
            result.add(SMGValueAndState.of(newState));
            continue;
          }

          SMGValueAndStateList resultValueAndState =
              evaluateBinaryExpression(lVal, rVal, binaryOperator, newState);
          result.addAll(resultValueAndState.getValueAndStateList());
        }
      }

      return SMGValueAndStateList.copyOf(result);
    }

    private SMGValueAndStateList evaluateBinaryExpression(SMGSymbolicValue lVal, SMGSymbolicValue rVal, BinaryOperator binaryOperator, SMGState newState) throws SMGInconsistentException {

      if (lVal.equals(SMGUnknownValue.getInstance()) || rVal.equals(SMGUnknownValue.getInstance())) {
        return SMGValueAndStateList.of(newState);
      }

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

        boolean isZero;

        switch (binaryOperator) {
        case PLUS:
        case SHIFT_LEFT:
        case BINARY_OR:
        case BINARY_XOR:
        case SHIFT_RIGHT:
          isZero = lVal.equals(SMGKnownSymValue.ZERO) && rVal.equals(SMGKnownSymValue.ZERO);
          SMGSymbolicValue val = (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
          return SMGValueAndStateList.of(newState, val);

        case MINUS:
        case MODULO:
          isZero = (lVal.equals(rVal));
          val = (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
          return SMGValueAndStateList.of(newState, val);

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if (rVal.equals(SMGKnownSymValue.ZERO)) {
            return SMGValueAndStateList.of(newState);
          }

          isZero = lVal.equals(SMGKnownSymValue.ZERO);
          val = (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
          return SMGValueAndStateList.of(newState, val);

        case MULTIPLY:
        case BINARY_AND:
          isZero = lVal.equals(SMGKnownSymValue.ZERO)
              || rVal.equals(SMGKnownSymValue.ZERO);
          val = (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
          return SMGValueAndStateList.of(newState, val);

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

        AssumeVisitor v = getAssumeVisitor(getCfaEdge(), newState);



          SMGValueAndStateList assumptionValueAndStates = v.evaluateBinaryAssumption(newState, binaryOperator, lVal, rVal);

          List<SMGValueAndState> result = new ArrayList<>(2);

          for (SMGValueAndState assumptionValueAndState : assumptionValueAndStates.getValueAndStateList()) {
            newState = assumptionValueAndState.getSmgState();
            SMGSymbolicValue assumptionVal = assumptionValueAndState.getObject();

            if (assumptionVal == SMGKnownSymValue.FALSE) {
              SMGValueAndState resultValueAndState =
                  SMGValueAndState.of(newState, SMGKnownSymValue.ZERO);
              result.add(resultValueAndState);
            } else {
              result.add(SMGValueAndState.of(newState));
            }
          }

          return SMGValueAndStateList.copyOf(result);
      }

      default:
        return SMGValueAndStateList.of(getInitialSmgState());
      }
    }

    @Override
    public SMGValueAndStateList visit(CCastExpression cast) throws CPATransferException {
      // For different types we need different visitors,
      // TODO doesn't calculate type reinterpretations
      return evaluateExpressionValue(getInitialSmgState(), getCfaEdge(), cast.getOperand());
    }

    protected SMGValueAndStateList dereferenceArray(CExpression exp, CType derefType) throws CPATransferException {

      List<SMGValueAndState> result = new ArrayList<>(2);

      ArrayVisitor v = getArrayVisitor(getCfaEdge(), getInitialSmgState());

      List<SMGAddressAndState> addressAndStates = exp.accept(v);

      for (SMGAddressAndState addressAndState : addressAndStates) {
        SMGAddress address = addressAndState.getObject();
        SMGState newState = addressAndState.getSmgState();

        if (address.isUnknown()) {
          // We can't resolve the field to dereference, therefore
          // we must assume, that it is invalid
          result.add(handleUnknownDereference(newState, cfaEdge));
          continue;
        }

        // a == &a[0]
        if (derefType instanceof CArrayType) {
          result.addAll(createAddress(newState, address.getObject(), address.getOffset()).asAddressValueAndStateList());
        } else {
          result.add(readValue(newState, address.getObject(), address.getOffset(), derefType, cfaEdge));
        }
      }

      return SMGValueAndStateList.copyOf(result);
    }

    protected final SMGValueAndStateList dereferencePointer(CExpression exp,
        CType derefType) throws CPATransferException {

      List<SMGValueAndState> result = new ArrayList<>(2);

      SMGAddressValueAndStateList addressAndStates = evaluateAddress(
          getInitialSmgState(), getCfaEdge(), exp);

      for (SMGAddressValueAndState addressAndState : addressAndStates.asAddressValueAndStateList()) {

        SMGAddressValue address = addressAndState.getObject();
        SMGState newState = addressAndState.getSmgState();

        if (address.isUnknown()) {
          // We can't resolve the field to dereference , therefore
          // we must assume, that it is invalid
          result.add(handleUnknownDereference(newState, getCfaEdge()));
          continue;
        }

        // a == &a[0]
        if (derefType instanceof CArrayType) {
          result.addAll(createAddress(newState, address.getObject(), address.getOffset()).asAddressValueAndStateList());
        } else {
          result.add(readValue(newState, address.getObject(), address.getOffset(), derefType, cfaEdge));
        }
      }

      return SMGValueAndStateList.copyOf(result);
    }

    @Override
    public SMGValueAndStateList visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      return SMGValueAndStateList.of(getInitialSmgState());
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

    /* Will be updated while evaluating left hand side expressions.
     * Represents the current state of the value state pair
     */
    private SMGState smgState;

    /*
     * If there is more than one result based on the current
     * smg State due to abstraction, store the additional smgStates
     * that have to be usd to calculate a different result for the current
     * value in this list.
     *
     */
    private final List<SMGState> smgStatesToBeProccessed = new ArrayList<>();

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

    public List<SMGState> getSmgStatesToBeProccessed() {
      return smgStatesToBeProccessed;
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

        SMGValueAndStateList symValueAndStates = null;

        try {
          symValueAndStates = evaluateAssumptionValue(smgState, edge, binaryExp);
        } catch (CPATransferException e) {
          UnrecognizedCCodeException e2 = new UnrecognizedCCodeException(
              "SMG cannot be evaluated", binaryExp);
          e2.initCause(e);
          throw e2;
        }

        SMGValueAndState symValueAndState = null;

        if (symValueAndStates.size() > 0) {
          symValueAndState = symValueAndStates.getValueAndStateList().get(0);
        } else {
          symValueAndState = SMGValueAndState.of(getNewState());
        }

        for (int c = 1; c < symValueAndStates.size(); c++) {
          smgStatesToBeProccessed.add(symValueAndStates.getValueAndStateList().get(c).getSmgState());
        }

        SMGSymbolicValue symValue = symValueAndState.getObject();
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
      case NOT_EQUALS:
        //TODO Check, if one of the two operand types is expressed as pointer, e.g. pointer, struct, array, etc
        return true;
        default :
          return false;
      }
    }

    @Override
    protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
        throws UnrecognizedCCodeException {
      return evaluateLeftHandSideExpression(pCPointerExpression);
    }

    private Value evaluateLeftHandSideExpression(CLeftHandSide leftHandSide)
        throws UnrecognizedCCodeException {

      SMGValueAndStateList valueAndStates = null;

      try {
        valueAndStates = evaluateExpressionValue(smgState, edge, leftHandSide);
      } catch (CPATransferException e) {
        UnrecognizedCCodeException e2 =
            new UnrecognizedCCodeException("SMG cannot be evaluated", leftHandSide);
        e2.initCause(e);
        throw e2;
      }

      SMGValueAndState valueAndState = null;

      if (valueAndStates.size() > 0) {
        valueAndState = valueAndStates.getValueAndStateList().get(0);
      } else {
        valueAndState = SMGValueAndState.of(getNewState());
      }

      for (int c = 1; c < valueAndStates.size(); c++) {
        smgStatesToBeProccessed.add(valueAndStates.getValueAndStateList().get(c).getSmgState());
      }

      SMGSymbolicValue value = valueAndState.getObject();
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

  /**
   * @param edge the edge to handle
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

  protected CSizeOfVisitor getSizeOfVisitor(CFAEdge pEdge, SMGState pState) {
    return new CSizeOfVisitor(machineModel, pEdge, pState, logger);
  }

  protected CSizeOfVisitor getBitSizeOfVisitor(CFAEdge pEdge, SMGState pState) {
    return new CBitSizeOfVisitor(machineModel, pEdge, pState, logger);
  }

  protected CSizeOfVisitor getSizeOfVisitor(CFAEdge pEdge, SMGState pState,
      CExpression pExpression) {
    return new CSizeOfVisitor(machineModel, pEdge, pState, logger, pExpression);
  }

  public static class SMGAddressValueAndState extends SMGValueAndState {

    private SMGAddressValueAndState(SMGState pState, SMGAddressValue pValue) {
      super(pState, pValue);
    }

    public SMGAddressAndState asSMGAddressAndState() {
      return SMGAddressAndState.of(getSmgState(), getObject().getAddress());
    }

    @Override
    public SMGAddressValue getObject() {
      return (SMGAddressValue) super.getObject();
    }

    public static SMGAddressValueAndState of(SMGState pState, SMGAddressValue pValue) {
      return new SMGAddressValueAndState(pState, pValue);
    }

    public static SMGAddressValueAndState of(SMGState pState) {
      return new SMGAddressValueAndState(pState, SMGUnknownValue.getInstance());
    }
  }

  public static class SMGAddressAndState extends SMGAbstractObjectAndState<SMGAddress> {

    private SMGAddressAndState(SMGState pState, SMGAddress pAddress) {
      super(pState, pAddress);
    }

    public static List<SMGAddressAndState> listOf(SMGState pInitialSmgState, SMGAddress pValueOf) {
      return ImmutableList.of(of(pInitialSmgState, pValueOf));
    }

    public static List<SMGAddressAndState> listOf(SMGState pInitialSmgState) {
      return ImmutableList.of(of(pInitialSmgState));
    }

    public static SMGAddressAndState of(SMGState pState) {
      return new SMGAddressAndState(pState, SMGAddress.getUnknownInstance());
    }

    public static SMGAddressAndState of(SMGState pState, SMGAddress pAddress) {
      return new SMGAddressAndState(pState, pAddress);
    }
  }

  public static class SMGValueAndStateList {

    private final List<? extends SMGValueAndState> valueAndStateList;

    public SMGValueAndStateList(List<? extends SMGValueAndState> list) {
      valueAndStateList = ImmutableList.copyOf(list);
    }

    public SMGValueAndStateList(SMGValueAndState pE) {
      valueAndStateList = ImmutableList.of(pE);
    }

    public int size() {
      return valueAndStateList.size();
    }

    @Override
    public String toString() {
      return valueAndStateList.toString();
    }

    @Override
    public boolean equals(Object pObj) {
      return valueAndStateList.equals(pObj);
    }

    @Override
    public int hashCode() {
      return valueAndStateList.hashCode();
    }

    public List<? extends SMGValueAndState> getValueAndStateList() {
      return valueAndStateList;
    }

    public static SMGValueAndStateList of(SMGValueAndState pE) {
      return new SMGValueAndStateList(pE);
    }

    public static SMGValueAndStateList of(SMGState smgState) {
      return of(SMGValueAndState.of(smgState));
    }

    public static SMGValueAndStateList of(SMGState smgState, SMGSymbolicValue val) {
      return of(SMGValueAndState.of(smgState, val));
    }

    public static SMGValueAndStateList copyOf(List<SMGValueAndState> pE) {
      return new SMGValueAndStateList(pE);
    }

    public List<SMGState> asSMGStateList() {

      return FluentIterable.from(valueAndStateList).transform(new Function<SMGValueAndState, SMGState>() {

        @Override
        public SMGState apply(SMGValueAndState valueAndState) {

          return valueAndState.getSmgState();
        }
      }).toList();
    }

    public static SMGValueAndStateList copyOfUnknownValue(List<SMGState> pNewStates) {

      List<SMGValueAndState> result = new ArrayList<>(pNewStates.size());

      for (SMGState state : pNewStates) {
        result.add(SMGValueAndState.of(state));
      }

      return copyOf(result);
    }
  }

  public static class SMGAddressValueAndStateList extends SMGValueAndStateList {


    private SMGAddressValueAndStateList(List<SMGAddressValueAndState> pList) {
      super(ImmutableList.copyOf(pList));
    }

    public List<SMGAddressAndState> asAddressAndStateList() {


      return FluentIterable.from(getValueAndStateList())
          .transform(new Function<SMGValueAndState, SMGAddressAndState>() {

            @Override
            public SMGAddressAndState apply(SMGValueAndState valueAndState) {

              SMGAddressValueAndState addressValueAndState = (SMGAddressValueAndState) valueAndState;


              SMGAddressValue addressValue = addressValueAndState.getObject();
              SMGState newState = addressValueAndState.getSmgState();

              if (addressValue.isUnknown()) {
                return SMGAddressAndState.of(newState);
              }

              return SMGAddressAndState.of(newState, addressValue.getAddress());
            }
          }).toList();
    }

    private SMGAddressValueAndStateList(SMGAddressValueAndState pE) {
      super(pE);
    }

    public List<SMGAddressValueAndState> asAddressValueAndStateList() {
      return FluentIterable.from(getValueAndStateList()).transform(new Function<SMGValueAndState, SMGAddressValueAndState>() {

            @Override
            public SMGAddressValueAndState apply(SMGValueAndState pE) {
              return (SMGAddressValueAndState) pE;
            }

          }).toList();
    }

    public static SMGAddressValueAndStateList of(SMGAddressValueAndState pE) {
      return new SMGAddressValueAndStateList(pE);
    }

    public static SMGAddressValueAndStateList of(SMGState smgState) {
      return of(SMGAddressValueAndState.of(smgState));
    }

    public static SMGAddressValueAndStateList copyOfAddressValueList(List<SMGAddressValueAndState> pList) {
      return new SMGAddressValueAndStateList(pList);
    }
  }

  public static class SMGValueAndState extends SMGAbstractObjectAndState<SMGSymbolicValue> {

    private SMGValueAndState(SMGState pState, SMGSymbolicValue pValue) {
      super(pState, pValue);
    }

    public static SMGValueAndState of(SMGState pState) {
      return new SMGValueAndState(pState, SMGUnknownValue.getInstance());
    }

    public static SMGValueAndState of(SMGState pState, SMGSymbolicValue pValue) {
      return new SMGValueAndState(pState, pValue);
    }
  }

  public static class SMGExplicitValueAndState extends SMGAbstractObjectAndState<SMGExplicitValue> {

    private SMGExplicitValueAndState(SMGState pState, SMGExplicitValue pValue) {
      super(pState, pValue);
    }

    public static SMGExplicitValueAndState of(SMGState pState) {
      return new SMGExplicitValueAndState(pState, SMGUnknownValue.getInstance());
    }

    public static SMGExplicitValueAndState of(SMGState pState, SMGExplicitValue pValue) {
      return new SMGExplicitValueAndState(pState, pValue);
    }
  }

  public abstract static class SMGAbstractObjectAndState<T> {
    private final SMGState smgState;
    private final T object;

    private SMGAbstractObjectAndState(SMGState pState, T pValue) {
      smgState = pState;
      object = pValue;
    }

    public T getObject() {
      return object;
    }

    public SMGState getSmgState() {
      return smgState;
    }

    @Override
    public String toString() {
      return object.toString() + " StateId: " + smgState.getId();
    }
  }

  public static class CBitSizeOfVisitor extends CSizeOfVisitor {

    public CBitSizeOfVisitor(
        MachineModel pModel,
        CFAEdge pEdge,
        SMGState pState,
        LogManagerWithoutDuplicates logger,
        CExpression pExpression) {
      super(pModel, pEdge, pState, logger, pExpression);
    }

    public CBitSizeOfVisitor(
        MachineModel pModel,
        CFAEdge pEdge,
        SMGState pState, LogManagerWithoutDuplicates pLogger) {
      super(pModel, pEdge, pState, pLogger);
    }

    @Override
    public Integer visit(CEnumType pEnumType) throws IllegalArgumentException {
      if (pEnumType.isBitField()) {
        return pEnumType.getBitFieldSize();
      }
      return super.visit(pEnumType);
    }

    @Override
    public Integer visit(CSimpleType pSimpleType) throws IllegalArgumentException {
      if (pSimpleType.isBitField()) {
        return pSimpleType.getBitFieldSize();
      }
      return super.visit(pSimpleType);
    }

    @Override
    public Integer visit(CTypedefType pTypedefType) throws IllegalArgumentException {
      if (pTypedefType.isBitField()) {
        return pTypedefType.getBitFieldSize();
      }
      return super.visit(pTypedefType);
    }

    @Override
    public Integer visit(CCompositeType pCompositeType) throws IllegalArgumentException {
      if (pCompositeType.isBitField()) {
        return pCompositeType.getBitFieldSize();
      }
      return super.visit(pCompositeType);
    }

    @Override
    public Integer visit(CElaboratedType pElaboratedType) throws IllegalArgumentException {
      if (pElaboratedType.isBitField()) {
        return pElaboratedType.getBitFieldSize();
      }
      return super.visit(pElaboratedType);
    }
  }

  public static class CSizeOfVisitor extends BaseSizeofVisitor {
    int sizeofCharInBits;
    private final CFAEdge edge;
    private final SMGState state;
    private final CExpression expression;
    private final SMGExpressionEvaluator eval;

    public CSizeOfVisitor(MachineModel pModel, CFAEdge pEdge, SMGState pState, LogManagerWithoutDuplicates logger,
        CExpression pExpression) {
      super(pModel);

      edge = pEdge;
      state = pState;
      expression = pExpression;
      eval = new SMGExpressionEvaluator(logger, pModel);
      sizeofCharInBits = pModel.getSizeofCharInBits();
    }

    public CSizeOfVisitor(MachineModel pModel, CFAEdge pEdge, SMGState pState,
        LogManagerWithoutDuplicates pLogger) {
      super(pModel);

      edge = pEdge;
      state = pState;
      expression = null;
      eval = new SMGExpressionEvaluator(pLogger, pModel);
      sizeofCharInBits = pModel.getSizeofCharInBits();
    }

    @Override
    public Integer visit(CCompositeType pCompositeType) throws IllegalArgumentException {
      int result = sizeofCharInBits;
      sizeofCharInBits = 1;
      result *= super.visit(pCompositeType);
      return result;
    }

    @Override
    public Integer visit(CElaboratedType pElaboratedType) throws IllegalArgumentException {
      int result = sizeofCharInBits;
      sizeofCharInBits = 1;
      result *= super.visit(pElaboratedType);
      return result;
    }

    @Override
    public Integer visit(CEnumType pEnumType) throws IllegalArgumentException {
      int result = sizeofCharInBits;
      sizeofCharInBits = 1;
      result *= super.visit(pEnumType);
      return result;
    }

    @Override
    public Integer visit(CFunctionType pFunctionType) throws IllegalArgumentException {
      int result = sizeofCharInBits;
      sizeofCharInBits = 1;
      result *= super.visit(pFunctionType);
      return result;
    }

    @Override
    public Integer visit(CPointerType pPointerType) throws IllegalArgumentException {
      int result = sizeofCharInBits;
      sizeofCharInBits = 1;
      result *= super.visit(pPointerType);
      return result;
    }

    @Override
    public Integer visit(CProblemType pProblemType) throws IllegalArgumentException {
      int result = sizeofCharInBits;
      sizeofCharInBits = 1;
      result *= super.visit(pProblemType);
      return result;
    }

    @Override
    public Integer visit(CSimpleType pSimpleType) throws IllegalArgumentException {
      int result = sizeofCharInBits;
      sizeofCharInBits = 1;
      result *= super.visit(pSimpleType);
      return result;
    }

    @Override
    public Integer visit(CTypedefType pTypedefType) throws IllegalArgumentException {
      int result = sizeofCharInBits;
      sizeofCharInBits = 1;
      result *= super.visit(pTypedefType);
      return result;
    }

    @Override
    public Integer visit(CVoidType pVoidType) throws IllegalArgumentException {
      int result = sizeofCharInBits;
      sizeofCharInBits = 1;
      result *= super.visit(pVoidType);
      return result;
    }

    @Override
    public Integer visit(CArrayType pArrayType) throws IllegalArgumentException {

      CExpression arrayLength = pArrayType.getLength();

      int sizeOfType = pArrayType.getType().accept(this);

      /* If the array type has a constant size, we can simply
       * get the length of the array, but if the size
       * of the array type is variable, we have to try and calculate
       * the current size.
       */
      int length;

      if(arrayLength == null) {
        // treat size of unknown array length type as ptr
        int result = sizeofCharInBits;
        sizeofCharInBits = 1;
        result *= super.visit(pArrayType);
        return result;
      } else if (arrayLength instanceof CIntegerLiteralExpression) {
        length = ((CIntegerLiteralExpression) arrayLength).getValue().intValue();
      } else if (edge instanceof CDeclarationEdge) {

        /* If we currently declare the array of this type,
         * we simply need to calculate the current length of the array
         * from the given expression in the type.
         */
        SMGExplicitValue lengthAsExplicitValue;

        try {
          lengthAsExplicitValue = eval.evaluateExplicitValueV2(state, edge, arrayLength);
        } catch (CPATransferException e) {
          throw new IllegalArgumentException(
              "Exception when calculating array length of " + pArrayType.toASTString("") + ".", e);
        }

        if (lengthAsExplicitValue.isUnknown()) {
          length = handleUnkownArrayLengthValue(pArrayType);
        } else {
          length = lengthAsExplicitValue.getAsInt();
        }

      } else {

        /*
         * If we are not at the declaration of the variable array type, we try to get the
         * smg object that represents the array, and calculate the current array size that way.
         */

        if(expression instanceof CLeftHandSide) {

          LValueAssignmentVisitor visitor = eval.getLValueAssignmentVisitor(edge, state);

          List<SMGAddressAndState> addressOfFieldAndState;
          try {
            addressOfFieldAndState = expression.accept(visitor);
          } catch (CPATransferException e) {
            return handleUnkownArrayLengthValue(pArrayType);
          }

          assert addressOfFieldAndState.size() > 0;

          SMGAddress addressOfField = addressOfFieldAndState.get(0).getObject();

          if (addressOfField.isUnknown()) {
            return handleUnkownArrayLengthValue(pArrayType);
          }

          SMGObject arrayObject = addressOfField.getObject();
          int offset = addressOfField.getOffset().getAsInt();
          return arrayObject.getSize() - offset;
        } else {
          throw new IllegalArgumentException(
              "Unable to calculate the size of the array type " + pArrayType.toASTString("") + ".");
        }
      }

      return length * sizeOfType;
    }

    protected int handleUnkownArrayLengthValue(CArrayType pArrayType) {
      throw new IllegalArgumentException(
          "Can't calculate array length of type " + pArrayType.toASTString("") + ".");
    }
  }

  public static class BinaryRelationResult {

    private final boolean isTrue;
    private final boolean isFalse;

    private final boolean impliesEqWhenTrue;
    private final boolean impliesNeqWhenTrue;

    private final boolean impliesEqWhenFalse;
    private final boolean impliesNeqWhenFalse;

    private final SMGSymbolicValue val1;
    private final SMGSymbolicValue val2;

    /**
     * Creates an object of the BinaryRelationResult. The object is used to
     * determine the relation between two symbolic values in the context of
     * the given smgState and the given binary operator. Note that the given
     * symbolic values, which may also be address values, do not have to be
     * part of the given Smg. The definition of an smg implies conditions for
     * its values, even if they are not part of it.
     *
     * @param pIsTrue boolean expression is true.
     * @param pIsFalse boolean expression is false
     * @param pImpliesEqWhenFalse if boolean expression is false, operands are equal
     * @param pImpliesNeqWhenFalse if boolean expression is false, operands are unequal
     * @param pImpliesEqWhenTrue if boolean expression is true, operands are equal
     * @param pImpliesNeqWhenTrue if boolean expression is true, operands are unequal
     * @param pVal1 operand 1 of boolean expression
     * @param pVal2 operand 2 of boolean expression
     *
     */
    public BinaryRelationResult(boolean pIsTrue, boolean pIsFalse, boolean pImpliesEqWhenFalse,
        boolean pImpliesNeqWhenFalse, boolean pImpliesEqWhenTrue, boolean pImpliesNeqWhenTrue,
        SMGSymbolicValue pVal1, SMGSymbolicValue pVal2) {
      isTrue = pIsTrue;
      isFalse = pIsFalse;
      impliesEqWhenFalse = pImpliesEqWhenFalse;
      impliesNeqWhenFalse = pImpliesNeqWhenFalse;
      impliesEqWhenTrue = pImpliesEqWhenTrue;
      impliesNeqWhenTrue = pImpliesNeqWhenTrue;
      val1 = pVal1;
      val2 = pVal2;
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

  private static class PointerComparisonResult {

    private final boolean isTrue;
    private final boolean isFalse;

    private PointerComparisonResult(boolean pIsTrue, boolean pIsFalse) {
      isTrue = pIsTrue;
      isFalse = pIsFalse;
    }

    public static PointerComparisonResult valueOf(boolean pIsTrue, boolean pIsFalse) {
      return new PointerComparisonResult(pIsTrue, pIsFalse);
    }

    public boolean isTrue() {
      return isTrue;
    }

    public boolean isFalse() {
      return isFalse;
    }
  }
}