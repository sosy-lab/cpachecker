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
 *         &lt;element ref="{}securityclass" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"securityclass"})
@XmlRootElement(name = "securityclasses")
public class Securityclasses {

  protected List<String> securityclass;

  /**
   * Gets the value of the securityclass property.
   *
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any
   * modification you make to the returned list will be present inside the JAXB object. This is why
   * there is not a <CODE>set</CODE> method for the securityclass property.
   *
   * <p>
   * For example, to add a new item, do as follows:
   *
   * <pre>
   * getSecurityclass().add(newItem);
   * </pre>
   *
   *
   * <p>
   * Objects of the following type(s) are allowed in the list {@link String }
   *
   *
   */
  public List<String> getSecurityclass() {
    if (securityclass == null) {
      securityclass = new ArrayList<>();
    }
    return this.securityclass;
  }

}
