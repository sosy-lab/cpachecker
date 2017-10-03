/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.scopebounded;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisDelegatingRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFATraversal;

@Options(prefix = "cpa.scopebounded.refiner")
public final class ScopeBoundedRefiner implements ARGBasedRefiner {

  final ScopeBoundedCPA cpa;
  final ARGBasedRefiner refiner;

  @Option(
    description =
        "Add at least this number of edges (if available) to CFA for each unrolled function"
  )
  int edgeIncrement = 50;

  @Option(
    description =
        "Give up and unroll everything if more than this fraction of stubs have to be unrolled"
  )
  int maxUnrollFraction = 5;

  // Intermediate variable equal to nStubs / maxUnrollFraction
  final int maxUnroll;

  final LogManager logger;

  public ScopeBoundedRefiner(
      final ARGBasedRefiner pRefiner, final ScopeBoundedCPA pCPA, final Configuration config)
      throws InvalidConfigurationException {

    config.inject(this);

    refiner = pRefiner;
    cpa = pCPA;
    logger = pCPA.getLogger();
    maxUnroll =
        cpa.getCFA()
                .getAllFunctionNames()
                .stream()
                .filter(cpa::isStub)
                .collect(Collectors.toSet())
                .size()
            / maxUnrollFraction;
  }

  @Override
  public final CounterexampleInfo performRefinementForPath(
      final ARGReachedSet pReached, final ARGPath pPath) throws CPAException, InterruptedException {

    final CounterexampleInfo info = refiner.performRefinementForPath(pReached, pPath);

    if (info.isSpurious()) {
      return info;
    } else {

      final CFAPathWithAssumptions path = info.getCFAPathWithAssignments();
      final Set<String> stubNames =
          path.stream()
              .map(e -> e.getCFAEdge().getSuccessor())
              .filter(
                  n -> {
                    if (n instanceof FunctionEntryNode) {
                      return cpa.isStub(((FunctionEntryNode) n).getFunctionName());
                    } else {
                      return false;
                    }
                  })
              .map(CFANode::getFunctionName)
              .collect(Collectors.toSet());

      if (!stubNames.isEmpty()) {

        final CFA cfa = cpa.getCFA();
        ImmutableSet.copyOf(stubNames)
            .forEach(
                stub -> {
                  final AtomicInteger numEdges = new AtomicInteger(0);
                  final Queue<String> toProcess = new LinkedList<>();
                  toProcess.add(stub);
                  while (numEdges.get() < edgeIncrement && !toProcess.isEmpty()) {
                    final String f = cpa.originalName(toProcess.poll());
                    final FunctionCallCollector collector = new FunctionCallCollector();
                    CFATraversal.dfs().traverseOnce(cfa.getFunctionHead(f), collector);
                    numEdges.addAndGet(collector.getNumEdges());
                    collector
                        .getFunctionCalls()
                        .forEach(
                            ce -> {
                              final String name = ce.getSuccessor().getFunctionName();
                              if (cpa.isStub(name) && !stubNames.contains(name)) {
                                stubNames.add(name);
                                toProcess.add(name);
                              }
                              // And what if this is NOT a stub? Should not be! Currently all
                              // peripheral
                              // functions have stubs, so if this function doesn't => it's not
                              // peripheral =>
                              // the caller function is not peripheral => it couldn't have a stub =>
                              // how could it end up in stubNames (?) => impossible.
                              // In fact it can be non-stub if it's an external function! But then
                              // it has 0
                              // internal CFA edges anyway.
                            });
                  }
                });

        // Known issue: already unrolled functions called from former stubs are counted twice, but
        // this is just a heuristic anyway
        if (ScopeBoundedPrecision.nUnrolledFunctions() + stubNames.size() > maxUnroll) {
          stubNames.addAll(
              cfa.getAllFunctionNames().stream().filter(cpa::isStub).collect(Collectors.toSet()));
        }

        stubNames
            .stream()
            .forEach(f -> ScopeBoundedPrecision.addFunctionToUnroll(cpa.originalName(f)));

        logger.log(
            Level.INFO,
            "Expanding verification scope. " + "Will unroll the following functions:\n ",
            ScopeBoundedPrecision.unrolledFunctions());

        info.getTargetPath()
            .asStatesList()
            .stream()
            .filter(s -> stubNames.contains(extractLocation(s).getFunctionName()))
            .findFirst()
            .ifPresent(s -> ImmutableList.copyOf(s.getParents()).forEach(pReached::removeSubtree));

        return CounterexampleInfo.spurious();
      } else {
        return info;
      }
    }
  }

  public static Refiner create(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    assert pCpa instanceof ScopeBoundedCPA;
    assert ((ScopeBoundedCPA) pCpa).getWrappedCpa() instanceof ARGCPA;

    final ScopeBoundedCPA cpa = (ScopeBoundedCPA) pCpa;
    final ARGBasedRefiner wrapped =
        ((AbstractARGBasedRefiner) ValueAnalysisDelegatingRefiner.create(cpa.getWrappedCpa()))
            .getRefiner();
    return AbstractARGBasedRefiner.forARGBasedRefiner(
        new ScopeBoundedRefiner(wrapped, cpa, cpa.getConfig()), cpa);
  }

  private class FunctionCallCollector extends CFATraversal.DefaultCFAVisitor {
    private final List<CFunctionCallEdge> functionCalls = new ArrayList<>();
    private int numEdges = 0;

    public Collection<CFunctionCallEdge> getFunctionCalls() {
      return Collections.unmodifiableCollection(functionCalls);
    }

    public int getNumEdges() {
      return numEdges;
    }

    @Override
    public CFATraversal.TraversalProcess visitEdge(final CFAEdge pEdge) {
      numEdges++;
      switch (pEdge.getEdgeType()) {
        case FunctionCallEdge:
          functionCalls.add((CFunctionCallEdge) pEdge);
          return CFATraversal.TraversalProcess.SKIP;

        case FunctionReturnEdge:
          return CFATraversal.TraversalProcess.SKIP;

        case CallToReturnEdge:
        default:
          return CFATraversal.TraversalProcess.CONTINUE;
      }
    }
  }
}
