/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetCPA;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetProvider;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter;
import org.sosy_lab.cpachecker.util.harness.PredefinedTypes;

@Options(prefix = "testcase")
public class TestCaseGeneratorAlgorithm implements Algorithm, StatisticsProvider {

  private static UniqueIdGenerator id = new UniqueIdGenerator();

  @Option(secure = true, name = "file", description = "export test harness to file as code")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testHarnessFile = null;

  @Option(secure = true, name = "values", description = "export test values to file (line separated)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testValueFile = null;

  @Option(
    secure = true,
    name = "values.compress",
    description = "zip all exported test values into a single file"
  )
  private boolean zipValues = false;

  @Option(secure = true, name = "values.zip", description = "Zip file into which all test values files are bundled")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testValueZip = null;

  @Option(
    secure = true,
    name = "inStats",
    description = "display all test targets and non-covered test targets in statistics"
  )
  private boolean printTestTargetInfoInStats = false;

  private final Algorithm algorithm;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final ConfigurableProgramAnalysis cpa;
  private final CFA cfa;
  private final HarnessExporter harnessExporter;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Set<CFAEdge> testTargets;
  private FileSystem zipFS = null;

  public TestCaseGeneratorAlgorithm(
      Algorithm pAlgorithm,
      CFA pCfa,
      Configuration pConfig,
      ConfigurableProgramAnalysis pCpa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
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
    harnessExporter = new HarnessExporter(pConfig, logger, pCfa);

    Preconditions.checkState(
        !isZippedTestCaseWritingEnabled() || testValueZip != null,
        "Need to specify testcase.values.zip if test case values are compressed.");
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReached)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    int uncoveredGoalsAtStart = testTargets.size();
    // clean up ARG
    if (pReached.getWaitlist().size() > 1
        || !pReached.getWaitlist().contains(pReached.getFirstState())) {
      pReached
          .getWaitlist()
          .stream()
          .filter(
              (AbstractState state) -> {
                return ((ARGState) state).getChildren().size() > 0;
              })
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
      if (isZippedTestCaseWritingEnabled()) {
        openZipFS();
      }

      while (pReached.hasWaitingState() && !testTargets.isEmpty()) {
        shutdownNotifier.shutdownIfNecessary();

        assert ARGUtils.checkARG(pReached);
        assert (from(pReached).filter(IS_TARGET_STATE).isEmpty());

        AlgorithmStatus status = AlgorithmStatus.UNSOUND_AND_PRECISE.withPrecise(false);
        try {
          status = algorithm.run(pReached);

        } catch (CPAException e) {
          // precaution always set precision to false, thus last target state not handled in case of
          // exception
          status = status.withPrecise(false);
          logger.logUserException(Level.WARNING, e, "Analysis not completed.");
          if (!(e instanceof CounterexampleAnalysisFailed
              || e instanceof RefinementFailedException
              || e instanceof InfeasibleCounterexampleException)) {
            throw e;
          }
        } catch (InterruptedException e1) {
          // may be thrown only be counterexample check, if not will be thrown again in finally
          // block due to respective shutdown notifier call)
          status = status.withPrecise(false);
          closeZipFS();
        } catch (Exception e2) {
          // precaution always set precision to false, thus last target state not handled in case of
          // exception
          status = status.withPrecise(false);
          throw e2;
        } finally {

          assert ARGUtils.checkARG(pReached);
          assert (from(pReached).filter(IS_TARGET_STATE).size() < 2);

          AbstractState reachedState = from(pReached).firstMatch(IS_TARGET_STATE).orNull();
          if (reachedState != null) {

            ARGState argState = (ARGState) reachedState;

            Collection<ARGState> parentArgStates = argState.getParents();

            assert (parentArgStates.size() == 1);

            ARGState parentArgState = parentArgStates.iterator().next();

            CFAEdge targetEdge = parentArgState.getEdgeToChild(argState);
            if (targetEdge != null) {
              if (testTargets.contains(targetEdge)) {

                if (status.isPrecise()) {
                  writeTestCaseFile(argState);

                  logger.log(Level.FINE, "Removing test target: " + targetEdge.toString());
                  testTargets.remove(targetEdge);
                } else {
                  logger.log(
                      Level.FINE,
                      "Status was not precise. Current test target is not removed:"
                          + targetEdge.toString());
                }
              } else {
                logger.log(
                    Level.FINE,
                    "Found test target is not in provided set of test targets:"
                        + targetEdge.toString());
              }
            } else {
              logger.log(Level.FINE, "Target edge was null.");
            }

            argState.removeFromARG();
            pReached.remove(reachedState);
            pReached.reAddToWaitlist(parentArgState);

            assert ARGUtils.checkARG(pReached);
          } else {
            logger.log(Level.FINE, "There was no target state in the reached set.");
          }
          shutdownNotifier.shutdownIfNecessary();
        }
      }

      cleanUpIfNoTestTargetsRemain(pReached);
    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "Problem while handling zip file with test cass");
    } finally {
      if (uncoveredGoalsAtStart != testTargets.size()) {
        logger.log(Level.SEVERE, TestTargetProvider.getCoverageInfo());
      }
      closeZipFS();
    }

    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private void cleanUpIfNoTestTargetsRemain(final ReachedSet pReached) {
    if (testTargets.isEmpty()) {
      List<AbstractState> waitlist = new ArrayList<>(pReached.getWaitlist());
      for (AbstractState state : waitlist) {
        pReached.removeOnlyFromWaitlist(state);
      }
    }
  }

