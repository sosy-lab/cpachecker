// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.test;

import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * SMGValue sub class for testing and debugging purposes only! This class enhances SMGValue by label
 * and overrides toString() to make debugging more convenient.
 */
class LabeledValue extends SMGValue {

  private final String label;

  LabeledValue(int pNestingLevel, String pLabel) {
    super(pNestingLevel);
    label = pLabel;
  }

  @Override
  public String toString() {
    return "SMGvalue: " + label;
  }

  @Override
  public boolean equals(Object other) {
    return super.equals(other);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
