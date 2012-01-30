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
package org.sosy_lab.cpachecker.util.blocking;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.blocking.interfaces.BlockComputer;

@Options(prefix="blockreducer")
public class BlockedCFAReducer implements BlockComputer {

  @Option(description="Do at most n summarizations on a node.")
  private int reductionThreshold = 100;

  @Option(description="Allow reduction of loop heads; calculate abstractions alwasy at loop heads?")
  private boolean allowReduceLoopHeads = false;

  @Option(description="Allow reduction of function entries; calculate abstractions alwasy at function entries?")
  private boolean allowReduceFunctionEntries = true;

  @Option(description="Allow reduction of function exits; calculate abstractions alwasy at function exits?")
  private boolean allowReduceFunctionExits = true;

  @Option(name="reducedCfaFile", description="write the reduced cfa to the specified file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File reducedCfaFile = new File("ReducedCfa.rsf");

  private final Deque<CFAFunctionDefinitionNode> inliningStack;

  public BlockedCFAReducer(Configuration pConfig) throws InvalidConfigurationException {
    if (pConfig != null) {
      pConfig.inject(this);
    }

    this.inliningStack = new ArrayDeque<CFAFunctionDefinitionNode>();
  }

  private boolean isAbstractionNode(ReducedNode pNode) {
    return (pNode.isLoopHead() && !allowReduceLoopHeads)
        || (pNode.isFunctionEntry() && !allowReduceFunctionEntries)
        || (pNode.isFunctionExit() && !allowReduceFunctionExits);
  }

  /**
   * Increment the number of summarizations that are done
   * with pNode as the rood-node.
   */
  private void incSummarizationsOnNode(ReducedNode pNode, int pIncBy) {
    assert(reductionThreshold > 0);
    pNode.incSummarizations(pIncBy);
  }

  public int getSummarizationsOnNode(ReducedNode pNode) {
    return pNode.getSummarizations();
  }

  protected boolean applySequenceRule(ReducedFunction pApplyTo) {
    boolean result = false;
    // TODO: ensure that this function is not applied across the scope of a loop.

    Queue<ReducedNode> toTraverse = new ArrayDeque<ReducedNode>();
    Set<ReducedEdge> traverseDone = new HashSet<ReducedEdge>();

    toTraverse.add(pApplyTo.getEntryNode());
    while (toTraverse.size() > 0) {
      ReducedNode u = toTraverse.remove();

      for (ReducedEdge e: pApplyTo.getLeavingEdges(u)) {
        ReducedNode v = e.getPointsTo();

        if (!traverseDone.add(e)) {
          continue;
        }

        if (u == v) {
          continue;
        }

        ReducedEdge[] vLeavingEdges = pApplyTo.getLeavingEdges(v);
        if (vLeavingEdges.length == 0) {
          continue;
        }

        if (getSummarizationsOnNode(u) + vLeavingEdges.length > reductionThreshold) {
          toTraverse.add(v);
          continue;
        }

        if (pApplyTo.getNumEnteringEdges(v) != 1) {
          toTraverse.add(v);
          continue;
        }

        if (isAbstractionNode(v)) {
          toTraverse.add(v);
          continue;
        }

        boolean uvRemoved = false;
        for (ReducedEdge f: vLeavingEdges) {
          ReducedNode w = f.getPointsTo();

          assert(u != v);
          assert(v != w);

          ReducedEdge sumEdge = new ReducedEdge(w);
          sumEdge.addEdge(e);
          sumEdge.addEdge(f);

          pApplyTo.removeEdge(v, w, f);
          if (!uvRemoved) {
            pApplyTo.removeEdge(u, v, e);
            this.incSummarizationsOnNode(u, v.getSummarizations());
            uvRemoved = true;
          }
          pApplyTo.addEdge(u, w, sumEdge);

          this.incSummarizationsOnNode(u, 1);
        }

        toTraverse.clear();
        traverseDone.clear();
        toTraverse.add(u);
        result = true;
      }
    }

    return result;
  }

  protected boolean applyChoiceRule(ReducedFunction pApplyTo) {
    boolean result = false;

    Deque<ReducedNode> toTraverse = new ArrayDeque<ReducedNode>();
    Set<ReducedNode> traverseDone = new HashSet<ReducedNode>();

    toTraverse.add(pApplyTo.getEntryNode());
    while (toTraverse.size() > 0) {
      ReducedNode u = toTraverse.removeFirst();
      if (!traverseDone.add(u)) {
        continue;
      }

      ReducedEdge[] leavingEdges = pApplyTo.getLeavingEdges(u);
      if (leavingEdges.length != 2 || getSummarizationsOnNode(u) >= reductionThreshold) {
        for (ReducedEdge e: leavingEdges) {
          toTraverse.add(e.getPointsTo());
        }
        continue;
      }

      ReducedNode v1 = leavingEdges[0].getPointsTo();
      ReducedNode v2 = leavingEdges[1].getPointsTo();

      if(v1 != v2) {
        toTraverse.add(v1);
        toTraverse.add(v2);
        continue;
      }

      ReducedEdge sumEdge = new ReducedEdge(v1);
      sumEdge.addEdge(leavingEdges[0]);
      sumEdge.addEdge(leavingEdges[1]);

      pApplyTo.removeEdge(u, v1, leavingEdges[0]);
      pApplyTo.removeEdge(u, v2, leavingEdges[1]);
      pApplyTo.addEdge(u, v1, sumEdge);

      incSummarizationsOnNode(u, 1);

      toTraverse.clear();
      traverseDone.clear();
      toTraverse.add(v1);
      result = true;
    }

    return result;
  }

  private static class FunctionNodeManager {
    private Map<CFANode, ReducedNode> nodeMapping = new HashMap<CFANode, ReducedNode>();

    public ReducedNode getWrapper(CFANode pNode) {
      ReducedNode result = nodeMapping.get(pNode);
      if (result == null) {
        result = new ReducedNode(pNode);
        nodeMapping.put(pNode, result);
      }
      return result;
    }
  }


  /**
   * Returns the summarized subgraph described by the function
   * and the outgoing function calls that get inlined.
   *
   */
  private ReducedFunction inlineAndSummarize(CFAFunctionDefinitionNode pFunctionNode, FunctionCallEdge pCalledBy) {
    inliningStack.push(pFunctionNode);

    Set<CFAEdge> traversed = new HashSet<CFAEdge>();
    Deque<ReducedNode> openEndpoints = new ArrayDeque<ReducedNode>();
    FunctionNodeManager functionNodes = new FunctionNodeManager();

    ReducedNode entryNode = functionNodes.getWrapper(pFunctionNode);
    ReducedNode exitNode = functionNodes.getWrapper(pFunctionNode.getExitNode());
    ReducedFunction result = new ReducedFunction(entryNode, exitNode);


    // First: Inline called functions in a summarized version.
    //        --> recursion

    // Start traversing at the entry node of the function
    openEndpoints.add(result.getEntryNode());
    while (openEndpoints.size() > 0) {
      ReducedNode uSn = openEndpoints.removeFirst();

      // Look at all leaving edges.
      for (CFAEdge e: CFAUtils.leavingEdges(uSn.getWrapped())) {
        if (!traversed.add(e)) {
          continue;
        }

        // Depending on the type of the edge...
        if (e instanceof FunctionCallEdge) {
          FunctionCallEdge callEdge = (FunctionCallEdge) e;
          ReducedNode callReturnTarget = functionNodes.getWrapper(callEdge.getSummaryEdge().getSuccessor());
          CFAFunctionDefinitionNode calledFunction = callEdge.getSuccessor();

          if (inliningStack.contains(calledFunction)) {
            System.out.println("Ignoring recursion of " + calledFunction.getFunctionName());
            result.addEdge(uSn, callReturnTarget);
          } else {
            System.out.println(String.format("Inlining %s to %s", calledFunction.getFunctionName(), pFunctionNode.getFunctionName()));
            ReducedFunction functionSum = inlineAndSummarize(calledFunction, callEdge);

            result.insertFunctionSum(functionSum);
            result.addEdge(uSn, functionSum.getEntryNode());

            // it is possible, that a function never returns e.g. if there is a loop
            // in the form of "labelXYZ: goto labelXYZ;"
            // --> integrate the exit of the function to the control flow only if it has entering edges!
            if (functionSum.getExitNode().getWrapped().getNumEnteringEdges() > 0) {
              result.addEdge(functionSum.getExitNode(), callReturnTarget);
            }
          }

          openEndpoints.add(callReturnTarget);
        } else {
          if (e.getSuccessor() == pFunctionNode.getExitNode()) {
            result.addEdge(uSn, exitNode);
          } else {
            ReducedNode vSn = functionNodes.getWrapper(e.getSuccessor());
            result.addEdge(uSn, vSn);
            openEndpoints.add(vSn);
          }
        }
      }
    }

    // Second: Summarize this function if the
    //         summarization-threshold is not already reached.
    applyReductionSequences(result);

    inliningStack.pop();
    return result;
  }

  protected void applyReductionSequences(ReducedFunction pApplyTo) {
    // Summarize the given function if the summarization-threshold is not already reached.
    boolean sequenceApplied, choiceApplied;
    do {
      sequenceApplied = applySequenceRule(pApplyTo);
      choiceApplied = applyChoiceRule(pApplyTo);
    } while (sequenceApplied || choiceApplied);
  }

  /**
   * Write the in-lined version of the CFA to the given PrintStream.
   *
   * @param pInlinedCfa
   * @param pOut
   */
  public void printInlinedCfa (Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> pInlinedCfa, PrintStream pOut) {
    for (ReducedNode u: pInlinedCfa.keySet()) {
      Map<ReducedNode, Set<ReducedEdge>> uTarget = pInlinedCfa.get(u);
      for (ReducedNode v: uTarget.keySet()) {
        for (int i=0; i<uTarget.get(v).size(); i++) {
          pOut.println(String.format("REL\t%s_%d_%d\t%s_%d_%d",
                u.getWrapped().getFunctionName(),
                u.getWrapped().getLineNumber(),
                u.getUniqueNodeId(),
                v.getWrapped().getFunctionName(),
                v.getWrapped().getLineNumber(),
                v.getUniqueNodeId()));
        }
      }
    }
  }

  /**
   * Compute the nodes of the given CFA that should be abstraction-nodes.
   */
  @Override
  public synchronized Set<CFANode> computeAbstractionNodes(final CFA pCfa) {
    assert(pCfa != null);
    assert(this.inliningStack.size() == 0);

    ReducedFunction reducedProgram = inlineAndSummarize(pCfa.getMainFunction(), null);

    if (reducedCfaFile != null) {
      try {
        Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = reducedProgram.getInlinedCfa();

        PrintStream out = new PrintStream (reducedCfaFile);
        printInlinedCfa(inlinedCfa, out);
        out.flush();
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Set<ReducedNode> abstractionNodes = reducedProgram.getAllActiveNodes();
    Set<CFANode> result = new HashSet<CFANode>(abstractionNodes.size());
    for (ReducedNode n : abstractionNodes) {
      result.add(n.getWrapped());
    }

    System.out.println(String.format("CFANodes: %d, AbstNodes: %d, summarizationThresold: %d", pCfa.getAllNodes().size(), result.size(), reductionThreshold));
    return result;
  }
}

