// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslMetadataException.AcslNodeMappingException;
import org.sosy_lab.cpachecker.cfa.ast.acsl.parser.AcslParser.AntlrToInternalNotImplementedException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestUtils;

@RunWith(Parameterized.class)
public class AcslMetadataParsingTest {
  private static final String TEST_DIR = "test/programs/acsl/";
  private final String programName;
  private final int expectedNumOfAnnotations;
  private final int expectedNumOfDeclarations;
  private final CFACreator cfaCreator;
  private final ImmutableList<CfaNodeAttribute> nodeAttributes;

  public AcslMetadataParsingTest(
      String pProgramName,
      int pExpectedNumOfAnnotations,
      int pExpectedNumOfDeclarations,
      ImmutableList<CfaNodeAttribute> pNodeAttributes)
      throws InvalidConfigurationException {
    programName = pProgramName;
    expectedNumOfAnnotations = pExpectedNumOfAnnotations;
    expectedNumOfDeclarations = pExpectedNumOfDeclarations;
    nodeAttributes = pNodeAttributes;
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromResource(AcslMetadataParsingTest.class, "withAcslMetadata.properties")
            .build();
    LogManager logManager = LogManager.createTestLogManager();
    cfaCreator = new CFACreator(config, logManager, ShutdownNotifier.createDummy());
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> b = ImmutableList.builder();
    b.add(
        task(
            "square_root_with_predicate.c",
            2,
            1,
            ImmutableList.of(new FunctionContractAttribute("sqroot", 3, 1, 0))));
    b.add(
        task(
            "square_with_predicate.c",
            3,
            1,
            ImmutableList.of(new FunctionContractAttribute("square", 2, 0, 0))));
    b.add(
        task(
            "square_with_logic_function.c",
            3,
            1,
            ImmutableList.of(new FunctionContractAttribute("square", 2, 0, 0))));
    b.add(
        task(
            "maxArray.c",
            4,
            1,
            ImmutableList.of(
                new FunctionContractAttribute("max", 2, 0, 1),
                new FunctionContractAttribute("maxArray", 1, 1, 1),
                new LoopAnnotationAttribute("maxArray", 2, 2, 2, "int i = 0;"),
                new AssertionAttribute(
                    "main",
                    "int m = maxArray(a, 50);",
                    "int j = -1;",
                    "assert m == max_Array(a,50);"))));
    // Assertions
    b.add(
        task(
            "after_else.c",
            1,
            0,
            ImmutableList.of(
                new AssertionAttribute("main", "a = 20;", "", "assert a == 10 || a == 20;"))));

    b.add(
        task(
            "after_for_loop2.c",
            1,
            0,
            ImmutableList.of(new AssertionAttribute("main", "b++;", "", "assert b == 20;"))));
    b.add(
        task(
            "after_if.c",
            1,
            0,
            ImmutableList.of(new AssertionAttribute("main", "a = 10;", "", "assert a != 20;"))));
    b.add(
        task(
            "after_loop.c",
            1,
            0,
            ImmutableList.of(new AssertionAttribute("main", "a++;", "", "assert a == 20;"))));
    b.add(
        task(
            "after_loop2.c",
            1,
            0,
            ImmutableList.of(new AssertionAttribute("main", "a++;", "", "assert a == 20;"))));
    b.add(
        task(
            "at_end.c",
            1,
            0,
            ImmutableList.of(new AssertionAttribute("main", "a = 10;", "", "assert a != 20;"))));
    b.add(task("badVariable.c", 0, 0, ImmutableList.of(new AssertionAttribute("", "", "", ""))));
    b.add(
        task(
            "end_of_do_while.c",
            1,
            0,
            ImmutableList.of(new AssertionAttribute("main", "a++;", "", "assert a <= 20;"))));
    b.add(
        task(
            "in_middle.c",
            1,
            0,
            ImmutableList.of(new AssertionAttribute("main", "a = 19;", "a++;", "assert a == 19"))));
    b.add(
        task(
            "same_annotation_twice.c",
            2,
            0,
            ImmutableList.of(
                new AssertionAttribute("main", "int x = 10;", "int z = x * x;", "assert x == 10;"),
                new AssertionAttribute("main", "int z = x * x;", "[x != 10]", "assert x == 10;"))));

    // Loop Annotations
    b.add(
        task(
            "double_loop_invariant.c",
            1,
            0,
            ImmutableList.of(new LoopAnnotationAttribute("main", 2, 2, 2, "int i = 0;"))));
    b.add(
        task(
            "even_while.c",
            1,
            0,
            ImmutableList.of(new LoopAnnotationAttribute("main", 2, 2, 1, "while"))));
    b.add(
        task(
            "even_while_nondet.c",
            1,
            0,
            ImmutableList.of(new LoopAnnotationAttribute("main", 2, 2, 1, "while"))));
    b.add(
        task(
            "even_do_while.c",
            1,
            0,
            ImmutableList.of(new LoopAnnotationAttribute("main", 2, 1, 1, "do"))));
    b.add(
        task(
            "inv_for.c",
            1,
            0,
            ImmutableList.of(new LoopAnnotationAttribute("main", 2, 2, 1, "int i = 0;"))));
    b.add(
        task(
            "inv_short-for.c",
            1,
            0,
            ImmutableList.of(new LoopAnnotationAttribute("main", 2, 2, 1, "y = y + 1;"))));

    // Function Contracts
    b.add(task("square_root.c", 1, 0, ImmutableList.of()));
    b.add(task("square.c", 0, 0, ImmutableList.of()));
    b.add(
        task(
            "square_result.c",
            1,
            0,
            ImmutableList.of(new FunctionContractAttribute("square", 1, 0, 0))));
    b.add(task("power.c", 1, 0, ImmutableList.of(new FunctionContractAttribute("power", 1, 2, 0))));
    b.add(
        task(
            "power_result.c",
            1,
            0,
            ImmutableList.of(new FunctionContractAttribute("power", 1, 2, 0))));
    b.add(
        task(
            "function_contract_empty_lines_before_func.c",
            1,
            0,
            ImmutableList.of(new FunctionContractAttribute("power", 1, 2, 0))));
    b.add(
        task(
            "function_contract_directly_before_func.c",
            1,
            0,
            ImmutableList.of(new FunctionContractAttribute("power", 1, 2, 0))));
    b.add(
        task(
            "multiple_function_contracts.c",
            2,
            0,
            ImmutableList.of(
                new FunctionContractAttribute("multiply", 0, 1, 0),
                new FunctionContractAttribute("multiply", 1, 0, 0))));
    b.add(
        task(
            "seperate_declaration_definition.c",
            2,
            0,
            ImmutableList.of(
                new FunctionContractAttribute("area", 1, 0, 0),
                new FunctionContractAttribute("area", 0, 2, 0))));
    b.add(task("annotation_at_wrong_location.c", 0, 0, ImmutableList.of()));
    b.add(
        task(
            "increment_pointer.c",
            1,
            0,
            ImmutableList.of(new FunctionContractAttribute("pointer_increment", 1, 1, 1))));
    b.add(task("function_contract_bad_variable.c", 0, 0, ImmutableList.of()));
    b.add(task("empty_comment.c", 0, 0, ImmutableList.of()));
    b.add(task("loop_invariant_bad_variable.c", 0, 0, ImmutableList.of()));
    b.add(
        task(
            "double_assertion.c",
            2,
            0,
            ImmutableList.of(
                new AssertionAttribute("main", "int z = x * x;", "[x != 10]", "assert x == 10;"),
                new AssertionAttribute(
                    "main", "int z = x * x;", "[x != 10]", "assert z == 100;"))));
    b.add(
        task(
            "loop_invariant_after_assertion.c",
            2,
            0,
            ImmutableList.of(
                new AssertionAttribute("main", "unsigned int x = 1;", "", "assert x == 1;"),
                new LoopAnnotationAttribute("main", 2, 2, 2, "int i = 0;"))));
    b.add(
        task(
            "inv_counter_for.c",
            1,
            0,
            ImmutableList.of(new LoopAnnotationAttribute("main", 2, 2, 2, "int i = 0;"))));
    b.add(
        task(
            "shadowed_variable.c",
            1,
            0,
            ImmutableList.of(
                new AssertionAttribute("main", "int y = p + x;", "p = y;", "assert y == i * x;"))));
    b.add(
        task(
            "nested_loops.c",
            2,
            0,
            ImmutableList.of(
                new LoopAnnotationAttribute("main", 2, 2, 2, "int i = 0;"),
                new LoopAnnotationAttribute("main", 2, 2, 3, "int j = 0;"))));
    return b.build();
  }

