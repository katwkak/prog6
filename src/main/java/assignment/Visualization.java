package assignment;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashSet;
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
    private static final int STEP_PIXELS = 5;


    /**
     * Initialize state of square to be at (0, 0)
     */
    public Visualization ()
    {
        this.virtualSquareCenter = new Point2D.Double(0, 0); // Default virtual center
        LinkedList<Shape> shapes = new LinkedList<>();
        for (int i = 0; i < SHAPE_COUNT; i++) {
            shapes.add(new Triangle(new Point(100 + i * 100, 100  + i * 100), new Point(0 + i * 100, 100 + i * 100), new Point(100 + i * 100, 0 + i * 100)));
        }
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
            case 'W': // Move up
                potentialNewCenter.y += STEP_PIXELS;
                System.out.println("up");
                break;
            case 'S': // Move down
                potentialNewCenter.y -= STEP_PIXELS;
                System.out.println("down");
                break;
            case 'A': // Move left
                potentialNewCenter.x -= STEP_PIXELS;
                System.out.println("left");
                break;
            case 'D': // Move right
                potentialNewCenter.x += STEP_PIXELS;
                System.out.println("right");
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
        System.out.println(virtualSquareCenter);
        return true;

    }

    /**
     * Calculates all the pixels visible from the square in the center
     *
     * @return              All points that should be drawn other than the square
     */
    public LinkedList<Point> drawGUI()
    {
        Set<Point> pixelsToDisplay = new HashSet<>();

        Point2D.Double virtualSquareCenterDouble = new Point2D.Double(virtualSquareCenter.getX(), virtualSquareCenter.getY());
        int rayDensity = 4;
        for (int guiX = 0; guiX < GUI_WIDTH; guiX += rayDensity) {
            for (int guiY = 0; guiY < GUI_HEIGHT; guiY += rayDensity) {

                int squareGUIStartX = (GUI_WIDTH - SQUARE_SIZE) / 2; // 190
                int squareGUIStartY = (GUI_HEIGHT - SQUARE_SIZE) / 2; // 190
                int squareGUIEndX = squareGUIStartX + SQUARE_SIZE;   // 211 (exclusive)
                int squareGUIEndY = squareGUIStartY + SQUARE_SIZE;   // 211 (exclusive)

                if (guiX >= squareGUIStartX && guiX < squareGUIEndX &&
                        guiY >= squareGUIStartY && guiY < squareGUIEndY) {
                    continue; // This pixel belongs to the blue square, don't draw over it
                }

                Point2D.Double virtualTarget = GUItoVirtual(pointToPoint2D(new Point(guiX, guiY)));

                double dirX = virtualTarget.x - virtualSquareCenterDouble.getX();
                double dirY = virtualTarget.y - virtualSquareCenterDouble.getY();

                Vector2D direction = new Vector2D(dirX, dirY);

                Shape hitShape = boundingVolumeHierarchy.intersectRay(virtualSquareCenterDouble, direction);
                if (hitShape != null) {
                    Point2D.Double intersection = hitShape.findIntersection(point2DToPoint(virtualSquareCenter), direction);
                    if (intersection != null) {
                        Point guiPoint = point2DToPoint(virtualToGUI(intersection));
                        if (guiPoint.x >= 0 && guiPoint.x < GUI_WIDTH && guiPoint.y >= 0 && guiPoint.y < GUI_HEIGHT) {
                            pixelsToDisplay.add(guiPoint);
                        }
                    }
                }
            }
        }

        return new LinkedList<>(pixelsToDisplay);
    }

    private Point2D.Double pointToPoint2D(Point point){
        return new Point2D.Double(point.x, point.y);
    }

    private Point point2DToPoint(Point2D.Double point){
        return new Point((int) point.x, (int) point.y);
    }

    private Point2D.Double virtualToGUI(Point2D.Double point) {
        double guiX = (point.x - virtualSquareCenter.getX()) + GUI_WIDTH / 2.0;
        double guiY = (GUI_HEIGHT / 2.0) - (point.y - virtualSquareCenter.getY());
        return new Point2D.Double(guiX, guiY);
    }

    private Point2D.Double GUItoVirtual(Point2D.Double point) {
        double virtualX = (point.x - GUI_WIDTH / 2.0) + virtualSquareCenter.getX();
        double virtualY = (GUI_HEIGHT / 2.0 - point.y) + virtualSquareCenter.getY();
        return new Point2D.Double(virtualX, virtualY);
    }
}
