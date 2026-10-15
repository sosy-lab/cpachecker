// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.harness.PredefinedTypes.isPredefinedFunction;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.PropertyTargetInformation;
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
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonCoverageProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetCPA;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetProvider;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetState;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.testcase.TestCaseExporter;

@Options(prefix = "testcase")
public class TestCaseGeneratorAlgorithm implements ProgressReportingAlgorithm, StatisticsProvider {

  public enum ProgressComputation {
    ABSOLUTE,
    RELATIVE_TOTAL
  }

  @Option(
      secure = true,
      name = "inStats",
      description = "display all test targets and non-covered test targets in statistics")
  private boolean printTestTargetInfoInStats = false;

  @Option(
      secure = true,
      description =
          "when generating tests covering error call stop as soon as generated one test case and"
              + " report false (only possible in combination with error call property"
              + " specification")
  private boolean reportCoveredErrorCallAsError = false;

  @Option(secure = true, name = "progress", description = "defines how progress is computed")
  private ProgressComputation progressType = ProgressComputation.RELATIVE_TOTAL;

  @Option(
      secure = true,
      name = "mutants",
      description =
          "how many mutated test cases should be additionally generated (disabled if <= 0)")
  private int numMutations = 0;

  @Option(
      secure = true,
      name = "execution",
      description =
          "once a test case is generated for a test goal (internally) "
              + "continue execution of test case from test goal "
              + "to detect collaterally covered test goals")
  private boolean detectFwdCollateralCoverage = false;

  private final Algorithm algorithm;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;

  private final ConfigurableProgramAnalysis cpa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private Set<CFAEdge> testTargets;
  private final Property specProp;
  private final ForwardCollateralCoverageDetector forwardCovDetector;

  private final TestCaseExporter exporter;
  private double progress = 0;

