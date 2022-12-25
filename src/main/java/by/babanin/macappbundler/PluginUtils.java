package by.babanin.macappbundler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;

public final class PluginUtils {

    private PluginUtils() {
    }

    public static void createDirectory(Path path) throws MojoExecutionException {
        try {
            Files.createDirectory(path);
        }
        catch(IOException e) {
            throw new MojoExecutionException("Unexpected error while creating \"" + path + "\" directory", e);
        }
    }

    public static void createFile(Path file) throws MojoExecutionException {
        try {
            Files.createFile(file);
        }
        catch(IOException e) {
            throw new MojoExecutionException("Unexpected error while creating \"" + file + "\" file", e);
        }
    }

    public static void writeFile(Path file,
            Iterable<? extends CharSequence> lines,
            OpenOption... options) throws MojoExecutionException {
        try {
            Files.write(file, lines, options);
        }
        catch(IOException e) {
            throw new MojoExecutionException("Unexpected error while writing \"" + file + "\" file", e);
        }
    }
}
