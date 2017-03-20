/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage.storage;

import java.util.Set;
import java.util.SortedSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo.Access;
import org.sosy_lab.cpachecker.util.Pair;

@Options(prefix="cpa.usage.unsafedetector")
public class UnsafeDetector {
  @Option(name="ignoreEmptyLockset", description="ignore unsafes only with empty callstacks")
  private boolean ignoreEmptyLockset = false;

  public UnsafeDetector(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  public boolean isUnsafe(AbstractUsagePointSet set) {
    if (set instanceof RefinedUsagePointSet) {
      return true;
    }
    return isUnsafe((UnrefinedUsagePointSet)set);
  }

  public boolean isUnsafe(Set<UsageInfo> set) {
    return isUnsafe(preparePointSet(set));
  }

  private UnrefinedUsagePointSet preparePointSet(Set<UsageInfo> set) {
    UnrefinedUsagePointSet tmpSet = new UnrefinedUsagePointSet();
    for (UsageInfo uinfo : set) {
      tmpSet.add(uinfo);
    }
    return tmpSet;
  }

  private boolean isUnsafe(UnrefinedUsagePointSet set) {
    return isUnsafe(set.getTopUsages());
  }

  public Pair<UsageInfo, UsageInfo> getUnsafePair(AbstractUsagePointSet set) {
    assert isUnsafe(set);

    if (set instanceof RefinedUsagePointSet) {
      return ((RefinedUsagePointSet)set).getUnsafePair();
    } else {
      UnrefinedUsagePointSet unrefinedSet = (UnrefinedUsagePointSet) set;
      Pair<UsagePoint, UsagePoint> result = getUnsafePair(unrefinedSet.getTopUsages());

      assert result != null;

      return Pair.of(unrefinedSet.getUsageInfo(result.getFirst()).getOneExample(),
          unrefinedSet.getUsageInfo(result.getSecond()).getOneExample());
    }
  }

  public Pair<UsagePoint, UsagePoint> getUnsafePointPair(UnrefinedUsagePointSet set) {
    return getUnsafePair(set.getTopUsages());
  }

  private boolean isUnsafe(SortedSet<UsagePoint> points) {
    /*if (points.size() >= 1) {
      Iterator<UsagePoint> iterator = points.iterator();
      UsagePoint point = iterator.next();

      if(ignoreEmptyLockset && point.locks.isEmpty()) {
        //special case when we ignore intersection of empty sets
        //at least one lockSet should be nonempty

        //we go with while locks is empty until first nonempty
        //search for non empty lockset
        while(point.locks.isEmpty() && iterator.hasNext()) {
          //skip accesses without locks
          point = iterator.next();
        }
        if(!point.locks.isEmpty()) {
          //we already have an empty intersection
          //since we had an access without locks
          //and now have an access with some locks
          return true;
        } else {
          //only empty locksets
          return false;
        }
      }
      if (point.access == Access.WRITE) {
        Set<LockIdentifier> lockSet = new HashSet<>(point.locks);
        if (lockSet.isEmpty()) {
          return true;
        }
        while (iterator.hasNext()) {
          point = iterator.next();
          if (point.access == Access.WRITE) {
            lockSet.retainAll(point.locks);
            if (lockSet.isEmpty()) {
              return true;
            }
          } else {
            // There can be a situation
            // (l1, l2, write), (l1, read), (l2, read)
            // Thus, we should process writes and reads differently
            //
            if (Sets.intersection(lockSet, point.locks).isEmpty()) {
              return true;
            }
          }
        }
      }
    }*/
    for (UsagePoint point1 : points) {
      for (UsagePoint point2 : points.tailSet(point1)) {
        if (isUnsafePair(point1, point2)) {
          return true;
        }
      }
    }
    return false;
  }

  private Pair<UsagePoint, UsagePoint> getUnsafePair(SortedSet<UsagePoint> set) {

    for (UsagePoint point1 : set) {
      for (UsagePoint point2 : set.tailSet(point1)) {
        if (point1.equals(point2)) {
          /* There can be an unsafe even with only one usage,
           * but at first we find two different usages
           */
          continue;
        }
        if (isUnsafePair(point1, point2)) {
          return Pair.of(point1, point2);
        }
      }
    }
    //Now we find an unsafe only from one usage
    if (!ignoreEmptyLockset) {
      for (UsagePoint point1 : set) {
        if (isUnsafePair(point1, point1)) {
          return Pair.of(point1, point1);
        }
      }
    }
    //If we can not find an unsafe here, fail
    return null;
  }

  public boolean isUnsafePair(UsagePoint point1, UsagePoint point2) {
    if (point1.isCompatible(point2) &&
        (point1.access == Access.WRITE || point2.access == Access.WRITE)) {
      if (ignoreEmptyLockset && point1.isEmpty() && point2.isEmpty()) {
        return false;
      }
      return true;
    }
    return false;
  }
 }
