/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.slicing;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.cwriter.CFAToCTranslator;

@Options(prefix = "slicing")
public class SliceExporter {

  @Option(
      secure = true,
      name = "exportToC.enable",
      description = "Whether to export slices as C program files")
  private boolean exportToC = false;

  @Option(
      secure = true,
      name = "exportToC.file",
      description = "File template for exported C program slices")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportToCFile = PathTemplate.ofFormatString("programSlice.%d.c");

  @Option(
      secure = true,
      name = "exportCriteria.enable",
      description = "Export the used slicing criteria to file")
  private boolean exportCriteria = false;

  @Option(
      secure = true,
      name = "exportCriteria.file",
      description = "File template for export of used slicing criteria")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportCriteriaFile =
      PathTemplate.ofFormatString("programSlice.%d.criteria.txt");

  private final LogManager logger;
  private int exportCount = -1;

  public SliceExporter(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;
  }

  /** Returns true if any leaving edge of any node is contained in pRelevantEdges. */
  private boolean containsRelevantEdge(Collection<CFANode> pNodes, Set<CFAEdge> pRelevantEdges) {

    for (CFANode node : pNodes) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        if (pRelevantEdges.contains(edge)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns true if the node hast at least one leaving edge and only leaving edges that are assume
   * edges and not contained in pRelevantEdges.
   */
  private boolean skipNextAssumeBranching(CFANode pNode, Set<CFAEdge> pRelevantEdges) {

    for (CFAEdge succEdge : CFAUtils.leavingEdges(pNode)) {
      if (succEdge.getEdgeType() != CFAEdgeType.AssumeEdge || pRelevantEdges.contains(succEdge)) {
        return false;
      }
    }

    return pNode.getNumLeavingEdges() > 0;
  }

  /**
   * Returns a node with content copied from specified pNode.
   *
   * <p>If pNodeMap already contains a node for the specified pNode (only one clone per node
   * number), the cached node from pNodeMap is returned; otherwise a new node with content copied
   * from specified pNode is created, stored in pNodeMap, and returned.
   */
  private CFANode cloneNode(CFANode pNode, Map<Integer, CFANode> pNodeMap) {

    CFANode newNode = pNodeMap.get(pNode.getNodeNumber());
    if (newNode != null) {
      return newNode;
    }

    AFunctionDeclaration functionName = pNode.getFunction();

    if (pNode instanceof CLabelNode) {

      CLabelNode labelNode = (CLabelNode) pNode;
      newNode = new CLabelNode(functionName, labelNode.getLabel());

    } else if (pNode instanceof CFATerminationNode) {

      newNode = new CFATerminationNode(functionName);

    } else if (pNode instanceof FunctionExitNode) {

      newNode = new FunctionExitNode(functionName);

    } else if (pNode instanceof CFunctionEntryNode) {

      CFunctionEntryNode entryNode = (CFunctionEntryNode) pNode;
      FunctionExitNode newExitNode =
          (FunctionExitNode) cloneNode(entryNode.getExitNode(), pNodeMap);

      newNode =
          new CFunctionEntryNode(
              entryNode.getFileLocation(),
              entryNode.getFunctionDefinition(),
              newExitNode,
              entryNode.getReturnVariable());
      newExitNode.setEntryNode((CFunctionEntryNode) newNode);

    } else {

      newNode = new CFANode(functionName);
    }

    newNode.setReversePostorderId(pNode.getReversePostorderId());

    if (pNode.isLoopStart()) {
      newNode.setLoopStart();
    }

    pNodeMap.put(pNode.getNodeNumber(), newNode);

    return newNode;
  }

  /**
   * Creates a new CFunctionCallEdge with the following connections:
   *
   * <p><code>
   *                                                           (dummy nodes)
   * [pPredecessor] --- CFunctionCallEdge ---> new CFunctionEntryNode(newFunctionExitNode())
   *        |                    |
   *        ----------- CFunctionSummaryEdge ---> [pSuccessor]
   * </code>
   */
  private CFunctionCallEdge cloneFunctionCall(
      CFunctionCallEdge pEdge, CFANode pPredecessor, CFANode pSuccessor) {

    CFunctionSummaryEdge summaryEdge = pEdge.getSummaryEdge();
    CFunctionEntryNode entryNode = pEdge.getSuccessor();

    FunctionExitNode newExitNode = new FunctionExitNode(entryNode.getFunction());

    CFunctionEntryNode newEntryNode =
        new CFunctionEntryNode(
            pEdge.getFileLocation(),
            entryNode.getFunctionDefinition(),
            newExitNode,
            entryNode.getReturnVariable());

    CFunctionSummaryEdge newSummaryEdge =
        new CFunctionSummaryEdge(
            summaryEdge.getRawStatement(),
            summaryEdge.getFileLocation(),
            pPredecessor,
            pSuccessor,
            summaryEdge.getExpression(),
            newEntryNode);

    pPredecessor.addLeavingSummaryEdge(newSummaryEdge);
    pSuccessor.addEnteringSummaryEdge(newSummaryEdge);

    return new CFunctionCallEdge(
        pEdge.getRawStatement(),
        pEdge.getFileLocation(),
        pPredecessor,
        newEntryNode,
        pEdge.getRawAST().get(),
        newSummaryEdge);
  }

  /**
   * Returns a new edge with content copied from specified pEdge and with specified predecessor and
   * successor.
   *
   * <p>Treatment of CFunctionCallEdges is special and handled in {@link #cloneFunctionCall}.
   */
  private CFAEdge cloneEdge(CFAEdge pEdge, CFANode pPredecessor, CFANode pSuccessor) {

    FileLocation loc = pEdge.getFileLocation();
    String raw = pEdge.getRawStatement();
    String desc = pEdge.getDescription();
    CFAEdgeType type = pEdge.getEdgeType();

    if (type == CFAEdgeType.BlankEdge) {

      return new BlankEdge(raw, loc, pPredecessor, pSuccessor, desc);

    } else if (type == CFAEdgeType.AssumeEdge && pEdge instanceof CAssumeEdge) {

      CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
      return new CAssumeEdge(
          raw,
          loc,
          pPredecessor,
          pSuccessor,
          assumeEdge.getRawAST().get(),
          assumeEdge.getTruthAssumption(),
          assumeEdge.isSwapped(),
          assumeEdge.isArtificialIntermediate());

    } else if (pEdge instanceof CFunctionSummaryStatementEdge) {

      CFunctionSummaryStatementEdge statementEdge = (CFunctionSummaryStatementEdge) pEdge;
      return new CFunctionSummaryStatementEdge(
          raw,
          statementEdge.getStatement(),
          loc,
          pPredecessor,
          pSuccessor,
          statementEdge.getFunctionCall(),
          statementEdge.getFunctionName());

    } else if (type == CFAEdgeType.StatementEdge && pEdge instanceof CStatementEdge) {

      CStatementEdge statementEdge = (CStatementEdge) pEdge;
      return new CStatementEdge(raw, statementEdge.getStatement(), loc, pPredecessor, pSuccessor);

    } else if (type == CFAEdgeType.DeclarationEdge && pEdge instanceof CDeclarationEdge) {

      CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;
      return new CDeclarationEdge(
          raw, loc, pPredecessor, pSuccessor, declarationEdge.getDeclaration());

    } else if (type == CFAEdgeType.ReturnStatementEdge && pEdge instanceof CReturnStatementEdge) {

      CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge) pEdge;
      return new CReturnStatementEdge(
          raw,
          returnStatementEdge.getRawAST().get(),
          loc,
          pPredecessor,
          (FunctionExitNode) pSuccessor);

    } else if (type == CFAEdgeType.FunctionCallEdge && pEdge instanceof CFunctionCallEdge) {

      return cloneFunctionCall((CFunctionCallEdge) pEdge, pPredecessor, pSuccessor);

    } else {
      throw new AssertionError(
          String.format(
              "Cannot clone edge: type=%s, class=%s", type.toString(), pEdge.getClass().getName()));
    }
  }

  /**
   * Creates a new function that resembles the sliced function (as specified by pRelevantEdges) as
   * closely as possible.
   *
   * <p>Nodes of the created function are added to pNewNodes.
   *
   * @param pEntryNode the entry node for the function.
   * @param pRelevantEdges the relevant edges for the program slice.
   * @param pNewNodes the mapping of function names to function nodes (multimap).
   * @return the function entry node of the created function.
   */
  private FunctionEntryNode createRelevantFunction(
      CFunctionEntryNode pEntryNode,
      Set<CFAEdge> pRelevantEdges,
      Multimap<String, CFANode> pNewNodes) {

    Map<Integer, CFANode> nodeMap =
        new HashMap<>(); // mapping of old-node-number --> new-node-clone
    Queue<CFAEdge> waitlist = new ArrayDeque<>(); // edges to be processed (cloned or replaced)
    Set<CFANode> visited =
        new HashSet<>(); // a node is visited when all its leaving edges were added to the waitlist

    FunctionEntryNode newEntryNode = (FunctionEntryNode) cloneNode(pEntryNode, nodeMap);
    CFAUtils.leavingEdges(pEntryNode).copyInto(waitlist);
    visited.add(pEntryNode);

    while (!waitlist.isEmpty()) {

      CFAEdge edge = waitlist.remove();
      CFANode pred = edge.getPredecessor();
      CFANode succ = edge.getSuccessor();

      CFAEdge newEdge;
      CFANode newPred = cloneNode(pred, nodeMap);
      CFANode newSucc;

      // step over function
      if (edge instanceof CFunctionCallEdge) {
        succ = ((CFunctionCallEdge) edge).getSummaryEdge().getSuccessor();
      }

      newSucc = cloneNode(succ, nodeMap);

      if (pRelevantEdges.contains(edge)
          || edge.getEdgeType() == CFAEdgeType.BlankEdge
          || edge.getEdgeType() == CFAEdgeType.AssumeEdge
          || edge.getEdgeType() == CFAEdgeType.DeclarationEdge
          || edge instanceof CFunctionSummaryStatementEdge) {

        newEdge = cloneEdge(edge, newPred, newSucc);

      } else {
        newEdge = new BlankEdge("", edge.getFileLocation(), newPred, newSucc, "slice-irrelevant");
      }

      newPred.addLeavingEdge(newEdge);

      // newSucc is only the successor of all non-function-call edges. function call handling
      // takes care of special node-edge relations during node creation
      if (!(newEdge instanceof CFunctionCallEdge)) {
        newSucc.addEnteringEdge(newEdge);
      }

      // don't visit a node twice and stay in function
      if (!visited.contains(succ)
          && !(succ instanceof FunctionExitNode)
          && !(succ instanceof FunctionEntryNode)) {
        // If all leaving edges of the current successors
        // are irrelevant assume edges, one branch is chosen (arbitrarily)
        // and used as the only leaving branch.
        // This makes it possible to replace all of the assume edges by a single blank edge.
        // WARNING: It is assumed that there are no relevant edges in all the other branches.
        //          Edges of those branches (even relevant edges) will not be cloned!
        //          This code *must* be updated if any slicing method is used that creates
        //          irrelevant branching conditions, but at the same time allows nested relevant
        //          statements in those branches.
        if (skipNextAssumeBranching(succ, pRelevantEdges)) {

          CFAEdge assumeEdge = succ.getLeavingEdge(0);
          waitlist.add(
              new BlankEdge(
                  "",
                  assumeEdge.getFileLocation(),
                  succ,
                  assumeEdge.getSuccessor(),
                  "slice-irrelevant"));

        } else {
          CFAUtils.leavingEdges(succ).copyInto(waitlist);
        }

        visited.add(succ);
      }
    }

    pNewNodes.putAll(pEntryNode.getFunctionName(), nodeMap.values());

    return newEntryNode;
  }

  /** Creates a new CFA that resembles the program slice as closely as possible. */
  private CFA createRelevantCfa(final Slice pSlice) {

    final CFA originalCfa = pSlice.getOriginalCfa();
    final ImmutableSet<CFAEdge> relevantEdges = pSlice.getRelevantEdges();

    NavigableMap<String, FunctionEntryNode> newFunctions = new TreeMap<>();
    SortedSetMultimap<String, CFANode> newNodes = TreeMultimap.create();
    FunctionEntryNode newMainEntryNode = null;

    for (String functionName : originalCfa.getAllFunctionNames()) {
      final boolean isMainFunction =
          functionName.equals(originalCfa.getMainFunction().getFunctionName());

      FunctionEntryNode entryNode = originalCfa.getFunctionHead(functionName);

      Collection<CFANode> functionNodes = CFATraversal.dfs().collectNodesReachableFrom(entryNode);

      if (isMainFunction || containsRelevantEdge(functionNodes, relevantEdges)) {
        final FunctionEntryNode newEntryNode =
            createRelevantFunction((CFunctionEntryNode) entryNode, relevantEdges, newNodes);
        newFunctions.put(functionName, newEntryNode);

        if (isMainFunction) {
          checkState(
              newMainEntryNode == null,
              "Trying to set entry node of main function, but one already exists: %s",
              newMainEntryNode);
          newMainEntryNode = newEntryNode;
        }
      }
    }
    assert newMainEntryNode != null;

    return new MutableCFA(
        originalCfa.getMachineModel(),
        newFunctions,
        newNodes,
        newMainEntryNode,
        originalCfa.getFileNames(),
        originalCfa.getLanguage());
  }

  /**
   * Executes the slice-exporter, which (depending on the configuration) exports program slices to C
   * program files.
   *
   * @param pSlice program slice to export
   */
  public void execute(Slice pSlice) {
    exportCount++;
    if (exportCriteria && exportCriteriaFile != null) {
      Concurrency.newThread(
              "Slice-criteria-Exporter",
              () -> {
                Path path = exportCriteriaFile.getPath(exportCount);

                try (Writer writer = IO.openOutputFile(path, Charset.defaultCharset())) {
                  StringBuilder output = new StringBuilder();
                  for (CFAEdge e : pSlice.getUsedCriteria()) {
                    FileLocation loc = e.getFileLocation();
                    output
                        .append(loc.getFileName())
                        .append(":")
                        .append(loc.getStartingLineNumber())
                        .append(":")
                        .append(e.getCode())
                        .append("\n");
                  }
                  writer.write(output.toString());

                } catch (IOException e) {
                  logger.logUserException(
                      Level.WARNING, e, "Could not write slicing criteria to file " + path);
                }
              })
          .start();
    }

    if (exportToC && exportToCFile != null) {
      Concurrency.newThread(
              "Slice-Exporter",
              () -> {
                CFA sliceCfa = createRelevantCfa(pSlice);

                Path path = exportToCFile.getPath(exportCount);

                try (Writer writer = IO.openOutputFile(path, Charset.defaultCharset())) {

                  String code = new CFAToCTranslator().translateCfa(sliceCfa);
                  writer.write(code);

                } catch (CPAException | IOException | InvalidConfigurationException e) {
                  logger.logUserException(Level.WARNING, e, "Could not write CFA to C file.");
                }
              })
          .start();
    }
  }
}
