// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGCPATransferRelationTest {

  private static final CType CHAR_TYPE = CNumericTypes.CHAR;
  private static final CType SHORT_TYPE = CNumericTypes.SHORT_INT;
  private static final CType UNSIGNED_SHORT_TYPE = CNumericTypes.UNSIGNED_SHORT_INT;
  private static final CType INT_TYPE = CNumericTypes.INT;
  private static final CType UNSIGNED_INT_TYPE = CNumericTypes.UNSIGNED_INT;
  private static final CType LONG_TYPE = CNumericTypes.LONG_INT;
  private static final CType UNSIGNED_LONG_TYPE = CNumericTypes.UNSIGNED_LONG_INT;

  // Float/Double is currently not supported by SMG2
  @SuppressWarnings("unused")
  private static final CType FLOAT_TYPE = CNumericTypes.FLOAT;

  @SuppressWarnings("unused")
  private static final CType DOUBLE_TYPE = CNumericTypes.DOUBLE;

  private static final CType[] TEST_TYPES =
      new CType[] {
        CHAR_TYPE,
        SHORT_TYPE,
        UNSIGNED_SHORT_TYPE,
        INT_TYPE,
        UNSIGNED_INT_TYPE,
        LONG_TYPE,
        UNSIGNED_LONG_TYPE
      };

  private static final MachineModel MACHINE_MODEL = MachineModel.LINUX64;
  // Pointer size for the machine model in bits
  private static final int POINTER_SIZE_IN_BITS =
      MACHINE_MODEL.getSizeof(MACHINE_MODEL.getPointerEquivalentSimpleType()) * 8;

  // Note: padding is on per default, meaning that the types get padding to align to their
  // "natural" memory offset. I.e. starting with a 2 byte type, then using a 4 byte type would
  // result in a padding of 2 byte between them. This is meant for structs with padding.
  private static final List<CType> STRUCT_UNION_TEST_TYPES =
      ImmutableList.of(
          SHORT_TYPE, // 2 byte padding after this!
          INT_TYPE, // No padding after this
          UNSIGNED_SHORT_TYPE, // 2 byte padding after this!
          UNSIGNED_INT_TYPE, // No padding after this
          LONG_TYPE, // No padding after this
          UNSIGNED_LONG_TYPE, // No padding after this
          CHAR_TYPE);

  private static final List<String> STRUCT_UNION_FIELD_NAMES =
      ImmutableList.of(
          "SHORT_TYPE",
          "INT_TYPE",
          "UNSIGNED_SHORT_TYPE",
          "UNSIGNED_INT_TYPE",
          "LONG_TYPE",
          "UNSIGNED_LONG_TYPE",
          "CHAR_TYPE");

  // TODO: add complex types, structs/arrays etc. as well.
  private static final List<CType> ARRAY_TEST_TYPES =
      ImmutableList.of(
          SHORT_TYPE,
          INT_TYPE,
          UNSIGNED_SHORT_TYPE,
          UNSIGNED_INT_TYPE,
          LONG_TYPE,
          UNSIGNED_LONG_TYPE,
          CHAR_TYPE);

  private static final BigInteger TEST_ARRAY_LENGTH = BigInteger.valueOf(50);
  /*
   * Declaration tests:
   *   declare variable without value and use afterwards
   *   declare with simple value and use
   *   declare stack array and use
   *   declare stack array with value
   *   declare stack struct
   *   declare stack struct with value
   *   declare String with array of chars
   *   declare String with String ("")
   *   declare String as char * and array of chars
   *   declare String as char * with String
   *
   * Function usage:
   *   malloc
   *   free
   *   ...
   */

  private SMGTransferRelation transferRelation;

  @Before
  public void init() throws InvalidConfigurationException {
    LogManager logManager = LogManager.createTestLogManager();
    SMGOptions smgOptions = new SMGOptions(Configuration.defaultConfiguration());

    transferRelation =
        new SMGTransferRelation(
            logManager,
            smgOptions,
            SMGCPAExportOptions.getNoExportInstance(),
            ShutdownNotifier.createDummy(),
            MACHINE_MODEL,
            ImmutableList.of(),
            ImmutableList.of());
    transferRelation.setInfo(
        SMGState.of(MACHINE_MODEL, logManager, smgOptions)
            .copyAndAddStackFrame(CFunctionDeclaration.DUMMY),
        null,
        new CDeclarationEdge(
            "", FileLocation.DUMMY, CFANode.newDummyCFANode(), CFANode.newDummyCFANode(), null));
  }

  /*
   * Test struct declaration without assignment.
   * i.e. struct TestStruct { int intField, ... };
   * struct TestStruct structName;
   */
  @Test
  public void localVariableDeclarationStructSimpleTypesTest() throws CPATransferException {
    String variableName = "variableName";
    String structName = "TestStruct";
    CType type =
        makeElaboratedTypeFor(
            structName, ComplexTypeKind.STRUCT, STRUCT_UNION_TEST_TYPES, STRUCT_UNION_FIELD_NAMES);
    // Make a non global and not external variable with the current type
    List<SMGState> statesAfterDecl =
        transferRelation.handleDeclarationEdge(
            null, declareVariableWithoutInitializer(variableName, type, false, false));
    // Since we declare variables we know there will be only 1 state afterwards
    assertThat(statesAfterDecl).hasSize(1);
    // This state must have a local variable the size of the type used (on the current stack frame)
    // The state should not have any errors
    SMGState state = statesAfterDecl.get(0);
    // TODO: error check
    SymbolicProgramConfiguration memoryModel = state.getMemoryModel();
    assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
    SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
    // SMG sizes are in bits! Also since this is a struct, padding has to be taken into account.
    BigInteger expectedSize = MACHINE_MODEL.getSizeofInBits(type);
    assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      BigInteger offsetInBits =
          MACHINE_MODEL.getFieldOffsetInBits(
              (CCompositeType) ((CElaboratedType) type).getRealType(),
              STRUCT_UNION_FIELD_NAMES.get(i));
      BigInteger sizeInBits = MACHINE_MODEL.getSizeofInBits(STRUCT_UNION_TEST_TYPES.get(i));
      // further, this memory is not written at all, meaning we can read it and it returns UNKNOWN
      ValueAndSMGState readValueAndState = state.readValue(memoryObject, offsetInBits, sizeInBits);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.getValue().isUnknown()).isTrue();
    }
  }

  /*
   * Test struct declaration with assignment.
   * i.e. struct TestStruct { int intField, ... };
   * struct TestStruct structName = {1, ...};
   */
  @Test
  public void localVariableDeclarationWithAssignmentStructSimpleTypesTest()
      throws CPATransferException {
    String variableName = "variableName";
    String structName = "TestStruct";
    CType type =
        makeElaboratedTypeFor(
            structName, ComplexTypeKind.STRUCT, STRUCT_UNION_TEST_TYPES, STRUCT_UNION_FIELD_NAMES);
    // Build assignments starting from numeric value 1 and increment after each assignment. Chars
    // are simply the int cast to a char.
    ImmutableList.Builder<CInitializer> listOfInitsBuilder = ImmutableList.builder();
    BigInteger value = BigInteger.ONE;
    for (CType currType : STRUCT_UNION_TEST_TYPES) {
      CExpression exprToInit;
      if (currType == CNumericTypes.CHAR) {
        exprToInit = makeCharExpressionFrom((char) value.intValue());
      } else {
        exprToInit = makeIntegerExpressionFrom(value, (CSimpleType) currType);
      }
      listOfInitsBuilder.add(makeCInitializerExpressionFor(exprToInit));
      value = value.add(BigInteger.ONE);
    }
    CInitializer initList = makeCInitializerListFor(listOfInitsBuilder.build());
    // Make a non global and not external variable with the current type
    List<SMGState> statesAfterDecl =
        transferRelation.handleDeclarationEdge(
            null, declareVariableWithInitializer(variableName, type, false, false, initList));
    // Since we declare variables we know there will be only 1 state afterwards
    assertThat(statesAfterDecl).hasSize(1);
    // This state must have a local variable the size of the type used (on the current stack frame)
    // The state should not have any errors
    SMGState state = statesAfterDecl.get(0);
    // TODO: error check
    SymbolicProgramConfiguration memoryModel = state.getMemoryModel();
    assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
    SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
    // SMG sizes are in bits! Also since this is a struct, padding has to be taken into account.
    BigInteger expectedSize = MACHINE_MODEL.getSizeofInBits(type);
    assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      BigInteger offsetInBits =
          MACHINE_MODEL.getFieldOffsetInBits(
              (CCompositeType) ((CElaboratedType) type).getRealType(),
              STRUCT_UNION_FIELD_NAMES.get(i));
      BigInteger sizeInBits = MACHINE_MODEL.getSizeofInBits(STRUCT_UNION_TEST_TYPES.get(i));
      // further, this memory is not written at all, meaning we can read it and it returns the
      // numeric values assigned, starting from 1 and incrementing after each field once
      ValueAndSMGState readValueAndState = state.readValue(memoryObject, offsetInBits, sizeInBits);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.getValue().isNumericValue()).isTrue();
      assertThat(readValueAndState.getValue().asNumericValue().bigInteger().intValueExact())
          .isEqualTo(i + 1);
    }
  }

  /*
   * Test struct declaration without assignment but complex nested types i.e. structs, arrays, pointers.
   * i.e. struct TestStruct { struct someStruct, int array[], ... };
   * struct TestStruct structName;
   */
  @Test
  public void localVariableDeclarationStructComplexTypesTest() throws CPATransferException {
    String variableName = "variableName";
    String structName = "TestStruct";
    String nestedStructFieldName = "NESTED_STRUCT";
    ImmutableList.Builder<String> overallFieldNames = ImmutableList.builder();
    overallFieldNames.addAll(STRUCT_UNION_FIELD_NAMES);
    overallFieldNames.add(nestedStructFieldName);
    overallFieldNames.addAll(
        transformedImmutableListCopy(STRUCT_UNION_FIELD_NAMES, name -> "ARRAY_" + name));
    overallFieldNames.addAll(
        transformedImmutableListCopy(STRUCT_UNION_FIELD_NAMES, name -> "POINTER_" + name));
    CType type =
        makeNestedStruct(
            structName,
            STRUCT_UNION_TEST_TYPES,
            overallFieldNames.build(),
            STRUCT_UNION_TEST_TYPES,
            STRUCT_UNION_FIELD_NAMES,
            structName + "Nested",
            STRUCT_UNION_TEST_TYPES,
            STRUCT_UNION_TEST_TYPES);
    // Make a non global and not external variable with the current type
    List<SMGState> statesAfterDecl =
        transferRelation.handleDeclarationEdge(
            null, declareVariableWithoutInitializer(variableName, type, false, false));
    // Since we declare variables we know there will be only 1 state afterwards
    assertThat(statesAfterDecl).hasSize(1);
    // This state must have a local variable the size of the type used (on the current stack frame)
    // The state should not have any errors
    SMGState state = statesAfterDecl.get(0);
    // TODO: error check
    SymbolicProgramConfiguration memoryModel = state.getMemoryModel();
    assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
    SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
    // SMG sizes are in bits! Also since this is a struct, padding has to be taken into account.
    BigInteger expectedSize = MACHINE_MODEL.getSizeofInBits(type);
    assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
    // Reading is a little more tricky since this is a struct with nested structures
    // We start with the simple types
    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      BigInteger offsetInBits =
          MACHINE_MODEL.getFieldOffsetInBits(
              (CCompositeType) ((CElaboratedType) type).getRealType(),
              STRUCT_UNION_FIELD_NAMES.get(i));
      BigInteger sizeInBits = MACHINE_MODEL.getSizeofInBits(STRUCT_UNION_TEST_TYPES.get(i));
      // further, this memory is not written at all, meaning we can read it and it returns UNKNOWN
      ValueAndSMGState readValueAndState = state.readValue(memoryObject, offsetInBits, sizeInBits);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.getValue().isUnknown()).isTrue();
    }

    // Read the nested struct. We can simply assume the offsets of the simple types from above + the
    // offset of the struct
    for (int i = 0; i < 1; i++) {
      BigInteger baseOffsetNestedStruct =
          MACHINE_MODEL.getFieldOffsetInBits(
              (CCompositeType) ((CElaboratedType) type).getRealType(), nestedStructFieldName);
      BigInteger offsetInBits =
          MACHINE_MODEL
              .getFieldOffsetInBits(
                  (CCompositeType) ((CElaboratedType) type).getRealType(),
                  STRUCT_UNION_FIELD_NAMES.get(i))
              .add(baseOffsetNestedStruct);
      BigInteger sizeInBits = MACHINE_MODEL.getSizeofInBits(STRUCT_UNION_TEST_TYPES.get(i));
      // further, this memory is not written at all, meaning we can read it and it returns UNKNOWN
      ValueAndSMGState readValueAndState = state.readValue(memoryObject, offsetInBits, sizeInBits);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.getValue().isUnknown()).isTrue();
    }

    // Now we repeat this for an array (they behave the same, so if one is empty, we can assume that
    // all are)
    BigInteger offsetOfArrayInBits =
        MACHINE_MODEL.getFieldOffsetInBits(
            (CCompositeType) ((CElaboratedType) type).getRealType(),
            "ARRAY_" + STRUCT_UNION_FIELD_NAMES.get(0));
    BigInteger sizeOfArrayInBits = MACHINE_MODEL.getSizeofInBits(STRUCT_UNION_TEST_TYPES.get(0));
    for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
      ValueAndSMGState readValueAndState =
          state.readValue(memoryObject, offsetOfArrayInBits, sizeOfArrayInBits);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.getValue().isUnknown()).isTrue();
      // increment the size onto the offset for the next element
      offsetOfArrayInBits = offsetOfArrayInBits.add(sizeOfArrayInBits);
    }

    // Check a pointer value also
    BigInteger offsetOfPointerInBits =
        MACHINE_MODEL.getFieldOffsetInBits(
            (CCompositeType) ((CElaboratedType) type).getRealType(),
            "POINTER_" + STRUCT_UNION_FIELD_NAMES.get(0));
    BigInteger sizeOfPointerInBits = BigInteger.valueOf(POINTER_SIZE_IN_BITS);
    // Check a pointer value
    ValueAndSMGState readValueAndState =
        state.readValue(memoryObject, offsetOfPointerInBits, sizeOfPointerInBits);
    // The read state should not have any errors
    // TODO: error check
    assertThat(readValueAndState.getValue().isUnknown()).isTrue();
  }

  /*
   * Test struct declaration with assignment.
   * i.e. struct TestStruct { struct otherStruct, int array[], ... };
   * struct TestStruct structName = {{1, 2, ...}, {-1, -2, ...}, ...};
   */
  @Test
  public void localVariableDeclarationWithAssignmentStructComplexTypesTest()
      throws CPATransferException {
    CInitializer initList =
        makeInitializerForNestedStruct(
            BigInteger.ONE,
            STRUCT_UNION_TEST_TYPES,
            STRUCT_UNION_TEST_TYPES,
            STRUCT_UNION_TEST_TYPES,
            STRUCT_UNION_TEST_TYPES);
    String variableName = "variableName";
    String structName = "TestStruct";
    String nestedStructFieldName = "NESTED_STRUCT";
    ImmutableList.Builder<String> overallFieldNames = ImmutableList.builder();
    overallFieldNames.addAll(STRUCT_UNION_FIELD_NAMES);
    overallFieldNames.add(nestedStructFieldName);
    overallFieldNames.addAll(
        transformedImmutableListCopy(STRUCT_UNION_FIELD_NAMES, name -> "ARRAY_" + name));
    overallFieldNames.addAll(
        transformedImmutableListCopy(STRUCT_UNION_FIELD_NAMES, name -> "POINTER_" + name));
    CType type =
        makeNestedStruct(
            structName,
            STRUCT_UNION_TEST_TYPES,
            overallFieldNames.build(),
            STRUCT_UNION_TEST_TYPES,
            STRUCT_UNION_FIELD_NAMES,
            structName + "Nested",
            STRUCT_UNION_TEST_TYPES,
            STRUCT_UNION_TEST_TYPES);
    // Make a non global and not external variable with the current type
    List<SMGState> statesAfterDecl =
        transferRelation.handleDeclarationEdge(
            null, declareVariableWithInitializer(variableName, type, false, false, initList));
    // Since we declare variables we know there will be only 1 state afterwards
    assertThat(statesAfterDecl).hasSize(1);
    // This state must have a local variable the size of the type used (on the current stack frame)
    // The state should not have any errors
    SMGState state = statesAfterDecl.get(0);
    // TODO: error check
    SymbolicProgramConfiguration memoryModel = state.getMemoryModel();
    assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
    SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
    // SMG sizes are in bits! Also since this is a struct, padding has to be taken into account.
    BigInteger expectedSize = MACHINE_MODEL.getSizeofInBits(type);
    assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
    // Reading is a little more tricky since this is a struct with nested structures
    // We start with the simple types. We know that the values are 1++ after each read in order
    // except pointers which will be 0.
    BigInteger expectedValue = BigInteger.ONE;
    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      BigInteger offsetInBits =
          MACHINE_MODEL.getFieldOffsetInBits(
              (CCompositeType) ((CElaboratedType) type).getRealType(),
              STRUCT_UNION_FIELD_NAMES.get(i));
      BigInteger sizeInBits = MACHINE_MODEL.getSizeofInBits(STRUCT_UNION_TEST_TYPES.get(i));
      // further, this memory is not written at all, meaning we can read it and it returns UNKNOWN
      ValueAndSMGState readValueAndState = state.readValue(memoryObject, offsetInBits, sizeInBits);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.getValue().isNumericValue()).isTrue();
      assertThat(readValueAndState.getValue().asNumericValue().bigInteger())
          .isEquivalentAccordingToCompareTo(expectedValue);
      expectedValue = expectedValue.add(BigInteger.ONE);
    }

    // Read the nested struct. We can simply assume the offsets of the simple types from above + the
    // offset of the struct
    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      BigInteger baseOffsetNestedStruct =
          MACHINE_MODEL.getFieldOffsetInBits(
              (CCompositeType) ((CElaboratedType) type).getRealType(), nestedStructFieldName);
      BigInteger offsetInBits =
          MACHINE_MODEL
              .getFieldOffsetInBits(
                  (CCompositeType) ((CElaboratedType) type).getRealType(),
                  STRUCT_UNION_FIELD_NAMES.get(i))
              .add(baseOffsetNestedStruct);
      BigInteger sizeInBits = MACHINE_MODEL.getSizeofInBits(STRUCT_UNION_TEST_TYPES.get(i));
      // further, this memory is not written at all, meaning we can read it and it returns UNKNOWN
      ValueAndSMGState readValueAndState = state.readValue(memoryObject, offsetInBits, sizeInBits);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.getValue().isNumericValue()).isTrue();
      assertThat(readValueAndState.getValue().asNumericValue().bigInteger())
          .isEquivalentAccordingToCompareTo(expectedValue);
      expectedValue = expectedValue.add(BigInteger.ONE);
    }

    // Now we repeat this for an array (they behave the same, so if one is empty, we can assume that
    // all are)
    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      BigInteger offsetOfArrayInBits =
          MACHINE_MODEL.getFieldOffsetInBits(
              (CCompositeType) ((CElaboratedType) type).getRealType(),
              "ARRAY_" + STRUCT_UNION_FIELD_NAMES.get(i));
      BigInteger sizeOfArrayInBits = MACHINE_MODEL.getSizeofInBits(STRUCT_UNION_TEST_TYPES.get(i));
      for (int j = 0; j < TEST_ARRAY_LENGTH.intValue(); j++) {
        ValueAndSMGState readValueAndState =
            state.readValue(memoryObject, offsetOfArrayInBits, sizeOfArrayInBits);
        // The read state should not have any errors
        // TODO: error check
        assertThat(readValueAndState.getValue().isNumericValue()).isTrue();
        assertThat(readValueAndState.getValue().asNumericValue().bigInteger())
            .isEquivalentAccordingToCompareTo(expectedValue);
        expectedValue = expectedValue.add(BigInteger.ONE);
        // increment the size onto the offset for the next element
        offsetOfArrayInBits = offsetOfArrayInBits.add(sizeOfArrayInBits);
      }
    }

    // Check a pointer values also
    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      BigInteger offsetOfPointerInBits =
          MACHINE_MODEL.getFieldOffsetInBits(
              (CCompositeType) ((CElaboratedType) type).getRealType(),
              "POINTER_" + STRUCT_UNION_FIELD_NAMES.get(i));
      BigInteger sizeOfPointerInBits = BigInteger.valueOf(POINTER_SIZE_IN_BITS);
      // Check a pointer value
      ValueAndSMGState readValueAndState =
          state.readValue(memoryObject, offsetOfPointerInBits, sizeOfPointerInBits);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.getValue().isNumericValue()).isTrue();
      assertThat(readValueAndState.getValue().asNumericValue().bigInteger())
          .isEquivalentAccordingToCompareTo(BigInteger.ZERO);
    }
  }

  /*
   * Declaration of arrays using array notation and no assignment: type array[];
   */
  @Test
  public void localVariableDeclarationArrayTypesTest() throws CPATransferException {
    String variableName = "variableName";
    for (CType type : ARRAY_TEST_TYPES) {
      // Make a non global and not external variable with the current type
      List<SMGState> statesAfterDecl =
          transferRelation.handleDeclarationEdge(
              null,
              declareVariableWithoutInitializer(
                  variableName, makeArrayTypeFor(type, TEST_ARRAY_LENGTH), false, false));
      // Since we declare variables we know there will be only 1 state afterwards
      assertThat(statesAfterDecl).hasSize(1);
      // This state must have a local variable the size of the type used (on the current stack
      // frame)
      // The state should not have any errors
      SMGState state = statesAfterDecl.get(0);
      // TODO: error check
      SymbolicProgramConfiguration memoryModel = state.getMemoryModel();
      assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
      SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
      // SMG sizes are in bits!
      BigInteger typeSize = MACHINE_MODEL.getSizeofInBits(type);
      BigInteger expectedSize = typeSize.multiply(TEST_ARRAY_LENGTH);
      assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
      // further, this memory is not written at all, meaning we can read it and it returns UNKNOWN
      // Since this is an array, we read only the type size, but length times
      for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
        BigInteger offset = BigInteger.valueOf(i).multiply(typeSize);
        ValueAndSMGState readValueAndState = state.readValue(memoryObject, offset, typeSize);
        // The read state should not have any errors
        // TODO: error check
        assertThat(readValueAndState.getValue().isUnknown()).isTrue();
      }
    }
  }

  /*
   * Array declaration using array notation with assignment.
   * i.e. int array[] = {1, 2, 3, 4, ...};
   */
  @Test
  public void localVariableDeclarationWithAssignmentArrayTypesTest() throws CPATransferException {
    String variableName = "variableName";
    for (CType type : ARRAY_TEST_TYPES) {
      // Build assignments starting from numeric value 0 and increment after each assignment. Chars
      // are simply the int cast to a char.
      ImmutableList.Builder<CInitializer> listOfInitsBuilder = ImmutableList.builder();
      for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
        CExpression exprToInit;
        if (type == CNumericTypes.CHAR) {
          exprToInit = makeCharExpressionFrom((char) i);
        } else {
          exprToInit = makeIntegerExpressionFrom(BigInteger.valueOf(i), (CSimpleType) type);
        }
        listOfInitsBuilder.add(makeCInitializerExpressionFor(exprToInit));
      }
      CInitializer initList = makeCInitializerListFor(listOfInitsBuilder.build());
      // Make a non global and not external variable with the current type
      List<SMGState> statesAfterDecl =
          transferRelation.handleDeclarationEdge(
              null,
              declareVariableWithInitializer(
                  variableName, makeArrayTypeFor(type, TEST_ARRAY_LENGTH), false, false, initList));
      // Since we declare variables we know there will be only 1 state afterwards
      assertThat(statesAfterDecl).hasSize(1);
      // This state must have a local variable the size of the type used (on the current stack
      // frame)
      // The state should not have any errors
      SMGState state = statesAfterDecl.get(0);
      // TODO: error check
      SymbolicProgramConfiguration memoryModel = state.getMemoryModel();
      assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
      SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
      // SMG sizes are in bits!
      BigInteger typeSize = MACHINE_MODEL.getSizeofInBits(type);
      BigInteger expectedSize = typeSize.multiply(TEST_ARRAY_LENGTH);
      assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
      // Check that the values match the assignment
      for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
        BigInteger offset = BigInteger.valueOf(i).multiply(typeSize);
        ValueAndSMGState readValueAndState = state.readValue(memoryObject, offset, typeSize);
        // The read state should not have any errors
        // TODO: error check
        assertThat(readValueAndState.getValue().isNumericValue()).isTrue();
        assertThat(readValueAndState.getValue().asNumericValue().bigInteger().intValueExact())
            .isEqualTo(i);
      }
    }
  }

  /*
   * Declaration without assignment of simple types i.e. int, char, unsigned short ...
   * i.e. short bla;
   */
  @Test
  public void localVariableDeclarationSimpleTypesTest() throws CPATransferException {
    String variableName = "variableName";
    for (CType type : TEST_TYPES) {
      // Make a non global and not external variable with the current type
      List<SMGState> statesAfterDecl =
          transferRelation.handleDeclarationEdge(
              null, declareVariableWithoutInitializer(variableName, type, false, false));
      // Since we declare variables we know there will be only 1 state afterwards
      assertThat(statesAfterDecl).hasSize(1);
      // This state must have a local variable the size of the type used (on the current stackframe)
      // The state should not have any errors
      SMGState state = statesAfterDecl.get(0);
      // TODO: error check
      SymbolicProgramConfiguration memoryModel = state.getMemoryModel();
      assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
      SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
      // SMG sizes are in bits!
      BigInteger expectedSize = MACHINE_MODEL.getSizeofInBits(type);
      assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
      // further, this memory is not written at all, meaning we can read it and it returns UNKNOWN
      ValueAndSMGState readValueAndState =
          state.readValue(memoryObject, BigInteger.ZERO, expectedSize);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.getValue().isUnknown()).isTrue();
    }
  }

  /*
   * Declaration with assignment of simple types i.e. int, char, unsigned short ...
   * i.e. short bla = 3;
   */
  @Test
  public void localVariableDeclarationWithAssignmentSimpleTypesTest() throws CPATransferException {
    String variableName = "variableName";
    BigInteger value = BigInteger.ONE;
    for (CType type : TEST_TYPES) {
      CExpression exprToInit;
      if (type == CNumericTypes.CHAR) {
        exprToInit = makeCharExpressionFrom((char) value.intValue());
      } else {
        exprToInit = makeIntegerExpressionFrom(value, (CSimpleType) type);
      }
      CInitializer initializer = makeCInitializerExpressionFor(exprToInit);

      // Make a non global and not external variable with the current type
      List<SMGState> statesAfterDecl =
          transferRelation.handleDeclarationEdge(
              null, declareVariableWithInitializer(variableName, type, false, false, initializer));
      // Since we declare variables we know there will be only 1 state afterwards
      assertThat(statesAfterDecl).hasSize(1);
      // This state must have a local variable the size of the type used (on the current stack
      // frame)
      // The state should not have any errors
      SMGState state = statesAfterDecl.get(0);
      // TODO: error check
      SymbolicProgramConfiguration memoryModel = state.getMemoryModel();
      assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
      SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
      // SMG sizes are in bits!
      BigInteger expectedSize = MACHINE_MODEL.getSizeofInBits(type);
      assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
      // further, in the memory there must be a SMGValue that maps to the Value entered
      ValueAndSMGState readValueAndState =
          state.readValue(memoryObject, BigInteger.ZERO, expectedSize);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.getValue().isExplicitlyKnown()).isTrue();
      assertThat(readValueAndState.getValue().asNumericValue().bigInteger().compareTo(value) == 0)
          .isTrue();
      // We increment the value to make them distinct
      value = value.add(BigInteger.ONE);
    }
  }

  private CVariableDeclaration declareVariableWithoutInitializer(
      String variableName, CType type, boolean isGlobal, boolean isExtern) {
    return declareVariableWithInitializer(variableName, type, isGlobal, isExtern, null);
  }

  private CVariableDeclaration declareVariableWithInitializer(
      String variableName,
      CType type,
      boolean isGlobal,
      boolean isExtern,
      CInitializer initializer) {
    CStorageClass storage = CStorageClass.AUTO;
    if (isExtern) {
      storage = CStorageClass.EXTERN;
    }
    // name, origName and qualifiedName are chosen as the same for now as we don't test functions
    // If we would want to test functions we would need to add the function name followed by ::
    // before the varName; i.e. main::varName
    return new CVariableDeclaration(
        FileLocation.DUMMY,
        isGlobal,
        storage,
        type,
        variableName,
        variableName,
        variableName,
        initializer);
  }

  /**
   * Make a struct/union with the name structureName and the types (and names of fields) specified
   * in the type/name lists in the order specified.
   */
  private CType makeElaboratedTypeFor(
      String structureName,
      ComplexTypeKind complexTypeKind,
      List<CType> listOfTypes,
      List<String> fieldNames) {
    ImmutableList.Builder<CCompositeTypeMemberDeclaration> typeBuilder =
        new ImmutableList.Builder<>();
    for (int i = 0; i < listOfTypes.size(); i++) {
      typeBuilder.add(new CCompositeTypeMemberDeclaration(listOfTypes.get(i), fieldNames.get(i)));
    }
    CCompositeType realType =
        new CCompositeType(
            false, false, complexTypeKind, typeBuilder.build(), structureName, structureName);
    return new CElaboratedType(
        false, false, complexTypeKind, structureName, structureName, realType);
  }

  @SuppressWarnings("unused")
  private CType makeArrayTypeFor(CType elementType, CExpression length) {
    return new CArrayType(false, false, elementType, length);
  }

  private CType makeArrayTypeFor(CType elementType, BigInteger length) {
    return new CArrayType(
        false,
        false,
        elementType,
        new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, length));
  }

  private CType makePointerTypeFor(CType pointerType) {
    return new CPointerType(false, false, pointerType);
  }

  private CInitializer makeCInitializerExpressionFor(CExpression expr) {
    return new CInitializerExpression(FileLocation.DUMMY, expr);
  }

  private CInitializer makeCInitializerListFor(List<CInitializer> list) {
    return new CInitializerList(FileLocation.DUMMY, list);
  }

  private CIntegerLiteralExpression makeIntegerExpressionFrom(BigInteger value, CSimpleType type) {
    Preconditions.checkArgument(
        value.compareTo(MACHINE_MODEL.getMinimalIntegerValue(type)) >= 0
            && value.compareTo(MACHINE_MODEL.getMaximalIntegerValue(type)) <= 0);
    return new CIntegerLiteralExpression(FileLocation.DUMMY, type, value);
  }

  private CExpression makeCharExpressionFrom(char value) {
    return new CCharLiteralExpression(FileLocation.DUMMY, CNumericTypes.CHAR, value);
  }

  /*
   * Builds a struct with structTypes + a nested struct according to the list of types in nestedStructTypesList after that + arrays out of each arrayTypesList with TEST_ARRAY_LENGTH + pointers out of pointerTypesList.
   * The names of the fields (of the top struct) will be according to structFieldNames. The nested struct field names will be according to nestedStructFieldNames.
   */
  private CType makeNestedStruct(
      String structName,
      List<CType> structTypesList,
      List<String> structFieldNames,
      List<CType> nestedStructTypesList,
      List<String> nestedStructFieldNames,
      String nestedStructName,
      List<CType> arrayTypesList,
      List<CType> pointerTypesList) {
    ImmutableList.Builder<CType> listTypesTop = ImmutableList.builder();
    listTypesTop.addAll(structTypesList);

    listTypesTop.add(
        makeElaboratedTypeFor(
            nestedStructName,
            ComplexTypeKind.STRUCT,
            nestedStructTypesList,
            nestedStructFieldNames));

    for (CType arrayType : arrayTypesList) {
      listTypesTop.add(makeArrayTypeFor(arrayType, TEST_ARRAY_LENGTH));
    }

    for (CType pointerType : pointerTypesList) {
      listTypesTop.add(makePointerTypeFor(pointerType));
    }

    return makeElaboratedTypeFor(
        structName, ComplexTypeKind.STRUCT, listTypesTop.build(), structFieldNames);
  }

  /*
   * This creates the initializer for the type created by makeNestedStruct(). It will use the beginninValue for the first value and then increment after each assignment except pointers, those will be 0.
   */
  private CInitializer makeInitializerForNestedStruct(
      BigInteger beginningValue,
      List<CType> structTypesList,
      List<CType> nestedStructTypesList,
      List<CType> arrayTypesList,
      List<CType> pointerTypesList) {
    BigInteger value = beginningValue;
    ImmutableList.Builder<CInitializer> topStructInitList = ImmutableList.builder();
    for (CType type : structTypesList) {
      CExpression exprToInit;
      if (type == CNumericTypes.CHAR) {
        exprToInit = makeCharExpressionFrom((char) value.intValue());
      } else {
        exprToInit = makeIntegerExpressionFrom(value, (CSimpleType) type);
      }
      topStructInitList.add(makeCInitializerExpressionFor(exprToInit));
      value = value.add(BigInteger.ONE);
    }

    // Now the nested struct
    ImmutableList.Builder<CInitializer> nestedStructInitList = ImmutableList.builder();
    for (CType type : nestedStructTypesList) {
      CExpression exprToInit;
      if (type == CNumericTypes.CHAR) {
        exprToInit = makeCharExpressionFrom((char) value.intValue());
      } else {
        exprToInit = makeIntegerExpressionFrom(value, (CSimpleType) type);
      }
      nestedStructInitList.add(makeCInitializerExpressionFor(exprToInit));
      value = value.add(BigInteger.ONE);
    }
    topStructInitList.add(makeCInitializerListFor(nestedStructInitList.build()));

    // Now the arrays
    for (CType type : arrayTypesList) {
      topStructInitList.add(makeArrayInitializer(value, TEST_ARRAY_LENGTH.intValue(), type));
      value = value.add(TEST_ARRAY_LENGTH);
    }

    // And the pointers
    for (@SuppressWarnings("unused") CType type : pointerTypesList) {
      topStructInitList.add(
          makeCInitializerExpressionFor(
              makeIntegerExpressionFrom(BigInteger.ZERO, (CSimpleType) INT_TYPE)));
    }

    return makeCInitializerListFor(topStructInitList.build());
  }

  private CInitializer makeArrayInitializer(BigInteger beginningValue, int length, CType type) {
    ImmutableList.Builder<CInitializer> arrayInitList = ImmutableList.builder();
    BigInteger value = beginningValue;
    for (int i = 0; i < length; i++) {
      CExpression exprToInit;
      if (type == CNumericTypes.CHAR) {
        exprToInit = makeCharExpressionFrom((char) value.intValue());
      } else {
        exprToInit = makeIntegerExpressionFrom(value, (CSimpleType) type);
      }
      arrayInitList.add(makeCInitializerExpressionFor(exprToInit));
      value = value.add(BigInteger.ONE);
    }
    return makeCInitializerListFor(arrayInitList.build());
  }
}
