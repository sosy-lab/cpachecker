// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFAReversePostorder;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
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
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassificationBuilder;

/** An instance of this class can be used to modify specific parts of a control flow graph (CFA). */
public class CCfaTransformer {

  private final Configuration configuration;
  private final LogManager logger;

  private final CFA originalCfa;
  private final Map<CFANode, Node> oldCfaNodeToNodeMap;

  private CCfaTransformer(
      Configuration pConfiguration,
      LogManager pLogger,
      CFA pOriginalCfa,
      Map<CFANode, Node> pNodeMap) {

    configuration = pConfiguration;
    logger = pLogger;

    originalCfa = pOriginalCfa;
    oldCfaNodeToNodeMap = pNodeMap;
  }

  public static CCfaTransformer createTransformer(
      Configuration pConfiguration, LogManager pLogger, CFA pCfa) {

    Objects.requireNonNull(pConfiguration, "pConfiguration must not be null");
    Objects.requireNonNull(pLogger, "pLogger must not be null");
    Objects.requireNonNull(pCfa, "pCfa must not be null");

    Map<CFANode, Node> nodeMap = new HashMap<>();

    for (CFANode cfaNode : pCfa.getAllNodes()) {
      nodeMap.put(cfaNode, new Node(cfaNode));
      if (cfaNode instanceof FunctionEntryNode) {
        FunctionExitNode functionExitNode = ((FunctionEntryNode) cfaNode).getExitNode();
        nodeMap.put(functionExitNode, new Node(functionExitNode));
      }
    }

    for (CFANode cfaNode : pCfa.getAllNodes()) {
      Node predecessor = nodeMap.get(cfaNode);
      for (CFAEdge cfaEdge : CFAUtils.allLeavingEdges(cfaNode)) {
        Node successor = nodeMap.get(cfaEdge.getSuccessor());
        Edge edge = new Edge(cfaEdge);
        attachEntering(successor, edge);
        attachLeaving(predecessor, edge);
      }
    }

    return new CCfaTransformer(pConfiguration, pLogger, pCfa, nodeMap);
  }

  public static Node createNode(CFANode pOldCfaNode) {

    Objects.requireNonNull(pOldCfaNode, "pOldCfaNode must not be null");

    return new Node(pOldCfaNode);
  }

  public static Edge createEdge(CFAEdge pOldCfaEdge) {

    Objects.requireNonNull(pOldCfaEdge, "pOldCfaEdge must not be null");

    return new Edge(pOldCfaEdge);
  }

  public static void attachEntering(Node pNode, Edge pEdge) {

    Objects.requireNonNull(pNode, "pNode must not be null");
    Objects.requireNonNull(pEdge, "pEdge must not be null");
    Preconditions.checkArgument(
        !pEdge.getSuccessor().isPresent(),
        "Cannot attach entering edge that already has a successor");

    pEdge.setSuccessor(Optional.of(pNode));
    pNode.addEnteringEdge(pEdge);
  }

  public static void detachEntering(Node pNode, Edge pEdge) {

    Objects.requireNonNull(pNode, "pNode must not be null");
    Objects.requireNonNull(pEdge, "pEdge must not be null");
    Preconditions.checkArgument(
        !pEdge.getSuccessor().isEmpty(), "Cannot detach entering edge that has no successor");

    pEdge.setSuccessor(Optional.empty());
    pNode.removeEnteringEdge(pEdge);
  }

  public static void attachLeaving(Node pNode, Edge pEdge) {

    Objects.requireNonNull(pNode, "pNode must not be null");
    Objects.requireNonNull(pEdge, "pEdge must not be null");
    Preconditions.checkArgument(
        !pEdge.getPredecessor().isPresent(),
        "Cannot attach leaving edge that already has a predecessor");

    pEdge.setPredecessor(Optional.of(pNode));
    pNode.addLeavingEdge(pEdge);
  }

