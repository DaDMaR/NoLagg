package com.bergerkiller.bukkit.nolagg.examine.reader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    public final int selectionWidth = 320;
    public final int selectionHeight = 310;
    final int graphwidth = 600;
    final int yoffset = 40;
    public JButton exportbutton;
    public JTextField filepath;
    public JTextArea description;
    private GraphBox ticktimes1;
    private SelectionBox selection;
    private JScrollPane locscroll;
    private JPopupMenu descriptionMenu;

    public MainWindow() {
        this(870, 572);
    }

    public MainWindow(int width, int height) {
        this(width, height, Toolkit.getDefaultToolkit().getScreenSize());
    }

    private MainWindow(int width, int height, Dimension screensize) {
        this((screensize.width - width) / 2, (screensize.height - height) / 2, width, height);
    }

    public MainWindow(int x, int y, int width, int height) {
        super();
        this.setBounds(x, y, width, height);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLayout(null);
        this.init();
        this.setTitle("NoLagg examination file reader by Bergerkiller");
        this.setVisible(true);
    }

    public <T extends Component> T append(T component) {
        super.add(component);
        return component;
    }

    public GraphArea add(String name, double totalduration) {
        GraphArea ga = this.ticktimes1.addArea();
        this.selection.add(name, ga.color, totalduration);
        return ga;
    }

    public void onClick(int index) {
        ExamReader.loadSegment(index);
    }

    public void reset(int newduration) {
        this.ticktimes1.reset(newduration);
        this.selection.clear();
    }

    public void orderAreas() {
        this.ticktimes1.orderAreas();
    }

    private void init() {
        final MainWindow main = this;

        this.selection = this.append(new SelectionBox(graphwidth + 10, yoffset, selectionWidth, selectionHeight) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSelectionChange(int index) {
                main.ticktimes1.setSelection(index);
            }

            @Override
            public void onItemClick(int index) {
                main.onClick(index);
            }
        });

        this.ticktimes1 = this.append(new GraphBox(5, yoffset, graphwidth, 500) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSelectionChange(GraphArea newarea) {
                if (newarea == null) {
                    main.selection.setSelection(-1);
                } else {
                    main.selection.setSelection(newarea.index);
                }
            }

            @Override
            public void onAreaClick(GraphArea area) {
                main.onClick(area == null ? -1 : area.index);
            }

        });

        // Export button
        exportbutton = append(new JButton());
        exportbutton.setText("Export");
        exportbutton.setBounds(main.getWidth() - 121, 5, 100, 30);

        this.getContentPane().addHierarchyBoundsListener(new HierarchyBoundsListener() {
            public void ancestorMoved(HierarchyEvent e) {
            }

            @Override
            public void ancestorResized(HierarchyEvent e) {
                // make sure all contents can fit riiight in :)
                main.ticktimes1.setSize(main.getWidth() - selectionWidth - 28, main.getHeight() - 80);
                main.locscroll.setLocation(main.getWidth() - selectionWidth - 20, main.getHeight() - 240);
                main.selection.setBounds(main.getWidth() - selectionWidth - 20, yoffset, selectionWidth, main.getHeight() - 285);
                main.filepath.setSize(main.getWidth() - 235, main.filepath.getHeight());
                main.exportbutton.setLocation(main.getWidth() - 121, 5);
            }
        });

        // description box
        this.description = new JTextArea();
        this.description.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        this.description.setEditable(false);
        this.description.getCaret().setBlinkRate(0);
        this.description.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                main.description.getCaret().setVisible(true);
            }
        });
        this.description.addMouseListener(new MouseListener() {
            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    main.descriptionMenu.show(main.description, e.getX(), e.getY());
                }
            }
        });

        // popup menu with copy button for description box
        this.descriptionMenu = this.append(new JPopupMenu());
        this.descriptionMenu.add(new JMenuItem("Copy")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                main.description.copy();
            }
        });
        this.descriptionMenu.add(new JMenuItem("Copy All")).addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                main.description.selectAll();
                main.description.copy();
            }
        });

        // file box
        this.filepath = this.append(new JTextField());
        this.filepath.setBounds(110, 5, 750, 30);
        this.filepath.setEditable(false);
        this.filepath.setDragEnabled(false);
        this.append(locscroll = new JScrollPane(description)).setSize(this.selectionWidth, 200);

        this.setResizable(true);

    }

}
