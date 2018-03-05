/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.dependencegraph;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.dominator.DominatorState;
import org.sosy_lab.cpachecker.cpa.flowdep.FlowDependenceState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState;
import org.sosy_lab.cpachecker.cpa.reachdef.ReachingDefState.ProgramDefinitionPoint;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.edges.ControlDependenceEdge;
import org.sosy_lab.cpachecker.util.dependencegraph.edges.FlowDependenceEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Factory for creating a {@link DependenceGraph} from a {@link CFA}. */
@Options(prefix = "dependenceGraph")
public class DGBuilder {

  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private Table<CFAEdge, Optional<MemoryLocation>, DGNode> nodes;
  private Set<DGEdge> edges;

  @Option(
    secure = true,
    description =
        "File to export dependence graph to. If `null`, dependence"
            + " graph will not be exported as dot."
  )
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportDot = Paths.get("DependenceGraph.dot");

  public DGBuilder(
      final CFA pCfa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
  }

  public DependenceGraph build()
      throws InvalidConfigurationException, InterruptedException, CPAException {
    nodes = HashBasedTable.create();
    edges = new HashSet<>();
    addFlowDependences();
    addFunctionControlDependences();
    addControlDependences();

    DependenceGraph dg = new DependenceGraph(nodes, edges);
    export(dg);
    logger.log(
        Level.FINE,
        "Create dependence graph with ",
        nodes.size(),
        " nodes and ",
        edges.size(),
        " edges.");
    return dg;
  }

  private void addFunctionControlDependences() {
    for (FunctionEntryNode n : cfa.getAllFunctionHeads()) {
      if (n == cfa.getMainFunction()) {
        continue;
      }
      for (CFAEdge ee : CFAUtils.enteringEdges(n)) {
        assert ee instanceof CFunctionCallEdge
            : "Edge to function entry node is not a function call edge: " + ee;

        for (CFAEdge le : CFAUtils.leavingEdges(n.getExitNode())) {
          assert le instanceof CFunctionReturnEdge
              : "Edge from function exit node is not a " + "function return edge: " + le;
          // Every return edge has to be linked to every call edge, and vice-versa.
          // if a function return is required, the function has to be entered first.
          // And if a function is entered, it must be able to leave it again, properly.

          // We require the dg node of the call without any parameter definition
          // - if a return edge requires that, it is already solved by flow dependence edges.
          // But here, we only require a function control dependence to make
          // sure that the function is actually called if a return is relevant
          DGNode call = getDGNode(ee, Optional.empty());
          // Take all dg nodes of return edges, since every return depends
          // on the function being called first
          Collection<DGNode> allRetNodes = getDGNodes(le);
          for (DGNode returnNode : allRetNodes) {
            DGEdge functionControlDependence = new FunctionControlDependenceEdge(call, returnNode);
            addDependence(functionControlDependence);
          }

          DGNode ret = getDGNode(le, Optional.empty());
          Collection<DGNode> allCallNodes = getDGNodes(ee);
          for (DGNode callNode : allCallNodes) {
            DGEdge functionControlDependence = new FunctionControlDependenceEdge(ret, callNode);
            addDependence(functionControlDependence);
          }
        }
      }
    }
  }

