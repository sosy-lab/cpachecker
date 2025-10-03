// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.IntStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.PropertyTargetInformation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
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
      name = "useExtractor",
      description = "run extraction analysis everytime a test goal is found.")
  private boolean useExtractor = false;

  @Option(
      secure = true,
      name = "extractor.config",
      //      required = true,
      description = "configuration file for test case extraction with CPAchecker")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path configFile = null;

  private final Algorithm algorithm;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final CFA cfa;
  private final ConfigurableProgramAnalysis cpa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification spec;
  private Set<CFAEdge> testTargets;
  private final Property specProp;
  private final CoreComponentsFactory factory;
  private final TestCaseExporter exporter;
  private double progress = 0;

  public TestCaseGeneratorAlgorithm(
      final Algorithm pAlgorithm,
      final CFA pCfa,
      final Configuration pConfig,
      final ConfigurableProgramAnalysis pCpa,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Specification pSpec,
      final CoreComponentsFactory coreFactory)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, TestCaseGeneratorAlgorithm.class);
    algorithm = pAlgorithm;
    cfa = pCfa;
    cpa = pCpa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    spec = pSpec;
    factory = coreFactory;
    assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(pConfig, logger, pCfa.getMachineModel());
    TestTargetCPA testTargetCpa =
        CPAs.retrieveCPAOrFail(pCpa, TestTargetCPA.class, TestCaseGeneratorAlgorithm.class);
    testTargets =
        ((TestTargetTransferRelation) testTargetCpa.getTransferRelation()).getTestTargets();

    exporter = new TestCaseExporter(pCfa, logger, pConfig);

    numMutations = Math.max(numMutations, 0);

    if (pSpec.getProperties().size() == 1) {
      specProp = pSpec.getProperties().iterator().next();
      Preconditions.checkArgument(
          specProp.isCoverage(), "Property %s not supported for test generation", specProp);
    } else {
      specProp = null;
    }
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

            ARGState parentArgState = getParentArgState(argState);

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
                  if (useExtractor) {
                    runExtractorAlgo(pReached, reachedState, cexInfo);
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

  private ARGState getParentArgState(ARGState argState) {
    Collection<ARGState> parentArgStates = argState.getParents();

    assert (parentArgStates.size() == 1);

    return parentArgStates.iterator().next();
  }

  // eReached is a single arg state that's the starting point of the extraction value analysis
  // after the algo is done, the newly reached states are added to this variable
  private void runExtractorAlgo(
      final ReachedSet pReached, AbstractState reachedState, CounterexampleInfo cexInfo) {

    Algorithm extractionAlgorithm;
    try {
      extractionAlgorithm = createExtractorAlgorithm();
    } catch (CPAException e) {
      logger.log(Level.FINE, "Could not create CPA Algorithm for extractor.");
      return;
    }
    // initialisation of starting state and reachedSet
    ARGState argState = (ARGState) reachedState;
    ARGState eStartState = createStartState(argState);
    initializeStartState(cexInfo, eStartState);
    ReachedSet eReached = factory.createReachedSet(cpa);
    eReached.add(eStartState, pReached.getPrecision(reachedState));
    // run value analysis and check what additional targets have been covered
    extractorRunCPAA(eReached, extractionAlgorithm);
    evaluateExtractorResult(eReached);
  }

  private Algorithm createExtractorAlgorithm() throws CPAException {
    Algorithm extractionAlgorithm = null;
    try {
      assert (configFile != null);
      ConfigurationBuilder lConfigBuilder = Configuration.builder().loadFromFile(configFile);
      Configuration lConfig = lConfigBuilder.build();
      // todo create new logger and shutdownnotifier?
      CoreComponentsFactory eFactory =
          new CoreComponentsFactory(
              lConfig, logger, shutdownNotifier, AggregatedReachedSets.empty());

      ConfigurableProgramAnalysis eCpas = eFactory.createCPA(cfa, spec);
      extractionAlgorithm = eFactory.createAlgorithm(eCpas, cfa, spec);

    } catch (InvalidConfigurationException e) {
      logger.log(
          Level.FINE,
          "Could not create CPA Algorithm for extractor because of Invalid Configuration.");
    } catch (IOException e) {
      logger.log(
          Level.FINE, "Could not create CPA Algorithm for extractor because of IO exception:" + e);
    } catch (InterruptedException e) {
      logger.log(
          Level.FINE,
          "Could not create CPA Algorithm for extractor because of Interrupted Exception.");

    } catch (Exception e) {
      logger.log(Level.FINE, "Could not create CPA Algorithm for extractor.");
    }
    if (extractionAlgorithm == null) {
      throw new CPAException("Could not create CPA Algorithm for extractor.");
    }
    return extractionAlgorithm;
  }

  private ARGState createStartState(ARGState argState) {
    CompositeState wrappedState = (CompositeState) argState.getWrappedState();
    List<AbstractState> elements =
        IntStream.range(0, wrappedState.getNumberOfStates())
            .mapToObj(i -> processElements(wrappedState.get(i)))
            .collect(ImmutableList.toImmutableList());
    return new ARGState(new CompositeState(elements), null);
  }

  // parses argstate and modifies it if necessesary during creation of startState
  // todo modify other elements of wrapped state as well?
  private AbstractState processElements(AbstractState abstractState) {
    if (abstractState instanceof ValueAnalysisState) {
      // todo add assert?
      return ValueAnalysisState.copyOf((ValueAnalysisState) abstractState);
    } else {
      return abstractState;
    }
  }

  // extracts individual expressions from the counterexample and adds them to the ValueAnalysisState
  // of the starting state in descending order relative to the line number of the c program
  private void initializeStartState(CounterexampleInfo cexInfo, ARGState eStartState) {
    // extract all variable assignments from reached state
    ValueAnalysisState valueAnalysisState =
        extractVAState((CompositeState) eStartState.getWrappedState());
    ExpressionValueVisitor visitor = new ExpressionValueVisitor(valueAnalysisState, "main",
        valueAnalysisState.getMachineModel(), new LogManagerWithoutDuplicates(logger));
    CFAPathWithAssumptions reachStateAssignments = cexInfo.getCFAPathWithAssignments();
    for (int i = reachStateAssignments.size() - 1; i >= 0; i--) {
      CFAEdgeWithAssumptions edgeWithAssignment = reachStateAssignments.get(i);
      ImmutableList<AExpressionStatement> stateExpStmts = edgeWithAssignment.getExpStmts();
      mapExpressions(stateExpStmts, valueAnalysisState, visitor);
    }
  }

  private void mapExpressions(
      ImmutableList<AExpressionStatement> expStmt,
      ValueAnalysisState valueAnalysisState,
      ExpressionValueVisitor visitor) {
    if (expStmt.isEmpty()) {
      return;
    }
    for (int i = 0; i < expStmt.size(); i++) {
      writeExpressionToState((CBinaryExpression) expStmt.get(i).getExpression(),
          valueAnalysisState, visitor);
    }
  }

  // reads the value assigned to a variable, and adds that value to the abstract state,
  // but only if there is no disctinct value tracked already for that variable
  private void writeExpressionToState(
      CBinaryExpression cBinaryExpression,
      ValueAnalysisState valueAnalysisState,
      ExpressionValueVisitor visitor) {
    try {
      MemoryLocation memLoc = visitor.evaluateMemoryLocation(cBinaryExpression.getOperand1());
      // if variable is already assigned then no new assignmend is added to the VAstate
      if (memLoc == null || valueAnalysisState.contains(memLoc)) {
        return;
      }
      CIntegerLiteralExpression op2 = (CIntegerLiteralExpression) cBinaryExpression.getOperand2();
      Value variableValue = new NumericValue(op2.getValue());
      if (!variableValue.isUnknown()) {
        valueAnalysisState.assignConstant(memLoc, variableValue, null);
      }
    } catch (UnrecognizedCodeException e) {
      logger.log(Level.FINE, "No Memorylocation found for CExpression.");
    }
  }

  private ValueAnalysisState extractVAState(CompositeState wrappedState) {
    assert (wrappedState != null);
    for (int i = wrappedState.getNumberOfStates() - 1; i >= 0; i--) {
      AbstractState element = wrappedState.get(i);
      if (element instanceof ValueAnalysisState) {
        return (ValueAnalysisState) element;
      }
    } // end while
    logger.log(Level.FINE, "Found no ValueAnalysisState in wrappedState.");
    // todo error handling (needed if there is assert?)
    return null;
  }

  // exploring the successors of eStartState for additional ARGstates
  private void extractorRunCPAA(ReachedSet eReached, Algorithm extractionAlgorithm) {

    AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_IMPRECISE;
    try {
      status = extractionAlgorithm.run(eReached);

    } catch (CPAException e) {
      // precaution always set precision to false, thus last target state not handled in case of
      // exception
      status = status.withPrecise(false);
      logger.logUserException(Level.WARNING, e, "Analysis not completed.");

    } catch (InterruptedException e1) {
      // may be thrown only be counterexample check, if not will be thrown again in finally
      // block due to respective shutdown notifier call
      status = status.withPrecise(false);
    }
  }

  private void evaluateExtractorResult(ReachedSet eReached) {
    // drop first element as it is the starting state that is always a TargetState
    from(eReached)
        .skip(1)
        .filter(AbstractStates::isTargetState)
        .forEach(element -> evaluateGoalState((ARGState) element));
  }

  // checks for singe ARGState if the targetEdge that led to that state is an uncovered test goal
  // if so, the goal is marked as covered
  private void evaluateGoalState(ARGState nextGoalState) {
    if (nextGoalState == null) {
      return;
    }
    ARGState parentArgState = getParentArgState(nextGoalState);
    CFAEdge targetEdge = parentArgState.getEdgeToChild(nextGoalState);
    if (targetEdge != null) {
      if (testTargets.contains(targetEdge)) {
        logger.log(Level.FINE, "Extractor: Removing test target: " + targetEdge);
        testTargets.remove(targetEdge);
      } else {
        logger.log(
            Level.FINE,
            "Extractor: Ignoring target as it is not in set of test targets." + targetEdge);
      }
    } else {
      logger.log(Level.FINE, "Extractor: Target edge was null.");
    }
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
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(TestTargetProvider.getTestTargetStatisitics(printTestTargetInfoInStats));
  }

  @Override
  public double getProgress() {
    return switch (progressType) {
      case ABSOLUTE -> progress;
      case RELATIVE_TOTAL ->
          progress / Math.max(1, TestTargetProvider.getTotalNumberOfTestTargets());
      default -> throw new AssertionError("Unhandled progress computation type: " + progressType);
    };
  }
}
