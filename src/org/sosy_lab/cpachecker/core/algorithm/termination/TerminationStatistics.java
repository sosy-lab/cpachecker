// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.valueWithPercentage;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.GeometricNonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.InfiniteFixpointRepetition;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.ConstantTerm;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.LassoAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankVar;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessToOutputFormatsUtils;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "termination")
public class TerminationStatistics extends LassoAnalysisStatistics {

  @Option(
      secure = true,
      description =
          "A human readable representation of the synthesized (non-)termination arguments is "
              + "exported to this file.")
  @FileOption(Type.OUTPUT_FILE)
  private Path resultFile = Path.of("terminationAnalysisResult.txt");

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

  private final int totalLoops;

  private final Set<Loop> analysedLoops = Sets.newConcurrentHashSet();

  private final Timer totalTime = new Timer();

  private final Timer loopTime = new Timer();

  private final Timer recursionTime = new Timer();

  private final Timer safetyAnalysisTime = new Timer();

  private final Multiset<Loop> safetyAnalysisRunsPerLoop = ConcurrentHashMultiset.create();

  private final LogManager logger;

  private final WitnessExporter witnessExporter;
  private final LocationStateFactory locFac;
  private @Nullable Loop nonterminatingLoop = null;

  public TerminationStatistics(
      Configuration pConfig, LogManager pLogger, int pTotalNumberOfLoops, CFA pCFA)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = checkNotNull(pLogger);
    totalLoops = pTotalNumberOfLoops;

