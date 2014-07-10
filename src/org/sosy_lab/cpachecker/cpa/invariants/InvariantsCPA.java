/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState.EdgeBasedAbstractionStrategyFactories;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptAllVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptSpecifiedVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * This is a CPA for collecting simple invariants about integer variables.
 */
public class InvariantsCPA implements ConfigurableProgramAnalysis, ReachedSetAdjustingCPA {

  /**
   * A formula visitor for collecting the variables contained in a formula.
   */
  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  @Options(prefix="cpa.invariants")
  public static class InvariantsOptions {

    @Option(values={"JOIN", "SEP", "PRECISIONDEPENDENT"}, toUppercase=true,
        description="which merge operator to use for InvariantCPA")
    private String merge = "PRECISIONDEPENDENT";

    @Option(description="determine target locations in advance and analyse paths to the target locations only.")
    private boolean analyzeTargetPathsOnly = true;

    @Option(description="determine variables relevant to the decision whether or not a target path assume edge is taken and limit the analyis to those variables.")
    private boolean analyzeRelevantVariablesOnly = true;

    @Option(description="the maximum number of variables to consider as interesting. -1 one disables the limit, but this is not recommended. 0 means that guessing interesting variables is disabled.")
    private volatile int interestingVariableLimit = 2;

    @Option(description="the maximum tree depth of a formula recorded in the environment.")
    private int maximumFormulaDepth = 4;

    @Option(description="controls whether to use abstract evaluation always, never, or only on already previously visited edges.")
    private EdgeBasedAbstractionStrategyFactories edgeBasedAbstractionStrategyFactory = EdgeBasedAbstractionStrategyFactories.VISITED_EDGES;

    @Option(description="controls the condition adjustment logic: STATIC means that condition adjustment is a no-op, INTERESTING_VARIABLES increases the interesting variable limit, MAXIMUM_FORMULA_DEPTH increases the maximum formula depth, ABSTRACTION_STRATEGY tries to choose a more precise abstraction strategy and COMPOUND combines the other strategies (minus STATIC).")
    private ConditionAdjusterFactories conditionAdjusterFactory = ConditionAdjusterFactories.COMPOUND;

  }

  /**
   * The configured options.
   */
  private final InvariantsOptions options;

  /**
   * The configuration.
   */
  private final Configuration config;

  /**
   * The log manager used.
   */
  private final LogManager logManager;

  /**
   * The reached set factory used.
   */
  private final ReachedSetFactory reachedSetFactory;

  /**
   * The notifier that tells us when to stop.
   */
  private final ShutdownNotifier shutdownNotifier;

  /**
   * The analyzed control flow automaton.
   */
  private final CFA cfa;

  private final MachineModel machineModel;

  private final WeakHashMap<CFANode, InvariantsPrecision> initialPrecisionMap = new WeakHashMap<>();

  private boolean relevantVariableLimitReached = false;

  private final Map<CFANode, InvariantsState> invariants = new HashMap<>();

  private final ConditionAdjuster conditionAdjuster;

  private Map<CFANode, ImmutableSet<CFANode>> reachableTargetLocations = new HashMap<>();

  private final Set<String> interestingVariables = new LinkedHashSet<>();

  private final MergeOperator mergeOperator;

