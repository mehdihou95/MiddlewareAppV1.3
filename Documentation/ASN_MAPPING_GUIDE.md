# ASN Mapping Rules Guide

## Overview
This guide outlines the requirements for creating mapping rules for ASN (Advanced Shipping Notice) headers and lines. Following these guidelines will ensure data integrity and prevent NULL constraint violations.

## Factory Default Values

The ASN factory provides automatic initialization of headers and lines with default values. These defaults are used when no mapping rules exist for specific fields.

### Header Factory Defaults
```java
// Header initialization in AsnFactory
header.setStatus("NEW");
header.setAsnNumber("DEFAULT");
header.setAsnType(1);
header.setAsnLevel(1);
header.setReceiptDttm(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

// Boolean flags
header.setHasImportError(false);
header.setHasSoftCheckError(false);
header.setHasAlerts(false);
header.setIsCogiGenerated(false);
header.setIsCancelled(false);
header.setIsClosed(false);
header.setIsGift(false);
header.setReceiptVariance(false);

// Other defaults
header.setIsWhseTransfer("0");
header.setQualityAuditPercent(BigDecimal.ZERO);
header.setAsnPriority(0);
header.setScheduleAppt(0);
header.setCreatedSourceType(0);
header.setLastUpdatedSourceType(0);
```

### Line Factory Defaults
```java
// Line initialization in AsnFactory
line.setStatus("NEW");
line.setAsnDetailStatus(4);
line.setIsCancelled(0);
line.setQtyConvFactor(BigDecimal.ONE);
line.setCreatedSourceType(1);
line.setLastUpdatedSourceType(1);
line.setQuantity(0);
line.setUnitOfMeasure("EA");

// Auto-incrementing line number if not mapped
line.setLineNumber(String.valueOf(defaultLineCounter.incrementAndGet()));
```

## ASN Headers

### Critical Fields
These fields **MUST** have mapping rules and cannot be null.

#### Core Fields
| Field Name | Type | Factory Default | Notes |
|------------|------|----------------|-------|
| `status` | String | "NEW" | **Handled automatically by factory** - Can be overridden by mapping |
| `asn_number` | String | "DEFAULT" | Should be overridden by mapping rules |
| `asn_type` | Integer | 1 | Type identifier for the ASN |
| `receipt_dttm` | String | Current Date | Format: YYYY-MM-DD |
| `asn_level` | Integer | 1 | Hierarchy level of the ASN |

#### Required Boolean Flags
All these fields are required and initialized by the factory:

| Field Name | Type | Factory Default | Notes |
|------------|------|----------------|-------|
| `has_import_error` | Boolean | false | Can be overridden by mapping |
| `has_soft_check_error` | Boolean | false | Can be overridden by mapping |
| `has_alerts` | Boolean | false | Can be overridden by mapping |
| `is_cogi_generated` | Boolean | false | Can be overridden by mapping |
| `is_cancelled` | Boolean | false | Can be overridden by mapping |
| `is_closed` | Boolean | false | Can be overridden by mapping |
| `is_gift` | Boolean | false | Can be overridden by mapping |
| `receipt_variance` | Boolean | false | Can be overridden by mapping |

#### Other Required Fields with Factory Defaults
| Field Name | Type | Factory Default | Notes |
|------------|------|----------------|-------|
| `is_whse_transfer` | String | "0" | Warehouse transfer indicator |
| `quality_audit_percent` | BigDecimal | 0 | Precision: 5,2 |
| `asn_priority` | Integer | 0 | Processing priority |
| `schedule_appt` | Integer | 0 | Appointment scheduling flag |
| `created_source_type` | Integer | 0 | Source type for creation |
| `last_updated_source_type` | Integer | 0 | Source type for last update |

### Optional Fields

#### Business Partner Information
| Field Name | Length | Type | Notes |
|------------|--------|------|-------|
| `business_partner_id` | - | String | Partner identifier |
| `business_partner_name` | - | String | Partner name |
| `business_partner_address_1` | 75 | String | Primary address |
| `business_partner_address_2` | 75 | String | Secondary address |
| `business_partner_address_3` | 75 | String | Additional address |
| `business_partner_city` | 40 | String | City |
| `business_partner_state_prov` | 3 | String | State/Province code |
| `business_partner_zip` | 10 | String | ZIP/Postal code |

#### Contact Information
| Field Name | Length | Type | Notes |
|------------|--------|------|-------|
| `contact_address_1` | 75 | String | Primary address |
| `contact_address_2` | 75 | String | Secondary address |
| `contact_address_3` | 75 | String | Additional address |
| `contact_city` | 40 | String | City |
| `contact_state_prov` | 3 | String | State/Province code |
| `contact_zip` | 10 | String | ZIP/Postal code |
| `contact_number` | 32 | String | Contact phone/reference |

