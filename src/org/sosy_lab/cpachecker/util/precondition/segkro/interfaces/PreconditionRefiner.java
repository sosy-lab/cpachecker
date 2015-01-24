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
package org.sosy_lab.cpachecker.util.precondition.segkro.interfaces;

import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathPosition;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;


public interface PreconditionRefiner {

  /**
   * Procedure that provides a new precision for refining preconditions.
   * The goal of this refinement process is to provide a
   * necessary and sufficient precondition.
   *
   * The refinement is done based on two program traces:
   *
   * @param pTraceToViolation         Trace to the error location
   * @param pTraceToValidTermination  Trace to the exit location of the entry function
   *
   * @param pWpPosition   The goal is to provide a precondition for this location.
   *
   * @return
   *
   * @throws SolverException
   * @throws InterruptedException
   * @throws CPATransferException
   */
  public abstract PredicatePrecision refine(
      final PathPosition pTraceToViolation,
      final PathPosition pTraceToValidTermination)
    throws SolverException, InterruptedException, CPATransferException;

}