  /**
   * Gets a factory for creating InvariantCPAs.
   *
   * @return a factory for creating InvariantCPAs.
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(InvariantsCPA.class).withOptions(InvariantsOptions.class);
  }

  /**
   * Creates an InvariantCPA.
   *
   * @param pConfig the configuration used.
   * @param pLogManager the log manager used.
   * @param pOptions the configured options.
   * @param pShutdownNotifier the shutdown notifier used.
   * @param pReachedSetFactory the reached set factory used.
   * @param pCfa the control flow automaton to analyze.
   * @throws InvalidConfigurationException if the configuration is invalid.
   */
  public InvariantsCPA(Configuration pConfig, LogManager pLogManager, InvariantsOptions pOptions,
      ShutdownNotifier pShutdownNotifier, ReachedSetFactory pReachedSetFactory, CFA pCfa) throws InvalidConfigurationException {
    this.config = pConfig;
    this.logManager = pLogManager;
    this.shutdownNotifier = pShutdownNotifier;
    this.reachedSetFactory = pReachedSetFactory;
    this.cfa = pCfa;
    this.options = pOptions;
    this.conditionAdjuster = pOptions.conditionAdjusterFactory.createConditionAdjuster(this);
    this.machineModel = pCfa.getMachineModel();
    if (pOptions.merge.equalsIgnoreCase("precisiondependent")) {
      mergeOperator = new InvariantsMergeOperator();
    } else if (pOptions.merge.equalsIgnoreCase("sep")) {
      mergeOperator = MergeSepOperator.getInstance();
    } else {
      assert pOptions.merge.equalsIgnoreCase("join");
      mergeOperator = new MergeJoinOperator(InvariantsDomain.INSTANCE);
    }
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return InvariantsDomain.INSTANCE;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return InvariantsTransferRelation.INSTANCE;
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public InvariantsState getInitialState(CFANode pNode) {
    Set<CFANode> relevantLocations = new LinkedHashSet<>();
    Set<CFANode> targetLocations = new LinkedHashSet<>();

    int interestingVariableLimit = options.interestingVariableLimit;

    // Determine the target locations
    boolean determineTargetLocations = options.analyzeTargetPathsOnly || options.interestingVariableLimit != 0;
    if (determineTargetLocations) {
      targetLocations = tryGetTargetLocations(pNode);
      determineTargetLocations = targetLocations != null;
      if (targetLocations == null) {
        targetLocations = ImmutableSet.of();
      }
    }
    if (shutdownNotifier.shouldShutdown()) {
      return new InvariantsState(new AcceptAllVariableSelection<CompoundInterval>(), machineModel, options.edgeBasedAbstractionStrategyFactory.getAbstractionStrategy());
    }
    if (options.analyzeTargetPathsOnly && determineTargetLocations) {
      relevantLocations.addAll(targetLocations);
    } else {
      relevantLocations.addAll(cfa.getAllNodes());
    }

    // Collect relevant edges and guess that information might be interesting
    Set<CFAEdge> relevantEdges = new LinkedHashSet<>();
    Set<InvariantsFormula<CompoundInterval>> interestingPredicates = new LinkedHashSet<>();
    Set<String> interestingVariables;
    synchronized (this.interestingVariables) {
      interestingVariables = new LinkedHashSet<>(this.interestingVariables);
    }

    boolean guessInterestingInformation = options.interestingVariableLimit != 0;
    if (guessInterestingInformation && !determineTargetLocations) {
      logManager.log(Level.WARNING, "Target states were not determined. Guessing interesting information is arbitrary.");
    }

    // Iterate backwards from all relevant locations to find the relevant edges
    for (CFANode location : relevantLocations) {
      Queue<CFANode> nodes = new ArrayDeque<>();
      nodes.offer(location);
      while (!nodes.isEmpty()) {
        location = nodes.poll();
        for (int i = 0; i < location.getNumEnteringEdges(); ++i) {
          CFAEdge edge = location.getEnteringEdge(i);
          if (relevantEdges.add(edge)) {
            nodes.offer(edge.getPredecessor());
          }
        }
      }
    }

    if (shutdownNotifier.shouldShutdown()) {
      return new InvariantsState(new AcceptAllVariableSelection<CompoundInterval>(), machineModel, options.edgeBasedAbstractionStrategyFactory.getAbstractionStrategy());
    }

    // Try to specify all relevant variables
    Set<String> relevantVariables = new LinkedHashSet<>();
    boolean specifyRelevantVariables = options.analyzeRelevantVariablesOnly;

    final VariableSelection<CompoundInterval> variableSelection;
    if (specifyRelevantVariables) {
      // Collect all variables related to variables found on relevant assume edges from other edges with a fix point iteration
      expandFixpoint(relevantVariables, targetLocations, -1);
      for (String variable : relevantVariables) {
        if (interestingVariables.size() >= interestingVariableLimit) {
          break;
        }
        interestingVariables.add(variable);
        expandFixpoint(interestingVariables, targetLocations, interestingVariableLimit);
      }
      variableSelection = new AcceptSpecifiedVariableSelection<>(relevantVariables);
    } else {
      variableSelection = new AcceptAllVariableSelection<>();
    }

    // Remove predicates from the collection of interesting predicates that are already covered by the set of interesting variables
    Iterator<InvariantsFormula<CompoundInterval>> interestingPredicateIterator = interestingPredicates.iterator();
    while (interestingPredicateIterator.hasNext()) {
      InvariantsFormula<CompoundInterval> interestingPredicate = interestingPredicateIterator.next();
      List<String> containedUninterestingVariables = new ArrayList<>(interestingPredicate.accept(COLLECT_VARS_VISITOR));
      containedUninterestingVariables.removeAll(interestingVariables);
      if (containedUninterestingVariables.size() <= 1) {
        interestingPredicateIterator.remove();
      }
    }

    relevantVariableLimitReached = interestingVariableLimit > interestingVariables.size();

    InvariantsPrecision precision = new InvariantsPrecision(relevantEdges,
        ImmutableSet.copyOf(limit(interestingVariables, interestingVariableLimit)),
        options.maximumFormulaDepth,
        options.edgeBasedAbstractionStrategyFactory);

    initialPrecisionMap.put(pNode, precision);

    InvariantsState invariant = invariants.get(pNode);
    if (invariant != null) {
      return new InvariantsState(variableSelection, machineModel, invariant, options.edgeBasedAbstractionStrategyFactory.getAbstractionStrategy());
    }

    // Create the configured initial state
    return new InvariantsState(variableSelection, machineModel, options.edgeBasedAbstractionStrategyFactory.getAbstractionStrategy());
  }

  @Override
  public InvariantsPrecision getInitialPrecision(CFANode pNode) {
    InvariantsPrecision precision = initialPrecisionMap.get(pNode);
    if (precision != null) {
      return precision;
    }
    getInitialState(pNode);
    return initialPrecisionMap.get(pNode);
  }

  public ImmutableSet<CFANode> tryGetTargetLocations(CFANode pInitialNode) {
    ImmutableSet<CFANode> targetLocations = this.reachableTargetLocations.get(pInitialNode);
    if (targetLocations != null) {
      return targetLocations;
    }
    try {
      // Create new configuration based on existing config but with default set of CPAs
      String specificationPropertyName = "specification";
      ConfigurationBuilder configurationBuilder = extractOptionFrom(config, specificationPropertyName);
      configurationBuilder.setOption("output.disable", "true");
      configurationBuilder.setOption("CompositeCPA.cpas", "cpa.location.LocationCPA");
      if (config.getProperty(specificationPropertyName) == null) {
        String specification = "config/specification/default.spc";
        configurationBuilder.setOption(specificationPropertyName, specification);
      }
      Configuration configuration = configurationBuilder.build();
      ConfigurableProgramAnalysis cpa = new CPABuilder(configuration, logManager, shutdownNotifier, reachedSetFactory).buildCPAs(cfa);
      ReachedSet reached = reachedSetFactory.create();
      reached.add(cpa.getInitialState(pInitialNode), cpa.getInitialPrecision(pInitialNode));
      CPAAlgorithm targetFindingAlgorithm = CPAAlgorithm.create(cpa, logManager, configuration, shutdownNotifier);

      Set<CFANode> tmpTargetLocations = new HashSet<>();

      boolean changed = true;
      while (changed) {
        targetFindingAlgorithm.run(reached);
        changed = tmpTargetLocations.addAll(FluentIterable.from(reached).filter(AbstractStates.IS_TARGET_STATE).transform(AbstractStates.EXTRACT_LOCATION).toList());
      }

      targetLocations = ImmutableSet.copyOf(tmpTargetLocations);

      CPAs.closeCpaIfPossible(cpa, logManager);
      CPAs.closeIfPossible(targetFindingAlgorithm, logManager);
      this.reachableTargetLocations.put(pInitialNode, targetLocations);
      return targetLocations;
    } catch (InvalidConfigurationException | CPAException | InterruptedException e) {
      if (!shutdownNotifier.shouldShutdown()) {
        logManager.logException(Level.WARNING, e, "Unable to find target locations. Defaulting to selecting all locations.");
      }
      return null;
    }
  }

  public void injectInvariant(CFANode pLocation, InvariantsState pInvariant) {
    this.invariants.put(pLocation, pInvariant);
  }

  public void addInterestingVariables(Iterable<String> pInterestingVariables) {
    synchronized (this.interestingVariables) {
      Iterables.addAll(this.interestingVariables, pInterestingVariables);
    }
  }

  /**
   * Limits the given iterable by the given amount of elements. A limit below 0 means that
   * no limit is applied.
   *
   * @param pIterable the iterable to be limited.
   * @param pLimit the limit.
   * @return the limited iterable.
   */
  private static <T> Iterable<T> limit(Iterable<T> pIterable, int pLimit) {
    if (pLimit >= 0) {
      return FluentIterable.from(pIterable).limit(pLimit);
    }
    return pIterable;
  }

  @Override
  public boolean adjustPrecision() {
    return conditionAdjuster.adjustConditions();
  }

  @Override
  public void adjustReachedSet(ReachedSet pReachedSet) {
    conditionAdjuster.adjustReachedSet(pReachedSet);
  }

  private static <T> boolean reachesLimit(Collection<T> pCollection, int pLimit) {
    return pLimit >= 0 && pCollection.size() >= pLimit;
  }

  private static void expandFixpoint(Set<String> pRelevantVariables, Set<CFANode> pRelevantLocations, int pLimit) {
    for (CFANode relevantLocation : pRelevantLocations) {
      expandFixpoint(pRelevantVariables, relevantLocation, pLimit);
    }
  }

  private static void expandFixpoint(Set<String> pRelevantVariables, CFANode pRelevantLocation, int pLimit) {
    int prevSize = -1;
    while (pRelevantVariables.size() > prevSize && !reachesLimit(pRelevantVariables, pLimit)) {
      prevSize = pRelevantVariables.size();
      expandOnce(pRelevantVariables, pRelevantLocation, pLimit);
    }
  }

  private static void expandOnce(Set<String> pRelevantVariables, CFANode pRelevantLocation, int pLimit) {

    Set<CFANode> pVisitedNodes = new HashSet<>();

    Queue<CFANode> relevantLocations = new ArrayDeque<>();
    pVisitedNodes.add(pRelevantLocation);
    relevantLocations.offer(pRelevantLocation);
    while (!relevantLocations.isEmpty() && !reachesLimit(pRelevantVariables, pLimit)) {
      CFANode currentRelevantLocation = relevantLocations.poll();

      Set<Pair<AssumeEdge, List<CFAEdge>>> assumeEdgesAndPaths = new HashSet<>();

      Queue<Pair<CFANode, List<CFAEdge>>> waitlist = new ArrayDeque<>();
      waitlist.offer(Pair.of(currentRelevantLocation, Collections.<CFAEdge>emptyList()));

      while (!waitlist.isEmpty()) {
        Pair<CFANode, List<CFAEdge>> currentPair = waitlist.poll();
        CFANode currentNode = currentPair.getFirst();
        List<CFAEdge> currentPath = currentPair.getSecond();
        for (CFAEdge enteringEdge : CFAUtils.enteringEdges(currentNode)) {
          if (enteringEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
            assumeEdgesAndPaths.add(Pair.of((AssumeEdge) enteringEdge, currentPath));
          } else if (pVisitedNodes.add(enteringEdge.getPredecessor())) {
            List<CFAEdge> newPath = new ArrayList<>(currentPath);
            newPath.add(enteringEdge);
            waitlist.offer(Pair.of(enteringEdge.getPredecessor(), newPath));
            addTransitivelyRelevantInvolvedVariables(pRelevantVariables, enteringEdge, pLimit);
          }
        }
      }

      for (Pair<AssumeEdge, List<CFAEdge>> assumeEdgeAndPath : assumeEdgesAndPaths) {
        AssumeEdge assumeEdge = assumeEdgeAndPath.getFirst();
        CFANode predecessor = assumeEdge.getPredecessor();
        if (pVisitedNodes.add(predecessor)) {
          addTransitivelyRelevantInvolvedVariables(pRelevantVariables, assumeEdge, pLimit);
          for (CFAEdge sisterEdge : CFAUtils.leavingEdges(predecessor)) {
            if (!assumeEdge.equals(sisterEdge)) {
              CFANode brotherNode = sisterEdge.getSuccessor();
              if (!mustReach(brotherNode, currentRelevantLocation, assumeEdge)
                  || anyOnPath(assumeEdgeAndPath.getSecond(), pRelevantVariables)) {
                addInvolvedVariables(pRelevantVariables, assumeEdge, pLimit);
              }
            }
          }
          relevantLocations.add(predecessor);
        }
      }
    }
  }

  private static boolean anyOnPath(List<CFAEdge> pPath, Set<String> pRelevantVariables) {
    for (CFAEdge edge : pPath) {
      if (!Collections.disjoint(InvariantsTransferRelation.INSTANCE.getInvolvedVariables(edge).keySet(), pRelevantVariables)) {
        return true;
      }
    }
    return false;
  }

  private static boolean mustReach(CFANode pStart, final CFANode pTarget, final CFAEdge pForbiddenEdge) {
    Set<CFANode> visited = new HashSet<>();
    visited.add(pStart);
    Queue<CFANode> waitlist = new ArrayDeque<>();
    waitlist.offer(pStart);
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      if (!current.equals(pTarget)) {
        FluentIterable<CFAEdge> leavingEdges = CFAUtils.leavingEdges(current);
        boolean continued = false;
        for (CFAEdge leavingEdge : leavingEdges) {
          if (!leavingEdge.equals(pForbiddenEdge)) {
            CFANode successor = leavingEdge.getSuccessor();
            if (continued |= visited.add(successor)) {
              waitlist.offer(successor);
            }
          }
        }
        if (!continued) {
          return false;
        }
      }
    }
    return true;
  }

