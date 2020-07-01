// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.CPAs;

public abstract class AlgorithmCompositionStrategy {

  protected LogManager logger;

  protected AlgorithmCompositionStrategy(LogManager pLogger) {
    logger = pLogger;
  }

  protected ImmutableList<AlgorithmContext> algorithmContexts;

  protected void initializeAlgorithmContexts(List<AnnotatedValue<Path>> pConfigFiles) {
    com.google.common.collect.ImmutableList.Builder<AlgorithmContext> contextBuilder =
        ImmutableList.<AlgorithmContext>builder();

    for (AnnotatedValue<Path> configFile : pConfigFiles) {
      contextBuilder.add(new AlgorithmContext(configFile));
    }
    algorithmContexts = contextBuilder.build();
  }

  public abstract boolean hasNextAlgorithm();

  public abstract AlgorithmContext getNextAlgorithm();

  public void finalCleanUp(final AlgorithmContext pLastContextRun) {
    for (AlgorithmContext context : algorithmContexts) {
      if (context != pLastContextRun
          && context != null
          && context.getCPA() != null
          && context.reuseCPA()) {
        CPAs.closeCpaIfPossible(context.getCPA(), logger);
      }
    }
  }

  interface Factory {
    AlgorithmCompositionStrategy create(
        Configuration config,
        LogManager logger,
        ShutdownNotifier shutdownNotifier,
        CFA cfa,
        Specification specification)
        throws InvalidConfigurationException;
  }
}
