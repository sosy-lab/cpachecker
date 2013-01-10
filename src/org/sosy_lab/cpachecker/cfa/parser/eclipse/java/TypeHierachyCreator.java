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
import java.util.Map;
import java.util.Queue;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.ASTConverter.ModifierBean;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;

/**
 * This Visitor Iterates the Compilation Unit for top-level Type Declarations,
 * converts them into types, and inserts them into a type Hierarchy.
 * Through bindings of the parser, every super type is converted and inserted
 * as well.
 */
public class TypeHierachyCreator extends ASTVisitor {

  private static final boolean VISIT_CHILDREN = true;

  private static final boolean SUCCESSFUL = true;

  private static final boolean UNSUCCESSFUL = false;

  @SuppressWarnings("unused")
  private final LogManager logger;


  private final Map< String ,JClassOrInterfaceType> types;
  private final Map<String, String> typeOfFiles;
  private  String fileOfCU;


/**
 * Creates the Visitor. The types are inserted in the parameter type.
 * The parameter typeOfFile stores the files a type was extracted from.
 *
 * @param pLogger Logger logging progress.
 * @param pTypes Resulting Types are inserted in this map.
 * @param pTypeOfFiles Maps types to the files they were extracted from.
 */
  public TypeHierachyCreator(LogManager pLogger, Map<String, JClassOrInterfaceType> pTypes, Map<String, String> pTypeOfFiles) {
    logger = pLogger;
    types = pTypes;
    typeOfFiles = pTypeOfFiles;
  }

  @Override
  public boolean visit(EnumDeclaration node){

    handleHierachy(node.resolveBinding());
    return VISIT_CHILDREN;
  }

  @Override
  public boolean visit(TypeDeclaration node) {

    ITypeBinding typeBinding = node.resolveBinding();

    handleHierachy(typeBinding);

    return VISIT_CHILDREN;
  }

  private void handleHierachy(ITypeBinding typeBinding) {
    if (typeBinding != null) {
      if(typeBinding.isClass() || typeBinding.isEnum()){

        JClassType type =  convertClassType(typeBinding);

         typeOfFiles.put(type.getName(), fileOfCU);

        boolean doesNotexist = add(type);

        if (doesNotexist) {
          JClassType nextType = type;
          boolean finished = typeBinding.getSuperclass() == null;
          while (!finished) {
            JClassType parentType = convertClassType(typeBinding.getSuperclass());
            //all Parents are already added if parent type already exists
            // just connect this to parent
            finished = types.containsValue(parentType);
            add(nextType, parentType);


            ITypeBinding[] interfaces = typeBinding.getInterfaces();
            List<JInterfaceType> implementedInterfaces = new LinkedList<>();

            for (ITypeBinding interfaceBinding : interfaces) {
              // It seems that you don't get only interfaces with getInterfaces.
              // TODO Investigate
              if (interfaceBinding.isInterface()) {
                JInterfaceType interfaceType = convertInterfaceType(interfaceBinding);
                implementedInterfaces.add(interfaceType);
              }
            }

            add(nextType, implementedInterfaces);

            typeBinding = typeBinding.getSuperclass();
            nextType = parentType;
            finished = finished || typeBinding.getSuperclass() == null;
          }

        }
      } else if(typeBinding.isInterface()) {

        JInterfaceType type = convertInterfaceType(typeBinding);
        add(type);

        typeOfFiles.put(type.getName(), fileOfCU);


        Queue<Pair<JInterfaceType ,ITypeBinding[]>> next = new LinkedList<>();
        next.add(Pair.of(type, typeBinding.getInterfaces()));


        while(!next.isEmpty()){

          ITypeBinding[] superInterfacesBinding = next.peek().getSecond();
          JInterfaceType nextType = next.poll().getFirst();
          List<JInterfaceType> superTypes = new LinkedList<>();

            for(ITypeBinding binding : superInterfacesBinding) {
              JInterfaceType superInterface = convertInterfaceType(binding);
              superTypes.add(superInterface);
              next.add( Pair.of(superInterface, binding.getInterfaces()));
            }

            add(nextType, superTypes);
        }
      }
    }
  }

