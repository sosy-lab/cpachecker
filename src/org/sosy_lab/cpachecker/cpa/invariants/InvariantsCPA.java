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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState.EdgeBasedAbstractionStrategyFactories;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor.VariableNameExtractor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.LogicalNot;
import org.sosy_lab.cpachecker.cpa.invariants.formula.SplitDisjunctionsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptAllVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.AcceptSpecifiedVariableSelection;
import org.sosy_lab.cpachecker.cpa.invariants.variableselection.VariableSelection;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;

/**
 * This is a CPA for collecting simple invariants about integer variables.
 */
public class InvariantsCPA extends AbstractCPA implements AdjustableConditionCPA {

  /**
   * A formula visitor for collecting the variables contained in a formula.
   */
  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  @Options(prefix="cpa.invariants")
  public static class InvariantsOptions {

    @Option(values={"JOIN", "SEP"}, toUppercase=true,
        description="which merge operator to use for InvariantCPA")
    private String merge = "JOIN";

    @Option(description="determine target locations in advance and analyse paths to the target locations only.")
    private boolean analyzeTargetPathsOnly = true;

    @Option(description="determine variables relevant to the decision whether or not a target path assume edge is taken and limit the analyis to those variables.")
    private boolean analyzeRelevantVariablesOnly = true;

    @Option(description="the maximum number of predicates to consider as interesting. -1 one disables the limit, but this is not recommended. 0 means that guessing interesting predicates is disabled.")
    private int interestingPredicatesLimit = 0;

    @Option(description="the maximum number of variables to consider as interesting. -1 one disables the limit, but this is not recommended. 0 means that guessing interesting variables is disabled.")
    private int interestingVariableLimit = 2;

    @Option(description="the maximum tree depth of a formula recorded in the environment.")
    private int maximumFormulaDepth = 4;

    @Option(description="whether or not to collect information about binary variable interrelations.")
    private boolean useBinaryVariableInterrelations = true;

    @Option(description="whether or not to use a bit vector formula manager when extracting invariant approximations from states.")
    private boolean useBitvectors = false;

    @Option(description="controls whether to use abstract evaluation always, never, or only on already previously visited edges.")
    private EdgeBasedAbstractionStrategyFactories edgeBasedAbstractionStrategyFactory = EdgeBasedAbstractionStrategyFactories.VISITED_EDGES;

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

  private final WeakHashMap<CFANode, InvariantsPrecision> initialPrecisionMap = new WeakHashMap<>();

  private boolean relevantVariableLimitReached = false;

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
    super(pOptions.merge, "sep", InvariantsDomain.INSTANCE, InvariantsTransferRelation.INSTANCE);
    this.config = pConfig;
    this.logManager = pLogManager;
    this.shutdownNotifier = pShutdownNotifier;
    this.reachedSetFactory = pReachedSetFactory;
    this.cfa = pCfa;
    this.options = pOptions;
  }

