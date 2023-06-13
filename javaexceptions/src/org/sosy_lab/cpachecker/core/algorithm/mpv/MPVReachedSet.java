// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpv;

import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.AbstractSingleProperty;
import org.sosy_lab.cpachecker.core.algorithm.mpv.property.MultipleProperties;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ResultProviderReachedSet;

/**
 * Reached set for multi-property verification algorithm, which provides verification result for
 * each checked property.
 */
public class MPVReachedSet extends ForwardingReachedSet implements ResultProviderReachedSet {

  @Nullable private MultipleProperties multipleProperties = null;

  public MPVReachedSet(ReachedSet pDelegate) {
    super(pDelegate);
  }

  public void setMultipleProperties(final MultipleProperties pMultipleProperties) {
    multipleProperties = pMultipleProperties;
  }

  @Override
  public Result getOverallResult() {
    if (multipleProperties == null) {
      return Result.UNKNOWN;
    }
    return multipleProperties.getOverallResult();
  }

  @Override
  public void printResults(PrintStream out) {
    if (multipleProperties == null) {
      return;
    }
    out.println("Result per each property:");
    for (AbstractSingleProperty property : multipleProperties.getProperties()) {
      out.println("  Property '" + property + "': " + property.getResult());
    }
  }
}
