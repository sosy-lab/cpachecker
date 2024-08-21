// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * {@link org.sosy_lab.cpachecker.util.cwriter.PathToCTranslator}: Converting [subset of] CFA back
 * to the C code (with no loops). Generates Counterexample.c.
 *
 * <p>{@link org.sosy_lab.cpachecker.util.cwriter.CExpressionInvariantExporter} writes the input
 * program back, with generated invariants inserted as {@code __VERIFIER_assume} statements.
 */
package org.sosy_lab.cpachecker.util.cwriter;