  @Override
  public InvariantsState getInitialState(CFANode pNode) {
    Set<CFANode> relevantLocations = new LinkedHashSet<>();
    Set<CFANode> targetLocations = new LinkedHashSet<>();

    // Determine the target locations
    boolean determineTargetLocations = options.analyzeTargetPathsOnly || options.interestingPredicatesLimit != 0 || options.interestingVariableLimit != 0;
    if (determineTargetLocations) {
      try {
        // Create new configuration based on existing config but with default set of CPAs
        Configuration configuration = Configuration.builder()
                                             .copyFrom(config)
                                             .setOption("output.disable", "true")
                                             .clearOption("cpa")
                                             .clearOption("cpas")
                                             .clearOption("CompositeCPA.cpas")
                                             .build();
        ConfigurableProgramAnalysis cpa = new CPABuilder(configuration, logManager, shutdownNotifier, reachedSetFactory).buildCPAs(cfa);
        ReachedSet reached = reachedSetFactory.create();
        reached.add(cpa.getInitialState(pNode), cpa.getInitialPrecision(pNode));
        CPAAlgorithm targetFindingAlgorithm = CPAAlgorithm.create(cpa, logManager, config, shutdownNotifier);

        boolean changed = true;
        while (changed) {
          targetFindingAlgorithm.run(reached);
          changed = targetLocations.addAll(FluentIterable.from(reached).filter(AbstractStates.IS_TARGET_STATE).transform(AbstractStates.EXTRACT_LOCATION).toList());
        }
        CPAs.closeCpaIfPossible(cpa, logManager);
        CPAs.closeIfPossible(targetFindingAlgorithm, logManager);
      } catch (InvalidConfigurationException | CPAException | InterruptedException e) {
        if (!shutdownNotifier.shouldShutdown()) {
          logManager.logException(Level.WARNING, e, "Unable to find target locations. Defaulting to selecting all locations.");
        }
        determineTargetLocations = false;
      }
    }
    if (shutdownNotifier.shouldShutdown()) {
      return new InvariantsState(options.useBitvectors, new AcceptAllVariableSelection<CompoundInterval>(), InvariantsPrecision.NONE);
    }
    if (options.analyzeTargetPathsOnly && determineTargetLocations) {
      relevantLocations.addAll(targetLocations);
    } else {
      relevantLocations.addAll(cfa.getAllNodes());
    }

    // Collect relevant edges and guess that information might be interesting
    Set<CFAEdge> relevantEdges = new HashSet<>();
    Set<InvariantsFormula<CompoundInterval>> interestingPredicates = new LinkedHashSet<>();
    Set<String> interestingVariables = new LinkedHashSet<>();

    boolean guessInterestingInformation = options.interestingPredicatesLimit != 0 || options.interestingVariableLimit != 0;
    if (guessInterestingInformation && !determineTargetLocations) {
      logManager.log(Level.WARNING, "Target states were not determined. Guessing interesting information is arbitrary.");
    }

    // Iterate backwards from all relevant locations to find the relevant edges
    for (CFANode location : relevantLocations) {
      Queue<CFANode> nodes = new ArrayDeque<>();
      Queue<Integer> distances = new ArrayDeque<>();
      nodes.offer(location);
      distances.offer(0);
      while (!nodes.isEmpty()) {
        location = nodes.poll();
        int distance = distances.poll();
        for (int i = 0; i < location.getNumEnteringEdges(); ++i) {
          CFAEdge edge = location.getEnteringEdge(i);
          if (relevantEdges.add(edge)) {
            nodes.offer(edge.getPredecessor());
            if (edge instanceof AssumeEdge) {
              if (guessInterestingInformation) {
                try {
                  InvariantsFormula<CompoundInterval> formula = ((CAssumeEdge) edge).getExpression().accept(InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(edge));
                  if (options.interestingVariableLimit != 0) {
                    addAll(interestingVariables, formula.accept(COLLECT_VARS_VISITOR), options.interestingVariableLimit);
                  }
                  if (options.interestingPredicatesLimit != 0) {
                    if (formula instanceof LogicalNot<?>) { // We don't care about negations here
                      formula = ((LogicalNot<CompoundInterval>) formula).getNegated();
                    }
                    for (InvariantsFormula<CompoundInterval> assumption : formula.accept(new SplitDisjunctionsVisitor<CompoundInterval>())) {
                      if (assumption instanceof LogicalNot<?>) { // We don't care about negations here either
                        assumption = ((LogicalNot<CompoundInterval>) assumption).getNegated();
                      }
                      interestingPredicates.add(assumption);
                    }
                  }
                } catch (UnrecognizedCCodeException e) {
                  logManager.logException(Level.SEVERE, e, "Found unrecognized C code on an edge. Cannot guess interesting information.");
                  guessInterestingInformation = false;
                }
              }
              distances.offer(distance + 1);
            } else {
              distances.offer(distance);
            }
          }
        }
      }
    }

    if (shutdownNotifier.shouldShutdown()) {
      return new InvariantsState(options.useBitvectors, new AcceptAllVariableSelection<CompoundInterval>(), InvariantsPrecision.NONE);
    }

    // Try to specify all relevant variables
    Set<String> relevantVariables = new HashSet<>();
    boolean specifyRelevantVariables = options.analyzeRelevantVariablesOnly;
    // Collect all variables from relevant edges
    for (CFAEdge edge : relevantEdges) {
      ExpressionToFormulaVisitor etfv = InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(edge);
      if (edge instanceof CAssumeEdge) {
        try {
          InvariantsFormula<CompoundInterval> assumption = ((CAssumeEdge) edge).getExpression().accept(etfv);
          relevantVariables.addAll(assumption.accept(COLLECT_VARS_VISITOR));
        } catch (UnrecognizedCCodeException e) {
          logManager.logException(Level.WARNING, e, "Found unrecognized C code on an edge. Cannot specify relevant variables explicitly. Considering all variables as relevant.");
          specifyRelevantVariables = false;
        }
      }
    }
    final VariableSelection<CompoundInterval> variableSelection;
    if (specifyRelevantVariables) {
      // Collect all variables related to variables found on relevant assume edges from other edges with a fix point iteration
      expand(relevantVariables, relevantEdges, -1);
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

    relevantVariableLimitReached = options.interestingVariableLimit > interestingVariables.size();

    InvariantsPrecision precision = new InvariantsPrecision(relevantEdges,
        ImmutableSet.copyOf(limit(interestingPredicates, options.interestingPredicatesLimit)),
        ImmutableSet.copyOf(limit(interestingVariables, options.interestingVariableLimit)),
        options.maximumFormulaDepth,
        options.useBinaryVariableInterrelations,
        options.edgeBasedAbstractionStrategyFactory);

    initialPrecisionMap.put(pNode, precision);

    // Create the configured initial state
    return new InvariantsState(options.useBitvectors, variableSelection, precision);
  }

  @Override
  public InvariantsPrecision getInitialPrecision(CFANode pNode) {
    InvariantsPrecision precision = initialPrecisionMap.get(pNode);
    if (precision != null) {
      return precision;
    }
    return getInitialState(pNode).getPrecision();
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
    if (relevantVariableLimitReached) {
      return false;
    }
    ++options.interestingVariableLimit;
    logManager.log(Level.INFO, "Adjusting interestingVariableLimit to " + options.interestingVariableLimit);
    return true;
  }

  private static void expand(Set<String> pRelevantVariables, Collection<CFAEdge> pCfaEdges, int pLimit) {
    if (reachesLimit(pRelevantVariables, pLimit)) {
      return;
    }
    int size = 0;
    while (pRelevantVariables.size() > size && !reachesLimit(pRelevantVariables, pLimit)) {
      size = pRelevantVariables.size();
      for (CFAEdge edge : pCfaEdges) {
        try {
          expand(pRelevantVariables, edge, pLimit);
        } catch (UnrecognizedCCodeException e) {
          // If an exception occurred, we simply do not expand the set of variables but may continue
        }
      }
    }
    assert !exceedsLimit(pRelevantVariables, pLimit);
  }

  private static <T> boolean exceedsLimit(Collection<T> pCollection, int pLimit) {
    return pLimit >= 0 && pCollection.size() > pLimit;
  }

  private static <T> boolean reachesLimit(Collection<T> pCollection, int pLimit) {
    return pLimit >= 0 && pCollection.size() >= pLimit;
  }

  private static void expand(Set<String> pRelevantVariables, CFAEdge pCfaEdge, int pLimit) throws UnrecognizedCCodeException {
    if (reachesLimit(pRelevantVariables, pLimit)) {
      return;
    }
    switch (pCfaEdge.getEdgeType()) {
    case AssumeEdge:
      // Assume that all assume edge variables are already recorded
      break;
    case BlankEdge:
      break;
    case CallToReturnEdge:
      break;
    case DeclarationEdge:
      handleDeclaration((CDeclarationEdge) pCfaEdge, pRelevantVariables, pLimit);
      break;
    case FunctionCallEdge:
      handleFunctionCall((CFunctionCallEdge) pCfaEdge, pRelevantVariables, pLimit);
      break;
    case FunctionReturnEdge:
      handleFunctionReturn((CFunctionReturnEdge) pCfaEdge, pRelevantVariables, pLimit);
      break;
    case MultiEdge:
      expand(pRelevantVariables, ((MultiEdge) pCfaEdge).getEdges(), pLimit);
      break;
    case ReturnStatementEdge:
      handleReturnStatement((CReturnStatementEdge) pCfaEdge, pRelevantVariables, pLimit);
      break;
    case StatementEdge:
      handleStatementEdge((CStatementEdge) pCfaEdge, pRelevantVariables, pLimit);
      break;
    default:
      break;
    }
    assert !exceedsLimit(pRelevantVariables, pLimit);
  }

  private static <T> void addAll(Collection<T> pTarget, Collection<T> pSource, int pLimit) {
    Iterator<T> elementIterator = pSource.iterator();
    while (!reachesLimit(pTarget, pLimit) && elementIterator.hasNext()) {
      pTarget.add(elementIterator.next());
    }
  }

  private static void handleFunctionCall(final CFunctionCallEdge pEdge, Set<String> pRelevantVariables, int pLimit) throws UnrecognizedCCodeException {

    List<String> formalParams = pEdge.getSuccessor().getFunctionParameterNames();
    List<CExpression> actualParams = pEdge.getArguments();
    int limit = Math.min(formalParams.size(), actualParams.size());
    formalParams = FluentIterable.from(formalParams).limit(limit).toList();
    actualParams = FluentIterable.from(actualParams).limit(limit).toList();

    for (Pair<String, CExpression> param : Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();

      InvariantsFormula<CompoundInterval> value = actualParam.accept(InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(new VariableNameExtractor() {

        @Override
        public String extract(CExpression pCExpression) throws UnrecognizedCCodeException {
          return InvariantsTransferRelation.getVarName(pCExpression, pEdge, pEdge.getPredecessor().getFunctionName());
        }
      }));

      String formalParam = InvariantsTransferRelation.scope(param.getFirst(), pEdge.getSuccessor().getFunctionName());
      if (pRelevantVariables.contains(formalParam)) {
        addAll(pRelevantVariables, value.accept(COLLECT_VARS_VISITOR), pLimit);
      }
    }

    return;
  }

  private static void handleDeclaration(CDeclarationEdge pEdge, Set<String> pRelevantVariables, int pLimit) throws UnrecognizedCCodeException {
    if (!(pEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return;
    }

    CVariableDeclaration decl = (CVariableDeclaration) pEdge.getDeclaration();

    String varName = decl.getName();
    if (!decl.isGlobal()) {
      varName = InvariantsTransferRelation.scope(varName, pEdge.getSuccessor().getFunctionName());
    }

    final InvariantsFormula<CompoundInterval> value;
    if (decl.getInitializer() != null && decl.getInitializer() instanceof CInitializerExpression) {
      CExpression init = ((CInitializerExpression)decl.getInitializer()).getExpression();
      value = init.accept(InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(pEdge));
    } else {
      value = CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.top());
    }

    if (pRelevantVariables.contains(varName)) {
      addAll(pRelevantVariables, value.accept(COLLECT_VARS_VISITOR), pLimit);
    }
  }

  private static void handleStatementEdge(CStatementEdge pCStatementEdge, Set<String> pRelevantVariables, int pLimit) throws UnrecognizedCCodeException {
    ExpressionToFormulaVisitor etfv = InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(pCStatementEdge);
    CStatementEdge statementEdge = pCStatementEdge;
    CStatement statement = statementEdge.getStatement();
    if (statement instanceof CAssignment) {
      CAssignment assignment = (CAssignment) statement;
      handleAssignment(pCStatementEdge.getPredecessor().getFunctionName(), pCStatementEdge, assignment.getLeftHandSide(), assignment.getRightHandSide().accept(etfv), pRelevantVariables, pLimit);
    }
  }

  private static void handleAssignment(String pFunctionName, CFAEdge pCfaEdge, CExpression leftHandSide, InvariantsFormula<CompoundInterval> pValue, Set<String> pRelevantVariables, int pLimit) throws UnrecognizedCCodeException {
    ExpressionToFormulaVisitor etfv = InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(pCfaEdge);
    final String varName;
    if (leftHandSide instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arraySubscriptExpression = (CArraySubscriptExpression) leftHandSide;
      varName = InvariantsTransferRelation.getVarName(arraySubscriptExpression.getArrayExpression(), pCfaEdge, pFunctionName);
      InvariantsFormula<CompoundInterval> subscript = arraySubscriptExpression.getSubscriptExpression().accept(etfv);
      for (String relevantVar : pRelevantVariables) {
        if (relevantVar.equals(varName) || relevantVar.startsWith(varName + "[")) {
          addAll(pRelevantVariables, pValue.accept(COLLECT_VARS_VISITOR), pLimit);
          if (!reachesLimit(pRelevantVariables, pLimit)) {
            pRelevantVariables.add(varName);
          }
          addAll(pRelevantVariables, subscript.accept(COLLECT_VARS_VISITOR), pLimit);
          break;
        }
      }
    } else {
      varName = InvariantsTransferRelation.getVarName(leftHandSide, pCfaEdge, pFunctionName);
      if (pRelevantVariables.contains(varName)) {
        addAll(pRelevantVariables, pValue.accept(COLLECT_VARS_VISITOR), pLimit);
      }
    }
  }

  private static void handleReturnStatement(CReturnStatementEdge pCStatementEdge, Set<String> pRelevantVariables, int pLimit) throws UnrecognizedCCodeException {
    String calledFunctionName = pCStatementEdge.getPredecessor().getFunctionName();
    CExpression returnedExpression = pCStatementEdge.getExpression();
    // If the return edge has no statement, no return value is passed: "return;"
    if (returnedExpression == null) {
      return;
    }
    ExpressionToFormulaVisitor etfv = InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(pCStatementEdge);
    InvariantsFormula<CompoundInterval> returnedInvExpression = returnedExpression.accept(etfv);
    String returnValueName = InvariantsTransferRelation.scope(InvariantsTransferRelation.RETURN_VARIABLE_BASE_NAME, calledFunctionName);
    if (pRelevantVariables.contains(returnValueName)) {
      addAll(pRelevantVariables, returnedInvExpression.accept(COLLECT_VARS_VISITOR), pLimit);
    }
  }

  private static void handleFunctionReturn(CFunctionReturnEdge pFunctionReturnEdge, Set<String> pRelevantVariables, int pLimit)
      throws UnrecognizedCCodeException {
      CFunctionSummaryEdge summaryEdge = pFunctionReturnEdge.getSummaryEdge();

      CFunctionCall expression = summaryEdge.getExpression();

      String calledFunctionName = pFunctionReturnEdge.getPredecessor().getFunctionName();

      String returnValueName = InvariantsTransferRelation.scope(InvariantsTransferRelation.RETURN_VARIABLE_BASE_NAME, calledFunctionName);

      InvariantsFormula<CompoundInterval> value = CompoundIntervalFormulaManager.INSTANCE.asVariable(returnValueName);

      // expression is an assignment operation, e.g. a = g(b);
      if (expression instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement)expression;

        handleAssignment(pFunctionReturnEdge.getSuccessor().getFunctionName(), pFunctionReturnEdge, funcExp.getLeftHandSide(), value, pRelevantVariables, pLimit);
      }
  }
}