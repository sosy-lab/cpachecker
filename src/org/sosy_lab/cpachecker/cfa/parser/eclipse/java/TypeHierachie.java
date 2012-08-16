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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JInterfaceType;



public class TypeHierachie {



  private static final boolean SUCCESSFUL = true;

  private static final boolean UNSUCCESSFUL = false;

  private final Map< String ,TypeHierachieNode> types = new HashMap<String, TypeHierachieNode>();

  private final LogManager logger;

  public TypeHierachie(LogManager pLogger) {
    logger = pLogger;
  }



  public void add(JInterfaceType pType, List<JClassType> pKnownInterfaceImplementingClasses, List<JInterfaceType> pSubInterfaces , List<JInterfaceType> pExtendedInterfaces)  {


    add(pType , pExtendedInterfaces);

    TypeHierachieInterfaceNode node =  (TypeHierachieInterfaceNode) types.get(pType.getName());


    for(JClassType subClass : pKnownInterfaceImplementingClasses){

     if(!types.containsKey(subClass.getName())){
       add(subClass);
     }

    TypeHierachieClassNode subClassNode = (TypeHierachieClassNode) types.get(subClass.getName());
     node.registerChild(subClassNode);
     subClassNode.registerParent(node);

    }

    for(JInterfaceType subInterface : pSubInterfaces){

      if(!types.containsKey(subInterface.getName())){
        add(subInterface);
      }

     TypeHierachieInterfaceNode subInterfaceNode = (TypeHierachieInterfaceNode) types.get(subInterface.getName());
      node.registerChild(subInterfaceNode);
      subInterfaceNode.registerParent(node);
     }





    /*



    for(JClassType subClass : pKnownInterfaceImplementingClasses){
      if(subClass != null){
        if(types.containsKey(subClass.getName())) {
          knownInterfaceImplementingClasses.add((TypeHierachieClassNode) types.get(subClass.getName()));
           types.get(subClass.getName()).registerParent(this);
        } else {
          List<JInterfaceType> subClassImplementedInterfaces = new ArrayList<JInterfaceType>();
          subClassImplementedInterfaces.add(pType);
          knownInterfaceImplementingClasses.add(new TypeHierachieClassNode(subClass,null, null, subClassImplementedInterfaces));
        }
      }
    }

    for(JInterfaceType subInterfaceType : pSubInterfaces){

      if(subInterfaceType != null) {
        if(types.containsKey(subInterfaceType.getName())) {
          directSubInterfaces.add((TypeHierachieInterfaceNode) types.get(subInterfaceType.getName()));
           types.get(subInterfaceType.getName()).registerParent(this);
        } else {

          List<JInterfaceType> subExtendedInterfaces = new ArrayList<JInterfaceType>();
          subExtendedInterfaces.add(pType);

          directSubInterfaces.add(new TypeHierachieInterfaceNode(subInterfaceType, null, null , subExtendedInterfaces));
        }
      }
    }

    */






  }

  public void add(JInterfaceType pType, List<JInterfaceType> pExtendedInterfaces)  {

    assert pExtendedInterfaces != null;

    if(!types.containsKey(pType.getName())){
      add(pType);
    }

    TypeHierachieInterfaceNode node =  (TypeHierachieInterfaceNode) types.get(pType.getName());


    for(JInterfaceType extendedInterfaces : pExtendedInterfaces){

     if(!types.containsKey(extendedInterfaces.getName())){
       add(extendedInterfaces);
     }

    TypeHierachieInterfaceNode extendedInterfaceNode = (TypeHierachieInterfaceNode) types.get(extendedInterfaces.getName());

     node.registerParent(extendedInterfaceNode);
     extendedInterfaceNode.registerChild(node);

    }


    /*
    for(JInterfaceType extendedType : pExtendedInterfaces){

      if(extendedType != null) {
        if(types.containsKey(extendedType.getName())) {
          extendedInterfaces.add((TypeHierachieInterfaceNode) types.get(extendedType.getName()));
           types.get(extendedType.getName()).registerChild(this);
        } else {

          List<JInterfaceType> extendedSubInterfaces = new ArrayList<JInterfaceType>();
          extendedSubInterfaces.add(pType);
          extendedInterfaces.add(new TypeHierachieInterfaceNode(extendedType, null, extendedSubInterfaces , null));
        }
      }
    }
    */

  }

  public boolean add(JInterfaceType pType)  {

    if(!types.containsKey(pType.getName())){
      types.put(pType.getName(), new TypeHierachieInterfaceNode(pType));
      return SUCCESSFUL;
    } else {
      return UNSUCCESSFUL;
    }
  }

