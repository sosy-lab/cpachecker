// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.github.difflib.algorithm.DiffException;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.LoopAbstractionExpressibleAsCode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Rewrite;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Rewrite.ConflictingModificationException;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyFactory;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Strategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyFactory;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.SummaryOptions;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

@Options(prefix = "loopabstraction")
public class LoopAbstractionAlgorithm implements Algorithm {

  @Option(
      secure = true,
      name = "outputdirectory",
      description = "directory in which the programs with inlined summaries are stored")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  private Path outputDirectory = Path.of("LA");

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final CFA cfa;
  private final StrategyFactory strategyFactory;

  public LoopAbstractionAlgorithm(
      Configuration pConfiguration,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa)
      throws InvalidConfigurationException {
    logger = pLogger;
    config = pConfiguration;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
    SummaryOptions summaryOptions = new SummaryOptions(config);
    strategyFactory =
        new StrategyFactory(
            logger,
            shutdownNotifier,
            summaryOptions.getMaxUnrollingsStrategy(),
            new StrategyDependencyFactory().createStrategy(summaryOptions.getCfaCreationStrategy()),
            cfa);
    config.inject(this);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    if (!cfa.getLoopStructure().isPresent()) {
      logger.logf(
          Level.WARNING, "Loop structure not present, cannot compute any loop abstractions!");
    }
    ImmutableCollection<Loop> loops = cfa.getLoopStructure().orElseThrow().getAllLoops();
    for (StrategiesEnum en : StrategiesEnum.values()) {
      Strategy strategy = strategyFactory.buildStrategy(en);
      if (!(strategy instanceof LoopAbstractionExpressibleAsCode)) {
        continue;
      }
      for (Loop loop : loops) {
        CFAEdge incomingEdge = Iterables.getOnlyElement(loop.getIncomingEdges());
        Path containingFile = incomingEdge.getFileLocation().getFileName();

        int offset = incomingEdge.getFileLocation().getNodeOffset();
        int len = incomingEdge.getFileLocation().getNodeLength();

        try {
          Rewrite r = new Rewrite(containingFile);
          Optional<String> summary =
              ((LoopAbstractionExpressibleAsCode) strategy).summarizeAsCode(loop);
          if (!summary.isPresent()) {
            continue;
          }
          r.insertIndented(
              offset,
              String.format(
                  "// START %s\n%s// END %s\n",
                  en.toString(), summary.orElseThrow(), en.toString()));
          r.delete(offset, len);
          String patchFilename =
              String.format(
                  "%s.loop_at_offset_%d.%s.patch",
                  containingFile.getFileName().toString(), offset, en.toString());
          try (Writer w =
              IO.openOutputFile(outputDirectory.resolve(patchFilename), Charset.defaultCharset())) {
            String filenameString = containingFile.getFileName().toString();
            w.append(r.asDiff(filenameString, filenameString, 5));
          }
        } catch (ConflictingModificationException | IOException | DiffException e) {
          logger.logfException(
              Level.WARNING,
              e,
              "Could not dump loop abstraction for file %s, loop %s, strategy %s.",
              containingFile,
              loop,
              en);
        }
      }
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }
}
