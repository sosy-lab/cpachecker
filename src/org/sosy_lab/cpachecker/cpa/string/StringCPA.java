// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNode;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.string.utils.StringCpaUtilMethods;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.string")
public class StringCPA extends AbstractCPA {

  @Option(
    secure = true,
    name = "merge",
    toUppercase = true,
    values = {"SEP", "JOIN"},
    description = "which merge operator to use for StringCPA")
  private String mergeType = "SEP";

  @Option(
    secure = true,
    name = "stop",
    toUppercase = true,
    values = {"SEP", "JOIN"},
    description = "which stop operator to use for StringCPA")
  private String stopType = "SEP";

  private final Configuration config;
  private final StringOptions options;
  private final LogManager logger;
  private final CFA cfa;
  private final HashMap<MemoryLocation, JReferencedMethodInvocationExpression> temporaryVars;

  private StringCPA(Configuration pConfig, LogManager pLogger, CFA pCfa)
      throws InvalidConfigurationException {
    super(DelegateAbstractDomain.<StringState>getInstance(), null);
    this.config = pConfig;
    this.logger = pLogger;
    this.cfa = pCfa;
    Pair<HashMap<MemoryLocation, JReferencedMethodInvocationExpression>, ImmutableSet<String>> tempPair =
        getAllStringLiterals();
    temporaryVars = tempPair.getFirst();
    options = new StringOptions(pConfig, tempPair.getSecond());
    config.inject(this, StringCPA.class);
    getMergeOperator();
    getStopOperator();
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(StringCPA.class);
  }

  @Override
  public StringTransferRelation getTransferRelation() {
    return new StringTransferRelation(logger, options, temporaryVars);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator(mergeType);
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator(stopType);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new StringState(ImmutableMap.of(), options);
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return DelegateAbstractDomain.<StringState>getInstance();
  }

  private Pair<HashMap<MemoryLocation, JReferencedMethodInvocationExpression>, ImmutableSet<String>>
      getAllStringLiterals() {
    ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
    EdgeCollectingCFAVisitor edgeVisitor = new CFATraversal.EdgeCollectingCFAVisitor();
    HashMap<MemoryLocation, JReferencedMethodInvocationExpression> temporaryVariableMap =
        new HashMap<>();
    CFATraversal.dfs().traverseOnce(cfa.getMainFunction(), edgeVisitor);
    for (CFAEdge edge : edgeVisitor.getVisitedEdges()) {
      Optional<AAstNode> optNode = edge.getRawAST();
      if (optNode.isPresent()) {
        AAstNode aNode = optNode.get();
        FluentIterable<JAstNode> iterator = CFAUtils.traverseRecursively((JAstNode) aNode);
        for (JAstNode jNode : iterator) {
          if (jNode instanceof JStringLiteralExpression) {
            builder.add(((JStringLiteralExpression) jNode).getValue());
          } else if (jNode instanceof JMethodInvocationAssignmentStatement) {
            addTemporaryVariableToMap(
                (JMethodInvocationAssignmentStatement) jNode,
                temporaryVariableMap);
          }
        }
      }
    }
    return Pair.of(temporaryVariableMap, builder.build());
  }

  /*
   * We can't create an AST for the Methods in the Java Standard Library (that feature is not
   * implemented). Thus we have to use a Workaround: store all temporary variables, if their
   * referenced variable is a string. Then analyze that if possible. TODO: handle methods on strings
   * properly
   */
  private void addTemporaryVariableToMap(
      JMethodInvocationAssignmentStatement jmias,
      HashMap<MemoryLocation, JReferencedMethodInvocationExpression> pTemporaryVariableMap) {
    JMethodInvocationExpression jRight = jmias.getRightHandSide();
    JLeftHandSide jLeft = jmias.getLeftHandSide();
    MemoryLocation tempVarName = null;
    if (jLeft instanceof JIdExpression) {
      if (StringCpaUtilMethods.isTemporaryVariable((JIdExpression) jLeft)) {
        JSimpleDeclaration jsimp = ((JIdExpression) jLeft).getDeclaration();
        tempVarName = MemoryLocation.forDeclaration(jsimp);
      } else {
        return;
      }
    } else if (jLeft instanceof JDeclaration) {
      if (StringCpaUtilMethods.isTemporaryVariable((JDeclaration) jLeft)) {
        tempVarName = MemoryLocation.forDeclaration((JDeclaration) jLeft);
      } else {
        return;
      }
    }
    if (jRight instanceof JReferencedMethodInvocationExpression) {
      JReferencedMethodInvocationExpression jrmie = (JReferencedMethodInvocationExpression) jRight;
      pTemporaryVariableMap.put(tempVarName, jrmie);
    }
  }
}
