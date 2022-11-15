// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.Appenders;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.ErrorPathShrinker;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.ExtendedWitnessExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessToOutputFormatsUtils;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.coverage.CoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.CoverageReportGcov;
import org.sosy_lab.cpachecker.util.cwriter.PathToCTranslator;
import org.sosy_lab.cpachecker.util.cwriter.PathToConcreteProgramTranslator;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfoExporter;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter;
import org.sosy_lab.cpachecker.util.testcase.TestCaseExporter;

@Options(prefix = "counterexample.export", deprecatedPrefix = "cpa.arg.errorPath")
public class CEXExporter {

  enum CounterexampleExportType {
    CBMC,
    CONCRETE_EXECUTION;
  }

  @Option(
      secure = true,
      name = "compressWitness",
      description = "compress the produced error-witness automata using GZIP compression.")
  private boolean compressWitness = true;

  @Option(
      secure = true,
      description =
          "exports a JSON file describing found faults, if fault localization is activated")
  private boolean exportFaults = true;

  @Option(
      secure = true,
      name = "codeStyle",
      description = "exports either CMBC format or a concrete path program")
  private CounterexampleExportType codeStyle = CounterexampleExportType.CBMC;

  @Option(
      secure = true,
      name = "filters",
      description =
          "Filter for irrelevant counterexamples to reduce the number of similar counterexamples"
              + " reported. Only relevant with analysis.stopAfterError=false and"
              + " counterexample.export.exportImmediately=true. Put the weakest and cheapest filter"
              + " first, e.g., PathEqualityCounterexampleFilter.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.cpa.arg.counterexamples")
  private List<CounterexampleFilter.Factory> cexFilterClasses =
      ImmutableList.of(PathEqualityCounterexampleFilter::new);

  private final CounterexampleFilter cexFilter;

  private final CEXExportOptions options;
  private final LogManager logger;
  private final WitnessExporter witnessExporter;
  private final ExtendedWitnessExporter extendedWitnessExporter;
  private final HarnessExporter harnessExporter;
  private final FaultLocalizationInfoExporter faultExporter;
  private TestCaseExporter testExporter;

  public CEXExporter(
      Configuration config,
      CEXExportOptions pOptions,
      LogManager pLogger,
      CFA cfa,
      ConfigurableProgramAnalysis cpa,
      WitnessExporter pWitnessExporter,
      ExtendedWitnessExporter pExtendedWitnessExporter)
      throws InvalidConfigurationException {
    config.inject(this);
    options = pOptions;
    logger = pLogger;
    witnessExporter = checkNotNull(pWitnessExporter);
    extendedWitnessExporter = checkNotNull(pExtendedWitnessExporter);

    if (!options.disabledCompletely()) {
      cexFilter =
          CounterexampleFilter.createCounterexampleFilter(config, pLogger, cpa, cexFilterClasses);
      harnessExporter = new HarnessExporter(config, pLogger, cfa);
      testExporter = new TestCaseExporter(cfa, logger, config);
      faultExporter = new FaultLocalizationInfoExporter(config);
    } else {
      cexFilter = null;
      harnessExporter = null;
      testExporter = null;
      faultExporter = null;
    }
  }

  /** See {@link #exportCounterexample(ARGState, CounterexampleInfo)}. */
  public void exportCounterexampleIfRelevant(
      final ARGState pTargetState, final CounterexampleInfo pCounterexampleInfo)
      throws InterruptedException {
    if (options.disabledCompletely()) {
      return;
    }

    if (cexFilter.isRelevant(pCounterexampleInfo)) {
      exportCounterexample(pTargetState, pCounterexampleInfo);
    } else {
      logger.log(
          Level.FINEST,
          "Skipping counterexample printing because it is similar to one of already printed.");
    }
  }

