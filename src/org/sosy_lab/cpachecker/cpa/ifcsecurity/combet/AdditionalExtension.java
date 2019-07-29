package org.sosy_lab.cpachecker.cpa.ifcsecurity.combet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>
 * Java-Class for the "additionalExtension"-complex type.
 *
 *
 * <pre>
 * &lt;complexType name="additionalExtension">
 *   &lt;complexContent>
 *     &lt;extension base="{}additionalType">
 *       &lt;all>
 *         &lt;element ref="{}description" minOccurs="0"/>
 *       &lt;/all>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "additionalExtension", propOrder = {
    "description"
})
public class AdditionalExtension
    extends AdditionalType
{

    protected String description;


    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

}