  public static void detachLeaving(Node pNode, Edge pEdge) {

    Objects.requireNonNull(pNode, "pNode must not be null");
    Objects.requireNonNull(pEdge, "pEdge must not be null");
    Preconditions.checkArgument(
        !pEdge.getPredecessor().isEmpty(), "Cannot detach leaving edge that has no predecessor");

    pEdge.setPredecessor(Optional.empty());
    pNode.removeLeavingEdge(pEdge);
  }

  public static void detachAll(Edge pEdge) {

    Objects.requireNonNull(pEdge, "pEdge must not be null");

    Optional<Node> predecessor = pEdge.getPredecessor();
    if (predecessor.isPresent()) {
      detachLeaving(predecessor.orElseThrow(), pEdge);
    }

    Optional<Node> successor = pEdge.getSuccessor();
    if (successor.isPresent()) {
      detachEntering(successor.orElseThrow(), pEdge);
    }
  }

  public Optional<Node> getNode(CFANode pCfaNode) {

    Objects.requireNonNull(pCfaNode, "pCfaNode must not be null");

    return Optional.ofNullable(oldCfaNodeToNodeMap.get(pCfaNode));
  }

  public CFA createCfa(NodeTransformer pNodeTransformer, EdgeTransformer pEdgeTransformer) {

    Objects.requireNonNull(pNodeTransformer, "pNodeTransformer must not be null");
    Objects.requireNonNull(pEdgeTransformer, "pEdgeTransformer must not be null");

    CfaBuilder cfaBuilder = new CfaBuilder(oldCfaNodeToNodeMap, pNodeTransformer, pEdgeTransformer);

    return cfaBuilder.createCfa(configuration, logger, originalCfa);
  }

  public static final class Node {

    private final CFANode oldCfaNode;
    private final List<Edge> enteringEdges;
    private final List<Edge> leavingEdges;

    private Node(CFANode pOldCfaNode) {

      oldCfaNode = pOldCfaNode;

      enteringEdges = new ArrayList<>();
      leavingEdges = new ArrayList<>();
    }

    public CFANode getOldCfaNode() {
      return oldCfaNode;
    }

    /**
     *
     *
     * <pre>{@code
     * Before:
     * --- a ---> [this] --- b ---->
     *
     * After:
     * --- a ---> [pNewNode] --- pEdge ---> [this] --- b ---->
     *
     * }</pre>
     */
    public void splitAndInsertEntering(Edge pEdge, Node pNewNode) {

      for (Iterator<CCfaTransformer.Edge> iterator = newEnteringIterator(); iterator.hasNext(); ) {
        CCfaTransformer.Edge enteringEdge = iterator.next();
        iterator.remove();
        CCfaTransformer.attachEntering(pNewNode, enteringEdge);
      }

      CCfaTransformer.attachEntering(this, pEdge);
      CCfaTransformer.attachLeaving(pNewNode, pEdge);
    }

    /**
     *
     *
     * <pre>{@code
     * Before:
     * --- a ---> [this] --- b ---->
     *
     * After:
     * --- a ---> [this] --- pEdge ---> [pNewNode] --- b ---->
     *
     * }</pre>
     */
    public void splitAndInsertLeaving(Edge pEdge, Node pNewNode) {

      for (Iterator<CCfaTransformer.Edge> iterator = newLeavingIterator(); iterator.hasNext(); ) {
        CCfaTransformer.Edge leavingEdge = iterator.next();
        iterator.remove();
        CCfaTransformer.attachLeaving(pNewNode, leavingEdge);
      }

      CCfaTransformer.attachEntering(pNewNode, pEdge);
      CCfaTransformer.attachLeaving(this, pEdge);
    }

    private void addEnteringEdge(Edge pEdge) {
      enteringEdges.add(pEdge);
    }

    private void removeEnteringEdge(Edge pEdge) {
      enteringEdges.remove(pEdge);
    }

    public Iterator<Edge> newEnteringIterator() {
      return new Iterator<>() {

        private Iterator<Edge> delegate = enteringEdges.iterator();
        private Edge current = null;

        @Override
        public boolean hasNext() {
          return delegate.hasNext();
        }

        @Override
        public Edge next() {
          return current = delegate.next();
        }

        @Override
        public void remove() {
          delegate.remove();
          current.setSuccessor(Optional.empty());
        }
      };
    }

