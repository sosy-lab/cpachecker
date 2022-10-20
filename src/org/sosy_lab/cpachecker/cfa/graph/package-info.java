// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Defines {@link Network} and {@link Graph} implementations for CFAs.
 *
 * <p>The key idea is that all connections of a CFA are fully defined by a {@link CfaNetwork}
 * instead of its individual elements (i.e., its nodes and edges). This offers a great amount of
 * flexibility as different views of a CFA can be created without changing the original CFA (e.g.,
 * by applying on-the-fly filters or transformations). Additionally, this makes modifying CFAs
 * easier, because for some implementations it isn't necessary to keep individual elements of a CFA
 * in sync (e.g., depending on the implementation, we don't need to create a CFA edge with specific
 * endpoints and also add/register the edge at those endpoints).
 *
 * <p>There are three main interfaces:
 *
 * <ul>
 *   <li>{@link CfaNetwork} extends {@link Network}: All {@link Network} implementations
 *       representing CFAs implement this interface. There are methods for {@link
 *       CfaNetwork#wrap(CFA) wrapping} existing CFAs and applying on-the-fly filters and
 *       transformations.
 *   <li>{@link MutableCfaNetwork} extends {@link CfaNetwork} and {@link MutableNetwork}: The
 *       mutable version of a {@link CfaNetwork} that also provides basic methods for modifying
 *       CFAs. Existing {@link MutableCFA} instances can be {@link
 *       MutableCfaNetwork#wrap(MutableCFA) wrapped}.
 *   <li>{@link FlexCfaNetwork} extends {@link MutableCfaNetwork}: A flexible {@link
 *       MutableCfaNetwork} that allows for more advanced operations that wouldn't be possible for
 *       all {@link MutableCfaNetwork} implementations.
 * </ul>
 */
package org.sosy_lab.cpachecker.cfa.graph;

import com.google.common.graph.Graph;
import com.google.common.graph.Network;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
