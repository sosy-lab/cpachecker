/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.Specification;

@Options(prefix = "slicing")
public class ReducerExtractor extends AllTargetsExtractor {

  private final Configuration config;

  @Option(
    secure = true,
    name = "conditionFiles",
    description =
        "path to condition files plus additional assumption guiding automaton when condition itself is in propriertary format and not in witness format"
  )
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private List<Path> conditionFiles =
      ImmutableList.of(
          Paths.get("output/AssumptionAutomaton.txt"),
          Paths.get("config/specification/AssumptionGuiding.spc"));

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
      Specification conditionSpec =
          Specification.fromFiles(
              ImmutableSet.of(), conditionFiles, pCfa, config, logger, shutdownNotifier);
      compositeSpec = Specification.combine(conditionSpec, pError);
    } catch (InvalidConfigurationException e) {
      logger.logException(
          Level.WARNING,
          e,
          "Failed to build composite specification of condition and property specification. Continue with property specification only.");
      compositeSpec = pError;
    }
    return super.getSlicingCriteria(pCfa, compositeSpec, shutdownNotifier, logger);
  }
}
