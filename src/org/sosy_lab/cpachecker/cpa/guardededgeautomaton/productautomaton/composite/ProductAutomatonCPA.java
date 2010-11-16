package org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.composite;

import java.util.Collection;

import org.sosy_lab.cpachecker.core.defaults.MergeSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeDomain;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class ProductAutomatonCPA extends CompositeCPA {

  public static ProductAutomatonCPA create(Collection<GuardedEdgeAutomatonCPA> pAutomatonCPAs) {
    Preconditions.checkNotNull(pAutomatonCPAs);
    Preconditions.checkArgument(pAutomatonCPAs.size() > 0);
    
    ImmutableList.Builder<AbstractDomain> domains = ImmutableList.builder();
    ImmutableList.Builder<TransferRelation> transferRelations = ImmutableList.builder();
    ImmutableList.Builder<StopOperator> stopOperators = ImmutableList.builder();
    ImmutableList.Builder<ConfigurableProgramAnalysis> lCPAs = ImmutableList.builder();

    for (ConfigurableProgramAnalysis sp : pAutomatonCPAs) {
      domains.add(sp.getAbstractDomain());
      transferRelations.add(sp.getTransferRelation());
      stopOperators.add(sp.getStopOperator());
      lCPAs.add(sp);
    }

    CompositeDomain compositeDomain = new CompositeDomain(domains.build());
    ProductAutomatonTransferRelation compositeTransfer = new ProductAutomatonTransferRelation(transferRelations.build());
    ProductAutomatonStopOperator compositeStop = new ProductAutomatonStopOperator(stopOperators.build());
    
    return new ProductAutomatonCPA(compositeDomain, compositeTransfer, compositeStop, lCPAs.build());
  }
  
  public ProductAutomatonCPA(AbstractDomain abstractDomain,
      TransferRelation transferRelation,
      StopOperator stopOperator,
      ImmutableList<ConfigurableProgramAnalysis> cpas) {
    super(abstractDomain, transferRelation, new MergeSepOperator(), stopOperator,
        ProductAutomatonPrecisionAdjustment.getInstance(), cpas);
  }

}
