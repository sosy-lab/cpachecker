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
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
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
import org.sosy_lab.cpachecker.util.blocking.container.ItemTree;
import org.sosy_lab.cpachecker.util.blocking.container.ReducedEdge;
import org.sosy_lab.cpachecker.util.blocking.container.ReducedFunction;
import org.sosy_lab.cpachecker.util.blocking.container.ReducedNode;
import org.sosy_lab.cpachecker.util.blocking.interfaces.BlockComputer;

@Options(prefix="blockreducer2")
public class BlockedCFAReducer2 implements BlockComputer {

  @Option(description="Do at most n summarizations on a node.")
  private int reductionThreshold = 100;

  @Option(description="Allow reduction of loop heads; calculate abstractions alwasy at loop heads?")
  private boolean alwaysAbstractOnLoopHeads = true;

  @Option(description="Allow reduction of function entries; calculate abstractions alwasy at function entries?")
  private boolean alwaysAbstractOnFunctionEntries = false;

  @Option(description="Allow reduction of function exits; calculate abstractions alwasy at function exits?")
  private boolean alwaysAbstractOnFunctionExits = false;

  @Option(description="Allow generic nodes to be abstraction nodes?")
  private boolean allowAbstOnGenericNodes = false;

  @Option(description="Allow function-entry nodes to be abstraction nodes?")
  private boolean allowAbstOnFunctionEntry = true;

  @Option(description="Allow function-exit nodes to be abstraction nodes?")
  private boolean allowAbstOnFunctionExit = true;

  @Option(description="Allow loop-head nodes to be abstraction nodes?")
  private boolean allowAbstOnLoopHeads = true;

