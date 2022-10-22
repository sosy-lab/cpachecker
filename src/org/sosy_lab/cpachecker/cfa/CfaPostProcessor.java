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
 * Represents a CFA post-processor.
 *
 * <p>CFA post-processors are executed in a specific order. During CFA creation, the following steps
 * are executed (in this order):
 *
 * <ol>
 *   <li>Parse file(s) and create a CFA for each function.
 *   <li>Do post-processings on each function CFA.
 *   <li>Insert call and return edges and build the supergraph.
 *   <li>Do post-processings that change the supergraph CFA.
 * </ol>
 */
public interface CfaPostProcessor {

  MutableCFA process(MutableCFA pCfa, LogManager pLogger);
}
