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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import javax.annotation.concurrent.GuardedBy;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
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
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.NumeralFormula;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptAllVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptSpecifiedVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This is a CPA for collecting simple invariants about integer variables.
 */
public class InvariantsCPA implements ConfigurableProgramAnalysis, ReachedSetAdjustingCPA, StatisticsProvider {

  /**
   * A formula visitor for collecting the variables contained in a formula.
   */
  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  @Options(prefix="cpa.invariants")
  public static class InvariantsOptions {

    @Option(secure=true, values={"JOIN", "SEP", "PRECISIONDEPENDENT"}, toUppercase=true,
        description="which merge operator to use for InvariantCPA")
    private String merge = "PRECISIONDEPENDENT";

    @Option(secure=true, description="determine target locations in advance and analyse paths to the target locations only.")
    private boolean analyzeTargetPathsOnly = true;

    @Option(secure=true, description="determine variables relevant to the decision whether or not a target path assume edge is taken and limit the analyis to those variables.")
    private boolean analyzeRelevantVariablesOnly = true;

    @Option(secure=true, description="the maximum number of variables to consider as interesting. -1 one disables the limit, but this is not recommended. 0 means that no variables are considered to be interesting.")
    private volatile int interestingVariableLimit = 2;

    @Option(
      secure = true,
      description =
          "the maximum number of adjustments of the interestingVariableLimit. -1 one disables the limit"
    )
    @IntegerOption(min = -1)
    private volatile int maxInterestingVariableAdjustments = -1;

    @Option(secure=true, description="the maximum tree depth of a formula recorded in the environment.")
    private volatile int maximumFormulaDepth = 4;

    @Option(secure=true, description="controls whether to use abstract evaluation always, never, or depending on entering edges.")
    private AbstractionStrategyFactories abstractionStateFactory = AbstractionStrategyFactories.ENTERING_EDGES;

    @Option(secure=true, description="controls the condition adjustment logic: STATIC means that condition adjustment is a no-op, INTERESTING_VARIABLES increases the interesting variable limit, MAXIMUM_FORMULA_DEPTH increases the maximum formula depth, ABSTRACTION_STRATEGY tries to choose a more precise abstraction strategy and COMPOUND combines the other strategies (minus STATIC).")
    private ConditionAdjusterFactories conditionAdjusterFactory = ConditionAdjusterFactories.COMPOUND;

    @Option(secure=true, description="include type information for variables, such as x >= MIN_INT && x <= MAX_INT")
    private boolean includeTypeInformation = true;
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
   * The target location provider used.
   */
  private final TargetLocationProvider targetLocationProvider;

  /**
   * The notifier that tells us when to stop.
   */
  private final ShutdownNotifier shutdownNotifier;

  /**
   * The analyzed control flow automaton.
   */
  private final CFA cfa;

  private final MachineModel machineModel;

  private final Specification specification;

  private final WeakHashMap<CFANode, InvariantsPrecision> initialPrecisionMap = new WeakHashMap<>();

  private boolean relevantVariableLimitReached = false;

  private final Map<CFANode, BooleanFormula<CompoundInterval>> invariants
      = Collections.synchronizedMap(new HashMap<>());

  private final ConditionAdjuster conditionAdjuster;

  @GuardedBy("itself")
  private final Set<MemoryLocation> interestingVariables = new LinkedHashSet<>();

  private final MergeOperator mergeOperator;
  private final AbstractDomain abstractDomain;

  private final StateToFormulaWriter writer;

  private final CompoundIntervalManagerFactory compoundIntervalManagerFactory = CompoundBitVectorIntervalManagerFactory.FORBID_SIGNED_WRAP_AROUND;

