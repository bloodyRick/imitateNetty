package originClientServer;



import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author by woyuan  2023/10/23
 * nio不是基于流的，而是基于缓冲区的，用buffer缓冲区比用流每次处理一个字节要高效的多
 */
public class SimpleClient {
    public static void main(String[] args) throws IOException {

        Logger logger = Logger.getLogger(SimpleClient.class.getName());

        // 得到selector
        Selector selector = Selector.open();

        // 得到客户端的channel
        SocketChannel socketChannel = SocketChannel.open();

        // 设置非阻塞
        socketChannel.configureBlocking(false);

        // 将channel注册到selector上，可以获得key，但是不关心任何事件，直到有事件ready再去处理
        SelectionKey selectionKey = socketChannel.register(selector, 0);

        // 设置感兴趣的事件
        selectionKey.interestOps(SelectionKey.OP_CONNECT);

        // 开始去连接服务端
        socketChannel.connect(new InetSocketAddress(8080));

        // 不断轮询事件
        while (true) {
            // 无事件则阻塞
            selector.select();

            // 从select方法出来，意味着已经有感兴趣的事件到了，但还是在selector上
            Set<SelectionKey> selectionKeys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                // 如果是连接成功事件
                if (key.isConnectable()) {
                    // 而且本channel已经连接成功
                    if (socketChannel.finishConnect()) {
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        logger.info("已经注册了读事件！");

                        // 然后向服务端发送消息
                        socketChannel.write(ByteBuffer.wrap("client send msg to server".getBytes()));
                    }
                }

                // 如果是读事件，服务端发来的
                if (key.isReadable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    // 先分配字节来接收数据
                    ByteBuffer buffer = ByteBuffer.allocate(1204);
                    // 向buffer写入服务端传来的数据
                    int len = channel.read(buffer);
                    byte[] readByte = new byte[len];
                    // 用于将缓冲区从写模式切换到读模式。
                    buffer.flip();
                    buffer.get(readByte);
                    logger.info("读到来自服务端的数据：" + new String(readByte));
                }


            }


        }


    }
}
