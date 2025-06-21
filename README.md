## Compiling the source code
javac -d out src/main/java/file/handler/** src/main/java/file/server/**

## Creating JAR file
jar --create --file fileserver.jar --main-class=file.server.FileServer -C out .

## Executing JAR file
java -jar fileserver.jar