  /*
  private Map<CFAEdge, DGNode> getCFAEdges() {
    EdgeCollectingCFAVisitor edgeCollector = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().traverse(cfa.getMainFunction(), edgeCollector);
    List<CFAEdge> allEdges = edgeCollector.getVisitedEdges();

    Map<CFAEdge, DGNode> dgNodes = new HashMap<>();
    for (CFAEdge e : allEdges) {
      dgNodes.put(e, getDGNode(e));
    }
    return dgNodes;
  }

  /**
   * Adds control dependencies to dependence graph.
   *
   * @see ControlDependenceEdge
   */
  private void addControlDependences()
      throws InterruptedException, InvalidConfigurationException, CPAException {
    PostDominators postDoms = PostDominators.create(cfa, logger, shutdownNotifier);
    NodeCollectingCFAVisitor v = new NodeCollectingCFAVisitor();
    CFATraversal.dfs().traverse(cfa.getMainFunction(), v);
    List<CFANode> branchingNodes =
        v.getVisitedNodes()
            .stream()
            .filter(n -> n.getNumLeavingEdges() > 1)
            .filter(n -> n.getLeavingEdge(0) instanceof CAssumeEdge)
            .collect(Collectors.toList());

    for (CFANode branch : branchingNodes) {
      Set<CFANode> postDominatorsOfBranchingNode = postDoms.getPostDominators(branch);
      FluentIterable<CFAEdge> assumeEdges = CFAUtils.leavingEdges(branch);
      assert assumeEdges.size() == 2;
      for (CFAEdge g : assumeEdges) {

        DGNode nodeDependentOn = getDGNode(g, Optional.empty());
        assert getDGNodes(g).size() == 1
            : "Only using one DG node, but multiple would exist: " + nodeDependentOn;
        List<CFANode> nodesOnPath = new ArrayList<>();
        Queue<CFAEdge> waitlist = new ArrayDeque<>(8);
        Set<CFAEdge> reached = new HashSet<>();
        waitlist.offer(g);
        while (!waitlist.isEmpty()) {
          CFAEdge current = waitlist.poll();
          if (!reached.contains(current)) {
            reached.add(current);
            CFANode precessorNode = current.getPredecessor();
            if (precessorNode.equals(branch)) {
              CFAUtils.leavingEdges(current.getSuccessor()).forEach(waitlist::offer);

            } else
            // branch node is not post-dominated by current node (condition 2 of control dependence)
            if (!postDominatorsOfBranchingNode.contains(precessorNode)) {
              // all nodes on path from branch to current are post-dominated by current
              // (condition 1 of control dependence)
              if (isPostDomOfAll(precessorNode, nodesOnPath, postDoms)) {
                Collection<DGNode> nodesDepending = getDGNodes(current);
                for (DGNode nodeDepending : nodesDepending) {
                  DGEdge controlDependency =
                      new ControlDependenceEdge(nodeDependentOn, nodeDepending);
                  addDependence(controlDependency);
                }
                nodesOnPath.add(precessorNode);
              }
              CFAUtils.leavingEdges(current.getSuccessor()).forEach(waitlist::offer);
            }
          }
        }
      }
    }
  }

  private boolean isPostDomOfAll(
      final CFANode pNode,
      final Collection<CFANode> pNodeSet,
      final PostDominators pPostDominators) {

    for (CFANode n : pNodeSet) {
      if (!pPostDominators.getPostDominators(n).contains(pNode)) {
        return false;
      }
    }
    return true;
  }

  private void addFlowDependences()
      throws InvalidConfigurationException, InterruptedException, CPAException {
    FlowDependences flowDependences = FlowDependences.create(cfa, logger, shutdownNotifier);

    for (Cell<CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, CFAEdge>> c :
        flowDependences.cellSet()) {

      CFAEdge edgeDepending = c.getRowKey();
      Optional<MemoryLocation> defOfEdge = c.getColumnKey();
      DGNode nodeDepending;
      if (defOfEdge.isPresent()) {
        nodeDepending = getDGNode(edgeDepending, defOfEdge);
      } else {
        nodeDepending = getDGNode(edgeDepending, Optional.empty());
      }
      for (Entry<MemoryLocation, CFAEdge> useAndDef : c.getValue().entries()) {
        DGNode dependency = getDGNode(useAndDef.getValue(), Optional.of(useAndDef.getKey()));
        DGEdge newEdge = new FlowDependenceEdge(dependency, nodeDepending);
        addDependence(newEdge);
      }
    }
  }

  /**
   * Returns the {@link DGNode} corresponding to the given {@link CFAEdge}. If a node for this edge
   * already exists, the existing node is returned. Otherwise, a new node is created.
   *
   * <p>Always use this method and never use {@link #createNode(CFAEdge, Optional)}!
   */
  private DGNode getDGNode(final CFAEdge pCfaEdge, final Optional<MemoryLocation> pCause) {
    if (!nodes.contains(pCfaEdge, pCause)) {
      nodes.put(pCfaEdge, pCause, createNode(pCfaEdge, pCause));
    }
    return nodes.get(pCfaEdge, pCause);
  }

  private Collection<DGNode> getDGNodes(final CFAEdge pCfaEdge) {
    if (!nodes.containsRow(pCfaEdge)) {
      nodes.put(pCfaEdge, Optional.empty(), createNode(pCfaEdge, Optional.empty()));
    }
    return nodes.row(pCfaEdge).values();
  }