  private static void addTransitivelyRelevantInvolvedVariables(Set<String> pRelevantVariables, CFAEdge pEdge, int pLimit) {
    Set<String> involvedVariables = InvariantsTransferRelation.INSTANCE.getInvolvedVariables(pEdge).keySet();
    if (!Collections.disjoint(pRelevantVariables, involvedVariables)) {
      addAll(pRelevantVariables, involvedVariables, pLimit);
    }
  }

  private static void addInvolvedVariables(Set<String> pRelevantVariables, CFAEdge pEdge, int pLimit) {
    addAll(pRelevantVariables, InvariantsTransferRelation.INSTANCE.getInvolvedVariables(pEdge).keySet(), pLimit);
  }

  private static <T> void addAll(Collection<T> pTarget, Collection<T> pSource, int pLimit) {
    Iterator<T> elementIterator = pSource.iterator();
    while (!reachesLimit(pTarget, pLimit) && elementIterator.hasNext()) {
      pTarget.add(elementIterator.next());
    }
  }

  public static interface ConditionAdjuster {

    boolean adjustConditions();

    void adjustReachedSet(ReachedSet pReachedSet);

  }

  private static interface ValueIncreasingAdjuster extends ConditionAdjuster {

    int getInc();

    void setInc(int pInc);

  }

