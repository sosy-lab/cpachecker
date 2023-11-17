// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.terminationviamemory;

import static com.google.common.base.Preconditions.checkState;
import static java.util.logging.Level.WARNING;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationStatistics;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessToOutputFormatsUtils;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

@Options(prefix = "terminationtoreach")
public class TerminationToReachStatistics extends TerminationStatistics implements Statistics {
  @Option(
      secure = true,
      name = "violation.witness",
      description = "Export termination counterexample to file as GraphML automaton ")
  @FileOption(Type.OUTPUT_FILE)
  private Path violationWitness = Path.of("nontermination_witness.graphml");

  @Option(
      secure = true,
      name = "violation.witness.dot",
      description = "Export termination counterexample to file as dot/graphviz automaton ")
  @FileOption(Type.OUTPUT_FILE)
  private Path violationWitnessDot = Path.of("nontermination_witness.dot");

  @Option(
      secure = true,
      name = "compressWitness",
      description = "compress the produced violation-witness automata using GZIP compression.")
  private boolean compressWitness = true;
  private ImmutableSet<Loop> nonterminatingLoops = null;

  public TerminationToReachStatistics(
      Configuration pConfig, LogManager pLogger, CFA pCFA)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pCFA);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    if (pResult == Result.FALSE && (violationWitness != null || violationWitnessDot != null)) {
      Iterator<ARGState> violations =
          pReached.stream()
              .filter(AbstractStates::isTargetState)
              .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
              .filter(s -> s.getCounterexampleInformation().isPresent())
              .iterator();
      Preconditions.checkState(violations.hasNext());
      exportTrivialWitness((ARGState) pReached.getFirstState(), violations.next());
      Preconditions.checkState(!violations.hasNext());
    }
  }

  @Override
  public String getName() {
    return null;
  }

  void setNonterminatingLoop(ImmutableSet<Loop> pLoop) {
    checkState(nonterminatingLoops == null);
    checkState(pLoop != null);
    nonterminatingLoops = pLoop;
  }

  private void exportTrivialWitness(final ARGState root, final ARGState loopStart) {
    CounterexampleInfo cexInfo = loopStart.getCounterexampleInformation().orElseThrow();

    ARGState loopStartInCEX =
        new ARGState(AbstractStates.extractStateByType(loopStart, LocationState.class), null);

    ARGState newRoot = new ARGState(root.getWrappedState(), null);
    Collection<ARGState> cexStates =
        copyStem(cexInfo.getTargetPath(), newRoot, loopStart, loopStartInCEX);

    ExpressionTree<Object> quasiInvariant = ExpressionTrees.getTrue();

    Function<? super ARGState, ExpressionTree<Object>> provideQuasiInvariant =
        (ARGState argState) -> {
          if (Objects.equals(argState, loopStartInCEX)) {
            return quasiInvariant;
          }
          return ExpressionTrees.getTrue();
        };

    for (Loop loop : nonterminatingLoops) {
      cexStates.addAll(addCEXLoopingPartToARG(loopStartInCEX, loop));
    }

    Predicate<? super ARGState> relevantStates = Predicates.in(cexStates);

    try {
      final Witness witness =
          witnessExporter.generateTerminationErrorWitness(
              newRoot,
              relevantStates,
              BiPredicates.bothSatisfy(relevantStates),
              state -> Objects.equals(state, loopStartInCEX),
              provideQuasiInvariant);

      if (violationWitness != null) {
        WitnessToOutputFormatsUtils.writeWitness(
            violationWitness,
            compressWitness,
            pAppendable -> WitnessToOutputFormatsUtils.writeToGraphMl(witness, pAppendable),
            logger);
      }

      if (violationWitnessDot != null) {
        WitnessToOutputFormatsUtils.writeWitness(
            violationWitnessDot,
            compressWitness,
            pAppendable -> WitnessToOutputFormatsUtils.writeToDot(witness, pAppendable),
            logger);
      }
    } catch (InterruptedException e) {
      logger.logUserException(
          WARNING, e, "Could not export termination witness due to interruption");
    }
  }
}
