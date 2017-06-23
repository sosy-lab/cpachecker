/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.coverage;

import java.io.PrintStream;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageCPA.CoverageMode;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;
import org.sosy_lab.cpachecker.util.coverage.CoverageReport;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

@Options
public abstract class CoverageStatistics extends AbstractStatistics {

  private final LogManager logger;

  protected final CoverageReport report;

  public static CoverageStatistics create(CoverageMode mode, Configuration pConfig, LogManager pLogger,
      CFA pCFA, CoverageData pCov) throws InvalidConfigurationException {

    switch (mode) {
      case REACHED:
        return new ReachedCoverageStatistics(pConfig, pLogger, pCFA);

      case TRANSFER:
        return new TransferCoverageStatistics(pConfig, pLogger, pCov);

      default:
        return null;
    }
  }

  public CoverageStatistics(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {

    this.logger = pLogger;

    report = new CoverageReport(pConfig, pLogger);
  }

  static class ReachedCoverageStatistics extends CoverageStatistics {

    protected final CFA cfa;

    public ReachedCoverageStatistics(Configuration pConfig, LogManager pLogger,
        CFA pCFA) throws InvalidConfigurationException {

      super(pConfig, pLogger);
      cfa = pCFA;
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      report.writeCoverageReport(pOut, pReached, cfa);
    }

    @Override
    public String getName() {
      return String.format("Code Coverage (Mode: Reached)");
    }
  }

  static class TransferCoverageStatistics extends CoverageStatistics {

    protected final CoverageData cov;

    public TransferCoverageStatistics(Configuration pConfig, LogManager pLogger,
        CoverageData pCov) throws InvalidConfigurationException {

      super(pConfig, pLogger);
      cov = pCov;
    }

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      report.writeCoverageReport(pOut, cov);
    }

    @Override
    public String getName() {
      return String.format("Code Coverage (Mode: Transfer)");
    }
  }
}
