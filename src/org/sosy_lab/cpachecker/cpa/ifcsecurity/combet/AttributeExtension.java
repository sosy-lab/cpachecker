package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <pre>
 * &lt;complexType name="attributeExtension">
 *   &lt;complexContent>
 *     &lt;extension base="{}attributeType">
 *       &lt;all>
 *         &lt;element ref="{}policy" minOccurs="0"/>
 *         &lt;element ref="{}mapping" minOccurs="0"/>
 *         &lt;element ref="{}certificates" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeExtension", propOrder = {
    "policy",
    "mapping",
    "certificates"
})
public class AttributeExtension
    extends AttributeType
{

    protected Policy policy;
    protected Mapping mapping;
    protected Certificates certificates;

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy value) {
        this.policy = value;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public void setMapping(Mapping value) {
        this.mapping = value;
    }

    public Certificates getCertificates() {
        return certificates;
    }

    public void setCertificates(Certificates value) {
        this.certificates = value;
    }

}
