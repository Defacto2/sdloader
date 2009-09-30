package sdloader.javaee;

import sdloader.SDLoader;
import junit.framework.TestCase;

public class WebAppManagerTest extends TestCase {

	public void testFindWebApp(){
		
		SDLoader loader = new SDLoader();
		loader.addWebAppContext(new WebAppContext("/hoge","test"));
		loader.addWebAppContext(new WebAppContext("/hogehoge","test"));
		loader.addWebAppContext(new WebAppContext("/hogehoge/foo","test"));
		loader.start();
		WebAppManager manager = loader.getWebAppManager();
		
		assertEquals("/hoge",manager.findWebApp("/hoge/test").getContextPath());
		assertEquals(null,manager.findWebApp("/hoge2"));
		assertEquals(null,manager.findWebApp("/hog"));
		assertEquals("/hogehoge",manager.findWebApp("/hogehoge").getContextPath());
		assertEquals("/hogehoge",manager.findWebApp("/hogehoge/").getContextPath());
		assertEquals("/hogehoge/foo",manager.findWebApp("/hogehoge/foo").getContextPath());
		assertEquals("/hogehoge/foo",manager.findWebApp("/hogehoge/foo/test").getContextPath());
	}
}