// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.UnsignedLongs;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.FunctionValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue.NegativeNaN;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.BuiltinFloatFunctions;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;
import org.sosy_lab.cpachecker.util.BuiltinOverflowFunctions;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This visitor visits values mostly on the right hand side to get values (SMG or not) (but also on
 * the left hand side, for example concrete values used in array access) Important: we need to reuse
 * the state given back by other visitors and use the state in this class just to give it to the
 * next/innermost visitor! Read operations have side effects, hence why using the most up to date
 * state is important.
 */
public class SMGCPAValueVisitor
    extends DefaultCExpressionVisitor<List<ValueAndSMGState>, CPATransferException>
    implements CRightHandSideVisitor<List<ValueAndSMGState>, CPATransferException> {

  /**
   * length of type LONG in Java (in bit). Needed to determine if a C type fits into a Java type.
   */
  private static final int SIZE_OF_JAVA_LONG = 64;

  // The evaluator translates C expressions into the SMG counterparts and vice versa.
  private final SMGCPAValueExpressionEvaluator evaluator;

  private final SMGState state;

  /** This edge is only to be used for debugging/logging! */
  private final CFAEdge cfaEdge;

  private final LogManagerWithoutDuplicates logger;

  public SMGCPAValueVisitor(
      SMGCPAValueExpressionEvaluator pEvaluator,
      SMGState currentState,
      CFAEdge edge,
      LogManagerWithoutDuplicates pLogger) {
    evaluator = pEvaluator;
    state = currentState;
    cfaEdge = edge;
    logger = pLogger;
  }

  @Override
  protected List<ValueAndSMGState> visitDefault(CExpression pExp) throws CPATransferException {
    // Just get a default value and log
    logger.logf(
        Level.INFO,
        "%s, Default value: CExpression %s could not be recognized and the default value %s was"
            + " used for its value. Related CFAEdge: %s",
        cfaEdge.getFileLocation(),
        pExp,
        SMGValue.zeroValue(),
        cfaEdge.getRawStatement());
    return ImmutableList.of(ValueAndSMGState.of(UnknownValue.getInstance(), state));
  }

  @Override
  public List<ValueAndSMGState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {

    return handleFunctions(pIastFunctionCallExpression);
  }

  @Override
  public List<ValueAndSMGState> visit(CArraySubscriptExpression e) throws CPATransferException {
    // Array subscript is default Java array usage. Example: array[5]
    // In C this can be translated to *(array + 5), but the array may be on the stack/heap (or
    // global, but be throw global and stack together when reading). Note: this is commutative!

    // The expression is split into array and subscript expression
    // Use the array expression in the visitor again to get the array address
    // The type of the arrayExpr may be pointer or array, depending on stack/heap
    CExpression arrayExpr = e.getArrayExpression();
    List<ValueAndSMGState> arrayValueAndStates = arrayExpr.accept(this);
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();
    for (ValueAndSMGState arrayValueAndState : arrayValueAndStates) {
      Value arrayValue = arrayValueAndState.getValue();

      // Evaluate the subscript as far as possible
      CExpression subscriptExpr = e.getSubscriptExpression();
      List<ValueAndSMGState> subscriptValueAndStates =
          subscriptExpr.accept(
              new SMGCPAValueVisitor(evaluator, arrayValueAndState.getState(), cfaEdge, logger));

      for (ValueAndSMGState subscriptValueAndState : subscriptValueAndStates) {
        Value subscriptValue = subscriptValueAndState.getValue();
        SMGState newState = subscriptValueAndState.getState();
        // If the subscript is a unknown value, we can't read anything and return unknown
        if (!subscriptValue.isNumericValue()) {
          // TODO: log this!
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(newState));
          continue;
        }
        // Calculate the offset out of the subscript value and the type
        BigInteger typeSizeInBits = evaluator.getBitSizeof(newState, e.getExpressionType());
        BigInteger subscriptOffset =
            typeSizeInBits.multiply(subscriptValue.asNumericValue().bigInteger());

        // Get the value from the array and return the value + state
        if (arrayExpr.getExpressionType() instanceof CPointerType) {
          // In the pointer case, the Value needs to be a AddressExpression
          Preconditions.checkArgument(arrayValue instanceof AddressExpression);
          AddressExpression addressValue = (AddressExpression) arrayValue;
          // The pointer might actually point inside of the array, take the offset of that into
          // account!
          Value arrayPointerOffsetExpr = addressValue.getOffset();
          if (!arrayPointerOffsetExpr.isNumericValue()) {
            // The offset is some non numeric Value and therefore not useable!
            // TODO: log
            resultBuilder.add(ValueAndSMGState.ofUnknownValue(newState));
            continue;
          }
          subscriptOffset =
              arrayPointerOffsetExpr.asNumericValue().bigInteger().add(subscriptOffset);

          resultBuilder.add(
              evaluator.readValueWithPointerDereference(
                  newState, addressValue.getMemoryAddress(), subscriptOffset, typeSizeInBits));
        } else {
          // Here our arrayValue holds the name of our variable
          Preconditions.checkArgument(arrayValue instanceof SymbolicValue);

          MemoryLocation maybeVariableIdent =
              ((SymbolicValue) arrayValue).getRepresentedLocation().orElseThrow();

          // This might actually point inside the array, add the offset
          if (maybeVariableIdent.isReference()) {
            // TODO: is it possible for this offset to be unknown?
            subscriptOffset =
                subscriptOffset.add(BigInteger.valueOf(maybeVariableIdent.getOffset()));
          }

          resultBuilder.add(
              evaluator.readStackOrGlobalVariable(
                  newState, maybeVariableIdent.getIdentifier(), subscriptOffset, typeSizeInBits));
        }
      }
    }
    return resultBuilder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CBinaryExpression e) throws CPATransferException {
    // binary expression, examples: +, -, *, /, ==, !=, < ....
    // visit left and right, then use the expression and return it. This also means we need to
    // create new SMG values (symbolic value ranges) for them, but don't save them in the SMG right
    // away (save, not write!) as this is only done when write is used.

    final BinaryOperator binaryOperator = e.getOperator();
    final CType calculationType = e.getCalculationType();
    final CExpression lVarInBinaryExp = e.getOperand1();
    final CExpression rVarInBinaryExp = e.getOperand2();
    ImmutableList.Builder<ValueAndSMGState> resultBuilder = ImmutableList.builder();

    for (ValueAndSMGState leftValueAndState : lVarInBinaryExp.accept(this)) {
      Value leftValue = leftValueAndState.getValue();
      // We can't work with unknowns
      if (leftValue.isUnknown()) {
        resultBuilder.add(ValueAndSMGState.ofUnknownValue(leftValueAndState.getState()));
        continue;
      }
      for (ValueAndSMGState rightValueAndState :
          rVarInBinaryExp.accept(
              new SMGCPAValueVisitor(evaluator, leftValueAndState.getState(), cfaEdge, logger))) {

        SMGState currentState = rightValueAndState.getState();

        Value rightValue = rightValueAndState.getValue();
        if (rightValue.isUnknown()) {
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }

        ValueAndSMGState castLeftValue = castCValue(leftValue, calculationType, currentState);
        leftValue = castLeftValue.getValue();
        currentState = castLeftValue.getState();
        if (binaryOperator != BinaryOperator.SHIFT_LEFT
            && binaryOperator != BinaryOperator.SHIFT_RIGHT) {
          /* For SHIFT-operations we do not cast the second operator.
           * We do not even need integer-promotion,
           * because the maximum SHIFT of 64 is lower than MAX_CHAR.
           *
           * ISO-C99 (6.5.7 #3): Bitwise shift operators
           * The integer promotions are performed on each of the operands.
           * The type of the result is that of the promoted left operand.
           * If the value of the right operand is negative or is greater than
           * or equal to the width of the promoted left operand,
           * the behavior is undefined.
           */
          ValueAndSMGState castRightValue = castCValue(rightValue, calculationType, currentState);
          rightValue = castRightValue.getValue();
          currentState = castRightValue.getState();
        }

        if (leftValue instanceof AddressExpression || rightValue instanceof AddressExpression) {
          resultBuilder.add(
              calculatePointerArithmetics(
                  leftValue,
                  rightValue,
                  binaryOperator,
                  e.getExpressionType(),
                  calculationType,
                  currentState));
          continue;
        }

        if (leftValue instanceof FunctionValue || rightValue instanceof FunctionValue) {
          resultBuilder.add(
              ValueAndSMGState.of(
                  calculateExpressionWithFunctionValue(binaryOperator, rightValue, leftValue),
                  currentState));
          continue;
        }

        if (leftValue instanceof SymbolicValue || rightValue instanceof SymbolicValue) {
          resultBuilder.add(
              ValueAndSMGState.of(
                  calculateSymbolicBinaryExpression(leftValue, rightValue, e), currentState));
          continue;
        }

        if (!leftValue.isNumericValue() || !rightValue.isNumericValue()) {
          logger.logf(
              Level.FINE,
              "Parameters to binary operation '%s %s %s' are no numeric values.",
              leftValue,
              binaryOperator,
              rightValue);
          resultBuilder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }

        if (isArithmeticOperation(binaryOperator)) {
          // Actual computations
          Value arithResult =
              arithmeticOperation(
                  (NumericValue) leftValue,
                  (NumericValue) rightValue,
                  binaryOperator,
                  calculationType);
          resultBuilder.add(castCValue(arithResult, e.getExpressionType(), currentState));

        } else if (isComparison(binaryOperator)) {
          // comparisons
          Value returnValue =
              booleanOperation(
                  (NumericValue) leftValue,
                  (NumericValue) rightValue,
                  binaryOperator,
                  calculationType);
          // we do not cast here, because 0 and 1 are small enough for every type.
          resultBuilder.add(ValueAndSMGState.of(returnValue, currentState));
          continue;
        } else {
          throw new AssertionError("Unhandled binary operator in the value visitor.");
        }
      }
    }
    return resultBuilder.build();
  }

  /**
   * TODO: this currently only casts values. What happens if i try smth like this:
   *
   * <p>int array[] = ...;
   *
   * <p>char * bla = (char *) array;
   */
  @Override
  public List<ValueAndSMGState> visit(CCastExpression e) throws CPATransferException {
    // Casts are not trivial within SMGs as there might be type reinterpretations used inside the
    // SMGs,
    // but this should be taken care of by the SMGCPAValueExpressionEvaluator and no longer be a
    // problem here!
    // Get the type and value from the nested expression (might be SMG) and cast the value
    // Also most of this code is taken from the value analysis CPA and modified
    CType targetType = e.getExpressionType();
    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    for (ValueAndSMGState valueAndState : e.getOperand().accept(this)) {
      SMGState currentState = valueAndState.getState();
      Value value = valueAndState.getValue();

      builder.add(castCValue(value, targetType, currentState));
    }
    return builder.build();
  }

  private ValueAndSMGState castCValue(Value value, CType targetType, SMGState currentState) {
    MachineModel machineModel = evaluator.getMachineModel();
    if (!value.isExplicitlyKnown()) {
      return ValueAndSMGState.of(
          castSymbolicValue(value, targetType, Optional.of(machineModel)), currentState);
    }

    // We only use numeric/symbolic/unknown values anyway and we can't cast unknowns
    if (!value.isNumericValue()) {
      logger.logf(
          Level.FINE, "Can not cast C value %s to %s", value.toString(), targetType.toString());
      return ValueAndSMGState.of(value, state);
    }
    NumericValue numericValue = (NumericValue) value;

    CType type = targetType.getCanonicalType();
    final int size;
    if (type instanceof CSimpleType) {
      size = evaluator.getBitSizeof(currentState, type).intValue();
    } else if (type instanceof CBitFieldType) {
      size = ((CBitFieldType) type).getBitFieldSize();
      type = ((CBitFieldType) type).getType();
    } else {
      return ValueAndSMGState.of(value, state);
    }

    return ValueAndSMGState.of(castNumeric(numericValue, type, machineModel, size), currentState);
  }

  @Override
  public List<ValueAndSMGState> visit(CFieldReference e) throws CPATransferException {
    // This is the field of a struct/union, so smth like struct.field or struct->field.
    // In the later case its a pointer dereference.
    // Read the value of the field from the object.

    // First we transform x->f into (*x).f per default
    CFieldReference explicitReference = e.withExplicitPointerDereference();

    // Owner expression; the struct/union with this field. Use this to get the address of the
    // general object.
    CExpression ownerExpression = explicitReference.getFieldOwner();
    CType fieldType =
        SMGCPAValueExpressionEvaluator.getCanonicalType(explicitReference.getExpressionType());
    // For (*pointer).field case or struct.field case the visitor returns the Value for the
    // correct SMGObject (if it exists)
    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    for (ValueAndSMGState valueAndState : ownerExpression.accept(this)) {
      SMGState currentState = valueAndState.getState();
      // This value is either a AddressValue for pointers i.e. (*struct).field or a general
      // SymbolicValue
      Value structValue = valueAndState.getValue();

      // Now get the offset of the current field
      BigInteger fieldOffset =
          evaluator.getFieldOffsetInBits(
              SMGCPAValueExpressionEvaluator.getCanonicalType(ownerExpression),
              explicitReference.getFieldName());

      // Get the size of the current field depending on its type; in the case of Strings however we
      // want to use Chars
      // if (fieldType instanceof CSimpleType) {
      // TODO: Strings
      // }

      BigInteger sizeOfField = evaluator.getBitSizeof(currentState, fieldType);

      // This is either a stack/global variable of the form struct.field or a pointer of the form
      // (*structP).field. The later needs a pointer deref
      if (ownerExpression instanceof CPointerExpression) {
        // In the pointer case, the Value needs to be a AddressExpression
        Preconditions.checkArgument(structValue instanceof AddressExpression);
        AddressExpression addressAndOffsetValue = (AddressExpression) structValue;
        // This AddressExpr theoretically can have a offset
        Value structPointerOffsetExpr = addressAndOffsetValue.getOffset();
        if (!structPointerOffsetExpr.isNumericValue()) {
          // The offset is some non numeric Value and therefore not usable!
          builder.add(ValueAndSMGState.ofUnknownValue(currentState));
          continue;
        }
        BigInteger finalFieldOffset =
            structPointerOffsetExpr.asNumericValue().bigInteger().add(fieldOffset);

        ValueAndSMGState readValueAndState =
            evaluator.readValueWithPointerDereference(
                currentState,
                addressAndOffsetValue.getMemoryAddress(),
                finalFieldOffset,
                sizeOfField);

        if (fieldType instanceof CPointerType) {
          readValueAndState =
              ValueAndSMGState.of(
                  AddressExpression.withZeroOffset(readValueAndState.getValue(), fieldType),
                  readValueAndState.getState());
        }

        builder.add(readValueAndState);

      } else if (ownerExpression instanceof CBinaryExpression
          || ownerExpression instanceof CIdExpression) {
        // In the non pointer case the Value is some SymbolicValue with the correct variable
        // identifier String inside its MemoryLocation
        Preconditions.checkArgument(structValue instanceof SymbolicValue);
        MemoryLocation maybeVariableIdent =
            ((SymbolicValue) structValue).getRepresentedLocation().orElseThrow();

        BigInteger finalFieldOffset = fieldOffset;
        if (maybeVariableIdent.isReference()) {
          finalFieldOffset = fieldOffset.add(BigInteger.valueOf(maybeVariableIdent.getOffset()));
        }

        ValueAndSMGState readValueAndState =
            evaluator.readStackOrGlobalVariable(
                currentState, maybeVariableIdent.getIdentifier(), finalFieldOffset, sizeOfField);

        if (fieldType instanceof CPointerType) {
          readValueAndState =
              ValueAndSMGState.of(
                  AddressExpression.withZeroOffset(readValueAndState.getValue(), fieldType),
                  readValueAndState.getState());
        }

        builder.add(readValueAndState);

      } else {
        throw new SMG2Exception(
            "Unknown field type in field expression when trying to retrieve a value.");
      }
    }
    return builder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CIdExpression e) throws CPATransferException {
    // essentially stack or global variables
    // Either CEnumerator, CVariableDeclaration, CParameterDeclaration
    // Could also be a type/function declaration, one if which is malloc().
    // We either read the stack/global variable for non pointer and non struct/unions, or package it
    // in a AddressExpression for pointers
    // or SymbolicValue with a memory location and the name of the variable inside of that.

    CSimpleDeclaration varDecl = e.getDeclaration();
    CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(e.getExpressionType());
    if (varDecl == null) {
      // The variable was not declared
      throw new SMG2Exception("Usage of undeclared variable: " + e.getName() + ".");
    }

    String variableName = varDecl.getQualifiedName();

    if (SMGCPAValueExpressionEvaluator.isStructOrUnionType(type) || type instanceof CArrayType) {
      // Struct/Unions/arrays on the stack/global; return the memory location in a symbolic value
      // but make the symbolic "Value" null. This is then used as interpretation such that the Value
      // of the memory location (on the stack) is used. This is used by assignments only as far as i
      // know, i.e. when assigning a complete array/struct to a new variable.
      return ImmutableList.of(
          ValueAndSMGState.of(
              new ConstantSymbolicExpression(
                  null, type, MemoryLocation.forIdentifier(variableName)),
              state));

    } else if (SMGCPAValueExpressionEvaluator.isAddressType(type)) {
      // Pointer/Array/Function types should return a Value that internally can be translated into a
      // SMGValue that leads to a SMGPointsToEdge that leads to the correct object (with potential
      // offsets inside of the points to edge). These have to be packaged into a AddressExpression
      // with an 0 offset. Modifications of the offset of the address can be done by subsequent
      // methods. (The check is fine because we already filtered out structs/unions)
      BigInteger sizeInBits = evaluator.getBitSizeof(state, e.getExpressionType());
      // Now use the qualified name to get the actual global/stack memory location
      ValueAndSMGState readValueAndState =
          evaluator.readStackOrGlobalVariable(
              state, varDecl.getQualifiedName(), BigInteger.ZERO, sizeInBits);

      Value addressValue = AddressExpression.withZeroOffset(readValueAndState.getValue(), type);

      return ImmutableList.of(ValueAndSMGState.of(addressValue, readValueAndState.getState()));

    } else {
      // Everything else should be readable and returnable directly; just return the Value
      BigInteger sizeInBits = evaluator.getBitSizeof(state, e.getExpressionType());
      // Now use the qualified name to get the actual global/stack memory location
      return ImmutableList.of(
          evaluator.readStackOrGlobalVariable(
              state, varDecl.getQualifiedName(), BigInteger.ZERO, sizeInBits));
    }
  }

  @Override
  public List<ValueAndSMGState> visit(CCharLiteralExpression e) throws CPATransferException {
    // Simple character expression; We use the numeric value
    int value = e.getCharacter();

    // We simply return the Value, as if a mapping to SMGValue is needed only after Value is written
    // into the memory, but when writing a mapping is created anyway
    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(value), state));
  }

  @Override
  public List<ValueAndSMGState> visit(CFloatLiteralExpression e) throws CPATransferException {
    // Floating point value expression
    BigDecimal value = e.getValue();

    // We simply return the Value, as if a mapping to SMGValue is needed only after Value is written
    // into the memory, but when writing a mapping is created anyway
    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(value), state));
  }

  @Override
  public List<ValueAndSMGState> visit(CIntegerLiteralExpression e) throws CPATransferException {
    // Simple int expression
    BigInteger value = e.getValue();

    // We simply return the Value, as if a mapping to SMGValue is needed only after Value is written
    // into the memory, but when writing a mapping is created anyway
    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(value), state));
  }

  @Override
  public List<ValueAndSMGState> visit(CStringLiteralExpression e) throws CPATransferException {
    // TODO: both the value and old smg analysis simply return unknown in this case
    // String string = e.getContentString();
    // ImmutableList.Builder<ValueAndSMGState> builder = ImmutableList.builder();
    return ImmutableList.of(ValueAndSMGState.ofUnknownValue(state));
  }

  @Override
  public List<ValueAndSMGState> visit(CTypeIdExpression e) throws CPATransferException {
    // Operators:
    // sizeOf, typeOf and
    // _Alignof or alignof = the number of bytes between successive addresses, careful because of
    // padding!

    final TypeIdOperator idOperator = e.getOperator();
    final CType innerType = e.getType();

    switch (idOperator) {
      case SIZEOF:
        BigInteger size = evaluator.getBitSizeof(state, innerType);
        return ImmutableList.of(ValueAndSMGState.of(new NumericValue(size), state));

      case ALIGNOF:
        BigInteger align = evaluator.getAlignOf(state, innerType);
        return ImmutableList.of(ValueAndSMGState.of(new NumericValue(align), state));

      case TYPEOF: // This can't really be solved here as we can only return Values

      default:
        return ImmutableList.of(ValueAndSMGState.of(UnknownValue.getInstance(), state));
    }
  }

  @Override
  public List<ValueAndSMGState> visit(CUnaryExpression e) throws CPATransferException {
    // Unary expression types like & (address of operator), sizeOf(), - (unary minus), TILDE
    // (bitwise not) and alignof

    UnaryOperator unaryOperator = e.getOperator();
    CExpression unaryOperand = e.getOperand();
    CType expressionType = e.getExpressionType();
    CType operandType = unaryOperand.getExpressionType();

    switch (unaryOperator) {
      case SIZEOF:
        BigInteger sizeInBits = evaluator.getBitSizeof(state, operandType);
        return ImmutableList.of(
            ValueAndSMGState.of(new NumericValue(sizeInBits.divide(BigInteger.valueOf(8))), state));

      case ALIGNOF:
        return ImmutableList.of(
            ValueAndSMGState.of(
                new NumericValue(
                    evaluator.getMachineModel().getAlignof(unaryOperand.getExpressionType())),
                state));

      default:
        break;
    }

    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    loop:
    for (ValueAndSMGState valueAndState : unaryOperand.accept(this)) {
      SMGState currentState = valueAndState.getState();
      Value value = valueAndState.getValue();

      if (value.isUnknown()) {
        builder.add(ValueAndSMGState.ofUnknownValue(currentState));
        continue;
      }

      // & operator is seperate as the value returned leads to the underlying memory location
      if (unaryOperator == UnaryOperator.AMPER) {
        // Check if a pointer already exits, if not create a pointer (points-to-edge), map it to a
        // unique value and return it.
        return evaluator.createAddress(unaryOperand, currentState, cfaEdge);
      }

      if (value instanceof SymbolicValue) {
        builder.add(
            ValueAndSMGState.of(
                createSymbolicExpression(value, operandType, unaryOperator, expressionType),
                currentState));
        continue;
      } else if (!value.isNumericValue()) {
        logger.logf(Level.FINE, "Invalid argument %s for unary operator %s.", value, unaryOperator);
        builder.add(ValueAndSMGState.ofUnknownValue(currentState));
        continue;
      }

      final NumericValue numericValue = (NumericValue) value;
      switch (unaryOperator) {
        case MINUS:
          builder.add(ValueAndSMGState.of(numericValue.negate(), currentState));
          continue loop;

        case TILDE:
          builder.add(
              ValueAndSMGState.of(new NumericValue(~numericValue.longValue()), currentState));
          continue loop;

        default:
          throw new AssertionError("Unknown unary operator: " + unaryOperator);
      }
    }
    return builder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CPointerExpression e) throws CPATransferException {
    // This should subevaluate to a AddressExpression in the visit call in the beginning as we
    // always evaluate to the address, but only
    // dereference and read it if its not a struct/union as those will be dereferenced by the field
    // expression

    // Get the type of the target
    CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(e.getExpressionType());
    // Get the expression that is dereferenced
    CExpression expr = e.getOperand();
    // Evaluate the expression to a Value; this should return a Symbolic Value with the address of
    // the target and a offset. If this fails this returns a UnknownValue.
    ImmutableList.Builder<ValueAndSMGState> builder = new ImmutableList.Builder<>();
    for (ValueAndSMGState valueAndState : expr.accept(this)) {
      SMGState currentState = valueAndState.getState();
      // Try to disassemble the values (AddressExpression)
      Value value = valueAndState.getValue();
      Preconditions.checkArgument(value instanceof AddressExpression);
      AddressExpression pointerValue = (AddressExpression) value;

      // The offset part of the pointer; its either numeric or we can't get a concrete value
      Value offset = pointerValue.getOffset();
      if (!offset.isNumericValue()) {
        // If the offset is not numericly known we can't read a value, return unknown
        builder.add(ValueAndSMGState.ofUnknownValue(currentState));
        continue;
      }

      if (SMGCPAValueExpressionEvaluator.isStructOrUnionType(type)) {
        // We don't want to read struct/union! In those cases we return the AddressExpression
        // such that the following visitor methods can dereference the fields correctly
        builder.add(ValueAndSMGState.of(value, currentState));

      } else if (type instanceof CArrayType) {
        // For arrays we want to actually read the values at the addresses

        // TODO: if String type, use char and eval each on its own for the size of the String
        // For Strings the offset changes obviously
        BigInteger sizeInBits = evaluator.getBitSizeof(currentState, type);
        BigInteger offsetInBits = offset.asNumericValue().bigInteger();

        // Dereference the Value and return it. The read checks for validity etc.
        ValueAndSMGState readArray =
            evaluator.readValueWithPointerDereference(
                currentState, pointerValue.getMemoryAddress(), offsetInBits, sizeInBits);
        currentState = readArray.getState();
        builder.add(readArray);

      } else if (type instanceof CPointerType) {
        // Default case either *pointer or *(pointer + smth), but both get transformed into a
        // AddressExpression Value type with the correct offset build in
        // Just dereference the pointer with the correct type
        BigInteger sizeInBits = evaluator.getBitSizeof(currentState, type);
        BigInteger offsetInBits = offset.asNumericValue().bigInteger();

        // Dereference the Value and return it. The read checks for validity etc.
        ValueAndSMGState readValueAndState =
            evaluator.readValueWithPointerDereference(
                currentState, pointerValue.getMemoryAddress(), offsetInBits, sizeInBits);
        currentState = readValueAndState.getState();
        // In the pointer case we would need to encapsulate it again
        builder.add(
            ValueAndSMGState.of(
                AddressExpression.withZeroOffset(readValueAndState.getValue(), type),
                currentState));

      } else if ((type instanceof CFunctionType
          && expr instanceof CUnaryExpression
          && ((CUnaryExpression) expr).getOperator() == CUnaryExpression.UnaryOperator.AMPER)) {
        // Special cases
        // TODO:
      } else {
        // "Normal" return types
        // Default case either *pointer or *(pointer + smth), but both get transformed into a
        // AddressExpression Value type with the correct offset build in, so
        // just dereference the pointer with the correct type
        BigInteger sizeInBits = evaluator.getBitSizeof(currentState, type);
        BigInteger offsetInBits = offset.asNumericValue().bigInteger();

        // Dereference the Value and return it. The read checks for validity etc.
        ValueAndSMGState readValue =
            evaluator.readValueWithPointerDereference(
                currentState, pointerValue.getMemoryAddress(), offsetInBits, sizeInBits);
        currentState = readValue.getState();
        builder.add(readValue);
      }
    }
    return builder.build();
  }

  @Override
  public List<ValueAndSMGState> visit(CAddressOfLabelExpression e) throws CPATransferException {
    // && expression
    // This is not in the C standard, just gcc
    // https://gcc.gnu.org/onlinedocs/gcc/Labels-as-Values.html
    // Returns a address to a function
    // TODO:

    return visitDefault(e);
  }

  @Override
  public List<ValueAndSMGState> visit(CImaginaryLiteralExpression e) throws CPATransferException {
    // TODO: do we even need those?
    // Imaginary part for complex numbers
    return visitDefault(e);
  }

  @Override
  public List<ValueAndSMGState> visit(CComplexCastExpression e) throws CPATransferException {
    // TODO: do we need those?
    // Cast for complex numbers?
    return visitDefault(e);
  }

  // Copied from value CPA
  private Value createSymbolicExpression(
      Value pValue, CType pOperandType, UnaryOperator pUnaryOperator, CType pExpressionType) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression operand = factory.asConstant(pValue, pOperandType);

    switch (pUnaryOperator) {
      case MINUS:
        return factory.negate(operand, pExpressionType);
      case TILDE:
        return factory.binaryNot(operand, pExpressionType);
      default:
        throw new AssertionError("Unhandled unary operator " + pUnaryOperator);
    }
  }

  // ++++++++++++++++++++ Below this point casting helper methods

  /** Taken from the value analysis CPA and modified. Casts symbolic {@link Value}s. */
  private Value castSymbolicValue(
      Value pValue, Type pTargetType, Optional<MachineModel> pMachineModel) {
    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

    if (pValue instanceof SymbolicValue && pTargetType instanceof CSimpleType) {
      return factory.cast((SymbolicValue) pValue, pTargetType, pMachineModel);
    }

    // If the value is not symbolic, just return it.
    return pValue;
  }

  /** Taken from the value analysis CPA. TODO: check that all casts are correct and add missing. */
  private Value castNumeric(
      @NonNull final NumericValue numericValue,
      final CType type,
      final MachineModel machineModel,
      final int size) {

    if (!(type instanceof CSimpleType)) {
      return numericValue;
    }

    final CSimpleType st = (CSimpleType) type;

    switch (st.getType()) {
      case BOOL:
        return convertToBool(numericValue);
      case INT128:
      case INT:
      case CHAR:
        {
          if (isNan(numericValue)) {
            // result of conversion of NaN to integer is undefined
            return UnknownValue.getInstance();

          } else if ((numericValue.getNumber() instanceof Float
                  || numericValue.getNumber() instanceof Double)
              && Math.abs(numericValue.doubleValue() - numericValue.longValue()) >= 1) {
            // if number is a float and float can not be exactly represented as integer, the
            // result of the conversion of float to integer is undefined
            return UnknownValue.getInstance();
          }

          final BigInteger valueToCastAsInt;
          if (numericValue.getNumber() instanceof BigInteger) {
            valueToCastAsInt = numericValue.bigInteger();
          } else if (numericValue.getNumber() instanceof BigDecimal) {
            valueToCastAsInt = numericValue.bigDecimalValue().toBigInteger();
          } else {
            valueToCastAsInt = BigInteger.valueOf(numericValue.longValue());
          }
          final boolean targetIsSigned = machineModel.isSigned(st);

          final BigInteger maxValue = BigInteger.ONE.shiftLeft(size); // 2^size
          BigInteger result = valueToCastAsInt.remainder(maxValue); // shrink to number of bits

          BigInteger signedUpperBound;
          BigInteger signedLowerBound;
          if (targetIsSigned) {
            // signed value must be put in interval [-(maxValue/2), (maxValue/2)-1]
            // upper bound maxValue / 2 - 1
            signedUpperBound = maxValue.divide(BigInteger.valueOf(2)).subtract(BigInteger.ONE);
            // lower bound -maxValue / 2
            signedLowerBound = maxValue.divide(BigInteger.valueOf(2)).negate();
          } else {
            signedUpperBound = maxValue.subtract(BigInteger.ONE);
            signedLowerBound = BigInteger.ZERO;
          }

          if (isGreaterThan(result, signedUpperBound)) {
            // if result overflows, let it 'roll around' and add overflow to lower bound
            result = result.subtract(maxValue);
          } else if (isLessThan(result, signedLowerBound)) {
            result = result.add(maxValue);
          }

          return new NumericValue(result);
        }

      case FLOAT:
      case DOUBLE:
      case FLOAT128:
        {
          // TODO: look more closely at the INT/CHAR cases, especially at the loggedEdges stuff
          // TODO: check for overflow(source larger than the highest number we can store in target
          // etc.)

          // casting to DOUBLE, if value is INT or FLOAT. This is sound, if we would also do this
          // cast in C.
          Value result;

          final int bitPerByte = machineModel.getSizeofCharInBits();

          if (isNan(numericValue) || isInfinity(numericValue)) {
            result = numericValue;
          } else if (size == machineModel.getSizeofFloat128() * 8) {
            result = new NumericValue(numericValue.bigDecimalValue());
          } else if (size == machineModel.getSizeofLongDouble() * bitPerByte) {

            if (numericValue.bigDecimalValue().doubleValue() == numericValue.doubleValue()) {
              result = new NumericValue(numericValue.doubleValue());
            } else if (numericValue.bigDecimalValue().floatValue() == numericValue.floatValue()) {
              result = new NumericValue(numericValue.floatValue());
            } else {
              result = UnknownValue.getInstance();
            }
          } else {
            // TODO: Think of floating point types!
            throw new AssertionError("Unhandled floating point type: " + type);
          }
          return result;
        }

      default:
        throw new AssertionError("Unhandled type: " + type);
    }
  }

  /** Taken from the value analysis CPA. */
  private Value convertToBool(final NumericValue pValue) {
    Number n = pValue.getNumber();
    if (isBooleanFalseRepresentation(n)) {
      return new NumericValue(0);
    } else {
      return new NumericValue(1);
    }
  }

  /** Taken from the value analysis CPA. */
  private boolean isBooleanFalseRepresentation(final Number n) {
    return ((n instanceof Float || n instanceof Double) && 0 == n.doubleValue())
        || (n instanceof BigInteger && BigInteger.ZERO.equals(n))
        || (n instanceof BigDecimal && ((BigDecimal) n).compareTo(BigDecimal.ZERO) == 0)
        || 0 == n.longValue();
  }

  /** Taken from the value analysis CPA. */
  private boolean isNan(NumericValue pValue) {
    Number n = pValue.getNumber();
    return n.equals(Float.NaN) || n.equals(Double.NaN) || NegativeNaN.VALUE.equals(n);
  }

  /** Taken from the value analysis CPA. */
  private boolean isInfinity(NumericValue pValue) {
    Number n = pValue.getNumber();
    return n.equals(Double.POSITIVE_INFINITY)
        || n.equals(Double.NEGATIVE_INFINITY)
        || n.equals(Float.POSITIVE_INFINITY)
        || n.equals(Float.NEGATIVE_INFINITY);
  }

  /**
   * Returns whether first integer is greater than second integer. Taken from the value analysis CPA
   */
  private boolean isGreaterThan(BigInteger i1, BigInteger i2) {
    return i1.compareTo(i2) > 0;
  }

  /**
   * Returns whether first integer is less than second integer. Taken from the value analysis CPA
   */
  private boolean isLessThan(BigInteger i1, BigInteger i2) {
    return i1.compareTo(i2) < 0;
  }

  // +++++++++++++++++++ Below this point methods for handling functions

  private boolean isUnspecifiedType(CType pType) {
    return pType instanceof CSimpleType
        && ((CSimpleType) pType).getType() == CBasicType.UNSPECIFIED;
  }

  private Value fmin(Number pOp1, Number pOp2) {
    if (Double.isNaN(pOp1.doubleValue())
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() > 0)
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() < 0)) {
      return new NumericValue(pOp2);
    }
    if (Double.isNaN(pOp2.doubleValue())
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() > 0)
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() < 0)) {
      return new NumericValue(pOp1);
    }

    final BigDecimal op1bd;
    final BigDecimal op2bd;

    if (pOp1 instanceof BigDecimal) {
      op1bd = (BigDecimal) pOp1;
    } else {
      op1bd = BigDecimal.valueOf(pOp1.doubleValue());
    }
    if (pOp2 instanceof BigDecimal) {
      op2bd = (BigDecimal) pOp2;
    } else {
      op2bd = BigDecimal.valueOf(pOp2.doubleValue());
    }

    if (op1bd.compareTo(op2bd) < 0) {
      return new NumericValue(op1bd);
    }
    return new NumericValue(op2bd);
  }

  private Value fdim(Number pOp1, Number pOp2, String pFunctionName) {
    if (Double.isNaN(pOp1.doubleValue()) || Double.isNaN(pOp2.doubleValue())) {
      return new NumericValue(Double.NaN);
    }

    if (Double.isInfinite(pOp1.doubleValue())) {
      if (Double.isInfinite(pOp2.doubleValue())) {
        if (pOp1.doubleValue() > pOp2.doubleValue()) {
          return new NumericValue(pOp1.doubleValue() - pOp2.doubleValue());
        }
        return new NumericValue(0.0);
      }
      if (pOp1.doubleValue() < 0) {
        return new NumericValue(0.0);
      }
      return new NumericValue(pOp1);
    }
    if (Double.isInfinite(pOp2.doubleValue())) {
      if (pOp2.doubleValue() < 0) {
        return new NumericValue(Double.NaN);
      }
      return new NumericValue(0.0);
    }

    final BigDecimal op1bd;
    final BigDecimal op2bd;

    if (pOp1 instanceof BigDecimal) {
      op1bd = (BigDecimal) pOp1;
    } else {
      op1bd = BigDecimal.valueOf(pOp1.doubleValue());
    }
    if (pOp2 instanceof BigDecimal) {
      op2bd = (BigDecimal) pOp2;
    } else {
      op2bd = BigDecimal.valueOf(pOp2.doubleValue());
    }
    if (op1bd.compareTo(op2bd) > 0) {
      BigDecimal difference = op1bd.subtract(op2bd);

      CSimpleType type = BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(pFunctionName);
      BigDecimal maxValue;
      switch (type.getType()) {
        case FLOAT:
          maxValue = BigDecimal.valueOf(Float.MAX_VALUE);
          break;
        case DOUBLE:
          maxValue = BigDecimal.valueOf(Double.MAX_VALUE);
          break;
        default:
          return Value.UnknownValue.getInstance();
      }
      if (difference.compareTo(maxValue) > 0) {
        return new NumericValue(Double.POSITIVE_INFINITY);
      }
      return new NumericValue(difference);
    }
    return new NumericValue(0.0);
  }

  private Optional<Boolean> isNegative(Number pNumber) {
    if (pNumber instanceof BigDecimal) {
      return Optional.of(((BigDecimal) pNumber).signum() < 0);
    } else if (pNumber instanceof Float) {
      float number = pNumber.floatValue();
      if (Float.isNaN(number)) {
        return Optional.of(false);
      }
      return Optional.of(number < 0 || 1 / number < 0);
    } else if (pNumber instanceof Double) {
      double number = pNumber.doubleValue();
      if (Double.isNaN(number)) {
        return Optional.of(false);
      }
      return Optional.of(number < 0 || 1 / number < 0);
    } else if (pNumber instanceof NegativeNaN) {
      return Optional.of(true);
    }
    return Optional.empty();
  }

  private Value fmax(Number pOp1, Number pOp2) {
    if (Double.isNaN(pOp1.doubleValue())
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() < 0)
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() > 0)) {
      return new NumericValue(pOp2);
    }
    if (Double.isNaN(pOp2.doubleValue())
        || (Double.isInfinite(pOp2.doubleValue()) && pOp2.doubleValue() < 0)
        || (Double.isInfinite(pOp1.doubleValue()) && pOp1.doubleValue() > 0)) {
      return new NumericValue(pOp1);
    }

    final BigDecimal op1bd;
    final BigDecimal op2bd;

    if (pOp1 instanceof BigDecimal) {
      op1bd = (BigDecimal) pOp1;
    } else {
      op1bd = BigDecimal.valueOf(pOp1.doubleValue());
    }
    if (pOp2 instanceof BigDecimal) {
      op2bd = (BigDecimal) pOp2;
    } else {
      op2bd = BigDecimal.valueOf(pOp2.doubleValue());
    }

    if (op1bd.compareTo(op2bd) > 0) {
      return new NumericValue(op1bd);
    }
    return new NumericValue(op2bd);
  }

  /*
   * Handles ALL function calls
   */
  private List<ValueAndSMGState> handleFunctions(
      CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
    CExpression functionNameExp = pIastFunctionCallExpression.getFunctionNameExpression();

    // We only handle builtin functions
    if (functionNameExp instanceof CIdExpression) {
      String calledFunctionName = ((CIdExpression) functionNameExp).getName();

      if (BuiltinFunctions.isBuiltinFunction(calledFunctionName)) {

        CType functionType = BuiltinFunctions.getFunctionType(calledFunctionName);

        if (isUnspecifiedType(functionType)) {
          // unsupported formula
          return ImmutableList.of(ValueAndSMGState.ofUnknownValue(state));
        }

        List<CExpression> parameterExpressions =
            pIastFunctionCallExpression.getParameterExpressions();
        ImmutableList.Builder<Value> parameterValuesBuilder = ImmutableList.builder();

        // Evaluate all parameters
        SMGState currentState = state;
        for (CExpression currParamExp : parameterExpressions) {
          // Here we expect only 1 result value
          List<ValueAndSMGState> newValuesAndStates =
              currParamExp.accept(new SMGCPAValueVisitor(evaluator, currentState, cfaEdge, logger));
          // TODO: this holds for everything except Strings; what to do with Strings?
          Preconditions.checkArgument(newValuesAndStates.size() == 1);
          Value newValue = newValuesAndStates.get(0).getValue();
          // CPA access has side effects! Always take the newest state!
          currentState = newValuesAndStates.get(0).getState();

          parameterValuesBuilder.add(newValue);
        }
        List<Value> parameterValues = parameterValuesBuilder.build();

        if (BuiltinOverflowFunctions.isBuiltinOverflowFunction(calledFunctionName)) {
          /*
           * Problem: this method needs a AbstractExpressionValueVisitor as input (the this)
           * but this class is not correctly abstracted such that i can inherit it here
           * (because it essentially is the same except for 1 method that would need to be
           * abstract)
           *
           * return BuiltinOverflowFunctions.evaluateFunctionCall(
           *   pIastFunctionCallExpression, this, evaluator.getMachineModel(), logger);
           */
          return ImmutableList.of(ValueAndSMGState.of(UnknownValue.getInstance(), currentState));
        } else if (BuiltinFloatFunctions.matchesAbsolute(calledFunctionName)) {
          assert parameterValues.size() == 1;

          final CType parameterType = parameterExpressions.get(0).getExpressionType();
          final Value parameter = parameterValues.get(0);

          if (parameterType instanceof CSimpleType && !((CSimpleType) parameterType).isSigned()) {
            return ImmutableList.of(ValueAndSMGState.of(parameter, currentState));

          } else if (parameter.isExplicitlyKnown()) {
            assert parameter.isNumericValue();
            final double absoluteValue = Math.abs(((NumericValue) parameter).doubleValue());

            // absolute value for INT_MIN is undefined behaviour, so we do not bother handling it
            // in any specific way
            return ImmutableList.of(
                ValueAndSMGState.of(new NumericValue(absoluteValue), currentState));
          }

        } else if (BuiltinFloatFunctions.matchesHugeVal(calledFunctionName)
            || BuiltinFloatFunctions.matchesInfinity(calledFunctionName)) {

          assert parameterValues.isEmpty();
          if (BuiltinFloatFunctions.matchesHugeValFloat(calledFunctionName)
              || BuiltinFloatFunctions.matchesInfinityFloat(calledFunctionName)) {

            return ImmutableList.of(
                ValueAndSMGState.of(new NumericValue(Float.POSITIVE_INFINITY), currentState));

          } else {
            assert BuiltinFloatFunctions.matchesInfinityDouble(calledFunctionName)
                    || BuiltinFloatFunctions.matchesInfinityLongDouble(calledFunctionName)
                    || BuiltinFloatFunctions.matchesHugeValDouble(calledFunctionName)
                    || BuiltinFloatFunctions.matchesHugeValLongDouble(calledFunctionName)
                : " Unhandled builtin function for infinity: " + calledFunctionName;

            return ImmutableList.of(
                ValueAndSMGState.of(new NumericValue(Double.POSITIVE_INFINITY), currentState));
          }

        } else if (BuiltinFloatFunctions.matchesNaN(calledFunctionName)) {
          assert parameterValues.isEmpty() || parameterValues.size() == 1;

          if (BuiltinFloatFunctions.matchesNaNFloat(calledFunctionName)) {
            return ImmutableList.of(ValueAndSMGState.of(new NumericValue(Float.NaN), currentState));
          } else {
            assert BuiltinFloatFunctions.matchesNaNDouble(calledFunctionName)
                    || BuiltinFloatFunctions.matchesNaNLongDouble(calledFunctionName)
                : "Unhandled builtin function for NaN: " + calledFunctionName;

            return ImmutableList.of(
                ValueAndSMGState.of(new NumericValue(Double.NaN), currentState));
          }
        } else if (BuiltinFloatFunctions.matchesIsNaN(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName);
              switch (paramType.getType()) {
                case FLOAT:
                  return Float.isNaN(numericValue.floatValue())
                      ? ImmutableList.of(ValueAndSMGState.of(new NumericValue(1), currentState))
                      : ImmutableList.of(ValueAndSMGState.of(new NumericValue(0), currentState));
                case DOUBLE:
                  return Double.isNaN(numericValue.doubleValue())
                      ? ImmutableList.of(ValueAndSMGState.of(new NumericValue(1), currentState))
                      : ImmutableList.of(ValueAndSMGState.of(new NumericValue(0), currentState));
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesIsInfinity(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName);
              switch (paramType.getType()) {
                case FLOAT:
                  return Float.isInfinite(numericValue.floatValue())
                      ? ImmutableList.of(ValueAndSMGState.of(new NumericValue(1), currentState))
                      : ImmutableList.of(ValueAndSMGState.of(new NumericValue(0), currentState));
                case DOUBLE:
                  return Double.isInfinite(numericValue.doubleValue())
                      ? ImmutableList.of(ValueAndSMGState.of(new NumericValue(1), currentState))
                      : ImmutableList.of(ValueAndSMGState.of(new NumericValue(0), currentState));
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFinite(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName);
              switch (paramType.getType()) {
                case FLOAT:
                  return Float.isInfinite(numericValue.floatValue())
                      ? ImmutableList.of(ValueAndSMGState.of(new NumericValue(0), currentState))
                      : ImmutableList.of(ValueAndSMGState.of(new NumericValue(1), currentState));
                case DOUBLE:
                  return Double.isInfinite(numericValue.doubleValue())
                      ? ImmutableList.of(ValueAndSMGState.of(new NumericValue(0), currentState))
                      : ImmutableList.of(ValueAndSMGState.of(new NumericValue(1), currentState));
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFloor(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(((BigDecimal) number).setScale(0, RoundingMode.FLOOR)),
                        currentState));
              } else if (number instanceof Float) {
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(Math.floor(number.floatValue())), currentState));
              } else if (number instanceof Double) {
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(Math.floor(number.doubleValue())), currentState));
              } else if (number instanceof NumericValue.NegativeNaN) {
                return ImmutableList.of(ValueAndSMGState.of(parameter, currentState));
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesCeil(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(((BigDecimal) number).setScale(0, RoundingMode.CEILING)),
                        currentState));
              } else if (number instanceof Float) {
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(Math.ceil(number.floatValue())), currentState));
              } else if (number instanceof Double) {
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(Math.ceil(number.doubleValue())), currentState));
              } else if (number instanceof NumericValue.NegativeNaN) {
                return ImmutableList.of(ValueAndSMGState.of(parameter, currentState));
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesRound(calledFunctionName)
            || BuiltinFloatFunctions.matchesLround(calledFunctionName)
            || BuiltinFloatFunctions.matchesLlround(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);
            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(((BigDecimal) number).setScale(0, RoundingMode.HALF_UP)),
                        currentState));
              } else if (number instanceof Float) {
                float f = number.floatValue();
                if (0 == f || Float.isInfinite(f)) {
                  return ImmutableList.of(ValueAndSMGState.of(parameter, currentState));
                }
                return ImmutableList.of(
                    ValueAndSMGState.of(new NumericValue(Math.round(f)), currentState));
              } else if (number instanceof Double) {
                double d = number.doubleValue();
                if (0 == d || Double.isInfinite(d)) {
                  return ImmutableList.of(ValueAndSMGState.of(parameter, currentState));
                }
                return ImmutableList.of(
                    ValueAndSMGState.of(new NumericValue(Math.round(d)), currentState));
              } else if (number instanceof NumericValue.NegativeNaN) {
                return ImmutableList.of(ValueAndSMGState.of(parameter, currentState));
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesTrunc(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);
            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              if (number instanceof BigDecimal) {
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(((BigDecimal) number).setScale(0, RoundingMode.DOWN)),
                        currentState));
              } else if (number instanceof Float) {
                float f = number.floatValue();
                if (0 == f || Float.isInfinite(f) || Float.isNaN(f)) {
                  // +/-0.0 and +/-INF and +/-NaN are returned unchanged
                  return ImmutableList.of(ValueAndSMGState.of(parameter, currentState));
                }
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(
                            BigDecimal.valueOf(number.floatValue())
                                .setScale(0, RoundingMode.DOWN)
                                .floatValue()),
                        currentState));
              } else if (number instanceof Double) {
                double d = number.doubleValue();
                if (0 == d || Double.isInfinite(d) || Double.isNaN(d)) {
                  // +/-0.0 and +/-INF and +/-NaN are returned unchanged
                  return ImmutableList.of(ValueAndSMGState.of(parameter, currentState));
                }
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(
                            BigDecimal.valueOf(number.doubleValue())
                                .setScale(0, RoundingMode.DOWN)
                                .doubleValue()),
                        currentState));
              } else if (number instanceof NumericValue.NegativeNaN) {
                return ImmutableList.of(ValueAndSMGState.of(parameter, currentState));
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFdim(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value operand1 = parameterValues.get(0);
            Value operand2 = parameterValues.get(1);
            if (operand1.isExplicitlyKnown() && operand2.isExplicitlyKnown()) {

              assert operand1.isNumericValue();
              assert operand2.isNumericValue();

              Number op1 = operand1.asNumericValue().getNumber();
              Number op2 = operand2.asNumericValue().getNumber();

              Value result = fdim(op1, op2, calledFunctionName);
              if (!Value.UnknownValue.getInstance().equals(result)) {
                return ImmutableList.of(ValueAndSMGState.of(result, currentState));
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmax(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value operand1 = parameterValues.get(0);
            Value operand2 = parameterValues.get(1);
            if (operand1.isExplicitlyKnown() && operand2.isExplicitlyKnown()) {

              assert operand1.isNumericValue();
              assert operand2.isNumericValue();

              Number op1 = operand1.asNumericValue().getNumber();
              Number op2 = operand2.asNumericValue().getNumber();

              return ImmutableList.of(ValueAndSMGState.of(fmax(op1, op2), currentState));
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmin(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value operand1 = parameterValues.get(0);
            Value operand2 = parameterValues.get(1);
            if (operand1.isExplicitlyKnown() && operand2.isExplicitlyKnown()) {

              assert operand1.isNumericValue();
              assert operand2.isNumericValue();

              Number op1 = operand1.asNumericValue().getNumber();
              Number op2 = operand2.asNumericValue().getNumber();

              return ImmutableList.of(ValueAndSMGState.of(fmin(op1, op2), currentState));
            }
          }
        } else if (BuiltinFloatFunctions.matchesSignbit(calledFunctionName)) {
          if (parameterValues.size() == 1) {
            Value parameter = parameterValues.get(0);

            if (parameter.isExplicitlyKnown()) {
              assert parameter.isNumericValue();
              Number number = parameter.asNumericValue().getNumber();
              Optional<Boolean> isNegative = isNegative(number);
              if (isNegative.isPresent()) {
                return ImmutableList.of(
                    ValueAndSMGState.of(
                        new NumericValue(isNegative.orElseThrow() ? 1 : 0), currentState));
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesCopysign(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value target = parameterValues.get(0);
            Value source = parameterValues.get(1);
            if (target.isExplicitlyKnown() && source.isExplicitlyKnown()) {
              assert target.isNumericValue();
              assert source.isNumericValue();
              Number targetNumber = target.asNumericValue().getNumber();
              Number sourceNumber = source.asNumericValue().getNumber();
              Optional<Boolean> sourceNegative = isNegative(sourceNumber);
              Optional<Boolean> targetNegative = isNegative(targetNumber);
              if (sourceNegative.isPresent() && targetNegative.isPresent()) {
                if (sourceNegative.orElseThrow().equals(targetNegative.orElseThrow())) {
                  return ImmutableList.of(
                      ValueAndSMGState.of(new NumericValue(targetNumber), currentState));
                }
                return ImmutableList.of(
                    ValueAndSMGState.of(target.asNumericValue().negate(), currentState));
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFloatClassify(calledFunctionName)) {

          if (parameterValues.size() == 1) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName);
              switch (paramType.getType()) {
                case FLOAT:
                  {
                    float v = numericValue.floatValue();
                    if (Float.isNaN(v)) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(0), currentState));
                    }
                    if (Float.isInfinite(v)) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(1), currentState));
                    }
                    if (v == 0.0) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(2), currentState));
                    }
                    if (Float.toHexString(v).startsWith("0x0.")) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(3), currentState));
                    }
                    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(4), currentState));
                  }
                case DOUBLE:
                  {
                    double v = numericValue.doubleValue();
                    if (Double.isNaN(v)) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(0), currentState));
                    }
                    if (Double.isInfinite(v)) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(1), currentState));
                    }
                    if (v == 0.0) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(2), currentState));
                    }
                    if (Double.toHexString(v).startsWith("0x0.")) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(3), currentState));
                    }
                    return ImmutableList.of(ValueAndSMGState.of(new NumericValue(4), currentState));
                  }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesModf(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value value = parameterValues.get(0);
            if (value.isExplicitlyKnown()) {
              NumericValue numericValue = value.asNumericValue();
              CSimpleType paramType =
                  BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName);
              switch (paramType.getType()) {
                case FLOAT:
                  {
                    long integralPart = (long) numericValue.floatValue();
                    float fractionalPart = numericValue.floatValue() - integralPart;
                    return ImmutableList.of(
                        ValueAndSMGState.of(new NumericValue(fractionalPart), currentState));
                  }
                case DOUBLE:
                  {
                    long integralPart = (long) numericValue.doubleValue();
                    double fractionalPart = numericValue.doubleValue() - integralPart;
                    return ImmutableList.of(
                        ValueAndSMGState.of(new NumericValue(fractionalPart), currentState));
                  }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFremainder(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value numer = parameterValues.get(0);
            Value denom = parameterValues.get(1);
            if (numer.isExplicitlyKnown() && denom.isExplicitlyKnown()) {
              NumericValue numerValue = numer.asNumericValue();
              NumericValue denomValue = denom.asNumericValue();
              switch (BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName)
                  .getType()) {
                case FLOAT:
                  {
                    float num = numerValue.floatValue();
                    float den = denomValue.floatValue();
                    if (Float.isNaN(num) || Float.isNaN(den) || Float.isInfinite(num) || den == 0) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(Float.NaN), currentState));
                    }
                    return ImmutableList.of(
                        ValueAndSMGState.of(
                            new NumericValue((float) Math.IEEEremainder(num, den)), currentState));
                  }
                case DOUBLE:
                  {
                    double num = numerValue.doubleValue();
                    double den = denomValue.doubleValue();
                    if (Double.isNaN(num)
                        || Double.isNaN(den)
                        || Double.isInfinite(num)
                        || den == 0) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(Double.NaN), currentState));
                    }
                    return ImmutableList.of(
                        ValueAndSMGState.of(
                            new NumericValue(Math.IEEEremainder(num, den)), currentState));
                  }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesFmod(calledFunctionName)) {
          if (parameterValues.size() == 2) {
            Value numer = parameterValues.get(0);
            Value denom = parameterValues.get(1);
            if (numer.isExplicitlyKnown() && denom.isExplicitlyKnown()) {
              NumericValue numerValue = numer.asNumericValue();
              NumericValue denomValue = denom.asNumericValue();
              switch (BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction(calledFunctionName)
                  .getType()) {
                case FLOAT:
                  {
                    float num = numerValue.floatValue();
                    float den = denomValue.floatValue();
                    if (Float.isNaN(num) || Float.isNaN(den) || Float.isInfinite(num) || den == 0) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(Float.NaN), currentState));
                    }
                    if (num == 0 && den != 0) {
                      // keep the sign on +0 and -0
                      return ImmutableList.of(ValueAndSMGState.of(numer, currentState));
                    }
                    // TODO computations on float/double are imprecise! Use epsilon environment?
                    return ImmutableList.of(
                        ValueAndSMGState.of(new NumericValue(num % den), currentState));
                  }
                case DOUBLE:
                  {
                    double num = numerValue.doubleValue();
                    double den = denomValue.doubleValue();
                    if (Double.isNaN(num)
                        || Double.isNaN(den)
                        || Double.isInfinite(num)
                        || den == 0) {
                      return ImmutableList.of(
                          ValueAndSMGState.of(new NumericValue(Double.NaN), currentState));
                    }
                    if (num == 0 && den != 0) {
                      // keep the sign on +0 and -0
                      return ImmutableList.of(ValueAndSMGState.of(numer, currentState));
                    }
                    // TODO computations on float/double are imprecise! Use epsilon environment?
                    return ImmutableList.of(
                        ValueAndSMGState.of(new NumericValue(num % den), currentState));
                  }
                default:
                  break;
              }
            }
          }
        } else if (BuiltinFloatFunctions.matchesIsgreater(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return ImmutableList.of(
                ValueAndSMGState.of(new NumericValue(num1 > num2 ? 1 : 0), currentState));
          }
        } else if (BuiltinFloatFunctions.matchesIsgreaterequal(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return ImmutableList.of(
                ValueAndSMGState.of(new NumericValue(num1 >= num2 ? 1 : 0), currentState));
          }
        } else if (BuiltinFloatFunctions.matchesIsless(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return ImmutableList.of(
                ValueAndSMGState.of(new NumericValue(num1 < num2 ? 1 : 0), currentState));
          }
        } else if (BuiltinFloatFunctions.matchesIslessequal(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return ImmutableList.of(
                ValueAndSMGState.of(new NumericValue(num1 <= num2 ? 1 : 0), currentState));
          }
        } else if (BuiltinFloatFunctions.matchesIslessgreater(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return ImmutableList.of(
                ValueAndSMGState.of(
                    new NumericValue(num1 > num2 || num1 < num2 ? 1 : 0), currentState));
          }
        } else if (BuiltinFloatFunctions.matchesIsunordered(calledFunctionName)) {
          Value op1 = parameterValues.get(0);
          Value op2 = parameterValues.get(1);
          if (op1.isExplicitlyKnown() && op2.isExplicitlyKnown()) {
            double num1 = op1.asNumericValue().doubleValue();
            double num2 = op2.asNumericValue().doubleValue();
            return ImmutableList.of(
                ValueAndSMGState.of(
                    new NumericValue(Double.isNaN(num1) || Double.isNaN(num2) ? 1 : 0),
                    currentState));
          }
        }
      }

      // Now check and use builtins of C (malloc etc.)
      SMGCPABuiltins smgBuiltins = new SMGCPABuiltins(evaluator, null, null, null, logger);
      // We don't check for the builtins because we want the failure handling of the
      // handleFunctioncall method
      return smgBuiltins.handleFunctioncall(
          pIastFunctionCallExpression, calledFunctionName, state, cfaEdge);
    }
    return ImmutableList.of(ValueAndSMGState.ofUnknownValue(state));
  }

  /* ++++++++++++++++++ Below this point value arithmetics and comparisons  ++++++++++++++++++ */

  // TODO: check if we can really just change the ordering / that all possible calculations are
  // commutative
  private Value calculateExpressionWithFunctionValue(
      BinaryOperator binaryOperator, Value val1, Value val2) {
    if (val1 instanceof FunctionValue) {
      return calculateOperationWithFunctionValue(binaryOperator, (FunctionValue) val1, val2);
    } else if (val2 instanceof FunctionValue) {
      return calculateOperationWithFunctionValue(binaryOperator, (FunctionValue) val2, val1);
    } else {
      return new Value.UnknownValue();
    }
  }

  /**
   * Calculates pointer/address arithmetic expressions. Valid is only address + value or value +
   * address and address minus value or address minus address. All others are simply unknown value!
   * One of the 2 entered values must be a AddressExpression, no other preconditions must be met.
   *
   * @param leftValue left hand side value of the arithmetic operation.
   * @param rightValue right hand side value of the arithmetic operation.
   * @param binaryOperator {@link BinaryOperator} in between the values.
   * @param expressionType {@link CType} of the final expression.
   * @param calculationType {@link CType} of the calculation. (Should be int for pointers)
   * @param currentState current {@link SMGState}
   * @return {@link ValueAndSMGState} with the result Value that may be {@link AddressExpression} /
   *     {@link UnknownValue} or a symbolic/numeric one depending on input + the new up to date
   *     state.
   */
  private ValueAndSMGState calculatePointerArithmetics(
      Value leftValue,
      Value rightValue,
      BinaryOperator binaryOperator,
      CType expressionType,
      CType calculationType,
      SMGState currentState) {
    // Find the address, check that the other is a numeric value and use as offset, else if both
    // are addresses we allow the distance, else unknown (we can't dereference symbolics)
    // TODO: stop for illegal pointer arith?
    if (binaryOperator != BinaryOperator.PLUS && binaryOperator != BinaryOperator.MINUS) {
      return ValueAndSMGState.ofUnknownValue(currentState);
    }

    // The canonical type is the return type of the pointer expression!
    CType canonicalReturnType = expressionType;
    if (calculationType instanceof CPointerType) {
      canonicalReturnType = ((CPointerType) expressionType).getType();
    }

    if (leftValue instanceof AddressExpression && !(rightValue instanceof AddressExpression)) {
      AddressExpression addressValue = (AddressExpression) leftValue;
      Value addressOffset = addressValue.getOffset();
      if (!rightValue.isNumericValue() || !addressOffset.isNumericValue()) {
        // TODO: symbolic values if possible
        return ValueAndSMGState.ofUnknownValue(currentState);
      }

      // We need the correct types here; the types of the returned value after the pointer
      // expression!
      Value correctlyTypedOffset =
          arithmeticOperation(
              new NumericValue(evaluator.getBitSizeof(currentState, canonicalReturnType)),
              (NumericValue) rightValue,
              BinaryOperator.MULTIPLY,
              calculationType);

      Value finalOffset =
          arithmeticOperation(
              (NumericValue) addressOffset,
              (NumericValue) correctlyTypedOffset,
              binaryOperator,
              calculationType);

      return ValueAndSMGState.of(addressValue.copyWithNewOffset(finalOffset), currentState);

    } else if (!(leftValue instanceof AddressExpression)
        && rightValue instanceof AddressExpression) {
      AddressExpression addressValue = (AddressExpression) rightValue;
      Value addressOffset = addressValue.getOffset();
      if (!leftValue.isNumericValue()
          || !addressOffset.isNumericValue()
          || binaryOperator == BinaryOperator.MINUS) {
        // TODO: symbolic values if possible
        return ValueAndSMGState.ofUnknownValue(currentState);
      }
      Value correctlyTypedOffset =
          arithmeticOperation(
              new NumericValue(evaluator.getBitSizeof(currentState, canonicalReturnType)),
              (NumericValue) leftValue,
              BinaryOperator.MULTIPLY,
              evaluator.getMachineModel().getPointerEquivalentSimpleType());

      Value finalOffset =
          arithmeticOperation(
              (NumericValue) correctlyTypedOffset,
              (NumericValue) addressOffset,
              binaryOperator,
              calculationType);

      return ValueAndSMGState.of(addressValue.copyWithNewOffset(finalOffset), currentState);

    } else {
      // Both are pointers, we allow minus here to get the distance
      AddressExpression addressRight = (AddressExpression) rightValue;
      AddressExpression addressLeft = (AddressExpression) leftValue;
      Value leftOffset = addressLeft.getOffset();
      Value rightOffset = addressRight.getOffset();

      // This fails if the underlying structure is not the same!
      // We need the non-equal method for SMGs here as it might be that due to abstraction 2 values
      // are not equal but refer to the same structure!
      if (binaryOperator != BinaryOperator.MINUS
          || !rightOffset.isNumericValue()
          || !leftOffset.isNumericValue()) {
        // TODO: symbolic values if possible
        return ValueAndSMGState.ofUnknownValue(currentState);
      }

      // Our offsets are in bits here! This also checks that its the same underlying memory object.
      Value distanceInBits =
          evaluator.calculateAddressDistance(
              currentState, addressLeft.getMemoryAddress(), addressRight.getMemoryAddress());

      if (!distanceInBits.isNumericValue()) {
        return ValueAndSMGState.of(distanceInBits, currentState);
      }

      // distance in bits / type size = distance
      // The type in both pointers is the same, we need the return type from one of them
      Value distance =
          arithmeticOperation(
              (NumericValue) distanceInBits,
              new NumericValue(
                  evaluator.getBitSizeof(
                      currentState, ((CPointerType) addressRight.getType()).getType())),
              BinaryOperator.DIVIDE,
              evaluator.getMachineModel().getPointerEquivalentSimpleType());

      return ValueAndSMGState.of(distance, currentState);
    }
  }

  /**
   * Join a symbolic expression with something else using a binary expression.
   *
   * <p>e.g. joining `a` and `5` with `+` will produce `a + 5`
   *
   * @param pLValue left hand side value
   * @param pRValue right hand side value
   * @param pExpression the binary expression with the operator
   * @return the calculated Value
   */
  public Value calculateSymbolicBinaryExpression(
      Value pLValue, Value pRValue, final CBinaryExpression pExpression) {

    // While the offsets and even the values of the addresses may be symbolic, the addresse
    // expressions themselfs may never be handled in such a way
    Preconditions.checkArgument(!(pLValue instanceof AddressExpression));
    Preconditions.checkArgument(!(pRValue instanceof AddressExpression));

    final BinaryOperator operator = pExpression.getOperator();

    final CType leftOperandType = pExpression.getOperand1().getExpressionType();
    final CType rightOperandType = pExpression.getOperand2().getExpressionType();
    final CType expressionType = pExpression.getExpressionType();
    final CType calculationType = pExpression.getCalculationType();

    return createSymbolicExpression(
        pLValue,
        leftOperandType,
        pRValue,
        rightOperandType,
        operator,
        expressionType,
        calculationType);
  }

  private SymbolicValue createSymbolicExpression(
      Value pLeftValue,
      CType pLeftType,
      Value pRightValue,
      CType pRightType,
      CBinaryExpression.BinaryOperator pOperator,
      CType pExpressionType,
      CType pCalculationType) {

    final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
    SymbolicExpression leftOperand;
    SymbolicExpression rightOperand;

    leftOperand = factory.asConstant(pLeftValue, pLeftType);
    rightOperand = factory.asConstant(pRightValue, pRightType);

    switch (pOperator) {
      case PLUS:
        return factory.add(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MINUS:
        return factory.minus(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MULTIPLY:
        return factory.multiply(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case DIVIDE:
        return factory.divide(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case MODULO:
        return factory.modulo(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_LEFT:
        return factory.shiftLeft(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case SHIFT_RIGHT:
        return factory.shiftRightSigned(
            leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_AND:
        return factory.binaryAnd(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_OR:
        return factory.binaryOr(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case BINARY_XOR:
        return factory.binaryXor(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case EQUALS:
        return factory.equal(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case NOT_EQUALS:
        return factory.notEqual(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_THAN:
        return factory.lessThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case LESS_EQUAL:
        return factory.lessThanOrEqual(
            leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_THAN:
        return factory.greaterThan(leftOperand, rightOperand, pExpressionType, pCalculationType);
      case GREATER_EQUAL:
        return factory.greaterThanOrEqual(
            leftOperand, rightOperand, pExpressionType, pCalculationType);
      default:
        throw new AssertionError("Unhandled binary operation " + pOperator);
    }
  }

  private NumericValue calculateOperationWithFunctionValue(
      BinaryOperator binaryOperator, FunctionValue val1, Value val2) {
    switch (binaryOperator) {
      case EQUALS:
        return new NumericValue(val1.equals(val2) ? 1 : 0);

      case NOT_EQUALS:
        return new NumericValue(val1.equals(val2) ? 0 : 1);

      default:
        throw new AssertionError(
            "Operation " + binaryOperator + " is not supported for function values");
    }
  }

  /**
   * Calculate an arithmetic operation on two Value types.
   *
   * @param lNum left hand side value
   * @param rNum right hand side value
   * @param op the binary operator
   * @param calculationType The type the result of the calculation should have
   * @return the resulting values
   */
  private Value arithmeticOperation(
      final NumericValue lNum,
      final NumericValue rNum,
      final BinaryOperator op,
      final CType calculationType) {

    // At this point we're only handling values of simple types.
    final CSimpleType type = getArithmeticType(calculationType);
    if (type == null) {
      logger.logf(
          Level.FINE, "unsupported type %s for result of binary operation %s", calculationType, op);
      return Value.UnknownValue.getInstance();
    }

    try {
      switch (type.getType()) {
        case INT:
          {
            // Both l and r must be of the same type, which in this case is INT, so we can cast to
            // long.
            long lVal = lNum.getNumber().longValue();
            long rVal = rNum.getNumber().longValue();
            long result = arithmeticOperation(lVal, rVal, op, calculationType);
            return new NumericValue(result);
          }
        case INT128:
          {
            BigInteger lVal = lNum.bigInteger();
            BigInteger rVal = rNum.bigInteger();
            BigInteger result = arithmeticOperation(lVal, rVal, op);
            return new NumericValue(result);
          }
        case DOUBLE:
          {
            if (type.isLong()) {
              return arithmeticOperationForLongDouble(lNum, rNum, op, calculationType);
            } else {
              double lVal = lNum.doubleValue();
              double rVal = rNum.doubleValue();
              double result = arithmeticOperation(lVal, rVal, op, calculationType);
              return new NumericValue(result);
            }
          }
        case FLOAT:
          {
            float lVal = lNum.floatValue();
            float rVal = rNum.floatValue();
            float result = arithmeticOperation(lVal, rVal, op);
            return new NumericValue(result);
          }
        default:
          {
            logger.logf(
                Level.FINE, "unsupported type for result of binary operation %s", type.toString());
            return Value.UnknownValue.getInstance();
          }
      }
    } catch (ArithmeticException ae) { // log warning and ignore expression
      logger.logf(
          Level.WARNING,
          "expression causes arithmetic exception (%s): %s %s %s",
          ae.getMessage(),
          lNum.bigDecimalValue(),
          op.getOperator(),
          rNum.bigDecimalValue());
      return Value.UnknownValue.getInstance();
    }
  }

  @SuppressWarnings("unused")
  private Value arithmeticOperationForLongDouble(
      NumericValue pLNum, NumericValue pRNum, BinaryOperator pOp, CType pCalculationType) {
    // TODO: cf. https://gitlab.com/sosy-lab/software/cpachecker/issues/507
    return Value.UnknownValue.getInstance();
  }

  /**
   * Calculate an arithmetic operation on two integer types.
   *
   * @param l left hand side value
   * @param r right hand side value
   * @param op the binary operator
   * @param calculationType The type the result of the calculation should have
   * @return the resulting value
   */
  private long arithmeticOperation(
      final long l, final long r, final BinaryOperator op, final CType calculationType) {

    // special handling for UNSIGNED_LONGLONG (32 and 64bit), UNSIGNED_LONG (64bit)
    // because Java only has SIGNED_LONGLONG
    CSimpleType st = getArithmeticType(calculationType);
    if (st != null) {
      if (evaluator.getMachineModel().getSizeofInBits(st) >= SIZE_OF_JAVA_LONG && st.isUnsigned()) {
        switch (op) {
          case DIVIDE:
            if (r == 0) {
              logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", l, r);
              return 0;
            }
            return UnsignedLongs.divide(l, r);
          case MODULO:
            return UnsignedLongs.remainder(l, r);
          case SHIFT_RIGHT:
            /*
             * from http://docs.oracle.com/javase/tutorial/java/nutsandbolts/op3.html
             *
             * The unsigned right shift operator ">>>" shifts a zero
             * into the leftmost position, while the leftmost position
             * after ">>" depends on sign extension.
             */
            return l >>> r;
          default:
            // fall-through, calculation is done correct as SINGED_LONG_LONG
        }
      }
    }

    switch (op) {
      case PLUS:
        return l + r;
      case MINUS:
        return l - r;
      case DIVIDE:
        if (r == 0) {
          logger.logf(Level.SEVERE, "Division by Zero (%d / %d)", l, r);
          return 0;
        }
        return l / r;
      case MODULO:
        return l % r;
      case MULTIPLY:
        return l * r;
      case SHIFT_LEFT:
        /* There is a difference in the SHIFT-operation in Java and C.
         * In C a SHIFT is a normal SHIFT, in Java the rVal is used as (r%64).
         *
         * http://docs.oracle.com/javase/specs/jls/se7/html/jls-15.html#jls-15.19
         *
         * If the promoted type of the left-hand operand is long, then only the
         * six lowest-order bits of the right-hand operand are used as the
         * shift distance. It is as if the right-hand operand were subjected to
         * a bitwise logical AND operator & (§15.22.1) with the mask value 0x3f.
         * The shift distance actually used is therefore always in the range 0 to 63.
         */
        return (r >= SIZE_OF_JAVA_LONG) ? 0 : l << r;
      case SHIFT_RIGHT:
        return l >> r;
      case BINARY_AND:
        return l & r;
      case BINARY_OR:
        return l | r;
      case BINARY_XOR:
        return l ^ r;

      default:
        throw new AssertionError("unknown binary operation: " + op);
    }
  }

  /**
   * Calculate an arithmetic operation on two double types.
   *
   * @param l left hand side value
   * @param r right hand side value
   * @param op the binary operator
   * @param calculationType The type the result of the calculation should have
   * @return the resulting value
   */
  private double arithmeticOperation(
      final double l, final double r, final BinaryOperator op, final CType calculationType) {

    checkArgument(
        calculationType.getCanonicalType() instanceof CSimpleType
            && !((CSimpleType) calculationType.getCanonicalType()).isLong(),
        "Value analysis can't compute long double values in a precise manner");

    switch (op) {
      case PLUS:
        return l + r;
      case MINUS:
        return l - r;
      case DIVIDE:
        return l / r;
      case MODULO:
        return l % r;
      case MULTIPLY:
        return l * r;
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
        throw new AssertionError("Trying to perform " + op + " on floating point operands");
      default:
        throw new AssertionError("Unknown binary operation: " + op);
    }
  }

  /**
   * Calculate an arithmetic operation on two int128 types.
   *
   * @param l left hand side value
   * @param r right hand side value
   * @param op the binary operator
   * @return the resulting value
   */
  private BigInteger arithmeticOperation(
      final BigInteger l, final BigInteger r, final BinaryOperator op) {

    switch (op) {
      case PLUS:
        return l.add(r);
      case MINUS:
        return l.subtract(r);
      case DIVIDE:
        if (r.equals(BigInteger.ZERO)) {
          // this matches the behavior of long
          logger.logf(Level.SEVERE, "Division by Zero (%s / %s)", l.toString(), r.toString());
          return BigInteger.ZERO;
        }
        return l.divide(r);
      case MODULO:
        return l.mod(r);
      case MULTIPLY:
        return l.multiply(r);
      case SHIFT_LEFT:
        // (C11, 6.5.7p3) "If the value of the right operand is negative
        // or is greater than or equal to the width of the promoted left operand,
        // the behavior is undefined"
        if (r.compareTo(BigInteger.valueOf(128)) <= 0 && r.signum() != -1) {
          return l.shiftLeft(r.intValue());
        } else {
          logger.logf(
              Level.SEVERE,
              "Right-hand side (%s) of the bitshift is larger than 128 or negative.",
              r.toString());
          return BigInteger.ZERO;
        }
      case SHIFT_RIGHT:
        if (r.compareTo(BigInteger.valueOf(128)) <= 0 && r.signum() != -1) {
          return l.shiftRight(r.intValue());
        } else {
          return BigInteger.ZERO;
        }
      case BINARY_AND:
        return l.and(r);
      case BINARY_OR:
        return l.or(r);
      case BINARY_XOR:
        return l.xor(r);
      default:
        throw new AssertionError("Unknown binary operation: " + op);
    }
  }

  /**
   * Calculate an arithmetic operation on two float types.
   *
   * @param l left hand side value
   * @param r right hand side value
   * @return the resulting value
   */
  private float arithmeticOperation(final float l, final float r, final BinaryOperator op) {

    switch (op) {
      case PLUS:
        return l + r;
      case MINUS:
        return l - r;
      case DIVIDE:
        return l / r;
      case MODULO:
        return l % r;
      case MULTIPLY:
        return l * r;
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
        throw new AssertionError("Trying to perform " + op + " on floating point operands");
      default:
        throw new AssertionError("Unknown binary operation: " + op);
    }
  }

  private Value booleanOperation(
      final NumericValue l,
      final NumericValue r,
      final BinaryOperator op,
      final CType calculationType) {

    // At this point we're only handling values of simple types.
    final CSimpleType type = getArithmeticType(calculationType);
    if (type == null) {
      logger.logf(
          Level.FINE, "unsupported type %s for result of binary operation %s", calculationType, op);
      return Value.UnknownValue.getInstance();
    }

    final int cmp;
    switch (type.getType()) {
      case INT128:
      case CHAR:
      case INT:
        {
          // TODO: test this in particular!
          BigInteger leftBigInt =
              l.getNumber() instanceof BigInteger
                  ? (BigInteger) l.getNumber()
                  : BigInteger.valueOf(l.longValue());
          BigInteger rightBigInt =
              r.getNumber() instanceof BigInteger
                  ? (BigInteger) r.getNumber()
                  : BigInteger.valueOf(r.longValue());
          cmp = leftBigInt.compareTo(rightBigInt);
          break;
        }
      case FLOAT:
        {
          float lVal = l.floatValue();
          float rVal = r.floatValue();

          if (Float.isNaN(lVal) || Float.isNaN(rVal)) {
            return new NumericValue(op == BinaryOperator.NOT_EQUALS ? 1L : 0L);
          }
          if (lVal == 0 && rVal == 0) {
            cmp = 0;
          } else {
            cmp = Float.compare(lVal, rVal);
          }
          break;
        }
      case DOUBLE:
        {
          double lVal = l.doubleValue();
          double rVal = r.doubleValue();

          if (Double.isNaN(lVal) || Double.isNaN(rVal)) {
            return new NumericValue(op == BinaryOperator.NOT_EQUALS ? 1L : 0L);
          }

          if (lVal == 0 && rVal == 0) {
            cmp = 0;
          } else {
            cmp = Double.compare(lVal, rVal);
          }
          break;
        }
      default:
        {
          logger.logf(
              Level.FINE,
              "unsupported type %s for result of binary operation %s",
              type.toString(),
              op);
          return Value.UnknownValue.getInstance();
        }
    }

    // return 1 if expression holds, 0 otherwise
    return new NumericValue(matchBooleanOperation(op, cmp) ? 1L : 0L);
  }

  /** returns True, iff cmp fulfills the boolean operation. */
  private boolean matchBooleanOperation(final BinaryOperator op, final int cmp) {
    switch (op) {
      case GREATER_THAN:
        return cmp > 0;
      case GREATER_EQUAL:
        return cmp >= 0;
      case LESS_THAN:
        return cmp < 0;
      case LESS_EQUAL:
        return cmp <= 0;
      case EQUALS:
        return cmp == 0;
      case NOT_EQUALS:
        return cmp != 0;
      default:
        throw new AssertionError("Unknown binary operation: " + op);
    }
  }

  /**
   * Returns a numeric type that can be used to perform arithmetics on an instance of the type
   * directly, or null if none.
   *
   * <p>Most notably, CPointerType will be converted to the unsigned integer type of correct size.
   *
   * @param type the input type
   */
  public CSimpleType getArithmeticType(CType type) {
    type = type.getCanonicalType();
    if (type instanceof CPointerType) {
      return CNumericTypes.INT;
    } else if (type instanceof CSimpleType) {
      return (CSimpleType) type;
    } else {
      return null;
    }
  }

  private boolean isArithmeticOperation(BinaryOperator binaryOperator) {
    switch (binaryOperator) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MODULO:
      case MULTIPLY:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
        return true;
      default:
        return false;
    }
  }

  private boolean isComparison(BinaryOperator binaryOperator) {
    switch (binaryOperator) {
      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL:
        return true;
      default:
        return false;
    }
  }

  /**
   * Only accessible for subclasses.
   *
   * @return the {@link SMGState} given to this visitor when it was created.
   */
  protected SMGState getInitialVisitorState() {
    return state;
  }

  /**
   * Only accessible for subclasses.
   *
   * @return the {@link CFAEdge} given to this visitor when it was created.
   */
  protected CFAEdge getInitialVisitorCFAEdge() {
    return cfaEdge;
  }

  /**
   * Only accessible for subclasses.
   *
   * @return the {@link SMGCPAValueExpressionEvaluator} given to this visitor when it was created.
   */
  protected SMGCPAValueExpressionEvaluator getInitialVisitorEvaluator() {
    return evaluator;
  }

  /**
   * Only accessible for subclasses.
   *
   * @return the {@link LogManagerWithoutDuplicates} given to this visitor when it was created.
   */
  protected LogManagerWithoutDuplicates getInitialVisitorLogger() {
    return logger;
  }
}
