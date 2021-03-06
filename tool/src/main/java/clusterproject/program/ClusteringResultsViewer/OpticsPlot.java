package clusterproject.program.ClusteringResultsViewer;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

import clusterproject.data.ClusteringResult;
import clusterproject.program.ClusteringResultsViewer.FilterWindow.HistogramData;
import clusterproject.program.MetaClustering.OpticsContainer;
import clusterproject.program.MetaClustering.OpticsResult;
import clusterproject.util.Util;

public class OpticsPlot extends JLayeredPane {

	private static final long serialVersionUID = 7515062269771305939L;

	private final static int BAR_OFFSET = 20;
	private static final int LABLE_OFFSET = 5;
	public static final boolean DEBUG_FORCE_SHOW_TOOLTIP = false;

	private final OpticsResult clusteringList;
	private final JPanel opticsBars;
	private final SpringLayout layout;
	private double threshhold = -Double.MIN_NORMAL;
	private double max = 0;
	private final MetaViewer clusteringViewer;
	private final JButton setHistogramDataButton;
	private final Map<Integer, Integer> indexMap;

	public OpticsPlot(MetaViewer clusteringViewer, OpticsResult clusteredList) {
		this.clusteringViewer = clusteringViewer;
		this.clusteringList = clusteredList;
		layout = new SpringLayout();
		setLayout(layout);
		this.indexMap = new HashMap<Integer, Integer>();
		int key = 0;
		for (final OpticsContainer clu : clusteredList)
			indexMap.put(key++, clu.inIndex);
		setOpaque(false);
		setHistogramDataButton = new JButton("Clusters to Histogram");
		setHistogramDataButton.addActionListener(e -> {
			adaptHistogramData();

		});
		add(setHistogramDataButton, new Integer(22));
		layout.putConstraint(SpringLayout.NORTH, setHistogramDataButton, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, setHistogramDataButton, 0, SpringLayout.EAST, this);
		setHistogramDataButton.setBackground(new Color(255, 255, 255, 0));// color
																			// doesnt
																			// matter
																			// just
																			// for
																			// transparancy
		// to kick in
		setHistogramDataButton.setOpaque(false);
		setHistogramDataButton.setFocusable(false);
		opticsBars = new JPanel();
		opticsBars.setOpaque(false);
		opticsBars.setLayout(new BorderLayout());

		layout.putConstraint(SpringLayout.NORTH, opticsBars, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, opticsBars, 0, SpringLayout.EAST, this);
		layout.putConstraint(SpringLayout.SOUTH, opticsBars, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, opticsBars, BAR_OFFSET, SpringLayout.WEST, this);
		for (int i = 1; i < clusteredList.size(); ++i) {
			if (clusteredList.get(i).distance > max && clusteredList.get(i).distance < Double.MAX_VALUE)
				max = clusteredList.get(i).distance;
		}
		if (max == 0)
			max = 1;
		else
			max *= 1.1;

		opticsBars.add(new OpticsDataPainter(this, clusteredList));
		add(opticsBars, new Integer(20));
		final JPanel threshholdClicker = new JPanel() {
			private static final long serialVersionUID = -343022910473819436L;

			@Override
			public void paint(Graphics g) {
				super.paint(g);
				final Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				final DecimalFormat df = new DecimalFormat("#.##");
				g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
				Util.drawRotatedString(g2, LABLE_OFFSET, LABLE_OFFSET, 90, df.format(max));
				final int endTickWidth = g.getFontMetrics().stringWidth(df.format(0));
				Util.drawRotatedString(g2, LABLE_OFFSET, getHeight() - LABLE_OFFSET - endTickWidth, 90, df.format(0));
			}
		};
		threshholdClicker.setBackground(Color.LIGHT_GRAY);
		layout.putConstraint(SpringLayout.NORTH, threshholdClicker, 0, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.EAST, threshholdClicker, 0, SpringLayout.WEST, opticsBars);
		layout.putConstraint(SpringLayout.SOUTH, threshholdClicker, 0, SpringLayout.SOUTH, this);
		layout.putConstraint(SpringLayout.WEST, threshholdClicker, 0, SpringLayout.WEST, this);
		add(threshholdClicker, new Integer(20));
		final MouseAdapter adapter = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				adaptThreshhold((((double) opticsBars.getHeight() - e.getY()) / opticsBars.getHeight()) * max);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				adaptThreshhold((((double) opticsBars.getHeight() - e.getY()) / opticsBars.getHeight()) * max);
			}
		};

		threshholdClicker.addMouseMotionListener(adapter);
		threshholdClicker.addMouseListener(adapter);
		adaptThreshhold(max / 2);

	}

	private void adaptHistogramData() {
		List<ClusteringResult> allClusterings = clusteringViewer.getClusterings();
		final List<ClusteringResult> newData = new ArrayList<ClusteringResult>();
		if (!clusteringList.isEmpty()
				&& !(allClusterings.get(clusteringList.get(0).inIndex) instanceof ClusteringResult))
			return;
		for (final OpticsContainer c : clusteringList)
			if (c.tag >= 0)
				newData.add(allClusterings.get(c.inIndex));
		// if (newData.size() == clusteringList.size())
		// clusteringViewer.setHistogramData(newData, "All Clusterings");
		// else
		clusteringViewer.setHistogramData(newData, HistogramData.Colored);

	}

	public LinkedHashSet<Integer> getHighlighted() {
		return clusteringViewer.getHighlighted();
	}

	public Set<Integer> getFilteredIndexes() {
		return clusteringViewer.getFilteredIndexes();
	}

	private void adaptThreshhold(double newthreshhold) {

		if (newthreshhold < 0)
			newthreshhold = -Double.MIN_VALUE;
		if (newthreshhold > max)
			newthreshhold = max;
		if (newthreshhold == threshhold)
			return;
		threshhold = newthreshhold;
		clusteringList.calculateClusters(threshhold);
		clusteringViewer.updateMDSPlot(clusteringList);
		if (HistogramData.Colored.equals(clusteringViewer.getHistogramData())) {
			adaptHistogramData();
		}
		SwingUtilities.invokeLater(() -> repaint());
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		final double height = (getHeight()) * (1 - threshhold / max);
		g.drawLine(0, (int) height, getWidth(), (int) height);

	}

	public void highlight(int selection, boolean replace, boolean singleClick) {
		final List<Integer> highlighted = new ArrayList<Integer>();
		highlighted.add(selection);
		clusteringViewer.mouseHighlight(selection, replace, singleClick);
	}

	public int getTruth() {
		return clusteringViewer.getGroundTruth();
	}

	public Double getDistanceToTruth(int i) {
		return clusteringViewer.getDistanceToTruth(i);
	}

	public Double getNMIToTruth(int i) {
		return clusteringViewer.getNMIToTruth(i);
	}

	private class OpticsDataPainter extends JComponent {
		private static final long serialVersionUID = 1159563867382183801L;
		private final static int INNER_SPACE = 2;
		private final static int BORDER_MIN_SIZE = 2;
		final OpticsPlot plot;
		final OpticsResult clusteringList;
		final List<Double> percentages;

		public OpticsDataPainter(OpticsPlot plot, OpticsResult clusteredList) {
			this.plot = plot;
			this.clusteringList = clusteredList;
			percentages = new ArrayList<Double>();
			for (final OpticsContainer clu : clusteredList)
				percentages.add(Math.min(clu.distance / max, 1));
			final MouseAdapter listener = new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					final int width = getWidth();
					final int length = clusteredList.size();
					final double singleWidth = width / (double) length;
					final int selected = (int) ((e.getX()) / singleWidth);
					// if(e.isShiftDown())
					// shiftHighlight(selection);//TODO: maybe shift selection?
					// else
					highlight(plot.getInIndex(selected), !e.isControlDown(), e.getClickCount() == 1);
				}

				@Override
				public void mouseMoved(MouseEvent e) {

					final int width = getWidth();
					final int length = clusteredList.size();
					final double singleWidth = width / (double) length;
					final int selected = (int) ((e.getX()) / singleWidth);
					final int myid = plot.getInIndex(selected);
					final Double dist = plot.getDistanceToTruth(myid);

					if (!dist.equals(Double.NaN) || DEBUG_FORCE_SHOW_TOOLTIP) {
						if (Math.abs(dist) < Double.MIN_NORMAL)
							setToolTipText("<html>" + "Equal to Ground Truth" + "</html>");
						else
							setToolTipText("<html>" + "Distance to Ground Truth: "
									+ Float.toString((float) (double) dist) + "<br> NMI: "
									+ Float.toString((float) (double) plot.getNMIToTruth(myid)) + "</html>");
					} else
						setToolTipText(null);
				}
			};
			addMouseListener(listener);
			addMouseMotionListener(listener);
		}

		@Override
		public void paint(Graphics g) {
			final Graphics2D g2 = (Graphics2D) g;
			final Set<Integer> filtered = plot.getFilteredIndexes();
			final int truth = plot.getTruth();
			final int width = getWidth();
			final double singleWidth = width / (double) clusteringList.size();
			double startx = 0;
			final int height = getHeight();
			for (int i = 0; i < clusteringList.size(); ++i) {
				final Color color = Util.getColor(clusteringList.get(i).tag + 2);
				final int myid = plot.getInIndex(i);
				final double heightPercent = percentages.get(i);
				final boolean highlighted = plot.getHighlighted().contains(myid);
				if (filtered != null && !filtered.contains(myid) && myid != truth)
					g2.setComposite(AlphaComposite.SrcOver.derive(Util.FILTER_ALPHA));
				else
					g2.setComposite(AlphaComposite.SrcOver);

				if (singleWidth - INNER_SPACE > BORDER_MIN_SIZE && !highlighted) {
					final int istartx = (int) Math.round(startx + INNER_SPACE / 2);
					final int iendx = (int) Math.round(startx - INNER_SPACE / 2 + singleWidth);
					final int istarty = (int) (height * (1 - heightPercent)) + 1;
					final int iendy = height - 1;
					final int istarty2 = (int) Math.min((height * (1 - heightPercent)), height - 2);
					final int iendy2 = height - 1;
					g2.setColor(color);
					if (istarty < iendy)
						g2.fillPolygon(new int[] { istartx, iendx, iendx, istartx },
								new int[] { istarty, istarty, iendy, iendy }, 4);
					g2.setColor(Color.BLACK);
					if (istarty2 < iendy2)
						g2.drawPolygon(new int[] { istartx, iendx, iendx, istartx },
								new int[] { istarty2, istarty2, iendy2, iendy2 }, 4);

				} else {
					if (highlighted) {
						final int istartx;
						final int iendx;
						final int iendy;
						if (singleWidth - INNER_SPACE > BORDER_MIN_SIZE) {
							istartx = (int) Math.round(startx + INNER_SPACE / 2);
							iendx = (int) Math.round(startx + singleWidth);
							iendy = height;
						} else {
							istartx = (int) Math.round(startx);
							iendx = (int) Math.round(startx + singleWidth);
							iendy = height - 1;
						}
						final int istarty = Math.min((int) (height * (1 - heightPercent)), height - 1);
						g2.setColor(Color.lightGray);
						g2.fillPolygon(new int[] { istartx, iendx, iendx, istartx }, new int[] { 0, 0, iendy, iendy },
								4);
						g2.setColor(Util.HIGHLIGHT_COLOR);
						if (istarty < iendy)
							g2.fillPolygon(new int[] { istartx, iendx, iendx, istartx },
									new int[] { istarty, istarty, iendy, iendy }, 4);
					} else {
						final int istartx = (int) Math.round(startx);
						final int iendx = (int) Math.round(startx + singleWidth);
						int istarty = (int) (height * (1 - heightPercent) - 1);

						final int iendy = height - 1;
						if (istarty >= iendy)
							istarty = iendy - 1;
						g2.setColor(color);
						if (istarty < iendy)
							g2.fillPolygon(new int[] { istartx, iendx, iendx, istartx },
									new int[] { istarty, istarty, iendy, iendy }, 4);
					}
				}
				startx += singleWidth;
			}
		}
	}

	private int getInIndex(int selected) {
		return indexMap.get(selected);
	}

}