  private static Object[] task(
      String program,
      int expectedNumOfAnnotations,
      int expectedNumOfDeclarations,
      ImmutableList<CfaNodeAttribute> nodeAttributes) {
    return new Object[] {
      program, expectedNumOfAnnotations, expectedNumOfDeclarations, nodeAttributes,
    };
  }

  @Test
  public void parseCorrectNumberOfAcslCommentsTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());

    if (programName.equals("badVariable.c")
        || programName.contains("bad_variable")
        || programName.equals("power.c")
        || programName.equals("square.c")
        || programName.equals("shadowed_variable.c")) {
      // A variable that is not declared in the scope should always throw an exception

      // ToDo: A shadowed variable should be recognized correctly by the parser. For this, the scope
      // that is given to the parser needs to be built up correctly.

      RuntimeException expectedException =
          assertThrows(RuntimeException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(expectedException)
          .hasMessageThat()
          .contains("is not declared in neither the C program nor the ACSL scope.");
    } else if (programName.equals("even_while_nondet.c") || programName.equals("even_do_while.c")) {
      // ToDo: Fix the node mapping.
      // Currently, the heads of do-while and non-deterministic
      // while-loops don't have the isLoopHead flag set to true. As a result, they are not
      // identified as loop head nodes.
      assertThrows(AcslNodeMappingException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
    } else if (programName.equals("maxArray.c")) {
      assertThrows(
          // ToDo: Implement TypeSpecifierContext
          AntlrToInternalNotImplementedException.class,
          () -> cfaCreator.parseFileAndCreateCFA(files));
    } else if (programName.equals("annotation_at_wrong_location.c")) {
      AcslNodeMappingException expected =
          assertThrows(
              AcslNodeMappingException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(expected.getMessage())
          .isEqualTo("Acsl assertion: 'assert \\false;' at line 13 has no CFA node");
    } else if (programName.equals("empty_comment.c")) {
      AcslMetadataException exception =
          assertThrows(AcslMetadataException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(exception.getMessage()).contains("is of unknown type.");
    } else {

      CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
      AcslMetadata acslMetadata = cfa.getAcslMetadata();
      assertThat(acslMetadata).isNotNull();
      assertThat(acslMetadata.numOfAnnotaniots()).isEqualTo(expectedNumOfAnnotations);
    }
  }

  @Test
  public void parseCorrectNumberOfAcslDeclarationsTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());

    if (programName.equals("badVariable.c")
        || programName.contains("bad_variable")
        || programName.equals("power.c")
        || programName.equals("square.c")
        || programName.equals("shadowed_variable.c")) {
      // A variable that is not declared in the scope should always throw an exception

      // ToDo: A shadowed variable should be recognized correctly by the parser. For this, the scope
      // that is given to the parser needs to be built up correctly.

      RuntimeException expectedException =
          assertThrows(RuntimeException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(expectedException)
          .hasMessageThat()
          .contains("is not declared in neither the C program nor the ACSL scope.");
    } else if (programName.equals("even_while_nondet.c") || programName.equals("even_do_while.c")) {
      // ToDo: Fix the node mapping.
      // Currently, the heads of do-while and non-deterministic
      // while-loops don't have the isLoopHead flag set to true. As a result, they are not
      // identified as loop head nodes.
      assertThrows(AcslNodeMappingException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
    } else if (programName.equals("maxArray.c")) {
      assertThrows(
          // ToDo: Implement TypeSpecifierContext
          AntlrToInternalNotImplementedException.class,
          () -> cfaCreator.parseFileAndCreateCFA(files));
    } else if (programName.equals("annotation_at_wrong_location.c")) {
      AcslNodeMappingException expected =
          assertThrows(
              AcslNodeMappingException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(expected.getMessage())
          .isEqualTo("Acsl assertion: 'assert \\false;' at line 13 has no CFA node");
    } else if (programName.equals("empty_comment.c")) {
      AcslMetadataException exception =
          assertThrows(AcslMetadataException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(exception.getMessage()).contains("is of unknown type.");
    } else {
      CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
      AcslMetadata acslMetadata = cfa.getAcslMetadata();
      assertThat(acslMetadata).isNotNull();
      assertThat(acslMetadata.globalAcslDeclarations()).hasSize(expectedNumOfDeclarations);
    }
  }

  @Test
  public void acslNodeMappingTest()
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());

    if (programName.equals("badVariable.c")
        || programName.contains("bad_variable")
        || programName.equals("power.c")
        || programName.equals("square.c")
        || programName.equals("shadowed_variable.c")) {
      // A variable that is not declared in the scope should always throw an exception

      // ToDo: A shadowed variable should be recognized correctly by the parser. For this, the scope
      // that is given to the parser needs to be built up correctly.

      RuntimeException expectedException =
          assertThrows(RuntimeException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(expectedException)
          .hasMessageThat()
          .contains("is not declared in neither the C program nor the ACSL scope.");
    } else if (programName.equals("even_while_nondet.c") || programName.equals("even_do_while.c")) {
      // ToDo: Fix the node mapping.
      // Currently, the heads of do-while and non-deterministic
      // while-loops don't have the isLoopHead flag set to true. As a result, they are not
      // identified as loop head nodes.
      assertThrows(AcslNodeMappingException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
    } else if (programName.equals("maxArray.c")) {
      assertThrows(
          // ToDo: Implement TypeSpecifierContext
          AntlrToInternalNotImplementedException.class,
          () -> cfaCreator.parseFileAndCreateCFA(files));
    } else if (programName.equals("annotation_at_wrong_location.c")) {
      // The wrong type of annotation at the wrong file location should raise an exception!
      AcslNodeMappingException expected =
          assertThrows(
              AcslNodeMappingException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(expected.getMessage())
          .isEqualTo("Acsl assertion: 'assert \\false;' at line 13 has no CFA node");
    } else if (programName.equals("empty_comment.c")) {
      AcslMetadataException exception =
          assertThrows(AcslMetadataException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(exception.getMessage()).contains("is of unknown type.");
    } else {
      ImmutableCFA cfa = cfaCreator.parseFileAndCreateCFA(files);
      AcslMetadata metadata = cfa.getAcslMetadata();
      assertThat(metadata).isNotNull();

      for (CfaNodeAttribute nodeAttribute : nodeAttributes) {

        if (nodeAttribute instanceof FunctionContractAttribute pContractAttribute) {
          FunctionEntryNode expectedNode = cfa.getFunctionHead(pContractAttribute.functionName);
          assertThat(metadata.functionContracts()).containsKey(expectedNode);
          ImmutableSet<AcslFunctionContract> actualContracts =
              metadata.functionContracts().get(expectedNode);
          assertThat(hasMatchingFunctionContract(actualContracts, pContractAttribute)).isTrue();
        } else if (nodeAttribute instanceof LoopAnnotationAttribute ploopAttribute) {
          Optional<ImmutableSet<CFANode>> loopHeads = cfa.getAllLoopHeads();
          assertThat(loopHeads).isPresent();
          FluentIterable<CFANode> heads = getIterable(ploopAttribute, loopHeads);
          assertThat(heads).isNotEmpty();
          ImmutableSet<CFANode> expectedNodes = heads.toSet();
          assertThat(
                  hasMatchingLoopAnnotation(
                      expectedNodes, metadata.loopAnnotations(), ploopAttribute))
              .isTrue();
        } else if (nodeAttribute instanceof AssertionAttribute assertionAttribute) {
          FluentIterable<CFANode> nodes = FluentIterable.from(cfa.nodes());
          nodes = nodes.filter(n -> n.getFunctionName().equals(assertionAttribute.function));
          nodes =
              nodes.filter(
                  n ->
                      !n.getEnteringEdges()
                          .filter(
                              e -> e.getRawStatement().equals(assertionAttribute.enteringStatement))
                          .isEmpty());
          nodes =
              nodes.filter(
                  n ->
                      !n.getLeavingEdges()
                          .filter(
                              e -> e.getRawStatement().equals(assertionAttribute.leavingStatement))
                          .isEmpty());
          assertThat(nodes).isNotEmpty();
        }
      }
    }
  }

  private static @NonNull FluentIterable<CFANode> getIterable(
      LoopAnnotationAttribute ploopAttribute, Optional<ImmutableSet<CFANode>> loopHeads) {
    FluentIterable<CFANode> heads = FluentIterable.from(loopHeads.orElseThrow());
    heads = heads.filter(n -> n.getFunctionName().equals(ploopAttribute.function));
    heads =
        heads.filter(
            n ->
                n.getNumEnteringEdges() == ploopAttribute.enteringEdges
                    && n.getNumLeavingEdges() == ploopAttribute.leavingEdges);
    heads =
        heads.filter(
            n ->
                !n.getEnteringEdges()
                    .filter(
                        e ->
                            e.getRawStatement().equals(ploopAttribute.rawStatement)
                                || e.getDescription().equals(ploopAttribute.rawStatement))
                    .isEmpty());
    return heads;
  }

  boolean hasMatchingFunctionContract(
      ImmutableSet<AcslFunctionContract> pContracts, FunctionContractAttribute pAttribute) {
    for (AcslFunctionContract contract : pContracts) {
      if (contract.getEnsuresClauses().size() == pAttribute.numEnsures
          && contract.getRequiresClauses().size() == pAttribute.numRequires
          && contract.getAssignsClauses().size() == pAttribute.numAssigns) {
        return true;
      }
    }
    return false;
  }

  boolean hasMatchingLoopAnnotation(
      ImmutableSet<CFANode> pExpectedNodes,
      ImmutableSetMultimap<CFANode, AcslLoopAnnotation> pActualAnnotations,
      LoopAnnotationAttribute pAttribute) {
    for (CFANode node : pExpectedNodes) {
      if (pActualAnnotations.containsKey(node)) {
        ImmutableSet<AcslLoopAnnotation> actualAnnotations = pActualAnnotations.get(node);
        for (AcslLoopAnnotation loopAnnotation : actualAnnotations) {
          if (loopAnnotation.getLoopInvariants().size() + loopAnnotation.getLoopAssigns().size()
              == pAttribute.numAnnotations) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public interface CfaNodeAttribute {}

  public record FunctionContractAttribute(
      String functionName, int numEnsures, int numRequires, int numAssigns)
      implements CfaNodeAttribute {}

  public record LoopAnnotationAttribute(
      String function, int leavingEdges, int enteringEdges, int numAnnotations, String rawStatement)
      implements CfaNodeAttribute {}

  public record AssertionAttribute(
      String function, String enteringStatement, String leavingStatement, String assertionString)
      implements CfaNodeAttribute {}
}
