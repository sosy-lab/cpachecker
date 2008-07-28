package cpaplugin.cpa.cpas.symbpredabs.logging;

import java.util.Collection;
import java.util.logging.Level;

import cpaplugin.logging.CPACheckerLogger;

/**
 * Poor man's lazy evaluation :-)
 * @author alb
 */
public class LazyLogger {
    static class LogLevel extends Level {
        /**
         * 
         */
        private static final long serialVersionUID = -2573882076078285905L;

        public LogLevel(String name, int value) {
            super(name, value);
        }
    };
    public static Level DEBUG_1 = new LogLevel("AG_DEBUG_1", 
            Level.FINE.intValue()-10);
    public static Level DEBUG_2 = new LogLevel("AG_DEBUG_2", 
            Level.FINE.intValue()-20);
    public static Level DEBUG_3 = new LogLevel("AG_DEBUG_3", 
            Level.FINE.intValue()-30);
    public static Level DEBUG_4 = new LogLevel("AG_DEBUG_4", 
            Level.FINE.intValue()-40);
    
    public static void log(Level lvl, Object o1) {
        if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
            CPACheckerLogger.log(lvl, o1.toString());
        }
    }
    
    public static void log(Level lvl, Object o1, Object o2) {
        if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
            CPACheckerLogger.log(lvl, o1.toString() + o2.toString());
        }
    }
    
    public static void log(Level lvl, Object o1, Object o2, Object o3) {
        if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
            CPACheckerLogger.log(lvl, o1.toString() + o2.toString() + 
                                 o3.toString());
        }
    }

    public static void log(Level lvl, Object o1, Object o2, 
                           Object o3, Object o4) {
        if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
            CPACheckerLogger.log(lvl, o1.toString() + o2.toString() + 
                                 o3.toString() + o4.toString());
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void log(Level lvl, Collection c) {
        if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
            StringBuffer buf = new StringBuffer();
            for (Object o : c) {
                buf.append(o.toString());
            }
            CPACheckerLogger.log(lvl, buf.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public static void log(Level lvl, Object o1, Collection c) {
        if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
            StringBuffer buf = new StringBuffer();
            buf.append(o1.toString());
            for (Object o : c) {
                buf.append(o.toString());
            }
            CPACheckerLogger.log(lvl, buf.toString());
        }
    }
}
