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
 *         &lt;element ref="{}name"/>
 *         &lt;element ref="{}description" minOccurs="0"/>
 *         &lt;element ref="{}attributes" minOccurs="0"/>
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
@XmlRootElement(name = "analysis")
public class Analysis {

    @XmlElement(required = true)
    protected String name;
    protected String description;
    protected Attributes attributes;


    public String getName() {
        return name;
    }


    public void setName(String value) {
        this.name = value;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String value) {
        this.description = value;
    }


    public Attributes getAttributes() {
        return attributes;
    }


    public void setAttributes(Attributes value) {
        this.attributes = value;
    }

}
