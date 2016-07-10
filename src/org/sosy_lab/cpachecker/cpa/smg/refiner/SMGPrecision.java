/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.refiner.SMGInterpolant.SMGPrecisionIncrement;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class SMGPrecision implements Precision {

  private final boolean allowsHeapAbstraction;
  private final LogManager logger;

  public SMGPrecision(LogManager pLogger, boolean pAllowsHeapAbstraction) {
    logger = pLogger;
    allowsHeapAbstraction = pAllowsHeapAbstraction;
  }

  public static SMGPrecision createStaticPrecision(boolean pEnableHeapAbstraction,
      LogManager pLogger) {
    return new SMGStaticPrecision(pLogger, pEnableHeapAbstraction);
  }

  public abstract Precision withIncrement(Map<CFANode, SMGPrecisionIncrement> pPrecisionIncrement);

  public abstract SMGPrecision join(SMGPrecision pPrecision);

  public static SMGPrecision createRefineablePrecision(SMGPrecision pPrecision) {

    SetMultimap<CFANode, SMGMemoryPath> emptyMemoryPaths = ImmutableSetMultimap.of();
    SetMultimap<CFANode, SMGAbstractionBlock> emptyAbstractionBlocks = ImmutableSetMultimap.of();
    return new SMGRefineablePrecision(pPrecision.logger, pPrecision.allowsHeapAbstraction,
        emptyMemoryPaths, emptyAbstractionBlocks);
  }

  public LogManager getLogger() {
    return logger;
  }

  public abstract boolean allowsAbstraction();

  public abstract boolean isTracked(SMGMemoryPath pPath, CFANode pCfaNode);

  public boolean allowsHeapAbstractionOnNode(CFANode pCfaNode) {
    return allowsHeapAbstraction && pCfaNode.isLoopStart();
  }

  public boolean allowsHeapAbstraction() {
    return allowsHeapAbstraction;
  }

  public abstract Set<SMGAbstractionBlock> getAbstractionBlocks(CFANode location);

  public static class SMGRefineablePrecision extends SMGPrecision {

    private final ImmutableSetMultimap<CFANode, SMGMemoryPath> trackedMemoryPaths;
    private final ImmutableSetMultimap<CFANode, SMGAbstractionBlock> abstractionBlocks;

    public SMGRefineablePrecision(LogManager pLogger, boolean pAllowsHeapAbstraction,
        SetMultimap<CFANode, SMGMemoryPath> pTrackedMemoryPaths,
        SetMultimap<CFANode, SMGAbstractionBlock> pAbstractionBlocks) {
      super(pLogger, pAllowsHeapAbstraction);
      trackedMemoryPaths = ImmutableSetMultimap.copyOf(pTrackedMemoryPaths);
      abstractionBlocks = ImmutableSetMultimap.copyOf(pAbstractionBlocks);
    }

    public Set<SMGMemoryPath> getTrackedMemPaths(CFANode pLocationNode) {
      if (trackedMemoryPaths.containsKey(pLocationNode)) {
        return ImmutableSet.of();
      } else {
        return trackedMemoryPaths.get(pLocationNode);
      }
    }

    @Override
    public Precision withIncrement(Map<CFANode, SMGPrecisionIncrement> pPrecisionIncrement) {

      SetMultimap<CFANode, SMGMemoryPath> resultMemoryPaths = HashMultimap.create();
      resultMemoryPaths.putAll(trackedMemoryPaths);
      SetMultimap<CFANode, SMGAbstractionBlock> resultAbstractionBlocks = HashMultimap.create();
      resultAbstractionBlocks.putAll(abstractionBlocks);

      for (Entry<CFANode, SMGPrecisionIncrement> entry : pPrecisionIncrement.entrySet()) {
        SMGPrecisionIncrement inc = entry.getValue();
        CFANode cfaNode = entry.getKey();
        Set<SMGAbstractionBlock> incAbstractionBlocks = inc.getAbstractionBlock();
        Set<SMGMemoryPath> incMemoryPaths = inc.getPathsToTrack();
        resultAbstractionBlocks.putAll(cfaNode, incAbstractionBlocks);
        resultMemoryPaths.putAll(cfaNode, incMemoryPaths);
      }

      return new SMGRefineablePrecision(getLogger(), allowsHeapAbstraction(), resultMemoryPaths,
          resultAbstractionBlocks);
    }

    @Override
    public SMGPrecision join(SMGPrecision pPrecision) {

      if (pPrecision instanceof SMGStaticPrecision) {
        return pPrecision;
      }

      SMGRefineablePrecision other = (SMGRefineablePrecision) pPrecision;

      SetMultimap<CFANode, SMGMemoryPath> resultMemoryPaths = HashMultimap.create();
      resultMemoryPaths.putAll(trackedMemoryPaths);
      resultMemoryPaths.putAll(other.trackedMemoryPaths);
      SetMultimap<CFANode, SMGAbstractionBlock> resultAbstractionBlocks = HashMultimap.create();
      resultAbstractionBlocks.putAll(abstractionBlocks);
      resultAbstractionBlocks.putAll(other.abstractionBlocks);

      return new SMGRefineablePrecision(getLogger(), allowsHeapAbstraction(), resultMemoryPaths,
          resultAbstractionBlocks);
    }

    @Override
    public boolean allowsAbstraction() {
      return true;
    }

    @Override
    public boolean isTracked(SMGMemoryPath pPath, CFANode pCfaNode) {

      if (trackedMemoryPaths.containsKey(pCfaNode)) {
        Set<SMGMemoryPath> trackedMemPaths = trackedMemoryPaths.get(pCfaNode);
        return trackedMemPaths.contains(pPath);
      } else {
        return false;
      }
    }

    @Override
    public Set<SMGAbstractionBlock> getAbstractionBlocks(CFANode location) {
      return abstractionBlocks.get(location);
    }
  }

  private static class SMGStaticPrecision extends SMGPrecision {

    public SMGStaticPrecision(LogManager pLogger, boolean pAllowsHeapAbstraction) {
      super(pLogger, pAllowsHeapAbstraction);
    }

    @Override
    public boolean allowsAbstraction() {
      return false;
    }

    @Override
    public boolean isTracked(SMGMemoryPath pPath, CFANode pCfaNode) {
      return true;
    }

    @Override
    public Precision withIncrement(Map<CFANode, SMGPrecisionIncrement> pPrecisionIncrement) {
      return this;
    }

    @Override
    public SMGPrecision join(SMGPrecision pPrecision) {
      return this;
    }

    @Override
    public Set<SMGAbstractionBlock> getAbstractionBlocks(CFANode pLocation) {
      return ImmutableSet.of();
    }
  }
}