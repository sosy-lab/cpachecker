// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.symbolicExecution;

import java.io.IOException;
import java.nio.file.Path;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsCPA;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

@Options(prefix = "symbolicExecution")
public class SymbolicExecutionCPA implements ConfigurableProgramAnalysis {

  private final ValueAnalysisCPA valueAnalysisCPA;
  private final ConstraintsCPA constraintsCPA;

  public ConstraintsCPA getConstraintsCPA() {
    return constraintsCPA;
  }

  @FileOption(Type.REQUIRED_INPUT_FILE)
  @Option(description = "Where to find the symbolic execution config without cegar.")
  private Path symbolicExecutionProperties =
      Path.of("../../../../../../config/includes/symbolicExecutionForDss.properties");

  public SymbolicExecutionCPA(
      Configuration pConfiguration,
      LogManager pLogManager,
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    pConfiguration.inject(this);
    try {
      ConfigurableProgramAnalysis cpa =
          new CoreComponentsFactory(
                  Configuration.builder().loadFromFile(symbolicExecutionProperties).build(),
                  pLogManager,
                  pShutdownNotifier,
                  AggregatedReachedSets.empty(),
                  pCfa)
              .createCPA(pSpecification);
      valueAnalysisCPA = CPAs.retrieveCPAOrFail(cpa, ValueAnalysisCPA.class, getClass());
      constraintsCPA = CPAs.retrieveCPAOrFail(cpa, ConstraintsCPA.class, getClass());
    } catch (IOException e) {
      throw new InvalidConfigurationException("Cannot parse given properties file", e);
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return new SymbolicExecutionDomain(
        valueAnalysisCPA.getAbstractDomain(), constraintsCPA.getAbstractDomain());
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new SymbolicExecutionTransferRelation(
        valueAnalysisCPA.getTransferRelation(), constraintsCPA.getTransferRelation());
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
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new SymbolicExecutionState(
        (ValueAnalysisState) valueAnalysisCPA.getInitialState(node, partition),
        (ConstraintsState) constraintsCPA.getInitialState(node, partition));
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(SymbolicExecutionCPA.class);
  }
}
