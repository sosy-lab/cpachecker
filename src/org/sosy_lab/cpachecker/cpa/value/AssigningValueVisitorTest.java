// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation.ValueTransferOptions;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue.NegativeNaN;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class AssigningValueVisitorTest {

  private LogManagerWithoutDuplicates logger =
      new LogManagerWithoutDuplicates(LogManager.createTestLogManager());

  private AssigningValueVisitor createVisitor(final MachineModel pMachine)
      throws InvalidConfigurationException {
    ValueAnalysisState valState = new ValueAnalysisState(pMachine);

    return new AssigningValueVisitor(
        ValueAnalysisState.copyOf(valState),
        true,
        ImmutableSet.of(),
        "",
        valState,
        pMachine,
        logger,
        new ValueTransferOptions(TestDataTools.configurationForTest().build()));
  }

  @Test
  public void testCheckingValueInRangeOfType() throws InvalidConfigurationException {
    MachineModel model = MachineModel.LINUX32;
    AssigningValueVisitor visitor = createVisitor(model);

    CSimpleType unsignedChar = CNumericTypes.UNSIGNED_CHAR;

    assertThat(visitor.isValueInRangeOfType(unsignedChar, new NumericValue(Integer.valueOf(-1))))
        .isFalse();
    assertThat(visitor.isValueInRangeOfType(unsignedChar, new NumericValue(Long.MAX_VALUE)))
        .isFalse();
    assertThat(visitor.isValueInRangeOfType(unsignedChar, new NumericValue(Integer.valueOf(0))))
        .isTrue();
    assertThat(visitor.isValueInRangeOfType(unsignedChar, new NumericValue(Integer.valueOf(255))))
        .isTrue();

    CSimpleType signedLong = CNumericTypes.SIGNED_LONG_INT;
    assertThat(visitor.isValueInRangeOfType(signedLong, new NumericValue(Integer.MIN_VALUE)))
        .isTrue();
    assertThat(visitor.isValueInRangeOfType(signedLong, new NumericValue(Integer.valueOf(1))))
        .isTrue();

    assertThat(visitor.isValueInRangeOfType(unsignedChar, BooleanValue.FALSE_VALUE)).isTrue();
    assertThat(visitor.isValueInRangeOfType(unsignedChar, new NumericValue(Rational.NEG_ONE)))
        .isTrue();
    assertThat(
            visitor.isValueInRangeOfType(CNumericTypes.FLOAT, new NumericValue(Integer.valueOf(2))))
        .isTrue();
    assertThat(
            visitor.isValueInRangeOfType(new CProblemType(""), new NumericValue(Integer.MAX_VALUE)))
        .isTrue();
    assertThat(visitor.isValueInRangeOfType(new CProblemType(""), NullValue.getInstance()))
        .isTrue();
  }

  @Test
  public void testInvertCastFromInteger() throws InvalidConfigurationException {
    MachineModel model = MachineModel.LINUX64;
    AssigningValueVisitor visitor = createVisitor(model);

    CSimpleType unsignedChar = CNumericTypes.UNSIGNED_CHAR;
    CSimpleType unsignedInt = CNumericTypes.UNSIGNED_INT;
    CSimpleType signedInt = CNumericTypes.SIGNED_INT;

    assertThat(
            visitor.invertCastFromInteger(
                unsignedChar, unsignedInt, new NumericValue(Double.valueOf(0)), false))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(
            visitor.invertCastFromInteger(
                unsignedChar, unsignedInt, new NumericValue(Float.valueOf(0)), false))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(
            visitor.invertCastFromInteger(
                unsignedChar, unsignedInt, new NumericValue(BigDecimal.ONE), false))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(
            visitor.invertCastFromInteger(
                unsignedChar, unsignedInt, new NumericValue(NegativeNaN.VALUE), false))
        .isEqualTo(UnknownValue.getInstance());

    assertThat(
            visitor.invertCastFromInteger(
                unsignedChar, CNumericTypes.FLOAT, new NumericValue(Float.valueOf(3.0f)), false))
        .isEqualTo(UnknownValue.getInstance());

    NumericValue val1 = new NumericValue(Integer.valueOf(255));
    assertThat(visitor.invertCastFromInteger(unsignedChar, unsignedInt, val1, false))
        .isEqualTo(val1);
    assertThat(visitor.invertCastFromInteger(unsignedChar, signedInt, val1, false)).isEqualTo(val1);
    assertThat(visitor.invertCastFromInteger(unsignedChar, CNumericTypes.FLOAT, val1, false))
        .isEqualTo(val1);

    assertThat(
            visitor.invertCastFromInteger(
                unsignedChar, unsignedInt, new NumericValue(NegativeNaN.VALUE), false))
        .isEqualTo(UnknownValue.getInstance());

    assertThat(
            visitor.invertCastFromInteger(
                unsignedInt, unsignedChar, new NumericValue(Integer.valueOf(12)), false))
        .isEqualTo(UnknownValue.getInstance());

    assertThat(
            visitor.invertCastFromInteger(
                unsignedChar, CNumericTypes.FLOAT, new NumericValue(Integer.valueOf(270)), false))
        .isEqualTo(UnknownValue.getInstance());

    assertThat(
            visitor.invertCastFromInteger(
                unsignedChar, CNumericTypes.FLOAT, new NumericValue(Integer.valueOf(-270)), false))
        .isEqualTo(UnknownValue.getInstance());

    assertThat(
            visitor.invertCastFromInteger(
                unsignedChar, signedInt, new NumericValue(Integer.valueOf(-1)), false))
        .isEqualTo(UnknownValue.getInstance());

    assertThat(
            visitor.invertCastFromInteger(
                CNumericTypes.SIGNED_CHAR,
                signedInt,
                new NumericValue(Integer.valueOf(-140)),
                false))
        .isEqualTo(UnknownValue.getInstance());

    assertThat(
            visitor.invertCastFromInteger(
                unsignedChar, signedInt, new NumericValue(Integer.valueOf(270)), false))
        .isEqualTo(UnknownValue.getInstance());

    assertThat(
            visitor.invertCastFromInteger(
                unsignedInt, signedInt, new NumericValue(BigInteger.valueOf(-3)), false))
        .isEqualTo(new NumericValue(new BigInteger("4294967293")));

    assertThat(
            visitor.invertCastFromInteger(
                unsignedInt, signedInt, new NumericValue(Integer.valueOf(-20)), false))
        .isEqualTo(new NumericValue(new BigInteger("4294967276")));

    assertThat(
            visitor.invertCastFromInteger(
                signedInt, unsignedInt, new NumericValue(Long.valueOf(4294967294L)), true))
        .isEqualTo(new NumericValue(BigInteger.valueOf(-2)));
    assertThat(
            visitor.invertCastFromInteger(
                CNumericTypes.SIGNED_CHAR,
                unsignedInt,
                new NumericValue(BigInteger.valueOf(4294967293L)),
                true))
        .isEqualTo(new NumericValue(BigInteger.valueOf(-3)));

    assertThat(
            visitor.invertCastFromInteger(
                CNumericTypes.SIGNED_CHAR,
                unsignedInt,
                new NumericValue(Long.valueOf(4294967167L)),
                true))
        .isEqualTo(UnknownValue.getInstance());
  }

  @Test
  public void testInvertCast() throws InvalidConfigurationException {
    MachineModel model = MachineModel.LINUX64;
    AssigningValueVisitor visitor = createVisitor(model);

    CSimpleType unsignedChar = CNumericTypes.UNSIGNED_CHAR;
    CSimpleType unsignedInt = CNumericTypes.UNSIGNED_INT;
    CSimpleType signedInt = CNumericTypes.SIGNED_INT;

    NumericValue val1 = new NumericValue(Integer.valueOf(257));
    // same canoncial type
    assertThat(
            visitor.invertCast(new CTypedefType(false, false, "myInt", signedInt), signedInt, val1))
        .isEqualTo(val1);

    // not both CSimple Types
    assertThat(visitor.invertCast(new CProblemType("test"), signedInt, val1)).isEqualTo(val1);
    assertThat(visitor.invertCast(signedInt, new CProblemType("test"), val1)).isEqualTo(val1);

    // both floats
    NumericValue val2 = new NumericValue(Float.valueOf(255));
    assertThat(visitor.invertCast(CNumericTypes.FLOAT, CNumericTypes.FLOAT, val2)).isEqualTo(val2);
    assertThat(visitor.invertCast(CNumericTypes.FLOAT, CNumericTypes.DOUBLE, val2))
        .isEqualTo(UnknownValue.getInstance());

    // test undo cast from integer to float
    // no equivalent values for integer
    assertThat(visitor.invertCast(signedInt, CNumericTypes.DOUBLE, new NumericValue(Double.NaN)))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(
            visitor.invertCast(
                signedInt, CNumericTypes.DOUBLE, new NumericValue(NegativeNaN.VALUE)))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(
            visitor.invertCast(
                signedInt, CNumericTypes.DOUBLE, new NumericValue(Double.POSITIVE_INFINITY)))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(
            visitor.invertCast(
                signedInt, CNumericTypes.DOUBLE, new NumericValue(Double.NEGATIVE_INFINITY)))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(visitor.invertCast(signedInt, CNumericTypes.FLOAT, new NumericValue(Float.NaN)))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(
            visitor.invertCast(
                signedInt, CNumericTypes.FLOAT, new NumericValue(Float.POSITIVE_INFINITY)))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(
            visitor.invertCast(
                signedInt, CNumericTypes.FLOAT, new NumericValue(Float.NEGATIVE_INFINITY)))
        .isEqualTo(UnknownValue.getInstance());

    // float representable as integer value
    assertThat(visitor.invertCast(signedInt, CNumericTypes.FLOAT, new NumericValue(Rational.ONE)))
        .isEqualTo(new NumericValue(BigInteger.ONE));
    assertThat(
            visitor.invertCast(
                signedInt, CNumericTypes.DOUBLE, new NumericValue(Double.valueOf(3.0))))
        .isEqualTo(new NumericValue(BigInteger.valueOf(3)));

    // float not representable as integer value
    assertThat(
            visitor.invertCast(
                signedInt, CNumericTypes.DOUBLE, new NumericValue(Rational.ofLongs(3, 5))))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(
            visitor.invertCast(
                signedInt, CNumericTypes.FLOAT, new NumericValue(Float.valueOf(2.3f))))
        .isEqualTo(UnknownValue.getInstance());

    // both integer types
    assertThat(visitor.invertCast(signedInt, unsignedInt, val1)).isEqualTo(val1);
    assertThat(visitor.invertCast(unsignedChar, unsignedInt, val1))
        .isEqualTo(UnknownValue.getInstance());
    assertThat(visitor.invertCast(unsignedChar, unsignedInt, NullValue.getInstance()))
        .isEqualTo(UnknownValue.getInstance());
  }
}
