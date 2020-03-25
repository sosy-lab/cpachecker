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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CDefaults;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.SpecificationProperty;
import org.sosy_lab.cpachecker.util.harness.HarnessExporter;
import org.sosy_lab.cpachecker.util.harness.PredefinedTypes;

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

  private static int testsWritten = 0;

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

  private static String printLineSeparated(List<String> pValues) {
    return Joiner.on("\n").join(pValues);
  }

  public void writeTestCaseFiles(
      final CounterexampleInfo pCex,
      Optional<SpecificationProperty> pSpec) throws IOException {
    // TODO check if this and openZipFS(), closeZipFS() are thread-safe
    if (areTestsEnabled()) {
      ARGPath targetPath = pCex.getTargetPath();

      if (testHarnessFile != null) {
        writeTestCase(
            testHarnessFile.getPath(id.getFreshId()), targetPath, pCex, FormatType.HARNESS, pSpec);
      }

      if (testValueFile != null) {
        writeTestCase(
            testValueFile.getPath(id.getFreshId()), targetPath, pCex, FormatType.PLAIN, pSpec);
      }

      if (testXMLFile != null) {
        Path testCaseFile = testXMLFile.getPath(id.getFreshId());
        if (isFirstTest()) {
          writeTestCase(
              testCaseFile.resolveSibling("metadata.xml"),
              targetPath,
              pCex,
              FormatType.METADATA,
              pSpec);
        }
        writeTestCase(testCaseFile, targetPath, pCex, FormatType.XML, pSpec);
      }
      increaseTestsWritten();
    }
  }

  private static void increaseTestsWritten() {
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
      final Optional<SpecificationProperty> pSpec) {
    final ARGState rootState = pTargetPath.getFirstState();
    final Predicate<? super ARGState> relevantStates = Predicates.in(pTargetPath.getStateSet());
    final BiPredicate<ARGState, ARGState> relevantEdges =
        BiPredicates.pairIn(ImmutableSet.copyOf(pTargetPath.getStatePairs()));
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
                XMLTestCaseExport.writeXMLMetadata(writer, cfa, pSpec.orElse(null), producerString);
                break;
              case PLAIN:
                testOutput =
                    writeTestInputNondetValues(
                        rootState,
                        relevantStates,
                        relevantEdges,
                        pCexInfo,
                        cfa,
                        TestCaseExporter::printLineSeparated);
                if (testOutput.isPresent()) {
                  writer.write(testOutput.orElseThrow());
                }
                break;
              case XML:
                testOutput =
                    writeTestInputNondetValues(
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
                            appendable, cfa, pSpec.orElse(null), producerString);
            break;
          case PLAIN:
            testOutput =
                writeTestInputNondetValues(
                    rootState,
                    relevantStates,
                    relevantEdges,
                    pCexInfo,
                    cfa,
                    TestCaseExporter::printLineSeparated);

            if (testOutput.isPresent()) {
              content = (Appender) appendable -> appendable.append(testOutput.orElseThrow());
            }
            break;
          case XML:
            testOutput =
                writeTestInputNondetValues(
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

  private FileSystem openZipFS() throws IOException {
    Map<String, String> env = new HashMap<>(1);
    env.put("create", "true");

    Preconditions.checkNotNull(testCaseZip);
    // create parent directories if do not exist
    Path parent = testCaseZip.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    return
        FileSystems.newFileSystem(URI.create("jar:" + testCaseZip.toUri().toString()), env, null);
  }

  private Optional<String> writeTestInputNondetValues(
      final ARGState pRootState,
      final Predicate<? super ARGState> pIsRelevantState,
      final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      final CounterexampleInfo pCounterexampleInfo,
      final CFA pCfa,
      final TestValuesToFormat formatter) {

    Preconditions.checkArgument(pCounterexampleInfo.isPreciseCounterExample()
        || !areAssignmentsOnPath(pCounterexampleInfo.getTargetPath()));
    Multimap<ARGState, CFAEdgeWithAssumptions> valueMap =
        getValueMap(pCounterexampleInfo);

    List<String> values = new ArrayList<>();
    Set<ARGState> visited = new HashSet<>();
    Deque<ARGState> stack = new ArrayDeque<>();
    Deque<CFAEdge> lastEdgeStack = new ArrayDeque<>();
    stack.push(pRootState);
    visited.addAll(stack);
    Optional<String> value;
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
        return Optional.of(formatter.convertToOutput(values));
      }
      ARGState parent = previous;
      Iterable<CFANode> parentLocs = AbstractStates.extractLocations(parent);
      for (ARGState child : parent.getChildren()) {
        if (pIsRelevantState.apply(child) && pIsRelevantEdge.test(parent, child)) {
          Iterable<CFANode> childLocs = AbstractStates.extractLocations(child);
          for (CFANode parentLoc : parentLocs) {
            for (CFANode childLoc : childLocs) {
              if (parentLoc.hasEdgeTo(childLoc)) {
                CFAEdge edge = parentLoc.getEdgeTo(childLoc);

                // add the required values for external non-void functions
                if (edge instanceof AStatementEdge) {
                  AStatementEdge statementEdge = (AStatementEdge) edge;
                  if (statementEdge.getStatement() instanceof AFunctionCall) {
                    value =
                        getReturnValueForExternalFunction(
                            (AFunctionCall) statementEdge.getStatement(),
                            edge,
                            valueMap.get(previous),
                            pCfa);
                    if (value.isPresent()) {
                      values.add(value.orElseThrow());
                    }
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
    return Optional.empty();
  }

  private Multimap<ARGState, CFAEdgeWithAssumptions> getValueMap(CounterexampleInfo pCounterexampleInfo) {
    if (pCounterexampleInfo.isPreciseCounterExample()) {
      return pCounterexampleInfo.getExactVariableValues();
    } else {
      return HashMultimap.create();
    }
  }

  private static boolean areAssignmentsOnPath(ARGPath pPath) {
    for (CFAEdge e : pPath.getInnerEdges()) {
      switch (e.getEdgeType()) {
        case BlankEdge:
        case CallToReturnEdge:
        case AssumeEdge:
        case ReturnStatementEdge:
        case FunctionCallEdge:
          // no assignments
          break;
        case DeclarationEdge:
          ADeclaration decl = ((ADeclarationEdge) e).getDeclaration();
          if (decl instanceof AVariableDeclaration) {
            if (((AVariableDeclaration) decl).getInitializer() != null) {
              return true;
            }
          }
          break;
        case StatementEdge:
          AStatement stmt = ((AStatementEdge) e).getStatement();
          if (stmt instanceof AAssignment) {
            return true;
          }
          break;
        case FunctionReturnEdge:
          AFunctionCall call = ((FunctionReturnEdge) e).getSummaryEdge().getExpression();
          if (call instanceof AAssignment) {
            return true;
          }
          break;
        default:
          throw new AssertionError("Unhandled edge type: " + e.getEdgeType());
      }
    }
    return false;
  }

  private Optional<String> getReturnValueForExternalFunction(
      final AFunctionCall functionCall,
      final CFAEdge edge,
      final @Nullable Collection<CFAEdgeWithAssumptions> pAssumptions,
      final CFA pCfa) {
    AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
    AFunctionDeclaration functionDeclaration = functionCallExpression.getDeclaration();

    if (!PredefinedTypes.isKnownTestFunction(functionDeclaration)
        && !(functionCallExpression.getExpressionType() instanceof CVoidType)
        && (functionCallExpression.getExpressionType() != JSimpleType.getVoid())) {
      // only write if not predefined like e.g. malloc and is non-void

      AExpression nameExpression = functionCallExpression.getFunctionNameExpression();
      if (nameExpression instanceof AIdExpression) {

        ASimpleDeclaration declaration = ((AIdExpression) nameExpression).getDeclaration();
        if (declaration != null && pCfa.getFunctionHead(declaration.getQualifiedName()) == null) {
          // external function with return value

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
                    return Optional.of(
                        String.valueOf(((ALiteralExpression) binExp.getOperand2()).getValue()));
                  }
                  if (binExp.getOperand1() instanceof ALiteralExpression
                      && binExp.getOperand2().equals(assignment.getLeftHandSide())) {
                    return Optional.of(
                        String.valueOf(((ALiteralExpression) binExp.getOperand1()).getValue()));
                  }
                }
              }
            }

            // could not find any value
            // or value is irrelevant (case of function call statement)
            // use default value
            Type returnType = functionDeclaration.getType().getReturnType();
            if (returnType instanceof CType) {
              returnType = ((CType) returnType).getCanonicalType();

              if (returnType instanceof CSimpleType
                  && ((CSimpleType) returnType).getType() == CBasicType.CHAR) {
                return Optional.of(" ");
              }

              if (!(returnType instanceof CCompositeType
                  || returnType instanceof CArrayType
                  || returnType instanceof CBitFieldType
                  || (returnType instanceof CElaboratedType
                      && ((CElaboratedType) returnType).getKind() != ComplexTypeKind.ENUM))) {

                return Optional.of(
                    String.valueOf(
                        ((ALiteralExpression)
                                ((CInitializerExpression)
                                        CDefaults.forType((CType) returnType, FileLocation.DUMMY))
                                    .getExpression())
                            .getValue()
                            .toString()));
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
    return Optional.empty();
  }

}