  @SuppressWarnings("unused")
  private void add(JInterfaceType pType, List<JClassType> pKnownInterfaceImplementingClasses, List<JInterfaceType> pSubInterfaces , List<JInterfaceType> pExtendedInterfaces)  {


    add(pType , pExtendedInterfaces);

    for(JClassType subClass : pKnownInterfaceImplementingClasses){

     if(!types.containsKey(subClass.getName())){
       add(subClass);
     }

     pType.registerSubType(subClass);
     subClass.registerSuperType(pType);

    }

    for(JInterfaceType subInterface : pSubInterfaces){

      if(!types.containsKey(subInterface.getName())){
        add(subInterface);
      }

      pType.registerSubType(subInterface);
      subInterface.registerSuperType(pType);
     }

  }

  private void add(JInterfaceType pType, List<JInterfaceType> pExtendedInterfaces)  {

    assert pExtendedInterfaces != null;

    if(!types.containsKey(pType.getName())){
      add(pType);
    }


    for(JInterfaceType extendedInterfaces : pExtendedInterfaces){

     if(!types.containsKey(extendedInterfaces.getName())){
       add(extendedInterfaces);
     }


    pType.registerSuperType(extendedInterfaces);
    extendedInterfaces.registerSubType(pType);

    }

  }

  private boolean add(JInterfaceType pType)  {

    if(!types.containsKey(pType.getName())){
      types.put(pType.getName(),pType);
      return SUCCESSFUL;
    } else {
      return UNSUCCESSFUL;
    }

  }

  @SuppressWarnings("unused")
  private void add(JClassType pType, JClassType pParentClass, List<JClassType> pDirectSubClasses, List<JInterfaceType> pImplementedInterfaces) {

      add(pType, pParentClass, pImplementedInterfaces);

      for(JClassType subClass : pDirectSubClasses){

       if(!types.containsKey(subClass.getName())){
         add(subClass);
       }

       pType.registerSubType(subClass);
       subClass.registerSuperType(pType);

      }

   }


  private void add(JClassType pType, JClassType pParentClass ,List<JInterfaceType> pImplementedInterfaces) {
    add(pType , pParentClass);
    add(pType, pImplementedInterfaces);
  }


  private void add(JClassType pType, List<JInterfaceType> pImplementedInterfaces) {


    assert pImplementedInterfaces != null;

    if(!types.containsKey(pType.getName())){
      add(pType);
    }


    for(JInterfaceType implementedType : pImplementedInterfaces){

     if(!types.containsKey(implementedType.getName())){
       add(implementedType);
     }

     pType.registerSuperType(implementedType);
     implementedType.registerSubType(pType);

    }

  }

  private void add(JClassType pType, JClassType pParentClass) {

    assert pParentClass != null;

    if(!types.containsKey(pType.getName())){
     add(pType);
    }

    if(!types.containsKey(pParentClass.getName())){
      add(pParentClass);
    }

    pType.registerSuperType(pParentClass);
    pParentClass.registerSubType(pType);

  }

  private boolean add(JClassType pType) {

    if(!types.containsKey(pType.getName())){
      types.put(pType.getName(), pType);
      return SUCCESSFUL;
    } else {
      return UNSUCCESSFUL;
    }
  }

  private JClassType convertClassType(ITypeBinding t) {

    assert t.isClass() ||t.isEnum();

    String name = ASTConverter.getFullyQualifiedClassOrInterfaceName(t);

    if(types.containsKey(name)) {
      return (JClassType) types.get(name);
    }

    ModifierBean mB = ModifierBean.getModifiers(t);
    return new JClassType(name, mB.getVisibility(), mB.isFinal(), mB.isAbstract(), mB.isStrictFp());
   }

  private JInterfaceType convertInterfaceType(ITypeBinding t) {

    assert t.isInterface();

    String name = ASTConverter.getFullyQualifiedClassOrInterfaceName(t);

    if(types.containsKey(name)) {
      return (JInterfaceType) types.get(name);
    }

    ModifierBean mB = ModifierBean.getModifiers(t);
    return new JInterfaceType(ASTConverter.getFullyQualifiedClassOrInterfaceName(t), mB.getVisibility());

  }

  /**
   * Sets the File this Visitor visits at the moment.
   * Necessary to map types to files.
   *
   * @param fileOfCU the file the Compilation Unit was extracted from.
   */
  public void setFileOfCU(String fileOfCU) {
    this.fileOfCU = fileOfCU;
  }

}