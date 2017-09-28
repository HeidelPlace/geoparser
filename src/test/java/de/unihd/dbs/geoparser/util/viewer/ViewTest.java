package de.unihd.dbs.geoparser.util.viewer;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.gazetteer.viewer.GazetteerViewerResources;
import de.unihd.dbs.geoparser.gazetteer.viewer.ViewerContext;
import de.unihd.dbs.geoparser.gazetteer.viewer.controller.LoginViewController;

import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class ViewTest extends ApplicationTest {

	@Test
	public void testViewConstructorWithValidPath() throws IOException {
		final View<LoginViewController> view = new View<>(GazetteerViewerResources.MAIN_VIEW_FXML_FILE);
		assertThat(view.getRootNode().getClass(), equalTo(AnchorPane.class));
		assertThat(view.getController().getClass(), equalTo(LoginViewController.class));
	}

	@Test(expected = IOException.class)
	public void testViewConstructorWithInvalidPath() throws IOException {
		@SuppressWarnings("unused")
		final View<LoginViewController> view = new View<>("some path");
	}

	@Override
	public void start(final Stage stage) throws Exception {
		ViewerContext.getInstance().config = new GeoparserConfig();
	}

}
