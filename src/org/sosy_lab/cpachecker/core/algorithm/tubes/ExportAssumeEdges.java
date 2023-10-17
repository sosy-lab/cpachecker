// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.tubes;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "tubes")
public class ExportAssumeEdges implements Algorithm {

  private final CFA cfa;
  private final Algorithm algorithm;
  private final LogManager logger;

  @FileOption(Type.OUTPUT_FILE)
  @Option(secure = true, name = "assumes.outputPath", description = "where to write assume edges")
  private Path outputPath = Path.of("assumes.txt");

  public ExportAssumeEdges(
      Algorithm pAlgorithm, Configuration pConfiguration, CFA pCFA, LogManager pLogManager)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    cfa = pCFA;
    logger = pLogManager;
    algorithm = pAlgorithm;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    ImmutableSet<@NonNull String> assumes =
        FluentIterable.from(cfa.edges())
            .filter(CAssumeEdge.class)
            .filter(e -> !e.getSuccessor().getFunctionName().startsWith("__VERIFIER_"))
            .transform(
                assume ->
                    assume.getLineNumber()
                        + ": "
                        + assume.getExpression().toParenthesizedASTString(false))
            .toSet();
    try {
      if (outputPath != null) {
        IO.writeFile(outputPath, Charset.defaultCharset(), Joiner.on("\n").join(assumes));
      } else {
        logger.log(Level.WARNING, "Path outputPath required but is null.");
      }
    } catch (IOException pE) {
      throw new CPAException("Could not write to " + outputPath, pE);
    }
    return algorithm.run(reachedSet);
  }
}
