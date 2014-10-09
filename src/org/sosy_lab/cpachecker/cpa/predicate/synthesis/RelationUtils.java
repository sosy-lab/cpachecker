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
package org.sosy_lab.cpachecker.cpa.predicate.synthesis;

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.predicate.synthesis.CExpressionInliner.SubstitutionProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.base.Function;
import com.google.common.base.Optional;


public enum RelationUtils {
  SINGLETON;

  public static boolean includes(CExpression findInside, Set<CIdExpression> findAnyOf) {
    CExpressionScout scout = new CExpressionScout(findAnyOf);
    return findInside.accept(scout);
  }

  @SuppressWarnings("unchecked")
  public static <R extends CExpression> R uninstanciate(R pExpr) {

    final Function<String, String> removeSsaIndex = new Function<String, String>() {
      @Override
      public String apply(String pArg0) {
        return removeSsaIndex(pArg0);
      }
    };

    CExpressionInliner inliner = new CExpressionInliner(new SubstitutionProvider() {
      @Override
      public Optional<CExpression> getSubstitutionFor(CIdExpression pLhs) {

        int separatorPos = pLhs.getName().indexOf("@");
        if (separatorPos == -1) {
          return Optional.absent();
        }

        CSimpleDeclaration oldDecl = pLhs.getDeclaration();
        CSimpleDeclaration newDecl = getRenamedDeclaration(oldDecl, removeSsaIndex);

        return Optional.of((CExpression) new CIdExpression(
            pLhs.getFileLocation(),
            pLhs.getExpressionType(),
            removeSsaIndex.apply(pLhs.getName()),
            newDecl));
      }
    });

    return (R) pExpr.accept(inliner).getSecond();
  }

  @SuppressWarnings("unchecked")
  public static <R extends CExpression> R instanciate(R pExpr, final SSAMap pSsaMap, final int ssaDelta) {

    CExpressionInliner inliner = new CExpressionInliner(new SubstitutionProvider() {
      @Override
      public Optional<CExpression> getSubstitutionFor(final CIdExpression pLhs) {

        if (pLhs.getName().contains("@")) {
          return Optional.absent();
        }

        final Function<String, String> addSsaIndex = new Function<String, String>() {
          @Override
          public String apply(String pArg0) {
            int varSsaIndex = pSsaMap.getIndex(pLhs.getName()) + ssaDelta;
            String ssaPostfix = "@" + varSsaIndex;
            return pArg0 + ssaPostfix;
          }
        };

        CSimpleDeclaration oldDecl = pLhs.getDeclaration();
        CSimpleDeclaration newDecl = getRenamedDeclaration(oldDecl, addSsaIndex);

        return Optional.of((CExpression) new CIdExpression(
            pLhs.getFileLocation(),
            pLhs.getExpressionType(),
            addSsaIndex.apply(pLhs.getName()),
            newDecl));
      }
    });

    return (R) pExpr.accept(inliner).getSecond();
  }

  public static CAssignment instanciateAssign(CAssignment pAssign, final SSAMap pSsaMap, AnalysisDirection direction) {
    final int lhsSsaDelta = direction == AnalysisDirection.BACKWARD ? 0 : +1;
    final int rhsSsaDelta = direction == AnalysisDirection.BACKWARD ? +1 : 0;

    if (pAssign instanceof CExpressionAssignmentStatement) {
      CExpressionAssignmentStatement assign = (CExpressionAssignmentStatement) pAssign;
      CLeftHandSide newLhs = instanciate(assign.getLeftHandSide(), pSsaMap, lhsSsaDelta);
      CExpression newRhs = instanciate(assign.getRightHandSide(), pSsaMap, rhsSsaDelta);

      return new CExpressionAssignmentStatement(
          pAssign.getFileLocation(),
          newLhs,
          newRhs);
    }

    return pAssign;
  }

  public static String removeSsaIndex(final String pInput) {
    int separatorPos = pInput.indexOf("@");
    if (separatorPos == -1) {
      return pInput;
    }

    return pInput.subSequence(0, separatorPos).toString();
  }

  public static CSimpleDeclaration getRenamedDeclaration(CSimpleDeclaration pOld, Function<String, String> pNameTransformer) {
    if (pOld instanceof CVariableDeclaration) {
      CVariableDeclaration old = (CVariableDeclaration) pOld;
      return new CVariableDeclaration(
          old.getFileLocation(),
          old.isGlobal(),
          old.getCStorageClass(),
          old.getType(),
          pNameTransformer.apply(old.getName()),
          old.getOrigName(),
          pNameTransformer.apply(old.getQualifiedName()),
          old.getInitializer());
    } else if (pOld instanceof CParameterDeclaration) {
      CParameterDeclaration old = (CParameterDeclaration) pOld;
      CParameterDeclaration n = new CParameterDeclaration(
          old.getFileLocation(),
          old.getType(),
          pNameTransformer.apply(old.getName()));
      n.setQualifiedName(old.getQualifiedName());
      return n;
    } else {
      return pOld;
    }
  }



}
