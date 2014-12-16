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
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.sosy_lab.common.io.Path;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public class AppliedCustomInstructionParser {

  /**
   * Creates a ImmutableMap if the file contains all reqiered data, null if not
   * @param file Path of the file to be read
   * @return ImmutableMap containing a startNode (key) and a set of endNodes (value).
   * @throws IOException if the file doesn't contain all required data.
   * @throws AppliedCustomInstructionParsingFailedException
   */
  public ImmutableMap<CFANode, AppliedCustomInstruction> parse (Path file)
      throws IOException, AppliedCustomInstructionParsingFailedException {

    // TODO wann parsingFailedException werfen?

    BufferedReader br = new BufferedReader(new FileReader(file.toFile()));
    String firstLine = br.readLine().trim();
    String[] secLine = br.readLine().trim().split(" ");
    br.close();

    CFANode firstNode = getCFANode(firstLine, GlobalInfo.getInstance().getCFAInfo().get());
    ImmutableSet<CFANode> secNodes = getCFANodes(secLine);

    //TODO immutableSet mit nur einem Element?
    if (sanityCheckCIApplication(firstNode, secNodes)) {
      return new ImmutableMap.Builder<CFANode, AppliedCustomInstruction>()
            .put(firstNode, new AppliedCustomInstruction(firstNode, secNodes))
            .build();
    }

    return null;
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
  public ImmutableSet<CFANode> getCFANodes (String[] pNodes) throws AppliedCustomInstructionParsingFailedException {
    ImmutableSet.Builder<CFANode> builder = new ImmutableSet.Builder<>();
    for (int i=0; i<pNodes.length; i++) {
      builder.add(getCFANode(pNodes[i], GlobalInfo.getInstance().getCFAInfo().get()));
    }
    return builder.build();
  }

  /**
   * Checks if all nodes out of the given set can be reached from the given pNode.
   * @param pNode CFANode
   * @param pSet Set of CFANodes
   * @return true if all nodes out of the given set can be reached from the given pNode, false if not.
   * @throws AppliedCustomInstructionParsingFailedException if the given node or set is null.
   */
  private boolean sanityCheckCIApplication (CFANode pNode, Set<CFANode> pSet)
        throws AppliedCustomInstructionParsingFailedException {

    if (pNode == null || pSet == null) {
      throw new AppliedCustomInstructionParsingFailedException
        ("The CFANode "+pNode+" is null, that is no valid value for sanityCheckCIApplication");
    }

    CFANode currentNode = pNode;
    Set<CFANode> endNodes = new HashSet<>();
    Set<CFANode> visitedNodes = new HashSet<>();
    Stack<Tupel> stack = new Stack<>();
    int i = 0;
    int visitedCurrenNodeNum = 0;

    while (visitedCurrenNodeNum != pNode.getNumLeavingEdges()) {
      CFANode tmp = currentNode.getLeavingEdge(i).getSuccessor();
      visitedNodes.add(tmp);

      // If tmp is endNode and in pSet => save that tmp is in pSet.
      // At the end of the method we compare the given pSet and the set of endNodes we visited,
      // to decide if all nodes of pSet are contained in the graph of pNode.
      if (tmp.getNumLeavingEdges() == 0 && pSet.contains(tmp)) {
        endNodes.add(tmp);
      }

      // If tmp is not an endNode => we want to go to the child node i.
      // If we want to go back from the child node to the parent node, we have to know which node
      // of the (perhaps more than one) possible parent nodes is the one we want to visit again.
      else if (tmp.getNumLeavingEdges()>0) {
        stack.push(new Tupel(currentNode, i));
        currentNode = tmp;
        i=0;
      }

      // all leaving edges are visited => we want to go to the parent node
      // and visit all the other child nodes of it
      else if (i == currentNode.getNumLeavingEdges()) {
        Tupel t = stack.pop();
        currentNode = t.getCFANode();
        i = t.getI();
      }

      // visit the next child node
      i++;
    }

    // if the set endNode is equal to pSet => all nodes of pSet are endNodes
    return pSet.equals(endNodes);
  }

}

class Tupel {

  private final CFANode cfa;
  private final int i;

  public Tupel(CFANode pCfa, int pI) {
    cfa = pCfa;
    i = pI;
  }

  public CFANode getCFANode() {
    return cfa;
  }

  public int getI() {
    return i;
  }
}