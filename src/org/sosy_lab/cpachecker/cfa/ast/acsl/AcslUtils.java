// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import java.util.function.BiPredicate;

public class AcslUtils {

  public static <T> boolean anyPermutationOf(BiPredicate<T, T> f, T a, T b) {
    return f.test(a, b) || f.test(b, a);
  }
}
