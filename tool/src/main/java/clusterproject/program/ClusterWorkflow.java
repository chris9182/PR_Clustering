package clusterproject.program;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import clusterproject.data.ClusteringResult;
import clusterproject.data.NumberVectorClusteringResult;
import clusterproject.data.PointContainer;
import clusterproject.program.Clustering.DBScan;
import clusterproject.program.Clustering.DiSHClustering;
import clusterproject.program.Clustering.EMClustering;
import clusterproject.program.Clustering.IClusterer;
import clusterproject.program.Clustering.ICustomClusterer;
import clusterproject.program.Clustering.IELKIClustering;
import clusterproject.program.Clustering.LloydKMeadians;
import clusterproject.program.Clustering.LloydKMeans;
import clusterproject.program.Clustering.MacQueenKMeans;
import clusterproject.program.Clustering.SNN;
import clusterproject.program.Clustering.SpectralClustering;
import clusterproject.program.Clustering.Parameters.Parameter;
import clusterproject.program.ClusteringResultsViewer.MetaViewer;
import clusterproject.program.MetaClustering.ClusteringError;
import clusterproject.program.MetaClustering.IMetaDistanceMeasure;
import clusterproject.program.MetaClustering.VariationOfInformation;
import clusterproject.program.MetaClustering.VariationOfInformationBootstrapped;
import clusterproject.util.FileUtils;
import clusterproject.util.HackedObjectInputStream;
import clusterproject.util.Util;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;

public class ClusterWorkflow extends JFrame {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final int OUTER_SPACE = 20;
	private static final int OPTIONS_WIDTH = 200;
	private static final boolean DEFAULT_ADD_GROUND_TRUTH = true;
	private static final boolean DEFAULT_KEEP_TRIVIAL_SOLUTIONS = false;
	private static final boolean DEFAULT_ADD_TRIVIAL_SOLUTIONS = true;

	private final static List<IClusterer> clusterersELKI = new ArrayList<IClusterer>();
	private final static List<IClusterer> clusterersOther = new ArrayList<IClusterer>();
	private final static List<IMetaDistanceMeasure> distances = new ArrayList<IMetaDistanceMeasure>();

	static {
		initClusterers();
		initDistances();
	}

	private final SpringLayout layout;

	private final PointContainer pointContainer;

	private IClusterer selectedClusterer;

	private final JComboBox<String> clustererSelector;
	private final List<IClusterer> workflow;

	private final JComboBox<String> distanceSelector;
	private final JFormattedTextField minPTSField;
	private final JFormattedTextField epsField;
	private final JFormattedTextField seedField;
	private final JCheckBox addGroundTruthBox;
	private final JCheckBox keepTrivialSolutionsBox;
	private final JCheckBox addTrivialSolutionsBox;

	private final JLabel wfLabel;

	private final JButton confirmClustererButton;
	private final JButton executeClusterersButton;
	private final JLayeredPane mainPanel;
	private JScrollPane wfScrollPane;
	private final JButton saveButton;
	private JButton loadClusterButton;
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final Random seededRandom = new Random();
	private Thread worker;

