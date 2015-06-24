package com.bergerkiller.bukkit.nolagg.examine.reader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Graph extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int VIEW_BOUND_RANGE = 10; // Range around viewport that is still rendered
    private final GraphBox container;
    public int graphXOffset = 80;
    public int graphYOffset = 50;
    public double maxvalue = 1.0;
    private GraphArea selectedArea = null;
    private int viewMinX, viewMaxX;
    private int duration = 500;
    private double scale = 1.0;
    private double yscale = 1.0;
    private double[] offset = new double[500];
    private List<GraphArea> areas = new ArrayList<GraphArea>();
    public Graph(GraphBox container) {
        this.container = container;
        this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        final Graph graph = this;
        final MouseMotionListener mouselistener = new MouseMotionListener() {

            public void mouseDragged(MouseEvent arg0) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int y = graph.getHeight() - e.getY() - graphYOffset;
                int x = e.getX() - graphXOffset - 2;

                if (graph.selectedArea != null) {
                    graph.selectedArea.setSelected(false);
                }
                boolean selected = false;
                if (x < graph.duration && y > 0) {
                    for (GraphArea area : graph.areas) {
                        if (area.isIn(x, y)) {
                            if (area != graph.selectedArea) {
                                graph.onSelectionChange(area);
                                graph.setSelection(area.index);
                            }
                            selected = true;
                            break;
                        }
                    }
                }
                if (graph.selectedArea != null && !selected) {
                    graph.onSelectionChange(null);
                    graph.setSelection(-1);
                }
            }
        };

        this.addMouseMotionListener(mouselistener);
        this.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                if (event.getWheelRotation() < 0) {
                    graph.yscale /= 1.5;
                } else {
                    graph.yscale *= 1.5;
                }
                graph.setYScale(graph.yscale);
            }
        });
        this.addMouseListener(new MouseListener() {

            public void mouseEntered(MouseEvent arg0) {
            }

            public void mouseExited(MouseEvent arg0) {
            }

            public void mousePressed(MouseEvent arg0) {
            }

            public void mouseReleased(MouseEvent arg0) {
            }

            @Override
            public void mouseClicked(MouseEvent arg0) {
                mouselistener.mouseMoved(arg0);
                if (graph.selectedArea == null) {
                    graph.onAreaClick(null);
                } else {
                    graph.onAreaClick(graph.selectedArea);
                }
            }
        });
    }

    public void updateView() {
        this.viewMinX = this.container.getMinViewX() - graphXOffset;
        this.viewMaxX = viewMinX + this.container.getWidth();
        this.viewMinX -= VIEW_BOUND_RANGE;
        this.viewMaxX += VIEW_BOUND_RANGE;
    }

    public void setSelection(int index) {
        boolean changed = false;
        boolean hasselect = false;
        GraphArea before = this.selectedArea;
        for (GraphArea area : this.areas) {
            if (area.index == index) {
                hasselect = true;
                if (this.selectedArea != area) {
                    this.selectedArea = area;
                    changed = true;
                    area.setSelected(true);
                }
            } else if (area.isSelected()) {
                if (this.selectedArea == area) {
                    this.selectedArea = null;
                    changed = true;
                }
                area.setSelected(false);
            }
        }
        if (this.selectedArea != null && !hasselect) {
            this.selectedArea.setSelected(false);
            this.selectedArea = null;
            changed = true;
        }
        if (changed) {
            if (before != null)
                this.repaint(before);
            if (this.selectedArea != null) {
                this.repaint(this.selectedArea);
            }
        }
    }

    public abstract void onSelectionChange(GraphArea newarea);

    public abstract void onAreaClick(GraphArea area);

    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        this.updateScale(height);
    }

    public void updateScale(int newheight) {
        this.scale = (double) (newheight - this.graphYOffset - 20) / this.maxvalue;
        this.scale *= this.yscale;
        this.generateAreas();
    }

    public GraphArea getSelectedArea() {
        return this.selectedArea;
    }

    public GraphArea getArea(int index) {
        return this.areas.get(index);
    }

    public GraphArea addArea() {
        GraphArea area = new GraphArea(this.duration, this.areas.size());
        this.areas.add(0, area);
        return area;
    }

    public void reset(int newduration) {
        this.areas.clear();
        this.duration = newduration;
        this.selectedArea = null;
        this.setPreferredSize(new Dimension(this.duration + graphXOffset - 1, 100));
        if (this.offset.length != newduration) {
            this.offset = new double[this.duration];
        }
        Arrays.fill(this.offset, 0.0);
    }

    public void generateOffsets() {
        Arrays.fill(offset, 0.0);
        // find out the maximum possible value needed
        for (GraphArea area : this.areas) {
            for (int x = 0; x < this.duration; x++) {
                offset[x] += area.values[x];
            }
        }
        maxvalue = this.getMaxOffset();
        // find out the needed scale
        this.updateScale(this.getHeight());
    }

    public double getMaxOffset() {
        double max = 0.0;
        for (double value : offset) {
            max = Math.max(max, value);
        }
        return max;
    }

    public void generateAreas() {
        // generate
        Arrays.fill(offset, 0.0);
        for (GraphArea area : this.areas) {
            for (int x = 0; x < this.duration; x++) {
                offset[x] = area.set(x, this.scale, offset[x]);
            }
        }
    }

    public void orderAreas() {
        this.generateOffsets();
        this.setYScale(1.0);
        if (!this.areas.isEmpty()) {
            this.setSize(new Dimension(this.duration + graphXOffset, this.getHeight()));
        }
    }

    public void drawText(double value, Graphics g, int x, int y, int mode) {
        drawText(Double.valueOf(value).toString(), g, x, y, mode);
    }

    public void drawText(int value, Graphics g, int x, int y, int mode) {
        drawText(Integer.valueOf(value).toString(), g, x, y, mode);
    }

    public void drawText(String text, Graphics g, int x, int y, int mode) {
        int stringLen = (int) g.getFontMetrics().getStringBounds(text, g).getWidth();
        if (mode == 1) {
            x -= stringLen / 2;
        } else if (mode == 2) {
            x -= stringLen;
        }
        g.drawString(text, x, y);
    }

    public void repaint(GraphArea area) {
        this.updateView();
        Graphics2D g2d = (Graphics2D) this.getGraphics();
        g2d.translate(this.graphXOffset, this.getHeight() - this.graphYOffset);
        g2d.scale(1.0, -1.0); // invert
        area.draw(g2d, viewMinX, viewMaxX);
    }

    public void setYScale(double yscale) {
        if (yscale < 1.0) {
            yscale = 1.0;
        }
        this.yscale = yscale;
        this.updateScale(this.getHeight());
        this.repaint();
    }

    protected void paintComponent(Graphics g) {
        this.updateView();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        // correctly invert coordinates
        Graphics2D g2d = (Graphics2D) g;

        if (this.areas.isEmpty()) {
            g.setColor(Color.RED);
            drawText("In here the process time in every tick is displayed in a graph", g, 300, 50, 1);
            drawText("You can hover over the graph or selection box to the right to highlight", g, 300, 80, 1);
            drawText("By clicking on part of the graph you can look deeper into the results", g, 300, 110, 1);
            drawText("Clicking on the background will make you go back one step", g, 300, 140, 1);
        }

        g2d.translate(this.graphXOffset, this.getHeight() - this.graphYOffset);
        g2d.scale(1.0, -1.0); // invert Y
        for (GraphArea area : this.areas) {
            area.draw(g, viewMinX, viewMaxX);
        }
        g.translate(-1, 0);
        g2d.scale(1.0, -1.0); // invert Y

        int bleft = this.getHeight() - this.graphYOffset;

        // draw scaler
        g.setColor(Color.WHITE);
        g.drawLine(0, 0, this.duration, 0);
        g.drawLine(0, 0, 0, -bleft);

        // horizontal + ticks
        int xinterval = 60;
        int lineX;
        for (int x = 0; x <= this.duration / xinterval; x++) {
            lineX = x * xinterval;
            if (lineX > viewMaxX) {
                break;
            } else if (lineX < viewMinX) {
                continue;
            }
            g.drawLine(lineX, 0, lineX, 10);
            drawText(lineX, g, lineX, 22, 1);
        }
        drawText("Time (ticks)", g, this.duration / 2, 40, 1);

        if (0 >= viewMinX) {
            // vertical value using scale
            int yinterval = 30;
            int lineY;
            int ymax = (this.getHeight() - this.graphYOffset) / yinterval;
            double v;
            for (int y = 0; y <= ymax; y++) {
                lineY = y * yinterval;
                g.drawLine(0, -lineY, -10, -y * yinterval);
                v = Math.round(lineY / this.scale * 100.0) / 100.0;
                drawText(v, g, -19, -lineY + 5, 2);
            }

            // rotated text
            g2d.rotate(-Math.PI * 0.5);
            drawText("Time spent (milliseconds)", g2d, ymax * yinterval / 2, -60, 1);
        }
    }

}
