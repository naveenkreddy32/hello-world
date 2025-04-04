import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public final class DynamicYamlLoader {

    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> fileCache = new ConcurrentHashMap<>();

    private DynamicYamlLoader() {}

    public static String getProperty(String filePath, String key) {
        return getProperties(filePath).get(key);
    }

    public static ConcurrentHashMap<String, String> getProperties(String filePath) {
        // If file was already loaded, return cached
        if (fileCache.containsKey(filePath)) {
            return fileCache.get(filePath);
        }

        ConcurrentHashMap<String, String> loadedProperties = new ConcurrentHashMap<>();

        try {
            Resource resource = resolveResource(filePath);

            YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
            yamlFactory.setResources(resource);

            Properties props = yamlFactory.getObject();

            if (props == null || props.isEmpty()) {
                throw new IllegalStateException("No properties found in YAML file: " + filePath);
            }

            props.forEach((k, v) -> loadedProperties.put(k.toString(), v.toString()));

            fileCache.put(filePath, loadedProperties); // Cache for reuse

        } catch (Exception e) {
            throw new RuntimeException("Error loading YAML file: " + filePath, e);
        }

        return loadedProperties;
    }

    private static Resource resolveResource(String filePath) {
        if (filePath.startsWith("classpath:")) {
            String path = filePath.substring("classpath:".length());
            return new ClassPathResource(path);
        } else {
            return new FileSystemResource(filePath);
        }
    }
}