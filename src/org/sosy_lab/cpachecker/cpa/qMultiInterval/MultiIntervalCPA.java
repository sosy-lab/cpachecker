/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.qMultiInterval;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

// Warning : this CPA-analysis contains...

/*
 * Important Information: This analysis depends on the class org.sosy_lab.cpachecker.cpa.interval. If the Interval class is changed, this analysis can break.
 */
@Options(prefix = "cpa.qMultiInterval")
public class MultiIntervalCPA extends AbstractCPA {

  private final LogManager logger;
  private IntervalMapping iMap;
  private MultiIntervalMinMaxMapping minmax;

  @Option(
    secure = true,
    name = "maxLoops",
    toUppercase = true,
    description =
        "After how many loopiterations the loop should be left. Then the variables will be overapproximated"
  )
  private int maxLoops = 10000;

  @Option(
    secure = true,
    name = "maxIntervals",
    toUppercase = true,
    description = "How many intervals can be saved in a variable"
  )
  private int maxIntervals = 10000;

  @Option(
    name = "intervalMapping",
    toUppercase = false,
    description = "Which Intervals should be used for the Vars(File)"
  )
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private transient Path iMapFile = Paths.get("betamap.conf");

  @Option(
    name = "MinMaxMapping",
    toUppercase = false,
    description =
        "Which is the lowes allowed remaining Entropy, the highest allowed leakage or the lowest allowed Min-Entropy(File)"
  )
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private transient Path minMax = Paths.get("betamap.conf");

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(MultiIntervalCPA.class);
  }

  @SuppressWarnings("unused")
  private MultiIntervalCPA(
      Configuration config, LogManager pLogger, ShutdownNotifier shutdownNotifier, CFA cfa)
      throws InvalidConfigurationException {

    super(
        "JOIN",
        "JOIN",
        DelegateAbstractDomain.<MultiIntervalState>getInstance(),
        null);
    config.inject(this);
    logger = pLogger;
    iMap = new IntervalMapping(iMapFile, logger);
    minmax = new MultiIntervalMinMaxMapping(minMax, logger);
    Range.maxOut = maxIntervals;

    logger.log(
        Level.WARNING,
        "There could occur some errors related to not creating a state if there is an if-clause without an else clause. To avoid this, it is recommended that every if has at least an else{} part. Even if its empty");
  }


  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    MultiIntervalState qstate = new MultiIntervalState(logger, maxLoops);
    return qstate;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new MultiIntervalRelation(logger, iMap, minmax);
  }
}
