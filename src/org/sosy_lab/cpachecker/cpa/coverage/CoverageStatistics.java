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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;
import org.sosy_lab.cpachecker.util.coverage.CoverageReportGcov;
import org.sosy_lab.cpachecker.util.coverage.CoverageReportStdoutSummary;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

@Options
public class CoverageStatistics extends AbstractStatistics {

  @Option(secure=true, name="coverage.file",
      description="print coverage info to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputCoverageFile = Paths.get("coverage.info");

  private final LogManager logger;
  private final CoverageData cov;

  public CoverageStatistics(Configuration pConfig, LogManager pLogger, CoverageData pCov)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    this.logger = pLogger;
    this.cov = pCov;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    CoverageReportStdoutSummary.write(cov, pOut);

    if (outputCoverageFile != null) {
      try (Writer w = IO.openOutputFile(outputCoverageFile, Charset.defaultCharset())) {
        CoverageReportGcov.write(cov, w);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write coverage information to file");
      }
    }
  }

  @Override
  public String getName() {
    return String.format("Code Coverage (Mode: Transfer)");
  }
}
