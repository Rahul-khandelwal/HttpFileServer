package file.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class to handle HTTP requests.
 */
public class FileRequestHandler implements HttpHandler {
  private static final String GENERIC_HTML_RESPONSE = """
      <!DOCTYPE html>
      <html lang="en">
      <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Index of "%s"</title>
          <style>
              body { font-family: 'Inter', sans-serif; margin: 20px; background-color: #f4f7f6; color: #333; }
              .container { max-width: 800px; margin: auto; background-color: #fff; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); }
              h1 { color: #2c3e50; margin-bottom: 25px; border-bottom: 2px solid #e0e0e0; padding-bottom: 10px; }
              ul { list-style: none; padding: 0; }
              li { margin-bottom: 10px; padding: 8px 0; border-bottom: 1px dashed #eee; display: flex; align-items: center; }
              li:last-child { border-bottom: none; }
              a { text-decoration: none; color: #3498db; font-weight: bold; transition: color 0.2s ease-in-out; }
              a:hover { color: #2980b9; }
              .icon { margin-right: 10px; font-size: 1.2em; }
              .icon-folder:before { content: '📁'; }
              .icon-file:before { content: '📄'; }
          </style>
      </head>
      <body>
      <div class="container">
          <ul>
              %s
          </ul>
      </div>
      </body>
      </html>
      """;

  private final String workingDir;

  /**
   * Creates the instance for request handler.
   *
   * @param workingDir Directory to scan files.
   */
  public FileRequestHandler(String workingDir) {
    this.workingDir = workingDir;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    System.out.printf("Request for file {%s} on thread {%s}.%n",
        exchange.getRequestURI().getPath(),
        Thread.currentThread().getName());

    if ("GET".equals(exchange.getRequestMethod())) {
      handleGet(exchange);
    } else {
      var response = GENERIC_HTML_RESPONSE.formatted(exchange.getRequestURI().getPath(),
          "<li>Only GET requests are supported!</li>");
      sendResponse(exchange, 403, response);
    }
  }

  private void handleGet(HttpExchange exchange) throws IOException {
    var filePath = Paths.get(workingDir + exchange.getRequestURI().toString());

    if (!Files.exists(filePath)) {
      var response = GENERIC_HTML_RESPONSE.formatted(exchange.getRequestURI().getPath(),
          "<li>File path not present!</li>");
      sendResponse(exchange, 404, response);
      return;
    }

    if (Files.isDirectory(filePath)) {
      handleDirectoryListing(exchange, filePath);
    } else {
      handleFile(exchange, filePath);
    }
  }

  private void handleDirectoryListing(HttpExchange exchange, Path filePath) throws IOException {
    var links = new StringBuilder();
    var requestPath = exchange.getRequestURI().getPath();

    // Add a link to the parent directory unless it's the root directory
    if (!requestPath.equals("/")) {
      var parentPath = requestPath;
      if (parentPath.endsWith("/")) {
        parentPath = parentPath.substring(0, parentPath.length() - 1);
      }
      parentPath = parentPath.substring(0, parentPath.lastIndexOf('/') + 1);
      links.append("<li><span class=\"icon icon-folder\"></span><a href=\"")
          .append(parentPath).append("\">.. (Parent Directory)</a></li>");
    }

    try (var files = Files.list(filePath)) {
      files.forEach(path -> {
        var itemPath =
            (requestPath.endsWith("/") ? requestPath : requestPath + "/") + path.getFileName();
        links.append("""
            <li>
                <span class="icon %s"></span>
                <a href="%s%s">%s</a>
            </li>
            """.formatted(Files.isDirectory(path) ? "icon-folder" : "icon-file",
            itemPath,
            Files.isDirectory(path) ? "/" : "",
            path.getFileName()));
      });
    }

    var response = GENERIC_HTML_RESPONSE.formatted(requestPath, links.toString());
    sendResponse(exchange, 200, response);
  }

  private void handleFile(HttpExchange exchange, Path filePath) throws IOException {
    var contentType = Files.probeContentType(filePath);
    if (contentType == null) {
      contentType = "application/octet-stream";
    }

    exchange.getResponseHeaders().set("Content-Type", contentType);
    // Add Cache-Control to prevent caching issues which might lead to inline display
    exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
    exchange.getResponseHeaders().set("Pragma", "no-cache");
    exchange.getResponseHeaders().set("Expires", "0");
    // Add Content-Disposition header to force download
    // This tells the browser to download the file instead of displaying it inline
    var fileName = filePath.getFileName().toString();
    exchange.getResponseHeaders()
        .set("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

    exchange.sendResponseHeaders(200, Files.size(filePath));

    // Write file content to the response body
    try (var os = exchange.getResponseBody();
         var fis = new FileInputStream(filePath.toFile())) {
      var buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        os.write(buffer, 0, bytesRead);
      }
    }
    exchange.close();
  }

  private void sendResponse(HttpExchange exchange, int statusCode, String response)
      throws IOException {
    var responseBytes = response.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/html");
    exchange.sendResponseHeaders(statusCode, responseBytes.length);
    try (var os = exchange.getResponseBody()) {
      os.write(responseBytes);
    }
    exchange.close();
  }
}
