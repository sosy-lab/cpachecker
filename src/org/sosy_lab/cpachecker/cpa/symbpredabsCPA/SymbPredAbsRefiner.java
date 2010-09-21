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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.WrapperPrecision;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.art.AbstractARTBasedRefiner;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Model;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.Predicate;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel.MathsatAssignable;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel.MathsatBooleanValue;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel.MathsatType;
import org.sosy_lab.cpachecker.util.symbpredabstraction.mathsat.MathsatModel.MathsatVariable;
import org.sosy_lab.cpachecker.util.symbpredabstraction.trace.CounterexampleTraceInfo;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@Options(prefix="cpas.symbpredabs")
public class SymbPredAbsRefiner extends AbstractARTBasedRefiner {

  @Option(name="refinement.addPredicatesGlobally")
  private boolean addPredicatesGlobally = false;
  
  @Option(name="errorPath.export")
  private boolean exportErrorPath = true;
  
  @Option(name="errorPath.file", type=Option.Type.OUTPUT_FILE)
  private File exportFile = new File("ErrorPathAssignment.txt");
  
  private final LogManager logger;
  private final SymbPredAbsFormulaManager formulaManager;
  private CounterexampleTraceInfo mCounterexampleTraceInfo;

  public SymbPredAbsRefiner(final ConfigurableProgramAnalysis pCpa) throws CPAException, InvalidConfigurationException {
    super(pCpa);

    SymbPredAbsCPA symbPredAbsCpa = this.getArtCpa().retrieveWrappedCpa(SymbPredAbsCPA.class);
    if (symbPredAbsCpa == null) {
      throw new CPAException(getClass().getSimpleName() + " needs a SymbPredAbsCPA");
    }

    symbPredAbsCpa.getConfiguration().inject(this);
    logger = symbPredAbsCpa.getLogger();
    formulaManager = symbPredAbsCpa.getFormulaManager();
  }