    witnessExporter =
        new WitnessExporter(
            pConfig,
            pLogger,
            Specification.alwaysSatisfied()
                .withAdditionalProperties(ImmutableSet.of(CommonVerificationProperty.TERMINATION)),
            pCFA);
    locFac = new LocationStateFactory(pCFA, AnalysisDirection.FORWARD, pConfig);
  }

  void algorithmStarted() {
    totalTime.start();
  }

  void algorithmFinished() {
    totalTime.stop();
    safetyAnalysisTime.stopIfRunning();
    lassoTime.stopIfRunning();
    loopTime.stopIfRunning();
  }

  void analysisOfLoopStarted(Loop pLoop) {
    boolean newLoop = analysedLoops.add(pLoop);
    checkState(newLoop);
    loopTime.start();
  }

  void analysisOfLoopFinished(Loop pLoop) {
    checkState(analysedLoops.contains(pLoop));
    loopTime.stop();
    recursionTime.stopIfRunning();
    safetyAnalysisTime.stopIfRunning();
    lassoTime.stopIfRunning();
    lassoConstructionTime.stopIfRunning();
    lassoNonTerminationTime.stopIfRunning();
    lassoTerminationTime.stopIfRunning();
  }

  void analysisOfRecursionStarted() {
    recursionTime.start();
  }

  void analysisOfRecursionFinished() {
    recursionTime.stop();
  }

  void safetyAnalysisStarted(Loop pLoop) {
    checkState(analysedLoops.contains(pLoop));
    safetyAnalysisRunsPerLoop.add(pLoop);
    safetyAnalysisTime.start();
  }

  void safetyAnalysisFinished(Loop pLoop) {
    checkState(analysedLoops.contains(pLoop));
    checkState(safetyAnalysisRunsPerLoop.contains(pLoop));
    safetyAnalysisTime.stop();
  }

  void setNonterminatingLoop(Loop pLoop) {
    checkState(nonterminatingLoop == null);
    checkState(pLoop != null);
    nonterminatingLoop = pLoop;
  }

  @Override
  public void synthesizedTerminationArgument(Loop pLoop, TerminationArgument pTerminationArgument) {
    checkState(analysedLoops.contains(pLoop));
    super.synthesizedTerminationArgument(pLoop, pTerminationArgument);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println("Total time :                                        " + totalTime);
    pOut.println("Time for recursion analysis:                        " + recursionTime);
    pOut.println();

    int loops = analysedLoops.size();
    pOut.println(
        "Number of analysed loops:                               "
            + valueWithPercentage(loops, totalLoops));
    pOut.println("Total time for loop analysis:                       " + loopTime);
    pOut.println(
        "  Avg time per loop analysis:                       " + format(loopTime.getAvgTime()));
    pOut.println(
        "  Max time per loop analysis:                       " + format(loopTime.getMaxTime()));
    pOut.println();

    int safetyAnalysisRuns = safetyAnalysisRunsPerLoop.size();
    assert safetyAnalysisRuns == safetyAnalysisTime.getNumberOfIntervals();
    int maxSafetyAnalysisRuns =
        safetyAnalysisRunsPerLoop.entrySet().stream()
            .mapToInt(Multiset.Entry::getCount)
            .max()
            .orElse(0);
    String loopsWithMaxSafetyAnalysisRuns =
        safetyAnalysisRunsPerLoop.entrySet().stream()
            .filter(e -> e.getCount() == maxSafetyAnalysisRuns)
            .map(Multiset.Entry::getElement)
            .map(l -> l.getLoopHeads().toString())
            .collect(Collectors.joining(", "));
    pOut.println(
        "Number of safety analysis runs:                     " + format(safetyAnalysisRuns));
    if (loops > 0) {
      pOut.println(
          "  Avg safety analysis run per loop:                 " + div(safetyAnalysisRuns, loops));
    }
    pOut.println(
        "  Max safety analysis run per loop:                 "
            + format(maxSafetyAnalysisRuns)
            + " \t for loops "
            + loopsWithMaxSafetyAnalysisRuns);

    pOut.println("Total time for safety analysis:                     " + safetyAnalysisTime);
    pOut.println(
        "  Avg time per safety analysis run:                 "
            + format(safetyAnalysisTime.getAvgTime()));
    pOut.println(
        "  Max time per safety analysis run:                 "
            + format(safetyAnalysisTime.getMaxTime()));
    pOut.println();

    int iterations = lassoTime.getNumberOfIntervals();
    int lassos = lassosPerLoop.size();
    int maxLassosPerLoop =
        lassosPerLoop.entrySet().stream().mapToInt(Multiset.Entry::getCount).max().orElse(0);
    String loopsWithMaxLassos =
        lassosPerLoop.entrySet().stream()
            .filter(e -> e.getCount() == maxLassosPerLoop)
            .map(Multiset.Entry::getElement)
            .map(l -> l.getLoopHeads().toString())
            .collect(Collectors.joining(", "));
    pOut.println("Number of analysed lassos:                          " + format(lassos));
    if (loops > 0) {
      pOut.println("  Avg number of lassos per loop:                    " + div(lassos, loops));
    }
    pOut.println(
        "  Max number of lassos per loop:                    "
            + format(maxLassosPerLoop)
            + " \t for loops "
            + loopsWithMaxLassos);
    if (loops > 0) {
      pOut.println(
          "  Avg number of lassos per iteration:               " + div(lassos, iterations));
    }
    pOut.println(
        "  Max number of lassos per iteration:               "
            + format(maxLassosPerIteration.get()));
    pOut.println();

    pOut.println("Total time for lassos analysis:                     " + lassoTime);
    pOut.println(
        "  Avg time per iteration:                           " + format(lassoTime.getAvgTime()));
    pOut.println(
        "  Max time per iteration:                           " + format(lassoTime.getMaxTime()));
    pOut.println("  Time for lassos construction:                     " + lassoConstructionTime);
    pOut.println(
        "    Avg time for lasso construction per iteration:  "
            + format(lassoConstructionTime.getAvgTime()));
    pOut.println(
        "    Max time for lasso construction per iteration:  "
            + format(lassoConstructionTime.getMaxTime()));
    pOut.println(
        "      Time for stem and loop construction:                     "
            + lassoStemLoopConstructionTime);
    pOut.println(
        "        Avg time for stem and loop construction per iteration:  "
            + format(lassoStemLoopConstructionTime.getAvgTime()));
    pOut.println(
        "        Max time for stem and loop construction per iteration:  "
            + format(lassoStemLoopConstructionTime.getMaxTime()));

    pOut.println("      Time for lassos creation:                     " + lassosCreationTime);
    pOut.println(
        "        Avg time for lassos creation per iteration:  "
            + format(lassosCreationTime.getAvgTime()));
    pOut.println(
        "        Max time for lassos creation per iteration:  "
            + format(lassosCreationTime.getMaxTime()));

    pOut.println("  Total time for non-termination analysis:          " + lassoNonTerminationTime);
    pOut.println(
        "    Avg time for non-termination analysis per lasso:"
            + format(lassoNonTerminationTime.getAvgTime()));
    pOut.println(
        "    Max time for non-termination analysis per lasso:"
            + format(lassoNonTerminationTime.getMaxTime()));
    pOut.println("  Total time for termination analysis:              " + lassoTerminationTime);
    pOut.println(
        "    Avg time for termination analysis per lasso:    "
            + format(lassoTerminationTime.getAvgTime()));
    pOut.println(
        "    Max time for termination analysis per lasso:    "
            + format(lassoTerminationTime.getMaxTime()));
    pOut.println();

    int totoalTerminationArguments = terminationArguments.size();
    int maxTerminationArgumentsPerLoop =
        terminationArguments.asMap().values().stream().mapToInt(Collection::size).max().orElse(0);
    String loopsWithMaxTerminationArguments =
        terminationArguments.asMap().entrySet().stream()
            .filter(e -> e.getValue().size() == maxTerminationArgumentsPerLoop)
            .map(Entry::getKey)
            .map(l -> l.getLoopHeads().toString())
            .collect(Collectors.joining(", "));
    pOut.println(
        "Total number of termination arguments:              "
            + format(totoalTerminationArguments));
    if (loops > 0) {
      pOut.println(
          "  Avg termination arguments per loop:               "
              + div(totoalTerminationArguments, loops));
    }
    pOut.println(
        "  Max termination arguments per loop:               "
            + format(maxTerminationArgumentsPerLoop)
            + " \t for loops "
            + loopsWithMaxTerminationArguments);

    pOut.println();
    Map<String, Integer> terminationArguementTypes = new HashMap<>();
    for (TerminationArgument terminationArgument : terminationArguments.values()) {
      String name = terminationArgument.getRankingFunction().getName();
      terminationArguementTypes.merge(name, 1, Integer::sum);
    }

    for (Entry<String, Integer> terminationArgument : terminationArguementTypes.entrySet()) {
      String name = terminationArgument.getKey();
      String whiteSpaces = " ".repeat(49 - name.length());
      pOut.println("  " + name + ":" + whiteSpaces + format(terminationArgument.getValue()));
    }

    exportSynthesizedArguments();

    if (pResult == Result.FALSE && (violationWitness != null || violationWitnessDot != null)) {
      Iterator<ARGState> violations =
          pReached.stream()
              .filter(AbstractStates::isTargetState)
              .map(s -> AbstractStates.extractStateByType(s, ARGState.class))
              .filter(s -> s.getCounterexampleInformation().isPresent())
              .iterator();
      Preconditions.checkState(nonterminatingLoop != null);
      Preconditions.checkState(violations.hasNext());
      exportViolationWitness((ARGState) pReached.getFirstState(), violations.next());
      Preconditions.checkState(!violations.hasNext());
    }
  }

  private void exportSynthesizedArguments() {
    if (resultFile != null) {
      logger.logf(FINER, "Writing result of termination analysis into %s.", resultFile);

      try (Writer writer = IO.openOutputFile(resultFile, StandardCharsets.UTF_8)) {
        writer.append("Non-termination arguments:\n");
        for (Entry<Loop, NonTerminationArgument> nonTerminationArgument :
            nonTerminationArguments.entrySet()) {
          writer.append(nonTerminationArgument.getKey().toString());
          writer.append(":\n");
          writer.append(nonTerminationArgument.getValue().toString());
          writer.append('\n');
        }

        writer.append("\n\nTermination arguments:\n");
        for (Loop loop : terminationArguments.keySet()) {
          for (TerminationArgument terminationArgument : terminationArguments.get(loop)) {
            writer.append(loop.toString());
            writer.append(":\n");
            writer.append(terminationArgument.toString());
            writer.append('\n');
          }
          writer.append('\n');
        }

      } catch (IOException e) {
        logger.logException(WARNING, e, "Could not export (non-)termination arguments.");
      }
    }
  }

  private void exportViolationWitness(final ARGState root, final ARGState loopStart) {
    CounterexampleInfo cexInfo = loopStart.getCounterexampleInformation().orElseThrow();

    ARGState loopStartInCEX =
        new ARGState(AbstractStates.extractStateByType(loopStart, LocationState.class), null);

    ARGState newRoot = new ARGState(root.getWrappedState(), null);
    Collection<ARGState> cexStates =
        copyStem(cexInfo.getTargetPath(), newRoot, loopStart, loopStartInCEX);

    NonTerminationArgument arg = nonTerminationArguments.get(nonterminatingLoop);
    ExpressionTree<Object> quasiInvariant = buildInvariantFrom(arg);

    Function<? super ARGState, ExpressionTree<Object>> provideQuasiInvariant =
        (ARGState argState) -> {
          if (Objects.equals(argState, loopStartInCEX)) {
            return quasiInvariant;
          }
          return ExpressionTrees.getTrue();
        };

    cexStates.addAll(addCEXLoopingPartToARG(loopStartInCEX));

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

  private Collection<ARGState> copyStem(
      final ARGPath pStem,
      final ARGState newRoot,
      final ARGState loopStart,
      final ARGState newLoopStart) {
    Collection<ARGState> newStates = new HashSet<>();
    boolean first = true;
    ARGState child, parent = newRoot;
    newStates.add(newRoot);

    for (ARGState state : pStem.asStatesList()) {
      if (first) {
        first = false;
        continue;
      }

      if (Objects.equals(state, loopStart)) {
        newLoopStart.addParent(parent);
        newStates.add(newLoopStart);
        break;
      }

      child = new ARGState(state.getWrappedState(), parent);
      parent = child;
      newStates.add(parent);
    }

    return newStates;
  }

  private Collection<ARGState> addCEXLoopingPartToARG(final ARGState pLoopEntry) {
    CFANode loc = AbstractStates.extractLocation(pLoopEntry);
    Preconditions.checkState(nonterminatingLoop.getLoopHeads().contains(loc));

    Collection<ARGState> relevantARGStates = new HashSet<>();

    Map<CFANode, ARGState> nodeToARGState =
        Maps.newHashMapWithExpectedSize(nonterminatingLoop.getLoopNodes().size());
    nodeToARGState.put(loc, pLoopEntry);
    Deque<CFANode> waitlist = new ArrayDeque<>();
    waitlist.push(loc);

    ARGState pred, succ;

    CFANode locContinueLoop;
    Pair<CFANode, CallstackState> context, newContext;
    ARGState predFun, succFun;
    Deque<Pair<CFANode, CallstackState>> waitlistFun;
    Map<Pair<CFANode, CallstackState>, ARGState> contextToARGState;

    while (!waitlist.isEmpty()) {
      loc = waitlist.pop();
      pred = nodeToARGState.get(loc);
      assert pred != null;

      for (CFAEdge leave : CFAUtils.leavingEdges(loc)) {
        if (nonterminatingLoop.getLoopNodes().contains(leave.getSuccessor())) {
          succ = nodeToARGState.get(leave.getSuccessor());
          if (succ == null) {
            succ = new ARGState(locFac.getState(leave.getSuccessor()), null);
            nodeToARGState.put(leave.getSuccessor(), succ);
            waitlist.push(leave.getSuccessor());
          }

          succ.addParent(pred);

        } else if (leave instanceof FunctionCallEdge && pred.getChildren().isEmpty()) {
          // function calls are not considered to be part of the loop
          locContinueLoop = ((FunctionCallEdge) leave).getSummaryEdge().getSuccessor();
          contextToARGState = new HashMap<>();
          context =
              Pair.of(
                  leave.getSuccessor(),
                  new CallstackState(
                      null, leave.getSuccessor().getFunctionName(), leave.getPredecessor()));
          waitlistFun = new ArrayDeque<>();
          waitlistFun.push(context);

          succFun = new ARGState(locFac.getState(leave.getSuccessor()), null);
          contextToARGState.put(context, succFun);

          succFun.addParent(pred);

          while (!waitlistFun.isEmpty()) {
            context = waitlistFun.pop();
            predFun = contextToARGState.get(context);
            assert predFun != null;

            for (CFAEdge leaveFun : CFAUtils.leavingEdges(context.getFirst())) {
              newContext = Pair.of(leaveFun.getSuccessor(), context.getSecond());

              if (leaveFun instanceof FunctionReturnEdge) {
                if (!context
                    .getSecond()
                    .getCallNode()
                    .equals(((FunctionReturnEdge) leaveFun).getSummaryEdge().getPredecessor())) {
                  continue; // false context
                }
                newContext =
                    Pair.of(leaveFun.getSuccessor(), context.getSecond().getPreviousState());
              }

              if (leaveFun instanceof FunctionCallEdge) {
                newContext =
                    Pair.of(
                        leaveFun.getSuccessor(),
                        new CallstackState(
                            context.getSecond(),
                            leaveFun.getSuccessor().getFunctionName(),
                            leaveFun.getPredecessor()));
              }

              if (!Objects.equals(leaveFun.getSuccessor(), locContinueLoop)) {
                succFun = contextToARGState.get(newContext);
              } else {
                succFun = nodeToARGState.get(locContinueLoop);
                assert (newContext.getSecond() == null);
              }
              if (succFun == null) {
                succFun = new ARGState(locFac.getState(leaveFun.getSuccessor()), null);
                if (!Objects.equals(leaveFun.getSuccessor(), locContinueLoop)) {
                  contextToARGState.put(newContext, succFun);
                  waitlistFun.push(newContext);
                } else {
                  nodeToARGState.put(leaveFun.getSuccessor(), succFun);
                  waitlist.push(leaveFun.getSuccessor());
                }
              }

              succFun.addParent(predFun);
            }
          }

          assert nodeToARGState.containsKey(locContinueLoop);
          relevantARGStates.addAll(contextToARGState.values());
        }
      }
    }
    relevantARGStates.addAll(nodeToARGState.values());
    return relevantARGStates;
  }

  private ExpressionTree<Object> buildInvariantFrom(NonTerminationArgument pArg) {
    ExpressionTree<Object> computedQuasiInvariant = ExpressionTrees.getTrue();
    if (pArg instanceof GeometricNonTerminationArgument) {
      computedQuasiInvariant = buildInvariantFrom((GeometricNonTerminationArgument) pArg);
    } else if (pArg instanceof InfiniteFixpointRepetition) {
      computedQuasiInvariant = buildInvaraintFrom((InfiniteFixpointRepetition) pArg);
    }
    return computedQuasiInvariant;
  }

  private ExpressionTree<Object> buildInvariantFrom(final GeometricNonTerminationArgument arg) {
    ExpressionTree<Object> result = ExpressionTrees.getTrue();

    if (likelyIsFixpoint(arg)) {
      String varName;
      CLiteralExpression litexpr;

      for (Entry<IProgramVar, Rational> entry : arg.getStateHonda().entrySet()) {
        RankVar rankVar = (RankVar) entry.getKey();
        if (rankVar.getTerm() instanceof ApplicationTerm
            && ((ApplicationTerm) rankVar.getTerm()).getParameters().length != 0) {
          // ignore UFs
          continue;
        }

        varName = toOrigName(entry.getKey().getTermVariable());
        litexpr = literalExpressionFrom(entry.getValue());
        result = And.of(result, LeafExpression.of(buildEquals(varName, litexpr)));
      }
    }

    return result;
  }

  private boolean likelyIsFixpoint(final GeometricNonTerminationArgument arg) {
    Map<IProgramVar, Rational> secondElem = new HashMap<>(arg.getStateHonda());

    for (Map<IProgramVar, Rational> gev : arg.getGEVs()) {
      for (Entry<IProgramVar, Rational> entry : gev.entrySet()) {
        secondElem.put(entry.getKey(), entry.getValue().add(secondElem.get(entry.getKey())));
      }
    }

    for (Entry<IProgramVar, Rational> entry : secondElem.entrySet()) {
      if (!entry.getValue().equals(arg.getStateHonda().get(entry.getKey()))) {
        return false;
      }
    }
    return true;
  }

  private ExpressionTree<Object> buildInvaraintFrom(final InfiniteFixpointRepetition arg) {
    ExpressionTree<Object> result = ExpressionTrees.getTrue();

    String varName;
    CLiteralExpression litexpr;
    Object termVal;

    for (Entry<Term, Term> entry : arg.getValuesAtHonda().entrySet()) {
      if (entry.getKey() instanceof TermVariable && entry.getValue() instanceof ConstantTerm) {
        varName = toOrigName((TermVariable) entry.getKey());
        termVal = ((ConstantTerm) entry.getValue()).getValue();

        if (termVal instanceof BigDecimal) {
          litexpr =
              new CFloatLiteralExpression(
                  FileLocation.DUMMY, CNumericTypes.FLOAT, (BigDecimal) termVal);
        } else if (termVal instanceof BigInteger) {
          litexpr =
              CIntegerLiteralExpression.createDummyLiteral(
                  ((BigInteger) termVal).longValue(), CNumericTypes.INT);
        } else if (termVal instanceof Rational) {
          litexpr = literalExpressionFrom((Rational) termVal);
        } else {
          continue;
        }

        result = And.of(result, LeafExpression.of(buildEquals(varName, litexpr)));
      }
    }

    return result;
  }

  private CLiteralExpression literalExpressionFrom(final Rational rat) {
    if (rat.numerator().mod(rat.denominator()).intValue() == 0) {
      return CIntegerLiteralExpression.createDummyLiteral(
          rat.numerator().divide(rat.denominator()).longValue(), CNumericTypes.INT);
    } else {
      return new CFloatLiteralExpression(
          FileLocation.DUMMY,
          CNumericTypes.FLOAT,
          new BigDecimal(rat.numerator()).divide(new BigDecimal(rat.denominator())));
    }
  }

  private CBinaryExpression buildEquals(final String varName, final CLiteralExpression litExpr) {
    CType type =
        litExpr instanceof CIntegerLiteralExpression ? CNumericTypes.INT : CNumericTypes.FLOAT;
    CIdExpression idexpr =
        new CIdExpression(
            FileLocation.DUMMY,
            type,
            varName,
            new CVariableDeclaration(
                FileLocation.DUMMY,
                false,
                CStorageClass.AUTO,
                type,
                varName,
                varName,
                varName,
                null));
    return new CBinaryExpression(
        FileLocation.DUMMY, type, type, idexpr, litExpr, BinaryOperator.EQUALS);
  }

  private String toOrigName(final TermVariable pTermVariable) {
    String varName = pTermVariable.getName();

    if (varName.startsWith("(")) {
      return expressionVarName(varName);
    }

    MemoryLocation memLoc = MemoryLocation.parseExtendedQualifiedName(varName);
    return memLoc.getIdentifier();
  }

  private String expressionVarName(final String varName) {
    String result = varName;

    if (result.startsWith("(")) {
      result = result.substring(1, result.length() - 1);
      List<String> t = extractArgs(result);

      if (t.get(0).startsWith("*")) {
        if (t.size() == 2) {
          return "*(" + expressionVarName(t.get(1)) + ")";
        } else if (t.size() == 3) {
          return "(" + expressionVarName(t.get(1)) + ")*(" + expressionVarName(t.get(2)) + ")";
        }
      }

      if (t.get(0).startsWith("+")) {
        if (t.size() == 3) {
          return "(" + expressionVarName(t.get(1)) + ")+(" + expressionVarName(t.get(2)) + ")";
        }
      }

      if (t.size() > 1) {
        StringBuilder sb = new StringBuilder();

        for (String s : t) {
          sb.append(expressionVarName(s));
        }

        return sb.toString();
      }
    }

    if (result.startsWith("|") && result.endsWith("|")) {
      result = result.substring(1, result.length() - 1);
    }

    MemoryLocation memLoc = MemoryLocation.parseExtendedQualifiedName(result);
    return memLoc.getIdentifier();
  }

  private List<String> extractArgs(final String input) {
    List<String> args = new ArrayList<>(2);
    String extendedInput = input + " ";

    int openBrackets = 0;
    StringBuilder bd = new StringBuilder();
    char c;

    for (int i = 0; i < extendedInput.length(); i++) {
      c = extendedInput.charAt(i);
      switch (c) {
        case '(':
          openBrackets++;
          break;
        case ')':
          openBrackets--;
          break;
        case ' ':
          if (openBrackets == 0) {
            if (bd.length() != 0) {
              args.add(bd.toString());
              bd = new StringBuilder();
            }

            continue;
          }
          break;
        default:
      }
      bd.append(c);
    }

    return args;
  }

  @Override
  public @Nullable String getName() {
    return "Termination Algorithm";
  }

  private static String format(TimeSpan pTimeSpan) {
    return pTimeSpan.formatAs(SECONDS);
  }

  private static String format(int value) {
    return String.format("%5d", value);
  }

  private static String div(double val, double full) {
    return String.format("%8.2f", val / full);
  }
}
