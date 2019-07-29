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
 *         &lt;element ref="{}mapitem"/>
 *         &lt;element ref="{}mapping"/>
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
    "mapitem",
    "mapping"
})
@XmlRootElement(name = "violationelement")
public class Violationelement {

    @XmlElement(required = true)
    protected Mapitem mapitem;
    @XmlElement(required = true)
    protected Mapping mapping;

    public Mapitem getMapitem() {
        return mapitem;
    }

    public void setMapitem(Mapitem value) {
        this.mapitem = value;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public void setMapping(Mapping value) {
        this.mapping = value;
    }

}
