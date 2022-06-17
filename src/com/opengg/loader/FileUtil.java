package com.opengg.loader;

import com.davidehrmann.vcdiff.VCDiffDecoder;
import com.davidehrmann.vcdiff.VCDiffDecoderBuilder;
import com.davidehrmann.vcdiff.VCDiffEncoderBuilder;
import com.opengg.core.Configuration;
import com.opengg.core.console.GGConsole;
import com.opengg.loader.loading.MapLoader;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileUtil {
    private static JFileChooser chooser = new JFileChooser();
   
    /**
     * Takes the diff between the source file and the edited file, writing to the given destination file.
     *
     * The diff can then be applied to the source file to retrieve the edited file.
     * @param source The base file
     * @param edited The modified file
     * @param destDiff The destination file the diff will be written to.
     */
    public static void diff(Path source, Path edited, Path destDiff) throws IOException {
        var dictionary = Files.readAllBytes(source);
        var uncompressed = Files.readAllBytes(edited);
        try(var outStream = new BufferedOutputStream(new FileOutputStream(destDiff.toString()))){
            var encoder = VCDiffEncoderBuilder.builder()
                    .withChecksum(true)
                    .withDictionary(dictionary)
                    .buildSimple();

            encoder.encode(uncompressed, outStream);
        }
    }

    /**
     * Creates a backup of the given map file in the user's temp directory.
     * @return The backup information needed to revert/delete the backup.
     */
    public static Backup createBackup(Project.MapInstance instance, MapLoader.MapFile file){
        try {
            var temp = Files.createTempFile(file.fileName().getFileName().toString(), "bkp");
            Files.copy(file.fileName(), temp, StandardCopyOption.REPLACE_EXISTING);

            return new Backup(instance, file, temp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Applies the given backup, reverting the changes made to the backed up file.
     *
     * This also deletes the backup.
     */
    public static void applyBackup(Backup backupData){
        try {
            var oldStream = backupData.file().channel();
            oldStream.close();

            Files.copy(backupData.backup(), backupData.file().fileName(), StandardCopyOption.REPLACE_EXISTING);
            Files.delete(backupData.backup());

            var newStream = FileChannel.open(backupData.file().fileName(), StandardOpenOption.READ, StandardOpenOption.WRITE);

            backupData.mapData().loadedFiles().remove(backupData.file());
            backupData.mapData().loadedFiles().add(new MapLoader.MapFile(backupData.file().fileName(), newStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Deletes the given backup without applying it.
     */
    public static void clearBackup(Backup backup){
        try {
            Files.delete(backup.backup());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Given a source file and a patch file, apply the patch to the source.
     */
    public static void patch(Path source, Path diff, Path destFile) throws IOException {
        var dictionary = Files.readAllBytes(source);
        var compressedData = Files.readAllBytes(diff);


        try(var outStream = new BufferedOutputStream(new FileOutputStream(destFile.toString()))){
            var decoder = new VCDiffDecoder(VCDiffDecoderBuilder.builder()
                    .withMaxTargetFileSize(2L * 1024L * 1024L * 1024L)
                    .withMaxTargetWindowSize(219073000)
                    .buildStreaming());

            decoder.decode(dictionary, compressedData, outStream);
        }
    }

    /**
     * Return the relative path between the source and subdirectory.
     * @return The relative path between the two paths, or the subpath if the two paths share no sections.
     */
    public static Path getRelativePath(Path source, Path sub){
        var path = source.toUri().relativize(sub.toUri()).getPath();
        return Path.of(path);
    }

    /**
     * Recursively generate a copy of the given directory using links.
     *
     * @param source The existing original directory to make a link tree from
     * @param dest The destination directory
     * @param isSymLink Whether or not the links should be symbolic. If they are symbolic, they may require
     * administrator permissions on Windows. If not symbolic (hard links) the destination must be in the same drive.
     * @param excludedExtensions A list of file extensions that should not be copied.
     */
    public static void generateLinkTree(Path source, Path dest, boolean isSymLink, String... excludedExtensions){
        try (var ds = Files.newDirectoryStream(source)) {
            for (var child : ds) {
                if(Files.isRegularFile(child)){
                    if(Arrays.stream(excludedExtensions).noneMatch(e -> child.toString().toLowerCase().endsWith(e))){
                        if(isSymLink){
                            Files.createSymbolicLink(dest.resolve(child.getFileName()), child);
                        }else{
                            Files.createLink(dest.resolve(child.getFileName()), child);
                        }
                    }
                }else if(Files.isDirectory(child)){
                    var newDir = dest.resolve(child.getFileName());
                    Files.createDirectories(newDir);
                    generateLinkTree(child, newDir, isSymLink, excludedExtensions);
                }
            }
        } catch (IOException e) {
            GGConsole.error("Failed creating symlink:" + e.getMessage());
        }
    }

    /**
     * Opens a save dialog.
     *
     * @param defaultPath The initial path to open this dialog to.
     * @param type The type of item that can be selected.
     * @param description The description to show to the user
     * @param folter A list of extension types that can be selected (eg. "exe", "bat")
     * @return The path selected by the user, if it exists.
     */
    public static Optional<Path> openSaveDialog(String defaultPath, LoadType type, String description, String... filter){
        if(Configuration.getBoolean("use-native-file-dialog")){
            var dialog = new FileDialog((Frame) null);
            dialog.setDirectory(defaultPath);
            dialog.setMode(FileDialog.SAVE);
            dialog.setVisible(true);
            return Optional.ofNullable(Path.of(dialog.getFile()));
        }else{
            //JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(switch (type){
                case DIRECTORY -> JFileChooser.DIRECTORIES_ONLY;
                case FILE -> JFileChooser.FILES_ONLY;
                case BOTH -> JFileChooser.FILES_AND_DIRECTORIES;
            });

            if(filter.length != 0){
                chooser.setFileFilter(new FileNameExtensionFilter(description, filter));
            }else{
                chooser.setDialogTitle(description);
            }

            if(defaultPath != null && !defaultPath.isEmpty())
                chooser.setCurrentDirectory(new File(defaultPath));


            int returnVal = chooser.showSaveDialog(BrickBench.CURRENT.window);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                return Optional.of(chooser.getSelectedFile().toPath());
            }else{
                return Optional.empty();
            }
        }
    }

    /**
     * Open a file selection dialog.
     *
     * @see FileUtil#openFileDialog(String, LoadType, String, boolean, String...)
     */
    public static Optional<Path> openFileDialog(String defaultPath, LoadType type, String description, String... filter){
        return openFileDialog(defaultPath, type, description, false, filter).map(p -> p.get(0));
    }

    /**
     * Open a file select dialog.
     *
     * This method gives the option to allow the user to input multiple items.
     * @param defaultPath The initial path to open this dialog to.
     * @param type The type of item that can be selected.
     * @param description The description to show to the user
     * @param allowMultiple Whether or not to allow multiple files to be selected.
     * @param filter A list of extension types that can be selected (eg. "exe", "bat")
     * @return The path selected by the user, if it exists.
     */
    public static Optional<List<Path>> openFileDialog(String defaultPath, LoadType type, String description, boolean allowMultiple, String... filter){
        if(Configuration.getBoolean("use-native-file-dialog")){
            var dialog = new FileDialog((Frame) null);
            dialog.setDirectory(defaultPath);
            dialog.setMode(FileDialog.LOAD);
            dialog.setVisible(true);
            dialog.setMultipleMode(allowMultiple);
            return Optional.ofNullable(dialog.getFiles()).map(fs -> Arrays.stream(fs).map(File::toPath).collect(Collectors.toList()));
        }else{
            //JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(switch (type){
                case DIRECTORY -> JFileChooser.DIRECTORIES_ONLY;
                case FILE -> JFileChooser.FILES_ONLY;
                case BOTH -> JFileChooser.FILES_AND_DIRECTORIES;
            });

            if(filter.length != 0){
                chooser.setFileFilter(new FileNameExtensionFilter(description, filter));
            }else{
                chooser.setDialogTitle(description);
            }

            if(defaultPath != null && !defaultPath.isEmpty())
                chooser.setCurrentDirectory(new File(defaultPath));

            chooser.setMultiSelectionEnabled(allowMultiple);

            int returnVal = chooser.showOpenDialog(BrickBench.CURRENT.window);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                if(allowMultiple){
                    return Optional.of(Arrays.stream(chooser.getSelectedFiles()).map(File::toPath).collect(Collectors.toList()));
                }else{
                    return Optional.of(List.of(chooser.getSelectedFile().toPath()));
                }
            }else{
                return Optional.empty();
            }
        }
    }

    public static void compressFolder(Path sourceDir, Path outputFile, List<Path> filesToSave) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(outputFile.toString()))) {
            compressDirectoryToZipFile(sourceDir.toUri(), sourceDir, zipOutputStream, filesToSave);
        }
    }

    private static void compressDirectoryToZipFile(URI basePath, Path dir, ZipOutputStream out, List<Path> filesToSave) throws IOException {
        var fileList = Files.list(dir).collect(Collectors.toList());
        for (var file : fileList) {
            if (Files.isDirectory(file)) {
                compressDirectoryToZipFile(basePath, file, out, filesToSave);
            } else {
                if (filesToSave == null) {
                    out.putNextEntry(new ZipEntry(basePath.relativize(file.toUri()).getPath()));
                    try (FileInputStream in = new FileInputStream(file.toFile())) {
                        IOUtils.copy(in, out);
                    }
                } else {
                    for (var save : filesToSave) {
                        if (file.equals(save)) {
                            out.putNextEntry(new ZipEntry(basePath.relativize(file.toUri()).getPath()));
                            try (FileInputStream in = new FileInputStream(file.toFile())) {
                                IOUtils.copy(in, out);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Unzip the given file into the given destination directory.
     */
    public static void unzip(String source, String dest) throws IOException {
        String fileBaseName = FilenameUtils.getBaseName(source);
        Path destFolderPath = Paths.get(dest);

        try (ZipFile zipFile = new ZipFile(new File(source), ZipFile.OPEN_READ, Charset.defaultCharset())){
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = destFolderPath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream in = zipFile.getInputStream(entry)){
                        try (OutputStream out = new FileOutputStream(entryPath.toFile())){
                            IOUtils.copy(in, out);
                        }
                    }
                }
            }
        }
    }

    public enum LoadType{FILE, DIRECTORY, BOTH}

    public record Backup(Project.MapInstance mapData, MapLoader.MapFile file, Path backup){}
}
