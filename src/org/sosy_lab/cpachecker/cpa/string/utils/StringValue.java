// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.utils;

public class StringValue {
private boolean unknown;

public boolean isUnknown() {
  return false;
}

public static final class UnknownStringValue extends StringValue {

  private static final UnknownStringValue instance = new UnknownStringValue();

  @Override
  public String toString() {
    return "UNKNOWN";
  }

  public static UnknownStringValue getInstance() {
    return instance;
  }

  public boolean isUnkown() {
    return true;
  }
}
}
