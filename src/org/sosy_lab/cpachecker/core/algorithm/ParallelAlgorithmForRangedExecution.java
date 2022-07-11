// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ParallelAlgorithmForRangedExecution extends ParallelAlgorithm {

  public ParallelAlgorithmForRangedExecution(
      Configuration pGlobalConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException {
    super(
        pGlobalConfig,
        pLogger,
        pShutdownNotifier,
        pSpecification,
        pCfa,
        pAggregatedReachedSets,
        ImmutableList.of());
  }

  public void computeAnalyses(List<Path> pPath2LoopBound, List<Path> pConfigFiles)
      throws InvalidConfigurationException, IOException, CPAException, InterruptedException {
    List<Callable<ParallelAnalysisResult>> newAnanlyses = new ArrayList<>();
    for (int i = 0; i < pConfigFiles.size(); i++) {
      ConfigurationBuilder builder = Configuration.builder();
      builder.loadFromFile(pConfigFiles.get(i));
      if (i > 0) {
        // Lower bound not for first analysis
        builder.setOption(
            "cpa.rangedAnalysis.path2LowerInputFile",
            pPath2LoopBound.get(i - 1).toAbsolutePath().toString());
      }
      if (i < pConfigFiles.size() - 1) {
        // UpperBpund not for last analysis

        builder.setOption(
            "cpa.rangedAnalysis.path2UpperInputFile",
            pPath2LoopBound.get(i).toAbsolutePath().toString());
      }
      newAnanlyses.add(
          super.createParallelAnalysis(
              builder.build(), i, false, false, pConfigFiles.get(i).toString()));
    }
    super.analyses = ImmutableList.copyOf(newAnanlyses);
  }
}
