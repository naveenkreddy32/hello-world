import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for loading YAML properties files.
 * Handles nested properties and provides a flattened view of the configuration.
 */
public class YamlPropertiesLoader {
    private static final Logger LOGGER = Logger.getLogger(YamlPropertiesLoader.class.getName());
    private static final Map<String, Map<String, String>> PROPERTIES_CACHE = new ConcurrentHashMap<>();
    
    // Private constructor to prevent instantiation
    private YamlPropertiesLoader() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }
    
    /**
     * Loads YAML properties from the specified file path.
     * The properties are cached to avoid repeated file reads.
     * 
     * @param filePath Path to the YAML file
     * @return Map containing flattened key-value pairs from the YAML file
     */
    public static Map<String, String> loadProperties(String filePath) {
        // Return cached properties if available
        if (PROPERTIES_CACHE.containsKey(filePath)) {
            return Collections.unmodifiableMap(PROPERTIES_CACHE.get(filePath));
        }
        
        Map<String, String> flattenedProperties = new HashMap<>();
        
        try (InputStream inputStream = new FileInputStream(filePath)) {
            Yaml yaml = new Yaml();
            Object yamlData = yaml.load(inputStream);
            
            if (yamlData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> yamlMap = (Map<String, Object>) yamlData;
                flattenProperties(yamlMap, "", flattenedProperties);
            } else {
                LOGGER.warning("Loaded YAML is not a map structure: " + filePath);
            }
            
            // Cache the properties
            PROPERTIES_CACHE.put(filePath, flattenedProperties);
            return Collections.unmodifiableMap(flattenedProperties);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading YAML file: " + filePath, e);
            return Collections.emptyMap();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing YAML content from: " + filePath, e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Recursively flattens nested YAML properties into a single-level map.
     * Nested keys are represented using dot notation (e.g., "parent.child.grandchild").
     * 
     * @param yamlMap The map containing possibly nested YAML properties
     * @param parentKey The parent key prefix for nested properties
     * @param flattenedMap The resulting flattened map
     */
    private static void flattenProperties(Map<String, Object> yamlMap, String parentKey, Map<String, String> flattenedMap) {
        for (Map.Entry<String, Object> entry : yamlMap.entrySet()) {
            String key = parentKey.isEmpty() ? entry.getKey() : parentKey + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                flattenProperties(nestedMap, key, flattenedMap);
            } else {
                flattenedMap.put(key, value != null ? value.toString() : "");
            }
        }
    }
    
    /**
     * Clears the properties cache.
     * This can be useful if files are expected to change during application runtime.
     */
    public static void clearCache() {
        PROPERTIES_CACHE.clear();
        LOGGER.info("YAML properties cache cleared");
    }
    
    /**
     * Reloads a specific YAML file, updating the cache.
     * 
     * @param filePath Path to the YAML file to reload
     * @return The reloaded properties
     */
    public static Map<String, String> reloadProperties(String filePath) {
        PROPERTIES_CACHE.remove(filePath);
        return loadProperties(filePath);
    }
}