  public interface ConditionAdjusterFactory {

    ConditionAdjuster createConditionAdjuster(InvariantsCPA pCPA);

  }

  public enum ConditionAdjusterFactories implements ConditionAdjusterFactory {

    STATIC {

      @Override
      public ConditionAdjuster createConditionAdjuster(final InvariantsCPA pCPA) {
        return new ConditionAdjuster() {

          @Override
          public boolean adjustConditions() {
            return true;
          }

          @Override
          public void adjustReachedSet(ReachedSet pReachedSet) {
            // No actions required
          }
        };
      }

    },

    INTERESTING_VARIABLES {

      @Override
      public ConditionAdjuster createConditionAdjuster(final InvariantsCPA pCPA) {
        return new InterestingVariableLimitAdjuster(pCPA);
      }

    },

    MAXIMUM_FORMULA_DEPTH {

      @Override
      public ConditionAdjuster createConditionAdjuster(final InvariantsCPA pCPA) {
        return new FormulaDepthAdjuster(pCPA);
      }

    },

    ABSTRACTION_STRATEGY {

      @Override
      public ConditionAdjuster createConditionAdjuster(final InvariantsCPA pCPA) {
        return new AbstractionStrategyAdjuster(pCPA);
      }

    },

    COMPOUND {

      @Override
      public ConditionAdjuster createConditionAdjuster(final InvariantsCPA pCPA) {
        return new CompoundConditionAdjuster(pCPA);
      }

    };

  }

