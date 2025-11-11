package assignment;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

public class Visualization
{
    private BoundedVolumeHierarchy boundingVolumeHierarchy;

    private Point2D.Double virtualSquareCenter;

    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 400;
    private static final int SQUARE_SIZE = 21;
    private static final int SHAPE_COUNT = 10;


    /**
     * Initialize state of square to be at (0, 0)
     */
    public Visualization ()
    {
        this.virtualSquareCenter = new Point2D.Double(0, 0); // Default virtual center
        LinkedList<Shape> shapes = new LinkedList<>();
        shapes.add(new Triangle(new Point(100, 100), new Point(0, 100), new Point(100, 0)));
//        Random rand = new Random();
//        for (int i = 0; i < SHAPE_COUNT; i++) {
//            Point p1 = new Point(rand.nextInt(GUI_WIDTH), rand.nextInt(GUI_HEIGHT));
//            Point p2 = new Point(rand.nextInt(GUI_WIDTH), rand.nextInt(GUI_HEIGHT));
//            Point p3 = new Point(rand.nextInt(GUI_WIDTH), rand.nextInt(GUI_HEIGHT));
//            shapes.add(new Triangle(p1, p2, p3));
//        }
        this.boundingVolumeHierarchy = new BoundedVolumeHierarchy(shapes);
        System.out.println("Bounding volume hierarchy: " + this.boundingVolumeHierarchy);
    }

    public Visualization(BoundedVolumeHierarchy boundingVolumeHierarchy) {
        this();
        this.boundingVolumeHierarchy = boundingVolumeHierarchy;
    }

    /**
     * @param boundingVolumeHierarchy        the BVH to set the boundedVolumeHierarchy to
     */
    public void setBoundingVolumeHierarchy(BoundedVolumeHierarchy boundingVolumeHierarchy)
    {
        this.boundingVolumeHierarchy = boundingVolumeHierarchy;
    }

    /**
     * Moves the square within the environment if it's able to move in the given direction
     *
     * @param keyPressed        the character of the keyPressed
     * @return              if the environment should be redrawn or not
     */
    public boolean moveSquare (char keyPressed)
    {
        if (boundingVolumeHierarchy == null) {
            System.err.println("Error: BVH not set in Visualization. Cannot move square.");
            return false;
        }

        Point2D.Double potentialNewCenter = new Point2D.Double(virtualSquareCenter.getX(), virtualSquareCenter.getY());

        switch (Character.toUpperCase(keyPressed)) {
            case 'W': // Move up (increase Y in virtual coordinates)
                potentialNewCenter.y ++;
                break;
            case 'S': // Move down (decrease Y in virtual coordinates)
                potentialNewCenter.y --;
                break;
            case 'A': // Move left (decrease X in virtual coordinates)
                potentialNewCenter.x --;
                break;
            case 'D': // Move right (increase X in virtual coordinates)
                potentialNewCenter.x ++;
                break;
            default:
                return false; // Invalid key
        }

        double halfSquareVirtual = SQUARE_SIZE / 2.0; // If 1 GUI pixel = 1 virtual unit

        // Create the 4 corners of the potential new virtual square bounding box
        Point2D.Double topLeft = new Point2D.Double(potentialNewCenter.x - halfSquareVirtual, potentialNewCenter.y + halfSquareVirtual);
        Point2D.Double topRight = new Point2D.Double(potentialNewCenter.x + halfSquareVirtual, potentialNewCenter.y + halfSquareVirtual);
        Point2D.Double bottomLeft = new Point2D.Double(potentialNewCenter.x - halfSquareVirtual, potentialNewCenter.y - halfSquareVirtual);
        Point2D.Double bottomRight = new Point2D.Double(potentialNewCenter.x + halfSquareVirtual, potentialNewCenter.y - halfSquareVirtual);

        // Check if any of these points would be inside a triangle
        // This is a simplified collision check using multiple point probes.
        // A more robust solution might require a BVH method that checks for intersection with a bounding box directly.
        Set<Shape> collisions;
        collisions = boundingVolumeHierarchy.findCollision(topLeft);
        if (!collisions.isEmpty()) return false;
        collisions = boundingVolumeHierarchy.findCollision(topRight);
        if (!collisions.isEmpty()) return false;
        collisions = boundingVolumeHierarchy.findCollision(bottomLeft);
        if (!collisions.isEmpty()) return false;
        collisions = boundingVolumeHierarchy.findCollision(bottomRight);
        if (!collisions.isEmpty()) return false;
        collisions = boundingVolumeHierarchy.findCollision(potentialNewCenter); // Check center too
        if (!collisions.isEmpty()) return false;


        // If no collision:
        this.virtualSquareCenter = potentialNewCenter; // Update position
        return true;

    }

    /**
     * Calculates all the pixels visible from the square in the center
     *
     * @return              All points that should be drawn other than the square
     */
    public LinkedList<Point> drawGUI()
    {
        LinkedList<Point> pixelsToDisplay = new LinkedList<>();

        Point2D.Double observerVirtualPointDouble = new Point2D.Double(virtualSquareCenter.getX(), virtualSquareCenter.getY());

        for (int guiX = 0; guiX < GUI_WIDTH; guiX++) {
            for (int guiY = 0; guiY < GUI_HEIGHT; guiY++) {

                int squareGUIStartX = (GUI_WIDTH - SQUARE_SIZE) / 2; // 190
                int squareGUIStartY = (GUI_HEIGHT - SQUARE_SIZE) / 2; // 190
                int squareGUIEndX = squareGUIStartX + SQUARE_SIZE;   // 211 (exclusive)
                int squareGUIEndY = squareGUIStartY + SQUARE_SIZE;   // 211 (exclusive)

                if (guiX >= squareGUIStartX && guiX < squareGUIEndX &&
                        guiY >= squareGUIStartY && guiY < squareGUIEndY) {
                    continue; // This pixel belongs to the blue square, don't draw over it
                }
                double virtualTargetX = (double) (guiX - (GUI_WIDTH / 2)) + observerVirtualPointDouble.getX();
                double virtualTargetY = (double) ((GUI_HEIGHT / 2) - guiY) + observerVirtualPointDouble.getY();

                double dirX = virtualTargetX - observerVirtualPointDouble.getX();
                double dirY = virtualTargetY - observerVirtualPointDouble.getY();

                Vector2D direction = new Vector2D(dirX, dirY);

                Shape hitShape = boundingVolumeHierarchy.intersectRay(observerVirtualPointDouble, direction);

                if (hitShape != null) {
                    pixelsToDisplay.add(new Point(guiX, guiY));
                }
            }
        }
        System.out.println(pixelsToDisplay);
        return pixelsToDisplay;
    }
}
