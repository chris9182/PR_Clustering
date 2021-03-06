package clusterproject.program.ClusterViewerElement;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import clusterproject.data.PointContainer;
import clusterproject.util.Util;

public class PointCanvas extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int HIGHLIGHT_FACTOR = 2;
	private static final int MIN_WIDTH_FOR_BORDER = 5;
	private static final Color HIGHLIGHT_COLOR = Util.HIGHLIGHT_COLOR;
	private static final double TRUTH_FACTOR = 1.5;
	private static final int MAX_POINTS = 10000;

	private final PointContainer pointContainer;
	private final ScatterPlot clusterViewer;

	private Point down;

	private Point current;

	public PointCanvas(PointContainer pointContainer, ScatterPlot clusterViewer) {
		this.pointContainer = pointContainer;
		this.clusterViewer = clusterViewer;
		setOpaque(false);
	}

	@Override
	public void paint(Graphics g) {
		final Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
		PointContainer drawingContainer = pointContainer;
		if (drawingContainer.getPoints().size() > MAX_POINTS) {
			drawingContainer = pointContainer.getSample(MAX_POINTS);
		}
		final int pointWidth = clusterViewer.getPointDiameter();
		final int pointCount = drawingContainer.getPoints().size();
		final double[] yCoordinates = new double[pointCount];
		final double[] xCoordinates = new double[pointCount];

		final List<double[]> points = drawingContainer.getPoints();

		for (int i = 0; i < pointCount; ++i) {
			yCoordinates[i] = clusterViewer.getPixelY(points.get(i));
		}
		for (int i = 0; i < pointCount; ++i) {
			xCoordinates[i] = clusterViewer.getPixelX(points.get(i));
		}

		if (!drawingContainer.hasClusters()) {
			for (int i = 0; i < pointCount; ++i) {
				if (Double.isNaN(xCoordinates[i]) || Double.isNaN(yCoordinates[i]))
					continue;

				g2.setColor(Color.GRAY);
				g2.fillOval((int) xCoordinates[i] - pointWidth / 2, (int) yCoordinates[i] - pointWidth / 2, pointWidth,
						pointWidth);
				g2.setColor(Color.BLACK);
				g2.drawOval((int) xCoordinates[i] - pointWidth / 2, (int) yCoordinates[i] - pointWidth / 2, pointWidth,
						pointWidth);

			}
		} else {
			final List<Integer> clusterIDs = drawingContainer.getClusterInformation().getClusterIDs();
			final int noiseIndex = drawingContainer.getClusterInformation().getCurrentNoiseIndex();
			final Stream<Integer> stream = clusterIDs.stream().distinct();
			final Map<Integer, Color> colorMap = new HashMap<Integer, Color>();
			final Set<Integer> filtered = drawingContainer.getMetaInformation().getFilteredIndexes();
			final LinkedHashSet<Integer> highlighted = drawingContainer.getMetaInformation().getHighlighted();
			final int truth = drawingContainer.getMetaInformation().getGroundTruth();
			final Iterator<Integer> iter = stream.iterator();
			while (iter.hasNext()) {
				final int i = iter.next();
				if (i != noiseIndex)
					colorMap.put(i, Util.getColor(i + 2));
				else
					colorMap.put(i, Util.getColor(0));
			}

			for (int i = 0; i < pointCount; ++i) {
				if (Double.isNaN(xCoordinates[i]) || Double.isNaN(yCoordinates[i]) || highlighted.contains(i))
					continue;
				if (filtered != null && !filtered.contains(i))
					g2.setComposite(AlphaComposite.SrcOver.derive(Util.FILTER_ALPHA));
				else
					g2.setComposite(AlphaComposite.SrcOver);

				g2.setColor(colorMap.get(clusterIDs.get(i)));

				g2.fillOval((int) xCoordinates[i] - pointWidth / 2, (int) yCoordinates[i] - pointWidth / 2, pointWidth,
						pointWidth);
				if (pointWidth >= MIN_WIDTH_FOR_BORDER) {
					if (filtered == null) {
						g2.setColor(Color.BLACK);
					} else {
						if (filtered.contains(i)) {
							g2.setColor(Color.BLACK);
						} else {
							g2.setColor(Color.GRAY);
						}
					}

					g2.drawOval((int) xCoordinates[i] - pointWidth / 2, (int) yCoordinates[i] - pointWidth / 2,
							pointWidth, pointWidth);
				}

			}

			if ((highlighted.size() >= 1))

				for (final int hIndex : highlighted) {
					if (hIndex == truth)
						continue;
					if (filtered != null && !filtered.contains(hIndex))
						g2.setComposite(AlphaComposite.SrcOver.derive(Util.FILTER_ALPHA));
					else
						g2.setComposite(AlphaComposite.SrcOver);

					g2.setColor(HIGHLIGHT_COLOR);

					g2.fillOval((int) (xCoordinates[hIndex] - pointWidth * HIGHLIGHT_FACTOR / 2),
							(int) (yCoordinates[hIndex] - pointWidth * HIGHLIGHT_FACTOR / 2),
							pointWidth * HIGHLIGHT_FACTOR, pointWidth * HIGHLIGHT_FACTOR);
					if (pointWidth >= MIN_WIDTH_FOR_BORDER) {
						g2.setColor(Color.BLACK);
						g2.drawOval((int) (xCoordinates[hIndex] - pointWidth * HIGHLIGHT_FACTOR / 2),
								(int) (yCoordinates[hIndex] - pointWidth * HIGHLIGHT_FACTOR / 2),
								pointWidth * HIGHLIGHT_FACTOR, pointWidth * HIGHLIGHT_FACTOR);
					}
				}

			if (truth >= 0) {
				g2.setComposite(AlphaComposite.SrcOver);
				g2.setColor(colorMap.get(clusterIDs.get(truth)));
				if (highlighted.contains(truth))
					g2.setColor(HIGHLIGHT_COLOR);
				else
					g2.setColor(colorMap.get(clusterIDs.get(truth)));
				final Shape star = createDefaultStar(pointWidth * TRUTH_FACTOR / 2, (xCoordinates[truth]),
						((yCoordinates[truth])));
				g2.fill(star);
				if (pointWidth >= MIN_WIDTH_FOR_BORDER) {
					g2.setColor(Color.BLACK);
					g2.draw(star);
				}
			}
		}
		if (down != null && current != null) {
			g2.setColor(Color.gray);
			g2.setComposite(AlphaComposite.SrcOver.derive(0.5f));
			final int x = (int) (down.getX() < current.getX() ? down.getX() : current.getX());
			final int y = (int) (down.getY() < current.getY() ? down.getY() : current.getY());
			final int xu = (int) (down.getX() > current.getX() ? down.getX() : current.getX());
			final int yu = (int) (down.getY() > current.getY() ? down.getY() : current.getY());
			g2.fillRect(x, y, xu - x, yu - y);
			g2.setColor(Color.black);
			g2.setComposite(AlphaComposite.SrcOver);
			g2.drawRect(x, y, xu - x, yu - y);
		}
	}

	private static Shape createDefaultStar(double radius, double centerX, double centerY) {
		return createStar(centerX, centerY, radius, radius * 2.63, 5, Math.toRadians(-18));
	}

	private static Shape createStar(double centerX, double centerY, double innerRadius, double outerRadius, int numRays,
			double startAngleRad) {
		final Path2D path = new Path2D.Double();
		final double deltaAngleRad = Math.PI / numRays;
		for (int i = 0; i < numRays * 2; i++) {
			final double angleRad = startAngleRad + i * deltaAngleRad;
			final double ca = Math.cos(angleRad);
			final double sa = Math.sin(angleRad);
			double relX = ca;
			double relY = sa;
			if ((i & 1) == 0) {
				relX *= outerRadius;
				relY *= outerRadius;
			} else {
				relX *= innerRadius;
				relY *= innerRadius;
			}
			if (i == 0) {
				path.moveTo(centerX + relX, centerY + relY);
			} else {
				path.lineTo(centerX + relX, centerY + relY);
			}
		}
		path.closePath();
		return path;
	}

	public void setSelection(Point down, Point current) {
		this.down = down;
		this.current = current;
		SwingUtilities.invokeLater(() -> repaint());

	}
}
