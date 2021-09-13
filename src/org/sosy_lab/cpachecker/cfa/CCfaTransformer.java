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
import java.util.function.BiFunction;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
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
import org.sosy_lab.cpachecker.util.MutableGraph;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassificationBuilder;

public final class CCfaTransformer {

  private CCfaTransformer() {}

  public static MutableGraph<CFANode, CFAEdge> createMutableGraph(CFA pCfa) {

    Objects.requireNonNull(pCfa, "pCfa must not be null");

    MutableGraph<CFANode, CFAEdge> mutableGraph = new CfaMutableGraph();

    for (CFANode cfaNode : pCfa.getAllNodes()) {
      mutableGraph.wrapNode(cfaNode);
      if (cfaNode instanceof FunctionEntryNode) {
        FunctionExitNode functionExitNode = ((FunctionEntryNode) cfaNode).getExitNode();
        mutableGraph.wrapNode(functionExitNode);
      }
    }
    
    for (CFANode cfaNode : pCfa.getAllNodes()) {
      MutableGraph.Node<CFANode, CFAEdge> predecessor = mutableGraph.getNode(cfaNode).orElseThrow();
      for (CFAEdge cfaEdge : CFAUtils.allLeavingEdges(cfaNode)) {
        MutableGraph.Node<CFANode, CFAEdge> successor =
            mutableGraph.getNode(cfaEdge.getSuccessor()).orElseThrow();
        MutableGraph.Edge<CFANode, CFAEdge> edge = mutableGraph.wrapEdge(cfaEdge);
        mutableGraph.attachEntering(successor, edge);
        mutableGraph.attachLeaving(predecessor, edge);
      }
    }

    return mutableGraph;
  }

  public static CFA createCfa(
      Configuration pConfiguration,
      LogManager pLogger,
      CFA pOriginalCfa,
      MutableGraph<CFANode, CFAEdge> pMutableGraph,
      CCfaNodeTransformer pNodeTransformer,
      CCfaEdgeTransformer pEdgeTransformer) {

    Objects.requireNonNull(pConfiguration, "pConfiguration must not be null");
    Objects.requireNonNull(pLogger, "pLogger must not be null");
    Objects.requireNonNull(pOriginalCfa, "pOriginalCfa must not be null");
    Objects.requireNonNull(pMutableGraph, "pMutableGraph must not be null");
    Objects.requireNonNull(pNodeTransformer, "pNodeTransformer must not be null");
    Objects.requireNonNull(pEdgeTransformer, "pEdgeTransformer must not be null");

    CfaBuilder cfaBuilder = new CfaBuilder(pMutableGraph, pNodeTransformer, pEdgeTransformer);

    return cfaBuilder.createCfa(pConfiguration, pLogger, pOriginalCfa);
  }

  /**
   * Returns a new CFA that represents the specified CFA with substituted AST-nodes.
   *
   * <p>The substitution of AST-nodes is specified by the substitution function that maps a CFA-edge
   * and its AST-node to the substituted AST-node for the edge.
   *
   * <p>The original CFA is not modified.
   *
   * @param pConfiguration the configuration that was used to create the original CFA
   * @param pLogger the logger to use
   * @param pCfa the original CFA
   * @param pSubstitutionFunction {@code CFA-edge, original AST-node --> substituted AST-node}
   * @return a new CFA that represents the specified CFA with substituted AST-nodes
   * @throws NullPointerException if any of the parameters is {@code null}
   */
  public static CFA substituteAstNodes(
      Configuration pConfiguration,
      LogManager pLogger,
      CFA pCfa,
      BiFunction<CFAEdge, CAstNode, CAstNode> pSubstitutionFunction) {

    Objects.requireNonNull(pConfiguration, "pConfiguration must not be null");
    Objects.requireNonNull(pLogger, "pLogger must not be null");
    Objects.requireNonNull(pCfa, "pCfa must not be null");
    Objects.requireNonNull(pSubstitutionFunction, "pSubstitutionFunction must not be null");

    MutableGraph<CFANode, CFAEdge> mutableGraph = createMutableGraph(pCfa);
    CCfaNodeTransformer nodeTransformer = CCfaNodeTransformer.DEFAULT;
    CCfaEdgeTransformer edgeTransformer =
        CCfaEdgeTransformer.forAstTransformer(pSubstitutionFunction);

    return createCfa(pConfiguration, pLogger, pCfa, mutableGraph, nodeTransformer, edgeTransformer);
  }

  private static final class CfaMutableGraph extends MutableGraph<CFANode, CFAEdge> {

