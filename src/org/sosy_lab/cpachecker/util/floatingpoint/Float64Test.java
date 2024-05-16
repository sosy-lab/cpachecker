// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.kframework.mpfr.BigFloat;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatP.Format;

/** Tests the CFloat interface for 64 bit floating point values. */
@SuppressWarnings("deprecation")
@RunWith(Parameterized.class)
public class Float64Test extends AbstractCFloatTestBase {
  @Override
  protected Format getFloatType() {
    return Format.Float64;
  }

  @Parameters(name = "{0}")
  public static ReferenceImpl[] getReferences() {
    return ReferenceImpl.values();
  }

  @Parameter(0)
  public ReferenceImpl refImpl;

  @Override
  protected ReferenceImpl getRefImpl() {
    return refImpl;
  }

  @Override
  public CFloat toTestedImpl(BigFloat value) {
    return testValueToCFloatImpl(value, getFloatType());
  }

  @Override
  public CFloat toTestedImpl(String repr) {
    return new CFloatImpl(repr, getFloatType());
  }

  @Override
  protected CFloat toTestedImpl(String repr, Map<Integer, Integer> fromStringStats) {
    return new CFloatImpl(repr, getFloatType(), fromStringStats);
  }

  @Override
  public CFloat toReferenceImpl(BigFloat value) {
    return switch (refImpl) {
      case MPFR -> new MpfrFloat(value, getFloatType());
      case JAVA -> new JDouble(value.doubleValue());
      case NATIVE -> new CFloatNative(toPlainString(value), getFloatType());
    };
  }

  @Test
  public void hardExpTest() {
    // Example of a "hard to round" input for the exponential function
    // Taken from "Handbook of Floating-Point Arithmetic", chapter 12
    String val = "7.5417527749959590085206221024712557043923055744016892276704E-10";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Ignore
  @Test
  public void roundingBugExpTest() {
    // Example of a value that is not correctly rounded by either Math.exp() or exp() from math.h
    String val = "-2.920024588250959e-01";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    CFloat r1 = tested.exp();
    CFloat r2 = reference.exp();

    assertEqual1Ulp(r1, r2);
  }

  @Ignore
  @Test
  public void roundingBugPowTest() {
    // This value is not correctly rounded by C, but works in Java
    String val1 = "3.5355339059327379e-01";
    String val2 = "-2.2021710233624257e+00";

    CFloat tested1 = toTestedImpl(val1);
    CFloat tested2 = toTestedImpl(val2);

    CFloat reference1 = toReferenceImpl(val1);
    CFloat reference2 = toReferenceImpl(val2);

    CFloat r1 = tested1.powTo(tested2);
    CFloat r2 = reference1.powTo(reference2);

    assertEqual1Ulp(r1, r2);
  }

  @Test
  public void toStringBugTest() {
    String val = "1.000001";

    CFloat tested = toTestedImpl(val);
    CFloat reference = toReferenceImpl(val);

    String r1 = tested.toString();
    String r2 = reference.toString();

    assertThat(r1).isEqualTo(r2);
  }

  @Test
  public void stringIdentityTest() {
    String val = "-6.4046723829733588e-08";

    CFloat t1 = toTestedImpl(val);
    CFloat t2 = toTestedImpl(t1.toString());

    CFloat r1 = toReferenceImpl(val);
    CFloat r2 = toReferenceImpl(r1.toString());

    assertThat(t1).isEqualTo(r1);
    assertThat(t1).isEqualTo(t2);
    assertThat(r1).isEqualTo(r2);
  }
}
