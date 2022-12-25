package by.babanin.macappbundler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

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

    public static void copyDirectoryRecursively(String sourceDirectory, String targetDirectory) throws MojoExecutionException {
        try(Stream<Path> stream = Files.walk(Paths.get(sourceDirectory))) {
            Iterator<Path> iterator = stream.iterator();
            while(iterator.hasNext()) {
                Path source = iterator.next();
                Path target = Paths.get(targetDirectory, source.toString().substring(sourceDirectory.length()));
                copy(source, target);
            }
        }
        catch(IOException e) {
            throw new MojoExecutionException(
                    "Unexpected error while coping directory from \"" + sourceDirectory + "\" to \"" + targetDirectory + "\"", e);
        }
    }

    private static void copy(Path source, Path target) throws MojoExecutionException {
        try {
            Files.copy(source, target);
        }
        catch(IOException e) {
            throw new MojoExecutionException("Unexpected error while coping directory from \"" + source + "\" to \"" + target + "\"", e);
        }
    }
}