    private final Map<CFANode, Node<CFANode, CFAEdge>> cfaNodeToMutableNodeMap;

    private CfaMutableGraph() {
      cfaNodeToMutableNodeMap = new HashMap<>();
    }

    @Override
    public Optional<Node<CFANode, CFAEdge>> getNode(CFANode pCfaNode) {

      Objects.requireNonNull(pCfaNode, "pCfaNode must not be null");

      return Optional.ofNullable(cfaNodeToMutableNodeMap.get(pCfaNode));
    }

    @Override
    public Node<CFANode, CFAEdge> wrapNode(CFANode pCfaNode) {

      Objects.requireNonNull(pCfaNode, "pCfaNode must not be null");

      Node<CFANode, CFAEdge> mutableNode = super.wrapNode(pCfaNode);
      cfaNodeToMutableNodeMap.put(pCfaNode, mutableNode);

      return mutableNode;
    }
  }
  
  private static final class CfaBuilder {

    private final MutableGraph<CFANode, CFAEdge> mutableGraph;

    private final CCfaNodeTransformer nodeTransformer;
    private final CCfaEdgeTransformer edgeTransformer;

    private final Map<MutableGraph.Node<CFANode, CFAEdge>, CFANode> nodeToNewCfaNode;
    private final Map<MutableGraph.Edge<CFANode, CFAEdge>, CFAEdge> edgeToNewCfaEdge;

    private CfaBuilder(
        MutableGraph<CFANode, CFAEdge> pMutableGraph,
        CCfaNodeTransformer pNodeTransformer,
        CCfaEdgeTransformer pEdgeTransformer) {

      mutableGraph = pMutableGraph;

      nodeTransformer = pNodeTransformer;
      edgeTransformer = pEdgeTransformer;

      nodeToNewCfaNode = new HashMap<>();
      edgeToNewCfaEdge = new HashMap<>();
    }

