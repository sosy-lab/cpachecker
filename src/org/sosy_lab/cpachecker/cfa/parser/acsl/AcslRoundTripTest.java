// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.acsl;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.acsl.AcslMetadata;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Checks for each annotated test program that replacing each annotation by its {@link
 * AAcslAnnotation#toAstString()} gives the same ACSL metadata.
 */
@RunWith(Parameterized.class)
public class AcslRoundTripTest {

  private static final Path TEST_DIR = Path.of("test", "programs", "acsl");

  private static final ImmutableSet<String> INVALID_PROGRAMS =
      ImmutableSet.of(
          "abs.c",
          "annotation_at_wrong_location.c",
          "badVariable.c",
          "badVariableStatementContract.c",
          "empty.c",
          "empty_comment.c",
          "function_contract_bad_variable.c",
          "loop_invariant_bad_variable.c",
          "maxArray.c",
          "power.c",
          "simple.c",
          "square.c",
          "traps.c");

  @Parameters(name = "{0}")
  public static List<Object[]> programs() throws IOException {
    try (Stream<Path> files = Files.list(TEST_DIR)) {
      return files
          .map(file -> file.getFileName().toString())
          .filter(name -> name.endsWith(".c"))
          .sorted()
          .map(name -> new Object[] {name})
          .collect(ImmutableList.toImmutableList());
    }
  }

  @Parameter(0)
  public String programName;

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private static CFA parse(Path pProgram) throws Exception {
    Configuration config =
        TestUtils.configurationForTest()
            .loadFromResource(AcslRoundTripTest.class, "withAcslMetadata.properties")
            .build();
    return new CFACreator(config, LogManager.createTestLogManager(), ShutdownNotifier.createDummy())
        .parseFileAndCreateCFA(ImmutableList.of(pProgram.toString()));
  }

  @Test
  public void printedAnnotationsGiveSameMetadata() throws Exception {
    Path program = TEST_DIR.resolve(programName);
    assume()
        .withMessage("The annotations are invalid")
        .that(INVALID_PROGRAMS)
        .doesNotContain(programName);
    CFA cfa = parse(program);
    AcslMetadata metadata = cfa.getAcslMetadata();

    Path printedProgram = tempFolder.getRoot().toPath().resolve(programName);
    Files.writeString(printedProgram, printAnnotations(Files.readString(program), metadata));
    CFA printedCfa = parse(printedProgram);

    assertWithMessage("metadata of %s", printedProgram)
        .that(printedCfa.getAcslMetadata())
        .isEqualTo(withNodesOf(metadata, cfa, printedCfa));
  }

  /** Replaces each annotation in the program by its printed form and keeps the line numbers. */
  private static String printAnnotations(String pProgram, AcslMetadata pMetadata) {
    List<AAcslAnnotation> annotations =
        ImmutableList.<AAcslAnnotation>builder()
            .addAll(pMetadata.assertions().values())
            .addAll(pMetadata.loopAnnotations().values())
            .addAll(pMetadata.functionContracts().values())
            .addAll(pMetadata.statementContracts().values())
            .build()
            .stream()
            .distinct()
            .sorted(
                Comparator.comparingInt(
                        (AAcslAnnotation annotation) ->
                            annotation.getFileLocation().getNodeOffset())
                    .reversed())
            .collect(ImmutableList.toImmutableList());

    StringBuilder program = new StringBuilder(pProgram);
    for (AAcslAnnotation annotation : annotations) {
      FileLocation location = annotation.getFileLocation();
      int start = location.getNodeOffset();
      int end = start + location.getNodeLength();
      String printed = annotation.toAstString();
      int lineBreaks = CharMatcher.is('\n').countIn(program.subSequence(start, end));
      assertThat(printed).doesNotContain("\n");
      program.replace(start, end, printed + "\n".repeat(lineBreaks));
    }
    return program.toString();
  }

  /** Returns the metadata with the nodes of the given isomorphic CFA. */
  private static AcslMetadata withNodesOf(AcslMetadata pMetadata, CFA pCfa, CFA pOtherCfa) {
    assertThat(pOtherCfa.nodes()).hasSize(pCfa.nodes().size());
    // The nodes of isomorphic CFAs correspond to each other in iteration order
    Map<CFANode, CFANode> nodes = new HashMap<>();
    Iterator<CFANode> otherNodes = pOtherCfa.nodes().iterator();
    for (CFANode node : pCfa.nodes()) {
      nodes.put(node, otherNodes.next());
    }
    return new AcslMetadata(
        pMetadata.globalAcslDeclarations(),
        withNodes(pMetadata.assertions(), nodes),
        withNodes(pMetadata.loopAnnotations(), nodes),
        withNodes(pMetadata.functionContracts(), nodes),
        withNodes(pMetadata.statementContracts(), nodes),
        withNodes(pMetadata.statementContractEnds(), nodes));
  }

  private static <T> ImmutableSetMultimap<CFANode, T> withNodes(
      ImmutableSetMultimap<CFANode, T> pMultimap, Map<CFANode, CFANode> pNodes) {
    ImmutableSetMultimap.Builder<CFANode, T> builder = ImmutableSetMultimap.builder();
    pMultimap.forEach((node, value) -> builder.put(pNodes.get(node), value));
    return builder.build();
  }
}
