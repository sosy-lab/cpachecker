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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.art.Path;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.mathsat.MathsatFormula;


public class PredicateMap
{
  private Map<CFAEdge, Set<AbstractionPredicate>> predicateMap;

  private Set<String> assumptions = new HashSet<String>();

  public PredicateMap(List<Collection<AbstractionPredicate>> pathPredicates, Path path)
  {
    predicateMap = new HashMap<CFAEdge, Set<AbstractionPredicate>>();

    int i = 0;
    for(Collection<AbstractionPredicate> predicates : pathPredicates)
    {
      if(predicates.size() > 0)
      {
        CFAEdge currentEdge = path.get(i).getSecond();

//System.out.println("checking edge " + currentEdge + " for predicates ...");

        // get the predicates for the current edge from the predicate map
        Set<AbstractionPredicate> predicatesAtEdge = predicateMap.get(currentEdge);

        // if there are not any yet, create a new set
        if(predicatesAtEdge == null)
          predicatesAtEdge = new HashSet<AbstractionPredicate>();

        // add each non-trivial predicate to the respective set of the predicate map
        for(AbstractionPredicate predicate : predicates)
        {
          if(!predicate.getSymbolicAtom().isFalse())
          {
            predicatesAtEdge.add(predicate);
//System.out.println("   adding predicate " + predicate + " to edge " + currentEdge);
          }
        }

        // if non-trivial predicates are associated with the edge, add them to the map
        if(predicatesAtEdge.size() > 0)
          predicateMap.put(currentEdge, predicatesAtEdge);
      }

      i++;
    }
  }

  public boolean isInterpolationPoint(CFAEdge edge)
  {
    return predicateMap.containsKey(edge);
  }

  public Set<String> getReferencedVariables()
  {
    Set<String> variables = new HashSet<String>();

    for(Set<AbstractionPredicate> predicates : predicateMap.values())
    {
      for(AbstractionPredicate predicate : predicates)
      {
        Collection<String> atoms = extractVariables(((MathsatFormula)predicate.getSymbolicAtom()).getTerm());

        for(String atom : atoms)
          variables.add(atom);
      }
    }

    return variables;
  }

  public Map<CFAEdge, Map<String, Long>> getAssumptions(Map<CFAEdge, Map<String, Long>> initalAssumptions)
  {
    for(Map.Entry<CFAEdge, Set<AbstractionPredicate>> entry : predicateMap.entrySet())
    {
      Map<String, Long> assumes = initalAssumptions.get(entry.getKey());

      for(AbstractionPredicate predicate : entry.getValue())
      {
        Collection<String> atoms = extractVariables(((MathsatFormula)predicate.getSymbolicAtom()).getTerm());

        // if there is only one atom ...
        // TODO: this needs cleanup!
        if(atoms.size() == 1)
        {
          if(assumes == null)
            initalAssumptions.put(entry.getKey(), assumes = new HashMap<String, Long>());

          String assume = predicate.getSymbolicAtom().toString().replace("(", "").replace(")", "");

          String[] splits = assume.split(" = ");

          // ... and it is compared to a constant ...
          if(splits.length == 2)
          {
            // ... add it as a fact
            try
            {
              Long constant = Long.parseLong(splits[1]);
//System.out.println("added fact " + atoms.toArray(new String[atoms.size()])[0] + " = " + splits[1] + " for expression " + predicate.getSymbolicAtom() + " at edge " + entry.getKey());

              assumes.put(atoms.toArray(new String[atoms.size()])[0], constant);

              assumptions.add(assume);
            }
            catch(NumberFormatException nfe)
            {
//System.out.println(splits[1] + " is not a number - no fact to add for expression " + predicate.getSymbolicAtom());
            }
          }
          else
          {
//System.out.println("can't extract fact from expression " + assume);
          }
        }
      }
    }

//System.out.println("\nassumptions: " + assumptions);

    return initalAssumptions;
  }



  // TODO: copy & paste code from branches/fshell3/src/org/sosy_lab/cpachecker/util/predicates/mathsat/MathsatFormulaManager.java
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
}