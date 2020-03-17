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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;

public abstract class GenericCFAMutationStrategy<ObjectKey, RollbackInfo>
    extends AbstractCFAMutationStrategy {

  private Deque<RollbackInfo> currentMutation = new ArrayDeque<>();
  private final Set<ObjectKey> previousMutations = new HashSet<>();
  private final int rate;

  public GenericCFAMutationStrategy(LogManager pLogger, int pRate) {
    super(pLogger);
    assert pRate >= 0;
    rate = pRate;
  }

  protected abstract Collection<ObjectKey> getAllObjects(ParseResult pParseResult);

  protected Collection<ObjectKey> getObjects(ParseResult pParseResult, int count) {
    List<ObjectKey> result = new ArrayList<>();

    int found = 0;
    for (ObjectKey object : getAllObjects(pParseResult)) {
      if (!canRemove(pParseResult, object)) {
        continue;
      }
      //      for (ObjectKey alreadyChosen : result) {
      //        if (!canRemoveInSameRound(object, alreadyChosen)) {
      //          continue;
      //        }
      //      }

      result.add(object);

      if (++found >= count) {
        break;
      }
    }

    return result;
  }

  protected boolean canRemove(
      @SuppressWarnings("unused") ParseResult pParseResult, ObjectKey pObject) {
    return !previousMutations.contains(pObject);
  }

  protected abstract RollbackInfo getRollbackInfo(ParseResult pParseResult, ObjectKey pObject);

  protected abstract void removeObject(ParseResult pParseResult, ObjectKey pObject);

  protected abstract void returnObject(ParseResult pParseResult, RollbackInfo pRollbackInfo);

  @Override
  public boolean mutate(ParseResult pParseResult) {
    currentMutation.clear();
    assert rate > 0;

    ImmutableCollection<ObjectKey> chosenObjects =
        ImmutableList.copyOf(getObjects(pParseResult, rate));
    if (chosenObjects.isEmpty()) {
      return false;
    }

    for (ObjectKey object : chosenObjects) {
      currentMutation.push(getRollbackInfo(pParseResult, object));
      removeObject(pParseResult, object);
    }

    previousMutations.addAll(chosenObjects);
    return true;
  }

  @Override
  public void rollback(ParseResult pParseResult) {
    Iterator<RollbackInfo> it = currentMutation.iterator();
    while (it.hasNext()) {
      returnObject(pParseResult, it.next());
    }
  }

  @Override
  public int countPossibleMutations(ParseResult parseResult) {
    int count = 0;
    for (ObjectKey o : getAllObjects(parseResult)) {
      if (canRemove(parseResult, o)) {
        count++;
      }
    }
    return count;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "(" + rate + ")";
  }
}
