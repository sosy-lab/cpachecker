// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// file is taken from Legion
package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Optional;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.concolic.ConcolicAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.concolic.NondetLocation;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This provider is instantiated by {@link ValueAnalysisTransferRelation} and is then provided to
 * the {@link ExpressionValueVisitor}. It can contain a List of values to be used by the same to
 * assign to an already known Value instead of performing unknown value handling (e.g. to assign a
 * value to a VERIFIER_nondet_* function call)
 */
public class NondeterministicValueProvider {

  private Map<NondetLocation, List<Value>> knownValuesMap;

  private List<Object> returnedValueHistory = new ArrayList<>();

  private Map<NondetLocation, List<Value>> returnedValueHistoryWithLocation = new HashMap<>();

  private Random rnd;

  public List<Object> getReturnedValueHistory() {
    return returnedValueHistory;
  }

  public Map<NondetLocation, List<Value>> getReturnedValueHistoryWithLocation() {
    return returnedValueHistoryWithLocation;
  }

  public void setValueToReturnedValueHistory(Value pReturnedValue, NondetLocation pLocation) {
    //    Object objectToInsert = null;
    if (!returnedValueHistoryWithLocation.containsKey(pLocation)) {
      returnedValueHistoryWithLocation.put(pLocation, new ArrayList<>());
    }
    returnedValueHistoryWithLocation.get(pLocation).add(pReturnedValue);

    if (pReturnedValue.isNumericValue()) {
      returnedValueHistory.add(pReturnedValue.asNumericValue().getNumber());

    } else if (pReturnedValue instanceof BooleanValue) {
      returnedValueHistory.add(((BooleanValue) pReturnedValue).isTrue());
    } else {
      throw new AssertionError("Unknown value type");
    }
  }

  public NondeterministicValueProvider() {
    knownValuesMap = new HashMap<>();

    this.rnd = new Random(1636672210L);
  }

