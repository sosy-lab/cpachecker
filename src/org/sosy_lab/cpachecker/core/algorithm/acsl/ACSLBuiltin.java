/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.acsl;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public abstract class ACSLBuiltin implements ACSLTerm {

  public static class Result extends ACSLBuiltin {

    private final String functionName;

    public Result(String pFunctionName) {
      functionName = pFunctionName;
    }

    public String getFunctionName() {
      return functionName;
    }

    @Override
    public String toString() {
      return "\\result";
    }

    @Override
    public int hashCode() {
      return 3 * functionName.hashCode() + 3;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Result) {
        Result other = (Result) obj;
        return functionName.equals(other.functionName);
      }
      return false;
    }

    @Override
    public CExpression accept(ACSLTermToCExpressionVisitor visitor)
        throws UnrecognizedCodeException {
      return visitor.visit(this);
    }

    @Override
    public ACSLTerm useOldValues() {
      throw new UnsupportedOperationException(
          "\\old should not be used on term containing \\result");
    }

    @Override
    public boolean isAllowedIn(Class<?> clauseType) {
      return clauseType.equals(EnsuresClause.class);
    }

    @Override
    public Set<ACSLBuiltin> getUsedBuiltins() {
      return ImmutableSet.of(this);
    }
  }

  public static class Old extends ACSLBuiltin {

    private final Identifier inner;

    public Old(Identifier identifier) {
      inner = identifier;
    }

    public ACSLTerm getInner() {
      return inner;
    }

    @Override
    public String toString() {
      return "\\old(" + inner.toString() + ")";
    }

    @Override
    public int hashCode() {
      return 7 * inner.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Old) {
        Old other = (Old) obj;
        return inner.equals(other.getInner());
      }
      return false;
    }

    @Override
    public CExpression accept(ACSLTermToCExpressionVisitor visitor)
        throws UnrecognizedCodeException {
      return visitor.visit(this);
    }

    @Override
    public ACSLTerm useOldValues() {
      return this;
    }

    @Override
    public boolean isAllowedIn(Class<?> clauseType) {
      return clauseType.equals(EnsuresClause.class);
    }

    @Override
    public Set<ACSLBuiltin> getUsedBuiltins() {
      return ImmutableSet.of(this);
    }
  }
}
