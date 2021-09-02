// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;

@Options(prefix = "string.cpa")
public class StringCPA extends AbstractCPA {

  @Option(
    secure = true,
    name = "merge",
    toUppercase = true,
    values = {"SEP", "JOIN"},
    description = "which merge operator to use for StringCPA")
  private String mergeType = "JOIN";

  @Option(
    secure = true,
    name = "stop",
    toUppercase = true,
    values = {"SEP", "JOIN"},
    description = "which stop operator to use for StringCPA")
  private String stopType = "SEP";

  private Configuration config;
  // private final StringTransferRelation transfer;
  private StringTransferRelation transfer;
  private StringOptions options;
  private LogManager logger;

  private StringCPA(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(
        DelegateAbstractDomain.<StringState>getInstance(),
        // new StringDomain(options),
        new StringTransferRelation(pLogger));
    this.config = pConfig;
    this.logger = pLogger;
    options = new StringOptions(pConfig);
    config.inject(this, StringCPA.class);
    this.transfer = new StringTransferRelation(pLogger);
    getMergeOperator();
    getStopOperator();
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(StringCPA.class);
  }

  @Override
  public StringTransferRelation getTransferRelation() {
    return new StringTransferRelation(logger);
  }

  @Override
  public MergeOperator getMergeOperator() {
    // return new MergeJoinOperator(getAbstractDomain());
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    // return new StopJoinOperator(getAbstractDomain());
    return buildStopOperator(stopType);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new StringState(ImmutableMap.of(), options);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return DelegateAbstractDomain.<StringState>getInstance();
  }

}
