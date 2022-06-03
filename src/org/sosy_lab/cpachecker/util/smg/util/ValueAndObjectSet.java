// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import java.util.Set;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/*
 * Sometimes we need to return a SMGValue and a Set of SMGObjects.
 */
public class ValueAndObjectSet {

  private final Set<SMGObject> objectSet;
  private final SMGValue value;

  public ValueAndObjectSet(Set<SMGObject> pObjectSet, SMGValue pValue) {
    objectSet = pObjectSet;
    value = pValue;
  }

  public SMGValue getValue() {
    return value;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (!(other instanceof ValueAndObjectSet)) {
      return false;
    }
    ValueAndObjectSet otherSMGaV = (ValueAndObjectSet) other;
    return getObjectSet().equals(otherSMGaV.getObjectSet()) && value.equals(otherSMGaV.getValue());
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public Set<SMGObject> getObjectSet() {
    return objectSet;
  }
}
