// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * Instances of this class represent arrays with characteristics of Java arrays.
 *
 * <p>Each array has a specific, constant size and can hold up to &lt;size&gt; different values with
 * indexes from 0 to &lt;size - 1&gt;.<br>
 * Each of its fields can be assigned a value with the same type, a subtype of the array or an
 * unknown value. No other types can be stored in instances of this class.
 */
public final class ArrayValue implements Value {

  @Serial private static final long serialVersionUID = -3963825961335658001L;

  // Array type and element type are only used for checking correctness of parameters
  private final JArrayType arrayType;

  /*
   * The element type of arrayType. We store this separately so we don't have to call
   * arrayType.getElementType() for each value we add to the array
   */
  private final JType elementType;

  private final int arraySize;
  private final Value[] values;

  /**
   * Creates a new <code>ArrayValue</code> instance representing an array of the given type and
   * size. No values are initialized.
   *
   * <p>This constructor resembles slightly the following declaration of arrays in Java (for
   * example):
   *
   * <pre>
   *   int[] a = new int[5];
   * </pre>
   *
   * @param pType the type of the array. Only values of this type's element type or subtypes of this
   *     type's element type can be stored in the returned <code>ArrayValue</code> object
   * @param pArraySize the size of the array
   */
  public ArrayValue(JArrayType pType, int pArraySize) {
    arrayType = pType;
    elementType = arrayType != null ? arrayType.getElementType() : null;
    arraySize = pArraySize;
    // we can't use concrete Value types because UnknownValue must be allowed
    values = new Value[pArraySize];

    if (elementType != null) {
      Arrays.fill(values, getInitialValue(elementType));
    } else {
      Arrays.fill(values, UnknownValue.getInstance());
    }
  }

  /**
   * Creates a new <code>ArrayValue</code> instance representing an array of the given type and
   * initialized with the given values. The size of the array is equal to the number of values
   * given.
   *
   * <p>This constructor resembles slightly the following declaration of arrays in Java (for
   * example):
   *
   * <pre>
   *   int[] a = { 1, 2, 3 };
   * </pre>
   *
   * <p>The given list of values may only contain values of types compatible with the given type
   * (that is values of the type or subtypes of this type and instances of {@link
   * Value.UnknownValue}). Otherwise, an <code>IllegalArgumentException</code> is thrown at runtime.
   *
   * @param pType the type of the array. Only values of this type's element type or subtypes of this
   *     type's element type can be stored in the returned <code>ArrayValue</code> object
   * @param pValues a <code>List</code> containing the initial values the array should have
   * @throws IllegalArgumentException if a given value is not compatible with the array type
   */
  public ArrayValue(JArrayType pType, List<Value> pValues) {
    arrayType = pType;
    elementType = arrayType != null ? arrayType.getElementType() : null;
    arraySize = pValues.size();

    for (Value currentValue : pValues) {
      checkValidValue(currentValue);
    }

    values = pValues.toArray(new Value[0]);
  }

  private Value getInitialValue(JType pType) {
    if (pType instanceof JClassOrInterfaceType) {
      return NullValue.getInstance();

    } else if (pType instanceof JSimpleType jSimpleType) {
      return switch (jSimpleType) {
        case BOOLEAN -> BooleanValue.valueOf(false);
        case BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE -> new NumericValue(0L);
        default -> throw new AssertionError("Unhandled type " + pType.getClass());
      };
    } else {
      throw new AssertionError("Unhandled type " + pType.getClass());
    }
  }

  /**
   * Returns a deep copy of the given <code>ArrayValue</code> instance.
   *
   * @param pArrayValue the <code>ArrayValue</code> instance to copy
   * @return a deep copy of the given object
   */
  public static ArrayValue copyOf(ArrayValue pArrayValue) {
    ArrayValue newArray = new ArrayValue(pArrayValue.arrayType, pArrayValue.arraySize);

    int counter = 0;
    for (Value v : pArrayValue.values) {
      if (v instanceof ArrayValue arrayValue) {
        newArray.values[counter] = ArrayValue.copyOf(arrayValue);
      } else {
        newArray.values[counter] = v;
      }

      counter++;
    }

    return newArray;
  }

  private void checkValidValue(Value pValue) {
    checkNotNull(pValue);

    final String errorMessage =
        "Illegal value " + pValue + " to store in array of type " + arrayType;

    if (pValue.isUnknown() || arrayType == null) {
      // as we already check for unknown values here, we won't include it in checks below.
      // this is always fine, do nothing

    } else if (arrayType.getDimensions() > 1) {
      if (!(pValue instanceof ArrayValue || pValue instanceof NullValue)) {
        throw new IllegalArgumentException(errorMessage);
      }
    } else if (elementType instanceof JClassOrInterfaceType && !isValidComplexValue(pValue)) {
      throw new IllegalArgumentException(errorMessage);

    } else if (elementType instanceof JSimpleType concreteType) {
      switch (concreteType) {
        case BYTE, CHAR, SHORT, INT, LONG -> {
          // check that, if Value is of NumericValue, it contains an integer
          if (!(pValue instanceof NumericValue numericValue)
              || (numericValue.doubleValue() % 1) != 0) {
            throw new IllegalArgumentException(errorMessage);
          }
        }
        case FLOAT, DOUBLE -> {
          if (!(pValue instanceof NumericValue)) {
            throw new IllegalArgumentException(errorMessage);
          }
        }
        case BOOLEAN -> {
          if (!(pValue instanceof BooleanValue)) {
            throw new IllegalArgumentException(errorMessage);
          }
        }
        default -> throw new IllegalArgumentException(errorMessage);
      }
    }
  }

  private boolean isValidComplexValue(Value pValue) {
    checkNotNull(pValue);

    return pValue.isUnknown()
        || pValue instanceof NullValue
        || pValue instanceof EnumConstantValue
        || arrayType == null;
  }

  /**
   * Returns the type of this array's values.
   *
   * @return the type of this array's values
   */
  public Type getElementType() {
    return elementType;
  }

  /**
   * Returns the type of this array. This includes information such as the element type and the
   * number of dimensions this array has.
   *
   * @return the {@link JArrayType} of this array
   */
  public JArrayType getArrayType() {
    return arrayType;
  }

  /**
   * Returns the size of this array.
   *
   * @return the size of this array
   */
  public int getArraySize() {
    return arraySize;
  }

  /**
   * Returns the value stored at the specified index.
   *
   * @param pIndex the index to return the value of
   * @return the value stored at the specified index
   */
  public Value getValueAt(int pIndex) {
    checkValidIndex(pIndex);

    return values[pIndex];
  }

  /**
   * Puts the specified value into the specified index of this <code>ArrayValue</code>.
   *
   * @param pValue the value to store at the specified index
   * @param pIndex the index to store the specified value at
   */
  public void setValue(Value pValue, int pIndex) {
    checkValidValue(pValue);
    checkValidIndex(pIndex);

    values[pIndex] = pValue;
  }

  private void checkValidIndex(int pIndex) {
    if (pIndex > values.length - 1 || pIndex < 0) {
      throw new IllegalArgumentException(
          "Array value has size "
              + (values.length - 1)
              + " but asked for access at index "
              + pIndex
              + ".");
    }
  }

  @Override
  public boolean isNumericValue() {
    return false;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  @Override
  public NumericValue asNumericValue() {
    return null;
  }

  @Override
  public Long asLong(CType type) {
    return null;
  }

  @Override
  public <T> T accept(ValueVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }
}
