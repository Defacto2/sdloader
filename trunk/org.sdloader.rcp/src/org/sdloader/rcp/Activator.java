package org.sdloader.rcp;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import sdloader.SDLoader;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.sdloader.rcp";

	// The shared instance
	private static Activator plugin;

	private SDLoader sdLoader;
	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		//�v���O�C���̃p�X�̎擾�@�������Â��H
		//�����`���[�̏ꍇ�̓v���W�F�N�g�̃p�X�����H
		String pluginDir = Platform.resolve(plugin.getBundle().getEntry("/"))
				.toString();
		if (pluginDir.startsWith("file:/"))
			pluginDir = pluginDir.substring("file:/".length());
		
		//SDLoader�̃x�[�X�f�B���N�g����ݒ�B���̃p�X������webapps��ǂݍ��݋N������B
		System.setProperty(SDLoader.KEY_SDLOADER_HOME, pluginDir);
		
		//JSP�p�̃p�X�ꗗ�쐬�@ServletAPI�Ƃ��͂��̃p�X�Ŏw��
		String jspLibDir = ClassPathUtils.createArchiveClassPathString(pluginDir + "lib");
		if (jspLibDir != null)
			System.setProperty(SDLoader.SDLOADER_JSP_LIBPATH, jspLibDir);

		sdLoader = new SDLoader();
		sdLoader.open();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
		sdLoader.close();
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public SDLoader getSdLoader() {
		return sdLoader;
	}

}