#### Appointment Details
| Field Name | Type | Notes |
|------------|------|-------|
| `appointment_id` | String(50) | Appointment identifier |
| `appointment_dttm` | Date | Appointment date/time |
| `appointment_duration` | Long | Duration in minutes |

#### Delivery Information
| Field Name | Type | Notes |
|------------|------|-------|
| `driver_name` | String(50) | |
| `tractor_number` | String(50) | |
| `delivery_stop_seq` | Integer | Stop sequence number |
| `pickup_end_dttm` | Date | |
| `delivery_start_dttm` | Date | |
| `delivery_end_dttm` | Date | |
| `actual_departure_dttm` | Date | |
| `actual_arrival_dttm` | Date | |

#### Quantities and Measurements
| Field Name | Type | Precision | Notes |
|------------|------|-----------|-------|
| `total_weight` | BigDecimal | 13,4 | |
| `total_volume` | BigDecimal | 13,4 | |
| `volume_uom_id_base` | Long | - | Unit of measure ID |
| `total_shipped_qty` | BigDecimal | 16,4 | |
| `total_received_qty` | BigDecimal | 16,4 | |
| `shipped_lpn_count` | Long | - | |
| `received_lpn_count` | Long | - | |

#### Equipment Information
| Field Name | Type | Length | Notes |
|------------|------|--------|-------|
| `equipment_type` | String | 8 | |
| `equipment_code` | String | 20 | |
| `equipment_code_id` | Long | - | |

#### Reference Numbers
| Field Name | Type | Length | Notes |
|------------|------|--------|-------|
| `manif_nbr` | String | 20 | Manifest number |
| `manif_type` | String | 4 | Manifest type |
| `work_ord_nbr` | String | 12 | Work order number |
| `cut_nbr` | String | 12 | Cut number |
| `assigned_carrier_code` | String | 10 | |
| `bill_of_lading_number` | String | 30 | |
| `pro_number` | String | 20 | |
| `firm_appt_ind` | Integer | - | |
| `buyer_code` | String | 3 | |

#### Additional Fields
| Field Name | Type | Notes |
|------------|------|-------|
| `notes` | String | General notes |
| `region_id` | Long | Region identifier |

## ASN Lines

### Critical Fields
These fields **MUST** have mapping rules or will use factory defaults.

#### Core Fields
| Field Name | Type | Factory Default | Notes |
|------------|------|----------------|-------|
| `header_asn_id` | Long | - | **Handled automatically by backend** |
| `client_id` | Long | - | **Handled automatically by backend** |
| `status` | String | "NEW" | Line status |
| `asn_detail_status` | Integer | 4 | Detail status code |
| `is_cancelled` | Integer | 0 | Cancellation status |
| `qty_conv_factor` | BigDecimal | 1.0 | Quantity conversion factor |
| `created_source_type` | Integer | 1 | Source type for creation |
| `last_updated_source_type` | Integer | 1 | Source type for last update |
| `quantity` | Integer | 0 | Line quantity |
| `unit_of_measure` | String | "EA" | Unit of measure |
| `line_number` | String | Auto-incrementing | Generated if not mapped |

### Optional Fields

#### Item Information
| Field Name | Type | Notes |
|------------|------|-------|
| `item_id` | Long | Item identifier |
| `item_name` | String | Item name |
| `item_attr_1` | String | Item attribute 1 |
| `item_attr_2` | String | Item attribute 2 |
| `item_attr_3` | String | Item attribute 3 |
| `item_attr_4` | String | Item attribute 4 |
| `item_attr_5` | String | Item attribute 5 |
| `item_number` | String | Item number |
| `item_description` | String | Item description |

#### Package Information
| Field Name | Type | Notes |
|------------|------|-------|
| `package_type_id` | Long | Package type identifier |
| `package_type_desc` | String | Package type description |
| `package_type_instance` | String | Package type instance |
| `epc_tracking_rfid_value` | String | RFID tracking value |
| `gtin` | String | Global Trade Item Number |
| `std_pack_qty` | BigDecimal | Standard pack quantity |
| `std_case_qty` | BigDecimal | Standard case quantity |
| `std_sub_pack_qty` | BigDecimal | Standard sub-pack quantity |
| `lpn_per_tier` | Integer | LPNs per tier |
| `tier_per_pallet` | Integer | Tiers per pallet |

#### Quantity Information
| Field Name | Type | Notes |
|------------|------|-------|
| `shipped_qty` | BigDecimal | Shipped quantity |
| `shipped_lpn_count` | Integer | Shipped LPN count |
| `units_assigned_to_lpn` | BigDecimal | Units assigned to LPN |
| `qty_uom_id` | Long | Quantity unit of measure ID |
| `qty_uom_id_base` | Long | Base quantity unit of measure ID |

