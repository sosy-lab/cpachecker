/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.StaticRefiner;
import org.sosy_lab.cpachecker.util.VariableClassification;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;

@Options(prefix="staticRefiner")
public class PredicateStaticRefiner extends StaticRefiner {

  @Option(description="Apply mined predicates on the corresponding scope. false = add them to the global precision.")
  private boolean applyScoped = true;

  @Option(description="Add all assumtions along a error trace to the precision.")
  private boolean addAllErrorTraceAssumes = false;

  @Option(description="Add all assumtions from the control flow automaton to the precision.")
  private boolean addAllControlFlowAssumes = false;

  @Option(description="Add all assumtions along a error trace to the precision.")
  private boolean addAssumesByBoundedBackscan = true;

  @Option(description = "Dump CFA assume edges as SMTLIB2 formulas to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path assumePredicatesFile = null;

  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView formulaManagerView;
  private final BooleanFormulaManager booleanManager;
  private final PredicateAbstractionManager predAbsManager;
  private final VariableClassification varClasses;
  private final Solver solver;
  private final CFA cfa;

  private Multimap<String, AStatementEdge> directlyAffectingStatements;

  public PredicateStaticRefiner(
      Configuration pConfig,
      LogManager pLogger,
      Solver pSolver,
      PathFormulaManager pPathFormulaManager,
      FormulaManagerView pFormulaManagerView,
      PredicateAbstractionManager pPredAbsManager,
      CFA pCfa) throws InvalidConfigurationException {
    super(pConfig, pLogger);

    pConfig.inject(this);

    this.cfa = pCfa;
    assert cfa.getVarClassification().isPresent();
    this.varClasses = cfa.getVarClassification().get();

    this.pathFormulaManager = pPathFormulaManager;
    this.predAbsManager = pPredAbsManager;
    this.solver = pSolver;
    this.formulaManagerView = pFormulaManagerView;
    this.booleanManager = formulaManagerView.getBooleanFormulaManager();

    if (assumePredicatesFile != null) {
      dumpAssumePredicate(assumePredicatesFile);
    }
  }

  private boolean isAssumeOnLoopVariable(AssumeEdge e) {
    Collection<String> referenced = varClasses.getVariablesOfExpression((CExpression) e.getExpression());

    for (String var: referenced) {
      if (varClasses.getLoopExitConditionVariables().contains(var)) {
        return true;
      }
    }

    return false;
  }

  private void buildDirectlyAffectingStatements() {
    if (directlyAffectingStatements != null) {
      return;
    }

    directlyAffectingStatements = LinkedHashMultimap.create();

    for (CFANode u : cfa.getAllNodes()) {
      Deque<CFAEdge> edgesToHandle = Queues.newArrayDeque(CFAUtils.leavingEdges(u));
      while (!edgesToHandle.isEmpty()) {
        CFAEdge e = edgesToHandle.pop();
        if (e instanceof MultiEdge) {
          edgesToHandle.addAll(((MultiEdge) e).getEdges());
        } else if (e instanceof CStatementEdge) {
          CStatementEdge stmtEdge = (CStatementEdge) e;
          if (stmtEdge.getStatement() instanceof CAssignment) {
            CAssignment assign = (CAssignment) stmtEdge.getStatement();

            if (assign.getLeftHandSide() instanceof CIdExpression) {
              String variable = ((CIdExpression)assign.getLeftHandSide()).getDeclaration().getQualifiedName();
              directlyAffectingStatements.put(variable, stmtEdge);
            }
          }
        }
      }
    }
  }

  private boolean isContradicting(AssumeEdge assume, AStatementEdge stmt) throws CPATransferException, InterruptedException {
    // Check stmt ==> assume?

    BooleanFormula stmtFormula = pathFormulaManager.makeAnd(
        pathFormulaManager.makeEmptyPathFormula(), stmt).getFormula();

    BooleanFormula assumeFormula = pathFormulaManager.makeAnd(
        pathFormulaManager.makeEmptyPathFormula(), assume).getFormula();

    BooleanFormula query = formulaManagerView.uninstantiate(booleanManager.and(stmtFormula, assumeFormula));
    boolean contra = solver.isUnsat(query);

    if (contra) {
      logger.log(Level.INFO, "Contradiction found ", query);
    }

    return contra;


    /* [a == 1]
     *  a = <literal>, where <literal> != 1
     *  a = a +-* <literal>
     *
     *  Variable classification can be used!!
     *
     *  if a IN the set Eq
     *
     */
  }

  private boolean hasContradictingOperationInFlow(AssumeEdge e) throws CPATransferException, InterruptedException {
    buildDirectlyAffectingStatements();

    Collection<String> referenced = varClasses.getVariablesOfExpression((CExpression) e.getExpression());
    for (String varName: referenced) {
      Collection<AStatementEdge> affectedByStmts = directlyAffectingStatements.get(varName);
      for (AStatementEdge stmtEdge: affectedByStmts) {
        if (isContradicting(e, stmtEdge)) {
          return true;
        }
      }
    }
    return false;
  }

  private Set<AssumeEdge> getAllNonLoopControlFlowAssumes() throws CPATransferException, InterruptedException {
    Set<AssumeEdge> result = new HashSet<>();

    for (CFANode u : cfa.getAllNodes()) {
      for (CFAEdge e : CFAUtils.leavingEdges(u)) {
        if (e instanceof AssumeEdge) {
          AssumeEdge assume = (AssumeEdge) e;
          if (!isAssumeOnLoopVariable(assume)) {
            if (hasContradictingOperationInFlow(assume)) {
              result.add(assume);
            }
          }
        }
      }
    }

    return result;
  }

  private Set<AssumeEdge> getAssumeEdgesAlongPath(UnmodifiableReachedSet reached, ARGState targetState) throws CPATransferException, InterruptedException {
    Set<AssumeEdge> result = new HashSet<>();

    Set<ARGState> allStatesOnPath = ARGUtils.getAllStatesOnPathsTo(targetState);
    for (ARGState s: allStatesOnPath) {
      CFANode u = AbstractStates.extractLocation(s);
      for (CFAEdge e : CFAUtils.leavingEdges(u)) {
        CFANode v = e.getSuccessor();
        Collection<AbstractState> reachedOnV = reached.getReached(v);

        boolean edgeOnTrace = false;
        for (AbstractState ve : reachedOnV) {
          if (allStatesOnPath.contains(ve)) {
            edgeOnTrace = true;
            break;
          }
        }

        if (edgeOnTrace) {
          if (e instanceof AssumeEdge) {
            AssumeEdge assume = (AssumeEdge) e;
            if (!isAssumeOnLoopVariable(assume)) {
              if (hasContradictingOperationInFlow(assume)) {
                result.add(assume);
              }
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * This method extracts a precision based only on static information derived from the CFA.
   *
   * @return a precision for the predicate CPA
   * @throws CPATransferException
   * @throws InterruptedException
   */
  public PredicatePrecision extractPrecisionFromCfa(UnmodifiableReachedSet pReached,
      List<ARGState> abstractionStatesTrace, boolean atomicPredicates) throws CPATransferException, InterruptedException {
    logger.log(Level.FINER, "Extracting precision from CFA...");

    // Predicates that should be tracked on function scope
    Multimap<String, AbstractionPredicate> functionPredicates = ArrayListMultimap.create();

    // Predicates that should be tracked globally
    Collection<AbstractionPredicate> globalPredicates = Lists.newArrayList();

    // Determine the ERROR location of the path (last node)
    ARGState targetState = abstractionStatesTrace.get(abstractionStatesTrace.size()-1);
    CFANode targetLocation = AbstractStates.extractLocation(targetState);

    // Determine the assume edges that should be considered for predicate extraction
    Set<AssumeEdge> assumeEdges = new HashSet<>();

    if (addAllControlFlowAssumes) {
      assumeEdges.addAll(getAllNonLoopControlFlowAssumes());
    } else {
      if (addAllErrorTraceAssumes) {
        assumeEdges.addAll(getAssumeEdgesAlongPath(pReached, targetState));
      }
      if (addAssumesByBoundedBackscan) {
        assumeEdges.addAll(getTargetLocationAssumes(Lists.newArrayList(targetLocation)).values());
      }
    }

    // Create predicates for the assume edges and add them to the precision
    for (AssumeEdge assume : assumeEdges) {
      // Create a boolean formula from the assume
      Collection<AbstractionPredicate> preds = assumeEdgeToPredicates(atomicPredicates, assume);

      // Check whether the predicate should be used global or only local
      boolean applyGlobal = true;
      if (applyScoped) {
        for (CIdExpression idExpr : getVariablesOfAssume(assume)) {
          CSimpleDeclaration decl = idExpr.getDeclaration();
          if (decl instanceof CVariableDeclaration) {
            if (!((CVariableDeclaration) decl).isGlobal()) {
              applyGlobal = false;
            }
          } else if (decl instanceof CParameterDeclaration) {
            applyGlobal = false;
          }
        }
      }

      // Add the predicate to the resulting precision
      if (applyGlobal) {
        logger.log(Level.FINEST, "Global predicates mined", preds);
        globalPredicates.addAll(preds);
      } else {
        logger.log(Level.FINEST, "Function predicates mined", preds);
        String function = assume.getPredecessor().getFunctionName();
        functionPredicates.putAll(function, preds);
      }
    }

    logger.log(Level.FINER, "Extracting finished.");

    return new PredicatePrecision(
        ImmutableSetMultimap.<Pair<CFANode,Integer>,
        AbstractionPredicate>of(),
        ArrayListMultimap.<CFANode, AbstractionPredicate>create(),
        functionPredicates,
        globalPredicates);
  }

  private Collection<AbstractionPredicate> assumeEdgeToPredicates(boolean atomicPredicates, AssumeEdge assume) throws CPATransferException, InterruptedException {
    BooleanFormula relevantAssumesFormula = pathFormulaManager.makeAnd(
        pathFormulaManager.makeEmptyPathFormula(), assume).getFormula();

    Collection<AbstractionPredicate> preds;
    if (atomicPredicates) {
      preds = predAbsManager.extractPredicates(relevantAssumesFormula);
    } else {
      preds = ImmutableList.of(predAbsManager.createPredicateFor(
          formulaManagerView.uninstantiate(relevantAssumesFormula)));
    }

    return preds;
  }

  protected void dumpAssumePredicate(Path target) {
    try (Writer w = Files.openOutputFile(target)) {
      for (CFANode u : cfa.getAllNodes()) {
        for (CFAEdge e: CFAUtils.leavingEdges(u)) {
          if (e instanceof AssumeEdge) {
            Collection<AbstractionPredicate> preds = assumeEdgeToPredicates(false, (AssumeEdge) e);
            for (AbstractionPredicate p: preds) {
              w.append(p.getSymbolicAtom().toString());
              w.append("\n");
            }
          }
        }
      }
    } catch (InterruptedException e) {
      logger.logUserException(Level.WARNING, e, "Interrupted, could not write assume predicates to file!");
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "IO exception! Could not write assume predicates to file!");
    } catch (CPATransferException e) {
      logger.logUserException(Level.WARNING, e, "Transfer exception! Could not write assume predicates to file!");
    }
  }


}