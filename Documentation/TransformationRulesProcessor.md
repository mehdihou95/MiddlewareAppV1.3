# Transformation Rules Documentation

## Overview
The Transformation Service provides a centralized way to handle data transformations and type conversions in the middleware application. It supports both simple string transformations and complex chained transformations with type conversion.

## Usage Patterns

### 1. Simple String Transformations (XML Processing)
Used for XML-to-XML transformations where only string manipulation is needed:
```java
String result = transformationService.applyTransformation(value, "uppercase");
```

### 2. Chained Transformations with Type Conversion (Entity Mapping)
Used for mapping XML values to entity fields with both transformation and type conversion:
```java
Object result = transformationService.transformAndConvert(
    value,                          // Input value
    "remove_leading_zeros|integer", // Transformation chain
    Integer.class                   // Target type
);
```

## Available Transformations

### 1. String Transformations

| Rule | Description | Example |
|------|-------------|---------|
| `uppercase` | Converts text to uppercase | "hello" → "HELLO" |
| `lowercase` | Converts text to lowercase | "HELLO" → "hello" |
| `trim` | Removes leading and trailing whitespace | " hello " → "hello" |

### 2. Date/Time Transformations

| Rule | Description | Format | Example |
|------|-------------|--------|---------|
| `date_format` | Formats date | yyyy-MM-dd | "2024-04-24" |
| `time_format` | Formats time | HH:mm:ss | "16:28:07" |
| `datetime_format` | Formats date and time | yyyy-MM-dd'T'HH:mm:ss | "2024-04-24T16:28:07" |

### 3. Numeric Transformations

| Rule | Description | Decimal Places | Example |
|------|-------------|----------------|---------|
| `remove_leading_zeros` | Removes leading zeros | N/A | "00123" → "123" |
| `decimal_format` | Decimal number formatting | 3 | "123.4567" → "123.457" |
| `integer_format` | Whole number formatting | 0 | "123.456" → "123" |
| `currency_format` | Currency formatting | 2 | "123.456" → "123.46" |

## Valid Transformation Chains

### Common Valid Combinations

| Chain | Description | Example | Use Case |
|-------|-------------|---------|----------|
| `trim\|uppercase` | Trims and uppercases text | " hello " → "HELLO" | Text normalization |
| `remove_leading_zeros\|integer_format` | Removes zeros and formats as integer | "00123.45" → "123" | ID fields |
| `trim\|date_format` | Trims and formats date | " 2024-04-24 " → "2024-04-24" | Date fields |
| `remove_leading_zeros\|decimal_format` | Removes zeros and formats decimals | "00123.456" → "123.456" | Numeric fields |
| `trim\|currency_format` | Trims and formats currency | " 123.456 " → "123.46" | Money fields |

### Invalid Combinations (Avoid)

| Chain | Why Invalid |
|-------|-------------|
| `integer_format\|remove_leading_zeros` | Wrong order - format after removal |
| `uppercase\|integer_format` | Type mismatch |
| `date_format\|currency_format` | Incompatible transformations |
| `decimal_format\|date_format` | Incompatible transformations |

## ASN Processing Example

ASN documents require specific transformation patterns for different field types. Here are the common patterns:

### ASN Header Fields

```java
// ASN Number
transformAndConvert("0000012345", "remove_leading_zeros|integer_format", Integer.class)
// Result: 12345 (Integer)

// Receipt Date
transformAndConvert(" 2024-04-24 ", "trim|date_format", Date.class)
// Result: 2024-04-24 (Date)

// Business Partner ID
transformAndConvert("00000789", "remove_leading_zeros|integer_format", Integer.class)
// Result: 789 (Integer)
```

### ASN Line Fields

```java
// Quantity
transformAndConvert("00123.456", "remove_leading_zeros|decimal_format", BigDecimal.class)
// Result: 123.456 (BigDecimal)

// Unit Price
transformAndConvert(" 45.6789 ", "trim|currency_format", BigDecimal.class)
// Result: 45.68 (BigDecimal)

// Line Number
transformAndConvert("000045", "remove_leading_zeros|integer_format", Integer.class)
// Result: 45 (Integer)
```

## Type Conversion Support

The service automatically handles conversion to various Java types:

| Target Type | Description | Example |
|-------------|-------------|---------|
| String | Text values | "Hello" |
| Integer/int | Whole numbers | 123 |
| Long/long | Large whole numbers | 1234567890 |
| Double/double | Floating-point numbers | 123.456 |
| BigDecimal | Precise decimal numbers | 123.456 |
| Boolean/boolean | True/false values | true |
| Date | Date values | 2024-04-24 |
| LocalDate | Modern date values | 2024-04-24 |
| LocalDateTime | Date and time values | 2024-04-24T16:28:07 |

## Features

1. **Chained Transformations**
   - Multiple transformations in sequence
   - Order-sensitive processing
   - Type-aware conversions

2. **Automatic Type Conversion**
   - Handles conversion between different data types
   - Maintains data integrity during conversion
   - Supports complex type hierarchies

3. **Use Case Specific Processing**
   - XML-to-XML transformations (string only)
   - Entity field mapping (with type conversion)
   - ASN-specific transformations

4. **Error Handling**
   - Comprehensive error logging
   - Chain-specific error context
   - Detailed transformation tracking

## Best Practices

1. **Choose Appropriate Transformation Chain**
   - Consider the final target type
   - Order transformations logically
   - Use established patterns for common cases

2. **Chain Order Matters**
   - Clean data first (trim, remove zeros)
   - Format next (decimal, date)
   - Convert type last

3. **Type Safety**
   - Validate chain compatibility
   - Consider target type constraints
   - Test with edge cases

4. **Performance Optimization**
   - Cache common chains
   - Minimize chain length
   - Use appropriate target types

## Security Considerations

1. **Input Validation**
   - Validate all input data
   - Sanitize user inputs
   - Validate transformation chains

2. **Error Handling**
   - Don't expose sensitive information
   - Log transformation errors
   - Provide clean error messages

3. **Data Integrity**
   - Maintain precision in chains
   - Validate intermediate results
   - Ensure type safety