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
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.SliceExpression.ArraySliceIndexVariable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentFormulaHandler.AssignmentConversionType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.AssignmentFormulaHandler.AssignmentOptions;
import org.sosy_lab.java_smt.api.BooleanFormula;

class MemoryFunctionHandler {

  private final CToFormulaConverterWithPointerAliasing conv;
  private final TypeHandlerWithPointerAliasing typeHandler;
  private final CFAEdge edge;
  private final String function;
  private final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final Constraints constraints;
  private final ErrorConditions errorConditions;
  private final MemoryRegionManager regionMgr;

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
  MemoryFunctionHandler(
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
  }

  CExpression convertSizeInBytesToSizeInElements(CExpression sizeInBytes, CType destinationType)
      throws UnrecognizedCodeException {

    // we need to know the element size
    // we ensure we have a pointer first and then take sizeof of the underlying type

    destinationType = CTypes.adjustFunctionOrArrayType(destinationType);
    if (!(destinationType instanceof CPointerType)) {
      throw new UnrecognizedCodeException(
          "Expected destination type to be a pointer (adjusted if necessary)", edge);
    }

    CPointerType destinationPointerType = (CPointerType) destinationType;

    CType destinationUnderlyingType = destinationPointerType.getType().getCanonicalType();
    long elementSizeInBytes = typeHandler.getSizeof(destinationUnderlyingType);

    // Second parameter will be processed separately depending on the actual function

    // Third parameter (size in bytes) processing

    // cast to size type first

    CType sizeType = conv.machineModel.getPointerEquivalentSimpleType();

    // we handle the possibility of having the last element partially copied
    // by treating it as being fully copied, as this situation can arise
    // in correct programs when structure padding is not copied
    // therefore, we want the ceiling of (byte_operation_size / element_size)
    // thus, we use (byte_operation_size + element_size - 1) / element_size
    // for easy computation; it can technically overflow, but that would mean
    // the array would take almost all memory that is addressable

    if (sizeInBytes instanceof CIntegerLiteralExpression literalSizeInBytes) {
      // compute the size in elements statically so that quantifier unrolling may be more precise

      long operationSizeInBytes = literalSizeInBytes.asLong();
      long operationSizeInElementsAsLong =
          (operationSizeInBytes + elementSizeInBytes - 1) / elementSizeInBytes;
      CExpression operationSizeInElements =
          CIntegerLiteralExpression.createDummyLiteral(operationSizeInElementsAsLong, sizeType);
      return operationSizeInElements;
    }

      // create an expression to compute the size dynamically

      CExpression operationSizeInBytes =
          new CCastExpression(FileLocation.DUMMY, sizeType, sizeInBytes);

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

    return operationSizeInElements;
  }

