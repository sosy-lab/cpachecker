// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Verify.verify;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.Constraints;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentFormulaHandler.AssignmentConversionType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentFormulaHandler.AssignmentOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.SliceVariable;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Handles memory manipulation functions {@code memset}, {@code memcpy}, {@copy memmove}. These
 * functions perform assignments to/from memory blocks.
 *
 * <p>The memory functions are encoded into slice assignments internally.
 *
 * @see SliceExpression
 * @see AssignmentHandler
 */
class MemoryManipulationFunctionHandler {

  private final CToFormulaConverterWithPointerAliasing conv;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final CFAEdge edge;
  private final String function;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;
  private final MemoryRegionManager regionMgr;

  /** Machine model pointer-equivalent size type, retained here for conciseness. */
  private final CType sizeType;

  /**
   * Creates a new MemoryFunctionHandler.
   *
   * @param pConv The C to SMT formula converter.
   * @param pEdge The current edge of the CFA (for logging purposes).
   * @param pFunction The name of the current function.
   * @param pSsa The SSA map.
   * @param pPts The underlying set of pointer targets.
   * @param pConstraints Additional constraints.
   * @param pErrorConditions Additional error conditions.
   */
  MemoryManipulationFunctionHandler(
      CToFormulaConverterWithPointerAliasing pConv,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions,
      MemoryRegionManager pRegionMgr) {
    conv = pConv;

    typeHandler = pConv.typeHandler;

    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    pts = pPts;
    constraints = pConstraints;
    errorConditions = pErrorConditions;
    regionMgr = pRegionMgr;

    sizeType = conv.machineModel.getPointerEquivalentSimpleType();
  }

