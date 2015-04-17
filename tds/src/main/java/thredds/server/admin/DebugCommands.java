/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import thredds.servlet.ServletUtil;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.io.PrintStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.io.RandomAccessFile;

/**
 * A Singleton class instantiated by Spring, to populate the Debug methods in the
 * DebugHandler class.
 *
 * @author caron
 * @since Jan 15, 2009
 */
@Component
public class DebugCommands {

  //////////////////////////////////////////////////////////////////////////////////////////

  private List<Category> dhList = new ArrayList<>();

  public List<Category> getCategories() {
    return dhList;
  }

  public Category findCategory(String name) {
    for (Category dh : dhList) {
      if (dh.name.equals(name))
        return dh;
    }
    return new Category(name);
  }

  public class Category {
    Map<String, Action> actions = new LinkedHashMap<>();
    String name;

    private Category(String name) {
      this.name = name;
      dhList.add(this);
    }

    public void addAction(Action act) {
      actions.put(act.name, act);
    }
  }

  static public abstract class Action {
    public String name, desc;

    public Action(String name, String desc) {
      this.name = name;
      this.desc = desc;
    }

    public abstract void doAction(Event e);
  }

  static public class Event {
    public HttpServletRequest req;
    public HttpServletResponse res;
    public PrintStream pw;
    public ByteArrayOutputStream bos;
    public String target;

    public Event(HttpServletRequest req, HttpServletResponse res, PrintStream pw, ByteArrayOutputStream bos, String target) {
      this.req = req;
      this.res = res;
      this.pw = pw;
      this.bos = bos;
      this.target = target;
    }
  }

  ///////////////////////////////////////////////////

  @Value("${tds.version}")
  private String webappVersion;

  @Value("${tds.version.builddate}")
  private String webappVersionBuildDate;

  public DebugCommands() {
    makeGeneralActions();
    makeDebugActions();
    makeCacheActions();
  }

  protected void makeCacheActions() {
    Category debugHandler = findCategory("Caches");
    Action act;

    act = new Action("showCaches", "Show All File Object Caches") {
      public void doAction(Event e) {
        Formatter f = new Formatter(e.pw);
        FileCacheIF fc;

        fc = RandomAccessFile.getGlobalFileCache();
        if (fc == null) f.format("%nRandomAccessFile : turned off%n");
        else {
          f.format("%n%n");
          fc.showCache(f);
        }

        fc = NetcdfDataset.getNetcdfFileCache();
        if (fc == null) f.format("NetcdfDatasetFileCache : turned off%n");
        else {
          f.format("%n%n");
          fc.showCache(f);
        }

        fc = GribCdmIndex.gribCollectionCache;
        if (fc == null) f.format("%nTimePartitionCache : turned off%n");
        else {
          f.format("%n%n");
          fc.showCache(f);
        }

        e.pw.flush();
      }
    };
    debugHandler.addAction(act);

    act = new Action("clearCaches", "Clear All File Object Caches") {
       public void doAction(Event e) {
         NetcdfDataset.getNetcdfFileCache().clearCache(false);
         RandomAccessFile.getGlobalFileCache().clearCache(false);
         FileCacheIF fc = GribCdmIndex.gribCollectionCache;
         if (fc != null) fc.clearCache(false);
         e.pw.println("  ClearCache ok");
       }
     };
     debugHandler.addAction(act);

    act = new Action("disableRAFCache", "Disable RandomAccessFile Cache") {
       public void doAction(Event e) {
         RandomAccessFile.getGlobalFileCache().disable();
         e.pw.println("  Disable RandomAccessFile Cache ok");
       }
     };
     debugHandler.addAction(act);

    act = new Action("forceRAFCache", "Force clear RandomAccessFile Cache") {
      public void doAction(Event e) {
        RandomAccessFile.getGlobalFileCache().clearCache(true);
        e.pw.println("  RandomAccessFile force clearCache done");
      }
    };
    debugHandler.addAction(act);


    act = new Action("disableNetcdfCache", "Disable NetcdfDatasetFile Cache") {
       public void doAction(Event e) {
         NetcdfDataset.disableNetcdfFileCache();
         e.pw.println("  Disable NetcdfFile Cache ok");
       }
     };
     debugHandler.addAction(act);

     act = new Action("forceNCCache", "Force clear NetcdfDatasetFile Cache") {
      public void doAction(Event e) {
        NetcdfDataset.getNetcdfFileCache().clearCache(true);
        e.pw.println("  NetcdfFileCache force clearCache done");
      }
    };
    debugHandler.addAction(act);

    act = new Action("disableTimePartitionCache", "Disable TimePartition Cache") {
       public void doAction(Event e) {
         GribCdmIndex.disableGribCollectionCache();
         e.pw.println("  Disable gribCollectionCache ok");
       }
     };
     debugHandler.addAction(act);

    act = new Action("forceGCCache", "Force clear TimePartition Cache") {
      public void doAction(Event e) {
        FileCacheIF fc = GribCdmIndex.gribCollectionCache;
        if (fc != null) fc.clearCache(true);
        e.pw.println("  gribCollectionCache force clearCache done");
      }
    };
    debugHandler.addAction(act);

  }

