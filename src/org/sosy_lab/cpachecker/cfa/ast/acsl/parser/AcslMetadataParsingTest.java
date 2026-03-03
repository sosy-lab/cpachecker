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
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopAnnotation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

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
        TestDataTools.configurationForTest()
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
            1,
            1,
            ImmutableList.of(new FunctionContractAttribute("sqroot", 3, 1, 0))));
    b.add(
        task(
            "square_with_predicate.c",
            2,
            1,
            ImmutableList.of(new FunctionContractAttribute("square", 2, 0, 0))));
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
    b.add(
        task(
            "square_root.c",
            1,
            0,
            ImmutableList.of(new FunctionContractAttribute("sqroot", 3, 1, 0))));
    b.add(
        task("square.c", 1, 0, ImmutableList.of(new FunctionContractAttribute("square", 2, 0, 0))));
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

    if (programName.equals("badVariable.c")) {
      RuntimeException expectedException =
          assertThrows(RuntimeException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(expectedException)
          .hasMessageThat()
          .isEqualTo("Variable y is not declared in neither the C program nor the ACSL scope.");
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

    if (programName.equals("badVariable.c")) {
      RuntimeException expectedException =
          assertThrows(RuntimeException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(expectedException)
          .hasMessageThat()
          .isEqualTo("Variable y is not declared in neither the C program nor the ACSL scope.");
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

    if (programName.equals("badVariable.c")) {
      RuntimeException expectedException =
          assertThrows(RuntimeException.class, () -> cfaCreator.parseFileAndCreateCFA(files));
      assertThat(expectedException)
          .hasMessageThat()
          .isEqualTo("Variable y is not declared in neither the C program nor the ACSL scope.");
    } else {
      CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
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
      if (contract.numOfEnsures() == pAttribute.numEnsures
          && contract.numOfRequires() == pAttribute.numRequires
          && contract.numOfAssigns() == pAttribute.numAssigns) {
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
          if (loopAnnotation.numOfLoopInvariants() == pAttribute.numAnnotations) {
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
