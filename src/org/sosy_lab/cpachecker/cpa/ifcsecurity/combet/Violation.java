package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}location"/>
 *         &lt;element ref="{}violationelement"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"location", "violationelement"})
@XmlRootElement(name = "violation")
public class Violation {

  @XmlElement(required = true)
  protected String location;
  @XmlElement(required = true)
  protected Violationelement violationelement;

  public String getLocation() {
    return location;
  }

  public void setLocation(String value) {
    this.location = value;
  }

  public Violationelement getViolationelement() {
    return violationelement;
  }

  public void setViolationelement(Violationelement value) {
    this.violationelement = value;
  }

}