  protected void makeDebugActions() {
    Category debugHandler = findCategory("Debug");
    Action act;

    act = new Action("enableRafHandles", "Toggle tracking open RAF") {
      public void doAction(Event e) {
        try {
          RandomAccessFile.setDebugLeaks( !RandomAccessFile.getDebugLeaks());
          e.pw.println("  Tracking RAF=" + RandomAccessFile.getDebugLeaks());
        } catch (Exception ioe) {
          e.pw.println(ioe.getMessage());
        }
      }
    };
    debugHandler.addAction(act);

    act = new Action("showRafHandles", "Show open RAF") {
      public void doAction(Event e) {
        try {
          List<String> names = RandomAccessFile.getOpenFiles();
          e.pw.println("count=" + names.size());
          for (String s : names) {
            e.pw.println("  " + s);
          }
        } catch (Exception ioe) {
          e.pw.println(ioe.getMessage());
        }
      }
    };
    debugHandler.addAction(act);
  }


  protected void makeGeneralActions() {
    Category debugHandler = findCategory("General");
    Action act;

    act = new Action("showVersion", "Show Build Version") {
      public void doAction(Event e) {
        try {
          e.pw.println("version= "+webappVersion);                 // LOOK could show all of TdsContext
          e.pw.println("build date= "+webappVersionBuildDate);
        } catch (Exception ioe) {
          e.pw.println(ioe.getMessage());
        }
      }
    };
    debugHandler.addAction(act);

    act = new Action("showRuntime", "Show Runtime info") {
      public void doAction(Event e) {
        Runtime runt = Runtime.getRuntime();
        double scale = 1.0 / (1000.0 * 1000.0);
        e.pw.println(" freeMemory= " + scale * runt.freeMemory() + " Mb");
        e.pw.println(" totalMemory= " + scale * runt.totalMemory() + " Mb");
        e.pw.println(" maxMemory= " + scale * runt.maxMemory() + " Mb");
        e.pw.println(" availableProcessors= " + runt.availableProcessors());
        e.pw.println();
        ServletUtil.showThreads(e.pw);
      }
    };
    debugHandler.addAction(act);

    /* act = new DebugHandler.Action("showLoggers", "Show Log4J info") {
      public void doAction(DebugHandler.Event e) {
        showLoggers(e.req, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("setLogger", null) {
      public void doAction(DebugHandler.Event e) {
        if (e.target == null) {
          e.pw.println(" Must be setLogger=loggerName");
          return;
        }

        StringTokenizer stoker = new StringTokenizer(e.target, "&=");
        if (stoker.countTokens() < 3) {
          e.pw.println(" Must be setLogger=loggerName&setLevel=levelName");
          return;
        }

        String loggerName = stoker.nextToken();
        stoker.nextToken(); // level=
        String levelName = stoker.nextToken();

        boolean isRootLogger = loggerName.equals("root");
        if (!isRootLogger && LogManager.exists(loggerName) == null) {
          e.pw.println(" Unknown logger=" + loggerName);
          return;
        }

        if (Level.toLevel(levelName, null) == null) {
          e.pw.println(" Unknown level=" + levelName);
          return;
        }

        Logger log = isRootLogger ? LogManager.getRootLogger() : LogManager.getLogger(loggerName);
        log.setLevel(Level.toLevel(levelName));
        e.pw.println(loggerName + " set to " + levelName);
        showLoggers(e.req, e.pw);
      }
    };
    debugHandler.addAction(act); */

    act = new Action("showRequest", "Show HTTP Request info") {
      public void doAction(Event e) {
        e.pw.println(ServletUtil.showRequestDetail(null, e.req));
      }
    };
    debugHandler.addAction(act);

    act = new Action("showSystemProperties", "Show Server info") {
      public void doAction(Event e) {
        ServletUtil.showServerInfo(e.pw);
      }
    };
    debugHandler.addAction(act);

    /* act = new DebugHandler.Action("showServletInfo", "Show Servlet info") {
      public void doAction(DebugHandler.Event e) {
        ServletUtil.showServletInfo(ThreddsDefaultServlet.this, e.pw);
      }
    };
    debugHandler.addAction(act);  */

    act = new Action("showSession", "Show HTTP Session info") {
      public void doAction(Event e) {
        ServletUtil.showSession(e.req, e.res, e.pw);
      }
    };
    debugHandler.addAction(act);

    act = new Action("showSecurity", "Show Security info") {
      public void doAction(Event e) {
        e.pw.println(ServletUtil.showSecurity(e.req, "admin"));
      }
    };
    debugHandler.addAction(act);

    /* debugHandler = DebugHandler.get("catalogs");
    act = new DebugHandler.Action("reinit", "Reinitialize") {
      public void doAction(DebugHandler.Event e) {
        // TODO The calls to reinit() and initCatalogs() are synchronized but should be atomic.
        // TODO Should change this to build config data structure and synch only when replacing the old with the new structure.
        catHandler.reinit();
        ThreddsConfig.readConfig(log);
        initCatalogs();
        e.pw.println("reinit ok");
      }
    };
    debugHandler.addAction(act); */
  }  

