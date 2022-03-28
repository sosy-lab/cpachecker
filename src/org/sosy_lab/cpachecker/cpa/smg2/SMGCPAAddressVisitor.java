// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This visitor is only meant to get the first memory (SMGObject) of the entered expression. This is
 * needed in the & operator.
 */
public class SMGCPAAddressVisitor
    extends DefaultCExpressionVisitor<Optional<SMGObjectAndOffset>, CPATransferException>
    implements CRightHandSideVisitor<Optional<SMGObjectAndOffset>, CPATransferException> {

  private final SMGCPAValueExpressionEvaluator evaluator;

  private final SMGState state;

  private final LogManagerWithoutDuplicates logger;

  /** This edge is only to be used for debugging/logging! */
  private final CFAEdge cfaEdge;

  public SMGCPAAddressVisitor(
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
  public Optional<SMGObjectAndOffset> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Optional<SMGObjectAndOffset> visitDefault(CExpression pExp)
      throws CPATransferException {
    // Just get a default value and log
    logger.logf(
        Level.INFO,
        "%s, Default value: CExpression %s could not find a address for %s. Related CFAEdge: %s",
        cfaEdge.getFileLocation(),
        pExp,
        SMGValue.zeroValue(),
        cfaEdge.getRawStatement());
    return Optional.empty();
  }

  @Override
  public Optional<SMGObjectAndOffset> visit(CArraySubscriptExpression e)
      throws CPATransferException {
    // Array subscript is default Java array usage. Example: array[5]
    // In C this can be translated to *(array + 5), but the array may be on the stack/heap (or
    // global, but be throw global and stack togehter when reading). Note: this is commutative!

    // The expression is split into array and subscript expression
    // Use the array expression in the visitor again to get the array address
    // The type of the arrayExpr may be pointer or array, depending on stack/heap
    CExpression arrayExpr = e.getArrayExpression();
    List<ValueAndSMGState> arrayValueAndStates =
        arrayExpr.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger));
    // We know that there can only be 1 return value for the array address
    ValueAndSMGState arrayValueAndState = arrayValueAndStates.get(0);

    Value arrayValue = arrayValueAndState.getValue();

    // Evaluate the subscript as far as possible
    CExpression subscriptExpr = e.getSubscriptExpression();
    List<ValueAndSMGState> subscriptValueAndStates =
        subscriptExpr.accept(
            new SMGCPAValueVisitor(evaluator, arrayValueAndState.getState(), cfaEdge, logger));

    // We know that there can only be 1 return value for the subscript
    // We don't care about additional values from the array, they would be just more chars of a
    // String, but we want
    // the first offset only!
    ValueAndSMGState subscriptValueAndState = subscriptValueAndStates.get(0);

    Value subscriptValue = subscriptValueAndState.getValue();
    SMGState newState = subscriptValueAndState.getState();
    // If the subscript is a unknown value, we can't read anything and return unknown
    if (!subscriptValue.isNumericValue()) {
      // TODO: log this!
      return Optional.empty();
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
      // The pointer might actually point inside of the array, take the offset of that into account!
      Value arrayPointerOffsetExpr = addressValue.getOffset();
      if (!arrayPointerOffsetExpr.isNumericValue()) {
        // The offset is some non numeric Value and therefore not useable!
        // TODO: log
        return Optional.empty();
      }
      subscriptOffset = arrayPointerOffsetExpr.asNumericValue().bigInteger().add(subscriptOffset);

      return evaluator.getTargetObjectAndOffset(
          newState, addressValue.getMemoryAddress(), subscriptOffset);
    } else {
      // Here our arrayValue holds the name of our variable
      Preconditions.checkArgument(arrayValue instanceof SymbolicValue);

      MemoryLocation maybeVariableIdent =
          ((SymbolicValue) arrayValue).getRepresentedLocation().orElseThrow();

      // This might actually point inside the array, add the offset
      if (maybeVariableIdent.isReference()) {
        // TODO: is it possible for this offset to be unknown?
        subscriptOffset = subscriptOffset.add(BigInteger.valueOf(maybeVariableIdent.getOffset()));
      }
      return evaluator.getTargetObjectAndOffset(
          newState, maybeVariableIdent.getIdentifier(), subscriptOffset);
    }
  }

  @Override
  public Optional<SMGObjectAndOffset> visit(CCastExpression e) throws CPATransferException {
    return e.getOperand().accept(this);
  }

  @Override
  public Optional<SMGObjectAndOffset> visit(CFieldReference e) throws CPATransferException {
    // This is the field of a struct/union, so smth like struct.field or struct->field.
    // In the later case its a pointer dereference.
    // Get the object of the field with its offset

    // First we transform x->f into (*x).f per default
    CFieldReference explicitReference = e.withExplicitPointerDereference();

    // Owner expression; the struct/union with this field. Use this to get the address of the
    // general object.
    CExpression ownerExpression = explicitReference.getFieldOwner();
    // For (*pointer).field case or struct.field case the visitor returns the Value for the
    // correct SMGObject (if it exists)
    List<ValueAndSMGState> structValuesAndStates =
        ownerExpression.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger));

    // The most up to date state is always the last one
    SMGState currentState = structValuesAndStates.get(structValuesAndStates.size() - 1).getState();

    // If the field we want to read is a String we get multiple returned values, but we take only
    // the first as this is the start of the memory we want!

    // This value is either a AddressValue for pointers i.e. (*struct).field or a general
    // SymbolicValue
    Value structValue = structValuesAndStates.get(0).getValue();

    // Now get the offset of the current field
    BigInteger fieldOffset =
        evaluator.getFieldOffsetInBits(
            SMGCPAValueExpressionEvaluator.getCanonicalType(ownerExpression),
            explicitReference.getFieldName());

    // This is either a stack/global variable of the form struct.field or a pointer of the form
    // (*structP).field. The later needs a pointer deref
    if (ownerExpression instanceof CPointerExpression) {
      // In the pointer case, the Value needs to be a AddressExpression
      Preconditions.checkArgument(structValue instanceof AddressExpression);
      AddressExpression addressAndOffsetValue = (AddressExpression) structValue;
      // This AddressExpr theoretically can have a offset
      Value structPointerOffsetExpr = addressAndOffsetValue.getOffset();
      if (!structPointerOffsetExpr.isNumericValue()) {
        // The offset is some non numeric Value and therefore not useable!

      }
      BigInteger finalFieldOffset =
          structPointerOffsetExpr.asNumericValue().bigInteger().add(fieldOffset);

      return evaluator.getTargetObjectAndOffset(
          currentState, addressAndOffsetValue.getMemoryAddress(), finalFieldOffset);

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

      return evaluator.getTargetObjectAndOffset(
          currentState, maybeVariableIdent.getIdentifier(), finalFieldOffset);

    } else {
      // TODO: improve error and check if its even needed
      throw new SMG2Exception("Unknown field type in field expression.");
    }
  }

  @Override
  public Optional<SMGObjectAndOffset> visit(CIdExpression e) throws CPATransferException {
    CSimpleDeclaration varDecl = e.getDeclaration();
    if (varDecl == null) {
      // The var was not declared
      SMGState errorState = state.withUndeclaredVariableUsage(e);
      throw new SMG2Exception(errorState);
    }
    return evaluator.getTargetObjectAndOffset(state, varDecl.getQualifiedName());
  }

  @Override
  public Optional<SMGObjectAndOffset> visit(CPointerExpression e) throws CPATransferException {
    // This should subavaluate to a AddressExpression in the visit call in the beginning as we
    // always evaluate to the address, but only
    // dereference and read it if its not a struct/union as those will be dereferenced by the field
    // expression

    // Get the type of the target
    CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(e.getExpressionType());
    // Get the expression that is dereferenced
    CExpression expr = e.getOperand();
    // Evaluate the expression to a Value; this should return a Symbolic Value with the address of
    // the target and a offset. If this fails this returns a UnknownValue.
    List<ValueAndSMGState> evaluatedExpr =
        expr.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger));
    // Take the last state as thats the most up to date one
    SMGState currentState = evaluatedExpr.get(evaluatedExpr.size() - 1).getState();
    // The list only has more than 1 entries if its a String and if thats the case we only care
    // about the first offset
    ValueAndSMGState valueAndState = evaluatedExpr.get(0);
    // Try to disassemble the values (AddressExpression)
    Value value = valueAndState.getValue();
    Preconditions.checkArgument(value instanceof AddressExpression);
    AddressExpression pointerValue = (AddressExpression) value;

    // The offset part of the pointer; its either numeric or we can't get a concrete value
    Value offset = pointerValue.getOffset();
    if (!offset.isNumericValue()) {
      // If the offset is not numericly known we can't read a value, return
      return Optional.empty();
    }

    if (type instanceof CFunctionType) {
      // Special cases
      // TODO:
      return visitDefault(e);
    } else {
      BigInteger offsetInBits = offset.asNumericValue().bigInteger();

      return evaluator.getTargetObjectAndOffset(
          currentState, pointerValue.getMemoryAddress(), offsetInBits);
    }
  }
}
