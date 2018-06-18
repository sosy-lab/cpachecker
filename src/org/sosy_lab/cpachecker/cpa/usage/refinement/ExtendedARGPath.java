/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.Pair;


public class ExtendedARGPath extends ARGPath {

  private final UsageInfo usage;
  //All blocks now check pairs
  private final Set<ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>> refinedAsTrueBy = new HashSet<>();
  private boolean isUnreachable = false;

  public ExtendedARGPath(ARGPath origin, UsageInfo target) {
    super(origin.asStatesList(), origin.getInnerEdges());
    usage = target;
  }

  public UsageInfo getUsageInfo() {
    return usage;
  }

  public void setAsTrueBy(ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> refiner) {
    refinedAsTrueBy.add(refiner);
  }

  public void setAsFalse() {
    isUnreachable = true;
  }

  public boolean isRefinedAsReachableBy(ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> refiner) {
    return refinedAsTrueBy.contains(refiner);
  }

  public boolean isUnreachable() {
    return isUnreachable;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (isUnreachable ? 1231 : 1237);
    result = prime * result + Objects.hashCode(refinedAsTrueBy);
    result = prime * result + Objects.hashCode(usage);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ExtendedARGPath other = (ExtendedARGPath) obj;
    return isUnreachable == other.isUnreachable
        && Objects.equals(refinedAsTrueBy, other.refinedAsTrueBy)
        && Objects.equals(usage, other.usage);
  }
}
