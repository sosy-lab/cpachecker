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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;


public class TypeHierachyCreator extends ASTVisitor {

  private static final boolean VISIT_CHILDREN = true;

  private final LogManager logger;

  private final ASTConverter astCreator;

  private final TypeHierachie typeHierachie;



  public TypeHierachyCreator(LogManager pLogger , TypeHierachie pTypeHierachie) {
    logger = pLogger;
    astCreator = new ASTConverter(logger);
    typeHierachie = pTypeHierachie;

  }

  @Override
  public boolean visit(TypeDeclaration node) {

    ITypeBinding typeBinding = node.resolveBinding();

    if (typeBinding != null) {
      if(typeBinding.isClass()){

        JClassType type =  astCreator.convertClassType(typeBinding);


        boolean doesNotexist = typeHierachie.add(type);

        if (doesNotexist) {
          JClassType nextType = type;
          boolean finished = typeBinding.getSuperclass() == null;
          while (!finished) {
            JClassType parentType = astCreator.convertClassType(typeBinding.getSuperclass());
            //all Parents are already added if parent type already exists
            // just connect this to parent
            finished = typeHierachie.containsClassType(parentType);
            typeHierachie.add(nextType, parentType);


            ITypeBinding[] interfaces = typeBinding.getInterfaces();
            List<JInterfaceType> implementedInterfaces = new LinkedList<JInterfaceType>();

            for (ITypeBinding interfaceBinding : interfaces) {
              // It seems that you don't get only interfaces with getInterfaces.
              // TODO Investigate
              if (interfaceBinding.isInterface()) {
                JInterfaceType interfaceType = astCreator.convertInterfaceType(interfaceBinding);
                implementedInterfaces.add(interfaceType);
              }
            }

            typeHierachie.add(nextType, implementedInterfaces);

            typeBinding = typeBinding.getSuperclass();
            nextType = parentType;
            finished = finished || typeBinding.getSuperclass() == null;
          }

        }
      } else if(typeBinding.isInterface()) {

        JInterfaceType type = astCreator.convertInterfaceType(typeBinding);
        typeHierachie.add(type);
        Queue<Pair<JInterfaceType ,ITypeBinding[]>> next = new LinkedList<Pair<JInterfaceType ,ITypeBinding[]>>();
        next.add(Pair.of(type, typeBinding.getInterfaces()));


        while(!next.isEmpty()){

          ITypeBinding[] superInterfacesBinding = next.peek().getSecond();
          JInterfaceType nextType = next.poll().getFirst();
          List<JInterfaceType> superTypes = new LinkedList<JInterfaceType>();




            for(ITypeBinding binding : superInterfacesBinding) {
              JInterfaceType superInterface = astCreator.convertInterfaceType(binding);
              superTypes.add(superInterface);
              next.add( Pair.of(superInterface, binding.getInterfaces()));
            }


            typeHierachie.add(nextType, superTypes);
        }
      }
    }
    return VISIT_CHILDREN;
  }
}