/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.mcmillan.waitlist;

import java.util.Map;

import org.sosy_lab.common.Timer;
import org.sosy_lab.cpachecker.util.predicates.ExtendedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.TheoremProver;

import com.google.common.collect.Maps;


public class Solver {

  private final ExtendedFormulaManager fmgr;
  private final TheoremProver prover;

  private final Map<Formula, Boolean> implicationCache = Maps.newHashMap();

  final Timer solverTime = new Timer();
  int implicationChecks = 0;
  int trivialImplicationChecks = 0;
  int cachedImplicationChecks = 0;

  public Solver(ExtendedFormulaManager pFmgr, TheoremProver pProver) {
    fmgr = pFmgr;
    prover = pProver;
  }

  public boolean implies(Formula a, Formula b) {
    implicationChecks++;

    if (a.isFalse() || b.isTrue()) {
      trivialImplicationChecks++;
      return true;
    }
    if (a.isTrue() || b.isFalse()) {
      // "true" implies only "true", but b is not "true"
      // "false" is implied only by "false", but a is not "false"
      trivialImplicationChecks++;
      return false;
    }
    if (a.equals(b)) {
      trivialImplicationChecks++;
      return true;
    }

    Formula f = fmgr.makeNot(fmgr.makeImplication(a, b));

    Boolean result = implicationCache.get(f);
    if (result != null) {
      cachedImplicationChecks++;
      return result;
    }

    solverTime.start();
    prover.init();
    try {
      result = prover.isUnsat(f);
    } finally {
      prover.reset();
      solverTime.stop();
    }
    implicationCache.put(f, result);
    return result;
  }

}
