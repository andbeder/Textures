package com.beder.util;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavaFileUnpackager {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFileChooser openChooser = new JFileChooser();
        openChooser.setDialogTitle("Select Packaged File (txt)");
        openChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (openChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            System.out.println("No packaged file selected. Exiting.");
            System.exit(0);
        }
        File packagedFile = openChooser.getSelectedFile();

        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setDialogTitle("Select Output Directory");
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (dirChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            System.out.println("No output directory selected. Exiting.");
            System.exit(0);
        }
        File outputDirectory = dirChooser.getSelectedFile();

        try (BufferedReader br = new BufferedReader(new FileReader(packagedFile))) {
            String line;
            String currentFilePath = null;
            StringBuilder contentBuf = new StringBuilder();
            boolean inCodeBlock = false;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("**File:") && line.endsWith("**")) {
                    // flush previous
                    if (currentFilePath != null && contentBuf.length() > 0) {
                        writeFile(outputDirectory, currentFilePath, contentBuf.toString());
                        contentBuf.setLength(0);
                    }
                    currentFilePath = line.substring("**File:".length(), line.length() - 2).trim();
                } else if (line.equals("```java")) {
                    inCodeBlock = true;
                } else if (line.equals("```") && inCodeBlock) {
                    inCodeBlock = false;
                } else if (inCodeBlock) {
                    contentBuf.append(line).append(System.lineSeparator());
                }
            }
            // final flush
            if (currentFilePath != null && contentBuf.length() > 0) {
                writeFile(outputDirectory, currentFilePath, contentBuf.toString());
            }
            System.out.println("Unpackaged Java files written to: " + outputDirectory);
        } catch (IOException e) {
            System.err.println("Error reading packaged file: " + e.getMessage());
        }
    }

    /**
     * Writes `content` into outputDirectory/<relativePath>,
     * dropping any absolute‐path root from originalPath.
     */
    private static void writeFile(File outputDirectory, String originalPath, String content) {
        // Normalize and turn into a Path
        Path orig = Paths.get(originalPath).normalize();
        Path rel;

        if (orig.isAbsolute()) {
            // drop the root (e.g. C:\) so we get only the path segments
            rel = orig.subpath(0, orig.getNameCount());
        } else {
            rel = orig;
        }

        File outputFile = new File(outputDirectory, rel.toString());
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Failed to create directories: " + parentDir.getAbsolutePath());
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writer.write(content);
            System.out.println("Written file: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing file " + outputFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }
}
