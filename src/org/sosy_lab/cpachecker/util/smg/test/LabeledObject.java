// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.test;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/**
 * SMGObject sub class for testing and debugging purposes only! This class enhances SMGObject by
 * label and overrides toString() to make debugging more convenient.
 */
class LabeledObject extends SMGObject {

  private final String label;

  LabeledObject(int pNestingLevel, BigInteger pSize, BigInteger pOffset, String pLabel) {
    super(pNestingLevel, pSize, pOffset);
    label = pLabel;
  }

  @Override
  public String toString() {
    return "LabeledObject{ " + label + " }";
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
