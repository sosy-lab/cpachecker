// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package contains interfaces and implementations for representing CFAs as {@link
 * com.google.common.graph.Network networks} and {@link com.google.common.graph.Graph graphs}.
 *
 * <p>The key idea is that all connections of a CFA are fully defined by a single {@link
 * org.sosy_lab.cpachecker.cfa.graph.CfaNetwork CfaNetwork} instead of its individual elements
 * (i.e., its nodes and edges). This offers a great amount of flexibility as different views of a
 * CFA can be created without actually changing the CFA (e.g., by applying simple on-the-fly filters
 * or transformations).
 *
 * <p>There are two main interfaces:
 *
 * <ul>
 *   <li>{@link org.sosy_lab.cpachecker.cfa.graph.CfaNetwork CfaNetwork} extends {@link
 *       com.google.common.graph.Network Network}: All {@link com.google.common.graph.Network
 *       Network} implementations representing CFAs implement this interface. There are static
 *       factory methods for {@link
 *       org.sosy_lab.cpachecker.cfa.graph.CfaNetwork#wrap(org.sosy_lab.cpachecker.cfa.CFA)
 *       wrapping} existing CFAs and applying simple on-the-fly filters and transformations.
 *   <li>{@link org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork MutableCfaNetwork} extends
 *       {@link org.sosy_lab.cpachecker.cfa.graph.CfaNetwork CfaNetwork} and {@link
 *       com.google.common.graph.MutableNetwork MutableNetwork}: The mutable version of a {@link
 *       org.sosy_lab.cpachecker.cfa.graph.CfaNetwork CfaNetwork} that also provides basic methods
 *       for modifying CFAs, like adding and removing nodes/edges. Existing {@link
 *       org.sosy_lab.cpachecker.cfa.MutableCFA MutableCFA} instances can be {@link
 *       org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork#wrap(org.sosy_lab.cpachecker.cfa.MutableCFA)
 *       wrapped} to create a {@link org.sosy_lab.cpachecker.cfa.graph.MutableCfaNetwork
 *       MutableCfaNetwork} that modifies the underlying {@link
 *       org.sosy_lab.cpachecker.cfa.MutableCFA MutableCFA}.
 * </ul>
 */
package org.sosy_lab.cpachecker.cfa.graph;
