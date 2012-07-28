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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.java;

import java.util.logging.Level;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.sosy_lab.common.LogManager;


/**
 * A Visitor of eclipse jdt core dom AST that checks if there Errors in
 * the AST (from syntax errors for example)
 */
class AstErrorChecker extends ASTVisitor {

  private final LogManager logger;

  public AstErrorChecker(LogManager logger){
    this.logger = logger;
  }

  @Override
  public void preVisit(ASTNode problem) {

    //System.out.println(problem.getNodeType());
    //System.out.println(problem);
    //System.out.println();

    // flags return the bitwise or of value Recovered = 8 , Malformed = 1
    if(ASTNode.RECOVERED == (problem.getFlags() & ASTNode.RECOVERED) || ASTNode.MALFORMED == (problem.getFlags() & ASTNode.MALFORMED )){
      logger.log(Level.SEVERE, "Syntax Error");

    }
  }



}
