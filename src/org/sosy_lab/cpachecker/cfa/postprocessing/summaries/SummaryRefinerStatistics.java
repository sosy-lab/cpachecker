// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.Rewrite.ConflictingModificationException;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.HavocStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.LoopStrategy;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

@Options
public class SummaryRefinerStatistics implements Statistics {

  private static final String EXTERN_DECLARATIONS =
      "extern void abort(void);\n"
          + "extern int __VERIFIER_nondet_int();\n"
          + "extern _Bool __VERIFIER_nondet_bool();\n"
          + "extern char __VERIFIER_nondet_char();\n"
          + "extern double __VERIFIER_nondet_double();\n"
          + "extern float __VERIFIER_nondet_float();\n"
          + "extern unsigned long __VERIFIER_nondet_ulong();\n"
          + "extern unsigned long long __VERIFIER_nondet_ulonglong();\n"
          + "extern unsigned int __VERIFIER_nondet_uint();\n"
          + "extern int __VERIFIER_nondet_int();\n";
  private Integer doubleRefinementsMade = 0;
  private Integer doubleRefinementsBecauseMaximumAmntFirstRefinements = 0;
  private Integer amountStrategiesRefinedAway = 0;
  private Integer distinctNodesWithStrategies = 0;
  private SummaryInformation summaryInformation;

  @Option(
      secure = true,
      name = "cfa.summaries.dumpinlinedsummaries",
      description =
          "once enabled, this will create a copy of the input program in the output directory where"
              + " parts of the program have been replaced by the corresponding summary that was"
              + " used during the analysis")
  private boolean outputTask = true;

  @Option(
      secure = true,
      name = "cfa.summaries.outputdirectory",
      description = "directory in which the programs with inlined summaries are stored")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  private Path outputDirectory = Path.of("LA");

  public SummaryRefinerStatistics(SummaryInformation pSummaryInformation, Configuration pConfig)
      throws InvalidConfigurationException {
    this.summaryInformation = pSummaryInformation;
    pConfig.inject(this);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    pOut.println(
        "Total Number of Double Refinements:                                     "
            + doubleRefinementsMade);
    pOut.println(
        "Amount of Double refinements after limit of first refinements:          "
            + doubleRefinementsBecauseMaximumAmntFirstRefinements);
    pOut.println(
        "Amount of Strategies which where refined away:                          "
            + amountStrategiesRefinedAway);
    pOut.println(
        "Amount of distinct Strategies after refinements:                        "
            + distinctNodesWithStrategies);
  }

  public void increaseDoubleRefinements() {
    this.doubleRefinementsMade += 1;
  }

  public void increaseStrategiesRefinedAway() {
    this.amountStrategiesRefinedAway += 1;
  }

  public void recalculateDistinctStartegies() {
    Set<StrategiesEnum> ignoredStrategies = new HashSet<>();
    ignoredStrategies.add(StrategiesEnum.BASE);
    distinctNodesWithStrategies =
        summaryInformation
            .getDistinctNodesWithStrategiesWithoutDissallowed(ignoredStrategies)
            .size();
  }

  public void increaseDoubleRefinementsCausedByMaximumAmountFirstRefinements() {
    this.doubleRefinementsBecauseMaximumAmntFirstRefinements += 1;
  }

  @Override
  public String getName() {
    return "Summary refiner statistics";
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    if (!outputTask) {
      return;
    }

    Map<CFANode, Set<StrategiesEnum>> stratMap = new HashMap<>();
    for (AbstractState a : pReached) {
      FluentIterable.from(AbstractStates.extractLocations(a))
          .filter(LoopStrategy::isLoopInit)
          .forEach(
              n ->
                  stratMap
                      .computeIfAbsent(n, x -> new HashSet<>())
                      .addAll(summaryInformation.getAllowedStrategies(n)));
    }

    // go over all loop locations and rewrite them if possible:
    Map<Path, Rewrite> rewrites = new HashMap<>();
    for (Map.Entry<CFANode, Set<StrategiesEnum>> entry : stratMap.entrySet()) {
      if (entry.getValue().size() != 1) {
        continue;
      }
      StrategiesEnum en = (StrategiesEnum) entry.getValue().toArray()[0];
      int offset2 = entry.getKey().getLeavingEdge(0).getFileLocation().getNodeOffset();
      Loop loop =
          summaryInformation.getLoop(entry.getKey().getLeavingEdge(0).getSuccessor()).orElseThrow();
      if (loop.getIncomingEdges().size() != 1) {
        continue;
      }
      CFAEdge e = Iterables.getOnlyElement(loop.getIncomingEdges());
      int offset = e.getFileLocation().getNodeOffset();
      assert offset == offset2; // TODO: remove code with offset2 above
      int len = e.getFileLocation().getNodeLength();

      Path containingFile =
          FluentIterable.from(loop.getIncomingEdges())
              .first()
              .orNull()
              .getFileLocation()
              .getFileName();
      Rewrite r =
          rewrites.computeIfAbsent(
              containingFile,
              x -> {
                try {
                  return new Rewrite(x);
                } catch (IOException e1) {
                  return null;
                }
              });
      if (r == null) {
        continue;
      }
      if (en.equals(StrategiesEnum.HAVOCSTRATEGY)) {
        String summary = HavocStrategy.summarizeAsCode(loop).orElseThrow();
        try {
          r.insertIndented(offset, summary);
          r.delete(offset, len);
        } catch (ConflictingModificationException e1) {
          continue;
        }
      }
    }

    // go over all source files and output the rewritten versions:
    for (Map.Entry<Path, Rewrite> entry : rewrites.entrySet()) {
      try {
        entry.getValue().insert(0, EXTERN_DECLARATIONS);
      } catch (ConflictingModificationException e1) {
      }
      try (Writer w =
          IO.openOutputFile(
              outputDirectory.resolve(entry.getKey().getFileName().toString()),
              Charset.defaultCharset())) {
        w.append(entry.getValue().apply());
      } catch (IOException e) {
      }
    }
  }
}
