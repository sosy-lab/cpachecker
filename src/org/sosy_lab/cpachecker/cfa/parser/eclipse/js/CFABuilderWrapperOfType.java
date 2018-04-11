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
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

interface CFABuilderWrapperOfType<T extends CFABuilderWrapperOfType<T>> extends CFABuilderWrapper {

  @Override
  @SuppressWarnings("unchecked")
  default T addParseResult(final ParseResult pParseResult) {
    return (T) CFABuilderWrapper.super.addParseResult(pParseResult);
  }

  @Override
  @SuppressWarnings("unchecked")
  default T appendTo(final CFABuilder pBuilder) {
    return (T) CFABuilderWrapper.super.appendTo(pBuilder);
  }

  @Override
  @SuppressWarnings("unchecked")
  default T append(final CFABuilder pBuilder) {
    return (T) CFABuilderWrapper.super.append(pBuilder);
  }

  @Override
  @SuppressWarnings("unchecked")
  default T append(final CFABuilderWrapper pBuilder) {
    return (T) CFABuilderWrapper.super.append(pBuilder);
  }

  @Override
  @SuppressWarnings("unchecked")
  default T appendEdge(final BiFunction<CFANode, CFANode, AbstractCFAEdge> pCreateEdge) {
    return (T) CFABuilderWrapper.super.appendEdge(pCreateEdge);
  }

  @Override
  @SuppressWarnings("unchecked")
  default T appendEdge(
      final CFANode pNextNode, final BiFunction<CFANode, CFANode, AbstractCFAEdge> pCreateEdge) {
    return (T) CFABuilderWrapper.super.appendEdge(pNextNode, pCreateEdge);
  }
}
