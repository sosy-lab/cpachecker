// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.cpa.value;

import java.util.Random;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
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
  private static long random_seed = 1636672210L;
  private final LogManager logger;

  @Option(
      description =
          "If this option is set to true, an own symbolic identifier is assigned to each array"
              + " slot when handling non-deterministic arrays of fixed length. If the length of"
              + " the array can't be determined, it won't be handled in either cases.")
  private boolean handleArrays = false;

  @Option(description = "Default size of arrays whose length can't be determined.")
  private int defaultArraySize = 20;

  private MachineModel machineModel;

  public RandomValueAssigner(LogManager logger, Configuration pConfig, MachineModel pMachineModel)
      throws InvalidConfigurationException {
    this.logger = logger;
    this.machineModel = pMachineModel;
    this.rnd = new Random(random_seed);
    pConfig.inject(this);
  }

  /**
   * Assign a random value to the {@link MemoryLocation} from the given {@link ValueAnalysisState}.
   *
   * @param pMemLocation the memory location to remove
   * @param pType the type of the memory location that should be removed
   * @param pPreviousState the {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} which
   *     preceedes pState. This state will be marked when a random value is assigned in pState.
   * @param pState the {@link org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState} to use. Value
   *     assignments will happen in this state
   * @param pValueVisitor unused, may be null
   */
  @Override
  public void handle(
      MemoryLocation pMemLocation,
      Type pType,
      ValueAnalysisState pPreviousState,
      ValueAnalysisState pState,
      @Nullable ExpressionValueVisitor pValueVisitor)
      throws UnrecognizedCodeException {

    if (pType instanceof CSimpleType) {
      createSimpleType(pMemLocation, pType, pPreviousState, pState);
      return;
    }

    if (pType instanceof CArrayType) {
      fillArray(pState, pMemLocation, ((CArrayType) pType), pValueVisitor);
      return;
    }

    pState.forget(pMemLocation);
  }

  /** Create a simple Type and assign it to the pMemLocation. */
  private void createSimpleType(
      MemoryLocation pMemLocation,
      Type pType,
      ValueAnalysisState pPreviousState,
      ValueAnalysisState pState) {

    CBasicType basicType = ((CSimpleType) pType).getType();
    Value value;

    if (basicType.isIntegerType()) {
      value = generateInteger((CSimpleType) pType);
    } else {
      switch (basicType) {
        case UNSPECIFIED:
          // If value is inspecified, forget it.
          pState.forget(pMemLocation);
          return;
        case BOOL:
          value = BooleanValue.valueOf(this.rnd.nextBoolean());
          break;
        case FLOAT:
          value = new NumericValue(this.rnd.nextFloat());
          break;
        case DOUBLE:
          value = new NumericValue(this.rnd.nextDouble());
          break;

        default:
          throw new IllegalArgumentException("Unknown values of c type " + basicType.name());
      }
    }
    pPreviousState.setNonDeterministicMark();
    logger.log(Level.FINE, "Assigning simple value: ", value.toString());
    pState.assignConstant(pMemLocation, value, pType);
  }

  /** Return a random integer in the correct range for this type. */
  private NumericValue generateInteger(CSimpleType pType) {
    long min = this.machineModel.getMinimalIntegerValue(pType).longValue();
    long max = this.machineModel.getMaximalIntegerValue(pType).longValue();
    long random = (long) ((this.rnd.nextDouble() * (max - min)) + min);
    return new NumericValue(random);
  }

  /** Fill an array with Values. */
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
