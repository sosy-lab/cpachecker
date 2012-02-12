/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;

/**
 * Immutable function that maps locations to their equivalence classes.
 */
public class RGLocationMapping {

  private final ImmutableMap<CFANode, Integer> locationMapping;
  /** Inverse map, lazily initialized */
  private ImmutableMultimap<Integer, CFANode> inverse;

  /**
   * Return location mapping, where all nodes belong to class number 1.
   * @param cfas
   * @return
   */
  public static RGLocationMapping getEmpty(CFA[] cfas){
    Map<CFANode, Integer> map = new HashMap<CFANode, Integer>(100);

    for (int i=0; i<cfas.length; i++){
      Collection<CFANode> nodes = cfas[i].getCFANodes().values();
      for (CFANode node : nodes){
        Integer oldValue = map.put(node, 1);
        if (oldValue != null){
          assert false;
        }
        assert oldValue == null;
      }
    }
    return new RGLocationMapping(map);
  }

  /**
   * Create location mapping from a map.
   * @param map
   * @return
   */
  public static RGLocationMapping copyOf(Map<CFANode, Integer> map){
    return new RGLocationMapping(map);
  }

  private RGLocationMapping(Map<CFANode, Integer> map){
    locationMapping = ImmutableMap.copyOf(map);
  }

  public boolean containsKey(Object pArg0) {
    return locationMapping.containsKey(pArg0);
  }

  public boolean containsValue(Object pArg0) {
    return locationMapping.containsValue(pArg0);
  }

  public Set<java.util.Map.Entry<CFANode, Integer>> entrySet() {
    return locationMapping.entrySet();
  }

  public Integer get(Object pArg0) {
    return locationMapping.get(pArg0);
  }

  public boolean isEmpty() {
    return locationMapping.isEmpty();
  }

  public Set<CFANode> keySet() {
    return locationMapping.keySet();
  }


  public int size() {
    return locationMapping.size();
  }

  public Collection<Integer> values() {
    return locationMapping.values();
  }

  public ImmutableMultimap<Integer, CFANode> inverse(){

    if (inverse == null){
      Builder<Integer, CFANode> bldr = ImmutableMultimap.builder();

      for (Entry<CFANode, Integer> entry : locationMapping.entrySet()){
        bldr.put(entry.getValue(), entry.getKey());
      }

      inverse = bldr.build();
    }

    return inverse;
  }

  public int getClassNo(){
    return inverse().keySet().size();
  }

  public String toString(){
    String str = "RGLocationMapping: "+inverse();
    return str;
  }

  public ImmutableMap<? extends CFANode, ? extends Integer> getMap() {
    return locationMapping;
  }

}
