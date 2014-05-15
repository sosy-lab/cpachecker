/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.counterexample;

import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

import com.google.common.collect.ImmutableSet;


public class CFAEdgeWithAssignments {

  private final CFAEdge edge;
  private final Set<Assignment> assignments;
  private final String edgeCode;
  private final String comment;

  public CFAEdgeWithAssignments(CFAEdge pEdge, Set<Assignment> pAssignments,
      @Nullable String pEdgeCode, @Nullable String pComment) {
    edge = pEdge;
    assignments = ImmutableSet.copyOf(pAssignments);
    edgeCode = pEdgeCode;
    comment = pComment;
  }

  public Set<Assignment> getAssignments() {
    return assignments;
  }

  public CFAEdge getCFAEdge() {
    return edge;
  }

  public String getAsCode() {
    return edgeCode;
  }

  @Override
  public String toString() {
    return edge.toString() + " " + assignments.toString();
  }

  public String getComment() {
    return comment;
  }
}