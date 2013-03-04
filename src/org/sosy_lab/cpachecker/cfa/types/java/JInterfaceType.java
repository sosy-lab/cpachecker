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

public class JInterfaceType extends JClassOrInterfaceType implements JReferenceType {

  private final Set<JClassType> interfaceImplementingClasses = new HashSet<>();
  private final Set<JInterfaceType> extendedInterfaces = new HashSet<>();
  private final Set<JInterfaceType> directSubInterfaces = new HashSet<>();



  public JInterfaceType(String fullyQualifiedpName  ,final VisibilityModifier pVisibility) {
    super(fullyQualifiedpName , pVisibility);
  }



  public Set<JClassType> getKnownInterfaceImplementingClasses() {
      return interfaceImplementingClasses;
  }


  public Set<JInterfaceType> getExtendedInterfaces() {
    return extendedInterfaces;
  }


  public Set<JInterfaceType> getDirectSubInterfaces() {
    return directSubInterfaces;
  }




  public void registerSuperType(JInterfaceType superType) {

    assert !extendedInterfaces.contains(superType);
    extendedInterfaces.add( superType);

  }


  public void registerSubType(JClassOrInterfaceType subType) {



    if (subType instanceof JInterfaceType){

      assert !directSubInterfaces.contains(subType);

      directSubInterfaces.add((JInterfaceType) subType);
    } else {

      assert !interfaceImplementingClasses.contains(subType);

      interfaceImplementingClasses.add((JClassType) subType);
    }

  }

  public List<JInterfaceType> getAllSubInterfacesOfInterface(){

     List<JInterfaceType> result = new ArrayList<>();
     Queue<Set<JInterfaceType>> toBeAdded = new LinkedList<>();


     for ( JInterfaceType subInterface : getDirectSubInterfaces()) {

       if (result.contains(subInterface)) {
         continue; //maybe Exception?
       }

       result.add(subInterface);
       toBeAdded.add(subInterface.getDirectSubInterfaces());
     }

     while (!toBeAdded.isEmpty()){
       for ( JInterfaceType subInterface : toBeAdded.poll()) {

         if (result.contains(subInterface)) {
           continue; //maybe Exception?
         }
         result.add(subInterface);
         toBeAdded.add(subInterface.getDirectSubInterfaces());
       }
     }

     return result;

  }

  public List<JInterfaceType> getAllSuperTypesOfInterface(){


     List<JInterfaceType> result = new ArrayList<>();
     Queue<Set<JInterfaceType>> toBeAdded = new LinkedList<>();


     for ( JInterfaceType superInterface : getExtendedInterfaces()) {


       if (result.contains(superInterface)) {
         continue; //maybe Exception?
       }

       result.add(superInterface);
       toBeAdded.add(superInterface.getExtendedInterfaces());
     }

     while (!toBeAdded.isEmpty()){
       for ( JInterfaceType superInterface : toBeAdded.poll()) {

         if (result.contains(superInterface)) {
           continue; //maybe Exception?
         }

         result.add(superInterface);
         toBeAdded.add(superInterface.getExtendedInterfaces());
       }
     }

     return result;

  }

  public List<JClassType>  getAllKnownImplementedClassesOfInterface(){

    List<JClassType> result = new LinkedList<>();
    List<JInterfaceType> subInterfaces = new LinkedList<>();


    result.addAll(this.getAllKnownDirectlyImplementedClassesOfInterface());

    for (JInterfaceType subInterface : subInterfaces) {

      result.addAll(subInterface.getAllKnownDirectlyImplementedClassesOfInterface());

    }

    subInterfaces.addAll(getAllSubInterfacesOfInterface());


    return result;

  }




  private List<JClassType> getAllKnownDirectlyImplementedClassesOfInterface(){


    List<JInterfaceType> subInterfaces = new LinkedList<>();

    subInterfaces.addAll(getAllSubInterfacesOfInterface());

     List<JClassType> result = new LinkedList<>();
     Queue<Set<JClassType>> toBeAdded = new LinkedList<>();

     for ( JClassType subClasses : getKnownInterfaceImplementingClasses()) {
       result.add(subClasses);
       toBeAdded.add(subClasses.getDirectSubClasses());
     }

     while (!toBeAdded.isEmpty()){
       for ( JClassType subClasses : toBeAdded.poll()) {
         result.add(subClasses);
         toBeAdded.add(subClasses.getDirectSubClasses());
       }
     }

     return result;

  }

  public List<JClassOrInterfaceType> getAllSubTypesOfInterfaces() {

    List<JClassOrInterfaceType> result = new LinkedList<>();
    result.addAll(getAllSubInterfacesOfInterface());
    result.addAll(getAllKnownImplementedClassesOfInterface());
    return result;

  }

}