// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Defines {@link com.google.common.graph.Network Network} and {@link com.google.common.graph.Graph
 * Graph} implementations for CFAs.
 *
 * <p>The key idea is that all connections of a CFA are fully defined by a {@link
 * org.sosy_lab.cpachecker.cfa.graph.CfaNetwork CfaNetwork} instead of its individual elements
 * (i.e., its nodes and edges). This offers a great amount of flexibility as different views of a
 * CFA can be created without changing the original CFA (e.g., by applying on-the-fly filters or
 * transformations). Additionally, this makes modifying CFAs easier, because for some
 * implementations it isn't necessary to keep individual elements of a CFA in sync (e.g., depending
 * on the implementation, we don't have to create a CFA edge with specific endpoints and also
 * add/register the edge at those endpoints).
 *
 * <p>There are three main interfaces:
 *
 * <ul>
 *   <li>{@link org.sosy_lab.cpachecker.cfa.graph.CfaNetwork CfaNetwork} extends {@link
 *       com.google.common.graph.Network Network}: All {@link com.google.common.graph.Network
 *       Network} implementations representing CFAs implement this interface. There are methods for
 *       {@link org.sosy_lab.cpachecker.cfa.graph.CfaNetwork#wrap(org.sosy_lab.cpachecker.cfa.CFA)
 *       wrapping} existing CFAs and applying on-the-fly filters and transformations.
 *   <li>{@link org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork MutableCfaNetwork} extends
 *       {@link org.sosy_lab.cpachecker.cfa.graph.CfaNetwork CfaNetwork} and {@link
 *       com.google.common.graph.MutableNetwork MutableNetwork}: The mutable version of a {@link
 *       org.sosy_lab.cpachecker.cfa.graph.CfaNetwork CfaNetwork} that also provides basic methods
 *       for modifying CFAs. Existing {@link org.sosy_lab.cpachecker.cfa.MutableCFA MutableCFA}
 *       instances can be {@link
 *       org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork#wrap(org.sosy_lab.cpachecker.cfa.MutableCFA)
 *       wrapped}.
 *   <li>{@link org.sosy_lab.cpachecker.cfa.graph.FlexCfaNetwork FlexCfaNetwork} extends {@link
 *       org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork MutableCfaNetwork}: A flexible {@link
 *       org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork MutableCfaNetwork} that allows for more
 *       advanced operations that wouldn't be possible for all {@link
 *       org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork MutableCfaNetwork} implementations.
 * </ul>
 */
package org.sosy_lab.cpachecker.cfa.graph;
