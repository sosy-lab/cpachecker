/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.join;

import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class SMGNodeMapping {
  private final Map<SMGObject, SMGObject> object_map = new HashMap<>();
  private final Map<Integer, Integer> value_map = new HashMap<>();

  @Override
  public int hashCode() {
    return Objects.hash(object_map, value_map);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof SMGNodeMapping)) {
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

  public Integer get(Integer i) {
    return value_map.get(i);
  }

  public SMGObject get (SMGObject o) {
    return object_map.get(o);
  }

  public void map(SMGObject key, SMGObject value) {
    object_map.put(key, value);
  }

  public void map(Integer key, Integer value) {
    value_map.put(key, value);
  }

  public void removeValue(Integer value) {

    for (Entry<Integer, Integer> entry : value_map.entrySet()) {
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

  public boolean containsKey(Integer key) {
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

  public Set<Entry<Integer, Integer>> getValue_mapEntrySet() {
    return value_map.entrySet();
  }
}