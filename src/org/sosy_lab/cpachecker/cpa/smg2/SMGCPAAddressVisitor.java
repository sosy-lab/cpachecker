// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
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
 * This visitor is only meant to get the memory (SMGObject) of the entered expression. This is
 * needed in allocations i.e. a = b; and the & operator. This class returns a list of optionals with
 * the target memory region + offset inside if one is found. If a expression is given to this
 * visitor with no memory region the optional will be empty; i.e. SMGCPAAddressVisitor(3); with 3
 * being a int for example. (this is done such that the method asking for the memory region can have
 * a dedicated error without exception catching).
 */
public class SMGCPAAddressVisitor
    extends DefaultCExpressionVisitor<List<Optional<SMGObjectAndOffset>>, CPATransferException>
    implements CRightHandSideVisitor<List<Optional<SMGObjectAndOffset>>, CPATransferException> {

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
  public List<Optional<SMGObjectAndOffset>> visit(
      CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
    // Evaluate the expression to a Value; this should return a Symbolic Value with the address of
    // the target and a offset if it really has a address. If this fails this returns a different
    // Value class.
    /*List<ValueAndSMGState> evaluatedExpr =
    pIastFunctionCallExpression.accept(
        new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger));
        */
    // TODO handle possible returns
    return ImmutableList.of(Optional.empty());
  }

  @Override
  protected List<Optional<SMGObjectAndOffset>> visitDefault(CExpression pExp)
      throws CPATransferException {
    // Just get a default value and log
    logger.logf(
        Level.INFO,
        "%s, Default value: CExpression %s could not find a address for %s. Related CFAEdge: %s",
        cfaEdge.getFileLocation(),
        pExp,
        SMGValue.zeroValue(),
        cfaEdge.getRawStatement());
    return ImmutableList.of(Optional.empty());
  }

  @Override
  public List<Optional<SMGObjectAndOffset>> visit(CArraySubscriptExpression e)
      throws CPATransferException {
    // Array subscript is default Java array usage. Example: array[5]
    // In C this can be translated to *(array + 5), but the array may be on the stack/heap (or
    // global, but be throw global and stack togehter when reading). Note: this is commutative!

    // The expression is split into array and subscript expression
    // Use the array expression in the visitor again to get the array address
    // The type of the arrayExpr may be pointer or array, depending on stack/heap
    CExpression arrayExpr = e.getArrayExpression();
    CExpression subscriptExpr = e.getSubscriptExpression();
    ImmutableList.Builder<Optional<SMGObjectAndOffset>> resultBuilder = ImmutableList.builder();

    for (ValueAndSMGState arrayValueAndState :
        arrayExpr.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger))) {
      Value arrayValue = arrayValueAndState.getValue();

      // Evaluate the subscript as far as possible
      for (ValueAndSMGState subscriptValueAndState :
          subscriptExpr.accept(
              new SMGCPAValueVisitor(evaluator, arrayValueAndState.getState(), cfaEdge, logger))) {

        Value subscriptValue = subscriptValueAndState.getValue();
        SMGState currentState = subscriptValueAndState.getState();
        // If the subscript is a unknown value, we can't read anything and return unknown
        if (!subscriptValue.isNumericValue()) {
          logger.log(
              Level.FINE,
              "A subscript value was found to be non concrete when trying to find a memory location"
                  + " of an array. No memory region could be returned.");
          resultBuilder.add(Optional.empty());
          continue;
        }
        // Calculate the offset out of the subscript value and the type
        BigInteger typeSizeInBits = evaluator.getBitSizeof(currentState, e.getExpressionType());
        BigInteger subscriptOffset =
            typeSizeInBits.multiply(subscriptValue.asNumericValue().bigInteger());

        // Get the value from the array and return the value + state
        // (the is pointer check is needed because of nested subscript; i.e. array[1][1]; as if we
        // access array[1] first, we can see that the next type is CArray which makes only sense for
        // nested arrays -> it reads a pointer and returns it even if the type is not a pointer
        // expr)
        boolean isPointer = evaluator.isPointerValue(arrayValue, currentState);
        if (arrayExpr.getExpressionType() instanceof CPointerType || isPointer) {
          if (isPointer) {
            CType exprType = e.getExpressionType();
            arrayValue =
                AddressExpression.withZeroOffset(
                    arrayValue,
                    new CPointerType(exprType.isConst(), exprType.isVolatile(), exprType));
          }

          // In the pointer case, the Value needs to be a AddressExpression
          if (!(arrayValue instanceof AddressExpression)) {
            throw new SMG2Exception(
                "A pointer expression was found to be not of the type AddressExpression and is"
                    + " therefore invalid.");
          }
          AddressExpression addressValue = (AddressExpression) arrayValue;
          // The pointer might actually point inside of the array, take the offset of that into
          // account!
          Value arrayPointerOffsetExpr = addressValue.getOffset();
          if (!arrayPointerOffsetExpr.isNumericValue()) {
            // The offset is some non numeric Value and therefore not usable!
            logger.log(
                Level.FINE,
                "A subscript value was found to be non concrete when trying to find a memory"
                    + " location in an array. No memory region could be returned.");
            resultBuilder.add(Optional.empty());
          }
          subscriptOffset =
              arrayPointerOffsetExpr.asNumericValue().bigInteger().add(subscriptOffset);

          resultBuilder.add(
              evaluator.getTargetObjectAndOffset(
                  currentState, addressValue.getMemoryAddress(), subscriptOffset));
        } else if (arrayValue instanceof SymbolicValue) {
       // Here our arrayValue holds the name of our variable
          MemoryLocation maybeVariableIdent =
              ((SymbolicValue) arrayValue).getRepresentedLocation().orElseThrow();

          // This might actually point inside the array, add the offset
          if (maybeVariableIdent.isReference()) {
            // TODO: is it possible for this offset to be unknown?
            subscriptOffset =
                subscriptOffset.add(BigInteger.valueOf(maybeVariableIdent.getOffset()));
          }
          resultBuilder.add(
              evaluator.getTargetObjectAndOffset(
                  currentState, maybeVariableIdent.getIdentifier(), subscriptOffset));
        } else {
          // Unknown case etc.
          logger.log(
              Level.FINE,
              "Unknown subscript value when trying to find a memory location of an array. No memory"
                  + " region could be returned.");
          resultBuilder.add(Optional.empty());
        }
      }
    }

    return resultBuilder.build();
  }

  @Override
  public List<Optional<SMGObjectAndOffset>> visit(CCastExpression e) throws CPATransferException {
    return e.getOperand().accept(this);
  }

  @Override
  public List<Optional<SMGObjectAndOffset>> visit(CFieldReference e) throws CPATransferException {
    // This is the field of a struct/union, so smth like struct.field or struct->field.
    // In the later case its a pointer dereference.
    // Get the object of the field with its offset

    // First we transform x->f into (*x).f per default
    CFieldReference explicitReference = e.withExplicitPointerDereference();

    // Owner expression; the struct/union with this field. Use this to get the address of the
    // general object.
    CExpression ownerExpression = explicitReference.getFieldOwner();
    ImmutableList.Builder<Optional<SMGObjectAndOffset>> resultBuilder = ImmutableList.builder();

    // For (*pointer).field case or struct.field case the visitor returns the Value for the
    // correct SMGObject (if it exists)
    for (ValueAndSMGState structValuesAndState :
        ownerExpression.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger))) {
      // This value is either a AddressValue for pointers i.e. (*struct).field or a general
      // SymbolicValue
      Value structValue = structValuesAndState.getValue();
      SMGState currentState = structValuesAndState.getState();

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
          // The offset is some non numeric Value and therefore not usable!
          logger.log(
              Level.FINE,
              "A field reference could not be resolved to a concrete offset when trying to find the"
                  + " memory location of a composite field. No memory region could be returned.");
          resultBuilder.add(Optional.empty());
        }
        BigInteger finalFieldOffset =
            structPointerOffsetExpr.asNumericValue().bigInteger().add(fieldOffset);

        resultBuilder.add(
            evaluator.getTargetObjectAndOffset(
                currentState, addressAndOffsetValue.getMemoryAddress(), finalFieldOffset));

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

        resultBuilder.add(
            evaluator.getTargetObjectAndOffset(
                currentState, maybeVariableIdent.getIdentifier(), finalFieldOffset));

      } else {
        throw new SMG2Exception(
            "Unknown field type in field expression in the search for memory addresses.");
      }
    }
    return resultBuilder.build();
  }

  @Override
  public List<Optional<SMGObjectAndOffset>> visit(CIdExpression e) throws CPATransferException {
    CSimpleDeclaration varDecl = e.getDeclaration();
    if (varDecl == null) {
      // The var was not declared
      throw new SMG2Exception("Usage of undeclared variable: " + e.getName() + ".");
    }
    return ImmutableList.of(evaluator.getTargetObjectAndOffset(state, varDecl.getQualifiedName()));
  }

  @Override
  public List<Optional<SMGObjectAndOffset>> visit(CPointerExpression e)
      throws CPATransferException {
    // This should subavaluate to a AddressExpression in the visit call in the beginning as we
    // always evaluate to the address

    // Get the type of the target
    CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(e.getExpressionType());
    // Get the expression that is dereferenced
    CExpression expr = e.getOperand();
    // Evaluate the expression to a Value; this should return a Symbolic Value with the address of
    // the target and a offset. If this fails this returns a UnknownValue.

    ImmutableList.Builder<Optional<SMGObjectAndOffset>> resultBuilder = ImmutableList.builder();
    for (ValueAndSMGState evaluatedSubExpr :
        expr.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger))) {
      SMGState currentState = evaluatedSubExpr.getState();
      // Try to disassemble the values (AddressExpression)
      Value value = evaluatedSubExpr.getValue();
      Preconditions.checkArgument(value instanceof AddressExpression);
      AddressExpression pointerValue = (AddressExpression) value;

      // The offset part of the pointer; its either numeric or we can't get a concrete value
      Value offset = pointerValue.getOffset();
      if (!offset.isNumericValue()) {
        // If the offset is not numericly known we can't read a value, return
        resultBuilder.add(Optional.empty());
        continue;
      }

      if (type instanceof CFunctionType) {
        // Special cases
        // TODO:
        logger.log(
            Level.FINE,
            "Currently there is no function pointer implementation. No memory region could be"
                + " returned.");
        resultBuilder.addAll(visitDefault(e));
      } else {
        BigInteger offsetInBits = offset.asNumericValue().bigInteger();
        resultBuilder.add(
            evaluator.getTargetObjectAndOffset(
                currentState, pointerValue.getMemoryAddress(), offsetInBits));
      }
    }
    return resultBuilder.build();
  }
}
