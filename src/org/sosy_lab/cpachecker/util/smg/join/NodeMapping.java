// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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

  /**
   * Replace the mapping existing from some value to oldTarget with the new target. This is needed
   * to update the mapping and not chain gets.
   */
  public void replaceValueMapping(SMGValue oldTarget, SMGValue newTarget) {
    for (Entry<SMGValue, SMGValue> entry : new ArrayList<>(valueMap.entrySet())) {
      if (entry.getValue().equals(oldTarget)) {
        valueMap.put(entry.getKey(), newTarget);
      }
    }
  }

  /**
   * Replace the mapping existing from some value to oldTarget with the new target. This is needed
   * to update the mapping and not chain gets.
   */
  public void replaceObjectMapping(SMGObject oldTarget, SMGObject newTarget) {
    for (Entry<SMGObject, SMGObject> entry : new ArrayList<>(objectMap.entrySet())) {
      if (entry.getValue().equals(oldTarget)) {
        objectMap.put(entry.getKey(), newTarget);
      }
    }
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

  public Collection<SMGObject> getMappedObjects() {
    return ImmutableSet.copyOf(objectMap.values());
  }

  public Collection<SMGValue> getMappedValues() {
    return ImmutableSet.copyOf(valueMap.values());
  }

  public Map<SMGObject, SMGObject> getObjectMap() {
    return objectMap;
  }

  public Map<SMGValue, SMGValue> getValueMap() {
    return valueMap;
  }

  @Override
  public int hashCode() {
    return Objects.hash(objectMap, valueMap);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof NodeMapping)) {
      return false;
    }
    NodeMapping other = (NodeMapping) obj;
    return Objects.equals(objectMap, other.objectMap) && Objects.equals(valueMap, other.valueMap);
  }
}
