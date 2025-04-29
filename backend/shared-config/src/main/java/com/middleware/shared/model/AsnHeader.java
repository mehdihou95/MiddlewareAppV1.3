package com.middleware.shared.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedAttributeNode;

@Entity
@Table(name = "asn_headers")
@AttributeOverride(name = "id", column = @Column(name = "asn_id"))
@NamedEntityGraphs({
    @NamedEntityGraph(
        name = "AsnHeader.withClient",
        attributeNodes = {
            @NamedAttributeNode("client")
        }
    ),
    @NamedEntityGraph(
        name = "AsnHeader.withClientAndLines",
        attributeNodes = {
            @NamedAttributeNode("client"),
            @NamedAttributeNode("lines")
        }
    )
})
@Getter
@Setter
public class AsnHeader extends BaseEntity {

    @Column(name = "status", nullable = false)
    private String status = "NEW";

    @Column(name = "asn_number", nullable = false)
    private String asnNumber;

    @Column(name = "asn_type", nullable = false)
    private Integer asnType;

    @Column(name = "business_partner_id")
    private String businessPartnerId;

    @Column(name = "business_partner_name")
    private String businessPartnerName;

    @Column(name = "receipt_dttm", nullable = false)
    private String receiptDttm;

    @Column(name = "asn_level", nullable = false)
    private Integer asnLevel;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "business_partner_address_1", length = 75)
    private String businessPartnerAddress1;

    @Column(name = "business_partner_address_2", length = 75)
    private String businessPartnerAddress2;

    @Column(name = "business_partner_address_3", length = 75)
    private String businessPartnerAddress3;

    @Column(name = "business_partner_city", length = 40)
    private String businessPartnerCity;

    @Column(name = "business_partner_state_prov", length = 3)
    private String businessPartnerStateProv;

    @Column(name = "business_partner_zip", length = 10)
    private String businessPartnerZip;

    @Column(name = "contact_address_1", length = 75)
    private String contactAddress1;

    @Column(name = "contact_address_2", length = 75)
    private String contactAddress2;

    @Column(name = "contact_address_3", length = 75)
    private String contactAddress3;

    @Column(name = "contact_city", length = 40)
    private String contactCity;

    @Column(name = "contact_state_prov", length = 3)
    private String contactStateProv;

    @Column(name = "contact_zip", length = 10)
    private String contactZip;

    @Column(name = "contact_number", length = 32)
    private String contactNumber;

    @Column(name = "appointment_id", length = 50)
    private String appointmentId;

    @Column(name = "appointment_dttm", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date appointmentDttm;

    @Column(name = "appointment_duration")
    private Long appointmentDuration;

    @Column(name = "driver_name", length = 50)
    private String driverName;

    @Column(name = "tractor_number", length = 50)
    private String tractorNumber;

    @Column(name = "delivery_stop_seq")
    private Integer deliveryStopSeq;

    @Column(name = "pickup_end_dttm", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date pickupEndDttm;

    @Column(name = "delivery_start_dttm", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date deliveryStartDttm;

    @Column(name = "delivery_end_dttm", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date deliveryEndDttm;

    @Column(name = "actual_departure_dttm", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date actualDepartureDttm;

    @Column(name = "actual_arrival_dttm", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date actualArrivalDttm;

    @Column(name = "total_weight", precision = 13, scale = 4)
    private BigDecimal totalWeight;

    @Column(name = "total_volume", precision = 13, scale = 4)
    private BigDecimal totalVolume;

    @Column(name = "volume_uom_id_base")
    private Long volumeUomIdBase;

    @Column(name = "total_shipped_qty", precision = 16, scale = 4)
    private BigDecimal totalShippedQty;

    @Column(name = "total_received_qty", precision = 16, scale = 4)
    private BigDecimal totalReceivedQty;

    @Column(name = "shipped_lpn_count")
    private Long shippedLpnCount;

    @Column(name = "received_lpn_count")
    private Long receivedLpnCount;

    @Column(name = "has_import_error", nullable = false)
    private Boolean hasImportError = false;

    @Column(name = "has_soft_check_error", nullable = false)
    private Boolean hasSoftCheckError = false;

    @Column(name = "has_alerts", nullable = false)
    private Boolean hasAlerts = false;

    @Column(name = "is_cogi_generated", nullable = false)
    private Boolean isCogiGenerated = false;

    @Column(name = "is_cancelled", nullable = false)
    private Boolean isCancelled = false;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;

    @Column(name = "is_gift", nullable = false)
    private Boolean isGift = false;

    @Column(name = "is_whse_transfer", length = 1, nullable = false)
    private String isWhseTransfer = "0";

    @Column(name = "quality_check_hold_upon_rcpt", length = 1)
    private String qualityCheckHoldUponRcpt;

    @Column(name = "quality_audit_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal qualityAuditPercent = BigDecimal.ZERO;

    @Column(name = "equipment_type", length = 8)
    private String equipmentType;

    @Column(name = "equipment_code", length = 20)
    private String equipmentCode;

    @Column(name = "equipment_code_id")
    private Long equipmentCodeId;

    @Column(name = "manif_nbr", length = 20)
    private String manifNbr;

    @Column(name = "manif_type", length = 4)
    private String manifType;

    @Column(name = "work_ord_nbr", length = 12)
    private String workOrdNbr;

    @Column(name = "cut_nbr", length = 12)
    private String cutNbr;

    @Column(name = "assigned_carrier_code", length = 10)
    private String assignedCarrierCode;

    @Column(name = "bill_of_lading_number", length = 30)
    private String billOfLadingNumber;

    @Column(name = "pro_number", length = 20)
    private String proNumber;

    @Column(name = "firm_appt_ind")
    private Integer firmApptInd;

    @Column(name = "buyer_code", length = 3)
    private String buyerCode;

    @Column(name = "asn_priority", nullable = false)
    private Integer asnPriority = 0;

    @Column(name = "schedule_appt", nullable = false)
    private Integer scheduleAppt = 0;

    @Column(name = "mfg_plnt", length = 3)
    private String mfgPlnt;

    @Column(name = "trailer_number", length = 20)
    private String trailerNumber;

    @Column(name = "destination_type", length = 1)
    private String destinationType;

    @Column(name = "contact_county", length = 40)
    private String contactCounty;

    @Column(name = "contact_country_code", length = 2)
    private String contactCountryCode;

    @Column(name = "receipt_variance", nullable = false)
    private Boolean receiptVariance = false;

    @Column(name = "receipt_type")
    private Integer receiptType;

    @Column(name = "variance_type")
    private Integer varianceType;

    @Column(name = "misc_instr_code_1", length = 25)
    private String miscInstrCode1;

    @Column(name = "misc_instr_code_2", length = 25)
    private String miscInstrCode2;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "ref_field_1", length = 25)
    private String refField1;

    @Column(name = "ref_field_2", length = 25)
    private String refField2;

    @Column(name = "ref_field_3", length = 25)
    private String refField3;

    @Column(name = "ref_field_4", length = 25)
    private String refField4;

    @Column(name = "ref_field_5", length = 25)
    private String refField5;

    @Column(name = "ref_field_6", length = 25)
    private String refField6;

    @Column(name = "ref_field_7", length = 25)
    private String refField7;

    @Column(name = "ref_field_8", length = 25)
    private String refField8;

    @Column(name = "ref_field_9", length = 25)
    private String refField9;

    @Column(name = "ref_field_10", length = 25)
    private String refField10;

    @Column(name = "ref_num1", precision = 13, scale = 5)
    private BigDecimal refNum1;

    @Column(name = "ref_num2", precision = 13, scale = 5)
    private BigDecimal refNum2;

    @Column(name = "ref_num3", precision = 13, scale = 5)
    private BigDecimal refNum3;

    @Column(name = "ref_num4", precision = 13, scale = 5)
    private BigDecimal refNum4;

    @Column(name = "ref_num5", precision = 13, scale = 5)
    private BigDecimal refNum5;

    @Column(name = "shipping_cost", precision = 13, scale = 4)
    private BigDecimal shippingCost;

    @Column(name = "shipping_cost_currency_code", length = 3)
    private String shippingCostCurrencyCode;

    @Column(name = "invoice_date", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date invoiceDate;

    @Column(name = "invoice_number", length = 30)
    private String invoiceNumber;

    @Version
    @Column(name = "hibernate_version")
    private Long hibernateVersion;

    @Column(name = "created_source_type", nullable = false)
    private Integer createdSourceType = 0;

    @Column(name = "created_source", length = 50)
    private String createdSource;

    @Column(name = "last_updated_source_type", nullable = false)
    private Integer lastUpdatedSourceType = 0;

    @Column(name = "last_updated_source", length = 50)
    private String lastUpdatedSource;

    @OneToMany(mappedBy = "header", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<AsnLine> lines = new HashSet<>();
}