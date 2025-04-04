import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public final class OptimizedYamlLoader {

    // Cache: filePath -> flat map of key=value
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> cache = new ConcurrentHashMap<>();

    private OptimizedYamlLoader() {
        // prevent instantiation
    }

    public static String getProperty(String filePath, String key) {
        ConcurrentHashMap<String, String> props = getProperties(filePath);
        return props.get(key);
    }

    public static ConcurrentHashMap<String, String> getProperties(String filePath) {
        if (cache.containsKey(filePath)) {
            return cache.get(filePath);
        }

        ConcurrentHashMap<String, String> flatMap = new ConcurrentHashMap<>();

        try (InputStream inputStream = resolveInputStream(filePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("YAML file not found: " + filePath);
            }

            Yaml yaml = new Yaml();
            Object data = yaml.load(inputStream);

            if (data instanceof Map) {
                flattenMap("", (Map<String, Object>) data, flatMap);
            } else {
                throw new IllegalStateException("Invalid or empty YAML structure: " + filePath);
            }

            cache.put(filePath, flatMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML file: " + filePath, e);
        }

        return flatMap;
    }

    private static InputStream resolveInputStream(String filePath) throws FileNotFoundException {
        if (filePath.startsWith("classpath:")) {
            String resourcePath = filePath.substring("classpath:".length());
            return OptimizedYamlLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        } else {
            return new FileInputStream(filePath); // External file
        }
    }

    private static void flattenMap(String parentKey, Map<String, Object> source, Map<String, String> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = parentKey.isEmpty() ? entry.getKey() : parentKey + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenMap(key, (Map<String, Object>) value, target);
            } else {
                target.put(key, String.valueOf(value));
            }
        }
    }
}