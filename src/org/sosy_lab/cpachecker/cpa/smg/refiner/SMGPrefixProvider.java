/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;
import org.sosy_lab.cpachecker.util.refinement.InfeasiblePrefix;


public class SMGPrefixProvider extends GenericPrefixProvider<SMGState> {

  public SMGPrefixProvider(LogManager pLogger,
      CFA pCfa, Configuration pConfig, SMGState pInitialState)
          throws InvalidConfigurationException {


    super(
        new SMGStrongestPostOperator(pLogger, pConfig, pCfa),
        pInitialState,
        pLogger,
        pCfa,
        pConfig,
        SMGCPA.class);
  }

  @Override
  public List<InfeasiblePrefix> extractInfeasiblePrefixes(ARGPath pPath, SMGState pInitial)
      throws CPAException, InterruptedException {

    List<InfeasiblePrefix> prefixes = super.extractInfeasiblePrefixes(pPath, pInitial);

    // Due to SMGCPA producing infeasible paths without feasible prefixes, that may contain no state
    // after the last edge of the error path (for example invalid read in an assumption edge), check
    // if the prefix is the whole path, and return the path

    List<Integer> wrongPrefixes = new ArrayList<>();

    for (int i = 0; i < prefixes.size(); i++) {
      InfeasiblePrefix prefix = prefixes.get(i);

      if (prefix.getPath().size() == pPath.size()) {
        wrongPrefixes.add(i);
      }
    }

    for (int i : wrongPrefixes) {
      prefixes.remove(i);
    }

    return prefixes;
  }
}
