/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit.refiner;

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.StaticRefiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

public class ExplicitStaticRefiner extends StaticRefiner {

  private ExplicitPrecision explicitPrecision;

  public ExplicitStaticRefiner(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa,
      ExplicitPrecision initialPrecision) throws InvalidConfigurationException {
    super(pConfig, pLogger, pCfa);

    explicitPrecision = initialPrecision;
  }

  @Override
  public ExplicitPrecision extractPrecisionFromCfa() throws CPATransferException {
    logger.log(Level.INFO, "Extracting precision from CFA...");

    VariableScopeProvider scopeProvider           = new VariableScopeProvider(cfa);
    ListMultimap<CFANode, AssumeEdge> locAssumes  = getTargetLocationAssumes(cfa);
    Multimap<CFANode, String> increment           = HashMultimap.create();

    for (CFANode targetLocation : locAssumes.keySet()) {
      for (AssumeEdge assume : locAssumes.get(targetLocation)) {
        String function = assume.getPredecessor().getFunctionName();
        for (String var : getQualifiedVariablesOfAssume(assume)) {
          if (scopeProvider.isDeclaredInFunction(function, var)) {
            var = function + "::" + var;
          }

          increment.put(assume.getSuccessor(), var);
        }
      }
    }

    return new ExplicitPrecision(explicitPrecision, increment);
  }
}
