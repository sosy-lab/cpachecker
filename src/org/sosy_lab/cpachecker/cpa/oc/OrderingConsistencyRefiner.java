// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.cpachecker.cpa.oc.EventType.WRITE;
import static org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView.INDEX_SEPARATOR;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

public class OrderingConsistencyRefiner implements Refiner {

  private final ConfigurableProgramAnalysis cpa;

  OrderingConsistencyRefiner(ConfigurableProgramAnalysis pCpa) {
    cpa = pCpa;
  }

  public static Refiner create(ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    return new OrderingConsistencyRefiner(pCpa);
  }

  private void addAndLog(ProverEnvironment prover, BooleanFormula formula)
      throws InterruptedException {
    //System.err.println(formula);
    prover.addConstraint(formula);
  }

  @Override
  public boolean performRefinement(ReachedSet pReached) throws CPAException, InterruptedException {
//    final Map<Integer, Set<MemoryEvent>> threads = new HashMap<>();
    final Map<MemoryEvent, Set<MemoryEvent>> programOrder = new HashMap<>();
    final Set<BooleanFormula> targetFormulae = new HashSet<>();
    final Set<MemoryEvent> allEvents = new HashSet<>();
    for (AbstractState abstractState : pReached.asCollection()) {
      OrderingConsistencyState orderingConsistencyState =
          AbstractStates.extractStateByType(abstractState, OrderingConsistencyState.class);
      if (orderingConsistencyState != null) {
        orderingConsistencyState
            .pid()
            .ifPresent(
                pid -> {
                  final var memoryEvents = ImmutableList.copyOf(orderingConsistencyState.waitingThreads().get(pid).pMemoryEvents());
                  for (int i = 0; i < memoryEvents.size() - 1; i++) {
                    final var event = memoryEvents.get(i);
                    final var nextEvent = memoryEvents.get(i + 1);
                    programOrder.computeIfAbsent(event, j -> new HashSet<>()).add(nextEvent);
                  }
                  allEvents.addAll(memoryEvents);
                  if(abstractState instanceof ARGState pARGState) {
                    if(pARGState.isTarget()) {
                      targetFormulae.add(orderingConsistencyState.waitingThreads().get(pid).pPathFormula().getFormula());
                    }
                  }
//                  threads.computeIfAbsent(pid, k -> new HashSet<>()).addAll(memoryEvents);
                });
      }
    }

    final var pCPA = CPAs.retrieveCPA(cpa, OrderingConsistencyCPA.class);
    final var solver = ((OrderingConsistencyTransferRelation)pCPA.getTransferRelation()).getSolver();
    final var prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
    final var fmgr = solver.getFormulaManager();
    final var imgr = solver.getFormulaManager().getIntegerFormulaManager();
    final var bmgr = solver.getFormulaManager().getBooleanFormulaManager();


    final HashMap<String, Formula> consts = new HashMap<>();
    for (MemoryEvent allEvent : allEvents) {
      consts.putAll(fmgr.extractVariables(allEvent.guard().get().getFormula()));
    }

    final Map<MemoryEvent, IntegerFormula> clk = new HashMap<>();
    final Map<MemoryLocation, Set<MemoryEvent>> writes = new HashMap<>();
    final Map<MemoryLocation, Set<MemoryEvent>> reads = new HashMap<>();
    final Map<MemoryEvent, Formula> cssa = new HashMap<>();
    for (MemoryEvent e : allEvents) {
      clk.put(e, imgr.makeVariable("clk_%d".formatted(e.id())));
      final var map = e.eventType() == WRITE ? writes : reads;
      map.computeIfAbsent(e.memoryLocation(), j -> new HashSet<>()).add(e);
      cssa.put(e, consts.get(e.cssaQualifiedName() + INDEX_SEPARATOR + (e.eventType() == WRITE ? 2 : 1)));
    }
//    guards.values().stream().flatMap(e -> solver)

    // write->read
    // final Map<Pair<MemoryEvent, MemoryEvent>, BooleanFormula> rf = new HashMap<>();
    for (Entry<MemoryLocation, Set<MemoryEvent>> entry : reads.entrySet()) {
      final var var = entry.getKey();
      for (MemoryEvent read : entry.getValue()) {
        final Set<BooleanFormula> allRf = new HashSet<>();
        for (MemoryEvent write : writes.getOrDefault(var, ImmutableSet.of())) {
          BooleanFormula rfConst =
              bmgr.makeVariable("rf_%d_%d".formatted(write.id(), read.id()));
          allRf.add(rfConst);
          // rf.put(Pair.of(write, read), rfConst);
          final var w = checkNotNull(cssa.get(write));
          final var r = checkNotNull(cssa.get(read));

          addAndLog(prover, bmgr.implication(rfConst, fmgr.makeEqual(w, r)));
          addAndLog(prover, bmgr.implication(rfConst, write.guard().get().getFormula()));
          addAndLog(prover, bmgr.implication(rfConst, read.guard().get().getFormula()));
          addAndLog(prover, bmgr.implication(rfConst, imgr.lessThan(clk.get(write), clk.get(read))));

          for (MemoryEvent write2 : writes.getOrDefault(var, ImmutableSet.of())) {
            addAndLog(
                prover,
                bmgr.implication(bmgr.and(rfConst, imgr.lessThan(clk.get(write), clk.get(write2))), imgr.lessThan(clk.get(read), clk.get(write2))));
          }

        }
        addAndLog(prover, bmgr.implication(read.guard().get().getFormula(), bmgr.or(allRf)));
      }
    }

    for (Entry<MemoryEvent, Set<MemoryEvent>> value : programOrder.entrySet()) {
      for (MemoryEvent memoryEvent : value.getValue()) {
        addAndLog(prover, imgr.lessThan(clk.get(value.getKey()), clk.get(memoryEvent)));
      }
    }

    try {
      addAndLog(prover, bmgr.or(targetFormulae));
      final var unsat = prover.isUnsat();
      System.err.printf("Unsat?: %s%n", unsat);
      return unsat;
    } catch (SolverException pE) {
      throw new RuntimeException(pE);
    }
  }
}
