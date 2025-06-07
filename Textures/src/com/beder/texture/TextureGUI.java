package com.beder.texture;

import com.beder.texture.mask.CopyMask;
import com.beder.texture.noise.CellNoiseGenerator;
import com.beder.texture.noise.PerlinNoiseGenerator;
import com.beder.texture.noise.SimplexNoiseGenerator;
import com.beder.texture.noise.VegetationNoiseGenerator;
import com.beder.texture.noise.VoronoiNoiseGenerator;
import com.beder.texture.scatter.ConfigureScatterDialog;
import com.beder.texture.scatter.ScatterOperation;
import com.beder.texture.scatter.SpriteRepository;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

/** 
 * TextureGUI is responsible for building and displaying the Swing UI,
 * and delegates all control/logic operations to TextureGenius.
 */
public class TextureGUI implements Redrawable {
    private final TextureGenius genius;
    private final int res;
    private ImagePair curImage;

    protected JFrame frame;
    private JPanel mainPanel;
    private JPanel imagePanel;
    private JPanel opControlPanel;
    private ImageIcon leftIcon;
    private ImageIcon rightIcon;
    private JButton generateButton;
    private JButton saveButton;
    private JButton scatterButton;
    private JButton loadImagesButton;

    public TextureGUI(TextureGenius genius) {
        this.genius = genius;
        this.res = genius.getRes();
        this.curImage = genius.getCurrentImage();
    }

