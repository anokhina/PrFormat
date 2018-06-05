package ru.org.sevn.netbeans.format;

import java.io.PrintStream;
import org.apache.maven.plugin.logging.Log;

public class PluginLog implements Log {
    
    private boolean debugEnabled = true;
    private boolean infoEnabled = true;
    private boolean warnEnabled = true;
    private boolean errorEnabled = true;

    static enum LEVEL {
        DEBUG, INFO, WARN, ERR
    }
    
    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void debug(CharSequence cs) {
        msg(LEVEL.DEBUG, cs, null);
    }

    @Override
    public void debug(CharSequence cs, Throwable thrwbl) {
        msg(LEVEL.DEBUG, cs, thrwbl);
    }

    @Override
    public void debug(Throwable thrwbl) {
        msg(LEVEL.DEBUG, null, thrwbl);
    }

    @Override
    public boolean isInfoEnabled() {
        return infoEnabled;
    }

    @Override
    public void info(CharSequence cs) {
        msg(LEVEL.INFO, cs, null);
    }

    @Override
    public void info(CharSequence cs, Throwable thrwbl) {
        msg(LEVEL.INFO, cs, thrwbl);
    }

    @Override
    public void info(Throwable thrwbl) {
        msg(LEVEL.INFO, null, thrwbl);
    }

    @Override
    public boolean isWarnEnabled() {
        return warnEnabled;
    }

    @Override
    public void warn(CharSequence cs) {
        msg(LEVEL.WARN, cs, null);
    }

    @Override
    public void warn(CharSequence cs, Throwable thrwbl) {
        msg(LEVEL.WARN, cs, thrwbl);
    }

    @Override
    public void warn(Throwable thrwbl) {
        msg(LEVEL.WARN, null, thrwbl);
    }

    @Override
    public boolean isErrorEnabled() {
        return errorEnabled;
    }

    @Override
    public void error(CharSequence cs) {
        msg(LEVEL.ERR, cs, null);
    }

    @Override
    public void error(CharSequence cs, Throwable thrwbl) {
        msg(LEVEL.ERR, cs, thrwbl);
    }

    @Override
    public void error(Throwable thrwbl) {
        msg(LEVEL.ERR, null, thrwbl);
    }
    
    private CharSequence any(CharSequence cs) {
        if (cs == null) return "";
        return cs;
    }
    
    public void msg(LEVEL lev, CharSequence cs, Throwable thrwbl) {
        final PrintStream ps = System.out;
        ps.println("" + lev.name() + "> " + any(cs));
        if (thrwbl != null) {
            thrwbl.printStackTrace(ps);
        }
    }
}