    private CFANode newCfaNodeIfAbsent(MutableGraph.Node<CFANode, CFAEdge> pNode) {

      CFANode newCfaNode = nodeToNewCfaNode.get(pNode);
      if (newCfaNode != null) {
        return newCfaNode;
      }

      CFANode originalCfaNode = pNode.getWrappedNode();

      if (originalCfaNode instanceof CLabelNode) {
        newCfaNode = nodeTransformer.transformCLabelNode((CLabelNode) originalCfaNode);
      } else if (originalCfaNode instanceof CFunctionEntryNode) {
        CFunctionEntryNode originalCfaEntryNode = (CFunctionEntryNode) originalCfaNode;
        MutableGraph.Node<CFANode, CFAEdge> exitNode =
            mutableGraph.getNode(originalCfaEntryNode.getExitNode()).orElseThrow();
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
        MutableGraph.Edge<CFANode, CFAEdge> pEdge,
        CFANode pNewCfaPredecessorNode,
        CFANode pNewCfaSuccessorNode) {

      for (MutableGraph.Edge<CFANode, CFAEdge> leavingEdge :
          mutableGraph.iterateLeaving(pEdge.getPredecessorOrElseThrow())) {
        if (leavingEdge.getWrappedEdge() instanceof CFunctionCallEdge) {
          CFunctionEntryNode cfaEntryNode =
              (CFunctionEntryNode) nodeToNewCfaNode.get(leavingEdge.getSuccessorOrElseThrow());
          return edgeTransformer.transformCFunctionSummaryEdge(
              (CFunctionSummaryEdge) pEdge.getWrappedEdge(),
              pNewCfaPredecessorNode,
              pNewCfaSuccessorNode,
              cfaEntryNode);
        }
      }

      throw new IllegalStateException("Missing function call edge for summary edge: " + pEdge);
    }

    private CFunctionCallEdge newCFunctionCallEdge(
        MutableGraph.Edge<CFANode, CFAEdge> pEdge,
        CFANode pNewCfaPredecessorNode,
        CFANode pNewCfaSuccessorNode) {
      for (MutableGraph.Edge<CFANode, CFAEdge> summaryEdge :
          mutableGraph.iterateLeaving(pEdge.getPredecessorOrElseThrow())) {
        if (summaryEdge.getWrappedEdge() instanceof CFunctionSummaryEdge) {
          CFunctionSummaryEdge cfaSummaryEdge =
              (CFunctionSummaryEdge) newCfaEdgeIfAbsent(summaryEdge, true);
          return edgeTransformer.transformCFunctionCallEdge(
              (CFunctionCallEdge) pEdge.getWrappedEdge(),
              pNewCfaPredecessorNode,
              (CFunctionEntryNode) pNewCfaSuccessorNode,
              cfaSummaryEdge);
        }
      }

      throw new IllegalStateException("Missing summary edge for function call edge: " + pEdge);
    }

    private CFunctionReturnEdge newCFunctionReturnEdge(
        MutableGraph.Edge<CFANode, CFAEdge> pEdge,
        CFANode pNewCfaPredecessorNode,
        CFANode pNewCfaSuccessorNode) {
      for (MutableGraph.Edge<CFANode, CFAEdge> summaryEdge :
          mutableGraph.iterateEntering(pEdge.getSuccessorOrElseThrow())) {
        if (summaryEdge.getWrappedEdge() instanceof CFunctionSummaryEdge) {
          CFunctionSummaryEdge cfaSummaryEdge =
              (CFunctionSummaryEdge) newCfaEdgeIfAbsent(summaryEdge, true);
          return edgeTransformer.transformCFunctionReturnEdge(
              (CFunctionReturnEdge) pEdge.getWrappedEdge(),
              (FunctionExitNode) pNewCfaPredecessorNode,
              pNewCfaSuccessorNode,
              cfaSummaryEdge);
        }
      }

      throw new IllegalStateException("Missing summary edge for function return edge: " + pEdge);
    }

    private CFAEdge newCfaEdgeIfAbsent(
        MutableGraph.Edge<CFANode, CFAEdge> pEdge, boolean pAllowInterprocedural) {

      CFAEdge newCfaEdge = edgeToNewCfaEdge.get(pEdge);
      if (newCfaEdge != null) {
        return newCfaEdge;
      }

      CFANode cfaPredecessorNode = nodeToNewCfaNode.get(pEdge.getPredecessorOrElseThrow());
      CFANode cfaSuccessorNode = nodeToNewCfaNode.get(pEdge.getSuccessorOrElseThrow());

      CFAEdge originalCfaEdge = pEdge.getWrappedEdge();

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

      MutableGraph.Node<CFANode, CFAEdge> mainEntryNode =
          mutableGraph.getNode(pOriginalCfa.getMainFunction()).orElseThrow();

      NavigableMap<String, FunctionEntryNode> newCfaFunctions = new TreeMap<>();
      TreeMultimap<String, CFANode> newCfaNodes = TreeMultimap.create();

      Set<MutableGraph.Node<CFANode, CFAEdge>> waitlisted =
          new HashSet<>(ImmutableList.of(mainEntryNode));
      Deque<MutableGraph.Node<CFANode, CFAEdge>> waitlist =
          new ArrayDeque<>(ImmutableList.of(mainEntryNode));

      while (!waitlist.isEmpty()) {

        MutableGraph.Node<CFANode, CFAEdge> currentNode = waitlist.remove();
        CFANode newCfaNode = newCfaNodeIfAbsent(currentNode);
        String functionName = newCfaNode.getFunction().getQualifiedName();

        if (newCfaNode instanceof FunctionEntryNode) {
          newCfaFunctions.put(functionName, (FunctionEntryNode) newCfaNode);
        }

        newCfaNodes.put(functionName, newCfaNode);

        for (MutableGraph.Edge<CFANode, CFAEdge> leavingEdge :
            mutableGraph.iterateLeaving(currentNode)) {
          MutableGraph.Node<CFANode, CFAEdge> successorNode = leavingEdge.getSuccessorOrElseThrow();
          if (waitlisted.add(successorNode)) {
            waitlist.add(successorNode);
          }
        }

        for (MutableGraph.Edge<CFANode, CFAEdge> enteringEdge :
            mutableGraph.iterateEntering(currentNode)) {
          MutableGraph.Node<CFANode, CFAEdge> predecessorNode =
              enteringEdge.getPredecessorOrElseThrow();
          if (waitlisted.add(predecessorNode)) {
            waitlist.add(predecessorNode);
          }
        }
      }

      // don't create create function call, return and summary edges
      for (MutableGraph.Node<CFANode, CFAEdge> currentNode : nodeToNewCfaNode.keySet()) {
        for (MutableGraph.Edge<CFANode, CFAEdge> edge : mutableGraph.iterateLeaving(currentNode)) {
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
      for (MutableGraph.Node<CFANode, CFAEdge> currentNode : nodeToNewCfaNode.keySet()) {
        for (MutableGraph.Edge<CFANode, CFAEdge> edge : mutableGraph.iterateLeaving(currentNode)) {
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
