/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.reachingdef;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.DefinitionPoint;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.collect.ImmutableSet;



public class ReachingDefinitionStorage implements Serializable {

  private static final long serialVersionUID = 5248630359398001325L;

  private static ReachingDefinitionStorage instance = new ReachingDefinitionStorage();

  private List<Map<String, Set<DefinitionPoint>>> savedReachingDefinitions;
  private Map<Map<String, Set<DefinitionPoint>>, Integer> elementsToSave;
  private int nextId = 0;

  private ReachingDefinitionStorage() {
    GlobalInfo.getInstance().addHelperStorage(this);
  }

  public int saveMap(Map<String, Set<DefinitionPoint>> reachDefs) {
    if (elementsToSave == null) {
      elementsToSave = new HashMap<>();
    }
    if (elementsToSave.containsKey(reachDefs)) { return elementsToSave.get(reachDefs); }

    if (nextId == Integer.MAX_VALUE) { throw new IllegalStateException(
        "More Elements must be stored than IDs are available"); }
    elementsToSave.put(reachDefs, nextId);

    int result = nextId;
    nextId++;

    return result;
  }

  public Map<String, Set<DefinitionPoint>> getMap(int pId) {
    return savedReachingDefinitions.get(pId);
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.writeInt(elementsToSave.size());
    @SuppressWarnings("unchecked")
    Map<String, Set<DefinitionPoint>>[] array = new Map[elementsToSave.size()];
    for (Map<String, Set<DefinitionPoint>> elem : elementsToSave.keySet()) {
      array[elementsToSave.get(elem)] = elem;
    }

    for (Map<String, Set<DefinitionPoint>> elem : array) {
      out.writeInt(elem.size());

      for (String key : elem.keySet()) {
        out.writeObject(key);

        out.writeInt(elem.get(key).size());
        for (DefinitionPoint point : elem.get(key)) {
          out.writeObject(point);
        }
      }
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    int numElem, numKeys, numPoints;
    String key;
    DefinitionPoint[] set;
    Map<String, Set<DefinitionPoint>> map;

    numElem = in.readInt();
    savedReachingDefinitions = new ArrayList<>(numElem);

    for (int i = 0; i < numElem; i++) {
      numKeys = in.readInt();

      map = new HashMap<>(numKeys);
      for (int j = 0; j < numKeys; j++) {
        key = (String) in.readObject();

        numPoints = in.readInt();
        set = new DefinitionPoint[numPoints];
        for (int k = 0; k < numPoints; k++) {
          set[k] = (DefinitionPoint) in.readObject();
        }

        map.put(key, ImmutableSet.copyOf(set));

      }
      savedReachingDefinitions.add(map);
    }
    instance.savedReachingDefinitions = savedReachingDefinitions;
  }

  public static ReachingDefinitionStorage getInstance() {
    return instance;
  }
}
