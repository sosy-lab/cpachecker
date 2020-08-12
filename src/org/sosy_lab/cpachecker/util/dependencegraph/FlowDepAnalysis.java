// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

final class FlowDepAnalysis extends ReachDefAnalysis<MemoryLocation, CFANode, CFAEdge> {

  private final FunctionEntryNode entryNode;
  private final List<CFAEdge> globalEdges;

  private final GlobalPointerState pointerState;
  private final ForeignDefUseData foreignDefUseData;
  private final Map<String, CFAEdge> declarationEdges;

  private final DependenceConsumer dependenceConsumer;

  private final Multimap<CFAEdge, ReachDefAnalysis.Def<MemoryLocation, CFAEdge>> dependences;
  private final Multimap<CFAEdge, MemoryLocation> maybeDefs;

  FlowDepAnalysis(
      Dominance.DomTree<CFANode> pDomTree,
      Dominance.DomFrontiers<CFANode> pDomFrontiers,
      FunctionEntryNode pEntryNode,
      List<CFAEdge> pGlobalEdges,
      GlobalPointerState pPointerState,
      ForeignDefUseData pForeignDefUseData,
      Map<String, CFAEdge> pDeclarationEdges,
      DependenceConsumer pDependenceConsumer) {

    super(SingleFunctionGraph.INSTANCE, pDomTree, pDomFrontiers);

    entryNode = pEntryNode;
    globalEdges = pGlobalEdges;

    pointerState = pPointerState;
    foreignDefUseData = pForeignDefUseData;
    declarationEdges = pDeclarationEdges;

    dependenceConsumer = pDependenceConsumer;

    dependences = ArrayListMultimap.create();
    maybeDefs = HashMultimap.create();
  }

  private CFunctionCallEdge getFunctionCallEdge(CFunctionSummaryEdge pSummaryEdge) {

    for (CFAEdge edge : CFAUtils.leavingEdges(pSummaryEdge.getPredecessor())) {
      if (edge instanceof CFunctionCallEdge) {
        return (CFunctionCallEdge) edge;
      }
    }

    throw new AssertionError("CFunctionSummaryEdge has no corresponding CFunctionCallEdge");
  }

  private Set<MemoryLocation> getCallEdgeDefs(CFunctionCallEdge pCallEdge) {

    Set<MemoryLocation> defs = new HashSet<>();
    AFunctionDeclaration function = pCallEdge.getSuccessor().getFunction();

    defs.addAll(foreignDefUseData.getForeignUses(function));

    for (AParameterDeclaration parameter : function.getParameters()) {
      defs.add(MemoryLocation.valueOf(parameter.getQualifiedName()));
    }

    return defs;
  }

  private Set<MemoryLocation> getSummaryEdgeDefs(CFunctionSummaryEdge pSummaryEdge) {

    Set<MemoryLocation> defs = new HashSet<>();

    AFunctionDeclaration function = pSummaryEdge.getFunctionEntry().getFunction();
    CFunctionCallEdge callEdge = getFunctionCallEdge(pSummaryEdge);
    EdgeDefUseData edgeDefUseData = EdgeDefUseData.extract(callEdge);

    defs.addAll(edgeDefUseData.getDefs());
    defs.addAll(foreignDefUseData.getForeignDefs(function));

    maybeDefs.putAll(pSummaryEdge, foreignDefUseData.getForeignDefs(function));

    for (CExpression expression : edgeDefUseData.getPointeeDefs()) {

      Set<MemoryLocation> possibleDefs = pointerState.getPossiblePointees(callEdge, expression);
      assert possibleDefs != null && !possibleDefs.isEmpty() : "No possible pointees";
      defs.addAll(possibleDefs);

      if (possibleDefs.size() > 1) {
        maybeDefs.putAll(pSummaryEdge, possibleDefs);
      }
    }

    return defs;
  }

