// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public record ThreadSyncFlagsBuilder(MPOROptions options, ImmutableList<MPORThread> threads) {

  public ThreadSyncFlags buildThreadSyncFlags() {
    ImmutableMap.Builder<MPORThread, CIdExpression> syncFlags = ImmutableMap.builder();
    for (MPORThread thread : threads) {
      String name = SeqNameUtil.buildThreadPrefix(options, thread.id()) + "_SYNC";
      // use unsigned char (8 bit), we only need values 0 and 1
      CIdExpression sync =
          SeqExpressionBuilder.buildIdExpressionWithIntegerInitializer(
              // TODO a thread could also start with pthread_mutex_lock -> initialize with 1
              true, CNumericTypes.UNSIGNED_CHAR, name, SeqInitializers.INT_0);
      syncFlags.put(thread, sync);
    }
    return new ThreadSyncFlags(syncFlags.buildOrThrow());
  }
}
