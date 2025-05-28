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
import com.google.common.collect.ImmutableSetMultimap;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
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
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;
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

  private final Algorithm algorithm;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final ConfigurableProgramAnalysis cpa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
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
    cpa = pCpa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
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
        assert from(pReached).filter(AbstractStates::isTargetState).isEmpty();

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
          // block due to respective shutdown notifier call)
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
                  runExtractorAlgo(pReached, (ARGState) reachedState, (ARGState) reachedState, cexInfo);

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

  private ARGState getParentArgState(ARGState argState)
  {
    Collection<ARGState> parentArgStates = argState.getParents();

    assert (parentArgStates.size() == 1);

    return parentArgStates.iterator().next();
  }

  // eReached is a single arg state thats the starting point of the extraction value analysis
  // after the algo is done, the newly reached states are added to this variable
  private void runExtractorAlgo(
      final ReachedSet pReached,
      AbstractState reachedState,
      ARGState argState,
      CounterexampleInfo cexInfo) {

    // initialisation of starting state and reachedSet
    ARGState eStartState = new ARGState(argState.getWrappedState(), null);
    getExpressions(cexInfo, eStartState);

//    SingletonPrecision.getInstance());
    ReachedSet eReached = factory.createReachedSet(cpa);
    eReached.add(eStartState, pReached.getPrecision(reachedState));
    assert from(eReached).filter(AbstractStates::isTargetState).isEmpty();

    // exploring of additional ARGstates
// todo add try catch finally block
//  status = extractorAlgo.run(eReached);
//    todo call processGoalState() for all elements of eReached that are testTargets
//    todo optimisation: do not call for the first element of eReached, as that got already covered

    AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_IMPRECISE;
    try {
      status = algorithm.run(eReached);

    } catch (CPAException e) {
      // precaution always set precision to false, thus last target state not handled in case of
      // exception
      status = status.withPrecise(false);
      logger.logUserException(Level.WARNING, e, "Analysis not completed.");

    } catch (InterruptedException e1) {
      // may be thrown only be counterexample check, if not will be thrown again in finally
      // block due to respective shutdown notifier call)
      status = status.withPrecise(false);
    } finally {


//  targetAbstractStates = from(eReached).filter(AbstractStates::isTargetState);
//    processGoalState(eStartState);
    }
  }

  // extracts individual expressions from the counterexample and adds them to the starting state
  // in descending order relative to the line number of the c program
  private void getExpressions(CounterexampleInfo cexInfo, ARGState eStartState) {
    // extract all variable assignments from reached state
    CFAPathWithAssumptions reachStateAssignments =
        cexInfo.getCFAPathWithAssignments();
    int counter = reachStateAssignments.size() - 1;
    while (counter >= 0) {
      CFAEdgeWithAssumptions edgeWithAssignment = reachStateAssignments.get(counter);
      ImmutableList<AExpressionStatement> stateExpStmts = edgeWithAssignment.getExpStmts();
      writeExpressionToState(stateExpStmts, eStartState);
      counter--;
    }
  }

  // reads the value assigned to a variable, and adds that value to the abstract state,
  // but only if there is no disctinct value tracked already for that variable
  private void writeExpressionToState(
      ImmutableList<AExpressionStatement> expStmt,
      ARGState eStartState) {
    if (expStmt.isEmpty()) return;
    assert expStmt.size()
        == 1; //todo can expStmt contain more than 1 element? replace with precondition?
    //todo check expStmt for being instance of CBinaryExpression
    // todo read from Expression

    expStmt.get(0).getExpression();
    // Value visit(CBinaryExpression pE) from AssigningValueVisitior class
//    getOperand1()
//        getOperand2()
//    getOperator()
    // todo write to ARGState
//    CompositeState wrappedState = (CompositeState) eStartState.getWrappedState();
//
//    ValueAnalysisState valueAnalysisState = (ValueAnalysisState) wrappedState.get(3);
//    if (valueAnalysisState == null) return;
//    valueAnalysisState.assignConstant("x", 1);
  }

  // checks for singe ARGState if the the targetEdge that led to that state is an uncovered test goal
  // if so, the goal is marked as covered
  private void processGoalState(ARGState nextGoalState) {
    if (nextGoalState == null) {
      return;
    }
    ARGState parentArgState = getParentArgState(nextGoalState);
    CFAEdge targetEdge = parentArgState.getEdgeToChild(nextGoalState);
    if (targetEdge != null) {
      if (testTargets.contains(targetEdge)) {
        logger.log(Level.FINE, "Removing test target: " + targetEdge);
        testTargets.remove(targetEdge);
      } else {
        logger.log(
            Level.FINE,
            "Extractor: Ignoring target as it is not in set of test targets."
                + targetEdge);
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
