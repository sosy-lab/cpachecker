// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue.Format;

/**
 * Tests the CFloat interface for 32 bit floating point values.
 *
 * <p>Unlike {@link Float32Test} this test class will only run basic regression tests on a much
 * smaller number of test values.
 */
@RunWith(Parameterized.class)
public class Float32QuickTest extends AbstractCFloatTestBase {
  @Override
  Format getFloatType() {
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
  int getNumberOfTests() {
    return 100;
  }

  @Test
  public void remTest() {
    double d0 = 1.9989e-02; // -1.7976931e+4f;
    double d1 = 6.1035e-05; // 1.8371173f;

    FloatValue r0 = FloatValue.fromString(Format.Float16, String.valueOf(d0));
    FloatValue r1 = FloatValue.fromString(Format.Float16, String.valueOf(d1));

    FloatValue r = FloatValue.fromString(Format.Float16, String.valueOf(-3.0518e-05));

    assertThat(r0.remainder(r1)).isEqualTo(r);
  }
}