  private Set<MemoryLocation> getSummaryEdgeUses(CFunctionSummaryEdge pSummaryEdge) {

    Set<MemoryLocation> uses = new HashSet<>();

    AFunctionDeclaration function = pSummaryEdge.getFunctionEntry().getFunction();
    CFunctionCallEdge callEdge = getFunctionCallEdge(pSummaryEdge);
    EdgeDefUseData edgeDefUseData = EdgeDefUseData.extract(callEdge);

    uses.addAll(edgeDefUseData.getUses());
    uses.addAll(foreignDefUseData.getForeignUses(function));

    for (CExpression expression : edgeDefUseData.getPointeeUses()) {

      Set<MemoryLocation> possibleUses = pointerState.getPossiblePointees(callEdge, expression);
      assert possibleUses != null && !possibleUses.isEmpty() : "No possible pointees";
      uses.addAll(possibleUses);
    }

    return uses;
  }

  private Set<MemoryLocation> getOtherEdgeDefs(CFAEdge pEdge) {

    Set<MemoryLocation> defs = new HashSet<>();
    EdgeDefUseData edgeDefUseData = EdgeDefUseData.extract(pEdge);

    defs.addAll(edgeDefUseData.getDefs());

    for (CExpression expression : edgeDefUseData.getPointeeDefs()) {

      Set<MemoryLocation> possibleDefs = pointerState.getPossiblePointees(pEdge, expression);
      assert possibleDefs != null && !possibleDefs.isEmpty() : "No possible pointees";
      defs.addAll(possibleDefs);

      if (possibleDefs.size() > 1) {
        maybeDefs.putAll(pEdge, possibleDefs);
      }
    }

    return defs;
  }

  private Set<MemoryLocation> getOtherEdgeUses(CFAEdge pEdge) {

    Set<MemoryLocation> uses = new HashSet<>();
    EdgeDefUseData edgeDefUseData = EdgeDefUseData.extract(pEdge);

    uses.addAll(edgeDefUseData.getUses());

    for (CExpression expression : edgeDefUseData.getPointeeUses()) {

      Set<MemoryLocation> possibleUses = pointerState.getPossiblePointees(pEdge, expression);
      assert possibleUses != null && !possibleUses.isEmpty() : "No possible pointees";
      uses.addAll(possibleUses);
    }

    return uses;
  }

  @Override
  protected Set<MemoryLocation> getEdgeDefs(CFAEdge pEdge) {

    if (pEdge instanceof CFunctionCallEdge) {
      return getCallEdgeDefs((CFunctionCallEdge) pEdge);
    } else if (pEdge instanceof CFunctionSummaryEdge) {
      return getSummaryEdgeDefs((CFunctionSummaryEdge) pEdge);
    } else {
      return getOtherEdgeDefs(pEdge);
    }
  }

  private Set<MemoryLocation> getEdgeUses(CFAEdge pEdge) {

    if (pEdge instanceof CFunctionCallEdge) {
      return ImmutableSet.of();
    } else if (pEdge instanceof CFunctionSummaryEdge) {
      return getSummaryEdgeUses((CFunctionSummaryEdge) pEdge);
    } else {
      return getOtherEdgeUses(pEdge);
    }
  }

  @Override
  protected Collection<Def<MemoryLocation, CFAEdge>> getReachDefs(MemoryLocation pVariable) {

    List<Def<MemoryLocation, CFAEdge>> reachDefs = new ArrayList<>();

    for (Def<MemoryLocation, CFAEdge> def : iterateDefsNewestFirst(pVariable)) {

      reachDefs.add(def);

      Optional<CFAEdge> optEdge = def.getEdge();
      if (optEdge.isPresent()) {

        CFAEdge edge = optEdge.orElseThrow();
        if (!maybeDefs.get(edge).contains(pVariable)
            && !EdgeDefUseData.extract(edge).hasPartialDefs()) {
          break;
        }

      } else {
        break;
      }
    }

    return reachDefs;
  }

