// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartitioningCheckingHelper;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public class PartitionChecker {

  private final Multimap<CFANode, AbstractState> partitionParts = HashMultimap.create();
  private final List<AbstractState> certificatePart = new ArrayList<>();
  private final Collection<AbstractState> mustBeInCertificate = new HashSet<>();

  private final PartitioningCheckingHelper partitionHelper;
  private final PartitioningIOHelper ioHelper;
  private final Precision initPrec;
  private final StopOperator stop;
  private final TransferRelation transfer;

  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;

  public PartitionChecker(
      final Precision pInitPrecision,
      final StopOperator pStop,
      final TransferRelation pTransfer,
      final PartitioningIOHelper pIOHelper,
      final PartitioningCheckingHelper pHelperInfo,
      final ShutdownNotifier pShutdownNotifier,
      final LogManager pLogger) {
    initPrec = pInitPrecision;
    stop = pStop;
    transfer = pTransfer;

    ioHelper = pIOHelper;
    partitionHelper = pHelperInfo;

    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
  }

  public void checkPartition(int pIndex) {
    Multimap<CFANode, AbstractState> statesPerLocation = HashMultimap.create();
    Pair<AbstractState[], AbstractState[]> partition = ioHelper.getPartition(pIndex);
    Preconditions.checkNotNull(partition);

    // add nodes of partition
    for (AbstractState internalNode : partition.getFirst()) {
      addElement(internalNode, true, statesPerLocation);
    }

    // add adjacent nodes of other partition
    for (AbstractState adjacentNode : partition.getSecond()) {
      addElement(adjacentNode, false, statesPerLocation);
    }

    AbstractState checkedState;
    CFANode loc;
    Collection<? extends AbstractState> successors;
    int nextPos = 0;

    while (nextPos < certificatePart.size()) {
      if (shutdownNotifier.shouldShutdown()) {
        partitionHelper.abortCheckingPreparation();
        return;
      }

      if (certificatePart.size() + partitionHelper.getCurrentCertificateSize()
          > ioHelper.getSavedReachedSetSize()) {
        logger.log(
            Level.SEVERE,
            "Checking failed, recomputed certificate bigger than original reached set.");
        partitionHelper.abortCheckingPreparation();
        return;
      }

      checkedState = certificatePart.get(nextPos++);

      // compute successors
      try {
        successors = transfer.getAbstractSuccessors(checkedState, initPrec);

        for (AbstractState successor : successors) {
          // check if covered
          loc = AbstractStates.extractLocation(successor);
          if (!stop.stop(successor, statesPerLocation.get(loc), initPrec)) {
            certificatePart.add(successor);
          }
        }
      } catch (CPATransferException | InterruptedException e) {
        logger.log(Level.SEVERE, "Checking failed, successor computation failed");
        partitionHelper.abortCheckingPreparation();
        return;
      } catch (CPAException e) {
        logger.log(Level.SEVERE, "Checking failed, checking successor coverage failed");
        partitionHelper.abortCheckingPreparation();
        return;
      }
    }
  }

  public void addCertificatePartsToCertificate(final Collection<AbstractState> pCertificate) {
    pCertificate.addAll(certificatePart);
  }

  public void addElementsCheckedInOtherPartitions(
      final Collection<AbstractState> pStatesMustBeInCertificate) {
    pStatesMustBeInCertificate.addAll(mustBeInCertificate);
  }

  public void addPartitionElements(final Multimap<CFANode, AbstractState> pPartitionElements) {
    pPartitionElements.putAll(partitionParts);
  }

  public void clearPartitionElementsSavedForInspection() {
    certificatePart.clear();
  }

  public void clearAllSavedPartitioningElements() {
    certificatePart.clear();
    partitionParts.clear();
    mustBeInCertificate.clear();
  }

  private void addElement(
      final AbstractState element,
      final boolean inCertificate,
      final Multimap<CFANode, AbstractState> pStatesPerLocation) {
    CFANode node = AbstractStates.extractLocation(element);
    pStatesPerLocation.put(node, element);
    if (inCertificate) {
      partitionParts.put(node, element);
      certificatePart.add(element);
    } else {
      mustBeInCertificate.add(element);
    }
  }
}
