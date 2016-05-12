package yale;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import javax.jnlp.*;

public class FileCopy extends JFrame implements ActionListener, PropertyChangeListener {
    static BasicService basicService = null;

    private static final long serialVersionUID = 1L;
    public static final String DONE = "OK\n";

    private JTextField txtSource;
    private JTextField txtTarget;
    private JProgressBar progressAll;
    private JProgressBar progressCurrent;
    private JTextArea detailsBox;
    private JTextArea txtIdentifiers;
    private JButton btnCopy;
    private CopyTask task;
    private JPopupMenu popup;
    String source = "";
    String target = "";

    public FileCopy() {
        buildGUI();
    }

    private void buildGUI() {
        setTitle("BRBL File Transfer Utility");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (task != null) task.cancel(true);
                dispose();
                System.exit(0);
            }
        });

        // Add the menu:

        JMenuBar menuBar;
        JMenu menu;

        //Create the menu bar.
        menuBar = new JMenuBar();

        //Build the first menu.
        menu = new JMenu("Actions");
        menu.setMnemonic(KeyEvent.VK_A); //?
        menuBar.add(menu);

        // Create first menu item:
        final JMenuItem menuItem = new JMenuItem("About",
                KeyEvent.VK_T);
        //menuItem.setAccelerator(KeyStroke.getKeyStroke(
        // KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.addActionListener(new AboutDialogAction());
        menu.add(menuItem);


        // Create second menu item:
        final JMenuItem menuItem2 = new JMenuItem("Help",
                KeyEvent.VK_H);
        //menuItem2.setAccelerator(KeyStroke.getKeyStroke(
        //KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem2.addActionListener(new HelpAction());
        menu.add(menuItem2);

        menuBar.add(menu);
        setJMenuBar(menuBar);

        // end: add menu

        JLabel lblSource = new JLabel("Source: ");
        JLabel lblTarget = new JLabel("Target: ");
        txtSource = new JTextField(50);
        txtSource.setText("");
        txtTarget = new JTextField(50);
        txtTarget.setText("");

        JPanel buttonsPanel = new JPanel();

        // Select source button
        JButton selectSourceButton = new JButton("Browse Source Folder");
        selectSourceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    //System.out.println(selectedFile.getName());
                    source = selectedFile.getAbsolutePath();
                    txtSource.setText(source);

                }
            }
        });

        // Select target button:
        JButton selectTargetButton = new JButton("Browse Target Folder");
        selectTargetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    //System.out.println(selectedFile.getName());
                    target = selectedFile.getAbsolutePath();
                    txtTarget.setText(target);
                }
            }
        });

        buttonsPanel.add(selectSourceButton, BorderLayout.WEST);
        buttonsPanel.add(selectTargetButton, BorderLayout.EAST);

        JLabel lblProgressAll = new JLabel("Overall: ");
        JLabel lblProgressCurrent = new JLabel("Current File: ");
        progressAll = new JProgressBar(0, 100);
        progressAll.setStringPainted(true);
        progressCurrent = new JProgressBar(0, 100);
        progressCurrent.setStringPainted(true);
        detailsBox = new JTextArea(5, 50);
        detailsBox.setEditable(false);
        DefaultCaret caret = (DefaultCaret) detailsBox.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(detailsBox, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        txtIdentifiers = new JTextArea(5, 50);
        txtIdentifiers.setEditable(true);
        DefaultCaret caret2 = (DefaultCaret) txtIdentifiers.getCaret();
        caret2.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane2 = new JScrollPane(txtIdentifiers);

        btnCopy = new JButton("Copy");
        btnCopy.setFocusPainted(false);
        btnCopy.setEnabled(false);
        btnCopy.addActionListener(this);

        DocumentListener listener = new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                boolean bEnabled = txtSource.getText().length() > 0 && txtTarget.getText().length() > 0;
                btnCopy.setEnabled(bEnabled);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                boolean bEnabled = txtSource.getText().length() > 0 && txtTarget.getText().length() > 0;
                btnCopy.setEnabled(bEnabled);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };

        txtSource.getDocument().addDocumentListener(listener);
        txtTarget.getDocument().addDocumentListener(listener);

        JPanel contentPane = (JPanel) getContentPane();
        contentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel panInputLabels = new JPanel(new BorderLayout(0, 5));
        JPanel panInputFields = new JPanel(new BorderLayout(0, 5));
        JPanel panProgressLabels = new JPanel(new BorderLayout(0, 5));
        JPanel panProgressBars = new JPanel(new BorderLayout(0, 5));

        panInputLabels.add(lblSource, BorderLayout.NORTH);
        panInputLabels.add(lblTarget, BorderLayout.CENTER);
        panInputFields.add(txtSource, BorderLayout.NORTH);
        panInputFields.add(txtTarget, BorderLayout.CENTER);
        panProgressLabels.add(lblProgressAll, BorderLayout.NORTH);
        panProgressLabels.add(lblProgressCurrent, BorderLayout.CENTER);
        panProgressBars.add(progressAll, BorderLayout.NORTH);
        panProgressBars.add(progressCurrent, BorderLayout.CENTER);

        JPanel panInput = new JPanel(new BorderLayout(0, 5));
        panInput.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Path"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panProgress = new JPanel(new BorderLayout(0, 5));
        panProgress.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Progress"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        final JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Info"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        final JPanel panIds = new JPanel(new BorderLayout());
        panIds.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Identifiers"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panControls = new JPanel(new BorderLayout());
        panControls.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        panInput.add(panInputLabels, BorderLayout.LINE_START);
        panInput.add(panInputFields, BorderLayout.CENTER);
        panIds.add(scrollPane2, BorderLayout.CENTER);
        panProgress.add(panProgressLabels, BorderLayout.LINE_START);
        panProgress.add(panProgressBars, BorderLayout.CENTER);
        infoPanel.add(scrollPane, BorderLayout.CENTER);
        infoPanel.add(panProgress, BorderLayout.SOUTH);
        panControls.add(btnCopy, BorderLayout.CENTER);

        JPanel panUpper = new JPanel(new BorderLayout());
        panUpper.add(buttonsPanel, BorderLayout.NORTH);
        panUpper.add(panInput, BorderLayout.CENTER);

        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        contentPane.add(panUpper, BorderLayout.NORTH);
        contentPane.add(panIds, BorderLayout.CENTER);
        contentPane.add(infoPanel, BorderLayout.CENTER);
        contentPane.add(panControls, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("Copy".equals(btnCopy.getText())) {
            File source = new File(txtSource.getText());
            File target = new File(txtTarget.getText());

            if (!source.exists()) {
                JOptionPane.showMessageDialog(this, "The source file/directory does not exist!", "ERROR", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!target.exists() && source.isDirectory()) target.mkdirs();
            else {
                int option = JOptionPane.showConfirmDialog(this, "The target file/directory already exists, do you want to overwrite it?", "Overwrite the target", JOptionPane.YES_NO_OPTION);
                if (option != JOptionPane.YES_OPTION) return;
            }

            task = this.new CopyTask(source, target);
            task.addPropertyChangeListener(this);
            task.execute();

            btnCopy.setText("Cancel");
        } else if ("Cancel".equals(btnCopy.getText())) {
            task.cancel(true);
            btnCopy.setText("Copy");
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            progressAll.setValue(progress);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FileCopy().setVisible(true);
            }
        });
    }

    /**
     * Actual copying
     * <p/>
     * TODO could print number of files and total download size
     */
    class CopyTask extends SwingWorker<Void, Integer> {
        private File source;
        private File target;

        /**
         * Progress indicators
         */
        private long totalBytes = 0L;
        private long copiedBytes = 0L;

        private Map<File, File> filesTocopy = new HashMap<File, File>();

        public CopyTask(File source, File target) {
            this.source = source;
            this.target = target;
            progressAll.setValue(0);
            progressCurrent.setValue(0);
        }

        @Override
        public Void doInBackground() throws Exception {
            detailsBox.append("\n");
            retrieveTotalBytes(source); // used to calculate progress
            try {
                gather(source, target);
                copyFiles();
            } catch (IOException e) {
                e.printStackTrace();
                detailsBox.append("\n Error in copying one or more files: \n");
                detailsBox.append(e.getCause().toString());
            }
            detailsBox.append("\nOK\n");
            return null;
        }

        @Override
        public void process(List<Integer> chunks) {
            for (int i : chunks) {
                progressCurrent.setValue(i);
            }
        }

        @Override
        public void done() {
            setProgress(100);
            btnCopy.setText("Copy");
        }


        // Copy files from map
        private void copyFiles() {
            final ExecutorService pool = Executors.newFixedThreadPool(10);
            final Set<File> files = filesTocopy.keySet();
            for (File source : files) {
                final File target = filesTocopy.get(source);
                pool.submit(new DownloadTask(source, target));
            }

            System.out.println("Done with the pool");
            pool.shutdown();
            try {
                pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                System.out.println("Pool termination file complete");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * Computes total number of bytes         *
         */
        private void retrieveTotalBytes(final File sourceFile) {
            final File[] files = sourceFile.listFiles();

            if (files == null) {
                return;
            }

            for (final File file : files) {
                if (file.isDirectory()) {
                    retrieveTotalBytes(file);
                } else {
                    totalBytes += file.length();
                }
            }
        }

        /**
         * Makes directories and appends to pool
         */
        private void gather(final File sourceFile, final File targetFile) throws IOException {
            if (sourceFile.isDirectory()) {

                if (!targetFile.exists()) {
                    boolean success = targetFile.mkdirs();
                    System.out.println("Target file created: " + success);
                }

                final String[] filePaths = sourceFile.list();

                for (final String filePath : filePaths) {
                    final File srcFile = new File(sourceFile, filePath);
                    final File destFile = new File(targetFile, filePath);

                    /*if (srcFile.isDirectory()) {
                        if (srcFile.getName().equals(txtIdentifiers.getText())) {
                            System.out.println("Matched directory identifier");
                        } else {
                            detailsBox.append("Skipped:" + srcFile.getName() + "\n");
                            return;
                        }
                    } */

                    gather(srcFile, destFile);
                }
            } else { // add file object to map for later retreival
                filesTocopy.put(sourceFile, targetFile);
            }
        }
    }


    private static class DownloadTask implements Runnable {

        private File name;
        private final File toPath;

        public DownloadTask(File name, File toPath) {
            this.name = name;
            this.toPath = toPath;
        }

        @Override
        public void run() {
            try {
                fileCopy(name, toPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void fileCopy(final File sourceFile, final File targetFile) throws IOException{
            //detailsBox.append("Copying file " + sourceFile.getAbsolutePath() + " ... " + "\n");

            System.out.println("Copying file:" + sourceFile.getAbsolutePath());

            final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
            final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile));

            final long fileBytes = sourceFile.length();
            long soFar = 0L;

            int readByte;

            while ((readByte = bis.read()) != -1) {
                bos.write(readByte);

                //setProgress((int) (copiedBytes++ * 100 / totalBytes));
                //publish((int) (soFar++ * 100 / fileBytes));
            }

            bis.close();
            bos.close();

            //publish(100);

            System.out.println("Done with file:" + sourceFile.getAbsolutePath());
        }
    }

}

