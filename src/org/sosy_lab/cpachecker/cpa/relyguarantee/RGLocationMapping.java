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

import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ParallelCFAS;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.Path;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

/**
 * Location mapping of thread i partions locations of other threads into equivalence
 * classes. It abstract individual location to set of locations that are seen as equivalent.
 * If every location belongs to a separate class, then the function becomes a vector of program
 * counters. Location mappings are immutable.
 */
public class RGLocationMapping {

  private final ImmutableMap<CFANode, RGLocationClass> locationMapping;
  private final ImmutableSet<RGLocationClass> partitioning;
  /** all inequalities that generated this mapping*/
  private final ImmutableSetMultimap<Path, Pair<CFANode, CFANode>> mismatchesForPath;
  /* Number of classes in the last non-monotonic refinement
  private  int numberOfNMClasses = 2;*/

  /**
   * Return location mapping, where all execution nodes of other threads
   * belong to one class.
   * @param pcfa
   * @param tid
   * @return
   */
  public static RGLocationMapping getEmpty(ParallelCFAS pcfa, int tid) {

    com.google.common.collect.ImmutableSet.Builder<CFANode> bldr = ImmutableSet.<CFANode>builder();


    for (int i=0; i < pcfa.getThreadNo(); i++ ){

      if (i != tid){
        bldr = bldr.addAll(pcfa.getCfa(i).getExecNodes());
      }
    }

    RGLocationClass defClass = new RGLocationClass(bldr.build());
    ImmutableSet<RGLocationClass> part = ImmutableSet.of(defClass);

    return new RGLocationMapping(part, ImmutableSetMultimap.<Path, Pair<CFANode, CFANode>>of());
  }


  public RGLocationMapping(ImmutableSet<RGLocationClass> partitioning, ImmutableSetMultimap<Path, Pair<CFANode, CFANode>> mismatchesForPath){
    this.partitioning =  partitioning;
    this.mismatchesForPath = mismatchesForPath;
    this.locationMapping = paritioningToMap(partitioning);
  }


  private ImmutableMap<CFANode, RGLocationClass> paritioningToMap(
      ImmutableSet<RGLocationClass> part) {

    // if two location classes contain the same node, the builder will throw an exception

    com.google.common.collect.ImmutableMap.Builder<CFANode, RGLocationClass> bldr =
        ImmutableMap.<CFANode, RGLocationClass>builder();

    for (RGLocationClass locClass : part){
      for (CFANode node : locClass.getClassNodes()){
        bldr = bldr.put(node, locClass);
      }
    }

    return bldr.build();
  }


  public boolean containsKey(Object pArg0) {
    return locationMapping.containsKey(pArg0);
  }

  public boolean containsValue(Object pArg0) {
    return locationMapping.containsValue(pArg0);
  }


  public int getClassNo(){
    return partitioning.size();
  }


  @Override
  public String toString(){
    String str = "RGLocationMapping: "+this.partitioning;
    return str;
  }

  @Override
  public int hashCode(){
    return locationMapping.hashCode();
  }

  public boolean equals(RGLocationMapping other){
    return other.partitioning.equals(partitioning);
  }

  public ImmutableMap<CFANode, RGLocationClass> getLocationMapping() {
    return locationMapping;
  }


  public ImmutableSet<RGLocationClass> getParitioning() {
    return partitioning;
  }


  public ImmutableSetMultimap<Path, Pair<CFANode, CFANode>> getMismatchesForPath() {
    return mismatchesForPath;
  }


  public RGLocationClass getLocationClass(CFANode node) {
    return locationMapping.get(node);
  }


  public RGLocationClass findSubsumingLocationMapping(Set<CFANode> nodes) {
    RGLocationClass sub = null;

    for (RGLocationClass partition : partitioning){
      if (partition.getClassNodes().containsAll(nodes)){
        sub = partition;
        break;
      }
    }

    return sub;
  }


}
