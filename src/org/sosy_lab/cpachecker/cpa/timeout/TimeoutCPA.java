/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.timeout;

import java.util.concurrent.TimeUnit;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;

@Options(prefix = "cpa.timeout")
public class TimeoutCPA extends AbstractCPA {

  // @Option(secure = true, name = "walltime", description = "Set the timeout length in seconds")
  private long walltime = -1;

  private TimeoutPrecisionAdjustment precisionAdjustment;
  private WalltimeLimit limit;
  private TimeoutTransferRelation transferRelation;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TimeoutCPA.class);
  }

  public void setWalltime(long pWalltime) {
    walltime = pWalltime;
    if (walltime > 0) {
      limit = WalltimeLimit.fromNowOn(walltime, TimeUnit.SECONDS);
      precisionAdjustment = new TimeoutPrecisionAdjustment(limit);
    }
  }


  public TimeoutCPA(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    super("sep", "sep", null /* lazy initialization */);
    config.inject(this);
    transferRelation = new TimeoutTransferRelation();
    if (walltime > 0) {
      limit = WalltimeLimit.fromNowOn(walltime, TimeUnit.SECONDS);
      precisionAdjustment = new TimeoutPrecisionAdjustment(limit);
    }
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return precisionAdjustment;
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new TimeoutState(false);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  public boolean hasTimedout() {
    return precisionAdjustment.hasTimedout();
  }

}
