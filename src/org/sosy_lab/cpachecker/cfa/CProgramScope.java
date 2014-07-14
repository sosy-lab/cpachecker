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
package org.sosy_lab.cpachecker.cfa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

/**
 * Used to store the types of the cfa that are
 * lost when only a single or a block of statements
 * of the original program is parsed.
 */
public class CProgramScope implements Scope {

  private final Multimap<String, CDeclaration> simpleDeclarations;
  private Map<String, CDeclaration> qualifiedDeclarations;

  /**
   * Creates an object of this class.
   *
   * When a single or a block of statements is supposed to be parsed, first a cfa for
   * the whole program has to be parsed to generate complex types for the variables.
   * These types and declarations are stored in this scope.
   *
   * @param cfa the cfa of the program, where single or block of statements are supposed to be parsed
   */
  public CProgramScope(CFA cfa) {

    assert cfa.getLanguage() == Language.C;

    /* Get all nodes, get all edges from nodes, get all declarations from edges,
     * assign every declaration its name.
     */
    Collection<CFANode> nodes = cfa.getAllNodes();

    Function<CFANode, Iterable<? extends CDeclaration>> transformToCDecl =
        new Function<CFANode, Iterable<? extends CDeclaration>>() {

          @Override
          public Iterable<? extends CDeclaration> apply(CFANode node) {

            // TODO Assumption correct?
            if (node.getNumLeavingEdges() != 1) {
              // Cannot contain declaration edge
              return Collections.emptyList();
            }

            // Has only one leaving edge
            CFAEdge edge = node.getLeavingEdge(0);

            return apply(edge);
          }

          private Collection<? extends CDeclaration> apply(CFAEdge pEdge) {

            if (pEdge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
              CDeclaration dcl = ((CDeclarationEdge) pEdge).getDeclaration();
              return Collections.singleton(dcl);
            }

            if (pEdge.getEdgeType() == CFAEdgeType.MultiEdge) {
              MultiEdge edge = (MultiEdge) pEdge;
              Collection<CDeclaration> result = new ArrayList<>();

              for (CFAEdge innerEdge : edge.getEdges()) {
                result.addAll(apply(innerEdge));
              }

              return result;
            }

            return Collections.emptySet();
          }
        };

    FluentIterable<CDeclaration> dcls =
        FluentIterable.from(nodes).transformAndConcat(transformToCDecl);

    Function<? super CDeclaration, String> transformToMultiMap = new Function<CDeclaration, String>() {

      @Override
      public String apply(CDeclaration dcl) {
        return dcl.getName();
      }
    };

    simpleDeclarations = dcls.index(transformToMultiMap);

    Function<? super CDeclaration, String> transformToMap = new Function<CDeclaration, String>() {

      @Override
      public String apply(CDeclaration dcl) {
        return dcl.getQualifiedName();
      }
    };

    qualifiedDeclarations = dcls.uniqueIndex(transformToMap);
  }

  /**
   * Returns an empty program scope.
   */
  private CProgramScope() {
    qualifiedDeclarations = Collections.emptyMap();
    simpleDeclarations = ImmutableListMultimap.of();
  }

  private boolean hasUniqueDeclarationForSimpleName(String simpleName) {

    return simpleDeclarations.get(simpleName).size() == 1;
  }

  private CDeclaration getUniqueDeclarationForSimpleName(String simpleName) {
    Collection<CDeclaration> dcls = simpleDeclarations.get(simpleName);

    Preconditions.checkArgument(dcls.size() == 1, "Simple name not unique.");

    return dcls.iterator().next();
  }

  @SuppressWarnings("unused")
  private boolean contains(String qualifiedName) {
    return qualifiedDeclarations.containsKey(qualifiedName);
  }

  @SuppressWarnings("unused")
  private CDeclaration getDeclaration(String qualifiedName) {

    if (!qualifiedDeclarations.containsKey(qualifiedName)) {
      throw new AssertionError("Qualified name not in Scope.");
    }

    return qualifiedDeclarations.get(qualifiedName);
  }

  public static CProgramScope empty() {
    return new CProgramScope();
  }

  @Override
  public boolean isGlobalScope() {
    return false;
  }

  @Override
  public boolean variableNameInUse(String pName, String pOrigName) {
    //TODO qualified names
    return hasUniqueDeclarationForSimpleName(pName);
  }

  @Override
  public CSimpleDeclaration lookupVariable(String pName) {

    //TODO qualified names
    if(hasUniqueDeclarationForSimpleName(pName)) {
      return getUniqueDeclarationForSimpleName(pName);
    }

    return null;
  }

  @Override
  public CFunctionDeclaration lookupFunction(String pName) {

    //TODO qualified names
    if (hasUniqueDeclarationForSimpleName(pName)) {

      CDeclaration dcl = getUniqueDeclarationForSimpleName(pName);

      if (dcl instanceof CFunctionDeclaration) {
        return (CFunctionDeclaration) dcl;
      }
    }

    return null;
  }

  @Override
  public CComplexType lookupType(String pName) {
    // TODO not implemented
    return null;
  }

  @Override
  public CType lookupTypedef(String pName) {
    // TODO not implemented
    return null;
  }

  @Override
  public void registerDeclaration(CSimpleDeclaration pDeclaration) {
    // TODO not implemented

  }

  @Override
  public boolean registerTypeDeclaration(CComplexTypeDeclaration pDeclaration) {
    // TODO not implemented
    return false;
  }

  @Override
  public String createScopedNameOf(String pName) {
    // TODO not implemented
    return null;
  }

  @Override
  public String getRenamedTypeName(String pType) {
    // TODO not implemented
    return null;
  }
}