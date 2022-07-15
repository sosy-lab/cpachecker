// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.livevar;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.collect.Multimap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

@Options
public class LiveVariablesCPA extends AbstractCPA {

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = {"SEP", "JOIN"},
      description = "which merge operator to use for LiveVariablesCPA")
  private String mergeType = "JOIN";

  @Option(
      secure = true,
      name = "stop",
      toUppercase = true,
      values = {"SEP", "JOIN", "NEVER"},
      description = "which stop operator to use for LiveVariablesCPA")
  private String stopType = "SEP";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LiveVariablesCPA.class);
  }

  private LiveVariablesCPA(final Configuration pConfig, final LogManager pLogger, final CFA cfa)
      throws InvalidConfigurationException {
    super(
        DelegateAbstractDomain.getInstance(),
        new LiveVariablesTransferRelation(
            cfa.getVarClassification(), pConfig, cfa.getLanguage(), cfa, pLogger));
    pConfig.inject(this, LiveVariablesCPA.class);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return ((LiveVariablesTransferRelation) getTransferRelation()).getInitialState(pNode);
  }

  /**
   * Returns the liveVariables that are currently computed. Calling this method makes only sense if
   * the analysis was completed
   *
   * @return a Multimap containing the variables that are live at each location
   */
  public Multimap<CFANode, Wrapper<ASimpleDeclaration>> getLiveVariables() {
    return ((LiveVariablesTransferRelation) getTransferRelation()).getLiveVariables();
  }
}
