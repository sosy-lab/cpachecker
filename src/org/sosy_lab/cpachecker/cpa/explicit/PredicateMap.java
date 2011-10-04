/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import static mathsat.api.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormula;


public class PredicateMap
{
  /**
   * the mapping from program location to a collection of predicates
   */
  private Map<CFANode, Set<AbstractionPredicate>> predicateMap;

  /**
   * the set of variable names, that are referenced in the path predicates
   */
  private Set<String> referencedVariables = new HashSet<String>();

  /**
   * This method acts as the constructor of the class.
   *
   * Given a list of sets of predicates and a program path, it creates the mapping from program location to a collection of predicates.
   *
   * @param pathPredicates the predicates as returned from the refinement
   * @param path the path to the error location
   */
  public PredicateMap(List<Collection<AbstractionPredicate>> pathPredicates, Path path)
  {
    predicateMap = new HashMap<CFANode, Set<AbstractionPredicate>>();

    int i = 0;
    for(Collection<AbstractionPredicate> predicates : pathPredicates)
    {
      if(predicates.size() > 0)
      {
        CFANode currentLocation = path.get(i).getSecond().getPredecessor();

        // get the predicates for the current location from the predicate map
        Set<AbstractionPredicate> predicatesAtEdge = predicateMap.get(currentLocation);

        // if there are not any yet, create a new set
        if(predicatesAtEdge == null)
          predicatesAtEdge = new HashSet<AbstractionPredicate>();

        // add each non-trivial predicate to the respective set of the predicate map
        for(AbstractionPredicate predicate : predicates)
        {
          if(!predicate.getSymbolicAtom().isFalse())
            predicatesAtEdge.add(predicate);
        }

        // if non-trivial predicates are associated with the edge, add them to the map
        if(predicatesAtEdge.size() > 0)
          predicateMap.put(currentLocation, predicatesAtEdge);
      }

      i++;
    }
  }

  /**
   * This method decides whether or not the given location is a interpolation point or not.
   *
   * @param location the location for which to decide whether it is a interpolation point or not
   * @return true if it is a interpolation point, else false
   */
  public boolean isInterpolationPoint(CFANode location)
  {
    return predicateMap.containsKey(location);
  }

  /**
   * This method returns the set of variables referenced in the predicates. The return value is only valid after a call to getVariablesFromPredicates.
   *
   * @return the set of variables referenced in the predicates
   */
  public Set<String> getReferencedVariables()
  {
    return referencedVariables;
  }

  /**
   * This method returns those variables that are reference in the predicates and groups them by program locations.
   *
   * @return a mapping from program locations to variables referenced in predicates at that program location
   */
  public Map<CFANode, Set<String>> getVariablesFromPredicates()
  {
    Map<CFANode, Set<String>> locToVarsMap = new HashMap<CFANode, Set<String>>();

    // for each program location in the mapping ...
    for(Map.Entry<CFANode, Set<AbstractionPredicate>> predicatesAtEdge : predicateMap.entrySet())
    {
      CFANode currentNode = predicatesAtEdge.getKey();

      // ... and for each predicate in the set of that location ...
      for(AbstractionPredicate predicate : predicatesAtEdge.getValue())
      {
        // ... get the names of the variables referenced in that predicate ...
        Collection<String> atoms = extractVariables(((MathsatFormula)predicate.getSymbolicAtom()).getTerm());

        // ... and add them to location-to-variables-mapping
        if(!atoms.isEmpty())
        {
          Set<String> variables = locToVarsMap.get(currentNode);

          if(variables == null)
            locToVarsMap.put(currentNode, variables = new HashSet<String>());

          variables.addAll(atoms);

          referencedVariables.addAll(atoms);
        }
      }
    }

    return locToVarsMap;
  }

  /**
   * This method extracts the variables in plain text from a MathSAT term.
   *
   * @todo: copy & paste code from branches/fshell3/src/org/sosy_lab/cpachecker/util/predicates/mathsat/MathsatFormulaManager.java
   * @param pTerm the MathSAT term from which to extract the variables
   * @return the collection of varaibles extracted from the MathSAT term
   */
  private static Collection<String> extractVariables(long pTerm) {
    HashSet<String> lVariables = new HashSet<String>();

    LinkedList<Long> lWorklist = new LinkedList<Long>();
    lWorklist.add(pTerm);

    while (!lWorklist.isEmpty()) {
      long lTerm = lWorklist.removeFirst();

      if (msat_term_is_variable(lTerm) != 0) {
        lVariables.add(msat_term_repr(lTerm));
      }
      else {
        int lArity = msat_term_arity(lTerm);
        for (int i = 0; i < lArity; i++) {
          long lSubterm = msat_term_get_arg(lTerm, i);
          lWorklist.add(lSubterm);
        }
      }
    }

    return lVariables;
  }

  @Override
  public String toString()
  {
    return predicateMap.toString();
  }
}