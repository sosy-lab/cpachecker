// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.validvars;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker.ProofCheckerCPA;

// requires that function names are unique, no two functions may have the same name although they
// have different signature
// currently ensured by parser
@Options(prefix = "cpa.validVars")
public class ValidVarsCPA extends AbstractCPA implements ProofCheckerCPA {

  @Option(
      secure = true,
      name = "merge",
      toUppercase = true,
      values = {"SEP", "JOIN"},
      description = "which merge operator to use for ValidVarsCPA")
  private String mergeType = "JOIN";

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ValidVarsCPA.class);
  }

  public ValidVarsCPA(Configuration config) throws InvalidConfigurationException {
    super("irrelevant", "sep", new ValidVarsDomain(), new ValidVarsTransferRelation());
    config.inject(this);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new ValidVarsState(ValidVars.initial);
  }
}
