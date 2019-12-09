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
import com.google.common.collect.ImmutableSet;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetCPA;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetProvider;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetState;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Property.CommonCoverageType;
import org.sosy_lab.cpachecker.util.SpecificationProperty;
import org.sosy_lab.cpachecker.util.error.DummyErrorState;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter;
import org.sosy_lab.cpachecker.util.testcase.TestCaseExporter;
import org.sosy_lab.cpachecker.util.testcase.XMLTestCaseExport;

@Options(prefix = "testcase")
public class TestCaseGeneratorAlgorithm implements ProgressReportingAlgorithm, StatisticsProvider {

  private static enum FormatType {
    HARNESS,
    METADATA,
    PLAIN,
    XML;
  }

  private static UniqueIdGenerator id = new UniqueIdGenerator();

  @Option(secure = true, name = "file", description = "export test harness to file as code")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testHarnessFile = null;

  @Option(secure = true, name = "values", description = "export test values to file (line separated)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testValueFile = null;

  @Option(
    secure = true,
    name = "xml",
    description = "export test cases to xm file (Test-Comp format)"
  )
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testXMLFile = null;

  @Option(
    secure = true,
    name = "compress",
    description = "zip all exported test cases into a single file"
  )
  private boolean zipTestCases = false;

  @Option(
    secure = true,
    name = "zip.file",
    description = "Zip file into which all test case files are bundled"
  )
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testCaseZip = null;

  @Option(
    secure = true,
    name = "inStats",
    description = "display all test targets and non-covered test targets in statistics"
  )
  private boolean printTestTargetInfoInStats = false;

  @Option(secure = true,  description = "when generating tests covering error call stop as soon as generated one test case and report false (only possible in combination with error call property specification")
  private boolean reportCoveredErrorCallAsError = false;

  private final Algorithm algorithm;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final ConfigurableProgramAnalysis cpa;
  private final CFA cfa;
  private final HarnessExporter harnessExporter;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Set<CFAEdge> testTargets;
  private final SpecificationProperty specProp;
  private final String producerString;
  private FileSystem zipFS = null;
  private double progress = 0;

  public TestCaseGeneratorAlgorithm(
      final Algorithm pAlgorithm,
      final CFA pCfa,
      final Configuration pConfig,
      final ConfigurableProgramAnalysis pCpa,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Specification pSpec)
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
    producerString = CPAchecker.getVersion(pConfig);

    Preconditions.checkState(
        !isZippedTestCaseWritingEnabled() || testCaseZip != null,
        "Need to specify testcase.zip.file if test case values are compressed.");

    if (pSpec.getProperties().size() == 1) {
      specProp = pSpec.getProperties().iterator().next();
      Preconditions.checkArgument(
          specProp.getProperty() instanceof CommonCoverageType,
          "Property %s not supported for test generation",
          specProp.getProperty());
    } else {
      specProp = null;
    }
  }

