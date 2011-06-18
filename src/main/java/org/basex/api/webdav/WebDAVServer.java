package org.basex.api.webdav;

import static org.basex.core.Text.*;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.basex.core.Main;
import org.basex.core.Prop;
import org.basex.server.Session;
import org.basex.util.Args;
import org.basex.util.Util;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.ServletRequest;
import com.bradmcevoy.http.ServletResponse;

/**
 * This is the starter class for running the WebDAV server. A database server
 * and the Jetty server is launched by the constructor. The Jetty server listens
 * for HTTP requests, which are then processed by the WebDAV implementation.
 * @author BaseX Team 2005-11, BSD License
 * @author Rositsa Shadura
 * @author Dimitar Popov
 */
public class WebDAVServer extends Main {
  /** Configuration: database user. */
  public static final String DBUSER = "org.basex.user";
  /** Configuration: database user password. */
  public static final String DBPASS = "org.basex.password";
  /** Configuration: database server host. */
  public static final String DBHOST = "org.basex.serverhost";
  /** Configuration: database server port. */
  public static final String DBPORT = "org.basex.serverport";
  /** Configuration: WebDAV server port. */
  public static final String WEBDAVPORT = "org.basex.webdavport";

  /** Usage. */
  private static final String WEBDAVINFO =
    " [-h][host] [-r][port] [-u][user] [-p][pass] [-w][webdavport]" + NL +
    "  -h<dbhost>  BaseX server host" + NL +
    "  -r<dbport>  BaseX server port" + NL +
    "  -u<dbuser>  BaseX user name" + NL +
    "  -p<dbpass>  BaseX user password" + NL +
    "  -w<webdavport>  WebDAV server port";

  /** HTTP server. */
  private final Server jetty;

  /**
   * Main method, launching the WebDAV implementation.
   * @param args command-line arguments
   */
  public static void main(final String[] args) {
    new WebDAVServer(args);
  }

  /**
   * Constructor.
   * @param args command-line arguments
   */
  public WebDAVServer(final String... args) {
    super(args);

    set(DBHOST, context.prop.get(Prop.HOST));
    set(DBPORT, Integer.toString(context.prop.num(Prop.SERVERPORT)));

    final HttpManager m = new HttpManager(new BXResourceFactory());
    final Handler h = new AbstractHandler() {
      @SuppressWarnings("unused")
      @Override
      public void handle(final String target, final HttpServletRequest request,
          final HttpServletResponse response, final int dispatch)
          throws IOException, ServletException {

        final ServletRequest req = new ServletRequest(request);
        final ServletResponse res = new ServletResponse(response);

        try {
          m.process(req, res);
        } finally {
          res.getOutputStream().flush();
          response.flushBuffer();
        }
      }
    };

    final String p = System.getProperty(WEBDAVPORT);
    final int port = p == null ?
        8985/* context.prop.num(Prop.WEBDAVPORT) */ : Integer.parseInt(p);
    jetty = new Server(port);
    jetty.setHandler(h);
    try {
      jetty.start();
    } catch(Exception ex) { Util.server(ex); }
  }

  /**
   * Store a configuration property as a system property.
   * @param k property key
   * @param v property value
   */
  private static void set(final String k, final String v) {
    if(System.getProperty(k) == null) System.setProperty(k, v);
  }

  @Override
  public void run() { }

  @Override
  protected Session session() { return null; }

  @Override
  protected boolean parseArguments(final String[] args) {
    final Args a = new Args(args, this, WEBDAVINFO);
    while(a.more()) {
      final char c = a.next();
      switch(c) {
        case 'h': set(DBHOST, a.string()); break;
        case 'r': set(DBPORT, a.string()); break;
        case 'u': set(DBUSER, a.string()); break;
        case 'p': set(DBPASS, a.string()); break;
        case 'w': set(WEBDAVPORT, a.string()); break;
        default: break;
      }
    }
    return true;
  }
}