  public TestCaseGeneratorAlgorithm(
      final Algorithm pAlgorithm,
      final CFA pCfa,
      final Configuration pConfig,
      final ConfigurableProgramAnalysis pCpa,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Specification pSpec)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    pConfig.inject(this);
    CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, TestCaseGeneratorAlgorithm.class);
    algorithm = pAlgorithm;
    cpa = pCpa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(pConfig, logger, pCfa.getMachineModel());
    TestTargetCPA testTargetCpa =
        CPAs.retrieveCPAOrFail(pCpa, TestTargetCPA.class, TestCaseGeneratorAlgorithm.class);
    testTargets =
        ((TestTargetTransferRelation) testTargetCpa.getTransferRelation()).getTestTargets();

    exporter = new TestCaseExporter(pCfa, logger, pConfig, testTargetCpa.isRunInParallel());

    numMutations = Math.max(numMutations, 0);

    if (pSpec.getProperties().size() == 1) {
      specProp = pSpec.getProperties().iterator().next();
      Preconditions.checkArgument(
          specProp.isCoverage(), "Property %s not supported for test-case generation", specProp);
      Preconditions.checkArgument(
          pSpec.getFiles().size() <= 1,
          "Further specification files not supported for test-case generation");
      Preconditions.checkArgument(
          pSpec.getPathToSpecificationAutomata().isEmpty(),
          "Specification automata not supported n test-case generation");
      Preconditions.checkArgument(
          pSpec.getSpecificationAutomata().isEmpty(),
          "Specification automata not supported n test-case generation");
    } else {
      if (pSpec.getProperties().size() > 1) {
        logger.log(
            Level.INFO,
            "Multiple properties are not supported by test-case generation and will be ignored.");
      }
      if (!pSpec.getFiles().isEmpty()) {
        logger.log(
            Level.INFO,
            "Specification files are not supported by test-case generation and will be ignored.");
      }
      if (!pSpec.getPathToSpecificationAutomata().isEmpty()
          || !pSpec.getSpecificationAutomata().isEmpty()) {
        logger.log(
            Level.INFO,
            "Specification automata are not supported by test-case generation and will be"
                + " ignored.");
      }
      specProp = null;
    }
    forwardCovDetector =
        new ForwardCollateralCoverageDetector(pLogger, pShutdownNotifier, pCfa, pConfig);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReached) throws CPAException, InterruptedException {
    int uncoveredGoalsAtStart = testTargets.size();
    progress = 0;
    // clean up ARG
    if (pReached.getWaitlist().size() > 1
        || !pReached.getWaitlist().contains(pReached.getFirstState())) {
      pReached.getWaitlist().stream()
          .filter((AbstractState state) -> !((ARGState) state).getChildren().isEmpty())
          .forEach(
              (AbstractState state) -> {
                ARGState argState = (ARGState) state;
                List<ARGState> removedChildren = new ArrayList<>(2);
                for (ARGState child : argState.getChildren()) {
                  if (!pReached.contains(child)) {
                    removedChildren.add(child);
                  }
                }
                for (ARGState child : removedChildren) {
                  child.removeFromARG();
                }
              });
    }

    try {
      while (pReached.hasWaitingState() && !testTargets.isEmpty()) {
        shutdownNotifier.shutdownIfNecessary();
        boolean shouldReturnFalse = false;
        boolean ignoreTargetState = false;

        assert ARGUtils.checkARG(pReached);
        //        assert from(pReached).filter(AbstractStates::isTargetState).isEmpty();

        AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_IMPRECISE;
        try {
          status = algorithm.run(pReached);

        } catch (CPAException e) {
          // precaution always set precision to false, thus last target state not handled in case of
          // exception
          status = status.withPrecise(false);
          logger.logUserException(Level.WARNING, e, "Analysis not completed.");
          if (e instanceof CounterexampleAnalysisFailed
              || e instanceof RefinementFailedException
              || e instanceof InfeasibleCounterexampleException) {

            ignoreTargetState = true;
          } else {
            throw e;
          }
        } catch (InterruptedException e1) {
          // may be thrown only be counterexample check, if not will be thrown again in finally
          // block due to respective shutdown notifier call
          status = status.withPrecise(false);
        } finally {

          assert ARGUtils.checkARG(pReached);
          assert (from(pReached).filter(AbstractStates::isTargetState).size() < 2);

          AbstractState reachedState =
              from(pReached).firstMatch(AbstractStates::isTargetState).orNull();
          if (reachedState != null) {
            boolean removeState = true;

            ARGState argState = (ARGState) reachedState;

            Collection<ARGState> parentArgStates = argState.getParents();
            assert (parentArgStates.size() == 1);
            ARGState parentArgState = parentArgStates.iterator().next();

            CFAEdge targetEdge = parentArgState.getEdgeToChild(argState);
            if (targetEdge != null) {
              if (testTargets.contains(targetEdge)) {

                if (status.isPrecise()) {
                  CounterexampleInfo cexInfo =
                      ARGUtils.tryGetOrCreateCounterexampleInformation(
                              argState, cpa, assumptionToEdgeAllocator)
                          .orElseThrow();
                  exporter.writeTestCaseFilesAndMutations(
                      cexInfo, Optional.ofNullable(specProp), numMutations);

                  logger.log(Level.FINE, "Removing test target: " + targetEdge);
                  testTargets.remove(targetEdge);
                  TestTargetProvider.processTargetPath(cexInfo);
                  if (detectFwdCollateralCoverage && !testTargets.isEmpty()) {
                    // TODO may want to handle exceptions
                    forwardCovDetector.detectForwardCollateralCov(cexInfo);
                  }
                  if (shouldReportCoveredErrorCallAsError()) {
                    addErrorStateWithTargetInformation(pReached);
                    shouldReturnFalse = true;
                  }
                  progress++;
                } else {
                  if (ignoreTargetState) {
                    TestTargetState targetState =
                        AbstractStates.extractStateByType(reachedState, TestTargetState.class);
                    Preconditions.checkNotNull(targetState);
                    Preconditions.checkArgument(targetState.isTarget());

                    targetState.changeToStopTargetStatus();
                    removeState = false;
                  }
                  logger.log(
                      Level.FINE,
                      "Status was not precise. Current test target is not removed:" + targetEdge);
                }
              } else {
                logger.log(
                    Level.FINE,
                    "Found test target is not in provided set of test targets:" + targetEdge);
              }
            } else {
              logger.log(Level.FINE, "Target edge was null.");
            }

            if (removeState) {
              argState.removeFromARG();
              pReached.remove(reachedState);
            }
            pReached.reAddToWaitlist(parentArgState);

            assert ARGUtils.checkARG(pReached);
          } else {
            logger.log(Level.FINE, "There was no target state in the reached set.");
          }
          shutdownNotifier.shutdownIfNecessary();
        }
        if (shouldReturnFalse) {
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }

      cleanUpIfNoTestTargetsRemain(pReached);
    } finally {
      if (uncoveredGoalsAtStart != testTargets.size()) {
        logger.log(Level.SEVERE, TestTargetProvider.getCoverageInfo());
      }
    }

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void cleanUpIfNoTestTargetsRemain(final ReachedSet pReached) {
    if (testTargets.isEmpty()) {
      pReached.clearWaitlist();
    }
  }

  private void addErrorStateWithTargetInformation(final ReachedSet pReached) {
    Preconditions.checkState(shouldReportCoveredErrorCallAsError());
    pReached.add(
        new DummyErrorState(pReached.getLastState()) {
          @Serial private static final long serialVersionUID = 5522643115974481914L;

          @Override
          public Set<TargetInformation> getTargetInformation() {
            return PropertyTargetInformation.singleton(specProp);
          }
        },
        SingletonPrecision.getInstance());
  }

  private boolean shouldReportCoveredErrorCallAsError() {
    return reportCoveredErrorCallAsError && CommonCoverageProperty.COVERAGE_ERROR.equals(specProp);
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider statisticsProvider) {
      statisticsProvider.collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(TestTargetProvider.getTestTargetStatisitics(printTestTargetInfoInStats));
  }

  @Override
  public double getProgress() {
    return switch (progressType) {
      case ABSOLUTE -> progress;
      case RELATIVE_TOTAL ->
          progress / Math.max(1, TestTargetProvider.getTotalNumberOfTestTargets());
    };
  }

  @Options(prefix = "testcase")
  static final class ForwardCollateralCoverageDetector {
    // TODO adapt, add documentation, logging, etc.

    @Option(
        secure = true,
        name = "execution.limit.cpu",
        description =
            "Limit for cpu time used for the continued execution of a test case (use seconds or"
                + " specify a unit; -1 for infinite)")
    @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = -1)
    private TimeSpan cpuTime = TimeSpan.ofNanos(-1);

    private final LogManager logger;
    private final ShutdownNotifier shutdownNotifier;
    private final CFA cfa;
    private final ConfigurableProgramAnalysis executionCPA;
    private final ReachedSet reached;
    private final Set<CFAEdge> testTargets;

    public ForwardCollateralCoverageDetector(
        final LogManager pLogger,
        final ShutdownNotifier pShutdown,
        final CFA pCfa,
        final Configuration pGeneratorConfig)
        throws InvalidConfigurationException, CPAException, InterruptedException {
      logger = pLogger;
      shutdownNotifier = pShutdown;
      cfa = pCfa;
      Configuration executionConfig = generateExecutionConfig(pGeneratorConfig);
      CoreComponentsFactory localFactory =
          new CoreComponentsFactory(
              executionConfig, logger, shutdownNotifier, AggregatedReachedSets.empty(), cfa);
      executionCPA = localFactory.createCPA(Specification.alwaysSatisfied());
      reached = localFactory.createReachedSet(executionCPA);
      testTargets =
          ((TestTargetTransferRelation)
                  CPAs.retrieveCPAOrFail(
                          executionCPA,
                          TestTargetCPA.class,
                          ForwardCollateralCoverageDetector.class)
                      .getTransferRelation())
              .getTestTargets();
    }

    private Configuration generateExecutionConfig(final Configuration pGeneratorConfig)
        throws InvalidConfigurationException {
      ConfigurationBuilder configBuilder = Configuration.builder().copyFrom(pGeneratorConfig);
      configBuilder.clearOption("CompositeCPA.cpas");
      configBuilder.clearOption("testcase.stopAtTarget");
      // TODO check right order of wrapper states
      // String compositeCPAs = pGeneratorConfig.
      // TODO set CPAs
      /*configBuilder.setOption(
      "CompositeCPA.cpas", "cpa.location.LocationCPA,cpa.callstack.CallstackCPA, cpa.value.ValueCPA, cpa.testtargets.TestTargetCPA");*/
      // need to copy test target options
      configBuilder.setOption("testcase.stopAtTarget", "false");
      return configBuilder.build();
    }

    // Extractor runs an additional analysis that checks if a generated test covers additional goals
    private void detectForwardCollateralCov(final CounterexampleInfo pCexInfo) {
      logger.log(Level.INFO, "Running Extractor Algorithm to compute extended path.");

      try {
        ShutdownManager lShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
        ResourceLimitChecker.createCpuTimeLimitChecker(logger, lShutdownManager, cpuTime).start();

        reached.clear();
        Preconditions.checkState(reached.isEmpty());
        Preconditions.checkState(!reached.hasWaitingState());

        AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_IMPRECISE;

        ARGState lastState = generateExecutionStartState(pCexInfo);
        ARGState predState;
        CFAEdge predToSuccEdge;
        Collection<? extends AbstractState> successors;
        try {
          Precision precision =
              executionCPA.getInitialPrecision(
                  AbstractStates.extractLocation(lastState),
                  StateSpacePartition.getDefaultPartition());

          do {
            lShutdownManager.getNotifier().shutdownIfNecessary();

            // get successors
            successors =
                executionCPA.getTransferRelation().getAbstractSuccessors(lastState, precision);

            if (successors.size() == 1) {

              predState = lastState;
              lastState = (ARGState) successors.iterator().next();
              predToSuccEdge = predState.getEdgeToChild(lastState);
              Preconditions.checkNotNull(predToSuccEdge);

              if (isInput(predToSuccEdge)) {
                break;
              }

              lShutdownManager.getNotifier().shutdownIfNecessary();
              // perform precision adjustment TODO leave out?
              Optional<PrecisionAdjustmentResult> precAdjustmentOptional =
                  executionCPA
                      .getPrecisionAdjustment()
                      .prec(lastState, precision, reached, Functions.identity(), lastState);
              if (precAdjustmentOptional.isPresent()) {
                PrecisionAdjustmentResult precAdjustmentResult =
                    precAdjustmentOptional.orElseThrow();
                Preconditions.checkState(precAdjustmentResult.abstractState() instanceof ARGState);
                lastState = (ARGState) precAdjustmentResult.abstractState();
                precision = precAdjustmentResult.precision();
              }

              reached.add(lastState, precision);

              // remove target state if necessary
              if (lastState.isTarget()) {
                // TODO do we want to have counterexample check?
                logger.log(Level.FINE, ""); // TODO adapt
                testTargets.remove(predToSuccEdge);
              }
            }
          } while (successors.size() == 1);
        } catch (CPAException e) {
          // precaution always set precision to false, thus last target state not handled in case of
          // exception
          logger.logUserException(Level.WARNING, e, "Analysis not completed.");

        } catch (InterruptedException e1) {
          // may be thrown only be counterexample check, if not will be thrown again in finally
          // block due to respective shutdown notifier call
          status = status.withPrecise(false);
        } finally {
          // todo shutdown here enough?
          lShutdownManager.requestShutdown("Analysis terminated");
        }

        // mark additionally covered test targets as covered

        logger.log(Level.INFO, "Finished Extraction Analysis.");
      } catch (IllegalArgumentException e2) {
        // TODO Auto-generated catch block
        e2.printStackTrace();
      }
    }

    private ARGState generateExecutionStartState(final CounterexampleInfo pCexInfo)
        throws IllegalArgumentException {
      // create start state for analysis
      // TODO adapt
      List<AbstractState> elements =
          ((CompositeState) pCexInfo.getTargetState().getWrappedState())
              .getWrappedStates().stream()
                  .map(absState -> processElements(pCexInfo, absState))
                  .collect(ImmutableList.toImmutableList());
      ARGState startARGNode = new ARGState(new CompositeState(elements), null);

      return startARGNode;
    }

    // ValueAnalysisState is explicitly copied before initialization to circumvent side effects
    private AbstractState processElements(CounterexampleInfo cexInfo, AbstractState abstractState) {
      if (abstractState instanceof ValueAnalysisState vaState) {
        return initializeVAState(cexInfo, ValueAnalysisState.copyOf(vaState));
      } else {
        return abstractState;
      }
    }

    // extracts individual expressions from the counterexample and adds them to the
    // ValueAnalysisState
    // of the starting state in descending order relative to the line number of the c program
    private ValueAnalysisState initializeVAState(
        CounterexampleInfo cexInfo, ValueAnalysisState valueAnalysisState) {
      // todo leave main here as it is working just fine
      ExpressionValueVisitor visitor =
          new ExpressionValueVisitor(
              valueAnalysisState,
              "main",
              cfa.getMachineModel(),
              new LogManagerWithoutDuplicates(logger));
      CFAPathWithAssumptions reachStateAssignments = cexInfo.getCFAPathWithAssignments();
      for (int i = reachStateAssignments.size() - 1; i >= 0; i--) {
        CFAEdgeWithAssumptions edgeWithAssignment = reachStateAssignments.get(i);
        ImmutableList<AExpressionStatement> stateExpStmts = edgeWithAssignment.getExpStmts();
        mapToExpStmt(stateExpStmts, valueAnalysisState, visitor);
      }
      return valueAnalysisState;
    }

    private void mapToExpStmt(
        ImmutableList<AExpressionStatement> expStmt,
        ValueAnalysisState valueAnalysisState,
        ExpressionValueVisitor visitor) {
      if (expStmt.isEmpty()) {
        return;
      }
      for (int i = 0; i < expStmt.size(); i++) {
        writeExpressionToState(
            (CBinaryExpression) expStmt.get(i).getExpression(), valueAnalysisState, visitor);
      }
    }

    // reads the value assigned to a variable, and adds that value to the ValueAnalysisstate,
    // but only if there is no distinct value already tracked for that variable
    private void writeExpressionToState(
        CBinaryExpression cBinaryExpression,
        ValueAnalysisState valueAnalysisState,
        ExpressionValueVisitor visitor) {
      try {
        MemoryLocation memLoc = visitor.evaluateMemoryLocation(cBinaryExpression.getOperand1());
        // if variable is already assigned then no new assignment is added to the VAstate
        if (memLoc == null || valueAnalysisState.contains(memLoc)) {
          return;
        }
        CIntegerLiteralExpression op2 = (CIntegerLiteralExpression) cBinaryExpression.getOperand2();
        Value variableValue = new NumericValue(op2.getValue());
        if (!variableValue.isUnknown()) {
          valueAnalysisState.assignConstant(memLoc, variableValue, op2.getExpressionType());
        }
      } catch (UnrecognizedCodeException e) {
        logger.log(Level.FINE, "No Memorylocation found for CExpression.");
      }
    }

    private boolean isInput(final CFAEdge pCFAEdge) {
      if (pCFAEdge instanceof AStatementEdge stmtEdge) {
        AStatement statement = stmtEdge.getStatement();

        if (statement instanceof AFunctionCall functionCall) {
          AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
          AFunctionDeclaration functionDeclaration = functionCallExpression.getDeclaration();

          if (!isPredefinedFunction(functionDeclaration)
              && !(functionCallExpression.getExpressionType() instanceof CVoidType)
              && (functionCallExpression.getExpressionType() != JSimpleType.VOID)) {
            // be conservative and assume that this is an input
            return true;
          }
        }
      }
      return false;
    }
  }
}
