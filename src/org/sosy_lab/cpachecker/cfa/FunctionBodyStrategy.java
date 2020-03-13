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
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

public class FunctionBodyStrategy extends AbstractCFAMutationStrategy {

  private final Set<String> answered = new TreeSet<>();
  private SortedSet<CFANode> deletedFunctionNodes;
  private FunctionEntryNode deletedFunctionEntry;

  public FunctionBodyStrategy(LogManager pLogger) {
    super(pLogger);
    // TODO Auto-generated constructor stub
  }

  @Override
  public long countPossibleMutations(ParseResult pParseResult) {
    return pParseResult.getFunctions().size();
  }

  @Override
  public boolean mutate(ParseResult pParseResult) {
    ImmutableSet<FunctionEntryNode> functionEntries =
        ImmutableSet.copyOf(pParseResult.getFunctions().values());
    for (FunctionEntryNode entryNode : functionEntries) {
      final String functionName = entryNode.getFunctionName();
      if (answered.contains(functionName)) {
        continue;
      }
      answered.add(functionName);

      logger.log(Level.INFO, "Removing function", functionName);

      deletedFunctionNodes = new TreeSet<>(pParseResult.getCFANodes().get(functionName));
      deletedFunctionEntry = entryNode;
      pParseResult.getCFANodes().removeAll(functionName);
      pParseResult.getFunctions().remove(functionName);

      return true;
    }
    return false;
  }

  @Override
  public void rollback(ParseResult pParseResult) {
    final String functionName = deletedFunctionEntry.getFunctionName();
    logger.logf(Level.FINE, "returning %s", functionName);
    pParseResult.getCFANodes().putAll(functionName, deletedFunctionNodes);
    pParseResult.getFunctions().put(functionName, deletedFunctionEntry);
  }
}