  @Override
  protected void insertCombiners(Dominance.DomFrontiers<CFANode> pDomFrontiers) {

    for (AParameterDeclaration declaration : entryNode.getFunctionParameters()) {
      MemoryLocation variable = MemoryLocation.valueOf(declaration.getQualifiedName());
      insertCombiner(entryNode, variable);
    }

    for (MemoryLocation variable : foreignDefUseData.getForeignUses(entryNode.getFunction())) {
      insertCombiner(entryNode, variable);
    }

    super.insertCombiners(pDomFrontiers);
  }

  @Override
  protected void traverseDomTree(Dominance.DomTraversable<CFANode> pDomTraversable) {

    globalEdges.forEach(this::pushEdge);

    // init function parameters
    for (CFAEdge callEdge : CFAUtils.allEnteringEdges(pDomTraversable.getNode())) {
      pushEdge(callEdge);
      popEdge(callEdge);
    }

    super.traverseDomTree(pDomTraversable);
  }

  private void handleDependence(CFAEdge pEdge, Def<MemoryLocation, CFAEdge> pDef) {

    MemoryLocation variable = pDef.getVariable();
    Set<ReachDefAnalysis.Def<MemoryLocation, CFAEdge>> defs = new HashSet<>();
    pDef.collect(defs);

    for (ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def : defs) {

      Optional<CFAEdge> optDefEdge = def.getEdge();
      if (optDefEdge.isPresent()) {
        dependenceConsumer.accept(optDefEdge.orElseThrow(), pEdge, variable);
      }
    }
  }

  @Override
  public void run() {

    super.run();

    dependences.forEach(this::handleDependence);

    addFunctionUseDependences();
    addReturnValueDependences();
    addForeignDefDependences();
  }

  private ReachDefAnalysis.Def<MemoryLocation, CFAEdge> getDeclaration(MemoryLocation pVariable) {

    for (ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def : iterateDefsOldestFirst(pVariable)) {
      Optional<CFAEdge> optEdge = def.getEdge();
      if (optEdge.isPresent() && optEdge.orElseThrow() instanceof CDeclarationEdge) {
        return def;
      }
    }

    return null;
  }

  @Override
  protected void pushNode(CFANode pNode) {

    super.pushNode(pNode);

    if (pNode instanceof FunctionExitNode) {
      for (MemoryLocation defVar : foreignDefUseData.getForeignDefs(pNode.getFunction())) {
        for (ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def : getReachDefs(defVar)) {
          for (CFAEdge returnEdge : CFAUtils.leavingEdges(pNode)) {
            dependences.put(returnEdge, def);
          }
        }
      }
    }
  }

  @Override
  protected void pushEdge(CFAEdge pEdge) {

    for (MemoryLocation useVar : getEdgeUses(pEdge)) {
      for (ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def : getReachDefs(useVar)) {
        assert def != null
            : String.format("Variable is missing definition: %s @ %s", useVar, pEdge);
        dependences.put(pEdge, def);
      }
    }

    for (MemoryLocation defVar : getEdgeDefs(pEdge)) {
      ReachDefAnalysis.Def<MemoryLocation, CFAEdge> declaration = getDeclaration(defVar);
      if (declaration != null) {
        dependences.put(pEdge, declaration);
      }
    }

    if (pEdge instanceof CDeclarationEdge) {
      CDeclaration declaration = ((CDeclarationEdge) pEdge).getDeclaration();
      CType type = declaration.getType();

      while (type instanceof CPointerType) {
        type = ((CPointerType) type).getType();
      }

      if (!declaration.isGlobal() && type instanceof CComplexType) {
        CComplexType complexType = (CComplexType) type;
        CFAEdge typeDeclarationEdge = declarationEdges.get(complexType.getQualifiedName());

        if (typeDeclarationEdge != null) {
          dependenceConsumer.accept(
              typeDeclarationEdge, pEdge, MemoryLocation.valueOf(complexType.getQualifiedName()));
        }
      }
    }

    super.pushEdge(pEdge);
  }

