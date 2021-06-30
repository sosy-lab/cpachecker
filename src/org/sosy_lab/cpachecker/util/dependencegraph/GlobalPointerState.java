// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerDomain;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerTransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.reachingdef.ReachingDefUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

abstract class GlobalPointerState {

  public abstract Set<MemoryLocation> getPossiblePointees(CFAEdge pEdge, CExpression pExpression);

  public static GlobalPointerState createFlowInsensitive(CFA pCfa)
      throws CPAException, InterruptedException {

    return FlowInsensitivePointerState.create(pCfa);
  }

  public static GlobalPointerState createFlowSensitive(
      CFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws CPAException, InterruptedException {

    return FlowSensitivePointerState.create(pCfa, pLogger, pShutdownNotifier);
  }

  private static final class FlowInsensitivePointerState extends GlobalPointerState {

    private static final Precision PRECISION = new Precision() {};
    private static final PointerTransferRelation POINTER_TRANSFER_RELATION =
        new PointerTransferRelation();

    private final PointerState pointerState;

    private FlowInsensitivePointerState(PointerState pPointerState) {
      pointerState = pPointerState;
    }

    @Override
    public Set<MemoryLocation> getPossiblePointees(CFAEdge pEdge, CExpression pExpression) {

      Set<MemoryLocation> possiblePointees =
          ReachingDefUtils.possiblePointees(pExpression, pointerState);

      return possiblePointees != null ? possiblePointees : ImmutableSet.of();
    }

    private static Collection<CFAEdge> getAllEdges(CFA pCfa) {

      List<CFAEdge> edges = new ArrayList<>();

      for (CFANode node : pCfa.getAllNodes()) {
        Iterables.addAll(edges, CFAUtils.leavingEdges(node));
      }

      return edges;
    }

    private static PointerState next(PointerState pPointerState, CFAEdge pEdge)
        throws CPATransferException, InterruptedException {

      Collection<? extends AbstractState> collection =
          POINTER_TRANSFER_RELATION.getAbstractSuccessorsForEdge(pPointerState, PRECISION, pEdge);

      Optional<? extends AbstractState> optState = collection.stream().findFirst();

      if (optState.isPresent()) {
        return (PointerState) optState.orElseThrow();
      }

      return pPointerState;
    }

    private static GlobalPointerState create(CFA pCfa) throws CPAException, InterruptedException {

      Collection<CFAEdge> edges = getAllEdges(pCfa);
      PointerState pointerState = PointerState.INITIAL_STATE;
      Map<MemoryLocation, LocationSet> pointsToMap = new HashMap<>();

      boolean changed = true;
      while (changed) {

        changed = false;

        for (CFAEdge edge : edges) {

          PointerState nextPointerState = next(pointerState, edge);

          for (Map.Entry<MemoryLocation, LocationSet> entry :
              nextPointerState.getPointsToMap().entrySet()) {

            LocationSet locationSet = pointsToMap.get(entry.getKey());

            if (locationSet == null) {

              pointsToMap.put(entry.getKey(), entry.getValue());
              pointerState = pointerState.addPointsToInformation(entry.getKey(), entry.getValue());

              changed = true;

            } else if (!locationSet.containsAll(entry.getValue())) {

              locationSet = locationSet.addElements(entry.getValue());
              pointsToMap.put(entry.getKey(), locationSet);
              pointerState = pointerState.addPointsToInformation(entry.getKey(), entry.getValue());

              changed = true;
            }
          }
        }
      }

      return new FlowInsensitivePointerState(pointerState);
    }
  }

  private static final class FlowSensitivePointerState extends GlobalPointerState {

    private final Map<CFAEdge, PointerState> pointerStates;

    private FlowSensitivePointerState(Map<CFAEdge, PointerState> pPointerStates) {
      pointerStates = pPointerStates;
    }

    @Override
    public Set<MemoryLocation> getPossiblePointees(CFAEdge pEdge, CExpression pExpression) {

      PointerState pointerState = pointerStates.get(pEdge);

      Set<MemoryLocation> possiblePointees = null;
      if (pointerState != null) {
        possiblePointees = ReachingDefUtils.possiblePointees(pExpression, pointerState);
      }

      return possiblePointees != null ? possiblePointees : ImmutableSet.of();
    }

    private static GlobalPointerState create(
        CFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
        throws InterruptedException, CPAException {

      Algorithm algorithm;
      ReachedSetFactory reachedFactory;
      ConfigurableProgramAnalysis cpa;

      try {

        Configuration config =
            Configuration.builder()
                .loadFromResource(GlobalPointerState.class, "pointerAnalysis.properties")
                .build();

        reachedFactory = new ReachedSetFactory(config, pLogger);
        cpa =
            new CPABuilder(config, pLogger, pShutdownNotifier, reachedFactory)
                .buildCPAs(pCfa, Specification.alwaysSatisfied(), new AggregatedReachedSets());
        algorithm = CPAAlgorithm.create(cpa, pLogger, config, pShutdownNotifier);

      } catch (InvalidConfigurationException ex) {
        pLogger.logUserException(Level.SEVERE, ex, "Unable to run pointer analysis");
        return null;
      }

      ReachedSet reached = reachedFactory.create();

      AbstractState initialState =
          cpa.getInitialState(pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      Precision initialPrecision =
          cpa.getInitialPrecision(
              pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition());
      reached.add(initialState, initialPrecision);

      algorithm.run(reached);
      assert !reached.hasWaitingState()
          : "CPA algorithm finished, but waitlist not empty: " + reached.getWaitlist();

      Map<CFAEdge, PointerState> pointerStates = new HashMap<>();

      for (AbstractState state : reached) {

        PointerState pointerState = AbstractStates.extractStateByType(state, PointerState.class);
        CFANode node = AbstractStates.extractLocation(state);

        for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {

          PointerState currentPointerState = pointerStates.get(edge);

          if (currentPointerState != null) {
            currentPointerState =
                (PointerState) PointerDomain.INSTANCE.join(currentPointerState, pointerState);
          } else {
            currentPointerState = pointerState;
          }

          pointerStates.put(edge, pointerState);
        }
      }

      return new FlowSensitivePointerState(pointerStates);
    }
  }
}
