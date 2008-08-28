package cpaplugin.cpa.cpas.symbpredabs;

/**
 * A "trace" that gives no information about the error trace :-)
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ConcreteTraceNoInfo implements ConcreteTrace {
    public String toString() {
        return "<ERROR TRACE>";
    }
}
