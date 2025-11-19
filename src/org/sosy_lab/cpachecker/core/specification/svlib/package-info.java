// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * This package and its sub-packages contain usefull classes to work with SV-LIB specifications. In
 * contrast to other languages where this is handled directly as an automaton, for SV-LIB the
 * specification is part of the program, and therefore requires some special treatment in some
 * cases.
 *
 * <p>Of particular note are traces and annotations like `:assert` and `:invariant`.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.core.specification.svlib;
