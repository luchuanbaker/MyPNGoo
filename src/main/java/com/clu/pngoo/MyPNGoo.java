package com.clu.pngoo;

import com.clu.util.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class MyPNGoo extends JFrame {

    private static final String exePath;

    private static Throwable t = null;

    static {
        String path = null;
        try {
            String tmpPath = System.getenv("tmp").split(";")[0];
            String fileName = "pngquanti.exe";
            File destFile = new File(tmpPath, fileName);
            if (!destFile.exists()) {
                URL url = MyPNGoo.class.getResource("/com/clu/pngoo/pngquanti/" + fileName);
                InputStream inputStream = url.openStream();
                FileOutputStream outputStream = new FileOutputStream(destFile);
                byte[] buff = new byte[1024];
                int len = -1;
                while ((len = inputStream.read(buff)) != -1) {
                    outputStream.write(buff, 0, len);
                }
                inputStream.close();
                outputStream.close();
            }
            path = destFile.getAbsolutePath();
        } catch (Throwable e) {
            t = e;
        }
        exePath = path;
    }

    private final Vector<Vector> dataVector1;
    private final DefaultTableModel tableModel;
    private JPanel rootPanel;
    private JComboBox comboBox1;
    private JScrollPane scrollPane1;
    private JTable table1;
    private JButton btnRemove;
    private JButton btnAdd;
    private JButton btnGo;
    private JButton btnClear;

    private static final String STATUS_COLUMN = "??????";

    private static final List<String> HEADER_LIST = Arrays.asList("??????", "??????", "????????????", "???????????????", STATUS_COLUMN);

    private static final Font FONT20 = new Font("????????????", Font.BOLD, 20);
    private static final Font FONT14 = new Font("????????????", Font.PLAIN, 17);

    public static void main(String[] args) {
        if (t != null) {
            showError("???????????????", t);
            System.exit(-1);
        }
        MyPNGoo frame = new MyPNGoo("MyPNGoo");
        frame.setVisible(true);
    }

    private FileFilter fileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().toLowerCase().endsWith(".png");
        }
    };

    private List<JButton> getAllButtons() {
        return Arrays.asList(this.btnRemove, this.btnClear, this.btnAdd, this.btnGo);
    }

    private int getStatusColumnIndex() {
        return HEADER_LIST.indexOf(STATUS_COLUMN);
    }

    @SuppressWarnings("unchecked")
    public MyPNGoo(String name) {
        super(name);
        this.setContentPane(this.rootPanel);
        JTableHeader tableHeader = this.table1.getTableHeader();
        tableHeader.setFont(FONT20);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setSize(1280, 768);
        this.setLocationRelativeTo(null);

        // ???????????????????????????(128)
        this.comboBox1.setSelectedIndex(this.comboBox1.getItemCount() - 2);

        // ?????????????????????????????????????????????????????????JOptionPane
        // initGlobalFontSetting(FONT20);
        // ??????JOptionPane???????????????
        UIManager.put("OptionPane.buttonFont", FONT20);
        UIManager.put("OptionPane.messageFont", FONT14);

        this.tableModel = new DefaultTableModel(null, HEADER_LIST.toArray(new String[0])) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.dataVector1 = (Vector<Vector>) tableModel.getDataVector();
        this.table1.setModel(tableModel);

        TableColumnModel columnModel = tableHeader.getColumnModel();
        IntStream.range(0, columnModel.getColumnCount()).forEach(i -> {
            TableColumn column = columnModel.getColumn(i);
            column.setPreferredWidth(i <= 1 ? 300 : 80);
        });

        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                        Transferable transferable = dtde.getTransferable();
                        Iterable<File> list = (Iterable<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        for (File file : list) {
                            addRow(file);
                        }
                        dtde.dropComplete(true);
                        // ??????
                        MyPNGoo.this.table1.updateUI();
                        MyPNGoo.this.adjustTableColumns();
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (IOException | UnsupportedFlavorException ioe) {
                    ioe.printStackTrace();
                }
            }
        });

        for (JButton button : this.getAllButtons()) {
            button.setFont(FONT20);
        }
        btnAdd.addActionListener(e -> {
            FileDialog fileDialog = new FileDialog(MyPNGoo.this);
            fileDialog.setTitle("??????png");
            fileDialog.setMode(FileDialog.LOAD);
            fileDialog.setMultipleMode(true);
            fileDialog.setVisible(true);
            File[] files = fileDialog.getFiles();
            if (!ArrayUtils.isEmpty(files)) {
                for (File file : files) {
                    addRow(file);
                }
                this.table1.addNotify();
            }
        });
        btnRemove.addActionListener(e -> {
            int[] selectedRows = this.table1.getSelectedRows();
            if (!ArrayUtils.isEmpty(selectedRows)) {
                IntStream.of(selectedRows)
                    .boxed()
                    .sorted(Comparator.<Integer>naturalOrder().reversed())
                    .forEach(this.tableModel::removeRow);
            }
        });
        btnClear.addActionListener(e -> {
            this.dataVector1.clear();
            this.table1.addNotify();
        });
        btnGo.addActionListener(e -> {
            if (this.dataVector1.isEmpty()) {
                JOptionPane.showMessageDialog(this, "??????????????????");
            } else {
                List<Map.Entry<Integer, File>> files = new ArrayList<>();
                for (int i = 0; i < this.dataVector1.size(); i++) {
                    Vector vector = this.dataVector1.get(i);
                    files.add(new AbstractMap.SimpleEntry<>(i, new File((String) vector.get(1))));
                }
                doProcess(files);
            }
        });
        table1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int statusColumnIndex = getStatusColumnIndex();
                if (table1.getSelectedColumn() == statusColumnIndex) {
                    JOptionPane.showMessageDialog(null, table1.getValueAt(table1.getSelectedRow(), statusColumnIndex));
                }
            }
        });
    }

    private static String formatSize(Long size) {
        if (size == null) {
            return null;
        }
        return String.format("%.2fK", size / 1024f);
    }

    private void addRow(File file) {
        if (fileFilter.accept(file)) {
            addRow(new Object[]{file.getName(), file.getAbsolutePath(), formatSize(file.length()), null, "?????????"});
        }
    }

