// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.type;


/** Singleton class for the special value <code>null</code> in Java. */
public enum JNullValue implements Value {
  INSTANCE;

  /**
   * Returns an instance of a <code>JNullValue</code> object.
   *
   * @return an instance of this object
   */
  public static Value getInstance() {
    return INSTANCE;
  }

  /** Always returns <code>true</code> since <code>null</code> is a specific value. */
  @Override
  public boolean isExplicitlyKnown() {
    return true;
  }

  @Override
  public <T> T accept(ValueVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    return "NULL";
  }
}
