package org.son.chat.common.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Date;
import java.util.Iterator;

import org.son.chat.common.net.core.AbstractISocketChannel;
import org.son.chat.common.net.core.IClientSocketService;
import org.son.chat.common.net.core.SocketChannelConfig;
import org.son.chat.common.net.handle.ISocketHandle;
import org.son.chat.common.net.util.NioUtil;

/**
 * @author solq
 */
public class ClientSocket extends AbstractISocketChannel implements IClientSocketService {

	public static ClientSocket valueOf(SocketChannelConfig socketChannelConfig) {
		ClientSocket clientSocket = new ClientSocket();
		clientSocket.socketChannelConfig = socketChannelConfig;
		clientSocket.socketHandle = new ISocketHandle() {

			@Override
			public void unRegister(AbstractSelectableChannel channel) {
				System.out.println("unRegister");
			}

			@Override
			public void register(AbstractSelectableChannel channel) {
				System.out.println("register");

			}

			@Override
			public void open(AbstractSelectableChannel channel) {
				System.out.println("open");

			}

			@Override
			public void close(AbstractSelectableChannel channel) {
				System.out.println("close");

			}
		};
		return clientSocket;
	}

	private Date heartbeatTime = new Date();
	private boolean connected = false;

	@Override
	public void start() {
		init();
		while (!close) {
			try {
				int n = selector.select(300);
				if (n < 0) {
					continue;
				}
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}

			for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext();) {
				// 得到下一个Key
				SelectionKey key = i.next();
				// 检查其是否还有效
				if (!key.isValid())
					continue;

				// 可能存在一次在channel 注册多个操作 不能用 elseif
				try {
					if (key.isAcceptable()) {
						handleAccept(key);
					}
					if (key.isConnectable()) {
						handleConnected(key);
					}
					if (key.isReadable()) {
						handleRead(key);
					}
					if (key.isWritable()) {
						handleWrite(key);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				i.remove();
			}
		}
		stop();
	}

	@Override
	public void stop() {
		if (selector != null && selector.isOpen()) {
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			selector = null;

			// 业务层通知
			unRegister();
			close();
		}
		close = false;
	}

	@Override
	public byte[] read() {
		return null;
	}

	@Override
	public void send(byte[] msg) {
		try {
			this.channel.write(ByteBuffer.wrap(msg));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init() {
		try {
			this.channel = SocketChannel.open(this.socketChannelConfig.getRemoteAddress());
			this.channel.configureBlocking(false);
			this.selector = Selector.open();
			this.connected = this.channel.isConnected();
			if (!this.connected) {
				// selectionKey().interestOps(SelectionKey.OP_CONNECT);
				// this.channel.register(this.selector, SelectionKey.OP_CONNECT);
			} else {
				while (!this.channel.finishConnect()) {
				}
				SelectionKey sk = this.channel.register(this.selector, 0);
				NioUtil.setOps(sk, SelectionKey.OP_READ);
				this.register();

				testWrite();
			}

			this.close = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void testWrite() throws IOException { 
		ByteBuffer buffer = ByteBuffer.allocate(1024 * 4);
		buffer.put("xxxxx".getBytes());
		buffer.flip();
		this.channel.write(buffer);
	}
}
