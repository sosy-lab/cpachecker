// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;

/**
 * An indexed identifier of SMT-LIB, e.g. {@code (_ extract 7 0)}. Its numeral indices select one
 * operator of a family, so they are kept apart from the symbol instead of in the name.
 */
public record SmtLibIndexedSymbol(String symbol, ImmutableList<Integer> indices)
    implements Serializable {

  public SmtLibIndexedSymbol {
    checkArgument(!indices.isEmpty(), "An indexed identifier has at least one index");
  }

  public static SmtLibIndexedSymbol of(String pSymbol, Integer... pIndices) {
    return new SmtLibIndexedSymbol(pSymbol, ImmutableList.copyOf(pIndices));
  }

  public int index(int pPosition) {
    return indices.get(pPosition);
  }

  @Override
  public String toString() {
    return "(_ " + symbol + " " + Joiner.on(' ').join(indices) + ")";
  }
}
