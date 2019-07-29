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
 *       &lt;all>
 *         &lt;element ref="{}securityclass"/>
 *         &lt;element ref="{}inferences"/>
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
@XmlRootElement(name = "policyitem")
public class Policyitem {

    @XmlElement(required = true)
    protected String securityclass;
    @XmlElement(required = true)
    protected Inferences inferences;

    public String getSecurityclass() {
        return securityclass;
    }

    public void setSecurityclass(String value) {
        this.securityclass = value;
    }

    public Inferences getInferences() {
        return inferences;
    }

    public void setInferences(Inferences value) {
        this.inferences = value;
    }

}
