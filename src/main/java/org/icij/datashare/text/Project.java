package org.icij.datashare.text;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.icij.datashare.Entity;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;
import static com.fasterxml.jackson.annotation.JsonCreator.Mode.PROPERTIES;


public class Project implements Entity {
    private static final long serialVersionUID = 2568979856231459L;

    public final String name;
    public final Path sourcePath;
    @JsonIgnore
    public final String allowFromMask;
    @JsonIgnore
    private final Pattern pattern;
    public final String label;
    public final String publisherName;
    public final String maintainerName;
    public final String logoUrl;
    public final String sourceUrl;
    public final Date creationDate;
    public final Date updateDate;


    @JsonCreator(mode = DELEGATING)
    public Project(String name) {
        this(name, Paths.get("/vault").resolve(name), "*");
    }
    @JsonCreator(mode = PROPERTIES)
    public Project( @JsonProperty("name") String name,
                    @JsonProperty("sourcePath") Path sourcePath) {
        this(name, sourcePath, "*");
    }

    public Project(String name, Path sourcePath, String allowFromMask) {
        this.name = name;
        this.label = name;
        this.sourcePath = sourcePath;
        this.sourceUrl = null;
        this.maintainerName = null;
        this.publisherName = null;
        this.logoUrl = null;
        this.creationDate = new Date();
        this.updateDate = new Date();
        this.allowFromMask = allowFromMask;
        this.pattern = Pattern.compile(allowFromMask.
                replace(".", "\\.").
                replace("*", "\\d{1,3}"));
    }

    public Project(String name,
                   String label,
                   Path sourcePath,
                   String sourceUrl,
                   String maintainerName,
                   String publisherName,
                   String logoUrl,
                   String allowFromMask,
                   Date creationDate,
                   Date updateDate) {
        this.name = name;
        this.label = label;
        this.sourcePath = sourcePath;
        this.sourceUrl = sourceUrl;
        this.maintainerName = maintainerName;
        this.publisherName = publisherName;
        this.logoUrl = logoUrl;
        this.allowFromMask = allowFromMask;
        this.creationDate = creationDate;
        this.updateDate = updateDate;
        this.pattern = Pattern.compile(allowFromMask.
                replace(".", "\\.").
                replace("*", "\\d{1,3}"));
    }

    public Project(String name, String allowFromMask) {
        this(name, Paths.get("/vault").resolve(name), allowFromMask);
    }

    public boolean isAllowed(final InetSocketAddress socketAddress) {
        return pattern.matcher(socketAddress.getAddress().getHostAddress()).matches();
    }

    public static boolean isAllowed(Project project, InetSocketAddress socketAddress) {
        return project == null || project.isAllowed(socketAddress);
    }

    @JsonIgnore
    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getMaintainerName() {
        return maintainerName;
    }

    public String getAllowFromMask() {
        return allowFromMask;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public static Project project(final String projectName) {
        return new Project(projectName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return name.equals(project.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() { return "Project{name='" + name + '\'' + '}';}
}
