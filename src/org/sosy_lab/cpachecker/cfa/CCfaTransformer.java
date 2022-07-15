// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.TreeMultimap;
import com.google.common.graph.EndpointPair;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdgeVisitor;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassificationBuilder;

public final class CCfaTransformer {

  private CCfaTransformer() {}

  public static CFA createCfa(
      Configuration pConfiguration,
      LogManager pLogger,
      CFA pOriginalCfa,
      CfaMutableNetwork pCfaMutableNetwork,
      BiFunction<CFAEdge, CAstNode, CAstNode> pCfaEdgeAstNodeSubstitution,
      BiFunction<CFANode, CAstNode, CAstNode> pCfaNodeAstNodeSubstitution) {

    checkNotNull(pConfiguration);
    checkNotNull(pLogger);
    checkNotNull(pOriginalCfa);
    checkNotNull(pCfaMutableNetwork);
    checkNotNull(pCfaEdgeAstNodeSubstitution);
    checkNotNull(pCfaNodeAstNodeSubstitution);

    CfaBuilder cfaBuilder =
        new CfaBuilder(
            pCfaMutableNetwork, pCfaEdgeAstNodeSubstitution, pCfaNodeAstNodeSubstitution);

    return cfaBuilder.createCfa(pConfiguration, pLogger, pOriginalCfa);
  }

  public static CFA createCfa(
      Configuration pConfiguration,
      LogManager pLogger,
      CFA pOriginalCfa,
      CfaMutableNetwork pCfaMutableNetwork,
      BiFunction<CFAEdge, CAstNode, CAstNode> pCfaEdgeAstNodeSubstitution) {
    return createCfa(
        pConfiguration,
        pLogger,
        pOriginalCfa,
        pCfaMutableNetwork,
        pCfaEdgeAstNodeSubstitution,
        (cfaNode, astNode) -> astNode);
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

    checkNotNull(pConfiguration);
    checkNotNull(pLogger);
    checkNotNull(pCfa);
    checkNotNull(pSubstitutionFunction);

    CfaMutableNetwork mutableGraph = CfaMutableNetwork.of(pCfa);

    return createCfa(pConfiguration, pLogger, pCfa, mutableGraph, pSubstitutionFunction);
  }

  private static final class CfaBuilder {

    private final CfaMutableNetwork graph;

    private final BiFunction<CFAEdge, CAstNode, CAstNode> cfaEdgeAstNodeSubstitution;
    private final BiFunction<CFANode, CAstNode, CAstNode> cfaNodeAstNodeSubstitution;

    private final Map<CFANode, CFANode> oldNodeToNewNode;
    private final Map<CFAEdge, CFAEdge> oldEdgeToNewEdge;

    private CfaBuilder(
        CfaMutableNetwork pCfaMutableNetwork,
        BiFunction<CFAEdge, CAstNode, CAstNode> pCfaEdgeAstNodeSubstitution,
        BiFunction<CFANode, CAstNode, CAstNode> pCfaNodeAstNodeSubstitution) {

      graph = pCfaMutableNetwork;

      cfaEdgeAstNodeSubstitution = pCfaEdgeAstNodeSubstitution;
      cfaNodeAstNodeSubstitution = pCfaNodeAstNodeSubstitution;

      oldNodeToNewNode = new HashMap<>();
      oldEdgeToNewEdge = new HashMap<>();
    }

    private CFunctionDeclaration newFunctionDeclaration(CFANode pOldNode) {
      return (CFunctionDeclaration)
          cfaNodeAstNodeSubstitution.apply(pOldNode, (CFunctionDeclaration) pOldNode.getFunction());
    }

    private CFALabelNode newCfaLabelNode(CFALabelNode pOldNode) {
      return new CFALabelNode(newFunctionDeclaration(pOldNode), pOldNode.getLabel());
    }

    private CFunctionEntryNode newCFunctionEntryNode(CFunctionEntryNode pOldNode) {

      // FIXME: don't rely on FunctionEntryNode#getExitNode
      // all connections between CFANodes/CFAEdges should be easily modifiable via CfaMutableNetwork
      CFANode oldExitNode = pOldNode.getExitNode();
      FunctionExitNode newExitNode = (FunctionExitNode) toNew(oldExitNode);

      Optional<CVariableDeclaration> oldReturnVariable = pOldNode.getReturnVariable();
      Optional<CVariableDeclaration> newReturnVariable;
      if (oldReturnVariable.isPresent()) {
        newReturnVariable =
            Optional.ofNullable(
                (CVariableDeclaration)
                    cfaNodeAstNodeSubstitution.apply(pOldNode, oldReturnVariable.orElseThrow()));
      } else {
        newReturnVariable = Optional.empty();
      }

      CFunctionEntryNode newEntryNode =
          new CFunctionEntryNode(
              pOldNode.getFileLocation(),
              newFunctionDeclaration(pOldNode),
              newExitNode,
              newReturnVariable);
      newExitNode.setEntryNode(newEntryNode);

      return newEntryNode;
    }

