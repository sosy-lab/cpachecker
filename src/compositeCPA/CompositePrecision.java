/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
/**
 * 
 */
package compositeCPA;

import java.util.ArrayList;
import java.util.List;

import cpa.common.interfaces.Precision;
import cpa.common.interfaces.WrapperPrecision;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class CompositePrecision implements WrapperPrecision {
  private final List<Precision> precisions;

  public CompositePrecision (List<Precision> precisions) {
    this.precisions = precisions;
  }

  public List<Precision> getPrecisions() {
    return precisions;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    } else if (other == null || !(other instanceof CompositePrecision)) {
      return false;
    }
    
    return precisions.equals(((CompositePrecision)other).precisions);
  }

  @Override
  public int hashCode() {
    return precisions.hashCode();
  }

  public Precision get(int idx) {
    return precisions.get(idx);
  }
  
  @Override
  public String toString() {
    return precisions.toString();
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public <T extends Precision> T retrieveWrappedPrecision(Class<T> pType) {
    for (Precision precision : precisions) {
      if (precision != null) {
        if (precision.getClass().equals(pType)) {
          return (T)precision;
        
        } else if (precision instanceof WrapperPrecision) {
          T wrappedPrecision = ((WrapperPrecision)precision).retrieveWrappedPrecision(pType);
          if (wrappedPrecision != null) {
            return wrappedPrecision;
          }
        }
      }
    }
    return null;
  }
  
  @Override
  public Precision replaceWrappedPrecision(Precision newPrecision) {
    Class<? extends Precision> newPrecClass = newPrecision.getClass();
    
    if (newPrecClass.equals(CompositePrecision.class)) {
      return newPrecision;
    }
    
    List<Precision> newPrecisions = new ArrayList<Precision>(precisions.size());
    boolean changed = false;
    for (Precision precision : precisions) {
      if (precision == null) {
        newPrecisions.add(null);
      
      } else if (newPrecClass.equals(precision.getClass())) {
        newPrecisions.add(newPrecision);
        changed = true;
      
      } else if (precision instanceof WrapperPrecision) {
        Precision newWrappedPrecision = ((WrapperPrecision)precision).replaceWrappedPrecision(newPrecision);
        if (newWrappedPrecision != null) {
          newPrecisions.add(newWrappedPrecision);
          changed = true;
          
        } else {
          newPrecisions.add(precision);
        }
      } else {
        newPrecisions.add(precision);
      }
    }
    return changed ? new CompositePrecision(newPrecisions) : null;
  }
}
