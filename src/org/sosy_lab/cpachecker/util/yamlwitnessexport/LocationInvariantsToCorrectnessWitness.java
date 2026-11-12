// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState.ReportingMethodNotImplementedException;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.util.witnesses.RelevantArgStatesCollector;

/**
 * Exports the correctness witness of an analysis that records the states it reaches at program
 * locations instead of building an ARG, for example {@link
 * org.sosy_lab.cpachecker.core.algorithm.simplerules.SimpleRulesAlgorithm} and {@link
 * org.sosy_lab.cpachecker.cpa.interpreter.InterpreterCPA}.
 *
 * <p>Such an analysis collects its proof while it runs and passes the recorded states to {@link
 * #exportInvariants}, which over-approximates the states of a location into the invariant of that
 * location. The recorded states are independent snapshots without parent links, so this exporter
 * never reports anything that needs the shape of an ARG, in particular no function contract.
 */
public class LocationInvariantsToCorrectnessWitness extends ARGToCorrectnessWitnessV2 {

  private final CorrectnessWitnessExportOptions options;
  private final RecordedStates recorded;
  private final LocationStateFactory locations;

  public LocationInvariantsToCorrectnessWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    this(pConfig, pCfa, pSpecification, pLogger, new RecordedStates());
  }

  private LocationInvariantsToCorrectnessWitness(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      RecordedStates pRecorded)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger, pRecorded);
    recorded = pRecorded;
    locations = new LocationStateFactory(pCfa, AnalysisDirection.FORWARD, pConfig);
    options = new CorrectnessWitnessExportOptions(pConfig);
  }

  /** Whether {@link #exportInvariants} writes a witness at all. */
  public boolean isExportEnabled() {
    return options.hasYamlWitnessExport();
  }

  /**
   * Write the correctness witness of an analysis that has proven the program safe.
   *
   * @param pInvariants the recorded states of each location; the disjunction of the states of a
   *     location has to hold whenever the program is at that location
   * @param pReached the reached set of the analysis, used only to report the likely quality of the
   *     witness (cf. option {@code witness.yamlexporter.analyseWitnessQuality})
   */
  public void exportInvariants(
      Multimap<CFANode, ImmutableList<ExpressionTreeReportingState>> pInvariants,
      UnmodifiableReachedSet pReached)
      throws InterruptedException, IOException, ReportingMethodNotImplementedException {
    if (!isExportEnabled()) {
      return;
    }
    recorded.states.clear();
    for (var entry : pInvariants.entries()) {
      recorded.states.put(
          entry.getKey(),
          new ARGState(
              new CompositeState(
                  ImmutableList.<AbstractState>builder()
                      .add(locations.getState(entry.getKey()))
                      .addAll(entry.getValue())
                      .build()),
              null));
    }
    // The root is only the entry of the program: there is no ARG that could be traversed from it.
    export(
        new ARGState(locations.getState(cfa.getMainFunction()), null),
        pReached,
        options.getYamlWitnessPathTemplate());
  }

  /** Reports the recorded states, which have no ARG edges that could be explored. */
  private static class RecordedStates implements RelevantArgStatesCollector {

    private final Multimap<CFANode, ARGState> states = LinkedHashMultimap.create();

    @Override
    public CollectedARGStates getRelevantStates(ARGState pRootState) {
      ImmutableListMultimap.Builder<CFANode, ARGState> loops = ImmutableListMultimap.builder();
      ImmutableListMultimap.Builder<CFANode, ARGState> calls = ImmutableListMultimap.builder();
      ImmutableListMultimap.Builder<FunctionEntryNode, ARGState> entries =
          ImmutableListMultimap.builder();
      for (var entry : states.entries()) {
        CFANode node = entry.getKey();
        if (node.isLoopStart()) {
          loops.put(node, entry.getValue());
        }
        if (node.getNumLeavingEdges() == 1 && node.getLeavingEdge(0) instanceof FunctionCallEdge) {
          calls.put(node, entry.getValue());
        }
        if (node instanceof FunctionEntryNode functionEntry) {
          // Function contracts are not part of every witness version and are not exported by
          // default, so the entry is also reported as a location invariant at the function body.
          calls.put(node, entry.getValue());
          entries.put(functionEntry, entry.getValue());
        }
      }
      return new CollectedARGStates(
          loops.build(), calls.build(), entries.build(), ImmutableListMultimap.of());
    }
  }
}
