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

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.predicates.weakening.InductiveWeakeningManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

@Options(prefix="cinvariants")
public class CExpressionInvariantExporter {

  @Option(secure=true, description="Attempt to simplify the invariant before "
      + "exporting [may be very expensive].")
  private boolean simplify = false;

  private final PathTemplate prefix;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final FormulaToCExpressionConverter formulaToCExpressionConverter;
  private final InductiveWeakeningManager inductiveWeakeningManager;

  public CExpressionInvariantExporter(
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      PathTemplate pPrefix)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    prefix = pPrefix;
    Solver solver = Solver.create(
        pConfiguration,
        pLogManager,
        pShutdownNotifier);
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    formulaToCExpressionConverter = new FormulaToCExpressionConverter(fmgr);
    inductiveWeakeningManager = new InductiveWeakeningManager(
        pConfiguration,
        solver, pLogManager, pShutdownNotifier);
  }

  /**
   * Export invariants extracted from {@code pReachedSet} into the file
   * specified by the options as {@code __VERIFIER_assume()} calls,
   * intermixed with the program source code.
   */
  public void exportInvariant(
      String analyzedPrograms,
      UnmodifiableReachedSet pReachedSet) throws IOException, InterruptedException {

    Splitter commaSplitter = Splitter.on(',').omitEmptyStrings().trimResults();
    List<String> programs = commaSplitter.splitToList(analyzedPrograms);

    for (String program : programs) {
      // Grab only the last component of the program filename.
      Path trimmedFilename = Paths.get(program).getFileName();
      if (trimmedFilename != null) {
        try (Writer output =
            MoreFiles.openOutputFile(
                prefix.getPath(trimmedFilename.toString()), Charset.defaultCharset())) {
          writeProgramWithInvariants(output, program, pReachedSet);
        }
      }
    }
  }

  private void writeProgramWithInvariants(
      Appendable out, String filename, UnmodifiableReachedSet pReachedSet)
      throws IOException, InterruptedException {

    Map<Integer, BooleanFormula> reporting = getInvariantsForFile(pReachedSet, filename);

    try (Stream<String> lines = Files.lines(Paths.get(filename))) {
      int lineNo = 0;
      Iterator<String> it = lines.iterator();
      while (it.hasNext()) {
        String line = it.next();
        Optional<String> invariant = getInvariantForLine(lineNo, reporting);
        if (invariant.isPresent()) {
          out.append("__VERIFIER_assume(")
              .append(invariant.get())
              .append(");\n");
        }
        out.append(line)
            .append('\n');
        lineNo++;
      }
    }
  }

  private Optional<String> getInvariantForLine(
      int lineNo, Map<Integer, BooleanFormula> reporting) throws InterruptedException{
    BooleanFormula formula = reporting.get(lineNo);
    if (formula == null) {
      return Optional.empty();
    }
    if (simplify) {
      formula = simplifyInvariant(formula);
    }
    return Optional.of(formulaToCExpressionConverter.formulaToCExpression(formula));
  }

  /**
   * @return Mapping from line numbers to states associated with the given line.
   */
  private Map<Integer, BooleanFormula> getInvariantsForFile(
      UnmodifiableReachedSet pReachedSet,
      String filename) {

    // One formula per reported state.
    Multimap<Integer, BooleanFormula> byState = HashMultimap.create();

    for (AbstractState state : pReachedSet) {

      CFANode loc = AbstractStates.extractLocation(state);
      if (loc != null && loc.getNumEnteringEdges() > 0) {
        CFAEdge edge = loc.getEnteringEdge(0);
        FileLocation location = edge.getFileLocation();
        FluentIterable<FormulaReportingState> reporting =
            AbstractStates.asIterable(state).filter(FormulaReportingState.class);

        if (location.getFileName().equals(filename) && !reporting.isEmpty()) {
          BooleanFormula reported = bfmgr.and(
              reporting.transform(s -> s.getFormulaApproximation(fmgr)).toList());
          byState.put(location.getStartingLineInOrigin(), reported);
        }
      }
    }
    return Maps.transformValues(
        byState.asMap(), invariants -> bfmgr.or(invariants)
    );
  }

  private BooleanFormula simplifyInvariant(BooleanFormula pInvariant)
      throws InterruptedException {
    return inductiveWeakeningManager.removeRedundancies(pInvariant);
  }
}
