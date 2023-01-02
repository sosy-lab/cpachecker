// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.DummyScope;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonParser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.pathformula.CachingPathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.BooleanFormula;

@Options(prefix = "witness.validation.termination")
public class NonTerminationWitnessValidator implements Algorithm, StatisticsProvider {

  private static final DummyTargetState DUMMY_TARGET_STATE =
      DummyTargetState.withSimpleTargetInformation("termination");

  private static final String REACHABILITY_SPEC_NAME = "ReachabilityObserver";
  private static final String STEM_SPEC_NAME = "StemEndController";
  private static final String WITNESS_BREAK_CONTROLLER_SPEC_NAME = "WitnessBreakController";
  private static final String WITNESS_BREAK_OBSERVER_SPEC_NAME = "WitnessBreakObserver";
  private static final String TERMINATION_OBSERVER_SPEC_NAME = "TerminationObserver";
  private static final String ITERATION_OBSERVER_SPEC_NAME = "RecurrentSetObserver";

  private static final String AUTOMATANAMEPREFIX = "AutomatonAnalysis_";
  private static final String BREAKSTATENAME = "_predefinedState_BREAK";

  @Option(
      secure = true,
      name = "reachCycle.config",
      required = true,
      description =
          "Use this configuration when checking that recurrent set (at cycle head) is reachable."
              + " Configuration must be precise, i.e., may only report real counterexamples")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path reachabilityConfig;

  @Option(
      secure = true,
      required = true,
      name = "inspectCycle.config",
      description =
          "Use this configuration when checking that when reach recurrent set, "
              + "execution can be extended to an infinite one")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path recurrentConfig;

  @Option(
      secure = true,
      required = true,
      name = "terminatingStatements",
      description =
          "Path to automaton specification describing which statements let the program terminate.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path terminatingStatementsAutomaton =
      Classes.getCodeLocation(NonTerminationWitnessValidator.class)
          .resolveSibling("config/specification/TerminatingStatements.spc");

  @Option(
      secure = true,
      name = "successAsViolation",
      description =
          "Report a successful validation of the witness, "
              + "i.e., a confirmation of the nontermination, as termination violation.")
  private boolean reportSuccessfulCheckAsViolation = true;

  private static final String RECURSIONDEPTH = "2"; // TODO should it be configurable?

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdown;
  private final CFA cfa;
  private final Automaton witness;
  private final String witnessAutomatonName;
  private final Automaton terminationAutomaton;
  private final String terminationAutomatonName;

  private final NonTerminationValidationStatistics statistics;

  public NonTerminationWitnessValidator(
      final CFA pCfa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ImmutableList<Automaton> pSpecificationAutomata)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    config = pConfig;
    logger = pLogger;
    shutdown = pShutdownNotifier;

    if (pSpecificationAutomata.size() < 1) {
      throw new InvalidConfigurationException("Witness file is missing in specification.");
    }
    if (pSpecificationAutomata.size() != 1) {
      throw new InvalidConfigurationException(
          "Expect that only violation witness is part of the specification.");
    }

    witness = pSpecificationAutomata.get(0);
    witnessAutomatonName = AUTOMATANAMEPREFIX + witness.getName();

    Scope scope =
        cfa.getLanguage() == Language.C ? new CProgramScope(cfa, logger) : DummyScope.getInstance();
    terminationAutomaton =
        AutomatonParser.parseAutomatonFile(
                terminatingStatementsAutomaton,
                config,
                logger,
                cfa.getMachineModel(),
                scope,
                cfa.getLanguage(),
                pShutdownNotifier)
            .get(0);
    terminationAutomatonName = AUTOMATANAMEPREFIX + terminationAutomaton.getName();

    statistics = new NonTerminationValidationStatistics();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    statistics.totalVal.start();

    FluentIterable<AutomatonInternalState> cycleHeadCandidates =
        from(witness.getStates())
            .filter(
                (AutomatonInternalState automState) -> {
                  return automState.isNontrivialCycleStart();
                });

    if (cycleHeadCandidates.isEmpty()) {
      throw new CPAException("Invalid witness. Witness is missing cycle start state.");
    }

    try {
      for (AutomatonInternalState stemEndRepeatStart : cycleHeadCandidates) {
        shutdown.shutdownIfNecessary();

        Optional<AbstractState> stemSynState = findStemEndLocation(stemEndRepeatStart);
        shutdown.shutdownIfNecessary();

        if (stemSynState.isPresent()) {
          CFANode stemEndLoc = AbstractStates.extractLocation(stemSynState.orElseThrow());
          CFANode afterInvCheck = new CFANode(stemEndLoc.getFunction());

          // extract quasi invariant which describes recurrent set, use true as default
          ExpressionTree<AExpression> quasiInvariant = ExpressionTrees.getTrue();

          for (AbstractState state : AbstractStates.asIterable(stemSynState.orElseThrow())) {
            if (state instanceof AutomatonState) {
              AutomatonState automatonState = (AutomatonState) state;
              if (automatonState.getOwningAutomaton() == witness) {
                quasiInvariant = automatonState.getCandidateInvariants();
                break;
              }
            }
          }

          shutdown.shutdownIfNecessary();

          CExpression expr =
              quasiInvariant.accept(new ToCExpressionVisitor(cfa.getMachineModel(), logger));

          shutdown.shutdownIfNecessary();

          // encode quasi invariant in assume edge leaving final stem location
          CAssumeEdge invCheck =
              new CAssumeEdge(
                  expr.toASTString(), FileLocation.DUMMY, stemEndLoc, afterInvCheck, expr, true);
          stemEndLoc.addLeavingEdge(invCheck);
          invCheck.getSuccessor().addEnteringEdge(invCheck);

          CAssumeEdge negInvCheck =
              new CAssumeEdge(
                  "!( " + invCheck.getRawStatement() + " )",
                  FileLocation.DUMMY,
                  stemEndLoc,
                  new CFANode(stemEndLoc.getFunction()),
                  invCheck.getExpression(),
                  false);

          logger.log(Level.INFO, "Check that recurrent set is reachable");
          if (checkReachabilityOfRecurrentSet(invCheck)) {
            shutdown.shutdownIfNecessary();

            // remove invariant/recurrent set encoding from CFA, only needed for previous step
            // continue with regular CFA
            stemEndLoc.removeLeavingEdge(invCheck);

            logger.log(Level.INFO, "Check that recurrent set is valid");
            if (confirmThatRecurrentSetIsProper(stemEndRepeatStart, stemEndLoc, negInvCheck)) {

              pReachedSet.popFromWaitlist();
              logger.log(Level.INFO, "Non-termination witness confirmed.");
              if (reportSuccessfulCheckAsViolation) {
                pReachedSet.add(
                    new ARGState(DUMMY_TARGET_STATE, null), SingletonPrecision.getInstance());
              }
              return AlgorithmStatus.SOUND_AND_PRECISE; // TODO correct choice here?
            }
          }
        }
      }

      logger.log(Level.INFO, "Could not confirm witness.");
      pReachedSet.add(
          new DummyErrorState(pReachedSet.getFirstState()), SingletonPrecision.getInstance());
      pReachedSet.popFromWaitlist();
      return AlgorithmStatus.SOUND_AND_IMPRECISE;
    } finally {
      statistics.totalVal.stop();
    }
  }

