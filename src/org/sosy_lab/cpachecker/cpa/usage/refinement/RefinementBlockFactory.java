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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMTransferRelation;
import org.sosy_lab.cpachecker.cpa.local.LocalTransferRelation;
import org.sosy_lab.cpachecker.cpa.usage.UsageCPA;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageInfoSet;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

@Options(prefix="cpa.usage")
public class RefinementBlockFactory {

  public static enum RefinementBlockTypes {
    IdentifierIterator(currentInnerBlockType.SingleIdentifier),
    PointIterator(currentInnerBlockType.UsageInfoSet),
    UsageIterator(currentInnerBlockType.UsageInfo),
    PathIterator(currentInnerBlockType.ExtendedARGPath),
    PredicateRefiner(currentInnerBlockType.ExtendedARGPath),
    CallstackFilter(currentInnerBlockType.ExtendedARGPath),
    ProbeFilter(currentInnerBlockType.ExtendedARGPath),
    SharedRefiner(currentInnerBlockType.ExtendedARGPath);

    public final currentInnerBlockType innerType;

    private RefinementBlockTypes(currentInnerBlockType type) {
      innerType = type;
    }
  }

  private static enum currentInnerBlockType {
    ExtendedARGPath,
    UsageInfoSet,
    SingleIdentifier,
    UsageInfo,
    ReachedSet;
  }

  private final static String CLASS_PREFIX = "org.sosy_lab.cpachecker.cpa.usage.refinement";
  Map<ARGState, ARGState> subgraphStatesToReachedState = new HashMap<>();
  final ConfigurableProgramAnalysis cpa;
  Configuration config;

  @Option(name = "refinementChain", description = "The order of refinement blocks")
  List<RefinementBlockTypes> RefinementChain;

  public RefinementBlockFactory(ConfigurableProgramAnalysis pCpa, Configuration pConfig) throws InvalidConfigurationException {
    cpa = pCpa;
    config = pConfig;
    pConfig.inject(this);
  }

  @SuppressWarnings("unchecked")
  public ConfigurableRefinementBlock<ReachedSet> create() throws InvalidConfigurationException {
    BAMCPA bam = CPAs.retrieveCPA(cpa, BAMCPA.class);
    BAMTransferRelation bamTransfer = bam.getTransferRelation();
    UsageCPA usCPA = CPAs.retrieveCPA(cpa, UsageCPA.class);
    LogManager logger = usCPA.getLogger();

    //Tricky way to create the chain, but it is difficult to dynamically know the parameter types
    RefinementInterface currentBlock = new RefinementPairStub();
    currentInnerBlockType currentBlockType = currentInnerBlockType.ExtendedARGPath;

    for (int i = RefinementChain.size() - 1; i >= 0; i--) {

      RefinementBlockTypes currentType = RefinementChain.get(i);
      if (currentBlockType == currentType.innerType) {
        switch (currentType) {
          case IdentifierIterator:
            currentBlock = new IdentifierIterator((ConfigurableRefinementBlock<SingleIdentifier>) currentBlock,
                config, cpa, bamTransfer);
            currentBlockType = currentInnerBlockType.ReachedSet;
            break;

          case PointIterator:
            currentBlock = new PointIterator((ConfigurableRefinementBlock<Pair<UsageInfoSet, UsageInfoSet>>) currentBlock,
                null);
            currentBlockType = currentInnerBlockType.SingleIdentifier;
            break;

          case UsageIterator:
            currentBlock = new UsagePairIterator((ConfigurableRefinementBlock<Pair<UsageInfo, UsageInfo>>) currentBlock,
                logger);
            currentBlockType = currentInnerBlockType.UsageInfoSet;
            break;

          case PathIterator:
            currentBlock = new PathPairIterator((ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock,
                bamTransfer);
            currentBlockType = currentInnerBlockType.UsageInfo;
            break;

          case PredicateRefiner:
            currentBlock = new PredicateRefinerAdapter((ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock,
                cpa);
            break;

          case CallstackFilter:
            currentBlock = new CallstackFilter((ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock,
                config);
            break;

          case ProbeFilter:
            currentBlock = new ProbeFilter((ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock,
                config);
            break;

          case SharedRefiner:
            //LocalCPA CPAForSharedRefiner = CPAs.retrieveCPA(cpa, LocalCPA.class);
            //assert(CPAForSharedRefiner != null);
            LocalTransferRelation RelationForSharedRefiner = new LocalTransferRelation(config);

            currentBlock = new SharedRefiner((ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock, RelationForSharedRefiner);

            break;

          default:
            throw new InvalidConfigurationException("The type " + RefinementChain.get(i) + " is not supported");
        }
      } else {
        throw new InvalidConfigurationException(currentType + " can not precede the " + currentBlock.getClass().getSimpleName());
      }
    }
    if (currentBlockType == currentInnerBlockType.ReachedSet) {
      return (ConfigurableRefinementBlock<ReachedSet>) currentBlock;
    } else {
      throw new InvalidConfigurationException("The first block is not take a reached set as parameter");
    }
  }
}
