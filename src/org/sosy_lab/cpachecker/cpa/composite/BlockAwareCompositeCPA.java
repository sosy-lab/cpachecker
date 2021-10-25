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
import static org.sosy_lab.cpachecker.core.AnalysisDirection.FORWARD;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options
public class BlockAwareCompositeCPA extends AbstractSingleWrapperCPA
    implements ConfigurableProgramAnalysis {
  private final Block block;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(
      secure = true,
      name = "cpa.predicate.direction",
      description = "Direction of the analysis?")
  private AnalysisDirection direction = FORWARD;

  private BlockAwareCompositeCPA(
      final Block pBlock, final ARGCPA pCPA, final Configuration pConfig)
      throws InvalidConfigurationException {
    super(pCPA);
    pConfig.inject(this);

    block = pBlock;
  }

  public static CPAFactory factory() {
    return new BlockAwareARGCPAFactory();
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return wrappedCpa.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    ARGTransferRelation transferRelation = (ARGTransferRelation) wrappedCpa.getTransferRelation();
    return new BlockAwareARGTransferRelation(block, transferRelation, direction);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return wrappedCpa.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return wrappedCpa.getStopOperator();
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return new BlockAwarePrecisionAdjustment(wrappedCpa.getPrecisionAdjustment(), block, direction);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return wrappedCpa.getInitialState(node, partition);
  }

  @Override
  public Precision getInitialPrecision(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return wrappedCpa.getInitialPrecision(node, partition);
  }
  
  @SuppressWarnings("FieldMayBeFinal")
  private static class BlockAwareARGCPAFactory extends AbstractCPAFactory {
    private CFA cfa = null;

    private Block block = null;

    private ARGCPA wrappedCPA = null;

    @Override
    public ConfigurableProgramAnalysis createInstance()
        throws InvalidConfigurationException, CPAException {
      final String message = "Missing data to create BlockAwareCompositeCPA: ";
      checkState(cfa != null, message + "CFA");
      checkState(block != null, message + "Block");
      checkState(wrappedCPA != null, message + "ARGCPA");

      final Configuration config = getConfiguration();
      return new BlockAwareCompositeCPA(block, wrappedCPA, config);
    }

    @Override
    public <T> CPAFactory set(T pObject, Class<T> pClass) throws UnsupportedOperationException {
      if (pClass.equals(CFA.class)) {
        cfa = (CFA) pObject;
      } else if (pClass.equals(Block.class)) {
        block = (Block) pObject;
      } else if (pClass.equals(ARGCPA.class)) {
        wrappedCPA = (ARGCPA) pObject;
      }

      return super.set(pObject, pClass);
    }
  }
}
