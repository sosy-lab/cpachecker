// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JConstructorDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodEntryNode;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class CFACreatorTest {

  // Must be static final because of https://gitlab.com/sosy-lab/software/cpachecker/-/issues/263
  private static final JClassType TEST_CLASS =
      JClassType.valueOf(
          "pack5.CallTests_true_assert",
          "CallTests_true_assert",
          VisibilityModifier.PUBLIC,
          false,
          false,
          false,
          JClassType.createObjectType(),
          ImmutableSet.of());

  private JMethodEntryNode N1;
  private JMethodEntryNode N2;
  private JMethodEntryNode N3;
  private JMethodEntryNode N4;
  private JMethodEntryNode N5;

  private Map<String, FunctionEntryNode> cfa;

  @Before
  public void init() {
    JMethodDeclaration functionDefinition1 = createFunctionDefinition("main", "String[]");
    N1 = new JMethodEntryNode(FileLocation.DUMMY, functionDefinition1, null, Optional.empty());

    JMethodDeclaration functionDefinition2 = createFunctionDefinition("main2", "String[]");
    N2 = new JMethodEntryNode(FileLocation.DUMMY, functionDefinition2, null, Optional.empty());

    JMethodDeclaration functionDefinition3 = createFunctionDefinition("callTests_true_assert", "");
    N3 = new JMethodEntryNode(FileLocation.DUMMY, functionDefinition3, null, Optional.empty());

    JMethodDeclaration functionDefinition4 =
        createFunctionDefinition("callTests_true_assert", "int");
    N4 = new JMethodEntryNode(FileLocation.DUMMY, functionDefinition4, null, Optional.empty());

    JMethodDeclaration functionDefinition5 =
        createFunctionDefinition("callTests_true_assert", "int_int");
    N5 = new JMethodEntryNode(FileLocation.DUMMY, functionDefinition5, null, Optional.empty());

    cfa =
        Maps.uniqueIndex(
            ImmutableList.of(N1, N2, N3, N4, N5), node -> node.getFunctionDefinition().getName());
  }

  @Test
  public void testGetJavaMainMethodSourceFileIsClasspathAndMainFunctionWithParameters()
      throws InvalidConfigurationException {
    String sourceFile = "test/programs/java/CallTests";
    String mainFunction = "pack5.CallTests_true_assert.main_String[]";
    FunctionEntryNode result =
        CFACreator.getJavaMainMethod(ImmutableList.of(sourceFile), mainFunction, cfa);

    assertThat(result).isEqualTo(N1);
  }

  @Test
  public void testGetJavaMainMethodForSameNameMethodsWithDifferentParameters()
      throws InvalidConfigurationException {
    String sourceFile = "pack5.CallTests_true_assert";
    String mainFunction = "callTests_true_assert";

    assertThat(CFACreator.getJavaMainMethod(ImmutableList.of(sourceFile), mainFunction, cfa))
        .isEqualTo(N3);

    mainFunction = "callTests_true_assert_int";
    assertThat(CFACreator.getJavaMainMethod(ImmutableList.of(sourceFile), mainFunction, cfa))
        .isEqualTo(N4);

    mainFunction = "callTests_true_assert_int_int";
    assertThat(CFACreator.getJavaMainMethod(ImmutableList.of(sourceFile), mainFunction, cfa))
        .isEqualTo(N5);
  }

  @Test
  public void testGetJavaMainMethodWithTwoSimilarNamedMethods()
      throws InvalidConfigurationException {
    String sourceFile = "pack5.CallTests_true_assert";
    String mainFunction = "main";

    FunctionEntryNode result =
        CFACreator.getJavaMainMethod(ImmutableList.of(sourceFile), mainFunction, cfa);

    assertThat(result).isEqualTo(N1);
  }

  @Test
  public void testParseSourceAndCreateCfaWithNoReturnAbort()
      throws InvalidConfigurationException, ParserException, InterruptedException {
    final Configuration config =
        TestDataTools.configurationForTest().setOption("language", "C").build();
    final CFACreator creator = createCfaCreatorForTesting(config);
    final String programSource =
        "extern void abort() __attribute__((__noreturn__));int main() { abort(); }";

    final CFA created = creator.parseSourceAndCreateCFA(programSource);

    Predicate<CFAEdge> isNoReturnFunctionCall =
        Predicates.and(
            CFACreatorTest::isTerminatingStatement,
            pCFAEdge -> CFACreatorTest.isFunctionCall(pCFAEdge, "abort"));
    FluentIterable<CFAEdge> cfaEdges = getAllEdges(created);
    assertWithMessage(
            "Expected function call to abort() with a succeeding CFATerminationNode in the CFA, but"
                + " none found.")
        .that(cfaEdges.anyMatch(isNoReturnFunctionCall))
        .isTrue();
  }

  @Test
  public void testParseSourceAndCreateCfaWithNoReturnFunctionAttribute()
      throws InvalidConfigurationException, ParserException, InterruptedException {
    final Configuration config =
        TestDataTools.configurationForTest().setOption("language", "C").build();
    final CFACreator creator = createCfaCreatorForTesting(config);
    final String programSource =
        "extern void myfunc() __attribute__((__noreturn__));int main() { myfunc(); }";

    final CFA created = creator.parseSourceAndCreateCFA(programSource);

    Predicate<CFAEdge> isNoReturnFunctionCall =
        Predicates.and(
            CFACreatorTest::isTerminatingStatement,
            pCFAEdge -> CFACreatorTest.isFunctionCall(pCFAEdge, "myfunc"));
    FluentIterable<CFAEdge> cfaEdges = getAllEdges(created);
    assertWithMessage(
            "Expected function call to myfunc() with a succeeding CFATerminationNode in the CFA,"
                + " but none found.")
        .that(cfaEdges.anyMatch(isNoReturnFunctionCall))
        .isTrue();
  }

  @Test
  public void testParseSourceAndCreateCfaWithReturningAbort()
      throws InvalidConfigurationException, ParserException, InterruptedException {
    final Configuration config =
        TestDataTools.configurationForTest()
            .setOption("language", "C")
            .setOption(
                "cfa.nonReturningFunctions", "[]") // do not handle 'abort' as aborting function
            .build();
    final CFACreator creator = createCfaCreatorForTesting(config);
    final String programSource = "extern void abort();int main() { abort(); }";

    final CFA created = creator.parseSourceAndCreateCFA(programSource);

    Predicate<CFAEdge> isNoReturnFunctionCall =
        Predicates.and(
            CFACreatorTest::isTerminatingStatement,
            pCFAEdge -> CFACreatorTest.isFunctionCall(pCFAEdge, "abort"));
    FluentIterable<CFAEdge> cfaEdges = getAllEdges(created);
    assertWithMessage(
            "Found function call to abort() with CFATerminationNode in the CFA, but CFA should"
                + " continue after function call.")
        .that(cfaEdges.anyMatch(isNoReturnFunctionCall))
        .isFalse();
  }

  @Test
  public void testParseSourceAndCreateCfaWithReturningAbortButExplicitTermination()
      throws InvalidConfigurationException, ParserException, InterruptedException {
    final Configuration config =
        TestDataTools.configurationForTest()
            .setOption("language", "C")
            .setOption("cfa.nonReturningFunctions", "abort") // handle 'abort' as aborting function
            .build();
    final CFACreator creator = createCfaCreatorForTesting(config);
    final String programSource = "extern void abort();int main() { abort(); }";

    final CFA created = creator.parseSourceAndCreateCFA(programSource);

    Predicate<CFAEdge> isNoReturnFunctionCall =
        Predicates.and(
            CFACreatorTest::isTerminatingStatement,
            pCFAEdge -> CFACreatorTest.isFunctionCall(pCFAEdge, "abort"));
    FluentIterable<CFAEdge> cfaEdges = getAllEdges(created);
    assertWithMessage(
            "Expected function call to abort() with a succeeding CFATerminationNode in the CFA, but"
                + " none found.")
        .that(cfaEdges.anyMatch(isNoReturnFunctionCall))
        .isTrue();
  }

  @Test
  public void testFileLocationsInCfa() throws IOException, InterruptedException, ParserException {
    Path program_path = Path.of("test/programs/cfa-creation/cfa-creation-test.c");
    CFA createdCFA =
        TestDataTools.makeCFA(
            IOUtils.toString(
                MoreFiles.asByteSource(program_path).openStream(), StandardCharsets.UTF_8));

    Path testFilepath = Path.of("./test");
    assertThat(TestDataTools.getEdge("x = 0", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 252, 10, 10, 10, 3, 13));
    assertThat(TestDataTools.getEdge("y = 0", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 265, 10, 11, 11, 3, 13));
    assertThat(TestDataTools.getEdge("[x == y]", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 282, 6, 12, 12, 7, 13));
    assertThat(TestDataTools.getEdge("!(x == y)", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 282, 6, 12, 12, 7, 13));
    assertThat(TestDataTools.getEdge("[x == 0]", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 292, 6, 12, 12, 17, 23));
    assertThat(TestDataTools.getEdge("!(x == 0)", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 292, 6, 12, 12, 17, 23));
    assertThat(TestDataTools.getEdge("[y == 0]", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 308, 6, 13, 13, 7, 13));
    assertThat(TestDataTools.getEdge("!(y == 0)", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 308, 6, 13, 13, 7, 13));
    assertThat(TestDataTools.getEdge("[t1 == t2]", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 384, 8, 21, 21, 10, 18));
    assertThat(TestDataTools.getEdge("!(t1 == t2)", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 384, 8, 21, 21, 10, 18));
    assertThat(TestDataTools.getEdge("[t1 == t3]", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 405, 8, 22, 22, 10, 18));
    assertThat(TestDataTools.getEdge("!(t1 == t3)", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 405, 8, 22, 22, 10, 18));
    assertThat(TestDataTools.getEdge("[t2 == t3]", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 426, 17, 23, 24, 10, 12));
    assertThat(TestDataTools.getEdge("!(t2 == t3)", createdCFA).getFileLocation())
        .isEqualTo(new FileLocation(testFilepath, 426, 17, 23, 24, 10, 12));
  }

  private CFACreator createCfaCreatorForTesting(Configuration config)
      throws InvalidConfigurationException {
    final LogManager logger = LogManager.createTestLogManager();
    final ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
    return new CFACreator(config, logger, shutdownNotifier);
  }

  /**
   * Returns whether the given CFA edge is a terminating statement. A terminating statement is a
   * {@link CFAEdgeType#StatementEdge} whose successor node is a {@link
   * org.sosy_lab.cpachecker.cfa.model.CFATerminationNode}.
   */
  private static boolean isTerminatingStatement(CFAEdge pCfaEdge) {
    return pCfaEdge.getEdgeType().equals(CFAEdgeType.StatementEdge)
        && pCfaEdge.getSuccessor() instanceof CFATerminationNode;
  }

  /**
   * Returns whether the given CFA edge is a function call to the given function name. Only returns
   * true if the function call is a direct call to the function. Function-pointers are considered
   * 'false'.
   */
  private static boolean isFunctionCall(CFAEdge pCfaEdge, String pExpectedFunctionName) {
    if (!(pCfaEdge instanceof AStatementEdge aStatementEdge)) {
      return false;
    }
    AStatement statement = aStatementEdge.getStatement();
    if (!(statement instanceof AFunctionCall aFunctionCall)) {
      return false;
    }
    AExpression callee = aFunctionCall.getFunctionCallExpression().getFunctionNameExpression();
    if (!(callee instanceof AIdExpression aIdExpression)) {
      return false;
    }
    String functionName = aIdExpression.getName();
    return functionName.equals(pExpectedFunctionName);
  }

  private FluentIterable<CFAEdge> getAllEdges(CFA pCfa) {
    final CFATraversal.EdgeCollectingCFAVisitor edgeCollector =
        new CFATraversal.EdgeCollectingCFAVisitor();
    CFATraversal.dfs().traverse(pCfa.getMainFunction(), edgeCollector);
    return FluentIterable.from(edgeCollector.getVisitedEdges());
  }

  private JMethodDeclaration createFunctionDefinition(
      String methodName, String parametersSubString) {
    StringBuilder name = new StringBuilder(TEST_CLASS.getName()).append("_").append(methodName);
    List<String> parameters = Splitter.on('_').splitToList(parametersSubString);
    List<JParameterDeclaration> jParameterDeclarations = new ArrayList<>(parameters.size());
    for (String parameter : parameters) {
      jParameterDeclarations.add(
          new JParameterDeclaration(FileLocation.DUMMY, JSimpleType.INT, parameter, "stub", false));
    }
    if (!parametersSubString.isEmpty()) {
      name.append("_").append(parametersSubString);
    }

    return new JConstructorDeclaration(
        FileLocation.DUMMY,
        null,
        name.toString(),
        methodName,
        jParameterDeclarations,
        VisibilityModifier.PUBLIC,
        false,
        TEST_CLASS);
  }
}
