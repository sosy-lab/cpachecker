package org.sosy_lab.cpachecker.fllesh.cpa.composite;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

import com.google.common.collect.ImmutableList;

public class CompoundCPA implements ConfigurableProgramAnalysis, WrapperCPA {

  private CompoundDomain mDomain;
  private List<ConfigurableProgramAnalysis> mCPAs;
  private CompoundPrecisionAdjustment mPrecisionAdjustment;
  private CompoundMergeOperator mMergeOperator;
  private CompoundStopOperator mStopOperator;
  private CompoundTransferRelation mTransferRelation;
  
  public CompoundCPA(List<ConfigurableProgramAnalysis> pCPAs) {
    this(pCPAs, new int[0]);
  }
  
  public CompoundCPA(List<ConfigurableProgramAnalysis> pCPAs, int[] pEqualityIndices) {
    mCPAs = new ArrayList<ConfigurableProgramAnalysis>(pCPAs);
    
    List<AbstractDomain> lDomains = new ArrayList<AbstractDomain>(pCPAs.size());
    List<PrecisionAdjustment> lPrecisionAdjustments = new ArrayList<PrecisionAdjustment>(pCPAs.size());
    ImmutableList.Builder<MergeOperator> lMergeOperators = ImmutableList.builder();
    List<StopOperator> lStopOperators = new ArrayList<StopOperator>(pCPAs.size());
    List<TransferRelation> lTransferRelations = new ArrayList<TransferRelation>(pCPAs.size());
    
    for (ConfigurableProgramAnalysis lCPA : pCPAs) {
      lDomains.add(lCPA.getAbstractDomain());
      lPrecisionAdjustments.add(lCPA.getPrecisionAdjustment());
      lMergeOperators.add(lCPA.getMergeOperator());
      lStopOperators.add(lCPA.getStopOperator());
      lTransferRelations.add(lCPA.getTransferRelation());
    }
    
    mDomain = new CompoundDomain(lDomains);
    mPrecisionAdjustment = new CompoundPrecisionAdjustment(lPrecisionAdjustments);
    mMergeOperator = new CompoundMergeOperator(lMergeOperators.build(), pEqualityIndices);
    mStopOperator = new CompoundStopOperator(lStopOperators);
    mTransferRelation = new CompoundTransferRelation(lTransferRelations, lDomains);
  }
  
  @Override
  public CompoundDomain getAbstractDomain() {
    return mDomain;
  }

  @Override
  public CompoundElement getInitialElement(CFAFunctionDefinitionNode pNode) {
    List<AbstractElement> lInitialElements = new ArrayList<AbstractElement>(mCPAs.size());
    
    for (ConfigurableProgramAnalysis lCPA : mCPAs) {
      lInitialElements.add(lCPA.getInitialElement(pNode));
    }
    
    return new CompoundElement(lInitialElements);
  }

  @Override
  public CompositePrecision getInitialPrecision(CFAFunctionDefinitionNode pNode) {
    List<Precision> lInitialPrecisions = new ArrayList<Precision>(mCPAs.size());
    
    for (ConfigurableProgramAnalysis lCPA : mCPAs) {
      lInitialPrecisions.add(lCPA.getInitialPrecision(pNode));
    }
    
    return new CompositePrecision(lInitialPrecisions);
  }

  @Override
  public CompoundMergeOperator getMergeOperator() {
    return mMergeOperator;
  }

  @Override
  public CompoundPrecisionAdjustment getPrecisionAdjustment() {
    return mPrecisionAdjustment;
  }

  @Override
  public CompoundStopOperator getStopOperator() {
    return mStopOperator;
  }

  @Override
  public CompoundTransferRelation getTransferRelation() {
    return mTransferRelation;
  }

  @Override
  public <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(
      Class<T> pType) {
    if (pType.isAssignableFrom(getClass())) {
      return pType.cast(this);
    }
    for (ConfigurableProgramAnalysis lCPA : mCPAs) {
      if (pType.isAssignableFrom(lCPA.getClass())) {
        return pType.cast(lCPA);
      } else if (lCPA instanceof WrapperCPA) {
        T result = ((WrapperCPA)lCPA).retrieveWrappedCpa(pType);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

}
