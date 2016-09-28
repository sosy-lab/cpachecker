/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.util.Set;

public class SMGUseGraphBuilder {

  public LogManager logger;
  public SMGFeasibilityChecker checker;

  public SMGUseGraphBuilder(LogManager pLogger, SMGFeasibilityChecker pChecker) {
    logger = pLogger;
    checker = pChecker;
  }

  public SMGUseGraph<SMGUseFieldVertice, SMGUseFieldEdge> createUseGraph(ARGPath path) throws CPAException, InterruptedException {

    Set<SMGUseFieldEdge> pGraphEdge = createUseFieldEdgesFromPath(path);
    return new SMGUseGraph<>(pGraphEdge);
  }

  private Set<SMGUseFieldEdge> createUseFieldEdgesFromPath(ARGPath pPath) throws CPAException, InterruptedException {

    return null;
  }
}