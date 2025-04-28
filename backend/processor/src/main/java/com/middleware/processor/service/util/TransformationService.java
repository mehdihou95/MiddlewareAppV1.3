package com.middleware.processor.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Generic transformation service that handles chained transformations and type conversion.
 * 
 * Example Usage:
 * 1. Simple transformation:
 *    transformAndConvert("  HELLO  ", "trim|lowercase", String.class) -> "hello"
 * 
 * 2. Number processing:
 *    transformAndConvert("00012345", "remove_leading_zeros|integer", Integer.class) -> 12345
 * 
 * 3. Date processing:
 *    transformAndConvert(" 2024-03-20 ", "trim|date_format", Date.class) -> Date object
 * 
 * 4. Decimal processing:
 *    transformAndConvert("123.4567", "decimal_format|currency", BigDecimal.class) -> 123.46
 * 
 * ASN-specific examples (for reference):
 * - ASN Number: "remove_leading_zeros|integer" with Integer.class
 * - Receipt Date: "trim|date_format" with Date.class
 * - Quantity: "remove_leading_zeros|decimal_format" with BigDecimal.class
 */
@Service
public class TransformationService {
    private static final Logger logger = LoggerFactory.getLogger(TransformationService.class);
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    
    private final Map<String, Function<String, Object>> transformationCache;
    
    public TransformationService() {
        this.transformationCache = new ConcurrentHashMap<>();
    }

    /**
     * Main method to transform a value using a chain of transformations and convert to target type.
     * This is the primary public API for transforming values.
     * 
     * @param value The input value to transform
     * @param transformationChain The chain of transformations (pipe-separated), e.g. "trim|uppercase" or "remove_leading_zeros|integer_format"
     * @param targetType The desired output type
     * @return The transformed and converted value
     * @throws TransformationException if transformation or conversion fails
     */
    public Object transformAndConvert(String value, String transformationChain, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            // Split the transformation chain
            String[] transformations = transformationChain != null ? 
                transformationChain.toLowerCase().split("\\|") : 
                new String[0];

            // Apply each transformation in sequence
            String result = value;
            for (String transformation : transformations) {
                result = applyTransformation(result, transformation.trim());
            }

            // Convert to target type
            return convertToTargetType(result, targetType);
        } catch (Exception e) {
            logger.error("Error in transformation chain '{}' for value '{}' to type '{}': {}", 
                transformationChain, value, targetType.getName(), e.getMessage());
            throw new TransformationException("Transformation failed", e);
        }
    }

    /**
     * Apply a single transformation to a string value.
     * This method is primarily used for XML-to-XML transformations where type conversion is not needed.
     * For entity field mapping with type conversion, use transformAndConvert instead.
     * 
     * @param value The string value to transform
     * @param transformation The transformation to apply
     * @return The transformed string value
     * @throws TransformationException if transformation fails
     */
    public String applyTransformation(String value, String transformation) {
        if (value == null || value.trim().isEmpty() || transformation == null || transformation.trim().isEmpty()) {
            return value;
        }
        
        try {
            switch (transformation) {
                case "uppercase":
                    return value.toUpperCase();
                    
                case "lowercase":
                    return value.toLowerCase();
                    
                case "trim":
                    return value.trim();
                    
                case "date_format":
                    return DATE_FORMAT.format(DATE_FORMAT.parse(value));
                    
                case "time_format":
                    return TIME_FORMAT.format(TIME_FORMAT.parse(value));
                    
                case "datetime_format":
                    return DATETIME_FORMAT.format(DATETIME_FORMAT.parse(value));
                    
                case "remove_leading_zeros":
                    String trimmed = value.replaceFirst("^0+", "");
                    return trimmed.isEmpty() ? "0" : trimmed;
                    
                case "decimal_format":
                    return formatDecimalNumber(value, 3);
                    
                case "integer_format":
                    return formatDecimalNumber(value, 0);
                    
                case "currency_format":
                    return formatDecimalNumber(value, 2);
                    
                default:
                    logger.warn("Unknown transformation: {}", transformation);
                    return value;
            }
        } catch (Exception e) {
            logger.error("Error applying transformation '{}' to value '{}': {}", 
                transformation, value, e.getMessage());
            throw new TransformationException("Transformation '" + transformation + "' failed", e);
        }
    }

    /**
     * Convert a value to the target type.
     */
    private Object convertToTargetType(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            String cleanValue = value.trim();
            
            if (String.class.equals(targetType)) {
                return cleanValue;
            }
            
            // Handle numeric types
            if (isNumericType(targetType)) {
                cleanValue = cleanValue.replace(",", ".")
                                     .replaceAll("[^\\d.\\-]", "");
                
                if (cleanValue.isEmpty() || cleanValue.equals(".") || cleanValue.equals("-")) {
                    return null;
                }
                
                BigDecimal decimal = new BigDecimal(cleanValue);
                
                if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
                    return decimal.setScale(0, RoundingMode.HALF_UP).intValue();
                } else if (Long.class.equals(targetType) || long.class.equals(targetType)) {
                    return decimal.setScale(0, RoundingMode.HALF_UP).longValue();
                } else if (Double.class.equals(targetType) || double.class.equals(targetType)) {
                    return decimal.doubleValue();
                } else if (BigDecimal.class.equals(targetType)) {
                    return decimal;
                }
            }
            
            // Handle date types
            if (java.util.Date.class.equals(targetType)) {
                return DATE_FORMAT.parse(cleanValue);
            } else if (java.time.LocalDate.class.equals(targetType)) {
                return java.time.LocalDate.parse(cleanValue);
            } else if (java.time.LocalDateTime.class.equals(targetType)) {
                return java.time.LocalDateTime.parse(cleanValue);
            }
            
            // Handle boolean
            if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
                return Boolean.parseBoolean(cleanValue);
            }
            
            throw new TransformationException("Unsupported target type: " + targetType.getName());
            
        } catch (Exception e) {
            logger.error("Error converting value '{}' to type '{}': {}", 
                value, targetType.getName(), e.getMessage());
            throw new TransformationException("Type conversion failed", e);
        }
    }

    /**
     * Format a decimal number with specified decimal places.
     */
    private String formatDecimalNumber(String value, int decimalPlaces) {
        try {
            String cleanValue = value.trim()
                                   .replace(",", ".")
                                   .replaceAll("[^\\d.\\-]", "");
            
            BigDecimal number = new BigDecimal(cleanValue);
            
            DecimalFormat formatter = new DecimalFormat();
            formatter.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
            formatter.setMinimumFractionDigits(decimalPlaces);
            formatter.setMaximumFractionDigits(decimalPlaces);
            formatter.setGroupingUsed(false);
            formatter.setRoundingMode(RoundingMode.HALF_UP);

            return formatter.format(number);
        } catch (Exception e) {
            logger.error("Error formatting decimal number: {} with {} decimal places: {}", 
                value, decimalPlaces, e.getMessage());
            throw new TransformationException("Decimal formatting failed", e);
        }
    }

    private boolean isNumericType(Class<?> type) {
        return Number.class.isAssignableFrom(type) ||
               type == int.class ||
               type == long.class ||
               type == double.class ||
               type == float.class;
    }
}

class TransformationException extends RuntimeException {
    public TransformationException(String message) {
        super(message);
    }
    
    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