  private final EdgeAnalyzer edgeAnalyzer;

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
   * @param pCfa the control flow automaton to analyze.
   * @throws InvalidConfigurationException if the configuration is invalid.
   */
  public InvariantsCPA(
      Configuration pConfig,
      LogManager pLogManager,
      InvariantsOptions pOptions,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {
    this.config = pConfig;
    this.logManager = pLogManager;
    this.shutdownNotifier = pShutdownNotifier;
    this.cfa = pCfa;
    this.specification = checkNotNull(pSpecification);
    this.targetLocationProvider =
        new CachingTargetLocationProvider(shutdownNotifier, logManager, cfa);
    this.options = pOptions;
    this.conditionAdjuster = pOptions.conditionAdjusterFactory.createConditionAdjuster(this);
    this.machineModel = pCfa.getMachineModel();
    abstractDomain = DelegateAbstractDomain.<InvariantsState>getInstance();
    if (pOptions.merge.equalsIgnoreCase("precisiondependent")) {
      mergeOperator = new InvariantsMergeOperator();
    } else if (pOptions.merge.equalsIgnoreCase("sep")) {
      mergeOperator = MergeSepOperator.getInstance();
    } else {
      assert pOptions.merge.equalsIgnoreCase("join");
      mergeOperator = new MergeJoinOperator(abstractDomain);
    }
    this.writer = new StateToFormulaWriter(config, logManager, shutdownNotifier, cfa);
    this.edgeAnalyzer = new EdgeAnalyzer(compoundIntervalManagerFactory, machineModel);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new InvariantsTransferRelation(compoundIntervalManagerFactory, machineModel);
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
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    Set<CFANode> relevantLocations = new LinkedHashSet<>();
    Set<CFANode> targetLocations = new LinkedHashSet<>();

    int interestingVariableLimit = options.interestingVariableLimit;

    // Determine the target locations
    boolean determineTargetLocations = options.analyzeTargetPathsOnly || options.interestingVariableLimit > 0;
    if (determineTargetLocations) {
      targetLocations = targetLocationProvider.tryGetAutomatonTargetLocations(pNode, specification);
      determineTargetLocations = targetLocations != null;
      if (targetLocations == null) {
        targetLocations = ImmutableSet.of();
      }
    }

    shutdownNotifier.shutdownIfNecessary();

    AbstractionState abstractionState =
        options.abstractionStateFactory
          .createStrategy(compoundIntervalManagerFactory, machineModel)
          .getAbstractionState();

    shutdownNotifier.shutdownIfNecessary();

    if (options.analyzeTargetPathsOnly && determineTargetLocations) {
      relevantLocations.addAll(targetLocations);
    } else {
      relevantLocations.addAll(cfa.getAllNodes());
    }

    // Collect relevant edges and guess that information might be interesting
    Set<CFAEdge> relevantEdges = new LinkedHashSet<>();
    Set<NumeralFormula<CompoundInterval>> interestingPredicates = new LinkedHashSet<>();
    Set<MemoryLocation> interestingVariables;
    synchronized (this.interestingVariables) {
      interestingVariables = new LinkedHashSet<>(this.interestingVariables);
    }

    if (interestingVariableLimit > 0 && !determineTargetLocations) {
      logManager.log(Level.WARNING, "Target states were not determined. Guessing interesting information is arbitrary.");
    }

    // Iterate backwards from all relevant locations to find the relevant edges
    for (CFANode location : relevantLocations) {
      Queue<CFANode> nodes = new ArrayDeque<>();
      nodes.offer(location);
      while (!nodes.isEmpty()) {
        shutdownNotifier.shutdownIfNecessary();

        location = nodes.poll();
        for (CFAEdge edge : CFAUtils.enteringEdges(location)) {
          if (relevantEdges.add(edge)) {
            nodes.offer(edge.getPredecessor());
          }
        }
      }
    }

    // Try to specify all relevant variables
    Set<MemoryLocation> relevantVariables = new LinkedHashSet<>();
    boolean specifyRelevantVariables = options.analyzeRelevantVariablesOnly;

    final VariableSelection<CompoundInterval> variableSelection;
    if (specifyRelevantVariables) {
      // Collect all variables related to variables found on relevant assume edges from other edges with a fix point iteration
      expandFixpoint(relevantVariables, targetLocations, -1);
      for (MemoryLocation variable : relevantVariables) {
        if (interestingVariableLimit >= 0 && interestingVariables.size() >= interestingVariableLimit) {
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
    Iterator<NumeralFormula<CompoundInterval>> interestingPredicateIterator = interestingPredicates.iterator();
    while (interestingPredicateIterator.hasNext()) {
      NumeralFormula<CompoundInterval> interestingPredicate = interestingPredicateIterator.next();
      List<MemoryLocation> containedUninterestingVariables = new ArrayList<>(interestingPredicate.accept(COLLECT_VARS_VISITOR));
      containedUninterestingVariables.removeAll(interestingVariables);
      if (containedUninterestingVariables.size() <= 1) {
        interestingPredicateIterator.remove();
      }
    }

    relevantVariableLimitReached = interestingVariableLimit < 0 || interestingVariableLimit > interestingVariables.size();

    InvariantsPrecision precision = new InvariantsPrecision(relevantEdges,
        ImmutableSet.copyOf(limit(interestingVariables, interestingVariableLimit)),
        options.maximumFormulaDepth,
        options.abstractionStateFactory.createStrategy(
            compoundIntervalManagerFactory,
            machineModel));

    initialPrecisionMap.put(pNode, precision);

    BooleanFormula<CompoundInterval> invariant = invariants.get(pNode);
    if (invariant != null) {
      InvariantsState state = new InvariantsState(
          variableSelection,
          compoundIntervalManagerFactory,
          machineModel,
          abstractionState,
          false,
          options.includeTypeInformation);
      state = state.assume(invariant);
    }

    // Create the configured initial state
    return new InvariantsState(
        variableSelection,
        compoundIntervalManagerFactory,
        machineModel,
        abstractionState,
        false,
        options.includeTypeInformation);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) throws InterruptedException {
    InvariantsPrecision precision = initialPrecisionMap.get(pNode);
    if (precision != null) {
      return precision;
    }
    getInitialState(pNode, pPartition);
    precision = initialPrecisionMap.get(pNode);

    // If no precision was mapped to the state, use the empty precision
    if (precision == null) {
      return InvariantsPrecision.getEmptyPrecision(
          options.abstractionStateFactory.createStrategy(
              compoundIntervalManagerFactory,
              machineModel));
    }
    return precision;
  }

  public void injectInvariant(CFANode pLocation, AssumeEdge pAssumption) throws UnrecognizedCodeException {
    if (pAssumption instanceof CAssumeEdge) {
      CAssumeEdge assumeEdge = (CAssumeEdge) pAssumption;
      MemoryLocationExtractor vne = new MemoryLocationExtractor(
          compoundIntervalManagerFactory,
          machineModel,
          pAssumption);
      ExpressionToFormulaVisitor etfv = new ExpressionToFormulaVisitor(compoundIntervalManagerFactory, machineModel, vne);
      CompoundIntervalFormulaManager compoundIntervalFormulaManager = new CompoundIntervalFormulaManager(compoundIntervalManagerFactory);
      BooleanFormula<CompoundInterval> assumption = compoundIntervalFormulaManager.fromNumeral(assumeEdge.getExpression().accept(etfv));
      if (!pAssumption.getTruthAssumption()) {
        assumption = compoundIntervalFormulaManager.logicalNot(assumption);
      }
      injectInvariant(pLocation, assumption);
    }
  }

  public void injectInvariant(CFANode pLocation, BooleanFormula<CompoundInterval> pAssumption) {
    invariants.put(pLocation, pAssumption);
  }

  public void addInterestingVariables(Iterable<MemoryLocation> pInterestingVariables) {
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

  private void expandFixpoint(Set<MemoryLocation> pRelevantVariables, Set<CFANode> pRelevantLocations, int pLimit) throws InterruptedException {
    for (CFANode relevantLocation : pRelevantLocations) {
      expandFixpoint(pRelevantVariables, relevantLocation, pLimit);
    }
  }

  private void expandFixpoint(Set<MemoryLocation> pRelevantVariables, CFANode pRelevantLocation, int pLimit) throws InterruptedException {
    int prevSize = -1;
    while (pRelevantVariables.size() > prevSize && !reachesLimit(pRelevantVariables, pLimit)) {
      // we cannot throw an interrupted exception during #getInitialState, but the analysis
      // will be shutdown afterwards by another notifier so we can safely end computation here
      shutdownNotifier.shutdownIfNecessary();
      prevSize = pRelevantVariables.size();
      expandOnce(pRelevantVariables, pRelevantLocation, pLimit);
    }
  }

  private void expandOnce(Set<MemoryLocation> pRelevantVariables, CFANode pRelevantLocation, int pLimit) {

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

  private boolean anyOnPath(List<CFAEdge> pPath, Set<MemoryLocation> pRelevantVariables) {
    for (CFAEdge edge : pPath) {
      if (!Collections.disjoint(edgeAnalyzer.getInvolvedVariableTypes(edge).keySet(), pRelevantVariables)) {
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

  private void addTransitivelyRelevantInvolvedVariables(Set<MemoryLocation> pRelevantVariables, CFAEdge pEdge, int pLimit) {
    Set<MemoryLocation> involvedVariables = edgeAnalyzer.getInvolvedVariableTypes(pEdge).keySet();
    if (!Collections.disjoint(pRelevantVariables, involvedVariables)) {
      addAll(pRelevantVariables, involvedVariables, pLimit);
    }
  }

  private void addInvolvedVariables(Set<MemoryLocation> pRelevantVariables, CFAEdge pEdge, int pLimit) {
    addAll(pRelevantVariables, edgeAnalyzer.getInvolvedVariableTypes(pEdge).keySet(), pLimit);
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
            return false;
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

    }

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
    private int amountAdjustments = 0;

    private InterestingVariableLimitAdjuster(InvariantsCPA pCPA) {
      cpa = pCPA;
    }

    @Override
    public boolean adjustConditions() {
      if (cpa.relevantVariableLimitReached
          || (amountAdjustments >= cpa.options.maxInterestingVariableAdjustments
              && cpa.options.maxInterestingVariableAdjustments >= 0)) {
        return false;
      }
      amountAdjustments++;

      cpa.initialPrecisionMap.clear();
      synchronized (cpa) {
        cpa.options.interestingVariableLimit += inc;
      }
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
      synchronized (cpa) {
        cpa.options.maximumFormulaDepth += inc;
      }
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
      if (cpa.options.abstractionStateFactory == AbstractionStrategyFactories.ALWAYS) {
        cpa.options.abstractionStateFactory = AbstractionStrategyFactories.ENTERING_EDGES;
      } else if (cpa.options.abstractionStateFactory == AbstractionStrategyFactories.ENTERING_EDGES) {
        cpa.options.abstractionStateFactory = AbstractionStrategyFactories.NEVER;
      } else {
        return false;
      }
      cpa.logManager.log(Level.INFO, "Adjusting abstraction strategy to", cpa.options.abstractionStateFactory);
      return true;
    }

    @Override
    public void adjustReachedSet(ReachedSet pReachedSet) {
      pReachedSet.clear();
    }

  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    writer.collectStatistics(pStatsCollection);
  }
}
