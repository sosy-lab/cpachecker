// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.dca;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.MoreCollectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.core.specification.SpecificationProperty;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.ControlAutomatonCPA;
import org.sosy_lab.cpachecker.util.ltl.formulas.LabelledFormula;

public class DCACPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DCACPA.class);
  }

  private final ControlAutomatonCPA automatonCPA;
  private final List<Automaton> automatonList;

  public DCACPA(ConfigurableProgramAnalysis pCpa, Specification pSpec, LogManager pLogger) {
    super(pCpa);

    Optional<SpecificationProperty> propertyOpt =
        pSpec.getProperties().stream().collect(MoreCollectors.toOptional());
    if (propertyOpt.isPresent()) {
      Property property = propertyOpt.orElseThrow().getProperty();
      pLogger.logf(Level.INFO, "Retrieved property from file: %s", property);
      if (property instanceof LabelledFormula) {
        pLogger.logf(Level.INFO, "Negated property: %s", ((LabelledFormula) property).not());
      }
    }

    checkArgument(pCpa instanceof ControlAutomatonCPA);
    automatonCPA = (ControlAutomatonCPA) pCpa;
    automatonList = new ArrayList<>();
  }

  void addAutomaton(Automaton pAutomaton) {
    checkArgument(
        automatonList.add(pAutomaton),
        DCACPA.class.getSimpleName() + " already contains the specified automaton.");
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    ImmutableList<AutomatonState> initStates =
        automatonList
            .stream()
            .map(automatonCPA::buildInitStateForAutomaton)
            .collect(ImmutableList.toImmutableList());
    AutomatonState buechiState = (AutomatonState) super.getInitialState(pNode, pPartition);
    return new DCAState(buechiState, initStates);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new FlatLatticeDomain();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new DCATransferRelation(automatonCPA.getTransferRelation());
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }
}