  public void setKnownValues(Map<NondetLocation, List<Value>> pKnownValues) {
    knownValuesMap = new HashMap<>();
    for (Map.Entry<NondetLocation, List<Value>> entry : pKnownValues.entrySet()) {
      knownValuesMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
  }

  public void clearKnownValues() {
    knownValuesMap = new HashMap<>();
    returnedValueHistory = new ArrayList<>();
    returnedValueHistoryWithLocation = new HashMap<>();
  }

  private Value findMatchingValue(NondetLocation location) {
    for (Map.Entry<NondetLocation, List<Value>> entry : knownValuesMap.entrySet()) {
      NondetLocation key = entry.getKey();
      if (key.lineNumber() == location.lineNumber()
          && key.columnNumberStart() <= location.columnNumberStart()
          && key.columnNumberEnd() >= location.columnNumberEnd()
          && !entry.getValue().isEmpty()) {
        return entry.getValue().remove(0);
      }
    }
    // Return null or a default value if no match is found
    return null;
  }

  public Optional<Value> getNextNondetValueFor(CType pType, NondetLocation pNondetLocation) {
    if (this.knownValuesMap.isEmpty()) {
      return Optional.absent();
    }

    //    NondetLocation key = new NondetLocation(filename, lineNumber, columnNumber);
    Value value = findMatchingValue(pNondetLocation);
    if (value != null) {
      if (!isAssignable(value, pType)) {
        throw new AssertionError("Value is not assignable");
      }
      return Optional.of(value);
    } else {
      return Optional.absent();
    }
  }

  /**
   * Check if value is assignable via the expr (aka. has the correct type)
   *
   * @param value The value to assign.
   * @param expressionType The type the related expression needs to be assignable.
   */
  private Boolean isAssignable(Value value, CType expressionType) {
    // Check basic types
    if (expressionType instanceof CSimpleType) {
      CBasicType type = ((CSimpleType) expressionType).getType();

      if (type == CBasicType.BOOL && value instanceof BooleanValue) {
        return true;
      }
      if (type == CBasicType.CHAR && value instanceof NumericValue) {
        return true;
      }
      if (type == CBasicType.INT && value instanceof NumericValue) {
        return true;
      }
      if (type == CBasicType.FLOAT && value instanceof NumericValue) {
        return true;
      }
      if (type == CBasicType.DOUBLE && value instanceof NumericValue) {
        return true;
      }
    }

    // Ignore complex types for now
    if (!(expressionType instanceof CSimpleType)) {
      return true;
    }

    return false;
  }

//  private void createSimpleType(
//      MemoryLocation pMemLocation, Type pType, ValueAnalysisState pState) {
//
//    CBasicType basicType = ((CSimpleType) pType).getType();
//    Value value;
//
//    if (basicType.isIntegerType()) {
//      value = generateInteger((CSimpleType) pType);
//    } else {
//      switch (basicType) {
//        case UNSPECIFIED:
//          // If value is inspecified, forget it.
//          pState.forget(pMemLocation);
//          return;
//        case BOOL:
//          value = BooleanValue.valueOf(this.rnd.nextBoolean());
//          break;
//        case FLOAT:
//          value = new NumericValue(this.rnd.nextFloat());
//          break;
//        case DOUBLE:
//          value = new NumericValue(this.rnd.nextDouble());
//          break;
//
//        default:
//          throw new IllegalArgumentException("Unknown values of c type " + basicType.name());
//      }
//    }
//    pState.setNonDeterministicMark();
//    logger.log(Level.ALL, "Assigning simple value: ", value);
//    pState.assignConstant(pMemLocation, value, pType);
//  }

  // function taken from Legion
  /** Return a random integer in the correct range for this type. */
  private NumericValue generateInteger(CSimpleType pType) {
//    long min = ConcolicAlgorithm.machineModel.getMinimalIntegerValue(pType).longValue();
//    long max = ConcolicAlgorithm.machineModel.getMaximalIntegerValue(pType).longValue();
    // todo
    // test values between -128 and 127
    // -> sometimes used as a loop counter -> should be small
    long random = this.rnd.nextLong(-128, 127);
//    long random = this.rnd.nextLong(min, max);
    return new NumericValue(random);
  }

  private Value generateChar(CSimpleType pType) {
//    long min = ConcolicAlgorithm.machineModel.getMinimalIntegerValue(pType).longValue();
//    long max = ConcolicAlgorithm.machineModel.getMaximalIntegerValue(pType).longValue();
    // todo
    // test values between -128 and 127
    // -> sometimes used as a loop counter -> should be small
    long random = this.rnd.nextLong(0, 65535);
//    long random = this.rnd.nextLong(min, max);
    char randomChar = (char) random;
    return Value.of(randomChar);
  }

  public Value getRandomValue(CType expressionType) {
    if (expressionType instanceof CSimpleType) {
      CBasicType type = ((CSimpleType) expressionType).getType();

      if (type == CBasicType.BOOL) {
        return BooleanValue.valueOf(this.rnd.nextBoolean());
      }
      if (type == CBasicType.CHAR) {
        return generateChar((CSimpleType) expressionType);
      }
      if (type == CBasicType.INT) {

        return generateInteger((CSimpleType) expressionType);
      }
      if (type == CBasicType.FLOAT) {
        return new NumericValue(this.rnd.nextFloat());
      }
      if (type == CBasicType.DOUBLE) {
        return new NumericValue(this.rnd.nextDouble());
      }
    }
    // 128 bit ints and floats are not natively supported in Java, leave out for now
    // Ignore complex types for now
    if (!(expressionType instanceof CSimpleType)) {
      throw new Error("Non-CSimpleType types are not supported");
    }

    throw new Error("Non-CSimpleType types are not supported");
  }



  public Value getRandomValue_old(CType expressionType) {
    if (expressionType instanceof CSimpleType) {
      CBasicType type = ((CSimpleType) expressionType).getType();

      if (type == CBasicType.BOOL) {
        return Value.of(true);
      }
      if (type == CBasicType.CHAR) {
        //        char c = 'a';
        return Value.of('a');
      }
      if (type == CBasicType.INT) {
        return Value.of(1);
      }
      if (type == CBasicType.FLOAT) {
        return Value.of((float) 0.1);
      }
      if (type == CBasicType.DOUBLE) {
        return Value.of((double) 0);
      }
    }
    // 128 bit ints and floats are not natively supported in Java, leave out for now
    // Ignore complex types for now
    if (!(expressionType instanceof CSimpleType)) {
      throw new Error("Non-CSimpleType types are not supported");
    }

    throw new Error("Non-CSimpleType types are not supported");
  }

  public Object getRandomValueForVERIFIERnondet(String expressionType) {

    if (expressionType.contains("__VERIFIER_nondet_bool()")) {
      return true;
    }
    if (expressionType.contains("__VERIFIER_nondet_char()")) {
      //        char c = 'a';
      return 'a';
    }
    if (expressionType.contains("__VERIFIER_nondet_int()")) {
      return 1;
    }
    if (expressionType.contains("__VERIFIER_nondet_float()")) {
      return (float) 0.1;
    }
    if (expressionType.contains("__VERIFIER_nondet_double()")) {
      return (double) 0;
    }
    if (expressionType.contains("__VERIFIER_nondet_long()")) {
      return (long) 0;
    }

    // Ignore other types for now
    throw new AssertionError("Non-CSimpleType types are not supported");
  }
}