  public void add(JClassType pType, JClassType pParentClass, List<JClassType> pDirectSubClasses, List<JInterfaceType> pImplementedInterfaces) {

      add(pType, pParentClass, pImplementedInterfaces);

      TypeHierachieClassNode node =  (TypeHierachieClassNode) types.get(pType.getName());


      for(JClassType subClasses : pDirectSubClasses){

       if(!types.containsKey(subClasses.getName())){
         add(subClasses);
       }

      TypeHierachieClassNode subClassNode = (TypeHierachieClassNode) types.get(subClasses.getName());
       node.registerChild(subClassNode);
       subClassNode.registerParent(node);

      }
      /*

      for(JClassType subClass : pDirectSubClasses){

        if(subClass != null){
          if(types.containsKey(subClass.getName())) {
             directSubClasses.add((TypeHierachieClassNode) types.get(subClass.getName()));
             types.get(subClass.getName()).registerParent(this);
          } else {
            JClassType subParentClass = pType;
            directSubClasses.add(new TypeHierachieClassNode(subClass, subParentClass, null, null));
          }
        }
      }


    */
   }


  public void add(JClassType pType, JClassType pParentClass ,List<JInterfaceType> pImplementedInterfaces) {
    add(pType , pParentClass);
    add(pType, pImplementedInterfaces);
  }


  public void add(JClassType pType, List<JInterfaceType> pImplementedInterfaces) {


    assert pImplementedInterfaces != null;

    if(!types.containsKey(pType.getName())){
      add(pType);
    }

    TypeHierachieClassNode node =  (TypeHierachieClassNode) types.get(pType.getName());


    for(JInterfaceType implementedType : pImplementedInterfaces){

     if(!types.containsKey(implementedType.getName())){
       add(implementedType);
     }

    TypeHierachieInterfaceNode implementedInterfaceNode = (TypeHierachieInterfaceNode) types.get(implementedType.getName());

     node.registerParent(implementedInterfaceNode);
     implementedInterfaceNode.registerChild(node);

    }

    /*
    for(JInterfaceType implementedType : pImplementedInterfaces){

      if(implementedType != null){
        if(types.containsKey(implementedType.getName())) {
           implementedInterfaces.add((TypeHierachieInterfaceNode) types.get(implementedType.getName()));
           types.get(implementedType.getName()).registerChild(this);
        } else {
          List<JClassType> classImplementsInterface = new ArrayList<JClassType>();
          classImplementsInterface.add(pType);

          implementedInterfaces.add(new TypeHierachieInterfaceNode(implementedType, classImplementsInterface, null, null));
        }
      }
    }

    */
  }

  public void add(JClassType pType, JClassType pParentClass) {

    assert pParentClass != null;

    if(!types.containsKey(pType.getName())){
     add(pType);
    }

    if(!types.containsKey(pParentClass.getName())){
      add(pParentClass);
    }

    TypeHierachieClassNode node =  (TypeHierachieClassNode) types.get(pType.getName());
    TypeHierachieClassNode parent = (TypeHierachieClassNode) types.get(pParentClass.getName());

    node.registerParent(parent);
    parent.registerChild(node);

    /*
    if(types.containsKey(pParentClass.getName())) {
      parentClass = (TypeHierachieClassNode) types.get(pParentClass.getName());
      types.get(pParentClass.getName()).registerChild(this);
    } else {
      List<JClassType> parentSubClass = new ArrayList<JClassType>();
      parentSubClass.add(pType);
      parentClass = new TypeHierachieClassNode(pParentClass, null, parentSubClass, null);
    }
    */

  }

  public boolean add(JClassType pType) {

    if(!types.containsKey(pType.getName())){
      types.put(pType.getName(), new TypeHierachieClassNode(pType));
      return SUCCESSFUL;
    } else {
      return UNSUCCESSFUL;
    }
  }




  public void clear(){
    types.clear();
  }

  public JClassType getClassType(String fullyQualifiedName){
    if(types.containsKey(fullyQualifiedName)
        && types.get(fullyQualifiedName) instanceof TypeHierachieClassNode){

      return (JClassType) types.get(fullyQualifiedName).getType();
    }
    return null;
  }

  public boolean containsClassType(String fullyQualifiedName){
    return types.containsKey(fullyQualifiedName) && types.get(fullyQualifiedName) instanceof TypeHierachieClassNode;
  }

  public boolean containsClassType(JClassType type) {
    return containsClassType(type.getName());
  }

