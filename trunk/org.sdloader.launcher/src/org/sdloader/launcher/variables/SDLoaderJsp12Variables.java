package org.sdloader.launcher.variables;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.sdloader.launcher.Activator;

public class SDLoaderJsp12Variables extends AbstractVariable {

	@Override
	protected URL getInstallLocation() {
		Bundle bundle = Activator.getDefault().getBundle();
		return bundle.getEntry("/lib/sdloader-jsp12.jar");
	}

}
