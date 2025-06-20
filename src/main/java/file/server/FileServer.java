package file.server;

import com.sun.net.httpserver.HttpServer;
import file.handler.FileRequestHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class FileServer {
    public static void main(String[] args) throws IOException {
        String workingDir = System.getProperty("user.dir");
        final ThreadFactory factory = Thread.ofVirtual().name("virtual-thread-", 0).factory();
        HttpServer fileServer = HttpServer.create();
        fileServer.setExecutor(Executors.newThreadPerTaskExecutor(factory));
        fileServer.createContext("/", new FileRequestHandler(workingDir));
        fileServer.bind(new InetSocketAddress(8001), 0);
        fileServer.start();
        System.out.println("Server started on port 8001. Serving files from: " + workingDir);
        System.out.println("Access in browser: http://localhost:8001/");
    }
}