    /**
     * Initialize and display the UI.
     */
    public void init() {
        frame = new JFrame("Texture Generator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainPanel = new JPanel(new BorderLayout());

        // Center: image display with arrows
        imagePanel = new JPanel(new MigLayout("gapx 10px", "", "[center][center]"));
        leftIcon = new ImageIcon(curImage.left.getScaledInstance(512, 512, Image.SCALE_SMOOTH));
        rightIcon = new ImageIcon(curImage.right.getScaledInstance(512, 512, Image.SCALE_SMOOTH));
        imagePanel.add(new JLabel(leftIcon));

        JPanel arrowsPanel = new JPanel();
        arrowsPanel.setLayout(new BoxLayout(arrowsPanel, BoxLayout.Y_AXIS));
        JButton copyButton = new JButton("Copy →");
        JButton mixButton  = new JButton("Mix ↔");
        arrowsPanel.add(copyButton);
        arrowsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        arrowsPanel.add(mixButton);

        imagePanel.add(arrowsPanel);
        imagePanel.add(new JLabel(rightIcon));

        mainPanel.add(imagePanel, BorderLayout.CENTER);

        // East: operations stack panel from genius
        JPanel stackPanel = genius.getStackPanel();
        mainPanel.add(stackPanel, BorderLayout.EAST);

        // South: buttons to add new operations
        JPanel opPanel = new JPanel(new FlowLayout());
        JButton simplexButton   = new JButton("Simplex");
        JButton cellNoiseButton = new JButton("Cell Noise");
        JButton perlinButton = new JButton("Perlin");
        JButton voronoiButton = new JButton("Voronoi");
        JButton vegetationButton = new JButton("Vegetation");

        // New Scatter button, enabled only when sprites available
        scatterButton = new JButton("Scatter");

        // Configure Scatter dialog launcher
        loadImagesButton = new JButton("Load");
        loadImagesButton.addActionListener(e -> {
            ConfigureScatterDialog dialog = new ConfigureScatterDialog(frame);
            dialog.setVisible(true);
        });

        opPanel.add(simplexButton);
        opPanel.add(cellNoiseButton);
        opPanel.add(perlinButton);
        opPanel.add(voronoiButton);
        opPanel.add(vegetationButton);
        opPanel.add(scatterButton);
        mainPanel.add(opPanel, BorderLayout.SOUTH);

        // North: operation configuration panel
        opControlPanel = new JPanel(new FlowLayout());
        generateButton = new JButton("Generate");
        saveButton  = new JButton("Save");
        saveButton.addActionListener(e -> {
            // 1) Permanently apply the current operation in the stack
            ImagePair img = genius.saveCurrent();                                  // :contentReference[oaicite:0]{index=0}:contentReference[oaicite:1]{index=1}
            applyImage(img);

            // 2) Prompt the user for a file location
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Texture");
            chooser.setSelectedFile(new File("texture.png"));
            if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File base = chooser.getSelectedFile();
                String name = base.getName().replaceFirst("(\\.[^.]+)?$", "");
                File leftFile  = new File(base.getParentFile(), name + "_left.png");
                File rightFile = new File(base.getParentFile(), name + "_right.png");

                // 3) Write out both left/right images
                try {
                    ImageIO.write(curImage.left,  "png", leftFile);
                    ImageIO.write(curImage.right, "png", rightFile);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(
                        frame,
                        "Failed to save image: " + ex.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });
        mainPanel.add(opControlPanel, BorderLayout.NORTH);

        frame.getContentPane().add(mainPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // --- Action Listeners ---
        saveButton.addActionListener(e -> {
            ImagePair img = genius.saveCurrent();
            applyImage(img);
        });
        generateButton.addActionListener(e -> {
            ImagePair img = genius.applyCurrent();
            applyImage(img);
        });

        simplexButton.addActionListener(e -> addOperation(new SimplexNoiseGenerator(this)));
        cellNoiseButton.addActionListener(e -> addOperation(new CellNoiseGenerator(this)));
        perlinButton.addActionListener(e -> addOperation(new PerlinNoiseGenerator(this)));
        voronoiButton.addActionListener(e -> addOperation(new VoronoiNoiseGenerator(this)));
        vegetationButton.addActionListener(e -> addOperation(new VegetationNoiseGenerator(this)));
        scatterButton.addActionListener(e -> addOperation(new ScatterOperation(this)));
        copyButton.addActionListener(e -> addOperation(new CopyMask(this)));
        //mixButton.addActionListener(e -> addOperation(new MixMask(this)));
    }
    
    /****
     * Called by button action listeners to create a new operation (layer)
     * @param o
     */
    private void addOperation(Operation op) {
        if (genius.isClean()) {
            ImagePair img = genius.addOperation(op);
            applyImage(img);
            showOptions();
            frame.repaint();
        }
    }

    /**
     * Display the configuration controls for the currently selected operation.
     */
    public void showOptions() {
        opControlPanel.removeAll();
        Operation op = genius.getCurrentOperation();

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBorder(
            BorderFactory.createTitledBorder(op.getTitle() + " Options")
        );
        controlPanel.add(op.getConfig());

        if (op instanceof ScatterOperation) {
            // ① show the Configure-dialog launcher
            controlPanel.add(loadImagesButton);
            // ② disable Apply until at least one sprite is configured
            generateButton.setEnabled(
                SpriteRepository.getInstance().getCount() > 0
            );
        } else {
            generateButton.setEnabled(true);
        }

        controlPanel.add(generateButton);
        opControlPanel.add(controlPanel);
        opControlPanel.add(saveButton);
        opControlPanel.revalidate();
        opControlPanel.repaint();
    }

    /**
     * Update the displayed images based on the given ImagePair.
     */
    @Override
    public void applyImage(ImagePair current) {
        this.curImage = current;
        leftIcon.setImage(current.left.getScaledInstance(512, 512, Image.SCALE_SMOOTH));
        rightIcon.setImage(current.right.getScaledInstance(512, 512, Image.SCALE_SMOOTH));
        ((JLabel) imagePanel.getComponent(0)).setIcon(leftIcon); // FIX: reset JLabel icon
        ((JLabel) imagePanel.getComponent(2)).setIcon(rightIcon);
        imagePanel.revalidate();
        imagePanel.repaint();
    }    
    
    @Override
    public int getRes() {
        return res;
    }

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}
