package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="URI" type="{http://www.w3.org/2001/XMLSchema}anyURI"/>
 *         &lt;element ref="{}hashes" minOccurs="0"/>
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
@XmlRootElement(name = "resource")
public class Resource {

    @XmlElement(name = "URI", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String uri;
    protected Hashes hashes;

    public String getURI() {
        return uri;
    }

    public void setURI(String value) {
        this.uri = value;
    }

    public Hashes getHashes() {
        return hashes;
    }

    public void setHashes(Hashes value) {
        this.hashes = value;
    }

}