  private static class CompoundConditionAdjuster implements ConditionAdjuster {

    private Timer timer = new Timer();

    private TimeSpan previousTimeSpan = null;

    private Deque<ValueIncreasingAdjuster> innerAdjusters = new ArrayDeque<>();

    private ConditionAdjuster defaultInner;

    public CompoundConditionAdjuster(InvariantsCPA pCPA) {
      innerAdjusters.add(new InterestingVariableLimitAdjuster(pCPA));
      innerAdjusters.add(new FormulaDepthAdjuster(pCPA));
      defaultInner = new AbstractionStrategyAdjuster(pCPA);
    }

    @Override
    public boolean adjustConditions() {
      if (!hasInner()) {
        return defaultInner.adjustConditions();
      }
      ValueIncreasingAdjuster inner = getCurrentInner();
      if (previousTimeSpan != null) {
        timer.stop();
        TimeSpan sinceLastAdjustment = timer.getLengthOfLastInterval();
        int comp = sinceLastAdjustment.compareTo(previousTimeSpan);
        int inc = inner.getInc();
        if (comp < 0) {
          inc *= 2;
        } else if (comp > 0 && inc > 1) {
          inc /= 2;
          swapInner();
        }
        inner.setInc(inc);
        previousTimeSpan = sinceLastAdjustment;
      } else if (timer.isRunning()) {
        timer.stop();
        previousTimeSpan = timer.getLengthOfLastInterval();
      }
      timer.start();
      boolean result = inner.adjustConditions();
      if (!result) {
        this.innerAdjusters.remove(inner);
        return adjustConditions();
      }
      return result;
    }

