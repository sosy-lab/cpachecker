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
package org.sosy_lab.cpachecker.util.ci;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;


public class AppliedCustomInstructionParser {

  private final ShutdownNotifier notifier;

  public AppliedCustomInstructionParser(final ShutdownNotifier pShN) {
    notifier = pShN;
  }

  /**
   * Creates a ImmutableMap if the file contains all required data, null if not
   * @param file Path of the file to be read
   * @return ImmutableMap containing a startNode (key) and a set of endNodes (value).
   * @throws IOException if the file doesn't contain all required data.
   * @throws AppliedCustomInstructionParsingFailedException
   * @throws InterruptedException
   */
  public ImmutableMap<CFANode, AppliedCustomInstruction> parse (Path file)
      throws IOException, AppliedCustomInstructionParsingFailedException, InterruptedException {

    Builder<CFANode, AppliedCustomInstruction> map = new ImmutableMap.Builder<>();
    CFAInfo cfaInfo = GlobalInfo.getInstance().getCFAInfo().get();

    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile()), "UTF-8"))) {

      String line = "";
      CFANode firstNode;
      ImmutableSet<CFANode> secNodes;

      while(br.ready()) {
        notifier.shutdownIfNecessary();

        if ((line = br.readLine())==null) {
          break;
        }
        String firstLine = line.trim();

        if ((line = br.readLine())==null){
          throw new AppliedCustomInstructionParsingFailedException("Wrong format, specification of end nodes not found. Expect that a custom instruction is specified in two lines. First line contains the start node and second line the end nodes.");
        }
        String[] secLine = line.trim().split("(\\w)+");

        firstNode = getCFANode(firstLine, cfaInfo);
        secNodes = getCFANodes(secLine, cfaInfo);

        if (sanityCheckCIApplication(firstNode, secNodes)) {
          map.put(firstNode, new AppliedCustomInstruction(firstNode, secNodes));
        }
      }
    }

    return map.build();
  }


  /**
   * Creates a new CFANode with respect to the given parameters
   * @param pNodeID String
   * @param cfaInfo CFAInfo
   * @return a new CFANode with respect to the given parameters
   * @throws AppliedCustomInstructionParsingFailedException if the node can't be created
   */
  public CFANode getCFANode (String pNodeID, CFAInfo cfaInfo) throws AppliedCustomInstructionParsingFailedException{
    try{
      return cfaInfo.getNodeByNodeNumber(Integer.parseInt(pNodeID));
    } catch (NumberFormatException ex) {
      throw new AppliedCustomInstructionParsingFailedException
        ("It is not possible to parse " + pNodeID + " to an integer!", ex);
    }
  }

  /**
   * Creates a ImmutableSet out of the given String[].
   * @param pNodes String[]
   * @return Immutable Set of CFANodes out of the String[]
   * @throws AppliedCustomInstructionParsingFailedException
   */
  public ImmutableSet<CFANode> getCFANodes (String[] pNodes, CFAInfo cfaInfo) throws AppliedCustomInstructionParsingFailedException {
    ImmutableSet.Builder<CFANode> builder = new ImmutableSet.Builder<>();
    for (int i=0; i<pNodes.length; i++) {
      builder.add(getCFANode(pNodes[i], cfaInfo));
    }
    return builder.build();
  }

  /**
   * Checks if all nodes out of the given set can be reached from the given pNode.
   * @param pNode CFANode
   * @param pSet Set of CFANodes
   * @return true if all nodes out of the given set can be reached from the given pNode, false if not.
   * @throws AppliedCustomInstructionParsingFailedException if the given node or set is null.
   * @throws InterruptedException
   */
  private boolean sanityCheckCIApplication (CFANode pNode, Set<CFANode> pSet)
        throws InterruptedException {

    Set<CFANode> endNodes = new HashSet<>();
    Set<CFANode> visitedNodes = new HashSet<>();
    Queue<CFANode> queue = new ArrayDeque<>();

    queue.add(pNode);
    visitedNodes.add(pNode);

    CFANode pred;

    while (!queue.isEmpty()) {
      notifier.shutdownIfNecessary();
      pred = queue.poll();

      // If tmp is endNode and in pSet => save that tmp is in pSet.
      // At the end of the method we compare the given pSet and the set of endNodes we visited,
      // to decide if all nodes of pSet are contained in the graph of pNode.
      if (pSet.contains(pred)) {
        endNodes.add(pred);
        continue;
      }

      // breadth-first-search
      for (CFANode succ : CFAUtils.successorsOf(pred)) {
        if (!visitedNodes.contains(succ)){
          queue.add(succ);
          visitedNodes.add(succ);
        }
      }
    }

    // if the set endNode is equal to pSet => all nodes of pSet are endNodes
    return pSet.equals(endNodes);
  }

}