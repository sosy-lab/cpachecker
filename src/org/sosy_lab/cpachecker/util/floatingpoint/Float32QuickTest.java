// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.floatingpoint;

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
    return Format.Float32;
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
}
