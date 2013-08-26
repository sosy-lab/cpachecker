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
package org.sosy_lab.cpachecker.util;

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
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

@Options(prefix="staticRefiner")
abstract public class StaticRefiner {

  @Option(description="collect at most this number of assumes along a path, backwards from each target (= error) location")
  protected int maxBackscanPathAssumes = 1;

  private final CFA cfa;
  private final VariableScopeProvider scope;
  protected final Configuration config;
  protected final LogManager logger;

  public StaticRefiner(
      Configuration pConfig,
      LogManager pLogger,
      CFA pCfa) throws InvalidConfigurationException {
    this.logger = pLogger;
    this.config = pConfig;

    this.cfa    = pCfa;
    this.scope  = new VariableScopeProvider(cfa);

    pConfig.inject(this, StaticRefiner.class);

    if (pConfig.getProperty("specification") == null) {
      throw new InvalidConfigurationException("No valid specification is given!");
    }
  }

  /**
   * This method extracts a precision based only on static information derived from the CFA.
   *
   * @return a precision for the predicate CPA
   * @throws CPATransferException
   */
  abstract public Precision extractPrecisionFromCfa() throws CPATransferException;

  protected boolean isDeclaredInFunction(String function, String var) {
    return scope.isDeclaredInFunction(function, var);
  }

  private static class VariableScopeProvider {
    private final CFA cfa;
    private final Multimap<String, String> declaredInFunction = HashMultimap.create();

    public VariableScopeProvider(CFA pCfa) {
      this.cfa = pCfa;
    }

    private boolean isDeclaredInFunction(String functionName, String variableName) {
      if (declaredInFunction.isEmpty()) {
        determinScopes();
      }

      return declaredInFunction.containsEntry(functionName, variableName);
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
            }

            else if (edge instanceof CDeclarationEdge) {
              CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
              if (!decl.isGlobal()) {
                if (decl instanceof CFunctionDeclaration) {
                  CFunctionDeclaration fdecl = (CFunctionDeclaration) decl;
                  for (CParameterDeclaration param: fdecl.getParameters()) {
                    declaredInFunction.put(function, param.getName());
                  }
                }

                else if (decl instanceof CVariableDeclaration) {
                  declaredInFunction.put(function, decl.getName());
                }
              }
            }
          }
          return TraversalProcess.CONTINUE;
        }
      });
    }
  }

  protected List<String> getQualifiedVariablesOfAssume(AssumeEdge pAssume) throws CPATransferException {
    if (pAssume.getExpression() instanceof CExpression) {
      CExpression ce = (CExpression) pAssume.getExpression();
      List<String> result = ce.accept(referencedVariablesVisitor);
      return result;
    } else {
      throw new RuntimeException("Only C programming language supported!");
    }
  }

  /**
   * This method finds in a backwards search, starting from the target locations in the
   * CFA, the list of n assume edges preceeding each target node, where n equals the
   * maxBackscanPathAssumes option.
   *
   * @param cfa the CFA to work in
   * @return the mapping from target nodes to the corresponding preceeding assume edges
   */
  protected ListMultimap<CFANode, AssumeEdge> getTargetLocationAssumes() {
    ListMultimap<CFANode, AssumeEdge> result  = ArrayListMultimap.create();
    Collection<CFANode> targetNodes           = getTargetNodesWithCPA(cfa);
    if (targetNodes.isEmpty()) {
      return result;
    }

    // backwards search to determine all relevant edges
    for (CFANode targetNode : targetNodes) {
      Deque<Pair<CFANode, Integer>> queue = new ArrayDeque<>();
      queue.add(Pair.of(targetNode, 0));
      Set<CFANode> explored = new HashSet<>();

      while (!queue.isEmpty()) {
        // Take the next node that should be explored from the queue
        Pair<CFANode, Integer> v = queue.pop();

        // Each node that enters node v
        for (CFAEdge e: CFAUtils.enteringEdges(v.getFirst())) {
          CFANode u = e.getPredecessor();

          boolean isAssumeEdge = (e instanceof AssumeEdge);
          int depthIncrease = isAssumeEdge ? 1 : 0;

          if (isAssumeEdge) {
            AssumeEdge assume = (AssumeEdge) e;
            if (v.getSecond() < maxBackscanPathAssumes) {
              result.put(targetNode, assume);
            } else {
              continue;
            }
          }

          if (!explored.contains(u)) {
            queue.add(Pair.of(u, v.getSecond() + depthIncrease));
          }
        }

        explored.add(v.getFirst());
      }
    }

    return result;
  }

  /**
   * This method starts a simple CPA on the given CFA in order to find all target nodes
   * that are syntactical reachable in the CFA.
   *
   * @param cfa the CFA to operate on
   * @return the collection of target nodes
   */
  private Collection<CFANode> getTargetNodesWithCPA(CFA cfa) {
    try {
      ReachedSetFactory lReachedSetFactory = new ReachedSetFactory(Configuration.defaultConfiguration(), logger);

      // create new configuration based on the existing one, but with a few options reset
      Configuration lConfig = Configuration.builder().copyFrom(config)
        .setOption("output.disable", "true")
        .clearOption("cpa")
        .clearOption("cpas")
        .clearOption("CompositeCPA.cpas")
        .clearOption("cpas")
        .clearOption("cpa.composite.precAdjust")
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
      List<String> operand1List = pIastBinaryExpression.getOperand1().accept(this);
      List<String> operand2List = pIastBinaryExpression.getOperand2().accept(this);

      operand2List.addAll(operand1List);

      return operand2List;
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression pIastCastExpression) throws CPATransferException {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression pIastCastExpression) throws CPATransferException {
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
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression pIastLiteralExpression) throws CPATransferException {
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

    @Override
    public List<String> visit(org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression pIastUnaryExpression) throws CPATransferException {
      return pIastUnaryExpression.getOperand().accept(this);
    }
  };
}
