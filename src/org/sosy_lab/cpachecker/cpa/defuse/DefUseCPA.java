// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.defuse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

@Options(prefix = "cpa.defuse")
public class DefUseCPA extends AbstractCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(DefUseCPA.class);
  }

  @Option(
      secure = true,
      name = "merge",
      values = {"sep", "join"},
      description = "which merge operator to use for DefUseCPA")
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
      List<String> parameterNames = ((CFunctionEntryNode) pNode).getFunctionParameterNames();

      for (String parameterName : parameterNames) {
        DefUseDefinition newDef = new DefUseDefinition(parameterName, null);
        defUseDefinitions.add(newDef);
      }
    }

    return new DefUseState(defUseDefinitions);
  }
}