    @Override
    public void adjustReachedSet(ReachedSet pReachedSet) {
      if (hasInner()) {
        getCurrentInner().adjustReachedSet(pReachedSet);
      } else {
        defaultInner.adjustReachedSet(pReachedSet);
      }
    }

    private boolean hasInner() {
      return !innerAdjusters.isEmpty();
    }

    private ValueIncreasingAdjuster getCurrentInner() {
      Preconditions.checkArgument(hasInner());
      return innerAdjusters.getFirst();
    }

    private void swapInner() {
      if (hasInner()) {
        innerAdjusters.addLast(innerAdjusters.removeFirst());
      }
    }

  }

  private static class InterestingVariableLimitAdjuster implements ValueIncreasingAdjuster {

    private final InvariantsCPA cpa;

    private int inc = 1;

    private InterestingVariableLimitAdjuster(InvariantsCPA pCPA) {
      cpa = pCPA;
    }

    @Override
    public boolean adjustConditions() {
      if (cpa.relevantVariableLimitReached) {
        return false;
      }
      cpa.initialPrecisionMap.clear();
      cpa.options.interestingVariableLimit += inc;
      cpa.logManager.log(Level.INFO, "Adjusting interestingVariableLimit to", cpa.options.interestingVariableLimit);
      return true;
    }

