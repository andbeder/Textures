package com.beder.util;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class JavaFilePackager {

    // All extensions to include in the package
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".java",        // Java classes
        ".js",          // LWC / Aura controllers
        ".html",        // LWC templates
        ".css",         // styling
        ".cmp", ".app", // Aura components & apps
        ".evt",         // Aura events
        ".design",      // Aura design files
        ".svg",         // SVG resources
        ".xml"          // meta-XML, e.g. -meta.xml
    ));

    public static void main(String[] args) {
        // Use native look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        List<File> sourceFiles = new ArrayList<>();
        List<File> roots       = new ArrayList<>();

        // 1) Build list of roots & gather files from args or chooser:
        if (args.length > 0) {
            File input = new File(args[0]);
            if (input.isFile() && matchesAllowed(input)) {
                sourceFiles.add(input);
                roots.add(input.getParentFile());
            }
            else if (input.isDirectory()) {
                roots.add(input);
                gatherFiles(input, sourceFiles);
            }
        } else {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Files and/or Folders");
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setMultiSelectionEnabled(true);
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                System.exit(0);
            }
            for (File f : chooser.getSelectedFiles()) {
                if (!f.exists()) continue;
                if (f.isFile() && matchesAllowed(f)) {
                    sourceFiles.add(f);
                    roots.add(f.getParentFile());
                } else if (f.isDirectory()) {
                    roots.add(f);
                    gatherFiles(f, sourceFiles);
                }
            }
        }

        // remove duplicate roots, preserve insertion order
        Set<File> uniqueRoots = new LinkedHashSet<>(roots);
        roots.clear();
        roots.addAll(uniqueRoots);

        // 2) Determine output file
        File outputFile;
        if (args.length > 1) {
            outputFile = new File(args[1]);
        } else {
            JFileChooser saver = new JFileChooser();
            saver.setDialogTitle("Save Packaged File");
            saver.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (saver.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
                System.exit(0);
            }
            outputFile = saver.getSelectedFile();
        }

        // 3) Package files and write out result
        try {
            String packagedContent = packageFiles(sourceFiles, roots);
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                writer.print(packagedContent);
            }
            System.out.println("Packaged " + sourceFiles.size()
                               + " file(s) into " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error packaging files: " + e.getMessage());
        }
    }

    /**
     * Packages all files in the given list (with their respective roots) into a single
     * formatted string. Each file is preceded by its relative path (computed against
     * the deepest matching root) and enclosed in code fences.
     *
     * @param sourceFiles List of files to include in the package
     * @param roots       List of root directories used to compute relative paths
     * @return A single String containing the packaged content of all files
     * @throws IOException If reading any file’s contents fails
     */
    public static String packageFiles(List<File> sourceFiles, List<File> roots) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (File f : sourceFiles) {
            String relPath = computeRelativePath(f, roots);
            sb.append("**File: ").append(relPath).append("**\n");
            sb.append("```\n");
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                sb.append(line).append("\n");
            }
            sb.append("```\n\n");
        }

        return sb.toString();
    }

    /**
     * Convenience method for packaging everything under a single directory.
     * Recursively gathers all allowed files under 'dir', then calls packageFiles().
     *
     * @param dir Root directory to package
     * @return A single String containing the packaged content of all files under 'dir'
     * @throws IOException If reading any file’s contents fails
     */
    public static String packageFiles(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("Expected a directory, but received: " + dir.getAbsolutePath());
        }

        List<File> sourceFiles = new ArrayList<>();
        gatherFiles(dir, sourceFiles);
        List<File> roots = Collections.singletonList(dir);

        return packageFiles(sourceFiles, roots);
    }

    /** Returns true if this file’s name ends with one of the allowed extensions */
    private static boolean matchesAllowed(File f) {
        String name = f.getName().toLowerCase(Locale.ROOT);
        for (String ext : ALLOWED_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively scans dir for files whose extension is in ALLOWED_EXTENSIONS.
     */
    private static void gatherFiles(File dir, List<File> out) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        for (File f : kids) {
            if (f.isDirectory()) {
                gatherFiles(f, out);
            } else if (f.isFile() && matchesAllowed(f)) {
                out.add(f);
            }
        }
    }

    /**
     * Given a file and a list of root directories, pick the deepest root
     * that is a prefix of the file’s path, and relativize against it.
     * If none match, just return file.getName().
     */
    private static String computeRelativePath(File file, List<File> roots) {
        Path filePath = file.toPath().toAbsolutePath();
        Path bestRoot = null;
        int   bestDepth = -1;
        for (File r : roots) {
            Path rp = r.toPath().toAbsolutePath();
            if (filePath.startsWith(rp) && rp.getNameCount() > bestDepth) {
                bestDepth = rp.getNameCount();
                bestRoot  = rp;
            }
        }
        if (bestRoot != null) {
            return bestRoot.relativize(filePath).toString();
        } else {
            return file.getName();
        }
    }
}
