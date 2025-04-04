import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DynamicYamlLoader {

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> fileCache = new ConcurrentHashMap<>();

    private DynamicYamlLoader() {}

    public static String getProperty(String filePath, String key) {
        return getProperties(filePath).get(key);
    }

    public static ConcurrentHashMap<String, String> getProperties(String filePath) {
        if (fileCache.containsKey(filePath)) {
            return fileCache.get(filePath);
        }

        ConcurrentHashMap<String, String> flatMap = new ConcurrentHashMap<>();

        try (InputStream input = resolveResource(filePath).getInputStream()) {
            Yaml yaml = new Yaml();
            Object loadedYaml = yaml.load(input);

            if (loadedYaml instanceof Map) {
                flattenMap("", (Map<String, Object>) loadedYaml, flatMap);
            } else {
                throw new IllegalStateException("Unexpected YAML structure in file: " + filePath);
            }

            fileCache.put(filePath, flatMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML file: " + filePath, e);
        }

        return flatMap;
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

    private static Resource resolveResource(String filePath) {
        if (filePath.startsWith("classpath:")) {
            return new ClassPathResource(filePath.substring("classpath:".length()));
        } else {
            return new FileSystemResource(filePath);
        }
    }
}