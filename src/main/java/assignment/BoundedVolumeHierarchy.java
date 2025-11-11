package assignment;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class BoundedVolumeHierarchy implements BVH
{
    private SplitMethod splitMethod;
    private BVHNode root;
    private boolean shapeRemovedSuccessfully;

    private enum Axis
    {
        X, // x-axis
        Y  // y-axis
    }

    public static void main(String[] args) {
        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            bvh.insert(new Triangle(new Point(rand.nextInt(10), rand.nextInt(10)), new Point(rand.nextInt(10), rand.nextInt(10)), new Point(rand.nextInt(10), rand.nextInt(10))));
        }
    }

    @Override
    public void setSplitMethod(SplitMethod splitMethod)
    {
        this.splitMethod = splitMethod;
    }

    private int partitionByMedian(List<Shape> shapes, Axis axis) {
        // sorts list based on axis
        shapes.sort(Comparator.comparingDouble(shape -> {
            Point2D.Double center = shape.getCenter();
            return axis == Axis.X ? center.getX() : center.getY();
        }));
        return shapes.size() / 2; // returns median index
    }

    @Override
    public void buildBVH(List<Shape> shapeList)
    {
        if (shapeList == null || shapeList.isEmpty()) {
            this.root = null;
            System.err.println("shape list is null or empty");
            return;
        }
        this.root = buildBVHRecursive(new java.util.ArrayList<>(shapeList), Axis.X);
    }

    private BVHNode buildBVHRecursive(List<Shape> shapeList, Axis axis) {
        if (shapeList == null || shapeList.isEmpty()) {
            return null;
        }

        if (shapeList.size() == 1) {
            return new BVHNode(shapeList.getFirst());
        }

        int medianIndex = partitionByMedian(shapeList, axis);

        Axis nextAxis = axis == Axis.X ? Axis.Y : Axis.X; // determine axis to partition by

        List<Shape> leftSubList = new java.util.ArrayList<>(shapeList.subList(0, medianIndex));
        List<Shape> rightSubList = new java.util.ArrayList<>(shapeList.subList(medianIndex, shapeList.size())); // Corrected size() here

        BVHNode left = buildBVHRecursive(leftSubList, nextAxis);
        BVHNode right = buildBVHRecursive(rightSubList, nextAxis);

        // add bounds to class
        Rectangle bounds;
        if (left == null) {
            bounds = right.bounds;
        } else if (right == null) {
            bounds = left.bounds;
        } else {
            bounds = combineBoundingBoxes(left.bounds, right.bounds);
        }
        return new BVHNode(bounds, left, right);
    }

    @Override
    public void insert(Shape shape)
    {
        if (root == null) {
            root = new BVHNode(shape);
            return;
        }
        root = insertRecursive(root, shape);
    }

    private BVHNode insertRecursive(BVHNode node, Shape shape) {
        if (node == null) {
            return new BVHNode(shape); // Create and return new leaf node
        }
        if (node.isLeaf()) {
            node.leftChild = new BVHNode(node.shape);
            node.shape = null;
            node.rightChild = new BVHNode(shape);
            node.bounds = combineBoundingBoxes(node.leftChild.bounds, node.rightChild.bounds);
            node.updateHeight();
            return node;
        }

        if (preferLeft(node, shape)) {
            node.leftChild = insertRecursive(node.leftChild, shape);
        } else {
            node.rightChild = insertRecursive(node.rightChild, shape);
        }

        node.bounds = combineBoundingBoxes(node.leftChild.bounds, node.rightChild.bounds);
        node.updateHeight();

        BVHNode balancedNode = rebalance(node);
        return balancedNode;
    }

    private BVHNode rebalance(BVHNode node) {
        int balance = getBalance(node);

        if (balance > 1) {
            // Check if it's Left-Left Case or Left-Right Case
            if (getBalance(node.leftChild) >= 0) {
                // Left-Left Case: Right Rotation
                return rotateRight(node);
            } else {
                // Left-Right Case: Left Rotation on child, then Right Rotation on node
                node.leftChild = rotateLeft(node.leftChild);
                return rotateRight(node);
            }
        }

        if (balance < -1) {
            // Check if it's Right-Right Case or Right-Left Case
            if (getBalance(node.rightChild) <= 0) {
                // Right-Right Case: Left Rotation
                return rotateLeft(node);
            } else {
                // Right-Left Case: Right Rotation on child, then Left Rotation on node
                node.rightChild = rotateRight(node.rightChild);
                return rotateLeft(node);
            }
        }

        return node;
    }

    private BVHNode rotateRight(BVHNode y) {
        BVHNode x = y.leftChild; // x is the new root of this subtree
        BVHNode T2 = x.rightChild; // T2 is the subtree that moves

        // Perform rotation
        x.rightChild = y;
        y.leftChild = T2;

        // Update heights
        y.updateHeight(); // y's children changed
        x.updateHeight(); // x's children changed

        // Update bounding boxes
        y.bounds = combineBoundingBoxes(Arrays.asList(height(y.leftChild) != -1 ? y.leftChild.bounds : null, height(y.rightChild) != -1 ? y.rightChild.bounds : null)); // Use null checks with combineBoundingBox
        x.bounds = combineBoundingBoxes(Arrays.asList(height(x.leftChild) != -1 ? x.leftChild.bounds : null, height(x.rightChild) != -1 ? x.rightChild.bounds : null)); // Need a better way to handle null bounds in combineBoundingBox

        return x;
    }

    private BVHNode rotateLeft(BVHNode x) {
        BVHNode y = x.rightChild; // y is the new root of this subtree
        BVHNode T2 = y.leftChild; // T2 is the subtree that moves

        // Perform rotation
        y.leftChild = x;
        x.rightChild = T2;

        // Update heights
        x.updateHeight(); // x's children changed
        y.updateHeight(); // y's children changed

        // Update bounding boxes
        x.bounds = combineBoundingBoxes(Arrays.asList(height(x.leftChild) != -1 ? x.leftChild.bounds : null, height(x.rightChild) != -1 ? x.rightChild.bounds : null));
        y.bounds = combineBoundingBoxes(Arrays.asList(height(y.leftChild) != -1 ? y.leftChild.bounds : null, height(y.rightChild) != -1 ? y.rightChild.bounds : null));

        return y;
    }

    private int height(BVHNode node) {
        return (node == null) ? -1 : node.height;
    }

    private int getBalance(BVHNode node) {
        if (node == null) {
            return 0;
        }
        return height(node.leftChild) - height(node.rightChild);
    }

    private boolean preferLeft(BVHNode node, Shape shape) {
        Rectangle bounds = calcBoundingBox(shape);
        BVHNode left = node.leftChild;
        BVHNode right = node.rightChild;
        double leftP = calcRectPerimeter(combineBoundingBoxes(Arrays.asList(bounds, left.bounds)));
        double rightP = calcRectPerimeter(combineBoundingBoxes(Arrays.asList(bounds, right.bounds)));
        if (leftP < rightP) {
            return true;
        } else if (leftP > rightP) {
            return false;
        } else if (left.height < right.height) {
            return true;
        } else if (right.height < left.height) {
            return false;
        } else {
            return Math.random() < 0.5;
        }
    }

    @Override
    public void remove(Shape shape)
    {
        if (root == null) {
            return;
        }
        shapeRemovedSuccessfully = false; // Reset for each call
        root = removeRecursive(root, shape);
    }

    private BVHNode removeRecursive(BVHNode node, Shape shape) {
        if (node == null) {
            return null;
        }
        if (node.isLeaf()) {
            if (node.shape.equals(shape)) {
                return null;
            }
            return node;
        }

        Rectangle targetBounds = calcBoundingBox(shape);
        boolean rightContains = contains(targetBounds, node.rightChild.bounds);
        boolean leftContains = contains(targetBounds, node.leftChild.bounds);

        if (leftContains) {
            node.leftChild = removeRecursive(node.leftChild, shape);
        }
        if (rightContains) {
            node.rightChild =  removeRecursive(node.rightChild, shape);
        }

        if (node.leftChild == null && node.rightChild == null) {
            return null;
        }

        if (node.leftChild == null) {
            return node.rightChild;
        }

        if (node.rightChild == null) {
            return node.leftChild;
        }

        node.bounds = combineBoundingBoxes(node.leftChild.bounds, node.rightChild.bounds);
        node.updateHeight();

        return rebalance(node);
    }

    // Checks if rect1 is completely contained within rect2
    private boolean contains(Rectangle rect1, Rectangle rect2) {
        if (rect2.minPos.getX() > rect1.minPos.getX()) {
            return false;
        }
        if (rect2.maxPos.getX() < rect1.maxPos.getX()) {
            return false;
        }

        if (rect2.minPos.getY() > rect1.minPos.getY()) {
            return false;
        }

        if (rect2.maxPos.getY() < rect1.maxPos.getY()) {
            return false;
        }
        return true;
    }

    @Override
    public Set<Shape> findCollision(Point2D.Double point)
    {
        if (root == null) {
            return new java.util.HashSet<>();
        }
        return findCollisionRecursive(root, point);
    }

    private Set<Shape> findCollisionRecursive(BVHNode node, Point2D.Double point) {
        if (node == null) {
            return new java.util.HashSet<>();
        }
        if (node.isLeaf()) {
            if (node.shape.containsPoint(point)) {
                Set<Shape> result = new java.util.HashSet<>();
                result.add(node.shape);
                return result;
            }
            return new java.util.HashSet<>();
        }

        if (!node.bounds.containsPoint(point)) {
            return new java.util.HashSet<>();
        }

        Set<Shape> leftCollisions = findCollisionRecursive(node.leftChild, point);
        Set<Shape> rightCollisions = findCollisionRecursive(node.rightChild, point);

        leftCollisions.addAll(rightCollisions);
        return leftCollisions;
    }

    private static class IntersectionInfo {
        Shape hitShape;
        double hitDistance;

        public IntersectionInfo(Shape hitShape, double hitDistance) {
            this.hitShape = hitShape;
            this.hitDistance = hitDistance;
        }
    }

    @Override
    public Shape intersectRay(Point2D.Double origin, Vector2D direction)
    {
        if (root == null) {
            return null; // No BVH, no intersection
        }
        // Start the recursive search. Initial closest distance is infinity.
        IntersectionInfo closestHit = intersectRayRecursive(root, origin, direction, Double.POSITIVE_INFINITY);

        return (closestHit != null) ? closestHit.hitShape : null;
    }

    private IntersectionInfo intersectRayRecursive(BVHNode node, Point2D.Double origin, Vector2D direction, double closestDistance) {
        if (node == null) {
            return null;
        }

        Point originInt = new Point((int) origin.getX(), (int) origin.getY()); // Explicit conversion to java.awt.Point

        if (!node.bounds.doesRayIntersect(originInt, direction)) {
            return null;
        }

        if (node.isLeaf()) {
            Point2D.Double intersectionPoint = node.shape.findIntersection(originInt, direction); // Assuming this takes Point
            if (intersectionPoint != null) {
                double hitDistance = BVH.distanceBetweenPoints(origin, intersectionPoint);
                if (hitDistance < closestDistance) {
                    return new IntersectionInfo(node.shape, hitDistance);
                }
            }
            return null;
        }

        IntersectionInfo leftHit = intersectRayRecursive(node.leftChild, origin, direction, closestDistance);
        IntersectionInfo rightHit = intersectRayRecursive(node.rightChild, origin, direction, closestDistance);

        // Determine the overall closest hit from this subtree
        if (leftHit == null && rightHit == null) {
            return null;
        } else if (leftHit == null) {
            return rightHit;
        } else if (rightHit == null) {
            return leftHit;
        } else {
            return (leftHit.hitDistance < rightHit.hitDistance) ? leftHit : rightHit;
        }
    }

    @Override
    public String toString() {
        if (this.root == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        toStringRecursive(this.root, 0, sb);
        return sb.toString();
    }

    private void toStringRecursive(BVHNode node, int depth, StringBuilder sb) {
        if (node == null) {
            return;
        }

        for (int i = 0; i < depth; i++) {
            sb.append("\t");
        }

        sb.append(node);
        sb.append("\n");

        toStringRecursive(node.leftChild, depth + 1, sb);
        toStringRecursive(node.rightChild, depth + 1, sb);
    }

    class BVHNode {
        Rectangle bounds;
        Shape shape;
        BVHNode leftChild;
        BVHNode rightChild;
        int height;

        // For leaf
        public BVHNode(Shape shape) {
            this.shape = shape;
            this.bounds = calcBoundingBox(shape);
            this.leftChild = null;
            this.rightChild = null;
            this.height = 0;
        }

        // For internal
        public BVHNode(Rectangle bounds, BVHNode left, BVHNode right) {
            this.shape = null;
            this.bounds = bounds;
            this.leftChild = left;
            this.rightChild = right;
            int leftHeight = (left == null) ? -1 : left.height;
            int rightHeight = (right == null) ? -1 : right.height;
            this.height = Math.max(leftHeight, rightHeight) + 1;
        }

        public boolean isLeaf() {
            return leftChild == null && rightChild == null;
        }

        public String toString() {
            if (isLeaf()) {
                return shape.toString();
            }
            return bounds.toString();
        }

        public void updateHeight() {
            if (isLeaf()) {
                this.height = 0;
            } else {
                int leftHeight = (leftChild == null) ? -1 : leftChild.height;
                int rightHeight = (rightChild == null) ? -1 : rightChild.height;
                this.height = 1 + Math.max(leftHeight, rightHeight);
            }
        }
    }

    public Rectangle calcBoundingBox(Shape shape){
        return new Rectangle(shape.getMinSurroundingPoint(), shape.getMaxSurroundingPoint());
    }

    public Rectangle combineBoundingBoxes(List<Rectangle> rectangles){
        return new Rectangle(
                new Point(
                        (int) rectangles.stream().mapToDouble(r -> r.minPos.getX()).min().orElse(0),
                        (int) rectangles.stream().mapToDouble(r -> r.minPos.getY()).min().orElse(0)
                ),
                new Point(
                        (int) rectangles.stream().mapToDouble(r -> r.maxPos.getX()).max().orElse(0),
                        (int) rectangles.stream().mapToDouble(r -> r.maxPos.getY()).max().orElse(0)
                )
        );
    }

    public Rectangle combineBoundingBoxes(Rectangle rect1, Rectangle rect2) {
        return combineBoundingBoxes(Arrays.asList(rect1, rect2));
    }


    private double calcRectPerimeter(Rectangle rectangle) {
        return 2 * (rectangle.maxPos.x - rectangle.minPos.x) + 2 * (rectangle.maxPos.y - rectangle.minPos.y);
    }

}