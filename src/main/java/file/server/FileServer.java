package file.server;

import com.sun.net.httpserver.HttpServer;
import file.handler.FileRequestHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class FileServer {
    public static void main(String[] args) throws IOException {
        int portNum = 8001;
        String workingDir = System.getProperty("user.dir");
        final ThreadFactory factory = Thread.ofVirtual().name("virtual-thread-", 0).factory();
        HttpServer fileServer = HttpServer.create();
        fileServer.setExecutor(Executors.newThreadPerTaskExecutor(factory));
        fileServer.createContext("/", new FileRequestHandler(workingDir));
        fileServer.bind(new InetSocketAddress(portNum), 0);
        fileServer.start();
        System.out.printf("Server started on port %d. Serving files from: %s%n", portNum, workingDir);
        System.out.printf("Access in browser: http://localhost:%d/%n", portNum);
    }
}
