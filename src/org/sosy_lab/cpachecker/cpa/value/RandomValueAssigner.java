/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.value;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.states.MemoryLocationValueHandler;

/**
 * Memory location value handler that assigns randomly chosen values to the given memory locations.
 */
@Options(prefix = "cpa.value.concolic")
public final class RandomValueAssigner implements MemoryLocationValueHandler {

  private Random rnd;
  private final LogManager logger;
  private HashMap<String,Value> loadedValues = new HashMap<>();

  @Option(
    description = "If this option is set to true, an own symbolic identifier is assigned to"
        + " each array slot when handling non-deterministic arrays of fixed length."
        + " If the length of the array can't be determined, it won't be handled in either cases.")
  private boolean handleArrays = false;

  @Option(description = "Default size of arrays whose length can't be determined.")
  private int defaultArraySize = 20;

  public RandomValueAssigner(LogManager logger, long seed, Configuration pConfig) throws InvalidConfigurationException {
    this.logger = logger;
    this.rnd = new Random(seed);
    pConfig.inject(this);
  }

  public RandomValueAssigner(LogManager logger, Configuration pConfig) throws InvalidConfigurationException {
    this.logger = logger;
    this.rnd = new Random();
    pConfig.inject(this);
  }

  /**
   * Assign a random value to the {@link MemoryLocation} from the given {@link ValueAnalysisState}.
   *
   * @param pMemLocation   the memory location to remove
   * @param pType          the type of the memory location that should be removed
   * @param pPreviousState the {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} which
   *                       preceedes pState. This state will be marked when a random value is
   *                       assigned in pState.
   * @param pState         the {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} to use.
   *                       Value assignments will happen in this state
   * @param pValueVisitor  unused, may be null
   */
  @Override
  public void handle(
      MemoryLocation pMemLocation,
      Type pType,
      ValueAnalysisState pPreviousState,
      ValueAnalysisState pState,
      @Nullable ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCodeException {

    // If the value is preloaded, do not generate a new one and assign
    // the already existing one.
    Value preload = loadedValues.get(pMemLocation.toString());
    if (preload != null) {
      pState.assignConstant(pMemLocation, preload, pType);
      logger.log(Level.INFO, "Reused preloaded value", preload, pMemLocation);
      return;
    }

    if (pType instanceof CSimpleType) {
      createSimpleType(pMemLocation, pType, pPreviousState, pState);
      return;
    }

    if (pType instanceof CArrayType) {
      fillArray(pState, pMemLocation, ((CArrayType) pType), pValueVisitor);
      return;
    }

    throw new IllegalArgumentException(
        "Unknown value was not of simple c type, was "
            + pType.toString()
            + " "
            + pType.getClass().toString());
  }

  /**
   * Add a value as preloaded to this value assigner.
   */
  public void loadValue(String name, Object value){
    if (value instanceof Boolean){
      loadedValues.put(name, BooleanValue.valueOf((Boolean)value));
    } else if (value instanceof Integer){
      loadedValues.put(name, new NumericValue((Integer)value));
    } else if (value instanceof Character){
      loadedValues.put(name, new NumericValue((Integer)value));
    } else if (value instanceof Float) {
      loadedValues.put(name, new NumericValue((Float)value));
    } else if (value instanceof Double) {
      loadedValues.put(name, new NumericValue((Double)value));
    } else if (value instanceof BigInteger) {
      BigInteger v = (BigInteger)value;
      String n = sanitize(name);
      loadedValues.put(n, new NumericValue(v));
      logger.log(Level.INFO, "Preloaded to ", n, v);
    } else {
      throw new IllegalArgumentException(String.format("Did not recognize value for loadedValues Map: %s.", value.getClass()));
    }
  }

  private String sanitize(String name){
    while (Character.isDigit(name.charAt(name.length()-1))) {
      name = name.substring(0, name.length()-1);
    }

    if (name.charAt(name.length()-1) == '@') {
      name = name.substring(0, name.length()-1);
    }

    return name;
  }

  /**
   * Create a simple Type and assign it to the pMemLocation.
   */
  private void createSimpleType(
      MemoryLocation pMemLocation,
      Type pType,
      ValueAnalysisState pPreviousState,
      ValueAnalysisState pState) {

    CBasicType basicType = ((CSimpleType) pType).getType();
    Value value;

    switch (basicType) {
      case UNSPECIFIED:
        // If value is inspecified, forget it.
        pState.forget(pMemLocation);
        return;
      case BOOL:
        value = BooleanValue.valueOf(this.rnd.nextBoolean());
        pPreviousState.nonDeterministicMark = true;
        break;
      case CHAR:
        // Generate randInt with char value range
        value = new NumericValue(this.rnd.nextInt(65536));
        pPreviousState.nonDeterministicMark = true;
        break;
      case INT:
        value = new NumericValue(this.rnd.nextInt());
        pPreviousState.nonDeterministicMark = true;
        this.logger.log(
            Level.INFO,
            "Assigned random value " + value + " to memory location " + pMemLocation);
        break;
      case FLOAT:
        value = new NumericValue(this.rnd.nextFloat());
        pPreviousState.nonDeterministicMark = true;
        break;
      case DOUBLE:
        value = new NumericValue(this.rnd.nextDouble());
        pPreviousState.nonDeterministicMark = true;
        break;

      default:
        throw new IllegalArgumentException("Unknown values of c type " + basicType.name());
    }
    pState.assignConstant(pMemLocation, value, pType);
  }

  /**
   * Fill an array with Values.
   */
  private void fillArray(
      final ValueAnalysisState pState,
      final MemoryLocation pArrayLocation,
      final CArrayType pArrayType,
      final ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCodeException {

    if (!handleArrays) {
      pState.forget(pArrayLocation);
      return;
    }

    CExpression arraySizeExpression = pArrayType.getLength();
    Value arraySizeValue;
    long arraySize;

    if (arraySizeExpression == null) { // array of unknown length
      arraySize = defaultArraySize;
    } else {
      arraySizeValue = arraySizeExpression.accept(pValueVisitor);
      if (!arraySizeValue.isExplicitlyKnown()) {
        arraySize = defaultArraySize;

      } else {
        assert arraySizeValue instanceof NumericValue;

        arraySize = ((NumericValue) arraySizeValue).longValue();
      }
    }

    for (int i = 0; i < arraySize; i++) {
      MemoryLocation arraySlotMemLoc =
          pValueVisitor.evaluateMemLocForArraySlot(pArrayLocation, i, pArrayType);

      handle(arraySlotMemLoc, pArrayType.getType(), null, pState, pValueVisitor);
    }
  }
}
