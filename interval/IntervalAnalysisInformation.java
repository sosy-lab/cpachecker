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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.interval;


import com.google.common.base.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public class IntervalAnalysisInformation {

  public static final IntervalAnalysisInformation EMPTY = new IntervalAnalysisInformation();

  final PersistentMap<String, Interval> assignments;

  protected IntervalAnalysisInformation(final PersistentMap<String, Interval> pAssignments){
    assignments = pAssignments;
  }

  private IntervalAnalysisInformation(){assignments = PathCopyingPersistentTreeMap.of();}

  @Override
  public boolean equals(Object o){
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IntervalAnalysisInformation that = (IntervalAnalysisInformation) o;
    return assignments.equals(that.assignments);
  }

  public PersistentMap<String, Interval> getAssignments() {
    return assignments;
  }

  @Override
  public int hashCode(){return Objects.hashCode(assignments);}

  @Override
  public String toString(){return "IntervalAnalysisInformation[" + assignments + "]";}
}
