package main;

import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;
import sdloader.util.Browser;

public class FlexProjectSampleStartMain {

	public static void main(String[] args) {

		// �C���X�^���X��
		SDLoader loader = new SDLoader(8080);
		
		loader.setUseNoCacheMode(true);
		
		//loader.setLineSpeed(LineSpeed.ISDN_64K_BPS);
		
		// WebApp�ǉ� ���[�g�𕡐��w��
		loader.addWebAppContext(new WebAppContext("/sample", "../SDLoaderSampleSC2009Winter-Flex3/bin-debug","WebContent"));

		// �N��
		loader.start();
		
		Browser.open("http://localhost:"+loader.getPort()+"/sample/main.html");
	}
}
