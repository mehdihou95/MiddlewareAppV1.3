package com.middleware.shared.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedAttributeNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "order_headers")
@AttributeOverride(name = "id", column = @Column(name = "order_id"))
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "OrderHeader.withClient",
        attributeNodes = {
            @NamedAttributeNode("client")
        }
    ),
    @NamedEntityGraph(
        name = "OrderHeader.withClientAndOrderLines",
        attributeNodes = {
            @NamedAttributeNode("client"),
            @NamedAttributeNode("orderLines")
        }
    )
})
@Getter
@Setter
public class OrderHeader extends BaseEntity {

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "creation_type", length = 20)
    private String creationType;

    @Column(name = "business_partner_id")
    private Long businessPartnerId;

    @Column(name = "business_partner_name", length = 100)
    private String businessPartnerName;

    @Column(name = "order_date_dttm")
    private LocalDateTime orderDateDttm;

    @Column(name = "order_recon_dttm")
    private LocalDateTime orderReconDttm;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "ext_purchase_order", length = 50)
    private String extPurchaseOrder;

    @Column(name = "cons_run_id")
    private Long consRunId;

    @Column(name = "o_facility_alias_id", length = 16)
    private String oFacilityAliasId;

    @Column(name = "o_facility_id")
    private Long oFacilityId;

    @Column(name = "o_dock_id", length = 8)
    private String oDockId;

    @Column(name = "o_address_1", length = 75)
    private String oAddress1;

    @Column(name = "o_address_2", length = 75)
    private String oAddress2;

    @Column(name = "o_address_3", length = 75)
    private String oAddress3;

    @Column(name = "o_city", length = 40)
    private String oCity;

    @Column(name = "o_state_prov", length = 3)
    private String oStateProv;

    @Column(name = "o_postal_code", length = 10)
    private String oPostalCode;

    @Column(name = "o_county", length = 40)
    private String oCounty;

    @Column(name = "o_country_code", length = 2)
    private String oCountryCode;

    @Column(name = "d_facility_alias_id", length = 16)
    private String dFacilityAliasId;

    @Column(name = "d_facility_id")
    private Long dFacilityId;

    @Column(name = "d_dock_id", length = 8)
    private String dDockId;

    @Column(name = "d_address_1", length = 75)
    private String dAddress1;

    @Column(name = "d_address_2", length = 75)
    private String dAddress2;

    @Column(name = "d_address_3", length = 75)
    private String dAddress3;

    @Column(name = "d_city", length = 40)
    private String dCity;

    @Column(name = "d_state_prov", length = 3)
    private String dStateProv;

    @Column(name = "d_postal_code", length = 10)
    private String dPostalCode;

    @Column(name = "d_county", length = 40)
    private String dCounty;

    @Column(name = "d_country_code", length = 2)
    private String dCountryCode;

    @Column(name = "bill_to_name", length = 91)
    private String billToName;

    @Column(name = "bill_facility_alias_id", length = 16)
    private String billFacilityAliasId;

    @Column(name = "bill_facility_id")
    private Long billFacilityId;

    @Column(name = "bill_to_address_1", length = 75)
    private String billToAddress1;

    @Column(name = "bill_to_address_2", length = 75)
    private String billToAddress2;

    @Column(name = "bill_to_address_3", length = 75)
    private String billToAddress3;

    @Column(name = "bill_to_city", length = 50)
    private String billToCity;

    @Column(name = "bill_to_state_prov", length = 3)
    private String billToStateProv;

    @Column(name = "bill_to_county", length = 40)
    private String billToCounty;

    @Column(name = "bill_to_postal_code", length = 10)
    private String billToPostalCode;

    @Column(name = "bill_to_country_code", length = 2)
    private String billToCountryCode;

    @Column(name = "bill_to_phone_number", length = 32)
    private String billToPhoneNumber;

    @Column(name = "bill_to_fax_number", length = 30)
    private String billToFaxNumber;

    @Column(name = "bill_to_email", length = 256)
    private String billToEmail;

    @Column(name = "incoterm_facility_id")
    private Long incotermFacilityId;

    @Column(name = "incoterm_facility_alias_id", length = 16)
    private String incotermFacilityAliasId;

    @Column(name = "incoterm_loc_ava_dttm")
    private LocalDateTime incotermLocAvaDttm;

    @Column(name = "incoterm_loc_ava_time_zone_id")
    private Long incotermLocAvaTimeZoneId;

    @Column(name = "pickup_tz", nullable = true)
    private Integer pickupTz;

    @Column(name = "delivery_tz", nullable = true)
    private Integer deliveryTz;

    @Column(name = "pickup_start_dttm")
    private LocalDateTime pickupStartDttm;

    @Column(name = "pickup_end_dttm")
    private LocalDateTime pickupEndDttm;

    @Column(name = "delivery_start_dttm")
    private LocalDateTime deliveryStartDttm;

    @Column(name = "delivery_end_dttm")
    private LocalDateTime deliveryEndDttm;

    @Column(name = "dsg_service_level_id")
    private Long dsgServiceLevelId;

    @Column(name = "dsg_carrier_id")
    private Long dsgCarrierId;

    @Column(name = "dsg_equipment_id")
    private Long dsgEquipmentId;

    @Column(name = "dsg_tractor_equipment_id")
    private Long dsgTractorEquipmentId;

    @Column(name = "dsg_mot_id")
    private Long dsgMotId;

    @Column(name = "baseline_mot_id")
    private Long baselineMotId;

    @Column(name = "baseline_service_level_id")
    private Long baselineServiceLevelId;

    @Column(name = "product_class_id")
    private Long productClassId;

    @Column(name = "protection_level_id")
    private Long protectionLevelId;

    @Column(name = "path_id")
    private Long pathId;

    @Column(name = "path_set_id")
    private Long pathSetId;

    @Column(name = "driver_type_id")
    private Long driverTypeId;

    @Column(name = "un_number_id")
    private Long unNumberId;

    @Column(name = "block_auto_create", nullable = false)
    private Integer blockAutoCreate = 0;

    @Column(name = "block_auto_consolidate", nullable = false)
    private Integer blockAutoConsolidate = 0;

    @Column(name = "has_split", nullable = false)
    private Integer hasSplit = 0;

    @Column(name = "is_booking_required", nullable = false)
    private Integer isBookingRequired = 0;

    @Column(name = "is_cancelled", nullable = false)
    private Integer isCancelled = 0;

    @Column(name = "is_hazmat", nullable = false)
    private Integer isHazmat = 0;

    @Column(name = "is_imported", nullable = false)
    private Integer isImported = 0;

    @Column(name = "is_partially_planned", nullable = false)
    private Integer isPartiallyPlanned = 0;

    @Column(name = "is_perishable", nullable = false)
    private Integer isPerishable = 0;

    @Column(name = "is_suspended", nullable = false)
    private Integer isSuspended = 0;

    @Column(name = "normalized_baseline_cost", precision = 13, scale = 4)
    private BigDecimal normalizedBaselineCost;

    @Column(name = "baseline_cost_currency_code", length = 3)
    private String baselineCostCurrencyCode;

    @Column(name = "orig_budg_cost", precision = 13, scale = 4)
    private BigDecimal origBudgCost;

    @Column(name = "budg_cost", precision = 13, scale = 4)
    private BigDecimal budgCost;

    @Column(name = "actual_cost", precision = 13, scale = 4)
    private BigDecimal actualCost;

    @Column(name = "baseline_cost", precision = 13, scale = 4)
    private BigDecimal baselineCost;

    @Column(name = "trans_resp_code", length = 3)
    private String transRespCode;

    @Column(name = "billing_method")
    private Integer billingMethod;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "equipment_type")
    private Integer equipmentType;

    @Column(name = "mv_currency_code", length = 3)
    private String mvCurrencyCode;

    @Column(name = "compartment_no")
    private Integer compartmentNo;

    @Column(name = "packaging", length = 15)
    private String packaging;

    @Column(name = "order_loading_seq")
    private Long orderLoadingSeq;

    @Column(name = "ref_field_1", length = 25)
    private String refField1;

    @Column(name = "ref_field_2", length = 25)
    private String refField2;

    @Column(name = "ref_field_3", length = 25)
    private String refField3;

    @Column(name = "created_source_type", nullable = false)
    private Integer createdSourceType = 0;

    @Column(name = "created_source", length = 50)
    private String createdSource;

    @Column(name = "created_dttm")
    private LocalDateTime createdDttm;

    @Column(name = "last_updated_source_type", nullable = false)
    private Integer lastUpdatedSourceType = 0;

    @Column(name = "last_updated_source", length = 50)
    private String lastUpdatedSource;

    @Column(name = "last_updated_dttm")
    private LocalDateTime lastUpdatedDttm;

    @Version
    @Column(name = "hibernate_version")
    private Long hibernateVersion;

    @Column(name = "actual_cost_currency_code", length = 3)
    private String actualCostCurrencyCode;

    @Column(name = "budg_cost_currency_code", length = 3)
    private String budgCostCurrencyCode;

    @OneToMany(mappedBy = "orderHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<OrderLine> orderLines = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdDttm = LocalDateTime.now();
        lastUpdatedDttm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedDttm = LocalDateTime.now();
    }
} 
