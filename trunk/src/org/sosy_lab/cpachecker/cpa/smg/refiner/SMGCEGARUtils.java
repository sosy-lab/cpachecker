/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.Precisions;

public class SMGCEGARUtils {

  private SMGCEGARUtils() {}

  public static SMGPrecision extractSMGPrecision(final ARGReachedSet pReached,
      ARGState state) {
    FluentIterable<Precision> precisions = Precisions
        .asIterable(pReached.asReachedSet().getPrecision(state));

    precisions = precisions.filter((Precision prec) -> {
      return prec instanceof SMGPrecision;
    });

    return (SMGPrecision) Iterables.getOnlyElement(precisions);
  }
}