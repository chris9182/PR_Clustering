package clusterproject.clustergenerator.program.Normalizers;

import java.awt.Dimension;

import javax.swing.JPanel;

import clusterproject.clustergenerator.data.PointContainer;
import clusterproject.clustergenerator.program.MainWindow;
import clusterproject.clustergenerator.program.Normalizers.Panel.StandardizeOptions;

public class Standardize implements INormalizer {

	StandardizeOptions options = new StandardizeOptions();

	@Override
	public JPanel getOptionsPanel() {
		return options;
	}

	@Override
	public String getName() {
		return "Standardize";
	}

	@Override
	public boolean normalize(PointContainer container) {
		double[][] data = new double[container.getPoints().size()][];
		data = container.getPoints().toArray(data);

		final double[] min = new double[data[0].length];
		final double[] dev = new double[data[0].length];
		final double[] mean = new double[data[0].length];

		for (int i = 0; i < data[0].length; ++i) {
			min[i] = Double.MAX_VALUE;
			dev[i] = 0;
			mean[i] = 0;
		}

		for (int i = 0; i < data.length; ++i) {
			for (int j = 0; j < data[i].length; ++j) {
				if (data[i][j] < min[j])
					min[j] = data[i][j];
				mean[j] += data[i][j];
			}
		}

		for (int i = 0; i < data[0].length; ++i) {
			mean[i] /= data.length;
		}
		for (int i = 0; i < data.length; ++i) {
			for (int j = 0; j < data[i].length; ++j) {
				dev[j] += Math.pow(data[i][j] - mean[j], 2);
			}
		}
		for (int i = 0; i < data[0].length; ++i) {
			dev[i] = Math.sqrt(dev[i] / (data.length - 1));
		}

		final double[][] newdata = new double[container.getPoints().size()][];

		for (int i = 0; i < data.length; i++) {
			newdata[i] = new double[data[i].length];
			for (int j = 0; j < data[i].length; j++) {
				newdata[i][j] = (data[i][j] - mean[j]) / dev[j];
			}
		}

		final PointContainer newContainer = new PointContainer(container.getDim());
		newContainer.addPoints(newdata);
		newContainer.copyClusterInfo(container);
		newContainer.setHeaders(container.getHeaders());

		final MainWindow newWindow = new MainWindow(newContainer);
		newWindow.setSize(new Dimension(1000, 800));
		newWindow.setLocationRelativeTo(null);
		newWindow.setVisible(true);
		newWindow.update();
		return false;
	}

}
