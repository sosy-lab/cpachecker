// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Distributed analyses take known CPAs {@link
 * org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis} and distribute them to many
 * workers. These workers execute the CPA on a subgraph of the CFA.
 */
package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;
