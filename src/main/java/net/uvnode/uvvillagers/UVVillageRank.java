package net.uvnode.uvvillagers;

/**
 *
 * @author James Cornwell-Shiel
 */
public class UVVillageRank implements Comparable<UVVillageRank> {

    private String _name;
    private int _threshold;
    private double _multiplier;
    private boolean _isHostile, _canTrade;
    /**
     *
     * @param name
     * @param threshold
     * @param multiplier
     * @param isHostile 
     * @param canTrade  
     */
    public UVVillageRank(String name, int threshold, double multiplier, boolean isHostile, boolean canTrade) {
        _name = name;
        _threshold = threshold;
        _multiplier = multiplier;
        _isHostile = isHostile;
        _canTrade = canTrade;
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
    public boolean canTrade() {
        return _canTrade;
    }

    /**
     *
     * @return
     */
    public boolean isHostile() {
        return _isHostile;
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
