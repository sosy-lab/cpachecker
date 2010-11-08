/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.symbpredabsCPA;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.AbstractARTBasedRefiner;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.cpa.symbpredabsCPA.SymbPredAbsAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.symbpredabstraction.CounterexampleTraceInfo;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Predicate;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

@Options(prefix="cpas.symbpredabs")
public class SymbPredAbsRefiner extends AbstractARTBasedRefiner {

  @Option(name="refinement.addPredicatesGlobally")
  private boolean addPredicatesGlobally = false;
  
  @Option(name="errorPath.export")
  private boolean exportErrorPath = true;
  
  @Option(name="errorPath.file", type=Option.Type.OUTPUT_FILE)
  private File exportFile = new File("ErrorPathAssignment.txt");

  @Option(name="refinement.msatCexFile", type=Option.Type.OUTPUT_FILE)
  private File dumpCexFile = new File("counterexample.msat");

  final Timer totalRefinement = new Timer();
  final Timer precisionUpdate = new Timer();
  final Timer artUpdate = new Timer();
  final Timer errorPathProcessing = new Timer();

  private final LogManager logger;
  private final SymbPredAbsFormulaManager formulaManager;
  private CounterexampleTraceInfo mCounterexampleTraceInfo;
  private Path targetPath;
  private List<CFANode> lastErrorPath = null;

  public SymbPredAbsRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    super(pCpa);

    SymbPredAbsCPA symbPredAbsCpa = this.getArtCpa().retrieveWrappedCpa(SymbPredAbsCPA.class);
    if (symbPredAbsCpa == null) {
      throw new CPAException(getClass().getSimpleName() + " needs a SymbPredAbsCPA");
    }

