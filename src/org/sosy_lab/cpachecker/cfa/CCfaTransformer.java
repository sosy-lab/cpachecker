// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Equivalence;
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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdgeVisitor;
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
      MutableCfaNetwork pMutableCfaNetwork,
      BiFunction<CFAEdge, CAstNode, CAstNode> pAstNodeSubstitution) {

    checkNotNull(pConfiguration);
    checkNotNull(pLogger);
    checkNotNull(pOriginalCfa);
    checkNotNull(pMutableCfaNetwork);
    checkNotNull(pAstNodeSubstitution);

    CfaBuilder cfaBuilder =
        new CfaBuilder(pMutableCfaNetwork, CCfaNodeTransformer.DEFAULT, pAstNodeSubstitution);

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

    checkNotNull(pConfiguration);
    checkNotNull(pLogger);
    checkNotNull(pCfa);
    checkNotNull(pSubstitutionFunction);

    MutableCfaNetwork mutableGraph = MutableCfaNetwork.of(pCfa);

    return createCfa(pConfiguration, pLogger, pCfa, mutableGraph, pSubstitutionFunction);
  }
  
  private static final class CfaBuilder {

    private final MutableCfaNetwork graph;

    private final CCfaNodeTransformer nodeTransformer;
    private final BiFunction<CFAEdge, CAstNode, CAstNode> astNodeSubstitutionFunction;

    private final Map<CFANode, CFANode> oldNodeToNewNode;
    private final Map<Equivalence.Wrapper<CFAEdge>, Equivalence.Wrapper<CFAEdge>> oldEdgeToNewEdge;

    private CfaBuilder(
        MutableCfaNetwork pMutableCfaNetwork,
        CCfaNodeTransformer pNodeTransformer,
        BiFunction<CFAEdge, CAstNode, CAstNode> pAstNodeSubstitutionFunction) {

      graph = pMutableCfaNetwork;

      nodeTransformer = pNodeTransformer;
      astNodeSubstitutionFunction = pAstNodeSubstitutionFunction;

      oldNodeToNewNode = new HashMap<>();
      oldEdgeToNewEdge = new HashMap<>();
    }

    private CFANode toNew(CFANode pOldNode) {

      CFANode newNode = oldNodeToNewNode.get(pOldNode);
      if (newNode != null) {
        return newNode;
      }

      if (pOldNode instanceof CLabelNode) {
        newNode = nodeTransformer.transformCLabelNode((CLabelNode) pOldNode);
      } else if (pOldNode instanceof CFunctionEntryNode) {
        CFunctionEntryNode oldEntryNode = (CFunctionEntryNode) pOldNode;
        CFANode oldExitNode = oldEntryNode.getExitNode();
        FunctionExitNode newExitNode = (FunctionExitNode) toNew(oldExitNode);
        newNode =
            nodeTransformer.transformCFunctionEntryNode((CFunctionEntryNode) pOldNode, newExitNode);
        newExitNode.setEntryNode((CFunctionEntryNode) newNode);
      } else if (pOldNode instanceof FunctionExitNode) {
        newNode = nodeTransformer.transformFunctionExitNode((FunctionExitNode) pOldNode);
      } else if (pOldNode instanceof CFATerminationNode) {
        newNode = nodeTransformer.transformCfaTerminationNode((CFATerminationNode) pOldNode);
      } else {
        newNode = nodeTransformer.transformCfaNode(pOldNode);
      }

      oldNodeToNewNode.put(pOldNode, newNode);

      return newNode;
    }

    private CAstNode substituteAst(CFAEdge pCfaEdge, CAstNode pCAstNode) {
      return astNodeSubstitutionFunction.apply(pCfaEdge, pCAstNode);
    }

    private CFunctionSummaryEdge newCFunctionSummaryEdge(
        Equivalence.Wrapper<CFAEdge> pOldEdge, CFANode pNewNodeU, CFANode pNewNodeV) {

      CFunctionSummaryEdge oldSummaryEdge = (CFunctionSummaryEdge) pOldEdge.get();
      CFANode oldSummaryEdgeNodeU = graph.incidentNodes(pOldEdge).nodeU();

      for (Equivalence.Wrapper<CFAEdge> outEdge : graph.outEdges(oldSummaryEdgeNodeU)) {
        if (outEdge.get() instanceof CFunctionCallEdge) {

          CFANode oldEntryNode = graph.incidentNodes(outEdge).nodeV();
          CFunctionEntryNode newEntryNode = (CFunctionEntryNode) toNew(oldEntryNode);

          CFunctionCall newFunctionCall =
              (CFunctionCall) substituteAst(oldSummaryEdge, oldSummaryEdge.getExpression());

          return new CFunctionSummaryEdge(
              oldSummaryEdge.getRawStatement(),
              oldSummaryEdge.getFileLocation(),
              pNewNodeU,
              pNewNodeV,
              newFunctionCall,
              newEntryNode);
        }
      }

      throw new IllegalStateException(
          "Missing function call edge for summary edge: " + oldSummaryEdge);
    }

    private CFunctionCallEdge newCFunctionCallEdge(
        Equivalence.Wrapper<CFAEdge> pOldEdge, CFANode pNewNodeU, CFANode pNewNodeV) {

      CFunctionCallEdge oldCallEdge = (CFunctionCallEdge) pOldEdge.get();
      CFANode oldCallEdgeNodeU = graph.incidentNodes(pOldEdge).nodeU();

      for (Equivalence.Wrapper<CFAEdge> outEdge : graph.outEdges(oldCallEdgeNodeU)) {

        if (outEdge.get() instanceof CFunctionSummaryEdge) {

          CFunctionSummaryEdge newSummaryEdge = (CFunctionSummaryEdge) toNew(outEdge, true).get();

          return new CFunctionCallEdge(
              oldCallEdge.getRawStatement(),
              oldCallEdge.getFileLocation(),
              pNewNodeU,
              (CFunctionEntryNode) pNewNodeV,
              newSummaryEdge.getExpression(),
              newSummaryEdge);
        }
        
      }

      throw new IllegalStateException(
          "Missing summary edge for function call edge: " + oldCallEdge);
    }

    private CFunctionReturnEdge newCFunctionReturnEdge(
        Equivalence.Wrapper<CFAEdge> pOldEdge, CFANode pNewNodeU, CFANode pNewNodeV) {

      CFunctionReturnEdge oldReturnEdge = (CFunctionReturnEdge) pOldEdge.get();
      CFANode oldReturnEdgeNodeV = graph.incidentNodes(pOldEdge).nodeV();

      for (Equivalence.Wrapper<CFAEdge> inEdge : graph.inEdges(oldReturnEdgeNodeV)) {
        if (inEdge.get() instanceof CFunctionSummaryEdge) {

          CFunctionSummaryEdge newSummaryEdge = (CFunctionSummaryEdge) toNew(inEdge, true).get();

          return new CFunctionReturnEdge(
              oldReturnEdge.getFileLocation(),
              (FunctionExitNode) pNewNodeU,
              pNewNodeV,
              newSummaryEdge);
        }
      }

      throw new IllegalStateException(
          "Missing summary edge for function return edge: " + oldReturnEdge);
    }

    private Equivalence.Wrapper<CFAEdge> toNew(
        Equivalence.Wrapper<CFAEdge> pOldEdge, boolean pBuildSupergraph) {

      Equivalence.Wrapper<CFAEdge> newEdge = oldEdgeToNewEdge.get(pOldEdge);
      if (newEdge != null) {
        return newEdge;
      }

      EndpointPair<CFANode> oldEndpoints = graph.incidentNodes(pOldEdge);
      CFANode newNodeU = toNew(oldEndpoints.nodeU());
      CFANode newNodeV = toNew(oldEndpoints.nodeV());

      CCfaEdgeVisitor<CFAEdge, NoException> transformingEdgeVisitor =
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
            public CFAEdge visit(CFunctionCallEdge pCFunctionCallEdge) {
              if (pBuildSupergraph) {
                return newCFunctionCallEdge(pOldEdge, newNodeU, newNodeV);
              } else {
                return null;
              }
            }

            @Override
            public CFAEdge visit(CFunctionReturnEdge pCFunctionReturnEdge) {
              if (pBuildSupergraph) {
                return newCFunctionReturnEdge(pOldEdge, newNodeU, newNodeV);
              } else {
                return null;
              }
            }

            @Override
            public CFAEdge visit(CFunctionSummaryEdge pCFunctionSummaryEdge) {
              if (pBuildSupergraph) {
                return newCFunctionSummaryEdge(pOldEdge, newNodeU, newNodeV);
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

      CCfaEdge oldCCfaEdge = (CCfaEdge) pOldEdge.get();
      CFAEdge newCfaEdge = oldCCfaEdge.accept(transformingEdgeVisitor);

      if (newCfaEdge != null) {

        newEdge = MutableCfaNetwork.wrap(newCfaEdge);

        if (!(newCfaEdge instanceof SummaryPlaceholderEdge)) {
          oldEdgeToNewEdge.put(pOldEdge, newEdge);
        }

        if (newCfaEdge instanceof CFunctionSummaryEdge) {
          CFunctionSummaryEdge cfaSummaryEdge = (CFunctionSummaryEdge) newCfaEdge;
          newNodeU.addLeavingSummaryEdge(cfaSummaryEdge);
          newNodeV.addEnteringSummaryEdge(cfaSummaryEdge);
        } else {
          newNodeU.addLeavingEdge(newCfaEdge);
          newNodeV.addEnteringEdge(newCfaEdge);
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

    private MutableCFA createUnconnectedFunctionCfa(CFA pOriginalCfa) {

      CFANode oldMainEntryNode = pOriginalCfa.getMainFunction();

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
        for (Equivalence.Wrapper<CFAEdge> outEdge : graph.outEdges(oldNode)) {
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

      // create supergraph (includes call, return, and summary edges)
      removeSummaryPlaceholderEdges();
      for (CFANode oldNode : oldNodeToNewNode.keySet()) {
        for (Equivalence.Wrapper<CFAEdge> outEdge : graph.outEdges(oldNode)) {
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
