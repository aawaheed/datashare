package org.icij.datashare;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public interface Deliverable extends Entity {
    enum Type {NLP, WEB, PLUGIN, UNKNOWN}
    URL getUrl();
    Path getBasePath();
    String getName();
    String getDescription();
    String getVersion();
    Type getType();
    File download() throws IOException;
    void install(File deliverable, Path targetDir) throws IOException;
    void install(Path targetDir) throws IOException; // install from local : url is the filesystem file
    void delete(Path installDir) throws IOException;
}
