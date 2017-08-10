/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.coverage;

import com.google.common.collect.Lists;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;

/**
 * Class responsible for extracting coverage information from ReachedSet and CFA
 * and writing it into a file.
 */
@Options
public class CoverageReport {

  @Option(secure=true,
      name="coverage.enabled",
      description="Compute and export information about the verification coverage?")
  private boolean enabled = true;

  @Option(secure=true,
      name="coverage.file",
      description="print coverage info to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputCoverageFile = Paths.get("coverage.info");

  private final Collection<CoverageWriter> reportWriters;

  public CoverageReport(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {

    pConfig.inject(this);

    this.reportWriters = Lists.newArrayList();
    this.reportWriters.add(new CoverageReportGcov(pConfig, pLogger));
    this.reportWriters.add(new CoverageReportStdoutSummary(pConfig));

  }

  public void writeCoverageReport(
      final PrintStream pStatisticsOutput,
      final CoverageCollector coverageCollector) {
    if (!enabled) {
      return;
    }

    Map<String, FileCoverageInformation> infosPerFile = coverageCollector.collectCoverage();

    for (CoverageWriter w: reportWriters) {
      w.write(infosPerFile, pStatisticsOutput, outputCoverageFile);
    }

  }
}
