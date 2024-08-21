// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Classes in this package are concerned with creating a Dependence Graph. A dependence graph
 * captures different kinds of dependencies between parts of a program. Parts of a program are
 * usually assignments and expressions, but inter-procedural dependencies, e.g. between methods, are
 * also possible.
 */
package org.sosy_lab.cpachecker.util.dependencegraph;
