// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.local.LocalTransferRelation;
import org.sosy_lab.cpachecker.cpa.usage.UsageCPA;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageInfoSet;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

@Options(prefix = "cpa.usage")
public class RefinementBlockFactory {

  public enum RefinementBlockTypes {
    IdentifierIterator(currentInnerBlockType.SingleIdentifier),
    PointIterator(currentInnerBlockType.UsageInfoSet),
    UsageIterator(currentInnerBlockType.UsageInfo),
    PathIterator(currentInnerBlockType.ExtendedARGPath),
    PredicateRefiner(currentInnerBlockType.ExtendedARGPath),
    CallstackFilter(currentInnerBlockType.ExtendedARGPath),
    ProbeFilter(currentInnerBlockType.ExtendedARGPath),
    SharedRefiner(currentInnerBlockType.ExtendedARGPath);

    public final currentInnerBlockType innerType;

    RefinementBlockTypes(currentInnerBlockType type) {
      innerType = type;
    }
  }

  private enum currentInnerBlockType {
    ExtendedARGPath,
    UsageInfoSet,
    SingleIdentifier,
    UsageInfo,
    ReachedSet;
  }

  Map<ARGState, ARGState> subgraphStatesToReachedState = new HashMap<>();
  final ConfigurableProgramAnalysis cpa;
  Configuration config;

  @Option(name = "refinementChain", description = "The order of refinement blocks", secure = true)
  List<RefinementBlockTypes> RefinementChain;

  public enum PathEquation {
    ARGStateId,
    CFANodeId;
  }

  @Option(name = "pathEquality", description = "The way how to identify two paths as equal")
  PathEquation pathEquation = PathEquation.CFANodeId;

  public RefinementBlockFactory(ConfigurableProgramAnalysis pCpa, Configuration pConfig)
      throws InvalidConfigurationException {
    cpa = pCpa;
    config = pConfig;
    pConfig.inject(this);
  }

  @SuppressWarnings("unchecked")
  public Refiner create() throws InvalidConfigurationException {
    BAMCPA bamCpa = CPAs.retrieveCPA(cpa, BAMCPA.class);
    UsageCPA usCPA = CPAs.retrieveCPA(cpa, UsageCPA.class);
    LogManager logger = usCPA.getLogger();

    // Tricky way to create the chain, but it is difficult to dynamically know the parameter types
    RefinementInterface currentBlock = new RefinementPairStub();
    currentInnerBlockType currentBlockType = currentInnerBlockType.ExtendedARGPath;

    for (int i = RefinementChain.size() - 1; i >= 0; i--) {

      RefinementBlockTypes currentType = RefinementChain.get(i);
      if (currentBlockType == currentType.innerType) {
        switch (currentType) {
          case IdentifierIterator:
            currentBlock =
                new IdentifierIterator(
                    (ConfigurableRefinementBlock<SingleIdentifier>) currentBlock,
                    config,
                    cpa,
                    bamCpa.getTransferRelation());
            currentBlockType = currentInnerBlockType.ReachedSet;
            break;

          case PointIterator:
            currentBlock =
                new PointIterator(
                    (ConfigurableRefinementBlock<Pair<UsageInfoSet, UsageInfoSet>>) currentBlock);
            currentBlockType = currentInnerBlockType.SingleIdentifier;
            break;

          case UsageIterator:
            currentBlock =
                new UsagePairIterator(
                    (ConfigurableRefinementBlock<Pair<UsageInfo, UsageInfo>>) currentBlock, logger);
            currentBlockType = currentInnerBlockType.UsageInfoSet;
            break;

          case PathIterator:
            currentBlock =
                new PathPairIterator(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>)
                        currentBlock,
                    bamCpa,
                    pathEquation);
            currentBlockType = currentInnerBlockType.UsageInfo;
            break;

          case PredicateRefiner:
            currentBlock =
                new PredicateRefinerAdapter(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>)
                        currentBlock,
                    cpa,
                    logger);
            break;

          case CallstackFilter:
            currentBlock =
                new CallstackFilter(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>)
                        currentBlock,
                    config);
            break;

          case ProbeFilter:
            currentBlock =
                new ProbeFilter(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>)
                        currentBlock,
                    config);
            break;

          case SharedRefiner:
            // LocalCPA CPAForSharedRefiner = CPAs.retrieveCPA(cpa, LocalCPA.class);
            // assert(CPAForSharedRefiner != null);
            LocalTransferRelation RelationForSharedRefiner = new LocalTransferRelation(config);

            currentBlock =
                new SharedRefiner(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>)
                        currentBlock,
                    RelationForSharedRefiner);

            break;

          default:
            throw new InvalidConfigurationException(
                "The type " + RefinementChain.get(i) + " is not supported");
        }
      } else {
        throw new InvalidConfigurationException(
            currentType + " can not precede the " + currentBlock.getClass().getSimpleName());
      }
    }
    if (currentBlockType == currentInnerBlockType.ReachedSet) {
      assert currentBlock instanceof Refiner;
      return (Refiner) currentBlock;
    } else {
      throw new InvalidConfigurationException(
          "The first block is not take a reached set as parameter");
    }
  }
}
