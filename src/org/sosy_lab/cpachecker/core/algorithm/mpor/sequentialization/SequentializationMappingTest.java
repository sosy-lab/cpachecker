// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization.SequentializationResult;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationMapping.BlockOrigin;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.test.TestCfaUtils;
import org.sosy_lab.cpachecker.util.test.TestUtils;

/**
 * Tests that the {@link SequentializationMapping} of a sequentialization relates its elements back
 * to the input program, for the encodings that the sequentialization can be written in.
 */
@RunWith(Parameterized.class)
public class SequentializationMappingTest {

  /** The concurrent input program that is sequentialized. */
  private static final Path INPUT_PROGRAM =
      Path.of("./test/programs/mpor/sequentialization/lazy01.c");

  @Parameters(name = "{0}")
  public static List<Object[]> encodings() {
    return ImmutableList.of(
        new Object[] {"default", ImmutableMap.of()},
        new Object[] {
          "if-else chains",
          ImmutableMap.of(
              "selectionEncodingForStatements", "IF_ELSE_CHAIN",
              "selectionEncodingForThreads", "IF_ELSE_CHAIN",
              "nondeterminismSource", "NEXT_THREAD_AND_NUM_STATEMENTS"),
        },
        new Object[] {
          "binary search trees",
          ImmutableMap.of(
              "selectionEncodingForStatements", "BINARY_SEARCH_TREE",
              "selectionEncodingForThreads", "BINARY_SEARCH_TREE",
              "nondeterminismSource", "NEXT_THREAD_AND_NUM_STATEMENTS"),
        },
        new Object[] {
          "dense bit vectors",
          ImmutableMap.of(
              "bitVectorEncoding", "HEXADECIMAL",
              "executeThreadsUntilConflict", "true",
              "partialOrderReductionPrecision", "ACCESS_ONLY"),
        },
        new Object[] {
          "sparse bit vectors",
          ImmutableMap.of(
              "bitVectorEncoding", "SPARSE",
              "executeThreadsUntilConflict", "true",
              "partialOrderReductionPrecision", "READ_AND_WRITE"),
        },
        new Object[] {
          "array program counters and long names",
          ImmutableMap.of("scalarProgramCounters", "false", "shortVariableNames", "false"),
        });
  }

  @Parameter(0)
  public String encodingName;

  @Parameter(1)
  public ImmutableMap<String, String> encodingOptions;

  private CFA inputCfa;

  private CFA outputCfa;

  private SequentializationMapping mapping;

  @Before
  public void sequentializeInputProgram() throws Exception {
    ConfigurationBuilder configBuilder = TestUtils.configurationForTest();
    for (Entry<String, String> option : encodingOptions.entrySet()) {
      configBuilder.setOption("analysis.algorithm.MPOR." + option.getKey(), option.getValue());
    }
    Configuration config = configBuilder.build();
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

  @Test
  public void testBlockOriginsCoverAllLabelsOfOutputProgram() {
    ImmutableMap<String, BlockOrigin> blockOrigins = mapping.blockOriginsByLabel();

    assertThat(blockOrigins).isNotEmpty();
    // every label of the output program that belongs to a block must be mapped back, otherwise
    // statements of the counterexample cannot be assigned to a thread
    ImmutableSet<String> labels =
        CFAUtils.allEdges(outputCfa)
            .transform(CFAEdge::getSuccessor)
            .filter(CFALabelNode.class)
            .transform(CFALabelNode::getLabel)
            .filter(label -> label.matches("T[0-9]+_[0-9]+"))
            .toSet();
    assertThat(labels).isNotEmpty();
    assertThat(blockOrigins.keySet()).containsAtLeastElementsIn(labels);

    // all input program edges of a block belong to the thread of that block
    for (BlockOrigin blockOrigin : blockOrigins.values()) {
      assertThat(blockOrigin.originalEdgeByStatement()).isNotEmpty();
      assertThat(blockOrigin.threadId()).isAtLeast(0);
    }
  }

  @Test
  public void testThreadCreationEdgesAreFromInputProgram() throws IOException {
    ImmutableMap<CFAEdge, Integer> creationEdges = mapping.threadIdByCreationEdge();

    // every pthread_create of the input program creates exactly one thread here, the main thread
    // is created by no edge
    ImmutableList<Integer> pthreadCreateLines = linesContaining("pthread_create(");
    assertThat(creationEdges).hasSize(pthreadCreateLines.size());
    ImmutableList<Integer> creationLines =
        creationEdges.keySet().stream()
            .map(edge -> edge.getFileLocation().getStartingLineInOrigin())
            .sorted()
            .collect(ImmutableList.toImmutableList());
    assertThat(creationLines).isEqualTo(pthreadCreateLines);
    for (CFAEdge creationEdge : creationEdges.keySet()) {
      assertThat(creationEdge.getRawStatement()).contains("pthread_create");
      assertThat(CFAUtils.allEdges(inputCfa).toSet()).contains(creationEdge);
    }
  }

  @Test
  public void testSubstitutedVariablesMapBackToInputProgram() {
    ImmutableMap<String, CSimpleDeclaration> originalDeclarations =
        mapping.originalDeclarationsBySubstituteName();

    assertThat(originalDeclarations).isNotEmpty();
    // every global variable declared in the input program itself, as opposed to in a header it
    // includes, must have a substitute in the sequentialization
    ImmutableSet<String> originalNames =
        originalDeclarations.values().stream()
            .map(CSimpleDeclaration::getName)
            .collect(ImmutableSet.toImmutableSet());
    boolean checkedAny = false;
    for (AVariableDeclaration global : CFAUtils.getGlobalVariableDeclarations(inputCfa)) {
      if (global.getFileLocation().getFileName().equals(INPUT_PROGRAM)) {
        assertThat(originalNames).contains(global.getName());
        checkedAny = true;
      }
    }
    assertThat(checkedAny).isTrue();
  }
}
