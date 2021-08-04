// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class NodeMapping {

  private final Map<SMGObject, SMGObject> objectMap = new HashMap<>();
  private final Map<SMGValue, SMGValue> valueMap = new HashMap<>();

  public SMGValue getMappedValue(SMGValue value) {
    return valueMap.get(value);
  }

  public SMGObject getMappedObject(SMGObject object) {
    return objectMap.get(object);
  }

  public void addMapping(SMGValue v1, SMGValue v2) {
    valueMap.put(v1, v2);
  }

  public void addMapping(SMGObject o1, SMGObject o2) {
    objectMap.put(o1, o2);
  }

  public boolean mappingExists(SMGObject pMappedObject) {
    return objectMap.containsValue(pMappedObject);
  }

  public boolean hasMapping(SMGValue pValue) {
    return valueMap.containsKey(pValue);
  }

  public boolean hasMapping(SMGObject pObject) {
    return objectMap.containsKey(pObject);
  }

}
