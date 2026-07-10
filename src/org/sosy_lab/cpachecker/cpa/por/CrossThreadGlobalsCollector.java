// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Statically approximates which global variables are touched by more than one thread, so that
 * abstraction-aware POR (see {@link AbstractionAwarePORPrecision}) can seed its wrapped analysis'
 * initial precision with them. Without this, a CEGAR-refined precision that starts out empty
 * makes {@code canIgnoreVariable} treat every global as ignorable at round 0; if the resulting
 * over-aggressive persistent-set reduction eliminates the only interleaving that reaches a
 * target, the CEGAR loop never sees a (spurious) counterexample to refine against and converges
 * on an unsound SAFE verdict. Pre-seeding with statically-genuine cross-thread conflicts (the
 * same def/use-intersection rule {@link PORState#dependent} uses at exploration time) closes that
 * gap without weakening the abstraction for globals that are actually thread-local in practice.
 */
final class CrossThreadGlobalsCollector {

  private CrossThreadGlobalsCollector() {
  }

  static ImmutableSet<MemoryLocation> collect(CFA cfa) {
    Set<String> threadEntryFunctions = new HashSet<>();
    threadEntryFunctions.add(cfa.getMainFunction().getFunctionName());
    for (CFAEdge edge : cfa.edges()) {
      if (edge instanceof AStatementEdge statementEdge
          && statementEdge.getStatement() instanceof AFunctionCall functionCall
          && functionCall.getFunctionCallExpression().getFunctionNameExpression()
              instanceof AIdExpression functionName
          && "pthread_create".equals(functionName.getName())) {
        List<? extends AExpression> params =
            functionCall.getFunctionCallExpression().getParameterExpressions();
        if (params.size() == 4) {
          threadEntryFunctions.add(ThreadFunctions.extractCreateFunctionName(params));
        }
      }
    }

    EdgeDefUseData.Extractor extractor =
        new EdgeDefUseData.CachingExtractor(EdgeDefUseData.createExtractor(true, true));

    Map<String, Set<MemoryLocation>> defsByFunction = new HashMap<>();
    Map<String, Set<MemoryLocation>> usesByFunction = new HashMap<>();
    for (String functionName : threadEntryFunctions) {
      CFANode entryNode = cfa.getFunctionHead(functionName);
      if (entryNode == null) {
        continue;
      }
      var visitor = new CFATraversal.EdgeCollectingCFAVisitor();
      CFATraversal.dfs().traverseOnce(entryNode, visitor);

      Set<MemoryLocation> defs = defsByFunction.computeIfAbsent(functionName, k -> new HashSet<>());
      Set<MemoryLocation> uses = usesByFunction.computeIfAbsent(functionName, k -> new HashSet<>());
      for (CFAEdge edge : visitor.getVisitedEdges()) {
        EdgeDefUseData data = extractor.extract(edge);
        defs.addAll(data.getDefs());
        uses.addAll(data.getUses());
      }
    }

    ImmutableSet.Builder<MemoryLocation> crossThreadGlobals = ImmutableSet.builder();
    List<String> functions = List.copyOf(threadEntryFunctions);
    for (int i = 0; i < functions.size(); i++) {
      for (int j = i + 1; j < functions.size(); j++) {
        Set<MemoryLocation> defsA = defsByFunction.get(functions.get(i));
        Set<MemoryLocation> usesA = usesByFunction.get(functions.get(i));
        Set<MemoryLocation> defsB = defsByFunction.get(functions.get(j));
        Set<MemoryLocation> usesB = usesByFunction.get(functions.get(j));
        addIntersection(crossThreadGlobals, defsA, usesB);
        addIntersection(crossThreadGlobals, usesA, defsB);
        addIntersection(crossThreadGlobals, defsA, defsB);
      }
    }
    return crossThreadGlobals.build();
  }

  private static void addIntersection(
      ImmutableSet.Builder<MemoryLocation> result,
      Set<MemoryLocation> a,
      Set<MemoryLocation> b) {
    for (MemoryLocation location : a) {
      if (b.contains(location)) {
        result.add(location);
      }
    }
  }
}
