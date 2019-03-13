package clusterproject.clustergenerator.program.ClusteringResultsViewer;

import java.awt.GridLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import clusterproject.clustergenerator.data.PointContainer;
import clusterproject.clustergenerator.program.ClusterViewerElement.ScatterPlot;

public class DifferenceWindow extends JFrame {
	private static final long serialVersionUID = -8757028109183157947L;
	private final PointContainer container1;
	private final PointContainer container2;
	private final PointContainer intersection;
	private final ScatterPlot scatterPlot1;
	private final ScatterPlot scatterPlot2;
	private final ScatterPlot intersectionScatterPlot;

	public DifferenceWindow(PointContainer c1, PointContainer c2) {
		container1 = c1;
		container2 = c2;
		setLayout(new GridLayout(1, 3));
		scatterPlot1 = new ScatterPlot(c1, true);
		scatterPlot1.autoAdjust();
		scatterPlot1.addAutoAdjust();

		scatterPlot2 = new ScatterPlot(c2, true);
		scatterPlot2.autoAdjust();
		scatterPlot2.addAutoAdjust();

		intersection = new PointContainer(container1.getDim());
		intersection.addPoints(container1.getPoints());
		intersection.setUpClusters();
		final List<double[]> c1Points = container1.getPoints();
		final List<double[]> c2Points = container2.getPoints();
		final List<Integer> c1IDs = container1.getClusterIDs();
		final List<Integer> c2IDs = container2.getClusterIDs();
		final int size = container1.getClusterIDs().size();
		final Map<double[], Integer> idMapc2 = new HashMap<double[], Integer>();
		for (int i = 0; i < size; ++i)
			idMapc2.put(c2Points.get(i), c2IDs.get(i));
		final Set<Integer> filtered = new HashSet<Integer>();
		for (int i = 0; i < size; ++i) {
			final double[] point = c1Points.get(i);
			if (c1IDs.get(i) != idMapc2.get(point)) {
				intersection.addClusterID(-1);
				filtered.add(i);
			} else
				intersection.addClusterID(-2);
		}
		final int differentCount = filtered.size();
		setTitle(((float) ((double) differentCount / size) * 100) + "% Different");
		intersection.setFilteredResults(filtered);

		intersectionScatterPlot = new ScatterPlot(intersection, true);
		intersectionScatterPlot.autoAdjust();
		intersectionScatterPlot.addAutoAdjust();
		add(scatterPlot1);
		add(intersectionScatterPlot);
		add(scatterPlot2);
	}

}
