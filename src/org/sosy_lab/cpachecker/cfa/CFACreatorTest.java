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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class CFACreatorTest {

  @Mock JMethodEntryNode N1;
  @Mock JMethodEntryNode N2;
  @Mock JMethodEntryNode N3;
  @Mock JMethodEntryNode N4;
  @Mock JMethodEntryNode N5;

  private Map<String, FunctionEntryNode> cfa;

  @Before
  public void init() {

    MockitoAnnotations.openMocks(this);
    JMethodDeclaration functionDefinition1 =
        createFunctionDefinition("pack5.CallTests_true_assert", "main", "String[]");
    when(N1.getFunctionDefinition()).thenReturn(functionDefinition1);
    JMethodDeclaration functionDefinition2 =
        createFunctionDefinition("pack5.CallTests_true_assert", "main2", "String[]");
    when(N2.getFunctionDefinition()).thenReturn(functionDefinition2);
    JMethodDeclaration functionDefinition3 =
        createFunctionDefinition("pack5.CallTests_true_assert", "callTests_true_assert", "");
    when(N3.getFunctionDefinition()).thenReturn(functionDefinition3);
    JMethodDeclaration functionDefinition4 =
        createFunctionDefinition("pack5.CallTests_true_assert", "callTests_true_assert", "int");
    when(N4.getFunctionDefinition()).thenReturn(functionDefinition4);
    JMethodDeclaration functionDefinition5 =
        createFunctionDefinition("pack5.CallTests_true_assert", "callTests_true_assert", "int_int");
    when(N5.getFunctionDefinition()).thenReturn(functionDefinition5);
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
        "extern void abort() __attribute__((__noreturn__));" + "int main() { abort(); }";

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
        "extern void myfunc() __attribute__((__noreturn__));" + "int main() { myfunc(); }";

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
    final String programSource = "extern void abort();" + "int main() { abort(); }";

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
    final String programSource = "extern void abort();" + "int main() { abort(); }";

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
    if (!(pCfaEdge instanceof AStatementEdge)) {
      return false;
    }
    AStatement statement = ((AStatementEdge) pCfaEdge).getStatement();
    if (!(statement instanceof AFunctionCall)) {
      return false;
    }
    AExpression callee =
        ((AFunctionCall) statement).getFunctionCallExpression().getFunctionNameExpression();
    if (!(callee instanceof AIdExpression)) {
      return false;
    }
    String functionName = ((AIdExpression) callee).getName();
    return functionName.equals(pExpectedFunctionName);
  }

  private FluentIterable<CFAEdge> getAllEdges(CFA pCfa) {
    final CFATraversal.EdgeCollectingCFAVisitor edgeCollector =
        new CFATraversal.EdgeCollectingCFAVisitor();
    CFATraversal.dfs().traverse(pCfa.getMainFunction(), edgeCollector);
    return FluentIterable.from(edgeCollector.getVisitedEdges());
  }

  private JMethodDeclaration createFunctionDefinition(
      String classPath, String methodName, String parametersSubString) {
    String name = classPath + "_" + methodName;
    List<String> parameters = Splitter.on('_').splitToList(parametersSubString);
    List<JParameterDeclaration> jParameterDeclarations = new ArrayList<>(parameters.size());
    for (String parameter : parameters) {
      jParameterDeclarations.add(
          new JParameterDeclaration(
              FileLocation.DUMMY, mock(JType.class), parameter, "stub", false));
    }
    if (!parametersSubString.isEmpty()) {
      name = name + "_" + parametersSubString;
    }

    return new JConstructorDeclaration(
        FileLocation.DUMMY,
        null,
        name,
        methodName,
        jParameterDeclarations,
        VisibilityModifier.PUBLIC,
        false,
        createDeclaringClassMock(classPath));
  }

  private JClassType createDeclaringClassMock(String classPath) {
    String simpleClassName;
    int indexOfLastDot = classPath.lastIndexOf(".");
    if (indexOfLastDot >= 0) {
      simpleClassName = classPath.substring(indexOfLastDot);
    } else {
      simpleClassName = classPath;
    }

    JClassType declaringClass = mock(JClassType.class);
    when(declaringClass.getName()).thenReturn(classPath);
    when(declaringClass.getSimpleName()).thenReturn(simpleClassName);
    return declaringClass;
  }
}
