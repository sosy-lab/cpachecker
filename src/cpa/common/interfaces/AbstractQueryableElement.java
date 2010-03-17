package cpa.common.interfaces;

import exceptions.InvalidQueryException;

/**
 * An AbstractElement that evaluates Properties (String-encoded) and 
 * returns whether they are satisfied in concrete states represented by the AbstractElement. 
 * @author rhein
 */
public interface AbstractQueryableElement extends AbstractElement {

  public String getCPAName();
  
  /**
   * Checks whether this AbstractElement satisfies the property.
   * Each CPA defines which properties can be evaluated.
   * @param property
   * @return if the property is satisfied
   * @throws InvalidSyntaxException if the property is not given in the (CPA-specific) syntax
   */
  public boolean checkProperty(String property) throws InvalidQueryException;
  
}