  /**
   * Adds the given dependence edge to the set of dependence edges and tells the nodes of the edge
   * about the new edge.
   */
  private void addDependence(DGEdge pEdge) {
    DGNode nodeDependentOn = pEdge.getStart();
    DGNode nodeDepending = pEdge.getEnd();
    nodeDependentOn.addOutgoingEdge(pEdge);
    nodeDepending.addIncomingEdge(pEdge);
    edges.add(pEdge);
  }

  /**
   * Creates a new node. Never call this method directly, but use {@link #getDGNode(CFAEdge,
   * Optional)} to retrieve nodes for {@link CFAEdge CFAEdges}!
   */
  private DGNode createNode(final CFAEdge pCfaEdge, final Optional<MemoryLocation> pCause) {
    if (pCause.isPresent()) {
      return new DGNode(pCfaEdge, pCause.get());
    } else {
      return new DGNode(pCfaEdge);
    }
  }

  private void export(DependenceGraph pDg) {
    if (exportDot != null) {
      try (Writer w = IO.openOutputFile(exportDot, Charset.defaultCharset())) {
        DGExporter.generateDOT(w, pDg);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write dependence graph to dot file");
        // continue with analysis
      }
    }
  }

  /**
   * Flow dependences of nodes in a {@link CFA}.
   *
   * <p>A node <code>I</code> is flow dependent on a node <code>J</code> if <code>J</code>
   * represents a variable assignment and the assignment is part of node <code>I</code>'s use-def
   * relation.
   */
  private static class FlowDependences
      extends ForwardingTable<
          CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, CFAEdge>> {

    // CFAEdge + defined memory location -> Edge defining the uses
    private Table<CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, CFAEdge>> dependences;

    private FlowDependences(
        final Table<CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, CFAEdge>>
            pDependences) {
      dependences = pDependences;
    }

    @Override
    protected Table<CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, CFAEdge>>
        delegate() {
      return dependences;
    }

    public static FlowDependences create(
        final CFA pCfa, final LogManager pLogger, final ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException, CPAException, InterruptedException {
      String configFile = "flowDependences.properties";

      Configuration config =
          Configuration.builder().loadFromResource(FlowDependences.class, configFile).build();
      ReachedSetFactory reachedFactory = new ReachedSetFactory(config, pLogger);
      ConfigurableProgramAnalysis cpa =
          new CPABuilder(config, pLogger, pShutdownNotifier, reachedFactory)
              .buildCPAs(pCfa, Specification.alwaysSatisfied(), new AggregatedReachedSets());
      Algorithm algorithm = CPAAlgorithm.create(cpa, pLogger, config, pShutdownNotifier);
      ReachedSet reached = reachedFactory.create();

      AbstractState initialState =
          cpa.getInitialState(pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      Precision initialPrecision =
          cpa.getInitialPrecision(
              pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      reached.add(initialState, initialPrecision);

      // populate reached set
      algorithm.run(reached);
      assert !reached.hasWaitingState()
          : "CPA algorithm finished, but waitlist not empty: " + reached.getWaitlist();

      Table<CFAEdge, Optional<MemoryLocation>, Multimap<MemoryLocation, CFAEdge>> dependencyMap =
          HashBasedTable.create();
      for (AbstractState s : reached) {
        assert s instanceof ARGState;
        ARGState wrappingState = (ARGState) s;
        FlowDependenceState flowDepState = getState(wrappingState, FlowDependenceState.class);

        for (CFAEdge g : flowDepState.getDependees()) {
          Set<Optional<MemoryLocation>> defs = flowDepState.getDefinitions(g);
          for (Optional<MemoryLocation> d : defs) {
            Multimap<MemoryLocation, CFAEdge> memLocUsedAndDefiner =
                getDependences(flowDepState, g, d);
            if (dependencyMap.contains(g, d)) {
              dependencyMap.get(g, d).putAll(memLocUsedAndDefiner);
            } else {
              dependencyMap.put(g, d, memLocUsedAndDefiner);
            }
          }
        }
      }

      return new FlowDependences(dependencyMap);
    }

    @SuppressWarnings("unchecked")
    private static <T extends AbstractState> T getState(
        final ARGState pState, final Class<T> stateClass) {
      T s = AbstractStates.extractStateByType(pState, stateClass);
      assert s != null
          : "No "
              + ReachingDefState.class.getCanonicalName()
              + " found in composite state: "
              + pState.toString();

      return s;
    }

    private static Multimap<MemoryLocation, CFAEdge> getDependences(
        final FlowDependenceState pFlowDepState,
        final CFAEdge pEdge,
        final Optional<MemoryLocation> pDef) {
      Multimap<MemoryLocation, CFAEdge> dependencies = HashMultimap.create();

      Multimap<MemoryLocation, ProgramDefinitionPoint> dependentDefs =
          pFlowDepState.getDependentDefs(pEdge, pDef);

      for (Entry<MemoryLocation, ProgramDefinitionPoint> e : dependentDefs.entries()) {
        ProgramDefinitionPoint defPoint = e.getValue();
        CFANode start = defPoint.getDefinitionEntryLocation();
        CFANode stop = defPoint.getDefinitionExitLocation();

        boolean added = false;
        for (CFAEdge g : CFAUtils.leavingEdges(start)) {
          if (g.getSuccessor().equals(stop)) {
            dependencies.put(e.getKey(), g);
            added = true;
          }
        }
        assert added : "No edge added for nodes " + start + " to " + stop;
      }

      return dependencies;
    }
  }

  /**
   * Map of {@link CFANode CFANodes} to their post-dominators.
   *
   * <p>Node <code>I</code> is post-dominated by node <code>J</code> if every path from <code>I
   * </code> to the program exit goes through <code>J</code>.
   */
  private static class PostDominators {

    private Map<CFANode, Set<CFANode>> postDominatorMap;

    private PostDominators(final Map<CFANode, Set<CFANode>> pPostDominatorMap) {
      postDominatorMap = pPostDominatorMap;
    }

    /**
     * Get all post-dominators of the given node.
     *
     * <p>Node <code>I</code> is post-dominated by node <code>J</code> if every path from <code>I
     * </code> to the program exit goes through <code>J</code>.
     *
     * <p>That means that every program path from the given node to the program exit has to go
     * through each node that is in the returned collection
     */
    public Set<CFANode> getPostDominators(final CFANode pNode) {
      checkState(
          postDominatorMap.containsKey(pNode), "Node " + pNode + " not in post-dominator map");
      return postDominatorMap.get(pNode);
    }

    public static PostDominators create(
        final CFA pCfa, final LogManager pLogger, final ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException, CPAException, InterruptedException {
      String configFile = "postDominators.properties";

      Configuration config =
          Configuration.builder().loadFromResource(PostDominators.class, configFile).build();
      ReachedSetFactory reachedFactory = new ReachedSetFactory(config, pLogger);
      ConfigurableProgramAnalysis cpa =
          new CPABuilder(config, pLogger, pShutdownNotifier, reachedFactory)
              .buildCPAs(pCfa, Specification.alwaysSatisfied(), new AggregatedReachedSets());
      Algorithm algorithm = CPAAlgorithm.create(cpa, pLogger, config, pShutdownNotifier);
      ReachedSet reached = reachedFactory.create();

      FunctionEntryNode mainFunction = pCfa.getMainFunction();
      Collection<CFANode> startNodes =
          CFAUtils.getProgramSinks(pCfa, pCfa.getLoopStructure().get(), mainFunction);

      for (CFANode n : startNodes) {
        StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
        AbstractState initialState = cpa.getInitialState(n, partition);
        Precision initialPrecision = cpa.getInitialPrecision(n, partition);
        reached.add(initialState, initialPrecision);
      }

      // populate reached set
      algorithm.run(reached);
      assert !reached.hasWaitingState()
          : "CPA algorithm finished, but waitlist not empty: " + reached.getWaitlist();

      Map<CFANode, Set<CFANode>> dependencyMap = new HashMap<>();
      for (AbstractState s : reached) {
        assert s instanceof ARGState : "AbstractState of reached set not a composite state: " + s;
        ARGState wrappingState = (ARGState) s;
        DominatorState postDomState = AbstractStates.extractStateByType(s, DominatorState.class);
        CFANode currNode = AbstractStates.extractLocation(s);

        assert !dependencyMap.containsKey(currNode) : "Second state for location:" + wrappingState;

        dependencyMap.put(currNode, postDomState);
      }

      return new PostDominators(dependencyMap);
    }
  }
}