  public JInterfaceType getInterfaceType(String fullyQualifiedName){
    if(types.containsKey(fullyQualifiedName)
        && types.get(fullyQualifiedName) instanceof TypeHierachieInterfaceNode){

      return (JInterfaceType) types.get(fullyQualifiedName).getType();
    }
    return null;

  }

  public boolean containsInterfaceType(String fullyQualifiedName){
    return types.containsKey(fullyQualifiedName) && types.get(fullyQualifiedName) instanceof TypeHierachieInterfaceNode;
  }

  public boolean containsInterfaceType(JInterfaceType type){
    return containsInterfaceType(type.getName());
  }


  public JClassOrInterfaceType getClassOrInterfaceType(String fullyQualifiedName){
      return types.get(fullyQualifiedName).getType();
  }

  public boolean containsClassOrInterfaceType(String fullyQualifiedName){
    return types.containsKey(fullyQualifiedName);
  }

  public boolean containsClassOrInterfaceType(JClassOrInterfaceType type){
    return containsClassOrInterfaceType(type.getName());
  }

  public List<JClassType> getAllSuperClasses(String fullyQualifiedName){
    if(containsClassType(fullyQualifiedName)){
     TypeHierachieClassNode node = (TypeHierachieClassNode) types.get(fullyQualifiedName);

     List<JClassType> result = new ArrayList<JClassType>();
     while(node.getParentClass() != null){
       result.add(node.getParentClass().getType() );
       node = node.getParentClass();
     }

     return result;
    }
    return null;
  }

  public List<JClassType> getAllSuperClasses(JClassType type){
    return getAllSuperClasses(type.getName());
  }

  public List<JInterfaceType> getAllImplementedInterfaces(String fullyQualifiedName){
    if(containsClassType(fullyQualifiedName)){
     TypeHierachieClassNode node = (TypeHierachieClassNode) types.get(fullyQualifiedName);

     List<JInterfaceType> result = new ArrayList<JInterfaceType>();
     Queue<Set<TypeHierachieInterfaceNode>> toBeAdded = new LinkedList<Set<TypeHierachieInterfaceNode>>();


     for( TypeHierachieInterfaceNode implementedInterface : node.getImplementedInterfaces()) {
       result.add(implementedInterface.getType());
       toBeAdded.add(implementedInterface.getExtendedInterfaces());
     }

     while(!toBeAdded.isEmpty()){
       for( TypeHierachieInterfaceNode implementedInterface : toBeAdded.poll()) {
         result.add(implementedInterface.getType());
         toBeAdded.add(implementedInterface.getExtendedInterfaces());
       }
     }

     return result;
    }
    return null;
  }

  public List<JInterfaceType> getAllImplementedInterfaces(JClassType type){
    return getAllImplementedInterfaces(type.getName());
  }

  public List<JClassOrInterfaceType> getAllSuperTypesOfClass(String fullyQualifiedName) {

    List<JClassOrInterfaceType> result = new LinkedList<JClassOrInterfaceType>();
    result.addAll(getAllSuperClasses(fullyQualifiedName));
    result.addAll(getAllImplementedInterfaces(fullyQualifiedName));
    return result;
  }

  public List<JClassOrInterfaceType> getAllSuperTypesOfClass(JClassType type) {
    return getAllSuperTypesOfClass(type.getName());
  }

  public List<JClassType> getAllSubTypesOfClass(String fullyQualifiedName){
    if(containsClassType(fullyQualifiedName)){
     TypeHierachieClassNode node = (TypeHierachieClassNode) types.get(fullyQualifiedName);

     List<JClassType> result = new LinkedList<JClassType>();
     Queue<Set<TypeHierachieClassNode>> toBeAdded = new LinkedList<Set<TypeHierachieClassNode>>();


     for( TypeHierachieClassNode subClasses : node.getDirectSubClasses()) {
       result.add(subClasses.getType());
       toBeAdded.add(subClasses.getDirectSubClasses());
     }

     while(!toBeAdded.isEmpty()){
       for( TypeHierachieClassNode subClasses : toBeAdded.poll()) {
         result.add(subClasses.getType());
         toBeAdded.add(subClasses.getDirectSubClasses());
       }
     }

     return result;
    }
    return null;
  }

  public List<JClassType> getAllSubTypesOfClass(JClassType type){
    return getAllSubTypesOfClass(type.getName());
  }