  private void writeTestCaseFile(final ARGState pTarget) {
    // write test harness
    if (testHarnessFile != null) {
      CounterexampleInfo cexInfo =
          ARGUtils.tryGetOrCreateCounterexampleInformation(pTarget, cpa, assumptionToEdgeAllocator)
              .get();

      Path file = testHarnessFile.getPath(id.getFreshId());
      ARGPath targetPath = cexInfo.getTargetPath();
      Object content =
          (Appender)
              appendable ->
                  harnessExporter.writeHarness(
                      appendable,
                      targetPath.getFirstState(),
                      Predicates.in(targetPath.getStateSet()),
                      Predicates.in(targetPath.getStatePairs()),
                      cexInfo);
      try {
        IO.writeFile(file, Charset.defaultCharset(), content);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write test harness to file");
      }
    }

    // write test values
    // TODO check if this and openZipFS(), closeZipFS() are thread-safe
    if (testValueFile != null) {
      CounterexampleInfo cexInfo =
          ARGUtils.tryGetOrCreateCounterexampleInformation(pTarget, cpa, assumptionToEdgeAllocator)
              .get();

      Path file = testValueFile.getPath(id.getFreshId());
      ARGPath targetPath = cexInfo.getTargetPath();
      Path fileName;
      try {
        if (zipValues) {
          // write to zip file
          Preconditions.checkArgument(zipFS != null);
          fileName = file.getFileName();
          file =
              zipFS.getPath(fileName != null ? fileName.toString() : id.getFreshId() + "test.txt");
          try (Writer writer =
              new OutputStreamWriter(
                  zipFS
                      .provider()
                      .newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE),
                  Charset.defaultCharset())) {
            writeTestInputNondetValues(
                targetPath.getFirstState(),
                Predicates.in(targetPath.getStateSet()),
                Predicates.in(targetPath.getStatePairs()),
                cexInfo,
                writer);
          }
        } else {
          Object content =
              (Appender)
                  appendable ->
                      writeTestInputNondetValues(
                          targetPath.getFirstState(),
                          Predicates.in(targetPath.getStateSet()),
                          Predicates.in(targetPath.getStatePairs()),
                          cexInfo,
                          appendable);
          IO.writeFile(file, Charset.defaultCharset(), content);
        }
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write test values to file");
      }
    }
  }

  private void writeTestInputNondetValues(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
      final CounterexampleInfo pCounterexampleInfo,
      final Appendable pAppendable)
      throws IOException {

    Preconditions.checkArgument(pCounterexampleInfo.isPreciseCounterExample());
    Multimap<ARGState, CFAEdgeWithAssumptions> valueMap =
        pCounterexampleInfo.getExactVariableValues();

    StringBuilder output = new StringBuilder();
    Set<ARGState> visited = Sets.newHashSet();
    Deque<ARGState> stack = Queues.newArrayDeque();
    Deque<CFAEdge> lastEdgeStack = Queues.newArrayDeque();
    stack.push(pRootState);
    visited.addAll(stack);
    while (!stack.isEmpty()) {
      ARGState previous = stack.pop();
      CFAEdge lastEdge = null;
      if (!lastEdgeStack.isEmpty()) {
        lastEdge = lastEdgeStack.pop();
      }
      if (AbstractStates.isTargetState(previous)) {
        // end of cex path reached, write test values
        assert lastEdge != null
            : "Expected target state to be different from root state, but was not";
        pAppendable.append(output.toString());
      }
      ARGState parent = previous;
      Iterable<CFANode> parentLocs = AbstractStates.extractLocations(parent);
      for (ARGState child : parent.getChildren()) {
        if (pIsRelevantState.apply(child) && pIsRelevantEdge.apply(Pair.of(parent, child))) {
          Iterable<CFANode> childLocs = AbstractStates.extractLocations(child);
          for (CFANode parentLoc : parentLocs) {
            for (CFANode childLoc : childLocs) {
              if (parentLoc.hasEdgeTo(childLoc)) {
                CFAEdge edge = parentLoc.getEdgeTo(childLoc);

                // add the required values for external non-void functions
                if (edge instanceof AStatementEdge) {
                  AStatementEdge statementEdge = (AStatementEdge) edge;
                  if (statementEdge.getStatement() instanceof AFunctionCall) {
                    writeReturnValueForExternalFunction(
                        (AFunctionCall) statementEdge.getStatement(),
                        edge,
                        output,
                        valueMap.get(previous));
                  }
                }

                if (visited.add(child)) {
                  stack.push(child);
                  lastEdgeStack.push(edge);
                }
              }
            }
          }
        }
      }
    }
  }

  private void writeReturnValueForExternalFunction(
      final AFunctionCall functionCall,
      final CFAEdge edge,
      final StringBuilder sb,
      final @Nullable Collection<CFAEdgeWithAssumptions> pAssumptions) {
    AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
    AFunctionDeclaration functionDeclaration = functionCallExpression.getDeclaration();

    if (!PredefinedTypes.isKnownTestFunction(functionDeclaration)
        && !(functionCallExpression.getExpressionType() instanceof CVoidType)
        && (functionCallExpression.getExpressionType() != JSimpleType.getVoid())) {
      // only write if not predefined like e.g. malloc and is non-void

      AExpression nameExpression = functionCallExpression.getFunctionNameExpression();
      if (nameExpression instanceof AIdExpression) {

        ASimpleDeclaration declaration = ((AIdExpression) nameExpression).getDeclaration();
        if (declaration != null && cfa.getFunctionHead(declaration.getQualifiedName()) == null) {
          // external function with return value
          boolean valueWritten = false;

          if (functionCall instanceof AFunctionCallAssignmentStatement) {
            AFunctionCallAssignmentStatement assignment =
                (AFunctionCallAssignmentStatement) functionCall;

            if (pAssumptions != null) {
              // get return value from assumptions in counterexample
              for (AExpression assumption :
                  FluentIterable.from(pAssumptions)
                      .filter(e -> e.getCFAEdge().equals(edge))
                      .transformAndConcat(CFAEdgeWithAssumptions::getExpStmts)
                      .transform(AExpressionStatement::getExpression)) {

                if (assumption instanceof ABinaryExpression
                    && ((ABinaryExpression) assumption).getOperator() == BinaryOperator.EQUALS) {

                  ABinaryExpression binExp = (ABinaryExpression) assumption;

                  if (binExp.getOperand2() instanceof ALiteralExpression
                      && binExp.getOperand1().equals(assignment.getLeftHandSide())) {
                    sb.append(((ALiteralExpression) binExp.getOperand2()).getValue());
                    sb.append('\n');
                    valueWritten = true;
                    break;
                  }
                  if (binExp.getOperand1() instanceof ALiteralExpression
                      && binExp.getOperand2().equals(assignment.getLeftHandSide())) {
                    sb.append(((ALiteralExpression) binExp.getOperand1()).getValue());
                    sb.append('\n');
                    valueWritten = true;
                    break;
                  }
                }
              }
            }

            if (!valueWritten) {
              // could not find any value
              // or value is irrelevant (case of function call statement)
              // use default value
              Type returnType = functionDeclaration.getType().getReturnType();
              if (returnType instanceof CType) {
                returnType = ((CType) returnType).getCanonicalType();

                if (!(returnType instanceof CCompositeType
                    || returnType instanceof CArrayType
                    || returnType instanceof CBitFieldType
                    || (returnType instanceof CElaboratedType
                        && ((CElaboratedType) returnType).getKind() != ComplexTypeKind.ENUM))) {

                  sb.append(
                      ((ALiteralExpression)
                              ((CInitializerExpression)
                                      CDefaults.forType((CType) returnType, FileLocation.DUMMY))
                                  .getExpression())
                          .getValue());
                  sb.append('\n');
                } else {
                  throw new AssertionError("Cannot write test case value (not a literal)");
                }
              } else {
                throw new AssertionError("Cannot write test case value (not a CType)");
              }
            }
          }
        }
      }
    }
  }

  private boolean isZippedTestCaseWritingEnabled() {
    return zipValues && testValueFile != null;
  }

  private void openZipFS() throws IOException {
    Map<String, String> env = new HashMap<>(1);
    env.put("create", "true");

    Preconditions.checkNotNull(testValueZip);
    // create parent directories if do not exist
    Path parent = testValueZip.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    zipFS =
        FileSystems.newFileSystem(URI.create("jar:" + testValueZip.toUri().toString()), env, null);
  }

  private void closeZipFS() {
    if (zipFS != null && zipFS.isOpen()) {
      try {
        zipFS.close();
      } catch (ClosedByInterruptException e1) {
        // nothing needs to be done, is closed anyway
      } catch (IOException e) {
        logger.logException(Level.SEVERE, e, "Problem while handling zip file with test cass");
      }
      zipFS = null;
    }
  }

  @Override
  public void collectStatistics(final Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(TestTargetProvider.getTestTargetStatisitics(printTestTargetInfoInStats));
  }
}
