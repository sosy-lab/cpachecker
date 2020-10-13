// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import com.google.common.base.Function;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.common.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.cpa.lock.LockCPA;
import org.sosy_lab.cpachecker.cpa.lock.LockReducer;
import org.sosy_lab.cpachecker.cpa.lock.LockTransferRelation;
import org.sosy_lab.cpachecker.cpa.usage.UsageCPA;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.storage.UsageInfoSet;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;

@Options(prefix = "cpa.usage.refinement")
public class RefinementBlockFactory {

  public static enum RefinementBlockTypes {
    IdentifierIterator(currentInnerBlockType.SingleIdentifier),
    PointIterator(currentInnerBlockType.UsageInfoSet),
    UsageIterator(currentInnerBlockType.UsageInfo),
    PathIterator(currentInnerBlockType.ExtendedARGPath),
    SinglePathIterator(currentInnerBlockType.ExtendedARGPath),
    PredicateRefiner(currentInnerBlockType.ExtendedARGPath),
    ImprecisePredicateRefiner(currentInnerBlockType.ExtendedARGPath),
    ThreadModularRefiner(currentInnerBlockType.ExtendedARGPath),
    SharedRefiner(currentInnerBlockType.ExtendedARGPath),
    LockRefiner(currentInnerBlockType.ExtendedARGPath);

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

  Map<ARGState, ARGState> subgraphStatesToReachedState = new HashMap<>();
  final ConfigurableProgramAnalysis cpa;
  Configuration config;

  @Option(name = "refinementChain", description = "The order of refinement blocks",
      secure = true)
  List<RefinementBlockTypes> RefinementChain;

  public static enum PathEquation {
    ARGStateId,
    CFANodeId;
  }

  @Option(name = "pathEquality", description = "The way how to identify two paths as equal")
  PathEquation pathEquation = PathEquation.CFANodeId;

  // Strange, but this option is much more better in true (even by time consumption)
  @Option(description = "Disable all caching into all internal refinement blocks", secure = true)
  private boolean disableAllCaching = false;

  @Option(
    description = "Refine any inconsistency of lock states or just incompatible pairs",
    secure = true)
  private boolean refineOnlyIncompatiblePairs = false;

  @Option(description = "Limit for unique iterations of iterators", secure = true)
  private int iterationLimit = Integer.MAX_VALUE;

  public RefinementBlockFactory(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig)
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
    ShutdownNotifier notifier = usCPA.getNotifier();

    //Tricky way to create the chain, but it is difficult to dynamically know the parameter types
    RefinementInterface currentBlock = new RefinementPairStub();
    currentInnerBlockType currentBlockType = currentInnerBlockType.ExtendedARGPath;

    Function<ARGState, Integer> idExtractor;

    switch (pathEquation) {
      case ARGStateId:
        idExtractor = ARGState::getStateId;
        break;

      case CFANodeId:
        idExtractor = s -> AbstractStates.extractLocation(s).getNodeNumber();
        break;

      default:
        throw new InvalidConfigurationException("Unexpexted type " + pathEquation);

    }

    PathRestorator computer;
    if (bamCpa != null) {
      computer = bamCpa.createBAMMultipleSubgraphComputer(idExtractor);
    } else {
      computer = new ARGPathRestorator(idExtractor);
    }

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
                    bamCpa == null ? null : bamCpa.getTransferRelation(),
                    disableAllCaching);
            currentBlockType = currentInnerBlockType.ReachedSet;
            break;

          case PointIterator:
            currentBlock =
                new PointIterator(
                    (ConfigurableRefinementBlock<Pair<UsageInfoSet, UsageInfoSet>>) currentBlock,
                    notifier);
            currentBlockType = currentInnerBlockType.SingleIdentifier;
            break;

          case UsageIterator:
            currentBlock = new UsagePairIterator((ConfigurableRefinementBlock<Pair<UsageInfo, UsageInfo>>) currentBlock,
                    logger,
                    notifier,
                    iterationLimit);
            currentBlockType = currentInnerBlockType.UsageInfoSet;
            break;

          case PathIterator:
            currentBlock =
                new PathPairIterator(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>)
                    currentBlock,
                    computer,
                    idExtractor,
                    notifier,
                    iterationLimit);
            currentBlockType = currentInnerBlockType.UsageInfo;
            break;

          case SinglePathIterator:
            currentBlock =
                new PathPairIterator(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock,
                    computer,
                    idExtractor,
                    notifier,
                    1);
            currentBlockType = currentInnerBlockType.UsageInfo;
            break;

          case ImprecisePredicateRefiner:
            currentBlock =
                PredicateRefinerAdapter.createImpreciseRefiner(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock,
                    cpa,
                    logger);
            break;

          case PredicateRefiner:
            currentBlock =
                PredicateRefinerAdapter.createPlainRefiner(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock,
                    cpa,
                    logger);
            break;

          case ThreadModularRefiner:
            currentBlock =
                PredicateRefinerAdapter.createThreadModularRefiner(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock,
                    cpa,
                    logger);
            break;

          case SharedRefiner:
            //LocalCPA CPAForSharedRefiner = CPAs.retrieveCPA(cpa, LocalCPA.class);
            //assert(CPAForSharedRefiner != null);
            LocalTransferRelation RelationForSharedRefiner = new LocalTransferRelation(config);

            currentBlock = new SharedRefiner((ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock, RelationForSharedRefiner);

            break;

          case LockRefiner:
            LockCPA lCPA = CPAs.retrieveCPA(cpa, LockCPA.class);
            LockTransferRelation lockTransfer =
                (LockTransferRelation) lCPA.getTransferRelation();
            LockReducer lReducer = (LockReducer) lCPA.getReducer();

            currentBlock =
                new LockRefiner(
                    (ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>>) currentBlock,
                    lockTransfer,
                    lReducer,
                    refineOnlyIncompatiblePairs);

            break;

          default:
            throw new InvalidConfigurationException("The type " + RefinementChain.get(i) + " is not supported");
        }
      } else {
        throw new InvalidConfigurationException(currentType + " can not precede the " + currentBlock.getClass().getSimpleName());
      }
    }
    if (currentBlockType == currentInnerBlockType.ReachedSet) {
      assert currentBlock instanceof Refiner;
      return (Refiner) currentBlock;
    } else {
      throw new InvalidConfigurationException("The first block is not take a reached set as parameter");
    }
  }
}
