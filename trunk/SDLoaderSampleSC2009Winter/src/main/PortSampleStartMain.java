package main;

import java.net.Inet4Address;

import sdloader.SDLoader;
import sdloader.javaee.WebAppContext;
import sdloader.util.Browser;

public class PortSampleStartMain {

	public static void main(String[] args) throws Exception{

		// �C���X�^���X��
		SDLoader loader = new SDLoader(8080);
		
		//�����|�[�g�T�m���g�p
		loader.setAutoPortDetect(true);
		
		//�O���|�[�g���g�p
		loader.setUseOutSidePort(true);
		
		// WebApp�ǉ�
		loader.addWebAppContext(new WebAppContext("/sample", "WebContent"));

		// �N��
		loader.start();
		
		int port = loader.getPort();
		String ip = Inet4Address.getLocalHost().getHostAddress();
		Browser.open("http://" + ip +":"+port+"/sample/index.html");
	}
}
