/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking;

import java.io.Serializable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

/**
 * Internal Class for Representing a 2-Pair
 * @param <T> Type of the first element
 * @param <E> Type of the second element
 */
public class Pair<T,E> implements Serializable{

 private static final long serialVersionUID = 921854014515969561L;
 /**
  * first element
  */
 private T first;
 /**
  * second element
  */
 private E second;

 /**
  * Generates a new 2-Pair
  * @param first The first element of the 2-Pair.
  * @param second The second element of the 2-Pair.
  */
 public Pair(T first, E second){
   this.first=first;
   this.second=second;
 }

 /**
  * Returns the first element of the 2-Pair.
  * @return The first element of the 2-Pair.
  */
 public T getFirst(){
   return first;
 }

 /**
  * Returns the second element of the 2-Pair.
  * @return The second element of the 2-Pair.
  */
 public E getSecond(){
   return second;
 }

 public void setFirst(T elem){
   first=elem;
 }

 public void setSecond(E elem){
   second=elem;
 }

 @Override
 public boolean equals(Object obj){
   if(obj == null){
     return false;
   }
   if(!(obj instanceof Pair)){
     return false;
   }
   @SuppressWarnings("unchecked")
   Pair<T,E> other=(Pair<T,E>) obj;
   return (first.equals(other.first) && second.equals(other.second));
 }

 @Override
 public String toString(){
   return "["+((first==null)?"Null":((first instanceof CExpression)?((CExpression)first).toASTString() :first.toString()))+","+((second==null)?"Null":((second instanceof CExpression)?((CExpression)second).toASTString():(second.toString())))+"]";
 }

 @Override
 public int hashCode() {
   // TODO Auto-generated method stub
   return super.hashCode();
 }
}