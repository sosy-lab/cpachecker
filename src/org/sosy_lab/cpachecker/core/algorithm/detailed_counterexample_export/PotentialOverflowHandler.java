// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.detailed_counterexample_export;

import java.math.BigInteger;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class PotentialOverflowHandler {

  public record ValueLiteralStub(CSimpleType type, BigInteger value) {}

  public static Optional<ValueLiteralStub> handlePotentialIntegerOverflow(
      AssumptionToEdgeAllocator assumptionToEdgeAllocator,
      BigInteger pIntegerValue,
      CSimpleType pType) {
    BigInteger lowerInclusiveBound =
        assumptionToEdgeAllocator.getMachineModel().getMinimalIntegerValue(pType);
    BigInteger upperInclusiveBound =
        assumptionToEdgeAllocator.getMachineModel().getMaximalIntegerValue(pType);

    assert lowerInclusiveBound.compareTo(upperInclusiveBound) < 0;

    if (pIntegerValue.compareTo(lowerInclusiveBound) < 0
        || pIntegerValue.compareTo(upperInclusiveBound) > 0) {
      if (assumptionToEdgeAllocator.assumeLinearArithmetics()) {
        return Optional.empty();
      }
      LogManagerWithoutDuplicates logManager =
          assumptionToEdgeAllocator.getLogger() instanceof LogManagerWithoutDuplicates
              ? (LogManagerWithoutDuplicates) assumptionToEdgeAllocator.getLogger()
              : new LogManagerWithoutDuplicates(assumptionToEdgeAllocator.getLogger());
      Value castValue =
          AbstractExpressionValueVisitor.castCValue(
              new NumericValue(pIntegerValue),
              pType,
              assumptionToEdgeAllocator.getMachineModel(),
              logManager,
              FileLocation.DUMMY);
      if (castValue.isUnknown()) {
        return Optional.empty();
      }

      Number number = castValue.asNumericValue().getNumber();
      final BigInteger valueAsBigInt;
      if (number instanceof BigInteger) {
        valueAsBigInt = (BigInteger) number;
      } else {
        valueAsBigInt = BigInteger.valueOf(number.longValue());
      }
      pType =
          enlargeTypeIfValueIsMinimalValue(
              assumptionToEdgeAllocator.getLogger(),
              assumptionToEdgeAllocator.getMachineModel(),
              pType,
              valueAsBigInt);
      return Optional.of(new ValueLiteralStub(pType, valueAsBigInt));
    }
    return Optional.of(new ValueLiteralStub(pType, pIntegerValue));
  }

  private static CSimpleType enlargeTypeIfValueIsMinimalValue(
      LogManager pLogger, MachineModel pModel, CSimpleType pType, final BigInteger valueAsBigInt) {
    // In C there are no negative literals, so to represent the minimal value of an integer
    // type, we need that number as positive literal of the next larger type,
    // and then negate it.
    // For example for LONG_MIN we want to have -9223372036854775808UL, so the literal is
    // of type unsigned long and negated. This is only important when exporting the value
    // e.g. inside a witness, since EclipseCDT will not like -9223372036854775808L.
    if (valueAsBigInt.abs().compareTo(pModel.getMaximalIntegerValue(pType)) > 0
        && valueAsBigInt.compareTo(BigInteger.ZERO) < 0
        && pType.getType().isIntegerType()) {
      while (valueAsBigInt.abs().compareTo(pModel.getMaximalIntegerValue(pType)) > 0
          && !nextLargerIntegerTypeIfPossible(pLogger, pType).equals(pType)) {
        pType = nextLargerIntegerTypeIfPossible(pLogger, pType);
      }
    }
    return pType;
  }

  private static CSimpleType nextLargerIntegerTypeIfPossible(
      LogManager pLogManager, CSimpleType pType) {
    if (pType.hasSignedSpecifier()) {
      return new CSimpleType(
          pType.isConst(),
          pType.isVolatile(),
          pType.getType(),
          pType.hasLongSpecifier(),
          pType.hasShortSpecifier(),
          false,
          true,
          pType.hasComplexSpecifier(),
          pType.hasImaginarySpecifier(),
          pType.hasLongLongSpecifier());
    } else {
      switch (pType.getType()) {
        case INT:
          if (pType.hasShortSpecifier()) {
            return CNumericTypes.SIGNED_INT;
          } else if (pType.hasLongSpecifier()) {
            return CNumericTypes.SIGNED_LONG_LONG_INT;
          } else if (pType.hasLongLongSpecifier()) {
            // fall through, this is already the largest type
          } else {
            // if it had neither specifier it is a plain (unsigned) int
            return CNumericTypes.SIGNED_LONG_INT;
          }
        // $FALL-THROUGH$
        default:
          // just log and do not throw an exception in order to not break things
          pLogManager.logf(Level.WARNING, "Cannot find next larger type for %s", pType);
          return pType;
      }
    }
  }
}
