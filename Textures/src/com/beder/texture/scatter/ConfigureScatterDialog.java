package com.beder.texture.scatter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigureScatterDialog extends JDialog {
    private final SpriteRepository repo;
    private final JPanel previewPanel;
    private final List<JTextField> weightFields;
    private final List<BufferedImage> loadedSprites;
    private final List<JPanel> spriteSlots;
    private JButton spriteSaveButton;

    public ConfigureScatterDialog(JFrame parent) {
        super(parent, "Configure Scatter Sprites", true);
        repo = SpriteRepository.getInstance();

        // Initialize collections and preview panel
        loadedSprites = new ArrayList<>();
        weightFields  = new ArrayList<>();
        spriteSlots   = new ArrayList<>();
        previewPanel  = new JPanel(new GridLayout(0, 4, 10, 10));
        JScrollPane scrollPane = new JScrollPane(previewPanel);

        // Preload existing sprites from repository
        for (int i = 0; i < repo.getCount(); i++) {
            BufferedImage img = repo.getSprite(i);
            int w = repo.getWeight(i);
            loadedSprites.add(img);
            JTextField weightField = new JTextField(String.valueOf(w), 3);
            weightFields.add(weightField);

            JPanel slot = buildSpriteSlot(img, weightField);
            spriteSlots.add(slot);
            previewPanel.add(slot);
        }

        // Controls
        JButton addButton    = new JButton("Add Sprite");
        JButton removeButton = new JButton("Remove Selected");
        spriteSaveButton          = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        addButton.addActionListener(e -> onAddSprites());
        removeButton.addActionListener(e -> onRemoveSprites());
        spriteSaveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());

        // Layout panels
        JPanel topPanel = new JPanel();
        topPanel.add(addButton);
        topPanel.add(removeButton);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(spriteSaveButton);
        bottomPanel.add(cancelButton);

        getContentPane().setLayout(new BorderLayout(10, 10));
        getContentPane().add(topPanel,    BorderLayout.NORTH);
        getContentPane().add(scrollPane,  BorderLayout.CENTER);
        getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(600, 400));
        pack();
        updateSaveState(); // ensure Apply button reflects sprite list
        setLocationRelativeTo(parent);
    }

    private JPanel buildSpriteSlot(BufferedImage img, JTextField weightField) {
        JPanel slot = new JPanel(new BorderLayout(5, 5));
        JLabel thumb = new JLabel(new ImageIcon(
            img.getScaledInstance(64, 64, Image.SCALE_SMOOTH)
        ));
        slot.add(thumb,       BorderLayout.CENTER);
        slot.add(weightField, BorderLayout.SOUTH);
        slot.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        slot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                boolean selected = slot.getBackground() == Color.LIGHT_GRAY;
                slot.setBackground(selected ? null : Color.LIGHT_GRAY);
            }
        });
        return slot;
    }

    private void onAddSprites() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image files", ImageIO.getReaderFileSuffixes()
        ));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    loadedSprites.add(img);
                    JTextField weightField = new JTextField("10", 3);
                    weightFields.add(weightField);

                    JPanel slot = buildSpriteSlot(img, weightField);
                    spriteSlots.add(slot);
                    previewPanel.add(slot);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to load: " + f.getName(),
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
            previewPanel.revalidate();
            previewPanel.repaint();
            updateSaveState();
        }
    }

    private void onRemoveSprites() {
        for (int i = spriteSlots.size() - 1; i >= 0; i--) {
            JPanel slot = spriteSlots.get(i);
            if (slot.getBackground() == Color.LIGHT_GRAY) {
                previewPanel.remove(slot);
                loadedSprites.remove(i);
                weightFields.remove(i);
                spriteSlots.remove(i);
            }
        }
        previewPanel.revalidate();
        previewPanel.repaint();
        updateSaveState();
    }

    private void updateSaveState() {
        spriteSaveButton.setEnabled(!loadedSprites.isEmpty());
    }

    private void onSave() {
        try {
            repo.clear();
            for (int i = 0; i < loadedSprites.size(); i++) {
                int w = Integer.parseInt(weightFields.get(i).getText());
                if (w < 1) throw new NumberFormatException();
                repo.addSprite(loadedSprites.get(i), w);
            }
            dispose();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                this,
                "All weights must be integers >= 1",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
