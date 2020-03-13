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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.util.Pair;

public class GlobalDeclarationStrategy extends AbstractCFAMutationStrategy {
  private final Set<Pair<ADeclaration, String>> answered = new HashSet<>();
  private final Stack<Pair<Pair<ADeclaration, String>, Integer>> lastAnswer = new Stack<>();
  private int atATime;

  public GlobalDeclarationStrategy(LogManager pLogger, int pAtATime) {
    super(pLogger);
    atATime = pAtATime;
  }

  private boolean canDeleteDeclarationPair(ParseResult pParseResult, Pair<ADeclaration, String> p) {
    ADeclaration decl = p.getFirst();
    if (decl instanceof AFunctionDeclaration
        && pParseResult.getFunctions().containsKey(decl.getName())) {
      return false;
    }
    return true;
  }

  @Override
  public long countPossibleMutations(ParseResult pParseResult) {
    int count = 0;
    for (Pair<ADeclaration, String> p : pParseResult.getGlobalDeclarations()) {
      if (canDeleteDeclarationPair(pParseResult, p)) {
        count++;
      }
    }
    if (atATime == 0) {
      atATime = (int) Math.round(Math.sqrt(count));
    }
    return count;
  }

  @Override
  public boolean mutate(ParseResult pParseResult) {
    lastAnswer.clear();

    List<Pair<ADeclaration, String>> prgd = pParseResult.getGlobalDeclarations();

    if (prgd.isEmpty()) {
      return false;
    }

    int foundToDelete = 0;
    for (int i = prgd.size() - 1; i >= 0; i--) {
      Pair<ADeclaration, String> p = prgd.get(i);
      if (answered.contains(p) || !canDeleteDeclarationPair(pParseResult, p)) {
        continue;
      }

      prgd.remove(p);
      lastAnswer.push(Pair.of(p, i));
      answered.add(p);
      logger.logf(
          Level.INFO,
          "removed from global declarations at index %d: %s %s, %s",
          i,
          p.getFirst().getType(),
          p.getFirst().getOrigName(),
          p.getFirst().getClass().getSimpleName());

      if (++foundToDelete >= atATime) {
        break;
      }
    }

    pParseResult =
        new ParseResult(
            pParseResult.getFunctions(),
            pParseResult.getCFANodes(),
            prgd,
            pParseResult.getFileNames());
    return foundToDelete > 0;
  }

  @Override
  public void rollback(ParseResult pParseResult) {
    List<Pair<ADeclaration, String>> prgd = pParseResult.getGlobalDeclarations();
    for (Pair<Pair<ADeclaration, String>, Integer> pp : lastAnswer) {
      prgd.add(pp.getSecond(), pp.getFirst());
    }
    pParseResult =
        new ParseResult(
            pParseResult.getFunctions(),
            pParseResult.getCFANodes(),
            prgd,
            pParseResult.getFileNames());
  }
}
