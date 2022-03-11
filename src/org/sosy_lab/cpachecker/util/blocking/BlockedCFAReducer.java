// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.blocking;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.blocking.interfaces.BlockComputer;

@Options(prefix = "blockreducer")
public class BlockedCFAReducer implements BlockComputer {

  @Option(secure = true, description = "Do at most n summarizations on a node.")
  private int reductionThreshold = 100;

  @Option(
      secure = true,
      description = "Allow reduction of loop heads; calculate abstractions always at loop heads?")
  private boolean allowReduceLoopHeads = false;

  @Option(
      secure = true,
      description =
          "Allow reduction of function entries; calculate abstractions always at function entries?")
  private boolean allowReduceFunctionEntries = true;

  @Option(
      secure = true,
      description =
          "Allow reduction of function exits; calculate abstractions always at function exits?")
  private boolean allowReduceFunctionExits = true;

  @Option(
      secure = true,
      name = "reducedCfaFile",
      description = "write the reduced cfa to the specified file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path reducedCfaFile = Path.of("ReducedCfa.rsf");

  private int functionCallSeq = 0;
  private final Deque<FunctionEntryNode> inliningStack;

  private final LogManager logger;

  public BlockedCFAReducer(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = checkNotNull(pLogger);
    inliningStack = new ArrayDeque<>();
  }

  private boolean isAbstractionNode(ReducedNode pNode) {
    return (pNode.isLoopHead() && !allowReduceLoopHeads)
        || (pNode.isFunctionEntry() && !allowReduceFunctionEntries)
        || (pNode.isFunctionExit() && !allowReduceFunctionExits);
  }

  /** Increment the number of summarizations that are done with pNode as the root-node. */
  private void incSummarizationsOnNode(ReducedNode pNode, int pIncBy) {
    assert reductionThreshold > 0;
    pNode.incSummarizations(pIncBy);
  }

  private int getSummarizationsOnNode(ReducedNode pNode) {
    return pNode.getSummarizations();
  }

  @VisibleForTesting
  boolean applySequenceRule(ReducedFunction pApplyTo) {
    boolean result = false;
    // TODO: ensure that this function is not applied across the scope of a loop.

    Queue<ReducedNode> toTraverse = new ArrayDeque<>();
    Set<ReducedEdge> traverseDone = new HashSet<>();

    toTraverse.add(pApplyTo.getEntryNode());
    while (!toTraverse.isEmpty()) {
      ReducedNode u = toTraverse.remove();

      for (ReducedEdge e : pApplyTo.getLeavingEdges(u)) {
        ReducedNode v = e.getPointsTo();

        if (!traverseDone.add(e)) {
          continue;
        }

        if (u == v) {
          continue;
        }

        List<ReducedEdge> vLeavingEdges = pApplyTo.getLeavingEdges(v);
        if (vLeavingEdges.isEmpty()) {
          continue;
        }

        if (getSummarizationsOnNode(u) + vLeavingEdges.size() > reductionThreshold) {
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
        for (ReducedEdge f : vLeavingEdges) {
          ReducedNode w = f.getPointsTo();

          assert u != v;
          assert v != w;

          ReducedEdge sumEdge = new ReducedEdge(w);
          sumEdge.addEdge(e);
          sumEdge.addEdge(f);

          pApplyTo.removeEdge(v, w, f);
          if (!uvRemoved) {
            pApplyTo.removeEdge(u, v, e);
            incSummarizationsOnNode(u, v.getSummarizations());
            uvRemoved = true;
          }
          pApplyTo.addEdge(u, w, sumEdge);

          incSummarizationsOnNode(u, 1);
        }

        toTraverse.clear();
        traverseDone.clear();
        toTraverse.add(u);
        result = true;
      }
    }

    return result;
  }

  @VisibleForTesting
  boolean applyChoiceRule(ReducedFunction pApplyTo) {
    boolean result = false;

    Deque<ReducedNode> toTraverse = new ArrayDeque<>();
    Set<ReducedNode> traverseDone = new HashSet<>();

    toTraverse.add(pApplyTo.getEntryNode());
    while (!toTraverse.isEmpty()) {
      ReducedNode u = toTraverse.removeFirst();
      if (traverseDone.contains(u)) {
        continue;
      }

      List<ReducedEdge> leavingEdges = pApplyTo.getLeavingEdges(u);
      if (leavingEdges.size() < 2 || getSummarizationsOnNode(u) >= reductionThreshold) {
        for (ReducedEdge e : leavingEdges) {
          toTraverse.add(e.getPointsTo());
        }
        traverseDone.add(u);
        continue;
      }

      // Find pairs of leaving edges, that point to the same target.
      boolean onePairMerged = false;
      for (int x = 0; x < leavingEdges.size() && !onePairMerged; x++) {
        for (int y = x + 1; y < leavingEdges.size() && !onePairMerged; y++) {
          ReducedEdge edgeX = leavingEdges.get(x);
          ReducedEdge edgeY = leavingEdges.get(y);

          ReducedNode v1 = edgeX.getPointsTo();
          ReducedNode v2 = edgeY.getPointsTo();

          if (v1 == v2) {
            ReducedEdge sumEdge = new ReducedEdge(v1);
            sumEdge.addEdge(edgeX);
            sumEdge.addEdge(edgeY);

            pApplyTo.removeEdge(u, v1, edgeX);
            pApplyTo.removeEdge(u, v2, edgeY);
            pApplyTo.addEdge(u, v1, sumEdge);

            incSummarizationsOnNode(u, 1);

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
    private final CFA cfa;
    private final Map<CFANode, ReducedNode> nodeMapping = new HashMap<>();
    private int functionCallId;

    public ReducedNode getWrapper(CFANode pNode) {
      ReducedNode result = nodeMapping.get(pNode);
      if (result == null) {
        boolean isLoopHead = cfa.getAllLoopHeads().orElseThrow().contains(pNode);
        result = new ReducedNode(pNode, isLoopHead);
        result.setFunctionCallId(functionCallId);
        nodeMapping.put(pNode, result);
      }
      return result;
    }

    public FunctionNodeManager(int pFunctionCallId, CFA pCfa) {
      functionCallId = pFunctionCallId;
      cfa = pCfa;
    }
  }

  /**
   * Returns the summarized subgraph described by the function and the outgoing function calls that
   * get inlined.
   */
  private ReducedFunction inlineAndSummarize(FunctionEntryNode pFunctionNode, CFA cfa) {
    functionCallSeq++;
    inliningStack.push(pFunctionNode);

    Set<CFAEdge> traversed = new HashSet<>();
    Deque<ReducedNode> openEndpoints = new ArrayDeque<>();
    FunctionNodeManager functionNodes = new FunctionNodeManager(functionCallSeq, cfa);

    ReducedNode entryNode = functionNodes.getWrapper(pFunctionNode);
    ReducedNode exitNode = functionNodes.getWrapper(pFunctionNode.getExitNode());
    ReducedFunction result = new ReducedFunction(entryNode, exitNode);

    // First: Inline called functions in a summarized version.
    //        --> recursion

    // Start traversing at the entry node of the function
    openEndpoints.add(result.getEntryNode());
    while (!openEndpoints.isEmpty()) {
      ReducedNode uSn = openEndpoints.removeFirst();

      // Look at all leaving edges.
      for (CFAEdge e : CFAUtils.leavingEdges(uSn.getWrapped())) {
        if (!traversed.add(e)) {
          continue;
        }

        // Depending on the type of the edge...
        if (e instanceof CFunctionCallEdge) {
          CFunctionCallEdge callEdge = (CFunctionCallEdge) e;
          ReducedNode callReturnTarget =
              functionNodes.getWrapper(callEdge.getSummaryEdge().getSuccessor());
          FunctionEntryNode calledFunction = callEdge.getSuccessor();

          if (inliningStack.contains(calledFunction)) {
            // Ignoring recursion of
            result.addEdge(uSn, callReturnTarget);
          } else {
            ReducedFunction functionSum = inlineAndSummarize(calledFunction, cfa);

            result.insertFunctionSum(functionSum);
            result.addEdge(uSn, functionSum.getEntryNode());

            // it is possible, that a function never returns e.g. if there is a loop
            // in the form of "labelXYZ: goto labelXYZ;"
            // --> integrate the exit of the function to the control flow only if it has entering
            // edges!
            if (functionSum.getExitNode().getWrapped().getNumEnteringEdges() > 0) {
              result.addEdge(functionSum.getExitNode(), callReturnTarget);
            }
          }

          openEndpoints.add(callReturnTarget);
        } else {
          if (Objects.equals(e.getSuccessor(), pFunctionNode.getExitNode())) {
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

  @VisibleForTesting
  void applyReductionSequences(ReducedFunction pApplyTo) {
    // Summarize the given function if the summarization-threshold is not already reached.
    boolean sequenceApplied, choiceApplied;
    do {
      sequenceApplied = applySequenceRule(pApplyTo);
      choiceApplied = applyChoiceRule(pApplyTo);
    } while (sequenceApplied || choiceApplied);
  }

  private String getRsfEntryFor(ReducedNode pNode) {
    return String.format(
        "%s_%s_%d_%d",
        pNode.getWrapped().getFunctionName(),
        pNode.getNodeKindText(),
        pNode.getFunctionCallId(),
        pNode.getWrapped().getNodeNumber());
  }

  /** Write the in-lined version of the CFA to the given output. */
  @VisibleForTesting
  void printInlinedCfa(
      Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> pInlinedCfa, Writer pOut)
      throws IOException {
    for (Entry<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> outerEntry :
        pInlinedCfa.entrySet()) {
      ReducedNode u = outerEntry.getKey();
      Map<ReducedNode, Set<ReducedEdge>> uTarget = outerEntry.getValue();
      for (Entry<ReducedNode, Set<ReducedEdge>> entry : uTarget.entrySet()) {
        ReducedNode v = entry.getKey();
        for (int i = 0; i < entry.getValue().size(); i++) {
          pOut.append("REL\t")
              .append(getRsfEntryFor(u))
              .append('\t')
              .append(getRsfEntryFor(v))
              .append(System.lineSeparator());
        }
      }
    }
  }

  /** Compute the nodes of the given CFA that should be abstraction-nodes. */
  @Override
  public ImmutableSet<CFANode> computeAbstractionNodes(final CFA pCfa) {
    assert pCfa != null;
    assert inliningStack.isEmpty();
    assert functionCallSeq == 0;

    functionCallSeq = 0;
    ReducedFunction reducedProgram = inlineAndSummarize(pCfa.getMainFunction(), pCfa);

    if (reducedCfaFile != null) {
      Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa =
          reducedProgram.getInlinedCfa();
      try (Writer w = IO.openOutputFile(reducedCfaFile, Charset.defaultCharset())) {
        printInlinedCfa(inlinedCfa, w);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write the reduced CFA to file");
      }
    }

    Set<ReducedNode> abstractionNodes = reducedProgram.getAllActiveNodes();
    Set<CFANode> result = Sets.newHashSetWithExpectedSize(abstractionNodes.size());
    for (ReducedNode n : abstractionNodes) {
      result.add(n.getWrapped());
    }

    return ImmutableSet.copyOf(result);
  }
}
