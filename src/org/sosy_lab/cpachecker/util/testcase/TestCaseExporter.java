// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.testcase;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import org.sosy_lab.common.Appender;
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
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter;
import org.sosy_lab.cpachecker.util.testcase.TestVector.TargetTestVector;

@Options(prefix = "testcase")
public class TestCaseExporter {

  private enum FormatType {
    HARNESS,
    METADATA,
    PLAIN,
    XML
  }

  private static UniqueIdGenerator id = new UniqueIdGenerator();

  @Option(secure = true, name = "file", description = "export test harness to file as code")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testHarnessFile = null;

  @Option(
      secure = true,
      name = "values",
      description = "export test values to file (line separated)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testValueFile = null;

  @Option(
      secure = true,
      name = "xml",
      description = "export test cases to xm file (Test-Comp format)")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testXMLFile = null;

  @Option(
      secure = true,
      name = "compress",
      description = "zip all exported test cases into a single file")
  private boolean zipTestCases = false;

  @Option(
      secure = true,
      name = "zip.file",
      description = "Zip file into which all test case files are bundled")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path testCaseZip = null;

  @Option(
      secure = true,
      description = "Only convert literal value and do not add suffix, e.g., for unsigned, etc.")
  private boolean plainLiteralValue = false;

  @Option(
      secure = true,
      description = "Do not output values for variables that are not initialized when declared")
  private boolean excludeInitialization = false;

  @Option(secure = true, description = "Random seed for mutation of test cases")
  private long mutationSeed = 0;

  private static int testsWritten = 0;

  private Random randomGen;

  private final CFA cfa;
  private final HarnessExporter harnessExporter;
  private final String producerString;

  private final LogManager logger;

  public TestCaseExporter(CFA pCfa, LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    Preconditions.checkState(
        !isZippedTestCaseWritingEnabled() || testCaseZip != null,
        "Need to specify testcase.zip.file if test case values are compressed.");

    logger = pLogger;
    cfa = pCfa;
    harnessExporter = new HarnessExporter(pConfig, logger, pCfa);
    producerString = CPAchecker.getVersion(pConfig);
    randomGen = new Random(mutationSeed);
  }

  private static String printLineSeparated(List<String> pValues) {
    return Joiner.on("\n").join(pValues);
  }

  public void writeTestCaseFiles(final List<String> pInputs, Optional<Property> pSpec) {
    if (areTestsEnabled()) {

      if (testHarnessFile != null) {
        // TODO writeTestCase(getTestCaseFiles(testHarnessFile, 1), targetPath, pCex,
        // FormatType.HARNESS, pSpec);
      }

      if (testValueFile != null) {
        writeTestCase(testValueFile.getPath(id.getFreshId()), pInputs, FormatType.PLAIN, pSpec);
      }

      if (testXMLFile != null) {
        Path testCaseFile = testXMLFile.getPath(id.getFreshId());
        if (isFirstTest()) {
          writeTestCase(
              testCaseFile.resolveSibling("metadata.xml"), pInputs, FormatType.METADATA, pSpec);
        }
        writeTestCase(testCaseFile, pInputs, FormatType.XML, pSpec);
      }
      increaseTestsWritten();
    }
  }

  public void writeTestCaseFiles(final CounterexampleInfo pCex, Optional<Property> pSpec) {
    writeTestCaseFilesAndMutations(pCex, pSpec, 0);
  }

  public void writeTestCaseFilesAndMutations(
      final CounterexampleInfo pCex, final Optional<Property> pSpec, final int numMutations) {
    // TODO check if this and openZipFS(), closeZipFS() are thread-safe
    if (areTestsEnabled()) {
      ARGPath targetPath = pCex.getTargetPath();
      final int numPaths = Math.max(1, numMutations + 1);

      if (testHarnessFile != null) {
        writeTestCase(
            getTestCaseFiles(testHarnessFile, 1), targetPath, pCex, FormatType.HARNESS, pSpec);
      }

      if (testValueFile != null) {
        writeTestCase(
            getTestCaseFiles(testValueFile, numPaths), targetPath, pCex, FormatType.PLAIN, pSpec);
      }

      if (testXMLFile != null) {
        List<Path> testCaseFiles = getTestCaseFiles(testXMLFile, numPaths);
        if (isFirstTest()) {
          List<Path> metadataFile = new ArrayList<>();
          metadataFile.add(testCaseFiles.get(0).resolveSibling("metadata.xml"));
          writeTestCase(metadataFile, targetPath, pCex, FormatType.METADATA, pSpec);
        }
        writeTestCase(testCaseFiles, targetPath, pCex, FormatType.XML, pSpec);
      }
      increaseTestsWritten();
    }
  }

  private List<Path> getTestCaseFiles(final PathTemplate pathGenerator, final int numPaths) {
    Preconditions.checkArgument(0 < numPaths);
    List<Path> testCaseFiles = new ArrayList<>();
    for (int i = 0; i < numPaths; i++) {
      testCaseFiles.add(pathGenerator.getPath(id.getFreshId()));
    }
    return testCaseFiles;
  }

  private static void increaseTestsWritten() {
    testsWritten++;
  }

  private static boolean isFirstTest() {
    return testsWritten == 0;
  }

  private void writeTestCase(
      final List<Path> pTestCaseFiles,
      final ARGPath pTargetPath,
      final CounterexampleInfo pCexInfo,
      final FormatType type,
      final Optional<Property> pSpec) {
    final ARGState rootState = pTargetPath.getFirstState();
    final Predicate<? super ARGState> relevantStates = Predicates.in(pTargetPath.getStateSet());
    final BiPredicate<ARGState, ARGState> relevantEdges =
        BiPredicates.pairIn(ImmutableSet.copyOf(pTargetPath.getStatePairs()));

    if (type == FormatType.PLAIN || type == FormatType.XML) {
      Optional<List<String>> origInputs =
          getInputNondetValuesOrdered(rootState, relevantStates, relevantEdges, pCexInfo);
      if (origInputs.isPresent()) {
        writeTestCase(pTestCaseFiles, origInputs.orElseThrow(), type, pSpec);
      }
    } else if (type == FormatType.METADATA) {
      writeTestCase(pTestCaseFiles, new ArrayList<>(), type, pSpec);
    } else {
      try {
        Preconditions.checkNotNull(pTestCaseFiles);
        Preconditions.checkArgument(!pTestCaseFiles.isEmpty());
        if (zipTestCases) {
          try (FileSystem zipFS = openZipFS()) {
            Path fileName = pTestCaseFiles.get(0).getFileName();
            Path testFile =
                zipFS.getPath(
                    fileName != null ? fileName.toString() : id.getFreshId() + "test.txt");
            try (Writer writer =
                new OutputStreamWriter(
                    zipFS
                        .provider()
                        .newOutputStream(
                            testFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE),
                    Charset.defaultCharset())) {

              switch (type) {
                case HARNESS:
                  harnessExporter.writeHarness(
                      writer, rootState, relevantStates, relevantEdges, pCexInfo);
                  break;
                default:
                  throw new AssertionError("Unknown test case format.");
              }
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
            default:
              throw new AssertionError("Unknown test case format.");
          }
          IO.writeFile(pTestCaseFiles.get(0), Charset.defaultCharset(), content);
        }
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write test case to file");
      }
    }
  }

  private void writeTestCase(
      final Path testCase,
      final List<String> pInputs,
      final FormatType pType,
      final Optional<Property> pSpec) {
    if (isFirstTest() && pType.equals(FormatType.XML)) {
      List<Path> metadataFile = new ArrayList<>();
      metadataFile.add(testCase.resolveSibling("metadata.xml"));
      writeTestCase(metadataFile, new ArrayList<>(), FormatType.METADATA, pSpec);
    }
    List<Path> testCaseList = new ArrayList<>();
    testCaseList.add(testCase);
    writeTestCase(testCaseList, pInputs, pType, pSpec);
    increaseTestsWritten();
  }

  private void writeTestCase(
      final List<Path> pTestCaseFiles,
      List<String> pOrigInputs,
      final FormatType pType,
      final Optional<Property> pSpec) {
    Preconditions.checkArgument(
        pType.equals(FormatType.PLAIN)
            || pType.equals(FormatType.XML)
            || pType.equals(FormatType.METADATA));
    try {
      List<String> nextInputs = pOrigInputs;

      if (zipTestCases) {
        try (FileSystem zipFS = openZipFS()) {
          for (Path pFile : pTestCaseFiles) {
            Path fileName = pFile.getFileName();
            Path testFile =
                zipFS.getPath(
                    fileName != null ? fileName.toString() : id.getFreshId() + "test.txt");
            try (Writer writer =
                new OutputStreamWriter(
                    zipFS
                        .provider()
                        .newOutputStream(
                            testFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE),
                    Charset.defaultCharset())) {

              switch (pType) {
                case PLAIN:
                  writer.write(
                      inputListToFormattedString(nextInputs, TestCaseExporter::printLineSeparated));
                  break;
                case METADATA:
                  XMLTestCaseExport.writeXMLMetadata(
                      writer, cfa, pSpec.orElse(null), producerString);
                  break;
                case XML:
                  writer.write(
                      inputListToFormattedString(nextInputs, XMLTestCaseExport.XML_TEST_CASE));
                  break;
                default:
                  throw new AssertionError("Unknown test case format.");
              }
            }
            nextInputs = mutateInputValues(pOrigInputs);
          }
        }
      } else {
        for (Path pFile : pTestCaseFiles) {
          Object content = null;

          switch (pType) {
            case PLAIN:
              String plainOutput =
                  inputListToFormattedString(nextInputs, TestCaseExporter::printLineSeparated);
              content = (Appender) appendable -> appendable.append(plainOutput);
              break;
            case METADATA:
              content =
                  (Appender)
                      appendable ->
                          XMLTestCaseExport.writeXMLMetadata(
                              appendable, cfa, pSpec.orElse(null), producerString);
              break;
            case XML:
              String xmlOutput =
                  inputListToFormattedString(nextInputs, XMLTestCaseExport.XML_TEST_CASE);
              content = (Appender) appendable -> appendable.append(xmlOutput);
              break;
            default:
              throw new AssertionError("Unknown test case format.");
          }
          IO.writeFile(pFile, Charset.defaultCharset(), content);
          nextInputs = mutateInputValues(pOrigInputs);
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

  private FileSystem openZipFS() throws IOException {
    Map<String, String> env = new HashMap<>(1);
    env.put("create", "true");

    Preconditions.checkNotNull(testCaseZip);
    // create parent directories if do not exist
    Path parent = testCaseZip.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    return FileSystems.newFileSystem(URI.create("jar:" + testCaseZip.toUri()), env, null);
  }

  private String unpack(final AAstNode pInputValue) {
    if (pInputValue instanceof CCastExpression) {
      return unpack(((CCastExpression) pInputValue).getOperand());
    } else {
      if (plainLiteralValue && pInputValue instanceof ALiteralExpression) {
        return String.valueOf(((ALiteralExpression) pInputValue).getValue());
      }
      return pInputValue.toASTString();
    }
  }

  private Optional<List<String>> getInputNondetValuesOrdered(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      final CounterexampleInfo pCounterexampleInfo) {

    final Multimap<ARGState, CFAEdgeWithAssumptions> valueMap = getValueMap(pCounterexampleInfo);
    Optional<TargetTestVector> maybeTestVector =
        harnessExporter.extractTestVector(pRootState, pIsRelevantState, pIsRelevantEdge, valueMap);

    if (maybeTestVector.isPresent()) {
      final TestVector vector = maybeTestVector.orElseThrow().getVector();

      List<String> inputs =
          vector.getTestInputsInOrder().stream()
              .filter(v -> !excludeInitialization || (v instanceof ExpressionTestValue))
              .map(v -> unpack(v.getValue()))
              .collect(ImmutableList.toImmutableList());
      return Optional.of(inputs);
    }
    return Optional.empty();
  }

  private List<String> mutateInputValues(final List<String> origInputs) {
    List<String> newInput = new ArrayList<>(origInputs);
    double prob, origVal;
    int val;
    for (int i = 0; i < newInput.size(); i++) {
      try {
        origVal = Double.parseDouble(newInput.get(i));
        prob = randomGen.nextDouble();
        if (prob < 0.02) {
          val = Integer.MIN_VALUE; // MIN
        } else if (prob < 0.04) {
          val = Integer.MAX_VALUE; // MAX
        } else if (prob < 0.19) {
          val = 0; // 0
        } else if (prob < 0.34) {
          val = (int) -origVal; // negate
        } else if (prob < 0.49) { // random
          val = randomGen.nextInt();
        } else {
          continue;
        }
        newInput.set(i, String.valueOf(val));
      } catch (NumberFormatException e) {
        continue;
      }
    }
    return newInput;
  }

  private String inputListToFormattedString(
      final List<String> inputs, final TestValuesToFormat formatter) {
    return formatter.convertToOutput(inputs);
  }

  private Multimap<ARGState, CFAEdgeWithAssumptions> getValueMap(
      CounterexampleInfo pCounterexampleInfo) {
    if (pCounterexampleInfo.isPreciseCounterExample()) {
      return pCounterexampleInfo.getExactVariableValues();
    } else {
      return HashMultimap.create();
    }
  }
}
