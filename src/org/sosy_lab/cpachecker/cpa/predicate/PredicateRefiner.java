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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.Iterables.skip;
import static com.google.common.collect.Iterables.transform;
import static org.sosy_lab.cpachecker.util.AbstractElements.extractElementByType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractElement.AbstractionElement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.CounterexampleTraceInfo;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.predicate")
public class PredicateRefiner extends AbstractARTBasedRefiner {

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
  private final PredicateRefinementManager<?,?> formulaManager;
  private CounterexampleTraceInfo mCounterexampleTraceInfo;
  private Path targetPath;
  protected List<CFANode> lastErrorPath = null;

  public PredicateRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    super(pCpa);

    PredicateCPA predicateCpa = this.getArtCpa().retrieveWrappedCpa(PredicateCPA.class);
    if (predicateCpa == null) {
      throw new CPAException(getClass().getSimpleName() + " needs a PredicateCPA");
    }

    predicateCpa.getConfiguration().inject(this);
    logger = predicateCpa.getLogger();
    formulaManager = predicateCpa.getPredicateManager();
    predicateCpa.getStats().addRefiner(this);
  }

  @Override
  public boolean performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException {
    totalRefinement.start();
    logger.log(Level.FINEST, "Starting refinement for PredicateCPA");

    // create path with all abstraction location elements (excluding the initial element)
    // the last element is the element corresponding to the error location
    ArrayList<PredicateAbstractElement> path = new ArrayList<PredicateAbstractElement>();
    List<ARTElement> artPath = new ArrayList<ARTElement>();
    
    // copy to the above lists, skipping initial element and non-abstraction elements
    for (ARTElement ae : transform(skip(pPath, 1), Pair.<ARTElement>getProjectionToFirst())) {
      PredicateAbstractElement pe = extractElementByType(ae, PredicateAbstractElement.class);
      if (pe instanceof AbstractionElement) {
        path.add(pe);
        artPath.add(ae);
      }
    }
    
    Precision oldPrecision = pReached.getPrecision(pReached.getLastElement());
    PredicatePrecision oldPredicatePrecision = receivePredicatePrecision(oldPrecision);
    if (oldPredicatePrecision == null) {
      throw new IllegalStateException("Could not find the PredicatePrecision for the error element");
    }

    logger.log(Level.ALL, "Abstraction trace is", path);

    // build the counterexample
    mCounterexampleTraceInfo = formulaManager.buildCounterexampleTrace(path);
    targetPath = null;

    // if error is spurious refine
    if (mCounterexampleTraceInfo.isSpurious()) {
      logger.log(Level.FINEST, "Error trace is spurious, refining the abstraction");
      precisionUpdate.start();
      Pair<ARTElement, PredicatePrecision> refinementResult =
              performRefinement(oldPredicatePrecision, path, artPath, mCounterexampleTraceInfo);
      precisionUpdate.stop();

      artUpdate.start();
      
      removeSubtree(pReached, pPath, refinementResult.getFirst(), refinementResult.getSecond());
      
      artUpdate.stop();
      totalRefinement.stop();
      return true;
    } else {
      // we have a real error
      logger.log(Level.FINEST, "Error trace is not spurious");
      errorPathProcessing.start();
      
      boolean preciseInfo = false;
      NavigableMap<Integer, Map<Integer, Boolean>> preds = mCounterexampleTraceInfo.getBranchingPredicates();
      if (preds.isEmpty()) {
        logger.log(Level.WARNING, "No information about ART branches available!");
      } else {
        targetPath = createPathFromPredicateValues(pPath, preds);
        
        if (targetPath != null) {
          // try to create a better satisfying assignment by replaying this single path
          try {
            CounterexampleTraceInfo info2 = formulaManager.checkPath(targetPath.asEdgesList());
            if (info2.isSpurious()) {
              logger.log(Level.WARNING, "Inconsistent replayed error path!");
            } else {
              mCounterexampleTraceInfo = info2;
              preciseInfo = true;
            }
          } catch (CPATransferException e) {
            // path is now suddenly a problem 
            logger.log(Level.WARNING, "Could not replay error path (" + e.getMessage() + ")!");
          }
        }
      }
      errorPathProcessing.stop();
      
      if (exportErrorPath && exportFile != null) {
        if (!preciseInfo) {
          logger.log(Level.WARNING, "The produced satisfying assignment is imprecise!");
        }

        formulaManager.dumpCounterexampleToFile(mCounterexampleTraceInfo, dumpCexFile);
        try {
          Files.writeFile(exportFile, mCounterexampleTraceInfo.getCounterexample());
        } catch (IOException e) {
          logger.log(Level.WARNING, "Could not write satisfying assignment for error path to file! ("
              + e.getMessage() + ")");
        }
      }
      totalRefinement.stop();
      return false;
    }
  }
  
  protected void removeSubtree(ARTReachedSet pReached, Path pPath, ARTElement pFirst, PredicatePrecision pSecond) {
    pReached.removeSubtree(pFirst, pSecond);    
  }

  protected final PredicatePrecision receivePredicatePrecision(Precision precision) {
    PredicatePrecision result = null;
    if (precision instanceof PredicatePrecision) {
      result = (PredicatePrecision)precision;
    } else if (precision instanceof WrapperPrecision) {
      result = ((WrapperPrecision)precision).retrieveWrappedPrecision(PredicatePrecision.class);
    }
    return result;
  }

  /**
   * pPath and pArtPath need to fit together such that
   * pPath.get(i) == pArtPath.get(i).retrieveWrappedElement(PredicateAbstractElement) 
   */
  private Pair<ARTElement, PredicatePrecision> performRefinement(PredicatePrecision oldPrecision,
      ArrayList<PredicateAbstractElement> pPath, List<ARTElement> pArtPath,
      CounterexampleTraceInfo pInfo) throws CPAException {

    Multimap<CFANode, AbstractionPredicate> oldPredicateMap = oldPrecision.getPredicateMap();
    Set<AbstractionPredicate> globalPredicates = oldPrecision.getGlobalPredicates();
    
    PredicateAbstractElement firstInterpolationElement = null;
    ARTElement firstInterpolationARTElement = null;
    boolean newPredicatesFound = false;
    
    List<CFANode> absLocations = new ArrayList<CFANode>(pArtPath.size());
    ImmutableSetMultimap.Builder<CFANode, AbstractionPredicate> pmapBuilder = ImmutableSetMultimap.builder();

    pmapBuilder.putAll(oldPredicateMap);

    // iterate synchronously through pArtPath and pPath
    int i = -1;
    for (ARTElement ae : pArtPath) {
      i++;
      PredicateAbstractElement e = pPath.get(i);
      CFANode loc = ae.retrieveLocationElement().getLocationNode();
      Collection<AbstractionPredicate> newpreds = getPredicatesForARTElement(pInfo, ae);     
      
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
    assert firstInterpolationElement == extractElementByType(firstInterpolationARTElement, PredicateAbstractElement.class);

    ImmutableSetMultimap<CFANode, AbstractionPredicate> newPredicateMap = pmapBuilder.build();
    PredicatePrecision newPrecision;
    if (addPredicatesGlobally) {
      newPrecision = new PredicatePrecision(newPredicateMap.values());
    } else {
      newPrecision = new PredicatePrecision(newPredicateMap, globalPredicates);
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
    return Pair.of(root, newPrecision);
  }

  protected Collection<AbstractionPredicate> getPredicatesForARTElement(
      CounterexampleTraceInfo pInfo, ARTElement pAE) {
    PredicateAbstractElement pE = AbstractElements.extractElementByType(pAE, PredicateAbstractElement.class);
    return pInfo.getPredicatesForRefinement(pE);
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

      result.add(Pair.of(currentElement, edge));
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
