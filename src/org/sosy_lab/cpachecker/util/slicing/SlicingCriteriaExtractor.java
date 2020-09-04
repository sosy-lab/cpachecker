// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.specification.Specification;

public interface SlicingCriteriaExtractor {

  /**
   * Extracts from <code>pCfa</code> a set of slicing criteria, i.e., a subset of the CFA's edges.
   * The slicing criteria depends on the type of extractor, the program, and the property <code>
   * pError</code>.
   *
   * @param pCfa program for which slicing criteria should be extracted
   * @param pError specification of non-reachability property
   * @param pShutdownNotifier notifies whether a shutdown was requested and extraction of slicing
   *     criteria should be aborted
   * @param pLogger object for writing log messages
   * @return subset of CFA edges
   * @throws InterruptedException if a shutdown is requested and this is detected with the help of
   *     the <code>pShutdownNotifier</code>
   */
  public Set<CFAEdge> getSlicingCriteria(
      CFA pCfa, Specification pError, ShutdownNotifier pShutdownNotifier, LogManager pLogger)
      throws InterruptedException;
}
