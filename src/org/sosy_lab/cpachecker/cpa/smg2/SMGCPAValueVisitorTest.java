// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

// TODO: run with more machine models
/* Test all SMGCPAValueVisitor visits. Some will be tested indirectly, for example value creation. */
public class SMGCPAValueVisitorTest {

  private LogManagerWithoutDuplicates logger;
  private SMGCPAValueExpressionEvaluator evaluator;
  private SMGState currentState;

  // The visitor should always use the currentState!
  private SMGCPAValueVisitor visitor;

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
  private static final int POINTER_SIZE =
      MACHINE_MODEL.getSizeof(MACHINE_MODEL.getPointerEquivalentSimpleType()) * 8;

  // Note: padding is on per default, meaning that the types get padding to allign to their
  // "natural" memory offset. I.e. starting with a 2 byte type, then using a 4 byte type would
  // result in a padding of 2 byte between them. This is meant for structs with padding.
  private static final List<CType> structOrUnionTestTypes =
      ImmutableList.of(
          SHORT_TYPE, // 2 byte padding after this!
          INT_TYPE, // No padding after this
          UNSIGNED_SHORT_TYPE, // 2 byte padding after this!
          UNSIGNED_INT_TYPE, // No padding after this
          LONG_TYPE, // No padding after this
          UNSIGNED_LONG_TYPE, // No padding after this
          CHAR_TYPE);

