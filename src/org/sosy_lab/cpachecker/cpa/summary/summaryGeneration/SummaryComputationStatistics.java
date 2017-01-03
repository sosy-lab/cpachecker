/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.summary.summaryGeneration;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;

/**
 * Statistics on summary generation.
 */
@Options(prefix="cpa.summary")
public class SummaryComputationStatistics implements Statistics {

  @Option(secure=true, name="file",
      description="export summary requests as a .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dotFile = Paths.get("SummaryRequests.dot");

  private final Multimap<String, Summary> computedSummaries;

  SummaryComputationStatistics(
      Multimap<String, Summary> pComputedSummaries,
      Configuration pConfiguration) throws InvalidConfigurationException {
    computedSummaries = Multimaps.unmodifiableMultimap(pComputedSummaries);
    pConfiguration.inject(this);
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {

    out.println("Summaries generated: ");
    out.println(
        computedSummaries.asMap().entrySet().stream().map(
            e -> e.getKey() + ":\n\n" + Joiner.on("\n*").join(
                e.getValue()
            )
        ).reduce(String::concat)
    );

    if (dotFile != null) {
      try {
        try (Writer w = MoreFiles.openOutputFile(dotFile, Charset.defaultCharset())) {
          SummaryGenToDotWriter.write(w, (SummaryComputationState) reached.getFirstState());
        }
      } catch (IOException pE) {
        out.println("Failed writing dotfile");
      }
    }
  }

  @Override
  public String getName() {
    return "SummaryGenerationCPA";
  }
}
