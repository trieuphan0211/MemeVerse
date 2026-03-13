package vn.stephenphan.filestorageservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    private Map<String, String> buckets;
    private Map<String, List<String>> allowedTypes;
    private Map<String, Integer> maxSizes;
    private int presignedUrlExpiry = 15;
}
