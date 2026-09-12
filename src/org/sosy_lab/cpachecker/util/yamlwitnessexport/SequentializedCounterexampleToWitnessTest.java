// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization.SequentializationResult;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationMapping;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationUtils;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.AbstractCounterexampleToWitness.WitnessPathStep;

/** Tests that steps of a sequentialized program are mapped back to the concurrent input program. */
public class SequentializedCounterexampleToWitnessTest {

  /** The concurrent input program that is sequentialized. */
  private static final Path INPUT_PROGRAM =
      Path.of("./test/programs/mpor/sequentialization/lazy01.c");

  private CFA inputCfa;

  private CFA outputCfa;

  private SequentializationMapping mapping;

  private SequentializedCounterexampleToWitness exporter;

  @Before
  public void sequentializeInputProgram() throws Exception {
    Configuration config = TestUtils.configurationForTest().build();
    LogManager logger = LogManager.createTestLogManager();
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();

    // always use the preprocessor, the input is a .c file
    CFACreator cfaCreator = MPORUtil.buildTestCfaCreatorWithPreprocessor(logger, shutdownNotifier);
    inputCfa = cfaCreator.parseFileAndCreateCFA(ImmutableList.of(INPUT_PROGRAM.toString()));

    SequentializationResult result =
        Sequentialization.tryBuildProgram(
            new MPOROptions(config),
            inputCfa,
            SequentializationUtils.of(inputCfa, config, logger, shutdownNotifier));
    mapping = result.mapping();
    outputCfa =
        TestCfaUtils.makeCfaFromString(
            result.program(), Map.entry("parser.usePreprocessor", "false"));
    exporter =
        new SequentializedCounterexampleToWitness(
            config, inputCfa, mapping, Specification.alwaysSatisfied(), logger);
  }

  /**
   * Returns the lines of {@link #INPUT_PROGRAM} that contain {@code pKeyword}, so that the tests do
   * not have to be updated whenever the input program changes.
   */
  private static ImmutableList<Integer> linesContaining(String pKeyword) throws IOException {
    ImmutableList<String> lines =
        ImmutableList.copyOf(Files.readAllLines(INPUT_PROGRAM, StandardCharsets.UTF_8));
    ImmutableList.Builder<Integer> rLines = ImmutableList.builder();
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).contains(pKeyword)) {
        rLines.add(i + 1);
      }
    }
    ImmutableList<Integer> rResult = rLines.build();
    assertThat(rResult).isNotEmpty();
    return rResult;
  }

  /** Returns the edges that start a block of the sequentialization, ordered by their label. */
  private ImmutableList<CFAEdge> blockEdges() {
    return CFAUtils.allEdges(outputCfa)
        .filter(edge -> edge.getPredecessor() instanceof CFALabelNode)
        .toSortedList(
            Comparator.comparing(edge -> ((CFALabelNode) edge.getPredecessor()).getLabel()));
  }

  @Test
  public void testBlockEdgesAreMappedToTheThreadThatExecutesThem() throws IOException {
    // the labels are sorted, so the statements of the main thread come first and have created all
    // other threads before their statements are projected
    Set<Integer> createdThreads = SequentializedCounterexampleToWitness.newCreatedThreads();
    ImmutableList<Integer> reachErrorLines = linesContaining("reach_error();");
    boolean foundTarget = false;
    for (CFAEdge edge : blockEdges()) {
      String label = ((CFALabelNode) edge.getPredecessor()).getLabel();
      Optional<WitnessPathStep> step = exporter.projectEdge(edge, createdThreads);
      if (step.isEmpty()) {
        continue;
      }
      CFAEdge originalEdge = step.orElseThrow().edge();
      // the thread of the label must be the thread that executes the input program edge
      String expectedThread = label.startsWith("T0_") ? "main" : "thread" + label.charAt(1);
      assertThat(step.orElseThrow().currentThread()).hasValue(expectedThread);
      assertThat(originalEdge.getFileLocation().getFileName()).isEqualTo(INPUT_PROGRAM);

      if (originalEdge.getRawStatement().contains("reach_error")) {
        // reach_error is called inside a thread of the input program, not in its main function
        assertThat(reachErrorLines)
            .contains(originalEdge.getFileLocation().getStartingLineInOrigin());
        assertThat(step.orElseThrow().currentThread())
            .isNotEqualTo(Optional.of(AbstractCounterexampleToWitness.MAIN_THREAD_NAME));
        foundTarget = true;
      }
    }
    assertThat(foundTarget).isTrue();
  }

  @Test
  public void testThreadCreationsAreMappedToTheCreatedThread() throws IOException {
    Set<Integer> createdThreads = SequentializedCounterexampleToWitness.newCreatedThreads();
    ImmutableList.Builder<String> createdThreadNames = ImmutableList.builder();
    for (CFAEdge edge : blockEdges()) {
      Optional<WitnessPathStep> step = exporter.projectEdge(edge, createdThreads);
      if (step.isPresent() && step.orElseThrow().createdThread().isPresent()) {
        assertThat(step.orElseThrow().edge().getRawStatement()).contains("pthread_create");
        createdThreadNames.add(step.orElseThrow().createdThread().orElseThrow());
      }
    }
    // every pthread_create of the input program creates exactly one thread here
    ImmutableList<String> created = createdThreadNames.build();
    assertThat(created).hasSize(linesContaining("pthread_create(").size());
    assertThat(created).containsNoDuplicates();
  }

  @Test
  public void testAssumptionsAreRewrittenToInputProgramVariables() {
    // a global variable of the input program must be mapped back to its own name
    String globalVariable = globalVariableName();
    CIdExpression substitute = findVariable(globalVariable);
    Optional<CExpression> translated = exporter.toOriginalVocabulary(substitute);
    assertThat(translated).isPresent();
    assertThat(translated.orElseThrow().toASTString()).isEqualTo(globalVariable);
    assertThat(substitute.toASTString()).isNotEqualTo(globalVariable);

    // a variable of the sequentialization without a counterpart cannot be translated
    assertThat(exporter.toOriginalVocabulary(findGhostVariable())).isEmpty();
  }

  /** Returns the name of a global variable that the input program itself declares. */
  private String globalVariableName() {
    for (AVariableDeclaration global : CFAUtils.getGlobalVariableDeclarations(inputCfa)) {
      if (global.getFileLocation().getFileName().equals(INPUT_PROGRAM)) {
        return global.getName();
      }
    }
    throw new AssertionError("no global variable in " + INPUT_PROGRAM);
  }

  private CIdExpression findVariable(String pOriginalName) {
    for (CVariableDeclaration declaration : variableDeclarations()) {
      CSimpleDeclaration original =
          mapping.originalDeclarationsBySubstituteName().get(declaration.getName());
      if (original != null && original.getName().equals(pOriginalName)) {
        return new CIdExpression(FileLocation.DUMMY, declaration);
      }
    }
    throw new AssertionError("no substitute of " + pOriginalName + " in the sequentialization");
  }

  private CIdExpression findGhostVariable() {
    for (CVariableDeclaration declaration : variableDeclarations()) {
      if (!mapping.originalDeclarationsBySubstituteName().containsKey(declaration.getName())) {
        return new CIdExpression(FileLocation.DUMMY, declaration);
      }
    }
    throw new AssertionError("no ghost variable in the sequentialization");
  }

  private ImmutableList<CVariableDeclaration> variableDeclarations() {
    return CFAUtils.allEdges(outputCfa)
        .filter(CDeclarationEdge.class)
        .transform(CDeclarationEdge::getDeclaration)
        .filter(CVariableDeclaration.class)
        .toList();
  }
}
