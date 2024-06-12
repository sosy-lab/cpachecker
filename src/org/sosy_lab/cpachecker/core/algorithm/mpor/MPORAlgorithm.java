// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * This is an implementation of a Partial Order Reduction (POR) algorithm, presented in the 2022
 * paper "Sound Sequentialization for Concurrent Program Verification". This algorithm aims at
 * producing a reduced sequentialization of a parallel C program. The reduced sequentialization can
 * be given to an existing verifier capable of verifying sequential C programs. The POR and the
 * verifier serve as modules, hence MPOR (Modular Partial Order Reduction).
 *
 * <p>Using an unbounded number of threads (e.g. while(true) { pthread_create... }) is undefined
 * behavior.
 */
public class MPORAlgorithm implements Algorithm /* TODO statistics? */ {

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    // TODO this method is called once initially with the set of reached states in the ARG
    throw new UnsupportedOperationException("Unimplemented method 'run'");
  }

  private final ConfigurableProgramAnalysis cpa;
  private final LogManager logger;
  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;
  private final CFA cfa;

  /** A mapping of function names to a set of executing thread IDs. */
  // TODO create a function for this
  private Map<String, Set<Integer>> functionThreadIds;

  /** A set of function names that are start routines extracted from pthread_create calls. */
  private Set<String> startRoutines;

  // TODO a reduced and sequentialized CFA that is created based on the POR algorithm

  public MPORAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa) {

    cpa = pCpa;
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    specification = pSpecification;
    cfa = pCfa;

    // TODO check for C program
    // TODO give the CFA as a parameter to all these functions so that the functions can be reused
    //  outside of this class too. (maybe make static?...)
    checkForParallelProgram();
    extractThreadStartRoutines();
  }

  /**
   * Checks whether any edge in the CFA contains a pthread_create call. If that is not the case, the
   * algorithm ends and the user is informed that MPOR is meant to analyze parallel programs.
   */
  private void checkForParallelProgram() {
    boolean isParallel = false;
    for (CFAEdge cfaEdge : CFAUtils.allEdges(cfa)) {
      if (PthreadFunction.isEdgeFunction(cfaEdge, PthreadFunction.CREATE)) {
        isParallel = true;
        break;
      }
    }
    Preconditions.checkArgument(
        isParallel, "MPOR expects parallel program with at least one pthread_create call");
  }

  /**
   * Searches the CFA for phtread_create calls and extracts the start routine (i.e. the function the
   * thread is executing) through regular expressions. The start routines (= function names) are
   * stored in {@link MPORAlgorithm#startRoutines}.
   */
  private void extractThreadStartRoutines() {
    startRoutines = new HashSet<>();
    // use a regex to extract the start routine from parameters given to pthread_create
    String startRoutineRegex =
        "pthread_create\\s*\\(\\s*[^,]*,\\s*[^,]*,\\s*([^\\s,]*)\\s*,\\s*[^)]*\\)";
    Pattern pattern = Pattern.compile(startRoutineRegex);

    // go through all edges and search for pthread_create calls
    for (CFAEdge cfaEdge : CFAUtils.allUniqueEdges(cfa)) {
      if (PthreadFunction.isEdgeFunction(cfaEdge, PthreadFunction.CREATE)) {

        // use the regex on the code of the current CFA Edge
        Matcher matcher = pattern.matcher(cfaEdge.getCode());
        if (matcher.find()) {
          String startRoutine = matcher.group(1);
          // if the function is given as a pointer, extract only the name
          if (startRoutine.charAt(0) == '&') {
            startRoutine = startRoutine.substring(1);
          }
          startRoutines.add(startRoutine);
        }
      }
    }
  }

  // TODO create a function that maps thread ids (in the analysis, we will use our own thread ids,
  //  runtime ids are not relevant) to their main functions (preferably in the form of a CfaEdge)
  // TODO is this idea sufficient to map from any CfaEdge to a thread id?

  // TODO use GlobalAccessChecker to check whether a CfaEdge reads or writes global / shared
  //  variables?
  // TODO find out what isImporantForThreading (sic) in ThreadingTransferRelation does

  // TODO use ThreadingState to get Thread IDs?
  //  or use the CFAEdge to find pthread_create statements and extract the ID from that.
  //  keep in mind that we need to keep track of all current states and edges for all threads.

  // TODO use CFAEdge getRawStatement or getCode to find out about thread creations,
  //  joins, barriers, etc.? use in combination with CFAEdgeType?

  // TODO use CFAToCTranslator translateCfa to generate a C program based on a CFA
  //  this will be used for the reduced and sequentialized CFA

  // TODO see CFAUtils.java for helpful functions

  /**
   * ONLY use this function if pSCCs was computed using Trajans SCC Algorithm because it returns the
   * SCCs in a reverse topological sort (from maximal to minimal), allowing us to simply return the
   * first SCC in the set.
   *
   * @param pSCCs as a set of sets of Integers
   * @return topologically maximal SCC, i.e. the SCC with the least outgoing edges
   */
  // TODO remove this function, the name is misleading.
  //  simply take the first SCC and comment that its ok because Trajans algorithm computes a reverse
  //  topological sort as a byproduct, no need to have a function for this.
  private ImmutableSet<Integer> computeTopologicallyMaximalSCC(
      ImmutableSet<ImmutableSet<Integer>> pSCCs) {
    Preconditions.checkNotNull(pSCCs);
    Preconditions.checkNotNull(pSCCs.iterator().next());

    return pSCCs.iterator().next();
  }

  /**
   * Computes the Strongly Connected Components (SCCs) of the given graph based on Trajan's SCC
   * Algorithm (1972) and the algorithms in {@link org.sosy_lab.cpachecker.util.GraphUtils}.
   *
   * <p>The algorithm returns the SCCs in reverse topological order (from maximal to minimal) and
   * has a linear complexity of O(N + E) where N is the number of nodes and E the number of edges.
   *
   * @return a set of sets of thread ids that form an SCC
   */
  private ImmutableSet<ImmutableSet<Integer>> computeSCCs(ConflictGraph pConflictGraph) {
    Preconditions.checkNotNull(pConflictGraph);

    // Variables for Trajan's algorithm
    int index = 0;
    Deque<Integer> stack = new ArrayDeque<>();
    Map<Integer, Integer> nodeIndex = new HashMap<>();
    Map<Integer, Integer> nodeLowLink = new HashMap<>();
    Set<Integer> onStack = new HashSet<>();
    List<Set<Integer>> sccList = new ArrayList<>();

    // Iterate over all nodes in the graph
    for (int node : pConflictGraph.getNodes()) {
      if (!nodeIndex.containsKey(node)) {
        strongConnect(node, pConflictGraph, index, stack, nodeIndex, nodeLowLink, onStack, sccList);
      }
    }

    // Convert the result list to ImmutableSet<ImmutableSet<Integer>>
    ImmutableSet.Builder<ImmutableSet<Integer>> sccs = ImmutableSet.builder();
    for (Set<Integer> scc : sccList) {
      sccs.add(ImmutableSet.copyOf(scc));
    }

    return sccs.build();
  }

  /**
   * Applies Tarjan's algorithm recursively to find and collect Strongly Connected Components
   * (SCCs).
   *
   * @param pNode the current node being visited
   * @param pConflictGraph the graph being analyzed
   * @param pIndex the current index in the DFS traversal
   * @param pStack the stack used to keep track of the nodes in the current path
   * @param pNodeIndex a map storing the index of each node
   * @param pNodeLowLink a map storing the lowest index reachable from each node
   * @param pOnStack a set to track nodes currently in the stack
   * @param pSccList a list to collect all the identified SCCs
   */
  private void strongConnect(
      int pNode,
      ConflictGraph pConflictGraph,
      int pIndex,
      Deque<Integer> pStack,
      Map<Integer, Integer> pNodeIndex,
      Map<Integer, Integer> pNodeLowLink,
      Set<Integer> pOnStack,
      List<Set<Integer>> pSccList) {

    pNodeIndex.put(pNode, pIndex);
    pNodeLowLink.put(pNode, pIndex);
    pIndex++;
    pStack.push(pNode);
    pOnStack.add(pNode);

    // Consider successors of the node
    Set<Integer> successors = pConflictGraph.getSuccessors(pNode);
    if (successors != null) {
      for (Integer successor : pConflictGraph.getSuccessors(pNode)) {
        if (!pNodeIndex.containsKey(successor)) {
          // Successor has not yet been visited; recurse on it
          strongConnect(
              successor,
              pConflictGraph,
              pIndex,
              pStack,
              pNodeIndex,
              pNodeLowLink,
              pOnStack,
              pSccList);
          pNodeLowLink.put(pNode, Math.min(pNodeLowLink.get(pNode), pNodeLowLink.get(successor)));
        } else if (pOnStack.contains(successor)) {
          // Successor is in the stack and hence in the current SCC
          pNodeLowLink.put(pNode, Math.min(pNodeLowLink.get(pNode), pNodeIndex.get(successor)));
        }
      }
    }

    // If node is a root node, pop the stack and generate an SCC
    if (pNodeLowLink.get(pNode).equals(pNodeIndex.get(pNode))) {
      Set<Integer> scc = new HashSet<>();
      int currentNode;
      do {
        currentNode = pStack.pop();
        pOnStack.remove(currentNode);
        scc.add(currentNode);
      } while (pNode != currentNode);
      pSccList.add(scc);
    }
  }
}
