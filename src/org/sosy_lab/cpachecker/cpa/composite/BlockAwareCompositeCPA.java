// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// @formatter:off

package org.sosy_lab.cpachecker.cpa.composite;

import static com.google.common.base.Preconditions.checkState;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options
public class BlockAwareCompositeCPA implements ConfigurableProgramAnalysis, WrapperCPA {
  private final Block block;
  private final CompositeCPA cpa;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      secure = true,
      name = "cpa.predicate.direction",
      description = "Direction of the analysis?")
  private AnalysisDirection direction = AnalysisDirection.FORWARD;

  private BlockAwareCompositeCPA(
      final Block pBlock, final CompositeCPA pCPA, final Configuration pConfig)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    block = pBlock;
    cpa = pCPA;
  }

  public static CPAFactory factory() {
    return new BlockAwareCompositeCPAFactory();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return cpa.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    CompositeTransferRelation transferRelation = cpa.getTransferRelation();
    return new BlockAwareCompositeTransferRelation(block, transferRelation, direction);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return cpa.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return cpa.getStopOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return cpa.getPrecisionAdjustment();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return cpa.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return cpa.getInitialPrecision(node, partition);
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> @Nullable T retrieveWrappedCpa(Class<T> type) {
    return cpa.retrieveWrappedCpa(type);
  }

  @Override
  public Iterable<ConfigurableProgramAnalysis> getWrappedCPAs() {
    return cpa.getWrappedCPAs();
  }

  @SuppressWarnings("FieldMayBeFinal")
  private static class BlockAwareCompositeCPAFactory extends AbstractCPAFactory {
    private CFA cfa = null;

    private Block block = null;

    private CompositeCPA wrappedCPA = null;

    @Override
    public ConfigurableProgramAnalysis createInstance()
        throws InvalidConfigurationException, CPAException {
      final String message = "Missing data to create BlockAwareCompositeCPA: ";
      checkState(cfa != null, message + "CFA");
      checkState(block != null, message + "Block");
      checkState(wrappedCPA != null, message + "CompositeCPA");

      final Configuration config = getConfiguration();
      final LogManager logger = getLogger();
      final ShutdownNotifier shutdownNotifier = getShutdownNotifier();

      return new BlockAwareCompositeCPA(block, wrappedCPA, config);
    }

    @Override
    public <T> CPAFactory set(T pObject, Class<T> pClass) throws UnsupportedOperationException {
      if (pClass.equals(CFA.class)) {
        cfa = (CFA) pObject;
      } else if (pClass.equals(Block.class)) {
        block = (Block) pObject;
      } else if (pClass.equals(CompositeCPA.class)) {
        wrappedCPA = (CompositeCPA) pObject;
      }

      return super.set(pObject, pClass);
    }
  }
}
