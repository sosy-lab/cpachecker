// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import java.util.Set;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGObjectsAndValues {

  private final Set<SMGObject> objects;
  private final Set<SMGValue> values;

  public SMGObjectsAndValues(Set<SMGObject> pObjects, Set<SMGValue> pValues) {
    objects = pObjects;
    values = pValues;
  }

  public Set<SMGObject> getObjects() {
    return objects;
  }

  public Set<SMGValue> getValues() {
    return values;
  }
}
