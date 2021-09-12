// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaNodeTransformer;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassificationBuilder;

public final class CCfaTransformer
    extends CfaTransformer<CCfaNodeTransformer, CCfaEdgeTransformer> {

  private final Configuration configuration;
  private final LogManager logger;

  private final CFA originalCfa;
  private final Map<CFANode, Node> originalCfaNodeToNodeMap;

  private CCfaTransformer(
      Configuration pConfiguration,
      LogManager pLogger,
      CFA pOriginalCfa,
      Map<CFANode, Node> pNodeMap) {

    configuration = pConfiguration;
    logger = pLogger;

    originalCfa = pOriginalCfa;
    originalCfaNodeToNodeMap = pNodeMap;
  }

  public static CCfaTransformer createTransformer(
      Configuration pConfiguration, LogManager pLogger, CFA pCfa) {

    Objects.requireNonNull(pConfiguration, "pConfiguration must not be null");
    Objects.requireNonNull(pLogger, "pLogger must not be null");
    Objects.requireNonNull(pCfa, "pCfa must not be null");

    Map<CFANode, Node> nodeMap = new HashMap<>();

    for (CFANode cfaNode : pCfa.getAllNodes()) {
      nodeMap.put(cfaNode, CfaTransformer.Node.forOriginal(cfaNode));
      if (cfaNode instanceof FunctionEntryNode) {
        FunctionExitNode functionExitNode = ((FunctionEntryNode) cfaNode).getExitNode();
        nodeMap.put(functionExitNode, Node.forOriginal(functionExitNode));
      }
    }

    for (CFANode cfaNode : pCfa.getAllNodes()) {
      Node predecessor = nodeMap.get(cfaNode);
      for (CFAEdge cfaEdge : CFAUtils.allLeavingEdges(cfaNode)) {
        Node successor = nodeMap.get(cfaEdge.getSuccessor());
        Edge edge = Edge.forOriginal(cfaEdge);
        successor.attachEntering(edge);
        predecessor.attachLeaving(edge);
    }
  }

    return new CCfaTransformer(pConfiguration, pLogger, pCfa, nodeMap);
  }

  @Override
  public Optional<Node> get(CFANode pCfaNode) {

    Objects.requireNonNull(pCfaNode, "pCfaNode must not be null");

    return Optional.ofNullable(originalCfaNodeToNodeMap.get(pCfaNode));
  }

  @Override
  public CFA createCfa(CCfaNodeTransformer pNodeTransformer, CCfaEdgeTransformer pEdgeTransformer) {

    Objects.requireNonNull(pNodeTransformer, "pNodeTransformer must not be null");
    Objects.requireNonNull(pEdgeTransformer, "pEdgeTransformer must not be null");

    CfaBuilder cfaBuilder =
        new CfaBuilder(originalCfaNodeToNodeMap, pNodeTransformer, pEdgeTransformer);

    return cfaBuilder.createCfa(configuration, logger, originalCfa);
  }
  
  private static final class CfaBuilder {

    private final Map<CFANode, CfaTransformer.Node> originalCfaNodeToNode;

    private final CCfaNodeTransformer nodeTransformer;
    private final CCfaEdgeTransformer edgeTransformer;

    private final Map<CfaTransformer.Node, CFANode> nodeToNewCfaNode;
    private final Map<CfaTransformer.Edge, CFAEdge> edgeToNewCfaEdge;

    private CfaBuilder(
        Map<CFANode, CfaTransformer.Node> pOriginalCfaNodeToNode,
        CCfaNodeTransformer pNodeTransformer,
        CCfaEdgeTransformer pEdgeTransformer) {

      originalCfaNodeToNode = pOriginalCfaNodeToNode;

      nodeTransformer = pNodeTransformer;
      edgeTransformer = pEdgeTransformer;

      nodeToNewCfaNode = new HashMap<>();
      edgeToNewCfaEdge = new HashMap<>();
    }

    private CFANode newCfaNodeIfAbsent(CfaTransformer.Node pNode) {

      CFANode newCfaNode = nodeToNewCfaNode.get(pNode);
      if (newCfaNode != null) {
        return newCfaNode;
      }

      CFANode originalCfaNode = pNode.getOriginalCfaNode();

      if (originalCfaNode instanceof CLabelNode) {
        newCfaNode = nodeTransformer.transformCLabelNode((CLabelNode) originalCfaNode);
      } else if (originalCfaNode instanceof CFunctionEntryNode) {
        CFunctionEntryNode originalCfaEntryNode = (CFunctionEntryNode) originalCfaNode;
        CfaTransformer.Node exitNode =
            originalCfaNodeToNode.get(originalCfaEntryNode.getExitNode());
        FunctionExitNode newCfaExitNode = (FunctionExitNode) newCfaNodeIfAbsent(exitNode);
        newCfaNode =
            nodeTransformer.transformCFunctionEntryNode(
                (CFunctionEntryNode) originalCfaNode, newCfaExitNode);
        newCfaExitNode.setEntryNode((CFunctionEntryNode) newCfaNode);
      } else if (originalCfaNode instanceof FunctionExitNode) {
        newCfaNode = nodeTransformer.transformFunctionExitNode((FunctionExitNode) originalCfaNode);
      } else if (originalCfaNode instanceof CFATerminationNode) {
        newCfaNode =
            nodeTransformer.transformCfaTerminationNode((CFATerminationNode) originalCfaNode);
      } else {
        newCfaNode = nodeTransformer.transformCfaNode(originalCfaNode);
      }

      nodeToNewCfaNode.put(pNode, newCfaNode);

      return newCfaNode;
    }

    private CFunctionSummaryEdge newCFunctionSummaryEdge(
        CfaTransformer.Edge pEdge, CFANode pNewCfaPredecessorNode, CFANode pNewCfaSuccessorNode) {

      for (CfaTransformer.Edge leavingEdge : pEdge.getPredecessorOrElseThrow().iterateLeaving()) {
        if (leavingEdge.getOriginalCfaEdge() instanceof CFunctionCallEdge) {
          CFunctionEntryNode cfaEntryNode =
              (CFunctionEntryNode) nodeToNewCfaNode.get(leavingEdge.getSuccessorOrElseThrow());
          return edgeTransformer.transformCFunctionSummaryEdge(
              (CFunctionSummaryEdge) pEdge.getOriginalCfaEdge(),
              pNewCfaPredecessorNode,
              pNewCfaSuccessorNode,
              cfaEntryNode);
        }
      }

      throw new IllegalStateException("Missing function call edge for summary edge: " + pEdge);
    }

    private CFunctionCallEdge newCFunctionCallEdge(
        CfaTransformer.Edge pEdge, CFANode pNewCfaPredecessorNode, CFANode pNewCfaSuccessorNode) {
      for (CfaTransformer.Edge summaryEdge : pEdge.getPredecessorOrElseThrow().iterateLeaving()) {
        if (summaryEdge.getOriginalCfaEdge() instanceof CFunctionSummaryEdge) {
          CFunctionSummaryEdge cfaSummaryEdge =
              (CFunctionSummaryEdge) newCfaEdgeIfAbsent(summaryEdge, true);
          return edgeTransformer.transformCFunctionCallEdge(
              (CFunctionCallEdge) pEdge.getOriginalCfaEdge(),
              pNewCfaPredecessorNode,
              (CFunctionEntryNode) pNewCfaSuccessorNode,
              cfaSummaryEdge);
        }
      }

      throw new IllegalStateException("Missing summary edge for function call edge: " + pEdge);
    }

    private CFunctionReturnEdge newCFunctionReturnEdge(
        CfaTransformer.Edge pEdge, CFANode pNewCfaPredecessorNode, CFANode pNewCfaSuccessorNode) {
      for (CfaTransformer.Edge summaryEdge : pEdge.getSuccessorOrElseThrow().iterateEntering()) {
        if (summaryEdge.getOriginalCfaEdge() instanceof CFunctionSummaryEdge) {
          CFunctionSummaryEdge cfaSummaryEdge =
              (CFunctionSummaryEdge) newCfaEdgeIfAbsent(summaryEdge, true);
          return edgeTransformer.transformCFunctionReturnEdge(
              (CFunctionReturnEdge) pEdge.getOriginalCfaEdge(),
              (FunctionExitNode) pNewCfaPredecessorNode,
              pNewCfaSuccessorNode,
              cfaSummaryEdge);
        }
      }

      throw new IllegalStateException("Missing summary edge for function return edge: " + pEdge);
    }

    private CFAEdge newCfaEdgeIfAbsent(CfaTransformer.Edge pEdge, boolean pAllowInterprocedural) {

      CFAEdge newCfaEdge = edgeToNewCfaEdge.get(pEdge);
      if (newCfaEdge != null) {
        return newCfaEdge;
      }

      CFANode cfaPredecessorNode = nodeToNewCfaNode.get(pEdge.getPredecessorOrElseThrow());
      CFANode cfaSuccessorNode = nodeToNewCfaNode.get(pEdge.getSuccessorOrElseThrow());

      CFAEdge originalCfaEdge = pEdge.getOriginalCfaEdge();

      if (originalCfaEdge instanceof BlankEdge) {
        newCfaEdge =
            edgeTransformer.transformBlankEdge(
                (BlankEdge) originalCfaEdge, cfaPredecessorNode, cfaSuccessorNode);
      } else if (originalCfaEdge instanceof CAssumeEdge) {
        newCfaEdge =
            edgeTransformer.transformCAssumeEdge(
                (CAssumeEdge) originalCfaEdge, cfaPredecessorNode, cfaSuccessorNode);
      } else if (originalCfaEdge instanceof CDeclarationEdge) {
        newCfaEdge =
            edgeTransformer.transformCDeclarationEdge(
                (CDeclarationEdge) originalCfaEdge, cfaPredecessorNode, cfaSuccessorNode);
      } else if (originalCfaEdge instanceof CFunctionSummaryStatementEdge) {
        if (pAllowInterprocedural) {
          newCfaEdge =
              edgeTransformer.transformCFunctionSummaryStatementEdge(
                  (CFunctionSummaryStatementEdge) originalCfaEdge,
                  cfaPredecessorNode,
                  cfaSuccessorNode);
        } else {
          newCfaEdge =
              new SummaryPlaceholderEdge(
                  "",
                  originalCfaEdge.getFileLocation(),
                  cfaPredecessorNode,
                  cfaSuccessorNode,
                  "summary-placeholder-edge");
        }
      } else if (originalCfaEdge instanceof CReturnStatementEdge) {
        newCfaEdge =
            edgeTransformer.transformCReturnStatementEdge(
                (CReturnStatementEdge) originalCfaEdge,
                cfaPredecessorNode,
                (FunctionExitNode) cfaSuccessorNode);
      } else if (originalCfaEdge instanceof CFunctionSummaryEdge) {
        if (pAllowInterprocedural) {
          newCfaEdge = newCFunctionSummaryEdge(pEdge, cfaPredecessorNode, cfaSuccessorNode);
        } else {
          newCfaEdge =
              new SummaryPlaceholderEdge(
                  "",
                  originalCfaEdge.getFileLocation(),
                  cfaPredecessorNode,
                  cfaSuccessorNode,
                  "summary-placeholder-edge");
        }
      } else if (originalCfaEdge instanceof CFunctionReturnEdge) {
        if (pAllowInterprocedural) {
          newCfaEdge = newCFunctionReturnEdge(pEdge, cfaPredecessorNode, cfaSuccessorNode);
        }
      } else if (originalCfaEdge instanceof CFunctionCallEdge) {
        if (pAllowInterprocedural) {
          newCfaEdge = newCFunctionCallEdge(pEdge, cfaPredecessorNode, cfaSuccessorNode);
        }
      } else if (originalCfaEdge instanceof CStatementEdge) {
        newCfaEdge =
            edgeTransformer.transformCStatementEdge(
                (CStatementEdge) originalCfaEdge, cfaPredecessorNode, cfaSuccessorNode);
      } else {
        throw new AssertionError("Unknown CFA edge type: " + originalCfaEdge.getClass());
      }

      if (newCfaEdge != null) {

        if (!(newCfaEdge instanceof SummaryPlaceholderEdge)) {
          edgeToNewCfaEdge.put(pEdge, newCfaEdge);
        }

        if (newCfaEdge instanceof CFunctionSummaryEdge) {
          CFunctionSummaryEdge cfaSummaryEdge = (CFunctionSummaryEdge) newCfaEdge;
          cfaPredecessorNode.addLeavingSummaryEdge(cfaSummaryEdge);
          cfaSuccessorNode.addEnteringSummaryEdge(cfaSummaryEdge);
        } else {
          cfaPredecessorNode.addLeavingEdge(newCfaEdge);
          cfaSuccessorNode.addEnteringEdge(newCfaEdge);
        }
      }

      return newCfaEdge;
    }

    private Optional<VariableClassification> createVariableClassification(
        Configuration pConfiguration, LogManager pLogger, CFA pCfa) {

      try {
        VariableClassificationBuilder builder =
            new VariableClassificationBuilder(pConfiguration, pLogger);
        return Optional.of(builder.build(pCfa));
      } catch (UnrecognizedCodeException | InvalidConfigurationException ex) {
        pLogger.log(Level.WARNING, ex);
        return Optional.empty();
      }
    }

    private void removeSummaryPlaceholderEdges() {

      List<SummaryPlaceholderEdge> summaryPlaceholderEdges = new ArrayList<>();

      for (CFANode newCfaNode : nodeToNewCfaNode.values()) {
        for (CFAEdge newCfaEdge : CFAUtils.allLeavingEdges(newCfaNode)) {
          if (newCfaEdge instanceof SummaryPlaceholderEdge) {
            summaryPlaceholderEdges.add((SummaryPlaceholderEdge) newCfaEdge);
          }
        }
      }

      for (SummaryPlaceholderEdge summaryPlaceholderEdge : summaryPlaceholderEdges) {
        summaryPlaceholderEdge.getPredecessor().removeLeavingEdge(summaryPlaceholderEdge);
        summaryPlaceholderEdge.getSuccessor().removeEnteringEdge(summaryPlaceholderEdge);
      }
    }

    private CFA createCfa(Configuration pConfiguration, LogManager pLogger, CFA pOriginalCfa) {

      CfaTransformer.Node mainEntryNode = originalCfaNodeToNode.get(pOriginalCfa.getMainFunction());

      NavigableMap<String, FunctionEntryNode> newCfaFunctions = new TreeMap<>();
      TreeMultimap<String, CFANode> newCfaNodes = TreeMultimap.create();

      Set<CfaTransformer.Node> waitlisted = new HashSet<>(ImmutableList.of(mainEntryNode));
      Deque<CfaTransformer.Node> waitlist = new ArrayDeque<>(ImmutableList.of(mainEntryNode));

      while (!waitlist.isEmpty()) {

        CfaTransformer.Node currentNode = waitlist.remove();
        CFANode newCfaNode = newCfaNodeIfAbsent(currentNode);
        String functionName = newCfaNode.getFunction().getQualifiedName();

        if (newCfaNode instanceof FunctionEntryNode) {
          newCfaFunctions.put(functionName, (FunctionEntryNode) newCfaNode);
        }

        newCfaNodes.put(functionName, newCfaNode);

        for (CfaTransformer.Edge leavingEdge : currentNode.iterateLeaving()) {
          CfaTransformer.Node successorNode = leavingEdge.getSuccessorOrElseThrow();
          if (waitlisted.add(successorNode)) {
            waitlist.add(successorNode);
          }
        }

        for (CfaTransformer.Edge enteringEdge : currentNode.iterateEntering()) {
          CfaTransformer.Node predecessorNode = enteringEdge.getPredecessorOrElseThrow();
          if (waitlisted.add(predecessorNode)) {
            waitlist.add(predecessorNode);
          }
        }
      }

      // don't create create function call, return and summary edges
      for (CfaTransformer.Node currentNode : nodeToNewCfaNode.keySet()) {
        for (CfaTransformer.Edge edge : currentNode.iterateLeaving()) {
          newCfaEdgeIfAbsent(edge, /* allow interprocedural edges = */ false);
        }
      }

      MutableCFA newMutableCfa =
          new MutableCFA(
              pOriginalCfa.getMachineModel(),
              newCfaFunctions,
              newCfaNodes,
              (FunctionEntryNode) nodeToNewCfaNode.get(mainEntryNode),
              pOriginalCfa.getFileNames(),
              pOriginalCfa.getLanguage());

      for (FunctionEntryNode function : newMutableCfa.getAllFunctionHeads()) {
        CFAReversePostorder sorter = new CFAReversePostorder();
        sorter.assignSorting(function);
      }

      if (pOriginalCfa.getLoopStructure().isPresent()) {
        try {
          newMutableCfa.setLoopStructure(LoopStructure.getLoopStructure(newMutableCfa));
        } catch (ParserException ex) {
          pLogger.log(Level.WARNING, ex);
        }
      }

      // create supergraph including function call, return and summary edges
      removeSummaryPlaceholderEdges();
      for (CfaTransformer.Node currentNode : nodeToNewCfaNode.keySet()) {
        for (CfaTransformer.Edge edge : currentNode.iterateLeaving()) {
          newCfaEdgeIfAbsent(edge, /* allow interprocedural edges = */ true);
        }
      }

      Optional<VariableClassification> variableClassification;
      if (pOriginalCfa.getVarClassification().isPresent()) {
        variableClassification =
            createVariableClassification(pConfiguration, pLogger, newMutableCfa);
      } else {
        variableClassification = Optional.empty();
      }

      return newMutableCfa.makeImmutableCFA(variableClassification);
    }

    private static final class SummaryPlaceholderEdge extends BlankEdge {

      private static final long serialVersionUID = -4605071143372536460L;

      public SummaryPlaceholderEdge(
          String pRawStatement,
          FileLocation pFileLocation,
          CFANode pPredecessor,
          CFANode pSuccessor,
          String pDescription) {
        super(pRawStatement, pFileLocation, pPredecessor, pSuccessor, pDescription);
      }
    }
  }
}
