package assignment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;

public class GUI
{
    public static Visualization visualization = new Visualization();

    private static final int SHAPE_COUNT = 10;

    public static void main(String[] args)
    {
        JFrame frame = new JFrame("BVH Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        BVHVisualization bvhVisualization = new BVHVisualization();
        frame.add(bvhVisualization);
        frame.setVisible(true);
        frame.setResizable(false);

        LinkedList<Shape> shapes = new LinkedList<>();
        for (int i = 0; i < SHAPE_COUNT; i++) {
            shapes.add(new Triangle(new Point(100 + i * 100, 100  + i * 100), new Point(0 + i * 100, 100 + i * 100), new Point(100 + i * 100, 0 + i * 100)));
        }
        BoundedVolumeHierarchy boundingVolumeHierarchy = new BoundedVolumeHierarchy();
        boundingVolumeHierarchy.buildBVH(shapes);

        visualization.setBoundingVolumeHierarchy(boundingVolumeHierarchy);
    }
}

class BVHVisualization extends JPanel
{
    public BVHVisualization()
    {
        this.setFocusable(true);
        this.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                super.keyPressed(e);
                if (GUI.visualization.moveSquare(e.getKeyChar()))
                    repaint();
            }
        });
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        LinkedList<Point> points = GUI.visualization.drawGUI();
        for (Point point : points)
            g.fillRect(point.x, point.y, 1, 1);

        g.setColor(Color.BLUE);
        g.fillRect(190, 190, 21, 21);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(400, 400);
    }
}
