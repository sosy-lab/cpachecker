// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AdjustablePrecision;

public class SingletonPrecision implements AdjustablePrecision {

  private static final SingletonPrecision mInstance = new SingletonPrecision();

  public static SingletonPrecision getInstance() {
    return mInstance;
  }

  private SingletonPrecision() {}

  @Override
  public String toString() {
    return "no precision";
  }

  @Override
  public AdjustablePrecision add(AdjustablePrecision pOtherPrecision) {
    return this;
  }

  @Override
  public AdjustablePrecision subtract(AdjustablePrecision pOtherPrecision) {
    return this;
  }

  @Override
  public boolean isEmpty() {
    return true;
  }
}
