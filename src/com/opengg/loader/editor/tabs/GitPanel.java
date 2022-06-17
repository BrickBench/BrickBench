package com.opengg.loader.editor.tabs;

import com.opengg.loader.editor.EditorState;
import com.opengg.loader.game.nu2.gizmo.GitNode;
import com.opengg.loader.game.nu2.NU2MapData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

public class GitPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
    public int camX = 0;
    public int camY = 0;
    public int offX = 0;
    public int offY = 0;

    public double scale = 0.2;
    public double origX = 0;
    public double origY = 0;

    public final int recXArc = 10;
    public final int recYArc = 10;
    public final int recPadX = 20;
    public final int recPadY = 45;

    public AffineTransform transform;
    public AffineTransform initialMouseTransform = new AffineTransform();
    public Point2D transformedPoint;
    public GitNode selected = null;


    public final Color conditionColor = Color.decode("#BF4431");
    public final Color actionColor = Color.decode("#4BBC6F");
    public final Color highlightColor = Color.decode("#F1C500");
    public final Color flowboxColor = Color.decode("#277FBE");
    public final Color collapseColor = Color.decode("#576475");
    public final Color bgColor = Color.decode("#393939");
    public final Color lineColor = Color.decode("#858585");
    public final Color mildWhite = Color.decode("#E3E7E8");

    public GitPanel() {
        setFocusable(true);
        this.addKeyListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        super.paintComponent(g);
        g.setFont(new Font("default", Font.BOLD, 14));
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(bgColor);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        offX = this.getWidth() / 2;
        offY = this.getHeight() / 2;

        AffineTransform orig = g2d.getTransform();
        transform = new AffineTransform(orig);
        transform.translate(offX, offY);
        transform.scale(scale, scale);
        transform.translate(-offX, -offY);
        transform.translate(camX, camY);
        g2d.transform(transform);

        if (EditorState.getActiveMap() != null && EditorState.getActiveMap().levelData() instanceof NU2MapData nu2) {
            for (GitNode node : nu2.git().gitNodes().values()) {
                if (node.name != null) {
                    int nameWidth = g.getFontMetrics().stringWidth(node.name);
                    g.setColor(lineColor);
                    for (var i : node.children) {
                        GitNode child = nu2.git().gitNodes().get(i);
                        arrowRectRect(g2d, node, child);
                    }
                    if (node.selected) {
                        g.setColor(highlightColor);
                    } else if (node.conditions.size() > 0) {
                        g.setColor(conditionColor);
                    } else if (node.gizmos.size() > 0) {
                        g.setColor(flowboxColor);
                    } else if (node.actions.size() > 0) {
                        g.setColor(actionColor);
                    } else {
                        g.setColor(collapseColor);
                    }
                    g.fillRoundRect((int) (node.x - nameWidth / 2 - recPadX / 2), (int) (node.y - recPadY / 2), nameWidth + recPadX, recPadY, recXArc, recYArc);

                    g.setColor(Color.BLACK);
                    g.drawString(node.name, (int) (node.x - nameWidth / 2), (int) (node.y + g.getFontMetrics().getAscent() / 2));
                    ((Graphics2D) g).setStroke(new BasicStroke(2));
                    g.drawRoundRect((int) (node.x - nameWidth / 2 - recPadX / 2), (int) (node.y - recPadY / 2), nameWidth + recPadX, recPadY, recXArc, recYArc);
                }
            }
        }
        try {
            g2d.transform(transform.createInverse());
        } catch (NoninvertibleTransformException e) {
            e.printStackTrace();
        }
        g.setColor(mildWhite);
        g.drawString(-camX + "," + -camY + "," + String.format("%.2f", scale), 20, 20);
    }

    public void arrowRectRect(Graphics2D g, GitNode node1, GitNode node2) {

        int nameWidth1 = g.getFontMetrics().stringWidth(node1.name);
        int nameWidth2 = g.getFontMetrics().stringWidth(node2.name);

        int topX1 = (int) (node1.x);
        int topY1 = (int) (node1.y - recPadY / 2);
        int leftX1 = (int) (node1.x - nameWidth1 / 2 - recPadX / 2);
        int leftY1 = (int) node1.y;
        int rightX1 = (int) (node1.x + nameWidth1 / 2 + recPadX / 2);
        int rightY1 = (int) (node1.y);
        int bottomX1 = (int) (node1.x);
        int bottomY1 = (int) (node1.y + recPadY / 2) + 2;
        int topX2 = (int) (node2.x);
        int topY2 = (int) (node2.y - recPadY / 2);
        int leftX2 = (int) (node2.x - nameWidth2 / 2 - recPadX / 2);
        int leftY2 = (int) node2.y;
        int rightX2 = (int) (node2.x + nameWidth2 / 2 + recPadX / 2);
        int rightY2 = (int) node2.y;
        int bottomX2 = (int) node2.x;
        int bottomY2 = (int) (node2.y + recPadY / 2) + 2;

        int finalX1 = 0;
        int finalY1 = 0;
        int finalX2 = 0;
        int finalY2 = 0;
        double dist = Double.POSITIVE_INFINITY;

        double temp = Point.distanceSq(topX1, topY1, leftX2, leftY2);
        if (temp < dist) {
            finalX1 = topX1;
            finalY1 = topY1;
            finalX2 = leftX2;
            finalY2 = leftY2;
            dist = temp;
        }
        temp = Point.distanceSq(topX1, topY1, rightX2, rightY2);
        if (temp < dist) {
            finalX1 = topX1;
            finalY1 = topY1;
            finalX2 = rightX2;
            finalY2 = rightY2;
            dist = temp;
        }
        temp = Point.distanceSq(topX1, topY1, bottomX2, bottomY2);
        if (temp < dist) {
            finalX1 = topX1;
            finalY1 = topY1;
            finalX2 = bottomX2;
            finalY2 = bottomY2;
            dist = temp;
        }

        temp = Point.distanceSq(leftX1, leftY1, topX2, topY2);
        if (temp < dist) {
            finalX1 = leftX1;
            finalY1 = leftY1;
            finalX2 = topX2;
            finalY2 = topY2;
            dist = temp;
        }
        temp = Point.distanceSq(leftX1, leftY1, rightX2, rightY2);
        if (temp < dist) {
            finalX1 = leftX1;
            finalY1 = leftY1;
            finalX2 = rightX2;
            finalY2 = rightY2;
            dist = temp;
        }
        temp = Point.distanceSq(leftX1, leftY1, bottomX2, bottomY2);
        if (temp < dist) {
            finalX1 = leftX1;
            finalY1 = leftY1;
            finalX2 = bottomX2;
            finalY2 = bottomY2;
            dist = temp;
        }

        temp = Point.distanceSq(bottomX1, bottomY1, topX2, topY2);
        if (temp < dist) {
            finalX1 = bottomX1;
            finalY1 = bottomY1;
            finalX2 = topX2;
            finalY2 = topY2;
            dist = temp;
        }
        temp = Point.distanceSq(bottomX1, bottomY1, rightX2, rightY2);
        if (temp < dist) {
            finalX1 = bottomX1;
            finalY1 = bottomY1;
            finalX2 = rightX2;
            finalY2 = rightY2;
            dist = temp;
        }
        temp = Point.distanceSq(bottomX1, bottomY1, leftX2, leftY2);
        if (temp < dist) {
            finalX1 = bottomX1;
            finalY1 = bottomY1;
            finalX2 = leftX2;
            finalY2 = leftY2;
            dist = temp;
        }

        temp = Point.distanceSq(rightX1, rightY1, topX2, topY2);
        if (temp < dist) {
            finalX1 = rightX1;
            finalY1 = rightY1;
            finalX2 = topX2;
            finalY2 = topY2;
            dist = temp;
        }
        temp = Point.distanceSq(rightX1, rightY1, bottomX2, bottomY2);
        if (temp < dist) {
            finalX1 = rightX1;
            finalY1 = rightY1;
            finalX2 = bottomX2;
            finalY2 = bottomY2;
            dist = temp;
        }
        temp = Point.distanceSq(rightX1, rightY1, leftX2, leftY2);
        if (temp < dist) {
            finalX1 = rightX1;
            finalY1 = rightY1;
            finalX2 = leftX2;
            finalY2 = leftY2;
            dist = temp;
        }

        this.drawArrowLine(g, finalX1, finalY1, finalX2, finalY2, 8, 8);

    }

    private void drawArrowLine(Graphics g, double x1, double y1, double x2, double y2, int d, int h) {
        double dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx * dx + dy * dy);
        double xm = D - d, xn = xm, ym = h, yn = -h, x;
        double sin = dy / D, cos = dx / D;

        x = xm * cos - ym * sin + x1;
        ym = xm * sin + ym * cos + y1;
        xm = x;

        x = xn * cos - yn * sin + x1;
        yn = xn * sin + yn * cos + y1;
        xn = x;

        int[] xpoints = {(int) x2, (int) xm, (int) xn};
        int[] ypoints = {(int) y2, (int) ym, (int) yn};

        ((Graphics2D) g).setStroke(new BasicStroke(3));
        g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        g.fillPolygon(xpoints, ypoints, 3);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        AffineTransform transform2 = new AffineTransform();
        try {
            transform2 = transform.createInverse();
        } catch (NoninvertibleTransformException noninvertibleTransformException) {
            noninvertibleTransformException.printStackTrace();
        }

        if (EditorState.getActiveMap() != null && EditorState.getActiveMap().levelData() instanceof NU2MapData nu2) {
            for (var node : nu2.git().gitNodes().values()) {
                int nameWidth1 = this.getGraphics().getFontMetrics().stringWidth(node.name);
                Rectangle rect = new Rectangle((int) (node.x - nameWidth1 / 2 - recPadX / 2), (int) (node.y - recPadY / 2), nameWidth1 + recPadX, recPadY);
                if (rect.contains(transform2.transform(e.getPoint(), null))) {
                    if (selected != null) selected.selected = false;
                    selected = node;
                    selected.selected = true;
                    EditorState.selectObject(selected);
                    this.repaint();
                    break;
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        try {
            transformedPoint = transform.inverseTransform(e.getPoint(), null);
        } catch (NoninvertibleTransformException noninvertibleTransformException) {
            noninvertibleTransformException.printStackTrace();
        }
        origX = transformedPoint.getX();
        origY = transformedPoint.getY();
        initialMouseTransform = transform;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        try {
            transformedPoint = initialMouseTransform.inverseTransform(e.getPoint(), null);
        } catch (NoninvertibleTransformException noninvertibleTransformException) {
            noninvertibleTransformException.printStackTrace();
        }
        double dX = transformedPoint.getX() - origX;
        double dY = transformedPoint.getY() - origY;
        origX = transformedPoint.getX();
        origY = transformedPoint.getY();
        camX += dX;
        camY += dY;
        repaint();
    }


    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scale += e.getPreciseWheelRotation() * -0.01;
        scale = Math.max(0.045, scale);
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_EQUALS) {
            scale = Math.max(0.045, scale - 0.01);
        }
        if (e.getKeyCode() == KeyEvent.VK_MINUS) {
            scale += 0.01;
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            camX += 10;
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            camX -= 10;
        }
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            camY += 10;
        }
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            camY -= 10;
        }
        this.repaint();
    }
}
