package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

final class FlowDepAnalysis extends ReachDefAnalysis<MemoryLocation, CFANode, CFAEdge> {

  private final FunctionEntryNode entryNode;
  private final List<CFAEdge> globalEdges;
  private final DependenceConsumer dependenceConsumer;

  private final Map<CFAEdge, DefsUses.Data> defsUses;
  private final Multimap<CFAEdge, ReachDefAnalysis.Def<MemoryLocation, CFAEdge>> dependences;

  protected FlowDepAnalysis(
      Dominance.DomTraversable<CFANode> pDomTraversable,
      Dominance.DomFrontiers<CFANode> pDomFrontiers,
      FunctionEntryNode pEntryNode,
      List<CFAEdge> pGlobalEdges,
      DependenceConsumer pDependenceConsumer) {
    super(SingleFunctionGraph.INSTANCE, pDomTraversable, pDomFrontiers);

    entryNode = pEntryNode;
    globalEdges = pGlobalEdges;
    dependenceConsumer = pDependenceConsumer;

    defsUses = new HashMap<>();
    dependences = ArrayListMultimap.create();
  }

  @Override
  protected Set<MemoryLocation> getEdgeDefs(CFAEdge pEdge) {
    return defsUses.get(pEdge).getDefs();
  }

  private Set<MemoryLocation> getEdgeUses(CFAEdge pEdge) {
    return defsUses.get(pEdge).getUses();
  }

  @Override
  protected void insertCombiners(Dominance.DomFrontiers<CFANode> pDomFrontiers) {

    // use a single call edge to determine function parameters
    // insert a Combiner for every parameter-variable
    CFAUtils.allEnteringEdges(entryNode)
        .first()
        .toJavaUtil()
        .ifPresent(
            edge -> {
              for (MemoryLocation variable : getEdgeDefs(edge)) {
                insertCombiner(entryNode, variable);
              }
            });

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

  private void initDefsUsesEdge(CFAEdge pEdge) {
    Optional<DefsUses.Data> optEdgeDefsUses = DefsUses.getData(pEdge);
    if (optEdgeDefsUses.isPresent()) {
      defsUses.put(pEdge, optEdgeDefsUses.orElseThrow());
    } else {
      throw new AssertionError("Pointers are currently unsupported!");
    }
  }

  private void initDefsUses() {

    Set<CFANode> nodes =
        CFATraversal.dfs()
            .ignoreFunctionCalls()
            .collectNodesReachableFromTo(entryNode, entryNode.getExitNode());

    for (CFAEdge edge : globalEdges) {
      initDefsUsesEdge(edge);
    }

    for (CFANode node : nodes) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        initDefsUsesEdge(edge);
      }
    }

    for (CFAEdge callEdge : CFAUtils.allEnteringEdges(entryNode)) {
      defsUses.put(callEdge, DefsUses.getCallDefs((FunctionCallEdge) callEdge));
    }
  }

  @Override
  public void run() {

    initDefsUses();

    super.run();

    dependences.forEach(
        (dependentEdge, dependentDef) -> {
          Set<ReachDefAnalysis.Def<MemoryLocation, CFAEdge>> defs = new HashSet<>();
          dependentDef.collect(defs);

          for (ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def : defs) {
            Optional<CFAEdge> optEdge = def.getEdge();
            if (optEdge.isPresent()) {
              dependenceConsumer.accept(
                  optEdge.orElseThrow(), dependentEdge, dependentDef.getVariable());
            }
          }
        });

    addReturnValueDependences();
  }

  private ReachDefAnalysis.Def<MemoryLocation, CFAEdge> getDeclaration(MemoryLocation pVariable) {

    for (ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def : getDefQueueIterator(pVariable)) {
      Optional<CFAEdge> optEdge = def.getEdge();
      if (optEdge.isPresent()) {
        return def;
      }
    }

    return null;
  }

  @Override
  protected void pushEdge(CFAEdge pEdge) {

    for (MemoryLocation useVar : getEdgeUses(pEdge)) {
      ReachDefAnalysis.Def<MemoryLocation, CFAEdge> def = getReachDef(useVar);
      assert def != null : String.format("Variable is missing definition: %s @ %s", useVar, pEdge);
      dependences.put(pEdge, def);
    }

    for (MemoryLocation defVar : getEdgeDefs(pEdge)) {
      ReachDefAnalysis.Def<MemoryLocation, CFAEdge> declaration = getDeclaration(defVar);
      if (declaration != null) {
        dependences.put(pEdge, declaration);
      }
    }

    super.pushEdge(pEdge);
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
  interface DependenceConsumer {

    void accept(CFAEdge pEdge, CFAEdge pDependent, MemoryLocation pCause);
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

      for (CFAEdge edge : CFAUtils.allLeavingEdges(pPredecessor)) {
        if (edge.getSuccessor().equals(pSuccessor)) {
          return Optional.of(edge);
        }
      }

      return Optional.empty();
    }

    private boolean ignoreEdge(CFAEdge pEdge) {
      return pEdge.getEdgeType() != CFAEdgeType.FunctionCallEdge
          && pEdge.getEdgeType() != CFAEdgeType.CallToReturnEdge;
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
