// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.util.SMGAndHasValueEdges;

public final class SMGStateToCExpressionVisitor
    implements CTypeVisitor<Set<AExpression>, NoException> {

  private final SMGState state;

  // Current memory that we work on
  private final SMGObject currentMemoryObject;

  // Current offset in currentMemoryObject. This needs to be used when accessing currentMemoryObject
  private final BigInteger currentOffsetInMemoryObjectInBits;

  // Current left-hand-side for the assignment
  private final CExpression leftHandSide;

  // All SMGObjects that we resolved before. If we find them again, we have a loop and can abort.
  private final ImmutableSet<SMGObject> visitedObjects;

  // We always resolve possible value assignments, and if true, we additionally resolve invariants.
  private final boolean resolveInvariants;

  private final MachineModel machineModel;
  private final SMGCPAExpressionEvaluator evaluator;
  private final LogManagerWithoutDuplicates logger;
  private final Map<SymbolicIdentifier, Value> smtModelAssignments;
  private final FileLocation fileLocation;

  private SMGStateToCExpressionVisitor(
      SMGState pState,
      SMGObject pObject,
      BigInteger pCurrentOffsetInMemoryObjectInBits,
      CExpression pLeftHandSide,
      SMGCPAExpressionEvaluator pEvaluator,
      LogManagerWithoutDuplicates pLogger,
      ImmutableSet<SMGObject> pVisitedObjects,
      Map<SymbolicIdentifier, Value> pCurrentModel,
      FileLocation pFileLocation,
      final boolean pResolveInvariants) {
    state = pState;
    currentMemoryObject = pObject;
    currentOffsetInMemoryObjectInBits = pCurrentOffsetInMemoryObjectInBits;
    leftHandSide = pLeftHandSide;
    machineModel = pState.getMachineModel();
    evaluator = pEvaluator;
    logger = pLogger;
    visitedObjects = pVisitedObjects;
    smtModelAssignments = pCurrentModel;
    fileLocation = pFileLocation;
    resolveInvariants = pResolveInvariants;
  }

  private SMGStateToCExpressionVisitor getUpdatedVisitor(
      SMGObject newTargetObject,
      BigInteger newOffsetInTargetMemoryInBits,
      CExpression newLeftHandSide,
      ImmutableSet<SMGObject> newVisitedObjects) {
    return new SMGStateToCExpressionVisitor(
        state,
        newTargetObject,
        newOffsetInTargetMemoryInBits,
        newLeftHandSide,
        evaluator,
        logger,
        newVisitedObjects,
        smtModelAssignments,
        fileLocation,
        resolveInvariants);
  }

  /**
   * Resolves value assignments, and invariants if requested, for the given variable and the memory
   * reachable from it.
   *
   * @param variableRenamingFunction renames the name and original name, but not the qualified name,
   *     of the given variable in the returned expressions, e.g., {@code x} to {@code \old(x)}.
   */
  public static Set<AExpression> resolveAssignmentsAndInvariantsForVariable(
      final CVariableDeclaration variableDeclaration,
      final SMGObject memoryObjectOfVariable,
      final CType variableType,
      final SMGState state,
      final SMGCPAExpressionEvaluator evaluator,
      final LogManagerWithoutDuplicates logger,
      final Map<SymbolicIdentifier, Value> modelAssignments,
      final FileLocation fileLocation,
      final boolean resolveInvariants,
      final Function<String, String> variableRenamingFunction) {

    checkNotNull(memoryObjectOfVariable);
    checkNotNull(variableType);
    checkNotNull(state);
    checkNotNull(evaluator);
    checkNotNull(logger);
    checkNotNull(modelAssignments);
    checkNotNull(fileLocation);
    checkNotNull(variableDeclaration);
    checkNotNull(variableRenamingFunction);
    checkArgument(!variableDeclaration.getQualifiedName().isEmpty());
    checkArgument(
        variableDeclaration.getType().getCanonicalType().equals(variableType.getCanonicalType()));

    CVariableDeclaration renamedVariableDeclaration =
        new CVariableDeclaration(
            variableDeclaration.getFileLocation(),
            variableDeclaration.isGlobal(),
            variableDeclaration.getCStorageClass(),
            variableDeclaration.getType(),
            variableRenamingFunction.apply(variableDeclaration.getName()),
            variableRenamingFunction.apply(variableDeclaration.getOrigName()),
            variableDeclaration.getQualifiedName(),
            variableDeclaration.getInitializer(),
            variableDeclaration.hasThreadLocalStorage());
    CExpression leftHandSide = new CIdExpression(fileLocation, renamedVariableDeclaration);

    SMGStateToCExpressionVisitor initialVisitor =
        new SMGStateToCExpressionVisitor(
            state,
            memoryObjectOfVariable,
            BigInteger.ZERO,
            leftHandSide,
            evaluator,
            logger,
            ImmutableSet.of(),
            modelAssignments,
            fileLocation,
            resolveInvariants);

    return variableType.getCanonicalType().accept(initialVisitor);
  }

  @Override
  public Set<AExpression> visit(CSimpleType pType) {
    Optional<ImmutableSet<SMGObject>> currentlyVisitedObjects = checkSMGObjectAccessForLoop();
    if (currentlyVisitedObjects.isEmpty()) {
      return ImmutableSet.of();
    }

    if (pType.hasComplexSpecifier() || pType.hasImaginarySpecifier()) {
      // TODO: Handle Complex and Imaginary type values
      throw new UnsupportedOperationException("Complex and imaginary types are not supported");
    }

    Optional<NumericValue> value =
        resolveSMGValueInCurrentMemoryAndOffsetFor(pType)
            .flatMap(this::getNumericValueAssignmentFromSMGValue);

    if (value.isEmpty()) {
      return ImmutableSet.of();
    }

    Optional<CLiteralExpression> maybeLiteral =
        transformNumericValueToCLiteral(pType, value.orElseThrow());
    if (maybeLiteral.isPresent()) {

      return ImmutableSet.of(
          new CBinaryExpressionBuilder(machineModel, logger)
              .buildBinaryExpressionUnchecked(
                  leftHandSide, maybeLiteral.orElseThrow(), BinaryOperator.EQUALS));
    }

    // TODO: Handle any additional simple C types.
    logger.logOnce(
        Level.FINE,
        "Could not create C literal from type " + pType + " with value " + value.orElseThrow());
    return ImmutableSet.of();
  }

  @Override
  public Set<AExpression> visit(CPointerType pType) {
    Optional<ImmutableSet<SMGObject>> currentlyVisitedObjects = checkSMGObjectAccessForLoop();

    if (currentlyVisitedObjects.isEmpty()) {
      return ImmutableSet.of();
    }

    Optional<SMGValue> value = resolveSMGValueInCurrentMemoryAndOffsetFor(pType);
    if (value.isEmpty()) {
      return ImmutableSet.of();
    }

    var smg = state.getMemoryModel().getSmg();
    SMGValue pointerValue = value.orElseThrow();

    if (!smg.isPointer(pointerValue)) {
      return ImmutableSet.of();
    }

    var pointsToEdge = smg.getPTEdge(pointerValue).orElseThrow();
    if (!pointsToEdge.targetSpecifier().isRegion()) {
      if (!resolveInvariants) {
        return ImmutableSet.of();
      }
      // TODO: Add handling using ACSL expressions for linked-list abstractions
      throw new UnsupportedOperationException(
          "Linked-list abstractions cannot be exported currently");
    }

    if (pointsToEdge.pointsTo().isZero()) {
      // Return numeric zero assignment
      CLiteralExpression literalZero =
          transformNumericValueToCLiteral(pType, new NumericValue(BigInteger.ZERO)).orElseThrow();
      // This uses the old left-hand-side, and is NOT allowed to be dereferenced!
      return ImmutableSet.of(
          new CBinaryExpressionBuilder(machineModel, logger)
              .buildBinaryExpressionUnchecked(leftHandSide, literalZero, BinaryOperator.EQUALS));
    }

    if (!(pointsToEdge.getOffset() instanceof NumericValue numericOffset)
        || !numericOffset.hasIntegerType()) {
      // We can not resolve symbolic offsets currently, and never non int type offsets
      // TODO: try ValueAssignments before giving up
      return ImmutableSet.of();
    }

    BigInteger newTargetOffsetInBits = numericOffset.bigIntegerValue();

    CExpression newLeftHandSide;
    if (newTargetOffsetInBits.equals(BigInteger.ZERO)) {
      newLeftHandSide = new CPointerExpression(fileLocation, pType.getType(), leftHandSide);
    } else {
      // Since we always remember the offset and not the pointer, we need to take this into account!
      // TODO: handle
      return ImmutableSet.of();
    }

    CType targetType = pType.getType().getCanonicalType();
    return targetType.accept(
        getUpdatedVisitor(
            pointsToEdge.pointsTo(),
            newTargetOffsetInBits,
            newLeftHandSide,
            currentlyVisitedObjects.orElseThrow()));
  }

  @Override
  public Set<AExpression> visit(CEnumType pType) {
    return ImmutableSet.of();
  }

  @Override
  public Set<AExpression> visit(CArrayType pType) {
    return ImmutableSet.of();
  }

  // Union or Struct
  @Override
  public Set<AExpression> visit(CCompositeType pType) {
    if (checkSMGObjectAccessForLoop().isEmpty()) {
      return ImmutableSet.of();
    }

    if (pType.getKind() == ComplexTypeKind.UNION) {
      // TODO:
      return ImmutableSet.of();
    }

    if (!currentOffsetInMemoryObjectInBits.equals(BigInteger.ZERO)) {
      // Base offsets + structs are more complex and extremely rare!
      return ImmutableSet.of();
    }

    ImmutableSet.Builder<AExpression> result = ImmutableSet.builder();
    for (Entry<CCompositeTypeMemberDeclaration, BigInteger> memberAndOffset :
        machineModel.getAllFieldOffsetsInBits(pType.getCanonicalType()).entrySet()) {
      BigInteger offsetInBits = memberAndOffset.getValue();
      BigInteger newOffsetInMemoryObjectInBits =
          currentOffsetInMemoryObjectInBits.add(offsetInBits);

      CCompositeTypeMemberDeclaration memberTypeDecl = memberAndOffset.getKey();
      CType accessedNestedType = memberTypeDecl.getType().getCanonicalType();
      // We always resolve field references as either struct.field or (*ptr).field here
      final CExpression newLeftHandSide =
          new CFieldReference(
              fileLocation,
              memberTypeDecl.getType(),
              memberTypeDecl.getName(),
              leftHandSide,
              false);

      // The members are in the same memory object, so they are not a loop and get the objects
      // visited before this object
      result.addAll(
          accessedNestedType.accept(
              getUpdatedVisitor(
                  currentMemoryObject,
                  newOffsetInMemoryObjectInBits,
                  newLeftHandSide,
                  visitedObjects)));
    }
    return result.build();
  }

  @Override
  public Set<AExpression> visit(CTypedefType pType) {
    return pType.getRealType().getCanonicalType().accept(this);
  }

  @Override
  public Set<AExpression> visit(CElaboratedType pType) {
    return ImmutableSet.of();
  }

  @Override
  public Set<AExpression> visit(CBitFieldType pType) {
    return ImmutableSet.of();
  }

  @Override
  public Set<AExpression> visit(CFunctionType pType) {
    return ImmutableSet.of();
  }

  @Override
  public Set<AExpression> visit(CProblemType pType) {
    return ImmutableSet.of();
  }

  @Override
  public Set<AExpression> visit(CVoidType pType) {
    return ImmutableSet.of();
  }

  /* ##################################### Helper Methods ##################################### */

  /**
   * Checks the current object at the start of every visit. Empty means a loop. The set retains all
   * visited SMGObjects and should be passed to subsequent evaluations.
   */
  private Optional<ImmutableSet<SMGObject>> checkSMGObjectAccessForLoop() {
    if (visitedObjects.contains(currentMemoryObject)) {
      return Optional.empty();
    }
    return Optional.of(
        ImmutableSet.<SMGObject>builder().addAll(visitedObjects).add(currentMemoryObject).build());
  }

  /**
   * Resolves the SMGValue present in the current memory object, starting at the current offset for
   * the size of the given type if possible.
   */
  private Optional<SMGValue> resolveSMGValueInCurrentMemoryAndOffsetFor(CType pType) {
    SymbolicProgramConfiguration spc = state.getMemoryModel();

    if (!spc.isObjectValid(currentMemoryObject)) {
      return Optional.empty();
    }

    if (!pType.hasKnownConstantSize()) {
      return Optional.empty();
    }

    BigInteger sizeInBits = machineModel.getSizeofInBits(pType);

    // We ignore the SMG update here, as it would only reflect new symbolic edges (which we ignore)
    // or cutting down of zero edges (which we can also ignore)
    SMGAndHasValueEdges readValue =
        spc.getSmg()
            .readValue(currentMemoryObject, currentOffsetInMemoryObjectInBits, sizeInBits, false);

    if (readValue.getHvEdges().size() == 1) {
      return Optional.of(readValue.getHvEdges().getFirst().hasValue());
    }

    return Optional.empty();
  }

  /**
   * Resolves SMGValues to NumericValues in the current SMGState. Resolves symbolic values to
   * NumericValues using the supplied SMT solver model (ValueAssignments) present in the visitor.
   */
  private Optional<NumericValue> getNumericValueAssignmentFromSMGValue(SMGValue pValue) {

    Optional<Value> maybeValue = state.getMemoryModel().getValueFromSMGValue(pValue);
    if (maybeValue.isEmpty()) {
      return Optional.empty();
    }

    return state.applySolverAssignment(maybeValue.orElseThrow(), smtModelAssignments);
  }

  private Optional<CLiteralExpression> transformNumericValueToCLiteral(
      final CType typeOfValue, final NumericValue numericValue) {

    if (numericValue.hasFloatType()) {
      // The NumericValue is a float. Check that the CType matches.
      if (typeOfValue instanceof CSimpleType simpleType
          && simpleType.getType().isFloatingPointType()) {
        FloatValue.Format floatFormatFromType =
            FloatValue.Format.fromCType(machineModel, simpleType);
        assert floatFormatFromType.equals(numericValue.getFloatValue().getFormat());

        return Optional.of(
            new CFloatLiteralExpression(
                fileLocation, machineModel, typeOfValue, numericValue.getFloatValue()));
      }

    } else {
      // Integer type NumericValue (e.g. a pointer that is 0, or an integer variable value)
      if (typeOfValue instanceof CPointerType
          || (typeOfValue instanceof CSimpleType simpleType
              && simpleType.getType().isIntegerType())) {
        BigInteger intNumericValue = numericValue.bigIntegerValue();
        assert !(typeOfValue instanceof CSimpleType simpleType)
            || (intNumericValue.compareTo(machineModel.getMinimalIntegerValue(simpleType)) >= 0
                && intNumericValue.compareTo(machineModel.getMaximalIntegerValue(simpleType)) <= 0);

        return Optional.of(
            new CIntegerLiteralExpression(fileLocation, typeOfValue, intNumericValue));
      }
    }

    logger.logOnce(
        Level.FINE,
        "Could not create C literal from type " + typeOfValue + " with value " + numericValue);
    return Optional.empty();
  }
}
