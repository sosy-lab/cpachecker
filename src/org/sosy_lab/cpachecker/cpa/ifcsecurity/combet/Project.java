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
 *         &lt;element ref="{}identifier"/>
 *         &lt;element ref="{}resources" minOccurs="0"/>
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
@XmlRootElement(name = "project")
public class Project {

    @XmlElement(required = true)
    protected String identifier;
    protected Resources resources;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String value) {
        this.identifier = value;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources value) {
        this.resources = value;
    }

}