    private FunctionExitNode newFunctionExitNode(FunctionExitNode pOldNode) {
      return new FunctionExitNode(newFunctionDeclaration(pOldNode));
    }

    private CFATerminationNode newCfaTerminationNode(CFATerminationNode pOldNode) {
      return new CFATerminationNode(newFunctionDeclaration(pOldNode));
    }

    private CFANode newCfaNode(CFANode pOldNode) {
      return new CFANode(newFunctionDeclaration(pOldNode));
    }

    private CFANode toNew(CFANode pOldNode) {

      CFANode newNode = oldNodeToNewNode.get(pOldNode);
      if (newNode != null) {
        return newNode;
      }

      if (pOldNode instanceof CFALabelNode) {
        newNode = newCfaLabelNode((CFALabelNode) pOldNode);
      } else if (pOldNode instanceof CFunctionEntryNode) {
        newNode = newCFunctionEntryNode((CFunctionEntryNode) pOldNode);
      } else if (pOldNode instanceof FunctionExitNode) {
        newNode = newFunctionExitNode((FunctionExitNode) pOldNode);
      } else if (pOldNode instanceof CFATerminationNode) {
        newNode = newCfaTerminationNode((CFATerminationNode) pOldNode);
      } else {
        newNode = newCfaNode(pOldNode);
      }

      oldNodeToNewNode.put(pOldNode, newNode);

      return newNode;
    }

    private CAstNode substituteAst(CFAEdge pCfaEdge, CAstNode pCAstNode) {
      return cfaEdgeAstNodeSubstitution.apply(pCfaEdge, pCAstNode);
    }

    private CFunctionSummaryEdge newCFunctionSummaryEdge(
        CFunctionSummaryEdge pOldSummaryEdge, CFANode pNewNodeU, CFANode pNewNodeV) {

      CFANode oldSummaryEdgeNodeU = graph.incidentNodes(pOldSummaryEdge).nodeU();

      for (CFAEdge outEdge : graph.outEdges(oldSummaryEdgeNodeU)) {
        if (outEdge instanceof CFunctionCallEdge) {

          CFANode oldEntryNode = graph.incidentNodes(outEdge).nodeV();
          CFunctionEntryNode newEntryNode = (CFunctionEntryNode) toNew(oldEntryNode);

          CFunctionCall newFunctionCall =
              (CFunctionCall) substituteAst(pOldSummaryEdge, pOldSummaryEdge.getExpression());

          return new CFunctionSummaryEdge(
              pOldSummaryEdge.getRawStatement(),
              pOldSummaryEdge.getFileLocation(),
              pNewNodeU,
              pNewNodeV,
              newFunctionCall,
              newEntryNode);
        }
      }

      throw new IllegalStateException(
          "Missing function call edge for summary edge: " + pOldSummaryEdge);
    }

    private CFunctionCallEdge newCFunctionCallEdge(
        CFunctionCallEdge pOldCallEdge, CFANode pNewNodeU, CFANode pNewNodeV) {

      CFANode oldCallEdgeNodeU = graph.incidentNodes(pOldCallEdge).nodeU();

      for (CFAEdge outEdge : graph.outEdges(oldCallEdgeNodeU)) {
        if (outEdge instanceof CFunctionSummaryEdge) {

          CFunctionSummaryEdge newSummaryEdge = (CFunctionSummaryEdge) toNew(outEdge, true);

          return new CFunctionCallEdge(
              pOldCallEdge.getRawStatement(),
              pOldCallEdge.getFileLocation(),
              pNewNodeU,
              (CFunctionEntryNode) pNewNodeV,
              newSummaryEdge.getExpression(),
              newSummaryEdge);
        }
      }

      throw new IllegalStateException(
          "Missing summary edge for function call edge: " + pOldCallEdge);
    }

    private CFunctionReturnEdge newCFunctionReturnEdge(
        CFunctionReturnEdge pOldReturnEdge, CFANode pNewNodeU, CFANode pNewNodeV) {

      CFANode oldReturnEdgeNodeV = graph.incidentNodes(pOldReturnEdge).nodeV();

      for (CFAEdge inEdge : graph.inEdges(oldReturnEdgeNodeV)) {
        if (inEdge instanceof CFunctionSummaryEdge) {

          CFunctionSummaryEdge newSummaryEdge = (CFunctionSummaryEdge) toNew(inEdge, true);

          return new CFunctionReturnEdge(
              pOldReturnEdge.getFileLocation(),
              (FunctionExitNode) pNewNodeU,
              pNewNodeV,
              newSummaryEdge);
        }
      }

      throw new IllegalStateException(
          "Missing summary edge for function return edge: " + pOldReturnEdge);
    }

