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
 *         &lt;element ref="{}project"/>
 *         &lt;element ref="{}analysis" minOccurs="0"/>
 *         &lt;element ref="{}tool" minOccurs="0"/>
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
@XmlRootElement(name = "info")
public class Info {

    @XmlElement(required = true)
    protected Project project;
    protected Analysis analysis;
    protected Tool tool;

    public Project getProject() {
        return project;
    }

    public void setProject(Project value) {
        this.project = value;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(Analysis value) {
        this.analysis = value;
    }

    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool value) {
        this.tool = value;
    }

}
