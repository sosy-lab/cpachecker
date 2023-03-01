// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
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
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

abstract class GlobalPointerState {

  public static final GlobalPointerState IGNORE_POINTERS = new IgnorePointersPointerState();

  public abstract ImmutableSet<MemoryLocation> getPossiblePointees(
      CFAEdge pEdge, CExpression pExpression);

  private static ImmutableSet<MemoryLocation> computeAddressableVariables(CFA pCfa) {

    Set<MemoryLocation> addressableVariables = new HashSet<>();
    EdgeDefUseData.Extractor extractor = EdgeDefUseData.createExtractor(false);

    for (CFAEdge edge : CFAUtils.allEdges(pCfa)) {
      addressableVariables.addAll(extractor.extract(edge).getDefs());
    }

    return ImmutableSet.copyOf(addressableVariables);
  }

  private static ImmutableSet<MemoryLocation> computeAddressedVariables(CFA pCfa) {

    Set<MemoryLocation> addressedVariables = new HashSet<>();
    Optional<VariableClassification> optVariableClassification = pCfa.getVarClassification();

    if (optVariableClassification.isPresent()) {

      VariableClassification variableClassification = optVariableClassification.orElseThrow();

      for (String variableName : variableClassification.getAddressedVariables()) {
        addressedVariables.add(MemoryLocation.fromQualifiedName(variableName));
      }
    }

    return ImmutableSet.copyOf(addressedVariables);
  }

  private static boolean isPointerUnknown(Set<MemoryLocation> pPossiblePointees) {

    // if there are no possible pointees, the pointer is unknown
    if (pPossiblePointees.isEmpty()) {
      return true;
    }

    // The current pointer analysis (pointer2) does not support structs/unions.
    // The pointer analysis treats pointers to struct/union instances as pointers to the
    // corresponding struct/union declaration type.
    // If such an unsupported case is encountered, the pointer is unknown.
    for (MemoryLocation possiblePointee : pPossiblePointees) {
      String identifier = possiblePointee.getIdentifier();
      if (identifier.contains("struct ") || identifier.contains("union ")) {
        return true;
      }
    }

    return false;
  }

  private static ImmutableSet<MemoryLocation> getPossiblePointees(
      PointerState pPointerState,
      ImmutableSet<MemoryLocation> pAddressableVariables,
      ImmutableSet<MemoryLocation> pAddressedVariables,
      CExpression pExpression) {

    Set<MemoryLocation> possiblePointees = null;

    if (pPointerState != null) {
      possiblePointees = ReachingDefUtils.possiblePointees(pExpression, pPointerState);
    }

    if (possiblePointees == null) {
      possiblePointees = ImmutableSet.of();
    }

    if (isPointerUnknown(possiblePointees)) {

      possiblePointees = new HashSet<>();

      if (!pAddressedVariables.isEmpty()) {
        possiblePointees = pAddressedVariables;
      } else {
        possiblePointees = pAddressableVariables;
      }

      if (possiblePointees.isEmpty()) {
        possiblePointees = pAddressableVariables;
      }
    }

    return ImmutableSet.copyOf(possiblePointees);
  }

  public static GlobalPointerState createFlowInsensitive(
      CFA pCfa, ShutdownNotifier pShutdownNotifier) throws CPAException, InterruptedException {

    return FlowInsensitivePointerState.create(pCfa, pShutdownNotifier);
  }

