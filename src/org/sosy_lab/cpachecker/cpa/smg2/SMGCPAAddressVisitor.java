// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffsetOrSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
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
    extends DefaultCExpressionVisitor<List<SMGObjectAndOffsetOrSMGState>, CPATransferException>
    implements CRightHandSideVisitor<List<SMGObjectAndOffsetOrSMGState>, CPATransferException> {

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
  public List<SMGObjectAndOffsetOrSMGState> visit(
      CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
    // Evaluate the expression to a Value; this should return a Symbolic Value with the address of
    // the target and a offset if it really has a address. If this fails this returns a different
    // Value class.
    /*List<ValueAndSMGState> evaluatedExpr =
    pIastFunctionCallExpression.accept(
        new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger));
        */
    // TODO handle possible returns
    return ImmutableList.of(SMGObjectAndOffsetOrSMGState.of(state));
  }

  @Override
  protected List<SMGObjectAndOffsetOrSMGState> visitDefault(CExpression pExp)
      throws CPATransferException {
    // Just get a default value and log
    logger.logf(
        Level.INFO,
        "%s, Default value: CExpression %s could not find a address for %s. Related CFAEdge: %s",
        cfaEdge.getFileLocation(),
        pExp,
        state,
        cfaEdge.getRawStatement());
    return ImmutableList.of(SMGObjectAndOffsetOrSMGState.of(state));
  }

  @Override
  public List<SMGObjectAndOffsetOrSMGState> visit(CArraySubscriptExpression e)
      throws CPATransferException {
    // Array subscript is default Java array usage. Example: array[5]
    // In C this can be translated to *(array + 5), but the array may be on the stack/heap (or
    // global, but be throw global and stack togehter when reading). Note: this is commutative!

    // The expression is split into array and subscript expression
    // Use the array expression in the visitor again to get the array address
    // The type of the arrayExpr may be pointer or array, depending on stack/heap
    CExpression arrayExpr = e.getArrayExpression();
    CExpression subscriptExpr = e.getSubscriptExpression();
    ImmutableList.Builder<SMGObjectAndOffsetOrSMGState> resultBuilder = ImmutableList.builder();

    for (ValueAndSMGState arrayValueAndState :
        arrayExpr.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger))) {
      Value arrayValue = arrayValueAndState.getValue();
      SMGState currentState = arrayValueAndState.getState();

      if (arrayValue.isUnknown()) {
        resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
        continue;
      }

      // Evaluate the subscript as far as possible
      for (ValueAndSMGState subscriptValueAndState :
          subscriptExpr.accept(new SMGCPAValueVisitor(evaluator, currentState, cfaEdge, logger))) {

        Value subscriptValue = subscriptValueAndState.getValue();
        currentState = subscriptValueAndState.getState();
        // If the subscript is a unknown value, we can't read anything and return unknown
        if (!subscriptValue.isNumericValue()) {
          logger.log(
              Level.FINE,
              "A subscript value was found to be non concrete when trying to find a memory location"
                  + " of an array. No memory region could be returned.");
          resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
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
        if ((arrayValue instanceof AddressExpression)) {
          AddressExpression arrayAddr = (AddressExpression) arrayValue;
          Value addrOffset = arrayAddr.getOffset();
          if (!addrOffset.isNumericValue()) {
            logger.log(
                Level.FINE,
                "A offset value was found to be non concrete when trying to find a memory"
                    + " location in an array. No memory region could be returned.");
            resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
          }
          BigInteger baseOffset = addrOffset.asNumericValue().bigInteger();
          BigInteger finalOffset = baseOffset.add(subscriptOffset);

          resultBuilder.add(
              evaluator.getTargetObjectAndOffset(
                  currentState, arrayAddr.getMemoryAddress(), finalOffset));

        } else if (arrayValue instanceof SymbolicIdentifier
            && ((SymbolicIdentifier) arrayValue).getRepresentedLocation().isPresent()) {
          MemoryLocation variableAndOffset =
              ((SymbolicIdentifier) arrayValue).getRepresentedLocation().orElseThrow();
          String varName = variableAndOffset.getIdentifier();
          BigInteger baseOffset = BigInteger.valueOf(variableAndOffset.getOffset());
          BigInteger finalOffset = baseOffset.add(subscriptOffset);

          Optional<SMGObjectAndOffset> maybeTarget =
              evaluator.getTargetObjectAndOffset(currentState, varName, finalOffset);
          if (maybeTarget.isPresent()) {
            resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(maybeTarget.orElseThrow()));
          } else {
            resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
          }

        } else {
          // Might be numeric 0 (0 object). All else cases are basically invalid requests.
          if (arrayValue.isNumericValue()
              && arrayValue.asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0) {
            resultBuilder.add(
                SMGObjectAndOffsetOrSMGState.of(SMGObject.nullInstance(), subscriptOffset));
          } else {
            resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
          }
        }
      }
    }
    return resultBuilder.build();
  }

  @Override
  public List<SMGObjectAndOffsetOrSMGState> visit(CCastExpression e) throws CPATransferException {
    return e.getOperand().accept(this);
  }

  @Override
  public List<SMGObjectAndOffsetOrSMGState> visit(CFieldReference e) throws CPATransferException {
    // This is the field of a struct/union, so smth like struct.field or struct->field.
    // In the later case its a pointer dereference.
    // Get the object of the field with its offset

    // First we transform x->f into (*x).f per default
    CFieldReference explicitReference = e.withExplicitPointerDereference();

    // Owner expression; the struct/union with this field. Use this to get the address of the
    // general object.
    CExpression ownerExpression = explicitReference.getFieldOwner();
    ImmutableList.Builder<SMGObjectAndOffsetOrSMGState> resultBuilder = ImmutableList.builder();
    for (ValueAndSMGState structValuesAndState :
        ownerExpression.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger))) {
      // This value is either a AddressValue for pointers i.e. (*struct).field or a general
      // SymbolicValue
      Value structValue = structValuesAndState.getValue();
      SMGState currentState = structValuesAndState.getState();
      if (structValue.isUnknown()) {
        resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
        continue;
      }

      // Now get the offset of the current field
      BigInteger fieldOffset =
          evaluator.getFieldOffsetInBits(
              SMGCPAValueExpressionEvaluator.getCanonicalType(ownerExpression),
              explicitReference.getFieldName());

      if (structValue instanceof AddressExpression) {
        AddressExpression structAddr = (AddressExpression) structValue;
        Value addrOffset = structAddr.getOffset();
        if (!addrOffset.isNumericValue()) {
          // Non numeric offset -> not usable
          resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
        }
        BigInteger baseOffset = addrOffset.asNumericValue().bigInteger();
        BigInteger finalFieldOffset = baseOffset.add(fieldOffset);

        resultBuilder.add(
            evaluator.getTargetObjectAndOffset(
                currentState, structAddr.getMemoryAddress(), finalFieldOffset));

      } else if (structValue instanceof SymbolicIdentifier
          && ((SymbolicIdentifier) structValue).getRepresentedLocation().isPresent()) {
        MemoryLocation variableAndOffset =
            ((SymbolicIdentifier) structValue).getRepresentedLocation().orElseThrow();
        String varName = variableAndOffset.getIdentifier();
        BigInteger baseOffset = BigInteger.valueOf(variableAndOffset.getOffset());
        BigInteger finalFieldOffset = baseOffset.add(fieldOffset);

        Optional<SMGObjectAndOffset> maybeTarget =
            evaluator.getTargetObjectAndOffset(currentState, varName, finalFieldOffset);
        if (maybeTarget.isPresent()) {
          resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(maybeTarget.orElseThrow()));
        } else {
          resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
        }

      } else {
        // Might be numeric 0 (0 object). All else cases are basically invalid requests.
        if (structValue.isNumericValue()
            && structValue.asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0) {
          SMGObjectAndOffsetOrSMGState.of(currentState);
          resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(SMGObject.nullInstance(), fieldOffset));
        } else {
          resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
        }
      }
    }
    return resultBuilder.build();
  }

  @Override
  public List<SMGObjectAndOffsetOrSMGState> visit(CIdExpression e) throws CPATransferException {
    CSimpleDeclaration varDecl = e.getDeclaration();
    if (varDecl == null) {
      // The var was not declared
      throw new SMG2Exception("Usage of undeclared variable: " + e.getName() + ".");
    }
    Optional<SMGObjectAndOffset> maybeTarget =
        evaluator.getTargetObjectAndOffset(state, varDecl.getQualifiedName());
    if (maybeTarget.isPresent()) {
      return ImmutableList.of(SMGObjectAndOffsetOrSMGState.of(maybeTarget.orElseThrow()));
    } else {
      return ImmutableList.of(SMGObjectAndOffsetOrSMGState.of(state));
    }
  }

  @Override
  public List<SMGObjectAndOffsetOrSMGState> visit(CPointerExpression e)
      throws CPATransferException {
    // This should subavaluate to a AddressExpression in the visit call in the beginning as we
    // always evaluate to the address

    // Get the type of the target
    CType type = SMGCPAValueExpressionEvaluator.getCanonicalType(e.getExpressionType());
    // Get the expression that is dereferenced
    CExpression expr = e.getOperand();
    // Evaluate the expression to a Value; this should return a Symbolic Value with the address of
    // the target and a offset. If this fails this returns a UnknownValue.

    ImmutableList.Builder<SMGObjectAndOffsetOrSMGState> resultBuilder = ImmutableList.builder();
    for (ValueAndSMGState evaluatedSubExpr :
        expr.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger))) {
      SMGState currentState = evaluatedSubExpr.getState();
      // Try to disassemble the values (AddressExpression)
      Value value = evaluatedSubExpr.getValue();
      if (!(value instanceof AddressExpression)) {
        resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
        continue;
      }

      AddressExpression pointerValue = (AddressExpression) value;

      // The offset part of the pointer; its either numeric or we can't get a concrete value
      Value offset = pointerValue.getOffset();
      if (!offset.isNumericValue()) {
        // If the offset is not numericly known we can't read a value, return
        resultBuilder.add(SMGObjectAndOffsetOrSMGState.of(currentState));
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
