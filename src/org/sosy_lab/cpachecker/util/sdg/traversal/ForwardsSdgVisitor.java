// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.traversal;

import java.util.Collection;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph;

/**
 * Represents a {@link SdgVisitor} that is used for forwards traversals of system dependence graphs
 * (SDGs).
 *
 * @param <N> the node type of the SDG
 * @see SystemDependenceGraph#traverse(Collection, ForwardsSdgVisitor)
 */
public interface ForwardsSdgVisitor<N extends SystemDependenceGraph.Node<?, ?, ?>>
    extends SdgVisitor<N> {}