  private Optional<AbstractState> findStemEndLocation(final AutomatonInternalState cycleStart)
      throws InterruptedException {
    // stem end (location) should be identical with beginning of repetition
    // use syntactical reachability to find stem end location
    // syntactical reachability is guided by witness

    try {
      List<Automaton> automata = new ArrayList<>(2);
      automata.add(witness);
      // reaching stem end in witness results in ERROR (simple termination and easy to search for)
      automata.add(getSpecForErrorAt(witnessAutomatonName, cycleStart, STEM_SPEC_NAME));
      automata.add(terminationAutomaton);
      automata.add(getSpecForStopAtWitnessTerminationBreak(WITNESS_BREAK_CONTROLLER_SPEC_NAME));
      Specification spec = Specification.fromAutomata(automata);

      // set up
      ReachedSet reached;
      ConfigurableProgramAnalysis cpa;
      Algorithm algorithm;

      ConfigurationBuilder singleConfigBuilder = Configuration.builder();
      singleConfigBuilder.setOption("cpa", "cpa.composite.CompositeCPA");
      singleConfigBuilder.setOption(
          "CompositeCPA.cpas", "cpa.location.LocationCPA, cpa.callstack.CallstackCPA");
      singleConfigBuilder.setOption("cpa.callstack.depth", RECURSIONDEPTH);
      singleConfigBuilder.setOption("analysis.traversal.order", "BFS");
      singleConfigBuilder.setOption("output.disable", "true");
      Configuration singleConfig = singleConfigBuilder.build();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(singleConfig, logger, shutdown, AggregatedReachedSets.empty());
      cpa = coreComponents.createCPA(cfa, spec);

      GlobalInfo.getInstance().setUpInfoFromCPA(cpa);

      algorithm = coreComponents.createAlgorithm(cpa, cfa, spec);

      AbstractState initialState =
          cpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      Precision initialPrecision =
          cpa.getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

      reached = coreComponents.createReachedSet(cpa);
      reached.add(initialState, initialPrecision);

      shutdown.shutdownIfNecessary();

      // run analysis
      logger.log(
          Level.INFO,
          "Search for program location at which infinite path(s) split into stem and looping part");
      algorithm.run(reached);

      Optional<AbstractState> stemState =
          reached.stream().filter(AbstractStates::isTargetState).findFirst();
      return stemState;

    } catch (IOException | InvalidConfigurationException | CPAException e) {
      logger.logException(
          Level.FINE,
          e,
          "Exception occurred while trying to find location which is visited infinitely in a"
              + " nonterminating execution");
      return Optional.empty();
    }
  }

