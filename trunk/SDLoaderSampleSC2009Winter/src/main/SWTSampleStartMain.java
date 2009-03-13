package main;

import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;
import sdloader.util.Browser;

public class SWTSampleStartMain {

	public static void main(String[] args) {

		// �C���X�^���X��
		SDLoader loader = new SDLoader(8080);
		
		//�����|�[�g�T�m���g�p
		loader.setAutoPortDetect(true);
		
		// WebApp�ǉ�
		loader.addWebAppContext(new WebAppContext("/sample", "WebContent"));

		// �N��
		loader.start();
		
		Browser.open("http://localhost:"+loader.getPort()+"/sample/swt.html");
	}
}
