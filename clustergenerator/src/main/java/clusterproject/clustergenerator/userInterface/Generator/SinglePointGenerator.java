package clusterproject.clustergenerator.userInterface.Generator;

import javax.swing.JPanel;

import clusterproject.clustergenerator.data.PointContainer;
import clusterproject.clustergenerator.userInterface.Generator.Panel.NoOptionsPanel;

public class SinglePointGenerator implements IGenerator{

	private final NoOptionsPanel optionsPanel=new NoOptionsPanel(this);

	public JPanel getPanel() {
		return optionsPanel;
	}

	public String getName() {
		return "SinglePoint";
	}

	public boolean canSimpleGenerate() {
		return false;
	}

	public boolean generate(PointContainer container) {
		return false;
	}




	public boolean canClickGenerate() {
		return true;
	}

	public boolean generate(double[] point,PointContainer container) {
		final int dim=container.getDim();
		int i;
		for(i=0;i<point.length&&i<dim;++i)
			if(point[i]==Double.NaN)point[i]=0;//TODO: handle NaN
		container.addPoint(point);
		return true;
	}

}
