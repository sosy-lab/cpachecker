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
 *         &lt;element ref="{}hashMethod"/>
 *         &lt;element ref="{}hashValue"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "hashMethod",
    "hashValue"
})
@XmlRootElement(name = "hash")
public class Hash {

    @XmlElement(required = true)
    protected String hashMethod;
    @XmlElement(required = true)
    protected String hashValue;

    public String getHashMethod() {
        return hashMethod;
    }

    public void setHashMethod(String value) {
        this.hashMethod = value;
    }

    public String getHashValue() {
        return hashValue;
    }

    public void setHashValue(String value) {
        this.hashValue = value;
    }

}