	public ClusterWorkflow(PointContainer container) {
		setTitle("Workflow-View");
		final NumberFormat integerFieldFormatter = NumberFormat.getIntegerInstance();
		integerFieldFormatter.setGroupingUsed(false);
		addGroundTruthBox = new JCheckBox("Add ground truth", DEFAULT_ADD_GROUND_TRUTH);
		keepTrivialSolutionsBox = new JCheckBox("Keep trivial solutions", DEFAULT_KEEP_TRIVIAL_SOLUTIONS);
		addTrivialSolutionsBox = new JCheckBox("Add trivial solutions", DEFAULT_ADD_TRIVIAL_SOLUTIONS);
		seedField = new JFormattedTextField(integerFieldFormatter);
		seedField.setValue(-1);
		seedField.setColumns(10);
		seedField.setHorizontalAlignment(SwingConstants.RIGHT);
		minPTSField = new JFormattedTextField(integerFieldFormatter);
		minPTSField.setValue(2);
		minPTSField.setColumns(5);
		minPTSField.setHorizontalAlignment(SwingConstants.RIGHT);
		final NumberFormat doubleFieldFormatter = NumberFormat.getNumberInstance();
		epsField = new JFormattedTextField(doubleFieldFormatter);
		epsField.setValue(new Double(-1.0));
		epsField.setColumns(5);
		epsField.setHorizontalAlignment(SwingConstants.RIGHT);
		progressBar.addChangeListener(e -> progressBar.repaint());
		progressBar.setStringPainted(true);
		progressBar.setString("Waiting");

		pointContainer = container;
		mainPanel = new JLayeredPane();

		add(mainPanel);

		getContentPane().setBackground(DataView.BACKGROUND_COLOR);

		workflow = new ArrayList<IClusterer>();
		layout = new SpringLayout();
		mainPanel.setLayout(layout);
		final JLabel addLabel = new JLabel("Add");
		mainPanel.add(addLabel, new Integer(1));
		layout.putConstraint(SpringLayout.NORTH, addLabel, OUTER_SPACE, SpringLayout.NORTH, mainPanel);
		layout.putConstraint(SpringLayout.WEST, addLabel, OUTER_SPACE, SpringLayout.WEST, mainPanel);

		final String[] names = new String[clusterersELKI.size() + clusterersOther.size()];
		for (int i = 0; i < names.length; ++i)
			if (i < clusterersELKI.size())
				names[i] = clusterersELKI.get(i).getName();
			else
				names[i] = clusterersOther.get(i - clusterersELKI.size()).getName();
		clustererSelector = new JComboBox<>(names);
		mainPanel.add(clustererSelector, new Integer(1));
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, clustererSelector, 0, SpringLayout.VERTICAL_CENTER,
				addLabel);
		layout.putConstraint(SpringLayout.WEST, clustererSelector, 2 * DataView.INNER_SPACE, SpringLayout.EAST,
				addLabel);

		clustererSelector.addActionListener(e -> openClustererSettings((String) clustererSelector.getSelectedItem()));

		final String[] names2 = new String[distances.size()];
		for (int i = 0; i < names2.length; ++i)
			names2[i] = distances.get(i).getName();
		distanceSelector = new JComboBox<>(names2);

		wfLabel = new JLabel("Workflow:");

		layout.putConstraint(SpringLayout.NORTH, wfLabel, OUTER_SPACE, SpringLayout.SOUTH, clustererSelector);
		layout.putConstraint(SpringLayout.WEST, wfLabel, OUTER_SPACE, SpringLayout.WEST, mainPanel);
		wfLabel.setVisible(false);
		mainPanel.add(wfLabel, new Integer(1));

		loadClusterButton = new JButton("Load Result");
		loadClusterButton.addActionListener(e -> {

			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.addChoosableFileFilter(FileUtils.crffilter);
			fileChooser.setFileFilter(FileUtils.crffilter);
			final JFrame chooserFrame = new JFrame();
			chooserFrame.setTitle("Load");
			chooserFrame.add(fileChooser);
			chooserFrame.setSize(new Dimension(400, 400));
			chooserFrame.setLocationRelativeTo(null);
			chooserFrame.setResizable(false);
			chooserFrame.setVisible(true);

			fileChooser.addActionListener(ev -> {
				if (ev.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
					chooserFrame.setVisible(false);
					chooserFrame.dispose();
					return;
				}
				final File selectedFile = fileChooser.getSelectedFile();
				if (selectedFile == null)
					return;

				if (FileUtils.crffilter.accept(selectedFile)) {
					loadCRFFile(selectedFile);
					chooserFrame.setVisible(false);
					chooserFrame.dispose();
				}

			});
		});
		layout.putConstraint(SpringLayout.SOUTH, loadClusterButton, -OUTER_SPACE, SpringLayout.SOUTH, mainPanel);
		layout.putConstraint(SpringLayout.EAST, loadClusterButton, -OPTIONS_WIDTH - 3 * OUTER_SPACE, SpringLayout.EAST,
				mainPanel);
		mainPanel.add(loadClusterButton, new Integer(2));

