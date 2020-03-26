/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.mutation.strategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.Pair;

@Options
public class FunctionStrategy
    extends GenericCFAMutationStrategy<String, Pair<FunctionEntryNode, SortedSet<CFANode>>> {
  @Option(
      secure = true,
      name = "mutations.functionsWhitelist",
      description = "Names of functions (separated with space) that should not be deleted from CFA")
  private String whitelist = "main";

  public FunctionStrategy(Configuration pConfig, LogManager pLogger, int pRate, int pStartDepth)
      throws InvalidConfigurationException {
    super(pLogger, pRate, pStartDepth, "Functions");
    pConfig.inject(this);
  }

  @Override
  protected Collection<String> getAllObjects(ParseResult pParseResult) {
    class FunctionSize implements Comparator<String> {
      private final ParseResult parseResult;
      public FunctionSize(final ParseResult pr) {
        parseResult = pr;
      }
      @Override
      public int compare(String pArg0, String pArg1) {
        return parseResult.getCFANodes().get(pArg0).size()
            - parseResult.getCFANodes().get(pArg1).size();
      }
    }
    List<String> v = List.of(whitelist.split(" "));
    List<String> answer = new ArrayList<>(pParseResult.getFunctions().keySet());
    answer.removeAll(v);
    Collections.sort(answer, new FunctionSize(pParseResult));
    return answer;
  }

  @Override
  protected Pair<FunctionEntryNode, SortedSet<CFANode>> getRollbackInfo(ParseResult pParseResult, String pObject) {
    return Pair.of(
        pParseResult.getFunctions().get(pObject),
        new TreeSet<>(pParseResult.getCFANodes().get(pObject)));
  }

  @Override
  protected void removeObject(ParseResult pParseResult, String functionName) {
    logger.logf(
        Level.INFO,
        "removing %s (entry is %s, %d nodes)",
        functionName,
        pParseResult.getFunctions().get(functionName),
        pParseResult.getCFANodes().get(functionName).size());
    pParseResult.getCFANodes().removeAll(functionName);
    pParseResult.getFunctions().remove(functionName);
  }

  @Override
  protected void returnObject(
      ParseResult pParseResult, Pair<FunctionEntryNode, SortedSet<CFANode>> pRollbackInfo) {
    String functionName = pRollbackInfo.getFirst().getFunctionName();
    logger.logf(
        Level.INFO,
        "returning %s (entry is %s, %d nodes)",
        functionName,
        pRollbackInfo.getFirst(),
        pRollbackInfo.getSecond().size());
    pParseResult.getCFANodes().putAll(functionName, pRollbackInfo.getSecond());
    pParseResult.getFunctions().put(functionName, pRollbackInfo.getFirst());
  }
}