  /**
   * This method handles memory manipulation functions {@code memset}, {@code memcpy}, {@copy
   * memmove} with arguments given by {@link CFunctionCallExpression}.
   *
   * @param functionName The function name. Must be {@code memset}, {@code memcpy}, or {@copy
   *     memmove}.
   * @param functionCall Function call expression which should be handled.
   * @return The return value of function, which is always the destination (first) parameter of the
   *     function.
   * @throws IllegalArgumentException If the function name is not that of a memory manipulation
   *     function.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during handling.
   */
  CExpression handleMemoryAssignmentFunction(
      final String functionName, final CFunctionCallExpression functionCall)
      throws UnrecognizedCodeException, InterruptedException {
    if (!conv.options.enableMemoryAssignmentFunctions()) {
      throw new UnrecognizedCodeException(
          "Memory assignment function present but their handling is disabled", functionCall);
    }

    // all of the functions have exactly three arguments
    // the first and third argument is the same for all functions
    final List<CExpression> arguments = functionCall.getParameterExpressions();
    verify(arguments.size() == 3);

    // TODO: make sure that the destination is flagged not to be ignored
    // for testing, this can be kludged by cpa.predicate.ignoreIrrelevantVariables=false
    CExpression destination = arguments.get(0);
    final CExpression secondArgument = arguments.get(1);
    final CExpression sizeInBytes = arguments.get(2);

    // Handover to function-specific code
    try {
      if (functionName.equals("memset")) {
        handleMemsetFunction(destination, secondArgument, sizeInBytes);
      } else if (functionName.equals("memcpy") || functionName.equals("memmove")) {
        // memcpy and memmove only differ in that memcpy is not well-defined if destination and
        // source overlap; we do not model this

        handleMemmoveFunction(destination, secondArgument, sizeInBytes);
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Unexpected function name '%s' in memory manipulation function processing",
                functionName));
      }
    } catch (UnrecognizedCodeException uce) {
      if (conv.options.ignoreUnrecognizedCodeInMemoryAssignmentFunctions()) {
        conv.logger.logfOnce(
            Level.WARNING,
            "Ignoring function %s with unrecognized code: %s",
            functionName,
            uce.getMessage());
      } else {
        throw uce;
      }
    }

    // return destination parameter
    return destination;
  }

  /**
   * Handles the {@code memmove} function. Can be also used for the {@code memcpy} function if we do
   * not encode the requirement that the source and destination memory blocks may not overlap.
   *
   * @param destination Destination argument.
   * @param source Source argument.
   * @param sizeInBytes Size argument, given in bytes.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during handling.
   */
  private void handleMemmoveFunction(
      final CExpression destination, final CExpression source, final CExpression sizeInBytes)
      throws UnrecognizedCodeException, InterruptedException {

    // process the pointer parameters to remove void* casts and remake multidimensional arrays into
    // single-dimension arrays
    final CExpression processedDestination = processPointerLikeArgument(destination, true);
    final CExpression processedSource = processPointerLikeArgument(source, false);

    // make sure both arguments have the same underlying sizes, otherwise, it is not possible to
    // assign
    // them by reinterpretation
    final CType destinationType = typeHandler.getSimplifiedType(processedDestination);
    final CPointerType adjustedDestinationType =
        (CPointerType)
            CTypes.adjustFunctionOrArrayType(destinationType);

    final CType sourceType = typeHandler.getSimplifiedType(processedSource);
    CPointerType adjustedSourceType =
        (CPointerType)
            CTypes.adjustFunctionOrArrayType(sourceType);

    final long underlyingDestinationBitSize = typeHandler.getBitSizeof(adjustedDestinationType.getType());
    final long underlyingSourceBitSize = typeHandler.getBitSizeof(adjustedSourceType.getType());

    if (underlyingDestinationBitSize != underlyingSourceBitSize) {
      throw new UnrecognizedCodeException(
          String.format(
              "Cannot assign between differently-sized elements of destination %s and source %s",
              destinationType, sourceType),
          edge);
    }

    // compute the size in elements
    // note that this is not completely precise as it treats fractional element assignment
    // as full element assignment; however, reinterpreting to bytes would not work correctly
    // with non-byte heaps
    final CExpression sizeInElements =
        convertSizeInBytesToSizeInElements(sizeInBytes, adjustedDestinationType);

    // the memcopy just indexes the destination and source with the same index
    // i.e. lhs[i] = rhs[i] for 0 <= i < sizeInElements
    SliceVariable sliceIndex = new SliceVariable(sizeInElements);
    SliceExpression lhs = new SliceExpression(processedDestination).withIndex(sliceIndex);
    SliceExpression rhs = new SliceExpression(processedSource).withIndex(sliceIndex);

    // reinterpret instead of casting to handle differently-typed but same-element-size arrays
    // (e.g. memmove from int* to float*)
    // force quantifiers if the option is set
    AssignmentOptions assignmentOptions =
        new AssignmentOptions(
            false,
            AssignmentConversionType.REINTERPRET,
            conv.options.forceQuantifiersInMemoryAssignmentFunctions(),
            false);
    AssignmentHandler assignmentHandler =
        new AssignmentHandler(
            conv,
            edge,
            function,
            ssa,
            pts,
            constraints,
            errorConditions,
            regionMgr,
            assignmentOptions);

    // construct the slice assignment
    // TODO: add relevancy checking
    AssignmentHandler.SliceAssignment sliceAssignment =
        new AssignmentHandler.SliceAssignment(lhs, Optional.empty(), Optional.of(rhs));

    // assign and add as a constraint
    BooleanFormula assignmentFormula = assignmentHandler.assign(ImmutableList.of(sliceAssignment));
    constraints.addConstraint(assignmentFormula);
  }

  /**
   * Handles the {@code memset} function.
   *
   * @param destination Destination argument.
   * @param sizeInBytes Size argument, given in bytes.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during handling.
   */
  private void handleMemsetFunction(
      final CExpression destination, final CExpression setValue, final CExpression sizeInBytes)
      throws UnrecognizedCodeException, InterruptedException {

    // process destination
    CExpression processedDestination = processPointerLikeArgument(destination, true);

    // compute the size in elements
    // note that this is not completely precise as it treats fractional element assignment
    // as full element assignment; however, reinterpreting to bytes would not work correctly
    // with non-byte heaps
    CType destinationType = processedDestination.getExpressionType().getCanonicalType();
    CExpression sizeInElements = convertSizeInBytesToSizeInElements(sizeInBytes, destinationType);

    // slice LHS, i.e. lhs[i] = ...
    SliceVariable sliceIndex = new SliceVariable(sizeInElements);
    SliceExpression lhs = new SliceExpression(processedDestination).withIndex(sliceIndex);

    // the assignments must be generated recursively, as we have to assign the correct value to set
    // to every simple type
    List<AssignmentHandler.SliceAssignment> assignments = new ArrayList<>();
    generateMemsetAssignments(lhs, setValue, assignments);

    // reinterpret instead of casting in assignment to properly handle memset of floats
    // force quantifiers if the option is set
    AssignmentOptions assignmentOptions =
        new AssignmentOptions(
            false,
            AssignmentConversionType.REINTERPRET,
            conv.options.forceQuantifiersInMemoryAssignmentFunctions(),
            false);
    AssignmentHandler assignmentHandler =
        new AssignmentHandler(
            conv,
            edge,
            function,
            ssa,
            pts,
            constraints,
            errorConditions,
            regionMgr,
            assignmentOptions);

    // assign and add as a constraint
    BooleanFormula assignmentFormula = assignmentHandler.assign(assignments);
    constraints.addConstraint(assignmentFormula);
  }

  /**
   * Generates memset assignments recursively. For each simple type, an appropriate value to set is
   * generated.
   *
   * <p>The resulting assignments are in form {@code lhs[i].field = field_appropriate_set_value;}
   *
   * @param lhs Left-hand slice expression.
   * @param setValue Value to set to every byte after being interpreted as unsigned char.
   * @param assignments A mutable list of assignments that the generated assignments will be added
   *     to.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   * @throws InterruptedException If a shutdown was requested during handling.
   */
  private void generateMemsetAssignments(
      final SliceExpression lhs,
      final CExpression setValue,
      final List<AssignmentHandler.SliceAssignment> assignments)
      throws UnrecognizedCodeException, InterruptedException {

    final CType lhsType = lhs.getFullExpressionType();

    if (lhsType instanceof CArrayType arrayType) {
      // for arrays, generate memset assignments for all of their elements
      // by adding a new array-length slice index to lhs

      final CExpression length = arrayType.getLength();
      if (length == null) {
        throw new UnrecognizedCodeException(
            "Unexpected flexible-array-member memset destination", edge);
      }
      final SliceVariable arrayIndex = new SliceVariable(length);
      final SliceExpression newSlice = lhs.withIndex(arrayIndex);
      generateMemsetAssignments(newSlice, setValue, assignments);

    } else if (lhsType instanceof CCompositeType compositeUnderlyingType) {

      // for structs and unions, perform recursive assignment
      // it should not matter that there will be multiple assignments for union fields

      for (CCompositeTypeMemberDeclaration member : compositeUnderlyingType.getMembers()) {
        final SliceExpression newSlice = lhs.withFieldAccess(member);
        generateMemsetAssignments(newSlice, setValue, assignments);
      }

    } else {
      // the RHS slice is constructed from the value to be set to the given type
      SliceExpression rhsSlice =
          new SliceExpression(constructMemsetValueForType(lhsType, setValue));

      // there is no indexing of rhs
      // TODO: add relevancy checking
      AssignmentHandler.SliceAssignment sliceAssignment =
          new AssignmentHandler.SliceAssignment(lhs, Optional.empty(), Optional.of(rhsSlice));

      assignments.add(sliceAssignment);
    }
  }

  /**
   * Construct an appropriate value to set to a non-array, non-composite type from memset setValue.
   *
   * @param type The type to construct the value for.
   * @param setValue Value to set to every byte after being interpreted as unsigned char.
   * @throws UnrecognizedCodeException If the C code was unrecognizable.
   */
  private CExpression constructMemsetValueForType(final CType type, final CExpression setValue)
      throws UnrecognizedCodeException {

    CType compatibleType = type;

    // convert enums and pointers to compatible simple types
    if (type instanceof CEnumType enumType) {
      compatibleType = enumType.getCompatibleType();
    } else if (type instanceof CPointerType) {
      compatibleType = conv.machineModel.getPointerEquivalentSimpleType();
    }

    // call an appropriate function depending on the type or throw if it is unsupported
    // notably, void is unsupported, so void* destination memset will throw
    if (compatibleType instanceof CBitFieldType bitfieldType) {
      return constructSetValueForBitFieldType(bitfieldType, setValue);
    } else if (compatibleType instanceof CSimpleType simpleType) {
      return constructSetValueForSimpleType(simpleType, setValue);
    } else {
      throw new UnrecognizedCodeException(
          String.format("Could not resolve type %s to a simple type in memset handling", type),
          edge);
    }
  }

  /**
   * Construct an appropriate value to set to a bitfield type from memset setValue. As bitfield
   * representation is implementation-defined, the value is only constructed if the original value
   * to set is definitely all-zeros or all-ones. Otherwise, the function throws.
   *
   * @param bitfieldType The bitfield type to construct the value for.
   * @param setValue Value to set to every byte after being interpreted as unsigned char.
   * @throws UnrecognizedCodeException If the value to set is not definitely all-zeros or all-ones.
   */
  private CExpression constructSetValueForBitFieldType(
      final CBitFieldType bitfieldType, final CExpression setValue)
      throws UnrecognizedCodeException {

    // the bitfield representation is implementation-defined,
    // but it is reasonable to assume that all-zeros will set it to all-zeros
    // and all-ones will set it to all-ones

    if (!(setValue instanceof CIntegerLiteralExpression)) {
      throw new UnrecognizedCodeException(
          "Non-literal memset value not supported for bitfields", edge);
    }

    // determine the value of literal
    final CIntegerLiteralExpression setValueLiteral = (CIntegerLiteralExpression) setValue;
    final int setByte = setValueLiteral.getValue().intValue() & 0xFF;
    if (setByte != 0 && setByte != 0xFF) {
      throw new UnrecognizedCodeException(
          "Only all-zeros and all-ones memset values supported for bitfields", edge);
    }

    // tailor the set value expression to the base type and bitfield size
    final CType bitfieldBaseType = bitfieldType.getType().getCanonicalType();
    long bitValue = (setByte != 0) ? ((1L << bitfieldType.getBitFieldSize()) - 1) : 0;
    return CIntegerLiteralExpression.createDummyLiteral(bitValue, bitfieldBaseType);
  }

  /**
   * Construct an appropriate value to set to a simple type from memset setValue.
   *
   * @param simpleType The simple type to construct the value for.
   * @param setValue Value to set to every byte after being interpreted as unsigned char.
   * @throws UnrecognizedCodeException If the type is not assignable from an integer type.
   */
  private CExpression constructSetValueForSimpleType(
      final CSimpleType simpleType, final CExpression setValue) throws UnrecognizedCodeException {

    // for floating-points, create the integer type with same size
    final long sizeInBytes = typeHandler.getSizeof(simpleType);
    final CBasicType basicType = simpleType.getType();
    CSimpleType integerType = simpleType;

    if (basicType.isFloatingPointType()) {

      // we need to construct the corresponding integer type with the same byte size
      // as the floating-point type so that we can populate it

      // TODO: integer type with the same sizeof as float should be resolved by a reverse lookup
      // into the machine model
      if (basicType == CBasicType.FLOAT) {
        // we use unsigned int in the meantime
        integerType = CNumericTypes.UNSIGNED_INT;
      } else if (basicType == CBasicType.DOUBLE) {
        // we use unsigned long long in the meantime
        integerType = CNumericTypes.UNSIGNED_LONG_LONG_INT;
      } else {
        throw new UnrecognizedCodeException(
            "Memset does not support destination with the used floating-point underlying type",
            edge);
      }
    }

    // the value to be set is specified as an unsigned byte, but the element may be larger,
    // so we need to compose the value to be set to each element
    // we set the expression to zero literal and then bit-or the shifted bytes containing the value
    // to be set
    final CExpression setValueUnsignedChar =
        new CCastExpression(FileLocation.DUMMY, CNumericTypes.UNSIGNED_CHAR, setValue);
    final CExpression setValueLowestByte =
        new CCastExpression(FileLocation.DUMMY, integerType, setValueUnsignedChar);

    CExpression setValueExpression = CIntegerLiteralExpression.createDummyLiteral(0, integerType);
    for (long byteIndex = 0; byteIndex < sizeInBytes; ++byteIndex) {
      // shift the byte left
      final long bitIndex = byteIndex * conv.getBitSizeof(CNumericTypes.UNSIGNED_CHAR);
      final CIntegerLiteralExpression bitIndexLiteral =
          CIntegerLiteralExpression.createDummyLiteral(bitIndex, integerType);
      final CBinaryExpression setValueShiftedByteExpression =
          new CBinaryExpression(
              FileLocation.DUMMY,
              integerType,
              integerType,
              setValueLowestByte,
              bitIndexLiteral,
              BinaryOperator.SHIFT_LEFT);

      // bit-or with previous formula
      // plus could also be used
      setValueExpression =
          new CBinaryExpression(
              FileLocation.DUMMY,
              integerType,
              integerType,
              setValueExpression,
              setValueShiftedByteExpression,
              BinaryOperator.BINARY_OR);
    }

    return setValueExpression;
  }

  /**
   * Converts the {@link CExpression} size in bytes to size in elements of a given pointer-like
   * type. If the size in bytes covers the last element only partially, it is covered by the
   * returned size in elements.
   *
   * @param sizeInBytes Size in bytes.
   * @param pointerLikeType Pointer-like type.
   * @return Size in elements.
   * @throws UnrecognizedCodeException If the
   */
  private CExpression convertSizeInBytesToSizeInElements(
      final CExpression sizeInBytes, final CType pointerLikeType) throws UnrecognizedCodeException {

    // we need to know the element size, ensure we have a pointer first

    final CType adjustedPointerLikeType = CTypes.adjustFunctionOrArrayType(pointerLikeType);
    if (!(adjustedPointerLikeType instanceof CPointerType)) {
      throw new UnrecognizedCodeException(
          "Expected type to be pointer-like in byte-size to element-size conversion", edge);
    }
    final CPointerType pointerType = (CPointerType) adjustedPointerLikeType;

    // take the byte size of the underlying type
    final CType underlyingType = pointerType.getType().getCanonicalType();
    final long elementSizeInBytes = typeHandler.getSizeof(underlyingType);

    // we handle the possibility of having the last element partially copied
    // by treating it as being fully copied, as this situation can arise
    // in correct programs when structure padding is not copied
    // therefore, we want the ceiling of (byte_operation_size / element_size)
    // thus, we use (byte_operation_size + element_size - 1) / element_size
    // for easy computation; it can technically overflow, but that would mean
    // the array would take almost all memory that is addressable

    // if possible, compute the size in elements statically so it remains a literal
    if (sizeInBytes instanceof CIntegerLiteralExpression literalSizeInBytes) {
      final long sizeInBytesAsLong = literalSizeInBytes.asLong();
      final long operationSizeInElementsAsLong =
          (sizeInBytesAsLong + elementSizeInBytes - 1) / elementSizeInBytes;
      final CExpression operationSizeInElements =
          CIntegerLiteralExpression.createDummyLiteral(operationSizeInElementsAsLong, sizeType);
      return operationSizeInElements;
    }

    // not possible to compute the size in elements statically, create a non-literal expression

    // cast the operation size to pointer-equivalent size type first
    CExpression operationSizeInBytes =
        new CCastExpression(FileLocation.DUMMY, sizeType, sizeInBytes);

    // create (byte_operation_size + (element_size - 1))
    CIntegerLiteralExpression elementSizeInBytesMinusOneLiteral =
        CIntegerLiteralExpression.createDummyLiteral(elementSizeInBytes - 1, sizeType);
    CExpression operationSizeByteCeiling =
        new CBinaryExpression(
            FileLocation.DUMMY,
            sizeType,
            sizeType,
            operationSizeInBytes,
            elementSizeInBytesMinusOneLiteral,
            BinaryOperator.PLUS);

    // create (byte_operation_size + element_size - 1) / element_size
    CIntegerLiteralExpression elementSizeInBytesLiteral =
        CIntegerLiteralExpression.createDummyLiteral(elementSizeInBytes, sizeType);
    CExpression operationSizeInElements =
        new CBinaryExpression(
            FileLocation.DUMMY,
            sizeType,
            sizeType,
            operationSizeByteCeiling,
            elementSizeInBytesLiteral,
            BinaryOperator.DIVIDE);

    // return the non-literal expression
    return operationSizeInElements;
  }

  /**
   * Processes a pointer-like argument to memory manipulation functions (destination or source).
   *
   * <p>First, if it exists, the outer cast from pointer to {@code void*} / {@code const void*} is
   * removed as it prevents us from discovering the actual type. If it is to {@code const void*} and
   * the argument should be non-const, an exception is thrown.
   *
   * <p>Next, if the outer expression is now address-of applied on an array-type expression, it is
   * removed as it just syntactically makes the array into an rvalue pointer.
   *
   * <p>Lastly, if the processed argument now is just a multidimensional array, it is flattened to a
   * single-dimensional array with appropriate size. This invokes undefined behavior in C, but is
   * not problematic for us since the pointer arithmetic is the same and memory manipulation
   * functions behave in the same fashion.
   *
   * @param argument The pointer-like argument to a memory manipulatrion function.
   * @param shouldBeNonconst If the argument should be non-const.
   * @return The processed argument.
   * @throws UnrecognizedCodeException If the argument should be non-const and its outer cast is
   *     const.
   */
  private CExpression processPointerLikeArgument(
      final CExpression argument, final boolean shouldBeNonconst) throws UnrecognizedCodeException {

    CExpression processedArgument = argument;

    // remove the C++ style cast of destination to void* if necessary
    // it is a no-op anyway apart from the effects on the typing system
    if (processedArgument instanceof CCastExpression destinationCast) {
      CType castType = destinationCast.getCastType().getCanonicalType();
      if (castType instanceof CPointerType castPointerType) {
        if (shouldBeNonconst && castPointerType.isConst()) {
          throw new UnrecognizedCodeException("Expected outer type cast to be non-const", edge);
        }
        CType castPointerUnderlyingType = castPointerType.getType().getCanonicalType();
        if (castPointerUnderlyingType instanceof CVoidType) {
          // remove cast to void*
          processedArgument = destinationCast.getOperand();
        }
      }
    }

    // remove the array-to-pointer conversion if it exists as it adds nothing
    if (processedArgument instanceof CUnaryExpression unaryDestination
        && unaryDestination.getOperator() == CUnaryExpression.UnaryOperator.AMPER
        && unaryDestination.getOperand().getExpressionType().getCanonicalType()
            instanceof CArrayType) {
      processedArgument = unaryDestination.getOperand();
    }

    if (!(processedArgument instanceof CIdExpression idExpression)) {
      // not an id expression, return
      return processedArgument;
    }

    // try to flatten a multidimensional array to a single-dimensional array
    // iteratively fuse the last two dimensions
    CType idType = idExpression.getExpressionType().getCanonicalType();
    boolean flattened = false;
    while (idType instanceof CArrayType lastDimensionType
        && lastDimensionType.getType() instanceof CArrayType secondLastDimensionType) {
      flattened = true;

      // get the length of the last two dimensions of the array
      final @Nullable CExpression lastDimensionLength = lastDimensionType.getLength();
      final @Nullable CExpression secondLastDimensionLength = secondLastDimensionType.getLength();

      // multiply them together
      final @Nullable CExpression multipliedLength;

      if (lastDimensionLength instanceof CIntegerLiteralExpression literalArrayLength
          && secondLastDimensionLength
              instanceof CIntegerLiteralExpression literalUnderlyingLength) {
        // dimension lengths are statically known, multiply them statically and
        final long multipliedLengthAsLong =
            literalArrayLength.asLong() * literalUnderlyingLength.asLong();
        multipliedLength =
            CIntegerLiteralExpression.createDummyLiteral(multipliedLengthAsLong, sizeType);
      } else if (lastDimensionLength != null && secondLastDimensionLength != null) {
        // dimension lengths are not statically known, multiply them as expressions
        multipliedLength =
            new CBinaryExpression(
                FileLocation.DUMMY,
                sizeType,
                sizeType,
                lastDimensionLength,
                secondLastDimensionLength,
                BinaryOperator.MULTIPLY);
      } else {
        // at least one of the dimension lengths is incomplete, make the multiplied length
        // incomplete as well
        multipliedLength = null;
      }

      // fuse the last two dimensions together
      idType =
          new CArrayType(
              lastDimensionType.isConst(),
              lastDimensionType.isVolatile(),
              secondLastDimensionType.getType(),
              multipliedLength);
    }
    if (!flattened) {
      // is not a flattened multidimensional array, return the processed argument
      return processedArgument;
    }
    // is a multidimensional array, make a new id expression with the flattened single-dimensional
    // array type
    return new CIdExpression(
        idExpression.getFileLocation(),
        idType,
        idExpression.getName(),
        idExpression.getDeclaration());
  }
}
