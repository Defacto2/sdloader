package sdloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import sdloader.command.Command;
import sdloader.command.CommandFactory;

public class CommandMonitor extends Thread {

	private int port = 0;

	private String commandKey = null;

	private ServerSocket serverSocket;

	private Lifecycle lifecycle;

	public CommandMonitor(final int port, final String commandKey,
			Lifecycle lifecycle) {
		try {
			if (port < 0) {
				return;
			}
			if (this.commandKey == null) {
				this.commandKey = generate();
			}
			this.port = port;
			this.commandKey = commandKey;
			setDaemon(true);
			setName("CommandMonitor");
			serverSocket = new ServerSocket(this.port, 1, InetAddress
					.getByName("127.0.0.1"));
			if (port == 0) {
				this.port = serverSocket.getLocalPort();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (serverSocket != null) {
			System.out.println("Monitor is working for " + commandKey
					+ ", port:" + port);
			this.lifecycle = lifecycle;
			this.start();
		}
	}

	public void run() {
		while (true) {
			Socket socket = null;
			try {
				socket = serverSocket.accept();
				InputStream is = socket.getInputStream();
				LineNumberReader reader = new LineNumberReader(
						new InputStreamReader(is));
				String key = reader.readLine();
				System.out.println("key : " + key);
				if (key == null) {
					continue;
				}
				Command command = CommandFactory.getCommand(key);
				if (command == Command.STOP) {
					synchronized (this) {
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						try {
							serverSocket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						try {
							lifecycle.stop();
						} finally {
							lifecycle = null;
						}
						System.out.println("stop command received.");
						System.exit(0);
					}
				} else if (command == Command.RESTART) {
					synchronized (this) {
						boolean isClosedNoramlly = false;
						try {
							lifecycle.stop();
							System.out
									.println("restart command received, so stop first.");
							isClosedNoramlly = true;
						} finally {
							if (isClosedNoramlly) {
								System.out.println("then start.");
								lifecycle.start();
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException ignore) {
					}
				}
				socket = null;
			}
		}
	}

	protected String generate() {
		return Long.toString((long) (Long.MAX_VALUE * Math.random()
				+ this.hashCode() + System.currentTimeMillis()), 36);
	}

	public static void monitor(final int port, final String commandKey,
			final Lifecycle lifecycle) {
		new CommandMonitor(port, commandKey, lifecycle);
	}
}
