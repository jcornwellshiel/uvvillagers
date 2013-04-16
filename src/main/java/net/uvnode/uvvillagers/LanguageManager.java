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
    
    public LanguageManager(Map<String,Object> strings) {
        _strings = new HashMap<String, String>();
        for(Map.Entry<String, Object> string : strings.entrySet()) {
            _strings.put(string.getKey(), ((String) string.getValue()).replace("#", "%"));
        }
    }

    public void setStrings() {
        _strings.clear();
    }
    
    public String getString(String name) {
        return _strings.get(name);
    }
    
    public Map<String, String> getAllStrings() {
        return _strings;
    }
}