  /**
   * Export an Error Trace in different formats, for example as C-file, dot-file or automaton.
   *
   * @param targetState state of an ARG, used as fallback, if pCounterexampleInfo contains no
   *     targetPath.
   * @param counterexample contains further information and the (optional) targetPath. If the
   *     targetPath is available, it will be used for the output. Otherwise we use backwards
   *     reachable states from pTargetState.
   */
  public void exportCounterexample(
      final ARGState targetState, final CounterexampleInfo counterexample) {
    checkNotNull(targetState);
    checkNotNull(counterexample);

    if (options.disabledCompletely()) {
      return;
    }

    if (exportFaults && counterexample instanceof FaultLocalizationInfo && faultExporter != null) {
      try {
        faultExporter.export(
            ((FaultLocalizationInfo) counterexample).getRankedList(),
            counterexample
                .getCFAPathWithAssignments()
                .get(counterexample.getCFAPathWithAssignments().size() - 1)
                .getCFAEdge());
      } catch (IOException | IndexOutOfBoundsException pE) {
        logger.logUserException(Level.WARNING, pE, "Could not export faults as JSON.");
        throw new AssertionError(pE);
      }
    }

    final ARGPath targetPath = counterexample.getTargetPath();
    final BiPredicate<ARGState, ARGState> isTargetPathEdge =
        BiPredicates.pairIn(ImmutableSet.copyOf(targetPath.getStatePairs()));
    final ARGState rootState = targetPath.getFirstState();
    final int uniqueId = counterexample.getUniqueId();

    if (options.getCoveragePrefix() != null) {
      Path outputPath = options.getCoveragePrefix().getPath(counterexample.getUniqueId());
      try (Writer gcovFile = IO.openOutputFile(outputPath, Charset.defaultCharset())) {
        CoverageReportGcov.write(CoverageCollector.fromCounterexample(targetPath), gcovFile);
      } catch (IOException e) {
        logger.logUserException(
            Level.WARNING, e, "Could not write coverage information for counterexample to file");
      }
    }

    writeErrorPathFile(options.getErrorPathFile(), uniqueId, counterexample);

    if (options.getCoreFile() != null) {
      // the shrinked errorPath only includes the nodes,
      // that are important for the error, it is not a complete path,
      // only some nodes of the targetPath are part of it
      ErrorPathShrinker pathShrinker = new ErrorPathShrinker();
      CFAPathWithAssumptions targetPAssum = null;
      if (counterexample.isPreciseCounterExample()) {
        targetPAssum = counterexample.getCFAPathWithAssignments();
      }
      List<Pair<CFAEdgeWithAssumptions, Boolean>> shrinkedErrorPath =
          pathShrinker.shrinkErrorPath(targetPath, targetPAssum);

      // present only the important edges in the Counterxample.core.txt output file
      List<CFAEdgeWithAssumptions> importantShrinkedErrorPath = new ArrayList<>();
      for (Pair<CFAEdgeWithAssumptions, Boolean> pair : shrinkedErrorPath) {
        if (pair.getSecond()) {
          importantShrinkedErrorPath.add(pair.getFirst());
        }
      }

      writeErrorPathFile(
          options.getCoreFile(),
          uniqueId,
          Appenders.forIterable(Joiner.on('\n'), importantShrinkedErrorPath));
    }

    final Set<ARGState> pathElements;
    Appender pathProgram = null;
    if (counterexample.isPreciseCounterExample()) {
      pathElements = targetPath.getStateSet();

      if (options.getSourceFile() != null) {
        switch (codeStyle) {
          case CONCRETE_EXECUTION:
            pathProgram =
                PathToConcreteProgramTranslator.translateSinglePath(
                    targetPath, counterexample.getCFAPathWithAssignments());
            break;
          case CBMC:
            pathProgram = PathToCTranslator.translateSinglePath(targetPath);
            break;
          default:
            throw new AssertionError("Unhandled case statement: " + codeStyle);
        }
      }

    } else {
      // Imprecise error path.
      // For the text export, we have no other chance,
      // but for the C code and graph export we use all existing paths
      // to avoid this problem.
      pathElements = ARGUtils.getAllStatesOnPathsTo(targetState);

      if (options.getSourceFile() != null) {
        switch (codeStyle) {
          case CONCRETE_EXECUTION:
            logger.log(
                Level.WARNING,
                "Cannot export imprecise counterexample to C code for concrete execution.");
            break;
          case CBMC:
            // "translatePaths" does not work if the ARG branches without assume edge
            if (ARGUtils.hasAmbiguousBranching(rootState, pathElements)) {
              pathProgram = PathToCTranslator.translateSinglePath(targetPath);
            } else {
              pathProgram = PathToCTranslator.translatePaths(rootState, pathElements);
            }
            break;
          default:
            throw new AssertionError("Unhandled case statement: " + codeStyle);
        }
      }
    }

    if (pathProgram != null) {
      writeErrorPathFile(options.getSourceFile(), uniqueId, pathProgram);
    }

    writeErrorPathFile(
        options.getDotFile(),
        uniqueId,
        (Appender)
            pAppendable ->
                ARGToDotWriter.write(
                    pAppendable,
                    rootState,
                    ARGState::getChildren,
                    Predicates.in(pathElements),
                    isTargetPathEdge));

    writeErrorPathFile(
        options.getAutomatonFile(),
        uniqueId,
        (Appender)
            pAppendable ->
                ARGUtils.producePathAutomaton(
                    pAppendable, rootState, pathElements, "ErrorPath" + uniqueId, counterexample));

    for (Pair<Object, PathTemplate> info : counterexample.getAllFurtherInformation()) {
      if (info.getSecond() != null) {
        writeErrorPathFile(info.getSecond(), uniqueId, info.getFirst());
      }
    }

    try {
      final Witness witness =
          witnessExporter.generateErrorWitness(
              rootState, Predicates.in(pathElements), isTargetPathEdge, counterexample);

      writeErrorPathFile(
          options.getWitnessFile(),
          uniqueId,
          (Appender)
              pApp -> {
                WitnessToOutputFormatsUtils.writeToGraphMl(witness, pApp);
              },
          compressWitness);

      writeErrorPathFile(
          options.getWitnessDotFile(),
          uniqueId,
          (Appender)
              pApp -> {
                WitnessToOutputFormatsUtils.writeToDot(witness, pApp);
              },
          compressWitness);
    } catch (InterruptedException e) {
      logger.logUserException(Level.WARNING, e, "Could not export witness due to interruption");
    }

    if (options.getExtendedWitnessFile() != null) {
      try {
        Witness extWitness =
            extendedWitnessExporter.generateErrorWitness(
                rootState, Predicates.in(pathElements), isTargetPathEdge, counterexample);
        writeErrorPathFile(
            options.getExtendedWitnessFile(),
            uniqueId,
            (Appender)
                pAppendable -> {
                  WitnessToOutputFormatsUtils.writeToGraphMl(extWitness, pAppendable);
                },
            compressWitness);
      } catch (InterruptedException e) {
        logger.logUserException(Level.WARNING, e, "Could not export witness due to interruption");
      }
    }

    writeErrorPathFile(
        options.getTestHarnessFile(),
        uniqueId,
        (Appender)
            pAppendable ->
                harnessExporter.writeHarness(
                    pAppendable,
                    rootState,
                    Predicates.in(pathElements),
                    isTargetPathEdge,
                    counterexample));

    if (options.exportToTest() && testExporter != null) {
      testExporter.writeTestCaseFiles(counterexample, Optional.empty());
    }
  }

  // Copied from org.sosy_lab.cpachecker.util.coverage.FileCoverageInformation.addVisitedLine(int)
  public void addVisitedLine(Map<Integer, Integer> visitedLines, int pLine) {
    checkArgument(pLine > 0);
    if (visitedLines.containsKey(pLine)) {
      visitedLines.put(pLine, visitedLines.get(pLine) + 1);
    } else {
      visitedLines.put(pLine, 1);
    }
  }

  private void writeErrorPathFile(@Nullable PathTemplate template, int uniqueId, Object content) {
    writeErrorPathFile(template, uniqueId, content, false);
  }

  private void writeErrorPathFile(
      @Nullable PathTemplate template, int uniqueId, Object content, boolean pCompress) {
    if (template != null) {
      // fill in index in file name
      Path file = template.getPath(uniqueId);

      try {
        if (!pCompress) {
          IO.writeFile(file, Charset.defaultCharset(), content);
        } else {
          file = file.resolveSibling(file.getFileName() + ".gz");
          IO.writeGZIPFile(file, Charset.defaultCharset(), content);
        }
      } catch (IOException e) {
        logger.logUserException(
            Level.WARNING, e, "Could not write information about the error path to file");
      }
    }
  }
}