  /* private void changeLogs(String datePattern, long maxFileSize, int maxFiles) {
    // get the existing appender
    Logger logger = LogManager.getLogger("thredds");
    FileAppender fapp = (FileAppender) logger.getAppender("threddsServlet");
    PatternLayout playout = (PatternLayout) fapp.getLayout();
    String filename = fapp.getFile();

    // create a new one
    Appender newAppender = null;

    try {
      if (null != datePattern) {
        newAppender = new DailyRollingFileAppender(playout, filename, datePattern);
      } else if (maxFileSize > 0) {
        RollingFileAppender rapp = new RollingFileAppender(playout, filename);
        rapp.setMaximumFileSize(maxFileSize);
        rapp.setMaxBackupIndex(maxFiles);
        newAppender = rapp;
      } else {
        return;
      }
    } catch (IOException ioe) {
      log.error("Error changing the logger", ioe);
    }

    // replace wherever you find it
    Logger root = LogManager.getRootLogger();
    replaceAppender(root, "threddsServlet", newAppender);

    Enumeration logEnums = LogManager.getCurrentLoggers();
    while (logEnums.hasMoreElements()) {
      Logger log = (Logger) logEnums.nextElement();
      replaceAppender(log, "threddsServlet", newAppender);
    }
  }  */

  /* private void replaceAppender(Logger logger, String want, Appender replaceWith) {
    Enumeration appenders = logger.getAllAppenders();
    while (appenders.hasMoreElements()) {
      Appender app = (Appender) appenders.nextElement();
      if (app.getName().equals(want)) {
        logger.removeAppender(app);
        logger.addAppender(replaceWith);
      }
    }
  }

  void showLoggers(HttpServletRequest req, PrintStream pw) {
    Logger root = LogManager.getRootLogger();
    showLogger(req, root, pw);

    Enumeration logEnums = LogManager.getCurrentLoggers();
    List<Logger> loggersSorted = Collections.list(logEnums);
    Collections.sort(loggersSorted, new LoggerComparator());
    for (Logger logger : loggersSorted) {
      showLogger(req, logger, pw);
    }
  }

  private void showLogger(HttpServletRequest req, Logger logger, PrintStream pw) {
    pw.print(" logger = " + logger.getName() + " level= ");
    String url = req.getRequestURI() + "?setLogger=" + logger.getName() + "&level=";
    showLevel(url, Level.ALL, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.DEBUG, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.INFO, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.WARN, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.ERROR, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.FATAL, logger.getEffectiveLevel(), pw);
    showLevel(url, Level.OFF, logger.getEffectiveLevel(), pw);
    pw.println();

    Enumeration appenders = logger.getAllAppenders();
    while (appenders.hasMoreElements()) {
      Appender app = (Appender) appenders.nextElement();
      pw.println("  appender= " + app.getName() + " " + app.getClass().getName());
      Layout layout = app.getLayout();
      if (layout instanceof PatternLayout) {
        PatternLayout playout = (PatternLayout) layout;
        pw.println("    layout pattern= " + playout.getConversionPattern());
      }
      if (app instanceof AppenderSkeleton) {
        AppenderSkeleton skapp = (AppenderSkeleton) app;
        if (skapp.getThreshold() != null)
          pw.println("    threshold=" + skapp.getThreshold());
      }
      if (app instanceof FileAppender) {
        FileAppender fapp = (FileAppender) app;
        pw.println("    file=" + fapp.getFile());
      }
    }
  }

  private void showLevel(String baseUrl, Level show, Level current, PrintStream pw) {
    if (show.toInt() != current.toInt())
      pw.print(" <a href='" + baseUrl + show + "'>" + show + "</a>");
    else
      pw.print(" " + show);
  }


  private class LoggerComparator implements Comparator<Logger> {
    public int compare(Logger log1, Logger log2) {
      return log1.getName().compareTo(log2.getName());
    }

    public boolean equals(Object o) {
      return this == o;
    }
  }  */

}
