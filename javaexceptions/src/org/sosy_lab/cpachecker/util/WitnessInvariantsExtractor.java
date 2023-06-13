// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant.ManagerKey;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInvariantsUtils;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;

/**
 * This class extracts invariants from the correctness witness automaton. Calling {@link
 * WitnessInvariantsExtractor#analyzeWitness()} analyzes the witness first by conducting a
 * reachability analysis over the witness automaton. Subsequently, the invariants can be extracted
 * from the reached set.
 */
@Options(prefix = "witness")
public class WitnessInvariantsExtractor {

  private Configuration config;
  private LogManager logger;
  private CFA cfa;
  private ShutdownNotifier shutdownNotifier;
  private ReachedSet reachedSet;
  private Specification automatonAsSpec;

  @Option(
      secure = true,
      name = "debug.checkForMissedInvariants",
      description =
          "Fail-fast if invariants in the witness exist that would not be accounted for. There are"
              + " cases where unaccounted invariants are perfectly fine, e.g. if those states in"
              + " the witness automaton are actually unreachable in the program. This is however"
              + " rarely the intention of the original producer of the witness, so this options can"
              + " be used to debug those cases.")
  private boolean checkForMissedInvariants = false;

  /**
   * Creates an instance of {@link WitnessInvariantsExtractor} and uses {@code pSpecification} and
   * {@code pPathToWitnessFile} to build the witness automaton so this automaton can be applied as
   * specification ({@code automatonAsSpec}) for the reachability analysis that is subsequently
   * called.
   *
   * @param pConfig the configuration
   * @param pLogger the logger
   * @param pCFA the cfa
   * @param pShutdownNotifier the shutdown notifier
   * @param pPathToWitnessFile the path to the witness file
   * @throws InvalidConfigurationException if the configuration is invalid
   */
  public WitnessInvariantsExtractor(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCFA,
      ShutdownNotifier pShutdownNotifier,
      Path pPathToWitnessFile)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    config = pConfig;
    config.inject(this);
    logger = pLogger;
    cfa = pCFA;
    shutdownNotifier = pShutdownNotifier;
    automatonAsSpec = buildSpecification(pPathToWitnessFile);
    analyzeWitness();
  }

  /**
   * Creates an instance of {@link WitnessInvariantsExtractor} and uses {@code pAutomaton} so {@code
   * pAutomaton} can be applied as specification ({@code automatonAsSpec}) for the reachability
   * analysis that is subsequently called.
   *
   * @param pConfig the configuration
   * @param pAutomaton the automaton used as specificatigiton
   * @param pLogger the logger
   * @param pCFA the cfa
   * @param pShutdownNotifier the shutdown notifier
   * @throws InvalidConfigurationException if the configuration is invalid
   * @throws CPAException if an error occurs during the reachability analysis
   */
  public WitnessInvariantsExtractor(
      Configuration pConfig,
      Automaton pAutomaton,
      LogManager pLogger,
      CFA pCFA,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    config = pConfig;
    logger = pLogger;
    cfa = pCFA;
    shutdownNotifier = pShutdownNotifier;
    automatonAsSpec = Specification.fromAutomata(ImmutableList.of(pAutomaton));
    analyzeWitness();
  }

  private Configuration generateLocalConfiguration(Configuration pConfig)
      throws InvalidConfigurationException {
    ConfigurationBuilder configBuilder =
        Configuration.builder()
            .loadFromResource(WitnessInvariantsExtractor.class, "witness-analysis.properties");
    List<String> copyOptions =
        Arrays.asList(
            "analysis.machineModel",
            "analysis.programNames",
            "cpa.callstack.skipRecursion",
            "cpa.callstack.skipVoidRecursion",
            "cpa.callstack.skipFunctionPointerRecursion");
    for (String copyOption : copyOptions) {
      configBuilder.copyOptionFromIfPresent(pConfig, copyOption);
    }
    return configBuilder.build();
  }

  private Specification buildSpecification(Path pathToWitnessFile)
      throws InvalidConfigurationException, InterruptedException {
    return Specification.fromFiles(
        ImmutableList.of(pathToWitnessFile), cfa, config, logger, shutdownNotifier);
  }

  private void analyzeWitness()
      throws InvalidConfigurationException, CPAException, InterruptedException {
    Configuration localConfig = generateLocalConfiguration(config);
    ReachedSetFactory reachedSetFactory = new ReachedSetFactory(localConfig, logger);
    CPABuilder builder = new CPABuilder(localConfig, logger, shutdownNotifier, reachedSetFactory);
    ConfigurableProgramAnalysis cpa =
        builder.buildCPAs(cfa, automatonAsSpec, AggregatedReachedSets.empty());
    CPAAlgorithm algorithm = CPAAlgorithm.create(cpa, logger, localConfig, shutdownNotifier);
    CFANode rootNode = cfa.getMainFunction();
    StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
    try {
      reachedSet = reachedSetFactory.createAndInitialize(cpa, rootNode, partition);
      algorithm.run(reachedSet);
    } catch (InterruptedException e) {
      // Candidate collection was interrupted,
      // but instead of throwing the exception here,
      // let it be thrown by the invariant generator.
    }
    if (checkForMissedInvariants) {
      AutomatonInvariantsUtils.checkForMissedInvariants(automatonAsSpec, reachedSet);
    }
  }

  /**
   * Extracts the invariants with their corresponding CFA location from {@link
   * WitnessInvariantsExtractor#reachedSet}. For two invariants at the same CFA location the
   * conjunction is applied for the two invariants.
   *
   * @return the set of location invariants that stores the extracted location invariants
   */
  public Set<ExpressionTreeLocationInvariant> extractInvariantsFromReachedSet()
      throws InterruptedException {
    Set<ExpressionTreeLocationInvariant> invariants = new LinkedHashSet<>();
    ConcurrentMap<ManagerKey, ToFormulaVisitor> toCodeVisitorCache = new ConcurrentHashMap<>();
    for (AbstractState abstractState : reachedSet) {
      shutdownNotifier.shutdownIfNecessary();
      CFANode location = AbstractStates.extractLocation(abstractState);
      for (AutomatonState automatonState :
          AbstractStates.asIterable(abstractState).filter(AutomatonState.class)) {
        ExpressionTree<AExpression> candidate = automatonState.getCandidateInvariants();
        String groupId = automatonState.getInternalStateName();
        ExpressionTreeLocationInvariant previousInv = null;
        if (!candidate.equals(ExpressionTrees.getTrue())) {
          // search if we already have an location invariant at this location,
          // if so assign it to previousInv:
          for (ExpressionTreeLocationInvariant inv : invariants) {
            if (inv.getLocation().equals(location)) {
              previousInv = inv;
            }
          }

          // extract expression tree for already existing invariant
          // at this location (or true treeotherwise)
          ExpressionTree<AExpression> previousExpression = ExpressionTrees.getTrue();
          if (previousInv != null) {
            @SuppressWarnings("unchecked")
            ExpressionTree<AExpression> expr =
                (ExpressionTree<AExpression>) (ExpressionTree<?>) previousInv.asExpressionTree();
            previousExpression = expr;
            invariants.remove(previousInv);
          }

          // make an OR between already existing expression tree (if it exists)
          // and the invariant at the currently looked at abstract state
          if (previousExpression.equals(ExpressionTrees.getTrue())) {
            invariants.add(
                new ExpressionTreeLocationInvariant(
                    groupId, location, candidate, toCodeVisitorCache));
          } else {
            invariants.add(
                new ExpressionTreeLocationInvariant(
                    groupId, location, Or.of(previousExpression, candidate), toCodeVisitorCache));
          }
        }
      }
    }
    return invariants;
  }

  /**
   * Extracts the invariants from {@link WitnessInvariantsExtractor#reachedSet} and stores it in
   * {@code pCandidates}. The invariants are regarded as candidates that can hold at several CFA
   * locations. Therefore, {@code pCandidateGroupLocations} is used that groups CFANodes by using a
   * groupID. For two invariants that are part of the same group the conjunction is applied for the
   * two invariants.
   *
   * @param pCandidates stores the invariants which are regarded as candidates
   * @param pCandidateGroupLocations stores as key the groupID with its associated CFANodes
   */
  public void extractCandidatesFromReachedSet(
      final Set<CandidateInvariant> pCandidates,
      final Multimap<String, CFANode> pCandidateGroupLocations)
      throws InterruptedException {
    Set<ExpressionTreeLocationInvariant> expressionTreeLocationInvariants = new HashSet<>();
    Map<String, ExpressionTree<AExpression>> expressionTrees = new HashMap<>();
    Set<CFANode> visited = new HashSet<>();
    Multimap<CFANode, ExpressionTreeLocationInvariant> potentialAdditionalCandidates =
        HashMultimap.create();
    ConcurrentMap<ManagerKey, ToFormulaVisitor> toCodeVisitorCache = new ConcurrentHashMap<>();
    for (AbstractState abstractState : reachedSet) {
      shutdownNotifier.shutdownIfNecessary();
      Iterable<CFANode> locations = AbstractStates.extractLocations(abstractState);
      Iterables.addAll(visited, locations);
      for (AutomatonState automatonState :
          AbstractStates.asIterable(abstractState).filter(AutomatonState.class)) {
        ExpressionTree<AExpression> candidate = automatonState.getCandidateInvariants();
        String groupId = automatonState.getInternalStateName();
        pCandidateGroupLocations.putAll(groupId, locations);
        if (!candidate.equals(ExpressionTrees.getTrue())) {
          ExpressionTree<AExpression> previous = expressionTrees.get(groupId);
          if (previous == null) {
            previous = ExpressionTrees.getTrue();
          }
          expressionTrees.put(groupId, And.of(previous, candidate));
          for (CFANode location : locations) {
            potentialAdditionalCandidates.removeAll(location);
            ExpressionTreeLocationInvariant candidateInvariant =
                new ExpressionTreeLocationInvariant(
                    groupId, location, candidate, toCodeVisitorCache);
            expressionTreeLocationInvariants.add(candidateInvariant);
            // Check if there are any leaving return edges:
            // The predecessors are also potential matches for the invariant
            for (FunctionReturnEdge returnEdge :
                CFAUtils.leavingEdges(location).filter(FunctionReturnEdge.class)) {
              CFANode successor = returnEdge.getSuccessor();
              if (!pCandidateGroupLocations.containsEntry(groupId, successor)
                  && !visited.contains(successor)) {
                potentialAdditionalCandidates.put(
                    successor,
                    new ExpressionTreeLocationInvariant(
                        groupId, successor, candidate, toCodeVisitorCache));
              }
            }
          }
        }
      }
    }
    for (Map.Entry<CFANode, Collection<ExpressionTreeLocationInvariant>> potentialCandidates :
        potentialAdditionalCandidates.asMap().entrySet()) {
      if (!visited.contains(potentialCandidates.getKey())) {
        for (ExpressionTreeLocationInvariant candidateInvariant : potentialCandidates.getValue()) {
          pCandidateGroupLocations.put(
              candidateInvariant.getGroupId(), potentialCandidates.getKey());
          expressionTreeLocationInvariants.add(candidateInvariant);
        }
      }
    }
    for (ExpressionTreeLocationInvariant expressionTreeLocationInvariant :
        expressionTreeLocationInvariants) {
      for (CFANode location :
          pCandidateGroupLocations.get(expressionTreeLocationInvariant.getGroupId())) {
        pCandidates.add(
            new ExpressionTreeLocationInvariant(
                expressionTreeLocationInvariant.getGroupId(),
                location,
                expressionTrees.get(expressionTreeLocationInvariant.getGroupId()),
                toCodeVisitorCache));
      }
    }
  }
}