  @Option(name="reducedCfaFile", description="write the reduced cfa to the specified file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File reducedCfaFile = new File("ReducedCfa.rsf");

  private int functionCallSeq = 0;
  private final Deque<CFAFunctionDefinitionNode> inliningStack;
  private final ControlFlowStatistics controlFlowStatistics;
  private final LogManager logger;

  public BlockedCFAReducer2(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    if (pConfig != null) {
      pConfig.inject(this);
    }

    this.controlFlowStatistics = new ControlFlowStatistics(pConfig, pLogger);
    this.inliningStack = new ArrayDeque<CFAFunctionDefinitionNode>();
    this.logger = pLogger;
  }

  private boolean isCouldAbstractNode(ReducedNode pNode) {
    switch (pNode.getNodeKind()) {
      case GENERIC:       return allowAbstOnGenericNodes;
      case FUNCTIONENTRY: return allowAbstOnFunctionEntry;
      case FUNCTIONEXIT:  return allowAbstOnFunctionExit;
      case LOOPHEAD :     return allowAbstOnLoopHeads;
      default:            return false;
    }
  }

  private boolean isMustAbstractNode(ReducedNode pNode) {
    switch (pNode.getNodeKind()) {
      case FUNCTIONENTRY: return alwaysAbstractOnFunctionEntries;
      case FUNCTIONEXIT:  return alwaysAbstractOnFunctionExits;
      case LOOPHEAD :     return alwaysAbstractOnLoopHeads;
      default:            return false;
    }
  }

  protected boolean applySequenceRule(ReducedFunction pApplyTo) {
    boolean result = false;

    Queue<ReducedNode> toTraverse = new ArrayDeque<ReducedNode>();
    Set<ReducedEdge> traverseDone = new HashSet<ReducedEdge>();

    toTraverse.add(pApplyTo.getEntryNode());
    while (toTraverse.size() > 0) {
      ReducedNode u = toTraverse.remove();

      for (ReducedEdge uv: pApplyTo.getLeavingEdges(u)) {
        ReducedNode v = uv.getPointsTo();

        if (!traverseDone.add(uv)) {
          continue;
        }

        if (u == v) {
          continue;
        }

        ReducedEdge[] vLeavingEdges = pApplyTo.getLeavingEdges(v);
        if (vLeavingEdges.length == 0) {
          continue;
        }

        if (pApplyTo.getNumEnteringEdges(v) != 1) {
          toTraverse.add(v);
          continue;
        }

        // Given a chain u-v-w of chains, remove v must not be removed
        // if the threshold of the incoming edges is reached.
        // But it can always be removed if it is not a "couldOrMustAbstractNode".
        // Node other than couldOrMustAbstractNode can always be removed.
        if (isCouldAbstractNode(v) && (uv.getReductions() >= reductionThreshold)) {
          toTraverse.add(v);
          continue;
        }

        if (isMustAbstractNode(v)) {
          toTraverse.add(v);
          continue;
        }

        boolean uvRemoved = false;
        for (ReducedEdge vw: vLeavingEdges) {
          ReducedNode w = vw.getPointsTo();

          assert(u != v);
          assert(v != w);

          ReducedEdge sumEdge = new ReducedEdge(w);
          sumEdge.addEdge(uv);
          sumEdge.addEdge(vw);

          pApplyTo.removeEdge(v, w, vw);
          if (!uvRemoved) {
            pApplyTo.removeEdge(u, v, uv);
            uvRemoved = true;
          }
          pApplyTo.addEdge(u, w, sumEdge);

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
      if (traverseDone.contains(u)) {
        continue;
      }

      ReducedEdge[] leavingEdges = pApplyTo.getLeavingEdges(u);
      if (leavingEdges.length < 2) {
        for (ReducedEdge e: leavingEdges) {
          toTraverse.add(e.getPointsTo());
        }
        traverseDone.add(u);
        continue;
      }

      // Find pairs of leaving edges, that point to the same target.
      boolean onePairMerged = false;
      for (int x=0; x<leavingEdges.length && !onePairMerged; x++) {
        for (int y=x+1; y<leavingEdges.length && !onePairMerged; y++) {
          ReducedEdge edgeX = leavingEdges[x];
          ReducedEdge edgeY = leavingEdges[y];

          ReducedNode v1 = edgeX.getPointsTo();
          ReducedNode v2 = edgeY.getPointsTo();

          if (v1 == v2) {
            if (edgeX.getReductions() >= reductionThreshold
             || edgeY.getReductions() >= reductionThreshold) {
              continue;
            }

            ReducedEdge sumEdge = new ReducedEdge(v1);
            sumEdge.addEdge(edgeX);
            sumEdge.addEdge(edgeY);

            pApplyTo.removeEdge(u, v1, edgeX);
            pApplyTo.removeEdge(u, v2, edgeY);
            pApplyTo.addEdge(u, v1, sumEdge);

            onePairMerged = true;
          } else {
            toTraverse.add(v1);
            toTraverse.add(v2);
          }
        }
      }

      if (onePairMerged) {
        toTraverse.clear();
        traverseDone.clear();
        toTraverse.add(u);
        result = true;
      } else {
        traverseDone.add(u);
      }
    }

    return result;
  }

  private static class FunctionNodeManager {
    private Map<CFANode, ReducedNode> nodeMapping = new HashMap<CFANode, ReducedNode>();
    private int functionCallId;
    private String[] callstack;

    public ReducedNode getWrapper(CFANode pNode) {
      ReducedNode result = nodeMapping.get(pNode);
      if (result == null) {
        result = new ReducedNode(pNode, this.callstack);
        result.setFunctionCallId(this.functionCallId);
        this.nodeMapping.put(pNode, result);
      }
      return result;
    }

    public FunctionNodeManager(int pFunctionCallId, String[] pCallstack) {
      this.functionCallId = pFunctionCallId;
      this.callstack = pCallstack;
    }
  }

  private String[] getInliningStackAsArray() {
    String[] result = new String[this.inliningStack.size()];
    int i = result.length-1;
    for (CFAFunctionDefinitionNode fnNode: this.inliningStack) {
      result[i] = fnNode.getFunctionName();
      i--;
    }
    return result;
  }


  /**
   * Returns the summarized subgraph described by the function
   * and the outgoing function calls that get inlined.
   *
   */
  public ReducedFunction inlineAndSummarize(CFAFunctionDefinitionNode pFunctionNode) {
    this.functionCallSeq++;
    this.inliningStack.push(pFunctionNode);

    Set<CFAEdge> traversed = new HashSet<CFAEdge>();
    Deque<ReducedNode> openEndpoints = new ArrayDeque<ReducedNode>();
    FunctionNodeManager functionNodes = new FunctionNodeManager(this.functionCallSeq, this.getInliningStackAsArray());

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
            //System.out.println(String.format("Inlining %s to %s", calledFunction.getFunctionName(), pFunctionNode.getFunctionName()));
            ReducedFunction functionSum = inlineAndSummarize(calledFunction);

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

  private String getRsfEntryFor(ReducedNode pNode, boolean pIsAbstractionNode) {
    return String.format("%s_%s_%s_%d_%d",
        pNode.getWrapped().getFunctionName(),
        pNode.getNodeKindText(),
        pIsAbstractionNode ? "ABST" : "NOABST",
        pNode.getFunctionCallId(),
        pNode.getWrapped().getLineNumber());
  }

  /**
   * Write the in-lined version of the CFA to the given PrintStream.
   *
   * @param pInlinedCfa
   * @param pOut
   */
  public void printInlinedCfa (Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> pInlinedCfa, PrintStream pOut, ItemTree<String, CFANode> pAbstractionNodes) {
    for (ReducedNode u: pInlinedCfa.keySet()) {
      Map<ReducedNode, Set<ReducedEdge>> uTarget = pInlinedCfa.get(u);
      for (ReducedNode v: uTarget.keySet()) {
        for (int i=0; i<uTarget.get(v).size(); i++) {
          pOut.println(String.format("REL\t%s\t%s",
              getRsfEntryFor(u, pAbstractionNodes.containsLeaf(u.getCallstack(), u.getWrapped())),
              getRsfEntryFor(v, pAbstractionNodes.containsLeaf(v.getCallstack(), v.getWrapped()))
          ));
        }
      }
    }
  }

  private void addAbstNode(ItemTree<String, CFANode> pTarget, String[] pStack, CFANode pNode) {
//    System.out.print("Abstractionnode: ");
//    for (String s : pStack) {
//      System.out.print(s + ">");
//    }
//    System.out.println(pNode.getLineNumber());

    pTarget.put(pStack).addLeaf(pNode, true);
  }

  private ItemTree<String, CFANode> filterAbstractionNodes(ReducedFunction pInlined) {
    assert(pInlined != null);

    Set<ReducedNode> activeNodes = pInlined.getAllActiveNodes();
    ItemTree<String, CFANode> result = new ItemTree<String, CFANode>();

    for (ReducedNode rn : activeNodes) {
      String[] cs = rn.getCallstack();
      CFANode cn = rn.getWrapped();

      if (cn.getNumEnteringEdges() == 0
       || cn.getNumLeavingEdges() == 0) {
        // We can skip nodes that have no leaving edges or no entering edges.
        continue;
      }

      switch (rn.getNodeKind()) {
        case FUNCTIONENTRY: if (allowAbstOnFunctionEntry) addAbstNode(result, cs, cn); break;
        case FUNCTIONEXIT: if (allowAbstOnFunctionExit) addAbstNode(result, cs, cn); break;
        case GENERIC: if (allowAbstOnGenericNodes) addAbstNode(result, cs, cn); break;
        case LOOPHEAD: if (allowAbstOnLoopHeads) addAbstNode(result, cs, cn); break;

        default: throw new RuntimeException("Invalid kind of ReducedNode!");
      }
    }

    return result;
  }

  private void writeReducedCfaRsf (File pTargetFile, ReducedFunction pInlined, ItemTree<String, CFANode> pAbstractionNodes) {
    assert(pInlined != null);
    assert(pTargetFile != null);

    try {
      Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = pInlined.getInlinedCfa();

      PrintStream out = new PrintStream (pTargetFile);
      printInlinedCfa(inlinedCfa, out, pAbstractionNodes);

      out.flush();
      out.close();
    } catch (IOException e) {
      logger.logException(Level.WARNING, e, "Writing the rsf-file describing the reduced CFA failed!");
    }
  }


  /**
   * Compute the nodes of the given CFA that should be abstraction-nodes.
   */
  @Override
  public synchronized ItemTree<String, CFANode> computeAbstractionNodes(final CFA pCfa) throws InvalidConfigurationException{
    assert(pCfa != null);
    assert(this.inliningStack.size() == 0);
    assert(this.functionCallSeq == 0);

    this.functionCallSeq = 0;
    ReducedFunction reducedProgram = inlineAndSummarize(pCfa.getMainFunction());
    ItemTree<String, CFANode> result = filterAbstractionNodes(reducedProgram);

    if (reducedCfaFile != null) {
      writeReducedCfaRsf(reducedCfaFile, reducedProgram, result);
    }

    // TODO: The following should be moved to a more general position in CPAchecker.
    controlFlowStatistics.writeStatistics(pCfa);

    System.out.println(String.format("Nodes in reduced CFA: %d", pCfa.getAllNodes().size()));
    System.out.println(String.format("Abstraction nodes in reduced CFA: %d", result.getNumberOfLeafs(false)));
    System.out.println(String.format("Summarization threshold: %d", reductionThreshold));

    return result;
  }

  public void setReductionThreshold(int pReductionThreshold) {
    this.reductionThreshold = pReductionThreshold;
  }

  public void setAllowAbstOnGenericNodes(boolean pAllowAbstOnGenericNodes) {
    this.allowAbstOnGenericNodes = pAllowAbstOnGenericNodes;
  }

}

