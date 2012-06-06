/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ldd;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.BasicType;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.ldd.LDDRegionManager;

public class LDDAbstractionCPA implements ConfigurableProgramAnalysis {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LDDAbstractionCPA.class);
  }

  private final LDDAbstractDomain domain;

  private final StopOperator stopOperator;

  private final LDDAbstractionTransferRelation transferRelation;

  private final LDDRegionManager regionManager;

  private final LDDAbstractState initialState;

  public LDDAbstractionCPA(CFA cfa, Configuration config, LogManager logger) throws InvalidConfigurationException {
    Map<String, Integer> variables = new HashMap<String, Integer>();

    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        if (edge instanceof DeclarationEdge) {
          DeclarationEdge declarationEdge = (DeclarationEdge) edge;
          IASTDeclaration declaration = declarationEdge.getDeclaration();
          if (declaration instanceof IASTVariableDeclaration) {
            String name = declaration.getName();
            IType type = declaration.getDeclSpecifier();
            registerVariable(name, type, variables);
          } else if (declaration instanceof IASTFunctionDeclaration) {
            IASTFunctionDeclaration funDecl = (IASTFunctionDeclaration) declaration;
            for (IASTParameterDeclaration paramDecl : funDecl.getDeclSpecifier().getParameters()) {
              String name = paramDecl.getName();
              IType type = paramDecl.getDeclSpecifier();
              registerVariable(name, type, variables);
            }
          }
        }
      }
    }
    for (CFAFunctionDefinitionNode node : cfa.getAllFunctionHeads()) {
      if (node instanceof FunctionDefinitionNode) {
        FunctionDefinitionNode fDefNode = (FunctionDefinitionNode) node;
        for (IASTParameterDeclaration paramDecl : fDefNode.getFunctionDefinition().getDeclSpecifier().getParameters()) {
          String name = paramDecl.getName();
          IType type = paramDecl.getDeclSpecifier();
          registerVariable(name, type, variables);
        }
      }
    }
    this.regionManager = new LDDRegionManager(variables.size());
    this.domain = new LDDAbstractDomain(this.regionManager);
    this.stopOperator = new StopSepOperator(this.domain);
    this.initialState = new LDDAbstractState(this.regionManager.makeTrue());
    this.transferRelation = new LDDAbstractionTransferRelation(this.regionManager, variables);
  }

  private void registerVariable(String name, IType type, Map<String, Integer> variables) {
    if (name != null && !name.isEmpty() && type != null && type instanceof IASTSimpleDeclSpecifier) {
      BasicType basicType = ((IASTSimpleDeclSpecifier) type).getType();
      if (basicType == BasicType.INT) {
        variables.put(name, variables.size());
      }
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return this.domain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return this.transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return MergeSepOperator.getInstance();
  }

  @Override
  public StopOperator getStopOperator() {
    return this.stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode) {
    return this.initialState;
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode) {
    return SingletonPrecision.getInstance();
  }

}
