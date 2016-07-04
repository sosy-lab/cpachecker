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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CExpressionReportingState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

@Options(prefix="cinvariants")
public class CExpressionInvariantExporter {

  @Option(secure=true, description="Output an "
      + "input file, with invariants embedded as assume constraints.")
  private boolean export = false;

  @Option(secure=true, description="Prefix to add to an output file, which would contain "
          + "assumed invariants. Ignored if |writeToStats| is set to |true|")
  @FileOption(Type.OUTPUT_FILE)
  private PathTemplate prefix = PathTemplate.ofFormatString("inv-%s");

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
      ReachedSet pReachedSet) throws IOException {

    if (!export) {
      return;
    }

    Splitter commaSplitter = Splitter.on(',').omitEmptyStrings().trimResults();
    List<String> programs = commaSplitter.splitToList(analyzedPrograms);

    for (String program : programs) {
        // Grab only the last component of the program filename.
        String trimmedFilename = checkNotNull(
            Paths.get(program).getFileName()).toString();
      try (Writer output =
          MoreFiles.openOutputFile(prefix.getPath(trimmedFilename), Charset.defaultCharset())) {
        writeProgramWithInvariants(output, program, pReachedSet);
      }
    }
  }

  private void writeProgramWithInvariants(
      Appendable out,
      String filename,
      ReachedSet pReachedSet
      )
      throws IOException {

    Multimap<Integer, CExpressionReportingState> reporting =
        getReportingStatesForFile(pReachedSet, filename);

    try (Stream<String> lines = Files.lines(Paths.get(filename))) {
      int lineNo = 0;
      Iterator<String> it = lines.iterator();
      while (it.hasNext()) {
        String line = it.next();
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
  }

  private String getInvariantForLine(
      int lineNo, Multimap<Integer, CExpressionReportingState> reporting) {
    Collection<CExpressionReportingState> report = reporting.get(lineNo);
    return Joiner.on("\n || ").join(
        report.stream().map(c -> c.reportInvariantAsCExpression()).iterator());
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