  @Override
  public AlgorithmStatus run(final ReachedSet pReached)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {
    int uncoveredGoalsAtStart = testTargets.size();
    progress = 0;
    // clean up ARG
    if (pReached.getWaitlist().size() > 1
        || !pReached.getWaitlist().contains(pReached.getFirstState())) {
      pReached
          .getWaitlist()
          .stream()
          .filter(
              (AbstractState state) -> {
                return !((ARGState) state).getChildren().isEmpty();
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

      boolean shouldReturnFalse, ignoreTargetState;
      while (pReached.hasWaitingState() && !testTargets.isEmpty()) {
        shutdownNotifier.shutdownIfNecessary();
        shouldReturnFalse = false;
        ignoreTargetState = false;

        assert ARGUtils.checkARG(pReached);
        assert (from(pReached).filter(IS_TARGET_STATE).isEmpty());

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
            boolean removeState = true;

            ARGState argState = (ARGState) reachedState;

            Collection<ARGState> parentArgStates = argState.getParents();

            assert (parentArgStates.size() == 1);

            ARGState parentArgState = parentArgStates.iterator().next();

            CFAEdge targetEdge = parentArgState.getEdgeToChild(argState);
            if (targetEdge != null) {
              if (testTargets.contains(targetEdge)) {

                if (status.isPrecise()) {
                  writeTestCaseFiles(argState);

                  logger.log(Level.FINE, "Removing test target: " + targetEdge.toString());
                  testTargets.remove(targetEdge);

                  if (shouldReportCoveredErrorCallAsError()) {
                    addErrorStateWithViolatedProperty(pReached);
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
    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "Problem while handling zip file with test case");
    } finally {
      if (uncoveredGoalsAtStart != testTargets.size()) {
        logger.log(Level.SEVERE, TestTargetProvider.getCoverageInfo());
      }
      closeZipFS();

    }

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void cleanUpIfNoTestTargetsRemain(final ReachedSet pReached) {
    if (testTargets.isEmpty()) {
      List<AbstractState> waitlist = new ArrayList<>(pReached.getWaitlist());
      for (AbstractState state : waitlist) {
        pReached.removeOnlyFromWaitlist(state);
      }
    }
  }

  private void addErrorStateWithViolatedProperty(final ReachedSet pReached) {
    Preconditions.checkState(shouldReportCoveredErrorCallAsError());
    pReached.add(
        new DummyErrorState(pReached.getLastState()) {
          private static final long serialVersionUID = 5522643115974481914L;

          @Override
          public Set<Property> getViolatedProperties() throws IllegalStateException {
            return ImmutableSet.of(
                new Property() {
                  @Override
                  public String toString() {
                    return specProp.getProperty().toString();
                  }
                });
          }
        },
        SingletonPrecision.getInstance());
  }

  private boolean shouldReportCoveredErrorCallAsError() {
    return reportCoveredErrorCallAsError
        && specProp != null
        && specProp.getProperty().equals(CommonCoverageType.COVERAGE_ERROR);
  }

  private void writeTestCaseFiles(final ARGState pTarget) {

    // TODO check if this and openZipFS(), closeZipFS() are thread-safe
    if (areTestsEnabled()) {
      CounterexampleInfo cexInfo =
          ARGUtils.tryGetOrCreateCounterexampleInformation(pTarget, cpa, assumptionToEdgeAllocator)
              .orElseThrow();
      ARGPath targetPath = cexInfo.getTargetPath();
      Preconditions.checkState(!zipTestCases || zipFS != null);

      if (testHarnessFile != null) {
        writeTestCase(
            testHarnessFile.getPath(id.getFreshId()), targetPath, cexInfo, FormatType.HARNESS);
      }

      if (testValueFile != null) {
        writeTestCase(
            testValueFile.getPath(id.getFreshId()), targetPath, cexInfo, FormatType.PLAIN);
      }

      if (testXMLFile != null) {
        Path testCaseFile = testXMLFile.getPath(id.getFreshId());
        if (testTargets.size() == TestTargetProvider.getCurrentNumOfTestTargets()) {
          writeTestCase(
              testCaseFile.resolveSibling("metadata.xml"),
              targetPath,
              cexInfo,
              FormatType.METADATA);
        }
        writeTestCase(testCaseFile, targetPath, cexInfo, FormatType.XML);
      }
    }
  }

  private void writeTestCase(
      final Path pFile,
      final ARGPath pTargetPath,
      final CounterexampleInfo pCexInfo,
      final FormatType type) {
    final ARGState rootState = pTargetPath.getFirstState();
    final Predicate<? super ARGState> relevantStates = Predicates.in(pTargetPath.getStateSet());
    final BiPredicate<ARGState, ARGState> relevantEdges =
        BiPredicates.pairIn(ImmutableSet.copyOf(pTargetPath.getStatePairs()));
    try {
      Optional<String> testOutput;

      if (zipTestCases) {
        Path fileName = pFile.getFileName();
        Path testFile =
            zipFS.getPath(fileName != null ? fileName.toString() : id.getFreshId() + "test.txt");
        try (Writer writer =
            new OutputStreamWriter(
                zipFS
                    .provider()
                    .newOutputStream(testFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE),
                Charset.defaultCharset())) {
          switch (type) {
            case HARNESS:
              harnessExporter.writeHarness(
                  writer, rootState, relevantStates, relevantEdges, pCexInfo);
              break;
            case METADATA:
              XMLTestCaseExport.writeXMLMetadata(writer, cfa, specProp, producerString);
              break;
            case PLAIN:
              testOutput =
                  TestCaseExporter.writeTestInputNondetValues(
                      rootState,
                      relevantStates,
                      relevantEdges,
                      pCexInfo,
                      cfa,
                      TestCaseExporter.LINE_SEPARATED);
              if (testOutput.isPresent()) {
                writer.write(testOutput.orElseThrow());
              }
              break;
            case XML:
              testOutput =
                  TestCaseExporter.writeTestInputNondetValues(
                      rootState,
                      relevantStates,
                      relevantEdges,
                      pCexInfo,
                      cfa,
                      XMLTestCaseExport.XML_TEST_CASE);
              if (testOutput.isPresent()) {
                writer.write(testOutput.orElseThrow());
              }
              break;
            default:
              throw new AssertionError("Unknown test case format.");
          }
        }
      } else {
        Object content = null;

        switch (type) {
          case HARNESS:
            content =
                (Appender)
                    appendable ->
                        harnessExporter.writeHarness(
                            appendable, rootState, relevantStates, relevantEdges, pCexInfo);
            break;
          case METADATA:
            content =
                (Appender)
                    appendable ->
                        XMLTestCaseExport.writeXMLMetadata(
                            appendable, cfa, specProp, producerString);
            break;
          case PLAIN:
            testOutput =
                TestCaseExporter.writeTestInputNondetValues(
                    rootState,
                    relevantStates,
                    relevantEdges,
                    pCexInfo,
                    cfa,
                    TestCaseExporter.LINE_SEPARATED);

            if (testOutput.isPresent()) {
              content = (Appender) appendable -> appendable.append(testOutput.orElseThrow());
            }
            break;
          case XML:
            testOutput =
                TestCaseExporter.writeTestInputNondetValues(
                    rootState,
                    relevantStates,
                    relevantEdges,
                    pCexInfo,
                    cfa,
                    XMLTestCaseExport.XML_TEST_CASE);
            if (testOutput.isPresent()) {
              content = (Appender) appendable -> appendable.append(testOutput.orElseThrow());
            }
            break;
          default:
            throw new AssertionError("Unknown test case format.");
        }
        if (content != null) {
          IO.writeFile(pFile, Charset.defaultCharset(), content);
        }
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write test case to file");
    }
  }

  private boolean areTestsEnabled() {
    return testValueFile != null || testHarnessFile != null || testXMLFile != null;
  }

  private boolean isZippedTestCaseWritingEnabled() {
    return zipTestCases && areTestsEnabled();
  }

  private void openZipFS() throws IOException {
    Map<String, String> env = new HashMap<>(1);
    env.put("create", "true");

    Preconditions.checkNotNull(testCaseZip);
    // create parent directories if do not exist
    Path parent = testCaseZip.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    zipFS =
        FileSystems.newFileSystem(URI.create("jar:" + testCaseZip.toUri().toString()), env, null);
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

  @Override
  public double getProgress() {
    return progress / Math.max(1, TestTargetProvider.getCurrentNumOfTestTargets());
  }
}