  @Before
  public void init() throws InvalidConfigurationException {
    logger = new LogManagerWithoutDuplicates(LogManager.createTestLogManager());

    evaluator = new SMGCPAValueExpressionEvaluator(MACHINE_MODEL, logger);

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

    String structVariableName = "structVariable";
    String structDeclrName = "structDeclaration";

    for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
      CFieldReference fieldRef =
          createFieldRefForStackVar(
              structDeclrName,
              structVariableName,
              i,
              structOrUnionTestTypes,
              true,
              ComplexTypeKind.STRUCT);

      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);
      Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);

      addHeapVariableToMemoryModel(
          0, getSizeInBitsForListOfCTypeWithPadding(structOrUnionTestTypes), addressValue);
      addStackVariableToMemoryModel(structVariableName, POINTER_SIZE);
      writeToStackVariableInMemoryModel(structVariableName, 0, POINTER_SIZE, addressValue);

      writeToHeapObjectByAddress(
          addressValue,
          getOffsetInBitsWithPadding(structOrUnionTestTypes, i),
          MACHINE_MODEL.getSizeof(structOrUnionTestTypes.get(i)).intValue() * 8,
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
   * This tests the struct field read struct->field with padding.
   * Does not test Strings! Writes ALL values into the struct, then reads repeatedly.
   * Not resets on the memory.
   */
  @Test
  public void readFieldDerefRepeatedTest()
      throws InvalidConfigurationException, CPATransferException {

    String structVariableName = "structVariable";
    String structDeclrName = "structDeclaration";

    // Address of the struct on the heap
    Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);

    // Add the heap object with padding, then map to stack var
    addHeapVariableToMemoryModel(
        0, getSizeInBitsForListOfCTypeWithPadding(structOrUnionTestTypes), addressValue);
    addStackVariableToMemoryModel(structVariableName, POINTER_SIZE);
    writeToStackVariableInMemoryModel(structVariableName, 0, POINTER_SIZE, addressValue);

    // Fill struct completely
    for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);
      // write the value into the struct on the heap
      writeToHeapObjectByAddress(
          addressValue,
          getOffsetInBitsWithPadding(structOrUnionTestTypes, i),
          MACHINE_MODEL.getSizeof(structOrUnionTestTypes.get(i)).intValue() * 8,
          intValue);
    }

    // We read the struct completely more than once, the values may never change!
    for (int j = 0; j < 4; j++) {
      // Read all struct fields once
      for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForStackVar(
                structDeclrName,
                structVariableName,
                i,
                structOrUnionTestTypes,
                true,
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

  /** This tests the struct field read: (*structP).field with padding. Does not test Strings! */
  @Test
  public void readFieldDerefPointerExplicitlyTest()
      throws InvalidConfigurationException, CPATransferException {
    String structVariableName = "structPointerVariable";
    String structDeclrName = "structDeclaration";

    for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);
      Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);

      addHeapVariableToMemoryModel(
          0, getSizeInBitsForListOfCTypeWithPadding(structOrUnionTestTypes), addressValue);
      addStackVariableToMemoryModel(structVariableName, POINTER_SIZE);
      writeToStackVariableInMemoryModel(structVariableName, 0, POINTER_SIZE, addressValue);

      writeToHeapObjectByAddress(
          addressValue,
          getOffsetInBitsWithPadding(structOrUnionTestTypes, i),
          MACHINE_MODEL.getSizeof(structOrUnionTestTypes.get(i)).intValue() * 8,
          intValue);

      CFieldReference fieldRef =
          createFieldRefForPointerNoDeref(
              structDeclrName, structVariableName, i, structOrUnionTestTypes);

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
    String structVariableName = "structPointerVariable";
    String structDeclrName = "structDeclaration";

    Value addressValue = new ConstantSymbolicExpression(new UnknownValue(), null);

    addHeapVariableToMemoryModel(
        0, getSizeInBitsForListOfCTypeWithPadding(structOrUnionTestTypes), addressValue);
    addStackVariableToMemoryModel(structVariableName, POINTER_SIZE);
    writeToStackVariableInMemoryModel(structVariableName, 0, POINTER_SIZE, addressValue);

    for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);

      writeToHeapObjectByAddress(
          addressValue,
          getOffsetInBitsWithPadding(structOrUnionTestTypes, i),
          MACHINE_MODEL.getSizeof(structOrUnionTestTypes.get(i)).intValue() * 8,
          intValue);
    }

    for (int j = 0; j < 4; j++) {
      for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForPointerNoDeref(
                structDeclrName, structVariableName, i, structOrUnionTestTypes);

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

    String structVariableName = "structVariable";
    String structDeclrName = "structDeclaration";

    for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
      CFieldReference fieldRef =
          createFieldRefForStackVar(
              structDeclrName,
              structVariableName,
              i,
              structOrUnionTestTypes,
              false,
              ComplexTypeKind.STRUCT);

      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);

      // Now create the SMGState, SPC and SMG with the struct already present and values written to
      // it
      addStackVariableToMemoryModel(
          structVariableName, getSizeInBitsForListOfCTypeWithPadding(structOrUnionTestTypes));
      // Write to the stack var
      writeToStackVariableInMemoryModel(
          structVariableName,
          getOffsetInBitsWithPadding(structOrUnionTestTypes, i),
          MACHINE_MODEL.getSizeof(structOrUnionTestTypes.get(i)).intValue() * 8,
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

    String structVariableName = "structVariable";
    String structDeclrName = "structDeclaration";

    // Now create the SMGState, SPC and SMG with the struct already present and values written to
    // it
    addStackVariableToMemoryModel(
        structVariableName, getSizeInBitsForListOfCTypeWithPadding(structOrUnionTestTypes));

    for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(i);

      // Write to the stack var
      writeToStackVariableInMemoryModel(
          structVariableName,
          getOffsetInBitsWithPadding(structOrUnionTestTypes, i),
          MACHINE_MODEL.getSizeof(structOrUnionTestTypes.get(i)).intValue() * 8,
          intValue);
    }

    for (int j = 0; j < 4; j++) {
      for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForStackVar(
                structDeclrName,
                structVariableName,
                i,
                structOrUnionTestTypes,
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
   * which is 256 as a int
   * Currently we read by type exactly and this means that we can only read types
   * with the exact size of the value last written. 0 always works!
   */
  @Test
  public void readFieldZeroWithUnionOnStackRepeatedTest() throws CPATransferException {

    String unionVariableName = "unionVariable";
    String unionDeclrName = "unionDeclaration";

    // Create the union once
    addStackVariableToMemoryModel(
        unionVariableName, getLargestSizeInBitsForListOfCType(structOrUnionTestTypes));

    // Write to the stack union the value 0 for all bits.
    writeToStackVariableInMemoryModel(
        unionVariableName,
        0,
        getLargestSizeInBitsForListOfCType(structOrUnionTestTypes),
        new NumericValue(0));

    for (int j = 0; j < 2; j++) {
      // Repeat to check that no value changed!
      for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForStackVar(
                unionDeclrName,
                unionVariableName,
                i,
                structOrUnionTestTypes,
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

    String unionVariableName = "unionVariable";
    String unionDeclrName = "unionDeclaration";

    // Create the union once
    addStackVariableToMemoryModel(
        unionVariableName, getLargestSizeInBitsForListOfCType(structOrUnionTestTypes));

    for (int k = 0; k < structOrUnionTestTypes.size(); k++) {
      // Create a Value that we want to be mapped to a SMGValue to write into the struct
      Value intValue = new NumericValue(k + 1);

      // Write to the stack union; Note: this is always offset 0!
      writeToStackVariableInMemoryModel(
          unionVariableName,
          0,
          MACHINE_MODEL.getSizeof(structOrUnionTestTypes.get(k)).intValue() * 8,
          intValue);

      // We write 1 type k, read all by i more than once (j iterations) and check that its
      // interpretation specific! Repeat for all other types.
      for (int i = 0; i < structOrUnionTestTypes.size(); i++) {
        CFieldReference fieldRef =
            createFieldRefForStackVar(
                unionDeclrName,
                unionVariableName,
                i,
                structOrUnionTestTypes,
                false,
                ComplexTypeKind.UNION);

        for (int j = 0; j < 4; j++) {
          List<ValueAndSMGState> resultList = fieldRef.accept(visitor);

          // Assert the correct returns
          assertThat(resultList).hasSize(1);
          Value resultValue = resultList.get(0).getValue();
          // Check that types with the size of the current have the correct value, and all other
          // should be unknown
          if (MACHINE_MODEL.getSizeof(structOrUnionTestTypes.get(k))
              == MACHINE_MODEL.getSizeof(structOrUnionTestTypes.get(i))) {
            assertThat(resultValue).isInstanceOf(NumericValue.class);
            assertThat(resultValue.asNumericValue().bigInteger())
                .isEqualTo(BigInteger.valueOf(k + 1));
          } else {
            assertThat(resultValue).isInstanceOf(UnknownValue.class);
          }
        }
      }
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

  /**
   * Assuming that the input is a signed value that may be smaller or bigger than the type entered.
   * Example: short -1 to unsigned short would result in max unsigned short.
   */
  private BigInteger convertToType(BigInteger value, CType type) {
    if (value.compareTo(BigInteger.ZERO) == 0) {
      return value;
    }
    //int byteSize = MachineModel.LINUX64.getSizeofInBits(type.getCanonicalType()).intValueExact() / 8;
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
    int size = 0;
    for (int j = 0; j < listOfTypes.size(); j++) {
      size += MACHINE_MODEL.getSizeof(listOfTypes.get(j)).intValue() * 8;
      // Take padding into account
      if (j + 1 < listOfTypes.size()) {
        int mod = size % (MACHINE_MODEL.getSizeof(listOfTypes.get(j + 1)).intValue() * 8);
        if (mod != 0) {
          size += mod;
        }
      }
    }
    return size;
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

  /*
   * Create a CFieldReference for a struct that is on the heap and has no pointer deref. Meaning access like: (*structAdress).field
   *
   * @param structName the name of the struct when the type is declared. NOT the struct variable! I.e. Books for: struct Books { int number; ...}
   * @param variableName the name of the variable on the stack that holds the pointer. This will be the qualified variable name! There name/original name will be some random String!
   * @param fieldNumberToRead the field you want to read in the end. See fieldTypes!
   * @param fieldTypes the types for the fields. There will be exactly as much fields as types, in the order given. The fields are simply named field, field1 etc. starting from 0.
   * @return the {@link CFieldReference}
   */
  public CFieldReference createFieldRefForPointerNoDeref(
      String structName, String variableName, int fieldNumberToRead, List<CType> fieldTypes) {
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

    CExpression structVarExpr = new CIdExpression(FileLocation.DUMMY, declararation);

    CExpression structPointerExpr =
        new CPointerExpression(FileLocation.DUMMY, elaboratedType, structVarExpr);

    // w/o pointer dereference (*struct).field
    // This is the reference given to the visitor
    return new CFieldReference(
        FileLocation.DUMMY,
        structType.getMembers().get(fieldNumberToRead).getType(), // return type of field
        "field" + fieldNumberToRead,
        structPointerExpr, // CIdExpr
        false);
  }

  /*
   * Create a CFieldReference for a struct that is on the stack and has no pointer deref.
   *
   * @param structName the name of the struct when the type is dclared. NOT the struct variable! I.e. Books for: struct Books { int number; ...}
   * @param variableName the name of the variable on the stack that holds the struct. This will be the qualified variable name! There name/original name will be some random String!
   * @param fieldNumberToRead the field you want to read in the end. See fieldTypes!
   * @param fieldTypes the types for the fields. There will be exactly as much fields as types, in the order given. The fields are simply named field, field1 etc. starting from 0.
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

    CExpression structVarExpr = new CIdExpression(FileLocation.DUMMY, declararation);

    // w/o pointer dereference struct.field
    // This is the reference given to the visitor
    return new CFieldReference(
        FileLocation.DUMMY,
        structType.getMembers().get(fieldNumberToRead).getType(), // return type of field
        "field" + fieldNumberToRead,
        structVarExpr, // CIdExpr
        deref);
  }

  /*
   * Add a stack variable with the entered size to the current memory model present in the state
   * and update the state and visitor. Adds a StackFrame if there is none.
   */
  private void addStackVariableToMemoryModel(String variableName, int sizeInBits) {
    if (currentState.getMemoryModel().getStackFrames().size() < 1) {
      // If there is no current stack we add it
      currentState = currentState.copyAndAddStackFrame(CFunctionDeclaration.DUMMY);
    }

    currentState = currentState.copyAndAddLocalVariable(sizeInBits, variableName);

    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
  }

  /*
   * Writes a value into the stack variable given at the offset/size given.
   * This expects the stack var to be present! Updates the visitor.
   */
  private void writeToStackVariableInMemoryModel(
      String stackVariableName, int writeOffsetInBits, int writeSizeInBits, Value valueToWrite) {
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
            new SMGOptions(Configuration.defaultConfiguration()));
    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
  }

  /**
   * Writes to the memory of the dereferenced addressValue. Note: the offset entered is always
   * taken! If the address points to a offset, this is ignored!
   *
   * @param addressValue address that dereferences to the object written to.
   * @param writeOffsetInBits offset where to begin write. This is the total offset used in the
   *     write!
   * @param writeSizeInBits size of the write.
   * @param valueToWrite value to be written.
   */
  private void writeToHeapObjectByAddress(
      Value addressValue, int writeOffsetInBits, int writeSizeInBits, Value valueToWrite)
      throws InvalidConfigurationException {
    SymbolicProgramConfiguration spc = currentState.getMemoryModel();
    SMGObjectAndOffset targetAndOffset = spc.dereferencePointer(addressValue).orElseThrow();
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
            new SMGOptions(Configuration.defaultConfiguration()));
    visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null), logger);
  }
}
