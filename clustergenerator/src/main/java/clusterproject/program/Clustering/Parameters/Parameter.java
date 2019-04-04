package clusterproject.program.Clustering.Parameters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import clusterproject.util.Util;

public class Parameter implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -2644835073796978627L;

	private final String name;

	private final Map<String, Object> parameters = new HashMap<String, Object>();
	private final Map<String, Object> additionalParameters = new HashMap<String, Object>();
	private final Map<String, Object> allParameters = new HashMap<String, Object>();

	public Parameter(String name) {
		this.name = name;
	}

	public void addParameter(String name, Object parameter) {
		parameters.put(name, parameter);
		allParameters.put(name, parameter);
	}

	public void addAdditionalParameter(String name, Object parameter) {
		additionalParameters.put(name, parameter);
		allParameters.put(name, parameter);
	}

	public String getName() {
		return name;
	}

	public String getInfoString() {
		String returnval = getName() + ": ";
		for (final String param : parameters.keySet()) {
			if (Util.META_PARAMS.contains(param))
				continue;
			returnval += param + ": " + parameters.get(param) + " ";

		}
		return returnval.trim();
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public Map<String, Object> getAdditionalParameters() {
		return additionalParameters;
	}

	public Map<String, Object> getAllParameters() {
		return allParameters;
	}

}