    private CFAEdge toNew(CFAEdge pOldEdge, boolean pBuildSupergraph) {

      @Nullable CFAEdge newEdge = oldEdgeToNewEdge.get(pOldEdge);
      if (newEdge != null) {
        return newEdge;
      }

      EndpointPair<CFANode> oldEndpoints = graph.incidentNodes(pOldEdge);
      CFANode newNodeU = toNew(oldEndpoints.nodeU());
      CFANode newNodeV = toNew(oldEndpoints.nodeV());

      CCfaEdgeVisitor<@Nullable CFAEdge, NoException> transformingEdgeVisitor =
          new CCfaEdgeVisitor<>() {

            @Override
            public CFAEdge visit(BlankEdge pBlankEdge) {
              return new BlankEdge(
                  pBlankEdge.getRawStatement(),
                  pBlankEdge.getFileLocation(),
                  newNodeU,
                  newNodeV,
                  pBlankEdge.getDescription());
            }

            @Override
            public CFAEdge visit(CAssumeEdge pCAssumeEdge) {

              CExpression newExpression =
                  (CExpression) substituteAst(pCAssumeEdge, pCAssumeEdge.getExpression());

              return new CAssumeEdge(
                  pCAssumeEdge.getRawStatement(),
                  pCAssumeEdge.getFileLocation(),
                  newNodeU,
                  newNodeV,
                  newExpression,
                  pCAssumeEdge.getTruthAssumption(),
                  pCAssumeEdge.isSwapped(),
                  pCAssumeEdge.isArtificialIntermediate());
            }

            @Override
            public CFAEdge visit(CDeclarationEdge pCDeclarationEdge) {

              CDeclaration newDeclaration =
                  (CDeclaration)
                      substituteAst(pCDeclarationEdge, pCDeclarationEdge.getDeclaration());

              return new CDeclarationEdge(
                  pCDeclarationEdge.getRawStatement(),
                  pCDeclarationEdge.getFileLocation(),
                  newNodeU,
                  newNodeV,
                  newDeclaration);
            }

            @Override
            public CFAEdge visit(CStatementEdge pCStatementEdge) {

              CStatement newStatement =
                  (CStatement) substituteAst(pCStatementEdge, pCStatementEdge.getStatement());

              return new CStatementEdge(
                  pCStatementEdge.getRawStatement(),
                  newStatement,
                  pCStatementEdge.getFileLocation(),
                  newNodeU,
                  newNodeV);
            }

            @Override
            public @Nullable CFAEdge visit(CFunctionCallEdge pCFunctionCallEdge) {
              if (pBuildSupergraph) {
                return newCFunctionCallEdge(pCFunctionCallEdge, newNodeU, newNodeV);
              } else {
                return null;
              }
            }

            @Override
            public @Nullable CFAEdge visit(CFunctionReturnEdge pCFunctionReturnEdge) {
              if (pBuildSupergraph) {
                return newCFunctionReturnEdge(pCFunctionReturnEdge, newNodeU, newNodeV);
              } else {
                return null;
              }
            }

            @Override
            public CFAEdge visit(CFunctionSummaryEdge pCFunctionSummaryEdge) {
              if (pBuildSupergraph) {
                return newCFunctionSummaryEdge(pCFunctionSummaryEdge, newNodeU, newNodeV);
              } else {
                return new SummaryPlaceholderEdge(
                    "",
                    pCFunctionSummaryEdge.getFileLocation(),
                    newNodeU,
                    newNodeV,
                    "summary-placeholder-edge");
              }
            }

            @Override
            public CFAEdge visit(CReturnStatementEdge pCReturnStatementEdge) {

              CReturnStatement newReturnStatement =
                  (CReturnStatement)
                      substituteAst(
                          pCReturnStatementEdge, pCReturnStatementEdge.getReturnStatement());

              return new CReturnStatementEdge(
                  pCReturnStatementEdge.getRawStatement(),
                  newReturnStatement,
                  pCReturnStatementEdge.getFileLocation(),
                  newNodeU,
                  (FunctionExitNode) newNodeV);
            }

            @Override
            public CFAEdge visit(CFunctionSummaryStatementEdge pCFunctionSummaryStatementEdge) {
              if (pBuildSupergraph) {

                CStatement newStatement =
                    (CStatement)
                        substituteAst(
                            pCFunctionSummaryStatementEdge,
                            pCFunctionSummaryStatementEdge.getStatement());
                CFunctionCall newFunctionCall =
                    (CFunctionCall)
                        substituteAst(
                            pCFunctionSummaryStatementEdge,
                            pCFunctionSummaryStatementEdge.getFunctionCall());

                return new CFunctionSummaryStatementEdge(
                    pCFunctionSummaryStatementEdge.getRawStatement(),
                    newStatement,
                    pCFunctionSummaryStatementEdge.getFileLocation(),
                    newNodeU,
                    newNodeV,
                    newFunctionCall,
                    pCFunctionSummaryStatementEdge.getFunctionName());
              } else {
                return new SummaryPlaceholderEdge(
                    "",
                    pCFunctionSummaryStatementEdge.getFileLocation(),
                    newNodeU,
                    newNodeV,
                    "summary-placeholder-edge");
              }
            }
          };

      newEdge = ((CCfaEdge) pOldEdge).accept(transformingEdgeVisitor);

      if (newEdge != null) {

        if (!(newEdge instanceof SummaryPlaceholderEdge)) {
          oldEdgeToNewEdge.put(pOldEdge, newEdge);
        }

        if (newEdge instanceof CFunctionSummaryEdge) {
          CFunctionSummaryEdge cfaSummaryEdge = (CFunctionSummaryEdge) newEdge;
          newNodeU.addLeavingSummaryEdge(cfaSummaryEdge);
          newNodeV.addEnteringSummaryEdge(cfaSummaryEdge);
        } else {
          newNodeU.addLeavingEdge(newEdge);
          newNodeV.addEnteringEdge(newEdge);
        }
      }

      return newEdge;
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

      for (CFANode newCfaNode : oldNodeToNewNode.values()) {
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

    private FunctionEntryNode determineMainFunctionEntryNode() {

      Set<CFANode> waitlisted = new HashSet<>();
      Deque<CFANode> waitlist = new ArrayDeque<>();

      for (CFANode node : graph.nodes()) {
        if (graph.inDegree(node) == 0) {
          waitlisted.add(node);
          waitlist.add(node);
        }
      }

      while (!waitlist.isEmpty()) {

        CFANode node = waitlist.remove();

        if (node instanceof FunctionEntryNode) {
          return (FunctionEntryNode) node;
        }

        for (CFANode adjacentNode : graph.adjacentNodes(node)) {
          if (waitlisted.add(adjacentNode)) {
            waitlist.add(adjacentNode);
          }
        }
      }

      throw new AssertionError("Unable to determine main function node");
    }

    private MutableCFA createUnconnectedFunctionCfa(CFA pOriginalCfa) {

      CFANode oldMainEntryNode = determineMainFunctionEntryNode();

      NavigableMap<String, FunctionEntryNode> newFunctions = new TreeMap<>();
      TreeMultimap<String, CFANode> newNodes = TreeMultimap.create();

      Set<CFANode> waitlisted = new HashSet<>(ImmutableList.of(oldMainEntryNode));
      Deque<CFANode> waitlist = new ArrayDeque<>(ImmutableList.of(oldMainEntryNode));

      while (!waitlist.isEmpty()) {

        CFANode oldNode = waitlist.remove();
        CFANode newNode = toNew(oldNode);
        String functionName = newNode.getFunction().getQualifiedName();

        if (newNode instanceof FunctionEntryNode) {
          newFunctions.put(functionName, (FunctionEntryNode) newNode);
        }

        newNodes.put(functionName, newNode);

        for (CFANode adjacentNode : graph.adjacentNodes(oldNode)) {
          if (waitlisted.add(adjacentNode)) {
            waitlist.add(adjacentNode);
          }
        }
      }

      for (CFANode oldNode : oldNodeToNewNode.keySet()) {
        for (CFAEdge outEdge : graph.outEdges(oldNode)) {
          // pBuildSupergraph == false
          toNew(outEdge, false);
        }
      }

      return new MutableCFA(
          pOriginalCfa.getMachineModel(),
          newFunctions,
          newNodes,
          (FunctionEntryNode) oldNodeToNewNode.get(oldMainEntryNode),
          pOriginalCfa.getFileNames(),
          pOriginalCfa.getLanguage());
    }

    private CFA createCfa(Configuration pConfiguration, LogManager pLogger, CFA pOriginalCfa) {

      MutableCFA newMutableCfa = createUnconnectedFunctionCfa(pOriginalCfa);

      newMutableCfa.getAllFunctionHeads().forEach(CFAReversePostorder::assignIds);

      if (pOriginalCfa.getLoopStructure().isPresent()) {
        try {
          newMutableCfa.setLoopStructure(LoopStructure.getLoopStructure(newMutableCfa));
        } catch (ParserException ex) {
          pLogger.log(Level.WARNING, ex);
        }
      }

      // create supergraph (includes call, return, and summary edges)
      removeSummaryPlaceholderEdges();
      for (CFANode oldNode : oldNodeToNewNode.keySet()) {
        for (CFAEdge outEdge : graph.outEdges(oldNode)) {
          // pBuildSupergraph == true
          toNew(outEdge, true);
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