  /** This method provides an approximation of the builtin functions memset, memcpy, memmove. */
  CExpression handleMemoryAssignmentFunction(
      final String functionName, final CFunctionCallExpression e)
      throws UnrecognizedCodeException, InterruptedException {
    if (!conv.options.enableMemoryAssignmentFunctions()) {
      throw new UnrecognizedCodeException("Handling of memory assignment functions is disabled", e);
    }

    // all of the functions have exactly three parameters
    // the first and third parameter is the same for all functions
    final List<CExpression> parameters = e.getParameterExpressions();
    verify(parameters.size() == 3);
    CExpression paramDestination = parameters.get(0);
    final CExpression secondParameter = parameters.get(1);
    final CExpression paramSizeInBytes = parameters.get(2);

    // Handover to function-specific code

    try {
      if (functionName.equals("memset")) {
        handleMemsetFunction(paramDestination, paramSizeInBytes, secondParameter);
      } else {
        // memcpy and memmove only differ in that memcpy is not well-defined
        // if destination and source overlap
        // TODO: should we somehow handle possible memcpy overlap here?

        handleMemmoveFunction(paramDestination, paramSizeInBytes, secondParameter);
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
    return paramDestination;
  }

  void handleMemmoveFunction(
      CExpression unprocessedDestination, CExpression sizeInBytes, CExpression unprocessedSource)
      throws UnrecognizedCodeException, InterruptedException {

    final CExpression destination = processDestinationParameter(unprocessedDestination);

    final CType destinationType = destination.getExpressionType().getCanonicalType();
    final CExpression sizeInElements =
        convertSizeInBytesToSizeInElements(sizeInBytes, destinationType);

    // we remove the C++ style cast of source to const void* or void* if necessary
    // it is a no-op anyway apart from the effects on the typing system

    CExpression source = unprocessedSource;

    if (source instanceof CCastExpression sourceCast) {
      CType castType = sourceCast.getCastType().getCanonicalType();
      if (castType instanceof CPointerType castPointerType) {
        CType castPointerUnderlyingType = castPointerType.getType().getCanonicalType();
        if (castPointerUnderlyingType instanceof CVoidType) {
          // remove cast to const void* or void*
          source = sourceCast.getOperand();
        }
      }
    }

    source = performMemoryFunctionKludges(source);

    // the memcopy just indexes the destination and source with the same index

    ArraySliceIndexVariable sliceIndex = new ArraySliceIndexVariable(sizeInElements);

    SliceExpression lhs = new SliceExpression(destination).withIndex(sliceIndex);
    SliceExpression rhs = new SliceExpression(source).withIndex(sliceIndex);

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

    // TODO: add relevancy checking
    AssignmentHandler.SliceAssignment sliceAssignment =
        new AssignmentHandler.SliceAssignment(lhs, Optional.empty(), Optional.of(rhs));

    BooleanFormula assignmentFormula = assignmentHandler.assign(ImmutableList.of(sliceAssignment));
    constraints.addConstraint(assignmentFormula);
  }

  void handleMemsetFunction(
      final CExpression unprocessedDestination,
      final CExpression sizeInBytes,
      final CExpression setValue)
      throws UnrecognizedCodeException, InterruptedException {

    CExpression destination = processDestinationParameter(unprocessedDestination);

    CType destinationType = destination.getExpressionType().getCanonicalType();
    CExpression sizeInElements = convertSizeInBytesToSizeInElements(sizeInBytes, destinationType);

    ArraySliceIndexVariable sliceIndex = new ArraySliceIndexVariable(sizeInElements);
    SliceExpression slice = new SliceExpression(destination).withIndex(sliceIndex);

    List<AssignmentHandler.SliceAssignment> assignments = new ArrayList<>();
    generateMemsetAssignments(slice, setValue, assignments);

    // reinterpret instead of casting in assignment to properly handle memset of floats
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
    BooleanFormula assignmentFormula = assignmentHandler.assign(assignments);

    constraints.addConstraint(assignmentFormula);
  }

  private void generateMemsetAssignments(
      final SliceExpression lhsSlice,
      final CExpression setValue,
      List<AssignmentHandler.SliceAssignment> assignments)
      throws UnrecognizedCodeException, InterruptedException {

    CType type = lhsSlice.getFullExpressionType();

    if (type instanceof CArrayType arrayType) {
      // we handle arrays inside memsetted element by memsetting all of their elements
      // this is done by adding a new index

      CExpression length = arrayType.getLength();
      if (length == null) {
        throw new UnrecognizedCodeException(
            "Unexpected incomplete-array memset destination field", edge);
      }

      ArraySliceIndexVariable arrayIndex = new ArraySliceIndexVariable(length);
      SliceExpression newSlice = lhsSlice.withIndex(arrayIndex);
      generateMemsetAssignments(newSlice, setValue, assignments);

      return;
    }

    if (type instanceof CCompositeType compositeUnderlyingType) {

      // for structs and unions, perform recursive assignment
      // it should not matter that there will be multiple assignments for union fields

      for (CCompositeTypeMemberDeclaration member : compositeUnderlyingType.getMembers()) {
        SliceExpression newSlice = lhsSlice.withFieldAccess(member);
        generateMemsetAssignments(newSlice, setValue, assignments);
      }
      return;
    }

    // the actual value to be set can be initialized by bitfield handling
    // or left to the simple type handling, in which case it will
    // create the element value by bit shifts and bit-or
    CExpression actualSetValue = null;

    if (type instanceof CBitFieldType bitfieldType) {
      // the bitfield representation is implementation-defined,
      // but it is reasonable to assume that all-zeros will set it to zero
      // and all-ones will set it to the maximum value

      if (!(setValue instanceof CIntegerLiteralExpression)) {
        throw new UnrecognizedCodeException(
            "Non-literal memset value not supported for bitfields", edge);
      }

      CIntegerLiteralExpression setLiteral = (CIntegerLiteralExpression) setValue;
      int setByte = setLiteral.getValue().intValue() & 0xFF;
      if (setByte != 0 && setByte != 0xFF) {
        throw new UnrecognizedCodeException(
            "Only all-zeros and all-ones memset values supported for bitfields", edge);
      }

      // tailor the set value expression to the type and bitfield size
      type = bitfieldType.getType().getCanonicalType();

      long bitValue = (setByte != 0) ? ((1L << bitfieldType.getBitFieldSize()) - 1) : 0;

      actualSetValue = CIntegerLiteralExpression.createDummyLiteral(bitValue, type);
    }

    if (type instanceof CEnumType) {
      // for enums, we simply their type with the underlying int type
      type =
          new CSimpleType(
              false, false, CBasicType.INT, false, false, false, true, false, false, false);
    }

    if (type instanceof CPointerType) {
      // assign reinterpreted size_t
      type = conv.machineModel.getPointerEquivalentSimpleType();
    }

    if (type instanceof CVoidType) {
      throw new UnrecognizedCodeException(
          "Could not resolve underlying memset type from void", edge);
    }

    if (!(type instanceof CSimpleType)) {
      throw new UnrecognizedCodeException("Memset destination type not supported", edge);
    }

    // for simple types, add assignment to the set value expression

    // create the set value expression if it had not been created yet
    // in bitfield handling

    if (actualSetValue == null) {
      CExpression setValueUnsignedChar =
          new CCastExpression(FileLocation.DUMMY, CNumericTypes.UNSIGNED_CHAR, setValue);
      actualSetValue = createSetValueExpression((CSimpleType) type, setValueUnsignedChar);
    }

    SliceExpression rhsSlice = new SliceExpression(actualSetValue);

    // there is no indexing of rhs
    // TODO: add relevancy checking
    AssignmentHandler.SliceAssignment sliceAssignment =
        new AssignmentHandler.SliceAssignment(
            lhsSlice, Optional.empty(), Optional.of(rhsSlice));

    assignments.add(sliceAssignment);
  }

  private CExpression createSetValueExpression(
      CSimpleType underlyingType, CExpression setValueUnsignedChar)
      throws UnrecognizedCodeException {

    long elementSizeInBytes = typeHandler.getSizeof(underlyingType);

    // Floating-point handling: create underlying integer type

    CBasicType underlyingBasicType = underlyingType.getType();

    CSimpleType underlyingIntegerType = underlyingType;

    if (underlyingBasicType.isFloatingPointType()) {

      // we need to construct the corresponding integer type with the same byte size
      // as the floating-point type so that we can populate it

      if (underlyingBasicType == CBasicType.FLOAT) {
        // TODO: integer type with the same sizeof as float should be resolved by a reverse lookup
        // into the machine model
        // we use unsigned int in the meantime
        underlyingIntegerType = CNumericTypes.UNSIGNED_INT;
      } else if (underlyingBasicType == CBasicType.DOUBLE) {
        // TODO: integer type with the same sizeof as float should be resolved by a reverse lookup
        // into the machine model
        // we use unsigned long long in the meantime
        underlyingIntegerType = CNumericTypes.UNSIGNED_LONG_LONG_INT;
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
    CExpression setValueExpression =
        CIntegerLiteralExpression.createDummyLiteral(0, underlyingIntegerType);

    CExpression setValueLowestByte =
        new CCastExpression(FileLocation.DUMMY, underlyingIntegerType, setValueUnsignedChar);

    for (long byteIndex = 0; byteIndex < elementSizeInBytes; ++byteIndex) {
      // shift the byte left
      long bitIndex = byteIndex * conv.getBitSizeof(CNumericTypes.UNSIGNED_CHAR);
      CIntegerLiteralExpression bitIndexLiteral =
          CIntegerLiteralExpression.createDummyLiteral(bitIndex, underlyingIntegerType);
      CBinaryExpression setValueShiftedByteExpression =
          new CBinaryExpression(
              FileLocation.DUMMY,
              underlyingIntegerType,
              underlyingIntegerType,
              setValueLowestByte,
              bitIndexLiteral,
              BinaryOperator.SHIFT_LEFT);

      // bit-or with previous formula
      // plus could also be used
      setValueExpression =
          new CBinaryExpression(
              FileLocation.DUMMY,
              underlyingIntegerType,
              underlyingIntegerType,
              setValueExpression,
              setValueShiftedByteExpression,
              BinaryOperator.BINARY_OR);
    }

    return setValueExpression;
  }

  private CExpression processDestinationParameter(CExpression destination)
      throws UnrecognizedCodeException {

    // First parameter (destination) processing
    // TODO: make sure that the destination is flagged not to be ignored
    // for testing, this can be kludged by cpa.predicate.ignoreIrrelevantVariables=false

    // we remove the C++ style cast of destination to void* if necessary
    // it is a no-op anyway apart from the effects on the typing system
    if (destination instanceof CCastExpression destinationCast) {
      CType castType = destinationCast.getCastType().getCanonicalType();
      if (castType instanceof CPointerType castPointerType) {
        if (castPointerType.isConst()) {
          throw new UnrecognizedCodeException(
              "Expected destination type cast to be non-const", edge);
        }
        CType castPointerUnderlyingType = castPointerType.getType().getCanonicalType();
        if (castPointerUnderlyingType instanceof CVoidType) {
          // remove cast to void*
          destination = destinationCast.getOperand();
        }
      }
    }

    destination = performMemoryFunctionKludges(destination);
    return destination;
  }

  private CExpression performMemoryFunctionKludges(CExpression pointerParameterExpression) {

    // KLUDGE: remove the array-to-pointer conversion as the unary operator has wrong type
    // note that this has to be done after the cast removal
    // TODO: resolve wrong unary operator type for kludge removal
    if (pointerParameterExpression instanceof CUnaryExpression unaryDestination
        && unaryDestination.getOperator() == CUnaryExpression.UnaryOperator.AMPER
        && unaryDestination.getOperand().getExpressionType().getCanonicalType()
            instanceof CArrayType) {
      pointerParameterExpression = unaryDestination.getOperand();
    }

    CType sizeType = conv.machineModel.getPointerEquivalentSimpleType();

    // KLUDGE: multidimensional arrays

    if (pointerParameterExpression instanceof CIdExpression idExpression) {
      CType idType = idExpression.getExpressionType().getCanonicalType();
      boolean multidimensionalKludge = false;
      while (idType instanceof CArrayType arrayType
          && arrayType.getType() instanceof CArrayType underlyingArrayType) {
        multidimensionalKludge = true;
        CExpression arrayTypeLength = arrayType.getLength();
        CExpression underlyingTypeLength = underlyingArrayType.getLength();
        if (arrayTypeLength instanceof CIntegerLiteralExpression literalArrayLength
            && underlyingTypeLength instanceof CIntegerLiteralExpression literalUnderlyingLength) {
          long multipliedLength = literalArrayLength.asLong() * literalUnderlyingLength.asLong();
          idType =
              new CArrayType(
                  arrayType.isConst(),
                  arrayType.isVolatile(),
                  underlyingArrayType.getType(),
                  CIntegerLiteralExpression.createDummyLiteral(multipliedLength, sizeType));
        } else {
          idType =
              new CArrayType(
                  arrayType.isConst(), arrayType.isVolatile(), underlyingArrayType.getType(), null);
        }
      }
      if (multidimensionalKludge) {
        // just a kludge, take the declaration from previous
        return new CIdExpression(
            idExpression.getFileLocation(),
            idType,
            idExpression.getName(),
            idExpression.getDeclaration());
      }
    }
    return pointerParameterExpression;
  }
}
