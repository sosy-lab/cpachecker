// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.WithholdingStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockGraphPath;

/**
 * What a {@link PathBasedExplorationEngine} publishes to the successors besides postconditions,
 * because successors key their contexts by path and cannot infer it from the postconditions alone.
 *
 * @param retractedContexts the contexts of the block, identified by their path through the block
 *     graph, that no longer produce a postcondition, so that successors drop what they derived from
 *     them
 * @param withholding for every block the sender knows of, whether that block withholds the
 *     postcondition of a context, see {@link PathBasedPreconditionHandler#mayMissContexts()}
 */
record ContextUpdate(
    ImmutableList<BlockGraphPath> retractedContexts,
    ImmutableMap<String, WithholdingStatus> withholding) {}
