import assignment.BVH;
import assignment.BoundedVolumeHierarchy;
import assignment.Rectangle;
import assignment.Shape;
import assignment.Triangle;
import assignment.Vector2D;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BVHTests {

    @Test
    public void buildBVHFindCollisionReturnsContainingTriangle() {
        Triangle left = new Triangle(
                new Point(-10, 0),
                new Point(0, 10),
                new Point(10, 0));
        Triangle right = new Triangle(
                new Point(50, 50),
                new Point(60, 50),
                new Point(55, 65));

        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.buildBVH(toShapeList(left, right));

        Set<Shape> hitsNearOrigin = bvh.findCollision(new Point2D.Double(0, 5));
        assertTrue(hitsNearOrigin.contains(left));

        Set<Shape> hitsFar = bvh.findCollision(new Point2D.Double(55, 60));
        assertTrue(hitsFar.contains(right));

        assertTrue(bvh.findCollision(new Point2D.Double(-100, -100)).isEmpty());
    }

    @Test
    public void insertAndRemoveMaintainShapePresence() {
        Triangle dynamic = new Triangle(
                new Point(-20, -10),
                new Point(-5, 5),
                new Point(5, -5));
        Triangle survivor = new Triangle(
                new Point(80, 0),
                new Point(90, 15),
                new Point(95, -5));

        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.insert(dynamic);

        assertTrue(bvh.findCollision(new Point2D.Double(-10, -3)).contains(dynamic));

        bvh.insert(survivor);
        assertTrue(bvh.findCollision(new Point2D.Double(88, 2)).contains(survivor));

        bvh.remove(dynamic);
        assertFalse(bvh.findCollision(new Point2D.Double(-10, -3)).contains(dynamic));
        assertTrue(bvh.findCollision(new Point2D.Double(88, 2)).contains(survivor));

        bvh.remove(survivor);
        assertTrue(bvh.findCollision(new Point2D.Double(88, 2)).isEmpty());
    }

    @Test
    public void intersectRayReturnsClosestShape() {
        Triangle closer = new Triangle(
                new Point(10, -5),
                new Point(10, 5),
                new Point(20, 0));
        Triangle farther = new Triangle(
                new Point(40, -5),
                new Point(40, 5),
                new Point(55, 0));

        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.buildBVH(toShapeList(closer, farther));

        Shape hit = bvh.intersectRay(new Point2D.Double(0, 0), new Vector2D(1, 0));
        assertSame(closer, hit);

        Shape miss = bvh.intersectRay(new Point2D.Double(-100, -100), new Vector2D(-1, 0));
        assertNull(miss);
    }

    @Test
    public void emptyBVHReturnsNoIntersections() {
        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        assertTrue(bvh.findCollision(new Point2D.Double(0, 0)).isEmpty());
        assertNull(bvh.intersectRay(new Point2D.Double(0, 0), new Vector2D(1, 1)));
    }

    @Test
    public void setSplitMethodProducesConsistentQueries() {
        Triangle triA = new Triangle(new Point(-30, 0), new Point(-20, 20), new Point(-10, 0));
        Triangle triB = new Triangle(new Point(25, -10), new Point(35, 15), new Point(45, -5));

        for (BVH.SplitMethod splitMethod : BVH.SplitMethod.values()) {
            BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
            bvh.setSplitMethod(splitMethod);
            bvh.buildBVH(toShapeList(triA, triB));

            assertTrue(bvh.findCollision(new Point2D.Double(-20, 5)).contains(triA),
                    () -> "Split " + splitMethod + " should still allow collision queries.");
            assertTrue(bvh.findCollision(new Point2D.Double(35, 0)).contains(triB),
                    () -> "Split " + splitMethod + " should still allow collision queries.");
        }
    }

    @Test
    public void removingNonexistentShapeDoesNotAffectOthers() {
        Triangle survivor = new Triangle(new Point(0, 0), new Point(10, 0), new Point(5, 10));
        Triangle ghost = new Triangle(new Point(100, 100), new Point(110, 100), new Point(105, 110));

        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.insert(survivor);
        bvh.remove(ghost);

        assertTrue(bvh.findCollision(new Point2D.Double(5, 2)).contains(survivor));
    }

    @Test
    public void reinsertingShapeRestoresCollisionDetection() {
        Triangle shape = new Triangle(new Point(-5, -5), new Point(-5, 5), new Point(5, 0));
        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();

        bvh.insert(shape);
        bvh.remove(shape);
        assertFalse(bvh.findCollision(new Point2D.Double(0, 0)).contains(shape));

        bvh.insert(shape);
        assertTrue(bvh.findCollision(new Point2D.Double(0, 0)).contains(shape));
    }

    @Test
    public void toStringMatchesSpecificationOrderingAndBoundingVolumes() {
        Triangle t1 = new Triangle(new Point(-12, -4), new Point(-8, 2), new Point(-5, -3));
        Triangle t2 = new Triangle(new Point(5, 1), new Point(8, 6), new Point(10, 2));
        Triangle t3 = new Triangle(new Point(15, -6), new Point(18, -2), new Point(20, -5));

        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.buildBVH(toShapeList(t1, t2, t3));

        String tree = bvh.toString();
        assertFalse(tree.isBlank());

        BVHStringParser.ParsedNode root = BVHStringParser.parse(tree);
        assertNotNull(root);
        Rectangle expectedRoot = boundingBoxForTriangles(t1, t2, t3);
        assertRectangleEquals(expectedRoot, root.boundingBox);

        List<Triangle> leaves = new ArrayList<>();
        collectLeafTriangles(root, leaves);
        assertEquals(3, leaves.size());
        assertTrue(leaves.contains(t1));
        assertTrue(leaves.contains(t2));
        assertTrue(leaves.contains(t3));

        List<String> parserPreOrder = new ArrayList<>();
        collectPreOrderValues(root, parserPreOrder);
        List<String> stringPreOrder = stripIndentationFromLines(tree);
        assertEquals(parserPreOrder, stringPreOrder);
    }

    @Test
    public void overlappingBoundingBoxesStillRepresentDistinctLeaves() {
        Triangle overlapA = new Triangle(new Point(0, 0), new Point(4, 0), new Point(2, 4));
        Triangle overlapB = new Triangle(new Point(1, 1), new Point(5, 1), new Point(3, 5));

        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.buildBVH(toShapeList(overlapA, overlapB));

        BVHStringParser.ParsedNode root = BVHStringParser.parse(bvh.toString());
        assertNotNull(root);
        assertRectangleEquals(boundingBoxForTriangles(overlapA, overlapB), root.boundingBox);

        List<Triangle> leaves = new ArrayList<>();
        collectLeafTriangles(root, leaves);
        assertEquals(2, leaves.size());
        assertTrue(leaves.contains(overlapA));
        assertTrue(leaves.contains(overlapB));

        Set<Shape> collisions = bvh.findCollision(new Point2D.Double(2, 2));
        assertTrue(collisions.contains(overlapA));
        assertTrue(collisions.contains(overlapB));
    }

    @Test
    public void heavyOverlapInsertAndRemoveMaintainsStructureAndBalance() {
        List<Triangle> overlaps = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            int base = i * 2;
            overlaps.add(new Triangle(
                    new Point(base, base),
                    new Point(base + 4, base),
                    new Point(base + 2, base + 4)
            ));
        }

        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.buildBVH(toShapeList(overlaps));
        assertAVLInvariant(bvh);

        BVHStringParser.ParsedNode root = BVHStringParser.parse(bvh.toString());
        assertNotNull(root);
        assertRectangleEquals(boundingBoxForTriangles(overlaps.toArray(Triangle[]::new)), root.boundingBox);

        List<Triangle> leaves = new ArrayList<>();
        collectLeafTriangles(root, leaves);
        assertTrue(leaves.containsAll(overlaps));

        Point2D.Double diagonalPoint = new Point2D.Double(6, 6);
        Set<Shape> diagonalHits = bvh.findCollision(diagonalPoint);
        assertFalse(diagonalHits.isEmpty());

        for (Triangle triangle : overlaps) {
            bvh.remove(triangle);
            assertAVLInvariant(bvh);
            assertFalse(bvh.findCollision(triangle.getCenter()).contains(triangle));
        }

        assertTrue(bvh.findCollision(new Point2D.Double(0, 0)).isEmpty());
    }

    @Test
    public void sequentialInsertionsMaintainAVLBalance() {
        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        List<Triangle> shapes = generateLinearChainTriangles(8, 0);

        for (Triangle triangle : shapes) {
            bvh.insert(triangle);
            assertAVLInvariant(bvh);
        }
    }

    @Test
    public void removalsMaintainAVLBalance() {
        List<Triangle> shapes = generateLinearChainTriangles(10, 100);
        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.buildBVH(toShapeList(shapes));
        assertAVLInvariant(bvh);

        for (int i = 0; i < shapes.size(); i++) {
            Triangle toRemove = shapes.get(i);
            bvh.remove(toRemove);
            assertAVLInvariant(bvh);
        }
    }

    @Test
    public void duplicateInsertionsAndRemovalsKeepTreeBalanced() {
        Triangle shared = new Triangle(new Point(0, 0), new Point(0, 10), new Point(5, 5));
        Triangle neighbor = new Triangle(new Point(20, 0), new Point(25, 5), new Point(30, 0));

        BoundedVolumeHierarchy bvh = new BoundedVolumeHierarchy();
        bvh.insert(shared);
        bvh.insert(shared);
        bvh.insert(neighbor);
        assertAVLInvariant(bvh);

        bvh.remove(shared);
        assertAVLInvariant(bvh);

        bvh.remove(shared);
        assertAVLInvariant(bvh);

        bvh.remove(neighbor);
        assertAVLInvariant(bvh);
    }

    private Rectangle boundingBoxForTriangles(Triangle... triangles) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Triangle triangle : triangles) {
            Point min = triangle.getMinSurroundingPoint();
            Point max = triangle.getMaxSurroundingPoint();

            minX = Math.min(minX, min.x);
            minY = Math.min(minY, min.y);
            maxX = Math.max(maxX, max.x);
            maxY = Math.max(maxY, max.y);
        }

        return new Rectangle(new Point(minX, minY), new Point(maxX, maxY));
    }

    private void assertRectangleEquals(Rectangle expected, Rectangle actual) {
        assertNotNull(actual);
        assertEquals(expected.minPos, actual.minPos);
        assertEquals(expected.maxPos, actual.maxPos);
    }

    private void collectLeafTriangles(BVHStringParser.ParsedNode node, List<Triangle> leaves) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            leaves.add(node.triangle);
            return;
        }
        for (BVHStringParser.ParsedNode child : node.children) {
            collectLeafTriangles(child, leaves);
        }
    }

    private void collectPreOrderValues(BVHStringParser.ParsedNode node, List<String> values) {
        if (node == null) {
            return;
        }
        values.add(node.rawValue);
        for (BVHStringParser.ParsedNode child : node.children) {
            collectPreOrderValues(child, values);
        }
    }

    private List<String> stripIndentationFromLines(String tree) {
        List<String> values = new ArrayList<>();
        String[] lines = tree.split("\\R");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.stripTrailing();
            if (trimmed.isEmpty()) {
                continue;
            }
            int idx = 0;
            while (idx < trimmed.length() && trimmed.charAt(idx) == '\t') {
                idx++;
            }
            values.add(trimmed.substring(idx));
        }
        return values;
    }

    private void assertAVLInvariant(BVH bvh) {
        String representation = bvh.toString();
        if (representation == null || representation.isBlank()) {
            return; // Empty tree is trivially balanced.
        }
        BVHStringParser.ParsedNode root = BVHStringParser.parse(representation);
        assertNotNull(root);
        computeHeightAndAssertBalance(root);
    }

    private int computeHeightAndAssertBalance(BVHStringParser.ParsedNode node) {
        if (node == null) {
            return -1;
        }
        if (node.children.isEmpty()) {
            return 0;
        }
        assertTrue(node.children.size() <= 2,
                () -> "BVH nodes should be binary; found " + node.children.size() + " children in " + node.rawValue);

        int leftHeight = computeHeightAndAssertBalance(node.children.get(0));
        int rightHeight = (node.children.size() > 1)
                ? computeHeightAndAssertBalance(node.children.get(1))
                : -1;

        assertTrue(Math.abs(leftHeight - rightHeight) <= 1,
                () -> "AVL invariant violated at node " + node.rawValue + " with balance "
                        + (leftHeight - rightHeight));

        return 1 + Math.max(leftHeight, rightHeight);
    }

    private List<Triangle> generateLinearChainTriangles(int count, int startX) {
        List<Triangle> triangles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int baseX = startX + i * 15;
            triangles.add(new Triangle(
                    new Point(baseX, 0),
                    new Point(baseX + 5, 10),
                    new Point(baseX + 10, 0)
            ));
        }
        return triangles;
    }

    private List<Shape> toShapeList(Shape... shapes) {
        return new ArrayList<>(Arrays.asList(shapes));
    }

    private List<Shape> toShapeList(List<? extends Shape> shapes) {
        return new ArrayList<>(shapes);
    }
}
