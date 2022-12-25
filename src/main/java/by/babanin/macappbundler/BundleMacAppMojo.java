package by.babanin.macappbundler;

import static by.babanin.macappbundler.PluginUtils.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.codehaus.plexus.velocity.VelocityComponent;

@Mojo(name = "bundle", defaultPhase = LifecyclePhase.PACKAGE)
public class BundleMacAppMojo extends AbstractMojo {

    private static final String APP_DIR_NAME_FORMAT = "%s.app";
    private static final String CONTENTS = "Contents";
    private static final String MAC_OS = "MacOS";
    private static final String RESOURCES = "Resources";
    private static final String JAVA = "Java";
    private static final String JRE = "jre";
    private static final String RUN_APP_SCRIPT_NAME = "run";
    private static final String JAVA_PATH = "Contents/Home/bin/java";
    private static final String INFO_FILE_NAME = "Info.plist";
    private static final String INFO_TEMPLATE_PATH = "by/babanin/macappbundler/Info.plist.template";
    private static final String DEFAULT_ICON_NAME = "GenericJavaApp.icns";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/${project.build.finalName}")
    private String buildPath;

    @Parameter(defaultValue = "${project.name}")
    private String name;

    @Parameter(required = true)
    private String jrePath;

    @Parameter(required = true)
    private List<FileSet> appJarSet;

    @Parameter
    private String iconPath;

    @Parameter(defaultValue = "${project.version}")
    private String version;

    @Component
    private VelocityComponent velocity;

    private Path buildDir;
    private Path appDir;
    private Path contentsDir;
    private Path macOsDir;
    private Path resourcesDir;
    private Path javaDir;
    private Path jreDir;
    private String appJarName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        initDirectoryStructure();
        copyJre();
        copyAppJar();
        genRunScript();
        copyIcon();
        genInfo();
    }

    private void initDirectoryStructure() throws MojoExecutionException {
        getLog().info("Creating directory structure");

        buildDir = Paths.get(buildPath);
        if(!Files.exists(buildDir)) {
            createDirectory(buildDir);
        }

        appDir = buildDir.resolve(String.format(APP_DIR_NAME_FORMAT, name));
        createDirectory(appDir);

        contentsDir = appDir.resolve(CONTENTS);
        createDirectory(contentsDir);

        macOsDir = contentsDir.resolve(MAC_OS);
        createDirectory(macOsDir);

        resourcesDir = contentsDir.resolve(RESOURCES);
        createDirectory(resourcesDir);

        javaDir = contentsDir.resolve(JAVA);
        createDirectory(javaDir);
    }

    private void copyJre() throws MojoExecutionException {
        getLog().info("Coping JRE");

        Path jreSource = Paths.get(jrePath);
        if(Files.exists(jreSource) && Files.isDirectory(jreSource)) {
            getLog().info("Copying the JRE from \"" + jreSource + "\" to \"" + javaDir + "\"");
            try {
                jreDir = javaDir.resolve(JRE);
                Files.copy(jreSource, jreDir);
            }
            catch(IOException e) {
                throw new MojoExecutionException("Unexpected error while coping JRE", e);
            }
        }
        else {
            throw new MojoExecutionException("JRE directory " + jreSource + " not found");
        }
    }

    private void copyAppJar() throws MojoExecutionException {
        getLog().info("Coping Application JAR file");

        if(appJarSet.size() == 1) {
            List<String> includes = appJarSet.get(0).getIncludes();
            if(includes.size() == 1) {
                appJarName = includes.get(0);
                Path appJar = Paths.get(appJarSet.get(0).getDirectory(), appJarName);
                if(Files.exists(appJar) && Files.isRegularFile(appJar)) {
                    try {
                        Files.copy(appJar, javaDir.resolve(appJarName));
                    }
                    catch(IOException e) {
                        throw new MojoExecutionException("Unexpected error while coping Application JAR", e);
                    }
                }
                else {
                    throw new MojoExecutionException("Application JAR \"" + appJar + "\" not found");
                }
            }
            else {
                throw new MojoExecutionException("Must have one included jar");
            }
        }
        else {
            throw new MojoExecutionException("Must have one file set");
        }
    }

    private void genRunScript() throws MojoExecutionException {
        getLog().info("Generating Application Run Script");

        Path runAppScriptFile = macOsDir.resolve(RUN_APP_SCRIPT_NAME);
        createFile(runAppScriptFile);
        List<String> script = new ArrayList<>();
        script.add("#!/bin/bash");
        script.add("cd \"$(dirname \"$0\")\"");
        Path javaPath = jreDir.resolve(JAVA_PATH);
        Path appJarPath = javaDir.resolve(appJarName);
        script.add("./" + javaPath + " -jar " + appJarPath);
        writeFile(runAppScriptFile, script);
        if(!SystemUtils.IS_OS_WINDOWS) {
            Set<PosixFilePermission> permissions = Arrays.stream(PosixFilePermission.values())
                    .collect(Collectors.toSet());
            try {
                Files.setPosixFilePermissions(runAppScriptFile, permissions);
            }
            catch(IOException e) {
                throw new MojoExecutionException("Unexpected error while set permissions", e);
            }
        }
        else {
            getLog().warn("The run script was created without executable file permissions for UNIX systems");
        }
    }

    private void copyIcon() throws MojoExecutionException {
        if(iconPath != null) {
            getLog().info("Coping icon");
            Path iconFile = Paths.get(project.getBasedir().getPath(), iconPath);
            if(Files.exists(iconFile) && Files.isRegularFile(iconFile)) {
                getLog().info("Coping icon from \"" + iconFile + "\"");
                try {
                    Files.copy(iconFile, resourcesDir.resolve(iconFile.getFileName()));
                }
                catch(IOException e) {
                    throw new MojoExecutionException("Unexpected error while coping icon", e);
                }
            }
            else {
                throw new MojoExecutionException("Icon not found");
            }
        }
    }

    private void genInfo() throws MojoExecutionException {
        getLog().info("Generating " + INFO_FILE_NAME);

        try {
            Velocity.init();
        } catch (Exception ex) {
            throw new MojoExecutionException("Unexpected error while initializing velocity", ex);
        }

        VelocityContext velocityContext = new VelocityContext();

        velocityContext.put("cfBundleExecutable", RUN_APP_SCRIPT_NAME);
        if (iconPath == null) {
            velocityContext.put("iconFile", DEFAULT_ICON_NAME);
        } else {
            Path fileName = Paths.get(iconPath).getFileName();
            velocityContext.put("iconFile", fileName);
        }
        velocityContext.put("name", name);
        velocityContext.put("version", version);

        Writer writer = null;
        try {
            Path infoFile = contentsDir.resolve(INFO_FILE_NAME);
            writer = new OutputStreamWriter(new FileOutputStream(infoFile.toFile()), StandardCharsets.UTF_8);
            velocity.getEngine().mergeTemplate(INFO_TEMPLATE_PATH, "UTF-8", velocityContext, writer);
        }
        catch(FileNotFoundException e) {
            throw new MojoExecutionException("Unexpected error while merging " + INFO_TEMPLATE_PATH, e);
        }
        finally {
            if(writer != null) {
                try {
                    writer.close();
                }
                catch(IOException e) {
                    throw new MojoExecutionException("Unexpected error while closing " + INFO_TEMPLATE_PATH, e);
                }
            }
        }
    }
}