    public Iterable<Edge> iterateEntering() {
      return this::newEnteringIterator;
    }

    private void addLeavingEdge(Edge pEdge) {
      leavingEdges.add(pEdge);
    }

    private void removeLeavingEdge(Edge pEdge) {
      leavingEdges.remove(pEdge);
    }

    public Iterator<Edge> newLeavingIterator() {
      return new Iterator<>() {

        private Iterator<Edge> delegate = leavingEdges.iterator();
        private Edge current = null;

        @Override
        public boolean hasNext() {
          return delegate.hasNext();
        }

        @Override
        public Edge next() {
          return current = delegate.next();
        }

        @Override
        public void remove() {
          delegate.remove();
          current.setPredecessor(Optional.empty());
        }
      };
    }

    public Iterable<Edge> iterateLeaving() {
      return this::newLeavingIterator;
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[oldCfaNode=%s, enteringEdges=%s, leavingEdges=%s]",
          getClass(),
          oldCfaNode,
          enteringEdges,
          leavingEdges);
    }
  }

  public static final class Edge {

    private final CFAEdge oldCfaEdge;
    private Optional<Node> predecessor;
    private Optional<Node> successor;

    private Edge(CFAEdge pOldCfaEdge) {
      oldCfaEdge = pOldCfaEdge;
      predecessor = Optional.empty();
      successor = Optional.empty();
    }

    public CFAEdge getOldCfaEdge() {
      return oldCfaEdge;
    }

    public Optional<Node> getPredecessor() {
      return predecessor;
    }

    private Node getPredecessorOrElseThrow() {
      return getPredecessor()
          .orElseThrow(() -> new IllegalStateException("Missing edge predecessor: " + this));
    }

    private void setPredecessor(Optional<Node> pPredecessor) {
      predecessor = pPredecessor;
    }

    public Optional<Node> getSuccessor() {
      return successor;
    }

    private Node getSuccessorOrElseThrow() {
      return getSuccessor()
          .orElseThrow(() -> new IllegalStateException("Missing edge successor: " + this));
    }

    private void setSuccessor(Optional<Node> pSuccessor) {
      successor = pSuccessor;
    }

    @Override
    public String toString() {
      return String.format(
          Locale.ENGLISH,
          "%s[oldCfaEdge=%s, predecessor=%s, successor=%s]",
          getClass(),
          oldCfaEdge,
          predecessor,
          successor);
    }
  }

  public interface NodeTransformer {

    CFANode transformCfaNode(CFANode pOldCfaNode);

    CFATerminationNode transformCfaTerminationNode(CFATerminationNode pOldCfaTerminationNode);

    FunctionExitNode transformFunctionExitNode(FunctionExitNode pOldFunctionExitNode);

    CFunctionEntryNode transformCFunctionEntryNode(
        CFunctionEntryNode pOldCFunctionEntryNode, FunctionExitNode pNewFunctionExitNode);

    CFANode transformCLabelNode(CLabelNode pOldCLabelNode);
  }

  public interface EdgeTransformer {

    CFAEdge transformBlankEdge(
        BlankEdge pOldBlankEdge, CFANode pNewPredecessor, CFANode pNewSuccessor);

    CFAEdge transformCAssumeEdge(
        CAssumeEdge pOldCAssumeEdge, CFANode pNewPredecessor, CFANode pNewSuccessor);

    CFAEdge transformCDeclarationEdge(
        CDeclarationEdge pOldCDeclarationEdge, CFANode pNewPredecessor, CFANode pNewSuccessor);

    CFAEdge transformCStatementEdge(
        CStatementEdge pOldCStatementEdge, CFANode pNewPredecessor, CFANode pNewSuccessor);

    CFunctionCallEdge transformCFunctionCallEdge(
        CFunctionCallEdge pOldCFunctionCallEdge,
        CFANode pNewPredecessor,
        CFunctionEntryNode pNewSuccessor,
        CFunctionSummaryEdge pNewCFunctionSummaryEdge);

    CFunctionReturnEdge transformCFunctionReturnEdge(
        CFunctionReturnEdge pOldCFunctionReturnEdge,
        FunctionExitNode pNewPredecessor,
        CFANode pNewSuccessor,
        CFunctionSummaryEdge pNewCFunctionSummaryEdge);

    CFunctionSummaryEdge transformCFunctionSummaryEdge(
        CFunctionSummaryEdge pOldCFunctionSummaryEdge,
        CFANode pNewPredecessor,
        CFANode pNewSuccessor,
        CFunctionEntryNode pNewCFunctionEntryNode);

    CReturnStatementEdge transformCReturnStatementEdge(
        CReturnStatementEdge pOldCReturnStatementEdge,
        CFANode pNewPredecessor,
        FunctionExitNode pNewSuccessor);

    CFunctionSummaryStatementEdge transformCFunctionSummaryStatementEdge(
        CFunctionSummaryStatementEdge pOldCFunctionSummaryStatementEdge,
        CFANode pNewPredecessor,
        CFANode pNewSuccessor);
  }

  private static final class CfaBuilder {

    private final Map<CFANode, Node> oldCfaNodeToNode;

    private final NodeTransformer nodeTransformer;
    private final EdgeTransformer edgeTransformer;

    private final Map<Node, CFANode> nodeToNewCfaNode;
    private final Map<Edge, CFAEdge> edgeToNewCfaEdge;

    private CfaBuilder(
        Map<CFANode, Node> pOldCfaNodeToNode,
        NodeTransformer pNodeTransformer,
        EdgeTransformer pEdgeTransformer) {

      oldCfaNodeToNode = pOldCfaNodeToNode;

      nodeTransformer = pNodeTransformer;
      edgeTransformer = pEdgeTransformer;

      nodeToNewCfaNode = new HashMap<>();
      edgeToNewCfaEdge = new HashMap<>();
    }

    private CFANode newCfaNodeIfAbsent(Node pNode) {

      CFANode newCfaNode = nodeToNewCfaNode.get(pNode);
      if (newCfaNode != null) {
        return newCfaNode;
      }

      CFANode oldCfaNode = pNode.getOldCfaNode();

      if (oldCfaNode instanceof CLabelNode) {
        newCfaNode = nodeTransformer.transformCLabelNode((CLabelNode) oldCfaNode);
      } else if (oldCfaNode instanceof CFunctionEntryNode) {
        CFunctionEntryNode oldCfaEntryNode = (CFunctionEntryNode) oldCfaNode;
        Node exitNode = oldCfaNodeToNode.get(oldCfaEntryNode.getExitNode());
        FunctionExitNode newCfaExitNode = (FunctionExitNode) newCfaNodeIfAbsent(exitNode);
        newCfaNode =
            nodeTransformer.transformCFunctionEntryNode(
                (CFunctionEntryNode) oldCfaNode, newCfaExitNode);
        newCfaExitNode.setEntryNode((CFunctionEntryNode) newCfaNode);
      } else if (oldCfaNode instanceof FunctionExitNode) {
        newCfaNode = nodeTransformer.transformFunctionExitNode((FunctionExitNode) oldCfaNode);
      } else if (oldCfaNode instanceof CFATerminationNode) {
        newCfaNode = nodeTransformer.transformCfaTerminationNode((CFATerminationNode) oldCfaNode);
      } else {
        newCfaNode = nodeTransformer.transformCfaNode(oldCfaNode);
      }

      nodeToNewCfaNode.put(pNode, newCfaNode);

      return newCfaNode;
    }

    private CFunctionSummaryEdge newCFunctionSummaryEdge(
        Edge pEdge, CFANode pNewCfaPredecessorNode, CFANode pNewCfaSuccessorNode) {

      for (Edge leavingEdge : pEdge.getPredecessorOrElseThrow().iterateLeaving()) {
        if (leavingEdge.getOldCfaEdge() instanceof CFunctionCallEdge) {
          CFunctionEntryNode cfaEntryNode =
              (CFunctionEntryNode) nodeToNewCfaNode.get(leavingEdge.getSuccessorOrElseThrow());
          return edgeTransformer.transformCFunctionSummaryEdge(
              (CFunctionSummaryEdge) pEdge.getOldCfaEdge(),
              pNewCfaPredecessorNode,
              pNewCfaSuccessorNode,
              cfaEntryNode);
        }
      }

      throw new IllegalStateException("Missing function call edge for summary edge: " + pEdge);
    }

    private CFunctionCallEdge newCFunctionCallEdge(
        Edge pEdge, CFANode pNewCfaPredecessorNode, CFANode pNewCfaSuccessorNode) {
      for (Edge summaryEdge : pEdge.getPredecessorOrElseThrow().iterateLeaving()) {
        if (summaryEdge.getOldCfaEdge() instanceof CFunctionSummaryEdge) {
          CFunctionSummaryEdge cfaSummaryEdge =
              (CFunctionSummaryEdge) newCfaEdgeIfAbsent(summaryEdge, true);
          return edgeTransformer.transformCFunctionCallEdge(
              (CFunctionCallEdge) pEdge.getOldCfaEdge(),
              pNewCfaPredecessorNode,
              (CFunctionEntryNode) pNewCfaSuccessorNode,
              cfaSummaryEdge);
        }
      }

      throw new IllegalStateException("Missing summary edge for function call edge: " + pEdge);
    }

    private CFunctionReturnEdge newCFunctionReturnEdge(
        Edge pEdge, CFANode pNewCfaPredecessorNode, CFANode pNewCfaSuccessorNode) {
      for (Edge summaryEdge : pEdge.getSuccessorOrElseThrow().iterateEntering()) {
        if (summaryEdge.getOldCfaEdge() instanceof CFunctionSummaryEdge) {
          CFunctionSummaryEdge cfaSummaryEdge =
              (CFunctionSummaryEdge) newCfaEdgeIfAbsent(summaryEdge, true);
          return edgeTransformer.transformCFunctionReturnEdge(
              (CFunctionReturnEdge) pEdge.getOldCfaEdge(),
              (FunctionExitNode) pNewCfaPredecessorNode,
              pNewCfaSuccessorNode,
              cfaSummaryEdge);
        }
      }

      throw new IllegalStateException("Missing summary edge for function return edge: " + pEdge);
    }

    private CFAEdge newCfaEdgeIfAbsent(Edge pEdge, boolean pAllowInterprocedural) {

      CFAEdge newCfaEdge = edgeToNewCfaEdge.get(pEdge);
      if (newCfaEdge != null) {
        return newCfaEdge;
      }

      CFANode cfaPredecessorNode = nodeToNewCfaNode.get(pEdge.getPredecessorOrElseThrow());
      CFANode cfaSuccessorNode = nodeToNewCfaNode.get(pEdge.getSuccessorOrElseThrow());

      CFAEdge oldCfaEdge = pEdge.getOldCfaEdge();

      if (oldCfaEdge instanceof BlankEdge) {
        newCfaEdge =
            edgeTransformer.transformBlankEdge(
                (BlankEdge) oldCfaEdge, cfaPredecessorNode, cfaSuccessorNode);
      } else if (oldCfaEdge instanceof CAssumeEdge) {
        newCfaEdge =
            edgeTransformer.transformCAssumeEdge(
                (CAssumeEdge) oldCfaEdge, cfaPredecessorNode, cfaSuccessorNode);
      } else if (oldCfaEdge instanceof CDeclarationEdge) {
        newCfaEdge =
            edgeTransformer.transformCDeclarationEdge(
                (CDeclarationEdge) oldCfaEdge, cfaPredecessorNode, cfaSuccessorNode);
      } else if (oldCfaEdge instanceof CFunctionSummaryStatementEdge) {
        if (pAllowInterprocedural) {
          newCfaEdge =
              edgeTransformer.transformCFunctionSummaryStatementEdge(
                  (CFunctionSummaryStatementEdge) oldCfaEdge, cfaPredecessorNode, cfaSuccessorNode);
        } else {
          newCfaEdge =
              new SummaryPlaceholderEdge(
                  "",
                  oldCfaEdge.getFileLocation(),
                  cfaPredecessorNode,
                  cfaSuccessorNode,
                  "summary-placeholder-edge");
        }
      } else if (oldCfaEdge instanceof CReturnStatementEdge) {
        newCfaEdge =
            edgeTransformer.transformCReturnStatementEdge(
                (CReturnStatementEdge) oldCfaEdge,
                cfaPredecessorNode,
                (FunctionExitNode) cfaSuccessorNode);
      } else if (oldCfaEdge instanceof CFunctionSummaryEdge) {
        if (pAllowInterprocedural) {
          newCfaEdge = newCFunctionSummaryEdge(pEdge, cfaPredecessorNode, cfaSuccessorNode);
        } else {
          newCfaEdge =
              new SummaryPlaceholderEdge(
                  "",
                  oldCfaEdge.getFileLocation(),
                  cfaPredecessorNode,
                  cfaSuccessorNode,
                  "summary-placeholder-edge");
        }
      } else if (oldCfaEdge instanceof CFunctionReturnEdge) {
        if (pAllowInterprocedural) {
          newCfaEdge = newCFunctionReturnEdge(pEdge, cfaPredecessorNode, cfaSuccessorNode);
        }
      } else if (oldCfaEdge instanceof CFunctionCallEdge) {
        if (pAllowInterprocedural) {
          newCfaEdge = newCFunctionCallEdge(pEdge, cfaPredecessorNode, cfaSuccessorNode);
        }
      } else if (oldCfaEdge instanceof CStatementEdge) {
        newCfaEdge =
            edgeTransformer.transformCStatementEdge(
                (CStatementEdge) oldCfaEdge, cfaPredecessorNode, cfaSuccessorNode);
      } else {
        throw new AssertionError("Unknown CFA edge type: " + oldCfaEdge.getClass());
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

      Node mainEntryNode = oldCfaNodeToNode.get(pOriginalCfa.getMainFunction());

      NavigableMap<String, FunctionEntryNode> newCfaFunctions = new TreeMap<>();
      TreeMultimap<String, CFANode> newCfaNodes = TreeMultimap.create();

      Set<Node> waitlisted = new HashSet<>(ImmutableList.of(mainEntryNode));
      Deque<Node> waitlist = new ArrayDeque<>(ImmutableList.of(mainEntryNode));

      while (!waitlist.isEmpty()) {

        Node currentNode = waitlist.remove();
        CFANode newCfaNode = newCfaNodeIfAbsent(currentNode);
        String functionName = newCfaNode.getFunction().getQualifiedName();

        if (newCfaNode instanceof FunctionEntryNode) {
          newCfaFunctions.put(functionName, (FunctionEntryNode) newCfaNode);
        }

        newCfaNodes.put(functionName, newCfaNode);

        for (Edge leavingEdge : currentNode.iterateLeaving()) {
          Node successorNode = leavingEdge.getSuccessorOrElseThrow();
          if (waitlisted.add(successorNode)) {
            waitlist.add(successorNode);
          }
        }

        for (Edge enteringEdge : currentNode.iterateEntering()) {
          Node predecessorNode = enteringEdge.getPredecessorOrElseThrow();
          if (waitlisted.add(predecessorNode)) {
            waitlist.add(predecessorNode);
          }
        }
      }

      // don't create create function call, return and summary edges
      for (Node currentNode : nodeToNewCfaNode.keySet()) {
        for (Edge edge : currentNode.iterateLeaving()) {
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

      try {
        newMutableCfa.setLoopStructure(LoopStructure.getLoopStructure(newMutableCfa));
      } catch (ParserException ex) {
        pLogger.log(Level.WARNING, ex);
      }

      // create supergraph including function call, return and summary edges
      removeSummaryPlaceholderEdges();
      for (Node currentNode : nodeToNewCfaNode.keySet()) {
        for (Edge edge : currentNode.iterateLeaving()) {
          newCfaEdgeIfAbsent(edge, /* allow interprocedural edges = */ true);
        }
      }

      Optional<VariableClassification> variableClassification =
          createVariableClassification(pConfiguration, pLogger, newMutableCfa);

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