//    public static void initGlobalFontSetting(Font fnt) {
//        FontUIResource fontRes = new FontUIResource(fnt);
//        for (Enumeration keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
//            Object key = keys.nextElement();
//            Object value = UIManager.get(key);
//            if (value instanceof FontUIResource) {
//                UIManager.put(key, fontRes);
//            }
//        }
//    }

    private void addRow(Object[] row) {
        this.dataVector1.add(toRow(row));
    }

    private void addRows(Object[][] rows) {
        this.dataVector1.addAll(toRows(rows));
    }

    private void adjustTableColumn(JTable myTable, JTableHeader header, TableColumn column) {
        int col = header.getColumnModel().getColumnIndex(column.getIdentifier());
        int width = (int) myTable.getTableHeader().getDefaultRenderer()
            .getTableCellRendererComponent(myTable, column.getIdentifier()
                , false, false, -1, col).getPreferredSize().getWidth();
        for (int row = 0; row < myTable.getRowCount(); row++) {
            int preferredWidth = (int) myTable.getCellRenderer(row, col).getTableCellRendererComponent(myTable,
                myTable.getValueAt(row, col), false, false, row, col).getPreferredSize().getWidth();
            width = Math.max(width, preferredWidth);
        }
        header.setResizingColumn(column); // ???????????????
        column.setWidth(width + myTable.getIntercellSpacing().width);
    }

    public void adjustTableColumns() {
        JTable myTable = this.table1;
        JTableHeader header = myTable.getTableHeader();
//        Enumeration columns = myTable.getColumnModel().getColumns();
//        while (columns.hasMoreElements()) {
//            TableColumn column = (TableColumn) columns.nextElement();
//            adjustTableColumn(myTable, header, column);
//        }
        adjustTableColumn(myTable, header, myTable.getColumnModel().getColumn(0));
    }

    private Vector<Object> toRow(Object[] anArray) {
        if (anArray == null) {
            return null;
        }

        Vector<Object> v = new Vector<Object>(anArray.length);
        for (Object o : anArray) {
            v.addElement(o);
        }
        for (int i = 0; i < HEADER_LIST.size() - anArray.length; i++) {
            v.addElement(null);
        }
        return v;
    }

    private Vector<Vector<Object>> toRows(Object[][] anArray) {
        if (anArray == null) {
            return null;
        }
        Vector<Vector<Object>> v = new Vector<>(anArray.length);
        for (Object[] o : anArray) {
            v.addElement(toRow(o));
        }
        return v;
    }

    private ExecutorService executorService = Executors.newFixedThreadPool(20);

    private Map.Entry<Integer, String> processOne(File file) throws IOException, InterruptedException {
        // pngquanti.exe 64 "D:\Program Files (x86)\??????PNGoo\libs\pngquanti\my-test.png" --ext .png --force --speed 3
        String extName = ".mypngoo.png";

        Object selectedItem = this.comboBox1.getSelectedItem();

        if (selectedItem == null) {
            throw new IllegalArgumentException("????????????Color");
        }

        String cmd = StringUtils.format(
            // --ext .png -- force ????????????????????????
            "\"{}\" {} \"{}\" --ext {} --force",
            exePath, selectedItem, file.getAbsolutePath(), extName
        );



        ProcessBuilder processBuilder = new ProcessBuilder(
            exePath, selectedItem.toString(), file.getAbsolutePath(),
            "--ext", extName, "--force"
        ).redirectErrorStream(true);
        Process process = processBuilder.start();

        List<String> rows;
        try (InputStream inputStream = process.getInputStream()) {
            rows = IOUtils.readLines(inputStream, "UTF-8");
        }

        // ??????????????????
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            rows = Collections.singletonList("??????");

            String fileName = file.getName();
            File newFile = new File(file.getParentFile(), fileName.substring(0, fileName.length() - ".png".length()/*????????????*/) + extName);
            // ??????????????????????????????????????????????????????
            long newSize = FileUtils.sizeOf(newFile);
            long oldSize = FileUtils.sizeOf(file);
            if (newSize < oldSize) {
                // ???????????????????????????????????????
                FileUtils.deleteQuietly(file);
                FileUtils.moveFile(newFile, file);
            } else {
                // ??????????????????
                FileUtils.deleteQuietly(newFile);
                rows = Collections.singletonList(newSize > oldSize ? "???????????????" : "?????????????????????");
            }
        }

        return new AbstractMap.SimpleEntry<>(exitCode, StringUtils.join(rows, "\n"));
    }

    private synchronized void doProcess(List<Map.Entry<Integer, File>> files) {
        List<JButton> buttons = this.getAllButtons();
        for (JButton button : buttons) {
            button.setEnabled(false);
        }
        new Thread(() -> {
            CountDownLatch countDownLatch = new CountDownLatch(files.size());
            try {
                for (Map.Entry<Integer, File> entry : files) {
                    executorService.submit(() -> {
                        Integer rowIndex = entry.getKey();
                        try {
                            updateRow(rowIndex, "?????????..");
                            File file = entry.getValue();
                            Map.Entry<Integer, String> result = processOne(file);
                            String resultMsg = result.getValue();
                            if (result.getKey() == 0) {
                                updateRow(rowIndex, file.length(), resultMsg);
                            } else {
                                updateRow(rowIndex, resultMsg);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            updateRow(rowIndex, StringUtils.format("???????????????{}", getException(e)));
                        } finally {
                            try {
                                countDownLatch.countDown();
                            } catch (Exception e) {
                                showError("??????", e);
                            }

                        }
                    });
                }
            } finally {
                try {
                    countDownLatch.await();
                } catch (Exception e) {
                    showError("????????????", e);
                } finally {
                    for (JButton button : buttons) {
                        button.setEnabled(true);
                    }
                }
            }
        }).start();
    }

    private void updateRow(int rowIndex, String msg) {
        updateRow(rowIndex, null, msg);
    }

    private void updateRow(int rowIndex, Long newSize, String msg) {
        if (rowIndex >= 0 && rowIndex < this.dataVector1.size()) {
            // ??????????????????
            @SuppressWarnings("unchecked")
            Vector<Object> row = (Vector<Object>) this.dataVector1.get(rowIndex);
            // ???????????????????????????
            row.set(3, formatSize(newSize));
            // ??????
            row.set(this.getStatusColumnIndex(), msg);
            this.table1.addNotify();
        }
    }


    private static void showError(String message, Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(null, StringUtils.format("{}:\n{}", message, getException(t)));
    }

    private static String getException(Throwable t) {
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
