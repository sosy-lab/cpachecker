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
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGCPATransferRelationTest {

  SMGState initialState;
  SMGOptions smgOptions;

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

  private static final CType[] TEST_TYPES = {
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
    smgOptions = new SMGOptions(Configuration.defaultConfiguration());
    initialState =
        SMGState.of(MACHINE_MODEL, logManager, smgOptions)
            .copyAndAddStackFrame(CFunctionDeclaration.DUMMY);

    // null for the constraintsStrengthenOperator is fine as long as we don't use it in tests!
    transferRelation =
        new SMGTransferRelation(
            logManager,
            smgOptions,
            SMGCPAExportOptions.getNoExportInstance(),
            MACHINE_MODEL,
            ImmutableList.of(),
            null);

    transferRelation.setInfo(
        initialState,
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
    // Make a non-global and not external variable with the current type
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
      // further, this memory is not written at all, meaning we can read it, and it returns UNKNOWN
      List<ValueAndSMGState> readValueAndState =
          state.readValue(memoryObject, offsetInBits, sizeInBits, null);
      Preconditions.checkArgument(readValueAndState.size() == 1);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.get(0).getValue().isExplicitlyKnown()).isFalse();
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
    // Make a non-global and not external variable with the current type
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
      // further, this memory is not written at all, meaning we can read it, and it returns the
      // numeric values assigned, starting from 1 and incrementing after each field once
      List<ValueAndSMGState> readValueAndState =
          state.readValue(memoryObject, offsetInBits, sizeInBits, null);
      Preconditions.checkArgument(readValueAndState.size() == 1);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();
      assertThat(readValueAndState.get(0).getValue().asNumericValue().bigInteger().intValueExact())
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
    // Make a non-global and not external variable with the current type
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
      // further, this memory is not written at all, meaning we can read it, and it returns UNKNOWN
      List<ValueAndSMGState> readValueAndState =
          state.readValue(memoryObject, offsetInBits, sizeInBits, null);
      Preconditions.checkArgument(readValueAndState.size() == 1);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.get(0).getValue().isExplicitlyKnown()).isFalse();
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
      // further, this memory is not written at all, meaning we can read it, and it returns UNKNOWN
      List<ValueAndSMGState> readValueAndState =
          state.readValue(memoryObject, offsetInBits, sizeInBits, null);
      Preconditions.checkArgument(readValueAndState.size() == 1);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.get(0).getValue().isExplicitlyKnown()).isFalse();
    }

    // Now we repeat this for an array (they behave the same, so if one is empty, we can assume that
    // all are)
    BigInteger offsetOfArrayInBits =
        MACHINE_MODEL.getFieldOffsetInBits(
            (CCompositeType) ((CElaboratedType) type).getRealType(),
            "ARRAY_" + STRUCT_UNION_FIELD_NAMES.get(0));
    BigInteger sizeOfArrayInBits = MACHINE_MODEL.getSizeofInBits(STRUCT_UNION_TEST_TYPES.get(0));
    for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
      List<ValueAndSMGState> readValueAndState =
          state.readValue(memoryObject, offsetOfArrayInBits, sizeOfArrayInBits, null);
      Preconditions.checkArgument(readValueAndState.size() == 1);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.get(0).getValue().isExplicitlyKnown()).isFalse();
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
    List<ValueAndSMGState> readValueAndState =
        state.readValue(memoryObject, offsetOfPointerInBits, sizeOfPointerInBits, null);
    Preconditions.checkArgument(readValueAndState.size() == 1);
    // The read state should not have any errors
    // TODO: error check
    assertThat(readValueAndState.get(0).getValue().isExplicitlyKnown()).isFalse();
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
    // Make a non-global and not external variable with the current type
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
      // further, this memory is not written at all, meaning we can read it, and it returns UNKNOWN
      List<ValueAndSMGState> readValueAndState =
          state.readValue(memoryObject, offsetInBits, sizeInBits, null);
      Preconditions.checkArgument(readValueAndState.size() == 1);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();
      assertThat(readValueAndState.get(0).getValue().asNumericValue().bigInteger())
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
      // further, this memory is not written at all, meaning we can read it, and it returns UNKNOWN
      List<ValueAndSMGState> readValueAndState =
          state.readValue(memoryObject, offsetInBits, sizeInBits, null);
      Preconditions.checkArgument(readValueAndState.size() == 1);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();
      assertThat(readValueAndState.get(0).getValue().asNumericValue().bigInteger())
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
        List<ValueAndSMGState> readValueAndState =
            state.readValue(memoryObject, offsetOfArrayInBits, sizeOfArrayInBits, null);
        Preconditions.checkArgument(readValueAndState.size() == 1);
        // There is a overflow happening here! a char is filled with a too large value
        // TODO: error check
        assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();

        if (readValueAndState
                .get(0)
                .getValue()
                .asNumericValue()
                .bigInteger()
                .compareTo(expectedValue)
            != 0) {
          // Overflow for char
          assertThat(
                  readValueAndState
                      .get(0)
                      .getValue()
                      .asNumericValue()
                      .bigInteger()
                      .add(BigInteger.valueOf(256)))
              .isEquivalentAccordingToCompareTo(expectedValue);
        } else {
          assertThat(readValueAndState.get(0).getValue().asNumericValue().bigInteger())
              .isEquivalentAccordingToCompareTo(expectedValue);
        }
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
      List<ValueAndSMGState> readValueAndState =
          state.readValue(memoryObject, offsetOfPointerInBits, sizeOfPointerInBits, null);
      Preconditions.checkArgument(readValueAndState.size() == 1);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();
      assertThat(readValueAndState.get(0).getValue().asNumericValue().bigInteger())
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
      // Make a non-global and not external variable with the current type
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
      // further, this memory is not written at all, meaning we can read it, and it returns UNKNOWN
      // Since this is an array, we read only the type size, but length times
      for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
        BigInteger offset = BigInteger.valueOf(i).multiply(typeSize);
        List<ValueAndSMGState> readValueAndState =
            state.readValue(memoryObject, offset, typeSize, null);
        Preconditions.checkArgument(readValueAndState.size() == 1);
        // The read state should not have any errors
        // TODO: error check
        assertThat(readValueAndState.get(0).getValue().isExplicitlyKnown()).isFalse();
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
      // Make a non-global and not external variable with the current type
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
        List<ValueAndSMGState> readValueAndState =
            state.readValue(memoryObject, offset, typeSize, null);
        Preconditions.checkArgument(readValueAndState.size() == 1);
        // The read state should not have any errors
        // TODO: error check
        assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();
        assertThat(
                readValueAndState.get(0).getValue().asNumericValue().bigInteger().intValueExact())
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
      // Make a non-global and not external variable with the current type
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
      // further, this memory is not written at all, meaning we can read it, and it returns UNKNOWN
      List<ValueAndSMGState> readValueAndState =
          state.readValue(memoryObject, BigInteger.ZERO, expectedSize, null);
      Preconditions.checkArgument(readValueAndState.size() == 1);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.get(0).getValue().isExplicitlyKnown()).isFalse();
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

      // Make a non-global and not external variable with the current type
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
      List<ValueAndSMGState> readValueAndState =
          state.readValue(memoryObject, BigInteger.ZERO, expectedSize, null);
      Preconditions.checkArgument(readValueAndState.size() == 1);
      // The read state should not have any errors
      // TODO: error check
      assertThat(readValueAndState.get(0).getValue().isExplicitlyKnown()).isTrue();
      assertThat(
              readValueAndState.get(0).getValue().asNumericValue().bigInteger().compareTo(value)
                  == 0)
          .isTrue();
      // We increment the value to make them distinct
      value = value.add(BigInteger.ONE);
    }
  }

  /*
   * Test basically int * pointer = malloc(...); with different sizes.
   * The sizes are entered using constant values.
   * The option for malloc failure is tested as well.
   */
  @Test
  public void localVariableDeclarationWithAssignmentMallocTest() throws CPATransferException {
    String variableName = "variableName";
    // Pointer size == int size
    CType sizeType = INT_TYPE;
    for (int i = 0; i < 5000; i = i + TEST_ARRAY_LENGTH.intValue()) {
      BigInteger sizeInBytes = BigInteger.valueOf(i);

      for (CType type : TEST_TYPES) {
        CType pointerType = new CPointerType(false, false, type);

        // Make a non-global and not external variable with the current type
        List<SMGState> statesAfterDecl =
            transferRelation.handleDeclarationEdge(
                null, declareVariableWithoutInitializer(variableName, pointerType, false, false));

        // Since we declare variables we know there will be only 1 state afterwards
        assertThat(statesAfterDecl).hasSize(1);
        // We check the variable later
        SMGState stateAfterDecl = statesAfterDecl.get(0);

        CFunctionCallAssignmentStatement mallocAndAssignmentExpr =
            new CFunctionCallAssignmentStatement(
                FileLocation.DUMMY,
                new CIdExpression(
                    FileLocation.DUMMY,
                    new CPointerType(false, false, pointerType),
                    variableName,
                    declareVariableWithoutInitializer(variableName, pointerType, false, false)),
                makeMalloc(
                    new CIntegerLiteralExpression(FileLocation.DUMMY, sizeType, sizeInBytes)));

        CStatementEdge mallocAndAssignmentEdge =
            new CStatementEdge(
                pointerType + " " + variableName + " = malloc(" + sizeInBytes + ")",
                mallocAndAssignmentExpr,
                FileLocation.DUMMY,
                CFANode.newDummyCFANode(),
                CFANode.newDummyCFANode());

        // Update transfer relation state
        transferRelation.setInfo(stateAfterDecl, null, mallocAndAssignmentEdge);

        Collection<SMGState> statesAfterMallocAssign =
            transferRelation.handleStatementEdge(mallocAndAssignmentEdge, mallocAndAssignmentExpr);

        assertThat(statesAfterMallocAssign instanceof List<?>).isTrue();
        List<SMGState> statesListAfterMallocAssign = (List<SMGState>) statesAfterMallocAssign;
        // Depending on the options this might have 2 return states, one where malloc succeeds and
        // one where it fails
        // TODO:
        SMGState stateAfterMallocAssignSuccess;
        if (sizeInBytes.compareTo(BigInteger.ZERO) == 0) {
          // malloc(0) is always a single null pointer
          assertThat(
                  checkMallocFailure(statesListAfterMallocAssign.get(0), variableName, pointerType))
              .isTrue();
          continue;

        } else if (!smgOptions.isEnableMallocFailure()) {
          assertThat(statesListAfterMallocAssign).hasSize(1);
          stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(0);

        } else {
          // Malloc can fail in this case, check the additional state
          assertThat(statesListAfterMallocAssign).hasSize(2);
          // (Since we return a list, the order should be success then failure)
          // It might however be that this is the wrong state, we check and flip them if necessary
          SMGState stateAfterMallocAssignFailure = statesListAfterMallocAssign.get(1);
          if (!checkMallocFailure(stateAfterMallocAssignFailure, variableName, pointerType)) {
            stateAfterMallocAssignFailure = statesListAfterMallocAssign.get(0);
            stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(1);
            assertThat(checkMallocFailure(stateAfterMallocAssignFailure, variableName, pointerType))
                .isTrue();
          } else {
            stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(0);
          }
        }
        // State 1 (always the malloc succeed case) has a variable with the pointer, which has a
        // value pointing to the memory allocated by malloc which has to have the sizeInBytes times
        // 8. No ErrorInfo should be set.

        // TODO: error check
        SymbolicProgramConfiguration memoryModel = stateAfterMallocAssignSuccess.getMemoryModel();
        assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
        SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
        // SMG sizes are in bits!
        BigInteger expectedSize = MACHINE_MODEL.getSizeofInBits(pointerType);
        assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
        // further, in the memory there must be a SMGValue that is a pointer (points to edge)
        // leading to the larger memory field with the size of malloc
        List<ValueAndSMGState> readValueAndState =
            stateAfterMallocAssignSuccess.readValue(
                memoryObject, BigInteger.ZERO, expectedSize, null);
        Preconditions.checkArgument(readValueAndState.size() == 1);
        // The read state should not have any errors
        // TODO: error check
        assertThat(memoryModel.isPointer(readValueAndState.get(0).getValue())).isTrue();
        SMGStateAndOptionalSMGObjectAndOffset mallocObjectAndOffset =
            stateAfterMallocAssignSuccess
                .dereferencePointer(readValueAndState.get(0).getValue())
                .get(0);
        assertThat(mallocObjectAndOffset.hasSMGObjectAndOffset()).isTrue();
        assertThat(
                mallocObjectAndOffset
                        .getSMGObject()
                        .getSize()
                        .compareTo(sizeInBytes.multiply(BigInteger.valueOf(8)))
                    == 0)
            .isTrue();
        assertThat(mallocObjectAndOffset.getSMGObject().getOffset().compareTo(BigInteger.ZERO) == 0)
            .isTrue();
        assertThat(mallocObjectAndOffset.getOffsetForObject().compareTo(BigInteger.ZERO) == 0)
            .isTrue();
        // Read the SMGObject to make sure that there is no value written
        // TODO:
      }
    }
  }

  /*
   * Test basically int * pointer = malloc(...); with different sizes but
   * always based on some binary expression multiplication of a sizeof.
   * The option for malloc failure is tested as well.
   */
  @Test
  public void localVariableDeclarationWithAssignmentMallocWithSizeOfBinaryExpressionTest()
      throws CPATransferException {
    String variableName = "variableName";
    for (int i = 0; i < 50; i++) {
      BigInteger sizeMultiplikator = BigInteger.valueOf(i);

      for (CType type : TEST_TYPES) {
        for (CType sizeofType : TEST_TYPES) {
          CType pointerType = new CPointerType(false, false, type);

          CExpression binarySizeExpression =
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  INT_TYPE,
                  INT_TYPE,
                  new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, sizeMultiplikator),
                  new CUnaryExpression(
                      FileLocation.DUMMY,
                      INT_TYPE,
                      new CIdExpression(
                          FileLocation.DUMMY,
                          new CVariableDeclaration(
                              FileLocation.DUMMY,
                              false,
                              CStorageClass.AUTO,
                              sizeofType,
                              "SomeTypeNotQual",
                              "SomeTypeNotQual",
                              "SomeType",
                              CDefaults.forType(sizeofType, FileLocation.DUMMY))),
                      CUnaryExpression.UnaryOperator.SIZEOF),
                  BinaryOperator.MULTIPLY);

          // Make a non-global and not external variable with the current type
          List<SMGState> statesAfterDecl =
              transferRelation.handleDeclarationEdge(
                  null, declareVariableWithoutInitializer(variableName, pointerType, false, false));

          // Since we declare variables we know there will be only 1 state afterwards
          assertThat(statesAfterDecl).hasSize(1);
          // We check the variable later
          SMGState stateAfterDecl = statesAfterDecl.get(0);

          CFunctionCallAssignmentStatement mallocAndAssignmentExpr =
              new CFunctionCallAssignmentStatement(
                  FileLocation.DUMMY,
                  new CIdExpression(
                      FileLocation.DUMMY,
                      new CPointerType(false, false, pointerType),
                      variableName,
                      declareVariableWithoutInitializer(variableName, pointerType, false, false)),
                  makeMalloc(binarySizeExpression));

          CStatementEdge mallocAndAssignmentEdge =
              new CStatementEdge(
                  pointerType
                      + " "
                      + variableName
                      + " = malloc("
                      + binarySizeExpression.toASTString()
                      + ")",
                  mallocAndAssignmentExpr,
                  FileLocation.DUMMY,
                  CFANode.newDummyCFANode(),
                  CFANode.newDummyCFANode());

          // Update transfer relation state
          transferRelation.setInfo(stateAfterDecl, null, mallocAndAssignmentEdge);

          Collection<SMGState> statesAfterMallocAssign =
              transferRelation.handleStatementEdge(
                  mallocAndAssignmentEdge, mallocAndAssignmentExpr);

          assertThat(statesAfterMallocAssign instanceof List<?>).isTrue();
          List<SMGState> statesListAfterMallocAssign = (List<SMGState>) statesAfterMallocAssign;
          // Depending on the options this might have 2 return states, one where malloc succeeds and
          // one where it fails
          // TODO:
          SMGState stateAfterMallocAssignSuccess;
          if (sizeMultiplikator.compareTo(BigInteger.ZERO) == 0) {
            // malloc(0) is always a single null pointer
            assertThat(
                    checkMallocFailure(
                        statesListAfterMallocAssign.get(0), variableName, pointerType))
                .isTrue();
            continue;

          } else if (!smgOptions.isEnableMallocFailure()) {
            assertThat(statesListAfterMallocAssign).hasSize(1);
            stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(0);

          } else {
            // Malloc can fail in this case, check the additional state
            assertThat(statesListAfterMallocAssign).hasSize(2);
            // (Since we return a list, the order should be success then failure)
            // It might however be that this is the wrong state, we check and flip them if necessary
            SMGState stateAfterMallocAssignFailure = statesListAfterMallocAssign.get(1);
            if (!checkMallocFailure(stateAfterMallocAssignFailure, variableName, pointerType)) {
              stateAfterMallocAssignFailure = statesListAfterMallocAssign.get(0);
              stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(1);
              assertThat(
                      checkMallocFailure(stateAfterMallocAssignFailure, variableName, pointerType))
                  .isTrue();
            } else {
              stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(0);
            }
          }
          // State 1 (always the malloc succeed case) has a variable with the pointer, which has a
          // value pointing to the memory allocated by malloc which has to have the sizeInBytes
          // times
          // 8. No ErrorInfo should be set.

          // TODO: error check
          SymbolicProgramConfiguration memoryModel = stateAfterMallocAssignSuccess.getMemoryModel();
          assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
          SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
          // SMG sizes are in bits!
          BigInteger expectedSize = MACHINE_MODEL.getSizeofInBits(pointerType);
          assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
          // further, in the memory there must be a SMGValue that is a pointer (points to edge)
          // leading to the larger memory field with the size of malloc
          List<ValueAndSMGState> readValueAndState =
              stateAfterMallocAssignSuccess.readValue(
                  memoryObject, BigInteger.ZERO, expectedSize, null);
          Preconditions.checkArgument(readValueAndState.size() == 1);
          // The read state should not have any errors
          // TODO: error check
          assertThat(memoryModel.isPointer(readValueAndState.get(0).getValue())).isTrue();
          SMGStateAndOptionalSMGObjectAndOffset mallocObjectAndOffset =
              stateAfterMallocAssignSuccess
                  .dereferencePointer(readValueAndState.get(0).getValue())
                  .get(0);
          assertThat(mallocObjectAndOffset.hasSMGObjectAndOffset()).isTrue();
          BigInteger expectedMemorySizeInBits =
              sizeMultiplikator
                  .multiply(BigInteger.valueOf(8))
                  .multiply(MACHINE_MODEL.getSizeof(sizeofType));
          assertThat(
                  mallocObjectAndOffset.getSMGObject().getSize().compareTo(expectedMemorySizeInBits)
                      == 0)
              .isTrue();
          assertThat(
                  mallocObjectAndOffset.getSMGObject().getOffset().compareTo(BigInteger.ZERO) == 0)
              .isTrue();
          assertThat(mallocObjectAndOffset.getOffsetForObject().compareTo(BigInteger.ZERO) == 0)
              .isTrue();
          // Read the SMGObject to make sure that there is no value written
          // TODO:
        }
      }
    }
  }

  /*
   * Checks the state for a variable with the name entered and the type entered.
   * This variable should have a failed malloc (value = 0) result.
   * If this is not the case, this method returns false.
   * True if the value read from the variable is zero, which is a malloc failure.
   */
  private boolean checkMallocFailure(
      SMGState stateAfterMallocAssignFailure, String variableName, CType pointerType)
      throws SMG2Exception {
    SymbolicProgramConfiguration memoryModel = stateAfterMallocAssignFailure.getMemoryModel();
    assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
    SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
    // SMG sizes are in bits!
    BigInteger expectedSize = MACHINE_MODEL.getSizeofInBits(pointerType);
    assertThat(memoryObject.getSize().compareTo(expectedSize) == 0).isTrue();
    // further, in the memory there must be a SMGValue that is a pointer (points to edge)
    // leading to the larger memory field with the size of malloc
    List<ValueAndSMGState> readValueAndState =
        stateAfterMallocAssignFailure.readValue(memoryObject, BigInteger.ZERO, expectedSize, null);
    Preconditions.checkArgument(readValueAndState.size() == 1);
    // The read state should not have any errors
    // TODO: error check
    // If now the read value is not numeric (!= 0) it can't be a malloc failure
    if (!readValueAndState.get(0).getValue().isNumericValue()) {
      return false;
    }

    assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();
    assertThat(
            readValueAndState
                    .get(0)
                    .getValue()
                    .asNumericValue()
                    .bigInteger()
                    .compareTo(BigInteger.ZERO)
                == 0)
        .isTrue();
    assertThat(memoryModel.getSMGValueFromValue(readValueAndState.get(0).getValue()).orElseThrow())
        .isEqualTo(SMGValue.zeroValue());
    return true;
  }

  /*
   * Tests essentially:
   * type testArray[size] = {....};
   * For the declareArrayVariableWithSimpleTypeWithValuesOnTheStack() method.
   */
  @Test
  public void checkStackArrayWithAssignments() throws CPATransferException {
    String variableName = "testArray";
    BigInteger[] values = new BigInteger[TEST_ARRAY_LENGTH.intValue()];

    for (CType testType : ARRAY_TEST_TYPES) {
      variableName = variableName + testType;
      for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
        if (((CSimpleType) testType).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          values[i] = BigInteger.valueOf(-i);
        } else {
          values[i] = BigInteger.valueOf(i);
        }
      }
      // Declares the array on the stack and assigns the values
      SMGState stateWithArray =
          declareArrayVariableWithSimpleTypeWithValuesOnTheStack(
              TEST_ARRAY_LENGTH.intValue(), values, variableName, testType);

      SymbolicProgramConfiguration memoryModel = stateWithArray.getMemoryModel();
      assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
      SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
      // SMG sizes are in bits!
      BigInteger expectedTypeSizeInBits = MACHINE_MODEL.getSizeofInBits(testType);
      // The size of the variable is the array size as it is on the stack
      assertThat(
              memoryObject.getSize().compareTo(expectedTypeSizeInBits.multiply(TEST_ARRAY_LENGTH))
                  == 0)
          .isTrue();

      for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
        BigInteger offsetInBits = BigInteger.valueOf(i).multiply(expectedTypeSizeInBits);
        List<ValueAndSMGState> readValueAndState =
            stateWithArray.readValue(memoryObject, offsetInBits, expectedTypeSizeInBits, null);
        Preconditions.checkArgument(readValueAndState.size() == 1);
        // The read state should not have any errors
        // TODO: error check
        // The value should be numeric
        assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();

        // Check the value (chars are also numerically saved!)
        BigInteger expectedValue;
        if (((CSimpleType) testType).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          expectedValue = BigInteger.valueOf(-i);
        } else {
          expectedValue = BigInteger.valueOf(i);
        }
        assertThat(
                readValueAndState
                        .get(0)
                        .getValue()
                        .asNumericValue()
                        .bigInteger()
                        .compareTo(expectedValue)
                    == 0)
            .isTrue();
      }
    }
  }

  /*
   * Tests essentially:
   * type * testName = malloc(size * sizeof(type));
   * testName[0] = value;
   * ....
   * For the declareArrayVariableWithSimpleTypeWithValuesOnTheHeap() method.
   */
  @Test
  public void checkHeapArrayWithAssignments() throws CPATransferException {
    String variableName = "testArray";
    BigInteger[] values = new BigInteger[TEST_ARRAY_LENGTH.intValue()];

    for (CType testType : ARRAY_TEST_TYPES) {
      for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
        if (((CSimpleType) testType).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          values[i] = BigInteger.valueOf(-i);
        } else {
          values[i] = BigInteger.valueOf(i);
        }
      }
      // Declares the array on the stack and assigns the values
      SMGState stateWithArray =
          declareArrayVariableWithSimpleTypeWithValuesOnTheHeap(
              TEST_ARRAY_LENGTH.intValue(), values, variableName, testType);

      SymbolicProgramConfiguration memoryModel = stateWithArray.getMemoryModel();
      assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
      SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
      // SMG sizes are in bits!
      BigInteger expectedTypeSizeInBits = MACHINE_MODEL.getSizeofInBits(testType);
      // The size of the variable is the size of a pointer
      assertThat(memoryObject.getSize().compareTo(BigInteger.valueOf(POINTER_SIZE_IN_BITS)) == 0)
          .isTrue();
      // Read the address from the variable and then dereference the pointer
      List<ValueAndSMGState> readAddressValueAndState =
          stateWithArray.readValue(
              memoryObject, BigInteger.ZERO, BigInteger.valueOf(POINTER_SIZE_IN_BITS), null);
      Preconditions.checkArgument(readAddressValueAndState.size() == 1);
      Value address = readAddressValueAndState.get(0).getValue();
      // TODO: address this type thing
      // assertThat(address instanceof ConstantSymbolicExpression).isTrue();
      SMGStateAndOptionalSMGObjectAndOffset maybeTargetOfPointer =
          stateWithArray.dereferencePointer(address).get(0);
      assertThat(maybeTargetOfPointer.hasSMGObjectAndOffset()).isTrue();
      SMGStateAndOptionalSMGObjectAndOffset targetOfPointer = maybeTargetOfPointer;
      // The offset of the address should be 0
      assertThat(targetOfPointer.getOffsetForObject().compareTo(BigInteger.ZERO)).isEqualTo(0);
      SMGObject arrayMemoryObject = targetOfPointer.getSMGObject();
      // The object is not 0
      assertThat(arrayMemoryObject.isZero()).isFalse();

      for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
        BigInteger offsetInBits = BigInteger.valueOf(i).multiply(expectedTypeSizeInBits);
        List<ValueAndSMGState> readValueAndState =
            stateWithArray.readValue(arrayMemoryObject, offsetInBits, expectedTypeSizeInBits, null);
        // The read state should not have any errors
        // TODO: error check
        // The value should be numeric
        assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();

        // Check the value (chars are also numerically saved!)
        BigInteger expectedValue;
        if (((CSimpleType) testType).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          expectedValue = BigInteger.valueOf(-i);
        } else {
          expectedValue = BigInteger.valueOf(i);
        }
        assertThat(
                readValueAndState
                        .get(0)
                        .getValue()
                        .asNumericValue()
                        .bigInteger()
                        .compareTo(expectedValue)
                    == 0)
            .isTrue();
      }
    }
  }

  /*
   * Tests essentially:
   * struct StructType variableName =  {....};
   * For the declareStructVariableWithSimpleTypeWithValuesOnTheStack() method.
   */
  @Test
  public void checkStackStructWithAssignments() throws CPATransferException {
    String variableName = "testStruct";
    String structTypeName = "testStructType";

    for (int sublist = 1; sublist < STRUCT_UNION_TEST_TYPES.size(); sublist++) {
      variableName = variableName + "sublist" + sublist;
      structTypeName = structTypeName + "sublist" + sublist;
      List<CType> structTestTypes = STRUCT_UNION_TEST_TYPES.subList(0, sublist);
      List<String> structFieldNames = STRUCT_UNION_FIELD_NAMES.subList(0, sublist);
      BigInteger[] values = new BigInteger[structTestTypes.size()];
      for (int i = 0; i < structTestTypes.size(); i++) {
        if (((CSimpleType) structTestTypes.get(i)).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          values[i] = BigInteger.valueOf(-i);
        } else {
          values[i] = BigInteger.valueOf(i);
        }
      }
      CType structType =
          makeElaboratedTypeFor(
              structTypeName, ComplexTypeKind.STRUCT, structTestTypes, structFieldNames);
      // Declares the struct on the stack and assigns the values
      SMGState stateWithStruct =
          declareStructVariableWithSimpleTypeWithValuesOnTheStack(
              values, variableName, structType, structTestTypes);

      SymbolicProgramConfiguration memoryModel = stateWithStruct.getMemoryModel();
      assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
      SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
      // The size of the variable is the size of the struct
      BigInteger sizeOfStructInBits = MACHINE_MODEL.getSizeofInBits(structType);
      assertThat(memoryObject.getSize().compareTo(sizeOfStructInBits) == 0).isTrue();
      // The object is not 0
      assertThat(memoryObject.isZero()).isFalse();

      // Read the values we entered
      for (int i = 0; i < structTestTypes.size(); i++) {
        CType currentFieldType = structTestTypes.get(i);
        BigInteger expectedTypeSizeInBits = MACHINE_MODEL.getSizeofInBits(currentFieldType);
        BigInteger offsetInBits =
            MACHINE_MODEL.getFieldOffsetInBits(
                (CCompositeType) ((CElaboratedType) structType).getRealType(),
                structFieldNames.get(i));
        List<ValueAndSMGState> readValueAndState =
            stateWithStruct.readValue(memoryObject, offsetInBits, expectedTypeSizeInBits, null);
        Preconditions.checkArgument(readValueAndState.size() == 1);
        // The read state should not have any errors
        // TODO: error check
        // The value should be numeric
        assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();

        assertThat(currentFieldType instanceof CSimpleType).isTrue();
        // Check the value (chars are also numerically saved!)
        BigInteger expectedValue;
        if (((CSimpleType) currentFieldType).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          expectedValue = BigInteger.valueOf(-i);
        } else {
          expectedValue = BigInteger.valueOf(i);
        }
        assertThat(
                readValueAndState
                        .get(0)
                        .getValue()
                        .asNumericValue()
                        .bigInteger()
                        .compareTo(expectedValue)
                    == 0)
            .isTrue();
      }
    }
  }

  /*
   * Tests essentially:
   * struct StructType * variableName = malloc(sizeof(struct StructType));
   * For the declareStructVariableWithSimpleTypeWithValuesOnTheHeap() method.
   */
  @Test
  public void checkHeapStructWithAssignments() throws CPATransferException {
    String variableName = "testStruct";
    String structTypeName = "testStructType";

    for (int sublist = 1; sublist < STRUCT_UNION_TEST_TYPES.size(); sublist++) {
      List<CType> structTestTypes = STRUCT_UNION_TEST_TYPES.subList(0, sublist);
      List<String> structFieldNames = STRUCT_UNION_FIELD_NAMES.subList(0, sublist);
      BigInteger[] values = new BigInteger[structTestTypes.size()];
      for (int i = 0; i < structTestTypes.size(); i++) {
        if (((CSimpleType) structTestTypes.get(i)).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          values[i] = BigInteger.valueOf(-i);
        } else {
          values[i] = BigInteger.valueOf(i);
        }
      }
      CType structType =
          makeElaboratedTypeFor(
              structTypeName, ComplexTypeKind.STRUCT, structTestTypes, structFieldNames);
      // Declares the struct on the heap with malloc and assigns the values
      SMGState stateWithStruct =
          declareStructVariableWithSimpleTypeWithValuesOnTheHeap(values, variableName, structType);

      SymbolicProgramConfiguration memoryModel = stateWithStruct.getMemoryModel();
      assertThat(memoryModel.getStackFrames().peek().containsVariable(variableName)).isTrue();
      SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableName);
      // The size of the variable is the size of a pointer
      assertThat(memoryObject.getSize().compareTo(BigInteger.valueOf(POINTER_SIZE_IN_BITS)) == 0)
          .isTrue();
      // Read the address from the variable and then dereference the pointer
      List<ValueAndSMGState> readAddressValueAndState =
          stateWithStruct.readValue(
              memoryObject, BigInteger.ZERO, BigInteger.valueOf(POINTER_SIZE_IN_BITS), null);
      Preconditions.checkArgument(readAddressValueAndState.size() == 1);
      Value address = readAddressValueAndState.get(0).getValue();
      // assertThat(address instanceof ConstantSymbolicExpression).isTrue();
      SMGStateAndOptionalSMGObjectAndOffset maybeTargetOfPointer =
          stateWithStruct.dereferencePointer(address).get(0);
      assertThat(maybeTargetOfPointer.hasSMGObjectAndOffset()).isTrue();
      SMGStateAndOptionalSMGObjectAndOffset targetOfPointer = maybeTargetOfPointer;
      // The offset of the address should be 0
      assertThat(targetOfPointer.getOffsetForObject().compareTo(BigInteger.ZERO)).isEqualTo(0);
      SMGObject arrayMemoryObject = targetOfPointer.getSMGObject();
      // The object is not 0
      assertThat(arrayMemoryObject.isZero()).isFalse();

      for (int i = 0; i < structTestTypes.size(); i++) {
        CType currentFieldType = structTestTypes.get(i);
        BigInteger expectedTypeSizeInBits = MACHINE_MODEL.getSizeofInBits(currentFieldType);
        BigInteger offsetInBits =
            MACHINE_MODEL.getFieldOffsetInBits(
                (CCompositeType) ((CElaboratedType) structType).getRealType(),
                structFieldNames.get(i));
        List<ValueAndSMGState> readValueAndState =
            stateWithStruct.readValue(
                arrayMemoryObject, offsetInBits, expectedTypeSizeInBits, null);
        // The read state should not have any errors
        // TODO: error check
        // The value should be numeric
        assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();

        assertThat(currentFieldType instanceof CSimpleType).isTrue();
        // Check the value (chars are also numerically saved!)
        BigInteger expectedValue;
        if (((CSimpleType) currentFieldType).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          expectedValue = BigInteger.valueOf(-i);
        } else {
          expectedValue = BigInteger.valueOf(i);
        }
        assertThat(
                readValueAndState
                        .get(0)
                        .getValue()
                        .asNumericValue()
                        .bigInteger()
                        .compareTo(expectedValue)
                    == 0)
            .isTrue();
      }
    }
  }

  /*
   * Declares variables for most simple types and assigns a value to them.
   * Test for declareSimpleTypedTestVariablesWithValues().
   */
  @Test
  public void checkStackVariablesWithSimpleTypesAndValues() throws CPATransferException {
    for (CType type : TEST_TYPES) {
      String variableName = "testVariable_" + type;
      // 2 because we test it once for positive and once for negative arguments
      for (int i = 0; i < 2; i++) {
        String variableNamePlus = variableName + i;
        BigInteger value;
        if (((CSimpleType) type).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          value = BigInteger.valueOf(-i);
        } else {
          value = BigInteger.valueOf(i);
        }

        // Declares a variable on the stack with the given name and type and value
        SMGState stateWithStruct =
            declareSimpleTypedTestVariablesWithValues(value, variableNamePlus, type);

        SymbolicProgramConfiguration memoryModel = stateWithStruct.getMemoryModel();
        assertThat(memoryModel.getStackFrames().peek().containsVariable(variableNamePlus)).isTrue();
        SMGObject memoryObject = memoryModel.getStackFrames().peek().getVariable(variableNamePlus);
        // The size of the variable is the size of the type
        BigInteger expectedTypeSizeInBits = MACHINE_MODEL.getSizeofInBits(type);
        assertThat(memoryObject.getSize().compareTo(expectedTypeSizeInBits) == 0).isTrue();

        List<ValueAndSMGState> readValueAndState =
            stateWithStruct.readValue(memoryObject, BigInteger.ZERO, expectedTypeSizeInBits, null);
        Preconditions.checkArgument(readValueAndState.size() == 1);
        // The read state should not have any errors
        // TODO: error check
        // The value should be numeric
        assertThat(readValueAndState.get(0).getValue().isNumericValue()).isTrue();

        assertThat(
                readValueAndState.get(0).getValue().asNumericValue().bigInteger().compareTo(value)
                    == 0)
            .isTrue();
      }
    }
  }

  /*
   * Test basic boolean 1 == 1, 0 == 0, numbers/chars == true and even address == address true
   *  with constant values in the CBinaryExpression.
   */
  @Test
  public void testBasicTrueAssumptionWithTrueTruthAssumption()
      throws CPATransferException, InterruptedException {
    for (CType type : STRUCT_UNION_TEST_TYPES) {
      for (int i = -TEST_ARRAY_LENGTH.intValue(); i < TEST_ARRAY_LENGTH.intValue(); i++) {
        BigInteger testValue = BigInteger.valueOf(i);
        if (type.equals(UNSIGNED_SHORT_TYPE)
            || type.equals(UNSIGNED_INT_TYPE)
            || type.equals(UNSIGNED_LONG_TYPE)
            || type.equals(CHAR_TYPE)) {
          testValue = testValue.abs();
        }
        CExpression equality;
        if (type.equals(CHAR_TYPE)) {
          equality =
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  INT_TYPE,
                  INT_TYPE,
                  makeCharExpressionFrom((char) testValue.intValue()),
                  makeCharExpressionFrom((char) testValue.intValue()),
                  BinaryOperator.EQUALS);
        } else {
          equality =
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  INT_TYPE,
                  INT_TYPE,
                  makeIntegerExpressionFrom(testValue, (CSimpleType) INT_TYPE),
                  makeIntegerExpressionFrom(testValue, (CSimpleType) INT_TYPE),
                  BinaryOperator.EQUALS);
        }
        Collection<SMGState> statesAfter = transferRelation.handleAssumption(null, equality, true);

        // The result is the unchanged initial state as we used constants only in the binary
        // expression and nothing was learned, read etc.
        assertThat(statesAfter).hasSize(1);
        for (SMGState state : statesAfter) {
          assertThat(state).isEqualTo(initialState);
        }
      }
    }
  }

  /*
   * Test basic boolean 1 == 1, 0 == 0  with constant values in the CBinaryExpression.
   * But with false truth assumption so everything is false. Null as return is expected.
   */
  @Test
  public void testBasicTrueAssumptionWithFalseTruthAssumption()
      throws CPATransferException, InterruptedException {
    for (CType type : STRUCT_UNION_TEST_TYPES) {
      for (int i = -TEST_ARRAY_LENGTH.intValue(); i < TEST_ARRAY_LENGTH.intValue(); i++) {
        BigInteger testValue = BigInteger.valueOf(i);
        if (type.equals(UNSIGNED_SHORT_TYPE)
            || type.equals(UNSIGNED_INT_TYPE)
            || type.equals(UNSIGNED_LONG_TYPE)
            || type.equals(CHAR_TYPE)) {
          testValue = testValue.abs();
        }
        CExpression equality;
        if (type.equals(CHAR_TYPE)) {
          equality =
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  INT_TYPE,
                  INT_TYPE,
                  makeCharExpressionFrom((char) testValue.intValue()),
                  makeCharExpressionFrom((char) testValue.intValue()),
                  BinaryOperator.EQUALS);
        } else {
          equality =
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  INT_TYPE,
                  INT_TYPE,
                  makeIntegerExpressionFrom(testValue, (CSimpleType) INT_TYPE),
                  makeIntegerExpressionFrom(testValue, (CSimpleType) INT_TYPE),
                  BinaryOperator.EQUALS);
        }
        Collection<SMGState> statesAfter = transferRelation.handleAssumption(null, equality, false);

        // The truth assumption is false -> the true assumption gets turned to false -> null return
        assertThat(statesAfter).isNull();
      }
    }
  }

  /*
   * Tests the equality of 2 struct addresses which are distinct!
   * Which means the == is false, and it may never learn that the 2 symbolic
   * expressions are equal as addresses only change through SMG merges.
   */
  @Test
  public void testHeapStructAddressEqualityTrueAssumptionWithTrueTruthAssumption()
      throws CPATransferException, InterruptedException {
    for (int sublist = 1; sublist < STRUCT_UNION_TEST_TYPES.size(); sublist++) {
      String structTypeName = "testStructType" + sublist;
      String variableName1 = "testStruct1_" + sublist;
      String variableName2 = "testStruct2_" + sublist;
      List<CType> structTestTypes = STRUCT_UNION_TEST_TYPES.subList(0, sublist);
      List<String> structFieldNames = STRUCT_UNION_FIELD_NAMES.subList(0, sublist);
      BigInteger[] values = new BigInteger[structTestTypes.size()];
      for (int i = 0; i < structTestTypes.size(); i++) {
        if (((CSimpleType) structTestTypes.get(i)).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          values[i] = BigInteger.valueOf(-i);
        } else {
          values[i] = BigInteger.valueOf(i);
        }
      }
      CType structType =
          makeElaboratedTypeFor(
              structTypeName, ComplexTypeKind.STRUCT, structTestTypes, structFieldNames);

      // Declares a struct on the heap with malloc and assigns the values
      declareStructVariableWithSimpleTypeWithValuesOnTheHeap(values, variableName1, structType);
      // Declares a new struct on the heap with malloc and assigns the values
      declareStructVariableWithSimpleTypeWithValuesOnTheHeap(values, variableName2, structType);

      CExpression equality =
          new CBinaryExpression(
              FileLocation.DUMMY,
              INT_TYPE,
              INT_TYPE,
              new CIdExpression(
                  FileLocation.DUMMY,
                  new CPointerType(false, false, structType),
                  variableName1,
                  new CVariableDeclaration(
                      FileLocation.DUMMY,
                      false,
                      CStorageClass.AUTO,
                      new CPointerType(false, false, structType),
                      variableName1,
                      variableName1,
                      variableName1,
                      null)),
              new CIdExpression(
                  FileLocation.DUMMY,
                  new CPointerType(false, false, structType),
                  variableName2,
                  new CVariableDeclaration(
                      FileLocation.DUMMY,
                      false,
                      CStorageClass.AUTO,
                      new CPointerType(false, false, structType),
                      variableName2,
                      variableName2,
                      variableName2,
                      null)),
              BinaryOperator.EQUALS);

      Collection<SMGState> statesAfter = transferRelation.handleAssumption(null, equality, true);

      // The truth assumption is false -> the true assumption gets turned to false -> null return
      // The reason is that these are 2 distinct addresses, and it's not allowed to assume them
      // equal!
      assertThat(statesAfter).isNull();
    }
  }

  /*
   * Tests the equality of 2 array addresses which are distinct!
   * Which means the == is false, and it may never learn that the 2 symbolic
   * expressions are equal as addresses only change through SMG merges.
   */
  @Test
  public void testHeapArrayAddressEqualityFalseAssumptionWithTrueTruthAssumption()
      throws CPATransferException, InterruptedException {
    for (CType testType : ARRAY_TEST_TYPES) {
      String variableName1 = "testStruct1_" + testType;
      String variableName2 = "testStruct2_" + testType;
      BigInteger[] values = new BigInteger[TEST_ARRAY_LENGTH.intValue()];
      for (int i = 0; i < TEST_ARRAY_LENGTH.intValue(); i++) {
        if (((CSimpleType) testType).isSigned() && Math.floorMod(i, 2) == 1) {
          // Make every second value a negative for signed values
          values[i] = BigInteger.valueOf(-i);
        } else {
          values[i] = BigInteger.valueOf(i);
        }
      }
      // Declares 2 distinct arrays on the heap and assigns the values
      declareArrayVariableWithSimpleTypeWithValuesOnTheHeap(
          TEST_ARRAY_LENGTH.intValue(), values, variableName1, testType);
      declareArrayVariableWithSimpleTypeWithValuesOnTheHeap(
          TEST_ARRAY_LENGTH.intValue(), values, variableName2, testType);

      CExpression equality =
          new CBinaryExpression(
              FileLocation.DUMMY,
              INT_TYPE,
              INT_TYPE,
              new CIdExpression(
                  FileLocation.DUMMY,
                  new CPointerType(false, false, testType),
                  variableName1,
                  new CVariableDeclaration(
                      FileLocation.DUMMY,
                      false,
                      CStorageClass.AUTO,
                      new CPointerType(false, false, testType),
                      variableName1,
                      variableName1,
                      variableName1,
                      null)),
              new CIdExpression(
                  FileLocation.DUMMY,
                  new CPointerType(false, false, testType),
                  variableName2,
                  new CVariableDeclaration(
                      FileLocation.DUMMY,
                      false,
                      CStorageClass.AUTO,
                      new CPointerType(false, false, testType),
                      variableName2,
                      variableName2,
                      variableName2,
                      null)),
              BinaryOperator.EQUALS);

      Collection<SMGState> statesAfter = transferRelation.handleAssumption(null, equality, true);

      // The truth assumption is false -> null return (because true truth assumption)
      // The reason is that these are 2 distinct addresses, and it's not allowed to assume them
      // equal!
      assertThat(statesAfter).isNull();
    }
  }

  /*
   * Test basic boolean false 1 == 0, -1 == 0 with constant values in the CBinaryExpression.
   * But with true truth assumption so everything is false. Null as return is expected.
   */
  @Test
  public void testBasicFalseAssumptionWithTrueTruthAssumption()
      throws CPATransferException, InterruptedException {
    for (CType type : STRUCT_UNION_TEST_TYPES) {
      for (int i = -TEST_ARRAY_LENGTH.intValue(); i < TEST_ARRAY_LENGTH.intValue(); i++) {
        BigInteger testValue = BigInteger.valueOf(i);
        BigInteger secondValue = testValue.add(BigInteger.ONE);
        if (type.equals(UNSIGNED_SHORT_TYPE)
            || type.equals(UNSIGNED_INT_TYPE)
            || type.equals(UNSIGNED_LONG_TYPE)
            || type.equals(CHAR_TYPE)) {
          testValue = testValue.abs();
          secondValue = secondValue.abs();
        }
        CExpression equality;
        if (type.equals(CHAR_TYPE)) {
          equality =
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  INT_TYPE,
                  INT_TYPE,
                  makeCharExpressionFrom((char) testValue.intValue()),
                  makeCharExpressionFrom((char) secondValue.intValue()),
                  BinaryOperator.EQUALS);
        } else {
          equality =
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  INT_TYPE,
                  INT_TYPE,
                  makeIntegerExpressionFrom(testValue, (CSimpleType) INT_TYPE),
                  makeIntegerExpressionFrom(secondValue, (CSimpleType) INT_TYPE),
                  BinaryOperator.EQUALS);
        }
        Collection<SMGState> statesAfter = transferRelation.handleAssumption(null, equality, true);

        // False statement with true truth -> false -> null
        assertThat(statesAfter).isNull();
      }
    }
  }

  /*
   * Test basic boolean false; 1 == 0, 0 == 1 with constant values in the CBinaryExpression.
   * But with false truth assumption so everything is true.
   */
  @Test
  public void testBasicFalseAssumptionWithFalseTruthAssumption()
      throws CPATransferException, InterruptedException {
    for (CType type : STRUCT_UNION_TEST_TYPES) {
      for (int i = -TEST_ARRAY_LENGTH.intValue(); i < TEST_ARRAY_LENGTH.intValue(); i++) {
        BigInteger testValue = BigInteger.valueOf(i);
        BigInteger secondValue = testValue.add(BigInteger.ONE);
        if (type.equals(UNSIGNED_SHORT_TYPE)
            || type.equals(UNSIGNED_INT_TYPE)
            || type.equals(UNSIGNED_LONG_TYPE)
            || type.equals(CHAR_TYPE)) {
          testValue = testValue.abs();
          secondValue = secondValue.abs();
        }
        CExpression equality;
        if (type.equals(CHAR_TYPE)) {
          equality =
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  INT_TYPE,
                  INT_TYPE,
                  makeCharExpressionFrom((char) testValue.intValue()),
                  makeCharExpressionFrom((char) secondValue.intValue()),
                  BinaryOperator.EQUALS);
        } else {
          equality =
              new CBinaryExpression(
                  FileLocation.DUMMY,
                  INT_TYPE,
                  INT_TYPE,
                  makeIntegerExpressionFrom(testValue, (CSimpleType) INT_TYPE),
                  makeIntegerExpressionFrom(secondValue, (CSimpleType) INT_TYPE),
                  BinaryOperator.EQUALS);
        }
        Collection<SMGState> statesAfter = transferRelation.handleAssumption(null, equality, false);

        // false statement w false truth assumption -> true -> no state change
        // because we used constants only
        assertThat(statesAfter).hasSize(1);
        for (SMGState state : statesAfter) {
          assertThat(state).isEqualTo(initialState);
        }
      }
    }
  }

  /*
   * Declares a variable with the entered name, type and value assignment on the stack with the
   * entered value and then uses the state with it and updates the transfer relation.
   */
  private SMGState declareSimpleTypedTestVariablesWithValues(
      BigInteger value, String variableName, CType type) throws CPATransferException {
    CExpression exprToInit;
    if (type == CNumericTypes.CHAR) {
      exprToInit = makeCharExpressionFrom((char) value.intValue());
    } else {
      exprToInit = makeIntegerExpressionFrom(value, (CSimpleType) type);
    }
    CInitializer initializer = makeCInitializerExpressionFor(exprToInit);

    // Make a non-global and not external variable with the current type
    List<SMGState> statesAfterDecl =
        transferRelation.handleDeclarationEdge(
            null, declareVariableWithInitializer(variableName, type, false, false, initializer));
    // Since we declare variables we know there will be only 1 state afterwards
    assertThat(statesAfterDecl).hasSize(1);
    // This state must have a local variable the size of the type used (on the current stack
    // frame)
    // The state should not have any errors
    SMGState state = statesAfterDecl.get(0);
    // Technically the Edge is always wrong. This influences only error detection, however.
    transferRelation.setInfo(
        state,
        null,
        new CDeclarationEdge(
            "", FileLocation.DUMMY, CFANode.newDummyCFANode(), CFANode.newDummyCFANode(), null));
    return state;
  }

  /*
   * Declare an array with the type and size entered, the values entered the order entered and
   * the variableName entered on the stack.
   * Returns the last state acquired, but also sets the transfer
   * relation with this state, so it can be ignored.
   *
   * This is tested in a dedicated test and therefore save to use in other tests!
   *
   */
  private SMGState declareArrayVariableWithSimpleTypeWithValuesOnTheStack(
      int size, BigInteger[] values, String variableName, CType type) throws CPATransferException {
    Preconditions.checkArgument(values.length == size);
    // Build assignments starting from numeric value 0 and increment after each assignment. Chars
    // are simply the int cast to a char.
    ImmutableList.Builder<CInitializer> listOfInitsBuilder = ImmutableList.builder();
    for (int i = 0; i < size; i++) {
      CExpression exprToInit;
      if (type == CNumericTypes.CHAR) {
        exprToInit = makeCharExpressionFrom((char) values[i].intValue());
      } else {
        exprToInit = makeIntegerExpressionFrom(values[i], (CSimpleType) type);
      }
      listOfInitsBuilder.add(makeCInitializerExpressionFor(exprToInit));
    }
    CInitializer initList = makeCInitializerListFor(listOfInitsBuilder.build());
    // Make a non-global and not external variable with the current type
    CType arrayType = makeArrayTypeFor(type, BigInteger.valueOf(size));
    List<SMGState> statesAfterDecl =
        transferRelation.handleDeclarationEdge(
            null, declareVariableWithInitializer(variableName, arrayType, false, false, initList));
    // Since we declare variables we know there will be only 1 state afterwards
    assertThat(statesAfterDecl).hasSize(1);
    // This state must have a local variable the size of the type used (on the current stack
    // frame)
    // The state should not have any errors
    SMGState state = statesAfterDecl.get(0);
    // Technically the Edge is always wrong. This influences only error detection, however.
    transferRelation.setInfo(
        state,
        null,
        new CDeclarationEdge(
            "", FileLocation.DUMMY, CFANode.newDummyCFANode(), CFANode.newDummyCFANode(), null));

    // Assign values
    SMGState lastState = state;
    BigInteger index = BigInteger.ZERO;
    for (BigInteger value : values) {
      CExpression valueExpr;
      if (type.equals(CHAR_TYPE)) {
        valueExpr =
            new CCharLiteralExpression(FileLocation.DUMMY, type, (char) value.intValueExact());
      } else if (type instanceof CSimpleType) {
        // all other currently used test types are in this category
        valueExpr = new CIntegerLiteralExpression(FileLocation.DUMMY, type, value);
      } else {
        // TODO: add arrays/structs
        throw new RuntimeException(
            "Unsupported type in struct test type. Add the handling to where the exception is"
                + " thrown.");
      }

      CExpressionAssignmentStatement assignmentExpr =
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY,
              new CArraySubscriptExpression(
                  FileLocation.DUMMY,
                  type,
                  new CIdExpression(
                      FileLocation.DUMMY,
                      arrayType,
                      variableName,
                      new CVariableDeclaration(
                          FileLocation.DUMMY,
                          false,
                          CStorageClass.AUTO,
                          arrayType,
                          variableName,
                          variableName,
                          variableName,
                          null)),
                  new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, index)),
              valueExpr);

      Collection<SMGState> statesAfterAssign =
          transferRelation.handleStatementEdge(null, assignmentExpr);
      // Since we assign variables we know there will be only 1 state afterwards
      assertThat(statesAfterAssign).hasSize(1);
      SMGState newState = state;
      for (SMGState stateAS : statesAfterAssign) {
        newState = stateAS;
      }
      // Technically the Edge is always wrong. This influences only error detection, however.
      transferRelation.setInfo(
          newState,
          null,
          new CDeclarationEdge(
              "", FileLocation.DUMMY, CFANode.newDummyCFANode(), CFANode.newDummyCFANode(), null));
      lastState = newState;
      index = index.add(BigInteger.ONE);
    }
    return lastState;
  }

  /*
   * Declare an array with the type and size entered, the values entered in the order entered on the
   * heap. Then a variable with the address of this array is made with the variableName. Returns the
   * last state acquired, but also sets the transfer relation with this state, so it can be ignored.
   *
   * This is tested in a dedicated test and therefore save to use in other tests!
   */
  private SMGState declareArrayVariableWithSimpleTypeWithValuesOnTheHeap(
      int size, BigInteger[] values, String variableName, CType type) throws CPATransferException {
    Preconditions.checkArgument(values.length == size);
    CType pointerType = new CPointerType(false, false, type);

    CExpression sizeExpression =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            INT_TYPE,
            BigInteger.valueOf(size).multiply(MACHINE_MODEL.getSizeof(type)));

    // Make a non-global and not external variable with the current type
    List<SMGState> statesAfterDecl =
        transferRelation.handleDeclarationEdge(
            null, declareVariableWithoutInitializer(variableName, pointerType, false, false));

    // Since we declare variables we know there will be only 1 state afterwards
    assertThat(statesAfterDecl).hasSize(1);
    // We check the variable later
    SMGState stateAfterDecl = statesAfterDecl.get(0);

    CFunctionCallAssignmentStatement mallocAndAssignmentExpr =
        new CFunctionCallAssignmentStatement(
            FileLocation.DUMMY,
            new CIdExpression(
                FileLocation.DUMMY,
                pointerType,
                variableName,
                declareVariableWithoutInitializer(variableName, pointerType, false, false)),
            makeMalloc(sizeExpression));

    CStatementEdge mallocAndAssignmentEdge =
        new CStatementEdge(
            pointerType + " " + variableName + " = malloc(" + sizeExpression.toASTString() + ")",
            mallocAndAssignmentExpr,
            FileLocation.DUMMY,
            CFANode.newDummyCFANode(),
            CFANode.newDummyCFANode());

    // Update transfer relation state
    transferRelation.setInfo(stateAfterDecl, null, mallocAndAssignmentEdge);

    Collection<SMGState> statesAfterMallocAssign =
        transferRelation.handleStatementEdge(mallocAndAssignmentEdge, mallocAndAssignmentExpr);

    // Per default this returns 2 states, one for successful memory allocation and one with a
    // failure (variable = 0). We ignore the error state if it exists in this!
    assertThat(statesAfterMallocAssign instanceof List<?>).isTrue();
    List<SMGState> statesListAfterMallocAssign = (List<SMGState>) statesAfterMallocAssign;
    // Depending on the options this might have 2 return states, one where malloc succeeds and
    // one where it fails
    SMGState stateAfterMallocAssignSuccess;
    if (size == 0 || !smgOptions.isEnableMallocFailure()) {
      // either its malloc(0) which is always a single null pointer, or its just 1 valid state
      // If malloc(0) happens we still use the state in the transferRelation
      assertThat(statesListAfterMallocAssign).hasSize(1);
      stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(0);

    } else {
      // Malloc can fail in this case, check the additional state
      assertThat(statesListAfterMallocAssign).hasSize(2);
      // (Since we return a list, the order should be success then failure)
      // It might however be that this is the wrong state, we check and flip them if necessary
      SMGState stateAfterMallocAssignFailure = statesListAfterMallocAssign.get(1);
      if (!checkMallocFailure(stateAfterMallocAssignFailure, variableName, pointerType)) {
        stateAfterMallocAssignFailure = statesListAfterMallocAssign.get(0);
        stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(1);
        assertThat(checkMallocFailure(stateAfterMallocAssignFailure, variableName, pointerType))
            .isTrue();
      } else {
        stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(0);
      }
    }
    transferRelation.setInfo(
        stateAfterMallocAssignSuccess,
        null,
        new CDeclarationEdge(
            "", FileLocation.DUMMY, CFANode.newDummyCFANode(), CFANode.newDummyCFANode(), null));

    // Assign values
    BigInteger index = BigInteger.ZERO;
    SMGState lastState = stateAfterDecl;
    for (BigInteger value : values) {
      CExpression valueExpr;
      if (type.equals(CHAR_TYPE)) {
        valueExpr =
            new CCharLiteralExpression(FileLocation.DUMMY, type, (char) value.intValueExact());
      } else if (type instanceof CSimpleType) {
        // all other currently used test types are in this category
        valueExpr = new CIntegerLiteralExpression(FileLocation.DUMMY, type, value);
      } else {
        // TODO: add arrays/structs
        throw new RuntimeException(
            "Unsupported type in struct test type. Add the handling to where the exception is"
                + " thrown.");
      }

      CExpressionAssignmentStatement assignmentExpr =
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY,
              new CArraySubscriptExpression(
                  FileLocation.DUMMY,
                  type,
                  new CIdExpression(
                      FileLocation.DUMMY,
                      new CPointerType(false, false, type),
                      variableName,
                      new CVariableDeclaration(
                          FileLocation.DUMMY,
                          false,
                          CStorageClass.AUTO,
                          new CPointerType(false, false, type),
                          variableName,
                          variableName,
                          variableName,
                          null)),
                  new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, index)),
              valueExpr);
      index = index.add(BigInteger.ONE);

      Collection<SMGState> statesAfterAssign =
          transferRelation.handleStatementEdge(null, assignmentExpr);
      // Since we assign variables we know there will be only 1 state afterwards
      assertThat(statesAfterAssign).hasSize(1);
      SMGState newState = lastState;
      for (SMGState stateAS : statesAfterAssign) {
        newState = stateAS;
      }
      // Technically the Edge is always wrong. This influences only error detection, however.
      transferRelation.setInfo(
          newState,
          null,
          new CDeclarationEdge(
              "", FileLocation.DUMMY, CFANode.newDummyCFANode(), CFANode.newDummyCFANode(), null));
      lastState = newState;
    }
    return lastState;
  }

  /*
   * Declare a struct with the types entered, the values entered in the order entered and the
   * variableName entered on the stack. Returns the last state acquired, but also sets the
   * transfer relation with this state, so it can be ignored. types should be the CTypes used in the struct type in the order they appear in the struct.
   *
   * This is tested in a dedicated test and therefore save to use in other tests!
   */
  private SMGState declareStructVariableWithSimpleTypeWithValuesOnTheStack(
      BigInteger[] values, String variableName, CType structType, List<CType> types)
      throws CPATransferException {
    // Build assignments starting from numeric value 1 and increment after each assignment. Chars
    // are simply the int cast to a char.
    ImmutableList.Builder<CInitializer> listOfInitsBuilder = ImmutableList.builder();
    for (int i = 0; i < types.size(); i++) {
      CType currType = types.get(i);
      BigInteger value = values[i];
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
    // Make a non-global and not external variable with the current type
    List<SMGState> statesAfterDecl =
        transferRelation.handleDeclarationEdge(
            null, declareVariableWithInitializer(variableName, structType, false, false, initList));
    // Since we declare variables we know there will be only 1 state afterwards
    assertThat(statesAfterDecl).hasSize(1);
    // This state must have a local variable the size of the type used (on the current stack frame)
    // The state should not have any errors
    SMGState state = statesAfterDecl.get(0);
    // Technically the Edge is always wrong. This influences only error detection, however.
    transferRelation.setInfo(
        state,
        null,
        new CDeclarationEdge(
            "", FileLocation.DUMMY, CFANode.newDummyCFANode(), CFANode.newDummyCFANode(), null));
    return state;
  }

  /*
   * Declare a struct with the types entered, the values entered in the order entered on the heap.
   * Then a variable with the address of this array is made with the variableName. This uses the ->
   * operator. But the SMG2 CPA translates this always to (struct*).field so its the same.
   * Returns the last state acquired, but also sets the transfer relation with this state, so it can be ignored.
   *
   * This is tested in a dedicated test and therefore save to use in other tests!
   */
  private SMGState declareStructVariableWithSimpleTypeWithValuesOnTheHeap(
      BigInteger[] values, String variableName, CType type) throws CPATransferException {
    CType pointerType = new CPointerType(false, false, type);

    CExpression sizeExpression =
        new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, MACHINE_MODEL.getSizeof(type));

    // Make a non-global and not external variable with the current type
    List<SMGState> statesAfterDecl =
        transferRelation.handleDeclarationEdge(
            null, declareVariableWithoutInitializer(variableName, pointerType, false, false));

    // Since we declare variables we know there will be only 1 state afterwards
    assertThat(statesAfterDecl).hasSize(1);
    // We check the variable later
    SMGState stateAfterDecl = statesAfterDecl.get(0);

    CFunctionCallAssignmentStatement mallocAndAssignmentExpr =
        new CFunctionCallAssignmentStatement(
            FileLocation.DUMMY,
            new CIdExpression(
                FileLocation.DUMMY,
                new CPointerType(false, false, pointerType),
                variableName,
                declareVariableWithoutInitializer(variableName, pointerType, false, false)),
            makeMalloc(sizeExpression));

    CStatementEdge mallocAndAssignmentEdge =
        new CStatementEdge(
            pointerType + " " + variableName + " = malloc(" + sizeExpression.toASTString() + ")",
            mallocAndAssignmentExpr,
            FileLocation.DUMMY,
            CFANode.newDummyCFANode(),
            CFANode.newDummyCFANode());

    // Update transfer relation state
    transferRelation.setInfo(stateAfterDecl, null, mallocAndAssignmentEdge);

    Collection<SMGState> statesAfterMallocAssign =
        transferRelation.handleStatementEdge(mallocAndAssignmentEdge, mallocAndAssignmentExpr);

    // Per default this returns 2 states, one for successful memory allocation and one with a
    // failure (variable = 0). We ignore the error state if it exists in this!
    assertThat(statesAfterMallocAssign instanceof List<?>).isTrue();
    List<SMGState> statesListAfterMallocAssign = (List<SMGState>) statesAfterMallocAssign;
    // Depending on the options this might have 2 return states, one where malloc succeeds and
    // one where it fails
    // TODO:
    SMGState stateAfterMallocAssignSuccess;
    if (MACHINE_MODEL.getSizeof(type).intValue() == 0 || !smgOptions.isEnableMallocFailure()) {
      // either its malloc(0) which is always a single null pointer, or its just 1 valid state
      // If malloc(0) happens we still use the state in the transferRelation
      assertThat(statesListAfterMallocAssign).hasSize(1);
      stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(0);

    } else {
      // Malloc can fail in this case, check the additional state
      assertThat(statesListAfterMallocAssign).hasSize(2);
      // (Since we return a list, the order should be success then failure)
      // It might however be that this is the wrong state, we check and flip them if necessary
      SMGState stateAfterMallocAssignFailure = statesListAfterMallocAssign.get(1);
      if (!checkMallocFailure(stateAfterMallocAssignFailure, variableName, pointerType)) {
        stateAfterMallocAssignFailure = statesListAfterMallocAssign.get(0);
        stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(1);
        assertThat(checkMallocFailure(stateAfterMallocAssignFailure, variableName, pointerType))
            .isTrue();
      } else {
        stateAfterMallocAssignSuccess = statesListAfterMallocAssign.get(0);
      }
    }
    transferRelation.setInfo(stateAfterMallocAssignSuccess, null, mallocAndAssignmentEdge);

    assertThat(type instanceof CElaboratedType).isTrue();
    CElaboratedType ceType = (CElaboratedType) type;
    assertThat(ceType.getRealType() instanceof CCompositeType).isTrue();
    List<CCompositeTypeMemberDeclaration> members =
        ((CCompositeType) ceType.getRealType()).getMembers();
    assertThat(members).hasSize(values.length);
    SMGState lastState = stateAfterMallocAssignSuccess;
    for (int i = 0; i < members.size(); i++) {
      CCompositeTypeMemberDeclaration member = members.get(i);
      BigInteger value = values[i];
      CType memberType = member.getType();
      String memberName = member.getName();
      CExpression valueExpr;
      if (memberType.equals(CHAR_TYPE)) {
        valueExpr =
            new CCharLiteralExpression(
                FileLocation.DUMMY, memberType, (char) value.intValueExact());
      } else if (memberType instanceof CSimpleType) {
        // all other currently used test types are in this category
        valueExpr = new CIntegerLiteralExpression(FileLocation.DUMMY, memberType, value);
      } else {
        // TODO: add arrays/structs
        throw new RuntimeException(
            "Unsupported type in struct test type. Add the handling to where the exception is"
                + " thrown.");
      }
      CExpressionAssignmentStatement assignExpr =
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY,
              new CFieldReference(
                  FileLocation.DUMMY,
                  memberType,
                  memberName,
                  new CIdExpression(
                      FileLocation.DUMMY,
                      pointerType,
                      variableName,
                      new CVariableDeclaration(
                          FileLocation.DUMMY,
                          false,
                          CStorageClass.AUTO,
                          pointerType,
                          variableName,
                          variableName,
                          variableName,
                          null)),
                  true),
              valueExpr);

      Collection<SMGState> statesAfterAssign =
          transferRelation.handleStatementEdge(null, assignExpr);

      // Since we assign variables we know there will be only 1 state afterwards
      assertThat(statesAfterAssign).hasSize(1);
      SMGState stateAfterAssign = stateAfterDecl;
      for (SMGState stateAS : statesAfterAssign) {
        stateAfterAssign = stateAS;
      }
      // Update transfer relation state. The edge will be wrong, which is fine as it only effects
      // error checks
      transferRelation.setInfo(stateAfterAssign, null, mallocAndAssignmentEdge);
      lastState = stateAfterAssign;
    }
    return lastState;
  }

  private CFunctionCallExpression makeMalloc(CExpression sizeExpr) {
    CFunctionType mallocType =
        new CFunctionType(
            CPointerType.POINTER_TO_VOID, Collections.singletonList(CNumericTypes.INT), false);
    CFunctionDeclaration mallocFunctionDeclaration =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            mallocType,
            "malloc",
            ImmutableList.of(
                new CParameterDeclaration(
                    FileLocation.DUMMY, CPointerType.POINTER_TO_VOID, "size")),
            ImmutableSet.of());
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        CPointerType.POINTER_TO_VOID,
        new CIdExpression(FileLocation.DUMMY, mallocFunctionDeclaration),
        Collections.singletonList(sizeExpr),
        mallocFunctionDeclaration);
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

  private CType makeArrayTypeFor(CType elementType, CExpression length) {
    return new CArrayType(false, false, elementType, length);
  }

  private CType makeArrayTypeFor(CType elementType, BigInteger length) {
    return makeArrayTypeFor(
        elementType, new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, length));
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