#### Weight Information
| Field Name | Type | Notes |
|------------|------|-------|
| `weight_uom_id` | Long | Weight unit of measure ID |
| `weight_uom_id_base` | Long | Base weight unit of measure ID |
| `actual_weight` | BigDecimal | Actual weight |
| `actual_weight_pack_count` | BigDecimal | Actual weight pack count |
| `nbr_of_pack_for_catch_wt` | BigDecimal | Number of packs for catch weight |

#### Date Information
| Field Name | Type | Notes |
|------------|------|-------|
| `mfg_date` | Date | Manufacturing date |
| `ship_by_date` | Date | Ship by date |
| `expire_date` | Date | Expiration date |

#### Manufacturing Information
| Field Name | Type | Notes |
|------------|------|-------|
| `mfg_plnt` | String | Manufacturing plant |
| `invn_type` | String | Inventory type |
| `prod_stat` | String | Product status |
| `cntry_of_orgn` | String | Country of origin |

#### Processing Information
| Field Name | Type | Notes |
|------------|------|-------|
| `proc_immd_needs` | String | Processing immediate needs |
| `quality_check_hold_upon_rcpt` | String | Quality check hold upon receipt |
| `reference_order_nbr` | String | Reference order number |
| `retail_price` | BigDecimal | Retail price |
| `exp_receive_condition_code` | String | Expected receive condition code |
| `asn_recv_rules` | String | ASN receive rules |
| `disposition_type` | String | Disposition type |
| `inv_disposition` | String | Inventory disposition |

#### Purchase Order Information
| Field Name | Type | Notes |
|------------|------|-------|
| `purchase_orders_line_item_id` | Long | Purchase order line item ID |
| `lot_number` | String | Lot number |
| `serial_number` | String | Serial number |

#### Reference Fields
| Field Name | Type | Notes |
|------------|------|-------|
| `ref_field_1` | String | Reference field 1 |
| `ref_field_2` | String | Reference field 2 |
| `ref_field_3` | String | Reference field 3 |
| `ref_field_4` | String | Reference field 4 |
| `ref_field_5` | String | Reference field 5 |
| `ref_field_6` | String | Reference field 6 |
| `ref_field_7` | String | Reference field 7 |
| `ref_field_8` | String | Reference field 8 |
| `ref_field_9` | String | Reference field 9 |
| `ref_field_10` | String | Reference field 10 |
| `ref_num1` | BigDecimal | Reference number 1 |
| `ref_num2` | BigDecimal | Reference number 2 |
| `ref_num3` | BigDecimal | Reference number 3 |
| `ref_num4` | BigDecimal | Reference number 4 |
| `ref_num5` | BigDecimal | Reference number 5 |

## Best Practices

### 1. Status Management
- Always set an initial status value
- Use consistent status values across the application
- Consider status transitions in your mapping rules

### 2. Date Handling
- Use proper date format transformations
- Consider timezone implications
- Validate date formats in mapping rules

### 3. Numeric Fields
- Use appropriate precision for decimal values
- Consider rounding rules where applicable
- Handle null numeric values appropriately

### 4. String Fields
- Respect maximum length constraints
- Consider string trimming rules
- Handle special characters appropriately

### 5. Default Values
- Set appropriate defaults for required fields
- Document default value logic
- Consider business rules when setting defaults

### 6. Factory Pattern Usage
- Factory provides consistent default values
- Auto-incrementing line numbers when not mapped
- Centralized initialization logic
- Thread-safe line number generation

### 7. Line Number Handling
- Let factory auto-generate if no mapping exists
- Override with mapped values when available
- Ensure uniqueness within each ASN
- Consider sequence requirements

## Common Mapping Scenarios

### Example 1: Basic Required Fields for ASN Header
```xml
<mapping>
    <field target="status" source="..." default="NEW"/>
    <field target="asn_number" source="..." required="true"/>
    <field target="asn_type" source="..." required="true"/>
    <field target="receipt_dttm" source="..." transform="date_format"/>
</mapping>
```

### Example 2: Boolean Flags for ASN Header
```xml
<mapping>
    <field target="has_import_error" default="false"/>
    <field target="has_soft_check_error" default="false"/>
    <field target="is_closed" default="false"/>
</mapping>
```

### Example 3: Basic Required Fields for ASN Line
```xml
<mapping>
    <field target="status" source="..." default="NEW"/>
    <field target="asn_detail_status" source="..." default="4"/>
    <field target="is_cancelled" source="..." default="0"/>
    <field target="qty_conv_factor" source="..." default="1.0"/>
</mapping>
```

## Troubleshooting

### Common Issues
1. NULL constraint violations
   - Check required field mappings
   - Verify default values
   - Validate transformation rules

2. Data type mismatches
   - Verify field types match entity
   - Check transformation rules
   - Validate numeric precision

3. String truncation
   - Check field length constraints
   - Implement trimming rules
   - Validate input data 