    @Override
    public void adjustReachedSet(ReachedSet pReachedSet) {
      pReachedSet.clear();
    }

    @Override
    public int getInc() {
      return this.inc;
    }

    @Override
    public void setInc(int pInc) {
      Preconditions.checkArgument(pInc > 0);
      this.inc = pInc;
    }
  }

  private static class FormulaDepthAdjuster implements ValueIncreasingAdjuster {

    private final InvariantsCPA cpa;

    private int inc = 1;

    private FormulaDepthAdjuster(InvariantsCPA pCPA) {
      cpa = pCPA;
    }

    @Override
    public boolean adjustConditions() {
      if (cpa.options.maximumFormulaDepth >= 2) {
        return false;
      }
      cpa.initialPrecisionMap.clear();
      cpa.options.maximumFormulaDepth += inc;
      cpa.logManager.log(Level.INFO, "Adjusting maximum formula depth to", cpa.options.maximumFormulaDepth);
      return true;
    }

    @Override
    public void adjustReachedSet(ReachedSet pReachedSet) {
      pReachedSet.clear();
    }

    @Override
    public int getInc() {
      return this.inc;
    }

    @Override
    public void setInc(int pInc) {
      Preconditions.checkArgument(pInc > 0);
      this.inc = pInc;
    }
  }

  private static class AbstractionStrategyAdjuster implements ConditionAdjuster {

    private final InvariantsCPA cpa;

    public AbstractionStrategyAdjuster(InvariantsCPA pCPA) {
      this.cpa = pCPA;
    }

    @Override
    public boolean adjustConditions() {
      if (cpa.options.edgeBasedAbstractionStrategyFactory == EdgeBasedAbstractionStrategyFactories.ALWAYS) {
        cpa.options.edgeBasedAbstractionStrategyFactory = EdgeBasedAbstractionStrategyFactories.VISITED_EDGES;
      } else if (cpa.options.edgeBasedAbstractionStrategyFactory == EdgeBasedAbstractionStrategyFactories.VISITED_EDGES) {
        cpa.options.edgeBasedAbstractionStrategyFactory = EdgeBasedAbstractionStrategyFactories.NEVER;
      } else {
        return false;
      }
      cpa.logManager.log(Level.INFO, "Adjusting abstraction strategy to", cpa.options.edgeBasedAbstractionStrategyFactory);
      return true;
    }

    @Override
    public void adjustReachedSet(ReachedSet pReachedSet) {
      pReachedSet.clear();
    }

  }

  private static ConfigurationBuilder extractOptionFrom(Configuration pConfiguration, String pKey) {
    ConfigurationBuilder builder = Configuration.builder().copyFrom(pConfiguration);
    try (Scanner pairScanner = new Scanner(pConfiguration.asPropertiesString())) {
      pairScanner.useDelimiter("\\s+");
      while (pairScanner.hasNext()) {
        String pair = pairScanner.next();
        try (Scanner keyScanner = new Scanner(pair)) {
          keyScanner.useDelimiter("\\s*=\\s*.*");
          if (keyScanner.hasNext()) {
            String key = keyScanner.next();
            if (!key.equals(pKey)) {
              builder.clearOption(key);
            }
          }
        }
      }
    }
    return builder;
  }

}