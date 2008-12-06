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

import cpa.common.interfaces.AbstractElement;
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

  public Collection<AbstractElement> CPAPlus (CPAPlus cpaPlus, AbstractElement initialState, Precision initialPrecision) throws CPAException
  {
    List<Pair<AbstractElement,Precision>> waitlist = new ArrayList<Pair<AbstractElement,Precision>>();
    HashSet<Pair<AbstractElement,Precision>> reached = new HashSet<Pair<AbstractElement,Precision>>();
    Collection<AbstractElement> simpleReached = new HashSet<AbstractElement>();

    LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, initialState,
    " added as initial state to CPAPlus");

    waitlist.add(new Pair<AbstractElement,Precision>(initialState, initialPrecision));
    reached.add(new Pair<AbstractElement,Precision>(initialState, initialPrecision));
    simpleReached.add(initialState);

    TransferRelationPlus transferRelation = cpaPlus.getTransferRelation ();
    MergeOperatorPlus mergeOperator = cpaPlus.getMergeOperator ();
    StopOperatorPlus stopOperator = cpaPlus.getStopOperator ();
    PrecisionAdjustment precisionAdjustment = cpaPlus.getPrecisionAdjustment();

    while (!waitlist.isEmpty ())
    {
      Pair<AbstractElement,Precision> e = choose(waitlist);
      e = precisionAdjustment.prec(e.getFirst(), e.getSecond(), reached);
      AbstractElement element = e.getFirst();
      Precision precision = e.getSecond();

      LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel, element,
          " with precision ", precision, " is popped from queue");
      List<AbstractElement> successors = null;
      try {
        if (!(e.getFirst() instanceof AbstractElementWithLocation)) {
          throw new CPAException("No Location information available, impossible to continue");
        }
        successors = transferRelation.getAllAbstractSuccessors (element, precision);
      } catch (Exception e1) {
        e1.printStackTrace();
        assert (false);
      }

      for (AbstractElement successor : successors)
      {
        LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
            "successor of ", element, " --> ", successor);

        List<Pair<AbstractElement,Precision>> toRemove = new Vector<Pair<AbstractElement,Precision>>();
        List<AbstractElement> toRemoveSimple = new Vector<AbstractElement>();
        List<Pair<AbstractElement,Precision>> toAdd = new Vector<Pair<AbstractElement,Precision>>();
        List<AbstractElement> toAddSimple = new Vector<AbstractElement>();

        for (Pair<AbstractElement, Precision> reachedEntry : reached) {
          AbstractElement reachedElement = reachedEntry.getFirst();
          AbstractElement mergedElement = mergeOperator.merge(
              successor, reachedElement, precision);
          LazyLogger.log(CustomLogLevel.CentralCPAAlgorithmLevel,
              " Merged ", successor, " and ", reachedElement, " --> ", mergedElement);
          if (!mergedElement.equals(reachedElement)) {
            LazyLogger.log(
                CustomLogLevel.CentralCPAAlgorithmLevel,
                "reached element ", reachedElement,
                " is removed from queue and ", mergedElement,
                " with precision ", precision, " is added to queue");
            waitlist.remove(new Pair<AbstractElement,Precision>(reachedElement, reachedEntry.getSecond()));
            waitlist.add(new Pair<AbstractElement,Precision>(mergedElement, precision));

            toRemove.add(new Pair<AbstractElement,Precision>(reachedElement, reachedEntry.getSecond()));
            toRemoveSimple.add(reachedElement);
            toAdd.add(new Pair<AbstractElement,Precision>(mergedElement, precision));
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

          waitlist.add(new Pair<AbstractElement,Precision>(successor,precision));
          reached.add(new Pair<AbstractElement,Precision>(successor,precision));
          simpleReached.add(successor);
        }
      }
    }

    return simpleReached;
  }

  private Pair<AbstractElement,Precision> choose(List<Pair<AbstractElement,Precision>> waitlist) {

    if(waitlist.size() == 1 || CPAMain.cpaConfig.getBooleanValue("analysis.bfs")){
      return waitlist.remove(0);
    } else if(CPAMain.cpaConfig.getBooleanValue("analysis.topSort")) {
      Pair<AbstractElement,Precision> currentElement = waitlist.get(0);
      CompositeElement compElem = (CompositeElement) currentElement.getFirst();
      for(int i=1; i<waitlist.size(); i++){
        Pair<AbstractElement,Precision> currentTempElement = waitlist.get(i);
        CompositeElement compTempElem = (CompositeElement)currentTempElement.getFirst();
        if(compTempElem.getLocationNode().getTopologicalSortId() > compElem.getLocationNode().getTopologicalSortId()){
          currentElement = currentTempElement;
          compElem = compTempElem;
        }
      }

      waitlist.remove(currentElement);
      return currentElement;
    } else {
      return waitlist.remove(waitlist.size()-1);
    }
  }
}
