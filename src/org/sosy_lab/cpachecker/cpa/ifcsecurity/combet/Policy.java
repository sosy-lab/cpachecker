package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element ref="{}description" minOccurs="0"/>
 *         &lt;element ref="{}securityclasses" minOccurs="0"/>
 *         &lt;element ref="{}allowed" minOccurs="0"/>
 *         &lt;element ref="{}notallowed" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {

})
@XmlRootElement(name = "policy")
public class Policy {

  protected String description;
  protected Securityclasses securityclasses;
  protected Allowed allowed;
  protected Notallowed notallowed;

  public String getDescription() {
    return description;
  }

  public void setDescription(String value) {
    this.description = value;
  }

  public Securityclasses getSecurityclasses() {
    return securityclasses;
  }

  public void setSecurityclasses(Securityclasses value) {
    this.securityclasses = value;
  }

  public Allowed getAllowed() {
    return allowed;
  }

  public void setAllowed(Allowed value) {
    this.allowed = value;
  }

  public Notallowed getNotallowed() {
    return notallowed;
  }

  public void setNotallowed(Notallowed value) {
    this.notallowed = value;
  }

}