  private void addFunctionUseDependences() {

    for (CFAEdge callEdge : CFAUtils.allEnteringEdges(entryNode)) {
      CFAEdge summaryEdge = callEdge.getPredecessor().getLeavingSummaryEdge();
      assert summaryEdge != null : "Missing summary edge for call edge: " + callEdge;
      for (MemoryLocation parameter : getEdgeDefs(callEdge)) {
        dependenceConsumer.accept(summaryEdge, callEdge, parameter);
      }
    }
  }

  private void addForeignDefDependences() {

    AFunctionDeclaration function = entryNode.getFunction();

    for (CFAEdge returnEdge : CFAUtils.leavingEdges(entryNode.getExitNode())) {
      CFAEdge summaryEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
      assert summaryEdge != null : "Missing summary edge for return edge: " + returnEdge;

      for (MemoryLocation defVar : foreignDefUseData.getForeignDefs(function)) {
        dependenceConsumer.accept(returnEdge, summaryEdge, defVar);
      }
    }
  }

  private void addReturnValueDependences() {

    Optional<? extends AVariableDeclaration> optRetVar = entryNode.getReturnVariable().toJavaUtil();

    if (optRetVar.isPresent()) {

      MemoryLocation returnVar = MemoryLocation.valueOf(optRetVar.get().getQualifiedName());

      for (CFAEdge defEdge : CFAUtils.allEnteringEdges(entryNode.getExitNode())) {
        for (CFAEdge returnEdge : CFAUtils.allLeavingEdges(entryNode.getExitNode())) {
          dependenceConsumer.accept(defEdge, returnEdge, returnVar);
        }
      }

      for (CFAEdge returnEdge : CFAUtils.allLeavingEdges(entryNode.getExitNode())) {
        CFAEdge summaryEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
        assert summaryEdge != null : "Missing summary edge for return edge: " + returnEdge;
        dependenceConsumer.accept(returnEdge, summaryEdge, returnVar);
      }
    }
  }

  @FunctionalInterface
  public interface DependenceConsumer {

    void accept(CFAEdge pDefEdge, CFAEdge pUseEdge, MemoryLocation pCause);
  }

  private static final class SingleFunctionGraph
      implements ReachDefAnalysis.Graph<CFANode, CFAEdge> {

    private static final SingleFunctionGraph INSTANCE = new SingleFunctionGraph();

    @Override
    public CFANode getPredecessor(CFAEdge pEdge) {
      return pEdge.getPredecessor();
    }

    @Override
    public CFANode getSuccessor(CFAEdge pEdge) {
      return pEdge.getSuccessor();
    }

    @Override
    public Optional<CFAEdge> getEdge(CFANode pPredecessor, CFANode pSuccessor) {

      for (CFAEdge edge : getLeavingEdges(pPredecessor)) {
        if (edge.getSuccessor().equals(pSuccessor)) {
          return Optional.of(edge);
        }
      }

      return Optional.empty();
    }

    private boolean ignoreEdge(CFAEdge pEdge) {
      return !(pEdge instanceof CFunctionCallEdge)
          && !(pEdge instanceof CFunctionReturnEdge)
          && !(pEdge instanceof CFunctionSummaryStatementEdge);
    }

    @Override
    public Iterable<CFAEdge> getLeavingEdges(CFANode pNode) {
      return () -> Iterators.filter(CFAUtils.allLeavingEdges(pNode).iterator(), this::ignoreEdge);
    }

    @Override
    public Iterable<CFAEdge> getEnteringEdges(CFANode pNode) {
      return () -> Iterators.filter(CFAUtils.allEnteringEdges(pNode).iterator(), this::ignoreEdge);
    }
  }
}