  public static GlobalPointerState createFlowSensitive(
      CFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws CPAException, InterruptedException {

    return FlowSensitivePointerState.create(pCfa, pLogger, pShutdownNotifier);
  }

  public static GlobalPointerState creatUnknown(CFA pCfa) {
    return new UnknownPointerState(
        computeAddressableVariables(pCfa), computeAddressedVariables(pCfa));
  }

  private static final class FlowInsensitivePointerState extends GlobalPointerState {

    private static final Precision PRECISION = new Precision() {};
    private static final PointerTransferRelation POINTER_TRANSFER_RELATION =
        new PointerTransferRelation();

    private final PointerState pointerState;
    private final ImmutableSet<MemoryLocation> addressableVariables;
    private final ImmutableSet<MemoryLocation> addressedVariables;

    private FlowInsensitivePointerState(
        PointerState pPointerState,
        ImmutableSet<MemoryLocation> pAddressableVariables,
        ImmutableSet<MemoryLocation> pAddressedVariables) {

      pointerState = pPointerState;
      addressableVariables = pAddressableVariables;
      addressedVariables = pAddressedVariables;
    }

    @Override
    public ImmutableSet<MemoryLocation> getPossiblePointees(
        CFAEdge pEdge, CExpression pExpression) {

      return GlobalPointerState.getPossiblePointees(
          pointerState, addressableVariables, addressedVariables, pExpression);
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

    private static GlobalPointerState create(CFA pCfa, ShutdownNotifier pShutdownNotifier)
        throws CPAException, InterruptedException {

      Collection<CFAEdge> edges = new ArrayList<>(pCfa.edges());
      PointerState pointerState = PointerState.INITIAL_STATE;
      Map<MemoryLocation, LocationSet> pointsToMap = new HashMap<>();

      boolean changed = true;
      while (changed) {

        changed = false;

        for (CFAEdge edge : edges) {

          if (pShutdownNotifier.shouldShutdown()) {
            return null;
          }

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

      return new FlowInsensitivePointerState(
          pointerState, computeAddressableVariables(pCfa), computeAddressedVariables(pCfa));
    }
  }

  private static final class FlowSensitivePointerState extends GlobalPointerState {

    private final Map<CFAEdge, PointerState> pointerStates;
    private final ImmutableSet<MemoryLocation> addressableVariables;
    private final ImmutableSet<MemoryLocation> addressedVariables;

    private FlowSensitivePointerState(
        Map<CFAEdge, PointerState> pPointerStates,
        ImmutableSet<MemoryLocation> pAddressableVariables,
        ImmutableSet<MemoryLocation> pAddressedVariables) {

      pointerStates = pPointerStates;
      addressableVariables = pAddressableVariables;
      addressedVariables = pAddressedVariables;
    }

    @Override
    public ImmutableSet<MemoryLocation> getPossiblePointees(
        CFAEdge pEdge, CExpression pExpression) {

      PointerState pointerState = pointerStates.get(pEdge);

      if (pointerState != null) {
        return GlobalPointerState.getPossiblePointees(
            pointerState, addressableVariables, addressedVariables, pExpression);
      } else {
        return addressableVariables;
      }
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
                .buildCPAs(pCfa, Specification.alwaysSatisfied(), AggregatedReachedSets.empty());
        algorithm = CPAAlgorithm.create(cpa, pLogger, config, pShutdownNotifier);

      } catch (InvalidConfigurationException ex) {
        pLogger.logUserException(Level.SEVERE, ex, "Unable to run pointer analysis");
        return null;
      }

      ReachedSet reached =
          reachedFactory.createAndInitialize(
              cpa, pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition());

      algorithm.run(reached);
      assert !reached.hasWaitingState()
          : "CPA algorithm finished, but waitlist not empty: " + reached.getWaitlist();

      Map<CFAEdge, PointerState> pointerStates = new HashMap<>();

      for (AbstractState state : reached) {

        PointerState pointerState = AbstractStates.extractStateByType(state, PointerState.class);
        CFANode node = AbstractStates.extractLocation(state);

        for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {

          if (pShutdownNotifier.shouldShutdown()) {
            return null;
          }

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

      return new FlowSensitivePointerState(
          pointerStates, computeAddressableVariables(pCfa), computeAddressedVariables(pCfa));
    }
  }

  private static final class UnknownPointerState extends GlobalPointerState {

    private final ImmutableSet<MemoryLocation> addressableVariables;
    private final ImmutableSet<MemoryLocation> addressedVariables;

    private UnknownPointerState(
        ImmutableSet<MemoryLocation> pAddressableVariables,
        ImmutableSet<MemoryLocation> pAddressedVariables) {

      addressableVariables = pAddressableVariables;
      addressedVariables = pAddressedVariables;
    }

    @Override
    public ImmutableSet<MemoryLocation> getPossiblePointees(
        CFAEdge pEdge, CExpression pExpression) {

      return GlobalPointerState.getPossiblePointees(
          null, addressableVariables, addressedVariables, pExpression);
    }
  }

  private static final class IgnorePointersPointerState extends GlobalPointerState {

    @Override
    public ImmutableSet<MemoryLocation> getPossiblePointees(
        CFAEdge pEdge, CExpression pExpression) {
      return ImmutableSet.of();
    }
  }
}
