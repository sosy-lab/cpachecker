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
 */
package org.sosy_lab.cpachecker.util.testcase;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.SpecificationProperty;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter;
import org.sosy_lab.cpachecker.util.testcase.TestVector.TargetTestVector;

@Options(prefix = "testcase")
public class TestCaseExporter {

  private static final TestValuesToFormat LINE_SEPARATED =
      valueList -> {
        StringBuilder strB = new StringBuilder();
        Joiner.on("\n").appendTo(strB, valueList);
        return strB.toString();
      };

  private static UniqueIdGenerator id = new UniqueIdGenerator();

  private static int testsWritten = 0;

  private enum FormatType {
    HARNESS,
    METADATA,
    PLAIN,
    XML;
  }

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
      description = "export test cases to xml file (Test-Comp format)")
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
  }

  private boolean areTestsEnabled() {
    return testValueFile != null || testHarnessFile != null || testXMLFile != null;
  }

  public void writeTestCaseFiles(
      CounterexampleInfo pCex, Optional<SpecificationProperty> pSpecProp) {

    // TODO check if this and openZipFS(), closeZipFS() are thread-safe
    if (areTestsEnabled()) {
        ARGPath targetPath = pCex.getTargetPath();

        if (testHarnessFile != null) {
          writeTestCase(
              testHarnessFile.getPath(id.getFreshId()),
              targetPath,
              pCex,
              FormatType.HARNESS,
              pSpecProp);
        }

      if (testValueFile != null) {
        writeTestCase(
            testValueFile.getPath(id.getFreshId()),
            targetPath,
            pCex,
            FormatType.PLAIN,
            pSpecProp);
      }

      if (testXMLFile != null) {
        Path testCaseFile = testXMLFile.getPath(id.getFreshId());
        if (isFirstTest()) {
          writeTestCase(
              testCaseFile.resolveSibling("metadata.xml"),
              targetPath,
              pCex,
              FormatType.METADATA,
              pSpecProp);
        }
        writeTestCase(testCaseFile, targetPath, pCex, FormatType.XML, pSpecProp);
      }
      increaseTestCount();
    }
  }

  private static void increaseTestCount() {
    testsWritten++;
  }

  private static boolean isFirstTest() {
    return testsWritten == 0;
  }

  private void writeTestCase(
      final Path pFile,
      final ARGPath pTargetPath,
      final CounterexampleInfo pCexInfo,
      final FormatType type,
      final Optional<SpecificationProperty> pSpecProp) {
    final ARGState rootState = pTargetPath.getFirstState();
    final Predicate<? super ARGState> relevantStates = Predicates.in(pTargetPath.getStateSet());
    final Predicate<? super Pair<ARGState, ARGState>> relevantEdges =
        Predicates.in(pTargetPath.getStatePairs());
    try {
      Optional<String> testOutput;

      if (zipTestCases) {
        try (FileSystem zipFS = openZipFS()) {
          Path fileName = pFile.getFileName();
          Path testFile =
              zipFS.getPath(fileName != null ? fileName.toString() : id.getFreshId() + "test.txt");
          try (Writer writer =
                   new OutputStreamWriter(
                       zipFS
                           .provider()
                           .newOutputStream(testFile, StandardOpenOption.CREATE,
                               StandardOpenOption.WRITE),
                       Charset.defaultCharset())) {
            switch (type) {
              case HARNESS:
                harnessExporter.writeHarness(
                    writer, rootState, relevantStates, relevantEdges, pCexInfo);
                break;
              case METADATA:
                XMLTestCaseExport.writeXMLMetadata(
                    writer, cfa, pSpecProp.orElse(null), producerString);
                break;
              case PLAIN:
                testOutput =
                    writeTestInputNondetValues(
                        rootState,
                        relevantStates,
                        relevantEdges,
                        pCexInfo,
                        TestCaseExporter.LINE_SEPARATED);
                if (testOutput.isPresent()) {
                  writer.write(testOutput.get());
                }
                break;
              case XML:
                testOutput =
                    writeTestInputNondetValues(
                        rootState,
                        relevantStates,
                        relevantEdges,
                        pCexInfo,
                        XMLTestCaseExport.XML_TEST_CASE);
                if (testOutput.isPresent()) {
                  writer.write(testOutput.get());
                }
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
          case METADATA:
            content =
                (Appender)
                    appendable ->
                        XMLTestCaseExport.writeXMLMetadata(
                            appendable, cfa, pSpecProp.orElse(null), producerString);
            break;
          case PLAIN:
            testOutput =
                writeTestInputNondetValues(
                    rootState,
                    relevantStates,
                    relevantEdges,
                    pCexInfo,
                    TestCaseExporter.LINE_SEPARATED);

            if (testOutput.isPresent()) {
              content = (Appender) appendable -> appendable.append(testOutput.get());
            }
            break;
          case XML:
            testOutput =
                writeTestInputNondetValues(
                    rootState,
                    relevantStates,
                    relevantEdges,
                    pCexInfo,
                    XMLTestCaseExport.XML_TEST_CASE);
            if (testOutput.isPresent()) {
              content = (Appender) appendable -> appendable.append(testOutput.get());
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

  private String unpack(final AAstNode pInputValue) {
    if (pInputValue instanceof CCastExpression) {
      return unpack(((CCastExpression) pInputValue).getOperand());
    } else {
      return pInputValue.toASTString();
    }
  }

  private Optional<String> writeTestInputNondetValues(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
      final CounterexampleInfo pCounterexampleInfo,
      final TestValuesToFormat formatter) {

    final Multimap<ARGState, CFAEdgeWithAssumptions> valueMap = getValueMap(pCounterexampleInfo);

    final Optional<TargetTestVector> maybeTestVector =
        harnessExporter.extractTestVector(pRootState, pIsRelevantState, pIsRelevantEdge, valueMap);

    if (maybeTestVector.isPresent()) {
      final TestVector vector = maybeTestVector.get().getVector();

      List<String> inputs =
          vector.getTestInputsInOrder().stream()
              .map(v -> unpack(v.getValue()))
              .collect(Collectors.toList());

      return Optional.of(formatter.convertToOutput(inputs));
    } else {
      return Optional.empty();
    }
  }

  private Multimap<ARGState, CFAEdgeWithAssumptions> getValueMap(CounterexampleInfo pCounterexampleInfo) {
    if (pCounterexampleInfo.isPreciseCounterExample()) {
      return pCounterexampleInfo.getExactVariableValues();
    } else {
      return HashMultimap.create();
    }
  }

  private boolean isZippedTestCaseWritingEnabled() {
    return zipTestCases && areTestsEnabled();
  }

  private FileSystem openZipFS() throws IOException {
    Map<String, String> env = new HashMap<>(1);
    env.put("create", "true");

    checkNotNull(testCaseZip);
    // create parent directories if do not exist
    Path parent = testCaseZip.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    return FileSystems.newFileSystem(URI.create("jar:" + testCaseZip.toUri().toString()), env, null);
  }
}
