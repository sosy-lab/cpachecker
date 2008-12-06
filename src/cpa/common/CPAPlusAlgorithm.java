/**
 *
 */
package cpa.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import logging.CustomLogLevel;
import logging.LazyLogger;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.CPAPlus;
import cpa.common.interfaces.MergeOperatorPlus;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.PrecisionAdjustment;
import cpa.common.interfaces.StopOperatorPlus;
import cpa.common.interfaces.TransferRelationPlus;
import exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CPAPlusAlgorithm {

  public Collection<AbstractElementWithLocation> CPAPlus (CPAPlus cpaPlus, AbstractElementWithLocation initialState,
      Precision initialPrecision) throws CPAException
  {
    List<Pair<AbstractElementWithLocation,Precision>> waitlist = new ArrayList<Pair<AbstractElementWithLocation,Precision>>();
    HashSet<Pair<AbstractElementWithLocation,Precision>> reached = new HashSet<Pair<AbstractElementWithLocation,Precision>>();
    Collection<AbstractElementWithLocation> simpleReached = new HashSet<AbstractElementWithLocation>();

    LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, initialState,
    " added as initial state to CPAPlus");

    waitlist.add(new Pair<AbstractElementWithLocation,Precision>(initialState, initialPrecision));
    reached.add(new Pair<AbstractElementWithLocation,Precision>(initialState, initialPrecision));
    simpleReached.add(initialState);

    TransferRelationPlus transferRelation = cpaPlus.getTransferRelation ();
    MergeOperatorPlus mergeOperator = cpaPlus.getMergeOperator ();
    StopOperatorPlus stopOperator = cpaPlus.getStopOperator ();
    PrecisionAdjustment precisionAdjustment = cpaPlus.getPrecisionAdjustment();

    while (!waitlist.isEmpty ())
    {
      Pair<AbstractElementWithLocation,Precision> e = choose(waitlist);
      e = precisionAdjustment.prec(e.getFirst(), e.getSecond(), reached);
      AbstractElementWithLocation element = e.getFirst();
      Precision precision = e.getSecond();

      LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, element,
          " with precision ", precision, " is popped from queue");
      List<AbstractElementWithLocation> successors = null;
      try {
        successors = transferRelation.getAllAbstractSuccessors (element, precision);
      } catch (CPATransferException e1) {
        e1.printStackTrace();
        assert (false);
      }

      for (AbstractElementWithLocation successor : successors)
      {
        LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
            "successor of ", element, " --> ", successor);

        List<Pair<AbstractElementWithLocation,Precision>> toRemove = new Vector<Pair<AbstractElementWithLocation,Precision>>();
        List<AbstractElementWithLocation> toRemoveSimple = new Vector<AbstractElementWithLocation>();
        List<Pair<AbstractElementWithLocation,Precision>> toAdd = new Vector<Pair<AbstractElementWithLocation,Precision>>();
        List<AbstractElementWithLocation> toAddSimple = new Vector<AbstractElementWithLocation>();

        for (Pair<AbstractElementWithLocation, Precision> reachedEntry : reached) {
          AbstractElementWithLocation reachedElement = reachedEntry.getFirst();
          AbstractElementWithLocation mergedElement = mergeOperator.merge(
              successor, reachedElement, precision);
          LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
              " Merged ", successor, " and ", reachedElement, " --> ", mergedElement);
          if (!mergedElement.equals(reachedElement)) {
            LazyLogger.log(
                CustomLogLevel.CentralCPAAlgorithmLevel,
                "reached element ", reachedElement,
                " is removed from queue and ", mergedElement,
                " with precision ", precision, " is added to queue");
            waitlist.remove(new Pair<AbstractElementWithLocation,Precision>(reachedElement, reachedEntry.getSecond()));
            waitlist.add(new Pair<AbstractElementWithLocation,Precision>(mergedElement, precision));

            toRemove.add(new Pair<AbstractElementWithLocation,Precision>(reachedElement, reachedEntry.getSecond()));
            toRemoveSimple.add(reachedElement);
            toAdd.add(new Pair<AbstractElementWithLocation,Precision>(mergedElement, precision));
            toAddSimple.add(mergedElement);
          }
        }
        reached.removeAll(toRemove);
        simpleReached.removeAll(toRemoveSimple);
        reached.addAll(toAdd);
        simpleReached.addAll(toAddSimple);

        if (!stopOperator.stop (successor, simpleReached, precision))
        {
          LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
              "No need to stop ", successor, " is added to queue");

          waitlist.add(new Pair<AbstractElementWithLocation,Precision>(successor,precision));
          reached.add(new Pair<AbstractElementWithLocation,Precision>(successor,precision));
          simpleReached.add(successor);
        }
      }
    }

    return simpleReached;
  }

  private Pair<AbstractElementWithLocation,Precision> choose(List<Pair<AbstractElementWithLocation,Precision>> waitlist) {

    if(waitlist.size() == 1 || CPAMain.cpaConfig.getBooleanValue("analysis.bfs")){
      return waitlist.remove(0);
    } else if(CPAMain.cpaConfig.getBooleanValue("analysis.topSort")) {
      Pair<AbstractElementWithLocation,Precision> currentElement = waitlist.get(0);
      for(int i=1; i<waitlist.size(); i++){
        Pair<AbstractElementWithLocation,Precision> currentTempElement = waitlist.get(i);
        if(currentTempElement.getFirst().getLocationNode().getTopologicalSortId() >
            currentElement.getFirst().getLocationNode().getTopologicalSortId()){
          currentElement = currentTempElement;
        }
      }

      waitlist.remove(currentElement);
      return currentElement;
    } else {
      return waitlist.remove(waitlist.size()-1);
    }
  }
}
