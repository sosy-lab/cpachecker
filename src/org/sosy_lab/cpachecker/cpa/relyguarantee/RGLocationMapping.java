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
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.Path;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.ImmutableSetMultimap;

/**
 * Immutable function that maps locations to their equivalence classes.
 */
public class RGLocationMapping {

  private final ImmutableMap<CFANode, Integer> locationMapping;
  /** Inverse map */
  private final ImmutableMultimap<Integer, CFANode> inverseMapping;
  /** all inequalities that generated this mapping*/
  private final ImmutableSetMultimap<Path, Pair<CFANode, CFANode>> mismatchesForPath;
  /* Number of classes in the last non-monotonic refinement
  private  int numberOfNMClasses = 2;*/

  /**
   * Return location mapping, where all execution nodes of other threads
   * belong to the deafult class (1).
   * @param pcfa
   * @param tid
   * @return
   */
  public static RGLocationMapping getEmpty(ParallelCFAS pcfa, int tid) {

    com.google.common.collect.ImmutableMap.Builder<CFANode, Integer> bldr =
        ImmutableMap.<CFANode, Integer>builder();

    for (int i=0; i < pcfa.getThreadNo(); i++ ){

      if (i != tid){
        Set<CFANode> nodes = pcfa.getCfa(i).getExecNodes();
        for (CFANode node : nodes){
          bldr = bldr.put(node, 1);
        }
      }
    }

    return new RGLocationMapping(bldr.build(), ImmutableSetMultimap.<Path, Pair<CFANode, CFANode>>of());
  }


  /**
   * Returns location mapping where all nodes that are not global declarations are mapped to their own
   * class.
   * @param pcfa
   * @return
   */
  public static RGLocationMapping getIndentity(ParallelCFAS pcfa, int tid){
    assert false : "Not implemented yet";
    return null;
  }


  public RGLocationMapping(ImmutableMap<CFANode, Integer> map, ImmutableSetMultimap<Path, Pair<CFANode, CFANode>> mismatchesForPath){
    this.locationMapping = map;
    this.mismatchesForPath = mismatchesForPath;
    this.inverseMapping = inverse(this.locationMapping);
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


  private ImmutableMultimap<Integer, CFANode> inverse(ImmutableMap<CFANode, Integer> map){

    Builder<Integer, CFANode> bldr = ImmutableMultimap.builder();

    for (Entry<CFANode, Integer> entry : map.entrySet()){
      bldr.put(entry.getValue(), entry.getKey());
    }

    return bldr.build();
  }

  public int getClassNo(){
    return inverseMapping.keySet().size();
  }

  public Collection<CFANode> classToNodes(Integer classNo) {
    return this.inverseMapping.get(classNo);
  }


  @Override
  public String toString(){
    String str = "RGLocationMapping: "+inverseMapping;
    return str;
  }

  @Override
  public int hashCode(){
    return locationMapping.hashCode();
  }


  public ImmutableMap<CFANode, Integer> getLocationMapping() {
    return locationMapping;
  }


  public ImmutableMultimap<Integer, CFANode> getInverseMapping() {
    return inverseMapping;
  }


  public ImmutableSetMultimap<Path, Pair<CFANode, CFANode>> getMismatchesForPath() {
    return mismatchesForPath;
  }


}
