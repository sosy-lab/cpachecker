// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taint;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.dependencegraph.CSystemDependenceGraph;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.dependencegraph.SystemDependenceGraph;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.taint")
class TaintTransferRelation extends ForwardingTransferRelation<TaintState, TaintState, Precision> {

  private static final MemoryLocation UNKNOWN = MemoryLocation.forIdentifier("__UNKNOWN");

  @Option(secure = true, name = "sourceFunctions", description = "Set of taint-source functions.")
  private Set<String> taintSourceFunctions = ImmutableSet.of();

  @Option(secure = true, name = "sinkFunctions", description = "Set of taint-sink functions.")
  private Set<String> taintSinkFunctions = ImmutableSet.of();

  private final EdgeDefUseData.Extractor defUseDataExtractor;
  private final CSystemDependenceGraph sdg;
  private final Multimap<CFAEdge, CSystemDependenceGraph.Node> nodesPerCfaNode =
      ArrayListMultimap.create();

  TaintTransferRelation(Configuration pConfig, CSystemDependenceGraph pSdg)
      throws InvalidConfigurationException {
    defUseDataExtractor = EdgeDefUseData.createExtractor(true);
    sdg = pSdg;
    for (CSystemDependenceGraph.Node node : sdg.getNodes()) {
      Optional<CFAEdge> optCfaEdge = node.getStatement();
      if (optCfaEdge.isPresent()) {
        nodesPerCfaNode.put(optCfaEdge.orElseThrow(), node);
      }
    }

    pConfig.inject(this);
  }

  private boolean containsTaintedUse(TaintState pTaintState, EdgeDefUseData pEdgeDefUseData) {
    if (pEdgeDefUseData.getPointeeUses().size() > 0 && pTaintState.isTainted(UNKNOWN)) {
      return true;
    }
    for (MemoryLocation memoryLocation : pEdgeDefUseData.getUses()) {
      if (pTaintState.isTainted(memoryLocation)) {
        return true;
      }
    }
    return false;
  }

  private TaintState handleEdge(TaintState pTaintState, CFAEdge pEdge) {
    EdgeDefUseData edgeDefUseData = defUseDataExtractor.extract(pEdge);
    boolean taintDefs =
        pTaintState.isTainted(pEdge) || containsTaintedUse(pTaintState, edgeDefUseData);
    TaintState newTaintState = pTaintState;
    if (taintDefs) {
      if (edgeDefUseData.getPointeeDefs().size() > 0) {
        newTaintState = newTaintState.taint(UNKNOWN);
      }
      for (MemoryLocation memoryLocation : edgeDefUseData.getDefs()) {
        newTaintState = newTaintState.taint(memoryLocation);
      }
    }
    return newTaintState;
  }

  @Override
  protected @Nullable TaintState handleAssumption(
      CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
      throws CPATransferException, InterruptedException {

    TaintState newTaintState = handleEdge(state, pCfaEdge);

    DependentEdgeCollectingSdgVisitor sdgVisitor = new DependentEdgeCollectingSdgVisitor();
    sdg.traverse(nodesPerCfaNode.get(pCfaEdge), sdgVisitor);

    for (CFAEdge dependentEdge : sdgVisitor.getDependentEdges()) {
      newTaintState = newTaintState.taint(dependentEdge);
    }

    return newTaintState;
  }

  @Override
  protected TaintState handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {
    return handleEdge(state, pCfaEdge);
  }

  @Override
  protected TaintState handleFunctionCallEdge(
      CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments,
      List<CParameterDeclaration> pParameters,
      String pCalledFunctionName)
      throws CPATransferException {
    return handleEdge(state, pCfaEdge);
  }

  @Override
  protected TaintState handleFunctionReturnEdge(
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall,
      CFunctionCall pSummaryExpr,
      String pCallerFunctionName)
      throws CPATransferException {
    return handleEdge(state, pCfaEdge);
  }

  @Override
  protected TaintState handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge)
      throws CPATransferException {
    return handleEdge(state, pCfaEdge);
  }

  @Override
  protected TaintState handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    return handleEdge(state, pCfaEdge);
  }

  @Override
  protected TaintState handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
      throws CPATransferException {
    TaintState newTaintState = state;
    if (pStatement instanceof CFunctionCall) {
      CFunctionCall functionCall = (CFunctionCall) pStatement;
      CFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
      CExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
      if (functionNameExpression instanceof CIdExpression) {
        String calledFunctionName = ((CIdExpression) functionNameExpression).getName();
        if (taintSourceFunctions.contains(calledFunctionName)) {
          newTaintState = newTaintState.taint(pCfaEdge);
        }
        if (taintSinkFunctions.contains(calledFunctionName)) {
          newTaintState = newTaintState.taintError();
        }
      }
    }
    return handleEdge(state, pCfaEdge);
  }

  private static final class DependentEdgeCollectingSdgVisitor
      implements CSystemDependenceGraph.ForwardsVisitor {

    private final Set<CFAEdge> dependentEdges;

    private DependentEdgeCollectingSdgVisitor() {
      dependentEdges = new LinkedHashSet<>();
    }

    private Set<CFAEdge> getDependentEdges() {
      return dependentEdges;
    }

    @Override
    public SystemDependenceGraph.VisitResult visitNode(CSystemDependenceGraph.Node pNode) {
      return SystemDependenceGraph.VisitResult.CONTINUE;
    }

    @Override
    public SystemDependenceGraph.VisitResult visitEdge(
        SystemDependenceGraph.EdgeType pType,
        CSystemDependenceGraph.Node pPredecessor,
        CSystemDependenceGraph.Node pSuccessor) {

      if (pType == SystemDependenceGraph.EdgeType.CONTROL_DEPENDENCY) {
        pSuccessor.getStatement().ifPresent(dependentEdges::add);
      }

      return SystemDependenceGraph.VisitResult.SKIP;
    }
  }
}
