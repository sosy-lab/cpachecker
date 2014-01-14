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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

import com.google.common.base.Preconditions;


public class Sideassignments {

  private final Deque<List<CAstNode>> preSideAssignments;
  private final Deque<List<CAstNode>> postSideAssignments;
  private final Deque<List<Pair<IASTExpression, CIdExpression>>> conditionalExpressions;

  public Sideassignments() {
    preSideAssignments = new ArrayDeque<>();
    postSideAssignments = new ArrayDeque<>();
    conditionalExpressions = new ArrayDeque<>();
  }

  public Sideassignments(Deque<List<CAstNode>> preSideAssignments, Deque<List<CAstNode>> postSideAssignments, Deque<List<Pair<IASTExpression, CIdExpression>>> conditionalExpressions) {
    this.preSideAssignments = preSideAssignments;
    this.postSideAssignments = postSideAssignments;
    this.conditionalExpressions = conditionalExpressions;
  }

  public void enterBlock() {
    preSideAssignments.push(new ArrayList<CAstNode>());
    postSideAssignments.push(new ArrayList<CAstNode>());
    conditionalExpressions.push(new ArrayList<Pair<IASTExpression, CIdExpression>>());
  }

  public void leaveBlock() {
    Preconditions.checkArgument(!preSideAssignments.isEmpty(), "leaving sideassignment block before handling all of them");
    Preconditions.checkArgument(!postSideAssignments.isEmpty(), "leaving sideassignment block before handling all of them");
    Preconditions.checkArgument(!conditionalExpressions.isEmpty(), "leaving sideassignment block before handling all of them");
    preSideAssignments.pop();
    postSideAssignments.pop();
    conditionalExpressions.pop();
  }

  public List<CAstNode> getAndResetPreSideAssignments() {
    List<CAstNode> result = new ArrayList<>(preSideAssignments.peek());
    preSideAssignments.peek().clear();
    return result;
  }

  public List<CAstNode> getAndResetPostSideAssignments() {
    List<CAstNode> result = new ArrayList<>(postSideAssignments.peek());
    postSideAssignments.peek().clear();
    return result;
  }

  public List<Pair<IASTExpression, CIdExpression>> getAndResetConditionalExpressions() {
    List<Pair<IASTExpression, CIdExpression>> result = new ArrayList<>(conditionalExpressions.peek());
    conditionalExpressions.peek().clear();
    return result;
  }

  public boolean hasConditionalExpression() {
    if (conditionalExpressions.isEmpty()) {
      return false;
    }
    return !conditionalExpressions.peek().isEmpty();
  }

  public boolean hasPreSideAssignments() {
    if (preSideAssignments.isEmpty()) {
      return false;
    }
    return !preSideAssignments.peek().isEmpty();
  }

  public boolean hasPostSideAssignments() {
    if (postSideAssignments.isEmpty()) {
      return false;
    }
    return !postSideAssignments.peek().isEmpty();
  }

  public List<Pair<IASTExpression, CIdExpression>> getConditionalExpressions() {
    return Collections.unmodifiableList(conditionalExpressions.peek());
  }

  public void addConditionalExpression(IASTExpression e, CIdExpression tempVar) {
    conditionalExpressions.peek().add(Pair.of(checkNotNull(e), checkNotNull(tempVar)));
  }

  public void addPreSideAssignment(CAstNode node) {
    preSideAssignments.peek().add(node);
  }

  public void addPostSideAssignment(CAstNode node) {
    postSideAssignments.peek().add(node);
  }
}
