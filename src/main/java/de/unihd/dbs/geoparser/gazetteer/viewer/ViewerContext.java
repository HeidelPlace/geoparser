package de.unihd.dbs.geoparser.gazetteer.viewer;

import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;
import de.unihd.dbs.geoparser.gazetteer.viewer.controller.LoginViewController;

/**
 * Central storage for global variables that are shared by controllers used in the {@link GazetteerViewerApp}, i.e., the
 * context of the application.
 * <p>
 * {@link ViewerContext} is implemented using the singleton pattern, i.e., only a single instance of the class
 * may exist. It can be accessed via {@link ViewerContext#getInstance()}.
 * <p>
 * Centralizing the variables here has several reasons. First, it allows to share state across different controllers
 * working in concert to run the {@link GazetteerViewerApp}. Second, it enables stubbing behavior in Unit-Tests. Third,
 * this design forms a basis for Context and Dependency Injection (CDI) tools like Google Guice. This technology has not
 * yet been used in this project, but might be an option in future releases...
 *
 * @author lrichter
 *
 */
public final class ViewerContext {

	private static ViewerContext instance;

	private ViewerContext() {
		// avoid external instantiation
	}

	/**
	 * Get the Singleton instance of {@link ViewerContext} (lazy loaded).
	 *
	 * @return the {@link ViewerContext} singelton instance.
	 */
	public static synchronized ViewerContext getInstance() {
		if (instance == null) {
			instance = new ViewerContext();
		}
		return instance;
	}

	/**
	 * The current geoparser configuration.
	 */
	public GeoparserConfig config;

	/**
	 * The connection to the gazetteer. Normally initialized by {@link LoginViewController}.
	 */
	public Gazetteer gazetteer;

	/**
	 * The JPA Persistence manager. Normally initialized by {@link LoginViewController}.
	 * <p>
	 * Ensure correct resource cleanup at the end of the application life-time! This is normally done by
	 * {@link GazetteerViewerApp}.
	 */
	public GazetteerPersistenceManager gazetteerPersistenceManager;

}
