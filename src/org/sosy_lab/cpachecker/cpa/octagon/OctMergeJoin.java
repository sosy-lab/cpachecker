/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.octagon;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.octagon.LibraryAccess;

public class OctMergeJoin implements MergeOperator{

  @Override
  public AbstractElement merge(AbstractElement element1, AbstractElement element2, Precision prec) {
    OctElement octEl1 = (OctElement) element1;
    OctElement octEl2 = (OctElement) element2;

    int dim1 = LibraryAccess.getDim(octEl1);
    int dim2 = LibraryAccess.getDim(octEl2);

    // TODO recursive join should be handled gracefully here
    //octEl2.addVariablesFrom(octEl1);
    //System.out.println(octEl1.getNumberOfVars() + "{ }" + octEl2.getNumberOfVars());

    assert(dim1 == dim2);

//  if(OctConstants.useWidening){
//  OctConstants.useWidening = false;
//  return LibraryAccess.widening(octEl2, octEl1);
//  }
//  else{
    //System.out.println("Using UNION");
//  System.out.println(" ============ Merging ================ ");
//  System.out.println(octEl1);
//  System.out.println(" ------- ");
//  System.out.println(octEl2);
//  System.out.println( ">>>>>>>>>>>>>>> merged <<<<<<<<<<<<<<<< " );
    OctElement ret =  LibraryAccess.union(octEl2, octEl1);
//  System.out.println(ret);
//  System.out.println();
    return ret;
    //}
  }
}
