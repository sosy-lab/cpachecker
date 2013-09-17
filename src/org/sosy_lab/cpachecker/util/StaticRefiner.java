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
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
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
import org.sosy_lab.cpachecker.util.CFATraversal.DefaultCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="staticRefiner")
abstract public class StaticRefiner {

  @Option(description="collect at most this number of assumes along a path, backwards from each target (= error) location")
  private int maxBackscanPathAssumes = 1;

  private final Configuration config;
  private final CFA cfa;
  private final VariableScopeProvider scope;
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
      CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), new DefaultCFAVisitor() {

        @Override
        public TraversalProcess visitEdge(CFAEdge pEdge) {
          if (pEdge instanceof MultiEdge) {
            for (CFAEdge edge : ((MultiEdge)pEdge).getEdges()) {
              visitEdge(edge);
            }

          } else {
            String function = pEdge.getPredecessor().getFunctionName();

            if (pEdge instanceof CDeclarationEdge) {
              CDeclaration decl = ((CDeclarationEdge) pEdge).getDeclaration();
              if (!decl.isGlobal()) {
                if (decl instanceof CFunctionDeclaration) {
                  CFunctionDeclaration fdecl = (CFunctionDeclaration) decl;
                  for (CParameterDeclaration param: fdecl.getParameters()) {
                    declaredInFunction.put(function, param.getName());
                  }

                } else if (decl instanceof CVariableDeclaration) {
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

  protected Set<String> getQualifiedVariablesOfAssume(AssumeEdge pAssume) throws CPATransferException {
    if (pAssume.getExpression() instanceof CExpression) {
      CExpression ce = (CExpression) pAssume.getExpression();
      CollectVariablesVisitor referencedVariablesVisitor = new CollectVariablesVisitor();
      ce.accept(referencedVariablesVisitor);
      return referencedVariablesVisitor.referencedVariables;
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
    // TODO Why do we analyze the whole CFA here?
    // Wouldn't it make more sense to just look at the current refinment's
    // target state?
    // (Possibly doing one static refinement per target state instead of only once.)

    ListMultimap<CFANode, AssumeEdge> result  = ArrayListMultimap.create();
    Collection<CFANode> targetNodes           = getTargetNodesWithCPA();
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
  private Collection<CFANode> getTargetNodesWithCPA() {
    try {
      ReachedSetFactory lReachedSetFactory = new ReachedSetFactory(Configuration.defaultConfiguration(), logger);

      // create new configuration based on the existing one, but with a few options reset
      Configuration lConfig = Configuration.builder().copyFrom(config)
        .setOption("output.disable", "true")
        .clearOption("cpa")
        .clearOption("cpas")
        .clearOption("CompositeCPA.cpas")
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
      logger.log(Level.WARNING, "Cannot find target locations of the CFA.");
      logger.logDebugException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // TODO This is probably not a good idea.
    // Instead we should just fail and do the normal refinement.
    return cfa.getAllNodes();
  }

  // TODO return a set of CIdExpression as soon as the issue with CFieldReferences is resolved
  // Then we can use CIdExpression.getDeclaration()
  // and the whole VariableScopeProvider becomes useless.
  private static class CollectVariablesVisitor extends DefaultCExpressionVisitor<Void, RuntimeException> {

    private final Set<String> referencedVariables = new HashSet<>();

    @Override
    protected Void visitDefault(CExpression pExp) {
      return null;
    }

    @Override
    public Void visit(CIdExpression pIastIdExpression) {
      referencedVariables.add(pIastIdExpression.getName());
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
      // TODO: Why not visit the operands here?
      return null;
    }

    @Override
    public Void visit(org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression pIastBinaryExpression) {
      pIastBinaryExpression.getOperand1().accept(this);
      pIastBinaryExpression.getOperand2().accept(this);
      return null;
    }

    @Override
    public Void visit(CCastExpression pIastCastExpression) {
      pIastCastExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pIastCastExpression) {
      pIastCastExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CFieldReference pIastFieldReference) {
      // TODO: Why add the field name here? This is not a variable name.
      // TODO: Why not visit the field owner expression here?
      referencedVariables.add(pIastFieldReference.getFieldName());
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pIastUnaryExpression) {
      pIastUnaryExpression.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CPointerExpression pIastUnaryExpression) {
      pIastUnaryExpression.getOperand().accept(this);
      return null;
    }
  }
}
