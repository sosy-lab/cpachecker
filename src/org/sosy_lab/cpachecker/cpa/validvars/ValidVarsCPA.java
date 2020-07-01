/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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

// requires that function names are unique, no two functions may have the same name although they have different signature
// currently ensured by parser
@Options(prefix = "cpa.validVars")
public class ValidVarsCPA extends AbstractCPA implements ProofCheckerCPA {

  @Option(secure=true, name="merge", toUppercase=true, values={"SEP", "JOIN"},
      description="which merge operator to use for ValidVarsCPA")
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
