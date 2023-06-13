// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.octagon.coefficients;

import org.sosy_lab.cpachecker.cpa.octagon.OctagonState;

@SuppressWarnings("rawtypes")
public abstract class AOctagonCoefficients implements IOctagonCoefficients {

  protected int size;
  protected OctagonState oct;

  protected AOctagonCoefficients(int size, OctagonState oct) {
    this.size = size;
    this.oct = oct;
  }

  /** {@inheritDoc} */
  @Override
  public int size() {
    return size;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    return prime * result;
  }

  protected abstract IOctagonCoefficients mulInner(IOctagonCoefficients pOct);

  protected abstract IOctagonCoefficients divInner(IOctagonCoefficients pOct);

  @Override
  public final IOctagonCoefficients mul(IOctagonCoefficients other) {
    if (other instanceof OctagonUniversalCoefficients) {
      return OctagonUniversalCoefficients.INSTANCE;
    } else if (other instanceof AOctagonCoefficients) {
      if (hasOnlyOneValue()) {
        return mulInner(other);
      } else if (other.hasOnlyOneValue()) {
        return ((AOctagonCoefficients) other).mulInner(this);
      }
      throw new IllegalArgumentException(
          "At least one of the coefficients has to be a single variable or constant.");
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients");
  }

  @Override
  public final IOctagonCoefficients div(IOctagonCoefficients other) {
    if (other instanceof OctagonUniversalCoefficients) {
      return OctagonUniversalCoefficients.INSTANCE;
    } else if (other instanceof AOctagonCoefficients) {
      if (other.hasOnlyOneValue()) {
        return divInner(other);
      }
      throw new IllegalArgumentException("The divisor has to be a single variable or constant.");
    }
    throw new IllegalArgumentException("Unkown subtype of OctCoefficients");
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }

    if (!(other instanceof AOctagonCoefficients)) {
      return false;
    }

    return true;
  }
}
