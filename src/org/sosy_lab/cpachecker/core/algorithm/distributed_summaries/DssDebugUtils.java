// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGToDotWriter;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DssDebugUtils {

  public static String argToDot(ReachedSet pReachedSet) throws IOException {
    StringBuilder sb = new StringBuilder();
    ARGToDotWriter.write(
        sb, pReachedSet.asCollection().stream().map(a -> (ARGState) a).toList(), "test");
    return sb.toString();
  }

  public static String prettyPrintBlock(
      String id,
      Multimap<String, @NonNull StateAndPrecision> preconditions,
      Multimap<String, @NonNull StateAndPrecision> violationConditions,
      Function<AbstractState, String> pStateToString) {
    String precondition =
        FluentIterable.from(preconditions.keySet())
            .transform(
                predecessor ->
                    FluentIterable.from(preconditions.get(predecessor))
                        .transform(p -> predecessor + ": " + pStateToString.apply(p.state()))
                        .join(Joiner.on("\n  ")))
            .join(Joiner.on("\n  "));
    String header = "Block " + id + " with preconditions:\n  " + precondition;
    header += "\nand violation conditions:\n  ";
    String postcondition =
        FluentIterable.from(violationConditions.keySet())
            .transform(
                successor ->
                    FluentIterable.from(violationConditions.get(successor))
                        .transform(p -> successor + ": " + pStateToString.apply(p.state()))
                        .join(Joiner.on("\n  ")))
            .join(Joiner.on("\n  "));
    header += postcondition;
    int rep = Splitter.on("\n").splitToStream(header).mapToInt(String::length).max().orElse(0);
    return "=".repeat(rep) + "\n" + header + "\n" + "=".repeat(rep);
  }

  public static String prettyPrintPredicateAnalysisBlock(
      BlockNode blockNode,
      Multimap<String, @NonNull StateAndPrecision> preconditions,
      Multimap<String, @NonNull StateAndPrecision> violationConditions) {
    return prettyPrintBlock(
        blockNode.getId(),
        preconditions,
        violationConditions,
        a ->
            AbstractStates.extractStateByType(a, PredicateAbstractState.class)
                .getPathFormula()
                .toString());
  }
}
