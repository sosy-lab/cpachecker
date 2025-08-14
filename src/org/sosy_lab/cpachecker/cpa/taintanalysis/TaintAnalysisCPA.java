// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.taintanalysis;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory.OptionalAnnotation;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;

public class TaintAnalysisCPA extends AbstractCPA implements ProofCheckerCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(TaintAnalysisCPA.class);
  }

  private TaintAnalysisCPA(LogManager logger, @Nullable @OptionalAnnotation CFA pCfa) {
    super(
        DelegateAbstractDomain.getInstance(),
        new TaintAnalysisTransferRelation(
            logger,
            pCfa != null ? pCfa.getLoopStructure().orElse(null) : null,
            pCfa != null ? pCfa.getAstCfaRelation() : null));
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new MergeJoinOperator(getAbstractDomain());
  }

  @Override
  public StopOperator getStopOperator() {
    return new StopSepOperator(getAbstractDomain());
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return new TaintAnalysisState(
        ImmutableSet.of(), ImmutableSet.of(), ImmutableMap.of(), ImmutableSet.of());
  }
}
