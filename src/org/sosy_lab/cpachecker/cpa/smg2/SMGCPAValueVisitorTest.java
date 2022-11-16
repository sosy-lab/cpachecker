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
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

// TODO: run with more machine models
/* Test all SMGCPAValueVisitor visits. Some will be tested indirectly, for example value creation. */
public class SMGCPAValueVisitorTest {
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

  // Struct/Union names/declaration names

  private static final String COMPOSITE_VARIABLE_NAME = "compositeVariableName";
  private static final String COMPOSITE_DECLARATION_NAME = "compositeDeclaration";

  private static final CType[] BIT_FIELD_TYPES =
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

  // Note: padding is on per default, meaning that the types get padding to allign to their
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

  // Reduce if the tests take to long; should be at the very least 4 however!
  private static final int TEST_ARRAY_LENGTH = 40;

  private LogManagerWithoutDuplicates logger;
  private SMGCPAExpressionEvaluator evaluator;
  private SMGState currentState;

  // The visitor should always use the currentState!
  private SMGCPAValueVisitor visitor;

  @Before
  public void init() throws InvalidConfigurationException {
    logger = new LogManagerWithoutDuplicates(LogManager.createTestLogManager());

    // null, null is fine as long as builtin functions are not used!
    evaluator = new SMGCPAExpressionEvaluator(MACHINE_MODEL, logger, null, null);

    currentState =
        SMGState.of(MACHINE_MODEL, logger, new SMGOptions(Configuration.defaultConfiguration()));

    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
  }

  // Resets state and visitor to an empty state
  public void resetSMGStateAndVisitor() throws InvalidConfigurationException {
    currentState =
        SMGState.of(MACHINE_MODEL, logger, new SMGOptions(Configuration.defaultConfiguration()));

    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
  }

