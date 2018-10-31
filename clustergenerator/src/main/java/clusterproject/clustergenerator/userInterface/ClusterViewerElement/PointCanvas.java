package clusterproject.clustergenerator.userInterface.ClusterViewerElement;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.JPanel;

import clusterproject.clustergenerator.data.PointContainer;
import clusterproject.clustergenerator.userInterface.ScatterPlot;

public class PointCanvas extends JPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private final PointContainer pointContainer;
	private final ScatterPlot clusterViewer;

	private double[] yCoordinates;// TODO: this needs to be in a shared container
	private double[] xCoordinates;

	public PointCanvas(PointContainer pointContainer, ScatterPlot clusterViewer) {
		this.pointContainer = pointContainer;
		this.clusterViewer = clusterViewer;
		setOpaque(false);
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		reset();
	}

	@Override
	public void paint(Graphics g) {

		super.paint(g);
		final Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final int pointWidth = clusterViewer.getPointDiameter();
		final int pointCount = pointContainer.getPoints().size();
		if (yCoordinates == null) {
			yCoordinates = new double[pointCount];
			xCoordinates = new double[pointCount];

			final List<double[]> points = pointContainer.getPoints();

			IntStream.range(0, yCoordinates.length)
					.forEach(i -> yCoordinates[i] = clusterViewer.getPixelY(points.get(i)));
			IntStream.range(0, xCoordinates.length)
					.forEach(i -> xCoordinates[i] = clusterViewer.getPixelX(points.get(i)));
		}

		for (int i = 0; i < pointCount; ++i) {
			if (xCoordinates[i] == Double.NaN || yCoordinates[i] == Double.NaN)
				continue;
			g2.setColor(Color.GRAY);
			g2.fillOval((int) xCoordinates[i] - pointWidth / 2, (int) yCoordinates[i] - pointWidth / 2, pointWidth,
					pointWidth);
			g2.setColor(Color.BLACK);
			g2.drawOval((int) xCoordinates[i] - pointWidth / 2, (int) yCoordinates[i] - pointWidth / 2, pointWidth,
					pointWidth);
		}
	}

	public void reset() {
		yCoordinates = null;// TODO: this will need to be changed
		xCoordinates = null;// maybe there will be cases where not everything needs to be reset i.e. editing
							// axis bounds

	}

}
