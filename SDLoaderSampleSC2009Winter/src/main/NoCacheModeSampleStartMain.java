package main;

import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;
import sdloader.util.Browser;

public class NoCacheModeSampleStartMain {

	public static void main(String[] args) {

		// �C���X�^���X��
		SDLoader loader = new SDLoader(8080);
		
		//�����|�[�g�T�m���g�p
		loader.setAutoPortDetect(true);
		
		//No-Cache���[�h���g�p
		loader.setUseNoCacheMode(true);
		
		// WebApp�ǉ�
		loader.addWebAppContext(new WebAppContext("/sample", "WebContent"));

		// �N��
		loader.start();
		
		Browser.open("http://localhost:"+loader.getPort()+"/sample/index.html");
	}
}
