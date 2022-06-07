// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.traversal;

import java.util.Collection;
import org.sosy_lab.cpachecker.util.sdg.SdgEdge;
import org.sosy_lab.cpachecker.util.sdg.SdgNode;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph;

/**
 * Represents a {@link SdgVisitor} that is used for forwards traversals of system dependence graphs
 * (SDGs).
 *
 * @param <V> the variable type of the SDG
 * @param <N> the SDG node type of the SDG
 * @param <E> the SDG edge type of the SDG
 * @see SystemDependenceGraph#traverse(Collection, ForwardsSdgVisitor)
 */
public interface ForwardsSdgVisitor<V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>>
    extends SdgVisitor<V, N, E> {}
