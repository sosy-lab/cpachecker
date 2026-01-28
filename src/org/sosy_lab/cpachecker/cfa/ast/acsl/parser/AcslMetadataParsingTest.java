// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
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
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslScope;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssigns;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.ast.acslDeprecated.test.ACSLParserTest;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class AcslMetadataParsingTest {
  private static final String TEST_DIR = "test/programs/acsl/";
  private final String programName;
  private final ImmutableList<String> expectedComments;
  private final CodeLoctation expectedLoc;
  private final CFACreator cfaCreator;
  private final LogManager logManager;

  public AcslMetadataParsingTest(
      String pProgramName, ImmutableList<String> pAnnotations, CodeLoctation pExpectedLoc)
      throws InvalidConfigurationException {
    programName = pProgramName;
    expectedComments = pAnnotations;
    expectedLoc = pExpectedLoc;
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
    // Regular Annotations (assertions and loop invariants)
    b.add(
        task(
            "after_else.c",
            ImmutableList.of("assert a == 10 || a == 20;"),
            new CodeLoctation(17, 7)));
    b.add(task("after_for_loop2.c", ImmutableList.of("assert b == 20;"), new CodeLoctation(16, 7)));
    b.add(task("after_if.c", ImmutableList.of("assert a != 20;"), new CodeLoctation(15, 7)));
    b.add(task("after_loop.c", ImmutableList.of("assert a == 20;"), new CodeLoctation(15, 7)));
    b.add(task("after_loop2.c", ImmutableList.of("assert  a == 20;"), new CodeLoctation(15, 7)));
    b.add(task("at_end.c", ImmutableList.of("assert a != 20;"), new CodeLoctation(15, 7)));
    b.add(task("badVariable.c", ImmutableList.of(), new CodeLoctation(12, 2)));
    b.add(task("end_of_do_while.c", ImmutableList.of("assert a <= 20"), new CodeLoctation(15, 7)));
    b.add(
        task(
            "even_while.c",
            ImmutableList.of("loop invariant x % 2 == 0;"),
            new CodeLoctation(21, 10)));
    b.add(
        task(
            "even_while_nondet.c",
            ImmutableList.of("loop invariant x % 2 == 0;"),
            new CodeLoctation(21, 3)));
    b.add(
        task(
            "even_do_while.c",
            ImmutableList.of("loop invariant  1 <= x <= 10 && x % 2 == 1;"),
            new CodeLoctation(21, 3)));
    b.add(task("in_middle.c", ImmutableList.of("assert a == 19;"), new CodeLoctation(16, 5)));
    b.add(
        task(
            "inv_for.c",
            ImmutableList.of("loop invariant x + y == 20;"),
            new CodeLoctation(13, 19)));
    b.add(
        task(
            "inv_short-for.c",
            ImmutableList.of("loop invariant x + y == 20;"),
            new CodeLoctation(13, 10)));
    b.add(
        task(
            "same_annotation_twice.c",
            ImmutableList.of("assert x == 10;", "assert x == 10;"),
            new CodeLoctation(12, 5)));

    // function contracts
    b.add(
        task(
            "power.c",
            ImmutableList.of("requires a > 0; requires b>= 0; ensures c > 0;"),
            new CodeLoctation(16, 1)));
    return b.build();
  }

  private static Object[] task(
      String program, ImmutableList<String> annotations, CodeLoctation expectedLoc) {
    return new Object[] {program, annotations, expectedLoc};
  }

  @Test
  public void parseCorrectNumberOfAcslCommentsTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());
    try {
      CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
      if (cfa.getMetadata().getAcslMetadata() != null) {
        assert cfa.getMetadata().getAcslMetadata().size() == expectedComments.size();
      }
    } catch (RuntimeException e) {
      assert programName.equals("badVariable.c");
      assert e.getMessage()
          .equals("Variable y is not declared in neither the C program nor the ACSL scope.");
    }
  }

  @Test
  public void parseMetadataTest() throws Exception {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());
    try {
      CFA cfa = cfaCreator.parseFileAndCreateCFA(files);

      CProgramScope programScope = new CProgramScope(cfa, logManager);

      ImmutableList.Builder<AAcslAnnotation> expectedBuilder = ImmutableList.builder();
      for (String s : expectedComments) {
        expectedBuilder.add(
            AcslParser.parseAcslAnnotation(s, FileLocation.DUMMY, programScope, AcslScope.empty()));
      }
      ImmutableList<AAcslAnnotation> expectedAnnotations = expectedBuilder.build();

      AcslMetadata acslMetadata = cfa.getMetadata().getAcslMetadata();
      ImmutableSetMultimap<AcslAssertion, CFANode> actualAssertions =
          acslMetadata.assertions().inverse();
      ImmutableSetMultimap<AcslLoopInvariant, CFANode> actualLoopInvariants =
          acslMetadata.invariants().inverse();
      ImmutableSetMultimap<AcslAssigns, CFANode> actualAssigns =
          acslMetadata.modifiedMemoryLocations().inverse();
      for (AAcslAnnotation expectedAnnotation : expectedAnnotations) {
        switch (expectedAnnotation) {
          case AcslAssertion assertion -> {
            assert actualAssertions.containsKey(assertion);
          }
          case AcslLoopInvariant loopInvariant -> {
            assert actualLoopInvariants.containsKey(loopInvariant);
          }
          case null, default -> {
            assert actualAssigns.containsKey(expectedAnnotation);
          }
        }
      }
    } catch (RuntimeException e) {
      assert programName.equals("badVariable.c");
      assert e.getMessage()
          .equals("Variable y is not declared in neither the C program nor the ACSL scope.");
    }
  }

  @Test
  public void updateCfaNodesCorrectlyTest()
      throws ParserException, IOException, InterruptedException, InvalidConfigurationException {
    List<String> files = ImmutableList.of(Path.of(TEST_DIR, programName).toString());
    try {
      CFA cfa = cfaCreator.parseFileAndCreateCFA(files);
      if (cfa.getMetadata().getAcslMetadata() != null
          && !cfa.getMetadata().getAcslMetadata().pAcslComments().isEmpty()) {
        AcslComment comment = cfa.getMetadata().getAcslMetadata().pAcslComments().getFirst();
        if (comment.hasCfaNode()) {
          FileLocation nodeLoc = describeFileLocation(comment.getCfaNode());
          assert nodeLoc.getStartingLineNumber() == expectedLoc.expectedLine;
          assert nodeLoc.getStartColumnInLine() == expectedLoc.expectedCol;
        }
      }
    } catch (RuntimeException e) {
      assert programName.equals("badVariable.c");
      assert e.getMessage()
          .equals("Variable y is not declared in neither the C program nor the ACSL scope.");
    }
  }

  public FileLocation describeFileLocation(CFANode node) {
    if (node instanceof FunctionEntryNode functionEntryNode) {
      return functionEntryNode.getFileLocation();
    }
    if (node.getNumLeavingEdges() > 0) {
      FileLocation loc = node.getLeavingEdge(0).getFileLocation();
      if (loc.isRealLocation()) {
        return loc;
      }
    }
    if (node.getNumEnteringEdges() > 0) {
      FileLocation loc = node.getEnteringEdge(0).getFileLocation();
      if (loc.isRealLocation()) {
        return loc;
      }
    }
    throw new RuntimeException("Node has no location");
  }

  public record CodeLoctation(int expectedLine, int expectedCol) {}
}
