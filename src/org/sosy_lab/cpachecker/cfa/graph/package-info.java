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
 * <p>The key idea is that, ignoring all summary edges, all connections of a CFA are fully defined
 * by a single {@link org.sosy_lab.cpachecker.cfa.graph.CfaNetwork CfaNetwork} instead of its
 * individual elements (i.e., its nodes and edges). This offers a great amount of flexibility as
 * different views of a CFA can be created without actually changing the CFA (e.g., by applying
 * simple on-the-fly filters or transformations). Additionally, this improves interoperability with
 * Guava's graph library and more abstract algorithms that work on graphs in general can be used.
 */
package org.sosy_lab.cpachecker.cfa.graph;
