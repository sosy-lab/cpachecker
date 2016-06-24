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
package org.sosy_lab.cpachecker.util.cwriter;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CExpressionReportingState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

@Options(prefix="cinvariants")
public class CExpressionInvariantExporter {

  @Option(secure=true, description="Output an "
      + "input file, with invariants embedded as assume constraints.")
  private boolean export = false;

  @Option(secure=true, description="Prefix to add to an output file, which would contain "
          + "assumed invariants. If the prefix is equal to '-', the output "
          + "would be printed to stdout.")
  private String prefix="inv";

  public CExpressionInvariantExporter(Configuration pConfiguration)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
  }

  /**
   * Export invariants extracted from {@code pReachedSet} into the file
   * specified by the options as {@code __VERIFIER_assume()} calls,
   * intermixed with the program source code.
   */
  public void exportInvariant(
      String analyzedPrograms,
      ReachedSet pReachedSet,
      PrintStream out) throws IOException {

    if (!export) {
      return;
    }

    Splitter commaSplitter = Splitter.on(',').omitEmptyStrings().trimResults();
    List<String> programs = commaSplitter.splitToList(analyzedPrograms);
    boolean writeToStream = prefix.equals("-");

    for (String program : programs) {
      Appendable output;
      if (writeToStream) {
        output = out;
      } else {
        String trimmedFilename = Paths.get(program).getFileName().toString();
        String trimmedPrefix = Paths.get(prefix).getFileName().toString();
        Path outputDir = Paths.get("output");
        Path prefixedFilename = Paths.get(trimmedPrefix + trimmedFilename);
        Path outPath = outputDir.resolve(prefixedFilename);
        output = MoreFiles.openOutputFile(outPath, Charset.defaultCharset());
      }

      writeProgramWithInvariants(output, program, pReachedSet);
      if (!writeToStream) {
        ((Writer) output).close();
      }
    }
  }

  private void writeProgramWithInvariants(
      Appendable out,
      String filename,
      ReachedSet pReachedSet
      )
      throws IOException {
    String sourceCode = MoreFiles.toString(Paths.get(filename), Charset.defaultCharset());

    List<String> lines = Splitter.on('\n').splitToList(sourceCode);

    Multimap<Integer, CExpressionReportingState> reporting =
        getReportingStatesForFile(pReachedSet, filename);

    int lineNo = 0;
    for (String line : lines) {
      String invariant = getInvariantForLine(lineNo, reporting);
      if (!invariant.isEmpty()) {
        out.append("__VERIFIER_assume(")
           .append(invariant)
           .append(");\n");
      }
      out.append(line)
         .append('\n');
      lineNo++;
    }
  }

  private String getInvariantForLine(
      int lineNo, Multimap<Integer, CExpressionReportingState> reporting) {
    Collection<CExpressionReportingState> report = reporting.get(lineNo);
    return Joiner.on(" || ").join(report.stream().map(c -> c.reportInvariantAsCExpression()).iterator());
  }

  private Multimap<Integer, CExpressionReportingState> getReportingStatesForFile(
      ReachedSet pReachedSet,
      String filename) {

    Multimap<Integer, CExpressionReportingState> out = HashMultimap.create();
    for (AbstractState state : pReachedSet) {

      CFANode loc = AbstractStates.extractLocation(state);
      if (loc != null && loc.getNumEnteringEdges() > 0) {
        CFAEdge edge = loc.getEnteringEdge(0);
        FileLocation location = edge.getFileLocation();
        FluentIterable<CExpressionReportingState> reporting =
            AbstractStates.asIterable(state)
                .filter(CExpressionReportingState.class);
        if (location.getFileName().equals(filename) && !reporting.isEmpty()) {
          out.putAll(location.getStartingLineInOrigin(), reporting);
        }
      }
    }
    return out;
  }

}
