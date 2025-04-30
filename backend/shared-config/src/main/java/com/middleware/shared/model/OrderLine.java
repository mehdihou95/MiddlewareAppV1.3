package com.middleware.shared.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;			 
import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "order_lines")
@AttributeOverride(name = "id", column = @Column(name = "line_id"))
@Getter
@Setter
public class OrderLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private OrderHeader orderHeader;

    @Column(name = "line_number")
    private String lineNumber;

    @Column(name = "item_number")
    private String itemNumber;

    @Column(name = "item_description")
    private String itemDescription;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "status")
    private String status;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_name", length = 100)
    private String itemName;

    @Column(name = "item_attr_1", length = 10)
    private String itemAttr1;

    @Column(name = "item_attr_2", length = 10)
    private String itemAttr2;

    @Column(name = "item_attr_3", length = 10)
    private String itemAttr3;

    @Column(name = "item_attr_4", length = 10)
    private String itemAttr4;

    @Column(name = "item_attr_5", length = 10)
    private String itemAttr5;

    @Column(name = "package_type_id")
    private Long packageTypeId;

    @Column(name = "package_type_desc", length = 50)
    private String packageTypeDesc;

    @Column(name = "package_type_instance", length = 100)
    private String packageTypeInstance;

    @Column(name = "epc_tracking_rfid_value", length = 32)
    private String epcTrackingRfidValue;

    @Column(name = "gtin", length = 25)
    private String gtin;

    @Column(name = "shipped_qty", precision = 16, scale = 4)
    private BigDecimal shippedQty;

    @Column(name = "std_pack_qty", precision = 13, scale = 4)
    private BigDecimal stdPackQty;

    @Column(name = "std_case_qty", precision = 16, scale = 4)
    private BigDecimal stdCaseQty;

    @Column(name = "order_detail_status", nullable = false)
    private Integer orderDetailStatus = 4;

    @Column(name = "std_sub_pack_qty", precision = 13, scale = 4)
    private BigDecimal stdSubPackQty;

    @Column(name = "lpn_per_tier")
    private Integer lpnPerTier;

    @Column(name = "tier_per_pallet")
    private Integer tierPerPallet;

    @Column(name = "mfg_plnt", length = 3)
    private String mfgPlnt;

    @Column(name = "mfg_date", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date mfgDate;

    @Column(name = "ship_by_date", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date shipByDate;

    @Column(name = "expire_date", columnDefinition = "DATE")
    @Temporal(TemporalType.DATE)
    private Date expireDate;

    @Column(name = "weight_uom_id_base")
    private Long weightUomIdBase;

    @Column(name = "is_cancelled", nullable = false)
    private Integer isCancelled = 0;

    @Column(name = "invn_type", length = 1)
    private String invnType;

    @Column(name = "prod_stat", length = 3)
    private String prodStat;

    @Column(name = "cntry_of_orgn", length = 4)
    private String cntryOfOrgn;

    @Column(name = "shipped_lpn_count")
    private Long shippedLpnCount;

    @Column(name = "units_assigned_to_lpn", precision = 16, scale = 4)
    private BigDecimal unitsAssignedToLpn;

    @Column(name = "proc_immd_needs", length = 1)
    private String procImmdNeeds;

    @Column(name = "quality_check_hold_upon_rcpt", length = 1)
    private String qualityCheckHoldUponRcpt;

    @Column(name = "reference_order_nbr", length = 12)
    private String referenceOrderNbr;

    @Column(name = "actual_weight", precision = 13, scale = 4)
    private BigDecimal actualWeight;

    @Column(name = "actual_weight_pack_count", precision = 13, scale = 4)
    private BigDecimal actualWeightPackCount;

    @Column(name = "nbr_of_pack_for_catch_wt", precision = 13, scale = 4)
    private BigDecimal nbrOfPackForCatchWt;

    @Column(name = "retail_price", precision = 16, scale = 4)
    private BigDecimal retailPrice;

    @Column(name = "created_source_type", nullable = false)
    private Integer createdSourceType = 1;

    @Column(name = "created_source", length = 50)
    private String createdSource;

    @Column(name = "last_updated_source_type", nullable = false)
    private Integer lastUpdatedSourceType = 1;

    @Column(name = "last_updated_source", length = 50)
    private String lastUpdatedSource;

    @Column(name = "hibernate_version")
    private Long hibernateVersion;

    @Column(name = "cut_nbr", length = 12)
    private String cutNbr;

    @Column(name = "qty_conv_factor", precision = 17, scale = 8, nullable = false)
    private BigDecimal qtyConvFactor = BigDecimal.ONE;

    @Column(name = "qty_uom_id")
    private Long qtyUomId;

    @Column(name = "weight_uom_id")
    private Long weightUomId;

    @Column(name = "qty_uom_id_base")
    private Long qtyUomIdBase;

    @Column(name = "exp_receive_condition_code", length = 10)
    private String expReceiveConditionCode;

    @Column(name = "order_recv_rules", length = 200)
    private String orderRecvRules;

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

    @Column(name = "disposition_type", length = 3)
    private String dispositionType;

    @Column(name = "inv_disposition", length = 15)
    private String invDisposition;

    @Column(name = "purchase_orders_line_item_id")
    private Long purchaseOrdersLineItemId;

    @Column(name = "notes", length = 500)
    private String notes;

    // Compatibility method for tests
    public void setLineNumber(String lineNumberStr) {
        if (lineNumberStr == null || lineNumberStr.trim().isEmpty()) {
            this.lineNumber = null;
            return;
        }
        this.lineNumber = lineNumberStr.trim();
    }
} 