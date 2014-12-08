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
package org.sosy_lab.cpachecker.util;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Multimap;


public class CFANodeClassification {

  public static enum CFANodeType {
    /**
     * Function entry node of the entry function
     */
    ENTRY,

    /**
     * Set of function entry nodes of all functions.
     */
    FUNCTION_ENTRY,

    /**
     * All locations that are possible targets of the analysis.
     */
    TARGET,

    /**
     * Function exit node of the entry function.
     */
    EXIT,

    /**
     * All function exit nodes of all functions and all loop heads of endless loops.
     */
    FUNCTION_SINK,

    /**
     * All function exit nodes of the entry function, and all loop heads of endless loops.
     */
    PROGRAM_SINK;

    public static CFANodeType typeFromString(final String pNodeTypeString) {
      return valueOf(pNodeTypeString);
    }
  }

  private final Multimap<CFANode, CFANodeType> classification;
  private final Multimap<CFANodeType, CFANode> classificationInverse;

  private CFANodeClassification() {
    this.classification = HashMultimap.create();
    this.classificationInverse = HashMultimap.create();
  }

  private void putNodesForType(CFANodeType pType, Set<? extends CFANode> pNodes) {
    for (CFANode u: pNodes) {
      classification.put(u, pType);
      classificationInverse.put(pType, u);
    }
  }

  public ImmutableMultimap<CFANodeType, CFANode> getNodesForTypeMap() {
    return ImmutableMultimap.copyOf(classificationInverse);
  }

  public static CFANodeClassification build(
    final LogManager logger,
    final FunctionEntryNode analysisEntryFunction,
    final TargetLocationProvider tlp,
    final CFA cfa) throws InvalidConfigurationException {

    CFANodeClassification result = new CFANodeClassification();

    logger.log(Level.FINE, "Computing CFA node classification.");

    result.putNodesForType(
        CFANodeType.ENTRY,
        ImmutableSet.of(analysisEntryFunction));

    result.putNodesForType(
        CFANodeType.EXIT,
        ImmutableSet.of(analysisEntryFunction.getExitNode()));

    result.putNodesForType(
        CFANodeType.FUNCTION_ENTRY,
        ImmutableSet.copyOf(cfa.getAllFunctionHeads()));

    result.putNodesForType(
        CFANodeType.FUNCTION_SINK,
        ImmutableSet.<CFANode>builder()
          .addAll(getAllEndlessLoopHeads(cfa.getLoopStructure().get()))
          .addAll(getAllFunctionExitNodes(cfa))
          .build());

    Builder<CFANode> builder = ImmutableSet.<CFANode>builder().addAll(getAllEndlessLoopHeads(cfa.getLoopStructure().get()));
    if (cfa.getAllNodes().contains(analysisEntryFunction.getExitNode())) {
      builder.add(analysisEntryFunction.getExitNode());
    }
    result.putNodesForType(
        CFANodeType.PROGRAM_SINK,
        builder.build());

    result.putNodesForType(
        CFANodeType.TARGET,
        tlp.tryGetAutomatonTargetLocations(analysisEntryFunction));

    return result;
  }

  private static Set<CFANode> getAllFunctionExitNodes(CFA cfa) {
    Set<CFANode> functionExitNodes = new HashSet<>();

    for (FunctionEntryNode node : cfa.getAllFunctionHeads()) {
      FunctionExitNode exitNode = node.getExitNode();
      if (cfa.getAllNodes().contains(exitNode)) {
        functionExitNodes.add(exitNode);
      }
    }
    return functionExitNodes;
  }

  private static Set<CFANode> getAllEndlessLoopHeads(LoopStructure structure) {
    ImmutableCollection<Loop> loops = structure.getAllLoops();
    Set<CFANode> loopHeads = new HashSet<>();

    for (Loop l : loops) {
      if (l.getOutgoingEdges().isEmpty()) {
        // one loopHead per loop should be enough for finding all locations
        for (CFANode head : l.getLoopHeads()) {
          loopHeads.add(head);
        }
      }
    }
    return loopHeads;
  }

  public boolean isClassifiedAs(CFANode pLocationNode, String pQueriedClass) {
    final CFANodeType queried = CFANodeType.typeFromString(pQueriedClass);
    return classificationInverse.get(queried).contains(pLocationNode);
  }

}