    symbPredAbsCpa.getConfiguration().inject(this);
    logger = symbPredAbsCpa.getLogger();
    formulaManager = symbPredAbsCpa.getFormulaManager();
    symbPredAbsCpa.getStats().addRefiner(this);
  }

  @Override
  public boolean performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException {
    totalRefinement.start();
    logger.log(Level.FINEST, "Starting refinement for SymbPredAbsCPA");

    // create path with all abstraction location elements (excluding the initial element)
    // the last element is the element corresponding to the error location
    ArrayList<SymbPredAbsAbstractElement> path = new ArrayList<SymbPredAbsAbstractElement>();
    List<ARTElement> artPath = new ArrayList<ARTElement>();
    
    Iterator<Pair<ARTElement,CFAEdge>> it = pPath.iterator();
    it.next(); // skip initial element
    while (it.hasNext()) {
      ARTElement ae = it.next().getFirst();
      SymbPredAbsAbstractElement symbElement =
        ae.retrieveWrappedElement(SymbPredAbsAbstractElement.class);

      if (symbElement instanceof AbstractionElement) {
        path.add(symbElement);
        artPath.add(ae);
      }
    }

    Precision oldPrecision = pReached.getPrecision(pReached.getLastElement());
    SymbPredAbsPrecision oldSymbPredAbsPrecision = null;
    if (oldPrecision instanceof SymbPredAbsPrecision) {
      oldSymbPredAbsPrecision = (SymbPredAbsPrecision)oldPrecision;
    } else if (oldPrecision instanceof WrapperPrecision) {
      oldSymbPredAbsPrecision = ((WrapperPrecision)oldPrecision).retrieveWrappedPrecision(SymbPredAbsPrecision.class);
    }
    if (oldSymbPredAbsPrecision == null) {
      throw new IllegalStateException("Could not find the SymbPredAbsPrecision for the error element");
    }

    logger.log(Level.ALL, "Abstraction trace is", path);

    // build the counterexample
    CounterexampleTraceInfo info = formulaManager.buildCounterexampleTrace(path);
    mCounterexampleTraceInfo = info;

    // if error is spurious refine
    if (info.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");
      precisionUpdate.start();
      Pair<ARTElement, SymbPredAbsPrecision> refinementResult =
              performRefinement(oldSymbPredAbsPrecision, path, artPath, info);
      precisionUpdate.stop();

      artUpdate.start();
      pReached.removeSubtree(refinementResult.getFirst(), refinementResult.getSecond());
      artUpdate.stop();
      totalRefinement.stop();
      return true;
    } else {
      // we have a real error
      logger.log(Level.FINEST, "Error trace is not spurious");
      errorPathProcessing.start();
      
      targetPath = null;
      boolean preciseInfo = false;
      NavigableMap<Integer, Map<Integer, Boolean>> preds = info.getBranchingPredicates();
      if (preds.isEmpty()) {
        logger.log(Level.WARNING, "No information about ART branches available!");
      } else {
        targetPath = createPathFromPredicateValues(pPath, preds);
        
        // try to create a better satisfying assignment by replaying this single path
        try {
          info = formulaManager.checkPath(targetPath.asEdgesList());
          if (info.isSpurious()) {
            logger.log(Level.WARNING, "Inconsistent replayed error path!");
            info = mCounterexampleTraceInfo;
          } else {
            mCounterexampleTraceInfo = info;
            preciseInfo = true;
          }
        } catch (CPATransferException e) {
          // path is now suddenly a problem 
          logger.log(Level.WARNING, "Could not replay error path (" + e.getMessage() + ")!");
        }
      }
      errorPathProcessing.stop();
      
      if (exportErrorPath && exportFile != null) {
        if (!preciseInfo) {
          logger.log(Level.WARNING, "The produced satisfying assignment is imprecise!");
        }

        formulaManager.dumpCounterexampleToFile(info, dumpCexFile);
        try {
          Files.writeFile(exportFile, info.getCounterexample());
        } catch (IOException e) {
          logger.log(Level.WARNING, "Could not write satisfying assignment for error path to file! ("
              + e.getMessage() + ")");
        }
      }
      totalRefinement.stop();
      return false;
    }
  }

  /**
   * pPath and pArtPath need to fit together such that
   * pPath.get(i) == pArtPath.get(i).retrieveWrappedElement(SymbPredAbsAbstractElement) 
   */
  private Pair<ARTElement, SymbPredAbsPrecision> performRefinement(SymbPredAbsPrecision oldPrecision,
      ArrayList<SymbPredAbsAbstractElement> pPath, List<ARTElement> pArtPath,
      CounterexampleTraceInfo pInfo) throws CPAException {

    Multimap<CFANode, Predicate> oldPredicateMap = oldPrecision.getPredicateMap();
    Set<Predicate> globalPredicates = oldPrecision.getGlobalPredicates();
    
    SymbPredAbsAbstractElement firstInterpolationElement = null;
    ARTElement firstInterpolationARTElement = null;
    boolean newPredicatesFound = false;
    
    List<CFANode> absLocations = new ArrayList<CFANode>(pArtPath.size());
    ImmutableSetMultimap.Builder<CFANode, Predicate> pmapBuilder = ImmutableSetMultimap.builder();

    pmapBuilder.putAll(oldPredicateMap);

    // iterate synchronously through pArtPath and pPath
    int i = -1;
    for (ARTElement ae : pArtPath) {
      i++;
      SymbPredAbsAbstractElement e = pPath.get(i);
      Collection<Predicate> newpreds = pInfo.getPredicatesForRefinement(e);
      CFANode loc = ae.retrieveLocationElement().getLocationNode();
      absLocations.add(loc);
      
      if (firstInterpolationElement == null && newpreds.size() > 0) {
        firstInterpolationElement = e;
        firstInterpolationARTElement = ae;
      }
      if (!newPredicatesFound && !oldPredicateMap.get(loc).containsAll(newpreds)) {
        // new predicates for this location
        newPredicatesFound = true;
      }

      pmapBuilder.putAll(loc, newpreds);
      pmapBuilder.putAll(loc, globalPredicates);
    }
    assert firstInterpolationElement != null;
    assert firstInterpolationElement == firstInterpolationARTElement.retrieveWrappedElement(SymbPredAbsAbstractElement.class);

    ImmutableSetMultimap<CFANode, Predicate> newPredicateMap = pmapBuilder.build();
    SymbPredAbsPrecision newPrecision;
    if (addPredicatesGlobally) {
      newPrecision = new SymbPredAbsPrecision(newPredicateMap.values());
    } else {
      newPrecision = new SymbPredAbsPrecision(newPredicateMap, globalPredicates);
    }

    logger.log(Level.ALL, "Predicate map now is", newPredicateMap);

    // We have two different strategies for the refinement root: set it to
    // the firstInterpolationElement or set it to highest location in the ART
    // where the same CFANode appears.
    // Both work, so this is a heuristics question to get the best performance.
    // My benchmark showed, that at least for the benchmarks-lbe examples it is
    // best to use strategy one iff newPredicatesFound.

    ARTElement root = null;
    if (newPredicatesFound) {
      logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 1: remove everything below", firstInterpolationElement, "from ART.");

      root = firstInterpolationARTElement;

    } else {
      if (absLocations.equals(lastErrorPath)) {
        throw new RefinementFailedException(RefinementFailedException.Reason.NoNewPredicates, null);
      }
      
      CFANode loc = firstInterpolationARTElement.retrieveLocationElement().getLocationNode();

      logger.log(Level.FINEST, "Found spurious counterexample,",
          "trying strategy 2: remove everything below node", loc, "from ART.");

      // find first element in path with location == loc
      for (ARTElement e : pArtPath) {
        if (e.retrieveLocationElement().getLocationNode().equals(loc)) {
          root = e;
          break;
        }
      }
      if (root == null) {
        throw new CPAException("Inconsistent ART, did not find element for " + loc);
      }
    }
    lastErrorPath = absLocations;
    return new Pair<ARTElement, SymbPredAbsPrecision>(root, newPrecision);
  }

  @Override
  protected Path getTargetPath(Path pPath) {
    if (targetPath == null) {
      logger.log(Level.WARNING, "The produced error path is imprecise!");
      return pPath;
    }
    return targetPath;
  }

  private Path createPathFromPredicateValues(Path pPath,
                          NavigableMap<Integer, Map<Integer, Boolean>> preds) {
    Path result = new Path();
    ARTElement currentElement = pPath.getFirst().getFirst();
    Integer currentIdx = -1;
    while (!currentElement.isTarget()) {
      Set<ARTElement> children = currentElement.getChildren();
      
      ARTElement child;
      CFAEdge edge;
      switch (children.size()) {
      
      case 0:
        logger.log(Level.WARNING, "ART target path terminates without reaching target element!");
        return null;
        
      case 1: // only one successor, easy
        child = Iterables.getOnlyElement(children);
        edge = currentElement.getEdgeToChild(child);
        break;
        
      case 2: // branch
        // first, find out the edges and the children
        CFAEdge trueEdge = null;
        CFAEdge falseEdge = null;
        ARTElement trueChild = null;
        ARTElement falseChild = null;

        for (ARTElement currentChild : children) {
          CFAEdge currentEdge = currentElement.getEdgeToChild(currentChild);
          if (!(currentEdge instanceof AssumeEdge)) {
            logger.log(Level.WARNING, "ART branches where there is no AssumeEdge!");
            return null;
          }

          if (((AssumeEdge)currentEdge).getTruthAssumption()) {
            trueEdge = currentEdge;
            trueChild = currentChild;
          } else {
            falseEdge = currentEdge;
            falseChild = currentChild;
          }
        }
        if (trueEdge == null || falseEdge == null) {
          logger.log(Level.WARNING, "ART branches with non-complementary AssumeEdges!");
          return null;
        }
        assert trueChild != null;
        assert falseChild != null;
        
        // search first idx where we have a predicate for the current branching
        Integer branchingId = currentElement.retrieveLocationElement().getLocationNode().getNodeNumber();
        Boolean predValue;
        do {
          Entry<Integer, Map<Integer, Boolean>> nextEntry = preds.higherEntry(currentIdx);
          if (nextEntry == null) {
            logger.log(Level.WARNING, "ART branches without direction information from solver!");
            return null;
          }
          
          currentIdx = nextEntry.getKey();
          predValue = nextEntry.getValue().get(branchingId);
        } while (predValue == null);
        
        // now select the right edge
        if (predValue) {
          edge = trueEdge;
          child = trueChild;
        } else {
          edge = falseEdge;
          child = falseChild;
        }
        break;
        
      default:
        logger.log(Level.WARNING, "ART splits with more than two branches!");
        return null;
      }

      result.add(new Pair<ARTElement, CFAEdge>(currentElement, edge));
      currentElement = child;
    }
    
    // need to add another pair with target element and outgoing edge
    Pair<ARTElement, CFAEdge> lastPair = pPath.getLast();
    if (currentElement != lastPair.getFirst()) {
      logger.log(Level.WARNING, "ART target path reached the wrong target element!");
      return null;
    }
    result.add(lastPair);
    
    return result;
  }
  
  public CounterexampleTraceInfo getCounterexampleTraceInfo() {
    return mCounterexampleTraceInfo;
  }
  
}
