package net.sf.sahi.util;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;

public class SocketPool {
	private List unused = new LinkedList();
	private final int START_PORT = 13300;
	private int lastPort;

	public SocketPool(int size) {
		for (int i = 0; i < size; i++) {
			returnToPool(START_PORT + i);
		}
		lastPort = START_PORT + size;
	}

	private Socket createSocket(int port) throws IOException {
		Socket socket = new Socket();
		try {
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(port));
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return socket;
	}

	Socket get() throws IOException {
		Socket socket;
		int port;
		synchronized (unused) {
			while (unused.isEmpty()) {
				try {
					unused.wait();
				} catch (InterruptedException e) {
				}
			}
			port = ((Integer) unused.remove(0)).intValue();
		}
		socket = createSocket(port);
//		System.out.println("Get:     " + port);
		return socket;
	}

	public Socket get(String host, int port) throws IOException {
		Socket socket = get();
		try {
			socket.connect(new InetSocketAddress(host, port));
		} catch (BindException e) {
			lastPort++;
//			System.out.println("### Creating New Socket : "+lastPort);
			socket = createSocket(lastPort);
			socket.connect(new InetSocketAddress(host, port));
		}
		return socket;
	}

	public void release(Socket socket) {
		returnToPool(socket.getLocalPort());
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void returnToPool(int port) {
//		System.out.println("Added to Pool " + port);
		synchronized (unused) {
			unused.add(unused.size(), new Integer(port));
			unused.notifyAll();
		}
	}

}