		executeClusterersButton = new JButton("Execute Workflow");
		executeClusterersButton.addActionListener(e -> executeWorkflow());
		executeClusterersButton.setVisible(false);
		layout.putConstraint(SpringLayout.SOUTH, executeClusterersButton, -OUTER_SPACE, SpringLayout.SOUTH, mainPanel);
		layout.putConstraint(SpringLayout.EAST, executeClusterersButton, -OPTIONS_WIDTH - 3 * OUTER_SPACE,
				SpringLayout.EAST, mainPanel);
		mainPanel.add(executeClusterersButton, new Integer(1));

		layout.putConstraint(SpringLayout.VERTICAL_CENTER, progressBar, 0, SpringLayout.VERTICAL_CENTER,
				executeClusterersButton);
		layout.putConstraint(SpringLayout.EAST, progressBar, -DataView.INNER_SPACE, SpringLayout.WEST,
				executeClusterersButton);
		mainPanel.add(progressBar, new Integer(1));
		progressBar.setVisible(false);

		layout.putConstraint(SpringLayout.SOUTH, distanceSelector, -DataView.INNER_SPACE, SpringLayout.NORTH,
				executeClusterersButton);
		layout.putConstraint(SpringLayout.EAST, distanceSelector, 0, SpringLayout.EAST, executeClusterersButton);
		mainPanel.add(distanceSelector, new Integer(1));

		final JLabel minPtsLabel = new JLabel("MinPts:");
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, minPtsLabel, 0, SpringLayout.VERTICAL_CENTER,
				distanceSelector);
		layout.putConstraint(SpringLayout.WEST, minPtsLabel, OUTER_SPACE, SpringLayout.WEST, mainPanel);
		mainPanel.add(minPtsLabel, new Integer(1));
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, minPTSField, 0, SpringLayout.VERTICAL_CENTER, minPtsLabel);
		layout.putConstraint(SpringLayout.WEST, minPTSField, DataView.INNER_SPACE, SpringLayout.EAST, minPtsLabel);
		mainPanel.add(minPTSField, new Integer(1));

		final JLabel seedLabel = new JLabel("Seed:");
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, seedLabel, 0, SpringLayout.VERTICAL_CENTER, minPTSField);
		layout.putConstraint(SpringLayout.WEST, seedLabel, OUTER_SPACE, SpringLayout.EAST, minPTSField);
		mainPanel.add(seedLabel, new Integer(1));
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, seedField, 0, SpringLayout.VERTICAL_CENTER, seedLabel);
		layout.putConstraint(SpringLayout.WEST, seedField, DataView.INNER_SPACE, SpringLayout.EAST, seedLabel);
		mainPanel.add(seedField, new Integer(1));
