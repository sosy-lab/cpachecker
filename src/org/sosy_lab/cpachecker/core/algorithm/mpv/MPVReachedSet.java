/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