  public List<JClassType> getAllKnownImplementedClassesOfInterface(String fullyQualifiedName){
    if(containsInterfaceType(fullyQualifiedName)){
     TypeHierachieInterfaceNode node = (TypeHierachieInterfaceNode) types.get(fullyQualifiedName);

     List<JClassType> result = new LinkedList<JClassType>();
     Queue<Set<TypeHierachieClassNode>> toBeAdded = new LinkedList<Set<TypeHierachieClassNode>>();


     for( TypeHierachieClassNode subClasses : node.getKnownInterfaceImplementingClasses()) {
       result.add(subClasses.getType());
       toBeAdded.add(subClasses.getDirectSubClasses());
     }

     while(!toBeAdded.isEmpty()){
       for( TypeHierachieClassNode subClasses : toBeAdded.poll()) {
         result.add(subClasses.getType());
         toBeAdded.add(subClasses.getDirectSubClasses());
       }
     }

     return result;
    }
    return null;
  }

  public List<JClassType> getAllKnownImplementedClassesOfInterface(JInterfaceType type){
    return getAllKnownImplementedClassesOfInterface(type.getName());
  }

  public List<JInterfaceType> getAllSuperInterfacesOfInterface(String fullyQualifiedName){
    if(containsInterfaceType(fullyQualifiedName)){
     TypeHierachieInterfaceNode node = (TypeHierachieInterfaceNode) types.get(fullyQualifiedName);

     List<JInterfaceType> result = new ArrayList<JInterfaceType>();
     Queue<Set<TypeHierachieInterfaceNode>> toBeAdded = new LinkedList<Set<TypeHierachieInterfaceNode>>();


     for( TypeHierachieInterfaceNode superInterface : node.getExtendedInterfaces()) {
       result.add(superInterface.getType());
       toBeAdded.add(superInterface.getExtendedInterfaces());
     }

     while(!toBeAdded.isEmpty()){
       for( TypeHierachieInterfaceNode superInterface : toBeAdded.poll()) {
         result.add(superInterface.getType());
         toBeAdded.add(superInterface.getExtendedInterfaces());
       }
     }

     return result;
    }
    return null;
  }

  public List<JInterfaceType> getAllSuperInterfacesOfInterface(JInterfaceType type){
    return getAllSuperInterfacesOfInterface(type.getName());
  }

  public List<JInterfaceType> getAllSubInterfacesOfInterface(String fullyQualifiedName){
    if(containsInterfaceType(fullyQualifiedName)){
     TypeHierachieInterfaceNode node = (TypeHierachieInterfaceNode) types.get(fullyQualifiedName);

     List<JInterfaceType> result = new ArrayList<JInterfaceType>();
     Queue<Set<TypeHierachieInterfaceNode>> toBeAdded = new LinkedList<Set<TypeHierachieInterfaceNode>>();


     for( TypeHierachieInterfaceNode subInterface : node.getDirectSubInterfaces()) {
       result.add(subInterface.getType());
       toBeAdded.add(subInterface.getDirectSubInterfaces());
     }

     while(!toBeAdded.isEmpty()){
       for( TypeHierachieInterfaceNode subInterface : toBeAdded.poll()) {
         result.add(subInterface.getType());
         toBeAdded.add(subInterface.getDirectSubInterfaces());
       }
     }

     return result;
    }
    return null;
  }

  public List<JInterfaceType> getAllSubInterfacesOfInterface(JInterfaceType type){
    return getAllSubInterfacesOfInterface(type.getName());
  }

  public List<JClassOrInterfaceType> getAllSubTypesOfInterfaces(String fullyQualifiedName) {

    List<JClassOrInterfaceType> result = new LinkedList<JClassOrInterfaceType>();
    result.addAll(getAllKnownImplementedClassesOfInterface(fullyQualifiedName));
    result.addAll(getAllSubInterfacesOfInterface(fullyQualifiedName));
    return result;

  }

  public List<JClassOrInterfaceType> getAllSubTypesOfInterfaces(JInterfaceType type) {
    return getAllSubTypesOfInterfaces(type.getName());
  }

  public List<JClassOrInterfaceType> getAllSubTypesOfType(String fullyQualifiedName) {

    List<JClassOrInterfaceType> result = new LinkedList<JClassOrInterfaceType>();

    if(containsClassType(fullyQualifiedName)){
      result.addAll(getAllSubTypesOfClass(fullyQualifiedName));
      return result;
    }else if(containsInterfaceType(fullyQualifiedName)){
      result.addAll(getAllSubTypesOfInterfaces(fullyQualifiedName));
      return result;
    }else {
      return null;
    }
  }