//		seedField

		layout.putConstraint(SpringLayout.VERTICAL_CENTER, addGroundTruthBox, 0, SpringLayout.VERTICAL_CENTER,
				minPTSField);
		layout.putConstraint(SpringLayout.WEST, addGroundTruthBox, OUTER_SPACE, SpringLayout.EAST, seedField);
		mainPanel.add(addGroundTruthBox, new Integer(1));

		final JLabel epsLabel = new JLabel("Eps:");
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, epsLabel, 0, SpringLayout.VERTICAL_CENTER,
				executeClusterersButton);
		layout.putConstraint(SpringLayout.WEST, epsLabel, OUTER_SPACE, SpringLayout.WEST, mainPanel);
		mainPanel.add(epsLabel, new Integer(1));
		layout.putConstraint(SpringLayout.VERTICAL_CENTER, epsField, 0, SpringLayout.VERTICAL_CENTER, epsLabel);
		layout.putConstraint(SpringLayout.WEST, epsField, 0, SpringLayout.WEST, minPTSField);
		mainPanel.add(epsField, new Integer(1));

		layout.putConstraint(SpringLayout.VERTICAL_CENTER, keepTrivialSolutionsBox, 0, SpringLayout.VERTICAL_CENTER,
				epsField);
		layout.putConstraint(SpringLayout.WEST, keepTrivialSolutionsBox, OUTER_SPACE, SpringLayout.EAST, epsField);
		mainPanel.add(keepTrivialSolutionsBox, new Integer(1));

		layout.putConstraint(SpringLayout.VERTICAL_CENTER, addTrivialSolutionsBox, 0, SpringLayout.VERTICAL_CENTER,
				keepTrivialSolutionsBox);
		layout.putConstraint(SpringLayout.WEST, addTrivialSolutionsBox, OUTER_SPACE, SpringLayout.EAST,
				keepTrivialSolutionsBox);
		mainPanel.add(addTrivialSolutionsBox, new Integer(1));

		saveButton = new JButton("Save Wf");
		saveButton.setEnabled(false);
		saveButton.addActionListener(e -> {
			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.addChoosableFileFilter(FileUtils.cwffilter);
			fileChooser.setApproveButtonText("Save");
			fileChooser.setFileFilter(FileUtils.cwffilter);
			final JFrame chooserFrame = new JFrame();
			chooserFrame.setTitle("Save");
			chooserFrame.add(fileChooser);
			chooserFrame.setSize(new Dimension(400, 400));
			chooserFrame.setLocationRelativeTo(null);
			chooserFrame.setResizable(false);
			chooserFrame.setVisible(true);

			fileChooser.addActionListener(ev -> {
				if (ev.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
					chooserFrame.setVisible(false);
					chooserFrame.dispose();
					return;
				}
				final File selectedFile = fileChooser.getSelectedFile();
				if (selectedFile == null)
					return;

				if (FileUtils.cwffilter.accept(selectedFile))
					saveCWFFile(selectedFile);
				else if (!selectedFile.getName().contains(".")) {
					saveCWFFile(new File(selectedFile.getPath() + "." + FileUtils.cwffilter.getExtensions()[0]));
				} else {
					return;
				}
				chooserFrame.setVisible(false);
				chooserFrame.dispose();
			});
		});
		layout.putConstraint(SpringLayout.SOUTH, saveButton, -OUTER_SPACE, SpringLayout.SOUTH, mainPanel);
		layout.putConstraint(SpringLayout.WEST, saveButton, (-OPTIONS_WIDTH + DataView.INNER_SPACE) / 2 - OUTER_SPACE,
				SpringLayout.EAST, mainPanel);
		layout.putConstraint(SpringLayout.EAST, saveButton, -OUTER_SPACE, SpringLayout.EAST, mainPanel);
		mainPanel.add(saveButton, new Integer(1));

		final JButton loadButton = new JButton("Load Wf");
		loadButton.addActionListener(e -> {
			if (worker != null && worker.isAlive())
				return;
			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.addChoosableFileFilter(FileUtils.cwffilter);
			fileChooser.setFileFilter(FileUtils.cwffilter);
			final JFrame chooserFrame = new JFrame();
			chooserFrame.add(fileChooser);
			chooserFrame.setSize(new Dimension(400, 400));
			chooserFrame.setLocationRelativeTo(null);
			chooserFrame.setResizable(false);
			chooserFrame.setVisible(true);

			fileChooser.addActionListener(ev -> {
				if (ev.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
					chooserFrame.setVisible(false);
					chooserFrame.dispose();
					return;
				}
				final File selectedFile = fileChooser.getSelectedFile();
				if (selectedFile == null)
					return;

				if (FileUtils.cwffilter.accept(selectedFile)) {
					loadCWFFile(selectedFile);
					chooserFrame.setVisible(false);
					chooserFrame.dispose();
				}
			});
		});
		layout.putConstraint(SpringLayout.SOUTH, loadButton, -OUTER_SPACE, SpringLayout.SOUTH, mainPanel);
		layout.putConstraint(SpringLayout.WEST, loadButton, (-OPTIONS_WIDTH - DataView.INNER_SPACE) / 2,
				SpringLayout.WEST, saveButton);
		layout.putConstraint(SpringLayout.EAST, loadButton, -DataView.INNER_SPACE, SpringLayout.WEST, saveButton);
		mainPanel.add(loadButton, new Integer(1));

		confirmClustererButton = new JButton("Confirm");
		confirmClustererButton.addActionListener(e -> addToWorkflow());
		layout.putConstraint(SpringLayout.SOUTH, confirmClustererButton, -DataView.INNER_SPACE, SpringLayout.NORTH,
				loadButton);
		layout.putConstraint(SpringLayout.WEST, confirmClustererButton, -OPTIONS_WIDTH - OUTER_SPACE, SpringLayout.EAST,
				mainPanel);
		layout.putConstraint(SpringLayout.EAST, confirmClustererButton, -OUTER_SPACE, SpringLayout.EAST, mainPanel);
		mainPanel.add(confirmClustererButton, new Integer(1));
		openClustererSettings(clusterersELKI.get(0).getName());

	}

	private void saveCWFFile(File selectedFile) {
		for (final IClusterer clusterer : workflow)
			clusterer.getSettingsString();
		try {
			final FileOutputStream fileOut = new FileOutputStream(selectedFile);
			final ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(workflow);
			out.close();
			fileOut.close();
		} catch (final IOException i) {
			i.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void loadCWFFile(File selectedFile) {
		if (worker != null && worker.isAlive())
			return;
		try {
			final FileInputStream fileIn = new FileInputStream(selectedFile);
			final ObjectInputStream in = new HackedObjectInputStream(fileIn);
			workflow.addAll((List<IClusterer>) in.readObject());
			in.close();
			fileIn.close();
		} catch (final IOException i) {
			i.printStackTrace();
			return;
		} catch (final ClassNotFoundException c) {
			c.printStackTrace();
			return;
		}
		showWorkflow();

	}

	@SuppressWarnings("unchecked")
	private void loadCRFFile(File selectedFile) {
		try {
			final FileInputStream fileIn = new FileInputStream(selectedFile);
			final ObjectInputStream in = new HackedObjectInputStream(fileIn);
			final List<ClusteringResult> sClusterings = (List<ClusteringResult>) in.readObject();
			boolean addGroundTruth = addGroundTruthBox.isSelected();
			openClusterViewer(sClusterings, addGroundTruth);
			in.close();
			fileIn.close();
		} catch (final IOException i) {
			i.printStackTrace();
			return;
		} catch (final ClassNotFoundException c) {
			c.printStackTrace();
			return;
		}

	}

	private void addToWorkflow() {
		if (worker != null && worker.isAlive())
			return;
		progressBar.setValue(0);
		progressBar.setString("Waiting");
		workflow.add(selectedClusterer);
		showWorkflow();
		openClustererSettings(selectedClusterer.getName());

	}

	private synchronized void executeWorkflow() {
		if (worker != null && worker.isAlive()) {
			final int option = JOptionPane.showConfirmDialog(null, "Do you want to cancel this operation?",
					"Cancel Clustering", JOptionPane.YES_NO_OPTION);
			if (option == JOptionPane.YES_OPTION)
				worker.interrupt();
			return;
		}
		if (pointContainer.getPoints().size() < 2) {
			JOptionPane.showMessageDialog(null, "Not enought data points!");
			return;
		}
		progressBar.setValue(0);
		progressBar.setMaximum(1);
		final List<NumberVectorClusteringResult> clusterings = new ArrayList<NumberVectorClusteringResult>();

		final double[][] data = pointContainer.getPoints().toArray(new double[pointContainer.getPoints().size()][]);
		final DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(data);
		final Database db = new StaticArrayDatabase(dbc, null);
		db.initialize();
		int maximum = 0;
		for (final IClusterer clusterer : workflow) {
			clusterer.setJProgressBar(progressBar);
			clusterer.setRandom(seededRandom);
			maximum += clusterer.getCount();
		}
		progressBar.setMaximum(maximum);
		final long seed = seedField.getValue() instanceof Integer ? (Integer) seedField.getValue()
				: (Long) seedField.getValue();
		if (seed > -1)
			seededRandom.setSeed(seed);
		else
			seededRandom.setSeed(System.currentTimeMillis());
		final boolean addGroundTruth = addGroundTruthBox.isSelected();
		final boolean keepTrivialSolutions = keepTrivialSolutionsBox.isSelected();
		final boolean addTrivialSolutions = addTrivialSolutionsBox.isSelected();
		worker = new Thread(() -> {
			progressBar.setString("Calculating Clusterings");
			for (final IClusterer clusterer : workflow) {
				if (clusterer instanceof IELKIClustering) {
					final IELKIClustering elkiClusterer = (IELKIClustering) clusterer;
					List<NumberVectorClusteringResult> results = null;
					try {
						if (Thread.interrupted())
							throw new InterruptedException();
						results = elkiClusterer.cluster(db);
					} catch (final InterruptedException e) {
						progressBar.setValue(0);
						progressBar.setString("Canceled");
						return;
					} catch (final Exception e) {
						e.printStackTrace();
						continue;
					}
					clusterings.addAll(results);
				}
			}
			progressBar.setString("Converting Results");
			if (pointContainer.hasClusters()) {
				final Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);

				final List<List<NumberVector>> pointList = new ArrayList<List<NumberVector>>();
				final Set<Integer> clusterIDs = new HashSet<Integer>(
						pointContainer.getClusterInformation().getClusterIDs());
				final int minID = Collections.min(clusterIDs);
				final int size = Collections.max(clusterIDs) + 1 - minID;

				for (int j = 0; j < size; ++j) {
					pointList.add(new ArrayList<NumberVector>());
				}
				int i = 0;
				for (final DBIDIter it = rel.iterDBIDs(); it.valid(); it.advance()) {
					pointList.get(pointContainer.getClusterInformation().getClusterIDs().get(i) - minID)
							.add(rel.get(it));
					i++;
				}
				final List<List<NumberVector>> betterPointList = new ArrayList<List<NumberVector>>();
				for (final List<NumberVector> lNV1 : pointList) {
					if (lNV1.isEmpty())
						continue;
					betterPointList.add(lNV1);
				}
				final NumberVector[][] clustersArr = new NumberVector[betterPointList.size()][];
				i = 0;
				for (final List<NumberVector> lNV2 : betterPointList) {
					NumberVector[] clusterArr = new NumberVector[lNV2.size()];
					clusterArr = lNV2.toArray(clusterArr);
					clustersArr[i] = clusterArr;
					++i;
				}
				final Parameter param = new Parameter(Parameter.GROUND_TRUTH);
				clusterings.add(0, new NumberVectorClusteringResult(clustersArr, param));

			}

			final List<ClusteringResult> sClusterings = new ArrayList<ClusteringResult>();
			if (clusterings != null && clusterings.size() > 0)
				sClusterings.addAll(Util.convertClusterings(clusterings, pointContainer.getHeaders()));

			double[][] customData = data;
			List<String> headersList = pointContainer.getHeaders();
			if (sClusterings.size() > 0) {
				customData = new double[pointContainer.getPoints().size()][];
				// this is (currently) only safe with no bootstaping
				customData = sClusterings.get(0).toPointContainer().getPoints().toArray(customData);
				headersList = sClusterings.get(0).getHeaders();
			}
			// customData can now be used as a reference to the points for non elki
			// clustering algorithms

			progressBar.setString("Calculating Clusterings");
			for (final IClusterer clusterer : workflow) {
				if (clusterer instanceof ICustomClusterer)
					try {
						{
							final ICustomClusterer simpleClusterer = (ICustomClusterer) clusterer;
							sClusterings.addAll(simpleClusterer.cluster(customData, headersList));
						}
					} catch (final InterruptedException e) {
						progressBar.setValue(0);
						progressBar.setString("Canceled");
						return;
					}
			}

			if (!keepTrivialSolutions) {
				progressBar.setString("Removing Triv. Solutions");
				final List<ClusteringResult> remove = new ArrayList<ClusteringResult>();
				for (final ClusteringResult result : sClusterings) {
					if (result.getParameter().getName().equals(Parameter.GROUND_TRUTH))
						continue;
					int length = 0;
					for (final double[][] cluster : result.getData())
						if (cluster.length > 0)
							++length;
					if (length == 1 || length == result.getPointCount()) {
						remove.add(result);
					}
				}
				sClusterings.removeAll(remove);
			}

			if (addTrivialSolutions) {
				progressBar.setString("Adding Triv. Solutions");
				final Parameter param = new Parameter("Trivial Solution");
				final ClusteringResult trivialOne = new ClusteringResult(new double[][][] { customData }, param,
						headersList);
				final double[][][] trivialDataAll = new double[customData.length][][];
				for (int i = 0; i < customData.length; ++i)
					trivialDataAll[i] = new double[][] { customData[i] };

				final Parameter param2 = new Parameter("Trivial Solution");
				final ClusteringResult trivialAll = new ClusteringResult(trivialDataAll, param2, headersList);

				if (!pointContainer.hasClusters() || !addGroundTruth) {
					sClusterings.add(0, trivialAll);
					sClusterings.add(0, trivialOne);
				} else {
					sClusterings.add(trivialOne);
					sClusterings.add(trivialAll);
				}
			}
//			final List<ClusteringResult> dedup = Util.removeDuplicates(sClusterings);
			progressBar.setString("Calculating Meta");
//			openClusterViewer(dedup);
			openClusterViewer(sClusterings, addGroundTruth);
			progressBar.setString("Done");
		});
		worker.start();

	}

	private void openClusterViewer(List<ClusteringResult> clusterings, boolean addGroundTruth) {
		final NumberFormat format = NumberFormat.getInstance();
		Number number = null;
		try {
			number = format.parse(epsField.getText());
		} catch (final ParseException e1) {
			e1.printStackTrace();
		}
		double eps = Double.MAX_VALUE;
		int minPTS = Integer.parseInt(minPTSField.getText());
		if (minPTS < 2)
			minPTS = 2;
		if (number != null)
			eps = number.doubleValue() < 0 ? Double.MAX_VALUE : number.doubleValue();
		ClusteringResult gt = null;
		for (int i = 0; i < clusterings.size(); ++i)
			if (clusterings.get(i).getParameter().getName().equals(Parameter.GROUND_TRUTH)) {
				gt = clusterings.get(i);
				if (!addGroundTruth)
					clusterings.remove(i);
				break;
			}
		final MetaViewer cv = new MetaViewer(gt, clusterings, getDistanceMeasure(), minPTS, eps);
		cv.setMinimumSize(new Dimension(800, 600));
		cv.setExtendedState(Frame.MAXIMIZED_BOTH);
		cv.setLocationRelativeTo(null);
		cv.pack();
		cv.setVisible(true);
	}

	private IMetaDistanceMeasure getDistanceMeasure() {
		for (final IMetaDistanceMeasure dist : distances)
			if (dist.getName().equals(distanceSelector.getSelectedItem()))
				return dist;
		return null;
	}

	private void openClustererSettings(String name) {
		if (selectedClusterer != null) {
			mainPanel.remove(selectedClusterer.getOptionsPanel());
			selectedClusterer.getOptionsPanel().setVisible(false);
		}
		selectedClusterer = null;
		for (final IClusterer clusterer : clusterersELKI)
			if (clusterer.getName().equals(name))
				selectedClusterer = clusterer.duplicate();
		for (final IClusterer clusterer : clusterersOther)
			if (clusterer.getName().equals(name))
				selectedClusterer = clusterer.duplicate();
		if (selectedClusterer == null)
			return;
		confirmClustererButton.setVisible(true);
		final JPanel options = selectedClusterer.getOptionsPanel();

		layout.putConstraint(SpringLayout.NORTH, options, OUTER_SPACE, SpringLayout.NORTH, mainPanel);
		layout.putConstraint(SpringLayout.EAST, options, -OUTER_SPACE, SpringLayout.EAST, mainPanel);
		layout.putConstraint(SpringLayout.WEST, options, -OUTER_SPACE - OPTIONS_WIDTH, SpringLayout.EAST, mainPanel);
		layout.putConstraint(SpringLayout.SOUTH, options, -DataView.INNER_SPACE, SpringLayout.NORTH,
				confirmClustererButton);

		options.setVisible(true);

		mainPanel.add(options, new Integer(1));
		SwingUtilities.invokeLater(() -> {
			revalidate();
			repaint();
		});

	}

	private void showWorkflow() {
		saveButton.setEnabled(!workflow.isEmpty());
		if (workflow.isEmpty()) {
			mainPanel.remove(wfScrollPane);
			wfLabel.setVisible(false);
			loadClusterButton.setVisible(true);
			executeClusterersButton.setVisible(false);
			progressBar.setVisible(false);
			return;
		}
		wfLabel.setVisible(true);
		loadClusterButton.setVisible(false);
		executeClusterersButton.setVisible(true);
		progressBar.setVisible(true);
		if (wfScrollPane != null)
			mainPanel.remove(wfScrollPane);
		final SpringLayout wfLayout = new SpringLayout();
		final JPanel wfPanel = new JPanel(wfLayout);
		wfScrollPane = new JScrollPane(wfPanel);
		wfScrollPane.setBorder(null);
		wfScrollPane.setOpaque(false);
		wfScrollPane.getViewport().setOpaque(false);
		wfPanel.setOpaque(false);

		layout.putConstraint(SpringLayout.NORTH, wfScrollPane, DataView.INNER_SPACE, SpringLayout.SOUTH, wfLabel);
		layout.putConstraint(SpringLayout.SOUTH, wfScrollPane, -OUTER_SPACE, SpringLayout.NORTH, distanceSelector);
		layout.putConstraint(SpringLayout.WEST, wfScrollPane, OUTER_SPACE, SpringLayout.WEST, mainPanel);
		layout.putConstraint(SpringLayout.EAST, wfScrollPane, -2 * OUTER_SPACE - OPTIONS_WIDTH, SpringLayout.EAST,
				mainPanel);

		Component alignment = Box.createVerticalStrut(0);
		wfLayout.putConstraint(SpringLayout.NORTH, alignment, 0, SpringLayout.NORTH, wfPanel);
		wfPanel.add(alignment);
		for (final IClusterer clusterer : workflow) {
			final JButton remove = new JButton("X");
			remove.addActionListener(e -> removeFromWorkflow(clusterer));
			wfPanel.add(remove);
			wfLayout.putConstraint(SpringLayout.NORTH, remove, DataView.INNER_SPACE, SpringLayout.SOUTH, alignment);
			wfLayout.putConstraint(SpringLayout.WEST, remove, DataView.INNER_SPACE, SpringLayout.WEST, wfPanel);
			final JLabel label = new JLabel(clusterer.getName() + ": " + clusterer.getSettingsString());
			wfLayout.putConstraint(SpringLayout.VERTICAL_CENTER, label, 0, SpringLayout.VERTICAL_CENTER, remove);
			wfLayout.putConstraint(SpringLayout.WEST, label, DataView.INNER_SPACE, SpringLayout.EAST, remove);
			wfPanel.add(label);

			alignment = remove;
		}

		wfPanel.setPreferredSize(new Dimension(0,
				DataView.INNER_SPACE + workflow.size() * (DataView.INNER_SPACE + executeClusterersButton.getHeight())));
		mainPanel.add(wfScrollPane, new Integer(1));
		SwingUtilities.invokeLater(() -> {
			revalidate();
			repaint();

		});

	}

	private void removeFromWorkflow(IClusterer clusterer) {
		workflow.remove(clusterer);
		showWorkflow();
	}

	private static void initDistances() {
		// TODO make distances and similar into maps
//		final List<IDistanceMeasure> distancesList = new ArrayList<IDistanceMeasure>();
		distances.add(new VariationOfInformation());
		distances.add(new VariationOfInformationBootstrapped());
		distances.add(new ClusteringError());
	}

	private static void initClusterers() {
		clusterersELKI.add(new LloydKMeans());
		clusterersELKI.add(new MacQueenKMeans());
		clusterersELKI.add(new LloydKMeadians());
		clusterersELKI.add(new DBScan());
		clusterersELKI.add(new DiSHClustering());
		clusterersELKI.add(new SNN());
		clusterersELKI.add(new EMClustering());
		// clusterers.add(new CLIQUEClustering());//XXX this is bugged

		clusterersOther.add(new SpectralClustering());

	}

}
