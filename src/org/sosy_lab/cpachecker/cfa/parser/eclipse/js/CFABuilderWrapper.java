/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import java.util.function.BiFunction;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

interface CFABuilderWrapper {
  CFABuilder getBuilder();

  default CFABuilderWrapper addParseResult(final ParseResult pParseResult) {
    this.getBuilder().addParseResult(pParseResult);
    return this;
  }

  default CFABuilderWrapper appendTo(final CFABuilder pBuilder) {
    pBuilder.append(this.getBuilder());
    return this;
  }

  default CFABuilderWrapper append(final CFABuilder pBuilder) {
    this.getBuilder().append(pBuilder);
    return this;
  }

  default CFABuilderWrapper append(final CFABuilderWrapper pBuilder) {
    return this.append(pBuilder.getBuilder());
  }

  default CFABuilderWrapper appendEdge(
      final BiFunction<CFANode, CFANode, AbstractCFAEdge> pCreateEdge) {
    this.getBuilder().appendEdge(pCreateEdge);
    return this;
  }

  default CFABuilderWrapper appendEdge(
      final CFANode pNextNode, final BiFunction<CFANode, CFANode, AbstractCFAEdge> pCreateEdge) {
    this.getBuilder().appendEdge(pNextNode, pCreateEdge);
    return this;
  }

  default CFANode createNode() {
    return this.getBuilder().createNode();
  }

  default ParseResult getParseResult() {
    return this.getBuilder().getParseResult();
  }

  default CFANode getExitNode() {
    return this.getBuilder().getExitNode();
  }

  default LogManager getLogger() {
    return this.getBuilder().getLogger();
  }

  default String getFunctionName() {
    return this.getBuilder().getFunctionName();
  }
}
