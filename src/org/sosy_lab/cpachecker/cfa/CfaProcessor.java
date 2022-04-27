// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.common.log.LogManager;

/**
 * Marker interface for CFA processors.
 *
 * <p>CFA processors are executed in a specific order. During CFA creation, the following steps are
 * executed (in this order):
 *
 * <ol>
 *   <li>Parse file(s) and create a CFA for each function.
 *   <li>Do those post-processings on each single function CFA that are adding/removing nodes/edges
 *       ({@link ModifyingIndependentFunctionPostProcessor}).
 *   <li>Do read-only post-processings on each single function CFA ({@link
 *       ReadOnlyIndependentFunctionPostProcessor}).
 *   <li>Insert call and return edges and build the supergraph.
 *   <li>Do those post-processings that change the supergraph CFA by adding/removing nodes/edges
 *       ({@link ModifyingSupergraphPostProcessor}).
 *   <li>Collect information about the finished supergraph CFA without modifying the CFA ({@link
 *       ReadOnlySupergraphPostProcessor}).
 * </ol>
 */
public interface CfaProcessor {

  @FunctionalInterface
  public interface ModifyingIndependentFunctionPostProcessor extends CfaProcessor {

    MutableCFA process(MutableCFA pCfa, LogManager pLogger);
  }

  @FunctionalInterface
  public interface ReadOnlyIndependentFunctionPostProcessor extends CfaProcessor {

    void process(MutableCFA pCfa, LogManager pLogger);
  }

  @FunctionalInterface
  public interface ModifyingSupergraphPostProcessor extends CfaProcessor {

    MutableCFA process(MutableCFA pCfa, LogManager pLogger);
  }

  @FunctionalInterface
  public interface ReadOnlySupergraphPostProcessor extends CfaProcessor {

    void process(MutableCFA pCfa, LogManager pLogger);
  }
}