  @Override
  public boolean performRefinement(ARTReachedSet pReached, Path pPath) throws CPAException {

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

      if (symbElement.isAbstractionNode()) {
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
      Pair<ARTElement, SymbPredAbsPrecision> refinementResult =
              performRefinement(oldSymbPredAbsPrecision, path, artPath, info);

      pReached.removeSubtree(refinementResult.getFirst(), refinementResult.getSecond());
      return true;
    } else {
      // we have a real error
      logger.log(Level.FINEST, "Error trace is not spurious");
      
      if (exportErrorPath) {
        try {
          Files.writeFile(exportFile, info.getCounterexample(), false);
        } catch (IOException e) {
          logger.log(Level.WARNING, "Could not write satisfying assignment for error path to file! ("
              + e.getMessage() + ")");
        }
      }
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
    
    ImmutableSetMultimap.Builder<CFANode, Predicate> pmapBuilder = ImmutableSetMultimap.builder();

    pmapBuilder.putAll(oldPredicateMap);

    // iterate synchronously through pArtPath and pPath
    int i = -1;
    for (ARTElement ae : pArtPath) {
      i++;
      SymbPredAbsAbstractElement e = pPath.get(i);
      Collection<Predicate> newpreds = pInfo.getPredicatesForRefinement(e);
      CFANode loc = ae.retrieveLocationElement().getLocationNode();
      
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
    return new Pair<ARTElement, SymbPredAbsPrecision>(root, newPrecision);
  }
  
  private static Pattern PRED_NAME = Pattern.compile("^.*__assume__(?=\\d+@\\d+$)");
  
  @Override
  protected Path getTargetPath(Path pPath) {
    Model m = mCounterexampleTraceInfo.getCounterexample();
    if (!(m instanceof MathsatModel)) {
      logger.log(Level.WARNING, "Error path analysis is currently only supported for Mathsat. Error path output may be incorrect.");
      return null;
    }
    MathsatModel model = (MathsatModel)m; 
    
    NavigableMap<Integer, Map<Integer, Boolean>> preds = Maps.newTreeMap();
    for (MathsatAssignable a : model.getAssignables()) {
      if (a instanceof MathsatVariable
          && a.getType() == MathsatType.Boolean) {
        
        Matcher matcher = PRED_NAME.matcher(a.getName());
        String name = matcher.replaceFirst("");
        if (!name.equals(a.getName())) {
          // pattern matched, so it's a variable with __assume__ in it
          
          String[] parts = name.split("@");
          assert parts.length == 2;
          Integer edgeId = Integer.parseInt(parts[0]);
          Integer idx = Integer.parseInt(parts[1]);          
          
          Map<Integer, Boolean> p = preds.get(idx);
          if (p == null) {
            p = new HashMap<Integer, Boolean>(2);
            preds.put(idx, p);
          }
          
          Boolean value = ((MathsatBooleanValue)model.getValue(a)).isTrue();
          p.put(edgeId, value);
        }             
      }
    }
    
    Path result = new Path();
    ARTElement currentElement = pPath.getFirst().getFirst();
    Integer currentIdx = 0;
    while (!currentElement.isTarget()) {
      Set<ARTElement> children = currentElement.getChildren();
      assert !children.isEmpty() : "Path ended to early";
      
      if (children.size() == 1) {
        // only one successor, easy
        ARTElement child = Iterables.getOnlyElement(children);
        CFAEdge edge = currentElement.getEdgeToChild(child);
        result.add(new Pair<ARTElement, CFAEdge>(currentElement, edge));
        currentElement = child;
        continue;
      }
      
      Set<CFAEdge> outgoingEdges = new HashSet<CFAEdge>();
      for (ARTElement child : children) {
        outgoingEdges.add(currentElement.getEdgeToChild(child));
      }
      if (outgoingEdges.size() == 1) {
        // several successors, but only one edge
        // just take any successor and hope this will work
        ARTElement child = Iterables.get(children, 0);
        CFAEdge edge = Iterables.getOnlyElement(outgoingEdges);
result.add(new Pair<ARTElement, CFAEdge>(currentElement, edge));
        currentElement = child;
        continue;
      }
      
      // several outgoing edges
      Set<Integer> edgeIds = new HashSet<Integer>();
      for (CFAEdge currentEdge : outgoingEdges) {
        assert currentEdge.getEdgeType() == CFAEdgeType.AssumeEdge : "Several outgoing edges but no AssumeEdge";
        edgeIds.add(currentEdge.getEdgeNumber());
      }
      
      // search first idx where we have a value for any of the outgoing edges 
      Map<Integer, Boolean> currentPreds;
      do {
        Entry<Integer, Map<Integer, Boolean>> nextEntry = preds.higherEntry(currentIdx);
        assert nextEntry != null : "Set of predicates ended before path ended";
        
        currentIdx = nextEntry.getKey();
        currentPreds = nextEntry.getValue();
      } while (Sets.intersection(edgeIds, currentPreds.keySet()).isEmpty());
      
      CFAEdge edge = null;
      for (CFAEdge currentEdge : outgoingEdges) {
        Boolean value = currentPreds.get(currentEdge.getEdgeNumber());
        // TODO find out why there is sometimes no value or more than one value
        if (value == Boolean.TRUE) {
          assert edge == null || edge == currentEdge : "At least two outgoing edges are possible";
          edge = currentEdge;
        }
      }
      assert edge != null : "No outgoing edge is possible";
      
      CFANode childLocation = edge.getSuccessor();
      
      ARTElement child = null;
      for (ARTElement currentChild : children) {
        if (childLocation.equals(currentChild.retrieveLocationElement().getLocationNode())) {
          child = currentChild;
          break;
        }
      }
      assert child != null : "Set of children and set of edges don't match";
      
      result.add(new Pair<ARTElement, CFAEdge>(currentElement, edge));
      currentElement = child;
    }
    
    Pair<ARTElement, CFAEdge> lastPair = pPath.getLast();
    assert currentElement == lastPair.getFirst() : "Target path did not lead to target element";
    result.add(lastPair);
    
    return result;
  }
  
  public CounterexampleTraceInfo getCounterexampleTraceInfo() {
    return mCounterexampleTraceInfo;
  }
  
}
