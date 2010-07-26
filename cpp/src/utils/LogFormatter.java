/**
 * Copyright 2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/** Prints just the date and the log message. */

public class LogFormatter extends Formatter {
    // date format
    private static final String FORMAT = "yyMMdd HHmmss";
    private final Date date = new Date();
    private final SimpleDateFormat formatter = new SimpleDateFormat(FORMAT);
    // line seperator
    private static final String NEWLINE = System.getProperty("line.separator");

    private static boolean loggedSevere = false;

    private static boolean showTime = true;
    private static boolean showThreadIDs = false;
    private static boolean showThreadNames = false;
    private static boolean showLoggerNames = false;
    private static String prefix = "";

    private static LogFormatter instance;
    private static long eon = System.currentTimeMillis(); 
    
    // install when this class is loaded
    static {
        instance = new LogFormatter();
        Handler[] handlers = LogFormatter.getLogger("").getHandlers();
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].setFormatter(instance);
            handlers[i].setLevel(Level.ALL);
        }
    }

    public static Logger getLogger(Object o) {
        if (o instanceof Class)
            return Logger.getLogger(((Class)o).getName());
        else return Logger.getLogger(o.toString());
    }
    
    /**
     * Gets a logger and, as a side effect, installs this as the default
     * formatter.
     */
    public static Logger getLogger(String name) {
        // just referencing this class installs it
        return Logger.getLogger(name);
    }
    
    /**
     * Set a file logger for the log tree starts with <code>name</code>
     * @param name the root name of the logger
     * @param path file path
     * @return the file logger
     * @throws IOException if cannot create file handler given <code>path</code>
     */
    public static Logger setFileLogger(String name, String path) 
            throws IOException {
        FileHandler handler = new FileHandler(path);
        handler.setFormatter(new LogFormatter());
        handler.setLevel(Level.ALL);
        Logger logger = Logger.getLogger(name);
        logger.addHandler(handler);
        return logger;
    }
    
    public static Logger setRotateFileLogger(String name, String dir, String file,
            int limit, int count, boolean append) throws IOException {
        // handler
        FileHandler handler = new FileHandler(dir+"/"+file+".%g",limit,count,append);
        handler.setFormatter(new LogFormatter());
        // logger
        Logger logger=Logger.getLogger(name);
        logger.addHandler(handler);
        return logger;
    }
    
    public static void clearLoggerHandlers(String name) {
        Logger logger = Logger.getLogger(name);
        Handler[] handlers = logger.getHandlers();
        for (int i = 0; i < handlers.length; i++)
            logger.removeHandler(handlers[i]);
    }

    /** When true, time is logged with each entry. */
    public static void setShowTime(boolean showTime) {
        LogFormatter.showTime = showTime;
    }

    /** When set true, thread IDs are logged. */
    public static void setShowThreadIDs(boolean showThreadIDs) {
        LogFormatter.showThreadIDs = showThreadIDs;
    }

    /**
     * zf: Show thread names in the log. Notice this is <b>very slow</b> and
     * maybe inaccurate due to VM nature.
     * 
     * @param showThreadNames
     */
    public static void setShowThreadNames(boolean showThreadNames) {
        LogFormatter.showThreadNames = showThreadNames;
    }

    /**
     * zhangkun: Show Loggers' names in the log
     * @param showClassNames
     */
    public static void setShowLoggerNames(boolean showClassNames) {
        LogFormatter.showLoggerNames = showClassNames;
    }

    public static void setPrefix(String pre) {
        prefix = pre;
    }
    
    /**
     * Set log levels for a number of packages.
     * @param config e.g. "odis.dfs" ==> "FINE"
     */
    // zf: from outside LogFormatter, please call loadConfig() instead.
    public static void setLogLevel(Properties config, Level defaultLevel) {
        Logger.getLogger("").setLevel(defaultLevel);
        for (Object key : config.keySet()) {
            String name = key.toString();
            String value = config.getProperty(name, null);
            Level level = null;
            try {
                if (value==null) level = defaultLevel;
                else level = Level.parse(value);
            } catch (IllegalArgumentException e) {
                level = defaultLevel;
            }
            Logger.getLogger(name).setLevel(level);
        }
    }

    /**
     * Format the given LogRecord.
     * 
     * @param record
     *            the log record to be formatted.
     * @return a formatted log record
     */
    public String format(LogRecord record) {
        StringBuffer buffer = new StringBuffer();

        // the date
        if (showTime) {
            synchronized(date) {
                date.setTime(record.getMillis());
                formatter.format(date, buffer, new FieldPosition(0));
            }
            buffer.append(" ");
        }

        if (prefix != null && !prefix.equals(""))
            buffer.append(prefix + " ");

        // the thread id
        if (showThreadIDs) {
            buffer.append(record.getThreadID());
            buffer.append(" ");
        }

        if (showThreadNames) {
            Thread t = getThread(record.getThreadID());
            if (t != null)
                buffer.append("<" + t.getName() + ">");
            else
                buffer.append("<?>");
            buffer.append(" ");
        }

        if (showLoggerNames) {
            buffer.append(record.getLoggerName() + " ");
        }

        // handle SEVERE specially
        if (record.getLevel() == Level.SEVERE) {
            buffer.append("[SEVERE] "); // flag it in log
            loggedSevere = true; // set global flag
        } else if (record.getLevel() == Level.WARNING) {
            buffer.append("[WARNING] ");
        }

        // the message
        buffer.append(formatMessage(record));

        buffer.append(NEWLINE);

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                buffer.append(sw.toString());
            } catch (Exception ex) {
            }
        }
        return buffer.toString();
    }

    private static Thread getThread(long id) {
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        ThreadGroup parent;
        while ((parent = g.getParent()) != null) {
            g = parent;
        }
        Thread[] trs = new Thread[g.activeCount() * 2];
        g.enumerate(trs);
        for (int i = 0; i < trs.length; i++) {
            if (trs[i] == null)
                break;
            if (trs[i].getId() == id)
                return trs[i];
        }
        return null;
    }

    /**
     * Returns <code>true</code> if this <code>LogFormatter</code> has
     * logged something at <code>Level.SEVERE</code>
     */
    public static boolean hasLoggedSevere() {
        return loggedSevere;
    }

    /**
     * Set the debug level of the given subsystems, eparated by :.
     */
    public static void setDebugLevel(Level debugLevel, String subsystems) {
        if (subsystems == null || "".equals(subsystems))
            return;
        if (subsystems.equalsIgnoreCase("_all")
                || subsystems.equalsIgnoreCase("_a")) { // Set all the
                                                        // subsystems we care
                                                        // about
            System.out.println("Setting debug level " + debugLevel
                    + " for all subsystems.");
            LogFormatter.getLogger("").setLevel(debugLevel);
        } else {
            String[] subs = subsystems.split(":");
            for (int i = 0; i < subs.length; i++) {
                String str = subs[i]; // package name
                System.out.println("Setting debug level " + debugLevel
                        + " for " + str);
                LogFormatter.getLogger(str).setLevel(debugLevel);
            }
        }
    }

    /** Returns a stream that, when written to, adds log lines. */
    public static PrintStream getLogStream(final Logger logger,
            final Level level) {
        return new PrintStream(new ByteArrayOutputStream() {
            private int scan = 0;

            private boolean hasNewline() {
                for (; scan < count; scan++) {
                    if (buf[scan] == '\n')
                        return true;
                }
                return false;
            }

            public void flush() throws IOException {
                if (!hasNewline())
                    return;
                logger.log(level, toString().trim());
                reset();
                scan = 0;
            }
        }, true);
    }
    
    /**
     * Return time between VM start and t. 
     */
    public static long rtime(long t) {
        return t - eon;
    }

}