  public List<JClassOrInterfaceType> getAllSubTypesOfType(JClassOrInterfaceType type) {
    return getAllSubTypesOfType(type.getName());
  }

  public List<JClassOrInterfaceType> getAllSuperTypesOfType(String fullyQualifiedName) {

    List<JClassOrInterfaceType> result = new LinkedList<JClassOrInterfaceType>();

    if(containsClassType(fullyQualifiedName)){
      result.addAll(getAllSuperTypesOfClass(fullyQualifiedName));

      return result;
    }else if(containsInterfaceType(fullyQualifiedName)){
      result.addAll(getAllSuperInterfacesOfInterface(fullyQualifiedName));
      return result;
    }else {
      return null;
    }
  }

  public List<JClassOrInterfaceType> getAllSuperTypesOfType(JClassOrInterfaceType type) {
    return getAllSubTypesOfType(type.getName());
  }

  private abstract class TypeHierachieNode {

    private final JClassOrInterfaceType type;

    public TypeHierachieNode(JClassOrInterfaceType pType) {
      type = pType;
    }


    public JClassOrInterfaceType getType() {
      return type;
    }

    protected abstract void registerParent(TypeHierachieNode parent);

    protected abstract void registerChild(TypeHierachieNode child);

    @Override
    public boolean equals(Object pObj) {

      return pObj instanceof TypeHierachieNode && type.equals(((TypeHierachieNode)pObj).getType());
    }


    @Override
    public int hashCode() {
      // TODO Auto-generated method stub
      return type.hashCode();
    }

  }

  private final class TypeHierachieClassNode extends TypeHierachieNode{


    private  TypeHierachieClassNode parentClass;
    private final  Set<TypeHierachieClassNode> directSubClasses = new HashSet<TypeHierachieClassNode>();
    private final  Set<TypeHierachieInterfaceNode> implementedInterfaces = new HashSet<TypeHierachieInterfaceNode>();

    public TypeHierachieClassNode(JClassType pType) {
      super(pType);
    }



    @Override
    public JClassType getType() {
      return (JClassType) super.getType();
    }

    public TypeHierachieClassNode getParentClass() {
      return parentClass;
    }


    public Set<TypeHierachieClassNode> getDirectSubClasses() {
       return directSubClasses;
    }

    public Set<TypeHierachieInterfaceNode> getImplementedInterfaces() {
      return implementedInterfaces;
    }


    @Override
    public void registerParent(TypeHierachieNode parent) {
      if(parent instanceof TypeHierachieInterfaceNode){
        implementedInterfaces.add((TypeHierachieInterfaceNode) parent);
      } else {
        parentClass = (TypeHierachieClassNode) parent;
      }
    }


    @Override
    public void registerChild(TypeHierachieNode pChild) {
      if(pChild instanceof TypeHierachieClassNode){
        directSubClasses.add( (TypeHierachieClassNode) pChild);
      } else {
        // TODO Exception for Class
      }

    }

  }

 private final class TypeHierachieInterfaceNode extends TypeHierachieNode{

    private final Set<TypeHierachieClassNode> knownInterfaceImplementingClasses = new HashSet<TypeHierachieClassNode>();
    private final Set<TypeHierachieInterfaceNode> extendedInterfaces = new HashSet<TypeHierachieInterfaceNode>();
    private final Set<TypeHierachieInterfaceNode> directSubInterfaces = new HashSet<TypeHierachieInterfaceNode>();


    public TypeHierachieInterfaceNode(JInterfaceType pType)  {
      super(pType);
    }


    @Override
    public JInterfaceType getType() {
      return (JInterfaceType) super.getType();
    }

    public Set<TypeHierachieClassNode> getKnownInterfaceImplementingClasses() {
        return knownInterfaceImplementingClasses;
    }


    public Set<TypeHierachieInterfaceNode> getExtendedInterfaces() {
      return extendedInterfaces;
    }


    public Set<TypeHierachieInterfaceNode> getDirectSubInterfaces() {
      return directSubInterfaces;
    }


    @Override
    protected void registerParent(TypeHierachieNode parent) {
      if(parent instanceof TypeHierachieInterfaceNode){
        extendedInterfaces.add((TypeHierachieInterfaceNode) parent);
      } else {
        //TODO Exception for false
      }

    }

    @Override
    protected void registerChild(TypeHierachieNode child) {
      if(child instanceof TypeHierachieInterfaceNode){
        directSubInterfaces.add((TypeHierachieInterfaceNode) child);
      } else {
        knownInterfaceImplementingClasses.add((TypeHierachieClassNode) child);
      }

    }
  }


}