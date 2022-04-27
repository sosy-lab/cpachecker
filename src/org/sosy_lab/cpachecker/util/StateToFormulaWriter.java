// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Verify.verify;
import static org.sosy_lab.cpachecker.util.AbstractStates.asIterable;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.filterLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.projectToType;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * This class allows to export the information of abstract states as SMT-formula. Therefore we
 * filter the abstract states for matching {@link FormulaReportingState}s and retrieve the formula
 * from there. Then we export the formulas in a fixed line-based format, which allows re-usage with
 * a further predicate analysis.
 *
 * <p>The solver used for the formula-creation is configured as usual (including options like
 * 'encodeBitVectorAs={Int,BV}'). Each abstract state is responsible for correct C-types in its own
 * formula and thus there can be wrong/imprecise bitvector-operations in the formula, for example,
 * every variable can be handled as 'signed integer'.
 */
@Options(prefix = "statesToFormulas")
public class StateToFormulaWriter implements StatisticsProvider {

  @Option(
      secure = true,
      description =
          "export abstract states as formula, e.g. for re-using them as PredicatePrecision.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportFile = null; // default is null to disable unwanted output of big files

  @Option(
      secure = true,
      description =
          "instead of writing the exact state-representation as a single formula, write its atoms"
              + " as a list of formulas. Therefore we ignore operators for conjunction and"
              + " disjunction.")
  private FormulaSplitter splitFormulas = FormulaSplitter.LOCATION;

  @Option(
      secure = true,
      description =
          "export formulas for all program locations or just the important locations,"
              + "which include loop-heads, funtion-calls and function-exits.")
  private boolean exportOnlyImporantLocations = false;

  private static final Splitter LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings();
  private static final Joiner LINE_JOINER = Joiner.on('\n');

  private final FormulaManagerView fmgr;
  private final LogManager logger;
  private final CFA cfa;

  public enum FormulaSplitter {
    LOCATION, // one formula per location: "(a=2&b=3)|(a=2&b=4)"
    STATE, // one formula per state: "a=2&b=3", "a=2&b=4"
    ATOM // really split into atoms: "a=2", "b=3", "b=4"
  }

  public StateToFormulaWriter(
      Configuration config, LogManager pLogger, ShutdownNotifier shutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    cfa = pCfa;
    if (exportFile != null) {
      @SuppressWarnings("resource")
      Solver solver = Solver.create(config, pLogger, shutdownNotifier);
      fmgr = solver.getFormulaManager();
    } else {
      fmgr = null;
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (exportFile != null) {
      pStatsCollection.add(
          new Statistics() {

            @Override
            public void printStatistics(
                PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {}

            @Override
            public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
              verify(fmgr != null);
              try (Writer w = IO.openOutputFile(exportFile, Charset.defaultCharset())) {
                write(pReached, w);
              } catch (IOException e) {
                logger.logUserException(Level.WARNING, e, "Could not write formulas to file");
              }
            }

            @Override
            public String getName() {
              return null;
            }
          });
    }
  }

  /** write all formulas for the whole reached-set. */
  private void write(UnmodifiableReachedSet pReachedSet, Appendable pAppendable)
      throws IOException {
    SetMultimap<CFANode, FormulaReportingState> locationPredicates = HashMultimap.create();
    for (AbstractState state : pReachedSet) {
      CFANode location = extractLocation(state);
      if (location != null && isImportantNode(location)) {
        FluentIterable<FormulaReportingState> formulaState =
            asIterable(state).filter(FormulaReportingState.class);
        locationPredicates.putAll(location, formulaState);
      }
    }
    write(locationPredicates, pAppendable);
  }

  /**
   * Filter important program locations. In some cases we re-use only states at abstraction
   * locations, which include loop-starts and function calls. We use a simple filter-mechanism to
   * export only them!
   */
  private boolean isImportantNode(CFANode location) {
    if (exportOnlyImporantLocations) {
      return (cfa.getAllLoopHeads().isPresent()
              && cfa.getAllLoopHeads().orElseThrow().contains(location))
          || location instanceof FunctionEntryNode
          || location instanceof FunctionExitNode
          || location.getLeavingSummaryEdge() != null
          || location.getEnteringSummaryEdge() != null;
    } else {
      // all locations are important
      return true;
    }
  }

  /** write all formulas for a single program location. */
  @SuppressWarnings("unused") // Could be useful if this class was used outside statistics
  private void write(
      final CFANode pCfaNode, UnmodifiableReachedSet pReachedSet, Appendable pAppendable)
      throws IOException {
    SetMultimap<CFANode, FormulaReportingState> statesToNode = HashMultimap.create();
    statesToNode.putAll(
        pCfaNode,
        projectToType(filterLocation(pReachedSet, pCfaNode), FormulaReportingState.class));
    write(statesToNode, pAppendable);
  }

  private void write(SetMultimap<CFANode, FormulaReportingState> pStates, Appendable pAppendable)
      throws IOException {

    // (global) definitions used for predicates
    final Set<String> definitions = new LinkedHashSet<>();

    // in this set, we collect the string representing each predicate
    // (potentially making use of the above definitions)
    final Multimap<CFANode, String> cfaNodeToPredicate = HashMultimap.create();

    // fill the above set and map
    for (CFANode cfaNode : pStates.keySet()) {
      getFormulasForNode(pStates.get(cfaNode))
          .forEach(
              f -> extractPredicatesAndDefinitions(cfaNode, definitions, cfaNodeToPredicate, f));
    }

    writeFormulas(pAppendable, definitions, cfaNodeToPredicate);
  }

  private Stream<BooleanFormula> getFormulasForNode(Set<FormulaReportingState> states) {
    final BooleanFormulaManagerView bfmgr = fmgr.getBooleanFormulaManager();

    Stream<BooleanFormula> formulas =
        states.stream().map(state -> state.getFormulaApproximation(fmgr));

    switch (splitFormulas) {
      case LOCATION:
        // create the disjunction of the found states for the current location
        formulas = Stream.of(formulas.collect(bfmgr.toDisjunction()));
        break;
      case STATE:
        // do not merge different location-formulas, nothing to do
        break;
      case ATOM:
        // atomize formulas
        formulas = formulas.flatMap(f -> fmgr.extractAtoms(f, false).stream());
        break;
      default:
        throw new AssertionError("unknown option");
    }

    // filter out formulas with no information
    return formulas.filter(f -> !bfmgr.isTrue(f) && !bfmgr.isFalse(f));
  }

  /**
   * dump a formula and split it into the predicate and some utility-stuff (named definition) that
   * consists of symbol-declarations and solver-specific queries.
   */
  private void extractPredicatesAndDefinitions(
      CFANode cfaNode,
      Set<String> definitions,
      Multimap<CFANode, String> cfaNodeToPredicate,
      BooleanFormula predicate) {

    List<String> lines = LINE_SPLITTER.splitToList(fmgr.dumpFormula(predicate).toString());

    // Get the predicate from the last line
    String predString = lines.get(lines.size() - 1);

    // Check that the dump format is correct
    if (!(predString.startsWith("(assert ") && predString.endsWith(")"))) {
      throw new AssertionError(
          "Writing formulas is only supported for solvers "
              + "that support the Smtlib2 format, please try using Mathsat5.");
    }

    // Add the definition part of the dump to the set of definitions
    definitions.addAll(lines.subList(0, lines.size() - 1));

    // Record the predicate to write it later at t
    cfaNodeToPredicate.put(cfaNode, predString);
  }

  /**
   * write the definitions and predicates in the commonly used precision-format (that is defined
   * somewhere else...)
   */
  private void writeFormulas(
      Appendable pAppendable, Set<String> definitions, Multimap<CFANode, String> cfaNodeToPredicate)
      throws IOException {

    // write definitions to file
    LINE_JOINER.appendTo(pAppendable, definitions);
    pAppendable.append("\n\n");

    // write states to file
    for (CFANode cfaNode : cfaNodeToPredicate.keySet()) {
      pAppendable.append(toKey(cfaNode));
      pAppendable.append(":\n");
      LINE_JOINER.appendTo(pAppendable, cfaNodeToPredicate.get(cfaNode));
      pAppendable.append("\n\n");
    }
  }

  private static String toKey(CFANode pCfaNode) {
    return pCfaNode.getFunctionName() + " " + pCfaNode;
  }
}
