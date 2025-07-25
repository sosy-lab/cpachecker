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
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffsetMaybeNestingLvl;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This visitor is only meant to get the memory (SMGObject) of the entered expression. This is
 * needed in allocations i.e. a = b; and the & operator. This class returns a list of optionals with
 * the target memory region + offset inside if one is found. If an expression is given to this
 * visitor with no memory region the optional will be empty; i.e. SMGCPAAddressVisitor(3); with 3
 * being an int for example. (this is done such that the method asking for the memory region can
 * have a dedicated error without exception catching).
 */
public class SMGCPAAddressVisitor
    extends DefaultCExpressionVisitor<
        List<SMGStateAndOptionalSMGObjectAndOffset>, CPATransferException>
    implements CRightHandSideVisitor<
        List<SMGStateAndOptionalSMGObjectAndOffset>, CPATransferException> {

  private final SMGCPAExpressionEvaluator evaluator;

  private final SMGState state;

  private final LogManagerWithoutDuplicates logger;

  /** This edge is only to be used for debugging/logging! */
  private final CFAEdge cfaEdge;

  private final SMGOptions options;

  public SMGCPAAddressVisitor(
      SMGCPAExpressionEvaluator pEvaluator,
      SMGState currentState,
      CFAEdge edge,
      LogManagerWithoutDuplicates pLogger,
      SMGOptions pOptions) {
    evaluator = pEvaluator;
    state = currentState;
    cfaEdge = edge;
    logger = pLogger;
    options = pOptions;
  }

  @Override
  public List<SMGStateAndOptionalSMGObjectAndOffset> visit(
      CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
    // Evaluate the expression to a Value; this should return a Symbolic Value with the address of
    // the target and an offset if it really has an address. If this fails this returns a different
    // Value class.
    // TODO handle possible returns
    return ImmutableList.of(SMGStateAndOptionalSMGObjectAndOffset.of(state));
  }

  @Override
  protected List<SMGStateAndOptionalSMGObjectAndOffset> visitDefault(CExpression pExp)
      throws CPATransferException {
    // Just get a default value and log
    logger.logf(
        Level.INFO,
        "%s, Default value: CExpression %s could not find an address for %s. Related CFAEdge: %s",
        cfaEdge.getFileLocation(),
        pExp,
        state,
        cfaEdge.getRawStatement());
    return ImmutableList.of(SMGStateAndOptionalSMGObjectAndOffset.of(state));
  }

  @Override
  public List<SMGStateAndOptionalSMGObjectAndOffset> visit(CStringLiteralExpression e)
      throws CPATransferException {
    String globalVarName = evaluator.getCStringLiteralExpressionVairableName(e);
    SMGState currentState = state;
    if (!currentState.isGlobalVariablePresent(globalVarName)) {
      Value sizeOfString =
          new NumericValue(evaluator.getBitSizeof(currentState, e.getExpressionType()));
      currentState =
          currentState.copyAndAddGlobalVariable(sizeOfString, globalVarName, e.getExpressionType());
      List<SMGState> statesWithString =
          evaluator.handleStringInitializer(
              currentState,
              null,
              cfaEdge,
              globalVarName,
              new NumericValue(BigInteger.ZERO),
              e.getExpressionType(),
              cfaEdge.getFileLocation(),
              e);
      Preconditions.checkArgument(statesWithString.size() == 1);
      currentState = statesWithString.get(0);
      // throw new SMGException("Could not find C String literal address.");
    }
    // TODO: assertion that the Strings are immutable
    ValueAndSMGState addressValueAndState =
        evaluator.createAddressForLocalOrGlobalVariable(globalVarName, currentState);
    Value addressValue = addressValueAndState.getValue();
    currentState = addressValueAndState.getState();

    return currentState.dereferencePointer(addressValue);
  }

  @Override
  public List<SMGStateAndOptionalSMGObjectAndOffset> visit(CArraySubscriptExpression e)
      throws CPATransferException {
    // Array subscript is default Java array usage. Example: array[5]
    // In C this can be translated to *(array + 5), but the array may be on the stack/heap (or
    // global, but we throw global and stack together when reading). Note: this is commutative!

    // The expression is split into array and subscript expression
    // Use the array expression in the visitor again to get the array address
    // The type of the arrayExpr may be pointer or array, depending on stack/heap
    CExpression arrayExpr = e.getArrayExpression();
    CExpression subscriptExpr = e.getSubscriptExpression();
    ImmutableList.Builder<SMGStateAndOptionalSMGObjectAndOffset> resultBuilder =
        ImmutableList.builder();

    for (ValueAndSMGState arrayValueAndState :
        arrayExpr.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger, options))) {
      Value arrayValue = arrayValueAndState.getValue();
      SMGState currentState = arrayValueAndState.getState();

      if (arrayValue.isUnknown()) {
        resultBuilder.add(SMGStateAndOptionalSMGObjectAndOffset.of(currentState));
        continue;
      }

      // Evaluate the subscript as far as possible
      for (ValueAndSMGState subscriptValueAndState :
          subscriptExpr.accept(
              new SMGCPAValueVisitor(evaluator, currentState, cfaEdge, logger, options))) {

        Value subscriptValue = subscriptValueAndState.getValue();
        currentState = subscriptValueAndState.getState();
        // If the subscript is an unknown value, we can't read anything and return unknown
        if (!subscriptValue.isNumericValue() && !options.trackErrorPredicates()) {
          logger.log(
              Level.FINE,
              "A subscript value was found to be non concrete when trying to find a memory location"
                  + " of an array. No memory region could be returned.");
          resultBuilder.add(
              SMGStateAndOptionalSMGObjectAndOffset.of(
                  currentState.withUnknownOffsetMemoryAccess()));
          continue;
        }
        // Calculate the offset out of the subscript value and the type
        BigInteger typeSizeInBits = evaluator.getBitSizeof(currentState, e.getExpressionType());
        Value subscriptOffset = evaluator.multiplyBitOffsetValues(subscriptValue, typeSizeInBits);

        // Get the value from the array and return the value + state
        // (the is pointer check is needed because of nested subscript; i.e. array[1][1]; as if we
        // access array[1] first, we can see that the next type is CArray which makes only sense for
        // nested arrays -> it reads a pointer and returns it even if the type is not a pointer
        // expr)
        resultBuilder.addAll(
            handleSubscriptExpression(arrayValue, subscriptOffset, currentState, e));
      }
    }
    return resultBuilder.build();
  }

  /*
   * Get the object and offset from an (array) subscript expression with address arrayValue and the
   *  subscript offset in bits, possibly symbolic. Might return multiple states when assigning
   *  concrete offsets with a solver.
   */
  private List<SMGStateAndOptionalSMGObjectAndOffset> handleSubscriptExpression(
      Value arrayValue,
      Value subscriptOffset,
      SMGState pCurrentState,
      CArraySubscriptExpression expr)
      throws CPATransferException {

    if ((arrayValue instanceof AddressExpression arrayAddr)) {
      Value addrOffset = arrayAddr.getOffset();
      if (!addrOffset.isNumericValue()) {
        if (!options.trackErrorPredicates()) {
          logger.log(
              Level.FINE,
              "A offset value was found to be non concrete when trying to find a memory"
                  + " location in an array. No memory region could be returned.");
          return ImmutableList.of(
              SMGStateAndOptionalSMGObjectAndOffset.of(
                  pCurrentState.withUnknownOffsetMemoryAccess()));
        } else if (addrOffset instanceof SymbolicValue symOffset
            && options.isFindConcreteValuesForSymbolicOffsets()) {
          // Assign concrete values for the offset
          if (pCurrentState.isConcreteAssignmentFeasible(symOffset)) {
            return handleSubscriptExpressionWithConcreteAssignment(
                arrayValue, pCurrentState, expr, symOffset);
          }
          // Fallthrough for no constraints or no assignment possible
        }
      }
      Value finalOffset = evaluator.addBitOffsetValues(addrOffset, subscriptOffset);

      if (finalOffset instanceof SymbolicValue symOffset
          && options.trackErrorPredicates()
          && options.isFindConcreteValuesForSymbolicOffsets()) {
        // Assign concrete values for the offset
        if (pCurrentState.isConcreteAssignmentFeasible(symOffset)) {
          return handleSubscriptExpressionWithConcreteAssignment(
              arrayValue, pCurrentState, expr, symOffset);
        }
        // Fallthrough for no constraints or no assignment possible
      }

      List<SMGStateAndOptionalSMGObjectAndOffset> targets =
          evaluator.getTargetObjectAndOffset(
              pCurrentState, arrayAddr.getMemoryAddress(), finalOffset);
      Preconditions.checkArgument(targets.size() == 1);
      return targets;

    } else if (pCurrentState.getMemoryModel().isPointer(arrayValue)) {
      // Local array
      List<SMGStateAndOptionalSMGObjectAndOffset> maybeTargetMemoriesAndOffsets =
          pCurrentState.dereferencePointer(arrayValue);
      // If this ever fails, handle the list.
      Preconditions.checkArgument(maybeTargetMemoriesAndOffsets.size() == 1);
      SMGStateAndOptionalSMGObjectAndOffset maybeTargetMemoryAndOffset =
          maybeTargetMemoriesAndOffsets.get(0);
      if (!maybeTargetMemoryAndOffset.hasSMGObjectAndOffset()) {
        return maybeTargetMemoriesAndOffsets;
      }
      Value baseOffset = maybeTargetMemoryAndOffset.getOffsetForObject();
      if (baseOffset instanceof SymbolicValue symOffset
          && options.trackErrorPredicates()
          && options.isFindConcreteValuesForSymbolicOffsets()) {
        // Assign concrete values for the offset
        if (pCurrentState.isConcreteAssignmentFeasible(symOffset)) {
          return handleSubscriptExpressionWithConcreteAssignment(
              arrayValue, pCurrentState, expr, symOffset);
        }
        // Fallthrough for no constraints or no assignment possible
      }
      Value finalOffset = evaluator.addBitOffsetValues(baseOffset, subscriptOffset);

      if (finalOffset instanceof SymbolicValue symOffset
          && options.trackErrorPredicates()
          && options.isFindConcreteValuesForSymbolicOffsets()) {
        // Assign concrete values for the offset
        if (pCurrentState.isConcreteAssignmentFeasible(symOffset)) {
          return handleSubscriptExpressionWithConcreteAssignment(
              arrayValue, pCurrentState, expr, symOffset);
        }
        // Fallthrough for no constraints or no assignment possible
      }

      return ImmutableList.of(
          SMGStateAndOptionalSMGObjectAndOffset.of(
              maybeTargetMemoryAndOffset.getSMGObject(), finalOffset, pCurrentState));

    } else if (arrayValue instanceof SymbolicIdentifier localArrayValue
        && localArrayValue.getRepresentedLocation().isPresent()) {
      // Local array in a local structure (e.g. a struct)
      MemoryLocation memLoc = localArrayValue.getRepresentedLocation().orElseThrow();
      String qualifiedVarName = memLoc.getIdentifier();
      Value finalOffset =
          evaluator.addBitOffsetValues(subscriptOffset, BigInteger.valueOf(memLoc.getOffset()));

      if (finalOffset instanceof SymbolicValue symOffset) {
        if (!options.trackPredicates()) {
          throw new UnsupportedOperationException(
              "Symbolic array subscript access not supported by this analysis.");
        } else if (options.isFindConcreteValuesForSymbolicOffsets()) {
          // Assign concrete values for the offset
          if (pCurrentState.isConcreteAssignmentFeasible(symOffset)) {
            return handleSubscriptExpressionWithConcreteAssignment(
                arrayValue, pCurrentState, expr, symOffset);
          }
          // Fallthrough for no constraints or no assignment possible

        } else {
          throw new UnsupportedOperationException(
              "Missing case in SMGCPAAddressVisitor. Report to CPAchecker issue tracker for SMG2"
                  + " analysis. Missing symbolic handling of an array subscript expression");
        }
      }

      Optional<SMGObjectAndOffsetMaybeNestingLvl> maybeTarget =
          evaluator.getTargetObjectAndOffset(pCurrentState, qualifiedVarName, finalOffset);

      return ImmutableList.of(SMGStateAndOptionalSMGObjectAndOffset.of(pCurrentState, maybeTarget));

    } else {
      // Might be numeric 0 (0 object). All else cases are basically invalid requests.
      if (arrayValue.isNumericValue()
          && arrayValue.asNumericValue().bigIntegerValue().compareTo(BigInteger.ZERO) == 0) {
        return ImmutableList.of(
            SMGStateAndOptionalSMGObjectAndOffset.of(
                SMGObject.nullInstance(), subscriptOffset, pCurrentState));
      } else {
        return ImmutableList.of(SMGStateAndOptionalSMGObjectAndOffset.of(pCurrentState));
      }
    }
  }

  private List<SMGStateAndOptionalSMGObjectAndOffset>
      handleSubscriptExpressionWithConcreteAssignment(
          Value arrayValue,
          SMGState pCurrentState,
          CArraySubscriptExpression exprCurrentlyUnderEval,
          SymbolicValue symOffsetToAssign)
          throws CPATransferException {
    options.incConcreteValueForSymbolicOffsetsAssignmentMaximum();
    Optional<SymbolicValue> maybeVariableToAssign =
        pCurrentState.isVariableAssignmentFeasible(symOffsetToAssign);

    if (maybeVariableToAssign.isPresent()) {
      SymbolicValue variableToAssign = maybeVariableToAssign.orElseThrow();
      List<SMGStateAndOptionalSMGObjectAndOffset> assignedStates =
          pCurrentState
              .assignConcreteValuesForSymbolicValuesAndReevaluateExpressionInAddressVisitor(
                  variableToAssign, exprCurrentlyUnderEval, cfaEdge);
      options.decConcreteValueForSymbolicOffsetsAssignmentMaximum();
      return assignedStates;
    } else {
      // Assignment using the solver only. While we get the concrete value here,
      //  we can't assign it to a variable.
      List<ValueAndSMGState> assignedResults =
          pCurrentState.findValueAssignmentsWithSolver(symOffsetToAssign, cfaEdge);
      ImmutableList.Builder<SMGStateAndOptionalSMGObjectAndOffset> concreteSubscriptHandling =
          ImmutableList.builder();
      for (ValueAndSMGState assignedValueAndState : assignedResults) {

        List<SMGStateAndOptionalSMGObjectAndOffset> assignedAndEvaluatedStates =
            handleSubscriptExpression(
                arrayValue,
                assignedValueAndState.getValue(),
                assignedValueAndState.getState(),
                exprCurrentlyUnderEval);
        if (options.isMemoryErrorTarget()) {
          for (SMGStateAndOptionalSMGObjectAndOffset assignedAndEvaldState :
              assignedAndEvaluatedStates) {
            if (assignedAndEvaldState.getSMGState().hasMemoryErrors()) {
              options.decConcreteValueForSymbolicOffsetsAssignmentMaximum();
              return ImmutableList.of(assignedAndEvaldState);
            }
          }
        }
        concreteSubscriptHandling.addAll(assignedAndEvaluatedStates);
      }
      options.decConcreteValueForSymbolicOffsetsAssignmentMaximum();
      return concreteSubscriptHandling.build();
    }
  }

  @Override
  public List<SMGStateAndOptionalSMGObjectAndOffset> visit(CCastExpression e)
      throws CPATransferException {
    return e.getOperand().accept(this);
  }

  @Override
  public List<SMGStateAndOptionalSMGObjectAndOffset> visit(CFieldReference e)
      throws CPATransferException {
    // This is the field of a struct/union, so something like struct.field or struct->field.
    // In the later case it's a pointer dereference.
    // Get the object of the field with its offset

    // First we transform x->f into (*x).f per default
    CFieldReference explicitReference = e.withExplicitPointerDereference();

    // Owner expression; the struct/union with this field. Use this to get the address of the
    // general object.
    CExpression ownerExpression = explicitReference.getFieldOwner();
    ImmutableList.Builder<SMGStateAndOptionalSMGObjectAndOffset> resultBuilder =
        ImmutableList.builder();
    for (ValueAndSMGState structValuesAndState :
        ownerExpression.accept(
            new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger, options))) {
      // This value is either an AddressValue for pointers i.e. (*struct).field or a general
      // SymbolicValue
      Value structValue = structValuesAndState.getValue();
      SMGState currentState = structValuesAndState.getState();
      if (structValue.isUnknown()) {
        resultBuilder.add(SMGStateAndOptionalSMGObjectAndOffset.of(currentState));
        continue;
      }

      // Now get the offset of the current field
      BigInteger fieldOffset =
          evaluator.getFieldOffsetInBits(
              SMGCPAExpressionEvaluator.getCanonicalType(ownerExpression),
              explicitReference.getFieldName());

      if (structValue instanceof AddressExpression structAddr) {
        Value addrOffset = structAddr.getOffset();
        if (!addrOffset.isNumericValue() && !options.trackErrorPredicates()) {
          // Non numeric offset -> not usable
          resultBuilder.add(SMGStateAndOptionalSMGObjectAndOffset.of(currentState));
        }

        Value finalFieldOffset = evaluator.addBitOffsetValues(addrOffset, fieldOffset);

        resultBuilder.addAll(
            evaluator.getTargetObjectAndOffset(
                currentState, structAddr.getMemoryAddress(), finalFieldOffset));

      } else if (structValue instanceof SymbolicIdentifier symbolicIdentifier
          && symbolicIdentifier.getRepresentedLocation().isPresent()) {
        MemoryLocation variableAndOffset =
            symbolicIdentifier.getRepresentedLocation().orElseThrow();
        String varName = variableAndOffset.getIdentifier();
        Value baseOffset = new NumericValue(BigInteger.valueOf(variableAndOffset.getOffset()));
        Value finalFieldOffset = evaluator.addBitOffsetValues(baseOffset, fieldOffset);

        Optional<SMGObjectAndOffsetMaybeNestingLvl> maybeTarget =
            evaluator.getTargetObjectAndOffset(currentState, varName, finalFieldOffset);

        resultBuilder.add(SMGStateAndOptionalSMGObjectAndOffset.of(currentState, maybeTarget));

      } else {
        // Might be numeric 0 (0 object). All else cases are basically invalid requests.
        if (structValue.isNumericValue()
            && structValue.asNumericValue().bigIntegerValue().compareTo(BigInteger.ZERO) == 0) {
          resultBuilder.add(
              SMGStateAndOptionalSMGObjectAndOffset.of(
                  SMGObject.nullInstance(), new NumericValue(fieldOffset), currentState));
        } else {
          resultBuilder.add(SMGStateAndOptionalSMGObjectAndOffset.of(currentState));
        }
      }
    }
    return resultBuilder.build();
  }

  @Override
  public List<SMGStateAndOptionalSMGObjectAndOffset> visit(CIdExpression e)
      throws CPATransferException {
    CSimpleDeclaration varDecl = e.getDeclaration();
    if (varDecl == null) {
      // The var was not declared
      throw new SMGException("Usage of undeclared variable: " + e.getName() + ".");
    }
    Optional<SMGObjectAndOffsetMaybeNestingLvl> maybeTarget =
        evaluator.getTargetObjectAndOffset(state, varDecl.getQualifiedName());
    if (maybeTarget.isPresent()) {
      return ImmutableList.of(
          SMGStateAndOptionalSMGObjectAndOffset.of(
              maybeTarget.orElseThrow().getSMGObject(),
              maybeTarget.orElseThrow().getOffsetForObject(),
              state));
    } else {
      return ImmutableList.of(SMGStateAndOptionalSMGObjectAndOffset.of(state));
    }
  }

  @Override
  public List<SMGStateAndOptionalSMGObjectAndOffset> visit(CPointerExpression e)
      throws CPATransferException {
    // This should sub-evaluate to an AddressExpression in the visit call in the beginning as we
    // always evaluate to the address

    // Get the type of the target
    CType type = SMGCPAExpressionEvaluator.getCanonicalType(e.getExpressionType());
    // Get the expression that is dereferenced
    CExpression expr = e.getOperand();
    // Evaluate the expression to a Value; this should return a Symbolic Value with the address of
    // the target and an offset. If this fails this returns an UnknownValue.

    ImmutableList.Builder<SMGStateAndOptionalSMGObjectAndOffset> resultBuilder =
        ImmutableList.builder();
    for (ValueAndSMGState evaluatedSubExpr :
        expr.accept(new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger, options))) {
      SMGState currentState = evaluatedSubExpr.getState();
      // Try to disassemble the values (AddressExpression)
      Value value = evaluatedSubExpr.getValue();
      if (!(value instanceof AddressExpression pointerValue)) {
        resultBuilder.add(SMGStateAndOptionalSMGObjectAndOffset.of(currentState));
        continue;
      }

      // The offset part of the pointer; its either numeric or we can't get a concrete value
      Value offset = pointerValue.getOffset();
      if (!offset.isNumericValue() && !options.trackErrorPredicates()) {
        // If the offset is not numerically known we can't read a value, return
        resultBuilder.add(SMGStateAndOptionalSMGObjectAndOffset.of(currentState));
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
        resultBuilder.addAll(
            evaluator.getTargetObjectAndOffset(
                currentState, pointerValue.getMemoryAddress(), offset));
      }
    }
    return resultBuilder.build();
  }
}
