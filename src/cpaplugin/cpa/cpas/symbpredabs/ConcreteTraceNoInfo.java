package cpaplugin.cpa.cpas.symbpredabs;

/**
 * A "trace" that gives no information about the error trace :-)
 * @author alb
 */
public class ConcreteTraceNoInfo implements ConcreteTrace {
    public String toString() {
        return "<ERROR TRACE>";
    }
}
