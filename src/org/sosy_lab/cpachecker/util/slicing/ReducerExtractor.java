// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.specification.Specification;

@Options(prefix = "slicing")
public class ReducerExtractor extends AllTargetsExtractor {

  private final Configuration config;

  @Option(
      secure = true,
      name = "conditionFiles",
      description =
          "path to condition files plus additional assumption guiding automaton when condition"
              + " itself is in propriertary format and not in witness format")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Set<Path> conditionFiles =
      ImmutableSet.of(
          Path.of("output/AssumptionAutomaton.txt"),
          Classes.getCodeLocation(ReducerExtractor.class)
              .resolveSibling("config/specification/AssumptionGuidingAutomaton.spc"));

  public ReducerExtractor(final Configuration pConfig) throws InvalidConfigurationException {
    config = pConfig;
    pConfig.inject(this);
    // TODO precondition checks
  }

  @Override
  public Set<CFAEdge> getSlicingCriteria(
      final CFA pCfa,
      final Specification pError,
      final ShutdownNotifier shutdownNotifier,
      LogManager logger)
      throws InterruptedException {
    Specification compositeSpec;
    try {
      compositeSpec =
          pError.withAdditionalSpecificationFile(
              conditionFiles, pCfa, config, logger, shutdownNotifier);
    } catch (InvalidConfigurationException e) {
      logger.logException(
          Level.WARNING,
          e,
          "Failed to build composite specification of condition and property specification."
              + " Continue with property specification only.");
      compositeSpec = pError;
    }
    return super.getSlicingCriteria(pCfa, compositeSpec, shutdownNotifier, logger);
  }
}
