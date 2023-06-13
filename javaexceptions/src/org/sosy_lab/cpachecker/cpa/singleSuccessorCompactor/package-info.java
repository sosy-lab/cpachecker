// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This CPA should be used as wrapper-CPA. It's location in the CPA-hierarchy should be between ARG-
 * and Composite-CPA. The transfer-relation calls the wrapped transfer-relation as long as there is
 * only/exactly one succeeding abstract state. This can be beneficial for very special programs like
 * RERS/ECA, where long chains of abstract states are part of the CFA.
 *
 * <p>The behavior is comparable with MultiEdges, but in contrast to them, this CPA is not depending
 * on chains in the CFA, because the wrapped transfer-relation might be sufficient to exclude
 * branches and only traverse a chain of CFA-nodes.
 */
package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;
