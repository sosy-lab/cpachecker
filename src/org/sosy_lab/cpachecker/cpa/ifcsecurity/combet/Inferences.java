package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}securityclasses" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"securityclasses"})
@XmlRootElement(name = "inferences")
public class Inferences {

  protected List<Securityclasses> securityclasses;

  /**
   * Gets the value of the securityclasses property.
   *
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any
   * modification you make to the returned list will be present inside the JAXB object. This is why
   * there is not a <CODE>set</CODE> method for the securityclasses property.
   *
   * <p>
   * For example, to add a new item, do as follows:
   *
   * <pre>
   * getSecurityclasses().add(newItem);
   * </pre>
   *
   *
   * <p>
   * Objects of the following type(s) are allowed in the list {@link Securityclasses }
   *
   *
   */
  public List<Securityclasses> getSecurityclasses() {
    if (securityclasses == null) {
      securityclasses = new ArrayList<>();
    }
    return this.securityclasses;
  }

}
