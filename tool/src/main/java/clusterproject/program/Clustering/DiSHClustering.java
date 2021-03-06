package clusterproject.program.Clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;

import clusterproject.data.NumberVectorClusteringResult;
import clusterproject.program.Clustering.Panel.DiSHOptions;
import clusterproject.program.Clustering.Parameters.Parameter;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.DiSH;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
//import scala.collection.mutable.BitSet;

public class DiSHClustering extends AbstractClustering implements IELKIClustering {
	private static final long serialVersionUID = -7172931268458883217L;

	private transient DiSHOptions optionsPanel = new DiSHOptions();
	private double eps;
	private double epsBound;
	private int Mu;
	private int MuBound;
	private int samples;

	@Override
	public JPanel getOptionsPanel() {
		return optionsPanel;
	}

	@Override
	public String getName() {
		return "DiSH";
	}

	@Override
	public List<NumberVectorClusteringResult> cluster(Database db) throws InterruptedException {
		final List<NumberVectorClusteringResult> clusterings = new ArrayList<NumberVectorClusteringResult>(getCount());
		final Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
		prepareSettings();
		if (random == null)
			random = new Random();
		for (int i = 0; i < samples; ++i) {
			if (Thread.interrupted())
				throw new InterruptedException();
			final double calcEps = eps + (epsBound - eps) * random.nextDouble();
			final int calcMu = random.nextInt((MuBound - Mu) + 1) + Mu;
			final ListParameterization params = new ListParameterization();
			params.addParameter(DiSH.Parameterizer.EPSILON_ID, calcEps);
			params.addParameter(DiSH.Parameterizer.MU_ID, calcMu);
			final DiSH<DoubleVector> dbscan = ClassGenericsUtil.parameterizeOrAbort(DiSH.class, params);
			final Clustering<SubspaceModel> result = dbscan.run(db);
			final List<NumberVector[]> clusterList = new ArrayList<NumberVector[]>();

			// TODO: noise index?
			result.getAllClusters().forEach(cluster -> {// what about the hierarchy?
				// XXX debug
				// import scala.collection.mutable.BitSet;
				// final BitSet bits = new BitSet(cluster.getModel().getDimensions());
				// System.err.println(bits);
				// XXX debug end

				final List<NumberVector> pointList = new ArrayList<NumberVector>();

				for (final DBIDIter it = cluster.getIDs().iter(); it.valid(); it.advance()) {
					pointList.add(rel.get(it));
				}
				NumberVector[] clusterArr = new NumberVector[pointList.size()];
				clusterArr = pointList.toArray(clusterArr);
				clusterList.add(clusterArr);
			});
			NumberVector[][] clustersArr = new NumberVector[clusterList.size()][];
			clustersArr = clusterList.toArray(clustersArr);
			final Parameter param = new Parameter(getName());
			param.addParameter("Mu", calcMu);
			param.addParameter("Epsilon", calcEps);
			clusterings.add(new NumberVectorClusteringResult(clustersArr, param));

			addProgress(1);
		}
		return clusterings;
	}

	@Override
	public IClusterer duplicate() {
		return new DiSHClustering();
	}

	@Override
	public String getSettingsString() {
		prepareSettings();
		return "Mu{LB:" + Mu + " UB:" + MuBound + "} Epsilon{LB:" + eps + " UB:" + epsBound + " Samples{" + samples
				+ "}";
	}

	@Override
	public int getCount() {
		prepareSettings();
		return samples;
	}

	private void prepareSettings() {
		if (optionsPanel == null)
			return;

		eps = optionsPanel.getLBEps();
		epsBound = optionsPanel.getUBEps();
		Mu = optionsPanel.getLBMu();
		MuBound = optionsPanel.getUBMu();
		samples = optionsPanel.getNSamples();
	}
}
