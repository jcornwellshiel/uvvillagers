package net.uvnode.uvvillagers;

/**
 *
 * @author James Cornwell-Shiel
 */
public class UVVillageRank implements Comparable<UVVillageRank> {

    private String _name;
    private int _threshold;
    private double _multiplier;

    /**
     *
     * @param name
     * @param threshold
     * @param multiplier
     */
    public UVVillageRank(String name, int threshold, double multiplier) {
        _name = name;
        _threshold = threshold;
        _multiplier = multiplier;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return _name;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        this._name = name;
    }

    /**
     *
     * @return
     */
    public int getThreshold() {
        return _threshold;
    }

    /**
     *
     * @param threshold
     */
    public void setThreshold(int threshold) {
        this._threshold = threshold;
    }

    /**
     *
     * @return
     */
    public double getMultiplier() {
        return _multiplier;
    }

    /**
     *
     * @param multiplier
     */
    public void setMultiplier(double multiplier) {
        this._multiplier = multiplier;
    }

    @Override
    public int compareTo(UVVillageRank other) {
        return _threshold - other.getThreshold();
    }
}
