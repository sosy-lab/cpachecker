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
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.specification.Specification;
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
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.coverage.CoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.CoverageReportGcov;
import org.sosy_lab.cpachecker.util.cwriter.PathToCTranslator;
import org.sosy_lab.cpachecker.util.cwriter.PathToConcreteProgramTranslator;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfoExporter;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter;
import org.sosy_lab.cpachecker.util.testcase.TestCaseExporter;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.CounterexampleToWitness;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Options(prefix = "counterexample.export", deprecatedPrefix = "cpa.arg.errorPath")
public class CEXExporter {

  enum CounterexampleExportType {
    CBMC,
    CONCRETE_EXECUTION,
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
  private final CFA cfa;

  private final CEXExportOptions options;
  private final LogManager logger;
  private final Specification specification;
  private final WitnessExporter witnessExporter;
  private final CounterexampleToWitness cexToWitness;
  private final ExtendedWitnessExporter extendedWitnessExporter;
  private final HarnessExporter harnessExporter;
  private final FaultLocalizationInfoExporter faultExporter;
  private TestCaseExporter testExporter;

  public CEXExporter(
      Configuration config,
      CEXExportOptions pOptions,
      LogManager pLogger,
      Specification pSpecification,
      CFA pCFA,
      ConfigurableProgramAnalysis cpa,
      WitnessExporter pWitnessExporter,
      ExtendedWitnessExporter pExtendedWitnessExporter)
      throws InvalidConfigurationException {

    config.inject(this);
    options = pOptions;
    logger = pLogger;
    specification = pSpecification;
    witnessExporter = checkNotNull(pWitnessExporter);
    extendedWitnessExporter = checkNotNull(pExtendedWitnessExporter);
    cfa = pCFA;

    if (!options.disabledCompletely()) {
      cexFilter =
          CounterexampleFilter.createCounterexampleFilter(config, pLogger, cpa, cexFilterClasses);
      harnessExporter = new HarnessExporter(config, pLogger, pCFA);
      testExporter = new TestCaseExporter(pCFA, logger, config);
      faultExporter = new FaultLocalizationInfoExporter(config);
      if (options.getYamlWitnessPathTemplate() != null) {
        cexToWitness = new CounterexampleToWitness(config, pCFA, pSpecification, pLogger);
      } else {
        cexToWitness = null;
      }
    } else {
      cexFilter = null;
      harnessExporter = null;
      testExporter = null;
      faultExporter = null;
      cexToWitness = null;
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
   *     targetPath is available, it will be used for the output. Otherwise, we use backwards
   *     reachable states from pTargetState.
   */
  public void exportCounterexample(
      final ARGState targetState, final CounterexampleInfo counterexample) {

    checkNotNull(targetState);
    checkNotNull(counterexample);

    if (options.disabledCompletely()) {
      return;
    }

    if (exportFaults
        && counterexample instanceof FaultLocalizationInfo faultLocalizationInfo
        && faultExporter != null) {
      try {
        CFAPathWithAssumptions errorPath = counterexample.getCFAPathWithAssignments();
        faultExporter.export(
            faultLocalizationInfo.getRankedList(), errorPath.getLast().getCFAEdge());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not export faults as JSON.");
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
        pathProgram =
            switch (codeStyle) {
              case CONCRETE_EXECUTION ->
                  PathToConcreteProgramTranslator.translateSinglePath(
                      targetPath, counterexample.getCFAPathWithAssignments());
              case CBMC -> PathToCTranslator.translateSinglePath(targetPath);
            };
      }

    } else {
      // Imprecise error path.
      // For the text export, we have no other chance,
      // but for the C code and graph export we use all existing paths
      // to avoid this problem.
      pathElements = ARGUtils.getAllStatesOnPathsTo(targetState);

      if (options.getSourceFile() != null) {
        switch (codeStyle) {
          case CONCRETE_EXECUTION ->
              logger.log(
                  Level.WARNING,
                  "Cannot export imprecise counterexample to C code for concrete execution.");
          case CBMC -> {
            // "translatePaths" does not work if the ARG branches without assume edge
            if (ARGUtils.hasAmbiguousBranching(rootState, pathElements)) {
              pathProgram = PathToCTranslator.translateSinglePath(targetPath);
            } else {
              pathProgram = PathToCTranslator.translatePaths(rootState, pathElements);
            }
          }
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

    if (options.getWitnessFile() != null
        || options.getWitnessDotFile() != null
        || options.getYamlWitnessPathTemplate() != null) {
      try {
        final Witness witness =
            witnessExporter.generateErrorWitness(
                rootState, Predicates.in(pathElements), isTargetPathEdge, counterexample);

        // for .graphml counterexamples
        switch (cfa.getMetadata().getProgramTransformation()) {
          case NONE ->
              writeErrorPathFile(
                  options.getWitnessFile(),
                  uniqueId,
                  (Appender) pApp -> WitnessToOutputFormatsUtils.writeToGraphMl(witness, pApp),
                  compressWitness);
          case SEQUENTIALIZATION -> {
            String updatedGraphml =
                buildDefaultSequentializationCounterexample(
                    cfa.getMetadata().getOriginalCfa().orElseThrow(), specification, logger);
            writeErrorPathFile(options.getWitnessFile(), uniqueId, updatedGraphml, compressWitness);
          }
        }

        // for .dot counterexamples
        writeErrorPathFile(
            options.getWitnessDotFile(),
            uniqueId,
            (Appender) pApp -> WitnessToOutputFormatsUtils.writeToDot(witness, pApp),
            compressWitness);

        // for .yaml counterexamples
        if (cfa.getMetadata().getOriginalCfa().isPresent()) {
          // TODO create .yaml witness from program transformation
        } else if (cfa.getMetadata().getInputLanguage() == Language.C) {
          if (options.getYamlWitnessPathTemplate() != null && cexToWitness != null) {
            try {
              cexToWitness.export(counterexample, options.getYamlWitnessPathTemplate(), uniqueId);
            } catch (IOException e) {
              logger.logUserException(
                  Level.WARNING, e, "Could not generate YAML violation witness.");
            }
          }
        } else {
          logger.log(
              Level.WARNING,
              "Cannot export violation witness to YAML format for languages other than C.");
        }

      } catch (InterruptedException e) {
        logger.logUserException(Level.WARNING, e, "Could not export witness due to interruption");
      }
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
                pAppendable -> WitnessToOutputFormatsUtils.writeToGraphMl(extWitness, pAppendable),
            compressWitness);
      } catch (InterruptedException e) {
        logger.logUserException(Level.WARNING, e, "Could not export witness due to interruption");
      }
    }

    if (options.getTestHarnessFile() != null) {
      Optional<String> harness =
          harnessExporter.writeHarness(
              rootState, Predicates.in(pathElements), isTargetPathEdge, counterexample);
      harness.ifPresent(
          content -> writeErrorPathFile(options.getTestHarnessFile(), uniqueId, content));
    }

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

  /** Returns a dummy {@code .graphml} counterexample without any meaningful nodes or edges. */
  private static String buildDefaultSequentializationCounterexample(
      CFA pOriginalCfa, Specification pSpecification, LogManager pLogger) {

    String defaultGraphml = "./test/programs/mpor/violation_witness/default_counterexample.graphml";

    try {
      File xmlFile = new File(defaultGraphml);
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document document = documentBuilder.parse(xmlFile);
      document.getDocumentElement().normalize();

      document = updateDefaultGraphml(pOriginalCfa, pSpecification, document);

      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      StringWriter stringWriter = new StringWriter();
      transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
      return stringWriter.toString();

    } catch (Exception e) {
      pLogger.log(Level.SEVERE, "Could not parse .graphml file: ", e);
      throw new IllegalArgumentException(e);
    }
  }

  private static Document updateDefaultGraphml(
      CFA pOriginalCfa, Specification pSpecification, Document pDocument) throws IOException {

    Path filePath = pOriginalCfa.getFileNames().getFirst();
    String cpaCheckerVersion = CPAchecker.getPlainVersion();
    String producer =
        cpaCheckerVersion.equals(CPAchecker.unknownVersion)
            ? "CPAchecker"
            : "CPAchecker-" + cpaCheckerVersion;
    String specification = AutomatonGraphmlCommon.getSpecificationAutomaton(pSpecification);
    String programHash = AutomatonGraphmlCommon.computeHash(filePath);
    String architecture = AutomatonGraphmlCommon.getArchitecture(pOriginalCfa.getMachineModel());
    String creationTime = AutomatonGraphmlCommon.getCreationTime();

    // update entries
    updateKeyDefault(pDocument, "originfile", filePath.toString());
    updateDataValue(pDocument, "producer", producer);
    updateDataValue(pDocument, "specification", specification);
    updateDataValue(pDocument, "programfile", filePath.toString());
    updateDataValue(pDocument, "programhash", programHash);
    updateDataValue(pDocument, "architecture", architecture);
    updateDataValue(pDocument, "creationtime", creationTime);

    return pDocument;
  }

  private static void updateDataValue(Document pDocument, String pKeyName, String pNewValue) {
    NodeList list = pDocument.getElementsByTagName("data");
    for (int i = 0; i < list.getLength(); i++) {
      Element data = (Element) list.item(i);
      if (pKeyName.equals(data.getAttribute("key"))) {
        data.setTextContent(pNewValue);
      }
    }
  }

  private static void updateKeyDefault(Document pDocument, String pKeyId, String pNewDefault) {
    NodeList keys = pDocument.getElementsByTagName("key");
    for (int i = 0; i < keys.getLength(); i++) {
      Element key = (Element) keys.item(i);
      if (pKeyId.equals(key.getAttribute("id"))) {
        NodeList defaults = key.getElementsByTagName("default");
        if (defaults.getLength() > 0) {
          defaults.item(0).setTextContent(pNewDefault);
        }
      }
    }
  }
}
