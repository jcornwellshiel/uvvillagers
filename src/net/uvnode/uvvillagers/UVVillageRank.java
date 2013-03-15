package net.uvnode.uvvillagers;


public class UVVillageRank implements Comparable<UVVillageRank> {
	private String _name;
	private int _threshold;
	private double _multiplier;

	public UVVillageRank(String name, int threshold, double multiplier) {
		_name = name;
		_threshold = threshold;
		_multiplier = multiplier;
	}

	public String getName() {
		return _name;
	}

	public void setName(String name) {
		this._name = name;
	}

	public int getThreshold() {
		return _threshold;
	}

	public void setThreshold(int threshold) {
		this._threshold = threshold;
	}

	public double getMultiplier() {
		return _multiplier;
	}

	public void setMultiplier(double multiplier) {
		this._multiplier = multiplier;
	}

	@Override
	public int compareTo(UVVillageRank other) {
		return _threshold - other.getThreshold();
	}
}
