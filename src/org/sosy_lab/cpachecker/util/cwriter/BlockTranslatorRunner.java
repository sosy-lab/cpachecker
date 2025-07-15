package org.sosy_lab.cpachecker.util.cwriter;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.function.Predicate;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.MergeBlockNodesDecomposition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockGraph;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNodeWithoutGraphInformation;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.linear_decomposition.LinearBlockNodeDecomposition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class BlockTranslatorRunner {

  private CFA cfa;
  private BlockGraph blockGraph;

  private Configuration config;
  private LogManager logger;
  private ShutdownManager shutdownManager;
  private ShutdownNotifier shutdownNotifier;

  private static final String TEST_PROGRAM_PATH = "test/programs/block_analysis/many-ifs.c";
  private static final String CONFIG_PATH =
      "config/distributed-summary-synthesis/dss-block-analysis.properties";
  private static final String OUTPUT_DIR = "output/block2c_generated_blocks";

  public static void main(String[] args) {
    try {
      BlockTranslatorRunner runner = new BlockTranslatorRunner();
      runner.setUp();
      runner.runBlockNodeTranslation();
    } catch (Exception e) {
      System.err.println("Error during translation:");
      e.printStackTrace();
    }
  }

  public void setUp() throws Exception {
    config = createTestConfiguration(CONFIG_PATH);
    logger = LogManager.createTestLogManager();
    shutdownManager = ShutdownManager.create();
    shutdownNotifier = shutdownManager.getNotifier();
    cfa = parseCFA(config, logger, shutdownNotifier, TEST_PROGRAM_PATH);
    blockGraph = createBlockGraph();
  }

  public void runBlockNodeTranslation()
      throws InvalidConfigurationException, CPAException, IOException {
    Path outputDir = Paths.get(OUTPUT_DIR);
    Files.createDirectories(outputDir);

    Block2CTranslator translator = new Block2CTranslator(config, cfa);
    Path programPath = Paths.get(TEST_PROGRAM_PATH);
    String programName = programPath.getFileName().toString();
    int dotIndex = programName.lastIndexOf('.');
    if (dotIndex > 0) {
      programName = programName.substring(0, dotIndex);
    }

    Path programOutputDir = outputDir.resolve(programName);
    Files.createDirectories(programOutputDir);
    for (BlockNode blockNode : blockGraph.getNodes()) {
      String translated = translator.translateBlockNode(blockNode);
      String filename = "block_" + blockNode.getId() + ".c";
      Path outputFile = programOutputDir.resolve(filename);
      Files.write(outputFile, translated.getBytes(Charset.defaultCharset()));
    }
  }

  private static Configuration createTestConfiguration(String propertiesFilePath)
      throws IOException, InvalidConfigurationException {
    return TestDataTools.configurationForTest()
        .loadFromFile(propertiesFilePath)
        .setOption("cpa.predicate.blk.alwaysAtJoin", "true")
        .setOption("cpa.predicate.blk.alwaysAtBranch", "true")
        .setOption("cpa.predicate.blk.alwaysAtProgramExit", "true")
        .setOption("cpa.predicate.blk.alwaysAtLoops", "false")
        .setOption("cpa.predicate.blk.alwaysAtFunctions", "false")
        .setOption("cpa.predicate.blk.alwaysAfterThreshold", "false")
        .setOption("cpa.predicate.blk.alwaysAtFunctionHeads", "true")
        .setOption("cpa.predicate.blk.alwaysAtFunctionCallNodes", "false")
        .setOption("cpa.predicate.blk.alwaysAtFunctionExit", "true")
        .build();
  }

  private static CFA parseCFA(
      Configuration config, LogManager logger, ShutdownNotifier notifier, String filePath)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {
    CFACreator creator = new CFACreator(config, logger, notifier);
    return creator.parseFileAndCreateCFA(ImmutableList.of(filePath));
  }

  private BlockGraph createBlockGraph()
      throws InvalidConfigurationException, CPAException, InterruptedException {
    BlockOperator blockOperator = new BlockOperator();
    blockOperator.setCFA(cfa);
    config.inject(blockOperator);

    Predicate<CFANode> isBlockEnd = n -> blockOperator.isBlockEnd(n, -1);
    MergeBlockNodesDecomposition decomposition =
        new MergeBlockNodesDecomposition(
            new LinearBlockNodeDecomposition(isBlockEnd),
            2,
            Comparator.comparing(BlockNodeWithoutGraphInformation::getId),
            true);

    return decomposition.decompose(cfa);
  }
}
