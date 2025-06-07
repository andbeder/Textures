package com.beder.util;

import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class PngBackgroundStripper {
    private final static int LOWER_BOUND = 179;
    private final static int UPPER_BOUND = 220;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 1) Let user pick a directory
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select Folder of PNGs");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                JOptionPane.showMessageDialog(null, "No directory selected. Exiting.");
                return;
            }
            File dir = chooser.getSelectedFile();
            if (!dir.isDirectory()) {
                JOptionPane.showMessageDialog(null, "Selected file is not a directory.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 2) Gather all .png files
            File[] pngFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
            if (pngFiles == null || pngFiles.length == 0) {
                JOptionPane.showMessageDialog(null, "No PNG files found in:\n" + dir.getAbsolutePath());
                return;
            }

            // 3) Build a modal progress dialog
            final JDialog progressDialog = new JDialog((Frame) null, "Processing PNGs", true);
            final JProgressBar progressBar = new JProgressBar(0, pngFiles.length);
            progressBar.setStringPainted(true);
            progressDialog.getContentPane().add(progressBar);
            progressDialog.pack();
            progressDialog.setLocationRelativeTo(null);

            // 4) Use a SwingWorker to do the work in background
            SwingWorker<Void, Integer> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    // create a fixed‐size pool of 4 threads
                    ExecutorService executor = Executors.newFixedThreadPool(16);
                    ExecutorCompletionService<Void> completionService =
                        new ExecutorCompletionService<>(executor);

                    // submit one Callable per image
                    for (File pngFile : pngFiles) {
                        completionService.submit((Callable<Void>) () -> {
                            try {
                                BufferedImage img = ImageIO.read(pngFile);
                                if (img != null) {
                                    // ensure ARGB
                                    if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                                        BufferedImage tmp = new BufferedImage(
                                            img.getWidth(), img.getHeight(),
                                            BufferedImage.TYPE_INT_ARGB
                                        );
                                        Graphics2D g2 = tmp.createGraphics();
                                        g2.drawImage(img, 0, 0, null);
                                        g2.dispose();
                                        img = tmp;
                                    }

                                    // strip background
                                    for (int y = 0; y < img.getHeight(); y++) {
                                        for (int x = 0; x < img.getWidth(); x++) {
                                            int p = img.getRGB(x, y);
                                            int r = (p >> 16) & 0xFF;
                                            int g = (p >> 8) & 0xFF;
                                            int b = p & 0xFF;
                                            if (r >= LOWER_BOUND && r <= UPPER_BOUND &&
                                                g >= LOWER_BOUND && g <= UPPER_BOUND &&
                                                b >= LOWER_BOUND && b <= UPPER_BOUND) {
                                                img.setRGB(x, y, 0xFF339933);
                                            }
                                        }
                                    }

                                    ImageIO.write(img, "PNG", pngFile);
                                }
                            } catch (IOException e) {
                                // optionally log e.getMessage()
                            }
                            return null;
                        });
                    }

                    // as each finishes, update progress
                    int completed = 0;
                    while (completed < pngFiles.length && !isCancelled()) {
                        try {
                            Future<Void> f = completionService.take();
                            f.get();  // rethrow if processing failed
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (ExecutionException e) {
                            // optionally log e.getCause()
                        }
                        completed++;
                        publish(completed);
                    }

                    executor.shutdown();
                    return null;
                }

                @Override
                protected void process(java.util.List<Integer> chunks) {
                    int latest = chunks.get(chunks.size() - 1);
                    progressBar.setValue(latest);
                    progressBar.setString("Processed " + latest + " / " + pngFiles.length);
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    int count = 0;
                    try {
                        get(); // rethrow any exception from doInBackground
                        count = pngFiles.length;
                    } catch (Exception e) {
                        // if cancelled or failed partway, get the last published value
                        // but for simplicity we'll just report what the bar shows
                        count = progressBar.getValue();
                    }
                    JOptionPane.showMessageDialog(
                        null,
                        "Processed " + count + " PNG file" + (count == 1 ? "" : "s") +
                        "\nin " + chooser.getSelectedFile().getAbsolutePath(),
                        "Done",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            };

            // 5) Start background work and show the dialog
            worker.execute();
            progressDialog.setVisible(true);
        });
    }
}
