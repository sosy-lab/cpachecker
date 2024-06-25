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
 * Tests the CFloat interface for 80 bit "extended precision" floating point values.
 *
 * <p>Only uses MPFR and the C implementation as reference as extended precision is not supported by
 * the JVM.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Extended_precision">Extended Precision on
 *     Wikipedia</a>
 */
@RunWith(Parameterized.class)
public class FloatExtendedTest extends AbstractCFloatTestBase {
  @Override
  Format getFloatType() {
    return Format.Extended;
  }

  @Parameters(name = "{0}")
  public static ReferenceImpl[] getReferences() {
    return new ReferenceImpl[] {ReferenceImpl.MPFR, ReferenceImpl.NATIVE};
  }

  @Parameter(0)
  public ReferenceImpl refImpl;

  @Override
  protected ReferenceImpl getRefImpl() {
    return refImpl;
  }
}