  /*
   * This tests the struct field read struct->field with padding. Does not test Strings! Writes only 1 value into the struct, reads and resets.
   */
  @Test
  public void readFieldDerefTest() throws InvalidConfigurationException, CPATransferException {
    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      CFieldReference fieldRef =
          createFieldRefForStackVar(
              COMPOSITE_DECLARATION_NAME,
              COMPOSITE_VARIABLE_NAME,
              i,
              STRUCT_UNION_TEST_TYPES,
              true,
              ComplexTypeKind.STRUCT);

      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);
      Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);

      addHeapVariableToMemoryModel(
          0, getSizeInBitsForListOfCTypeWithPadding(STRUCT_UNION_TEST_TYPES), addressValue);
      addStackVariableToMemoryModel(COMPOSITE_VARIABLE_NAME, POINTER_SIZE_IN_BITS);
      writeToStackVariableInMemoryModel(
          COMPOSITE_VARIABLE_NAME, 0, POINTER_SIZE_IN_BITS, addressValue);

      writeToHeapObjectByAddress(
          addressValue,
          getOffsetInBitsWithPadding(STRUCT_UNION_TEST_TYPES, i),
          MACHINE_MODEL.getSizeof(STRUCT_UNION_TEST_TYPES.get(i)).intValue() * 8,
          intValue);

      List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      // The returned Value should be the value we entered into the field,
      // which is a NumericValue = testNumericValue
      assertThat(resultValue).isInstanceOf(NumericValue.class);
      assertThat(resultValue.asNumericValue().bigInteger()).isEqualTo(BigInteger.valueOf(i));

      resetSMGStateAndVisitor();
    }
  }

  /*
   * This tests the struct field read (*struct).field with pointers as values.
   * We test pointers of all types. We also fill the struct first completely and don't reset.
   * The result should always be a AddressExpression with the correct address value and no offset.
   */
  @Test
  public void readStructDerefWithPointerValuesTest()
      throws InvalidConfigurationException, CPATransferException {
    List<Value> addresses = new ArrayList<>();
    Value addressOfStructValue = SymbolicValueFactory.getInstance().newIdentifier(null);

    addHeapVariableToMemoryModel(
        0, STRUCT_UNION_TEST_TYPES.size() * POINTER_SIZE_IN_BITS, addressOfStructValue);
    addStackVariableToMemoryModel(COMPOSITE_VARIABLE_NAME, POINTER_SIZE_IN_BITS);
    writeToStackVariableInMemoryModel(
        COMPOSITE_VARIABLE_NAME, 0, POINTER_SIZE_IN_BITS, addressOfStructValue);

    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      // Create a Value as address
      Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);

      writeToHeapObjectByAddress(
          addressOfStructValue, i * POINTER_SIZE_IN_BITS, POINTER_SIZE_IN_BITS, addressValue);

      // remember the address for the index
      addresses.add(addressValue);
    }

    // Read twice to check that there are no side effects when reading
    for (int repeatRead = 0; repeatRead < 2; repeatRead++) {
      for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
        CFieldReference fieldRef =
            createStructFieldRefWithPointerNoDeref(
                COMPOSITE_DECLARATION_NAME,
                COMPOSITE_VARIABLE_NAME,
                i,
                transformedImmutableListCopy(
                    STRUCT_UNION_TEST_TYPES, n -> new CPointerType(false, false, n)));

        List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();

        // The return should always be a AddressExpression with the address as memory address
        assertThat(resultValue).isInstanceOf(AddressExpression.class);
        assertThat(((AddressExpression) resultValue).getMemoryAddress())
            .isEqualTo(addresses.get(i));
        // Offset is always 0 as there is no binary expr around them
        assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
        assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().bigInteger())
            .isEqualTo(BigInteger.ZERO);
      }
    }
  }

  /*
   * This tests the struct field read struct->field with pointers as values.
   * Struct is a pointer variable, pointing to the struct on the heap.
   * We test pointers of all types. We also fill the struct first completely and don't reset.
   * The result should always be a AddressExpression with the correct address value and no offset.
   */
  @Test
  public void readFieldDerefWithPointerValuesTest()
      throws InvalidConfigurationException, CPATransferException {
    List<Value> addresses = new ArrayList<>();
    Value addressOfStructValue = SymbolicValueFactory.getInstance().newIdentifier(null);

    addHeapVariableToMemoryModel(
        0, STRUCT_UNION_TEST_TYPES.size() * POINTER_SIZE_IN_BITS, addressOfStructValue);
    addStackVariableToMemoryModel(COMPOSITE_VARIABLE_NAME, POINTER_SIZE_IN_BITS);
    writeToStackVariableInMemoryModel(
        COMPOSITE_VARIABLE_NAME, 0, POINTER_SIZE_IN_BITS, addressOfStructValue);

    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      // Create a Value as address
      Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);

      writeToHeapObjectByAddress(
          addressOfStructValue, i * POINTER_SIZE_IN_BITS, POINTER_SIZE_IN_BITS, addressValue);

      // remember the address for the index
      addresses.add(addressValue);
    }

    // Read twice to check that there are no side effects when reading
    for (int repeatRead = 0; repeatRead < 2; repeatRead++) {
      for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForStackVar(
                COMPOSITE_DECLARATION_NAME,
                COMPOSITE_VARIABLE_NAME,
                i,
                transformedImmutableListCopy(
                    STRUCT_UNION_TEST_TYPES, n -> new CPointerType(false, false, n)),
                true,
                ComplexTypeKind.STRUCT);

        List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();

        // The return should always be a AddressExpression with the address as memory address
        assertThat(resultValue).isInstanceOf(AddressExpression.class);
        assertThat(((AddressExpression) resultValue).getMemoryAddress())
            .isEqualTo(addresses.get(i));
        // Offset is always 0 as there is no binary expr around them
        assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
        assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().bigInteger())
            .isEqualTo(BigInteger.ZERO);
      }
    }
  }

  /*
   * This tests the struct field read struct.field with pointers as values.
   * The struct is on the stack. We test pointers of all types. We also fill the struct first completely and don't reset.
   * The result should always be a AddressExpression with the correct address value and no offset.
   */
  @Test
  public void readFieldFromStackWithPointerValuesTest() throws CPATransferException {
    List<Value> addresses = new ArrayList<>();

    addStackVariableToMemoryModel(
        COMPOSITE_VARIABLE_NAME, STRUCT_UNION_TEST_TYPES.size() * POINTER_SIZE_IN_BITS);

    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      // Create a Value as address
      Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);

      writeToStackVariableInMemoryModel(
          COMPOSITE_VARIABLE_NAME, i * POINTER_SIZE_IN_BITS, POINTER_SIZE_IN_BITS, addressValue);

      // remember the address for the index
      addresses.add(addressValue);
    }

    // Read twice to check that there are no side effects when reading
    for (int repeatRead = 0; repeatRead < 2; repeatRead++) {
      for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForStackVar(
                COMPOSITE_DECLARATION_NAME,
                COMPOSITE_VARIABLE_NAME,
                i,
                transformedImmutableListCopy(
                    STRUCT_UNION_TEST_TYPES, n -> new CPointerType(false, false, n)),
                false,
                ComplexTypeKind.STRUCT);

        List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();

        // The return should always be a AddressExpression with the address as memory address
        assertThat(resultValue).isInstanceOf(AddressExpression.class);
        assertThat(((AddressExpression) resultValue).getMemoryAddress())
            .isEqualTo(addresses.get(i));
        // Offset is always 0 as there is no binary expr around them
        assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
        assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().bigInteger())
            .isEqualTo(BigInteger.ZERO);
      }
    }
  }

  /*
   * This tests the struct field read struct->field with padding.
   * Does not test Strings! Writes ALL values into the struct, then reads repeatedly.
   * Not resets on the memory.
   */
  @Test
  public void readFieldDerefRepeatedTest()
      throws InvalidConfigurationException, CPATransferException {
    setupHeapStructAndFill(COMPOSITE_VARIABLE_NAME, STRUCT_UNION_TEST_TYPES);

    // We read the struct completely more than once, the values may never change!
    for (int j = 0; j < 4; j++) {
      // Read all struct fields once
      for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForStackVar(
                COMPOSITE_DECLARATION_NAME,
                COMPOSITE_VARIABLE_NAME,
                i,
                STRUCT_UNION_TEST_TYPES,
                true,
                ComplexTypeKind.STRUCT);

        List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        // The returned Value should be the value we entered into the field,
        // which is a NumericValue = testNumericValue
        assertThat(resultValue).isInstanceOf(NumericValue.class);
        checkValue(STRUCT_UNION_TEST_TYPES.get(i), i, resultValue);
      }
    }
  }

  /** This tests the struct field read: (*structP).field with padding. Does not test Strings! */
  @Test
  public void readFieldDerefPointerExplicitlyTest()
      throws InvalidConfigurationException, CPATransferException {
    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);
      Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);

      addHeapVariableToMemoryModel(
          0, getSizeInBitsForListOfCTypeWithPadding(STRUCT_UNION_TEST_TYPES), addressValue);
      addStackVariableToMemoryModel(COMPOSITE_VARIABLE_NAME, POINTER_SIZE_IN_BITS);
      writeToStackVariableInMemoryModel(
          COMPOSITE_VARIABLE_NAME, 0, POINTER_SIZE_IN_BITS, addressValue);

      writeToHeapObjectByAddress(
          addressValue,
          getOffsetInBitsWithPadding(STRUCT_UNION_TEST_TYPES, i),
          MACHINE_MODEL.getSizeof(STRUCT_UNION_TEST_TYPES.get(i)).intValue() * 8,
          intValue);

      CFieldReference fieldRef =
          createStructFieldRefWithPointerNoDeref(
              COMPOSITE_DECLARATION_NAME, COMPOSITE_VARIABLE_NAME, i, STRUCT_UNION_TEST_TYPES);

      List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      // The returned Value should be the value we entered into the field,
      // which is a NumericValue = testNumericValue
      assertThat(resultValue).isInstanceOf(NumericValue.class);
      assertThat(resultValue.asNumericValue().bigInteger()).isEqualTo(BigInteger.valueOf(i));

      resetSMGStateAndVisitor();
    }
  }

  /*
   * This tests the struct field read: (*structP).field with padding repeatedly no resets.
   * Does not test Strings! Values should not change in between reads.
   */
  @Test
  public void readFieldDerefPointerExplicitlyRepeatedlyTest()
      throws InvalidConfigurationException, CPATransferException {
    Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);

    addHeapVariableToMemoryModel(
        0, getSizeInBitsForListOfCTypeWithPadding(STRUCT_UNION_TEST_TYPES), addressValue);
    addStackVariableToMemoryModel(COMPOSITE_VARIABLE_NAME, POINTER_SIZE_IN_BITS);
    writeToStackVariableInMemoryModel(
        COMPOSITE_VARIABLE_NAME, 0, POINTER_SIZE_IN_BITS, addressValue);

    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);

      writeToHeapObjectByAddress(
          addressValue,
          getOffsetInBitsWithPadding(STRUCT_UNION_TEST_TYPES, i),
          MACHINE_MODEL.getSizeof(STRUCT_UNION_TEST_TYPES.get(i)).intValue() * 8,
          intValue);
    }

    for (int j = 0; j < 4; j++) {
      for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
        CFieldReference fieldRef =
            createStructFieldRefWithPointerNoDeref(
                COMPOSITE_DECLARATION_NAME, COMPOSITE_VARIABLE_NAME, i, STRUCT_UNION_TEST_TYPES);

        List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        // The returned Value should be the value we entered into the field,
        // which is a NumericValue = testNumericValue
        assertThat(resultValue).isInstanceOf(NumericValue.class);
        assertThat(resultValue.asNumericValue().bigInteger()).isEqualTo(BigInteger.valueOf(i));
      }
    }
  }

  /**
   * This tests struct.field read with struct as a stack variable with no pointer deref and padding.
   * Does not test Strings.
   */
  @Test
  public void readFieldWithStructOnStackTest()
      throws CPATransferException, InvalidConfigurationException {
    for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
      CFieldReference fieldRef =
          createFieldRefForStackVar(
              COMPOSITE_DECLARATION_NAME,
              COMPOSITE_VARIABLE_NAME,
              i,
              STRUCT_UNION_TEST_TYPES,
              false,
              ComplexTypeKind.STRUCT);

      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);

      // Now create the SMGState, SPC and SMG with the struct already present and values written to
      // it
      addStackVariableToMemoryModel(
          COMPOSITE_VARIABLE_NAME, getSizeInBitsForListOfCTypeWithPadding(STRUCT_UNION_TEST_TYPES));
      // Write to the stack var
      writeToStackVariableInMemoryModel(
          COMPOSITE_VARIABLE_NAME,
          getOffsetInBitsWithPadding(STRUCT_UNION_TEST_TYPES, i),
          MACHINE_MODEL.getSizeof(STRUCT_UNION_TEST_TYPES.get(i)).intValue() * 8,
          intValue);

      List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      // The returned Value should be the value we entered into the field,
      // which is a NumericValue = testNumericValue
      assertThat(resultValue).isInstanceOf(NumericValue.class);
      assertThat(resultValue.asNumericValue().bigInteger()).isEqualTo(BigInteger.valueOf(i));

      resetSMGStateAndVisitor();
    }
  }

  /**
   * This tests struct.field read with struct as a stack variable with no pointer deref and padding
   * repeatedly with no resets. Does not test Strings.
   */
  @Test
  public void readFieldWithStructOnStackRepeatedTest() throws CPATransferException {
    // Now create the SMGState, SPC and SMG with the struct already present and values written to
    // it
    createStackVarOnStackAndFill(COMPOSITE_VARIABLE_NAME, STRUCT_UNION_TEST_TYPES);

    for (int j = 0; j < 4; j++) {
      for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForStackVar(
                COMPOSITE_DECLARATION_NAME,
                COMPOSITE_VARIABLE_NAME,
                i,
                STRUCT_UNION_TEST_TYPES,
                false,
                ComplexTypeKind.STRUCT);

        List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        // The returned Value should be the value we entered into the field,
        // which is a NumericValue = testNumericValue
        assertThat(resultValue).isInstanceOf(NumericValue.class);
        assertThat(resultValue.asNumericValue().bigInteger()).isEqualTo(BigInteger.valueOf(i));
      }
    }
  }

  /*
   * This tests union.field read with union as a stack variable with no pointer deref and padding
   * repeatedly with no resets. Does not test Strings. Unions memory is differently allocated!
   * From C99:
   * The size of a union is sufficient to contain the largest of its members. The value of at
   * most one of the members can be stored in a union object at any time. A pointer to a union
   * object, suitably converted, points to each of its members (or if a member is a bit-field, then
   * to the unit in which it resides), and vice versa.
   * The returned Value depends on the type in unions! Example: write short 1
   * 0000 0001
   * Now read int, we read:
   * 0000 0001 0000 0000
   * which is 256 as an int
   * Currently we read by type exactly and this means that we can only read types
   * with the exact size of the value last written. 0 always works!
   */
  @Test
  public void readFieldZeroWithUnionOnStackRepeatedTest() throws CPATransferException {
    // Create the union once
    addStackVariableToMemoryModel(
        COMPOSITE_VARIABLE_NAME, getLargestSizeInBitsForListOfCType(STRUCT_UNION_TEST_TYPES));

    // Write to the stack union the value 0 for all bits.
    writeToStackVariableInMemoryModel(
        COMPOSITE_VARIABLE_NAME,
        0,
        getLargestSizeInBitsForListOfCType(STRUCT_UNION_TEST_TYPES),
        new NumericValue(0));

    for (int j = 0; j < 2; j++) {
      // Repeat to check that no value changed!
      for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForStackVar(
                COMPOSITE_DECLARATION_NAME,
                COMPOSITE_VARIABLE_NAME,
                i,
                STRUCT_UNION_TEST_TYPES,
                false,
                ComplexTypeKind.UNION);

        List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        // Check that 0 always holds
        assertThat(resultValue).isInstanceOf(NumericValue.class);
        assertThat(resultValue.asNumericValue().bigInteger()).isEqualTo(BigInteger.ZERO);
      }
    }
  }

  /*
   * Union on stack, write 1 type, check value for all types with the same size, all else unknown.
   * See readFieldZeroWithUnionOnStackRepeatedTest for general union information!
   * TODO: negative values because of unsigned!
   */
  @Test
  public void readFieldWithUnionOnStackRepeatedTest() throws CPATransferException {
    // Create the union once
    addStackVariableToMemoryModel(
        COMPOSITE_VARIABLE_NAME, getLargestSizeInBitsForListOfCType(STRUCT_UNION_TEST_TYPES));

    for (int k = 0; k < STRUCT_UNION_TEST_TYPES.size(); k++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(k + 1);

      // Write to the stack union; Note: this is always offset 0!
      writeToStackVariableInMemoryModel(
          COMPOSITE_VARIABLE_NAME,
          0,
          MACHINE_MODEL.getSizeof(STRUCT_UNION_TEST_TYPES.get(k)).intValue() * 8,
          intValue);

      // We write 1 type k, read all by i more than once (j iterations) and check that its
      // interpretation specific! Repeat for all other types.
      for (int i = 0; i < STRUCT_UNION_TEST_TYPES.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForStackVar(
                COMPOSITE_DECLARATION_NAME,
                COMPOSITE_VARIABLE_NAME,
                i,
                STRUCT_UNION_TEST_TYPES,
                false,
                ComplexTypeKind.UNION);

        for (int j = 0; j < 4; j++) {
          List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

          // Assert the correct returns
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          // Check that types with the size of the current have the correct value, and all other
          // should be unknown
          if (MACHINE_MODEL
                  .getSizeof(STRUCT_UNION_TEST_TYPES.get(k))
                  .compareTo(MACHINE_MODEL.getSizeof(STRUCT_UNION_TEST_TYPES.get(i)))
              == 0) {
            assertThat(resultValue).isInstanceOf(NumericValue.class);
            assertThat(resultValue.asNumericValue().bigInteger())
                .isEqualTo(BigInteger.valueOf(k + 1));
          } else {
            // Unknowns are tranlated into ConstantSymbolicExpression
            assertThat(resultValue).isInstanceOf(ConstantSymbolicExpression.class);
          }
        }
      }
    }
  }

  /**
   * Read an array on the stack with a constant subscript expression for multiple types and values
   * saved in the array. Example: int * array = array[] {1, 2, 3, ...}; array[0]; ....
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readStackArraySubscriptConstMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      // Create a stack array with size * length bytes and fill it
      setupStackArray(arrayVariableName, currentArrayType);

      // Now we read the entire array twice.(twice because values may change when reading in SMGs,
      // and we don't want that)
      for (int j = 0; j < 2; j++) {
        for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
          CArraySubscriptExpression arraySubscriptExpr =
              arraySubscriptStackAccess(arrayVariableName, currentArrayType, k, TEST_ARRAY_LENGTH);

          List<ValueAndSMGState> resultList = arraySubscriptExpr.accept(visitor);

          // Assert the correct return values depending on type
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          checkValue(currentArrayType, k, resultValue);
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Read an array on the stack with a variable subscript expression for multiple types and values
   * saved in the array. Example: int * array = array[] {1, 2, 3, ...}; array[variable]; ....
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readStackArraySubscriptWithVariableMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";
    String indiceVariableName = "indexVariableName";

    CType indexVarType = INT_TYPE;

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      // Create a stack array with size * length bytes and fill it
      setupStackArray(arrayVariableName, currentArrayType);
      // Now create length stack variables holding the indices to access the array
      setupIndexVariables(indiceVariableName);

      // Now we read the entire array twice.(twice because values may change when reading in SMGs,
      // and we don't want that)
      for (int j = 0; j < 2; j++) {
        for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
          CArraySubscriptExpression arraySubscriptExpr =
              arraySubscriptStackAccessWithVariable(
                  arrayVariableName,
                  indiceVariableName + k,
                  currentArrayType,
                  indexVarType,
                  TEST_ARRAY_LENGTH);

          List<ValueAndSMGState> resultList = arraySubscriptExpr.accept(visitor);

          // Assert the correct return values depending on type
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          checkValue(currentArrayType, k, resultValue);
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Read an array on the heap with a constant subscript expression for multiple types and values
   * saved in the array. Example: int * array = malloc(); fill; ... = array[0]; ....
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readHeapArraySubscriptConstMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      setupHeapArray(arrayVariableName, currentArrayType);

      // Now we read the entire array twice.(twice because values may change when reading in SMGs,
      // and we don't want that)
      for (int j = 0; j < 2; j++) {
        for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
          CArraySubscriptExpression arraySubscriptExpr =
              arraySubscriptHeapAccess(arrayVariableName, currentArrayType, k);

          List<ValueAndSMGState> resultList = arraySubscriptExpr.accept(visitor);

          // Assert the correct return values depending on type
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          checkValue(currentArrayType, k, resultValue);
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Read an array on the heap with a variable subscript expression on the stack for multiple types
   * and values saved in the array.
   *
   * <p>Example: int * array = malloc(); fill; for (var++) {... = array[var]; ....}
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readHeapArraySubscriptWithVariableMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";
    String indexVariableName = "indexVariable";

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      setupHeapArray(arrayVariableName, currentArrayType);
      setupIndexVariables(indexVariableName);

      // Now we read the entire array twice.(twice because values may change when reading in SMGs,
      // and we don't want that)
      for (int j = 0; j < 2; j++) {
        for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
          // Indice is always int type
          CArraySubscriptExpression arraySubscriptExpr =
              arraySubscriptHeapAccessWithVariable(
                  arrayVariableName, indexVariableName + k, INT_TYPE, currentArrayType);

          List<ValueAndSMGState> resultList = arraySubscriptExpr.accept(visitor);

          // Assert the correct return values depending on type
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          checkValue(currentArrayType, k, resultValue);
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Read an array on the heap with a constant expression for multiple types and values saved in the
   * array. Example: int * array = malloc(); fill; ... = *array; ... = *(array + 1);
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readNestedPointerHeapArrayLeadingToConstArrayMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String pointerArrayName = "pointerArrayVariable";

    // We want to test the arrays for all basic types
    for (CType currentValueArrayType : ARRAY_TEST_TYPES) {
      int sizeOfCurrentTypeInBits = MACHINE_MODEL.getSizeof(currentValueArrayType).intValue() * 8;
      // address to the heap where the array starts
      Value addressValueArray = SymbolicValueFactory.getInstance().newIdentifier(null);
      // Create the array on the heap; size is type size in bits * size of array
      addHeapVariableToMemoryModel(
          0, sizeOfCurrentTypeInBits * TEST_ARRAY_LENGTH, addressValueArray);

      // Now write some distinct values into the array, for signed we want to test negatives!
      for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
        // Create a Value that we want to be mapped to a SMGValue to write into the array depending
        // on the type
        Value arrayValue = transformInputIntoValue(currentValueArrayType, k);

        // Write to the heap array
        writeToHeapObjectByAddress(
            addressValueArray, sizeOfCurrentTypeInBits * k, sizeOfCurrentTypeInBits, arrayValue);
      }
      // Now that we have a filled value array, we create another array whose pointer is in a stack
      // variable and that is filled with pointers. Each pointer simply corrosponds to the same
      // position in the value array. so pointerArray[0] -> valueArray[0]
      // Stack variable holding the address (the pointer)
      Value addressPointerArray = SymbolicValueFactory.getInstance().newIdentifier(null);
      addStackVariableToMemoryModel(pointerArrayName, POINTER_SIZE_IN_BITS);
      writeToStackVariableInMemoryModel(
          pointerArrayName, 0, POINTER_SIZE_IN_BITS, addressPointerArray);
      // Create the array on the heap; size is pointer size
      addHeapVariableToMemoryModel(
          0, POINTER_SIZE_IN_BITS * TEST_ARRAY_LENGTH, addressPointerArray);
      // now fill the array with pointers
      // Note: we should reuse the pointer for index 0 from above, but I am lazy and it does not
      // matter in a test
      SMGObject objectForAddressValue =
          currentState.dereferencePointer(addressValueArray).get(0).getSMGObject();
      for (int j = 0; j < TEST_ARRAY_LENGTH; j++) {
        // We need a mapping from each value representing a address to a SMGValue that is mapped to
        // a SMGPointsToEdge (modeling the pointer). We simply use numeric values for this. We
        // remember the pointer values such that the index is the same as the location of the
        // pointer in the array.
        Value address = addPointerToMemory(objectForAddressValue, j * sizeOfCurrentTypeInBits);
        // Write the pointer to the heap array
        writeToHeapObjectByAddress(
            addressPointerArray, POINTER_SIZE_IN_BITS * j, POINTER_SIZE_IN_BITS, address);
      }

      // Now we read the entire pointer array twice and dereference the pointer (+- offset) within
      // to check the value of the value array. (twice because values may change when reading in
      // SMGs, and we don't want that)
      for (int read = 0; read < 2; read++) {
        for (int pointerArrayOffset = 0;
            pointerArrayOffset < TEST_ARRAY_LENGTH;
            pointerArrayOffset++) {
          // the real index will always be pointerArrayOffset + valueArrayOffset and that should
          // start with -1 and end with TEST_ARRAY_LENGTH to test out of bounds
          for (int valueArrayOffset = -pointerArrayOffset - 1;
              valueArrayOffset < TEST_ARRAY_LENGTH;
              valueArrayOffset++) {
            int realIndex = valueArrayOffset + pointerArrayOffset;
            CPointerExpression arrayPointerExpr =
                pointerWithBinaryAccessFromExpression(
                    pointerArrayName, currentValueArrayType, pointerArrayOffset, valueArrayOffset);
            if (realIndex < 0 || realIndex >= TEST_ARRAY_LENGTH) {
              // Out of bounds
              List<ValueAndSMGState> visitedValuesAndStates = arrayPointerExpr.accept(visitor);
              assertThat(visitedValuesAndStates).hasSize(1);
              ValueAndSMGState visitedValueAndState = visitedValuesAndStates.get(0);
              // Error state + unknown value
              assertThat(visitedValueAndState.getValue())
                  .isEqualTo(Value.UnknownValue.getInstance());
              // There are 2 errors. 1 read and 1 write error. The read error is first however and
              // its the one we are interested in.
              assertThat(visitedValueAndState.getState().getErrorInfo()).hasSize(1);
              SMGErrorInfo error = visitedValueAndState.getState().getErrorInfo().get(0);

              assertThat(error.isInvalidRead()).isTrue();
            } else {
              List<ValueAndSMGState> resultList = arrayPointerExpr.accept(visitor);

              // Assert the correct return values depending on type
              assertThat(resultList).hasSize(1);
              Value resultValue = resultList.get(0).getValue();
              checkValue(currentValueArrayType, realIndex, resultValue);
            }
          }
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Read an array on the heap with a constant expression as offset. The read value is a pointer
   * again to an array that is read as well. Example: int * array = malloc(); fill; **array or
   * **(array + 1) or *(*(array + 1) + 1)
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readHeapArrayConstMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {

      setupHeapArray(arrayVariableName, currentArrayType);

      // Now we read the entire array twice.(twice because values may change when reading in SMGs,
      // and we don't want that)
      for (int j = 0; j < 2; j++) {
        for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
          CPointerExpression arrayPointerExpr =
              arrayPointerAccess(arrayVariableName, currentArrayType, k);

          List<ValueAndSMGState> resultList = arrayPointerExpr.accept(visitor);

          // Assert the correct return values depending on type
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          checkValue(currentArrayType, k, resultValue);
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /*
   * Read an array that's behind 2 pointers.
   * Example: **array or *(*array + 1).
   * Creation could be int * arrayP = malloc(); int ** array = &arrayP;
   */
  @Test
  public void readHeapArrayConst2PointersMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";
    String addressToAddressVariableName = "addressToAddressVariableName";

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      int sizeOfCurrentTypeInBits = MACHINE_MODEL.getSizeof(currentArrayType).intValue() * 8;
      // address to the heap where the array starts
      Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
      // Create the array on the heap; size is type size in bits * size of array
      addHeapVariableToMemoryModel(0, sizeOfCurrentTypeInBits * TEST_ARRAY_LENGTH, addressValue);
      // Stack variable holding the address (the pointer) to the array
      addStackVariableToMemoryModel(arrayVariableName, POINTER_SIZE_IN_BITS);
      writeToStackVariableInMemoryModel(arrayVariableName, 0, POINTER_SIZE_IN_BITS, addressValue);
      // Stack variable holding the address (the pointer) to the address of the array
      addStackVariableToMemoryModel(addressToAddressVariableName, POINTER_SIZE_IN_BITS);
      // We need a mapping from addressForAddressValue to a SMGValue that is mapped to a
      // SMGPointsToEdge (modeling the pointer)
      SMGObject objectForAddressValue =
          currentState.getMemoryModel().getStackFrames().peek().getVariable(arrayVariableName);
      Value addressForAddressValue = addPointerToMemory(objectForAddressValue, 0);

      writeToStackVariableInMemoryModel(
          addressToAddressVariableName, 0, POINTER_SIZE_IN_BITS, addressForAddressValue);

      // Now write some distinct values into the array, for signed we want to test negatives!
      for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
        // Create a Value that we want to be mapped to a SMGValue to write into the array depending
        // on the type
        Value arrayValue = transformInputIntoValue(currentArrayType, k);

        // Write to the heap array
        writeToHeapObjectByAddress(
            addressValue, sizeOfCurrentTypeInBits * k, sizeOfCurrentTypeInBits, arrayValue);
      }

      // Now we read the entire array twice.(twice because values may change when reading in SMGs,
      // and we don't want that)
      for (int j = 0; j < 2; j++) {
        for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
          CPointerExpression arrayPointerExpr =
              pointerOfPointerAccess(addressToAddressVariableName, currentArrayType, k);

          List<ValueAndSMGState> resultList = arrayPointerExpr.accept(visitor);

          // Assert the correct return values depending on type
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          checkValue(currentArrayType, k, resultValue);
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Read an array on the heap with a variable expression for multiple types and values saved in the
   * array. Example: int * array = malloc(); fill; ... = *(array + variable);
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readHeapArrayVariableMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";
    String indexVariableName = "indexVariableName";
    CType indexVarType = INT_TYPE;

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      setupHeapArray(arrayVariableName, currentArrayType);
      setupIndexVariables(indexVariableName);

      // Now we read the entire array twice.(twice because values may change when reading in SMGs,
      // and we don't want that)
      for (int j = 0; j < 2; j++) {
        for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
          CPointerExpression arrayPointerExpr =
              arrayPointerAccessPlusVariableIndexOnTheRight(
                  arrayVariableName, indexVariableName + k, indexVarType, currentArrayType);

          List<ValueAndSMGState> resultList = arrayPointerExpr.accept(visitor);

          // Assert the correct return values depending on type
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          checkValue(currentArrayType, k, resultValue);
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Read an array on the heap with a variable expression for multiple types and values saved in the
   * array. Example: ... = *(variable + array); Essentially the same as
   * readHeapArrayVariableMultipleTypesRepeated() but with variable + array.
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readHeapArrayVariableLeftMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";
    String indexVariableName = "indexVariableName";
    CType indexVarType = INT_TYPE;

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      setupHeapArray(arrayVariableName, currentArrayType);
      setupIndexVariables(indexVariableName);

      // Now we read the entire array twice.(twice because values may change when reading in SMGs,
      // and we don't want that)
      for (int j = 0; j < 2; j++) {
        for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
          CPointerExpression arrayPointerExpr =
              arrayPointerAccessPlusVariableIndexOnTheLeft(
                  arrayVariableName, indexVariableName + k, indexVarType, currentArrayType);

          List<ValueAndSMGState> resultList = arrayPointerExpr.accept(visitor);

          // Assert the correct return values depending on type
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          checkValue(currentArrayType, k, resultValue);
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Read an array on the heap with a pointer not pointing to the beginning and a variable
   * expression for multiple types and values saved in the array. Note: we test plus and minus
   * indexes as a pointer into an array minus a number is still valid! Example:
   *
   * <p>int * array = malloc();
   *
   * <p>int * arrayP = array + something;
   *
   * <p>fill array;
   *
   * <p>... = *(arrayP +- variable);
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readHeapArrayMultiplePointerVariableMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";
    String indexVariableName = "indexVariableName";
    CType indexVarType = INT_TYPE;

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      int sizeOfCurrentTypeInBits = MACHINE_MODEL.getSizeof(currentArrayType).intValue() * 8;
      // address to the heap where the array starts
      Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);
      // Create the array on the heap; size is type size in bits * size of array
      addHeapVariableToMemoryModel(0, sizeOfCurrentTypeInBits * TEST_ARRAY_LENGTH, addressValue);
      // Stack variable holding the address (the pointer)
      addStackVariableToMemoryModel(arrayVariableName, POINTER_SIZE_IN_BITS);
      writeToStackVariableInMemoryModel(arrayVariableName, 0, POINTER_SIZE_IN_BITS, addressValue);

      // Now write some distinct values into the array, for signed we want to test negatives!
      // Also create pointers
      for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
        // Create a Value that we want to be mapped to a SMGValue to write into the array depending
        // on the type
        Value arrayValue = transformInputIntoValue(currentArrayType, k);

        // Write to the heap array
        writeToHeapObjectByAddress(
            addressValue, sizeOfCurrentTypeInBits * k, sizeOfCurrentTypeInBits, arrayValue);
      }

      // Now create length stack variables holding the indices to access the array
      for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
        addStackVariableToMemoryModel(
            indexVariableName + k, MACHINE_MODEL.getSizeof(indexVarType).intValue() * 8);
        writeToStackVariableInMemoryModel(
            indexVariableName + k,
            0,
            MACHINE_MODEL.getSizeof(indexVarType).intValue() * 8,
            new NumericValue(k));
      }

      // Pointers
      for (int j = 0; j < TEST_ARRAY_LENGTH; j++) {
        Value newPointer =
            addPointerToExistingHeapObject(j * sizeOfCurrentTypeInBits, addressValue);
        addStackVariableToMemoryModel(arrayVariableName + j, POINTER_SIZE_IN_BITS);
        writeToStackVariableInMemoryModel(
            arrayVariableName + j, 0, POINTER_SIZE_IN_BITS, newPointer);
      }

      // Now we read the entire array twice for every valid combination of pointers and variables.
      // (twice because values may change when reading in SMGs, and we don't want that)
      for (int k = 0; k < 2; k++) {
        for (int pointerNum = 0; pointerNum < TEST_ARRAY_LENGTH; pointerNum++) {
          for (int index = -pointerNum; index < TEST_ARRAY_LENGTH - pointerNum; index++) {
            // We start with negatives because it's easier to make them + again (We need minus 1 and
            // plus 1 etc. in most tests, the binary operation is what makes the minus however, so
            // minus -1 is +1, but this way we can keep track of where we are in relation to the
            // pointer)
            CPointerExpression arrayPointerExpr;
            if (index < 0) {
              arrayPointerExpr =
                  arrayPointerAccessMinusVariableIndexOnTheRight(
                      arrayVariableName + pointerNum,
                      indexVariableName + -index,
                      indexVarType,
                      currentArrayType);
            } else {
              arrayPointerExpr =
                  arrayPointerAccessPlusVariableIndexOnTheRight(
                      arrayVariableName + pointerNum,
                      indexVariableName + index,
                      indexVarType,
                      currentArrayType);
            }

            List<ValueAndSMGState> resultList = arrayPointerExpr.accept(visitor);

            // Assert the correct return values depending on type
            assertThat(resultList).hasSize(1);
            Value resultValue = resultList.get(0).getValue();
            checkValue(currentArrayType, pointerNum + index, resultValue);
          }
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Try read an array on the heap with a pointer not pointing to the beginning and a variable
   * expression for multiple types and values saved in the array. The catch is that we always read
   * before the array beginns or after it ends and test what happens! Example:
   *
   * <p>int * array = malloc(100 * sizeOf....);
   *
   * <p>int * arrayP = array + something;
   *
   * <p>fill array;
   *
   * <p>... = *(arrayP +- variable); with arrayP +- variable < 0 or >= 100
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readHeapArrayMultiplePointerVariableMultipleTypesRepeatedOutOfBoundsRead()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";
    String indexVariableName = "indexVariableName";
    CType indexVarType = INT_TYPE;

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      int sizeOfCurrentTypeInBits = MACHINE_MODEL.getSizeof(currentArrayType).intValue() * 8;
      // address to the heap where the array starts
      Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);
      // Create the array on the heap; size is type size in bits * size of array
      addHeapVariableToMemoryModel(0, sizeOfCurrentTypeInBits * TEST_ARRAY_LENGTH, addressValue);
      // Stack variable holding the address (the pointer)
      addStackVariableToMemoryModel(arrayVariableName, POINTER_SIZE_IN_BITS);
      writeToStackVariableInMemoryModel(arrayVariableName, 0, POINTER_SIZE_IN_BITS, addressValue);

      // We don't need to fill the array for obvious reasons
      // Now create length stack variables holding the indices to access the array
      for (int k = -TEST_ARRAY_LENGTH; k <= TEST_ARRAY_LENGTH; k++) {
        addStackVariableToMemoryModel(
            indexVariableName + k, MACHINE_MODEL.getSizeof(indexVarType).intValue() * 8);
        writeToStackVariableInMemoryModel(
            indexVariableName + k,
            0,
            MACHINE_MODEL.getSizeof(indexVarType).intValue() * 8,
            new NumericValue(k));
      }

      // Pointers to all valid positions but also invalid in -1 and length
      for (int j = -1; j <= TEST_ARRAY_LENGTH; j++) {
        Value newPointer =
            addPointerToExistingHeapObject(j * sizeOfCurrentTypeInBits, addressValue);
        addStackVariableToMemoryModel(arrayVariableName + j, POINTER_SIZE_IN_BITS);
        writeToStackVariableInMemoryModel(
            arrayVariableName + j, 0, POINTER_SIZE_IN_BITS, newPointer);
      }

      // Read only invalid
      for (int k = 0; k < 2; k++) {
        for (int pointerNum = -1; pointerNum <= TEST_ARRAY_LENGTH; pointerNum++) {
          for (int index = -TEST_ARRAY_LENGTH; index <= TEST_ARRAY_LENGTH; index++) {
            // We start with negatives because it's easier to make them + again (We need minus 1 and
            // plus 1 etc. in most tests, the binary operation is what makes the minus however, so
            // minus -1 is +1, but this way we can keep track of where we are in relation to the
            // pointer)
            CPointerExpression arrayPointerExpr;
            BigInteger expectedOffset;
            if (pointerNum + index < 0 || pointerNum + index >= TEST_ARRAY_LENGTH) {
              arrayPointerExpr =
                  arrayPointerAccessPlusVariableIndexOnTheRight(
                      arrayVariableName + pointerNum,
                      indexVariableName + index,
                      indexVarType,
                      currentArrayType);
              expectedOffset =
                  BigInteger.valueOf(pointerNum + index)
                      .multiply(BigInteger.valueOf(sizeOfCurrentTypeInBits));
            } else if (pointerNum - index < 0 || pointerNum - index >= TEST_ARRAY_LENGTH) {
              arrayPointerExpr =
                  arrayPointerAccessMinusVariableIndexOnTheRight(
                      arrayVariableName + pointerNum,
                      indexVariableName + index,
                      indexVarType,
                      currentArrayType);
              expectedOffset =
                  BigInteger.valueOf(pointerNum - index)
                      .multiply(BigInteger.valueOf(sizeOfCurrentTypeInBits));
            } else {
              continue;
            }

            List<ValueAndSMGState> visitedValuesAndStates = arrayPointerExpr.accept(visitor);
            assertThat(visitedValuesAndStates).hasSize(1);
            ValueAndSMGState visitedValueAndState = visitedValuesAndStates.get(0);
            // Error state + unknown value
            assertThat(visitedValueAndState.getValue()).isEqualTo(Value.UnknownValue.getInstance());
            // There are 2 errors. 1 read and 1 write error. The read error is first however and
            // it's the one we are interested in.
            assertThat(visitedValueAndState.getState().getErrorInfo()).hasSize(1);
            SMGErrorInfo error = visitedValueAndState.getState().getErrorInfo().get(0);

            assertThat(error.isInvalidRead()).isTrue();

            BigInteger expectedObjectSize =
                BigInteger.valueOf(sizeOfCurrentTypeInBits)
                    .multiply(BigInteger.valueOf(TEST_ARRAY_LENGTH));

            // TODO: rework once i make more useful error msg
            assertThat(error.getErrorDescription())
                .contains(
                    "with size "
                        + expectedObjectSize
                        + " bits at offset "
                        + expectedOffset
                        + " bit with read type size "
                        + sizeOfCurrentTypeInBits
                        + " bit");
          }
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Read an array on the heap with a pointer not pointing to the beginning and a variable
   * expression for multiple types and values saved in the array. Note: same as
   * readHeapArrayMultiplePointerVariableMultipleTypesRepeated() but we make all plus operations
   * minus and all minus to plus but make the actuall numbers negative. Example:
   *
   * <p>int * array = malloc();
   *
   * <p>int * arrayP = array + something;
   *
   * <p>fill array;
   *
   * <p>... = *(arrayP +- (-variable));
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void readHeapArrayMultiplePointerNegativeVariableMultipleTypesRepeated()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";
    String indexVariableName = "indexVariableName";
    CType indexVarType = INT_TYPE;

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      int sizeOfCurrentTypeInBits = MACHINE_MODEL.getSizeof(currentArrayType).intValue() * 8;
      // address to the heap where the array starts
      Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);
      // Create the array on the heap; size is type size in bits * size of array
      addHeapVariableToMemoryModel(0, sizeOfCurrentTypeInBits * TEST_ARRAY_LENGTH, addressValue);
      // Stack variable holding the address (the pointer)
      addStackVariableToMemoryModel(arrayVariableName, POINTER_SIZE_IN_BITS);
      writeToStackVariableInMemoryModel(arrayVariableName, 0, POINTER_SIZE_IN_BITS, addressValue);

      // Now write some distinct values into the array, for signed we want to test negatives!
      // Also create pointers
      for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
        // Create a Value that we want to be mapped to a SMGValue to write into the array depending
        // on the type
        Value arrayValue = transformInputIntoValue(currentArrayType, k);

        // Write to the heap array
        writeToHeapObjectByAddress(
            addressValue, sizeOfCurrentTypeInBits * k, sizeOfCurrentTypeInBits, arrayValue);
      }

      // Now create length stack variables holding the indices to access the array but negative
      // numbers
      for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
        addStackVariableToMemoryModel(
            indexVariableName + k, MACHINE_MODEL.getSizeof(indexVarType).intValue() * 8);
        writeToStackVariableInMemoryModel(
            indexVariableName + k,
            0,
            MACHINE_MODEL.getSizeof(indexVarType).intValue() * 8,
            new NumericValue(-k));
      }

      // Pointers
      for (int j = 0; j < TEST_ARRAY_LENGTH; j++) {
        Value newPointer =
            addPointerToExistingHeapObject(j * sizeOfCurrentTypeInBits, addressValue);
        addStackVariableToMemoryModel(arrayVariableName + j, POINTER_SIZE_IN_BITS);
        writeToStackVariableInMemoryModel(
            arrayVariableName + j, 0, POINTER_SIZE_IN_BITS, newPointer);
      }

      // Now we read the entire array twice for every valid combination of pointers and variables.
      // (twice because values may change when reading in SMGs, and we don't want that)
      for (int k = 0; k < 2; k++) {
        for (int pointerNum = 0; pointerNum < TEST_ARRAY_LENGTH; pointerNum++) {
          for (int index = -pointerNum; index < TEST_ARRAY_LENGTH - pointerNum; index++) {
            // We start with negatives because it's easier to make them + again (We need minus 1 and
            // plus 1 etc. in most tests, the binary operation is what makes the minus however, so
            // minus -1 is +1, but this way we can keep track of where we are in relation to the
            // pointer)
            CPointerExpression arrayPointerExpr;
            if (index < 0) {
              // *((pointer + 3) + (-3) == *(pointer) or *(pointer + 0)
              arrayPointerExpr =
                  arrayPointerAccessPlusVariableIndexOnTheRight(
                      arrayVariableName + pointerNum,
                      indexVariableName + -index,
                      indexVarType,
                      currentArrayType);
            } else {
              // *((pointer + 3) - (-3) == *(pointer + 6)
              arrayPointerExpr =
                  arrayPointerAccessMinusVariableIndexOnTheRight(
                      arrayVariableName + pointerNum,
                      indexVariableName + index,
                      indexVarType,
                      currentArrayType);
            }

            List<ValueAndSMGState> resultList = arrayPointerExpr.accept(visitor);

            // Assert the correct return values depending on type
            assertThat(resultList).hasSize(1);
            Value resultValue = resultList.get(0).getValue();
            checkValue(currentArrayType, pointerNum + index, resultValue);
          }
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Test distance of 2 pointers from the same array on the stack. Tests 0, positive and negative
   * results.
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void stackArrayPointerDistance()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";
    String indexVariableName = "indexVariableName";
    CType indexVarType = INT_TYPE;

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      int sizeOfCurrentTypeInBits = MACHINE_MODEL.getSizeof(currentArrayType).intValue() * 8;
      // address to the heap where the array starts
      Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
      // Create the array on the heap; size is type size in bits * size of array
      addHeapVariableToMemoryModel(0, sizeOfCurrentTypeInBits * TEST_ARRAY_LENGTH, addressValue);
      // Stack variable holding the address (the pointer)
      addStackVariableToMemoryModel(arrayVariableName, POINTER_SIZE_IN_BITS);
      writeToStackVariableInMemoryModel(arrayVariableName, 0, POINTER_SIZE_IN_BITS, addressValue);

      // Now write some distinct values into the array, for signed we want to test negatives!
      for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
        // Create a Value that we want to be mapped to a SMGValue to write into the array depending
        // on the type
        Value arrayValue = transformInputIntoValue(currentArrayType, k);

        // Write to the heap array
        writeToHeapObjectByAddress(
            addressValue, sizeOfCurrentTypeInBits * k, sizeOfCurrentTypeInBits, arrayValue);

        // Now create length stack variables holding the indices to access the array
        addStackVariableToMemoryModel(
            indexVariableName + k, MACHINE_MODEL.getSizeof(indexVarType).intValue() * 8);
        writeToStackVariableInMemoryModel(
            indexVariableName + k,
            0,
            MACHINE_MODEL.getSizeof(indexVarType).intValue() * 8,
            new NumericValue(k));
      }

      // Now we read the entire array twice.(twice because values may change when reading in SMGs,
      // and we don't want that)
      for (int j = 0; j < 2; j++) {
        for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
          CPointerExpression arrayPointerExpr =
              arrayPointerAccessPlusVariableIndexOnTheRight(
                  arrayVariableName, indexVariableName + k, indexVarType, currentArrayType);

          List<ValueAndSMGState> resultList = arrayPointerExpr.accept(visitor);

          // Assert the correct return values depending on type
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          checkValue(currentArrayType, k, resultValue);
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /**
   * Test distance of 2 pointers from the same array on the heap. Tests 0, positive and negative
   * results.
   *
   * @throws CPATransferException should never be thrown!
   * @throws InvalidConfigurationException should never be thrown!
   */
  @Test
  public void heapArrayPointerDistance()
      throws CPATransferException, InvalidConfigurationException {
    String arrayVariableName = "arrayVariable";

    // We want to test the arrays for all basic types
    for (CType currentArrayType : ARRAY_TEST_TYPES) {
      int sizeOfCurrentTypeInBits = MACHINE_MODEL.getSizeof(currentArrayType).intValue() * 8;
      // address to the heap where the array starts
      Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);
      // Create the array on the heap; size is type size in bits * size of array
      addHeapVariableToMemoryModel(0, sizeOfCurrentTypeInBits * TEST_ARRAY_LENGTH, addressValue);
      // Stack variable holding the address (the pointer)
      addStackVariableToMemoryModel(arrayVariableName, POINTER_SIZE_IN_BITS);
      writeToStackVariableInMemoryModel(arrayVariableName, 0, POINTER_SIZE_IN_BITS, addressValue);

      // Now create a lot of pointers pointing to the same structure but different offsets
      for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
        Value newPointer =
            addPointerToExistingHeapObject(k * sizeOfCurrentTypeInBits, addressValue);
        addStackVariableToMemoryModel(arrayVariableName + k, POINTER_SIZE_IN_BITS);
        writeToStackVariableInMemoryModel(
            arrayVariableName + k, 0, POINTER_SIZE_IN_BITS, newPointer);
      }

      // Now we read the entire array twice.(twice because values may change when reading in SMGs,
      // and we don't want that)
      for (int j = 0; j < TEST_ARRAY_LENGTH; j++) {
        for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
          // Obviously a smaller minus a larger offset makes no sense (if you don't want useless
          // values)
          CBinaryExpression arrayDistanceExpr =
              arrayPointerMinusArrayPointer(
                  arrayVariableName + j, arrayVariableName + k, currentArrayType);

          List<ValueAndSMGState> resultList = arrayDistanceExpr.accept(visitor);

          // Assert the correct return values depending on type
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          assertThat(resultValue.isNumericValue()).isTrue();
          assertThat(resultValue.asNumericValue().longValue()).isEqualTo(j - k);
        }
      }
      // Reset memory model
      resetSMGStateAndVisitor();
    }
  }

  /*
   * Test casting of char concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * char to char (char is 1 byte; signed -128 to 127 unsigned 0 to 255)
   * char to signed short (short is 2 byte; -32,768 to 32,767 or 0 to 65,535)
   * char to unsigned short
   * char to signed int (int is 4 byte; signed -2,147,483,648 to 2,147,483,647)
   * char to unsigned int (unsigned 0 to 4,294,967,295)
   * char to signed long (long is 8 bytes; signed -9223372036854775808 to 9223372036854775807)
   * char to unsigned long (unsigned 0 to 18446744073709551615)
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castCharTest() throws CPATransferException {
    // 255 is max for unsigned. Java chars are unsigned!
    // According to the C 99 standard, char, unsigned char and signed char should behave the same.
    // As we use the numeric values, they essentially do.
    char[] testChars = new char[] {((char) 0), ((char) 1), 'a', 'A', ((char) 127), ((char) 255)};

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (char testChar : testChars) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CCharLiteralExpression(FileLocation.DUMMY, CNumericTypes.CHAR, testChar));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(BigInteger.valueOf(testChar), typeToTest));
      }
    }
  }

  /*
   * Test casting of signed short concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * signed short to char
   * signed short to signed short
   * signed short to unsigned short
   * signed short to signed int
   * signed short to unsigned int
   * signed short to signed long
   * signed short to unsigned long
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castSignedShortTest() throws CPATransferException {
    // Min value, -1, 0, 1, max value
    short[] testValues = new short[] {Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE};

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (short testValue : testValues) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, SHORT_TYPE, BigInteger.valueOf(testValue)));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        // Check the returned value. It should be == for all except char
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(BigInteger.valueOf(testValue), typeToTest));
      }
    }
  }

  /*
   * Test casting of unsigned short concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * unsigned short to char
   * unsigned short to signed short
   * unsigned short to unsigned short
   * unsigned short to signed int
   * unsigned short to unsigned int
   * unsigned short to signed long
   * unsigned short to unsigned long
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castUnsignedShortTest() throws CPATransferException {
    // 0, 1, max value signed short, max value signed short * 2, max value unsigned short
    int[] testValues =
        new int[] {0, 1, Short.MAX_VALUE, Short.MAX_VALUE * 2, Short.MAX_VALUE * 2 + 1};

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (int testValue : testValues) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, UNSIGNED_SHORT_TYPE, BigInteger.valueOf(testValue)));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        // Check the returned value. It should be == for all except char
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(BigInteger.valueOf(testValue), typeToTest));
      }
    }
  }

  /*
   * Test casting of signed int concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * signed int to char
   * signed int to signed short
   * signed int to unsigned short
   * signed int to signed int
   * signed int to unsigned int
   * signed int to signed long
   * signed int to unsigned long
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castSignedIntTest() throws CPATransferException {
    // Min value, -1, 0, 1, max value
    int[] testValues = new int[] {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (int testValue : testValues) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(testValue)));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        // Check the returned value. It should be == for all except char
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(BigInteger.valueOf(testValue), typeToTest));
      }
    }
  }

  /*
   * Test casting of unsigned int concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * unsigned int to char
   * unsigned int to signed short
   * unsigned int to unsigned short
   * unsigned int to signed int
   * unsigned int to unsigned int
   * unsigned int to signed long
   * unsigned int to unsigned long
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castUnsignedIntTest() throws CPATransferException {
    // 0, 1, max value signed int, double max v signed, max unsigned
    BigInteger[] testValues =
        new BigInteger[] {
          BigInteger.valueOf(0),
          BigInteger.valueOf(1),
          BigInteger.valueOf(Integer.MAX_VALUE),
          BigInteger.valueOf(Integer.MAX_VALUE).multiply(BigInteger.TWO),
          BigInteger.valueOf(Integer.MAX_VALUE).multiply(BigInteger.TWO).add(BigInteger.ONE)
        };

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (BigInteger testValue : testValues) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CIntegerLiteralExpression(FileLocation.DUMMY, UNSIGNED_INT_TYPE, testValue));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        // Check the returned value. It should be == for all except char
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(testValue, typeToTest));
      }
    }
  }

  /*
   * Test casting of signed long concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * signed long to char
   * signed long to signed short
   * signed long to unsigned short
   * signed long to signed int
   * signed long to unsigned int
   * signed long to signed long
   * signed long to unsigned long
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castSignedLongTest() throws CPATransferException {
    // min value, -1,  0, 1, max value
    long[] testValues = new long[] {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (long testValue : testValues) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, LONG_TYPE, BigInteger.valueOf(testValue)));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        // Check the returned value. It should be == for all except char
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(BigInteger.valueOf(testValue), typeToTest));
      }
    }
  }

  /*
   * Test casting of unsigned long concrete values.
   * Assuming Linux 64bit. If signed/unsigned is missing signed is assumed.
   * Tests for casting of values:
   * unsigned long to char
   * unsigned long to signed short
   * unsigned long to unsigned short
   * unsigned long to signed int
   * unsigned long to unsigned int
   * unsigned long to signed long
   * unsigned long to unsigned long
   *
   * Some types (long double etc.) are left out but could be added later.
   */
  @Test
  public void castUnsignedLongTest() throws CPATransferException {
    // 0, 1, max value signed long, double that, max unsigned long
    BigInteger[] testValues =
        new BigInteger[] {
          BigInteger.valueOf(0),
          BigInteger.valueOf(1),
          BigInteger.valueOf(Long.MAX_VALUE),
          BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO),
          BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO).add(BigInteger.ONE)
        };

    for (CType typeToTest : BIT_FIELD_TYPES) {
      for (BigInteger testValue : testValues) {
        CCastExpression castExpression =
            new CCastExpression(
                FileLocation.DUMMY,
                typeToTest,
                new CIntegerLiteralExpression(FileLocation.DUMMY, UNSIGNED_LONG_TYPE, testValue));

        List<ValueAndSMGState> result = castExpression.accept(visitor);
        // Chars are translated into their numeric values by the value analysis
        // Also, the numeric value is max 255, therefore every datatype should be able to hold that!
        Value value = result.get(0).getValue();

        assertThat(value).isInstanceOf(NumericValue.class);
        // Check the returned value. It should be == for all except char
        assertThat(value.asNumericValue().bigInteger())
            .isEqualTo(convertToType(testValue, typeToTest));
      }
    }
  }

  /*
   * Test getting the address of a stack variable with a simple type.
   * int bla = 3;
   * int * blaP = &bla;
   */
  @Test
  public void testAddressOperatorOnSimpleStackVariable() throws CPATransferException {
    String variableName = "varName";
    // Just use the simple types from the struct/union tests
    for (CType typeToTest : STRUCT_UNION_TEST_TYPES) {
      // Name the variable uniquely such that all variables can exist concurrently
      addStackVariableToMemoryModel(
          variableName + typeToTest, MACHINE_MODEL.getSizeof(typeToTest).intValue() * 8);

      // Create some Value that does not equal any type size.
      Value intValue = new NumericValue(99);

      // Write to the stack var; Note: this is always offset 0!
      writeToStackVariableInMemoryModel(
          variableName + typeToTest,
          0,
          MACHINE_MODEL.getSizeof(typeToTest).intValue() * 8,
          intValue);

      CVariableDeclaration decl =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              false,
              CStorageClass.AUTO,
              typeToTest,
              variableName + "NotQual",
              variableName + "NotQual",
              variableName + typeToTest,
              CDefaults.forType(typeToTest, FileLocation.DUMMY));

      CUnaryExpression amperVar = wrapInAmper(new CIdExpression(FileLocation.DUMMY, decl));

      List<ValueAndSMGState> resultList = amperVar.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
      assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().longValue() == 0)
          .isTrue();
      resultValue = ((AddressExpression) resultValue).getMemoryAddress();
      // & actually changes the state!
      currentState = resultList.get(0).getState();

      SMGObject expectedTarget =
          currentState
              .getMemoryModel()
              .getObjectForVisibleVariable(variableName + typeToTest)
              .orElseThrow();
      BigInteger expectedOffset = BigInteger.ZERO;
      // The returned Value is an address, theoretically addresses may be any Value type
      assertThat(resultValue).isInstanceOf(Value.class);
      // First check general existance of a points to edge, then its target and offset
      assertThat(currentState.dereferencePointer(resultValue).get(0).hasSMGObjectAndOffset())
          .isTrue();
      assertThat(currentState.dereferencePointer(resultValue).get(0).getSMGObject())
          .isEqualTo(expectedTarget);
      assertThat(currentState.dereferencePointer(resultValue).get(0).getOffsetForObject())
          .isEqualTo(expectedOffset);
      // Check that the other methods return the correct points-to-edge leading to the correct
      // memory location and never to the 0 object
      assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
          .isNotEqualTo(SMGObject.nullInstance());
      assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
          .isEqualTo(expectedTarget);

      // The reverse applies as well
      assertThat(
              currentState
                  .getMemoryModel()
                  .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                  .isPresent())
          .isTrue();

      SMGValue smgValueForPointer =
          currentState
              .getMemoryModel()
              .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
              .orElseThrow();
      Value valueForSMGValue =
          currentState.getMemoryModel().getValueFromSMGValue(smgValueForPointer).orElseThrow();
      assertThat(valueForSMGValue).isEqualTo(resultValue);
    }
  }

  /*
   * Test variable == *&variable for heap variables. In this case an array.
   * Example: int * arrayP = malloc();
   * int ** pointerToArrayP = &arrayP;
   * *pointerToArrayP == arrayP;
   */
  @Test
  public void testAddressOperatorOnPointerToHeapArray()
      throws CPATransferException, InvalidConfigurationException {
    for (CType currentTestType : ARRAY_TEST_TYPES) {
      String arrayVariableName = "arrayName" + currentTestType;
      setupHeapArray(arrayVariableName, currentTestType);

      // &array, but array itself is a pointer, we get the type:  type **
      CUnaryExpression amperOfArrayPointerExpr =
          wrapInAmper(arrayPointerAccess(arrayVariableName, currentTestType, 0).getOperand());

      List<ValueAndSMGState> resultList = amperOfArrayPointerExpr.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
      assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().longValue() == 0)
          .isTrue();
      resultValue = ((AddressExpression) resultValue).getMemoryAddress();
      // & actually changes the state!
      currentState = resultList.get(0).getState();

      SMGObject expectedTarget =
          currentState
              .getMemoryModel()
              .getObjectForVisibleVariable(arrayVariableName)
              .orElseThrow();
      BigInteger expectedOffset = BigInteger.ZERO;
      // The returned Value is an address, theoretically addresses may be any Value type
      assertThat(resultValue).isInstanceOf(Value.class);
      // First check general existance of a points to edge, then its target and offset
      assertThat(currentState.dereferencePointer(resultValue).get(0).hasSMGObjectAndOffset())
          .isTrue();
      assertThat(currentState.dereferencePointer(resultValue).get(0).getSMGObject())
          .isEqualTo(expectedTarget);
      assertThat(currentState.dereferencePointer(resultValue).get(0).getOffsetForObject())
          .isEqualTo(expectedOffset);
      // Check that the other methods return the correct points-to-edge leading to the correct
      // memory location and never to the 0 object
      assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
          .isNotEqualTo(SMGObject.nullInstance());
      assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
          .isEqualTo(expectedTarget);

      // The reverse applies as well
      assertThat(
              currentState
                  .getMemoryModel()
                  .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                  .isPresent())
          .isTrue();

      SMGValue smgValueForPointer =
          currentState
              .getMemoryModel()
              .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
              .orElseThrow();
      Value valueForSMGValue =
          currentState.getMemoryModel().getValueFromSMGValue(smgValueForPointer).orElseThrow();
      assertThat(valueForSMGValue).isEqualTo(resultValue);
    }
  }

  /*
   * Test *variable == &*variable on the heap. In this case an array.
   * Example: int * arrayP = malloc();
   * int * ArrayPCopy = &*arrayP;
   * *pointerToArrayP == *arrayP;
   * and
   * int * ArrayPCopy = &*(arrayP + 1);
   * *pointerToArrayP == *(arrayP + 1);
   */
  @Test
  public void testAddressOperatorOnHeapArray()
      throws CPATransferException, InvalidConfigurationException {

    String indiceVarName = "indice";
    setupIndexVariables(indiceVarName);
    for (CType currentTestType : ARRAY_TEST_TYPES) {
      BigInteger sizeOfCurrentTypeInBits =
          MACHINE_MODEL.getSizeof(currentTestType).multiply(BigInteger.valueOf(8));
      String arrayVariableName = "arrayName" + currentTestType;
      Value heapAddress = setupHeapArray(arrayVariableName, currentTestType);

      // Test & on every array entry (create every possible valid pointer to it)
      for (int currentIndice = 0; currentIndice < TEST_ARRAY_LENGTH; currentIndice++) {
        // &*(array + currentIndice)
        CUnaryExpression arrayAmperOfPointerExpr =
            wrapInAmper(arrayPointerAccess(arrayVariableName, currentTestType, currentIndice));

        List<ValueAndSMGState> resultList = arrayAmperOfPointerExpr.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
        assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().longValue() == 0)
            .isTrue();
        resultValue = ((AddressExpression) resultValue).getMemoryAddress();
        // & actually changes the state!
        currentState = resultList.get(0).getState();

        SMGObject expectedTarget =
            currentState.dereferencePointer(heapAddress).get(0).getSMGObject();
        BigInteger expectedOffset =
            BigInteger.valueOf(currentIndice).multiply(sizeOfCurrentTypeInBits);
        // The returned Value is an address, theoretically addresses may be any Value type
        assertThat(resultValue).isInstanceOf(Value.class);
        // First check general existance of a points to edge, then its target and offset
        assertThat(currentState.dereferencePointer(resultValue).get(0).hasSMGObjectAndOffset())
            .isTrue();
        assertThat(currentState.dereferencePointer(resultValue).get(0).getSMGObject())
            .isEqualTo(expectedTarget);
        assertThat(currentState.dereferencePointer(resultValue).get(0).getOffsetForObject())
            .isEqualTo(expectedOffset);
        // Check that the other methods return the correct points-to-edge leading to the correct
        // memory location and never to the 0 object
        assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
            .isNotEqualTo(SMGObject.nullInstance());
        assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
            .isEqualTo(expectedTarget);

        // The reverse applies as well
        assertThat(
                currentState
                    .getMemoryModel()
                    .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                    .isPresent())
            .isTrue();

        SMGValue smgValueForPointer =
            currentState
                .getMemoryModel()
                .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                .orElseThrow();
        Value valueForSMGValue =
            currentState.getMemoryModel().getValueFromSMGValue(smgValueForPointer).orElseThrow();
        assertThat(valueForSMGValue).isEqualTo(resultValue);
      }
    }
  }

  /*
   * Test array[x] == *&array[x] for stack variables.
   * Example: int array[x];
   * ...
   * int * pointerToArray = &array[y];
   * *pointerToArray == array[y];
   */
  @Test
  public void testAddressOperatorOnPointerToStackArray()
      throws CPATransferException, InvalidConfigurationException {
    for (CType currentTestType : ARRAY_TEST_TYPES) {
      BigInteger sizeOfCurrentTypeInBits =
          MACHINE_MODEL.getSizeof(currentTestType).multiply(BigInteger.valueOf(8));
      String arrayVariableName = "arrayName" + currentTestType;
      Value heapAddress = setupHeapArray(arrayVariableName, currentTestType);

      // Test & on every array entry (create every possible valid pointer to it)
      for (int currentIndice = 0; currentIndice < TEST_ARRAY_LENGTH; currentIndice++) {
        // &*array[currentIndice]
        CUnaryExpression arrayAmperOfPointerExpr =
            wrapInAmper(
                arraySubscriptHeapAccess(arrayVariableName, currentTestType, currentIndice));

        List<ValueAndSMGState> resultList = arrayAmperOfPointerExpr.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
        assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().longValue() == 0)
            .isTrue();
        resultValue = ((AddressExpression) resultValue).getMemoryAddress();
        // & actually changes the state!
        currentState = resultList.get(0).getState();

        SMGObject expectedTarget =
            currentState.dereferencePointer(heapAddress).get(0).getSMGObject();
        BigInteger expectedOffset =
            BigInteger.valueOf(currentIndice).multiply(sizeOfCurrentTypeInBits);
        // The returned Value is an address, theoretically addresses may be any Value type
        assertThat(resultValue).isInstanceOf(Value.class);
        // First check general existance of a points to edge, then its target and offset
        assertThat(currentState.dereferencePointer(resultValue).get(0).hasSMGObjectAndOffset())
            .isTrue();
        assertThat(currentState.dereferencePointer(resultValue).get(0).getSMGObject())
            .isEqualTo(expectedTarget);
        assertThat(currentState.dereferencePointer(resultValue).get(0).getOffsetForObject())
            .isEqualTo(expectedOffset);
        // Check that the other methods return the correct points-to-edge leading to the correct
        // memory location and never to the 0 object
        assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
            .isNotEqualTo(SMGObject.nullInstance());
        assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
            .isEqualTo(expectedTarget);

        // The reverse applies as well
        assertThat(
                currentState
                    .getMemoryModel()
                    .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                    .isPresent())
            .isTrue();

        SMGValue smgValueForPointer =
            currentState
                .getMemoryModel()
                .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                .orElseThrow();
        Value valueForSMGValue =
            currentState.getMemoryModel().getValueFromSMGValue(smgValueForPointer).orElseThrow();
        assertThat(valueForSMGValue).isEqualTo(resultValue);
      }
    }
  }

  /*
   * Test amper (address) operator in structs on the stack.
   * *&(struct) == struct with the struct on the stack
   */
  @Test
  public void testAddressOperatorOnStackStruct() throws CPATransferException {
    for (int j = 0; j < STRUCT_UNION_TEST_TYPES.size() - 1; j++) {
      List<CType> listOfTypes = STRUCT_UNION_TEST_TYPES.subList(0, j + 1);
      // Now create the SMGState, SPC and SMG with the struct already present and values written
      // Name the struct and var uniquely such that we have all possible vars on the stack at the
      // same time
      createStackVarOnStackAndFill(COMPOSITE_VARIABLE_NAME + j, listOfTypes);

      CUnaryExpression amperFieldRef =
          wrapInAmper(
              exprForStructOrUnionOnStackVar(
                  COMPOSITE_DECLARATION_NAME + j,
                  COMPOSITE_VARIABLE_NAME + j,
                  listOfTypes,
                  false,
                  ComplexTypeKind.STRUCT));

      List<ValueAndSMGState> resultList = amperFieldRef.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
      assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().longValue() == 0)
          .isTrue();
      resultValue = ((AddressExpression) resultValue).getMemoryAddress();
      // & actually changes the state!
      currentState = resultList.get(0).getState();

      SMGObject expectedTarget =
          currentState
              .getMemoryModel()
              .getObjectForVisibleVariable(COMPOSITE_VARIABLE_NAME + j)
              .orElseThrow();
      BigInteger expectedOffset = BigInteger.ZERO;
      // The returned Value is an address, theoretically addresses may be any Value type
      assertThat(resultValue).isInstanceOf(Value.class);
      // First check general existance of a points to edge, then its target and offset
      assertThat(currentState.dereferencePointer(resultValue).get(0).hasSMGObjectAndOffset())
          .isTrue();
      assertThat(currentState.dereferencePointer(resultValue).get(0).getSMGObject())
          .isEqualTo(expectedTarget);
      assertThat(currentState.dereferencePointer(resultValue).get(0).getOffsetForObject())
          .isEqualTo(expectedOffset);
      // Check that the other methods return the correct points-to-edge leading to the correct
      // memory location and never to the 0 object
      assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
          .isNotEqualTo(SMGObject.nullInstance());
      assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
          .isEqualTo(expectedTarget);

      // The reverse applies as well
      assertThat(
              currentState
                  .getMemoryModel()
                  .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                  .isPresent())
          .isTrue();

      SMGValue smgValueForPointer =
          currentState
              .getMemoryModel()
              .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
              .orElseThrow();
      Value valueForSMGValue =
          currentState.getMemoryModel().getValueFromSMGValue(smgValueForPointer).orElseThrow();
      assertThat(valueForSMGValue).isEqualTo(resultValue);
    }
  }

  /*
   * Test amper (address) operator in structs on the heap.
   * *&(struct) == struct with the struct on the stack but as struct is type * the result of & will be **.
   */
  @Test
  public void testAddressOperatorOnHeapStruct() throws Exception {
    for (int j = 0; j < STRUCT_UNION_TEST_TYPES.size() - 1; j++) {
      List<CType> listOfTypes = STRUCT_UNION_TEST_TYPES.subList(0, j + 1);
      // Now create the SMGState, SPC and SMG with the struct already present and values written
      // Name the struct and var uniquely such that we have all possible vars on the stack at the
      // same time
      setupHeapStructAndFill(COMPOSITE_VARIABLE_NAME + j, listOfTypes);

      CUnaryExpression amperStructRef =
          wrapInAmper(
              createPointerRefForStructPointerNoDeref(
                      COMPOSITE_DECLARATION_NAME + j, COMPOSITE_VARIABLE_NAME + j, listOfTypes)
                  .getOperand());

      List<ValueAndSMGState> resultList = amperStructRef.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
      assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().longValue() == 0)
          .isTrue();
      resultValue = ((AddressExpression) resultValue).getMemoryAddress();
      // & actually changes the state!
      currentState = resultList.get(0).getState();

      // We want the address of the address to the struct!
      SMGObject expectedTarget =
          currentState
              .getMemoryModel()
              .getObjectForVisibleVariable(COMPOSITE_VARIABLE_NAME + j)
              .orElseThrow();
      BigInteger expectedOffset = BigInteger.ZERO;
      // The returned Value is an address, theoretically addresses may be any Value type
      assertThat(resultValue).isInstanceOf(Value.class);
      // First check general existance of a points to edge, then its target and offset
      SMGStateAndOptionalSMGObjectAndOffset resultMaybeTarget =
          currentState.dereferencePointer(resultValue).get(0);
      assertThat(resultMaybeTarget.hasSMGObjectAndOffset()).isTrue();
      assertThat(resultMaybeTarget.getSMGObject()).isEqualTo(expectedTarget);
      assertThat(resultMaybeTarget.getOffsetForObject()).isEqualTo(expectedOffset);
      // Check that the other methods return the correct points-to-edge leading to the correct
      // memory location and never to the 0 object
      assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
          .isNotEqualTo(SMGObject.nullInstance());
      assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
          .isEqualTo(expectedTarget);

      // The reverse applies as well
      assertThat(
              currentState
                  .getMemoryModel()
                  .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                  .isPresent())
          .isTrue();

      SMGValue smgValueForPointer =
          currentState
              .getMemoryModel()
              .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
              .orElseThrow();
      Value valueForSMGValue =
          currentState.getMemoryModel().getValueFromSMGValue(smgValueForPointer).orElseThrow();
      assertThat(valueForSMGValue).isEqualTo(resultValue);
    }
  }

  /*
   * Test *&((*struct).field) == (*struct).field with the struct on the heap
   */
  @Test
  public void testAddressOperatorOnStructMembersOnHeap()
      throws InvalidConfigurationException, CPATransferException {
    for (int j = 0; j < STRUCT_UNION_TEST_TYPES.size() - 1; j++) {
      List<CType> listOfTypes = STRUCT_UNION_TEST_TYPES.subList(0, j + 1);
      // Now create the SMGState, SPC and SMG with the struct already present and values written
      // Name the struct and var uniquely such that we have all possible vars on the stack at the
      // same time
      Value addressForHeap = setupHeapStructAndFill(COMPOSITE_VARIABLE_NAME + j, listOfTypes);

      for (int indice = 0; indice < listOfTypes.size(); indice++) {
        // We deref here so its struct->field
        CUnaryExpression amperFieldRef =
            wrapInAmper(
                createFieldRefForStackVar(
                    COMPOSITE_DECLARATION_NAME + j,
                    COMPOSITE_VARIABLE_NAME + j,
                    indice,
                    listOfTypes,
                    true,
                    ComplexTypeKind.STRUCT));

        List<ValueAndSMGState> resultList = amperFieldRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
        assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().longValue() == 0)
            .isTrue();
        resultValue = ((AddressExpression) resultValue).getMemoryAddress();
        // & actually changes the state!
        currentState = resultList.get(0).getState();

        SMGObject expectedTarget =
            currentState.dereferencePointer(addressForHeap).get(0).getSMGObject();
        BigInteger expectedOffset =
            BigInteger.valueOf(getOffsetInBitsWithPadding(listOfTypes, indice));
        // The returned Value is an address, theoretically addresses may be any Value type
        assertThat(resultValue).isInstanceOf(Value.class);
        // First check general existance of a points to edge, then its target and offset
        assertThat(currentState.dereferencePointer(resultValue).get(0).hasSMGObjectAndOffset())
            .isTrue();
        assertThat(currentState.dereferencePointer(resultValue).get(0).getSMGObject())
            .isEqualTo(expectedTarget);
        assertThat(currentState.dereferencePointer(resultValue).get(0).getOffsetForObject())
            .isEqualTo(expectedOffset);
        // Check that the other methods return the correct points-to-edge leading to the correct
        // memory location and never to the 0 object
        assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
            .isNotEqualTo(SMGObject.nullInstance());
        assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
            .isEqualTo(expectedTarget);

        // The reverse applies as well
        assertThat(
                currentState
                    .getMemoryModel()
                    .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                    .isPresent())
            .isTrue();

        SMGValue smgValueForPointer =
            currentState
                .getMemoryModel()
                .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                .orElseThrow();
        Value valueForSMGValue =
            currentState.getMemoryModel().getValueFromSMGValue(smgValueForPointer).orElseThrow();
        assertThat(valueForSMGValue).isEqualTo(resultValue);
      }
    }
  }

  /*
   * Test *&(struct.field) == struct.field with the struct on the stack
   */
  @Test
  public void testAddressOperatorOnStructMembersOnStack() throws CPATransferException {
    for (int j = 0; j < STRUCT_UNION_TEST_TYPES.size() - 1; j++) {
      List<CType> listOfTypes = STRUCT_UNION_TEST_TYPES.subList(0, j + 1);
      // Now create the SMGState, SPC and SMG with the struct already present and values written
      // Name the struct and var uniquely such that we have all possible vars on the stack at the
      // same time
      createStackVarOnStackAndFill(COMPOSITE_VARIABLE_NAME + j, listOfTypes);

      for (int indice = 0; indice < listOfTypes.size(); indice++) {
        CUnaryExpression amperFieldRef =
            wrapInAmper(
                createFieldRefForStackVar(
                    COMPOSITE_DECLARATION_NAME + j,
                    COMPOSITE_VARIABLE_NAME + j,
                    indice,
                    listOfTypes,
                    false,
                    ComplexTypeKind.STRUCT));

        List<ValueAndSMGState> resultList = amperFieldRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        assertThat(((AddressExpression) resultValue).getOffset().isNumericValue()).isTrue();
        assertThat(((AddressExpression) resultValue).getOffset().asNumericValue().longValue() == 0)
            .isTrue();
        resultValue = ((AddressExpression) resultValue).getMemoryAddress();
        // & actually changes the state!
        currentState = resultList.get(0).getState();

        SMGObject expectedTarget =
            currentState
                .getMemoryModel()
                .getObjectForVisibleVariable(COMPOSITE_VARIABLE_NAME + j)
                .orElseThrow();
        BigInteger expectedOffset =
            BigInteger.valueOf(getOffsetInBitsWithPadding(listOfTypes, indice));
        // The returned Value is an address, theoretically addresses may be any Value type
        assertThat(resultValue).isInstanceOf(Value.class);
        // First check general existance of a points to edge, then its target and offset
        assertThat(currentState.dereferencePointer(resultValue).get(0).hasSMGObjectAndOffset())
            .isTrue();
        assertThat(currentState.dereferencePointer(resultValue).get(0).getSMGObject())
            .isEqualTo(expectedTarget);
        assertThat(currentState.dereferencePointer(resultValue).get(0).getOffsetForObject())
            .isEqualTo(expectedOffset);
        // Check that the other methods return the correct points-to-edge leading to the correct
        // memory location and never to the 0 object
        assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
            .isNotEqualTo(SMGObject.nullInstance());
        assertThat(currentState.getPointsToTarget(resultValue).orElseThrow().getSMGObject())
            .isEqualTo(expectedTarget);

        // The reverse applies as well
        assertThat(
                currentState
                    .getMemoryModel()
                    .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                    .isPresent())
            .isTrue();

        SMGValue smgValueForPointer =
            currentState
                .getMemoryModel()
                .getAddressValueForPointsToTarget(expectedTarget, expectedOffset)
                .orElseThrow();
        Value valueForSMGValue =
            currentState.getMemoryModel().getValueFromSMGValue(smgValueForPointer).orElseThrow();
        assertThat(valueForSMGValue).isEqualTo(resultValue);
      }
    }
  }

  /*
   * Test sizeof on different simple types. (Note: sizeof returns the amount of bytes!)
   */
  @Test
  public void testSizeofSimpleTypes() throws CPATransferException, InvalidConfigurationException {
    String variableName = "varName";
    for (CType typeToTest : STRUCT_UNION_TEST_TYPES) {
      addStackVariableToMemoryModel(
          variableName, MACHINE_MODEL.getSizeof(typeToTest).intValue() * 8);

      // Create some Value that does not equal any type size. This should NEVER be read!
      Value intValue = new NumericValue(99);

      // Write to the stack var; Note: this is always offset 0!
      writeToStackVariableInMemoryModel(
          variableName, 0, MACHINE_MODEL.getSizeof(typeToTest).intValue() * 8, intValue);

      CVariableDeclaration decl =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              false,
              CStorageClass.AUTO,
              typeToTest,
              variableName + "NotQual",
              variableName + "NotQual",
              variableName,
              CDefaults.forType(typeToTest, FileLocation.DUMMY));

      CUnaryExpression sizeOfVar = wrapInSizeof(new CIdExpression(FileLocation.DUMMY, decl));

      List<ValueAndSMGState> resultList = sizeOfVar.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      // The returned Value should == size if the current type by index (in bytes)
      assertThat(resultValue).isInstanceOf(NumericValue.class);
      // Both as bytes
      assertThat(resultValue.asNumericValue().bigInteger())
          .isEqualTo(MACHINE_MODEL.getSizeof(typeToTest));
      resetSMGStateAndVisitor();
    }
  }

  /*
   * Test sizeof on stack structs with padding and on struct fields. Struct on the stack.
   * (Note: sizeof returns the amount of bytes!)
   */
  @Test
  public void testSizeofStructs() throws CPATransferException, InvalidConfigurationException {
    for (int j = 0; j < STRUCT_UNION_TEST_TYPES.size() - 1; j++) {
      List<CType> listOfTypes = STRUCT_UNION_TEST_TYPES.subList(0, j + 1);
      // Now create the SMGState, SPC and SMG with the struct already present and values written
      createStackVarOnStackAndFill(COMPOSITE_VARIABLE_NAME, listOfTypes);

      // Test sizeof all fields individially
      for (int i = 0; i < listOfTypes.size(); i++) {
        CUnaryExpression sizeOfFieldRef =
            wrapInSizeof(
                createFieldRefForStackVar(
                    COMPOSITE_DECLARATION_NAME,
                    COMPOSITE_VARIABLE_NAME,
                    i,
                    listOfTypes,
                    false,
                    ComplexTypeKind.STRUCT));

        List<ValueAndSMGState> resultList = sizeOfFieldRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        // The returned Value should == size if the current type by index (in bytes)
        assertThat(resultValue).isInstanceOf(NumericValue.class);
        // Both as bytes
        assertThat(resultValue.asNumericValue().bigInteger())
            .isEqualTo(MACHINE_MODEL.getSizeof(listOfTypes.get(i)));
      }
      // Test sizeof the entire struct
      CUnaryExpression sizeOfVariableRef =
          wrapInSizeof(
              exprForStructOrUnionOnStackVar(
                  COMPOSITE_DECLARATION_NAME,
                  COMPOSITE_VARIABLE_NAME,
                  listOfTypes,
                  false,
                  ComplexTypeKind.STRUCT));

      List<ValueAndSMGState> resultList = sizeOfVariableRef.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      // The returned Value should == size of the entire struct but with padding
      assertThat(resultValue).isInstanceOf(NumericValue.class);
      // Transform the result in bits
      assertThat(resultValue.asNumericValue().bigInteger().multiply(BigInteger.valueOf(8)))
          .isEqualTo(BigInteger.valueOf(getSizeInBitsForListOfCTypeWithPadding(listOfTypes)));
      resetSMGStateAndVisitor();
    }
  }

  /*
   * Test the size of stack unions. They are always the max size of the datatypes inside.
   */
  @Test
  public void testSizeofUnions() throws CPATransferException, InvalidConfigurationException {
    for (int j = 0; j < STRUCT_UNION_TEST_TYPES.size() - 1; j++) {
      List<CType> listOfTypes = STRUCT_UNION_TEST_TYPES.subList(0, j + 1);
      // Create the union once
      addStackVariableToMemoryModel(
          COMPOSITE_VARIABLE_NAME, getLargestSizeInBitsForListOfCType(listOfTypes));

      // Write every possible values once
      for (int i = 0; i < j; i++) {
        // Create a Value that we want to be mapped to a SMGValue to write into the union
        Value intValue = new NumericValue(j);

        // Write to the stack union; Note: this is always offset 0!
        writeToStackVariableInMemoryModel(
            COMPOSITE_VARIABLE_NAME,
            0,
            MACHINE_MODEL.getSizeof(listOfTypes.get(j)).intValue() * 8,
            intValue);

        // We write 1 type i, read all sizes by k and check that the size is the same as for the
        // type! Repeat for all other types in the union currently.
        for (int k = 0; k < listOfTypes.size(); k++) {
          CUnaryExpression sizeofUnionFieldRef =
              wrapInSizeof(
                  createFieldRefForStackVar(
                      COMPOSITE_DECLARATION_NAME,
                      COMPOSITE_VARIABLE_NAME,
                      k,
                      listOfTypes,
                      false,
                      ComplexTypeKind.UNION));

          List<ValueAndSMGState> resultList = sizeofUnionFieldRef.accept(visitor);

          // Assert the correct returns
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          // The returned Value should == size of the current type (at index i)
          assertThat(resultValue).isInstanceOf(NumericValue.class);
          // Transform the result in bits
          assertThat(resultValue.asNumericValue().bigInteger())
              .isEqualTo(MACHINE_MODEL.getSizeof(listOfTypes.get(k)));
        }

        // Test sizeof the entire union
        CUnaryExpression sizeofUnionRef =
            wrapInSizeof(
                exprForStructOrUnionOnStackVar(
                    COMPOSITE_DECLARATION_NAME,
                    COMPOSITE_VARIABLE_NAME,
                    listOfTypes,
                    false,
                    ComplexTypeKind.UNION));

        List<ValueAndSMGState> resultList = sizeofUnionRef.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        // The returned Value should == size of the entire struct but with padding
        assertThat(resultValue).isInstanceOf(NumericValue.class);
        // Transform the result in bits
        assertThat(resultValue.asNumericValue().bigInteger().multiply(BigInteger.valueOf(8)))
            .isEqualTo(BigInteger.valueOf(getLargestSizeInBitsForListOfCType(listOfTypes)));
      }
      resetSMGStateAndVisitor();
    }
  }

  /*
   * Test sizeof on a heap array accessed via pointers. (Note: sizeof returns the amount of bytes!)
   */
  @Test
  public void testSizeofHeapArraysWithPointer()
      throws InvalidConfigurationException, CPATransferException {
    for (CType currentTestType : ARRAY_TEST_TYPES) {
      String arrayVariableName = "arrayName";
      String indiceVarName = "indice";
      setupHeapArray(arrayVariableName, currentTestType);
      setupIndexVariables(indiceVarName);

      // Test size of every array entry
      for (int currentIndice = 0; currentIndice < TEST_ARRAY_LENGTH; currentIndice++) {
        CUnaryExpression arrayPointerExpr =
            wrapInSizeof(arrayPointerAccess(arrayVariableName, currentTestType, currentIndice));

        List<ValueAndSMGState> resultList = arrayPointerExpr.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        // The returned Value should == size of the current type (at index i)
        assertThat(resultValue).isInstanceOf(NumericValue.class);
        // Transform the result in bits
        assertThat(resultValue.asNumericValue().bigInteger())
            .isEqualTo(MACHINE_MODEL.getSizeof(currentTestType));
      }

      // Test size of the pointer (get the pointer access and strip the pointer part)
      CUnaryExpression arrayPointerExpr =
          wrapInSizeof(arrayPointerAccess(arrayVariableName, currentTestType, 0).getOperand());

      List<ValueAndSMGState> resultList = arrayPointerExpr.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      // The returned Value should == size of the current type (at index i)
      assertThat(resultValue).isInstanceOf(NumericValue.class);
      // Transform the result in bits
      assertThat(resultValue.asNumericValue().bigInteger().intValue())
          .isEqualTo(POINTER_SIZE_IN_BITS / 8);

      resetSMGStateAndVisitor();
    }
  }

  /*
   * Test sizeof on a heap array accessed via subscript. (Note: sizeof returns the amount of bytes!)
   */
  @Test
  public void testSizeofHeapArraysWithSubscript()
      throws InvalidConfigurationException, CPATransferException {
    for (CType currentTestType : ARRAY_TEST_TYPES) {
      String arrayVariableName = "arrayName";
      String indiceVarName = "indice";
      setupHeapArray(arrayVariableName, currentTestType);
      setupIndexVariables(indiceVarName);

      // Test size of every array entry
      for (int currentIndice = 0; currentIndice < TEST_ARRAY_LENGTH; currentIndice++) {
        CUnaryExpression arraySubscriptExpr =
            wrapInSizeof(
                arraySubscriptHeapAccessWithVariable(
                    arrayVariableName, indiceVarName + currentIndice, INT_TYPE, currentTestType));

        List<ValueAndSMGState> resultList = arraySubscriptExpr.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        // The returned Value should == size of the current type (at index i)
        assertThat(resultValue).isInstanceOf(NumericValue.class);
        // Transform the result in bits
        assertThat(resultValue.asNumericValue().bigInteger())
            .isEqualTo(MACHINE_MODEL.getSizeof(currentTestType));
      }

      // Test size of the pointer
      CUnaryExpression arraySizeofExpr =
          wrapInSizeof(
              arraySubscriptHeapAccessWithVariable(
                      arrayVariableName, indiceVarName + 0, INT_TYPE, currentTestType)
                  .getArrayExpression());

      List<ValueAndSMGState> resultList = arraySizeofExpr.accept(visitor);

      // Assert the correct returns
      assertThat(resultList).hasSize(1);
      Value resultValue = resultList.get(0).getValue();
      // The returned Value should == size of the current type (at index i)
      assertThat(resultValue).isInstanceOf(NumericValue.class);
      // Transform the result in bits
      assertThat(resultValue.asNumericValue().bigInteger().intValue())
          .isEqualTo(POINTER_SIZE_IN_BITS / 8);

      resetSMGStateAndVisitor();
    }
  }

  /*
   * Test sizeof on a stack array accessed via (const indice) subscript.
   * (Note: sizeof returns the amount of bytes!)
   */
  @Test
  public void testSizeofStackArraysWithSubscript()
      throws InvalidConfigurationException, CPATransferException {
    for (CType currentTestType : ARRAY_TEST_TYPES) {
      String arrayVariableName = "arrayName";

      // Create a stack array and fill it
      setupStackArray(arrayVariableName, currentTestType);

      // Test size of every array entry
      for (int currentIndice = 0; currentIndice < TEST_ARRAY_LENGTH; currentIndice++) {
        CUnaryExpression arraySubscriptExpr =
            wrapInSizeof(
                arraySubscriptStackAccess(
                    arrayVariableName, currentTestType, currentIndice, TEST_ARRAY_LENGTH));

        List<ValueAndSMGState> resultList = arraySubscriptExpr.accept(visitor);

        // Assert the correct returns
        assertThat(resultList).hasSize(1);
        Value resultValue = resultList.get(0).getValue();
        // The returned Value should == size of the current type (at index i)
        assertThat(resultValue).isInstanceOf(NumericValue.class);
        // Transform the result in bits
        assertThat(resultValue.asNumericValue().bigInteger())
            .isEqualTo(MACHINE_MODEL.getSizeof(currentTestType));
      }

      // Test size of the array

      resetSMGStateAndVisitor();
    }
  }

  /*
   * +++++++++++++++++++++++++++++ Helper methods below this point +++++++++++++++++++++++++++++
   */

  /**
   * Wraps the entered expression into an amper (&) expression.
   *
   * @param exprToWrap the {@link CExpression} you want to wrap.
   * @return {@link CUnaryExpression} wrapping the entered expr into a &.
   */
  private CUnaryExpression wrapInAmper(CExpression exprToWrap) {
    CType typeOfExpr = exprToWrap.getExpressionType();
    CType newType = new CPointerType(false, false, typeOfExpr);
    return new CUnaryExpression(
        FileLocation.DUMMY, newType, exprToWrap, CUnaryExpression.UnaryOperator.AMPER);
  }

  /**
   * Wraps the entered expression into a sizeof expression.
   *
   * @param exprToWrap the {@link CExpression} you want to wrap.
   * @return {@link CUnaryExpression} wrapping the entered expr into a sizeof.
   */
  private CUnaryExpression wrapInSizeof(CExpression exprToWrap) {
    return new CUnaryExpression(
        FileLocation.DUMMY, INT_TYPE, exprToWrap, CUnaryExpression.UnaryOperator.SIZEOF);
  }

  /*
   * Assuming that the input is a signed value that may be smaller or bigger than the type entered.
   * Example: short -1 to unsigned short would result in max unsigned short.
   */
  private BigInteger convertToType(BigInteger value, CType type) {
    if (value.compareTo(BigInteger.ZERO) == 0) {
      return value;
    }
    // int byteSize = MachineModel.LINUX64.getSizeofInBits(type.getCanonicalType()).intValueExact()
    // / 8;
    if (type == CHAR_TYPE) {
      return BigInteger.valueOf(value.byteValue());
    } else if (type == SHORT_TYPE) {
      return BigInteger.valueOf(value.shortValue());
    } else if (type == UNSIGNED_SHORT_TYPE) {
      if (value.shortValue() < 0) {
        return BigInteger.valueOf(value.shortValue() & 0xFFFF);
      }
      return BigInteger.valueOf(value.shortValue());
    } else if (type == INT_TYPE) {
      return BigInteger.valueOf(value.intValue());
    } else if (type == UNSIGNED_INT_TYPE) {
      return BigInteger.valueOf(Integer.toUnsignedLong(value.intValue()));
    } else if (type == LONG_TYPE) {
      return BigInteger.valueOf(value.longValue());
    } else if (type == UNSIGNED_LONG_TYPE) {
      BigInteger longValue = BigInteger.valueOf(value.longValue());
      if (longValue.signum() < 0) {
        return longValue.add(BigInteger.ONE.shiftLeft(64));
      }
      return longValue;
    }
    // TODO: float/double
    return BigInteger.ZERO;
  }

  private int getSizeInBitsForListOfCTypeWithPadding(List<CType> listOfTypes) {
    // Just use the machine model to get the padded size correctly
    ImmutableList.Builder<CCompositeTypeMemberDeclaration> builder = new ImmutableList.Builder<>();
    for (int i = 0; i < listOfTypes.size(); i++) {
      builder.add(new CCompositeTypeMemberDeclaration(listOfTypes.get(i), "field" + i));
    }
    List<CCompositeTypeMemberDeclaration> members = builder.build();

    CCompositeType structType =
        new CCompositeType(false, false, ComplexTypeKind.STRUCT, members, "someName", "someName");
    CElaboratedType elaboratedType =
        new CElaboratedType(
            false, false, ComplexTypeKind.STRUCT, "someName", "someName", structType);
    return MACHINE_MODEL.getSizeofInBits(elaboratedType).intValue();
  }

  @SuppressWarnings("unused")
  private int getSizeInBitsForListOfCTypeWithoutPadding(List<CType> listOfTypes) {
    int size = 0;
    for (CType type : listOfTypes) {
      size += MACHINE_MODEL.getSizeof(type).intValue() * 8;
    }
    return size;
  }

  private int getLargestSizeInBitsForListOfCType(List<CType> listOfTypes) {
    int size = 0;
    for (CType type : listOfTypes) {
      int tempSize = MACHINE_MODEL.getSizeof(type).intValue() * 8;
      if (tempSize > size) {
        size = tempSize;
      }
    }
    return size;
  }

  /**
   * Returns the offset for the index given with the list of types given. This respects padding and
   * assumes that the struct with the types has those in the exact order as it is given in the list.
   *
   * @param listOfTypes the list of types in the struct.
   * @param offsetBeginning where you want the offset.
   * @return offset with padding at index offsetBeginning.
   */
  private int getOffsetInBitsWithPadding(List<CType> listOfTypes, int offsetBeginning) {
    int offset = 0;
    for (int j = 0; j < offsetBeginning; j++) {
      offset += MACHINE_MODEL.getSizeof(listOfTypes.get(j)).intValue() * 8;
      // Take padding into account (the next element always exists)
      int mod = offset % (MACHINE_MODEL.getSizeof(listOfTypes.get(j + 1)).intValue() * 8);
      if (mod != 0) {
        offset += mod;
      }
    }
    return offset;
  }

  @SuppressWarnings("unused")
  private int getOffsetInBitsWithoutPadding(List<CType> listOfTypes, int offsetBeginning) {
    int offset = 0;
    for (int j = 0; j < offsetBeginning; j++) {
      offset += MACHINE_MODEL.getSizeof(listOfTypes.get(j)).intValue() * 8;
    }
    return offset;
  }

  /**
   * Creates a stack variable named after the input {@link String} and puts a struct behind it with
   * the arguments in the order of the list of types given. Those are then filled with numeric
   * values equalling their index in the list of types.
   *
   * @param variableName name of the struct variable on the stack.
   * @param testTypesList list of types in the struct in the order of the list.
   * @throws SMG2Exception should never be thrown.
   */
  private void createStackVarOnStackAndFill(String variableName, List<CType> testTypesList)
      throws SMG2Exception {
    addStackVariableToMemoryModel(
        variableName, getSizeInBitsForListOfCTypeWithPadding(testTypesList));

    for (int i = 0; i < testTypesList.size(); i++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);

      // Write to the stack var
      writeToStackVariableInMemoryModel(
          variableName,
          getOffsetInBitsWithPadding(testTypesList, i),
          MACHINE_MODEL.getSizeof(testTypesList.get(i)).intValue() * 8,
          intValue);
    }
  }

  public CPointerExpression createPointerRefForStructPointerNoDeref(
      String structName, String variableName, List<CType> fieldTypes) {
    ImmutableList.Builder<CCompositeTypeMemberDeclaration> builder = new ImmutableList.Builder<>();
    for (int i = 0; i < fieldTypes.size(); i++) {
      builder.add(new CCompositeTypeMemberDeclaration(fieldTypes.get(i), "field" + i));
    }
    List<CCompositeTypeMemberDeclaration> members = builder.build();

    CCompositeType structType =
        new CCompositeType(false, false, ComplexTypeKind.STRUCT, members, structName, structName);

    CElaboratedType elaboratedType =
        new CElaboratedType(
            false, false, ComplexTypeKind.STRUCT, structName, structName, structType);

    CPointerType structPointerType = new CPointerType(false, false, elaboratedType);

    CSimpleDeclaration declararation =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            structPointerType,
            variableName + "NotQual",
            variableName + "NotQual",
            variableName,
            CDefaults.forType(structPointerType, FileLocation.DUMMY));

    CIdExpression structVarExpr = new CIdExpression(FileLocation.DUMMY, declararation);

    return new CPointerExpression(FileLocation.DUMMY, elaboratedType, structVarExpr);
  }

  /**
   * Create a CFieldReference for a struct that is on the heap and has no pointer deref. Meaning
   * access like: (*structAdress).field
   *
   * @param structName the name of the struct when the type is declared. NOT the struct variable!
   *     I.e. Books for: struct Books { int number; ...}
   * @param variableName the name of the variable on the stack that holds the pointer. This will be
   *     the qualified variable name! There name/original name will be some random String!
   * @param fieldNumberToRead the field you want to read in the end. See fieldTypes!
   * @param fieldTypes the types for the fields. There will be exactly as much fields as types, in
   *     the order given. The fields are simply named field, field1 etc. starting from 0.
   * @return the {@link CFieldReference}
   */
  public CFieldReference createStructFieldRefWithPointerNoDeref(
      String structName, String variableName, int fieldNumberToRead, List<CType> fieldTypes) {

    CPointerExpression structPointerExpr =
        createPointerRefForStructPointerNoDeref(structName, variableName, fieldTypes);

    // The type structure comes from createPointerRefForStructPointerNoDeref()
    CElaboratedType elaboratedType = (CElaboratedType) structPointerExpr.getExpressionType();

    CCompositeType structType = (CCompositeType) elaboratedType.getRealType();

    // w/o pointer dereference (*struct).field
    // This is the reference given to the visitor
    return new CFieldReference(
        FileLocation.DUMMY,
        structType.getMembers().get(fieldNumberToRead).getType(), // return type of field
        "field" + fieldNumberToRead,
        structPointerExpr,
        false);
  }

  public CIdExpression exprForStructOrUnionOnStackVar(
      String structName,
      String variableName,
      List<CType> fieldTypes,
      boolean deref,
      ComplexTypeKind structOrUnion) {
    ImmutableList.Builder<CCompositeTypeMemberDeclaration> builder = new ImmutableList.Builder<>();
    for (int i = 0; i < fieldTypes.size(); i++) {
      builder.add(new CCompositeTypeMemberDeclaration(fieldTypes.get(i), "field" + i));
    }
    List<CCompositeTypeMemberDeclaration> members = builder.build();

    CCompositeType structType =
        new CCompositeType(false, false, structOrUnion, members, structName, structName);

    CElaboratedType elaboratedType =
        new CElaboratedType(false, false, structOrUnion, structName, structName, structType);

    CPointerType structPointerType = new CPointerType(false, false, elaboratedType);

    CSimpleDeclaration declararation;

    if (deref) {
      declararation =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              false,
              CStorageClass.AUTO,
              structPointerType,
              variableName + "NotQual",
              variableName + "NotQual",
              variableName,
              CDefaults.forType(structPointerType, FileLocation.DUMMY));
    } else {
      declararation =
          new CVariableDeclaration(
              FileLocation.DUMMY,
              false,
              CStorageClass.AUTO,
              elaboratedType,
              variableName + "NotQual",
              variableName + "NotQual",
              variableName,
              CDefaults.forType(elaboratedType, FileLocation.DUMMY));
    }

    return new CIdExpression(FileLocation.DUMMY, declararation);
  }

  /**
   * Create a CFieldReference for a struct that is on the stack and has no pointer deref.
   *
   * @param structName the name of the struct when the type is dclared. NOT the struct variable!
   *     I.e. Books for: struct Books { int number; ...}
   * @param variableName the name of the variable on the stack that holds the struct. This will be
   *     the qualified variable name! There name/original name will be some random String!
   * @param fieldNumberToRead the field you want to read in the end. See fieldTypes!
   * @param fieldTypes the types for the fields. There will be exactly as much fields as types, in
   *     the order given. The fields are simply named field, field1 etc. starting from 0.
   * @param deref if true its struct->field, if its false its struct.field
   * @param structOrUnion ComplexTypeKind either union or struct
   * @return the {@link CFieldReference}
   */
  public CFieldReference createFieldRefForStackVar(
      String structName,
      String variableName,
      int fieldNumberToRead,
      List<CType> fieldTypes,
      boolean deref,
      ComplexTypeKind structOrUnion) {

    CExpression structVarExpr =
        exprForStructOrUnionOnStackVar(structName, variableName, fieldTypes, deref, structOrUnion);
    CType structExprType = structVarExpr.getExpressionType();
    // If you want to know more about the type nesting look into exprForStructOrUnionOnStackVar()
    if (structExprType instanceof CPointerType) {
      structExprType = ((CPointerType) structExprType).getType();
    }
    CCompositeType structRetType =
        (CCompositeType) ((CElaboratedType) structExprType).getRealType();
    // w/o pointer dereference struct.field
    // This is the reference given to the visitor
    return new CFieldReference(
        FileLocation.DUMMY,
        structRetType.getMembers().get(fieldNumberToRead).getType(), // return type of field
        "field" + fieldNumberToRead,
        structVarExpr, // CIdExpr
        deref);
  }

  /*
   * Create a stack array with the name and type entered with size TEST_ARRAY_LENGTH.
   * This is then filled with values using transformInputIntoValue() and the indice
   * of the element. Check results using checkValue()!
   */
  private void setupStackArray(String arrayVariableName, CType arrayElementType)
      throws SMG2Exception {
    int sizeOfCurrentTypeInBits = MACHINE_MODEL.getSizeof(arrayElementType).intValue() * 8;
    // Create the array on the stack; size is type size in bits * size of array
    addStackVariableToMemoryModel(arrayVariableName, sizeOfCurrentTypeInBits * TEST_ARRAY_LENGTH);

    // Now write some distinct values into the array, for signed we want to test negatives!
    for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the array depending
      // on the type
      Value arrayValue = transformInputIntoValue(arrayElementType, k);

      // Write to the stack array
      writeToStackVariableInMemoryModel(
          arrayVariableName, sizeOfCurrentTypeInBits * k, sizeOfCurrentTypeInBits, arrayValue);
    }
  }

  /**
   * Creates a stack variable named stackVariableName that points to a heap section with a struct
   * with the types from listOfTypes. The struct will be filled with values with
   * transformInputIntoValue(currentType, index). Only check return values with checkValue()!
   *
   * @param stackVariableName name of the stack variable.
   * @param listOfTypes the list of types in the struct in the order they should be in the struct.
   * @return the Value that is the address of the memory on the heap! NOT the stack!
   * @throws InvalidConfigurationException should never be thrown.
   * @throws SMG2Exception should never be thrown.
   */
  private Value setupHeapStructAndFill(String stackVariableName, List<CType> listOfTypes)
      throws InvalidConfigurationException, SMG2Exception {
    // Address of the struct on the heap
    Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);

    // Add the heap object with padding, then map to stack var
    addHeapVariableToMemoryModel(
        0, getSizeInBitsForListOfCTypeWithPadding(listOfTypes), addressValue);
    addStackVariableToMemoryModel(stackVariableName, POINTER_SIZE_IN_BITS);
    writeToStackVariableInMemoryModel(stackVariableName, 0, POINTER_SIZE_IN_BITS, addressValue);

    // Fill struct completely
    for (int i = 0; i < listOfTypes.size(); i++) {
      CType currentType = listOfTypes.get(i);
      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = transformInputIntoValue(currentType, i);
      // write the value into the struct on the heap
      writeToHeapObjectByAddress(
          addressValue,
          getOffsetInBitsWithPadding(listOfTypes, i),
          MACHINE_MODEL.getSizeof(currentType).intValue() * 8,
          intValue);
    }
    return addressValue;
  }

  /*
   * Creates a array on the heap with TEST_ARRAY_LENGTH entries filled via
   * transformInputIntoValue() by indice position with the type entered and the
   * stack variable accessable via the String that is pointing to the array on the heap.
   * Check return value with checkValue()!
   *
   * returns the addressValue to the heap location. (NOT THE STACK!) If you want the stack use currentState.getMemoryModel().getObjectForVisibleVariable(stackVariableName)
   */
  private Value setupHeapArray(String arrayVariableName, CType currentArrayType)
      throws InvalidConfigurationException, SMG2Exception {
    int sizeOfCurrentTypeInBits = MACHINE_MODEL.getSizeof(currentArrayType).intValue() * 8;
    // address to the heap where the array starts
    Value addressValue = SymbolicValueFactory.getInstance().newIdentifier(null);
    // Create the array on the heap; size is type size in bits * size of array
    addHeapVariableToMemoryModel(0, sizeOfCurrentTypeInBits * TEST_ARRAY_LENGTH, addressValue);
    // Stack variable holding the address (the pointer)
    addStackVariableToMemoryModel(arrayVariableName, POINTER_SIZE_IN_BITS);
    writeToStackVariableInMemoryModel(arrayVariableName, 0, POINTER_SIZE_IN_BITS, addressValue);

    // Now write some distinct values into the array, for signed we want to test negatives!
    for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the array depending
      // on the type
      Value arrayValue = transformInputIntoValue(currentArrayType, k);

      // Write to the heap array
      writeToHeapObjectByAddress(
          addressValue, sizeOfCurrentTypeInBits * k, sizeOfCurrentTypeInBits, arrayValue);
    }
    return addressValue;
  }

  /*
   * Create TEST_ARRAY_LENGTH.size() index variables
   * accessed via indexVariableName + indexNumber with INT_TYPE as index type.
   */
  private void setupIndexVariables(String indexVariableName) throws SMG2Exception {
    for (int k = 0; k < TEST_ARRAY_LENGTH; k++) {
      // create length stack variables holding the indices to access i.e. an array
      addStackVariableToMemoryModel(
          indexVariableName + k, MACHINE_MODEL.getSizeof(INT_TYPE).intValue() * 8);
      writeToStackVariableInMemoryModel(
          indexVariableName + k,
          0,
          MACHINE_MODEL.getSizeof(INT_TYPE).intValue() * 8,
          new NumericValue(k));
    }
  }

  /*
   * Add a stack variable with the entered size to the current memory model present in the state
   * and update the state and visitor. Adds a StackFrame if there is none.
   */
  private void addStackVariableToMemoryModel(String variableName, int sizeInBits)
      throws SMG2Exception {
    if (currentState.getMemoryModel().getStackFrames().size() < 1) {
      // If there is no current stack we add it
      currentState = currentState.copyAndAddStackFrame(CFunctionDeclaration.DUMMY);
    }

    currentState = currentState.copyAndAddLocalVariable(sizeInBits, variableName, null);

    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
  }

  /*
   * Writes a value into the stack variable given at the offset/size given.
   * This expects the stack var to be present! Updates the visitor.
   */
  private void writeToStackVariableInMemoryModel(
      String stackVariableName, int writeOffsetInBits, int writeSizeInBits, Value valueToWrite) {
    if (valueToWrite instanceof AddressExpression) {
      ValueAndSMGState valueToWriteAndState = currentState.transformAddressExpression(valueToWrite);
      valueToWrite = valueToWriteAndState.getValue();
      currentState = valueToWriteAndState.getState();
    }
    Preconditions.checkArgument(!(valueToWrite instanceof AddressExpression));
    SMGValueAndSMGState valueAndState = currentState.copyAndAddValue(valueToWrite);
    SMGValue smgValue = valueAndState.getSMGValue();
    currentState = valueAndState.getSMGState();
    currentState =
        currentState.writeValue(
            currentState
                .getMemoryModel()
                .getObjectForVisibleVariable(stackVariableName)
                .orElseThrow(),
            BigInteger.valueOf(writeOffsetInBits),
            BigInteger.valueOf(writeSizeInBits),
            smgValue);

    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
  }

  /**
   * Creates a SMGObject of the entered size. Then creates a pointer (points to edge) to it at the
   * offset entered. Then the pointer is mapped to a SMGValue that is mapped to the entered value as
   * address value. The global state and visitor are updated with the new items.
   *
   * @param offset in bits of the pointer. Essentially where the pointer starts in the object.
   * @param size of the object in bits.
   * @param addressValue the address to the object. This is a mapping to the object + offset.
   * @throws InvalidConfigurationException should never be thrown.
   */
  private void addHeapVariableToMemoryModel(int offset, int size, Value addressValue)
      throws InvalidConfigurationException {
    SymbolicProgramConfiguration spc = currentState.getMemoryModel();

    SMGObject smgHeapObject = SMGObject.of(0, BigInteger.valueOf(size), BigInteger.valueOf(0));
    spc = spc.copyAndAddHeapObject(smgHeapObject);

    // Mapping to the smg points to edge
    spc =
        spc.copyAndAddPointerFromAddressToRegion(
            addressValue, smgHeapObject, BigInteger.valueOf(offset));

    // This state now has the stack variable that is the pointer to the struct and the struct with a
    // value in the second int, and none in the first
    currentState =
        SMGState.of(
            MachineModel.LINUX64,
            spc,
            logger,
            new SMGOptions(Configuration.defaultConfiguration()),
            currentState.getErrorInfo());
    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
  }

  /**
   * @param pTarget pointer target {@link SMGObject}
   * @param offset offset in bits of the pointer.
   * @throws InvalidConfigurationException should never be thrown
   */
  private Value addPointerToMemory(SMGObject pTarget, int offset)
      throws InvalidConfigurationException {

    ValueAndSMGState addressAndState =
        currentState.searchOrCreateAddress(pTarget, BigInteger.valueOf(offset));

    currentState = addressAndState.getState();
    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
    return addressAndState.getValue();
  }

  private Value addPointerToExistingHeapObject(int offset, Value addressOfTargetWith0Offset)
      throws SMG2Exception {
    // Get the pte for the objects 0 position via the original malloc pointer (always the value in
    // the addressExpr)
    SMGStateAndOptionalSMGObjectAndOffset objectAndOffset =
        currentState.dereferencePointer(addressOfTargetWith0Offset).get(0);
    Preconditions.checkArgument(objectAndOffset.getOffsetForObject().longValue() == 0);

    // Mapping to the smg points to edge
    ValueAndSMGState newPointerValueAndState =
        currentState.searchOrCreateAddress(
            objectAndOffset.getSMGObject(), BigInteger.valueOf(offset));
    currentState = newPointerValueAndState.getState();

    // This state now has the stack variable that is the pointer to the struct and the struct with a
    // value in the second int, and none in the first
    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
    return newPointerValueAndState.getValue();
  }

  /**
   * Writes to the memory of the dereferenced addressValue. Note: the offset entered is always
   * taken! If the address points to an offset, this is ignored!
   *
   * @param addressValue address that dereferences to the object written to.
   * @param writeOffsetInBits offset where to begin write. This is the total offset used in the
   *     write!
   * @param writeSizeInBits size of the write.
   * @param valueToWrite value to be written.
   * @throws SMG2Exception does not happen
   */
  private void writeToHeapObjectByAddress(
      Value addressValue, int writeOffsetInBits, int writeSizeInBits, Value valueToWrite)
      throws InvalidConfigurationException, SMG2Exception {
    SymbolicProgramConfiguration spc = currentState.getMemoryModel();
    SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
        currentState.dereferencePointer(addressValue).get(0);
    spc = spc.copyAndCreateValue(valueToWrite);
    spc =
        spc.writeValue(
            targetAndOffset.getSMGObject(),
            BigInteger.valueOf(writeOffsetInBits),
            BigInteger.valueOf(writeSizeInBits),
            spc.getSMGValueFromValue(valueToWrite).orElseThrow());

    currentState =
        SMGState.of(
            MachineModel.LINUX64,
            spc,
            logger,
            new SMGOptions(Configuration.defaultConfiguration()),
            currentState.getErrorInfo());
    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
  }

  /**
   * CArraySubscriptExpression for array[subscriptIndexInt] access and element type elementType. The
   * array is on the stack! lengthInt denotes the length if the array.
   *
   * @param variableName qualified name of the array on the stack.
   * @param elementType {@link CType} of the array elements.
   * @param subscriptIndexInt index of the array access.
   * @param lengthInt length of the array.
   * @return a {@link CArraySubscriptExpression} as described above.
   */
  public CArraySubscriptExpression arraySubscriptStackAccess(
      String variableName, CType elementType, int subscriptIndexInt, int lengthInt) {

    CIntegerLiteralExpression length =
        new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(lengthInt));

    CIntegerLiteralExpression subscriptAccess =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(subscriptIndexInt));

    /*
    Builder<CInitializer> builder = new ImmutableList.Builder<>();
    for (int i = 0; i < lengthInt; i++) {
      // Just make everything 0, we read the SMG anyway
      builder.add(new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO));
    }

    // Initial List of arguments in the array, may not be the current state of the array!
    CInitializerList initializerList = new CInitializerList(FileLocation.DUMMY, builder.build());
     */

    CArrayType arrayType = new CArrayType(false, false, elementType, length);

    // initializer = null because we model values/memory using the SMG!
    CVariableDeclaration declararation =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            arrayType, // this is always array type for stack arrays
            variableName + "NotQual",
            variableName + "NotQual",
            variableName,
            null);

    CIdExpression idExpr = new CIdExpression(FileLocation.DUMMY, declararation);
    return new CArraySubscriptExpression(FileLocation.DUMMY, elementType, idExpr, subscriptAccess);
  }

  /**
   * Access an array thats on the stack via subscript. The index used is also a variable on the
   * stack. Example: array[index]
   *
   * @param arrayVariableName qualified name of the array on the stack.
   * @param indexVariableName qualified name of the index variable on the stack.
   * @param elementType {@link CType} of the elements of the array
   * @param indexVariableType {@link CType} of the index variable.
   * @param lengthInt length of the array.
   * @return a {@link CArraySubscriptExpression} as described above.
   */
  public CArraySubscriptExpression arraySubscriptStackAccessWithVariable(
      String arrayVariableName,
      String indexVariableName,
      CType elementType,
      CType indexVariableType,
      int lengthInt) {

    CIntegerLiteralExpression length =
        new CIntegerLiteralExpression(FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(lengthInt));

    CArrayType arrayType = new CArrayType(false, false, elementType, length);

    // initializer = null because we model values/memory using the SMG!
    CVariableDeclaration arrayDeclararation =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            arrayType, // this is always array type for stack arrays
            arrayVariableName + "NotQual",
            arrayVariableName + "NotQual",
            arrayVariableName,
            null);

    // initializer = null because we model values/memory using the SMG!
    CVariableDeclaration indexDeclararation =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            indexVariableType,
            indexVariableName + "NotQual",
            indexVariableName + "NotQual",
            indexVariableName,
            null);

    CIdExpression arrayExpr = new CIdExpression(FileLocation.DUMMY, arrayDeclararation);
    CIdExpression indexVarExpr = new CIdExpression(FileLocation.DUMMY, indexDeclararation);
    return new CArraySubscriptExpression(FileLocation.DUMMY, elementType, arrayExpr, indexVarExpr);
  }

  /**
   * CArraySubscriptExpression for an array on the heap via a pointer like:
   * arrayPointer[subscriptIndexInt] access and element type elementType.
   *
   * @param variableName qualified name of the pointer to the array.
   * @param elementType {@link CType} of the elements of the array.
   * @param subscriptIndexInt index integer used to read the array.
   * @return a {@link CArraySubscriptExpression} as described above.
   */
  public CArraySubscriptExpression arraySubscriptHeapAccess(
      String variableName, CType elementType, int subscriptIndexInt) {
    CIntegerLiteralExpression subscriptAccess =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(subscriptIndexInt));

    CPointerType pointerType = new CPointerType(false, false, elementType);

    // initializer = null because we model values/memory using the SMG!
    CVariableDeclaration declararation =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            pointerType,
            variableName + "NotQual",
            variableName + "NotQual",
            variableName,
            null);

    CIdExpression idExpr = new CIdExpression(FileLocation.DUMMY, declararation);
    return new CArraySubscriptExpression(FileLocation.DUMMY, elementType, idExpr, subscriptAccess);
  }

  /**
   * Access an array on the heap via a pointer and subscript. Example: arrayP[index] with index as a
   * variable.
   *
   * @param variableName name of the array pointer variable.
   * @param indexVariableName name of the index variable. Must be on the stack!
   * @param indexVariableType {@link CType} of the index variable.
   * @param elementType {@link CType} of the elements in the array.
   * @return a {@link CArraySubscriptExpression} as described above.
   */
  public CArraySubscriptExpression arraySubscriptHeapAccessWithVariable(
      String variableName, String indexVariableName, CType indexVariableType, CType elementType) {
    CPointerType pointerType = new CPointerType(false, false, elementType);

    // initializer = null because we model values/memory using the SMG!
    CVariableDeclaration arrayDeclararation =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            pointerType,
            variableName + "NotQual",
            variableName + "NotQual",
            variableName,
            null);

    // initializer = null because we model values/memory using the SMG!
    CVariableDeclaration declararationIndex =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            indexVariableType,
            indexVariableName + "NotQual",
            indexVariableName + "NotQual",
            indexVariableName,
            null);

    CIdExpression arrayExpr = new CIdExpression(FileLocation.DUMMY, arrayDeclararation);
    CIdExpression indexExpr = new CIdExpression(FileLocation.DUMMY, declararationIndex);
    return new CArraySubscriptExpression(FileLocation.DUMMY, elementType, arrayExpr, indexExpr);
  }

  /**
   * Access of an int array on the heap with a pointer binary expr, i.e. *(array + 1)
   *
   * @param variableName name of the array pointer variable on the stack (qualified name!).
   * @param arrayIndiceInt the index to be read. 1 in the example above.
   * @param elementType {@link CType} of the elements in the array.
   * @return {@link CPointerExpression} for the described array.
   */
  public CPointerExpression arrayPointerAccess(
      String variableName, CType elementType, int arrayIndiceInt) {
    CIntegerLiteralExpression arrayIndice =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(Math.abs(arrayIndiceInt)));

    // The type for the returned value after the pointer
    CPointerType cPointerReturnType = new CPointerType(false, false, elementType);
    // The type for the returned value after the binary expr
    // TODO: why the f does this change?
    CPointerType cPointerBinaryOperType = new CPointerType(false, false, elementType);

    // initializer = null because we model values/memory using the SMG!
    CVariableDeclaration idDeclararation =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            cPointerReturnType,
            variableName + "NotQual",
            variableName + "NotQual",
            variableName,
            null);

    CIdExpression idExpr = new CIdExpression(FileLocation.DUMMY, idDeclararation);
    if (arrayIndiceInt == 0) {
      // *(array)
      return new CPointerExpression(FileLocation.DUMMY, elementType, idExpr);
    }

    CBinaryExpression.BinaryOperator cBinOperator;
    if (arrayIndiceInt >= 0) {
      cBinOperator = CBinaryExpression.BinaryOperator.PLUS;
    } else {
      cBinOperator = CBinaryExpression.BinaryOperator.MINUS;
    }

    CBinaryExpression arrayExpr =
        new CBinaryExpression(
            FileLocation.DUMMY,
            cPointerReturnType,
            cPointerBinaryOperType,
            idExpr,
            arrayIndice,
            cBinOperator);

    // *(array +- something)
    return new CPointerExpression(FileLocation.DUMMY, elementType, arrayExpr);
  }

  /**
   * Access a pointer with binary expression that has a nested pointer with binary expression in it.
   * Example: *(*(pointer + 1) + 1). The inner pointer deref needs to return another pointer! (or
   * not if you want to test that)
   *
   * @param variableName Name of the innermost pointer stack variable.
   * @param elementType final return {@link CType}.
   * @param innerIndiceInt index for the inner pointer deref. If negative, it will be transformed
   *     into pointer - abs(input).
   * @param outerindiceInt index for outer pointer deref. If negative, it will be transformed into
   *     pointer - abs(input).
   * @return {@link CPointerExpression} with the described properties.
   */
  public CPointerExpression pointerWithBinaryAccessFromExpression(
      String variableName, CType elementType, int innerIndiceInt, int outerindiceInt) {
    // The type for the returned value after the pointer
    CPointerType cPointerReturnType = new CPointerType(false, false, elementType);
    // The inner return type needs to be a nested ** type, since it adds 1 pointer itself we need 1
    // extra
    CPointerExpression inner = arrayPointerAccess(variableName, cPointerReturnType, innerIndiceInt);
    CIntegerLiteralExpression outerIndice =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(Math.abs(outerindiceInt)));

    // The type for the returned value after the binary expr
    CPointerType cPointerBinaryOperType = new CPointerType(false, false, elementType);

    if (outerindiceInt == 0) {
      // *inner
      return new CPointerExpression(FileLocation.DUMMY, elementType, inner);
    }

    CBinaryExpression.BinaryOperator cBinOperator;
    if (outerindiceInt >= 0) {
      cBinOperator = CBinaryExpression.BinaryOperator.PLUS;
    } else {
      cBinOperator = CBinaryExpression.BinaryOperator.MINUS;
    }

    CBinaryExpression outerBinaryExpr =
        new CBinaryExpression(
            FileLocation.DUMMY,
            cPointerReturnType,
            cPointerBinaryOperType,
            inner,
            outerIndice,
            cBinOperator);

    // *(inner +- something)
    return new CPointerExpression(FileLocation.DUMMY, elementType, outerBinaryExpr);
  }

  /**
   * Access of a pointer on the heap with a pointer binary expr, i.e. *(*pointerOfPointer + 1) with
   * pointerOfPointer typed **.
   *
   * @param outerVariableName name of the pointer variable on the stack (qualified name!).
   * @param indiceInt the index to be read. 1 in the example above.
   * @param elementType {@link CType} return element in the end.
   * @return {@link CPointerExpression} for the pointer of the pointer at the index given.
   */
  public CPointerExpression pointerOfPointerAccess(
      String outerVariableName, CType elementType, int indiceInt) {
    CIntegerLiteralExpression arrayIndice =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, INT_TYPE, BigInteger.valueOf(Math.abs(indiceInt)));

    // The type for the returned value after the first deref type*
    CPointerType cPointerReturnType = new CPointerType(false, false, elementType);
    // outermost type; type**
    CPointerType cPointerPointerReturnType = new CPointerType(false, false, cPointerReturnType);
    // The type for the returned value after the binary expr
    // TODO: why the f does this change?
    CPointerType cPointerBinaryOperType = new CPointerType(false, false, elementType);

    // initializer = null because we model values/memory using the SMG!
    CVariableDeclaration declararation =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            cPointerPointerReturnType, // pointer(pointer(elementType))
            outerVariableName + "NotQual",
            outerVariableName + "NotQual",
            outerVariableName,
            null);

    CIdExpression idExpr = new CIdExpression(FileLocation.DUMMY, declararation);
    CPointerExpression pointerToArray =
        new CPointerExpression(FileLocation.DUMMY, cPointerReturnType, idExpr);
    if (indiceInt == 0) {
      // **pointerOfPointer
      return new CPointerExpression(FileLocation.DUMMY, elementType, pointerToArray);
    }

    CBinaryExpression.BinaryOperator cBinOperator;
    if (indiceInt >= 0) {
      cBinOperator = CBinaryExpression.BinaryOperator.PLUS;
    } else {
      cBinOperator = CBinaryExpression.BinaryOperator.MINUS;
    }

    CBinaryExpression binaryExpr =
        new CBinaryExpression(
            FileLocation.DUMMY,
            cPointerReturnType,
            cPointerBinaryOperType,
            pointerToArray,
            arrayIndice,
            cBinOperator);

    // *(*pointerOfPointer +- something)
    return new CPointerExpression(FileLocation.DUMMY, elementType, binaryExpr);
  }

  /**
   * Access of an int array on the heap with a pointer binary expr, i.e. *(arrayP + index) with
   * arrayP as an array pointer and index as a variable.
   *
   * @param arrayVariableName name of the array pointer variable on the stack (qualified name!).
   * @param indexVariableName name of the index variable. Qualified name! Should be a variable that
   *     either really can be there in a C program (no compiliation problem) or can be there as a
   *     result of a read by the analysis (unknown etc.)
   * @param indexVariableType {@link CType} of the index variable.
   * @param elementType {@link CType} of the elements in the array.
   * @return {@link CPointerExpression} for the described array.
   */
  public CPointerExpression arrayPointerAccessPlusVariableIndexOnTheRight(
      String arrayVariableName,
      String indexVariableName,
      CType indexVariableType,
      CType elementType) {
    // The type for the returned value after the pointer
    CPointerType cPointerReturnType = new CPointerType(false, false, elementType);
    // The type for the returned value after the binary expr
    CPointerType cPointerBinaryOperType = new CPointerType(false, false, INT_TYPE);

    return buildCPointerExpressionFromBinary(
        cPointerReturnType,
        cPointerBinaryOperType,
        createCIdExprWithType(cPointerReturnType, arrayVariableName),
        createCIdExprWithType(indexVariableType, indexVariableName),
        elementType,
        CBinaryExpression.BinaryOperator.PLUS);
  }

  /*
   * Same as arrayPointerAccessPlusVariableIndexOnTheRight() but this form *(variable + pointer)
   */
  public CPointerExpression arrayPointerAccessPlusVariableIndexOnTheLeft(
      String arrayVariableName,
      String indexVariableName,
      CType indexVariableType,
      CType elementType) {
    // The type for the returned value after the pointer
    CPointerType cPointerReturnType = new CPointerType(false, false, elementType);
    // The type for the returned value after the binary expr
    CPointerType cPointerBinaryOperType = new CPointerType(false, false, INT_TYPE);

    return buildCPointerExpressionFromBinary(
        cPointerReturnType,
        cPointerBinaryOperType,
        createCIdExprWithType(indexVariableType, indexVariableName),
        createCIdExprWithType(cPointerReturnType, arrayVariableName),
        elementType,
        CBinaryExpression.BinaryOperator.PLUS);
  }

  /*
   * Same as arrayPointerAccessPlusVariableIndexOnTheRight() but this form *(pointer - variable)
   */
  public CPointerExpression arrayPointerAccessMinusVariableIndexOnTheRight(
      String arrayVariableName,
      String indexVariableName,
      CType indexVariableType,
      CType elementType) {
    // The type for the returned value after the pointer
    CPointerType cPointerReturnType = new CPointerType(false, false, elementType);
    // The type for the returned value after the binary expr
    CPointerType cPointerBinaryOperType = new CPointerType(false, false, INT_TYPE);

    return buildCPointerExpressionFromBinary(
        cPointerReturnType,
        cPointerBinaryOperType,
        createCIdExprWithType(cPointerReturnType, arrayVariableName),
        createCIdExprWithType(indexVariableType, indexVariableName),
        elementType,
        CBinaryExpression.BinaryOperator.MINUS);
  }

  /*
   * pointer - pointer = distance of the members (we allow this only for the same data structure!)
   */
  public CBinaryExpression arrayPointerMinusArrayPointer(
      String leftArrayVariableName, String rightArrayVariableName, CType arrayType) {
    // The type for the returned value after the pointer
    CPointerType cPointerReturnType = new CPointerType(false, false, arrayType);
    // The type for the returned value after the binary expr
    CPointerType cPointerBinaryOperType = new CPointerType(false, false, INT_TYPE);

    // In this case the operation and binary type are always equal!
    return new CBinaryExpression(
        FileLocation.DUMMY,
        cPointerBinaryOperType,
        cPointerBinaryOperType,
        createCIdExprWithType(cPointerReturnType, leftArrayVariableName),
        createCIdExprWithType(cPointerReturnType, rightArrayVariableName),
        CBinaryExpression.BinaryOperator.MINUS);
  }

  private CIdExpression createCIdExprWithType(CType indexVariableType, String indexVariableName) {
    // initializer = null because we model values/memory using the SMG!
    CVariableDeclaration declararationIndex =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            indexVariableType,
            indexVariableName,
            indexVariableName,
            indexVariableName,
            null);
    return new CIdExpression(FileLocation.DUMMY, declararationIndex);
  }

  private CPointerExpression buildCPointerExpressionFromBinary(
      CPointerType cPointerReturnType,
      CPointerType cPointerBinaryOperType,
      CIdExpression leftIdExpr,
      CIdExpression rightIdExpr,
      CType elementType,
      CBinaryExpression.BinaryOperator cBinOperator) {

    CBinaryExpression binaryExpr =
        new CBinaryExpression(
            FileLocation.DUMMY,
            cPointerReturnType,
            cPointerBinaryOperType,
            leftIdExpr,
            rightIdExpr,
            cBinOperator);
    return new CPointerExpression(FileLocation.DUMMY, elementType, binaryExpr);
  }

  /**
   * Checks Values with type valueType and original integer numValue. numValue has to be the same as
   * the one used for the expected value such that transformInputIntoValue(numValue) ==
   * valueToCheck!!! Consistent with transformInputIntoValue()!
   *
   * @param valueType {@link CType} for the original input value.
   * @param numValue the original value used as input.
   * @param valueToCheck the result Value to be checked against the numValue.
   */
  private void checkValue(CType valueType, int numValue, Value valueToCheck) {
    Value expected = transformInputIntoValue(valueType, numValue);
    assertThat(valueToCheck).isInstanceOf(expected.getClass());
    if (valueType instanceof CSimpleType) {
      assertThat(valueToCheck).isInstanceOf(NumericValue.class);
      assertThat(valueToCheck.asNumericValue().bigInteger())
          .isEqualTo(expected.asNumericValue().bigInteger());
    } else {
      // may be some other type, arrays, structs etc.
      // TODO:
    }
  }

  /**
   * Generates Values from types and integers (for example loop indices). Use checkValue() to check
   * in the end!
   *
   * @param valueType CType of the original value
   * @param numValue the current value to be transformed into Value. Note: this does not mean the
   *     resulting Value == input! Only check with checkValue().
   * @return some Value for the type and input value.
   */
  private @Nullable Value transformInputIntoValue(CType valueType, int numValue) {
    if (valueType instanceof CSimpleType) {
      if (((CSimpleType) valueType).isSigned()) {
        // Make every second number negative
        return new NumericValue(numValue % 2 == 0 ? numValue : -numValue);
      } else {
        return new NumericValue(numValue);
      }
    } else {
      // may be some other type, arrays, structs etc.
      // TODO:
      return null;
    }
  }
}
