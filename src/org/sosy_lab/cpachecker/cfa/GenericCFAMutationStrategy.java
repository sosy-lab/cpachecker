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
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;

public abstract class GenericCFAMutationStrategy<ObjectKey, RollbackInfo>
    extends AbstractCFAMutationStrategy {

  private Deque<RollbackInfo> currentMutation = new ArrayDeque<>();
  private final Set<ObjectKey> previousMutations = new HashSet<>();
  private final int rate;
  private int depth;
  private int batchNum = -1;
  private int batchSize = -1;
  private int batchCount = -1;
  private int before = -1;
  private int after = -1;

  public GenericCFAMutationStrategy(LogManager pLogger, int pRate, int pStartDepth) {
    super(pLogger);
    assert pRate >= 0;
    rate = pRate;
    depth = pStartDepth;
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

    if (batchNum == batchCount && !goNextLevel(pParseResult)) {
      return false;
    } else {
      logger.logf(Level.SEVERE, "Batch number %d / %d", ++batchNum, batchCount);
    }

    ImmutableCollection<ObjectKey> chosenObjects =
        ImmutableList.copyOf(getObjects(pParseResult, batchSize));
    if (chosenObjects.isEmpty()) {
      if (!goNextLevel(pParseResult)) {
        return false;
      } else {
        batchNum++;
        chosenObjects = ImmutableList.copyOf(getObjects(pParseResult, batchSize));
        if (chosenObjects.isEmpty()) {
          return false;
        }
      }
    }

    logger.logf(
        Level.SEVERE,
        "Depth %d, Batch %d. Removed %d objects",
        depth,
        batchNum,
        chosenObjects.size());

    after -= chosenObjects.size();
    System.out.flush();
    for (ObjectKey object : chosenObjects) {
      currentMutation.push(getRollbackInfo(pParseResult, object));
      removeObject(pParseResult, object);
    }
    System.out.flush();

    previousMutations.addAll(chosenObjects);
    return true;
  }

  private void initBatch(ParseResult pParseResult) {
    if (depth == 0) {
      batchCount = 1;
    } else if (depth == 1) {
      batchCount = rate;
    } else {
      assert false;
    }

    before = countPossibleMutations(pParseResult);
    after = before;
    batchSize = before;
    if (depth == 1) {
      batchSize = (batchSize - 1) / rate + 1;
    }
    logger.logf(Level.SEVERE, "batchsize init %d at depth %d", batchSize, depth);
  }

  private boolean goNextLevel(ParseResult pParseResult) {
    if (batchSize == -1) {
      initBatch(pParseResult);
      batchNum = 0;
      return true;
    } else if (batchSize == 1) {
      for (ObjectKey o : getAllObjects(pParseResult)) {
        System.out.println("" + this + " remained " + o);
      }
      return false;
    }

    depth++;
    batchNum = 0;
    batchCount = batchCount * rate;
    batchSize = (batchSize - 1) / rate + 1;

    logger.logf(Level.SEVERE, "previous mutations was %d", previousMutations.size());
    previousMutations.clear();
    logger.logf(
        Level.SEVERE, "batch size updated %d x %d at depth %d", batchSize, batchCount, depth);
    return true;
  }

  @Override
  public void rollback(ParseResult pParseResult) {
    System.out.flush();
    logger.logf(Level.SEVERE, "rollbacked %d", currentMutation.size());
    after += currentMutation.size();
    Iterator<RollbackInfo> it = currentMutation.iterator();
    while (it.hasNext()) {
      returnObject(pParseResult, it.next());
    }
    System.out.flush();
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
    return this.getClass().getSimpleName()
        + "("
        + rate
        + "), before: "
        + before
        + ", after: "
        + after;
  }
}
