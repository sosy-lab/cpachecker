// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;

// TODO remove this class entirely and pass the bin expr builder -> better interface
/** A place to initialize, store and get static values used in the mpor package. */
public class MPORStatics {

  // Binary Expression Builder =====================================================================

  private static CBinaryExpressionBuilder binExprBuilder = null;

  public static boolean isBinExprBuilderSet() {
    return binExprBuilder != null;
  }

  public static CBinaryExpressionBuilder binExprBuilder() {
    checkArgument(binExprBuilder != null, "binExprBuilder was not initialized yet");
    return binExprBuilder;
  }

  public static void setBinExprBuilder(CBinaryExpressionBuilder pBinExprBuilder) {
    checkNotNull(pBinExprBuilder);
    checkArgument(binExprBuilder == null, "binExprBuilder was initialized already");
    binExprBuilder = pBinExprBuilder;
  }
}
