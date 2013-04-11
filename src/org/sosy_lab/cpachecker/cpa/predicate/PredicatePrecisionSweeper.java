/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractStatistics;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;


public class PredicatePrecisionSweeper {

  private final LogManager logger;
  private final AbstractStatistics statistics;

  private static final String FALSE_PREDICATE = "false";

  public PredicatePrecisionSweeper(LogManager pLogger, AbstractStatistics pStatistics) {
    this.logger = pLogger;
    this.statistics = pStatistics;
  }

  private Set<String> getDeclaredVariables(CFA pCFA) {
    Set<String> declaredVariables = new HashSet<>();
    Set<CFAEdge> knownEdges = new HashSet<>();

    for (CFANode u : pCFA.getAllNodes()) {
      Stack<CFAEdge> edges = new Stack<>();
      edges.addAll(CFAUtils.allLeavingEdges(u).toList());
      while(!edges.empty()) {
        CFAEdge e = edges.pop();
        if (knownEdges.add(e)) {
          if (e instanceof MultiEdge) {
            MultiEdge me = (MultiEdge) e;
            edges.addAll(me.getEdges());
          } else if (e instanceof CDeclarationEdge) {
            CDeclaration decl = ((CDeclarationEdge) e).getDeclaration();
            if (decl instanceof CFunctionDeclaration) {
              CFunctionDeclaration fnDecl = (CFunctionDeclaration) decl;
              for (CParameterDeclaration paramDecl: fnDecl.getParameters()) {
                declaredVariables.add(paramDecl.getQualifiedName());
              }
            } else if (decl instanceof CVariableDeclaration) {
              CVariableDeclaration varDecl = (CVariableDeclaration) decl;
              declaredVariables.add(varDecl.getQualifiedName());
            }
          }
        }
      }
    }

    return declaredVariables;
  }

  private Set<String> getVariablesTalkedAbout(AbstractionPredicate pTalkedAboutIn) {
    Set<String> result = new HashSet<>();
    String p = pTalkedAboutIn.getSymbolicAtom().toString();
    for (String s : p.split("[\\s()``]")) {
      result.add(s);
    }

    return result;
  }

  public boolean shouldSweepPredicate(Set<String> declaredVariables, AbstractionPredicate pred) {
    Set<String> predicateVariables = getVariablesTalkedAbout(pred);
    if (predicateVariables.size() <= 2 && predicateVariables.contains(FALSE_PREDICATE)) {
      return false;
    }

    return Sets.intersection(predicateVariables, declaredVariables).isEmpty();
  }

  public PredicatePrecision sweepPrecision(CFA pCFA, PredicatePrecision pToSweep) {
    Preconditions.checkNotNull(pCFA);
    Preconditions.checkNotNull(pToSweep);

    if (pCFA.getLanguage() != Language.C) {
      logger.log(Level.WARNING, "Sweeping only supported for the C programming language!");
      return pToSweep;
    }

    Set<AbstractionPredicate> sweepedPredicates = new HashSet<>();
    Set<String> declaredVariables = getDeclaredVariables(pCFA);

    // Sweep local predicates ...
    Multimap<CFANode, AbstractionPredicate> localPredicates = ArrayListMultimap.create();
    for (CFANode u: pToSweep.getLocalPredicates().keySet()) {
      for (AbstractionPredicate p: pToSweep.getLocalPredicates().get(u)) {
        if (shouldSweepPredicate(declaredVariables, p)) {
          sweepedPredicates.add(p);
        } else {
          localPredicates.put(u, p);
        }
      }
    }

    // Sweep global predicates ...
    Collection<AbstractionPredicate> globalPredicates = Lists.newArrayList();
    for (AbstractionPredicate p: pToSweep.getGlobalPredicates()) {
      if (shouldSweepPredicate(declaredVariables, p)) {
        sweepedPredicates.add(p);
      } else {
        globalPredicates.add(p);
      }
    }

    // Sweep function predicates ...
    Multimap<String, AbstractionPredicate> functionPredicates = ArrayListMultimap.create();
    for (String f: pToSweep.getFunctionPredicates().keys()) {
      for (AbstractionPredicate p: pToSweep.getFunctionPredicates().get(f)) {
        if (shouldSweepPredicate(declaredVariables, p)) {
          sweepedPredicates.add(p);
        } else {
          functionPredicates.put(f, p);
        }
      }
    }

    statistics.addKeyValueStatistic("Sweeped predicates", sweepedPredicates.size());
    logger.log(Level.FINE, "Sweeped", sweepedPredicates);

    return new PredicatePrecision(localPredicates, functionPredicates, globalPredicates);
  }

}
