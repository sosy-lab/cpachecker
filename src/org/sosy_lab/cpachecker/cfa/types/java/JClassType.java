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
package org.sosy_lab.cpachecker.cfa.types.java;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.java.VisibilityModifier;


public class JClassType extends JClassOrInterfaceType implements JReferenceType {


  private final boolean isFinal;
  private final boolean isAbstract;
  private final boolean isStrictFp;

  private  JClassType superClass;
  private final  Set<JClassType> directSubClasses = new HashSet<JClassType>();
  private final  Set<JInterfaceType> implementedInterfaces = new HashSet<JInterfaceType>();


  public JClassType(String fullyQualifiedName  ,final VisibilityModifier pVisibility, final boolean pIsFinal,
      final boolean pIsAbstract, final boolean pStrictFp) {
    super(fullyQualifiedName, pVisibility);
    isFinal = pIsFinal;
    isAbstract = pIsAbstract;
    isStrictFp = pStrictFp;

     assert !isFinal || !isAbstract : "Classes can't be abstract and final";
     assert (getVisibility() != VisibilityModifier.PRIVATE) || (getVisibility() != VisibilityModifier.PROTECTED) : " Classes can't be private or protected";

  }

  public boolean isFinal() {
    return isFinal;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public boolean isStrictFp() {
    return isStrictFp;
  }

  public JClassType getParentClass() {
    return superClass;
  }


  public Set<JClassType> getDirectSubClasses() {
     return directSubClasses;
  }

  public Set<JInterfaceType> getImplementedInterfaces() {
    return implementedInterfaces;
  }


   public void registerSuperType(JClassOrInterfaceType superType) {

    if(superType instanceof JInterfaceType){

      assert !implementedInterfaces.contains(superType);

      implementedInterfaces.add((JInterfaceType) superType);

    } else {

      assert superClass == null;

      superClass = (JClassType) superType;

    }
  }



  public void registerSubType(JClassType pChild) {

      assert !directSubClasses.contains(pChild);
      directSubClasses.add( pChild);

  }

  public List<JClassType> getAllSuperClasses(){

     List<JClassType> result = new ArrayList<JClassType>();

     result.add(superClass);

     JClassType superSuperClass = superClass;

     while(superSuperClass.getParentClass() != null){

        superSuperClass = superSuperClass.getParentClass();

       //Termination Check (maybe Exception?)
       if(result.contains(superSuperClass)) {
         break;
       }

       result.add(superSuperClass.getParentClass());
       superSuperClass = superSuperClass.getParentClass();

     }

     return result;
    }

  public List<JInterfaceType> getAllImplementedInterfaces(){

    List<JClassType> superClasses = getAllSuperClasses();

    List<JInterfaceType> result = new LinkedList<JInterfaceType>();

    for(JClassType superClass : superClasses){
      result.addAll(superClass.getAllSuperInterfacesOfImplementedInterfacsOfClass());
    }

    return result;
  }


  private List<JInterfaceType> getAllSuperInterfacesOfImplementedInterfacsOfClass() {

     List<JInterfaceType> result = new ArrayList<JInterfaceType>();
     Queue<Set<JInterfaceType>> toBeAdded = new LinkedList<Set<JInterfaceType>>();

     for( JInterfaceType implementedInterface : this.getImplementedInterfaces()) {

       //Termination Check (maybe Exception?)
       if(result.contains(implementedInterface)) {
         continue;
       }

       result.add(implementedInterface);
       toBeAdded.add(implementedInterface.getExtendedInterfaces());
     }

     while(!toBeAdded.isEmpty()){

       for( JInterfaceType implementedInterface : toBeAdded.poll()) {

         //Termination Check (maybe Exception?)
         if(result.contains(implementedInterface)) {
           continue;
         }

         result.add(implementedInterface);
         toBeAdded.add(implementedInterface.getExtendedInterfaces());
       }

     }

     return result;
  }

  public List<JClassOrInterfaceType> getAllSuperTypesOfClass() {

    List<JClassOrInterfaceType> result = new LinkedList<JClassOrInterfaceType>();
    result.addAll(getAllSuperClasses());
    result.addAll(getAllImplementedInterfaces());
    return result;
  }

  public List<JClassType> getAllSubTypesOfClass(){


     List<JClassType> result = new LinkedList<JClassType>();
     Queue<Set<JClassType>> toBeAdded = new LinkedList<Set<JClassType>>();


     for( JClassType subClass : this.getDirectSubClasses()) {

       if(result.contains(subClass)) {
         continue; //maybe Exception?
       }

       result.add(subClass);
       toBeAdded.add(subClass.getDirectSubClasses());
     }

     while(!toBeAdded.isEmpty()){
       for( JClassType subClass : toBeAdded.poll()) {

         if(result.contains(subClass)) {
           continue; //maybe Exception?
         }

         result.add(subClass);
         toBeAdded.add(subClass.getDirectSubClasses());
       }
     }

     return result;
    }

}