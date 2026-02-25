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
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.test.ACSLParserTest;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class AcslMetadataParsingTest {
  private static final String TEST_DIR = "test/programs/acsl/";
  private final String programName;
  private final int expectedNumOfAnnotations;
  private final CFACreator cfaCreator;
  private final LogManager logManager;
  private final CfaNodeAttribute nodeAttribute;

  public AcslMetadataParsingTest(
      String pProgramName, int pExpectedNumOfAnnotations, CfaNodeAttribute pNodeAttribute)
      throws InvalidConfigurationException {
    programName = pProgramName;
    expectedNumOfAnnotations = pExpectedNumOfAnnotations;
    nodeAttribute = pNodeAttribute;
    Configuration config =
        TestDataTools.configurationForTest()
            .loadFromResource(ACSLParserTest.class, "acslToWitness.properties")
            .build();
    logManager = LogManager.createTestLogManager();
    cfaCreator = new CFACreator(config, logManager, ShutdownNotifier.createDummy());
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    ImmutableList.Builder<Object[]> b = ImmutableList.builder();
    // Assertions
    b.add(task("after_else.c", 1, new EmptyNodeAttribute()));
    b.add(task("after_for_loop2.c", 1, new EmptyNodeAttribute()));
    b.add(task("after_if.c", 1, new EmptyNodeAttribute()));
    b.add(task("after_loop.c", 1, new EmptyNodeAttribute()));
    b.add(task("after_loop2.c", 1, new EmptyNodeAttribute()));
    b.add(task("at_end.c", 1, new EmptyNodeAttribute()));
    b.add(task("badVariable.c", 0, new EmptyNodeAttribute()));
    b.add(task("end_of_do_while.c", 1, new EmptyNodeAttribute()));
    b.add(task("in_middle.c", 1, new EmptyNodeAttribute()));
    b.add(task("same_annotation_twice.c", 2, new EmptyNodeAttribute()));

    // Loop Annotations
    b.add(
        task(
            "double_loop_invariant.c",
            1,
            new LoopAnnotationAttribute("main", 2, 2, 2, "int i = 0;")));
    b.add(task("even_while.c", 1, new LoopAnnotationAttribute("main", 2, 2, 1, "while")));
    b.add(task("even_while_nondet.c", 1, new LoopAnnotationAttribute("main", 2, 2, 1, "while")));
    b.add(task("even_do_while.c", 1, new LoopAnnotationAttribute("main", 2, 1, 1, "do")));
    b.add(task("inv_for.c", 1, new LoopAnnotationAttribute("main", 2, 2, 1, "int i = 0;")));
    b.add(task("inv_short-for.c", 1, new LoopAnnotationAttribute("main", 2, 2, 1, "y = y + 1;")));

    // Function Contracts
    b.add(task("square_root.c", 1, new FunctionContractAttribute("sqroot", 3, 1, 0)));
    b.add(task("square.c", 1, new FunctionContractAttribute("square", 2, 0, 0)));
    b.add(task("square_result.c", 1, new FunctionContractAttribute("square", 1, 0, 0)));
    b.add(task("power.c", 1, new FunctionContractAttribute("power", 1, 2, 0)));
    b.add(task("power_result.c", 1, new FunctionContractAttribute("power", 1, 2, 0)));
    return b.build();
  }

  private static Object[] task(
      String program, int expectedNumOfAnnotations, CfaNodeAttribute nodeAttribute) {
    return new Object[] {program, expectedNumOfAnnotations, nodeAttribute};
  }

  @Test
  public void parseCorrectNumberOfAcslCommentsTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());

    if (programName.equals("badVariable.c")) {
      RuntimeException expectedException =
          assertThrows(
              RuntimeException.class, () -> cfaCreator.parseFilesAndCreateAcslMetadata(files));
      assertThat(expectedException)
          .hasMessageThat()
          .isEqualTo("Variable y is not declared in neither the C program nor the ACSL scope.");
    } else {
      AcslMetadata acslMetadata = cfaCreator.parseFilesAndCreateAcslMetadata(files);
      assertThat(acslMetadata).isNotNull();
      assertThat(acslMetadata.numOfAnnotaniots()).isEqualTo(expectedNumOfAnnotations);
    }
  }

  @Test
  public void acslNodeMappingTest()
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());

    if (programName.equals("badVariable.c")) {
      RuntimeException expectedException =
          assertThrows(
              RuntimeException.class, () -> cfaCreator.parseFilesAndCreateAcslMetadata(files));
      assertThat(expectedException)
          .hasMessageThat()
          .isEqualTo("Variable y is not declared in neither the C program nor the ACSL scope.");
    } else {
      CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
      AcslMetadata metadata = cfa.getAcslMetadata();
      assertThat(metadata).isNotNull();

      if (nodeAttribute instanceof FunctionContractAttribute pContractAttribute) {
        FunctionEntryNode expectedNode = cfa.getFunctionHead(pContractAttribute.functionName);
        assertThat(metadata.functionContracts()).containsKey(expectedNode);
        ImmutableSet<AcslFunctionContract> actualContracts =
            metadata.functionContracts().get(expectedNode);
        assertThat(hasMatchingFunctionContract(actualContracts, pContractAttribute)).isTrue();
      }
      if (nodeAttribute instanceof LoopAnnotationAttribute ploopAttribute) {
        Optional<ImmutableSet<CFANode>> loopHeads = cfa.getAllLoopHeads();
        assertThat(loopHeads).isPresent();
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
        assertThat(heads).isNotEmpty();
        ImmutableSet<CFANode> expectedNodes = heads.toSet();
        assertThat(
                hasMatchingLoopAnnotation(
                    expectedNodes, metadata.loopAnnotations(), ploopAttribute))
            .isTrue();
      }
    }
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

  interface CfaNodeAttribute {}

  public record EmptyNodeAttribute() implements CfaNodeAttribute {}

  public record FunctionContractAttribute(
      String functionName, int numEnsures, int numRequires, int numAssigns)
      implements CfaNodeAttribute {}

  public record LoopAnnotationAttribute(
      String function, int leavingEdges, int enteringEdges, int numAnnotations, String rawStatement)
      implements CfaNodeAttribute {}
}
