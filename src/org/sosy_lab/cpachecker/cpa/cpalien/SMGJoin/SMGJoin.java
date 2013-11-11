/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien.SMGJoin;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cpa.cpalien.SMGObject;

enum SMGJoinStatus {
  EQUAL,
  LEFT_ENTAIL,
  RIGHT_ENTAIL,
  INCOMPARABLE,
  INCOMPLETE,
}

public class SMGJoin {

}

class SMGUpdateJoinStatus {
  public static SMGJoinStatus updateStatus(SMGJoinStatus pStatus1, SMGJoinStatus pStatus2) {
    if (pStatus1 == SMGJoinStatus.EQUAL) {
      return pStatus2;
    } else if (pStatus2 == SMGJoinStatus.EQUAL) {
      return pStatus1;
    } else if (pStatus1 == SMGJoinStatus.INCOMPARABLE ||
               pStatus2 == SMGJoinStatus.INCOMPARABLE ||
               pStatus1 != pStatus2) {
      return SMGJoinStatus.INCOMPARABLE;
    }
    return pStatus1;
  }
}

class SMGNodeMapping {
  final private Map<SMGObject, SMGObject> object_map = new HashMap<>();
  final private Map<Integer, Integer> value_map = new HashMap<>();

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((object_map == null) ? 0 : object_map.hashCode());
    result = prime * result + ((value_map == null) ? 0 : value_map.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SMGNodeMapping other = (SMGNodeMapping) obj;
    if (object_map == null) {
      if (other.object_map != null) {
        return false;
      }
    } else if (!object_map.equals(other.object_map)) {
      return false;
    }
    if (value_map == null) {
      if (other.value_map != null) {
        return false;
      }
    } else if (!value_map.equals(other.value_map)) {
      return false;
    }
    return true;
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

  public void map(SMGObject key, SMGObject value){
    object_map.put(key, value);
  }

  public void map(Integer key, Integer value) {
    value_map.put(key, value);
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
}
