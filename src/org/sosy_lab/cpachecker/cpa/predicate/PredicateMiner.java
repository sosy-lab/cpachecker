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

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;


@Options(prefix="predicate.mining")
public class PredicateMiner {

  @Option(description="Collect at most n assumes allong a path backwards from a target (error) location.")
  private int maxBackscanPathAssumes = 1;

  @Option(description="Apply mined predicates on the corresponding scope. false = add them to the global precision.")
  private boolean applyScoped = true;

  private final PathFormulaManager pathFormulaManager;
  private final FormulaManagerView formulaManagerView;
  private final AbstractionManager abstractionManager;

  private final Configuration config;
  private final LogManager logger;

  private final CExpressionVisitor<List<String>, CPATransferException> referencedVariablesVisitor = new CExpressionVisitor<List<String>, CPATransferException>() {
    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression pIastIdExpression) throws CPATransferException {
      return Lists.newArrayList(pIastIdExpression.getName());
    }

    @Override
    public List<String>visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws CPATransferException {
      return Lists.newArrayList();
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression pIastBinaryExpression) throws CPATransferException {
      List<String> result1 = pIastBinaryExpression.getOperand1().accept(this);
      List<String> result2 = pIastBinaryExpression.getOperand2().accept(this);

      result2.addAll(result1);

      return result2;
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression pIastCastExpression) throws CPATransferException {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression pIastCharLiteralExpression) throws CPATransferException {
      return Lists.newArrayList();
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference pIastFieldReference) throws CPATransferException {
      return Lists.newArrayList(pIastFieldReference.getFieldName());
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression pIastFloatLiteralExpression) throws CPATransferException {
      return Lists.newArrayList();
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression pIastIntegerLiteralExpression) throws CPATransferException {
      return Lists.newArrayList();
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression pIastStringLiteralExpression) throws CPATransferException {
      return Lists.newArrayList();
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression pIastTypeIdExpression) throws CPATransferException {
      return Lists.newArrayList();
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression pCTypeIdInitializerExpression) throws CPATransferException {
      return Lists.newArrayList();
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression pIastUnaryExpression) throws CPATransferException {
      return pIastUnaryExpression.getOperand().accept(this);
    }

  };

  public PredicateMiner(Configuration config, LogManager logger, PathFormulaManager pathFormulaManager, FormulaManagerView formulaManagerView, AbstractionManager abstractionManager) throws InvalidConfigurationException {
    this.logger = logger;
    this.config = config;
    this.pathFormulaManager = pathFormulaManager;
    this.formulaManagerView = formulaManagerView;
    this.abstractionManager = abstractionManager;

    config.inject(this);

    if (config.getProperty("specification") == null) {
      throw new InvalidConfigurationException("No valid specification is given!");
    }
  }


  private static class VariableScopeProvider {
    private final CFA cfa;
    private final Multimap<String, String> declaredInFunction = HashMultimap.create();

    public VariableScopeProvider(CFA pCfa) {
      this.cfa = pCfa;
    }

    private void determinScopes() {
      declaredInFunction.clear();
      CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), new CFAVisitor() {

        @Override
        public TraversalProcess visitNode(CFANode pNode) {
          return TraversalProcess.CONTINUE;
        }

        @Override
        public TraversalProcess visitEdge(CFAEdge pEdge) {
          Stack<CFAEdge> stack = new Stack<>();
          stack.add(pEdge);
          while (!stack.isEmpty()) {
            String function = pEdge.getPredecessor().getFunctionName();
            CFAEdge edge = stack.pop();
            if (edge instanceof MultiEdge) {
              stack.addAll(((MultiEdge) edge).getEdges());
            } else if (edge instanceof CDeclarationEdge) {
              CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
              if (!decl.isGlobal()) {
	              if (decl instanceof CFunctionDeclaration) {
	                CFunctionDeclaration fdecl = (CFunctionDeclaration) decl;
	                for (CParameterDeclaration param: fdecl.getParameters()) {
	                  declaredInFunction.put(function, param.getName());
	                }
	              } else if (decl instanceof CVariableDeclaration){
	                declaredInFunction.put(function, decl.getName());
	              }
              }
            }
          }
          return TraversalProcess.CONTINUE;
        }
      });
    }

    private boolean isDeclaredInFunction(String functionName, String variableName) {
      if (declaredInFunction.isEmpty()) {
        determinScopes();
      }

      return declaredInFunction.containsEntry(functionName, variableName);
    }

  }


  public ListMultimap<CFANode, AssumeEdge> getTargetLocationAssumes(final CFA cfa) {
    ListMultimap<CFANode, AssumeEdge> result = ArrayListMultimap.create();

    Collection<CFANode> targetNodes = getTargetNodesWithCPA(cfa);

    if (targetNodes.isEmpty()) {
      return result;
    }

    // backwards search to determine all relevant edges
    for (CFANode n : targetNodes) {

      Deque<Pair<CFANode, Integer>> queue = new ArrayDeque<>();
      queue.add(Pair.of(n, 0));
      Set<CFANode> visited = new HashSet<>();

      while (!queue.isEmpty()) {
        Pair<CFANode, Integer> v = queue.pop();

        for (CFAEdge e: CFAUtils.enteringEdges(v.getFirst())) {
          CFANode u = e.getPredecessor();

          boolean isAssumeEdge = (e instanceof AssumeEdge);
          if (isAssumeEdge) {
            AssumeEdge assume = (AssumeEdge) e;
            result.put(n, assume);
          }

          if (!visited.contains(u)) {
        	  if (v.getSecond() < maxBackscanPathAssumes) {
        		  int depthIncrease = isAssumeEdge ? 1 : 0;
        		  queue.add(Pair.of(u, v.getSecond() + depthIncrease));
        	  }
          }
        }

        visited.add(v.getFirst());
      }

    }

    return result;
  }

  private Collection<CFANode> getTargetNodesWithCPA(CFA cfa) {
    try {
      ReachedSetFactory lReachedSetFactory = new ReachedSetFactory(Configuration.defaultConfiguration(), logger);

      // create new configuration based on existing config but with default set of CPAs
      Configuration lConfig = Configuration.builder()
                                           .copyFrom(config)
                                           .setOption("output.disable", "true")
                                           .clearOption("cpa")
                                           .clearOption("cpas")
                                           .clearOption("CompositeCPA.cpas")
                                           .build();

      CPABuilder lBuilder = new CPABuilder(lConfig, logger, lReachedSetFactory);
      ConfigurableProgramAnalysis lCpas = lBuilder.buildCPAs(cfa);
      Algorithm lAlgorithm = new CPAAlgorithm(lCpas, logger, lConfig);
      ReachedSet lReached = lReachedSetFactory.create();
      lReached.add(lCpas.getInitialState(cfa.getMainFunction()), lCpas.getInitialPrecision(cfa.getMainFunction()));

      lAlgorithm.run(lReached);

      return from(lReached)
               .filter(IS_TARGET_STATE)
               .transform(EXTRACT_LOCATION)
               .toSet();

    } catch (CPAException | InvalidConfigurationException e) {
      logger.log(Level.WARNING, "Error during CFA reduction, using full CFA");
      logger.logDebugException(e);
    } catch (InterruptedException e) {
      // not handled.
    }
    return cfa.getAllNodes();
  }

  private List<String> getQualifiedVariablesOfAssume(AssumeEdge pAssume) throws CPATransferException {
    if (pAssume.getExpression() instanceof CExpression) {
      CExpression ce = (CExpression) pAssume.getExpression();
      List<String> result = ce.accept(referencedVariablesVisitor);
      return result;
    } else {
      throw new RuntimeException("Only C programming language supported!");
    }
  }

  public PredicatePrecision minePrecisionFromCfa(CFA pCfa) throws CPATransferException {
    logger.log(Level.INFO, "Mining precision from CFA...");

    Multimap<CFANode, AbstractionPredicate> localPredicates = ArrayListMultimap.create();
    Multimap<String, AbstractionPredicate> functionPredicates = ArrayListMultimap.create();
    Collection<AbstractionPredicate> globalPredicates = Lists.newArrayList();

    VariableScopeProvider scopeProvider = new VariableScopeProvider(pCfa);

    ListMultimap<CFANode, AssumeEdge> locAssumes = getTargetLocationAssumes(pCfa);

    for (CFANode targetLocation : locAssumes.keySet()) {
      for (AssumeEdge assume : locAssumes.get(targetLocation)) {
        PathFormula relevantAssumesFormula = pathFormulaManager.makeFormulaForPath(Lists.newArrayList((CFAEdge) assume));
        BooleanFormula assumeFormula = formulaManagerView.uninstantiate(relevantAssumesFormula.getFormula());

        String function = assume.getPredecessor().getFunctionName();
        AbstractionPredicate predicate = abstractionManager.makePredicate(assumeFormula);

        boolean applyGlobal = true;
        if (applyScoped) {
          for (String var : getQualifiedVariablesOfAssume(assume)) {
            logger.log(Level.FINE, "Checking scope of ", function, var);
            if (scopeProvider.isDeclaredInFunction(function, var)) {
              // Apply the predicate in function scope
              // as soon one of the variable the assumption talks about is local.
              applyGlobal = false;
              logger.log(Level.FINE, "Local scoped variable mined", function, var);
              break;
            }
          }

          if (!applyGlobal) {
            functionPredicates.put(function, predicate);
          }
        }

        if (applyGlobal) {
          logger.log(Level.FINE, "Global predicate mined", predicate);
          globalPredicates.add(predicate);
        }

        logger.log(Level.FINE, "Mining result", "Function:", function, "Predicate:", predicate);
      }
    }

    logger.log(Level.INFO, "Mining finished.");

    return new PredicatePrecision(
        ImmutableSetMultimap.<Pair<CFANode,Integer>, AbstractionPredicate>of(),
        localPredicates, functionPredicates, globalPredicates);
  }

}
