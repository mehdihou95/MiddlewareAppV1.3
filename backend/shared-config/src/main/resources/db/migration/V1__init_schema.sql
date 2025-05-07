-- Create base configuration tables-- Create Clients table first (no dependencies)
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create update trigger for clients
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_clients_timestamp
    BEFORE UPDATE ON clients
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Create Interfaces table (depends on CLIENTS)
CREATE TABLE interfaces (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL CHECK (length(name) >= 3),
    type VARCHAR(20) NOT NULL,
    description VARCHAR(500),
    schema_path VARCHAR(255),
    root_element VARCHAR(100) NOT NULL,
    namespace VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT uk_client_name UNIQUE (client_id, name)
);

CREATE TRIGGER update_interfaces_timestamp
    BEFORE UPDATE ON interfaces
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Create Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    password_reset_token VARCHAR(255),
    password_reset_expiry TIMESTAMP,
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_users_timestamp
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Create User Roles table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    roles VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create ASN Headers table
CREATE TABLE asn_headers (
    asn_id BIGSERIAL PRIMARY KEY,
    asn_number VARCHAR(50) NOT NULL,
    asn_type INTEGER NOT NULL,
    business_partner_id VARCHAR(50),
    business_partner_name VARCHAR(50),
    receipt_dttm TIMESTAMP NOT NULL,
    asn_level INTEGER NOT NULL DEFAULT 0,
    region_id BIGINT,
    business_partner_address_1 VARCHAR(75),
    business_partner_address_2 VARCHAR(75),
    business_partner_address_3 VARCHAR(75),
    business_partner_city VARCHAR(40),
    business_partner_state_prov VARCHAR(3),
    business_partner_zip VARCHAR(10),
    contact_address_1 VARCHAR(75),
    contact_address_2 VARCHAR(75),
    contact_address_3 VARCHAR(75),
    contact_city VARCHAR(40),
    contact_state_prov VARCHAR(3),
    contact_zip VARCHAR(10),
    contact_number VARCHAR(32),
    appointment_id VARCHAR(50),
    appointment_dttm DATE,
    appointment_duration BIGINT,
    driver_name VARCHAR(50),
    tractor_number VARCHAR(50),
    delivery_stop_seq INTEGER,
    pickup_end_dttm DATE,
    delivery_start_dttm DATE,
    delivery_end_dttm DATE,
    actual_departure_dttm DATE,
    actual_arrival_dttm DATE,
    total_weight NUMERIC(13,4),
    total_volume NUMERIC(13,4),
    volume_uom_id_base BIGINT,
    total_shipped_qty NUMERIC(16,4),
    total_received_qty NUMERIC(16,4),
    shipped_lpn_count BIGINT,
    received_lpn_count BIGINT,
    has_import_error BOOLEAN NOT NULL DEFAULT FALSE,
    has_soft_check_error BOOLEAN NOT NULL DEFAULT FALSE,
    has_alerts BOOLEAN NOT NULL DEFAULT FALSE,
    is_cogi_generated BOOLEAN NOT NULL DEFAULT FALSE,
    is_cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    is_gift BOOLEAN NOT NULL DEFAULT FALSE,
    is_whse_transfer VARCHAR(1) NOT NULL DEFAULT '0',
    quality_check_hold_upon_rcpt VARCHAR(1),
    quality_audit_percent NUMERIC(5,2) NOT NULL DEFAULT 0,
    equipment_type VARCHAR(8),
    equipment_code VARCHAR(20),
    equipment_code_id BIGINT,
    manif_nbr VARCHAR(20),
    manif_type VARCHAR(4),
    work_ord_nbr VARCHAR(12),
    cut_nbr VARCHAR(12),
    assigned_carrier_code VARCHAR(10),
    bill_of_lading_number VARCHAR(30),
    pro_number VARCHAR(20),
    firm_appt_ind INTEGER,
    buyer_code VARCHAR(3),
    asn_priority INTEGER NOT NULL DEFAULT 0,
    schedule_appt INTEGER NOT NULL DEFAULT 0,
    mfg_plnt VARCHAR(3),
    trailer_number VARCHAR(20),
    destination_type VARCHAR(1),
    contact_county VARCHAR(40),
    contact_country_code VARCHAR(2),
    receipt_variance BOOLEAN NOT NULL DEFAULT FALSE,
    receipt_type INTEGER,
    variance_type INTEGER,
    misc_instr_code_1 VARCHAR(25),
    misc_instr_code_2 VARCHAR(25),
    ref_field_1 VARCHAR(25),
    ref_field_2 VARCHAR(25),
    ref_field_3 VARCHAR(25),
    ref_field_4 VARCHAR(25),
    ref_field_5 VARCHAR(25),
    ref_field_6 VARCHAR(25),
    ref_field_7 VARCHAR(25),
    ref_field_8 VARCHAR(25),
    ref_field_9 VARCHAR(25),
    ref_field_10 VARCHAR(25),
    ref_num1 NUMERIC(13,5),
    ref_num2 NUMERIC(13,5),
    ref_num3 NUMERIC(13,5),
    ref_num4 NUMERIC(13,5),
    ref_num5 NUMERIC(13,5),
    shipping_cost NUMERIC(13,4),
    shipping_cost_currency_code VARCHAR(3),
    invoice_date DATE,
    invoice_number VARCHAR(30),
    hibernate_version BIGINT DEFAULT 0,
    created_source_type INTEGER NOT NULL DEFAULT 0,
    created_source VARCHAR(50),
    last_updated_source_type INTEGER NOT NULL DEFAULT 0,
    last_updated_source VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    notes VARCHAR(1000),
    client_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TRIGGER update_asn_headers_timestamp
    BEFORE UPDATE ON asn_headers
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Create ASN Lines table
CREATE TABLE asn_lines (
    line_id BIGSERIAL PRIMARY KEY,
    header_asn_id BIGINT NOT NULL,
    line_number VARCHAR(255),
    item_number VARCHAR(50),
    item_description VARCHAR(255),
    quantity INTEGER,
    unit_of_measure VARCHAR(50),
    lot_number VARCHAR(50),
    serial_number VARCHAR(50),
    status VARCHAR(20),
    item_id BIGINT,
    item_name VARCHAR(100),
    item_attr_1 VARCHAR(10),
    item_attr_2 VARCHAR(10),
    item_attr_3 VARCHAR(10),
    item_attr_4 VARCHAR(10),
    item_attr_5 VARCHAR(10),
    package_type_id BIGINT,
    package_type_desc VARCHAR(50),
    package_type_instance VARCHAR(100),
    epc_tracking_rfid_value VARCHAR(32),
    gtin VARCHAR(25),
    shipped_qty NUMERIC(16,4),
    std_pack_qty NUMERIC(13,4),
    std_case_qty NUMERIC(16,4),
    asn_detail_status INTEGER NOT NULL DEFAULT 4,
    std_sub_pack_qty NUMERIC(13,4),
    lpn_per_tier INTEGER,
    tier_per_pallet INTEGER,
    mfg_plnt VARCHAR(3),
    mfg_date DATE,
    ship_by_date DATE,
    expire_date DATE,
    weight_uom_id_base BIGINT,
    is_cancelled INTEGER NOT NULL DEFAULT 0,
    invn_type VARCHAR(1),
    prod_stat VARCHAR(3),
    cntry_of_orgn VARCHAR(4),
    shipped_lpn_count BIGINT,
    units_assigned_to_lpn NUMERIC(16,4),
    proc_immd_needs VARCHAR(1),
    quality_check_hold_upon_rcpt VARCHAR(1),
    reference_order_nbr VARCHAR(12),
    actual_weight NUMERIC(13,4),
    actual_weight_pack_count NUMERIC(13,4),
    nbr_of_pack_for_catch_wt NUMERIC(13,4),
    retail_price NUMERIC(16,4),
    created_source_type INTEGER NOT NULL DEFAULT 1,
    created_source VARCHAR(50),
    last_updated_source_type INTEGER NOT NULL DEFAULT 1,
    last_updated_source VARCHAR(50),
    hibernate_version BIGINT,
    cut_nbr VARCHAR(12),
    qty_conv_factor NUMERIC(17,8) NOT NULL DEFAULT 1,
    qty_uom_id BIGINT,
    weight_uom_id BIGINT,
    qty_uom_id_base BIGINT,
    exp_receive_condition_code VARCHAR(10),
    asn_recv_rules VARCHAR(200),
    ref_field_1 VARCHAR(25),
    ref_field_2 VARCHAR(25),
    ref_field_3 VARCHAR(25),
    ref_field_4 VARCHAR(25),
    ref_field_5 VARCHAR(25),
    ref_field_6 VARCHAR(25),
    ref_field_7 VARCHAR(25),
    ref_field_8 VARCHAR(25),
    ref_field_9 VARCHAR(25),
    ref_field_10 VARCHAR(25),
    ref_num1 NUMERIC(13,5),
    ref_num2 NUMERIC(13,5),
    ref_num3 NUMERIC(13,5),
    ref_num4 NUMERIC(13,5),
    ref_num5 NUMERIC(13,5),
    disposition_type VARCHAR(3),
    inv_disposition VARCHAR(15),
    purchase_orders_line_item_id BIGINT,
    notes VARCHAR(500),
    client_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (header_asn_id) REFERENCES asn_headers(asn_id),
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TRIGGER update_asn_lines_timestamp
    BEFORE UPDATE ON asn_lines
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Create Processed Files table
CREATE TABLE processed_files (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    error_message VARCHAR(1000),
    content TEXT,
    processed_at TIMESTAMP NOT NULL,
    client_id BIGINT NOT NULL,
    interface_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (interface_id) REFERENCES interfaces(id)
);

CREATE TRIGGER update_processed_files_timestamp
    BEFORE UPDATE ON processed_files
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Create Mapping Rules table
CREATE TABLE mapping_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    xml_path VARCHAR(255) NOT NULL,
    database_field VARCHAR(255) NOT NULL,
    transformation VARCHAR(255),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    default_value VARCHAR(255),
    priority INTEGER,
    source_field VARCHAR(255),
    target_field VARCHAR(255),
    target_level VARCHAR(20) NOT NULL DEFAULT 'HEADER',
    validation_rule VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    table_name VARCHAR(255),
    data_type VARCHAR(50),
    is_attribute BOOLEAN NOT NULL DEFAULT FALSE,
    xsd_element VARCHAR(255),
    is_default BOOLEAN,
    transformation_rule TEXT,
    description VARCHAR(500),
    interface_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (interface_id) REFERENCES interfaces(id),
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TRIGGER update_mapping_rules_timestamp
    BEFORE UPDATE ON mapping_rules
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Create HTTP Audit Logs table
CREATE TABLE http_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    entity_id BIGINT NOT NULL,
    client_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    request_method VARCHAR(10),
    request_url TEXT,
    request_params TEXT,
    response_status INTEGER,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    execution_time BIGINT,
    duration BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

-- Create Method Audit Logs table
CREATE TABLE method_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    method VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    error TEXT,
    duration BIGINT NOT NULL,
    level VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);

-- Create SFTP Config table
CREATE TABLE sftp_config (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    interface_id BIGINT NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL DEFAULT 22,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    private_key TEXT,
    private_key_passphrase VARCHAR(255),
    remote_directory VARCHAR(255) NOT NULL,
    file_pattern VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    connection_timeout INTEGER DEFAULT 30000,
    channel_timeout INTEGER DEFAULT 15000,
    thread_pool_size INTEGER DEFAULT 4,
    retry_attempts INTEGER DEFAULT 3,
    retry_delay INTEGER DEFAULT 5000,
    polling_interval INTEGER DEFAULT 60000,
    monitored_directories JSONB,
    processed_directory VARCHAR(255),
    error_directory VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (interface_id) REFERENCES interfaces(id),
    CONSTRAINT unique_client_interface UNIQUE (client_id, interface_id)
);

CREATE TRIGGER update_sftp_config_timestamp
    BEFORE UPDATE ON sftp_config
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Create AS2 Config table
CREATE TABLE as2_config (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    interface_id BIGINT NOT NULL,
    server_id VARCHAR(255) NOT NULL,
    partner_id VARCHAR(255) NOT NULL,
    local_id VARCHAR(255) NOT NULL,
    partner_as2_id VARCHAR(255) NOT NULL,
    partner_as2_url VARCHAR(255) NOT NULL,
    certificate_alias VARCHAR(255),
    encryption_algorithm VARCHAR(50) DEFAULT 'AES256',
    signature_algorithm VARCHAR(50) DEFAULT 'SHA256',
    mdn_mode VARCHAR(50) DEFAULT 'SYNC',
    mdn_digest_algorithm VARCHAR(50) DEFAULT 'SHA256',
    encrypt_message BOOLEAN DEFAULT TRUE,
    sign_message BOOLEAN DEFAULT TRUE,
    request_mdn BOOLEAN DEFAULT TRUE,
    mdn_url VARCHAR(255),
    compression BOOLEAN DEFAULT TRUE,
    retry_count INTEGER DEFAULT 3,
    retry_interval INTEGER DEFAULT 60,
    api_name VARCHAR(50) DEFAULT 'SERVER',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (interface_id) REFERENCES interfaces(id)
);

CREATE TRIGGER update_as2_config_timestamp
    BEFORE UPDATE ON as2_config
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Create indexes
CREATE INDEX idx_clients_id ON clients(id);
CREATE INDEX idx_clients_code ON clients(code);
CREATE INDEX idx_interfaces_client_type ON interfaces(client_id, type);
CREATE INDEX idx_interfaces_type ON interfaces(type);
CREATE INDEX idx_asn_headers_document_number ON asn_headers(asn_number);
CREATE INDEX idx_asn_lines_item_number ON asn_lines(item_number);
CREATE INDEX idx_processed_files_file_name ON processed_files(file_name);
CREATE INDEX idx_mapping_rules_source_field ON mapping_rules(source_field);
CREATE INDEX idx_mapping_rules_target_level ON mapping_rules(target_level);
CREATE INDEX idx_asn_headers_client_id ON asn_headers(client_id);
CREATE INDEX idx_asn_lines_client_id ON asn_lines(client_id);
CREATE INDEX idx_processed_files_client_id ON processed_files(client_id);
CREATE INDEX idx_mapping_rules_client_id ON mapping_rules(client_id);
CREATE INDEX idx_mapping_rules_interface_id ON mapping_rules(interface_id);
CREATE INDEX idx_http_audit_logs_username ON http_audit_logs(username);
CREATE INDEX idx_http_audit_logs_client_id ON http_audit_logs(client_id);
CREATE INDEX idx_http_audit_logs_created_at ON http_audit_logs(created_at);
CREATE INDEX idx_method_audit_logs_username ON method_audit_logs(username);
CREATE INDEX idx_method_audit_logs_created_at ON method_audit_logs(created_at);
CREATE INDEX idx_processed_files_interface_id ON processed_files(interface_id);

-- Insert default client
INSERT INTO clients (name, code, description, status)
VALUES ('DEFAULT_CLIENT', 'DEFAULT', 'Default client for existing data', 'ACTIVE');

-- Create Order Headers table
CREATE TABLE order_headers (
    order_id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL,
    creation_type VARCHAR(20),
    business_partner_id BIGINT,
    business_partner_name VARCHAR(100),
    order_date_dttm TIMESTAMP,
    order_recon_dttm TIMESTAMP,
    status VARCHAR(20),
    notes VARCHAR(500),
    ext_purchase_order VARCHAR(50),
    cons_run_id BIGINT,
    o_facility_alias_id VARCHAR(16),
    o_facility_id BIGINT,
    o_dock_id VARCHAR(8),
    o_address_1 VARCHAR(75),
    o_address_2 VARCHAR(75),
    o_address_3 VARCHAR(75),
    o_city VARCHAR(40),
    o_state_prov VARCHAR(3),
    o_postal_code VARCHAR(10),
    o_county VARCHAR(40),
    o_country_code VARCHAR(2),
    d_facility_alias_id VARCHAR(16),
    d_facility_id BIGINT,
    d_dock_id VARCHAR(8),
    d_address_1 VARCHAR(75),
    d_address_2 VARCHAR(75),
    d_address_3 VARCHAR(75),
    d_city VARCHAR(40),
    d_state_prov VARCHAR(3),
    d_postal_code VARCHAR(10),
    d_county VARCHAR(40),
    d_country_code VARCHAR(2),
    bill_to_name VARCHAR(91),
    bill_facility_alias_id VARCHAR(16),
    bill_facility_id BIGINT,
    bill_to_address_1 VARCHAR(75),
    bill_to_address_2 VARCHAR(75),
    bill_to_address_3 VARCHAR(75),
    bill_to_city VARCHAR(50),
    bill_to_state_prov VARCHAR(3),
    bill_to_county VARCHAR(40),
    bill_to_postal_code VARCHAR(10),
    bill_to_country_code VARCHAR(2),
    bill_to_phone_number VARCHAR(32),
    bill_to_fax_number VARCHAR(30),
    bill_to_email VARCHAR(256),
    incoterm_facility_id BIGINT,
    incoterm_facility_alias_id VARCHAR(16),
    incoterm_loc_ava_dttm TIMESTAMP,
    incoterm_loc_ava_time_zone_id BIGINT,
    pickup_tz INTEGER,
    delivery_tz INTEGER,
    pickup_start_dttm TIMESTAMP,
    pickup_end_dttm TIMESTAMP,
    delivery_start_dttm TIMESTAMP,
    delivery_end_dttm TIMESTAMP,
    dsg_service_level_id BIGINT,
    dsg_carrier_id BIGINT,
    dsg_equipment_id BIGINT,
    dsg_tractor_equipment_id BIGINT,
    dsg_mot_id BIGINT,
    baseline_mot_id BIGINT,
    baseline_service_level_id BIGINT,
    product_class_id BIGINT,
    protection_level_id BIGINT,
    path_id BIGINT,
    path_set_id BIGINT,
    driver_type_id BIGINT,
    un_number_id BIGINT,
    block_auto_create INTEGER NOT NULL DEFAULT 0,
    block_auto_consolidate INTEGER NOT NULL DEFAULT 0,
    has_split INTEGER NOT NULL DEFAULT 0,
    is_booking_required INTEGER NOT NULL DEFAULT 0,
    is_cancelled INTEGER NOT NULL DEFAULT 0,
    is_hazmat INTEGER NOT NULL DEFAULT 0,
    is_imported INTEGER NOT NULL DEFAULT 0,
    is_partially_planned INTEGER NOT NULL DEFAULT 0,
    is_perishable INTEGER NOT NULL DEFAULT 0,
    is_suspended INTEGER NOT NULL DEFAULT 0,
    normalized_baseline_cost NUMERIC(13,4),
    baseline_cost_currency_code VARCHAR(3),
    orig_budg_cost NUMERIC(13,4),
    budg_cost NUMERIC(13,4),
    actual_cost NUMERIC(13,4),
    baseline_cost NUMERIC(13,4),
    trans_resp_code VARCHAR(3),
    billing_method INTEGER,
    priority INTEGER,
    equipment_type INTEGER,
    mv_currency_code VARCHAR(3),
    compartment_no INTEGER,
    packaging VARCHAR(15),
    order_loading_seq BIGINT,
    ref_field_1 VARCHAR(25),
    ref_field_2 VARCHAR(25),
    ref_field_3 VARCHAR(25),
    created_source_type INTEGER NOT NULL DEFAULT 0,
    created_source VARCHAR(50),
    last_updated_source_type INTEGER NOT NULL DEFAULT 0,
    last_updated_source VARCHAR(50),
    created_dttm TIMESTAMP,
    last_updated_dttm TIMESTAMP,
    hibernate_version BIGINT,
    actual_cost_currency_code VARCHAR(3),
    budg_cost_currency_code VARCHAR(3),
    client_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

-- Create Order Lines table
CREATE TABLE order_lines (
    line_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    line_number VARCHAR(255),
    item_number VARCHAR(50),
    item_description VARCHAR(255),
    quantity INTEGER,
    unit_of_measure VARCHAR(50),
    lot_number VARCHAR(50),
    serial_number VARCHAR(50),
    status VARCHAR(20),
    item_id BIGINT,
    item_name VARCHAR(100),
    item_attr_1 VARCHAR(10),
    item_attr_2 VARCHAR(10),
    item_attr_3 VARCHAR(10),
    item_attr_4 VARCHAR(10),
    item_attr_5 VARCHAR(10),
    package_type_id BIGINT,
    package_type_desc VARCHAR(50),
    package_type_instance VARCHAR(100),
    epc_tracking_rfid_value VARCHAR(32),
    gtin VARCHAR(25),
    shipped_qty NUMERIC(16,4),
    std_pack_qty NUMERIC(13,4),
    std_case_qty NUMERIC(16,4),
    order_detail_status INTEGER NOT NULL DEFAULT 4,
    std_sub_pack_qty NUMERIC(13,4),
    lpn_per_tier INTEGER,
    tier_per_pallet INTEGER,
    mfg_plnt VARCHAR(3),
    mfg_date DATE,
    ship_by_date DATE,
    expire_date DATE,
    weight_uom_id_base BIGINT,
    is_cancelled INTEGER NOT NULL DEFAULT 0,
    invn_type VARCHAR(1),
    prod_stat VARCHAR(3),
    cntry_of_orgn VARCHAR(4),
    shipped_lpn_count BIGINT,
    units_assigned_to_lpn NUMERIC(16,4),
    proc_immd_needs VARCHAR(1),
    quality_check_hold_upon_rcpt VARCHAR(1),
    reference_order_nbr VARCHAR(12),
    actual_weight NUMERIC(13,4),
    actual_weight_pack_count NUMERIC(13,4),
    nbr_of_pack_for_catch_wt NUMERIC(13,4),
    retail_price NUMERIC(16,4),
    created_source_type INTEGER NOT NULL DEFAULT 1,
    created_source VARCHAR(50),
    last_updated_source_type INTEGER NOT NULL DEFAULT 1,
    last_updated_source VARCHAR(50),
    hibernate_version BIGINT,
    cut_nbr VARCHAR(12),
    qty_conv_factor NUMERIC(17,8) NOT NULL DEFAULT 1,
    qty_uom_id BIGINT,
    weight_uom_id BIGINT,
    qty_uom_id_base BIGINT,
    exp_receive_condition_code VARCHAR(10),
    order_recv_rules VARCHAR(200),
    ref_field_1 VARCHAR(25),
    ref_field_2 VARCHAR(25),
    ref_field_3 VARCHAR(25),
    ref_field_4 VARCHAR(25),
    ref_field_5 VARCHAR(25),
    ref_field_6 VARCHAR(25),
    ref_field_7 VARCHAR(25),
    ref_field_8 VARCHAR(25),
    ref_field_9 VARCHAR(25),
    ref_field_10 VARCHAR(25),
    ref_num1 NUMERIC(13,5),
    ref_num2 NUMERIC(13,5),
    ref_num3 NUMERIC(13,5),
    ref_num4 NUMERIC(13,5),
    ref_num5 NUMERIC(13,5),
    disposition_type VARCHAR(3),
    inv_disposition VARCHAR(15),
    purchase_orders_line_item_id BIGINT,
    notes VARCHAR(500),
    client_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES order_headers(order_id),
    FOREIGN KEY (client_id) REFERENCES clients(id)
);

CREATE TRIGGER update_order_lines_timestamp
    BEFORE UPDATE ON order_lines
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();