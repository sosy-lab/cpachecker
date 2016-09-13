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
package org.sosy_lab.cpachecker.cpa.defuse;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Options(prefix = "cpa.defuse")
public class DefUseCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DefUseCPA.class);
  }

  @Option(secure=true, name="merge", values={"sep", "join"},
      description="which merge operator to use for DefUseCPA")
  private String mergeType = "sep";

  private DefUseCPA(Configuration config) throws InvalidConfigurationException {
    super(
        "irrelevant", // operator-initialization is overridden
        "SEP",
        new DefUseDomain(),
        new DefUseTransferRelation());
    config.inject(this);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    Set<DefUseDefinition> defUseDefinitions = new HashSet<>();
    if (pNode instanceof CFunctionEntryNode) {
      List<String> parameterNames = ((CFunctionEntryNode)pNode).getFunctionParameterNames();

      for (String parameterName : parameterNames) {
        DefUseDefinition newDef = new DefUseDefinition(parameterName, null);
        defUseDefinitions.add(newDef);
      }
    }

    return new DefUseState(defUseDefinitions);
  }
}
