// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;

/**
 * Represents a CFA post-processor.
 *
 * <p>During CFA creation, the following steps are executed (in this order):
 *
 * <ol>
 *   <li>Parse file(s) and create a {@link CfaConnectedness#UNCONNECTED_FUNCTIONS CFA for each
 *       function}.
 *   <li>Run CFA post-processors on each function CFA.
 *   <li>Insert function call and return edges and build a {@link CfaConnectedness#SUPERGRAPH single
 *       supergraph CFA}.
 *   <li>Run CFA post-processors on the supergraph CFA.
 * </ol>
 */
public interface CfaPostProcessor {

  /**
   * Executes this CFA post-processor on the specified {@link MutableCFA}.
   *
   * <p>A CFA post-processor may modify the specified {@link MutableCFA} or create a new {@link
   * MutableCFA}. In both cases, the returned {@link MutableCFA} represents the result of the
   * post-processor.
   *
   * @param pCfa the {@link MutableCFA} to post-process
   * @param pLogger the logger to use during CFA post-processing
   * @param pShutdownNotifier the shutdown notifier to use during CFA post-processing
   * @return the post-processed CFA (can be the specified {@link MutableCFA} or a completely new
   *     {@link MutableCFA})
   * @throws NullPointerException if any parameter is {@code null}
   */
  MutableCFA execute(MutableCFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier);
}
