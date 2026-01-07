// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.BiMap;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.ImportDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssFactory;
import org.sosy_lab.cpachecker.util.test.CPATestRunner;
import org.sosy_lab.cpachecker.util.test.TestResults;

public class DistributedSummarySynthesisTest {

  private static final String CONFIGURATION_FILE_GENERATE_BLOCK_GRAPH =
      "config/generateBlockGraph.properties";
  private static final String CONFIGURATION_FILE_GENERATE_CFA = "config/generateCFA.properties";
  private static final Language language = Language.C;
  private static final String PROGRAM = "doc/examples/example.c";
  private static final String BLOCKS_JSON_PATH = "block_analysis/blocks.json";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  // discard printed statistics; we only care about generation
  @SuppressWarnings("checkstyle:IllegalInstantiation") // ok for statistics
  private final PrintStream statisticsStream =
      new PrintStream(ByteStreams.nullOutputStream(), true, Charset.defaultCharset());

  @Test
  public void testBlockDecompositionExportsJson() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_GENERATE_BLOCK_GRAPH);
    File expectedBlocksJson = tempFolder.getRoot().toPath().resolve(BLOCKS_JSON_PATH).toFile();

    TestResults result = CPATestRunner.run(config, PROGRAM);
    result.getCheckerResult().printStatistics(statisticsStream);
    result.getCheckerResult().writeOutputFiles();

    result.assertIs(Result.DONE);
    assertWithMessage(
            "Expected block graph JSON at path '%s', but does not exist", BLOCKS_JSON_PATH)
        .that(expectedBlocksJson.exists())
        .isTrue();
    assertWithMessage("Block graph JSON '%s' is empty file", BLOCKS_JSON_PATH)
        .that(Files.readString(expectedBlocksJson.toPath(), StandardCharsets.UTF_8))
        .isNotEmpty();
  }

  @Test
  public void testImportDecompositionCanDecomposeWithShiftedCfa() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_GENERATE_BLOCK_GRAPH);
    Path expectedBlocksJson = tempFolder.getRoot().toPath().resolve(BLOCKS_JSON_PATH);
    TestResults result = CPATestRunner.run(config, PROGRAM);
    CFA originalCFA = result.getCheckerResult().getCfa();

    config = getConfig(CONFIGURATION_FILE_GENERATE_CFA);
    result = CPATestRunner.run(config, PROGRAM);
    CFA shiftedCFA = result.getCheckerResult().getCfa();

    ImportDecomposition decomposition = new ImportDecomposition(expectedBlocksJson);
    BlockGraph blockGraphWithOriginalCFA = decomposition.decompose(originalCFA);
    BlockGraph blockGraphWithShiftedCFA = decomposition.decompose(shiftedCFA);

    assertThat(originalCFA.nodes()).isNotEqualTo(shiftedCFA.nodes());
    assertThat(blockGraphWithShiftedCFA).isEqualTo(blockGraphWithOriginalCFA);
  }

  @Test
  public void testBlockGraphCanExportWithShiftedCfa() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_GENERATE_BLOCK_GRAPH);
    Path expectedBlocksJson = tempFolder.getRoot().toPath().resolve(BLOCKS_JSON_PATH);
    TestResults result = CPATestRunner.run(config, PROGRAM);
    String blocksJsonWithOriginalCFA = Files.readString(expectedBlocksJson, StandardCharsets.UTF_8);
    CFA originalCFA = result.getCheckerResult().getCfa();

    result = CPATestRunner.run(config, PROGRAM);
    String blocksJsonWithShiftedCFA = Files.readString(expectedBlocksJson, StandardCharsets.UTF_8);
    CFA shiftedCFA = result.getCheckerResult().getCfa();

    assertThat(originalCFA.nodes()).isNotEqualTo(shiftedCFA.nodes());
    assertThat(blocksJsonWithOriginalCFA).isEqualTo(blocksJsonWithShiftedCFA);
  }

  @Test
  public void testDssFactoryResetsCfaNodeIdMap() throws Exception {
    Configuration config = getConfig(CONFIGURATION_FILE_GENERATE_CFA);
    TestResults result = CPATestRunner.run(config, PROGRAM);
    CFA originalCFA = result.getCheckerResult().getCfa();

    result = CPATestRunner.run(config, PROGRAM);
    CFA shiftedCFA = result.getCheckerResult().getCfa();

    BiMap<Integer, CFANode> cfaNodeIdMapWithOriginalCFA =
        DssFactory.createCfaNodeIdMap(originalCFA);
    BiMap<Integer, CFANode> cfaNodeIdMapWithShiftedCFA = DssFactory.createCfaNodeIdMap(shiftedCFA);

    assertThat(originalCFA.nodes()).isNotEqualTo(shiftedCFA.nodes());
    assertThat(cfaNodeIdMapWithOriginalCFA).isNotEqualTo(cfaNodeIdMapWithShiftedCFA);
    assertThat(cfaNodeIdMapWithOriginalCFA.keySet()).isEqualTo(cfaNodeIdMapWithShiftedCFA.keySet());
  }

  private Configuration getConfig(String configFile)
      throws InvalidConfigurationException, IOException {
    // Do not use TestDataTools.configurationForTest() because we want output files
    Configuration configForFiles =
        Configuration.builder()
            .setOption("output.path", tempFolder.getRoot().getAbsolutePath())
            .build();
    FileTypeConverter fileTypeConverter = FileTypeConverter.create(configForFiles);
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);
    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder
        .loadFromFile(configFile)
        .setOption("language", language.name())
        .addConverter(FileOption.class, fileTypeConverter);
    return configBuilder.build();
  }
}
