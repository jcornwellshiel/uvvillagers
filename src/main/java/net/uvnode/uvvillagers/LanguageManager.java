/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.uvnode.uvvillagers;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author James Cornwell-Shiel
 */
public class LanguageManager {
    private Map<String,String> _strings;
    
    /**
     *
     * @param strings
     */
    public LanguageManager(Map<String,Object> strings) {
        _strings = new HashMap<>();
        for(Map.Entry<String, Object> string : strings.entrySet()) {
            _strings.put(string.getKey(), ((String) string.getValue()).replace("#", "%").replace('&', '\u00A7').replace("\u00A7\u00A7", "&"));
        }
    }

    /**
     *
     */
    public void setStrings() {
        _strings.clear();
    }
    
    /**
     *
     * @param name
     * @return
     */
    public String getString(String name) {
        if (_strings.containsKey(name)) {
            return _strings.get(name);
        } else {
            return "";
        }
    }
    
    /**
     *
     * @return
     */
    public Map<String, String> getAllStrings() {
        return _strings;
    }
}
