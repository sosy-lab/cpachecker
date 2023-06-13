// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGNodeMapping {
  private final Map<SMGObject, SMGObject> object_map = new HashMap<>();
  private final Map<SMGValue, SMGValue> value_map = new HashMap<>();

  @Override
  public int hashCode() {
    return Objects.hash(object_map, value_map);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SMGNodeMapping)) {
      return false;
    }
    SMGNodeMapping other = (SMGNodeMapping) obj;
    return Objects.equals(object_map, other.object_map)
        && Objects.equals(value_map, other.value_map);
  }

  public SMGNodeMapping() {}

  public SMGNodeMapping(SMGNodeMapping origin) {
    object_map.putAll(origin.object_map);
    value_map.putAll(origin.value_map);
  }

  public SMGValue get(SMGValue i) {
    return value_map.get(i);
  }

  public SMGObject get(SMGObject o) {
    return object_map.get(o);
  }

  public void map(SMGObject key, SMGObject value) {
    object_map.put(key, value);
  }

  public void map(SMGValue key, SMGValue value) {
    value_map.put(key, value);
  }

  public void removeValue(SMGValue value) {

    for (Entry<SMGValue, SMGValue> entry : value_map.entrySet()) {
      if (entry.getValue().equals(value)) {
        value_map.remove(entry.getKey());
        return;
      }
    }
  }

  public void removeValue(SMGObject value) {
    for (Entry<SMGObject, SMGObject> entry : object_map.entrySet()) {
      if (entry.getValue().equals(value)) {
        object_map.remove(entry.getKey());
        return;
      }
    }
  }

  public boolean containsKey(SMGValue key) {
    return value_map.containsKey(key);
  }

  public boolean containsKey(SMGObject key) {
    return object_map.containsKey(key);
  }

  public boolean containsValue(SMGObject value) {
    return object_map.containsValue(value);
  }

  public Set<Entry<SMGObject, SMGObject>> getObject_mapEntrySet() {
    return object_map.entrySet();
  }

  public Set<Entry<SMGValue, SMGValue>> getValue_mapEntrySet() {
    return value_map.entrySet();
  }

  @Override
  public String toString() {
    return "Objects:\n" + object_map + "\nValues:\n" + value_map;
  }
}
