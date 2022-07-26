// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import org.sosy_lab.cpachecker.cfa.CFA;

/** Decomposes the CFA in coherent blocks */
public interface CFADecomposer {

  BlockTree cut(CFA cfa);
}
