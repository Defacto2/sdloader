package main;

import sdloader.SDLoader;
import sdloader.constants.LineSpeed;
import sdloader.javaee.WebAppContext;
import sdloader.util.Browser;

public class LineSpeedSampleMain {

	public static void main(String[] args) {

		// �C���X�^���X��
		SDLoader loader = new SDLoader(8080);
		loader.setUseNoCacheMode(true);
		loader.setAutoPortDetect(true);

		// ������x��ݒ�
		loader.setLineSpeed(LineSpeed.ISDN_64K_BPS);

		loader.addWebAppContext(new WebAppContext("/sample", "WebContent"));
		loader.start();
		Browser.open("http://localhost:" + loader.getPort()
				+ "/sample/nkb/index.html");
	}
}
