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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm.InstanceType;

/** A place to initialize, store and get static values. */
public class MPORStatics {

  // Algorithm Instance Type =======================================================================

  private static InstanceType instanceType = null;

  public static InstanceType instanceType() {
    checkArgument(instanceType != null, "instanceType was not initialized yet");
    return instanceType;
  }

  public static void setInstanceType(InstanceType pInstanceType) {
    checkNotNull(pInstanceType);
    checkArgument(instanceType == null, "instanceType was initialized already");
    instanceType = pInstanceType;
  }

  // Sequentialization Error =======================================================================

  private static String seqError = null;

  public static boolean isSeqErrorSet() {
    return seqError != null;
  }

  public static String seqError() {
    checkArgument(seqError != null, "sequentializationError was not initialized yet");
    return seqError;
  }

  public static void setSeqError(String pSeqError) {
    checkNotNull(pSeqError);
    checkArgument(seqError == null, "sequentializationError was initialized already");
    seqError = pSeqError;
  }

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