  private boolean checkReachabilityOfRecurrentSet(final CAssumeEdge quasiInvariantAsAssumeEdge)
      throws InterruptedException {
    // run a reachability analysis from the initial states to the recurrent set
    // assume that recurrent set is reachable if end location of quasiInvariantAsAssumeEdge is
    // reachable
    // encode reaching that end location as an error and check if a real counterexample is found

    statistics.cycleReachTime.start();
    try {
      logger.log(Level.INFO, "Prepare check for reachability of recurrent set.");
      // set up specification
      List<Automaton> automata = new ArrayList<>(2);
      automata.add(witness);
      automata.add(getSpecForErrorAt(quasiInvariantAsAssumeEdge.getSuccessor()));
      automata.add(terminationAutomaton);
      // stop when reach break state
      automata.add(getSpecForStopAtWitnessTerminationBreak(WITNESS_BREAK_CONTROLLER_SPEC_NAME));
      Specification spec = Specification.fromAutomata(automata);

      // set up
      ReachedSet reached;
      ConfigurableProgramAnalysis cpa;
      Algorithm algorithm;

      ConfigurationBuilder singleConfigBuilder = Configuration.builder();
      singleConfigBuilder.loadFromFile(reachabilityConfig);
      singleConfigBuilder.setOption("cpa.callstack.depth", RECURSIONDEPTH);
      singleConfigBuilder.setOption("output.disable", "true");
      Configuration singleConfig = singleConfigBuilder.build();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(singleConfig, logger, shutdown, AggregatedReachedSets.empty());
      cpa = coreComponents.createCPA(cfa, spec);

      GlobalInfo.getInstance().setUpInfoFromCPA(cpa);

      algorithm = coreComponents.createAlgorithm(cpa, cfa, spec);

      AbstractState initialState =
          cpa.getInitialState(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      Precision initialPrecision =
          cpa.getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

      reached = coreComponents.createReachedSet(cpa);
      reached.add(initialState, initialPrecision);

      shutdown.shutdownIfNecessary();

      // run analysis
      logger.log(Level.INFO, "Start checking whether recurrent set is reachable.");
      AlgorithmStatus result = algorithm.run(reached);

      if (result.isPrecise()) {
        if (reached.wasTargetReached()) {
          logger.log(Level.INFO, "Recurrent set is reachable.");
          return true;
        }
      }

    } catch (IOException | InvalidConfigurationException | CPAException e) {
      logger.logException(
          Level.FINE, e, "Exception occurred while proving that recurrent set is reachable.");
    } finally {
      statistics.cycleReachTime.stop();
    }
    logger.log(Level.INFO, "Could not establish that recurrent set is reachable.");
    return false;
  }

  private boolean confirmThatRecurrentSetIsProper(
      final AutomatonInternalState pStemEndCycleStart,
      final CFANode pStemEndLoc,
      final CAssumeEdge pNegInvCheck)
      throws InterruptedException {
    // First, check that assumptions in witness for repetition part of witness (from cycle start)
    // only restrict nondeterminism
    // Second, check when starting in the recurrent set, the recurrent set is not left
    // Therefore, start a reachability analysis from the recurrent set that stops exploration when
    // getting to the cycle start of the recurrent set again
    // require that sink states in automaton are encoded as break states

    logger.log(
        Level.INFO, "Check that assumptions in witnesses only restrict nondeterministic choices");
    if (!areWitnessAssumptionsInLoopOnlyNondeterminismRestricting(
        pStemEndLoc, pStemEndCycleStart)) {
      return false;
    }

    statistics.cycleCheckTime.start();
    try {
      // set up specification
      List<Automaton> automataSecondCheck = new ArrayList<>(4);
      List<Automaton> automata = new ArrayList<>(5);
      // add witness automaton, but change initial state to beginning of recurrent set
      try {
        automataSecondCheck.add(
            new Automaton(
                witness.getName(),
                witness.getInitialVariables(),
                witness.getStates(),
                pStemEndCycleStart.getName()));
      } catch (InvalidAutomatonException e) {
        logger.logException(Level.INFO, e, "Failed to set up specification to check recurrent set");
        return false;
      }
      // sink states as errors, if reached may leave recurrent set
      automataSecondCheck.add(
          getSpecForErrorAt(
              witnessAutomatonName, BREAKSTATENAME, WITNESS_BREAK_OBSERVER_SPEC_NAME));
      // executing statements ending program as errors, if reached (i.e. executed) may leave
      // recurrent set
      automataSecondCheck.add(terminationAutomaton);
      automataSecondCheck.add(
          getSpecForErrorAt(
              terminationAutomatonName, BREAKSTATENAME, TERMINATION_OBSERVER_SPEC_NAME));
      automata.addAll(automataSecondCheck);
      // break when should be in recurrent set again
      // still allow successor computation along pNegInvCheck
      // i.e., visit stemEndLoc and  internal automaton state (multiple unrollings, edge
      // need to consider both to support multiple unrollings of syntactical path and staying in
      // automaton state for a sequence of CFA edges
      Automaton automatonRedetectRecurrentSet =
          getSpecForDetectingCycleIterationEnd(
              pStemEndLoc,
              witnessAutomatonName,
              pStemEndCycleStart,
              pNegInvCheck.getSuccessor(),
              ITERATION_OBSERVER_SPEC_NAME);
      automata.add(automatonRedetectRecurrentSet);
      Specification spec = Specification.fromAutomata(automata);
      Specification spec2 = Specification.fromAutomata(automataSecondCheck);

      shutdown.shutdownIfNecessary();

      logger.log(
          Level.INFO, "Checking infinite part of non-termination witness, often the loop part");
      logger.log(Level.INFO, "Try using complete witness information");

      boolean confirmedRecurrentSet =
          setUpAndRunAnalysisForRecurrentSetCheck(
              spec, pStemEndLoc, pNegInvCheck, pStemEndCycleStart, automatonRedetectRecurrentSet);

      if (!confirmedRecurrentSet) {
        logger.log(
            Level.INFO,
            "First check of recurrent set check failed. Continue with check only relying on"
                + " recurrent set information, but do not stop exploration at cyclehead.");
        confirmedRecurrentSet =
            setUpAndRunAnalysisForRecurrentSetCheck(
                spec2, pStemEndLoc, pNegInvCheck, pStemEndCycleStart, null);
      }

      return confirmedRecurrentSet;

    } catch (IOException | InvalidConfigurationException e) {
      logger.logException(
          Level.FINE,
          e,
          "Exception occurred while checking validity (closedness) of given recurrent set.");
      return false;
    } finally {
      statistics.cycleCheckTime.stop();
    }
  }

  private boolean setUpAndRunAnalysisForRecurrentSetCheck(
      final Specification spec,
      final CFANode pStemEndLoc,
      final CAssumeEdge pNegInvCheck,
      final AutomatonInternalState pStemEndCycleStart,
      final @Nullable Automaton pAutomatonRedetectRecurrentSet)
      throws InterruptedException {
    // set up
    try {
      ReachedSet reached;
      ConfigurableProgramAnalysis cpa;
      Algorithm algorithm;

      ConfigurationBuilder singleConfigBuilder = Configuration.builder();
      singleConfigBuilder.loadFromFile(recurrentConfig);
      singleConfigBuilder.setOption("cpa.callstack.depth", RECURSIONDEPTH);
      singleConfigBuilder.setOption("output.disable", "true");
      Configuration singleConfig = singleConfigBuilder.build();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(singleConfig, logger, shutdown, AggregatedReachedSets.empty());
      cpa = coreComponents.createCPA(cfa, spec);

      Preconditions.checkArgument(
          cpa instanceof ARGCPA, "Require ARGCPA to check validity of recurrent set:");

      ConfigurableProgramAnalysis wrappedCPA = ((ARGCPA) cpa).getWrappedCPAs().get(0);

      GlobalInfo.getInstance().setUpInfoFromCPA(cpa);

      algorithm = coreComponents.createAlgorithm(cpa, cfa, spec);

      shutdown.shutdownIfNecessary();

      // build initial precision
      Precision initialPrecision =
          cpa.getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

      PredicateCPA predCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
      if (predCPA != null) {
        PredicatePrecision predPrec = getPredicatePrecisionFromCandidateInvariants(predCPA);
        // TODO unify with initial predicate precision instead of replacement?
        initialPrecision =
            Precisions.replaceByType(
                initialPrecision, predPrec, precision -> precision instanceof PredicatePrecision);
      }

      // build initial state which should be restricted to recurrent set
      CAssumeEdge invCheckLoop =
          new CAssumeEdge(
              "!(" + pNegInvCheck.getRawStatement() + ")",
              FileLocation.DUMMY,
              pStemEndLoc,
              pStemEndLoc,
              pNegInvCheck.getExpression(),
              true);
      pStemEndLoc.addLeavingEdge(invCheckLoop);
      AbstractState initialState =
          setUpInitialAbstractStateForRecurrentSet(
              pStemEndLoc, wrappedCPA, initialPrecision, invCheckLoop, pStemEndCycleStart);
      pStemEndLoc.removeLeavingEdge(invCheckLoop);
      // TODO okay that initial states is non-abstraction state?

      reached = coreComponents.createReachedSet(cpa);
      reached.add(initialState, initialPrecision);

      shutdown.shutdownIfNecessary();

      // run analysis
      logger.log(Level.INFO, "Start checking recurrent set");
      AlgorithmStatus result = algorithm.run(reached);

      if (!result.isSound()) {
        logger.log(
            Level.INFO,
            "Current check became unsound. Do not use its result for witness valiation.");
        return false;
      }

      // check that no sink state is reachable from recurrent set
      if (reached.wasTargetReached()) {
        logger.log(Level.INFO, "May leave recurrent set in current check.");
        return false;
      }

      for (AbstractState stateWithoutSucc :
          from(reached)
              .filter(
                  (AbstractState state) -> {
                    ARGState argState = (ARGState) state;
                    return !argState.isCovered() && argState.getChildren().isEmpty();
                  })) {
        shutdown.shutdownIfNecessary();

        for (AbstractState compState : AbstractStates.asIterable(stateWithoutSucc)) {

          // check that no terminating states are reached
          if (compState instanceof LocationState) {
            if (((LocationState) compState).getLocationNode().getNumLeavingEdges() == 0) {
              // assume that analysis removed such infeasible successors, thus this state is
              // feasible
              logger.log(
                  Level.INFO,
                  "May reach a terminating state from the recurrent set in current check.");
              return false;
            }

            // check if callstack state may be the reason why no successors exists
          } else if (compState instanceof CallstackState) {

            if (((CallstackState) compState).getDepth() == 0) {
              if (AbstractStates.extractLocation(stateWithoutSucc) instanceof FunctionExitNode) {
                // at end of function no successors exist, likely because callstack CPA does not
                // know the correct successor
                logger.log(
                    Level.INFO,
                    "Function return without known caller found. May be unsound to continue. Abort"
                        + " current check.");
                return false;
              }
            }

            // check that when one iteration of infinite paths ends, it ends in the recurrent set
          } else if (compState instanceof AutomatonState) {
            AutomatonState amState = (AutomatonState) compState;

            if (amState.getOwningAutomaton() == pAutomatonRedetectRecurrentSet) {
              if (amState.getInternalStateName().equals(SuccessorState.FINISHED.toString())) {
                pNegInvCheck.getPredecessor().addLeavingEdge(pNegInvCheck);

                // reached end of iteration, i.e., found cycle start at stem end location again
                // check that after iteration remain in recurrent set
                // check that there does not exist a successor from this state along the negated
                // description of the recurrent set (represented by assume edge pNegInvCheck)
                for (AbstractState succ :
                    wrappedCPA
                        .getTransferRelation()
                        .getAbstractSuccessorsForEdge(
                            ((ARGState) stateWithoutSucc).getWrappedState(),
                            reached.getPrecision(stateWithoutSucc),
                            pNegInvCheck)) {
                  java.util.Optional<PrecisionAdjustmentResult> precResult =
                      wrappedCPA
                          .getPrecisionAdjustment()
                          .prec(
                              succ,
                              reached.getPrecision(stateWithoutSucc),
                              reached,
                              Functions.identity(),
                              succ);
                  pNegInvCheck.getPredecessor().removeLeavingEdge(pNegInvCheck);
                  if (precResult.isPresent()) {
                    logger.log(Level.INFO, "May leave the recurrent set in current check.");
                    return false;
                  }
                }
              }
            }
          }
        }
      }
    } catch (CPAException | InvalidConfigurationException | IOException e) {
      return false;
    }
    return true;
  }

  private PredicatePrecision getPredicatePrecisionFromCandidateInvariants(PredicateCPA pPredCPA) {
    PredicateAbstractionManager pAMgr = pPredCPA.getPredicateManager();
    BooleanFormula invariant;
    PathFormulaManager pfmgr = pPredCPA.getPathFormulaManager();
    if (pfmgr instanceof CachingPathFormulaManager) {
      pfmgr = ((CachingPathFormulaManager) pfmgr).delegate;
    }

    ToFormulaVisitor toFormula =
        new ToFormulaVisitor(pPredCPA.getSolver().getFormulaManager(), pfmgr, null);
    Collection<AbstractionPredicate> predicates = new ArrayList<>();

    predicates.add(pAMgr.makeFalsePredicate());

    for (ExpressionTree<AExpression> candidate : witness.getAllCandidateInvariants()) {
      try {
        invariant = candidate.accept(toFormula);
        predicates.addAll(pAMgr.getPredicatesForAtomsOf(invariant));
      } catch (ToFormulaException e) {
        logger.logException(Level.FINE, e, "Ignoring candidate invariant in precision generation.");
      }
    }

    return new PredicatePrecision(
        ArrayListMultimap.create(),
        ArrayListMultimap.create(),
        ArrayListMultimap.create(),
        predicates);
  }

  private AbstractState setUpInitialAbstractStateForRecurrentSet(
      final CFANode pRecurrentSetLoc,
      final ConfigurableProgramAnalysis cpaWrappedInARGCPA,
      final Precision pInitialPrecision,
      final CAssumeEdge pAssumeRecurrentSetInvariant,
      final AutomatonInternalState pStemEndCycleStart)
      throws InterruptedException {

    Preconditions.checkArgument(
        Objects.equals(
            pAssumeRecurrentSetInvariant.getPredecessor(),
            pAssumeRecurrentSetInvariant.getSuccessor()));
    AbstractState initialDefault =
        cpaWrappedInARGCPA.getInitialState(
            pRecurrentSetLoc, StateSpacePartition.getDefaultPartition());

    Collection<? extends AbstractState> succ;
    try {
      succ =
          cpaWrappedInARGCPA
              .getTransferRelation()
              .getAbstractSuccessorsForEdge(
                  initialDefault, pInitialPrecision, pAssumeRecurrentSetInvariant);
      List<AbstractState> initialCandidate = new ArrayList<>(succ.size());
      for (AbstractState successor : succ) {
        for (AbstractState innerState : AbstractStates.asIterable(successor)) {
          if (innerState instanceof AutomatonState
              && ((AutomatonState) innerState)
                  .getInternalStateName()
                  .equals(pStemEndCycleStart.getName())) {
            initialCandidate.add(successor);
          }
        }

        if (initialCandidate.size() == 1) {
          return new ARGState(initialCandidate.get(0), null);
        }
      }
    } catch (CPATransferException e) {
      logger.logException(
          Level.FINE, e, "Failed to properly set up initial state for recurrent checking.");
    }

    logger.log(
        Level.WARNING,
        "Failed to add the information of the recurrent set. Try to check witness with recurrent"
            + " set TRUE");

    return new ARGState(initialDefault, null);
  }

  private boolean areWitnessAssumptionsInLoopOnlyNondeterminismRestricting(
      final CFANode pRecurrentStart, final AutomatonInternalState pRecurrentStartInWitness)
      throws InterruptedException {
    // so far we do not support all possible assumption
    // for simplicity, we only allow assumptions for external method calls and declarations without
    // initialization
    // furthermore, there may be at most one assumption which restricts the value of the
    // assigned/declared variable in form of an equality
    statistics.cycleCheckTime.start();
    try {
      List<Automaton> automata = new ArrayList<>(1);
      // add witness automaton, but change initial state to beginning of recurrent set
      try {
        automata.add(
            new Automaton(
                witness.getName(),
                witness.getInitialVariables(),
                witness.getStates(),
                pRecurrentStartInWitness.getName()));
        automata.add(getSpecForStopAtWitnessTerminationBreak(WITNESS_BREAK_CONTROLLER_SPEC_NAME));
      } catch (InvalidAutomatonException | IOException | InvalidConfigurationException e) {
        logger.logUserException(
            Level.INFO, e, "Failed to set up specification to check assumptions.");
        return false;
      }
      automata.add(terminationAutomaton);
      Specification spec = Specification.fromAutomata(automata);

      // set up
      ReachedSet reached;
      ConfigurableProgramAnalysis cpa;
      Algorithm algorithm;

      ConfigurationBuilder singleConfigBuilder = Configuration.builder();
      singleConfigBuilder.setOption("cpa", "cpa.arg.ARGCPA");
      singleConfigBuilder.setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA");
      singleConfigBuilder.setOption("CompositeCPA.cpas", "cpa.location.LocationCPA");
      singleConfigBuilder.setOption("analysis.traversal.order", "BFS");
      singleConfigBuilder.setOption("output.disable", "true");
      Configuration singleConfig = singleConfigBuilder.build();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(singleConfig, logger, shutdown, AggregatedReachedSets.empty());
      cpa = coreComponents.createCPA(cfa, spec);

      GlobalInfo.getInstance().setUpInfoFromCPA(cpa);

      algorithm = coreComponents.createAlgorithm(cpa, cfa, spec);

      AbstractState initialState =
          cpa.getInitialState(pRecurrentStart, StateSpacePartition.getDefaultPartition());
      Precision initialPrecision =
          cpa.getInitialPrecision(pRecurrentStart, StateSpacePartition.getDefaultPartition());

      reached = coreComponents.createReachedSet(cpa);
      reached.add(initialState, initialPrecision);

      shutdown.shutdownIfNecessary();

      // run analysis
      // arbitrary assumption on stem are okay
      logger.log(Level.INFO, "Explore assumptions in witness automaton. Focus on looping part.");
      algorithm.run(reached);

      ARGState argState;
      CFAEdge edge;
      CExpression assigned;
      CBinaryExpression assumption;

      for (AbstractState state : reached) {
        for (AutomatonState amState :
            AbstractStates.asIterable(state).filter(AutomatonState.class)) {
          if (!amState.getAssumptions().isEmpty()) {
            argState = (ARGState) state;

            if (amState.getAssumptions().size() > 1) {
              logger.log(
                  Level.INFO,
                  "Support at most one assumption per edge in witness, but this witness has more.");
              return false;
            }

            if (!(amState.getAssumptions().get(0) instanceof CBinaryExpression)) {
              logger.log(
                  Level.INFO, "Found a disallowed assumption. Only support binary assumptions.");
              return false;
            } else {
              assumption = (CBinaryExpression) amState.getAssumptions().get(0);
            }

            if (!(assumption.getOperator() == BinaryOperator.EQUALS)) {
              logger.log(
                  Level.INFO,
                  "Found a disallowed operator in assumption. Only equality is supported.");
              return false;
            }

            // check that assumption is after nondeterministic statement
            // it is okay if there is an assumption, but no parent because assumptions are added by
            // the transfer relation
            for (ARGState parent : argState.getParents()) {
              edge = parent.getEdgeToChild(argState);
              switch (edge.getEdgeType()) {
                case StatementEdge:
                  CStatement stmt = ((CStatementEdge) edge).getStatement();
                  if (stmt instanceof CFunctionCallAssignmentStatement) {
                    // external function call
                    assigned = ((CFunctionCallAssignmentStatement) stmt).getLeftHandSide();
                    if (!assumption.getOperand1().equals(assigned)
                        && !assumption.getOperand2().equals(assigned)) {
                      logger.log(
                          Level.INFO,
                          "Cannot detect that assumption only restricts assigned variable."
                              + " Assumption might be too strong.");
                      return false;
                    }
                  } else {
                    logger.log(Level.INFO, "Found an assumption for a deterministic edge.");
                    return false;
                  }
                  break;
                case DeclarationEdge:
                  CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
                  if (decl instanceof CVariableDeclaration
                      && ((CVariableDeclaration) decl).getInitializer() == null) {

                    // check that assumption only affects declared variable
                    if (!decl.getName().equals(assumption.getOperand1().toASTString())
                        && !decl.getName().equals(assumption.getOperand2().toASTString())) {
                      logger.log(
                          Level.INFO,
                          "Cannot detect that declared variables refers to declared variable."
                              + " Assumption might be too strong.");
                      return false;
                    }
                  } else {
                    logger.log(
                        Level.INFO,
                        "Found an unallowed assumption for a declaration with initializer.");
                    return false;
                  }
                  break;
                default:
                  logger.log(Level.INFO, "Found an assumption for a deterministic statement");
                  return false;
              }
            }
          }
        }
      }
      return true;

    } catch (InvalidConfigurationException | CPAException e) {
      logger.logException(Level.FINE, e, "Failed to check proper use of assumptions in witness.");
      return false;
    } finally {
      statistics.cycleCheckTime.stop();
    }
  }

  private enum SuccessorState {
    BREAK,
    ERROR,
    STOP,
    FINISHED;
  }

  private Automaton getSpecForErrorAt(
      final String automatonCPAName,
      final AutomatonInternalState automatonState,
      final String fileName)
      throws IOException, InvalidConfigurationException {
    return getSpecForErrorAt(automatonCPAName, automatonState.getName(), fileName);
  }

  private Automaton getSpecForErrorAt(
      final String automatonCPAName, final String internalStateName, final String fileName)
      throws IOException, InvalidConfigurationException {
    return writeObserver(
        fileName,
        "CHECK(" + automatonCPAName + " , \"state==" + internalStateName + "\")",
        SuccessorState.ERROR);
  }

  private Automaton getSpecForErrorAt(final CFANode loc)
      throws IOException, InvalidConfigurationException {
    return writeObserver(
        REACHABILITY_SPEC_NAME,
        "CHECK(location , \"nodenumber==" + loc.getNodeNumber() + "\")",
        SuccessorState.ERROR);
  }

  private Automaton writeObserver(
      final String automatonFileName, final String checkStatement, final SuccessorState succState)
      throws InvalidConfigurationException, IOException {
    Path tmpSpec = Files.createTempFile(automatonFileName, "spc");
    try (Writer writer = Files.newBufferedWriter(tmpSpec, Charset.defaultCharset())) {
      writer.append("OBSERVER AUTOMATON ");
      writer.append(automatonFileName);

      writer.append("\nINITIAL STATE Init;\n");

      writer.append("STATE USEFIRST Init :\n");
      writer.append(checkStatement);
      writer.append(" -> " + succState + ";\n\n");

      writer.append("END AUTOMATON");
    }

    return getAutomaton(tmpSpec);
  }

  private Automaton getSpecForStopAtWitnessTerminationBreak(final String fileName)
      throws IOException, InvalidConfigurationException {
    Path tmpSpec = Files.createTempFile(fileName, "spc");

    try (Writer writer = Files.newBufferedWriter(tmpSpec, Charset.defaultCharset())) {

      writer.append("CONTROL AUTOMATON ");
      writer.append(fileName);

      writer.append("\nINITIAL STATE Init;\n");

      // needed to set up initial states, no cycle detection during set up
      writer.append("STATE USEFIRST Init :\n");
      writer.append("CHECK(");
      writer.append(witnessAutomatonName);
      writer.append(" , \"state==");
      writer.append(BREAKSTATENAME);
      writer.append("\") -> STOP;\n");
      writer.append("CHECK(");
      writer.append(terminationAutomatonName);
      writer.append(" , \"state==");
      writer.append(BREAKSTATENAME);
      writer.append("\") -> STOP;\n\n");

      writer.append("END AUTOMATON");
    }

    return getAutomaton(tmpSpec);
  }

  private Automaton getSpecForDetectingCycleIterationEnd(
      final CFANode loc,
      final String automatonCPAName,
      final AutomatonInternalState automatonState,
      final CFANode nodeToContinue,
      final String fileName)
      throws IOException, InvalidConfigurationException {
    Path tmpSpec = Files.createTempFile(fileName, "spc");

    try (Writer writer = Files.newBufferedWriter(tmpSpec, Charset.defaultCharset())) {

      writer.append("CONTROL AUTOMATON ");
      writer.append(fileName);

      writer.append("\nINITIAL STATE Init;\n");

      // needed to set up initial states, no cycle detection during set up
      writer.append("STATE USEFIRST Init :\n");
      writer.append(" TRUE -> GOTO Checking;\n\n");

      writer.append("STATE USEFIRST Checking :\n");
      writer.append("CHECK(location, \"nodenumber==");
      writer.append(Integer.toString(loc.getNodeNumber()));
      writer.append("\") && CHECK(");
      writer.append(automatonCPAName);
      writer.append(" , \"state==");
      writer.append(automatonState.getName());
      writer.append("\")");
      writer.append(" -> GOTO " + SuccessorState.FINISHED + ";\n\n");

      writer.append("STATE USEFIRST " + SuccessorState.FINISHED + ":\n");
      writer.append("CHECK(location, \"nodenumber==");
      writer.append(Integer.toString(nodeToContinue.getNodeNumber()));
      writer.append("\") -> GOTO " + SuccessorState.FINISHED + ";\n");
      writer.append(" TRUE -> " + SuccessorState.STOP + ";\n\n");

      writer.append("END AUTOMATON");
    }

    return getAutomaton(tmpSpec);
  }

  private Automaton getAutomaton(final Path automatonSpec) throws InvalidConfigurationException {
    Scope scope =
        cfa.getLanguage() == Language.C ? new CProgramScope(cfa, logger) : DummyScope.getInstance();

    return AutomatonParser.parseAutomatonFile(
            automatonSpec,
            config,
            logger,
            cfa.getMachineModel(),
            scope,
            cfa.getLanguage(),
            shutdown)
        .get(0);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(statistics);
  }

  private static class NonTerminationValidationStatistics implements Statistics {

    private final StatTimer totalVal = new StatTimer("Total time for validation");
    private final StatTimer cycleReachTime =
        new StatTimer("Time to check reachability of cycle start");
    private final StatTimer cycleCheckTime =
        new StatTimer("Time for checking infinite execution from cycle start");

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);

      writer.put(totalVal);

      writer = writer.beginLevel();
      writer.put(cycleReachTime);
      writer.put(cycleCheckTime);
      // writer = writer.endLevel();
    }

    @Override
    public @Nullable String getName() {
      return "Nontermination Witness Validation";
    }
  }
}
