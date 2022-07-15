// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;
import org.sosy_lab.cpachecker.util.coverage.CoverageReportGcov;
import org.sosy_lab.cpachecker.util.coverage.CoverageReportStdoutSummary;

@Options
public class CoverageStatistics implements Statistics {

  @Option(secure = true, name = "coverage.file", description = "print coverage info to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputCoverageFile = Path.of("coverage.info");

  private final LogManager logger;
  private final CoverageData cov;

  public CoverageStatistics(Configuration pConfig, LogManager pLogger, CoverageData pCov)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    logger = pLogger;
    cov = pCov;
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
    return "Code Coverage (Mode: Transfer)";
  }
}
