// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Contains wrapper classes so that CExpressions can be used as SeqExpressions. It's not a nice
 * solution but required if we want to use both logical AND, OR, NOT expressions and CIdExpressions
 * in the same function call.
 */
package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.expression.c_to_seq;
